package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;
//
//  ArmiesIterator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is free for use in non-profit purposes.
//	For other uses please contact lux@sillysoft.net
//

/**

An Iterator of all the Countries with more than <I>minArmies</i> armies that is owned by <i>player</i>.

*/

public class ArmiesIterator extends CountryIterator {

private int player;
private int armies;

public ArmiesIterator(int player, int minArmies, Country[] countries)
	{
	this.countries = countries;
	this.player = player;
	armies = minArmies;
	
	getNextReady();
	}

// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	if (armies < 0 && countries[code].getOwner() == player && countries[code].getArmies() <= Math.abs(armies))
		{
		// then they want all countries with less or equal then abs(armies)
		return true;
		}
	else if (armies > 0 && countries[code].getOwner() == player && countries[code].getArmies() >= armies)
		return true;
	
	return false;
	}

}
