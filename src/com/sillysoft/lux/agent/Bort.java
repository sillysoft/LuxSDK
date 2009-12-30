package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Bort.java
//	Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//


//
//  Bort expands slower than Boscoe does. 
//	He tries to do only one attack per turn.
//
//	Also when attacking for cards he will move in half the armies
//


public class Bort extends Boscoe 
{

public String name()
	{
	return "Bort";
	}

public String description()
	{
	return "Bort is slow and steady.";
	}

protected void attackFromCluster( Country root )
	{
	debug("Bort's attackFromCluster was called");
	// now run some attack methods for the cluster centered around root:
	if (root == null)
		{
		System.out.println("ERROR in Bort.attackFromCluster(). root==null 466215");
		return;
		}

	// expand once in the easyist way
	if (! attackEasyExpand(root))
			{
			attackFillOut(root);
			}

	// and consolidate our borders as much as possible
	while ( attackConsolidate(root) )
		{}

	// we will also try and get a card at the end of attackPhase if we haven't already
	}	// end of Bort.attackFromCluster


protected void setmoveInMemoryBeforeCardAttack(Country attacker)
	{
	moveInMemory = attacker.getArmies()/2;
	}

public String youWon()
	{ 
	String[] answers = new String[] { "I am Muay Thai master",
		"My legend starts now!",
		"Ancient words of wisdom\n ...'you suck'",
		"Power is nothing without skill",
		"My dad could beat you, and he's dead!",
		"For a loser, you did pretty well",
		"Meditate on your loss",
		"You fought well. I was honored" };

	return answers[ rand.nextInt(answers.length) ];
	}
}
