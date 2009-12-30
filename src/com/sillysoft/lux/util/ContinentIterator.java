package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  ContinentIterator.java
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
An Iterator of all the Countries that are members of <I>continent</i>.
*/


public class ContinentIterator extends CountryIterator 
{
private int continent;

public ContinentIterator(int continent, Country[] countries)
	{
	this.countries = countries;
	this.continent = continent;
	
	getNextReady();
	}

public ContinentIterator(int continent, CountryIterator iter)
	{
	chained = iter;
	this.countries = iter.countries;
	this.continent = continent;
	
	getNextReady();
	}


// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	if (countries[code].getContinent() == continent)
		{
		return true;
		}
	return false;
	}

}
