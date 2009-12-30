package com.sillysoft.lux.util;

import com.sillysoft.lux.*;

//
//  BoardHelper.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

/** This class is a collection of static methods for getting information out 
of a set of countries. 
<p>
The majority of the methods in this class require a final parameter of 
Country[] countries.  If the method calls for countries, do not attempt to 
pass in anything less than the entire board.  Failure to do so will result
in undefined behavior.
<p>
There are three groups of methods in this class:  Those that return 
information about the state of the game, those that return information about
the state of the board layout, and those that return paths between countries 
and/or continents.  
<p>
Have fun.
*/

public class BoardHelper {

/** Returns the country owned by <i>player</i> with the most armies on it.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are owned by <i>player</i>.  Once it has that list, it inspects each
to find out how many armies are on, and makes a note of whichever has the 
most.
<p>
To find the countries owned by <i>player</i>, it uses a PlayerIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is null.
<p>
* @param player 	the player interested in
* @param countries  the board
* @return   		A country object
* @see CountryIterator
* @see PlayerIterator
*/

public static Country getPlayersBiggestArmy( int player, Country[] countries )
	{
	CountryIterator armies = new PlayerIterator(player,countries);
	int biggestArmies = -1;
	Country root = null;
	while (armies.hasNext())
		{
		Country a = armies.next();
		if (a.getArmies() > biggestArmies)
			{
			biggestArmies = a.getArmies();
			root = a;
			}
		}
	return root;
	}

/** Same as getPlayersBiggestArmy() except it will return the biggest army that has at least one enemy neighbor. Will return null if the player has no countries. */
public static Country getPlayersBiggestArmyWithEnemyNeighbor( int player, Country[] countries )
	{
	CountryIterator armies = new PlayerIterator(player,countries);
	int biggestArmies = -1;
	Country root = null;
	while (armies.hasNext())
		{
		Country a = armies.next();
		if (a.getArmies() > biggestArmies && a.getNumberEnemyNeighbors() > 0)
			{
			biggestArmies = a.getArmies();
			root = a;
			}
		}
	return root;
	}
		
/** This method calculates the total number of armies owned by <i>player</i>.	
<p>
This method first searches through all the <i>countries</i> looking for those 
which are owned by <i>player</i>.  Once it has that list, it inspects each
to find out how many armies are on, and adds them together for a grand
total.
<p>
To find the countries owned by <i>player</i>, it simply iterates over the
entire board (ie, each Country in <i>countries</i>).  It does not use a 
PlayerIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is zero.
<p>
* @param player 	the player interested in
* @param countries  the board
* @return   		An integer
* @see Country
*/
public static int getPlayerArmies( int player, Country[] countries )
	{
	int enemies = 0;
	for (int i = 0; i< countries.length; i++)
		{
		if (countries[i].getOwner() == player)
			enemies += countries[i].getArmies();
		}
	
	return enemies;
	}
	
/** This method calculates the number of countries owned by <i>player</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are owned by <i>player</i>.  As it finds them, it increments a value,
which, when finished, is returned to the caller.
<p>
To find the countries owned by <i>player</i>, it simply iterates over the
entire board (ie, each Country in <i>countries</i>).  It does not use a 
PlayerIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is zero.
<p>
* @param player 	the player interested in
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see PlayerIterator
*/
public static int getPlayerCountries( int player, Country[] countries )
	{
	int number = 0;
	for (int i = 0; i< countries.length; i++)
		{
		if (countries[i].getOwner() == player)
			number++;
		}
	
	return number;
	}
	
/** This method calculates the number of armies owned by <i>player</i> in 
<i>continent</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it inspects each
to find out first if it is owned by <i>player</i>, and then adds the number
of armies on it to a counter if so.  The final value in the counter is
what is returned.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is zero.
<p>
In the event that <i>continent</i> is invalid, the value returned is zero. 
<p>
* @param player 	the player interested in
* @param continent  the continent interested in
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int getPlayerArmiesInContinent( 
							int player, int continent, Country[] countries )
	{
	int enemies = 0;
	CountryIterator continentE = new ContinentIterator(continent, countries);
	while (continentE.hasNext()) {
		Country c = continentE.next();
		if (c.getOwner() == player)
			enemies += c.getArmies();
		}
	
	return enemies;
	}

/** this method calculates the number of armies owned by anyone who is NOT 
<i>player</i> in <i>continent</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it inspects each
to find out first if it is owned by <i>player</i>, and then adds the number
of armies on it to a counter if <i><b>not</b></i>.  The final value in the 
counter is what is returned.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is the total number of armies on the <i>continent</i> 
regardless of which player they belong to.
<p>
In the event that <i>continent</i> is invalid, the value returned is zero. 
<p>
* @param player 	the player interested in
* @param continent  the continent interested in
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int getEnemyArmiesInContinent( 
  							int player, int continent, Country[] countries )
	{
	int enemies = 0;
	CountryIterator continentE = new ContinentIterator(continent, countries);
	while (continentE.hasNext()) {
		Country c = continentE.next();
		if (c.getOwner() != player)
			enemies += c.getArmies();
		}
	
	return enemies;
	}
	
/** Get the number of armies owned by the player that are in countries that directly adjoin the given continent. */
public static int getPlayerArmiesAdjoiningContinent(int ID, int cont, Country[] countries)
	{
	int[] borders = BoardHelper.getContinentBorders(cont, countries);
	// Make a list of the countries that must be counted - to avoid duplicate counting
	List adjoining = new ArrayList();
	for (int b = 0; b < borders.length; b++)
		{
		Country[] neighbors = countries[ borders[b] ].getAdjoiningList();
		for (int j = 0; j < neighbors.length; j++)
			if (neighbors[j].getOwner() == ID && neighbors[j].getContinent() != cont && ! adjoining.contains(neighbors[j]))
				adjoining.add(neighbors[j]);
		}
	
	int result = 0;	
	for (int b = 0; b < adjoining.size(); b++)
		result += ((Country)adjoining.get(b)).getArmies();
	return result;
	}

	

/** This method checks if <i>player</i> still owns any countries.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are owned by <i>player</i>.  If it finds one, it immediatly stops
looking and returns true.  If no country is found with <i>player</i> as
the owner, then it returns false.
<p>
To find a country owned by <i>player</i>, it simply iterates over the
entire board (ie, each Country in <i>countries</i>).  It does not use
any type of Iterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is the total number of armies on the <i>continent</i> 
regardless of which player they belong to.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is false.
<p>
This method could be used to determine if any countries are unchosen during
the initial selection process by passing in a -1 as the <i>player</i>.
The use of the resulting information is dubious, at best, and other 
routines exist that are better suited to this use.  
<p>
* @param player 	the player interested in
* @param countries  the board
* @return   		A boolean (true or false)
* @see Country
* @see BoardHelper#playerOwnsContinentCountry
* @see BoardHelper#getSmallestOpenCont
* @see BoardHelper#getSmallestEmptyCont
*/
public static boolean playerIsStillInTheGame( int player, Country[] countries )
	{
	// We cycle through the countries:	
	for (int i = 0; i < countries.length; i++)
		{
		// If we find a country that player owns, we return true:
		if (countries[i].getOwner() == player)
				return true;
		}
	// If it got to here then player doesn't own any countries (and is 
	// therefore out of the game).
	return false;
	}

/** Returns the number of continents.
<p>
This method first increments a counter, looking for a null return value
from ContinentIterator.  The value returned is the counter, less one.
<p>
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int numberOfContinents( Country[] countries )
	{
	// there are a variable number of continents, so keep looking until 
	// one doesn't exist
	boolean lastContExisted = true;		
	int i;
	for (i = 0; lastContExisted; i++) 
		{
		lastContExisted = false;
		ContinentIterator iter = new ContinentIterator( i, countries );
		if (iter.hasNext()) 
			{
			lastContExisted = true;
			}
		}
	return i-1;
	}


/** Returns the number of countries in <i>continent</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it simply 
iterates over the list and increments a counter as it does so.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>continent</i> is invalid, the value returned is zero. 
<p>
* @param continent  the continent interested in
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int getContinentSize(int continent, Country[] countries)
	{
	int count = 0;
	ContinentIterator iter = new ContinentIterator( continent, countries );
	while (iter.hasNext()) {
		iter.next();
		count++;
		}
	return count;
	}

/** Returns the code of a country inside <i>continent</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it simply 
iterates over the list and increments a counter as it does so.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>continent</i> is invalid, the value returned is zero. 
<p>
* @param continent  the continent interested in
* @param countries  the board
* @return   		An integer
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int getCountryInContinent(int continent, Country[] countries)
	{
	if (continent < 0)
		System.out.println("BoardHelper.getCountryInContinent() called with a continent code of "+continent);
		
	ContinentIterator ce = new ContinentIterator(continent, countries);
	return ce.next().getCode();
	}

	
/** This method returns an array of countryCodes of the border countries 
of <i>continent</i>. 
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it inspects each
to determine what countries are adjacent.  If any of the adjacent countries
is part of a different continent, then the root country is added to a list
of 'border' countries.  Finally, it returns the list of border countries.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>continent</i> is invalid, the value returned is a 
null list... or it throws a wobbly which is caught by the game engine, 
causing you to forfeit the remainder of the current phase.
<p>
* @param continent  the continent interested in
* @param countries  the board
* @return   		An array of integers representing a series of countries
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int[] getContinentBorders( int continent, Country[] countries )
	{
	// We need a list for temporary results:
	List tempResults = new ArrayList();
	
	// Loop through the countries in the continent:
	boolean border;
	ContinentIterator iter = new ContinentIterator( continent, countries );
	while (iter.hasNext()) {
		Country c = iter.next();
		border = false;
		Country[] neighbors = c.getAdjoiningList();
		// Now loop through the neighbors and see if any of them are in 
		// another continent
		for (int j = 0; j < neighbors.length; j++)
			{
			if ( neighbors[j].getContinent() != continent )
				{
				// If country[i] has a neighbor in another continent, then 
				// country[i] is a border country:
				border = true;
				}
			}
		
		if (border)
			tempResults.add(c);
		}
	
	// We are done. Copy the list into an array:
	int[] result = new int[tempResults.size()];
	for (int i = 0; i < result.length; i++)
		result[i] = ((Country)tempResults.get(i)).getCode();
	
	return result;
	}
	
/** This method returns an array of countryCodes of the border countries 
<i>outside</i> the requested <i>continent</i>. 
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it inspects each
to determine what countries are adjacent.  If any of the adjacent countries
is part of a different continent, then the adjacent country is added to a list
of 'border' countries.  Finally, it returns the list of border countries.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>continent</i> is invalid, the value returned is a 
null list... or it throws a wobbly which is caught by the game engine, 
causing you to forfeit the remainder of the current phase.
<p>
Warning:  This method has not been blessed.  It was snuck in by the 
documentor.
<p> 
* @param continent  the continent interested in
* @param countries  the board
* @return   		An array of integers representing a series ofcountries
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static int[] getContinentBordersBeyond( 
   									int continent, Country[] countries )
	{
	// Redirect to method that works for maps with one-way connections
	return getDefensibleBordersBeyond(continent, countries);

/**
	// We need a list for temporary results:
	List tempResults = new ArrayList();
	
	// Loop through the countries in the continent:
	ContinentIterator iter = new ContinentIterator( continent, countries );
	while (iter.hasNext()) {
		Country c = iter.next();
		Country[] neighbors = c.getAdjoiningList();
		// Now loop through the neighbors and see if any of them are in 
		// another continent
		for (int j = 0; j < neighbors.length; j++)
			{
			if ( neighbors[j].getContinent() != continent )
				{
				// If c has a neighbor in another continent, then 
				// the neighbor is a border-beyond country:
				if (!tempResults.contains(neighbors[j]))
					tempResults.add(neighbors[j]);
				}
			}
		}
	
	// We are done. Copy the list into an array:
	int[] result = new int[tempResults.size()];
	for (int i = 0; i < result.length; i++)
		result[i] = ((Country)tempResults.get(i)).getCode();
	
	return result;
**/	
	}
	
