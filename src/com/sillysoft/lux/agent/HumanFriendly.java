package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Cluster.java
//	Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

// HumanFriendly is like Cluster, but he will never attack countries owned by Human's

import java.util.List;
import java.util.ArrayList;

public class HumanFriendly extends Cluster 
{
public String name()
	{
	return "HumanFriendly";
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "HumanFriendly likes is a radical bot. He likes Humans.";
	}

protected void attackFromCluster( Country root )
	{
	// now run some attack methods for the cluster centered around root:
	if (root != null)
		{
		// expand as long as possible the easyist ways
		while ( attackEasyExpand(root) )	{}

		attackFillOut(root);

		while ( attackEasyExpand(root) )	{}

		while ( attackConsolidate(root) )
			{}

		while ( attackSplitUp(root, 1.2f) )
			{}
		}
	}



// If any of our border countries around <root>'s cluster have only one enemy then attack it 
// (if they have some chance of winning)
// return true if we won at least one attack 
protected boolean attackEasyExpand(Country root)
	{
	// get the borders of the cluster centered on <root>:
	CountryIterator borders = new ClusterBorderIterator( root );

	boolean wonAttack = false;
	while (borders.hasNext()) {
		Country border = borders.next();

		CountryIterator neighbors = new NeighborIterator(border);
		int enemies = 0;
		Country enemy = null;
		while (neighbors.hasNext()) {
			Country neighbor = neighbors.next();
			if (! board.getAgentName(neighbor.getOwner()).equals( "Human" )) {
				enemies++;
				enemy = neighbor;
				}
			}
		if (enemies == 1 && border.getArmies() > enemy.getArmies()) {
			// then we will attack that one country and move everything in, thus expanding our borders.
			moveInMemory = 1000000;
			if (board.attack( border, enemy, true) > 0)
				wonAttack = true;
			moveInMemory = -1;
			}
		}
	return wonAttack;
	}


// Attack any countries next to <root>'s cluster that has zero not-owned-by-us neighbors
// This kills little islands to fill out our territory.
// return true if we won at least one attack 
protected boolean attackFillOut(Country root )
	{
	boolean wonAttack = false;
	CountryIterator borders = new ClusterBorderIterator( root );
	while (borders.hasNext()) {
		Country border = borders.next();

		CountryIterator neighbors = new NeighborIterator(border);
		while (neighbors.hasNext()) {
			Country neighbor = neighbors.next();
			if (! board.getAgentName(neighbor.getOwner()).equals( "Human" ) && neighbor.getNumberPlayerNotNamedNeighbors("Human", board) == 0) {
				// attack it
				if (neighbor.getArmies() < border.getArmies()) {
					moveInMemory = 0; // since we are attacking from a border we remember to move zero armies in
					if (board.attack( border, neighbor, true) == 7)
						wonAttack = true;
					moveInMemory = -1;
					}
				}
			}
		}
	return wonAttack;
	}

// If we can, we consolidate our borders, by attacking from two or more borderCountries into a common enemy
// return true if we won at least one attack
protected boolean attackConsolidate( Country root)
	{
	CountryIterator borders = new ClusterBorderIterator( root );
	boolean wonAttack = false;

	while (borders.hasNext())
		{
		Country border = borders.next();

		CountryIterator neighbors = new NeighborIterator(border);
		int enemies = 0;
		Country enemy = null;
		while (neighbors.hasNext())
			{
			Country neighbor = neighbors.next();
			if (! board.getAgentName(neighbor.getOwner()).equals( "Human" ))
				{
				enemies++;
				enemy = neighbor;
				}
			}
		if (enemies == 1 && enemy.getNumberPlayerNeighbors(ID) > 1)
			{
			// then this enemy could be a good point to consolidate.
			// look for other border countries to consolidate into enemy...
			List ours = new ArrayList(); // this will store all the countries that will participate in the attack.
			CountryIterator possibles = new NeighborIterator(enemy);
			while (possibles.hasNext() )
				{
				Country poss = possibles.next();
				if (poss.getOwner()==ID && poss.getNumberPlayerNotNamedNeighbors("Human", board)==1)
					{
					// then <poss> will join in the merge into <enemy>
					ours.add(poss);
					}
				}
			// now we attack if the odds are good.
			int ourArmies = 0;
			for (int i = 0; i < ours.size(); i++)
				ourArmies += ((Country)ours.get(i)).getArmies();
			if (ourArmies > enemy.getArmies())
				{
				// AAaaaaaaaaaeeeeeeeeeiiiiiiiii! Attack him from all our countries
				for (int i = 0; i < ours.size() && enemy.getOwner() != ID; i++)
					{
					if (((Country)ours.get(i)).getArmies() > 1)
						{
						moveInMemory = 1000000;
						if (board.attack( (Country)ours.get(i), enemy, true) > 0)
							wonAttack = true;
						}
					}
				moveInMemory = -1;
				}
			}
		}
	return wonAttack;
	}

// for each border of <root>'s cluster, we split up our border country into its ememy countries.
// but only when (our armies) > (enemy armies)*attackRatio.
// An attack ratio of 1.0 is when we at least tie them
// return true if we won at least one attack 
protected boolean attackSplitUp( Country root, float attackRatio )
	{
	/**** STAGE 4 ATTACK ****/
	// Now the third stage. If it leeds to a good chance of more land, we split our borders into two or more armie groups.
	CountryIterator borders = new ClusterBorderIterator(root );
	boolean wonAttack = false;

	while (borders.hasNext()) {
		Country border = borders.next();

		CountryIterator neighbors = new NeighborIterator(border);
		int enemies = 0;
		int enemiesArmies = 0;
		Country enemy = null;
		while (neighbors.hasNext()) {
			Country neighbor = neighbors.next();
			if (! board.getAgentName(neighbor.getOwner()).equals( "Human" )) {
				enemies++;
				enemiesArmies += neighbor.getArmies();
				enemy = neighbor;
				}
			}

		// We only perform this operation when we far outnumber them.
		if (border.getArmies() > enemiesArmies*attackRatio) {
			int armiesPer = border.getArmies()/Math.max(enemies,1); // use the max function to avoid divide by zero
			// then we will attack from this border to all of its enemy neighbors.
			neighbors = new NeighborIterator(border);
			while (neighbors.hasNext() && border.getArmies() > 1) {
				Country neighbor = neighbors.next();
				if (! board.getAgentName(neighbor.getOwner()).equals( "Human" )) { // then we kill this enemy with 1/<enemies>
					moveInMemory = armiesPer;
					if (board.attack( border, neighbor, true) > 0)
						wonAttack = true;
					moveInMemory = -1;
					// xxagentxx: if we lose lots of armies in the first attacks, the last attacks might not happen because we are out of armies. This is a bug, but not very serious.
					wonAttack = true;
					}
				}
			}
		}
	return wonAttack;
	}



}	// End of Cluster class
