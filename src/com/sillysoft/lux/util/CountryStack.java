package com.sillysoft.lux.util;

import com.sillysoft.lux.Country;

//
//  CountryStack.java
//
//  Created by dustin on Wed Nov 07 2001.
//  Copyright (c) 2001 Sillysoft. All rights reserved.

/**

This is an implementation of a stack. You can add to the top, or add with a value.
If added with a (int) value then the the added item will be sorted into the correct 
place in the stack.
											<P>
It can use CountryElement as its member type. There is a CountryPathStack subclass that extends it to allow each unit to be a list of Country's.
												<P>
You can pop from the front.
*/

public class CountryStack 
{
protected CountryElement start;

public CountryStack()
	{
	start = null;
	}
	
/** Add a Country to the top of the stack.  */
public void push( Country country )
	{
	this.pushWithValue( country, 0 );
	}

/** Add a Country into the stack. It will be ordered according to its value, lowest first.  */
public void pushWithValue( Country country, int value)
	{
	CountryElement newElement = new CountryElement( country, value ); 
	
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
		CountryElement finger = start;
		
		while ( finger.getNext() != null && finger.getNext().getValue() < value ) 
			{
			finger = finger.getNext();
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
	
/** Get the country-code from the top of the stack. */
public int pop()	
	{
	int temp = -1;
	
	if (start == null)
		{
		System.out.println("Error in CountryStack.pop()  The stack is empty, you can't pop!");
		}
	else
		{
		temp = start.getCode();
		start = start.getNext();
		}
	
	return temp;
	}

public int topValue()
	{
	return start.getValue();
	}

public boolean isEmpty()
	{
	return ( start == null );
	}

public int size()
	{
	if (start == null)
		return 0;
	
	return start.size();
	}
	

} // End of class
