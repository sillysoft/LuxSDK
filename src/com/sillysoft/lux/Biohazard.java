///////////////////////////////////////////////////////
//  Biohazard.java
//  Lux Map Generator
//
//  Biohazard takes an existing map 
//  file and randomly selects countries that are 
//  deemed hazardous. These countries are removed 
//  from the map and marked with a biohazard icon. 
//  The map size determines the amount of wasteland
//  generated, but does not change the original map
//  size in any way. Version 1.0 supports Classic 
//  and Deux. You must have the original map file to
//  use the Biohazard version.
//
//  Version: 1.0 (5/8/07)
//  By: David Sant (RandomGuy)
///////////////////////////////////////////////////////

package com.sillysoft.lux;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.BoardHelper;
import com.sillysoft.lux.util.ContinentIterator;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.imageio.*;
import java.awt.image.*;
 
public class Biohazard implements LuxMapGenerator, ImageObserver  {

  //random number generator
  Random rand;
  MapLoader m;
  private String boardSize;   // a value from the choices array
  int boardSeed; // seed number used in file names

  //lux map variables
  String version;
  String width;
  String height;
  String theme;
  String author;
  String email;
  String webpage;
  String title;
  String description;
  String playerinfo;
  String shortContLabels;
          
  //vectors to hold repeated map variables
  private int numCountries;
  private Country[] countries;
  private Vector deadCountries;
  private Vector newCountryIDs;
  private Vector origCountryIDs;
  private Vector connections;
  private Vector countryPolygons;
  private Vector countryNames;
  private Vector countryArmyLocations;
  private Vector contNames;
  private Vector contBonus;
  private Vector contColor;
  private Vector contLabelLoc;
  private Vector lines;
  private Vector players;
  private Vector initialOwners;
  private Vector initialArmies;
  
  boolean success = false;

  String mapFile;
 
  int deadCount; //number of dead countries to aim for
  double deadPercent; //percentage of countries to wipe out
      
  //theme:
  BufferedImage background;
  BufferedImage foreground;
  BufferedImage overground;
  Graphics2D backG, foreG, overG;
  
  //the game calls this when it wants a new map
  //write a map xml to "out"
  public boolean generate(PrintWriter out, String choice, int seed, MapLoader loader) {
  
    debug("SEED = " + seed);
    
    //seed must completely determine our map
    rand = new Random(seed);
    m = loader;
    boardSize = choice;
    boardSeed = seed;

    //initialize vars
    countryPolygons = new Vector();
    connections = new Vector();
    deadCountries = new Vector();
    newCountryIDs = new Vector();
    origCountryIDs = new Vector();
    countryNames = new Vector();
    countryArmyLocations = new Vector();
    contNames = new Vector();
    contBonus = new Vector();
    contColor = new Vector();
    lines = new Vector();
    contLabelLoc = new Vector();
    initialOwners = new Vector();
    initialArmies = new Vector();
    
    InitMapChoice(boardSize);
    
    debug ("mapFile= " + mapFile);
    debug ("deadPercent= " + deadPercent);
    
    //how many countries are in this map?
    numCountries = GetMapSize(mapFile);
    debug ("numCountries= " + numCountries);
    
    //determine the number of countries to waste
    deadCount = Math.round((float)(numCountries * deadPercent));
    debug ("deadCount= " + deadCount);
    
    //minimum of 1 wasteland country
    if (deadCount==0) {
        deadCount = 1;
    }
    
    //init country array
    countries = new Country[numCountries];
    
    //init vectors because we may get the countries out of order (they are grouped by cont)
    //and this ensure there is a space relating to the countryID for each value
    for (int i = 0; i < numCountries; i++) {
           countryPolygons.add ("");
           connections.add  ("");
           newCountryIDs.add  ("");
           countryNames.add  ("");
           countryArmyLocations.add  (""); 
           initialOwners.add  (""); 
           initialArmies.add  (""); 
    }

    //read map into variables
    ReadXMLMap (mapFile);
    
    //try to get deadcount # of hazardous countries
    for (int iDeadLoop = 0; iDeadLoop < deadCount; iDeadLoop++) {
        //try 5 times to get a valid dead country before skipping
        success = false;

        for (int iTry = 0; iTry < 5 && success == false; iTry++) {
            //randomly pick a country
            int deadTestID = rand.nextInt(numCountries);

            //if it is already dead, pick again
            if (deadCountries.contains(deadTestID)) {
                success = false; //try again, this one is already dead               
            } else {
                //see if the map is valid with this country dead
                if (TestForValidMap(deadTestID)) {
                    //if so, add it and move on
                    success = true;
                    deadCountries.add(deadTestID);
                 } else {
                    //invalid map, try again
                    success = false;
                 }
            }
        }
     }

    //write new map file
    WriteMap(out);

    //create and write out the theme
	if (! mapFile.startsWith("Spicy Deux"))
		doTheme();

    return true;
  }
  
  private void InitMapChoice(String choice) {
      //determine map file and percentage from choice
    if(choice.equals(CHOICE_CLASSIC_L)) {
      mapFile = "Classic.luxb";
      deadPercent = .025;
    } else if(choice.equals(CHOICE_CLASSIC_M)) {
      mapFile = "Classic.luxb";
      deadPercent = .05;
    } else if(choice.equals(CHOICE_CLASSIC_H)) {
      mapFile = "Classic.luxb";
      deadPercent = .1;
    } else if(choice.equals(CHOICE_CLASSIC_E)) {
      mapFile = "Classic.luxb";
      deadPercent = .2;
    }  else if(choice.equals(CHOICE_DEUX_L)) {
      mapFile = "Spicy Deux.luxb";
      deadPercent = .025;
    }  else if(choice.equals(CHOICE_DEUX_M)) {
      mapFile = "Spicy Deux.luxb";
      deadPercent = .05;
    } else if (choice.equals(CHOICE_DEUX_H)) {
      mapFile = "Spicy Deux.luxb";
      deadPercent = .1;
    } else if(choice.equals(CHOICE_DEUX_E)) {
      mapFile = "Spicy Deux.luxb";
      deadPercent = .2;
    } else if (choice.equals(CHOICE_ARMSRACE_L)) {
      mapFile = "Arms Race.luxb";
      deadPercent = .05;
    }
  }

  //this function reads a luxb file and determines the number of countries within it.
  //we do this first because our variable loading routines need to know the number of countries
  private int GetMapSize(String mapName) {
    
    int countryCount = 0;
      
        //try to get the file path
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapName;
     
	  Document doc = null;
	  File mapFile = new File(LuxbPath);
	  if (mapFile.exists() || LuxbPath.endsWith("Spicy Deux.luxb")) {
		try	{
			doc = parseLuxMap (LuxbPath);		
		  } catch(Exception e) {
			e.printStackTrace();
		  }
		}
	  else {
	    String shortMapName = mapName;
		if (shortMapName.endsWith(".luxb"))
			shortMapName = shortMapName.substring(0, shortMapName.length()-5);
		throw new RuntimeException("Could not find the map file. Please install the map '"+shortMapName+"' from the plugin manager and try again.");
	  }
          
      //add up the number of countries in each continent
      NodeList nodeList = doc.getElementsByTagName("continent");
      for (int iCont = 0; iCont < nodeList.getLength(); iCont++) {
          Element contNode = (Element) nodeList.item(iCont);
          NodeList countryList = contNode.getElementsByTagName("country");
          countryCount = countryCount + countryList.getLength();
      }

    return countryCount;
  }
  