/** This method simply determines if <i>player</i> owns all the countries 
that are part of <i>continent</i>.
<p>
This method first searches through all the <i>countries</i> looking for those 
which are part of <i>continent</i>.  Once it has that list, it inspects each
to find out first if it is <i>not</i> owned by <i>player</i>.  If it finds
any that are not, it returns false.  If all the countries are successfully
checked, then it returns true.
<p>
To find the countries in the <i>continent</i>, it uses a ContinentIterator.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is the total number of armies on the <i>continent</i> 
regardless of which player they belong to.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is false, which is sane.
<p>
Warning:  In the event that <i>continent</i> is invalid, the value returned is
<i><b>true</b></i>, which is not sane.
<p>
* @param player 	the player interested in
* @param continent  the continent interested in
* @param countries  the board
* @return   		A boolean (true or false)
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static boolean playerOwnsContinent( 
							int player, int continent, Country[] countries )
	{
	// Loop through all the countries in the desired continent, checking to 
	// see if the desired player owns them. If we find a country that he 
	// does not own, then we return false:
	ContinentIterator iter = new ContinentIterator( continent, countries );
	while (iter.hasNext()) {
		if ( iter.next().getOwner() != player )
			return false;
		}
	
	// Otherwise we return true:
	return true;
	}	// End of playerOwnsContinent()


/** This method simply determines if <i>player</i> owns any full continents.
<p>
For each continent on the board, this method searches through all the 
<i>countries</i> in it, looking for any that are not owned by <i>player</i>.
If it determines that a given continent is not fully owned by <i>player</i>,
it moves on to the next.  If at any time it determines that <i>player</i>
owns a full continent, it returns true.  If it runs through all the continents
without finding one owned by <i>player</i>, it returns false.
<p>
To find the countries in the <i>continent</i>s, it uses a ContinentIterator.
It also uses this ContinentIterator to determine when there are no more
continents to be checked.
<p>
In the event that <i>player</i> is invalid, or is no longer in the game, the 
value returned is false.
<p>
* @param player 	the player interested in
* @param countries  the board
* @return   		A boolean (true or false)
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static boolean playerOwnsAnyContinent( int player, Country[] countries )
	{
	// there are a variable number of continents, so keep looking until one 
	// doesn't exist
	boolean lastContExisted = true;		
	for (int i = 0; lastContExisted; i++) {
		lastContExisted = false;
		ContinentIterator iter = new ContinentIterator( i, countries );
		if (iter.hasNext()) {
			if (playerOwnsContinent( player, i, countries )) {
				return true;
				}
			lastContExisted = true;
			}
		}
	return false;
	}

/** The same as playerOwnsAnyContinent() except it only considers continents with positive bonuses. */
public static boolean playerOwnsAnyPositiveContinent( int player, Country[] countries, Board board )
	{
	// there are a variable number of continents, so keep looking until one 
	// doesn't exist
	boolean lastContExisted = true;		
	for (int i = 0; lastContExisted; i++) {
		lastContExisted = false;
		ContinentIterator iter = new ContinentIterator( i, countries );
		if (iter.hasNext()) {
			if (board.getContinentBonus(i) > 0 && playerOwnsContinent( player, i, countries )) {
				return true;
				}
			lastContExisted = true;
			}
		}
	return false;
	}


