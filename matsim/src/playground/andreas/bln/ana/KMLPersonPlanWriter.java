/* *********************************************************************** *
 * project: org.matsim.*
 * MyKMLNetWriterTest.java
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

package playground.andreas.bln.ana;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;
import org.matsim.vis.kml.MatsimKmlStyleFactory;

public class KMLPersonPlanWriter {

	private static final Logger log = Logger.getLogger(KMLPersonPlanWriter.class);

	private String kmzFileName;
	private String outputDirectory;

	private PersonImpl person;

	private ArrayList<Link> activityLinks;

	private NetworkLayer network;

	private boolean writeActivityLinks = true;
	private boolean writeFullPlan = true;

	private MatsimKmlStyleFactory styleFactory;
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private StyleType networkLinkStyle;
	private MyFeatureFactory networkFeatureFactory;
	private StyleType networkNodeStyle;

	private CoordinateTransformation coordinateTransform = new IdentityTransformation();

	PlanImpl personsPlan;



	public KMLPersonPlanWriter(NetworkLayer network, PersonImpl person) {
		this.network = network;
		this.person = person;

		this.personsPlan = this.person.getSelectedPlan();
	}

	public KMLPersonPlanWriter() {
		// Should never be used
	}

	public void writeFile() {

		String outputFile;

		if (this.kmzFileName == null || this.kmzFileName.equals("")) {
			outputFile = this.outputDirectory + "/" + this.person.getId() + ".kmz";
		} else {
			outputFile = this.outputDirectory + "/" + this.kmzFileName;
		}

		ObjectFactory LocalkmlObjectFactory = new ObjectFactory();

		// create main file and document

		DocumentType mainDoc = LocalkmlObjectFactory.createDocumentType();
		mainDoc.setId("mainDoc");
		mainDoc.setOpen(Boolean.TRUE);

		KmlType mainKml = LocalkmlObjectFactory.createKmlType();
		mainKml.setAbstractFeatureGroup(LocalkmlObjectFactory.createDocument(mainDoc));

		// create a folder
		FolderType mainFolder = LocalkmlObjectFactory.createFolderType();
		mainFolder.setId("2dnetworklinksfolder");
		mainFolder.setName("Matsim Data");
		mainFolder.setOpen(Boolean.TRUE);
		mainDoc.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(mainFolder));

		// create the writer
		KMZWriter writer = new KMZWriter(outputFile);

		this.styleFactory = new MatsimKmlStyleFactory(writer, mainDoc);
		this.networkFeatureFactory = new MyFeatureFactory(this.coordinateTransform);

		try {

			// add the MATSim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createScreenOverlay(logo));

			// add the person's activity links to the kml
			if(this.writeActivityLinks){
				createActivityLinks();
				FolderType activityFolder = getActivityLinksFolder(this.activityLinks, "Activity Links of Person " + this.person.getId());
				if (activityFolder != null) {
					activityFolder.setVisibility(Boolean.FALSE);
					mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityFolder));
				}
			}
			
			// write the person's full plan
			if(this.writeFullPlan){
				FolderType activityFolder = getFullPlan();
				if (activityFolder != null) {
					activityFolder.setOpen(Boolean.TRUE);
					mainFolder.getAbstractFeatureGroup().add(LocalkmlObjectFactory.createFolder(activityFolder));
				}
			}

		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo because of: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		log.info("... wrote agent " + this.person.getId());
	}

	private FolderType getFullPlan() throws IOException {

		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();

		FolderType linkFolder = this.kmlObjectFactory.createFolderType();
		linkFolder.setName("Full Plan of " + this.person.getId());

		Coord fromCoords = null;

		for (Iterator iterator = this.personsPlan.getPlanElements().iterator(); iterator.hasNext();) {
			PlanElement planElement = (PlanElement) iterator.next();

			if (planElement instanceof BasicActivity) {

				ActivityImpl act = (ActivityImpl) planElement;
				fromCoords = act.getCoord();

				AbstractFeatureType abstractFeature = this.networkFeatureFactory.createActFeature(act, this.networkNodeStyle);
				StringBuffer stringOut = new StringBuffer();
				stringOut.append("Act: " + act.getType());
				stringOut.append(", Link: " + act.getLinkId());
				stringOut.append(", X: " + act.getCoord().getX() + ", Y: " + act.getCoord().getY());
				stringOut.append(", StartTime: " + Time.writeTime(act.getStartTime()) + ", Duration: " + Time.writeTime(act.getDuration()) + ", EndTime: " + Time.writeTime(act.getEndTime()));

				abstractFeature.setDescription(stringOut.toString());
				linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
			}

			if (planElement instanceof LegImpl) {

				LegImpl leg = (LegImpl) planElement;

				if (leg.getMode() == TransportMode.car) {

					ArrayList<Link> tempLinkList = getLinksOfCarLeg(leg);

					FolderType routeLinksFolder = this.kmlObjectFactory.createFolderType();
					routeLinksFolder.setName(leg.getMode().toString() + " mode, dur: " + Time.writeTime(leg.getTravelTime()) + ", dist: " + leg.getRoute().getDistance());

					for (Link link : tempLinkList) {
						AbstractFeatureType abstractFeature = this.networkFeatureFactory.createCarLinkFeature(link,	this.networkLinkStyle);
						routeLinksFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
					}
					linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(routeLinksFolder));

				} else if (leg.getMode() == TransportMode.pt) {

					if (iterator.hasNext()) {

						Coord toCoords = null;

						for (Iterator tempIterator = this.personsPlan.getPlanElements().iterator(); tempIterator.hasNext();) {
							PlanElement tempPlanElement = (PlanElement) tempIterator.next();
							if (tempPlanElement == planElement) {
								toCoords = ((ActivityImpl) tempIterator.next()).getCoord();
							}

						}

						AbstractFeatureType abstractFeature = this.networkFeatureFactory.createPTLinkFeature(fromCoords, toCoords, leg, this.networkLinkStyle);
						abstractFeature.setDescription(((GenericRouteImpl) leg.getRoute()).getRouteDescription());
						linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
					}

				}				
			}
		}
		return linkFolder;
	}

	private FolderType getActivityLinksFolder(ArrayList<Link> links, String description) throws IOException {

		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();

		FolderType linkFolder = this.kmlObjectFactory.createFolderType();
		linkFolder.setName(description);

		for (Link link : links) {
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createLinkFeature(link, this.networkLinkStyle);
			linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
		}

		return linkFolder;
	}

	private void createActivityLinks() {

		this.activityLinks = new ArrayList<Link>();

		if (this.person != null) {
			PlanImpl selectedPlan = this.person.getSelectedPlan();
			if (selectedPlan != null) {
				for (PlanElement planElement : selectedPlan.getPlanElements()) {
					if (planElement instanceof BasicActivity) {
						BasicActivity act = (BasicActivity) planElement;
						this.activityLinks.add(this.network.getLink(act.getLinkId()));
					}
				}
			}
		}
	}

	private ArrayList<Link> getLinksOfCarLeg(LegImpl leg) {

		ArrayList<Link> links = new ArrayList<Link>();
			if (leg.getMode() == TransportMode.car) {

				if (leg.getRoute() != null) {
					NodeNetworkRouteImpl tempRoute = (NodeNetworkRouteImpl) leg.getRoute();
					for (Link link : tempRoute.getLinks()) {
						links.add(link);
					}
				}

			} else { log.error("You gave me a non car leg. Can't handle this one."); }
		return links;
	}

	/*
	 * Getters & Setters
	 */

	public void setWriteActivityLinks(boolean value) {
		this.writeActivityLinks = value;
	}

	public void setCoordinateTransformation(CoordinateTransformation coordinateTransform) {
		this.coordinateTransform = coordinateTransform;
	}

	public CoordinateTransformation getCoordinateTransformation() {
		return this.coordinateTransform;
	}

	public void setOutputDirectory(String directory) {
		this.outputDirectory = directory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setKmzFileName(String name) {
		this.kmzFileName = name;
	}

	public String getKmzFileName() {
		return this.kmzFileName;
	}

//	public static void main(String[] args) {
//		final String netFilename = "E:\\oev-test\\output\\network.multimodal.xml";
//		final String kmzFilename = "test.kmz";
//		final String outputDirectory = "E:\\oev-test\\GE";
//
//		Gbl.createConfig(null);
//		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
//		new MatsimNetworkReader(network).readFile(netFilename);
//
//		KMLPersonPlanWriter test = new KMLPersonPlanWriter();
//
//		test.setKmzFileName(kmzFilename);
//		test.setOutputDirectory(outputDirectory);
////		test.setNetwork(network);
//
//		test.writeFile();
//
//		log.info("Done!");
//	}

}
