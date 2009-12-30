package com.sillysoft.lux;

import com.sillysoft.lux.agent.LuxAgent;

/** The Board is the connection between the game world and agents. 
* Commands such as choosing countries or attacking are all ordered through calls to the Board
* during the proper turn phase. The Board also contains some methods for querying the game state
* and parameters. 

* When initialized, each agent is sent a reference to a Board object to interact with.

An important data structure available through the Board is the array of Country's in the game. Available through the getCountries() method, this array contains all aspects of the current board position.	*/

public class Board 
{

/** Cashes in the given card set. Each parameter must be a reference to a different Card instance sent via cardsPhase(). 
It returns true if the set was cashed, false otherwise. */
public boolean cashCards( Card card, Card card2, Card card3 )
	{
	return false;
	}
	
/** Places numberOfArmies armies in the given country. */
public void placeArmies( int numberOfArmies, Country country )
	{}

/** Places numberOfArmies armies in the given country. */
public void placeArmies( int numberOfArmies, int countryCode )
	{}
			
/** If <i>attackTillDead</i> is true then perform attacks until one side or the other has been defeated. 
Otherwise perform a single attack.<P>
This method may only be called from within an agent's attackPhase() method. */	
public int attack( Country attacker, Country defender, boolean attackTillDead)
	{
	return -1;
	}
	
/** If <i>attackTillDead</i> is true then perform attacks until one side or the other has been defeated.
Otherwise perform a single attack.<P>
This method may only be called from within an agent's attackPhase() method. */	

// this is the one that actually calls world.attack()
public int attack( int countryCodeAttacker, int countryCodeDefender, boolean attackTillDead)
	{
	return -1;
	}

/** Order a fortification move.	
This method may only be called from within an agent's 'fortifyPhase()' method. It returns 1 on a successful fortify, 0 if no armies could be fortified (countries must always keep 1 army on them) and a negative number on failure.	*/
public int fortifyArmies( int numberOfArmies, Country origin, Country destination)
	{
	return -1;
	}
	
/** Order a fortification move.	
This method may only be called from within an agent's 'fortifyPhase()' method. It returns 1 on a successful fortify, 0 if no armies could be fortified (countries must always keep 1 army on them) and a negative number on failure.	*/
public int fortifyArmies( int numberOfArmies, int countryCodeOrigin, int countryCodeDestination)
	{
	return -1;
	}

// Info methods ****

//These methods are provided for the agents to get information about the game.
/** Will return an array of all the countries in the game. The array is ordered by country code, so c[i].getCode() equals i.	*/
public Country[] getCountries()
	{
	return null;
	}
	
/** Returns the number of countries in the game.	*/	
public int getNumberOfCountries()
	{
	return -1;
	}
	
/** Returns the number of continents in the game.	*/	
public int getNumberOfContinents()
	{
	return -1;
	}

/** Returns the number of bonus armies given for owning the specified continent.	*/	
public int getContinentBonus( int cont )
	{
	return -1;
	}

/** Returns the name of the specified continent (or null if the map did not give one).	*/	
public String getContinentName( int cont )
	{
	return null;
	}
	
/** Returns the number of players that started in the game.	*/	
public int getNumberOfPlayers()
	{
	return -1;
	}
	
/** Returns the number of players that are still own at least one country.	*/	
public int getNumberOfPlayersLeft()
	{
	return -1;
	}
	
/** Returns the current income of the specified player.	*/	
public int getPlayerIncome(int player)
	{
	return -1;
	}
	
/** Returns the TextField specified name of the given player.	*/	
public String getPlayerName(int player)
	{
	return null;
	}

/** Returns whatever the name() method of the of the given agent returns.	*/
public String getAgentName(int player)
	{
	return null;
	}

/** Returns the number of cards that the specified player has.	*/	
public int getPlayerCards(int player)
	{
	return -1;
	}
	
/** Returns the number of armies given by the next card cash.	*/	
public int getNextCardSetValue()
	{
	return -1;
	}

/** Returns true if the current player has taken over a country this turn. False otherwise. */
public boolean tookOverACountry()
	{
	return false;
	}
	
/** Returns whether or not cards are on in the preferences.	*/
public boolean useCards()
	{
	return false;
	}
	

/** Returns whether or not cards get transferred when a player is killed.	*/
public boolean transferCards()
	{
	return false;
	}
	
/** Returns whether or not cards are immediately cashed when taking over a player and ending up with 5 or more cards.	*/
public boolean immediateCash()
	{
	return false;
	}	
	
/** Gives a String representation of the board. */
public String toString()
	{
	return null;
	}
		

/** Send a chat using the agent type (ie Cluster) as the name. This will only work in network games. */		
public boolean sendChat(String message)
	{	return false;	}

/** Send a chat using the agent name (ie dustin) as the name. This will only work in network games. */
public boolean sendChat(String message, LuxAgent sender)
	{	return false;	}

/** Send an emote using the agent type (ie Cluster) as the name. This will only work in network games. */
public boolean sendEmote(String message)
	{	return false;	}
	
/** Send an emote using the agent name (ie dustin) as the name. This will only work in network games. */
public boolean sendEmote(String message, LuxAgent sender)
	{	return false;	}

/** Play the audio file at the specified URL. Will return true if played or false if not. Users can turn off Agent sounds in their preferences. In network games this method */
public boolean playAudioAtURL(String audioURL)
	{
	return false;
	}
	
/** Retrieve a string from persistant storage based on the given key. If no value is found in storage then the defaultValue parameter will be returned. */
public String storageGet(String key, String defaultValue)
	{
	return null;
	}
	
/** Retrieve a boolean from persistant storage based on the given key. If no value is found in storage then the defaultValue parameter will be returned. */			
public boolean storageGetBoolean(String key, boolean defaultValue) 
	{
	return false;
	}
	
/** Retrieve an int from persistant storage based on the given key. If no value is found in storage then the defaultValue parameter will be returned. */
public int storageGetInt(String key, int defaultValue) 
	{
	return -1;
	}

/** Retrieve a float from persistant storage based on the given key. If no value is found in storage then the defaultValue parameter will be returned. */
public float storageGetFloat(String key, float defaultValue) 
	{
	return 0f;
	}
	
/** Save a string associated with the given key into persistant storage. */	
public void storagePut(String key, String value) 
	{}
	
/** Save a boolean associated with the given key into persistant storage. */	
public void storagePutBoolean(String key, boolean value) 
	{}
	
/** Save an int associated with the given key into persistant storage. */	
public void storagePutInt(String key, int value) 
	{}
	
/** Save a float associated with the given key into persistant storage. */	
public void storagePutFloat(String key, float value) 
	{}
	
/** Remove any value from persistant storage associated with the given key. */	
public void storageRemoveKey(String key)
	{}

/** Return a LuxAgent instance of the specified type (ie Angry, Trotsky, etc). Will return null if the desired agent type could not be loaded. */
public LuxAgent getAgentInstance(String agentType)
	{
	return null;
	}

/** Return a string representation the card progression for this game. If cards are not being used then it will return "0". */
public String getCardProgression()
	{
	return null;
	}
	
/** Return the percent increase of the continents. */
public int getContinentIncrease()
	{
	return 0;
	}

/** Return the number of seconds left in this turn. */
public int getTurnSecondsLeft()
	{
	return 0;
	}
	
/** Return the count of the turn rounds for the game */
public int getTurnCount()
	{
	return 0;
	}

/** Get the local path to the Agents folder, where any resource files you need can be stored. */
public static String getAgentPath()
	{
	return null;
	}
				
} // End of the class
