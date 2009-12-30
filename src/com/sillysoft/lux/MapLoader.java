package com.sillysoft.lux;

//
//  MapLoader.java
//  Lux
//
//	This is a stub file. You will need it to compile your LuxMapGenerator class.
//
//	Put it in the same folder as your class and type 'javac *.java'. 
//	That command will execute the java compiler and produce .class files.
//

public class MapLoader 
{

/**
An optional method for informing the user during lengthy operations. 
If it takes a while to generate your map then please call this to give the user some feedback.	*/
public void setLoadText(String text)
	{	
	}
	
	
/**
This method gives you the location that LuxMapGenerators are stored.

This method can be useful if you are using an external script to do your work. 
It will return the path to the MapGenerator folder. This can be radically 
different on different systems. On MacOSX it might be		<br>&nbsp;&nbsp;
	'/Users/dustin/Library/Application Support/Lux/MapGenerators/'. 		<br>
On windows it could be 										<br>&nbsp;&nbsp;
	'C:\Program Files\Lux\Support\MapGenerators\'.		*/
public static String getMapGeneratorPath()
	{
	return null;
	}

/** Is the player-info area being displayed inside the map window? The default Lux setting is true.		*/
public boolean isPlayerInfoInsideMap()
	{
	return true;
	}
	
/**
Will return the numeric version number of the running Lux.		*/
public static float getLuxVersion()
	{
	return 0;
	}
}
