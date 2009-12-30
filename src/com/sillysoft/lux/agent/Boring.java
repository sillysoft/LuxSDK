package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

import java.util.Random;
import java.util.List;

public class Boring implements LuxAgent 
{
// A random number generator:
private Random rand;

// This agent's ownerCode:
private int ID;

private Board board;
private Country[] countries;

public Boring()
	{
	rand = new Random();
	}

public void setPrefs( int newID, Board theboard )
	{
	ID = newID;
	board = theboard;
	countries = board.getCountries();
	}

public String name()
	{
	return "Boring";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "Boring is an AI that does very little.";
	}

public void cardsPhase(  Card[] cards )
	{
	// the world will force a cardCash when we have 5 cards
	}

public int pickCountry(  )
	{
	// Generate a random number (from 0 to 41):
	int cc = rand.nextInt(countries.length);

	// Continue geneerating random country codes until we have found a country with no owner:
	while ( countries[cc].getOwner() != -1 )
		{
		cc = rand.nextInt(countries.length);
		}
	// Return the unowned country's countryCode:
	return cc;
	}


public void placeInitialArmies( int numberOfArmies )
    {
    placeArmies(numberOfArmies);
    }

public void placeArmies( int numberOfArmies )
	{
	// Generate a random number (from 0 to 41):
	int cc = rand.nextInt(countries.length);

	// Continue generating random country codes until we have found a country that we own:
	while ( countries[cc].getOwner() != ID )
		{
		cc = rand.nextInt(countries.length);
		}
	// put all of our armies there:
	board.placeArmies( numberOfArmies, cc );
	}

public void attackPhase(  )
	{
	// The BoringAgent never attacks anyone.
	return;
	}

public int moveArmiesIn( int countryCodeAttacker, int countryCodeDefender)
	{
	// Since BoringAgent never attacks anyone, this method will never be called.
	return 0;
	}

public void fortifyPhase(  )
	{
	// The BoringAgent never moves anywhere.
	return;
	}

public String youWon()
	{ return null; }


// This method isn't used for anything, but it is part of the interface.
public String message( String message, Object data )
	{
	return null;
	}
}
