package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  CountryPathStack.java
//
//  Created by dustin on Thu Nov 08 2001.
//  Copyright (c) 2001 Sillysoft. All rights reserved.
//

/**

This is an implementation of a stack that stores CountryPath objects. You can add to the top, or add with a value.
If added with a (int) value then the the added item will be sorted into the correct 
place in the stack.
											<P>
You can pop from the front.

*/

public class CountryPathStack extends CountryStack
{

public void pushWithValueAndHistory( Country country, int value, int[] history)
	{
	CountryPath newElement = new CountryPath( country, value, history ); 
	
	if (start == null)
		{
		start = newElement;
		}
	else if ( value <= start.getValue() )
		{
		newElement.setNext( start );
		start = newElement;
		}
	else	
		{
		CountryPath finger = (CountryPath) start;
		
		while ( finger.getNext() != null && finger.getNext().getValue() < value ) 
			{
			finger = (CountryPath) finger.getNext();
			}
			
		// So now the new country should go after finger:
		if (finger.getNext() == null)
			{
			finger.setNext( newElement );
			}
		else
			{
			newElement.setNext( finger.getNext() );
			finger.setNext( newElement );
			}
		}
	}

public int[] topHistory()
	{
	return ( (CountryPath) start).getHistory();
	}



}
