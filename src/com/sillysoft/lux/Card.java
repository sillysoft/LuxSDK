package com.sillysoft.lux;

import java.util.List;
import java.util.ArrayList;

/** The Card class provides a data structure for representing cards, as well as some methods for checking sets.*/
public class Card
{
// The country code of the country that is on this card:
int countryCode;

// The symbol on the card (we just assign the three different 
// pictures to be 0 for horses, 1 for cannons, 2 for men, and 3 
// for wildcard (on which there will be a country code of -1):
int symbol;

public Card( int assocCode, int symbol )
	{
	this.symbol = symbol;
	if ( symbol == 3 )
		{
		countryCode = -1;
		}
	else
		{
		countryCode = assocCode;
		}
	}

/** Returns the country code associated with this card. It will return -1 if the card is wild. */
public int getCode()
	{
	return countryCode;
	}

/** Returns the symbol on the card. Normal symbols are 0,1,2 while wildcards are 3.*/
public int getSymbol()
	{
	return symbol;
	}


/** A test of whether these three cards are a cashable set. */
public static boolean isASet( Card card1, Card card2, Card card3 )
	{
	if (card1 == null || card2 == null || card3 == null)
		return false;

	// Cycle through the cards and count the number of each symbol:
	int symbolCount[] = new int[4];
	symbolCount[ card1.getSymbol() ]++;
	symbolCount[ card2.getSymbol() ]++;
	symbolCount[ card3.getSymbol() ]++;

	if ( symbolCount[0] + symbolCount[3] == 3 || symbolCount[1] + symbolCount[3] == 3 || symbolCount[2] + symbolCount[3] == 3 )
		return true;

	if ( ( Math.min( symbolCount[0], 1 ) + Math.min( symbolCount[1], 1 ) + Math.min( symbolCount[2], 1 ) + symbolCount[3] ) > 2 )
		return true;

	// Otherwise, they have no set:
	return false;
	}
/** A test of whether the array of cards contains at least one cashable set. */
public static boolean containsASet( Card[] cards )
	{
	if (cards == null || cards.length < 3)
		return false;
	// A player has a match if he has either three cards with the same symbol, or one of each symbol.
	// Wildcard's symbol is 3, it can be anything.

	// Cycle through the cards and count the number of each symbol:
	int symbolCount[] = new int[4];
	for (int i = 0; i < cards.length; i++)
		{
		symbolCount[ cards[i].getSymbol() ]++;
		}

	// See if they have three of a kind (3 counts for anything):
	for (int i = 0; i < 3; i++)
		{
		if ( symbolCount[i] + symbolCount[3] > 2 )
			{
			return true;
			}
		}

	// Now see if they have one of each (3 counts for anything):
	if ( ( Math.min( symbolCount[0], 1 ) + Math.min( symbolCount[1], 1 ) + Math.min( symbolCount[2], 1 ) + symbolCount[3] ) > 2 )
		{
		return true;
		}

	// Otherwise, we have no set:
	return false;
	}

/** This returns a size-3 Card array, containing a random set from amongst the given array.
*The return value is undefined if there is no set. Use containsASet() to check first. */
public static Card[] getRandomSet( Card[] cards )
	{
	Card[] toReturn = new Card[3];

	// Cycle through the cards and count the number of each symbol:
	int symbolCount[] = new int[4];
	for (int i = 0; i < cards.length; i++)
		{
		symbolCount[ cards[i].getSymbol() ]++;
		}

	// See if they have three of a kind (3 counts for anything):
	for (int i = 0; i < 3; i++)
		{
		if ( symbolCount[i] + symbolCount[3] > 2 )
			{
			int setCounter = 0;
			// Then they have a set with symbol i, let's get it:
			for (int j = 0; setCounter != 3 && j < cards.length; j++)
				{
				if ( cards[j].getSymbol() == i || cards[j].getSymbol() == 3 )
					{
					toReturn[ setCounter ] = cards[j];
					setCounter++;
					}
				}
			return toReturn;
			}
		}

	// OK, so they don't have three symbols of a kind. that means that they must have one of each.
	// Let's find them:
	for (int j = 0; j < cards.length; j++)
		{
		if ( toReturn[0] == null && cards[j].getSymbol() == 0 )
			{
			toReturn[0] = cards[j];
			}
		else if ( toReturn[1] == null && cards[j].getSymbol() == 1 )
			{
			toReturn[1] = cards[j];
			}
		else if ( toReturn[2] == null && cards[j].getSymbol() == 2 )
			{
			toReturn[2] = cards[j];
			}
		else if ( cards[j].getSymbol() == 3 )
			{
			if ( symbolCount[0] == 0 && toReturn[0] == null )
				{
				toReturn[0] = cards[j];
				}
			else if ( symbolCount[1] == 0 && toReturn[1] == null )
				{
				toReturn[1] = cards[j];
				}
			else if ( symbolCount[2] == 0 && toReturn[2] == null )
				{
				toReturn[2] = cards[j];
				}
			}
		}
	return toReturn;
	}


/** This returns a size-3 Card array, containing a set from amongst the given array that uses as many cards owned by <i>player</i> as possible. Returns null if no sets are found.	*/
public static Card[] getBestSet( Card[] cards, int player, Country[] countries )
    {	
	// First look for a set using non wildcards:
	List noWildList = new ArrayList();
	List wildList = new ArrayList();
	for (int i = 0; i < cards.length; i++)
		{
		if (cards[i].getSymbol() != 3)
			noWildList.add(cards[i]);
		else
			wildList.add(cards[i]);
		}
	if (cards.length > noWildList.size())
		{
		// Then there were some wildcards. Look for a set amongst the non-wildcards:
		Card[] noWildCards = new Card[noWildList.size()];
		for (int i = 0; i < noWildList.size(); i++)
			noWildCards[i] = (Card) noWildList.get(i);
			
		Card[] bestNoWildSet = Card.getBestSet(noWildCards, player, countries);
		if (bestNoWildSet != null)
			{
			// We found a set using no wildcards, return it:
			return bestNoWildSet;
			}
			
		// No set was found using none of our wildcards. try with only one if we have multiple wildcards:
		if (cards.length > noWildList.size() + 1)
			{
			Card[] oneWildCardList = new Card[noWildList.size() + 1];
			for (int i = 0; i < noWildList.size(); i++)
				oneWildCardList[i] = (Card) noWildList.get(i);
			oneWildCardList[oneWildCardList.length-1] = (Card) wildList.get(0);
			
			Card[] bestOneWildSet = Card.getBestSet(oneWildCardList, player, countries);
			if (bestOneWildSet != null)
				{
				// We found a set using no wildcards, return it:
				return bestOneWildSet;
				}			
			}
		}
		
	// our attempts at finding a set with no wildcards have failed (or we have no wildcards).
			
    // populate an array with the ownercodes of the cards
    int[] owners = new int[cards.length];
    int owned = 0;

    for (int i = 0; i < cards.length; i++)
        {
		if (cards[i].getCode() == -1)
			owners[i] = -1;
		else
			{
			owners[i] = countries[cards[i].getCode()].getOwner();
			if (owners[i] == player)
				owned++;
			}
        }

    // start by getting an array with all the owned cards
    Card[] ownedCards = new Card[owned];
    int count = 0;
    
    for (int i = 0; i < cards.length; i++)
        {
        if (owners[i] == player)
            {
            ownedCards[count] = cards[i];
            count++;
            }
        }

    if (owned > 2)
        {
        if (Card.containsASet( ownedCards ))
            {
            return Card.getRandomSet( ownedCards );
            }
        }

    // OK, they have no sets made up of three of their cards.
    // look for sets with two now
    if (owned > 1)
        {
        Card[] ownedPlus = new Card[owned+1];
        for (int i = 0; i < owned; i++)
            ownedPlus[i] = ownedCards[i];
 
        // now loop over all the non-owned cards looking for a set using only one of them
        for (int c = 0; c < cards.length; c++)
            {
            if (owners[c] != player)
                {
                ownedPlus[owned] = cards[c];
                if (Card.containsASet( ownedPlus ))
                    {
                    return Card.getRandomSet( ownedPlus );
                    }
                }
            }
        }

    // ok, there is no set using two of the player's cards
    if (owned > 0)
        {
        Card[] ownedPlusPlus = new Card[owned+2];
        for (int i = 0; i < owned; i++)
            ownedPlusPlus[i] = ownedCards[i];
 
        for (int c = 0; c < cards.length; c++)
        for (int d = 0; d < cards.length; d++)
            {
            if (owners[c] != player && owners[d] != player && c != d)
                {
                ownedPlusPlus[owned] = cards[c];
                ownedPlusPlus[owned+1] = cards[d];
                if (Card.containsASet( ownedPlusPlus ))
                    {
                    return Card.getRandomSet( ownedPlusPlus );
                    }
                }
            }
        }

	// Check for any set at all
	if (Card.containsASet(cards))
		return Card.getRandomSet(cards);

    return null;
    }

/** The way this method is implemented all wildcards are equal to each other!!! */
public boolean equals(Object other)
	{
	if (other instanceof Card)
		{
		Card card = (Card) other;
		return (card.countryCode == this.countryCode && card.symbol == this.symbol);
		}

	return false;
	}

/** Gives a String representation of the card.*/
public String toString()
	{
	return "<Card symbol=\""+symbol+"\" country=\""+countryCode+"\">";
	}

}