 //reads a map file to determine how many players are in the starting scenario
 private int HowManyPlayersInMap() {
          
    try {
        //try to get the file path
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapFile;
     
      Document doc = parseLuxMap (LuxbPath);      
          
      players = new Vector();
      
      //for each country, add the player ID to our player vector (if new player)
      NodeList nodeList = doc.getElementsByTagName("continent");
      for (int iCont = 0; iCont < nodeList.getLength(); iCont++) {
          Element contNode = (Element) nodeList.item(iCont);
          NodeList countryList = contNode.getElementsByTagName("country");
          for (int iCountry=0; iCountry < countryList.getLength(); iCountry++) {
               Element countryNode = (Element) countryList.item(iCountry);
              
              NodeList countryChildNode = countryNode.getElementsByTagName("initialOwner");
              Element luxbNode = (Element) countryChildNode.item(0);
              Integer playerID = Integer.parseInt(getCharacterDataFromElement(luxbNode));

              if (!(players.contains(playerID))) {
                  players.add(playerID);
              }
          }
      }
      
    } catch(Exception e) {
      e.printStackTrace();
    }

    //return how many players we found
    return players.size();
 }
  
  //this function loads all the variables with the ones from our map
  private void ReadXMLMap(String mapName) {
      
    try {
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapName;
     
      debug("Path= " + LuxbPath);
      
        Document doc = parseLuxMap(LuxbPath);
           
//<?xml version="1.0" encoding="UTF-8"?>
//<luxboard> 

    //<version>1.7</version>
    NodeList nodeList = doc.getElementsByTagName("version");
    Element luxbNode = (Element) nodeList.item(0);
    version = getCharacterDataFromElement(luxbNode);
      
    //<width>799</width>
    nodeList = doc.getElementsByTagName("width");
    luxbNode = (Element) nodeList.item(0);
    width = getCharacterDataFromElement(luxbNode);

    //<height>511</height>
    nodeList = doc.getElementsByTagName("height");
    luxbNode = (Element) nodeList.item(0);
    height = getCharacterDataFromElement(luxbNode);

    //<theme>Ocean</theme>
    nodeList = doc.getElementsByTagName("theme");
    luxbNode = (Element) nodeList.item(0);
    theme = getCharacterDataFromElement(luxbNode);

    //<author>AJ Bertenshaw</author>
    nodeList = doc.getElementsByTagName("author");
    luxbNode = (Element) nodeList.item(0);
    author = getCharacterDataFromElement(luxbNode);

    //<email>aj@bertenshaw.net</email>
    nodeList = doc.getElementsByTagName("email");
    luxbNode = (Element) nodeList.item(0);
    email = getCharacterDataFromElement(luxbNode);
      
    //<webpage>http://Bertenshaw.Net/fun/lux/</webpage>
    nodeList = doc.getElementsByTagName("webpage");
    luxbNode = (Element) nodeList.item(0);
    webpage = getCharacterDataFromElement(luxbNode);

    //<title>Classic Risk</title>
    nodeList = doc.getElementsByTagName("title");
    luxbNode = (Element) nodeList.item(0);
    title = getCharacterDataFromElement(luxbNode);

    //<description>The original map layout from the boardgame Risk. This map is included with Lux.</description>
    nodeList = doc.getElementsByTagName("description");
    luxbNode = (Element) nodeList.item(0);
    description = getCharacterDataFromElement(luxbNode);
      
    //<playerInfoLocation>341,-1</playerInfoLocation>
    nodeList = doc.getElementsByTagName("playerInfoLocation");
    luxbNode = (Element) nodeList.item(0);
    playerinfo = getCharacterDataFromElement(luxbNode);
    
    //<playerInfoLocation>341,-1</playerInfoLocation>
    nodeList = doc.getElementsByTagName("shortContinentLabels");
    luxbNode = (Element) nodeList.item(0);
    shortContLabels = getCharacterDataFromElement(luxbNode);
    
//<continent>
//	<continentname>Australia</continentname>
//	<bonus>2</bonus>
//	<color>0.8/0.0/0.8</color>
//	<country>
//      ...
//	</country>    
//</continent>
        
        //read existing map file and store values into our country collection
        nodeList = doc.getElementsByTagName("continent");
        for (int iCont = 0; iCont < nodeList.getLength(); iCont++) {
          Element contNode = (Element) nodeList.item(iCont);

          NodeList contChildNode = contNode.getElementsByTagName("continentname");
          luxbNode = (Element) contChildNode.item(0);
          String contName = getCharacterDataFromElement(luxbNode);
          
          contChildNode = contNode.getElementsByTagName("bonus");
          luxbNode = (Element) contChildNode.item(0);
          String bonus = getCharacterDataFromElement(luxbNode);
 
          contChildNode = contNode.getElementsByTagName("color");
          luxbNode = (Element) contChildNode.item(0);
          String color = getCharacterDataFromElement(luxbNode);

          //label location
          contChildNode = contNode.getElementsByTagName("labellocation");
          luxbNode = (Element) contChildNode.item(0);
          String contLabel = getCharacterDataFromElement(luxbNode);

          //store continent values
          contNames.add(contName);
          contBonus.add(bonus);
          contColor.add(color);
          contLabelLoc.add(contLabel);
 
//		<id>0</id>
//		<name>Eastern Australia</name>
//		<initialOwner>3</initialOwner>
//		<initialArmies>4</initialArmies>
//		<adjoining>1,2</adjoining>
//		<armylocation>693,107</armylocation>
//		<polygon>699,84 687,85 682,93 676,95 672,101 666,118 673,123 676,144 686,139 690,158 695,147 698,135 712,120 713,107 705,89</polygon>

          NodeList countryNodes = contNode.getElementsByTagName("country");
          for (int iCountry = 0; iCountry < countryNodes.getLength(); iCountry++) {
              Element countryNode = (Element) countryNodes.item(iCountry);

              NodeList countryChildNode = countryNode.getElementsByTagName("id");
              luxbNode = (Element) countryChildNode.item(0);
              Integer countryId = Integer.parseInt(getCharacterDataFromElement(luxbNode));
              
              countryChildNode = countryNode.getElementsByTagName("name");
              luxbNode = (Element) countryChildNode.item(0);
              String countryName = getCharacterDataFromElement(luxbNode);

              countryChildNode = countryNode.getElementsByTagName("adjoining");
              luxbNode = (Element) countryChildNode.item(0);
              String adjoining = getCharacterDataFromElement(luxbNode);

              countryChildNode = countryNode.getElementsByTagName("armylocation");
              luxbNode = (Element) countryChildNode.item(0);
              String armylocation = getCharacterDataFromElement(luxbNode);

              countryChildNode = countryNode.getElementsByTagName("initialOwner");
              luxbNode = (Element) countryChildNode.item(0);
              String initOwner = getCharacterDataFromElement(luxbNode);

              countryChildNode = countryNode.getElementsByTagName("initialArmies");
              luxbNode = (Element) countryChildNode.item(0);
              String initArmy = getCharacterDataFromElement(luxbNode);              
              
              //if no army specified, set it to 1 (the default)
              if (initArmy == "") {
                  initArmy = "1";
              }
                            
              //create country
              countries[countryId] = new Country(countryId, iCont, this);
                                    
              //init polygon vector because a country can have multiple
              countryPolygons.set(countryId, new Vector());
              
              //save country name and army location
              countryNames.set (countryId, countryName);
              countryArmyLocations.set (countryId, armylocation);
              initialOwners.set (countryId, initOwner);
              initialArmies.set (countryId, initArmy);
                           
              //save the polygon tags for each country
              countryChildNode = countryNode.getElementsByTagName("polygon");
              for (int iPolygons = 0; iPolygons < countryChildNode.getLength(); iPolygons++) {
                  luxbNode = (Element) countryChildNode.item(iPolygons);
                  String polygon = getCharacterDataFromElement(luxbNode);

                  ((Vector)countryPolygons.get(countryId)).add(polygon);
              }

              //create a vector for storing adjoining countries individually
              Vector adjList = new Vector();
              String[] adjListArray = adjoining.split(",");
              for (int iAdj = 0; iAdj < adjListArray.length; iAdj++) {
                  adjList.add(adjListArray[iAdj]);
              }

              //save connection vector
              connections.set(countryId, adjList);             
          }
       }

       //set up connections, once all countries exist
       for (int iOrig = 0; iOrig < numCountries; iOrig++) {
            for (int iConn = 0; iConn < ((Vector)connections.get(iOrig)).size(); iConn++) {
                Vector connect = (Vector)connections.get(iOrig);
                
                //get country object for adjoining country
                int countryIndex = Integer.parseInt(connect.get(iConn).toString());
                Country toJoin = countries[countryIndex];
               
                //add connection
                countries[iOrig].addToAdjoiningList(toJoin, this);
            }            
       }

        //<line><position>463,123 481,123</position></line>
        //<line><position>236,386 266,407</position><color>1.0/0.6/0.0</color><width>3</width></line>

        //read lines from map file
       nodeList = doc.getElementsByTagName("line");
       for (int iLine = 0; iLine < nodeList.getLength(); iLine++) {
          Element lineNode = (Element) nodeList.item(iLine);
         
          NodeList lineChildNode = lineNode.getElementsByTagName("position");
          luxbNode = (Element) lineChildNode.item(0);
          String position = getCharacterDataFromElement(luxbNode);

          lineChildNode = lineNode.getElementsByTagName("color");
          luxbNode = (Element) lineChildNode.item(0);
          String color = getCharacterDataFromElement(luxbNode);

          lineChildNode = lineNode.getElementsByTagName("width");
          luxbNode = (Element) lineChildNode.item(0);
          String linewidth = getCharacterDataFromElement(luxbNode);

          //just store as one string to rewrite later
          String sLine = "";
          sLine = "<position>" + position + "</position>";
          if (color != "") {
              sLine = sLine + "<color>" + color + "</color>";
          }
          if (linewidth != "") {
              sLine = sLine + "<width>" + linewidth + "</width>";
          }
          lines.add(sLine);
       }
        
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  //this function tests whether a map is valid with a specific country missing.
  //it generates a new set of countries, skipping any previous dead country or the
  //new one. It then sets the connections based on the new mapped IDs
  private boolean TestForValidMap(int deadCountryID) {
      //number of countries in the new map
      int newMapCount = numCountries - deadCountries.size()-1;
      
      //init a test map array
      Country[] testMap = new Country[newMapCount];

       int iCurrent = 0;
       int contID;
       
       //create countries() with non-dead countries from original map
       for (int iOrig = 0; iOrig < numCountries; iOrig++) {
           if (iOrig == deadCountryID || deadCountries.contains(iOrig)) {
               //this country is skipped in our test world
               newCountryIDs.set(iOrig, -1);
           } else {
               newCountryIDs.set(iOrig, iCurrent);
               
               contID = countries[iOrig].getContinent();
               
               testMap[iCurrent] = new Country(iCurrent, contID, this);

               iCurrent++;
           }
       }
       
       //do connections
       //for each country on the original board
       for (int iOrig = 0; iOrig < numCountries; iOrig++) {
           //get the ID on our new board
           int testMapID = (Integer)(newCountryIDs.get(iOrig));
           
           //if not <0 (meaning it is not wasteland) then...
           if (testMapID > -1) {
               //get list of adjoining country codes from original map
               int[] joiningList = countries[iOrig].getAdjoiningCodeList();
                
                //for each connection that the original country had
               for (int iConn = 0; iConn < joiningList.length; iConn++) {
                   //get the ID of the same country on our map
                    int toJoinID = (Integer)(newCountryIDs.get(joiningList[iConn]));
                    
                    //if the country we are joining to is not wasteland
                    if (toJoinID > -1) { 
                        //create country object
                        Country toJoin = testMap[toJoinID];
               
                        //add to test map connection
                        testMap[testMapID].addToAdjoiningList(toJoin, this);
                    }
                }
           }
       }
       
       //now that the map is built, see if it is valid and return the value
       if (IsValidMap(testMap)) {
           return true;
       } else {
           return false;
       }
  }
  
  //this function takes a map, in the form of a country array and attempts to determine if it is playable.
  //it does this by looking for any country that can connect to every other one. theoretically, a person who controls that 
  //country has the ability to reach everyone else and win the game.
  private boolean IsValidMap(Country[] thisMap) {
     //find one country that can touch every other
      //for each country
      for (int i = 0; i < thisMap.length; i++) {
          boolean cantConnect = false;
          //check every other country
          for (int iCheck = 0; iCheck < thisMap.length && cantConnect == false; iCheck++) {
              try {
              //use friendly path because everyone has the same "owner" - if returns null, then no path was found
              //only check for countries different than the one we are starting from
              if (BoardHelper.friendlyPathBetweenCountries(i, iCheck, thisMap) == null && iCheck != i) {
                  cantConnect = true;
              }
              }catch (Exception e) {
                  //if a country is removed that is the sole connection for another country, the friendly path
                  //returns a null pointer (a country has no adjoining) so this catches that and returns that we
                  //cant connect from there
                  debug(e.toString());
                  debug("i: "+i);
                  debug("iCheck: "+iCheck);
                  cantConnect = true;
              }
          }
          if (cantConnect == false) {
              return true;
          }
      }
      return false;
  }
  
  //function to read characters from an XML element
  public static String getCharacterDataFromElement(Element e) {
   if (e != null) {
     Node child = e.getFirstChild();
     if (child instanceof CharacterData) {
        CharacterData cd = (CharacterData) child;
        return cd.getData();
     }
   }
   return "";
 }
  
  //this function writes our new mapfile to the print writer
  private void WriteMap(PrintWriter out) {
      //final count of countries
       int newMapCount = numCountries - deadCountries.size();
       
       //init map
       Country[] testMap = new Country[newMapCount];

       int iCurrent = 0;
       int contID;
       
       //create countries() with non-dead countries from original map
       for (int iOrig = 0; iOrig < numCountries; iOrig++) {
           if (deadCountries.contains(iOrig)) {
               //this country is skipped in our test world
               newCountryIDs.set(iOrig, -1);
           } else {
               newCountryIDs.set(iOrig, iCurrent);
               origCountryIDs.add (iCurrent, iOrig);
               
               contID = countries[iOrig].getContinent();
               
               testMap[iCurrent] = new Country(iCurrent, contID, this);
               testMap[iCurrent].setName (countries[iOrig].getName(),this);
                         
               iCurrent++;
           }
       }
       
       //do connections
       //for each country on the original board
       for (int iOrig = 0; iOrig < numCountries; iOrig++) {
           //get the ID on our new board
           int testMapID = (Integer)(newCountryIDs.get(iOrig));
           
           //if not <0 (meaning it is not wasteland) then...
           if (testMapID > -1) {
               //get list of adjoining country codes from original map
               int[] joiningList = countries[iOrig].getAdjoiningCodeList();
                
                //for each connection that the original country had
               for (int iConn = 0; iConn < joiningList.length; iConn++) {
                   //get the ID of the same country on our map
                    int toJoinID = (Integer)(newCountryIDs.get(joiningList[iConn]));
                    
                    //if the country we are joining to is not wasteland
                    if (toJoinID > -1) { 
                        //create country object
                        Country toJoin = testMap[toJoinID];
               
                        //add to test map
                        testMap[testMapID].addToAdjoiningList(toJoin, this);
                    }
                }
           }
       }
       
       //we built our test map, so now start writing the file
       
       //first write the map info. the theme is dynamic based on the original map's theme and the seed
           out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              ("Biohazard Deux".equals(theme) ? "<theme>" + theme + "</theme>\n" : "<theme>Biohazard_" + theme + boardSeed + "</theme>\n") +
              "<author>Biohazard by RandomGuy</author>\n" +
              "<email></email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + title + " - Biohazard</title>\n" +
              "<description>Biohazard map generator modified map. Original map description: " + description + " Type: " + boardSize + "seed: " + boardSeed + "</description>\n");

           //player info location
           out.write("<playerInfoLocation>" +  playerinfo + "</playerInfoLocation>");
           
           //short continent labels
           if (shortContLabels != "") {
               out.write("<shortContinentLabels>" +  shortContLabels + "</shortContinentLabels>");
           }
          
           //for each continent in original file
           for (int i=0; i<contNames.size(); i++) {
              //if we still have countries in the continent in our map
              if (BoardHelper.getContinentSize(i, testMap) > 0) {
                  //write the continent
                   out.write("<continent>\n" +
                     	"  <continentname>" + contNames.get(i) + "</continentname>\n" +
                        "  <bonus>" + contBonus.get(i) + "</bonus>\n");

                   //cont color
                   if (contColor.get(i) != "") {
                       out.write("  <color>" + contColor.get(i) + "</color>\n");
                   }
                   
                   //label location
                   if (contLabelLoc.get(i) != "") {
                       out.write("  <labellocation>" + contLabelLoc.get(i) + "</labellocation>\n");
                   }
                   
                   //now iterate through each country in the continent
                   ContinentIterator contIt = new ContinentIterator(i, testMap);
                  
                   //we know we have one country
                   do  {
                       Country ctry = contIt.next();

                       //get the country code, and determine the original country id to serve as an index to our vectors
                      int newCode = ctry.getCode();
                      int origCode = (Integer)origCountryIDs.get(newCode);
                       
                      debug("Country: " + countryNames.get(origCode).toString());
                      debug("Code: " + ctry.getCode());
                      debug("initialOwner: " + initialOwners.get(origCode).toString());
                      debug("initialArmies: " + initialArmies.get(origCode).toString());
                      debug("armylocation: " + countryArmyLocations.get(origCode));
                     
                      //write the country tag
                      out.write("  <country>\n" +
                                "    <id>" + ctry.getCode() + "</id>\n" +
                                "    <name>" + countryNames.get(origCode).toString() + "</name>\n" +
                                 "   <initialOwner>" + initialOwners.get(origCode).toString() + "</initialOwner>\n" +
                                 "   <initialArmies>" + initialArmies.get(origCode).toString() + "</initialArmies>\n" +
                                "    <armylocation>" + countryArmyLocations.get(origCode) + "</armylocation>\n");
                      
                      //subroutines for writing connection list
                      writeConnections(out, ctry.getAdjoiningCodeList());
                      
                      //how many polygon tags do we have?
                      int numPolygons = ((Vector)countryPolygons.get(origCode)).size();
                      debug("numPolygons: " + numPolygons);

                      //write out each one
                      for (int iPoly = 0; iPoly < numPolygons; iPoly++) {
                         debug("polygon: " + ((Vector)countryPolygons.get(origCode)).get(iPoly).toString());
                         writePolygon(out, ((Vector)countryPolygons.get(origCode)).get(iPoly).toString());
                      }

                      out.write("  </country>\n");
                   } while (contIt.hasNext());
                   
                   //close cont tag
                   out.write("</continent>\n");
               }
           }

           //lines
           for (int i=0; i< lines.size(); i++) {
               out.write("<line>" + lines.get(i) + "</line>");
           }
           
           out.write("</luxboard>\n");
           out.flush();
  }
  
  //this function writes a theme folder. it first copies the original folder theme files
  //then modifies them by "yellowing" out the dead country shapes in the background, and adding 
  //hazard icons to the foreground. If either file doesn't exist, it gets created.
  private void doTheme() {

    try {
      //first delete any old theme directories
      File support = new File(new File(m.getMapGeneratorPath()).getParent());
      File[] supportChildren = support.listFiles();
      File themes = null;
      //find the themes folder
      for(int i = 0; i < supportChildren.length; i += 1) {
        if(supportChildren[i].getName().toLowerCase().equals("themes")) {
          themes = supportChildren[i];
        }
      }
      //delete any folder (and files) that start with HazardWaste_
      File[] themesChildren = themes.listFiles();
      for(int i = 0; i < themesChildren.length; i += 1) {
        if(themesChildren[i].getName().indexOf("Biohazard_") != -1) {
          File[] oldThemeFiles = themesChildren[i].listFiles();
          for(int j = 0; j < oldThemeFiles.length; j += 1) {
            oldThemeFiles[j].delete();
          }
          themesChildren[i].delete();
        }

      }

      //create our theme folder
      new File(themes.getPath() + File.separator + "Biohazard_" + theme + boardSeed).mkdir();

      //copy theme files for the existing theme
      for(int i = 0; i < themesChildren.length; i += 1) {
        if(themesChildren[i].getName().equals(theme)) {
          File[] origThemeFiles = themesChildren[i].listFiles();
          for(int j = 0; j < origThemeFiles.length; j += 1) {
             copyFile(origThemeFiles[j].getAbsolutePath(), themes.getPath() + File.separator + "Biohazard_" + theme + boardSeed + File.separator + origThemeFiles[j].getName());
          }
        }
      }
      
      //get our overground and background images into memory
      loadOverground();
      loadBackground();

      overG = overground.createGraphics();
      backG = background.createGraphics();
      
      //backG.drawImage(background,0,0,Integer.parseInt(width), Integer.parseInt(height), this);

      overG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
      backG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
              
      backG.setColor(Color.YELLOW);
      backG.setStroke(new BasicStroke(1));

      Boolean foundHazard = false;
      BufferedImage HazardImage = null;
      
      //find included hazard image to get icon
      String hazardPath = MapLoader.getMapGeneratorPath()  + "biohazard.jpg";
      
      File hazard = new File(hazardPath);
      
      //if we found it, read the file
      if (hazard.exists()) {
          foundHazard = true;
          HazardImage = ImageIO.read(hazard);
      } else {
        foundHazard = false;
      }
              
      //fill in missing countries
      for (int i = 0; i < deadCountries.size(); i++) {
            int deadID = (Integer)deadCountries.get(i);
            int numPolygons = ((Vector)countryPolygons.get(deadID)).size();
            for (int iPoly = 0; iPoly < numPolygons; iPoly++) {
                 String poly = ((Vector)countryPolygons.get(deadID)).get(iPoly).toString();
                 fillPolygon(backG, getPolygon(poly));
            }
            //add hazard icon
            if (foundHazard) {
                BufferedImage hazardIcon = HazardImage.getSubimage(1, 1, 22, 20);
                             
                //place where the original country army indicator was
                String[] xy = countryArmyLocations.get(deadID).toString().split(",");
                int x = Integer.parseInt(xy[0]);
                int y = Integer.parseInt(xy[1]);
                              
                overG.drawImage(hazardIcon, x-10, Integer.parseInt(height)-y-10, this);
            }
      }
           
      //write out the new overground and background files
      ImageIO.write(overground, "png", new File(support.getPath() + File.separator + "Themes" + File.separator + "Biohazard_" + theme + boardSeed + File.separator + "overground.png"));  
      ImageIO.write(background, "jpg", new File(support.getPath() + File.separator + "Themes" + File.separator + "Biohazard_" + theme + boardSeed + File.separator + "background.jpg"));  
       
    } catch(Exception e) {
      debug(e.toString());
    }

  }
  
    //attempts to load an existing overground.png. Creates one if it isn't found
    private void loadOverground() {
    try {
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String overgroundPath = support.getPath() + File.separator + "Themes" + File.separator +
                                "Biohazard_"+ theme + boardSeed + File.separator + "overground.png";
      
      File over = new File(overgroundPath);
      
      if (over.exists()) {
        overground = ImageIO.read(over);
      } else {
        overground = new BufferedImage(Integer.parseInt(width), Integer.parseInt(height), BufferedImage.TYPE_INT_ARGB);
      }
     
    } catch(Exception e) {
      debug(e.toString());
    }
  }

    //resizes an image based on the passed in scalew and scaleh
    private BufferedImage getScaledImage(Image image, double scalew, double scaleh) {
        int w = (int)(scalew*image.getWidth(null));
        int h = (int)(scaleh*image.getHeight(null));
        int type = BufferedImage.TYPE_INT_RGB;
        BufferedImage out = new BufferedImage(w, h, type);
        Graphics2D g2 = out.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance(scalew, scaleh);
        g2.drawImage(image, at, null);
        g2.dispose();
        return out;
    }    
    
    //attempts to load an existing background.png. Creates one if it isn't found
    //because some maps use the default themes, with a background of different size,
    //we scale it so when we write to it, it doesn't get distorted by Lux's scaling later
 private void loadBackground() {
    try {
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String backgroundPath = support.getPath() + File.separator + "Themes" + File.separator +
                                "Biohazard_"+ theme + boardSeed + File.separator + "background.jpg";
      
      File back = new File(backgroundPath);
      
      if (back.exists()) {
         //read image
        BufferedImage tempBack = ImageIO.read(back);
        
        //create a new backgroun image
        background = new BufferedImage(Integer.parseInt(width), Integer.parseInt(height), BufferedImage.TYPE_INT_RGB);
        
        double tempW = tempBack.getWidth();
        double tempH = tempBack.getHeight();
        double subW = 0;
        double subH = 0;

        //determine the scale ratio of the map to the theme backgound
        subW = Double.parseDouble(width)/tempW;
        subH = Double.parseDouble(height)/tempH;
        
        //and scale our original file
        BufferedImage origBG = getScaledImage(tempBack, subW, subH);
        
        //now write to our background file, the scaled image (should match sizes exactly)
        Graphics2D graph = background.createGraphics();
        graph.drawImage(origBG, 0, 0, this);
      } else {
        background = new BufferedImage(Integer.parseInt(width), Integer.parseInt(height), BufferedImage.TYPE_INT_ARGB);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

 //this function copies one file to a target location
  private void copyFile(String source, String target) {
        byte[] iobuff = new byte[1024];
        int bytes;

        try {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream (target);

        while ( (bytes = fis.read( iobuff )) != -1 )
        {
        fos.write( iobuff, 0, bytes );
        }

        fis.close();
        fos.close();
        } catch(Exception e) {
            debug(e.toString());
        }
  }
 
   //write the xml representation of a country polygon (currently just uses a passed cdv string
  private void writePolygon(PrintWriter out, String polygon) {
    out.write("<polygon>" + polygon + "</polygon>\n");
  }

  //takes a string representation of a polygon and converts it to a polygon object
  private Polygon getPolygon(String polygon) {
      
      Polygon p = new Polygon();
      
      String[] coords = polygon.split(" ");
      for (int i =0; i< coords.length; i++) {
          String[] xy = coords[i].toString().split(",");
          p.addPoint(Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
      }
      
      return p;
  }
  
  //takes a polygon object and fills that space on an image
  private void fillPolygon(Graphics g, Polygon p) {
    int[] ypoints = new int[p.npoints];
    //y coordinates are reversed on images compared to lux map coordinates
    for(int i = 0; i < p.npoints; i += 1) {
      ypoints[i] = Integer.parseInt(height) - p.ypoints[i];
    }
    g.fillPolygon(p.xpoints, ypoints, p.npoints);
  }  
  
  //write the xml representation of a country's adjoining list
  private void writeConnections(PrintWriter out, int[] conns) {
    out.write("<adjoining>");
    for(int i = 0; i < conns.length-1; i += 1) {
      out.write("" + conns[i] + ",");
    }
    if(conns.length > 0) out.write("" + conns[conns.length-1]);
    out.write("</adjoining>\n");
  }
 
  //find the center of gravity of a polygon's points - currently unused
  private Point polygonCenter(Polygon p) {
    double x = 0;
    double y = 0;
    for(int i = 0; i < p.npoints; i += 1) {
      x += p.xpoints[i];
      y += p.ypoints[i];
    }
    x /= p.npoints;
    y /= p.npoints;
    return new Point((int)x, (int)y);
  }

  //required interface function. should be true on release
  public boolean canCache() {
    return false;
  }

  public String description() {
    return "Biohazard takes an existing map file and randomly selects countries that are deemed hazardous. These countries are removed from the map and marked with a biohazard icon. The map choice determines the amount of wasteland generated, but does not change the original map size in any way. Version 1.0 supports Classic and Deux. You must have the original map file to use the Biohazard version.";
  }

  //user size choices
  static final String CHOICE_CLASSIC_L = "Biohazard - Classic - light";
  static final String CHOICE_CLASSIC_M = "Biohazard - Classic - medium";
  static final String CHOICE_CLASSIC_H = "Biohazard - Classic - heavy";
  static final String CHOICE_CLASSIC_E = "Biohazard - Classic - extreme";
  static final String CHOICE_DEUX_L = "Bio - Deux - light";
  static final String CHOICE_DEUX_M = "Bio - Deux - medium";
  static final String CHOICE_DEUX_H = "Bio - Deux - heavy";
  static final String CHOICE_DEUX_E = "Bio - Deux - extreme";
  static final String CHOICE_ARMSRACE_L = "Biohazard - Arms Race - light";

  public java.util.List getChoices() {
    Vector v = new Vector();
    v.add(CHOICE_CLASSIC_L);
    v.add(CHOICE_CLASSIC_M);
    v.add(CHOICE_CLASSIC_H);
    v.add(CHOICE_CLASSIC_E);
    v.add(CHOICE_DEUX_L);
    v.add(CHOICE_DEUX_M);
    v.add(CHOICE_DEUX_H);
    v.add(CHOICE_DEUX_E);
   // v.add(CHOICE_ARMSRACE_S);
    return v;
  }

  //interface function; unused
  public String message(String message, Object data) {
   	{
	if ("scenarioPlayerCount".equals(message))
		{
                debug("get scenario number");
                InitMapChoice((String)data);

                int numPlayers = HowManyPlayersInMap();
		// How many players does the scenario for this size have?
		String choice = (String) data;
		return String.valueOf(numPlayers);
		}

	return null;
	}	
  }

  public String name() {
    return "Biohazard";
  }

  public float version() {
    return 1.2f;
  }

  private void debug(String s) {
    System.out.println(":" + s);
  }
  
   //a method we have to have to draw images
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {return false;}
  
private Document parseLuxMap(String LuxbPath) throws Exception
	{
	DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();	

    if (! new File(LuxbPath).exists() && LuxbPath.endsWith("Spicy Deux.luxb"))
		{
		return docBuilder.parse (new StringBufferInputStream(SpicyDeuxXML()));
		}
		
	return docBuilder.parse (new File(LuxbPath));
	}

private String SpicyDeuxXML()
	{
	return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<luxboard><version>2.0</version><width>958</width><height>613</height><theme>Biohazard Deux</theme><author>Sillysoft Games</author><email>lux@sillysoft.net</email><webpage>http://sillysoft.net</webpage><title>Bio Deux</title><description>After the nukes.</description><continent><continentname>Australia</continentname><bonus>3</bonus><labellocation>791,172</labellocation><color>0.5843137254901961/0.3568627450980392/0.06274509803921569</color><country><id>0</id><name>Eastern Australia</name><initialOwner>0</initialOwner><adjoining>1,2</adjoining><armylocation>829,137</armylocation><polygon>838,100 824,101 818,111 811,113 806,121 799,141 807,147 811,172 823,166 827,189 833,176 837,161 854,143 855,128 845,106</polygon></country><country><id>1</id><name>New Guinea</name><initialOwner>0</initialOwner><initialArmies>6</initialArmies><adjoining>0,2,3</adjoining><armylocation>826,203</armylocation><polygon>833,196 841,189 850,189 836,207 815,214 808,208 819,199 813,195 826,190</polygon></country><country><id>2</id><name>Western Australia</name><initialOwner>0</initialOwner><adjoining>0,1,3</adjoining><armylocation>781,146</armylocation><polygon>757,115 766,113 771,117 779,117 785,122 794,124 800,123 806,121 799,141 807,147 811,172 813,185 796,185 791,176 785,179 776,169 772,161 754,153 753,142 758,127</polygon></country><country><id>3</id><name>Indonesia</name><initialOwner>0</initialOwner><initialArmies>11</initialArmies><adjoining>1,2,32</adjoining><armylocation>752,221</armylocation><polygon>763,239 766,232 761,217 771,221 785,224 778,214 770,212 779,201 763,201 761,217 757,208 746,209 741,200 763,194 785,193 781,185 758,188 733,195 736,203 723,208 709,235 718,233 723,225 742,206 742,224</polygon></country><country><id>32</id><name>Siam</name><initialOwner>0</initialOwner><initialArmies>2</initialArmies><adjoining>3,31,34</adjoining><armylocation>718,284</armylocation><polygon>705,274 705,263 713,267 715,243 723,227 733,223 729,236 719,249 721,259 733,245 742,251 741,265 733,273 735,283 724,303 701,302 697,286</polygon></country></continent><continent><continentname>South America</continentname><bonus>4</bonus><labellocation>316,148</labellocation><color>0.5019607843137255/0.0/0.0</color><country><id>4</id><name>Argentina</name><initialOwner>1</initialOwner><adjoining>5,6</adjoining><armylocation>296,109</armylocation><polygon>291,41 297,45 289,56 296,73 291,76 301,87 320,109 310,116 321,124 329,135 321,141 313,136 304,143 296,141 284,147 281,119 277,91 272,61 279,46</polygon></country><country><id>5</id><name>Brazil</name><initialOwner>1</initialOwner><initialArmies>2</initialArmies><adjoining>4,6,7,12</adjoining><armylocation>333,187</armylocation><polygon>321,141 329,135 321,124 310,116 320,109 339,130 345,148 362,157 364,178 375,200 362,209 341,217 329,233 313,221 304,233 283,226 279,206 285,199 278,189 284,177 293,181 310,170</polygon></country><country><id>6</id><name>Peru</name><initialOwner>1</initialOwner><initialArmies>6</initialArmies><adjoining>4,5,7</adjoining><armylocation>298,163</armylocation><polygon>260,212 279,206 285,199 278,189 284,177 293,181 310,170 321,141 313,136 304,143 296,141 284,147 286,165 271,176 257,200</polygon></country><country><id>7</id><name>Venezuela</name><initialOwner>1</initialOwner><initialArmies>11</initialArmies><adjoining>5,6,14</adjoining><armylocation>285,238</armylocation><polygon>329,233 313,221 304,233 283,226 279,206 260,212 268,230 268,243 283,255 296,248 310,250 316,238</polygon></country></continent><continent><continentname>Africa</continentname><bonus>6</bonus><labellocation>499,236</labellocation><color>0.0/0.0/0.0</color><country><id>8</id><name>South Africa</name><initialOwner>0</initialOwner><initialArmies>3</initialArmies><adjoining>9,10,11</adjoining><armylocation>526,152</armylocation><polygon>497,190 512,190 515,181 525,179 529,190 538,191 536,173 545,171 550,181 565,172 554,159 555,147 545,137 539,122 525,115 512,115 508,129 502,147 497,158 494,170</polygon></country><country><id>9</id><name>Congo</name><initialOwner>1</initialOwner><adjoining>8,10,12</adjoining><armylocation>515,209</armylocation><polygon>497,190 512,190 515,181 525,179 529,190 536,226 525,231 507,232 501,215 487,215 495,202</polygon></country><country><id>10</id><name>East Africa</name><initialOwner>2</initialOwner><adjoining>8,9,11,12,13,30</adjoining><armylocation>557,238</armylocation><polygon>525,231 536,226 529,190 538,191 536,173 545,171 550,181 565,172 567,188 565,203 574,221 589,233 596,255 575,250 573,257 560,271 555,287 537,281 521,261</polygon></country><country><id>11</id><name>Madagascar</name><initialOwner>3</initialOwner><initialArmies>3</initialArmies><adjoining>8,10</adjoining><armylocation>586,169</armylocation><polygon>579,171 590,184 597,169 594,149 585,140 574,152 579,161</polygon></country><country><id>12</id><name>North Africa</name><initialOwner>4</initialOwner><adjoining>5,9,10,13,26,28</adjoining><armylocation>460,281</armylocation><polygon>487,215 501,215 507,232 525,231 521,261 493,281 497,296 513,310 491,320 490,331 471,331 449,326 440,314 430,302 421,281 421,259 429,243 445,232 472,237 488,230</polygon></country><country><id>13</id><name>Egypt</name><initialOwner>0</initialOwner><adjoining>10,12,28,30</adjoining><armylocation>524,293</armylocation><polygon>555,287 549,303 544,311 520,319 513,310 497,296 493,281 521,261 537,281</polygon></country></continent><continent><continentname>Mexico</continentname><bonus>1</bonus><labellocation>208,268</labellocation><color>1.0/0.5019607843137255/0.0</color><country><id>14</id><name>Central America</name><initialOwner>4</initialOwner><adjoining>7,15,16</adjoining><armylocation>200,301</armylocation><polygon>157,325 181,327 201,321 218,304 215,296 217,280 224,274 231,275 243,283 241,267 253,262 254,250 268,243 247,245 232,262 217,265 195,277 188,296 172,314 177,301 185,284 172,297</polygon></country></continent><continent><continentname>USA</continentname><bonus>4</bonus><labellocation>140,370</labellocation><color>0.17647058823529413/0.21176470588235294/0.5843137254901961</color><country><id>15</id><name>Western United States</name><initialOwner>2</initialOwner><initialArmies>11</initialArmies><adjoining>14,16,17,18</adjoining><armylocation>181,349</armylocation><polygon>201,321 201,339 213,338 215,368 205,368 147,367 147,347 157,325 181,327</polygon></country><country><id>16</id><name>Eastern United States</name><initialOwner>2</initialOwner><initialArmies>2</initialArmies><adjoining>14,15,18,19</adjoining><armylocation>243,337</armylocation><polygon>218,304 201,321 201,339 213,338 215,368 244,357 267,353 301,373 305,362 296,355 284,351 285,344 274,341 271,328 257,313 259,293 249,308 237,311 232,305 224,309</polygon></country><country><id>20</id><name>Alaska</name><initialOwner>2</initialOwner><initialArmies>6</initialArmies><adjoining>17,21,40</adjoining><armylocation>71,440</armylocation><polygon>91,418 67,406 38,415 39,440 50,460 65,470 112,459 128,423 116,406</polygon></country></continent><continent><continentname>Canada</continentname><bonus>5</bonus><labellocation>231,434</labellocation><color>0.0/0.7490196078431373/1.0</color><country><id>17</id><name>Western Canada</name><initialOwner>2</initialOwner><adjoining>15,18,20,21</adjoining><armylocation>166,394</armylocation><polygon>131,386 116,406 128,423 203,417 205,368 147,367</polygon></country><country><id>18</id><name>Ontario</name><initialOwner>1</initialOwner><adjoining>15,16,17,19,21</adjoining><armylocation>232,382</armylocation><polygon>223,413 230,404 255,393 262,382 267,353 244,357 215,368 205,368 203,417 212,415</polygon></country><country><id>19</id><name>Quebec</name><initialOwner>3</initialOwner><adjoining>16,18,22</adjoining><armylocation>286,389</armylocation><polygon>263,394 269,399 266,424 286,417 292,407 301,417 308,401 322,389 321,382 301,373 267,353 262,382</polygon></country><country><id>21</id><name>Northwest Territories</name><initialOwner>4</initialOwner><initialArmies>3</initialArmies><adjoining>17,18,20,42</adjoining><armylocation>186,439</armylocation><polygon>112,459 136,464 171,455 163,473 146,473 149,488 177,479 195,482 209,459 184,460 175,454 194,453 208,452 224,470 248,457 255,425 230,430 223,413 212,415 203,417 128,423</polygon></country><country><id>42</id><name>Nunavut</name><initialOwner>0</initialOwner><adjoining>21,22</adjoining><armylocation>264,470</armylocation><polygon>230,489 250,494 276,474 296,452 288,436 261,446 244,465</polygon></country></continent><continent><continentname>Scandinavia</continentname><bonus>3</bonus><labellocation>441,476</labellocation><color>0.6078431372549019/0.7529411764705882/0.8941176470588236</color><country><id>22</id><name>Greenland</name><initialOwner>3</initialOwner><initialArmies>6</initialArmies><adjoining>19,23,42</adjoining><armylocation>363,513</armylocation><polygon>280,514 295,533 320,553 359,568 388,572 406,551 430,543 415,506 404,464 379,452 358,435 352,413 335,427 332,459 319,488 307,499 286,499</polygon></country><country><id>23</id><name>Iceland</name><initialOwner>3</initialOwner><initialArmies>11</initialArmies><adjoining>22,24,25</adjoining><armylocation>421,435</armylocation><polygon>404,440 423,447 435,435 422,423 409,429</polygon></country><country><id>24</id><name>Scandinavia</name><initialOwner>3</initialOwner><initialArmies>2</initialArmies><adjoining>23,25,27,29</adjoining><armylocation>499,422</armylocation><polygon>537,415 536,467 502,453 494,435 477,423 481,406 493,411 497,397 512,411 507,421 521,440 529,439 519,427 521,416</polygon></country></continent><continent><continentname>Europe</continentname><bonus>6</bonus><labellocation>479,374</labellocation><color>0.0/0.5019607843137255/0.0</color><country><id>25</id><name>Great Britain</name><initialOwner>3</initialOwner><adjoining>23,24,26,27</adjoining><armylocation>461,387</armylocation><polygon>457,410 464,406 458,399 469,386 466,380 451,377 455,388 449,395 448,386 445,385 437,382 437,394 449,397 449,406</polygon></country><country><id>26</id><name>Western Europe</name><initialOwner>1</initialOwner><adjoining>12,25,27,28</adjoining><armylocation>453,344</armylocation><polygon>469,377 479,368 478,353 470,353 469,346 461,341 460,331 449,329 440,335 440,353 459,352 464,358 453,369</polygon></country><country><id>27</id><name>Northern Europe</name><initialOwner>4</initialOwner><adjoining>24,25,26,28,29</adjoining><armylocation>514,383</armylocation><polygon>469,377 488,397 497,391 517,395 521,406 529,411 526,377 511,368 506,375 479,368</polygon></country><country><id>28</id><name>Southern Europe</name><initialOwner>2</initialOwner><initialArmies>3</initialArmies><adjoining>12,13,26,27,29,30</adjoining><armylocation>523,359</armylocation><polygon>479,368 506,375 511,368 526,377 544,364 531,345 521,343 514,347 502,361 496,358 509,340 502,329 494,335 503,339 487,355 478,353</polygon></country><country><id>29</id><name>Ukraine</name><initialOwner>3</initialOwner><adjoining>24,27,28,30,33,35</adjoining><armylocation>566,403</armylocation><polygon>536,467 568,449 562,442 549,445 551,435 560,430 585,449 604,455 607,430 596,411 597,387 596,364 584,359 590,334 569,347 560,362 544,364 526,377 529,411 537,415</polygon></country></continent><continent><continentname>Middle East</continentname><bonus>3</bonus><labellocation>608,330</labellocation><color>1.0/1.0/0.0</color><country><id>30</id><name>Middle East</name><initialOwner>4</initialOwner><initialArmies>7</initialArmies><adjoining>10,13,28,29,31,33</adjoining><armylocation>573,315</armylocation><polygon>544,311 549,303 555,301 563,289 573,271 573,257 597,267 616,285 607,296 595,291 586,305 591,309 601,301 616,296 625,296 622,308 635,319 627,332 601,333 590,334 569,347 550,349 533,344 531,333 544,328 556,329 553,315</polygon></country><country><id>33</id><name>Afghanistan</name><initialOwner>4</initialOwner><initialArmies>7</initialArmies><adjoining>29,30,31,34,35</adjoining><armylocation>635,352</armylocation><polygon>601,333 595,364 597,387 651,373 682,355 663,340 669,325 635,319 627,332</polygon></country></continent><continent><continentname>India</continentname><bonus>1</bonus><labellocation>661,267</labellocation><color>0.8/0.8/0.8</color><country><id>31</id><name>India</name><initialOwner>4</initialOwner><initialArmies>5</initialArmies><adjoining>30,32,33,34</adjoining><armylocation>664,296</armylocation><polygon>633,295 641,281 649,278 661,243 669,248 669,263 686,281 697,286 701,302 697,317 680,325 669,325 635,319 622,308 625,296</polygon></country></continent><continent><continentname>Far East</continentname><bonus>5</bonus><labellocation>750,337</labellocation><color>0.10196078431372549/0.10196078431372549/0.30196078431372547</color><country><id>34</id><name>China</name><initialOwner>1</initialOwner><adjoining>31,32,33,35,36,37</adjoining><armylocation>721,329</armylocation><polygon>669,323 663,340 682,355 693,363 709,365 739,353 770,341 773,331 769,323 777,309 770,295 752,283 735,283 724,303 701,302 697,317 680,325</polygon></country><country><id>37</id><name>Mongolia</name><initialOwner>2</initialOwner><adjoining>34,36,38,39,40</adjoining><armylocation>751,369</armylocation><polygon>778,382 800,351 789,339 795,328 785,320 781,339 770,341 739,353 709,365 713,391</polygon></country><country><id>38</id><name>Japan</name><initialOwner>4</initialOwner><adjoining>37,40</adjoining><armylocation>811,325</armylocation><polygon>825,359 838,351 826,347 823,316 795,299 796,320 813,332 823,344</polygon></country></continent><continent><continentname>Russia</continentname><bonus>7</bonus><labellocation>722,441</labellocation><color>0.4/0.0/0.2</color><country><id>35</id><name>Ural</name><initialOwner>4</initialOwner><adjoining>29,33,34,36</adjoining><armylocation>632,406</armylocation><polygon>615,461 628,459 641,453 656,416 679,382 693,363 682,355 651,373 597,387 596,411 607,430 604,455 595,469 598,477 607,491 633,503 640,502 639,496 629,493 617,489 605,473 610,464</polygon></country><country><id>36</id><name>Siberia</name><initialOwner>0</initialOwner><adjoining>34,35,37,39,41</adjoining><armylocation>685,430</armylocation><polygon>644,482 652,473 670,478 694,494 718,500 718,454 713,391 709,365 693,363 679,382 656,416 641,453</polygon></country><country><id>39</id><name>Irkutsk</name><initialOwner>1</initialOwner><initialArmies>3</initialArmies><adjoining>36,37,40,41</adjoining><armylocation>752,418</armylocation><polygon>805,437 778,382 713,391 718,454</polygon></country><country><id>40</id><name>Kamchatka</name><initialOwner>2</initialOwner><adjoining>20,37,38,39,41</adjoining><armylocation>853,439</armylocation><polygon>853,465 873,465 893,458 921,458 922,439 916,425 898,417 881,415 878,399 862,380 861,400 883,425 866,422 855,411 829,412 809,397 829,387 818,367 800,351 778,382 805,437</polygon></country><country><id>41</id><name>Yakutsk</name><initialOwner>3</initialOwner><adjoining>36,39,40</adjoining><armylocation>765,464</armylocation><polygon>730,511 755,499 753,483 777,477 781,484 796,478 799,466 820,476 848,473 853,465 805,437 718,454 718,500</polygon></country></continent></luxboard>";
	}
}