/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public abstract class AbstractTransitDriver implements TransitDriverAgent, PassengerAccessEgress, PlanAgent {

	private static final Logger log = Logger.getLogger(AbstractTransitDriver.class);

	private TransitVehicle vehicle = null;

	private int nextLinkIndex = 0;
	private final TransitStopAgentTracker agentTracker;
	private Person dummyPerson;
	private TransitRouteStop currentStop = null;
	protected TransitRouteStop nextStop;
	private ListIterator<TransitRouteStop> stopIterator;
	private InternalInterface internalInterface;
	private final boolean isGeneratingDeniedBoardingEvents ;
	
	/* package */ MobsimAgent.State state = MobsimAgent.State.ACTIVITY ; 
	// yy not so great: implicit instantiation at activity.  kai, nov'11
	@Override
	public MobsimAgent.State getState() {
		return this.state ;
	}

	@Override
	public abstract void endLegAndComputeNextState(final double now);
	public abstract NetworkRoute getCarRoute();
	public abstract TransitLine getTransitLine();
	public abstract TransitRoute getTransitRoute();
	public abstract Departure getDeparture();
	@Override
	public abstract double getActivityEndTime();

	public AbstractTransitDriver(InternalInterface internalInterface, TransitStopAgentTracker agentTracker2) {
		super();
		this.internalInterface = internalInterface;
		this.agentTracker = agentTracker2;
		if ( this.internalInterface != null ) {
			this.isGeneratingDeniedBoardingEvents = Boolean.parseBoolean(
					this.internalInterface.getMobsim().getScenario().getConfig().vspExperimental().getValue(
							VspExperimentalConfigKey.isGeneratingBoardingDeniedEvent
					) ) ;
		} else {
			this.isGeneratingDeniedBoardingEvents = false ;
		}
	}

	protected void init() {
		if (getTransitRoute() != null) {
			this.stopIterator = getTransitRoute().getStops().listIterator();
			this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		} else {
			this.nextStop = null;
		}
		this.nextLinkIndex = 0;
	}

	protected void setDriver(Person personImpl) {
		this.dummyPerson = personImpl;
	}

	@Override
	public Id chooseNextLinkId() {
		NetworkRoute netR = getCarRoute();
		List<Id> linkIds = netR.getLinkIds();
		if (this.nextLinkIndex < linkIds.size()) {
			return linkIds.get(this.nextLinkIndex);
		}
		if (this.nextLinkIndex == linkIds.size()) {
			return netR.getEndLinkId();
		}
		return null;
	}
	
	@Override
	public void abort( final double now ) {
		this.state = MobsimAgent.State.ABORT ;
	}

	@Override
	public Id getCurrentLinkId() {
		int currentLinkIndex = this.nextLinkIndex - 1;
		if (currentLinkIndex < 0) {
			return getCarRoute().getStartLinkId();
		} else if (currentLinkIndex >= getCarRoute().getLinkIds().size()) {
			return getCarRoute().getEndLinkId();
		} else {
			return getCarRoute().getLinkIds().get(currentLinkIndex);
		}
	}

	@Override
	public void notifyMoveOverNode(Id nextLinkId) {
		this.nextLinkIndex++;
	}

	@Override
	public TransitStopFacility getNextTransitStop() {
		if (this.nextStop == null) {
			return null;
		}
		return this.nextStop.getStopFacility();
	}

	@Override
	public double handleTransitStop(final TransitStopFacility stop, final double now) {
		assertExpectedStop(stop);
		processEventVehicleArrives(stop, now);
		ArrayList<PTPassengerAgent> passengersLeaving = findPassengersLeaving(stop);
		int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size() + passengersLeaving.size();
		List<PTPassengerAgent> passengersEntering = findPassengersEntering(stop, freeCapacity, now);
		double stopTime = this.vehicle.getStopHandler().handleTransitStop(stop, now, passengersLeaving, passengersEntering, this);
		if(stopTime == 0.0){
			stopTime = longerStopTimeIfWeAreAheadOfSchedule(now, stopTime);
		}
		if (stopTime == 0.0) {
			depart(now);
		}
		return stopTime;
	}

	protected final void sendTransitDriverStartsEvent(final double now) {
		// check if "Wenden"
		if(getTransitLine() == null){
			this.internalInterface.getMobsim().getEventsManager().processEvent(new TransitDriverStartsEvent(now, this.dummyPerson.getId(),
					this.vehicle.getId(), new IdImpl("Wenden"), new IdImpl("Wenden"), new IdImpl("Wenden")));
		} else {
			this.internalInterface.getMobsim().getEventsManager().processEvent(new TransitDriverStartsEvent(now, this.dummyPerson.getId(),
					this.vehicle.getId(), getTransitLine().getId(), getTransitRoute().getId(), getDeparture().getId()));
		}
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(final Id linkId) {
	}

	Netsim getSimulation(){
		return this.internalInterface.getMobsim();
	}

	/**Design comments:<ul>
	 * <li> Keeping this for the time being, since the derived methods somehow need to get the selected plan.  Might
	 * keep track of the selected plan directly, but someone would need to look more into the design. kai, jun'11
	 * <li> For that reason, I made the method package-private.  There is, however, probably not much harm to make 
	 * it public again as long as it is not part of the PlanDriverAgent interface.  kai, jun'11
	 * </ul>
	 */
	Person getPerson() {
		return this.dummyPerson;
	}

	@Override
	public TransitVehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public void setVehicle(final MobsimVehicle vehicle) {
		// QVehicle to fulfill the interface; should be a TransitVehicle at runtime!
		this.vehicle = (TransitVehicle) vehicle;
	}

	private void processEventVehicleArrives(final TransitStopFacility stop,
			final double now) {
		EventsManager events = this.internalInterface.getMobsim().getEventsManager();
		if (this.currentStop == null) {
			this.currentStop = this.nextStop;
			events.processEvent(new VehicleArrivesAtFacilityEvent(now, this.vehicle.getVehicle().getId(), stop.getId(),
					now - this.getDeparture().getDepartureTime() - this.currentStop.getDepartureOffset()));
		}
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		if (stop != this.nextStop.getStopFacility()) {
			throw new RuntimeException("Expected different stop.");
		}
	}

	protected double longerStopTimeIfWeAreAheadOfSchedule(final double now,
			final double stopTime) {
		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = getActivityEndTime() + this.nextStop.getDepartureOffset();
			if (now + stopTime < earliestDepTime) {
				return earliestDepTime - now;
			}
		}
		return stopTime;
	}

	private void depart(final double now) {
		EventsManager events = this.internalInterface.getMobsim().getEventsManager();
		events.processEvent(new VehicleDepartsAtFacilityEvent(now, this.vehicle.getVehicle().getId(),
				this.currentStop.getStopFacility().getId(),
				now - this.getDeparture().getDepartureTime() - this.currentStop.getDepartureOffset()));
		this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		if(this.nextStop == null) {
			assertVehicleIsEmpty();
		}
		this.currentStop = null;
	}

	private void assertVehicleIsEmpty() {
		if (this.vehicle.getPassengers().size() > 0) {
			RuntimeException e = new RuntimeException("Transit vehicle is at last stop but still contains passengers that did not leave the vehicle!");
			log.error("Transit vehicle must be empty after last stop! vehicle-id = " + this.vehicle.getVehicle().getId(), e);
			for (PassengerAgent agent : this.vehicle.getPassengers()) {
				if (agent instanceof PersonDriverAgentImpl) {
				log.error("Agent is still in transit vehicle: agent-id = " + ((PersonDriverAgentImpl) agent).getPerson().getId());
				}
			}
			throw e;
		}
	}

	@Override
	public boolean handlePassengerEntering(final PTPassengerAgent passenger, final double time) {
		boolean handled = this.vehicle.addPassenger(passenger);
		if(handled){
			this.agentTracker.removeAgentFromStop(passenger, this.currentStop.getStopFacility().getId());
			MobsimAgent planAgent = (MobsimAgent) passenger;
			if (planAgent instanceof PersonDriverAgentImpl) { 
				Id agentId = planAgent.getId();
				Id linkId = planAgent.getCurrentLinkId();
				this.internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId) ;
			}
			MobsimDriverAgent agent = (MobsimDriverAgent) passenger;
			EventsManager events = this.internalInterface.getMobsim().getEventsManager();
			events.processEvent(((EventsFactory) events.getFactory()).createPersonEntersVehicleEvent(time,
					agent.getId(), this.vehicle.getVehicle().getId()));
		}
		return handled;
	}

	@Override
	public boolean handlePassengerLeaving(final PTPassengerAgent passenger, final double time) {
		boolean handled = this.vehicle.removePassenger(passenger);
		if(handled){
//			MobsimDriverAgent agent = (MobsimDriverAgent) passenger;
			EventsManager events = this.internalInterface.getMobsim().getEventsManager();
			events.processEvent(new PersonLeavesVehicleEvent(time, passenger.getId(), this.vehicle.getVehicle().getId()));
			
			// from here on works only if PassengerAgent can be cast into MobsimAgent ... but this is how it was before.
			// kai, sep'12
			
			MobsimAgent agent = (MobsimAgent) passenger ;
			agent.notifyArrivalOnLinkByNonNetworkMode(this.currentStop.getStopFacility().getLinkId());
			agent.endLegAndComputeNextState(time);
			this.internalInterface.arrangeNextAgentState(agent) ;
			// (cannot set trEngine to TransitQSimEngine because there are tests where this will not work. kai, dec'11)
		}
		return handled;
	}

	@Override
	public int getNumberOfPassengers() {
		return this.vehicle.getPassengers().size();
	}

	private List<PTPassengerAgent> findPassengersEntering(
			final TransitStopFacility stop, int freeCapacity, double now) {
		ArrayList<PTPassengerAgent> passengersEntering = new ArrayList<PTPassengerAgent>();
		for (PTPassengerAgent agent : this.agentTracker.getAgentsAtStop(stop.getId())) {
			if ( !this.isGeneratingDeniedBoardingEvents ) {
				if (freeCapacity == 0) {
					break;
				}
			}
			List<TransitRouteStop> stops = getTransitRoute().getStops();
			List<TransitRouteStop> stopsToCome = stops.subList(stopIterator.nextIndex(), stops.size());
			if (agent.getEnterTransitRoute(getTransitLine(), getTransitRoute(), stopsToCome)) {
				if ( !this.isGeneratingDeniedBoardingEvents ) {
					// this tries to leave the pre-existing code intact; thus the replication of code below
					passengersEntering.add(agent);
					freeCapacity--;
				} else {
					if ( freeCapacity >= 1 ) {
						passengersEntering.add(agent);
						freeCapacity--;
					} else {
//						double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay() ;
						// does not work for test (when machinery is not initialized). kai, oct'12
						
						Id vehicleId = this.vehicle.getId() ;
						Id agentId = agent.getId() ;
						this.internalInterface.getMobsim().getEventsManager().processEvent(
								new BoardingDeniedEvent(now, agentId, vehicleId)
								) ;
					}
				}
			}
		}
		return passengersEntering;
	}

	private ArrayList<PTPassengerAgent> findPassengersLeaving(
			final TransitStopFacility stop) {
		ArrayList<PTPassengerAgent> passengersLeaving = new ArrayList<PTPassengerAgent>();
		for (PassengerAgent passenger : this.vehicle.getPassengers()) {
			if (((PTPassengerAgent) passenger).getExitAtStop(stop)) {
				passengersLeaving.add((PTPassengerAgent) passenger);
			}
		}
		return passengersLeaving;
	}

	protected NetworkRouteWrapper getWrappedCarRoute(NetworkRoute carRoute) {
		return new NetworkRouteWrapper(carRoute);
	}

	@Override
	public Id getId() {
		return this.dummyPerson.getId() ;
	}
	
	/**
	 * for junit tests in same package
	 */
	abstract /*package*/ Leg getCurrentLeg() ;


	/**
	 * A simple wrapper that delegates all get-Methods to another instance, blocks set-methods
	 * so this will be read-only, and returns in getVehicleId() a vehicle-Id specific to this driver,
	 * and not to the NetworkRoute. This allows to share one NetworkRoute from a TransitRoute with
	 * multiple transit drivers, thus saving memory.
	 *
	 * @author mrieser
	 */
	protected class NetworkRouteWrapper implements NetworkRoute, Cloneable {

		private final NetworkRoute delegate;

		public NetworkRouteWrapper(final NetworkRoute route) {
			this.delegate = route;
		}

		@Override
		public List<Id> getLinkIds() {
			return this.delegate.getLinkIds();
		}

		@Override
		public NetworkRoute getSubRoute(final Id fromLinkId, final Id toLinkId) {
			return this.delegate.getSubRoute(fromLinkId, toLinkId);
		}

		@Override
		public double getTravelCost() {
			return this.delegate.getTravelCost();
		}

		@Override
		public Id getVehicleId() {
			return AbstractTransitDriver.this.vehicle.getVehicle().getId();
		}

		@Override
		public void setLinkIds(final Id startLinkId, final List<Id> srcRoute, final Id endLinkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setTravelCost(final double travelCost) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setVehicleId(final Id vehicleId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setEndLinkId(final Id  linkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setStartLinkId(final Id linkId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		@Deprecated
		public double getDistance() {
			return this.delegate.getDistance();
		}

		@Override
		public Id getEndLinkId() {
			return this.delegate.getEndLinkId();
		}

		@Override
		public Id getStartLinkId() {
			return this.delegate.getStartLinkId();
		}

		@Deprecated
		@Override
		public double getTravelTime() {
			return this.delegate.getTravelTime();
		}

		@Override
		public void setDistance(final double distance) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setTravelTime(final double travelTime) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public NetworkRouteWrapper clone() {
			try {
				return (NetworkRouteWrapper) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
		}

	}


}