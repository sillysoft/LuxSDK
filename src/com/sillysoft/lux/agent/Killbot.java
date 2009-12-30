package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Killbot.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

import java.util.Random;
import java.util.List;

public class Killbot extends Vulture
{

public Killbot()
	{
	backer = new BetterPixie();
	}

public void setPrefs( int ID, Board board )
	{
	backer.setPrefs(ID, board);
	super.setPrefs(ID, board);
	}

public void cardsPhase( Card[] cards )
	{
	backer.cardsPhase(cards);	
	}

public String name()
	{
	return "Killbot";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "Killbot is programmed to kill.";
	}

public String youWon()
	{ 
	String[] answers = new String[] {
		"Die puny humans",
		"Programmed to kill",
		"Man versus machine?\n   No contest",
		"Balls of steel",
		"Killbot Killbot Killbot!\n   A name you shall not soon forget",
		"Killbot sterilize",
		"Humans are a disease\n   Killbot is the cure",
		"Email your sorrows to\n   killbot@gmail.com",
		"First came mankind,\n   then came Killbot,\n   the end."
		};

	return answers[ rand.nextInt(answers.length) ];
	}

}
