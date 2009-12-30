///////////////////////////////////////////////////////
//
// TerrorismLux.java
//
// TerrorismLux takes an existing map file and generates
// hidden terrorist connections between countries. It
// creates hidden one-way connections between countries,
// representing foreign terror cells operating within another
// country's borders. 
//
// Created on August 28, 2007, 12:00 AM
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
 
public class TerrorismLux implements LuxMapGenerator  {

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
  private Vector terrorCoords;
  
  boolean success = false;
  boolean bShowTerrorCells = false;

  String mapFile;
 
  int terrorCount; //number of terrorism links to add
  double terrorPercent;
      
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
    //deadCountries = new Vector();
    //newCountryIDs = new Vector();
    //origCountryIDs = new Vector();
    countryNames = new Vector();
    countryArmyLocations = new Vector();
    contNames = new Vector();
    contBonus = new Vector();
    contColor = new Vector();
    lines = new Vector();
    contLabelLoc = new Vector();
    initialOwners = new Vector();
    initialArmies = new Vector();
    terrorCoords = new Vector();
    
    InitMapChoice(boardSize);
    
    debug ("mapFile= " + mapFile);
    debug ("terrorPercent= " + terrorPercent);
    
    //how many countries are in this map?
    numCountries = GetMapSize(mapFile);
    debug ("numCountries= " + numCountries);
    
    //determine the number of countries to waste
    terrorCount = Math.round((float)(numCountries * terrorPercent));
    debug ("terrorCount= " + terrorCount);
    
    //minimum of 1 terror link
    if (terrorCount==0) {
        terrorCount = 1;
    }
    
    //init country array
    countries = new Country[numCountries];
    
    //init vectors because we may get the countries out of order (they are grouped by cont)
    //and this ensure there is a space relating to the countryID for each value
    for (int i = 0; i < numCountries; i++) {
           countryPolygons.add ("");
           connections.add  ("");
           countryNames.add  ("");
           countryArmyLocations.add  (""); 
           initialOwners.add  (""); 
           initialArmies.add  (""); 
    }

    //read map into variables
    ReadXMLMap (mapFile);
    
    //try to get terrorCount # of links
    for (int iTerrorLoop = 0; iTerrorLoop < terrorCount; iTerrorLoop++) {
        //randomly pick a country to start
        int fromCountryID = rand.nextInt(numCountries);
        //randomly pick a country to target
        int targetCountryID = rand.nextInt(numCountries);

        //get list of adjoining country codes from original map
        int[] joiningList = countries[fromCountryID].getAdjoiningCodeList();

        boolean bFound;
        
        bFound = false;
        
        //for each connection that the original country had
        for (int iConn = 0; iConn < joiningList.length; iConn++) {
            //get the ID joining country
            int toJoinID = joiningList[iConn];

            //if the country we are joining to is the same as our terror link
            if (toJoinID == targetCountryID) { 
                bFound = true;
            }
        }

        if (bFound == false) {
            //create country object
            Country toJoin = countries[targetCountryID];

            //add to map connections
            countries[fromCountryID].addToAdjoiningList(toJoin, this);
            
            terrorCoords.add(countryArmyLocations.get(fromCountryID));
        }
        
     }

    //write new map file
    WriteMap(out);

