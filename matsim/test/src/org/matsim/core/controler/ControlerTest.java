/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import java.io.File;
import java.util.ArrayList;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class ControlerTest extends MatsimTestCase {

	/**
	 * Tests that the travel times are correctly calculated during the simulation.
	 * 
	 * @author mrieser
	 */
	public void testTravelTimeCalculation() {

		Config config = loadConfig(null);

		Fixture f = new Fixture();

		/* Create 2 persons driving from link 1 to link 3, both starting at the
		 * same time at 7am.  */
		Population population = new PopulationImpl();
		Person person1 = null;

		person1 = new PersonImpl(new IdImpl(1));
		Plan plan1 = person1.createPlan(true);
		ActivityImpl a1 = plan1.createActivity("h", f.link1);
		a1.setEndTime(7.0*3600);
		LegImpl leg1 = plan1.createLeg(TransportMode.car);
		NetworkRoute route1 = (NetworkRoute)f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		leg1.setRoute(route1);
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.add(f.node2);
		nodes.add(f.node3);
		route1.setNodes(f.link1, nodes, f.link3);
		plan1.createActivity("h", f.link3);
		population.getPersons().put(person1.getId(), person1);

		Person person2 = new PersonImpl(new IdImpl(2));
		Plan plan2 = person2.createPlan(true);
		ActivityImpl a2 = plan2.createActivity("h", f.link1);
		a2.setEndTime(7.0*3600);
		LegImpl leg2 = plan2.createLeg(TransportMode.car);
		NetworkRoute route2 = (NetworkRoute)f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		leg2.setRoute(route2);
		route2.setNodes(f.link1, nodes, f.link3);
		plan2.createActivity("h", f.link3);
		population.getPersons().put(person2.getId(), person2);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(0);

		// Now run the simulation
		Controler controler = new Controler(config, f.network, population);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.run();

		// test if we got the right result
		// the actual result is 151sec, not 150, as each vehicle "loses" 1sec in the buffer
		assertEquals("TravelTimeCalculator has wrong result",
				151.0, controler.getTravelTimeCalculator().getLinkTravelTime(f.link2, 7*3600), 0.0);

		// now test that the ReRoute-Strategy also knows about these travel times...
		config.controler().setLastIteration(1);
		Module strategyParams = config.getModule("strategy");
		strategyParams.addParam("maxAgentPlanMemorySize", "4");
		strategyParams.addParam("ModuleProbability_1", "1.0");
		strategyParams.addParam("Module_1", "ReRoute");
		// Run the simulation again
		controler = new Controler(config, f.network, population);
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.setWriteEventsInterval(0);
		controler.run();

		// test that the plans have the correct times
		assertEquals("ReRoute seems to have wrong travel times.",
				151.0, ((LegImpl) (person1.getPlans().get(1).getPlanElements().get(1))).getTravelTime(), 0.0);
	}

	/**
	 * Tests that a custom scoring function factory doesn't get overwritten
	 * in the initialization process of the Controler.
	 * 
	 * @author mrieser
	 */
	public void testSetScoringFunctionFactory() {
		final Config config = loadConfig(null);
		config.controler().setLastIteration(0);

		// create a very simple network with one link only and an empty population
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(100, 0));
		network.createLink(new IdImpl(1), node1, node2, 100, 1, 3600, 1);
		Population population = new PopulationImpl();

		final Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.setScoringFunctionFactory(new DummyScoringFunctionFactory());
		assertTrue("Custom ScoringFunctionFactory was not set.",
				controler.getScoringFunctionFactory() instanceof DummyScoringFunctionFactory);

		controler.run();

		assertTrue("Custom ScoringFunctionFactory got overwritten.",
				controler.getScoringFunctionFactory() instanceof DummyScoringFunctionFactory);
	}

	/**
	 * Tests that plans with missing routes are completed (=routed) before the mobsim starts.
	 * 
	 * @author mrieser
	 */
	public void testCalcMissingRoutes() {
		Config config = loadConfig(null);
		Fixture f = new Fixture();

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = new PopulationImpl();
		Person person1 = null;
		LegImpl leg1 = null;
		LegImpl leg2 = null;

		person1 = new PersonImpl(new IdImpl(1));
		// --- plan 1 ---
		Plan plan1 = person1.createPlan(true);
		ActivityImpl a1 = plan1.createActivity("h", f.link1);//(String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
		a1.setEndTime(7.0*3600);
		leg1 = plan1.createLeg(TransportMode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan1.createActivity("h", f.link3);
		// --- plan 2 ---
		Plan plan2 = person1.createPlan(true);
		ActivityImpl a2 = plan2.createActivity("h", f.link1);//(String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
		a2.setEndTime(7.0*3600);

		leg2 = plan2.createLeg(TransportMode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan2.createActivity("h", f.link3);
		population.getPersons().put(person1.getId(), person1);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		// Now run the simulation
		Controler controler = new Controler(config, f.network, population);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		assertNotNull(leg1.getRoute());
		assertNotNull(leg2.getRoute());
	}

	/**
	 * Tests that plans with missing act locations are completed (=xy2links and routed) before the mobsim starts.
	 * 
	 * @author mrieser
	 */
	public void testCalcMissingActLinks() {
		Config config = loadConfig(null);
		Fixture f = new Fixture();

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = new PopulationImpl();
		Person person1 = null;
		ActivityImpl act1a = null;
		ActivityImpl act1b = null;
		ActivityImpl act2a = null;
		ActivityImpl act2b = null;
		LegImpl leg1 = null;
		LegImpl leg2 = null;

		person1 = new PersonImpl(new IdImpl(1));
		// --- plan 1 ---
		Plan plan1 = person1.createPlan(true);
		act1a = plan1.createActivity("h", new CoordImpl(-50.0, 10.0));
		act1a.setEndTime(7.0*3600);
		leg1 = plan1.createLeg(TransportMode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		act1b = plan1.createActivity("h", new CoordImpl(1075.0, -10.0));
		// --- plan 2 ---
		Plan plan2 = person1.createPlan(true);
		act2a = plan2.createActivity("h", new CoordImpl(-50.0, -10.0));
		act2a.setEndTime(7.9*3600);
		leg2 = plan2.createLeg(TransportMode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		act2b = plan2.createActivity("h", new CoordImpl(1111.1, 10.0));
		population.getPersons().put(person1.getId(), person1);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		// Now run the simulation
		Controler controler = new Controler(config, f.network, population);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have their act-locations calculated
		assertEquals(f.link1, act1a.getLink());
		assertEquals(f.link3, act1b.getLink());
		assertEquals(f.link1, act2a.getLink());
		assertEquals(f.link3, act2b.getLink());

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		assertNotNull(leg1.getRoute());
		assertNotNull(leg2.getRoute());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsInterval() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(10);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(3);
		assertEquals(3, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 1)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 2)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 3)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 4)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 5)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 6)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 7)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 8)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 9)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 10)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsNever() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				0 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(0);
		assertEquals(0, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 1)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsAlways() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 1)).exists());
	}
	
	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsTxt() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(new EventsFileFormat[] {EventsFileFormat.txt});

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_XML, 0)).exists());
	}
	
	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsXml() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(new EventsFileFormat[] {EventsFileFormat.xml});
		
		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_XML, 0)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsTxtXml() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(new EventsFileFormat[] {EventsFileFormat.txt, EventsFileFormat.xml});
		
		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();
		
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_TXT, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS_XML, 0)).exists());
	}
	

	/** A helper class for testSetScoringFunctionFactory() */
	/*package*/ static class DummyScoringFunctionFactory implements ScoringFunctionFactory {

		public ScoringFunction getNewScoringFunction(final Plan plan) {
			return new ScoringFunctionAccumulator();
		}
	}

	/**
	 * @author mrieser
	 */
	private static class Fixture {
		NetworkLayer network = new NetworkLayer();
		Node node1 = null;
		Node node2 = null;
		Node node3 = null;
		Node node4 = null;
		Link link1 = null;
		Link link2 = null;
		Link link3 = null;
		
		protected Fixture() {
			/* Create a simple network with 4 nodes and 3 links:
			 *
			 * (1)---1---(2)-----------2-------------(3)---3---(4)
			 *
			 * Link 2 has a capacity of 1veh/100secs, links 1 and 3 of 2veh/1sec.
			 * This way, 2 vehicles can start at the same time on link 2, of which
			 * one can leave link 2 without waiting, while the other one has to
			 * wait an additional 100sec. Given a free speed travel time of 100sec,
			 * the average travel time on that link should be 150sec for the two cars
			 * (one having 100secs, the other having 200secs to cross the link).
			 */
			this.network = new NetworkLayer();
			this.network.setCapacityPeriod(Time.parseTime("01:00:00"));
			this.node1 = network.createNode(new IdImpl(1), new CoordImpl(-100.0, 0.0));
			this.node2 = network.createNode(new IdImpl(2), new CoordImpl(0.0, 0.0));
			this.node3 = network.createNode(new IdImpl(3), new CoordImpl(1000.0, 0.0));
			this.node4 = network.createNode(new IdImpl(4), new CoordImpl(1100.0, 0.0));
			this.link1 = network.createLink(new IdImpl(1), this.node1, this.node2, 100, 10, 7200, 1);
			this.link2 = network.createLink(new IdImpl(2), this.node2, this.node3, 1000, 10, 36, 1);
			this.link3 = network.createLink(new IdImpl(3), this.node3, this.node4, 100, 10, 7200, 1);
		}
	}
	
}
