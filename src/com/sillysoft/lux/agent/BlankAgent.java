package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  BlankAgent.java
//  Lux
//



import java.util.*;

public class BlankAgent implements LuxAgent
{
// This agent's ownerCode:
protected int ID;

// Store some refs the board and to the country array
protected Board board;
protected Country[] countries;

// It might be useful to have a random number generator
protected Random rand;

public BlankAgent()
	{
	rand = new Random();
	}

// Save references 
public void setPrefs( int newID, Board theboard )
	{
	ID = newID;		// this is how we distinguish what countries we own

	board = theboard;
	countries = board.getCountries();
	}

public String name()
	{
	return "BlankAgent";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "BlankAgent is a file for you to use as a basis for your own agent.";
	}

public int pickCountry()
	{
	return -1;
	}

public void placeInitialArmies( int numberOfArmies )
	{
	}

public void cardsPhase( Card[] cards )
	{
	}

public void placeArmies( int numberOfArmies )
	{
	}

public void attackPhase()
	{
	}

public int moveArmiesIn( int cca, int ccd)
	{
	return 0;
	}

public void fortifyPhase()
	{
	}

public String youWon()
	{ 
	// For variety we store a bunch of answers and pick one at random to return.
	String[] answers = new String[] {
		"I won",
		"beee!"
		};

	return answers[ rand.nextInt(answers.length) ];
	}

public String message( String message, Object data )
	{
	return null;
	}

}
