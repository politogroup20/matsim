/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeIntervalls.java
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

/**
 * 
 */
package playground.rost.eaflow.Intervall.src.Intervalls;


import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;

//import playground.rost.eaflow.ea_flow.BellmanFordIntervallBased.IntervallNode;

/**
 * class representing the flow of an edge in a Time Expanded Network
 * @author Manuel Schneider
 *
 */
public class VertexIntervalls {

//------------------------FIELDS----------------------------------//
	
	/**
	 * internal binary search tree holding distinkt Intervalls
	 */
	private AVLTree _tree;
	
	/**
	 * reference to the last Intervall
	 */
	private VertexIntervall _last; 
	
	/**
	 * flag for debug mode
	 */
	private static boolean _debug = false;
	
	public static int rem;
	
	
	
//-----------------------METHODS----------------------------------//
//****************************************************************//
	
	 
//----------------------CONSTRUCTORS------------------------------//	
	
	/**
	 * Default Constructor Constructs an object containing only 
	 * one EdgeIntervall [0,Integer.MAX_VALUE) with flow equal to 0
	 */
	public VertexIntervalls(){
		VertexIntervall intervall = new VertexIntervall(0,Integer.MAX_VALUE);
		_tree = new AVLTree();
		_tree.insert(intervall);
		_last = intervall;
	}
	

//------------------------SPLITTING--------------------------------//	
	
	/**
	 * Finds the EgdeIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeIntervall for further modification
 	 */
	public VertexIntervall splitAt(int t){
		
		boolean found = false;
		VertexIntervall j = null;
		
		VertexIntervall i = getIntervallAt(t);
			if (i != null){
				found = true;
				//update last
				if(i == _last){
					j = i.splitAt(t);
					_last = j;
				}else {
					j = i.splitAt(t);
				}
			}
		if (found){
			_tree.insert(j);
			return j;
		}
		else throw new IllegalArgumentException("there is no Intervall that can be split at "+t);
	}

//------------------------------FLOW-------------------------//	
	
	/**
	 * Gives the predesessing Link on the Vertex at time t
	 * @param t time
	 * @return flow at t
	 */
	public Link getPred(int t){
		return getIntervallAt(t).getPredecessor();
	}
	

//------------------------------GETTER-----------------------//
	
	

	/**
	 * Finds the VertexIntervall containing t in the collection
	 * @param t time
	 * @return  VertexIntervall  containing t
	 */
	public VertexIntervall getIntervallAt(int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		VertexIntervall i = (VertexIntervall) _tree.contains(t);
		if(i==null)throw new IllegalArgumentException("there is no Intervall containing"+t);
		return i;
	}
	
	
	/**
	 * Geves a String representation of all stored Intervalls linewise
	 * @return String representation
	 */
	public String toString(){
		String l,r;
		switch (Intervall.getMode()){
		case 1: 
			l="[";
			r="]";
			break;
		case 2:
			l="(";
			r="]";
			break;
		case 3:
			l="[";
			r=")";
			break;
		case 4:
			l="(";
			r=")";
			break;
		default: 
			l="|";
			r="|";
			break;
		}
		StringBuilder str = new StringBuilder();
		for(_tree.reset();!_tree.isAtEnd();_tree.increment()){
			VertexIntervall i= (VertexIntervall) _tree._curr.obj;
			if(i.getPredecessor() != null){
				str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" d:"+i.getReachable()+ " scanned: " + i.isScanned() + " pred:"+i.getPredecessor().getId().toString() +"\n");
			}else{
				
				str.append(l+i.getLowBound()+";"+i.getHighBound()+r+" d:"+i.getReachable()+ " scanned: " + i.isScanned() + " pred: null"+"\n");
			}
		}
			
