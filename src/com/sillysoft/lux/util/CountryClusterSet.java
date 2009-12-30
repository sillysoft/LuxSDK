package com.sillysoft.lux.util;

import com.sillysoft.lux.*;
import java.util.List;
import java.util.ArrayList;

//
//  CountryClusterSet.java
//  Lux
//
//  Created by Dustin Sacks on 1/12/05.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

/**

CountryClusterSet is a data structure that stores a set of CountryCluster objects.

*/

public class CountryClusterSet 
{

/** Private constructor. Use the static methods to get the kind of CountryClusterSet you want. */
private CountryClusterSet(List clusters)
	{
	this.clusters = clusters;
	}

// A list of lists. Each one containing a cluster of countries.
private List clusters;

/** Create a set of CountryClusters making up all the countries owned by the given player. */
public static CountryClusterSet getAllCountriesOwnedBy(int owner, Country[] countries)
	{
	// Get the set of all countries owned by this player
	List fullSet = new ArrayList();
	CountryIterator e = new PlayerIterator(owner, countries);
	while (e.hasNext())
		{
		fullSet.add(e.next());
		}

	if (fullSet.size() == 0)
		System.out.println("WARNING: CountryClusterSet created with a dead player");

	// Now divide it up into clusters
	List clusters = new ArrayList();

	while (fullSet.size() > 0)
		{
		List cluster = new ArrayList();
		cluster.add(fullSet.get(0));

		for (int i = 0; i < cluster.size(); i++) 
			{
			CountryIterator neighbors = new NeighborIterator( (Country)cluster.get(i) );
			while (neighbors.hasNext()) 
				{
				Country neighbor = neighbors.next();
				if (neighbor.getOwner() == owner && ! cluster.contains(neighbor) ) 
					{
					cluster.add(neighbor);
					}
				}
			}

		clusters.add(cluster);
		// remove all the countries in this cluster from the full set.
		// we will continue making clusters out of what is left
		for (int i = 0; i < cluster.size(); i++) 
			fullSet.remove(cluster.get(i));
		}

	return new CountryClusterSet(clusters);
	}

/** Get the set of clusters that contain every Country in the given array that is not owned by the given player. */
public static CountryClusterSet getAllCountriesNotOwnedBy(int notOwnedBy, Country[] countries)
	{
	// Get a list of all enemy countries:
	List outstandingCountries = new ArrayList();
	for (int i = 0; i < countries.length; i++)
		if (countries[i].getOwner() != notOwnedBy)
			outstandingCountries.add(countries[i]);

	List clusters = new ArrayList();			// the end result

	// Now divide them into clusters, pulling them out of the outstandingCountries list as we go:
	while (outstandingCountries.size() > 0)
		{
		Country nextRoot = (Country) outstandingCountries.remove(0);

		// Build a cluster out from the root
		List cluster = new ArrayList();
		cluster.add(nextRoot);

		for (int i = 0; i < cluster.size(); i++) 
			{
			CountryIterator neighbors = new NeighborIterator( (Country)cluster.get(i) );
			while (neighbors.hasNext()) 
				{
				Country neighbor = neighbors.next();
				if (neighbor.getOwner() != notOwnedBy && ! cluster.contains(neighbor) && outstandingCountries.contains(neighbor)) 
					{
					cluster.add(neighbor);
					outstandingCountries.remove(neighbor);
					}
				}
			}
		clusters.add(cluster);
		}

	return new CountryClusterSet(clusters);
	}

/** Get the set of clusters that contain all the countries in 'startingCountries' as well as all connecting countries that are hostile to the given owner. */
public static CountryClusterSet getHostileCountries(int hostileToOwner, List startingCountries)
	{
	List clusters = new ArrayList();

	List fullSet = new ArrayList(startingCountries);
	while(fullSet.size() > 0)
		{
		List cluster = new ArrayList();
		cluster.add(fullSet.get(0));

		for (int i = 0; i < cluster.size(); i++) 
			{
			CountryIterator neighbors = new NeighborIterator( (Country)cluster.get(i) );
			while (neighbors.hasNext()) 
				{
				Country neighbor = neighbors.next();
				if (neighbor.getOwner() != hostileToOwner && ! cluster.contains(neighbor) ) 
					{
					cluster.add(neighbor);
					}
				}
			}

		clusters.add(cluster);
		// remove all the countries in this cluster from the unused list.
		// we will continue making clusters out of what is left
		for (int i = 0; i < cluster.size(); i++) 
			fullSet.remove(cluster.get(i));
		}

	return new CountryClusterSet(clusters);
	}


public int numberOfClusters()
	{
	return clusters.size();
	}

public CountryCluster getCluster(int i)
	{
	return new CountryCluster((List)clusters.get(i));
	}

/** Return the number of Clusters in this set. */
public int size()
	{
	return clusters.size();
	}

/** Sort the clusters in this set so they go from weakest (in number of armies) to strongest. */
public void orderWeakestFirst()
	{
	// Bubble sort. slow but easy to type out...
	boolean madeChange = true;
	while (madeChange)
		{
		madeChange = false;
		for (int i = 0; i < clusters.size()-1; i++)
			{
			if (getCluster(i).getArmies() > getCluster(i+1).getArmies())
				{
				clusters.add(i, clusters.remove(i+1));
				}
			}
		}
	}
}
