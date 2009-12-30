package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Noisy.java
//  Lux
//
//  Created by Dustin Sacks on Thu Jul 01 2004.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

public class Noisy extends Cluster
{

public void setPrefs( int ID, Board board )
	{
	super.setPrefs(ID, board);
	this.commonMethod("setPrefs");
	}

public int pickCountry()
	{
	this.commonMethod("pickCountry");
	return super.pickCountry();
	}

public void placeInitialArmies( int numberOfArmies )
	{
	this.commonMethod("placeInitialArmies");
	super.placeInitialArmies(numberOfArmies);
	}

public void cardsPhase( Card[] cards )
	{
	this.commonMethod("cardsPhase");
	}

public void placeArmies( int numberOfArmies )
	{
	this.commonMethod("placeArmies");
	super.placeArmies(numberOfArmies);
	}

public void attackPhase()
	{
	this.commonMethod("attackPhase");

	super.attackPhase();
	}

public int moveArmiesIn( int countryCodeAttacker, int countryCodeDefender )
	{
	this.commonMethod("moveArmiesIn");
	return super.moveArmiesIn(countryCodeAttacker, countryCodeDefender);
	}

public void fortifyPhase()
	{
	this.commonMethod("fortifyPhase");
	super.fortifyPhase();
	}

public String name()
	{
	if (board != null)
		this.commonMethod("name");
	return "Noisy";
	}

public float version()
	{
	if (board != null)
		this.commonMethod("version");
	return 1.0f;
	}

public String description()
	{
	if (board != null)
		this.commonMethod("description");
	return "Noisy is a test of the new Board.sendChat(String text) method.";
	}



public String youWon()
	{
	this.commonMethod("youWon");
	return "Burp =)";
	}

public String message( String message, Object data )
	{
	this.commonMethod("message: "+message);
	return null;
	}

protected void commonMethod(String text)
	{
	if (board == null)
		{
		System.out.println("board=null");
		return;
		}
	board.sendEmote("is going through "+text);
	board.sendEmote("is going through "+text, this);
	board.sendChat(ID+"-"+text);
	board.sendChat(ID+"-"+text, this);
	}
}	