		return str.toString();
		
	}
	
	
	/**
	 * gives the last Stored VertexIntervall
	 * @return VertexIntervall with maximal lowbound
	 */
	public VertexIntervall getLast(){
		return _last;
	}
	
	
	/**
	 * checks weather last is referenced right
	 * @return true iff everything is OK
	 */
	public boolean checkLast(){
		return _last==_tree._getLast().obj;
	}
	
	/**
	 * Checks weather the given VertexIntervall is the last
	 * @param o EgeIntervall which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(VertexIntervall o){
		return (_last.equals(o));
	}
	
	/**
	 * setter for debug mode
	 * @param debug debug mode true is on
	 */
	public static void debug(boolean debug){
		VertexIntervalls._debug=debug;
	}
	
	/**
	 * gives the next VertexIntervall with respect ot the order contained 
	 * @param o schould be contained
	 * @return next VertexIntervall iff o is not last and contained
	 */
	public VertexIntervall getNext(VertexIntervall o){
		_tree.goToNodeAt(o.getLowBound());
		
			VertexIntervall j = (VertexIntervall) _tree._curr.obj;
			if(j.equals(o)){
				_tree.increment();
				if(!_tree.isAtEnd()){
					VertexIntervall i = (VertexIntervall) _tree._curr.obj;
					_tree.reset();
					return i;
				}else 	throw new IllegalArgumentException("Intervall was already last");
			}
			else throw new IllegalArgumentException("Intervall was not contained");
		
		
	}
	
	/**
	 * finds the first intervall within which
	 *  the node is reachable from the source
	 * @return specified Intervall or null if none exist
	 */
	public VertexIntervall getFirstPossible(){
		VertexIntervall result = this.getIntervallAt(0);
		while(!this.isLast(result)){
			if (result.getReachable()){
				return result;
			}else{
				result=this.getNext(result);
				//TODO more effcient
			}
		}
		if (result.getReachable()){
			return result;
		}	
		return null;
	}
	
	/**
	 * calculates the where it is reachable 
	 * @return min time or Integer.MAX_VALUE if it is not reachable at all
	 */
	public int firstPossibleTime(){
		VertexIntervall test =this.getFirstPossible();
		if(test!=null){
			return test.getLowBound();
		}else{
			return Integer.MAX_VALUE;
		}
		
	}
	
	/**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervalls at which node is reachable
	 * @return true iff anything was changed
	 */
	public boolean setTrue(ArrayList<VertexIntervall> arrive,Link link) {
		boolean changed = false;
		ArrayList<VertexIntervall> arrivecondensed = new ArrayList<VertexIntervall>();
		if(!arrive.isEmpty()){
			//TODO ROST review whether "condensing" the intervall is usefull (the intervalls are not ordered
			//when using bow edges)
			
//			VertexIntervall last= arrive.get(0);
//			for(int j=1; j< arrive.size(); j++){
//				VertexIntervall present = arrive.get(j);
//				if(last.getHighBound()==present.getLowBound() ){
//					last.setHighBound(present.getHighBound());
//					//System.out.println("blub---------------------------------------------");
//				}else{
//					arrivecondensed.add(last);
//					last=present;
//				}
//			}	
//			arrivecondensed.add(last);	
//			if(arrivecondensed.size()!=arrive.size()){
//				//System.out.println("new: "+arrivecondensed.size()+" old: "+arrive.size());
//			}
			//arrivecondensed=arrive;
			rem+=arrive.size()-arrivecondensed.size();
			
			for(int i=0; i< arrive.size(); i++){
				boolean temp= setTrue(arrive.get(i),link);
				if(temp){
					changed=true;
				}
			}
		}
		return changed;
	}
	
	/**
	 * Sets arrival true for all time steps in arrive and sets predecessor to link for each time t
	 * where it was null beforehand
	 * @param arrive Intervall at which node is reachable
	 * @return true iff anything was changed
	 */
	public boolean setTrue(VertexIntervall arrive,Link link){
		boolean changed = false;
		VertexIntervall ourIntervall = this.getIntervallAt(arrive.getLowBound());
		int t= ourIntervall.getHighBound();
		while(ourIntervall.getLowBound() < arrive.getHighBound()){			
			if(!ourIntervall.getReachable() && !ourIntervall.isScanned())
			{
				//test if the intervalls intersect at all (using the condition in while head above)
				if(arrive.getLowBound() >= ourIntervall.getLowBound()
						||
						arrive.getHighBound() > ourIntervall.getLowBound())
				{
					//if arrive contains ourIntervall, we relabel it completely
					if(arrive.contains(ourIntervall))
					{
						setArrivalAttributes(ourIntervall, arrive, link);
						changed = true;
					}
					else if(ourIntervall.contains(arrive))
					{
						//if arrive is contained..
						
						//we adapt our intervall, so that our lowbound equals
						//the low bound of the arrive intervall..
						if(ourIntervall.getLowBound() < arrive.getLowBound())
						{
							ourIntervall = this.splitAt(arrive.getLowBound());
						}
						//or we set our highbound to the highbound of arrival
						if(ourIntervall.getHighBound() > arrive.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							ourIntervall = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//ourintervall has exactly the same bounds as arrive
						//so relabel it completely
						setArrivalAttributes(ourIntervall, arrive, link);
						changed = true;
					}
					else
					{
						//ourIntervall intersects arrive, but is neither contained nor does it contain
						//arrive. thus they overlap somewhere

						//if the lowerBound of arrive, is greater than our lower bound
						//we set our lower bound to the bound of arrive
						if(arrive.getLowBound() > ourIntervall.getLowBound() && arrive.getLowBound() < ourIntervall.getHighBound())
						{
							ourIntervall = this.splitAt(arrive.getLowBound());
						}
						//we adapt our highbound, so that they are the same
						if(arrive.getHighBound() < ourIntervall.getHighBound())
						{
							this.splitAt(arrive.getHighBound());
							ourIntervall = this.getIntervallAt(arrive.getHighBound()-1);
						}
						//we set the attributes
						setArrivalAttributes(ourIntervall, arrive, link);
						changed = true;
					}
				}
			}
			t = ourIntervall.getHighBound();
			//pick next Intervall
			if(Integer.MAX_VALUE==t){
				break;
			}
			ourIntervall= this.getIntervallAt(t);
		}	
		return changed;
	}
	
	protected void setArrivalAttributes(VertexIntervall ourIntervall, VertexIntervall arrive, Link link)
	{
		ourIntervall.setReachable(true);
		ourIntervall.setScanned(false);
		ourIntervall.setPredecessor(link);
		ourIntervall.setLastDepartureAtFromNode(arrive.getLastDepartureAtFromNode());
		ourIntervall.setTravelTimeToPredecessor(arrive.getTravelTimeToPredecessor());
	}
	

	
	/**
	 * finds the next VertexIntervall that has flow less than u after time t
	 * @param t time
	 * @param u capacity
	 * @return
	 */
	/**
	public VertexIntervall minPossible(int t,int u){
		if (t<0){
			throw new IllegalArgumentException("time shold not be negative");
		}
		if (u<=0){
			throw new IllegalArgumentException("capacity shold be positive");
		}
		for(_tree.goToNodeAt(t);_tree.isAtEnd() ;_tree.increment()){
			if(((VertexIntervall)_tree._curr.obj).getFlow()<u){
				return (VertexIntervall)_tree._curr.obj;
			}
		}
		return null;
	}
	**/
	
	//------------------------Clean Up--------------------------------//
	/**
	 * unifies adjacent intervalls, call only when you feel it is safe to do
	 */
	public int cleanup() {
		int gain = 0;
		int timestop = getLast().getHighBound();		
		VertexIntervall i, j;
		i = getIntervallAt(0);
		while (i != null) {
		  if (i.getHighBound() == timestop) break;	
		  j = this.getIntervallAt(i.getHighBound());
		  if(i.getHighBound() != j.getLowBound())
			  throw new RuntimeException("error in cleanup!");
		  if ((i.getHighBound() == j.getLowBound()) && (i.getReachable() == j.getReachable()) &&
				  (i.getPredecessor() == j.getPredecessor()) && (i.isScanned() == j.isScanned())
				  && (i.getTravelTimeToPredecessor() == j.getTravelTimeToPredecessor())
				  && (i.getLastDepartureAtFromNode() == j.getLastDepartureAtFromNode())) {
			 
			  //TODO ROST dont remove both intervalls, no new insert needed MORE EFFICIENT!
			  _tree.remove(i);
			  _tree.remove(j);
			  VertexIntervall vI = new VertexIntervall(i.getLowBound(), j.getHighBound());
			  vI.setReachable(i.getReachable());
			  vI.setScanned(i.isScanned());
			  vI.setTravelTimeToPredecessor(i.getTravelTimeToPredecessor());
			  vI.setPredecessor(i.getPredecessor());
			  vI.setLastDepartureAtFromNode(i.getLastDepartureAtFromNode());
			  _tree.insert(vI);
			  i = vI;
			  gain++;
		  } else {
			  i = j;
		  }		 		 
		}
		_last = (VertexIntervall) _tree._getLast().obj;
		
		return gain;
	}
	
	public VertexIntervall getFirstUnscannedIntervall()
	{
		int lowBound = 0;
		while(lowBound < Integer.MAX_VALUE)
		{
			VertexIntervall vI = this.getIntervallAt(lowBound);
			if(vI.getReachable() &&  !vI.isScanned())
				return vI;
			lowBound = vI.getHighBound();
		}
		return null;
	}
	
	public Integer getFirstTimePointWithDistTrue()
	{
		VertexIntervall vIntervall = this.getFirstUnscannedIntervall();
		if(vIntervall == null)
			return null;
		else
			return vIntervall.getLowBound();
	}
	
	
//------------------------MAIN METHOD--------------------------------//
	
	

}
