package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Boscoe.java
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
//  Boscoe is Yakool with a slowed down attack strategy
//	(he only does tripleAttackPack and card-getting attacks)
//

public class Boscoe extends Yakool 
{

public String name()
	{
	return "Boscoe";
	}

public String description()
	{
	return "Boscoe really likes Brie cheese.";
	}

public void cardsPhase( Card[] cards )
	{
	super.cardsPhase(cards);
	cashCardsIfPossible(cards);
	}

protected void attackFromCluster( Country root )
	{
	debug("Boscoe's attackFromCluster was called");
	// now run some attack methods for the cluster centered around root:
	if (root == null)
		{
		System.out.println("ERROR in Boscoe.attackFromCluster(). root==null 466215");
		return;
		}

	// expand as long as possible the easyist ways
	while ( tripleAttackPack(root) )	{}
	}	// end of Boscoe.attackFromCluster


protected void setmoveInMemoryBeforeCardAttack(Country attacker)
	{
	moveInMemory = 0;
	}


public String youWon()
	{ 
	String[] answers = new String[] { "Just another average day for Boscoe",
	"I'm a hard workin dude",
	"I've got big plans in mind now",
	"I dabbled in communism once, \nbut I got over it",
	"I'm a staunch union boy",
	"Time for everyone to learn oldspeak" };

	return answers[ rand.nextInt(answers.length) ];
	}
}
