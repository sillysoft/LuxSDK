package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  CountryPath.java
//
//  Created by dustin on Thu Nov 08 2001.
//  

/**

This class represents a path of connected countries. It is used to store paths through the graph of countries.

*/

public class CountryPath extends CountryElement
{

private int[] history;


public CountryPath( Country country, int newValue, int[] newHistory)
	{
	next = null;
	
	countryCode = country.getCode();
	
	orderValue = newValue;
	
	history = newHistory;
	}
	
			
public int[] getHistory()
	{
	return history;
	} 


	   
}
