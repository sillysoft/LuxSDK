package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Trotsky.java
//  Lux
//
//  Created by Dustin Sacks on 11/10/04.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

import java.util.*;

public class Trotsky extends Communist
{


public String name()
	{
	return "Trotsky";
	}
	
public float version()
	{
	return 1.3f;
	}
	
public String description()
	{
	return "Communism with some added smarts.";
	}

protected List targetContinents;		// a list of Integers for each continent we want

public void cardsPhase( Card[] cards )
	{	
	if (Card.containsASet(cards))
		{
		Card[] set = Card.getBestSet( cards, ID, countries );
		board.cashCards(set[0], set[1], set[2]);
		}
	}

/** Set the internal targetContinents List to contain the continent IDs of conts that we want. */	
protected void setTargetContinents()
	{
	debug("trotsky's setTargetContinents() called");
	targetContinents = new ArrayList();
	double[] friendlyRatio = new double[numContinents]; 
	for(int i = 0; i < numContinents; i++)
		{
		if (BoardHelper.playerOwnsContinent(ID, i, countries))
			{
			if (board.getContinentBonus(i) > 0)
				{
				targetContinents.add(new Integer(i));
				}
			friendlyRatio[i] = -1;	// kill its ratio
			}
		else
			{
			// we don't own this cont
			int friendlies = BoardHelper.getPlayerArmiesInContinent(ID, i, countries);
			friendlies += BoardHelper.getPlayerArmiesAdjoiningContinent(ID, i, countries);
			int enemies = BoardHelper.getEnemyArmiesInContinent(ID, i, countries);

			friendlyRatio[i] = (double)friendlies / (double)Math.max( enemies, 1);
			}
		}

	// In addition to anything we own, add the best non-owned cont
	double bestRatio = -1;
	int bestCont = -1;
	for (int i = 0; i < numContinents; i++)
		{
		debug("friendlyRatio[i]="+friendlyRatio[i]);
		if (friendlyRatio[i] > bestRatio && board.getContinentBonus(i) > 0)
			{
			bestRatio = friendlyRatio[i];
			bestCont = i;
			}
		}

	if (bestRatio == 0)
		{
		// This means that we only have countries in (or touching) continents that are worth zero or less.
		// Do we have a non-zero ratio from a cont that is worth zero? try that first
		for (int i = 0; i < numContinents; i++)
			{
			if (friendlyRatio[i] > bestRatio && board.getContinentBonus(i) == 0)
				{
				bestRatio = friendlyRatio[i];
				bestCont = i;
				}
			}
		if (bestRatio != 0)
			{
			// we got one (probably the castle lux country-side
			targetContinents.add(new Integer(bestCont));
			}
		else
			{
			// darn. we only have a ratio for negative value continents
			// take the best ratio
			for (int i = 0; i < numContinents; i++)
				{
				if (friendlyRatio[i] > bestRatio)
					{
					bestRatio = friendlyRatio[i];
					bestCont = i;
					}
				}
			targetContinents.add(new Integer(bestCont));
			}
		}
	else
		{
		if (bestCont == -1)
			{
			debug("Trotsky cannot find a good continent, going for 0.");
			targetContinents.add(new Integer(0));
			}
		else
			targetContinents.add(new Integer(bestCont));
		}

//	debug(name()+" targets:");
//	for (int i = 0; i < targetContinents.size(); i++)
//		debug(" -> "+board.getContinentName( ((Integer)targetContinents.get(i)).intValue() ));
	}


public int pickCountry()
	{
	return pickCountryInSmallContinent();
	}

// this over-rides the SmartAgentBase method to provide better support for Castle Lux
protected int pickCountryInSmallContinent()
	{
	if (goalCont == -1)
	// then we don't have a target cont yet
		{
		goalCont = -1;
		goalCont = BoardHelper.getSmallestPositiveEmptyCont(countries, board);

		if (goalCont == -1) // oops, there are no unowned conts
			goalCont = BoardHelper.getSmallestPositiveOpenCont(countries, board);
		}

	if (goalCont == -1 || board.getContinentBonus(goalCont) < 1)
		{
		// our goal continent has no bonus.
		// pick a country touching one of our contries
		return pickCountryTouchingUs();
		}

	// if we are here then we DO have a target cont.
	return pickCountryInContinent( goalCont );
	}

// We place armies one at a time on the weakest country that we own in our target continents
public void placeArmies( int numberOfArmies )
	{
	if (hogWildCheck())
		{
		Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
		placeArmiesOnClusterBorder(numberOfArmies, root);
		return;
		}
		
	setTargetContinents();

	int leftToPlace = numberOfArmies;
	while (leftToPlace > 0)
		{
//		System.out.println("leftToPlace="+leftToPlace+", targetCont="+targetContinents);
		int leastArmies = 1000000;
		CountryIterator ours = new PlayerIterator(ID, countries);
		while (ours.hasNext() && leftToPlace > 0)
			{
			Country us = ours.next();

			if (isTargetContinent(us.getContinent()))
				leastArmies = Math.min(leastArmies, us.getArmies());
			}

		if (leastArmies == 1000000)
			{
			// nothing found? happens on Arms Race. just place evenly
			System.out.println("engaging ARMS RACE workaround!");
			while (leftToPlace > 0)
				{
				ours = new PlayerIterator(ID, countries);
				while (ours.hasNext() && leftToPlace > 0)
					{
					Country us = ours.next();
					
					board.placeArmies(1, us);
					leftToPlace -= 1;
					}
				}
			}
			
		// Now place an army on anything with less or equal to <leastArmies>
		CountryIterator placers = new ArmiesIterator(ID, -(leastArmies), countries);

		while (placers.hasNext())
			{
			Country us = placers.next();

			if (isTargetContinent(us.getContinent()) || adjoinsTargetContinent(us))
				{
				board.placeArmies(1, us);
				leftToPlace -= 1;
				}
			}
		}
	}

// We pick expando as the country we own that has the weakest enemy country beside it in our taget continents.
protected void setExpandos()
	{
	debug("trotsky's setexpando called");
	int leastNeighborArmies = 1000000;
	expando = -1;
	expandTo = -1;

	for (int i = 0; i < board.getNumberOfCountries(); i++)
		{
		if ( countries[i].getOwner() == ID )
			{
			// We own this country, so it COULD be expando.

			// Get country[i]'s neighbors:
			Country[] neighbors = countries[i].getAdjoiningList();

			// Now loop through the neighbors and find the weakest in our target:
			for (int j = 0; j < neighbors.length; j++)
				{
				if ( neighbors[j].getOwner() != ID && neighbors[j].getArmies() < leastNeighborArmies && isTargetContinent(neighbors[j].getContinent()) )
					{
					leastNeighborArmies = neighbors[j].getArmies();
					expando = i;
					expandTo = neighbors[j].getCode();
					}
				}
			}
		}

	if (expando == -1)
		{
		// this means we do not touch the countries that we want. This usually happens on Castle Lux
		// when we have conquered a section and are blocked only by bridges.
		// We will set an expando if we outnumber anyone by 20 armies
		for (int i = 0; i < board.getNumberOfCountries(); i++)
			{
			if ( countries[i].getOwner() == ID )
				{
				// We own this country, so it COULD be expando.

				// Get country[i]'s neighbors:
				Country[] neighbors = countries[i].getAdjoiningList();

				// Now loop through the neighbors and find the weakest in our target:
				for (int j = 0; j < neighbors.length; j++)
					{
					if ( neighbors[j].getOwner() != ID && neighbors[j].getArmies() < leastNeighborArmies )
						{
						if (countries[i].getArmies() > neighbors[j].getArmies() + 20)
							{
							leastNeighborArmies = neighbors[j].getArmies();
							expando = i;
							expandTo = neighbors[j].getCode();
							}
						}
					}
				}
			}
		}

	if (expando != -1 && expandTo != -1)
		{
		debug(" -> expando is "+expando+", in cont "+board.getContinentName(countries[expando].getContinent()));
		debug(" -> expandTo is "+expandTo+", in cont "+board.getContinentName(countries[expandTo].getContinent()));
		}
	else
		debug(" -> expando or expandTo is -1");

	}

public void attackPhase()
	{
	setExpandos();	// this sets expando and expandTo

	if (expando == -1)
		return;	// nowhere to go

	// Now we see if we have a good chance of taking the weakest link over:
	if ( expandTo != -1 && countries[expando].getArmies() > countries[expandTo].getArmies() )
		{
		// We attack till dead, with max dice:
		board.attack( expando, expandTo, true);
		}

	attackHogWild();
	attackStalemate();
	}

protected boolean isTargetContinent(int contCode)
	{
	if (targetContinents == null)
		return false;

	return targetContinents.contains(new Integer(contCode));
	}

protected boolean adjoinsTargetContinent( Country c)
	{
	Country[] adjoins = c.getAdjoiningList();
	for (int i = 0; i < adjoins.length; i++)
		{
		if (isTargetContinent(adjoins[i].getContinent()))
			return true;
		}
	return false;
	}

public void fortifyPhase()
	{
	if (hogWildCheck())
		{
		// move armies outwards for attacking
		Country root = BoardHelper.getPlayersBiggestArmy(ID, countries);
		fortifyCluster( root );
		}
	else
		super.fortifyPhase();
	}

public String youWon()
	{ return "Communism with smarts and style.\nWhat a fun combination!"; }

public int moveArmiesIn( int cca, int ccd )
	{
	if (hogWildCheck())
		{
		// move everyone to the side with the most enemies
		int Aenemies = countries[cca].getNumberEnemyNeighbors();
		int Denemies = countries[ccd].getNumberEnemyNeighbors();

		// If the attacking country had more enemies, then we leave all possible 
		// armies in the country they attacked from (thus we move in 0):
		if ( Aenemies > Denemies )
			return 0;

		// Otherwise the defending country has more neighboring enemies, move in everyone:
		return countries[cca].getArmies()-1;
		}
		
	return super.moveArmiesIn( cca, ccd );
	}

}
