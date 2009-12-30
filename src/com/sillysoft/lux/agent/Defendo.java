package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

import java.util.*;

//
//  Defendo.java
//	Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//



public class Defendo extends SmartAgentBase 
{

protected List desiredCountries;

public String name()
	{
	return "Defendo";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "Defendo will try to hold on to the countries he starts with.";
	}


public int pickCountry()
	{
	return pickCountryInSmallContinent();
	}
	
public void placeArmies( int numberOfArmies )
	{
/*	if (ownDesiredCountries())
		{
		placeArmiesCommunist(numberOfArmies);
		}
	else
		{
		placeArmiesToRetakeLands();
		}	*/
	}
	
protected void placeArmiesCommunist(int numberOfArmies)
	{
	int leftToPlace = numberOfArmies;
	while (leftToPlace > 0)
		{
		int leastArmies = 1000000;
		CountryIterator ours = new PlayerIterator(ID, countries);
		while (ours.hasNext() && leftToPlace > 0)
			{
			Country us = ours.next();

			leastArmies = Math.min(leastArmies, us.getArmies());
			}

		// Now place an army on anything with less or equal to <leastArmies>
		CountryIterator placers = new ArmiesIterator(ID, -(leastArmies), countries);

		while (placers.hasNext())
			{
			Country us = placers.next();
			board.placeArmies(1, us);
			leftToPlace -= 1;
			}
		}
	}

public void attackPhase( )
	{
	}

public int moveArmiesIn( int cca, int ccd)
	{
	return -1;
	}

public void fortifyPhase()
	{
	}	// End of fortifyPhase() method


public String youWon()
	{ 
	String[] answers = new String[] { "Not possible!" };

	return answers[ rand.nextInt(answers.length) ];
	}
}	// End of Cluster class
