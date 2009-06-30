/* *********************************************************************** *
 * project: org.matsim.*
 * AgentWait2LinkEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.events;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Person;
import org.matsim.core.population.LegImpl;

public class AgentWait2LinkEvent extends AgentEvent implements BasicAgentWait2LinkEvent {

	public static final String EVENT_TYPE = "wait2link";

	public AgentWait2LinkEvent(final double time, final Person agent, final Link link, final LegImpl leg) {
		super(time, agent, link, leg);
	}

	public AgentWait2LinkEvent(final double time, final Id agentId, final Id linkId) {
		super(time, agentId, linkId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
