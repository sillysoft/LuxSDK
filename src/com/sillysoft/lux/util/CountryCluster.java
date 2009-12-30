package com.sillysoft.lux.util;

import com.sillysoft.lux.*;
import java.util.List;
import java.util.ArrayList;

//
//  CountryCluster.java
//  Lux
//
//  Created by Dustin Sacks on 1/12/05.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

/**

CountryCluster is a data structure which stores a set of connected country objects. It can be used to find a traversal route through the cluster.

*/


public class CountryCluster 
{

/** Create a CountryCluster made up of every Country that is owned by the owner of 'root' and connected through friendly countries to 'root'. */
public static CountryCluster getOwnedCluster(Country root)
	{
	return CountryCluster.getOwnedCluster(root, false);
	}

/** Create a CountryCluster made up of every Country that is owned by the owner of 'root' and connected through friendly countries to 'root'.  This constructor differs from the first one in that it will try to order the countries based on least enemies first. */
public static CountryCluster getOwnedCluster(Country root, boolean orderNeighbors)
	{
	if (root == null)
		throw new NullPointerException("CountryCluster created with a null root country");

	// Build the cluster out from the root
	List cluster = new ArrayList();
	cluster.add(root);
	int owner = root.getOwner();

	for (int i = 0; i < cluster.size(); i++) 
		{
		// Use an OrderedNeighborIterator to pull off the countries with the least enemies first.
		// This should mean that we will end on a country with some enemies (if possible).
		CountryIterator neighbors;
		if (orderNeighbors)
			neighbors = new OrderedNeighborIterator( (Country)cluster.get(i) );
		else
			neighbors = new NeighborIterator( (Country)cluster.get(i) );
		while (neighbors.hasNext()) 
			{
			Country neighbor = neighbors.next();
			if (neighbor.getOwner() == owner && ! cluster.contains(neighbor) ) 
				{
				cluster.add(neighbor);
				}
			}
		}

	return new CountryCluster(cluster);
	}

private List cluster;

/** Create a CountryCluster containing exactly the countries in the given List. The CountryCluster is backed by the List, so any changes to the List will change the CountryCluster. */
public CountryCluster(List countries)
	{
	cluster = countries;
	}

/** Get a count of the combined number of armies in this cluster. */
public int getArmies()
	{
	int result = 0;
	for (int i = 0; i < cluster.size(); i++) 
		result += ((Country)cluster.get(i)).getArmies();
	return result;
	}

/** Return a List containing all the Country object that are included in this CountryCluster. */
public List getList()
	{
	return cluster;
	}

/** Get a count of the number of countries in this cluster. */
public int size()
	{
	return cluster.size();
	}

/** A possibly very slow method to get a CountryRoute that traverses through all the countries in the cluster. It will be 'simple' in that the route can be followed by always moving all the armies forward. If such a route exists then this method will be sure to find one. For large clusters (30+ countries) this method can take a VERY long time. I believe this problem is NP-complete, which means that mathematicians have not been able to find a fast way of solving it. */
public CountryRoute getSimpleRoute()
	{
	return getSimpleRoute(false);
	}

/** Same as	getSimpleRoute() but a speed optimization can be made if the cluster is all owned by one player. */
public CountryRoute getSimpleRoute(boolean optimizeForSingleOwner)
	{
	// We will try and get a simple walk through the cluster.
	// Start at each country and do a walk.
	// If we find a full route then good. Otherwise, too bad.
	for (int i = 0; i < cluster.size(); i++)
		{
		Country startAt = (Country) cluster.get(i);
		// We can eliminate some starting points if we assume 
		// that the cluster is all owned by a single player.
		if (! optimizeForSingleOwner || startAt.getNumberEnemyNeighbors() > 0)
			{ 
			CountryRoute result = getSimpleRouteStartingAt(startAt);
			if (result != null)
				return result;
			}
		}

	return null;
	}

/** Same as getSimpleRoute(boolean optimizeForSingleOwner) but a speed optimization can be made assuming the goal is for a specific player to kill the entire found route. */
public CountryRoute getSimpleRoute(boolean optimizeForSingleOwner, int optimizeForAttackingPlayer)
	{
	// Loop 1: start by examining countries that directly neighbor the attacking player
	for (int i = 0; i < cluster.size(); i++)
		{
		Country startAt = (Country) cluster.get(i);
		// We can eliminate some starting points if we assume 
		// that the cluster is all owned by a single player.
		if (startAt.getNumberPlayerNeighbors(optimizeForAttackingPlayer) > 0)
			{ 
			CountryRoute result = getSimpleRouteStartingAt(startAt);
			if (result != null)
				return result;
			}
		}

	// Loop 2: start at countries even if they don't border the attacking player directly

	for (int i = 0; i < cluster.size(); i++)
		{
		Country startAt = (Country) cluster.get(i);
		// We can eliminate some starting points if we assume 
		// that the cluster is all owned by a single player.
		if (startAt.getNumberPlayerNeighbors(optimizeForAttackingPlayer) == 0
			&& (! optimizeForSingleOwner || startAt.getNumberEnemyNeighbors() > 0))
			{ 
			CountryRoute result = getSimpleRouteStartingAt(startAt);
			if (result != null)
				return result;
			}
		}

	return null;
	}


/** Try and get a Route through the cluster that starts at the given country. If none can be found then return null. */
public CountryRoute getSimpleRouteStartingAt(Country start)
	{
	// A cluster with only one country is itself a route
	if (cluster.size() == 1)
		return new CountryRoute(cluster);

	// Do a recursive search for a route in the cluster made by removing the start country
	CountryRoute found = null;
	CountryCluster startRemoved = this.cloneRemoving(start);
	Country[] adjoining = start.getAdjoiningList();
	for (int i = 0; i < adjoining.length && found == null; i++)
		{
		if (cluster.contains(adjoining[i]))
			found = startRemoved.getSimpleRouteStartingAt(adjoining[i]);
		}

	if (found == null)
		return null;

	// Take the recursively found path and add the removed start country at the start
	List result = new ArrayList();
	result.add(start);
	for (int i = 0; i < found.size(); i++)
		result.add(found.get(i));

	return new CountryRoute(result);
	} 

private CountryCluster cloneRemoving(Country remove)
	{
	List cloneList = new ArrayList(cluster);
	cloneList.remove(remove);
	return new CountryCluster(cloneList);
	}

/*
	Country walkFrom = start;
	List walked = new ArrayList();
	walked.add(walkFrom);

	while (walkFrom != null)
		{
		walkFrom = expandWalk(walkFrom, walked, cluster);
		}

	if (walked.size() == cluster.size())
		{
		// success! we walked the whole way.
		// return the path
		return new CountryRoute(walked);
		}

	return null;
	}		*/

/** Take a country to walk from and lists of the already walked countries and all the countries. Walk to one country untouched so far and return the new country (or null if none can be found). */
private static Country expandWalk(Country walkFrom, List walked, List cluster)
	{
	Country[] adjoining = walkFrom.getAdjoiningList();
	for (int i = 0; i < adjoining.length; i++)
		{
		if (cluster.contains(adjoining[i]) && ! walked.contains(adjoining[i]))
			{
			// then lets walk here
			walked.add(adjoining[i]);
			return adjoining[i];
			}
		}
	return null;
	}


/** Return a country that borders the cluster and is owned by ownerID. If mutliple countries with that owner exist then return the one with the most armies. If no bordering countries exist then return null. */
public Country getStrongestNeighborOwnedBy(int ownerID)
	{
	int strongestArmies = -1;
	Country strongestNeighbor = null;

	for (int i = 0; i < cluster.size(); i++)
		{
		Country next = (Country) cluster.get(i);
		Country strongestCheck = next.getStrongestNeighborOwnedBy(ownerID);
		if (strongestCheck != null && strongestCheck.getArmies() > strongestArmies)
			{
			strongestArmies = strongestCheck.getArmies();
			strongestNeighbor = strongestCheck;
			}
		}
	return strongestNeighbor;
	}

public int estimatedNumberOfArmiesNeededToConquer()
	{
	return (int) ( (getArmies() + size())*1.2 );
	}

public String toString()
	{
	StringBuffer buffer = new StringBuffer("\n\t<CountryCluster size:"+size()+" countries:\n\t");
	for (int i = 0; i < size(); i++)
		{
		buffer.append(cluster.get(i));
		if (i != size()-1)
			buffer.append("\n\t");
		}
	buffer.append(" end of cluster>");
	return buffer.toString();
	}

}