    return true;
  }
  
  private void InitMapChoice(String choice) {
      //determine map file and percentage from choice
    if(choice.equals(CHOICE_CLASSIC_L)) {
      mapFile = "Classic Risk.luxb";
      terrorPercent = .075;
      bShowTerrorCells = false;
    } else if(choice.equals(CHOICE_CLASSIC_LV)) {
      mapFile = "Classic Risk.luxb";
      terrorPercent = .075;
      bShowTerrorCells = true;
    } else if(choice.equals(CHOICE_CLASSIC_E)) {
      mapFile = "Classic Risk.luxb";
      terrorPercent = .15;
      bShowTerrorCells = false;
    } else if(choice.equals(CHOICE_CLASSIC_EV)) {
      mapFile = "Classic Risk.luxb";
      terrorPercent = .15;
      bShowTerrorCells = true;
    }  else if(choice.equals(CHOICE_DEUX_L)) {
      mapFile = "Classic Part Deux.luxb";
      terrorPercent = .075;
      bShowTerrorCells = false;
    }  else if(choice.equals(CHOICE_DEUX_LV)) {
      mapFile = "Classic Part Deux.luxb";
      terrorPercent = .075;
      bShowTerrorCells = true;
    } else if (choice.equals(CHOICE_DEUX_E)) {
      mapFile = "Classic Part Deux.luxb";
      terrorPercent = .15;
      bShowTerrorCells = false;
    } else if(choice.equals(CHOICE_DEUX_EV)) {
      mapFile = "Classic Part Deux.luxb";
      terrorPercent = .15;
      bShowTerrorCells = true;
    }
  }

  //this function reads a luxb file and determines the number of countries within it.
  //we do this first because our variable loading routines need to know the number of countries
  private int GetMapSize(String mapName) {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    
    int countryCount = 0;
      
    try {
        //try to get the file path
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapName;
     
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse (new File(LuxbPath));      
          
      //add up the number of countries in each continent
      NodeList nodeList = doc.getElementsByTagName("continent");
      for (int iCont = 0; iCont < nodeList.getLength(); iCont++) {
          Element contNode = (Element) nodeList.item(iCont);
          NodeList countryList = contNode.getElementsByTagName("country");
          countryCount = countryCount + countryList.getLength();
      }
      
    } catch(Exception e) {
      e.printStackTrace();
    }

    return countryCount;
  }
  
 //reads a map file to determine how many players are in the starting scenario
 private int HowManyPlayersInMap() {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
          
    try {
      players = new Vector();

      //try to get the file path
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapFile;
     
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse (new File(LuxbPath));      
      
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
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      
    try {
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String LuxbPath = support.getPath() + File.separator + "Boards" + File.separator +
                                "Saved" + File.separator + mapName;
     
      debug("Path= " + LuxbPath);
      
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse (new File(LuxbPath));      
           
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
       //first write the map info. the theme is dynamic based on the original map's theme and the seed
           out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              "<theme>" + theme  + "</theme>\n" +
              "<author>TerrorismLux by RandomGuy</author>\n" +
              "<email></email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + title + " - TerrorismLux</title>\n" +
              "<description>TerrorismLux map generator modified map. Original map description: " + description + " Type: " + boardSize + "seed: " + boardSeed + "</description>\n");

           //player info location
           out.write("<playerInfoLocation>" +  playerinfo + "</playerInfoLocation>\n");
           
           //short continent labels
           if (shortContLabels != "") {
               out.write("<shortContinentLabels>" +  shortContLabels + "</shortContinentLabels>\n");
           }
          
           //for each continent in original file
           for (int i=0; i<contNames.size(); i++) {
              //if we still have countries in the continent in our map
              if (BoardHelper.getContinentSize(i, countries) > 0) {
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
                   ContinentIterator contIt = new ContinentIterator(i, countries);
                  
                   //we know we have one country
                   do  {
                       Country ctry = contIt.next();

                       //get the country code, and determine the original country id to serve as an index to our vectors
                      int newCode = ctry.getCode();
                       
                      debug("Country: " + countryNames.get(newCode).toString());
                      debug("Code: " + ctry.getCode());
                      debug("initialOwner: " + initialOwners.get(newCode).toString());
                      debug("initialArmies: " + initialArmies.get(newCode).toString());
                      debug("armylocation: " + countryArmyLocations.get(newCode));
                     
                      //write the country tag
                      out.write("  <country>\n" +
                                "    <id>" + ctry.getCode() + "</id>\n" +
                                "    <name>" + countryNames.get(newCode).toString() + "</name>\n" +
                                 "   <initialOwner>" + initialOwners.get(newCode).toString() + "</initialOwner>\n" +
                                 "   <initialArmies>" + initialArmies.get(newCode).toString() + "</initialArmies>\n" +
                                "    <armylocation>" + countryArmyLocations.get(newCode) + "</armylocation>\n");
                      
                      //subroutines for writing connection list
                      writeConnections(out, ctry.getAdjoiningCodeList());
                      
                      //how many polygon tags do we have?
                      int numPolygons = ((Vector)countryPolygons.get(newCode)).size();
                      debug("numPolygons: " + numPolygons);

                      //write out each one
                      for (int iPoly = 0; iPoly < numPolygons; iPoly++) {
                         debug("polygon: " + ((Vector)countryPolygons.get(newCode)).get(iPoly).toString());
                         writePolygon(out, ((Vector)countryPolygons.get(newCode)).get(iPoly).toString());
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
          
           if (bShowTerrorCells==true) {                   
               for (int i=0; i< terrorCoords.size(); i++) {
                   String[] xy = terrorCoords.get(i).toString().split(",");;
                   int x = Integer.parseInt(xy[0]);
                   int y = Integer.parseInt(xy[1]);
                   writeTerrorIcon(out,x+5,y+5);
               }
            }
          
           
           

           
           out.write("</luxboard>\n");
           out.flush();
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
 
  
//<line><position>381,234 381,230</position><color>0.0/0.4/0.2</color><width>10</width><above>true</above></line>
//<line><position>379,224 383,224</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>379,240 383,240</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>380,243 383,243</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>384,243 388,243</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>388,243 391,237</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>390,237 390,232</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>390,232 388,228</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>
//<line><position>378,239 378,228</position><color>0.0/0.2/0.2</color><above>true</above></line>
//<line><position>382,227 382,238</position><color>0.0/0.2/0.2</color><above>true</above></line>
//<line><position>379,234 384,234</position><color>0.0/0.2/0.2</color><above>true</above></line>
//<line><position>379,230 384,230</position><color>0.0/0.2/0.2</color><above>true</above></line>
//<line><position>378,226 382,226</position><color>0.0/0.2/0.2</color><above>true</above></line>
 
          
   //write the xml representation of a terror icon using lines
  //x=381, y=230
  private void writeTerrorIcon(PrintWriter out, int x, int y) {
   out.write("<line><position>" + x + "," + (y+4) + " " + x + "," + (y+2) + "</position><color>0.0/0.4/0.2</color><width>10</width><above>true</above></line>\n");
   out.write("<line><position>" + (x-2) + "," + (y-4) + " " + (x+2) + "," + (y-4) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x-2) + "," + (y+10) + " " + (x+2) + "," + (y+10) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x-1) + "," + (y+13) + " " + (x+2) + "," + (y+13) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x+3) + "," + (y+13) + " " + (x+8) + "," + (y+13) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x+7) + "," + (y+13) + " " + (x+10) + "," + (y+7) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x+9) + "," + (y+7) + " " + (x+9) + "," + (y+2) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x+9) + "," + (y+2) + " " + (x+7) + "," + (y-2) + "</position><color>0.0/0.4/0.2</color><width>3</width><above>true</above></line>\n");
   out.write("<line><position>" + (x-3) + "," + (y+9) + " " + (x+3) + "," + (y+9) + "</position><color>0.0/0.2/0.2</color><width>2</width><above>true</above></line>\n");
   out.write("<line><position>" + (x-3) + "," + (y+9) + " " + (x-3) + "," + (y-2) + "</position><color>0.0/0.2/0.2</color><above>true</above></line>\n");
   out.write("<line><position>" + (x+1) + "," + (y-3) + " " + (x+1) + "," + (y+8) + "</position><color>0.0/0.2/0.2</color><above>true</above></line>\n");
   out.write("<line><position>" + (x-2) + "," + (y+4) + " " + (x+3) + "," + (y+4) + "</position><color>0.0/0.2/0.2</color><above>true</above></line>\n");
   out.write("<line><position>" + (x-2) + "," + (y) + " " + (x+3) + "," + (y) + "</position><color>0.0/0.2/0.2</color><above>true</above></line>\n");
   out.write("<line><position>" + (x-3) + "," + (y-4) + " " + (x+1) + "," + (y-4) + "</position><color>0.0/0.2/0.2</color><above>true</above></line>\n");
  }
  
  
   //write the xml representation of a country polygon (currently just uses a passed cdv string
  private void writePolygon(PrintWriter out, String polygon) {
    out.write("<polygon>" + polygon + "</polygon>\n");
  }
  
  //required interface function. should be true on release
  public boolean canCache() {
    return false;
  }

  public String description() {
    return "TerrorismLux takes an existing map file and generates hidden terrorist connections between countries. It creates hidden one-way connections between countries, representing foreign terror cells operating within another country's borders. The Vi generators create maps where the terrorist countries are visible (indicated with a grenade icon).";
  }
  
  //user size choices
  static final String CHOICE_CLASSIC_L = "TerrorismLux - Classic - Lt";
  static final String CHOICE_CLASSIC_LV = "TerrorismLux - Classic - Lt Vi";
  static final String CHOICE_CLASSIC_E = "TerrorismLux - Classic - Ex";
  static final String CHOICE_CLASSIC_EV = "TerrorismLux - Classic - Ex Vi";
  static final String CHOICE_DEUX_L = "TerrorismLux - Deux - Li";
  static final String CHOICE_DEUX_LV = "TerrorismLux - Deux - Li Vi";
  static final String CHOICE_DEUX_E = "TerrorismLux - Deux - Ex";
  static final String CHOICE_DEUX_EV = "TerrorismLux - Deux - Ex Vi";

  public java.util.List getChoices() {
    Vector v = new Vector();
    v.add(CHOICE_CLASSIC_L);
    v.add(CHOICE_CLASSIC_E);
    v.add(CHOICE_DEUX_L);
    v.add(CHOICE_DEUX_E);
    v.add(CHOICE_CLASSIC_LV);
    v.add(CHOICE_CLASSIC_EV);
    v.add(CHOICE_DEUX_LV);
    v.add(CHOICE_DEUX_EV);
    //v.add(CHOICE_TEST1);
    //v.add(CHOICE_TEST2);
    //v.add(CHOICE_TEST3);
   // v.add(CHOICE_ARMSRACE_L);
    return v;
  }

  //interface function used to return number of players for a map
  public String message(String message, Object data) {
   	{
	if ("scenarioPlayerCount".equals(message))
		{
                debug("get scenario number");
                //set map file
                InitMapChoice((String)data);

                //get how many players are in the map starting scenario
                int numPlayers = HowManyPlayersInMap();
                
		return String.valueOf(numPlayers);
		}

	return null;
	}	
  }

  public String name() {
    return "TerrorismLux";
  }

  public float version() {
    return 1.1f;
  }

  private void debug(String s) {
    System.out.println(":" + s);
  }
}