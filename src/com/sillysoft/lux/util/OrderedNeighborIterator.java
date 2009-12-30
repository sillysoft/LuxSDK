package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;
import java.util.List;
import java.util.ArrayList;

//
//  OrderedNeighborIterator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**  An Iterator of the neighbors to a given Country that returns them in the order of least enemy neighbors first.	*/


public class OrderedNeighborIterator extends CountryIterator {

public OrderedNeighborIterator(Country c)
	{
	this.countries = c.getAdjoiningList();

	// Get an array with the proper order
	List ordered = new ArrayList();
	ordered.add(countries[0]);
	for (int i = 1; i < countries.length; i++)
		{
		int enemies = countries[i].getNumberEnemyNeighbors();
		for (int j = 0; j < ordered.size(); j++)
			{
			if (enemies < ((Country)ordered.get(j)).getNumberEnemyNeighbors())
				{
				ordered.add(j, countries[i]);
				break;	// exit the j for loop, because we have added the country
				}
			}
		// Put it at the end if we never added it
		if (! ordered.contains(countries[i]))
			ordered.add(countries[i]);
		}
	
	Country[] orderedCountries = new Country[ordered.size()];
	for (int i = 0; i < ordered.size(); i++)
		orderedCountries[i] = (Country) ordered.get(i);
		
	this.countries = orderedCountries;
	getNextReady();
	}
	
// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	// Every element of our array is a hit, since we precalculated it
	return true;
	}

}
