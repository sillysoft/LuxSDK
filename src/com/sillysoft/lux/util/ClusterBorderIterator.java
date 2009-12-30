package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;
import java.util.ArrayList;
import java.util.List;

//
//  ClusterBorderIterator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**
Iterate through the countries that form the border of a cluster around a root Country.
NOTE: This class is slightly different than the other CountryIterators.
 	When created with a Country <i>root</i>, this will give an enumeration of all the coutries on the border of the cluster centered around this root.
*/


public class ClusterBorderIterator extends CountryIterator {

public ClusterBorderIterator(Country root)
	{
	List cluster = new ArrayList();
	cluster.add(root);
	
	List borders = new ArrayList();
	
	for (int i = 0; i < cluster.size(); i++) {
		CountryIterator neighbors = new NeighborIterator( (Country)cluster.get(i) );
		while (neighbors.hasNext()) {
			Country neighbor = neighbors.next();
			if (neighbor.getOwner() == root.getOwner()) {
				if (! cluster.contains(neighbor) ) {
					cluster.add(neighbor);
					}
				}
			else
				{
				// an enemy, so cluster.get(i) is a border of this cluster
				if (! borders.contains(cluster.get(i)))
					borders.add(cluster.get(i));
				}
			}
		}
	
	// So we have found the borders of the cluster. set the countries array to it
	countries = new Country[borders.size()];
	for (int i = 0; i < countries.length; i++)
		countries[i] = (Country)borders.get(i);
	
	getNextReady();
	}

// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	return true;
	}

}