/** Checks whether or not any player fully owns <i>continent</i>.
<p>
The first country in the <i>continent</i> is checked for its owner, and then
every additional country within the <i>continent</i> is inspected to see
if the same player owns them.  If so, true is returned.  The moment it 
finds a country that is owned by a player other than the one found in the
first country, false is returned.
<p>
To find the countries in the <i>continent</i>s, it uses a ContinentIterator.
<p>
Warning:  In the event that <i>continent</i> is invalid, the value returned is
<i><b>true</b></i>, which is not sane.
<p>
* @param continent  the continent interested in
* @param countries  the board
* @return   		A boolean (true or false)
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static boolean anyPlayerOwnsContinent( int continent, Country[] countries )
	{
	ContinentIterator iter = new ContinentIterator( continent, countries );
	int owner = iter.next().getOwner();
	while (iter.hasNext()) {
		if (iter.next().getOwner() != owner)
			return false;
		}
	return true;
	}

/** This method simply determines if <i>player</i> owns ANY of the countries
that are part of <i>continent</i>.
<p>
Each country in the <i>continent</i> is checked for its owner.  If any
country is owned by <i>player</i>, then true is returned.  If all countries
are checked and none are found to be owned by <i>player</i>, false is 
returned.
<p>
To find the countries in the <i>continent</i>s, it uses a ContinentIterator.
<p>
In the event that either <i>player</i> or <i>continent</i> is invalid, or
the <i>player</i> is no longer in the game, the value returned is
false.
<p>
* @param player 	the player interested in
* @param continent  the continent interested in
* @param countries  the board
* @return   		A boolean (true or false)
* @see Country
* @see CountryIterator
* @see ContinentIterator
*/
public static boolean playerOwnsContinentCountry( 
 							int player, int continent, Country[] countries )
	{
	// Loop through all the countries in the desired continent, checking to 
	// see if the desired player owns them. If we find a country that he does
	// own, then we return true:
	ContinentIterator iter = new ContinentIterator( continent, countries );
	while (iter.hasNext()) {
		if ( iter.next().getOwner() == player ) {
			return true;
			}
		}
	// Otherwise we return false:			
	return false;
	}	// End of playerOwnsContinentCountry()


	
/** Returns the cont-code of the smallest continent that is totally empty, or
-1 if there are no unowned conts.
<p>
This method first searches through all the <i>continents</i> and makes a
note of the number of countries in each.  The index of the one with the 
smallest number of countries, and which is totally unowned (ie, each country 
is owned by -1, the default at-map-creation-time non-player), is returned.
<p>
The method iterates over the entire board one continent at a time, checking
for 'full' ownership by player -1.  If none are found to be owned by the 
non-player, than -1 is returned. 
<p>
* @param countries  the board
* @return   		An integer
* @see Country
* @see BoardHelper#playerOwnsContinent
* @see BoardHelper#getContinentSize
*/
public static int getSmallestEmptyCont(Country[] countries)
	{
	int numContinents = BoardHelper.numberOfContinents(countries);
	// First of all we look at all the continents, and choose the smallest 
	// one that is FULLY empty
	int smallUnownedContSize = 1000000;
	int smallUnownedCont = -1;
	for (int cont = 0; cont < numContinents; cont++) {
		int size = BoardHelper.getContinentSize(cont, countries);
		if (size < smallUnownedContSize 
  				&& BoardHelper.playerOwnsContinent( -1, cont, countries ))
			{
			smallUnownedContSize = size;
			smallUnownedCont = cont;
			}
		}
	
	return smallUnownedCont;
	}

/**
This method is the same as getSmallestEmptyCont() except it only
considers continents that have bonus values of greater than zero.	*/
public static int getSmallestPositiveEmptyCont(Country[] countries, Board board)
	{
	int numContinents = BoardHelper.numberOfContinents(countries);
	// First of all we look at all the continents, and choose the smallest 
	// one that is FULLY empty
	int smallUnownedContSize = 1000000;
	int smallUnownedCont = -1;
	for (int cont = 0; cont < numContinents; cont++) {
		if (board.getContinentBonus(cont) > 0) {
			int size = BoardHelper.getContinentSize(cont, countries);
			if (size < smallUnownedContSize 
					&& BoardHelper.playerOwnsContinent( -1, cont, countries ))
				{
				smallUnownedContSize = size;
				smallUnownedCont = cont;
				}
			}
		}
	
	return smallUnownedCont;
	}
	
