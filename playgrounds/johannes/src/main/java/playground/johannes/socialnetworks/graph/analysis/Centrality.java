/* *********************************************************************** *
 * project: org.matsim.*
 * Centrality.java
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
package playground.johannes.socialnetworks.graph.analysis;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.matrix.MatrixCentrality;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrixDecorator;

/**
 * @author illenberger
 *
 */
public class Centrality {

	private AdjacencyMatrixDecorator<Vertex> y;
	
	private MatrixCentrality mCentrality;
	
	public void init(Graph graph) {
		y = new AdjacencyMatrixDecorator<Vertex>(graph);
		mCentrality = new MatrixCentrality();
		mCentrality.run(y);
	}
	
	public Distribution closenessDistribution() {
		return new Distribution(mCentrality.getVertexCloseness());
	}
	
	public Distribution vertexBetweennessDistribution() {
		Distribution distr = new Distribution();
		for(int i = 0; i < mCentrality.getVertexBetweenness().length; i++) {
			distr.add(mCentrality.getVertexBetweenness()[i]);
		}
		return distr;
	}
}
