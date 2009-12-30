package com.sillysoft.lux.util;

import com.sillysoft.lux.*;
import java.util.List;
import java.util.ArrayList;

//
//  CountryRoute.java
//  Lux
//
//  Created by Dustin Sacks on 1/12/05.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

/**

CountryRoute is a data structure that stores an ordered path of Country's. 

*/

public class CountryRoute 
{

private List route;

public CountryRoute(List countries)
	{
	route = countries;
	}

public CountryRoute(Country[] countryArray)
	{
	route = new ArrayList();
	for (int i = 0; i < countryArray.length; i++)
		route.add(countryArray[i]);
	}

public CountryRoute(int[] codeArray, Country[] countries)
	{
	route = new ArrayList();
	for (int i = 0; i < codeArray.length; i++)
		route.add(countries[codeArray[i]]);
	}

public Country start()
	{
	return (Country) route.get(0);
	}

public Country end()
	{
	return (Country) route.get(route.size()-1);
	}

/** The number of Country's in this route. */
public int size()
	{
	return route.size();
	}

/** Get a Country out of the route. */
public Country get(int index)
	{
	return (Country) route.get(index);
	}

/** Return true if this route contains the given object (which should be a Country). */
public boolean contains(Object o)
	{
	return route.contains(o);
	}

/** Get the total count of armies along this route. */
public int getArmies()
	{
	int result = 0;
	for (int i = 0; i < route.size(); i++)
		result += ((Country)route.get(i)).getArmies();

	return result;
	}

// How many armies not owned by the player are on this route?
public int costNotCountingPlayer(int player)
	{
	int result = 0;
	for (int i = 0; i < route.size(); i++)
		if (((Country)route.get(i)).getOwner() != player)
			result += ((Country)route.get(i)).getArmies();

	return result;
	}

// How many armies not owned by the players are on this route?
public int costNotCountingPlayer(int player, int player2)
	{
	int result = 0;
	for (int i = 0; i < route.size(); i++)
		if (((Country)route.get(i)).getOwner() != player && ((Country)route.get(i)).getOwner() != player2 )
			result += ((Country)route.get(i)).getArmies();

	return result;
	}

/** Append the given CountryRoute onto the end of this one and return the result. 

Currently this is done in a very simple manner, there may end up being duplicates.
*/
public CountryRoute append(CountryRoute other)
	{
	// Start by copying everything in this route until we hit the other one
	List result = new ArrayList();
	Country otherRouteEntry = null;
	for (int i = 0; i < route.size(); i++)
		{
		if (other.contains(route.get(i)))
			{
			otherRouteEntry = get(i);
			break;	// exit the for loop
			}
		else
			result.add(route.get(i));
		}

	if (end().equals(otherRouteEntry))
		{
		// the end of this route is where we first hit the other one.
		// no problem - just copy the other one.
		for (int i = 0; i < other.route.size(); i++)
			result.add(other.route.get(i));
		return new CountryRoute(result);
		}

	// Otherwise we hit the other route before we finished going through this route.
	// They must be merged somehow
	if (other.end().equals(otherRouteEntry))
		{
		// just reverse it
		for (int i = other.route.size()-1; i >= 0; i--)
			result.add(other.route.get(i));
		return new CountryRoute(result);
		}

	// We did not hit the other route at its start or end
	// Try and make a new route from where we intersect.
	// Back up a country and check all of its neighbors inside the other route, since there may be more then one
	CountryCluster otherCluster = new CountryCluster(other.route);
	Country lastNonDupe = (Country) result.get(result.size() - 1);
	Country[] adjoining = lastNonDupe.getAdjoiningList();
	for (int i = 0; i < adjoining.length; i++)
		{
		if (other.contains(adjoining[i]))
			{
			CountryRoute newRoute = otherCluster.getSimpleRouteStartingAt(adjoining[i]);
			if (newRoute != null)
				{
				// we found a route starting at a country we can intersect
				for (int j = 0; j < newRoute.size(); j++)
					result.add(newRoute.route.get(j));
				return new CountryRoute(result);
				}
			}
		}

	// So it looks like we have a complex route.
	// What should we do ? XXXX
	System.out.println("CountryRoute.append() could not complete its task. Returning a partly garbage route.");
	// Take over as many countries as possible I suppose
	// This will not return a valid route
	for (int i = 0; i < other.route.size(); i++)
		{
		if (! result.contains(other.route.get(i)))
			result.add(other.route.get(i));
		}

	return new CountryRoute(result);
	}

/** Return a route that is the same as this one but in the reverse order. */
public CountryRoute reverse()
	{
	List result = new ArrayList();
	for (int i = 0; i < route.size(); i++)
		result.add(0, route.get(i));

	return new CountryRoute(result);
	}

public String toString()
	{
	StringBuffer buffer = new StringBuffer("\n\t<CountryRoute size:"+route.size()+" route:\n\t");
	for (int i = 0; i < route.size(); i++)
		{
		buffer.append(route.get(i));
		if (i != route.size()-1)
			buffer.append("\n\t");
		}
	buffer.append(" end route>");
	return buffer.toString();
	}

}