/** Returns the cont-code of the smallest continent that has at least one 
unowned country.
<p>
This method first searches through all the <i>continents</i> and makes a
note of the number of countries in each.  The index of the one with the 
smallest number of countries, and which has at least one country owned
by -1 (ie,the default at-map-creation-time non-player), is returned.
<p>
The method iterates over the entire board one continent at a time, checking
for 'any' ownership by player -1.  If the entire board is found to be owned 
by active players, than -1 is returned. 
<p>
* @param countries  the board
* @return   		An integer
* @see Country
* @see BoardHelper#playerOwnsContinentCountry
* @see BoardHelper#getContinentSize
*/
public static int getSmallestOpenCont(Country[] countries)
	{
	int numContinents = BoardHelper.numberOfContinents(countries);
	// First of all we look at all the continents, and choose the smallest
	// one that has at least one country empty
	int smallUnownedContSize = 1000000;
	int smallUnownedCont = -1;
	for (int cont = 0; cont < numContinents; cont++) {
		int size = BoardHelper.getContinentSize(cont, countries);
		if (size < smallUnownedContSize 
  			&& BoardHelper.playerOwnsContinentCountry( -1, cont, countries ))
			{
			smallUnownedContSize = size;
			smallUnownedCont = cont;
			}
		}
	
	return smallUnownedCont;
	}

/**
This method is the same as getSmallestOpenCont() except it only considers
continents that have a bonus of greater than zero. */
public static int getSmallestPositiveOpenCont(Country[] countries, Board board)
	{
	int numContinents = BoardHelper.numberOfContinents(countries);
	// First of all we look at all the continents, and choose the smallest
	// one that has at least one country empty
	int smallUnownedContSize = 1000000;
	int smallUnownedCont = -1;
	for (int cont = 0; cont < numContinents; cont++) {
		if (board.getContinentBonus(cont) > 0) {
			int size = BoardHelper.getContinentSize(cont, countries);
			if (size < smallUnownedContSize 
				&& BoardHelper.playerOwnsContinentCountry( -1, cont, countries ))
				{
				smallUnownedContSize = size;
				smallUnownedCont = cont;
				}
			}
		}
	
	return smallUnownedCont;
	}

/** Find the closest country to <i>CC</i> that is owned by <i>owner</i>.
<p>
This method uses a self-sorting queueing mechanism to determine the closest
country to <i>CC</i> that is owned by <i>owner</i>.  Starting with <i>CC</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on) until it
finds a path that terminates in a country owned by the given <i>owner</i>.
That Country object is returned.  The actual path information is
discarded.  To capture the path, see easyCostCountryWithOwner.
<p>
If <i>CC</i> or <i>owner</i> is invalid, null is returned.
<p>
* @param	CC 		the interesting country
* @param	owner  	the index of the interesting player
* @param	countries  the board
* @return  			A country object
* @see  	Country
* @see  	BoardHelper#easyCostCountryWithOwner
*/
public static Country closestCountryWithOwner( 
 								Country CC, int owner, Country[] countries )
	{
	int cc = CC.getCode();
	int retval = BoardHelper.closestCountryWithOwner(
 						cc, owner, countries);
	if (retval < 0)
		return null;
		
	return(countries[retval]);
	}
	
