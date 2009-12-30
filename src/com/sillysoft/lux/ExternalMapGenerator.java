package com.sillysoft.lux;

//
//  ExternalMapGenerator.java
//  Lux
//
//  This class is a implementation of the MapGenerator interface 
//	that uses an external script to do the work.
//

import java.io.*;
import java.util.*;

public class ExternalMapGenerator implements LuxMapGenerator
{

private static final String externalScriptName = "luxmap.pl";

public String name()
	{
	return "ExternalMapGenerator";
	}

public List getChoices()
	{
	List choices = new Vector();
	choices.add("ExternalMap");
	return choices;
	}

public float version()
	{
	return 1.0f;
	}

public String description()
	{
	return "The ExternalMapGenerator uses the perl script 'luxmap.pl' to do the work of generating the map.";
	}
	
public boolean generate(PrintWriter out, String choice, int seed, MapLoader loader)
	{
	try
        {
        loader.setLoadText("Invoking external script");
        
        // Invoke an OS command to run the external script.
        // The first element is the path of the script to run.
        // All other elements are the arguments sent to the script.
		String[] commands = new String[] {
			MapLoader.getMapGeneratorPath() + externalScriptName,
			choice,
			String.valueOf(seed)
				};
			
		Process pr = Runtime.getRuntime().exec( commands );
		
		// Get a reader for the process:
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));

		// Read in the result and send it along to the PrintWriter:
		String result;
		while ( (result = in.readLine()) != null )
			{
			out.println(result);
			}
		}
	catch (Exception e)
		{
		System.out.println("ExternalMapGenerator caught an exception: "+e);
		return false;
		}

	
	return true;
	}

public boolean canCache()
	{
	return true;
	}

public String message( String message, Object data )
	{
	return null;
	}

}
