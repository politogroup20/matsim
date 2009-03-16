/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.controler;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.listener.StartupListener;
import org.matsim.gbl.Gbl;

import playground.meisterk.org.matsim.controler.listener.KTIYear3StartupListener;

public class KTIYear3Controler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = Gbl.createConfig(args);
		Controler controler = new Controler(config);
		
		StartupListener sl = new KTIYear3StartupListener(controler);
		controler.addControlerListener(sl);
		
		controler.run();

	}

}