/** Find the closest country to <i>CC</i> that is owned by <i>owner</i>.
<p>
This method uses a self-sorting queueing mechanism to determine the closest
country to <i>CC</i> that is owned by <i>owner</i>.  Starting with <i>CC</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on) until it
finds a path that terminates in a country owned by the given <i>owner</i>.
The country code of that country is returned.  The actual path information is
discarded.  To capture the path, see easyCostCountryWithOwner.
<p>
If <i>CC</i> is invalid, -1 is returned.
<p>
If <i>owner</i> is invalid, -2 is returned.
<p>
* @param	CC 		the country code of the interesting country
* @param	owner  	the index of the interesting player
* @param	countries  the board
* @return  			An integer
* @see  	Country
* @see  	BoardHelper#easyCostCountryWithOwner
*/
public static int closestCountryWithOwner( 
 								int CC, int owner, Country[] countries )
	{
	if ( CC < 0 || countries.length <= CC )
		return -1;
	
	int testCode = CC;
	int distanceSoFar = 0;
	
	// We keep track of which countries we have already seen (so we don't 
	// consider the same country twice). We do it with a boolean array, with 
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
	haveSeenAlready[CC] = true;
	
	// Create a Q to store the country-codes and their distance from the 
	// start country:
	CountryStack Q = new CountryStack();
	
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries left:
	while ( true )
		{
		Country[] neighbors = countries[testCode].getAdjoiningList();
		
		for (int i = 0; i < neighbors.length; i++)
			{
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
				{
				Q.pushWithValue( neighbors[i], distanceSoFar+1 );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			return -2;
		
		distanceSoFar = Q.topValue();
		testCode = Q.pop();
		
		if ( countries[testCode].getOwner() == owner )
			return testCode;
		}
	} // End of closestCountryWithOwner
	
	/** Find the closest country to a country in <i>startingCountryList</i> that is owned by <i>owner</i>.
<p>
This method uses a self-sorting queueing mechanism to determine the closest
country to <i>CC</i> that is owned by <i>owner</i>.  Starting with the countries in <i>startingCountryList</i>,
it enqueues and follows every path outwards from their neighbors (and their
neighbors, and so on) until it
finds a path that terminates in a country owned by the given <i>owner</i>.
The country code of that country is returned.  The actual path information is
discarded.  To capture the path, see easyCostCountryWithOwner.
<p>
If <i>CC</i> is invalid, -1 is returned.
<p>
If <i>owner</i> is invalid, -2 is returned.
<p>
* @param	startingCountryList 		a List containing the counries to start searching from
* @param	owner  	the index of the interesting player
* @param	countries  the board
* @return  			An integer
* @see  	Country
* @see  	BoardHelper#easyCostCountryWithOwner
*/
public static int closestCountryWithOwner( 
 								List startingCountryList, int owner, Country[] countries )
	{
	if ( startingCountryList.size() < 1 )
		return -1;
	
	int[] startingCodes = new int[startingCountryList.size()];
	for (int i = 0; i < startingCountryList.size(); i++)
		startingCodes[i] = ((Country) startingCountryList.get(i)).getCode();
		
	// We keep track of which countries we have already seen (so we don't 
	// consider the same country twice). We do it with a boolean array, with 
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
		
	// Create a Q to store the country-codes and their distance from the 
	// start country:
	CountryStack Q = new CountryStack();

	for (int i = 0; i < startingCodes.length; i++)
		{
		haveSeenAlready[startingCodes[i]] = true;
		Q.pushWithValue( countries[startingCodes[i]], 0 );
		}
		
	int testCode = Q.pop();	
	int distanceSoFar = 0;
		
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries left:
	while ( true )
		{
		Country[] neighbors = countries[testCode].getAdjoiningList();
		
		for (int i = 0; i < neighbors.length; i++)
			{
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
				{
				Q.pushWithValue( neighbors[i], distanceSoFar+1 );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			return -2;
		
		distanceSoFar = Q.topValue();
		testCode = Q.pop();
		
		if ( countries[testCode].getOwner() == owner )
			return testCode;
		}
	} // End of closestCountryWithOwner

/** This method searches for the country owned by <i>owner</i> that has the 
easiest path to get to country <i>CC</i>.
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CC</i>, and any one that is 
owned by <i>owner</i>.  Starting with <i>CC</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in a country owned by the given 
<i>owner</i>.  
<p>
This method returns a list that contains the path. The list is simply an  
array of Country objects, each holding the next country in line. Due to the 
method of enqueueing, the zero element of the array is the country with 
<i>owner</i> that was found, while the last element of the array will be 
<i>CC</i>. 
<p>
If <i>CC</i> is owned by <i>owner</i>, an array of one length is returned,
with <i>CC</i> at index [0].
<p>
If <i>CC</i> is invalid, or if no paths are found, null is returned.
<p>
Keep in mind that if you are trying to target a given player, and you own
<i>CC</i>, this method does not take into account that you might also own
one of the intervening countries on the path returned.  Trying to attack
a country that you own is not productive.
<p>
* @param	CC 		the country code of the interesting country
* @param	owner  	the index of the interesting player
* @param	countries  the board
* @return  			An array of Country objects
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
*/
public static Country[] easyCostCountryWithOwner( 
   								Country CC, int owner, Country[] countries )
	{	
	int cc = CC.getCode();
	int[] retval = BoardHelper.easyCostCountryWithOwner(
   					cc, owner, countries);
	if (retval == null)
		return null;					
	Country[] rets = new Country[retval.length];
	for(int i = 0; i < retval.length; i++)
		{
		rets[i] = countries[retval[i]];
		}
	return(rets);
	}
	
/** This method searches for the country owned by <i>owner</i> that has the 
easiest path to get to country <i>CC</i>.
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CC</i>, and any one that is 
owned by <i>owner</i>.  Starting with <i>CC</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in a country owned by the given 
<i>owner</i>.  
<p>
This method returns a list that contains the path. The list is simply an int 
array, each holding the next country code in line. Due to the method of
enqueueing, the zero element of the array is the country with <i>owner</i> 
that was found, while the last element of the array will be <i>CC</i>. 
<p>
If <i>CC</i> is owned by <i>owner</i>, an array of one length is returned,
with <i>CC</i> at index [0].
<p>
If <i>CC</i> is invalid, or if no paths are found, null is returned.
<p>
Keep in mind that if you are trying to target a given player, and you own
<i>CC</i>, this method does not take into account that you might also own
one of the intervening countries on the path returned.  Trying to attack
a country that you own is not productive.
<p>
* @param	CC 		the country code of the interesting country
* @param	owner  	the index of the interesting player
* @param	countries  the board
* @return  			An array of integers
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
*/
public static int[] easyCostCountryWithOwner( 
   								int CC, int owner, Country[] countries )
	{	
	if ( CC < 0 || countries.length <= CC )
		{
		System.out.println("ERROR from easyCostCountryWithOwner: bad params");
		return null;
		}
	
	// First let's check to see if country CC is owned by owner:
	if ( countries[CC].getOwner() == owner )
		{
		// Then we just return a list with only <CC> in it:
		int[] result = new int[1];
		result[0] = CC;
		return result;
		}
	
	int testCode = CC;
	int armiesSoFar = 0;
	int[] testCodeHistory = new int[1];
	testCodeHistory[0] = CC;
	
	// We keep track of which countries we have already seen (so we don't
	// consider the same country twice). We do it with a boolean array, with
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
	haveSeenAlready[CC] = true;
	
	// Create a Q (with a history) to store the country-codes and their cost 
	// so far:
	CountryPathStack Q = new CountryPathStack();
	
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries in the Q:
	while ( true )
		{
		Country[] neighbors = countries[testCode].getAdjoiningList();
		
		for (int i = 0; i < neighbors.length; i++)
			{
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
				{
				// Create the new node's history array. (It is just testCode's 
				// history with its CC added at the beginning):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				newHistory[0] = neighbors[i].getCode();
				for (int j = 1; j < newHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j-1];
					}
				Q.pushWithValueAndHistory( 
					neighbors[i], 
					// If the neighbor is owned by the proper person then minus 
					// its armies from the value so if gets pulled off the Q next.
					// Without this there is a bug					
					armiesSoFar + (neighbors[i].getOwner() == owner ? -neighbors[i].getArmies() : neighbors[i].getArmies()),
					newHistory );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			{
			System.out.println("ERROR in easyCostCountryWithOwner->can't pop");
			return null;
			}
		
		armiesSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		
		if ( countries[testCode].getOwner() == owner )
			return testCodeHistory;
		}
	} // End of easyCostFromCountries

/** This method returns a Country[] containing a list from country <i>CCF</i>
to some country in <i>continent</i>, along the easiest path. 
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CCF</i>, and any one that is 
part of <i>continent</i>.  Starting with <i>CCF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in a country which is part of  
<i>continent</i>.  
<p>
This method returns a list that contains the path. The list is simply an 
array of Country objects, 
each holding the next countryin line. Due to the method of
enqueueing, the zero element of the array is the country in <i>continent</i> 
that was found, while the last element of the array (the one with the
highest value index) will be <i>CCF</i>. 
<p>
If <i>CCF</i> is part of <i>continent</i>, an array of length two is 
returned, with <i>CC</i> at index [1] and one of its neighbors at index [0].
<p>
If <i>CCF</i> is invalid, or if no paths are found, null is returned.
<p>
Keep in mind that this method will  
take into account the ownership of intervening countries on the path returned.
The path is guaranteed to detour around 'friendly' countries.
<p>
* @param	CCF		the country code of the interesting country
* @param	continent  the index of the interesting continent
* @param	countries  the board
* @return  			An array of Country objects
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
*/
public static Country[] easyCostFromCountryToContinent( 
							Country CCF, int continent, Country[] countries )
	{
	int ccf = CCF.getCode();
	int[] retval = BoardHelper.easyCostFromCountryToContinent(
   						ccf, continent, countries);
	if (retval == null)
		return null;
	Country[] rets = new Country[retval.length];
	for(int i = 0; i < retval.length; i++)
		{
		rets[i] = countries[retval[i]];
		}
	return(rets);
	}
	
/** This method returns an int[] containing a list from country <i>CCF</i>
to some country in <i>continent</i>, along the easiest path. 
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CCF</i>, and any one that is 
part of <i>continent</i>.  Starting with <i>CCF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in a country which is part of  
<i>continent</i>.  
<p>
This method returns a list that contains the path. The list is simply an int 
array, each holding the next country code in line. Due to the method of
enqueueing, the zero element of the array is the country in <i>continent</i> 
that was found, while the last element of the array (the one with the
highest value index) will be <i>CCF</i>. 
<p>
If <i>CCF</i> is part of <i>continent</i>, an array of length two is 
returned, with <i>CC</i> at index [1] and one of its neighbors at index [0].
<p>
If <i>CCF</i> is invalid, or if no paths are found, null is returned.
<p>
Keep in mind that this method will  
take into account the ownership of intervening countries on the path returned.
The path is guaranteed to detour around 'friendly' countries.
<p>
* @param	CCF		the country code of the interesting country
* @param	continent  the index of the interesting continent
* @param	countries  the board
* @return  			An array of integers
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
*/
public static int[] easyCostFromCountryToContinent( 
								int CCF, int continent, Country[] countries )
	{
	if ( CCF < 0 || countries.length <= CCF || continent < 0 )
		{
		System.out.println(
  		"ERROR in easyCostFromCountryToContinent() -> bad parameters.");
		return null;
		}
	
	int CCFOwner = countries[CCF].getOwner();
	int testCode = CCF;
	int armiesSoFar = 0;
	int[] testCodeHistory = new int[1];
	testCodeHistory[0] = CCF;

	// We keep track of which countries we have already seen (so we don't
	// consider the same country twice). We do it with a boolean array, with
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
	haveSeenAlready[CCF] = true;
	
	// Create a Q (with a history) to store the country-codes and their cost 
	// so far:
	CountryPathStack Q = new CountryPathStack();
	
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries in the Q:
	while ( true )
		{
		// Get the current countries neighbors, and cycle through them:
		Country[] neighbors = countries[testCode].getAdjoiningList();
		for (int i = 0; i < neighbors.length; i++)
			{
			// We only care about them if we haven't seen them before and if 
			// they aren't owned by CCF's owner:
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] 
 					&& neighbors[i].getOwner() != CCFOwner )
				{
				// Create the new node's history array. (It is just 
				// testCode's history with its CC added at the end):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				for (int j = 0; j < testCodeHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j];
					}
				newHistory[newHistory.length-1] = neighbors[i].getCode();
				Q.pushWithValueAndHistory( 
					neighbors[i], 
					// If the neighbor is in the proper continent
					// its armies from the value so if gets pulled off the Q next.
					// Without this there is a bug					
					armiesSoFar + (neighbors[i].getContinent() == continent ? -neighbors[i].getArmies() : neighbors[i].getArmies()),
					newHistory );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			{
			return null;
			}
		
		armiesSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		
		if ( countries[testCode].getContinent() == continent )
			return testCodeHistory;
		}
	
	}	// End of easyCostFromCountryToContinent

/** This method finds the easyest path from country <i>CCF</i> (CCFrom) to 
<i>CCT</i> (CCTo), that doesn't cross a country owned by <i>CCF</i>'s owner.
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CCF</i> and <i>CCT</i>.  
Starting with <i>CCF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in the desired country <i>CCT</i>
<p>
This method returns a list that contains the path. The list is simply an  
array of Country objects, each holding the next country in line. 
Due to the method of
enqueueing, the zero element of the array will be country <i>CCT</i> 
that was found, while the last element of the array (the one with the
highest value index) will be <i>CCF</i>. 
<p>
Keep in mind that this method will  
take into account the ownership of intervening countries on the path returned.
The path is guaranteed to detour around 'friendly' countries if possible.
<p>
If <i>CCF</i> is invalid, or if no paths are found because all paths are
blocked by 'friendly' countries, null is returned.
<p>
* @param	CCF		the country code of the from country
* @param	CCT		the country code of the to country
* @param	countries  the board
* @return  			An array of Country objects
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
* @see  	BoardHelper#easyCostFromCountryToContinent
*/
public static Country[] easyCostBetweenCountries( 
								Country CCF, Country CCT, Country[] countries )
	{
	int ccf = CCF.getCode();
	int cct = CCT.getCode();
	int[] retval = BoardHelper.easyCostBetweenCountries(
   						ccf, cct, countries);
	if (retval == null)
		return null;						
	Country[] rets = new Country[retval.length];
	for(int i = 0; i < retval.length; i++)
		{
		rets[i] = countries[retval[i]];
		}
	return(rets);
	}
   							
/** This method finds the easyest path from country <i>CCF</i> (CCFrom) to 
<i>CCT</i> (CCTo), that doesn't cross a country owned by <i>CCF</i>'s owner.
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies) between <i>CCF</i> and <i>CCT</i>.  
Starting with <i>CCF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the accumulation of enemy armies, 
until it finds a path that terminates in the desired country <i>CCT</i>
<p>
This method returns a list that contains the path. The list is simply an int 
array, each holding the next country code in line. Due to the method of
enqueueing, the zero element of the array will be country <i>CCT</i>, 
that was found, while the last element of the array (the one with the
highest value index) will be <i>CCF</i>. 
<p>
Keep in mind that this method will  
take into account the ownership of intervening countries on the path returned.
The path is guaranteed to detour around 'friendly' countries if possible.
<p>
If <i>CCF</i> is invalid, or if no paths are found because all paths are
blocked by 'friendly' countries, null is returned.
<p>
* @param	CCF		the country code of the from country
* @param	CCT		the country code of the to country
* @param	countries  the board
* @return  			An array of integers
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
* @see  	BoardHelper#easyCostFromCountryToContinent
*/
public static int[] easyCostBetweenCountries( 
   									int CCF, int CCT, Country[] countries )
	{
	if ( CCF < 0 || countries.length <= CCF 
 				|| CCT < 0 || countries.length <= CCT )
		{
		System.out.println(
  		"ERROR in easyCostBetweenCountries() -> bad parameters.");
		return null;
		}
	
	int CCFOwner = countries[CCF].getOwner();
	int testCode = CCF;
	int armiesSoFar = 0;
	int[] testCodeHistory = new int[1];
	testCodeHistory[0] = CCF;

	// We keep track of which countries we have already seen (so we don't 
	// consider the same country twice). We do it with a boolean array, with
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
	haveSeenAlready[CCF] = true;
	
	// Create a Q (with a history) to store the country-codes and their cost 
	// so far:
	CountryPathStack Q = new CountryPathStack();
	
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries in the Q:
	while ( true )
		{
		// Get the current countries neighbors, and cycle through them:
		Country[] neighbors = countries[testCode].getAdjoiningList();
		for (int i = 0; i < neighbors.length; i++)
			{
			// We only care about them if we haven't seen them before and if 
			// they aren't owned by CCF's owner:
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] 
  					&& neighbors[i].getOwner() != CCFOwner )
				{
				// Create the new node's history array. (It is just 
				// testCode's history with its CC added at the beginning):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				newHistory[0] = neighbors[i].getCode();
				for (int j = 1; j < newHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j-1];
					}
				Q.pushWithValueAndHistory( 
					neighbors[i], 
					// If the neighbor is the proper country then minus 
					// its armies from the value so if gets pulled off the Q next.
					// Without this there is a bug					
					armiesSoFar + (neighbors[i].getCode() == CCT ? -neighbors[i].getArmies() : neighbors[i].getArmies()),
					newHistory );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			{
			return null;
			}
		
		armiesSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		
		if ( testCode == CCT )
			return testCodeHistory;
		}
	} // End of easyCostBetweenCountries

/** This method finds the shortest path (in terms of countries) from 
country <i>CF</i> (CFrom) to <i>CT</i> (CTo), that ONLY goes through 
countries owned by <i>CF</i>'s owner.
<p>
This method uses a self-sorting queueing mechanism to determine the shortest
path (in terms of countries crossed) between <i>CF</i> and <i>CT</i>.  
Starting with <i>CF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the ownership of the neighbors, 
until it finds a path that terminates in the desired country <i>CT</i>
<p>
This method returns a list that contains the path. The list is simply an  
array of Country objects, each holding the next country in line.  
The zero element of the array will be country <i>CF</i>, while the last 
element of the array (the one with the highest value index) will be <i>CT</i>. 
<p>
Keep in mind that this method will take into account the ownership of 
intervening countries on the path returned.  If a valid path is found, 
the path is guaranteed to only cross 'friendly' countries.
<p>
If <i>CCF</i> is invalid, or if no paths are found because all paths are
blocked by 'enemy' countries, null is returned.
<p>
* @param	CF 		the from country
* @param	CT 		the to country
* @param	countries  the board
* @return  			An array of country objects
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
* @see  	BoardHelper#easyCostFromCountryToContinent
*/
public static Country[] friendlyPathBetweenCountries( 
							Country CF, Country CT, Country[] countries )
	{
	int ccf = CF.getCode();
	int cct = CT.getCode();
	int[] retval = BoardHelper.friendlyPathBetweenCountries(
  					ccf, cct, countries);
	if (retval == null)
		return null;
	Country[] rets = new Country[retval.length];
	for(int i = 0; i < retval.length; i++)
		{
		rets[i] = countries[retval[i]];
		}
	return(rets);
	}
	
/** This method finds the shortest path (in terms of countries) from 
country <i>CCF</i> (CCFrom) to <i>CCT</i> (CCTo), that ONLY goes through 
countries owned by <i>CCF</i>'s owner.
<p>
This method uses a self-sorting queueing mechanism to determine the shortest
path (in terms of countries crossed) between <i>CCF</i> and <i>CCT</i>.  
Starting with <i>CCF</i>,
it enqueues and follows every path outwards from its neighbors (and their
neighbors, and so on), making note of the ownership of the neighbors, 
until it finds a path that terminates in the desired country <i>CCT</i>
<p>
This method returns a list that contains the path. The list is simply an int 
array, each holding the next country code in line. The zero element of the array 
will be country <i>CCF</i>, while the last element of the array (the one with the
highest value index) will be <i>CCT</i>. 
<p>
Keep in mind that this method will  
take into account the ownership of intervening countries on the path returned.
The path is guaranteed to only cross 'friendly' countries if possible.
<p>
If <i>CCF</i> is invalid, or if no paths are found because all paths are
blocked by 'enemy' countries, null is returned.
<p>
* @param	CCF		the country code of the from country
* @param	CCT		the country code of the to country
* @param	countries  the board
* @return  			An array of integers
* @see  	Country
* @see  	BoardHelper#closestCountryWithOwner
* @see  	BoardHelper#easyCostFromCountryToContinent
*/
public static int[] friendlyPathBetweenCountries( 
 									int CCF, int CCT, Country[] countries )
	{
	if ( CCF < 0 || countries.length <= CCF 
 				|| CCT < 0 || countries.length <= CCT )
		{
		System.out.println(
   		"ERROR in friendlyPathBetweenCountries() -> bad parameters.");
		return null;
		}
	
	int CCFOwner = countries[CCF].getOwner();
	int testCode = CCF;
	int distanceSoFar = 0;
	int[] testCodeHistory = new int[1];
	testCodeHistory[0] = CCF;

	// We keep track of which countries we have already seen (so we don't 
	// consider the same country twice). We do it with a boolean array, with 
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
	haveSeenAlready[CCF] = true;
	
	// Create a Q (with a history) to store the country-codes and their cost
	// so far:
	CountryPathStack Q = new CountryPathStack();
	
	// Loop over the expand-enqueue until either the correct 
	// country is found or there are no more countries in the Q:
	while ( true )
		{
		// Get the current countries neighbors, and cycle through them:
		Country[] neighbors = countries[testCode].getAdjoiningList();
		for (int i = 0; i < neighbors.length; i++)
			{
			// We only care about them if we haven't seen them before and if they ARE owned by CCF's owner:
			if ( neighbors[i].getOwner() == CCFOwner && ! haveSeenAlready[ neighbors[i].getCode() ] )
				{
				// Create the new node's history array. (It is just testCode's history 
				// with its CC added at the end):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				for (int j = 0; j < testCodeHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j];
					}
				newHistory[newHistory.length-1] = neighbors[i].getCode();

				Q.pushWithValueAndHistory( neighbors[i], distanceSoFar+1, newHistory );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
	
	
		if ( Q.isEmpty() )
			{
			// Then there is no path. return null:
			return null;
			}
		
		distanceSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		
		if ( testCode == CCT )
			return testCodeHistory;
		}
	} // End of friendlyPathBetweenCountries
	
/****** The New-age seeker methods **********/
  							
/** This method will return an array of country-codes consisting of the
cheapest route between a country owned by <i>owner</i> and the 
<i>continent</i>. 
<p>
This method uses a self-sorting queueing mechanism to determine the easiest
path (in terms of enemy armies in the way) between a country owned by 
<i>owner</i> and a country found in <i>continent</i>.  
Starting with each country in <i>continent</i>,
it enqueues and follows every path outwards from their neighbors (and their
neighbors, and so on), making note of the ownership of the neighbors and
the number of armies,
until it finds a path that terminates in any country owned by <i>owner</i>.
<p>
This method returns a list that contains the path. The list is simply an int 
array, each holding the next country code in line. Due to the method of
enqueueing, the zero element of the array will be the country owned by
<i>owner</i> that was found, while the last element of the array (the one 
with the highest value index) will be in <i>continent</i>. 
<p>
Keep in mind that this method only takes into account the ownership of 
countries as it searches outwards, looking for any owned by <i>owner</i>. 
<p>
If <i>CCF</i> is invalid, or if no paths are found, null is returned.
<p>
* @param	owner  	the index of the interested player
* @param	continent  the index of the interested continent
* @param	countries  the board
* @return  			An array of integers
* @see  	Country
*/
public static int[] cheapestRouteFromOwnerToCont( 
  							int owner, int continent, Country[] countries )
	{	
	if ( owner < 0 || continent < 0)
		{
		System.out.println(
			"ERROR in cheapestRouteFromOwnerToCont() -> bad parameters");
		return null;
		}
		
	if (playerOwnsContinentCountry(owner, continent, countries))
		{
		// the player owns a country in the continent already. That country itself is the cheapest route
		ContinentIterator iter = new ContinentIterator( continent, countries );
		while (iter.hasNext()) {
			Country next = iter.next();
			if ( next.getOwner() == owner ) {
				return new int[] { next.getCode() } ;
				}
			}
		
		}
		
	// We keep track of which countries we have already seen (so we don't
	// consider the same country twice). We do it with a boolean array, with 
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}		
		
	// Create a Q (with a history) to store the country-codes and their cost 
	// so far:
	CountryPathStack Q = new CountryPathStack();
		
	// We explore from all the borders of <continent>
	int testCode, armiesSoFar;
	int[] testCodeHistory;
	int[] borderCodes = BoardHelper.getContinentBorders( continent, countries );
	for (int i = 0; i < borderCodes.length; i++)
		{
		testCode = borderCodes[i];
		armiesSoFar = 0;
		testCodeHistory = new int[1];
		testCodeHistory[0] = testCode;
		haveSeenAlready[testCode] = true;
		
		Q.pushWithValueAndHistory(
 					countries[borderCodes[i]], 0, testCodeHistory );
		}
	
	// So now we have all the continent borders in the Q (all with cost 0),
	// expand every possible outward path (in the order of cost).
	// eventually we should find a country owned by <owner>,
	// then we return that path's history
	while ( true )
		{
		armiesSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		
		if ( countries[testCode].getOwner() == owner )
			{
			// we have found the best path. return it
			return testCodeHistory;
			}
				
		int[] canAttackInto = getAttackList(countries[testCode], countries);

		for (int i = 0; i < canAttackInto.length; i++)
			{
			if ( ! haveSeenAlready[ canAttackInto[i] ] )
				{
				// Create the new node's history array. (It is just 
				// testCode's history with its CC added at the beginning):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				newHistory[0] = canAttackInto[i];
				for (int j = 1; j < newHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j-1];
					}
				Q.pushWithValueAndHistory( 
					countries[canAttackInto[i]], 
					// If the neighbor is owned by the proper person then minus 
					// its armies from the value so if gets pulled off the Q next.
					// Without this there is a bug					
					armiesSoFar + (countries[canAttackInto[i]].getOwner() == owner ? -countries[canAttackInto[i]].getArmies() : countries[canAttackInto[i]].getArmies()),
					newHistory );
				haveSeenAlready[ countries[canAttackInto[i]].getCode() ] = true;
				}
			}
		
		if ( Q.isEmpty() )
			{
			System.out.println(
				"ERROR in cheapestRouteFromOwnerToCont->can't pop");
			return null;
			}
		}
	} // End of cheapestRouteFromOwnerToCont

/** Create a copy of the countries array, for simulation. */ 
public static Country[] getCountriesCopy(Country[] countries) 
	{ 
		Country[] countriesCopy = new Country[countries.length]; 
		// pass 1: allocate the countries 
		for (int i = 0; i < countries.length; i++) 
		{ 
			countriesCopy[i] = new Country(i, countries[i].getContinent(), null); 
			countriesCopy[i].setArmies(countries[i].getArmies(), null); 
			countriesCopy[i].setName(countries[i].getName(), null); 
			countriesCopy[i].setOwner(countries[i].getOwner(), null); 
		} 
		// pass 2: create the AdjoiningLists 
		for (int i = 0; i < countries.length; i++) 
		{ 
			Country[] around = countries[i].getAdjoiningList(); 
			for (int j = 0; j < around.length; j++) 
				countriesCopy[i].addToAdjoiningList(countriesCopy[around[j].getCode()], null); 
		} 
		return countriesCopy; 
	}



/**  Get the defensible borders of a continent - those that can be used to attack the continent.
  The BoardHelper.getContinentBorders() gives only the borders with outgoing connections.
  With standard two-way connections maps, this will give the same result as BoardHelper.getContinentBorders()	*/
public static int[] getDefensibleBorders(int continent, Country[] countries)
    {
        // we need a Vector to store the results, since we do not know how many there will be
        Vector borders = new Vector();
        
        // for all the countries *not* in continent, can they see the continent?
        for (int c = 0; c < countries.length; c++)
            if (countries[c].getContinent() != continent)
            {
                Country[] neighbors = countries[c].getAdjoiningList();
                for (int i = 0; i < neighbors.length; i++)
                    if (neighbors[i].getContinent() == continent  &&  !borders.contains(neighbors[i]))
                        borders.add(neighbors[i]);
            }
        
        // Copy the results into an array and return it
        int[] result = new int[borders.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = ((Country)borders.get(i)).getCode();
	return result;
    }

    
/**  Get the countries beyond the defensible borders of a continent - those that can be used to attack the continent.
  With standard two-way connections maps, this will give the same result as BoardHelper.getContinentBordersBeyond().	*/
public static int[] getDefensibleBordersBeyond(int continent, Country[] countries)
    {
        // we need a Vector to store the results, since we do not know how many there will be
        Vector beyond = new Vector();
        
        // for all the countries *not* in continent, can they see the continent?
        for (int c = 0; c < countries.length; c++)
            if (countries[c].getContinent() != continent)
            {
                Country[] neighbors = countries[c].getAdjoiningList();
                for (int i = 0; i < neighbors.length; i++)
                    if (neighbors[i].getContinent() == continent  &&  !beyond.contains(countries[c]))
                        beyond.add(countries[c]);
            }
        
        // Copy the results into an array and return it
        int[] result = new int[beyond.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = ((Country)beyond.get(i)).getCode();
	return result;
    }


/**  Get the list of countries that can attack the target.
  With standard two-way connections maps, this will give the same result(CC instead of countries) as Country.getAdjoiningList() .	*/
public static int[] getAttackList(Country target, Country[] countries)
    {
	// we need a Vector to store the results, since we do not know how many there will be
	Vector attackList = new Vector();
	
	// for all the countries, can they see the target?
	for (int c = 0; c < countries.length; c++)
		if (countries[c].canGoto(target))
			attackList.add(countries[c]);
	
	// Copy the results into an array and return it
	int[] result = new int[attackList.size()];
	for (int i = 0; i < result.length; i++)
		result[i] = ((Country)attackList.get(i)).getCode();
	return result;
    }



}
