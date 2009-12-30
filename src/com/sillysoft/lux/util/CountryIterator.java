package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  CountryIterator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//
//	A CountryIterator is just a loop through an array of countries that will only 
//	return the Countries that meet the desired criteria.
// 


/**  An abstract class to serve as the superclass for a variety of different Country Iterators.
	<P>
The real power of CountryIterators is that they can be chained together. For example
	<BR>
  CountryIterator foo = new PlayerIterator(0, new ContinentIterator(1, new NeighborIterator(5, countries)));	<br>
Will return an enumeration of all the countries owned by player 0 that are in continent 1 and border couuntry 5
	<P>
The only difference among the subclasses provided is the constructor, which all take different parameters.
	<P>
All subclasses must:
	<BR>
1. Have a constructor that sets the class variable <i>countries</i> to an array of Countries (usually it's all the countries in the game). The constructor must call getNextReady() as its last action.
	<p>
2. implement the
<BR>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; abstract protected boolean isAHit( int code );
<BR> 	method, with <i>code</i> being the array index of the country to check to see if it should be returned.
*/

abstract public class CountryIterator {

private int checkedTo = -1;
private Country next = null;
protected Country[] countries;

protected CountryIterator chained;	// a chained CountryIterator or null


/** Whether or not the Iterator contains more Countrys */
public boolean hasNext()
	{
	if (next == null)
		return false;
	return true;
	}

/** Removes the next Country from the Iterator and returns it. */
public Country next()
	{
	Country temp = next;
	getNextReady();
	return temp;
	}

/** An internal method, you shouldn't use it. */
protected void getNextReady()
	{
	for (checkedTo++; checkedTo < countries.length && ! isAFullHit(checkedTo); checkedTo++)
		{}
	
	if (checkedTo == countries.length)
		next = null;
	else
		next = countries[checkedTo];
	}


/** Check the stored iterator to see if it's not a hit for them.
Only return true if it's a hit for both us and chained. */
private boolean isAFullHit(int code)
	{
	if (chained != null)
		{
		if (! chained.isAFullHit(code))
			return false;
		}
		
	return isAHit(code);
	}

/** Subclasses must implement this method to specialize their behavior. */
abstract protected boolean isAHit( int code );
}
