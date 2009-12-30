package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  PlayerIterator.java
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
An Iterator of all the Countries that are owned by <I>player</i>.
*/


public class PlayerIterator extends CountryIterator {

private int player;

public PlayerIterator(int player, Country[] countries)
	{
	this.countries = countries;
	this.player = player;
	
	getNextReady();
	}

public PlayerIterator(int player, CountryIterator iter)
	{
	chained = iter;
	this.countries = iter.countries;
	this.player = player;
	
	getNextReady();
	}

// This method should be over-riden in subclasses for different kinds.
protected boolean isAHit( int code )
	{
	if (countries[code].getOwner() == player)
		{
		return true;
		}
	return false;
	}

}
