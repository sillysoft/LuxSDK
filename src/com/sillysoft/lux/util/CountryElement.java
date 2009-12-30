package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  CountryElement.java
//
//  Created by dustin on Wed Nov 07 2001.
//  Copyright (c) 2001 Sillysoft. All rights reserved.
//


/**
A simple class for use in the stacks.
				<P>
It contains a country, a value (for use in ordering the stack),
and a reference to the next element.

*/


public class CountryElement
{

protected CountryElement next;

protected int countryCode;

protected int orderValue;

public CountryElement()
	{
	next = null;
	
	countryCode = -1;
	
	orderValue = -1;
	}

public CountryElement( Country country, int newValue)
	{
	next = null;
	
	countryCode = country.getCode();
	
	orderValue = newValue;
	}

public CountryElement getNext()
	{
	return next;
	}
	
public void setNext( CountryElement newElement )
	{
	next = newElement;
	}
	
public int getCode()
	{
	return countryCode;
	} 
	   
public int getValue()
	{
	return orderValue;
	}

public int size()
	{
	if (next == null)
		return 1;
	
	return (next.size()+1);
	}
	
}
