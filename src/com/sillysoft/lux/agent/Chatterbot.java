package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Chatterbot.java
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

public class Chatterbot extends Vulture
{

public Chatterbot()
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
	board.sendChat("test1", this);
	board.sendChat("test2");
	board.sendEmote("test3", this);
	board.sendEmote("test4");
	
	backer.cardsPhase(cards);	
	}

public String name()
	{
	return "Chatterbot";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "Chatterbot is programmed to kill.";
	}

public String youWon()
	{ 
	String[] answers = new String[] {
		"Die puny humans",
		"Programmed to kill",
		"Man versus machine?\n   No contest",
		"Balls of steel",
		"Chatterbot Chatterbot Chatterbot!\n   A name you shall not soon forget",
		"Chatterbot sterilize",
		"Humans are a disease\n   Chatterbot is the cure",
		"Email your sorrows to\n   killbot@gmail.com",
		"First came mankind,\n   then came Chatterbot,\n   the end."
		};

	return answers[ rand.nextInt(answers.length) ];
	}

}
