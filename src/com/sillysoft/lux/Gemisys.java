package com.sillysoft.lux;

//
//  Gemisys.java
//  Lux
//
//  This class is an implementation of the MapGenerator interface 
//  that uses an external script to do the work.
// 
//  Feel free to use this file as a basis for your own external map
//  generator.  So long as the OS you are running on supports the Process
//  class, it should work for any external program that generates .luxb
//  maps.

//  To convert to your own generator, these are the steps.
//  1) Copy this file and name it something distinctive (ex: ElectroLux.java).
//  1) Change the class name to the same as the file name (ex:
//        public class ElectroLux implements LuxMapGenerator
//  1) Modify the scriptName to point at your external script, program, 
//     exe, etc.  Whatever it is that generates the maps (ex:
//        private static final String scriptName = "householdappliance.pl";
//  1) Modify the String name() method to return your generator's name (ex:
//        public String name() { return "ElectroLux"; }
//  1) To use getChoices directly, without having to modify it, set your
//     script to return a series of available choices, one per line, when 
//     called with a single command line argument of "choices".  For example,
//     Gemisys returns (without the comment markers, obviously):

// 500x400
// 600x500
// 700x600
// 800x700
// 900x750

//     Feel free to use a different method in your own code, just make sure
//     that the changed protocol is mirrored in both sides of the call, in
//     this file and in the script, exe, etc.
//   1) Change the description()
//   1) Make sure your script can be called with exactly two arguments, 
//      the first being identical to one of your previously returned supported
//      choices, the other being a random number generator seed (which, you 
//      can of course ignore, if your generator only generates a single map).

//  modification history
//  --------------------
//  01b,07jul04,rip Gemisys port of MabilGen to 4.0 beta of lux mapgen interf.
//  01a,01apr04,rip MabilGen written to 3.98 beta of lux mapgen interface

import java.io.*;
import java.util.*;

public class Gemisys implements LuxMapGenerator
{

private static final String scriptName = "gemisys.pl";

public String name() { return "Gemisys"; }

public List getChoices()
  {
  System.out.println(name()+" getChoices() called...");
  List vChoices = new Vector();
  String[] command =
    {
    MapLoader.getMapGeneratorPath() + scriptName,
    "choices"
    };
  System.out.println(name()+" path: "+command[0]);

  try
    {
    Process pr = Runtime.getRuntime().exec( command );
    BufferedReader in =
      new BufferedReader( new InputStreamReader( pr.getInputStream() )); 
    String result;
    while ((result = in.readLine()) != null )
      {
      if( result != "")
        {
        vChoices.add( result );
        System.out.println(name()+" choice "+result+" added.");
        }
      }
    }
  catch (Exception e)
    {
    System.out.println(name()+" caught an exception: "+e);
    }

  return(vChoices);
  }
  
public float version()
  {
  return 1.0f;
  }

public String description()
  {
  return name()+" map generator brought to you by gemisys at ferkel co uk!";
  }
  
public boolean generate(PrintWriter out, String choice, int seed, 
    MapLoader loader)
  {
  try
    {
    loader.setLoadText("Invoking external script");
        
    // Invoke an OS command to run the external script.
    // The first element is the path of the script to run.
    // All other elements are the arguments sent to the script.
    String[] commands = new String[] 
      {
      MapLoader.getMapGeneratorPath() + scriptName,
      choice,
      String.valueOf(seed)
      };
      
    Process pr = Runtime.getRuntime().exec( commands );
    
    // Get a reader for the process:
    BufferedReader in = 
        new BufferedReader(new InputStreamReader(pr.getInputStream()));

    // Read in the result and send it along to the PrintWriter:
    String result;
    while ( (result = in.readLine()) != null )
      {
      out.println(result);
      }
    }
  catch (Exception e)
    {
    System.out.println(name()+" caught an exception: "+e);
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

