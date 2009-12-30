package com.sillysoft.lux;

/** A Country instance represents a single territory in the game.<P>
Each Country stores a country-code, continent-code, owner-code, number of armies, and number of fortifyable armies.  As well, each Country stores an array containing references to all adjoining Country's.
<P>
When first initialized by the game world, each Country will be given a permanent country-code, continent-code and adjoining-list. The owner-code and number of armies are set to -1.
<P>
The country-code is a unique number used to identify countries. The array returned by the Board.getCountries() will always be ordered by country-code.
*/
public class Country 
{

/**	Returns the country-code of this Country.	*/
public int getCode()
	{
	return -1;
	}
	
/**	Returns the current owner-code of this Country.	*/
public int getOwner()
	{
	return -1;
	}

/**	Returns the continent-code of this Country.	*/
public int getContinent()
	{
	return -1;
	}

/**	Returns the number of armies in this Country.	*/
public int getArmies()
	{
	return -1;
	}
	
/**	Returns the number of armies in this Country that may be fortified somewhere else. This is only garanteed to be accurate during the fortifyPhase of each Agent.	*/	
public int getMoveableArmies()
	{
	return -1;
	}
	
/**	Returns an array containing all of the Country's that are touching this Country.	*/
public Country[] getAdjoiningList()
	{
	return null;
	}
	
/** An adjacency test.	
Note that is it possible for maps to contain one-way connections. Thus a.canGoto(b) will NOT always return the same result as b.canGoto(a). */
public boolean canGoto( Country country )
	{
	return false;
	}
	
/** An adjacency test.	
Note that is it possible for maps to contain one-way connections. Thus a.canGoto(b) will NOT always return the same result as b.canGoto(a). */
public boolean canGoto( int countryCode )
	{
	return false;
	}
	
/**	Depreciated: use canGoto() instead.
This method will not behave correctly when used on a map that has one-way connections. Use the canGoto() methods instead. */
public boolean isNextTo( Country country )
	{
	return false;
	}
	
/**	Depreciated: use canGoto() instead.
This method will not behave correctly when used on a map that has one-way connections. Use the canGoto() methods instead. */
public boolean isNextTo( int countryCode )
	{
	return false;
	}

/** Returns a reference to the weakest neighbor that is owned by another player, or null if there are no enemy neighbors.*/
public Country getWeakestEnemyNeighbor() 
	{
	return null;
	}
	
/** Operates like getWeakestEnemyNeighbor but limits its search to the given continent. */
// gives the weakest enemy country in specified continent
// returns null if there are no matches
public Country getWeakestEnemyNeighborInContinent( int cont ) 
	{
	return null;
	}
	
/** Returns the number of adjacent countries. */
public int getNumberNeighbors() 
	{
	return -1;
	}

/** Returns the number of neighbor countries owned by players that don't own this Country. */
public int getNumberEnemyNeighbors() 
	{
	return -1;
	}

/** Returns the number of adjacent countries owned by <i>player</i>.*/
public int getNumberPlayerNeighbors(int player) 
	{
	return -1;
	}
	
/** Returns the number of adjacent countries not owned by <i>player</i>.*/
public int getNumberNotPlayerNeighbors(int player) 
	{
	return -1;
	}
	
/** Return the name of the country. This will only return the correct value for maps that explicitly set country names. */	
public String getName()
	{
	return null;
	}	
	
/** Returns a String representation of the country.  */
public String toString()
	{
	return null;
	}

/** Return an int array containing the country codes of all the neigbors of this country that are owned by the same player. Useful to determine where this country can fortify to. */
public int[] getFriendlyAdjoiningCodeList()
	{
	return null;
	}

/** Returns a reference to the neighbor that is owned by 'player' with the most number of armies on it. If none exist then return null. */
public Country getStrongestNeighborOwnedBy(int player) {
	return null;
	}
	

/** Returns the number of adjacent countries not owned by an agent named <i>agentName</i>.*/
public int getNumberPlayerNotNamedNeighbors(String agentName, Board board) {
	return -1;
	}	

/** Return an int array containing the country codes of all the countries that this country can attack. */
public int[] getHostileAdjoiningCodeList() {
	return null;
	}
	
/** Return an int array containing the country codes of all adjoining countries. */
public int[] getAdjoiningCodeList() {
	return null;
	}

/** Create a new country. The passkey object must be suplied to make any changes to the object. So you can only change Country objects that you creats, and not the ones the Board sends you. */
public Country(int newCountryCode, int newContCode, Object passkey)
	{}

/** Sets the continent code of the Country, as long as the passkey object is the same as supplied in the constructor. */
public void setContinentCode(int newContinentCode, Object passkey)
	{}

/** Sets the owner code of the Country, as long as the passkey object is the same as supplied in the constructor. */
public void setOwner( int newOwner, Object passkey)
	{}

/** Sets the number of armies on the Country, as long as the passkey object is the same as supplied in the constructor. */
public void setArmies( int newArmies, Object passkey)
	{}
	
/** Adds one to the army count of the the Country, as long as the passkey object is the same as supplied in the constructor. */
public void addArmy(Object passkey)
	{}


/** Add a connection from this Country object to the destinationCountry object. To be traversable both ways, the connection should be added in reverse as well. */
public void addToAdjoiningList( Country destinationCountry, Object passkey )
	{}

/** Add a 2-way connection between this Country object and the otherCountry object. */	
public void addToAdjoiningListBoth( Country otherCountry, Object passkey )
	{}

/** Set the name of the Country. */		
public void setName(String name, Object passkey)
	{}

public void clearAdjoiningList(Object passkey)
	{}
}
