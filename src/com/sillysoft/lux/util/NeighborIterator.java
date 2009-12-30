package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  NeighborIterator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**  An Iterator of the neighbors to a given Country.	*/

public class NeighborIterator extends CountryIterator {

Country base;

// This one is for backwards compatibility. It can only be the last in a link
public NeighborIterator(Country c)
	{
	this.countries = c.getAdjoiningList();
	base = null;
	
	getNextReady();
	}

public NeighborIterator(Country c, Country[] countries)
	{
	this.countries = countries;
	base = c;
	
	getNextReady();
	}
	
public NeighborIterator(int code, Country[] countries)
	{
	this.countries = countries;
	base = countries[code];
	
	getNextReady();
	}


public NeighborIterator(Country c, CountryIterator iter)
	{
	this.countries = iter.countries;
	chained = iter;
	base = c;
	
	getNextReady();
	}
	
public NeighborIterator(int code, CountryIterator iter)
	{
	this.countries = iter.countries;
	chained = iter;
	base = countries[code];
	
	getNextReady();
	}

	
// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	if (base == null)
		{
		// backward compatible version
		return true;
		}
		
	if (base.canGoto(countries[code]))
		{
		return true;
		}
	return false;
	}

}
