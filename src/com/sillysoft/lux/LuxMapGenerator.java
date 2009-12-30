package com.sillysoft.lux;

//
//  LuxMapGenerator.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//

/**
The LuxMapGenerator interface allows you to create MapGenerators and have Lux load and run them.
*/

public interface LuxMapGenerator 
{

/** A name for your generator. This must NOT change from run to run. 	*/
public String name();

/** The version of your generator.  */
public float version();

/** A description of your generator.  */
public String description();

/**
Return a List of Strings giving the different map styles this generator can do. 
Example:				<P>
<pre>
public List getChoices()
	{
	if (choices == null)
		{
		choices = new Vector();
		choices.add("tiny");
		choices.add("small");
		choices.add("medium");
		choices.add("large");
		choices.add("huge");
		}
	return choices;
	}	</pre>
 * @see List
 */
public java.util.List getChoices();


/**
Generate a map of the given 'choice' and output it to the given PrintWriter.

Use <i>choice</i> as the style. The <i>seed</i> is be a unique 
number that identifies the map. Use it to seed your random number generator.
The map file should be printed onto the given PrintWriter. 
Most likely using one of the methods:				<P>&nbsp;&nbsp;&nbsp;

		void print(String s)		<br>&nbsp;&nbsp;&nbsp;
		void println(String s)		<P>

The given <i>MapLoader</i> has two methods of interest.          		<P>&nbsp;&nbsp;&nbsp;

		void setLoadText(String text)   - will display words where the board will go when not using a cached-map. <P>
		static String getMapGeneratorPath() - can be used to obtain the folder path of where MapGenerators are stored on the local filesystem.
<P>
This method should return true on success. If an error ocurs then false should be returned and a 
message to stdout should be printed.
 * @see PrintWriter
 * @see MapLoader
*/
public boolean generate(java.io.PrintWriter out, String choice, int seed, MapLoader loader);


/**
Is Lux allowed to create and cache maps from this generator.

This should always be 'true' when distributing a generator since it speeds up the experience of the user. This method is provided so that developers can turn off caching while creating their LuxMapGenerator in order to make testing easier. When created, cached files can be found in Boards/Random/ of your support folder. */
public boolean canCache();

/** This method is not currently used. If something new is needed in the future it will be done using this function, while still letting old MapGenerators be compatible.	*/ 
public String message( String message, Object data );

}
