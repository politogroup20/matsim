/* *********************************************************************** *
 * project: org.matsim.*
 * CoordUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.geometry;

import org.matsim.api.core.v01.Coord;

public abstract class CoordUtils {
	
	public static Coord createCoord( final double xx, final double yy ) {
		return new Coord(xx, yy);
	}
	
	public static Coord createCoord( final double xx, final double yy, final double zz){
		return new Coord(xx, yy, zz);
	}
	
	public static Coord plus ( Coord coord1, Coord coord2 ) {
		if(is2D(coord1) && is2D(coord2)){
			/* Both are 2D coordinates. */
			double xx = coord1.getX() + coord2.getX();
			double yy = coord1.getY() + coord2.getY();
			return new Coord(xx, yy);			
		} else if(!is2D(coord1) && !is2D(coord2)){
			/* Both are 3D coordinates. */
			double xx = coord1.getX() + coord2.getX();
			double yy = coord1.getY() + coord2.getY();
			double zz = coord1.getZ() + coord2.getZ();
			return new Coord(xx, yy, zz);			
		} else{
			throw new RuntimeException("Cannot 'plus' coordinates if one has elevation (z) and the other not.");
		}
	}
	
	public static Coord minus ( Coord coord1, Coord coord2 ) {
		if(is2D(coord1) && is2D(coord2)){
			/* Both are 2D coordinates. */
			double xx = coord1.getX() - coord2.getX();
			double yy = coord1.getY() - coord2.getY();
			return new Coord(xx, yy);			
		} else if(!is2D(coord1) && !is2D(coord2)){
			/* Both are 3D coordinates. */
			double xx = coord1.getX() - coord2.getX();
			double yy = coord1.getY() - coord2.getY();
			double zz = coord1.getZ() - coord2.getZ();
			return new Coord(xx, yy, zz);			
		} else{
			throw new RuntimeException("Cannot 'minus' coordinates if one has elevation (z) and the other not.");
		}
	}
	
	public static Coord scalarMult( double alpha, Coord coord ) {
		if(is2D(coord)){
			/* 2D coordinate. */
			double xx = alpha * coord.getX();
			double yy = alpha * coord.getY();
			return new Coord(xx, yy);			
		} else {
			/* 3D coordinate. */
			double xx = alpha * coord.getX();
			double yy = alpha * coord.getY();
			double zz = alpha * coord.getZ();
			return new Coord(xx, yy, zz);			
		} 
	}
	
	
	public static Coord getCenter( Coord coord1, Coord coord2 ) {
		if(is2D(coord1) && is2D(coord2)){
			/* Both are 2D coordinates. */
			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
			return new Coord(xx, yy);			
		} else if(!is2D(coord1) && !is2D(coord2)){
			/* Both are 3D coordinates. */
			double xx = 0.5*( coord1.getX() + coord2.getX() ) ;
			double yy = 0.5*( coord1.getY() + coord2.getY() ) ;
			double zz = 0.5*( coord1.getZ() + coord2.getZ() ) ;
			return new Coord(xx, yy, zz);			
		} else{
			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
		}
	}
	
	public static double length( Coord coord1 ) {
		if(is2D(coord1)){
			return Math.sqrt( coord1.getX()*coord1.getX() + coord1.getY()*coord1.getY() ) ;
		} else{
			return Math.sqrt( 
					coord1.getX()*coord1.getX() + 
					coord1.getY()*coord1.getY() +
					coord1.getZ()*coord1.getZ()) ;
		}
	}
	
	/**
	 * Note: If the given {@link Coord} has elevation, it's elevation will stay 
	 * the same (jjoubert, Sep '16). 
	 * @param coord1
	 * @return
	 */
	public static Coord rotateToRight( Coord coord1 ) {
		if(is2D(coord1)){
			final double y = -coord1.getX();
			return new Coord(coord1.getY(), y);
		} else{
			final double y = -coord1.getX();
			return new Coord(coord1.getY(), y, coord1.getZ());			
		}
	}

	
	public static Coord getCenterWOffset( Coord coord1, Coord coord2 ) {
		if(is2D(coord1) && is2D(coord2)){
			/* Both are 2D coordinates. */
			Coord fromTo = minus( coord2, coord1 ) ;
			Coord offset = scalarMult( 0.1 , rotateToRight( fromTo ) ) ;
			Coord centerWOffset = plus( getCenter( coord1, coord2 ) , offset ) ;
			return centerWOffset ;
		} else if(!is2D(coord1) && !is2D(coord2)){
			/* TODO Both are 3D coordinates. */
			throw new RuntimeException("3D version not implemented.");
		} else{
			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
		}
	}

	public static double calcEuclideanDistance(Coord coord, Coord other) {
		/* Depending on the coordinate system that is used, determining the 
		 * distance based on the euclidean distance will lead to wrong results. 
		 * However, if the distance is not to large (<1km) this will be a usable 
		 * distance estimation. Another comfortable way to calculate correct 
		 * distances would be, to use the distance functions provided by 
		 * geotools lib. May be we need to discuss what part of GIS functionality 
		 * we should implement by our own and for what part we could use an 
		 * existing GIS like geotools. We need to discuss this in terms of code 
		 * robustness, performance and so on ... [gl] */
		if(is2D(coord) && is2D(other)){
			/* Both are 2D coordinates. */
			double xDiff = other.getX()-coord.getX();
			double yDiff = other.getY()-coord.getY();
			return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
		} else if(!is2D(coord) && !is2D(other)){
			/* Both are 3D coordinates. */
			double xDiff = other.getX()-coord.getX();
			double yDiff = other.getY()-coord.getY();
			double zDiff = other.getZ()-coord.getZ();
			return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff) + (zDiff*zDiff));
		} else{
			throw new RuntimeException("Cannot get the center for coordinates if one has elevation (z) and the other not.");
		}
	}

	
	/**
	 * Method should only be used in within this class, and only by 
	 * {@link #distancePointLinesegment(Coord, Coord, Coord)}. 
	 * @param coord1
	 * @param coord2
	 * @return
	 */
	private static double dotProduct( Coord coord1, Coord coord2 ) {
		if(is2D(coord1) && is2D(coord2)){
			/* Both are 2D coordinates. */
			return 	coord1.getX()*coord2.getX() + 
					coord1.getY()*coord2.getY();
		} else if(!is2D(coord1) && !is2D(coord2)){
			/* Both are 3D coordinates. */
			return 	coord1.getX()*coord2.getX() + 
					coord1.getY()*coord2.getY() +
					coord1.getZ()*coord2.getZ();
		} else{
			throw new RuntimeException("Cannot get the dot-product of coordinates if one has elevation (z) and the other not.");
		}
	}
	
	
	/**
	 * Calculates the shortest distance of a point to a line segment. The line segment
	 * is given by two points, <code>lineFrom</code> and <code>lineTo</code>. Note that
	 * the line segment has finite length, and thus the shortest distance cannot
	 * always be the distance on the tangent to the line through <code>point</code>.
	 * 
	 * <br><br>
	 * The 3D version was adapted from the C++ implementation of 
	 * <a href="http://geomalgorithms.com/a02-_lines.html">Dan Sunday</a>. 
	 *
	 * @param lineFrom The start point of the line segment
	 * @param lineTo The end point of the line segment
	 * @param point The point whose distance to the line segment should be calculated
	 * @return the distance of <code>point</code> to the line segment given by the two
	 *    end points of the line segment, <code>lineFrom</code> and <code>lineTo</code>
	 *
	 * @author mrieser, jwjoubert
	 */
	public static double distancePointLinesegment(final Coord lineFrom, final Coord lineTo, final Coord point) {
		if(is2D(lineFrom) && is2D(lineTo) && is2D(point)){
			/* All coordinates are 2D and in the XY plane. */

			/* The shortest distance is where the tangent of the line goes 
			 * through "point". The dot product (point - P) dot (lineTo - lineFrom) 
			 * must be 0, when P is a point on the line. P can be substituted 
			 * with lineFrom + u*(lineTo - lineFrom). Thus it must be:
			 *    (point - lineFrom - u*(lineTo - lineFrom)) dot (lineTo - lineFrom) == 0
			 * From this follows:
			 *        (point.x - lineFrom.x)(lineTo.x - lineFrom.x) + (point.y - lineFrom.y)(lineTo.y - lineFrom.y)
			 *    u = ---------------------------------------------------------------------------------------------
			 *       (lineTo.x - lineFrom.x)(lineTo.x - lineFrom.x) + (lineTo.y - lineFrom.y)(lineTo.y - lineFrom.y)
			 *
			 * Substituting this gives:
			 *   x = lineFrom.x + u*(lineFrom.x - lineTo.x) , y = lineFrom.y + u*(lineFrom.y - lineTo.y)
			 *
			 * The shortest distance is now the distance between "point" and 
			 * (x | y)
			 */
			double lineDX = lineTo.getX() - lineFrom.getX();
			double lineDY = lineTo.getY() - lineFrom.getY();
			
			if ((lineDX == 0.0) && (lineDY == 0.0)) {
				// the line segment is a point without dimension
				return calcEuclideanDistance(lineFrom, point);
			}
			
			double u = ((point.getX() - lineFrom.getX())*lineDX + (point.getY() - lineFrom.getY())*lineDY) /
					(lineDX*lineDX + lineDY*lineDY);
			
			if (u <= 0) {
				// (x | y) is not on the line segment, but before lineFrom
				return calcEuclideanDistance(lineFrom, point);
			}
			if (u >= 1) {
				// (x | y) is not on the line segment, but after lineTo
				return calcEuclideanDistance(lineTo, point);
			}
			
			return calcEuclideanDistance(new Coord(lineFrom.getX() + u * lineDX, lineFrom.getY() + u * lineDY), point);
		} else if(!is2D(lineFrom) && !is2D(lineTo) && !is2D(point)){
			/* All coordinates are 3D. */
			double lineDX = lineTo.getX() - lineFrom.getX();
			double lineDY = lineTo.getY() - lineFrom.getY();
			double lineDZ = lineTo.getZ() - lineFrom.getZ();
			
			if((lineDX == 0.0) && (lineDY == 0.0) && (lineDZ == 0.0)){
				return calcEuclideanDistance(lineFrom, point);
			}
			
			Coord v = minus(lineTo, lineFrom);
			Coord w = minus(point, lineFrom);
			
			double c1 = dotProduct(w, v);
			if(c1 <= 0.0){
				Coord m = minus(point, lineFrom);
				return Math.sqrt(dotProduct(m, m));
			}
			
			double c2 = dotProduct(v, v);
			if(c2 <= c1){
				Coord m = minus(point, lineTo);
				return Math.sqrt(dotProduct(m, m));
			}
			
			double b = c1 / c2;
			Coord p = plus(lineFrom, scalarMult(b, v));
			Coord m = minus(point, p);
			return Math.sqrt(dotProduct(m, m));
		} else{
			throw new RuntimeException("All given coordinates must either be 2D, or 3D. A mix is not allowed.");
		}
	}

	/**
	 * Checks if a {@link Coord} is 2D. That is, does <i>not</i> have elevation (z).
	 * @param c
	 * @return
	 */
	private static boolean is2D(Coord c){
		@SuppressWarnings("unused")
		double z;
		try{
			z = c.getZ();
			return false;
		} catch(Exception e){
			return true;
		}
	}
	
}
