//////////////////////
//CastleInfinity Lux Map Generator
//Generates maps for Lux in the style of Castle Lux SI
//Greg McGlynn
//////////////////////

package org.mcglynns.lux;

import com.sillysoft.lux.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.*;

public class CastleInfinity implements LuxMapGenerator, ImageObserver {

  //used in making names
  static char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
                                        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'};
  static char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
  static int numConsonants = consonants.length;
  static int numVowels = vowels.length;

  Random rand;

  int width, height;

  //how big the hexagons are:
  double hexRad = 32;

  Vector countryPolygons;
  Vector connections;
  Polygon[][] castles;
  Vector[][] castleConnections;
  Polygon[] castleCenters;
  Vector bridgePolygons;
  Vector bridgeConnections;
  Vector allPolygons;
  int numCastles;

  String[] castleNames;
  String[] countrysideNames;

  String choice;
  int seed;
  MapLoader m;
  PrintWriter out;
  
  int scenarioOwnerCount;

  //theme:
  BufferedImage background;
  BufferedImage foreground;
  Graphics2D backG, foreG;

  BufferedImage siForeground = null;

  //get the foreground from Castle Lux SI, from which we get building images
  private void loadSIForeground() {
    try {
      File support = new File(m.getMapGeneratorPath()).getParentFile();
      String siForegroundPath = support.getPath() + File.separator + "Themes" + File.separator +
                                "Castle Lux SI" + File.separator + "overground.png";
      siForeground = ImageIO.read(new File(siForegroundPath));
    } catch(Exception e) {
      e.printStackTrace();
	  
	  throw new RuntimeException(" \nYou need to install the Castle Lux SI map for the CastleInfinity generator to work.");
    }
  }

  //Lux calls this when it needs a map, write out the xml and theme for a new map
  public boolean generate(PrintWriter pw, String theChoice, int theSeed, MapLoader loader) {

    m = loader;
    out = pw;
    choice = theChoice;
    seed = theSeed;

    if(siForeground == null) {
      loadSIForeground();
    }


    rand = new Random(seed);

    countryPolygons = new Vector();;
    connections = new Vector();
    bridgePolygons = new Vector();
    bridgeConnections = new Vector();
    allPolygons = new Vector();

    if(choice == CHOICE_SMALL) {
      width = 591;
      height = 485;
      numCastles = 4;
    } else if(choice == CHOICE_NORMAL) {
      width = 785;
      height = 625;
      numCastles = 6;
    } else if(choice == CHOICE_BIG) {
      width = 1062;
      height = 765;
      numCastles = 10;
    }
    castles = new Polygon[numCastles][4];
    castleConnections = new Vector[numCastles][4];
    castleNames = new String[numCastles];
    countrysideNames = new String[numCastles];

    //get new theme images
    background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    backG = (Graphics2D)background.getGraphics();
    foreG = (Graphics2D)foreground.getGraphics();
    backG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    foreG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    backG.setColor(new Color(0, 0, 173));
    backG.fillRect(0, 0, width, height);

    //create a map-spanning grid of hexagons
    makeHexagonalGrid();

    //chocse hexagons for castle locations
    pickCastleCenters();

    //name and create each castle
    for(int i = 0; i < numCastles; i += 1) {
      castleNames[i] = makeCastleName();
      countrysideNames[i] = makeCountrysideName(castleNames[i].substring(0, castleNames[i].length() - 7));
      makeCastleAround(castleCenters[i], i);
    }

    //make borders between countryside continents
    makeCountrysideBorders();


    //bring all polygons into one vector so we know ids.
    allPolygons = new Vector();
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      allPolygons.add(countryPolygons.get(i));
    }
    for(int i = 0; i < numCastles; i += 1) {
      for(int j = 0; j < 4; j += 1) {
        allPolygons.add(castles[i][j]);
      }
    }
    for(int i = 0; i < bridgePolygons.size(); i += 1) {
      allPolygons.add(bridgePolygons.get(i));
    }

    //now we have all the data we need to write the xml file
    //write the header...
    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              "<theme>" + choice+seed + "</theme>\n" +
              "<author>CastleInfinity Generator (by Greg McGlynn)</author>\n" +
              "<email>greg@mcglynns.org</email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + choice + " #" + seed + "</title>\n" +
              "<description>This map was made by the CastleInfinity LuxMapGenerator. Type: " + choice + "seed: " + seed + "</description>\n");

    writeHexagons(); //the countryside
    writeCastles(); //the castles
    writeBridges(); //the bridges

    out.write("</luxboard>"); //done the xml
    out.flush();

    //create and write out the theme
    doTheme();

    return true;
  }

  //make a grid of hexagons that spans the map. the grid needs to be flush with the sides of the map
  private void makeHexagonalGrid() {
    //make the hexagons...
    for(double x = hexRad; x <= width-hexRad; x += Math.sqrt(3)*hexRad) {
      for(double y = hexRad-hexRad/3; y <= height-hexRad+hexRad/3; y += hexRad*3) {
        countryPolygons.add(hexagonAround(new Point((int)x, (int)y)));
      }
    }
    for(double x = hexRad+Math.sqrt(3)/2*hexRad; x <= width-hexRad; x += Math.sqrt(3)*hexRad) {
      for(double y = 5*hexRad/2-hexRad/3; y <= height-hexRad+hexRad/3; y += hexRad*3) {
        countryPolygons.add(hexagonAround(new Point((int)x, (int)y)));
      }
    }

    //connect the ones that touch
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      connections.add(new Vector());
      for(int j = 0; j < countryPolygons.size(); j += 1) {
        if(i != j && dist(polygonCenter((Polygon)countryPolygons.get(i)),
                          polygonCenter((Polygon)countryPolygons.get(j))) < hexRad*1.9) {
          ((Vector)connections.get(i)).add((Polygon)countryPolygons.get(j));
        }
      }
    }

    //make hexagons flush with sides of map
    for(int h = 0; h < countryPolygons.size(); h += 1) {
      Polygon p = (Polygon)countryPolygons.get(h);
      Point center = polygonCenter(p);

      if(center.x > hexRad*Math.sqrt(3)*2/3 && center.x < hexRad*Math.sqrt(3)*4/3) { //left
        p.xpoints = new int[8];
        p.ypoints = new int[8];
        p.npoints = 8;
        for(int i = 0; i < 3; i += 1) {
          p.xpoints[i] = center.x + (int)((hexRad-1)*Math.cos(Math.PI/6 + i*Math.PI/3));
          p.ypoints[i] = center.y + (int)((hexRad-1)*Math.sin(Math.PI/6 + i*Math.PI/3));
        }
        p.xpoints[3] = 5;
        p.ypoints[3] = p.ypoints[1];
        for(int i = 3; i < 6; i += 1) {
          p.xpoints[i+2] = center.x + (int)((hexRad-1)*Math.cos(Math.PI/6 + i*Math.PI/3));
          p.ypoints[i+2] = center.y + (int)((hexRad-1)*Math.sin(Math.PI/6 + i*Math.PI/3));
        }
        p.xpoints[4] = 5;
        p.ypoints[4] = p.ypoints[6];
      }
      if(center.x > width - (hexRad*Math.sqrt(3)*4/3) && center.x < width - (hexRad*Math.sqrt(3)*2/3)) { //right
        p.xpoints = new int[8];
        p.ypoints = new int[8];
        p.npoints = 8;
        for(int i = 0; i < 6; i += 1) {
          p.xpoints[i] = center.x + (int)((hexRad-1)*Math.cos(Math.PI/6 + i*Math.PI/3));
          p.ypoints[i] = center.y + (int)((hexRad-1)*Math.sin(Math.PI/6 + i*Math.PI/3));
        }
        p.xpoints[6] = width-7;
        p.ypoints[6] = p.ypoints[4];
        p.xpoints[7] = width-7;
        p.ypoints[7] = p.ypoints[1];

      }

      if(center.y < hexRad*1.5) { //bottom
        for(int j = 0; j < p.npoints; j += 1) {
           if(p.ypoints[j] < center.y) {
             p.ypoints[j] = 0;
           }
        }
      }
      if(center.y > height - hexRad*1.5) { //top
        for(int j = 0; j < p.npoints; j += 1) {
          if(p.ypoints[j] > center.y) {
            p.ypoints[j]  = height;
          }
        }
      }

    }

  }


  //pick <numCastles> hexagons to be the centers of castles. 
  //castles must be a certain distance from the side of the map and from each other
  private void pickCastleCenters() {

    Polygon ret[] = new Polygon[numCastles];

    loop1:
    while(true) {
      loop2:
      for(int i = 0; i < numCastles; i += 1) {
        loop3:
        for(int j = 0; j < 15; j += 1) {
          int index;
          while(!canBeCastleCenter(index = rand.nextInt(countryPolygons.size())));
          ret[i] = (Polygon)countryPolygons.get(index);
          for(int k = 0; k < i; k += 1) {
            if(dist(polygonCenter(ret[i]), polygonCenter(ret[k])) < 240) {
              continue loop3;
            }
          }
          continue loop2;
        }
        continue loop1;
      }
      break;
    }

    for(int i = 0; i < numCastles; i += 1) {
      removeHexagon(ret[i]);
    }

    castleCenters = ret;


  }

  static final int cOR = 45; //castleOutRadius
  static final int cIR = 30; //castleInRadius
  static final double[] castleAngles = new double[]{0, Math.PI/4, Math.PI/2, Math.PI/2, Math.PI/4, 0};
  static final double[] castleRadii = new double[]{cOR-2, Math.sqrt(2)*(cOR-2), cOR-2, cIR, Math.sqrt(2)*cIR, cIR};

  //make the polygons and connections of a castle centered on Polygon p
  private void makeCastleAround(Polygon p, int castleIndex) {
    Point c = polygonCenter(p);

    //make the polygons
    int outRadius = 60;
    int inRadius = 40;
    Polygon[] parts = new Polygon[4];
    double angle = 0;
    for(int index = 0; index < 4; index += 1) {
      int[] xpoints = new int[6];
      int[] ypoints = new int[6];
      for(int i = 0; i < 6; i += 1) {
        xpoints[i] = (int)(c.x + castleRadii[i]*Math.cos(angle + castleAngles[i]));
        ypoints[i] = (int)(c.y + castleRadii[i]*Math.sin(angle + castleAngles[i]));
      }
      parts[index] = new Polygon(xpoints, ypoints, 6);
      angle += Math.PI/2;
    }

    castles[castleIndex] = parts;

    removeHexagon(p);

    //find the hexagons the castle now borders
    Polygon[] surroundingHexagons = new Polygon[6];
    for(int i = 0; i < 6; i += 1) {
      Polygon nearest = null;
      Point guessedCenter = new Point((int)(c.x + hexRad*Math.sqrt(3)*Math.cos(i*Math.PI/3)),
                                      (int)(c.y + hexRad*Math.sqrt(3)*Math.sin(i*Math.PI/3)));
      double shortestGap = 10000;
      Integer index = null;
      for(int j = 0; j < countryPolygons.size(); j += 1) {
        Polygon thePolygon = (Polygon)countryPolygons.get(j);
        Point center = polygonCenter(thePolygon);
        double gap = dist(guessedCenter, center);
        if(gap < shortestGap) {
          shortestGap = gap;
          nearest = thePolygon;
        }
      }
      surroundingHexagons[i] = nearest;

    }

    //adjust the shapes of the hexagons the castle intrudes onto
    Rectangle castleBounds = new Rectangle(c.x - cOR, c.y - cOR, cOR*2, cOR*2);
    Polygon hex;
    hex = surroundingHexagons[0];
    for(int i = 0; i < hex.npoints; i += 1) {
      if(castleBounds.contains(hex.xpoints[i], hex.ypoints[i]) && hex.ypoints[i] > c.y) {
        hex.xpoints[i] = c.x + cOR;
        hex.ypoints[i] = (int)(c.y + cOR*Math.tan(Math.PI/6));
      } else if(castleBounds.contains(hex.xpoints[i], hex.ypoints[i]) && hex.ypoints[i] < c.y) {
        hex.xpoints[i] = c.x + cOR;
        hex.ypoints[i] = (int)(c.y - cOR*Math.tan(Math.PI/6));
      }
    }

    hex = surroundingHexagons[3];
    for(int i = 0; i < hex.npoints; i += 1) {
      if(castleBounds.contains(hex.xpoints[i], hex.ypoints[i]) && hex.ypoints[i] > c.y) {
        hex.xpoints[i] = c.x - cOR;
        hex.ypoints[i] = (int)(c.y + cOR*Math.tan(Math.PI/6));
      } else if(castleBounds.contains(hex.xpoints[i], hex.ypoints[i]) && hex.ypoints[i] < c.y) {
        hex.xpoints[i] = c.x - cOR;
        hex.ypoints[i] = (int)(c.y - cOR*Math.tan(Math.PI/6));
      }
    }

    Point c1 = polygonCenter(surroundingHexagons[1]);
    surroundingHexagons[1].xpoints = new int[7];
    surroundingHexagons[1].ypoints = new int[7];
    surroundingHexagons[1].xpoints[0] = c.x + cOR;
    surroundingHexagons[1].ypoints[0] = (int)(c.y + cOR*Math.tan(Math.PI/6));
    surroundingHexagons[1].xpoints[1] = c.x + cOR;
    surroundingHexagons[1].ypoints[1] = c.y + cOR;
    surroundingHexagons[1].xpoints[2] = c.x;
    surroundingHexagons[1].ypoints[2] = c.y + cOR;
    surroundingHexagons[1].xpoints[3] = c.x;
    surroundingHexagons[1].ypoints[3] = (int)(c1.y + hexRad/2);
    surroundingHexagons[1].xpoints[4] = c1.x;
    surroundingHexagons[1].ypoints[4] = (int)(c1.y + hexRad);
    surroundingHexagons[1].xpoints[5] = (int)(c1.x + Math.sqrt(3)/2*hexRad);
    surroundingHexagons[1].ypoints[5] = (int)(c1.y + hexRad/2);
    surroundingHexagons[1].xpoints[6] = (int)(c1.x + Math.sqrt(3)/2*hexRad);
    surroundingHexagons[1].ypoints[6] = (int)(c1.y - hexRad/2);
    surroundingHexagons[1].npoints = 7;

    surroundingHexagons[2].xpoints = new int[7];
    surroundingHexagons[2].ypoints = new int[7];
    surroundingHexagons[4].xpoints = new int[7];
    surroundingHexagons[4].ypoints = new int[7];
    surroundingHexagons[5].xpoints = new int[7];
    surroundingHexagons[5].ypoints = new int[7];
    surroundingHexagons[2].npoints = 7;
    surroundingHexagons[4].npoints = 7;
    surroundingHexagons[5].npoints = 7;
    for(int i = 0; i < 7; i += 1) {
      surroundingHexagons[2].xpoints[i] = c.x + -(surroundingHexagons[1].xpoints[i]-c.x);
      surroundingHexagons[2].ypoints[i] = surroundingHexagons[1].ypoints[i];
      surroundingHexagons[4].xpoints[i] = surroundingHexagons[2].xpoints[i];
      surroundingHexagons[4].ypoints[i] = c.y + -(surroundingHexagons[1].ypoints[i]-c.y);
      surroundingHexagons[5].xpoints[i] = surroundingHexagons[1].xpoints[i];
      surroundingHexagons[5].ypoints[i] = surroundingHexagons[4].ypoints[i];
    }

    //create the conections from castle to hexagons
    castleConnections[castleIndex][0] = new Vector();
    castleConnections[castleIndex][0].add(castles[castleIndex][1]);
    castleConnections[castleIndex][0].add(castles[castleIndex][3]);
    castleConnections[castleIndex][0].add(surroundingHexagons[0]);
    castleConnections[castleIndex][0].add(surroundingHexagons[1]);
    castleConnections[castleIndex][1] = new Vector();
    castleConnections[castleIndex][1].add(castles[castleIndex][0]);
    castleConnections[castleIndex][1].add(castles[castleIndex][2]);
    castleConnections[castleIndex][1].add(surroundingHexagons[2]);
    castleConnections[castleIndex][1].add(surroundingHexagons[3]);
    castleConnections[castleIndex][2] = new Vector();
    castleConnections[castleIndex][2].add(castles[castleIndex][1]);
    castleConnections[castleIndex][2].add(castles[castleIndex][3]);
    castleConnections[castleIndex][2].add(surroundingHexagons[3]);
    castleConnections[castleIndex][2].add(surroundingHexagons[4]);
    castleConnections[castleIndex][3] = new Vector();
    castleConnections[castleIndex][3].add(castles[castleIndex][2]);
    castleConnections[castleIndex][3].add(castles[castleIndex][0]);
    castleConnections[castleIndex][3].add(surroundingHexagons[5]);
    castleConnections[castleIndex][3].add(surroundingHexagons[0]);
    //create the connections from hexagons to castle
    for(int i = 0; i < 4; i += 1) {
      for(int j = 0; j < castleConnections[castleIndex][i].size(); j += 1) {
        int hexIndex = countryPolygons.indexOf(castleConnections[castleIndex][i].get(j));
        if(hexIndex != -1) ((Vector)connections.get(hexIndex)).add(castles[castleIndex][i]);
      }
    }
  }




  //create borders between the countryside continents. each countryside continent
  //consists of all hexagons closer to a given castle than any other. borders between
  //continents should consist of choke points in the forms of penalty bridges or land
  //bridges
  private void makeCountrysideBorders() {
    //countrySideConnectionType[i][j] = 
    //   0 - castles i and j are not connected
    //   1 - castles i and j are connected by a land bridge
    //   2 - castles i and j are connected by a penalty bridge
    int[][] countrysideConnectionType = new int[numCastles][numCastles];

    //borders[i][j] consists of the countries on the border between i and j
    Vector[][] borders = new Vector[numCastles][numCastles];
    for(int i = 0; i < numCastles; i += 1) {
      for(int j = 0; j < numCastles; j += 1) {
        countrysideConnectionType[i][j] = 0; //no connections
        borders[i][j] = new Vector();
      }
    }

    //the points at the center of each castle
    Point[] castlePoints = new Point[numCastles];
    for(int i = 0; i < numCastles; i += 1) {
      castlePoints[i] = polygonCenter(castleCenters[i]);
    }

    //now, for each country, determine if it is on the border between its two
    //closest castles. pick connection types for continents that border each other
    loop:
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      Point closestCastle = null;
      Point secondClosest = null;
      int closestIndex = -1;
      int secondClosestIndex = -1;
      double closestDist = 10000;
      double secondClosestDist = 10000;
      for(int j = 0; j < numCastles; j += 1) {
        double dist = dist(castlePoints[j], polygonCenter((Polygon)countryPolygons.get(i)));
        if(dist < secondClosestDist) {
          if(dist < closestDist) {
            secondClosest = closestCastle;
            secondClosestDist = closestDist;
            secondClosestIndex = closestIndex;
            closestCastle = castlePoints[j];
            closestDist = dist;
            closestIndex = j;
          } else {
            secondClosest = castlePoints[j];
            secondClosestDist = dist;
            secondClosestIndex = j;
          }
        }
      }

      if(secondClosest == null) continue loop;

      Point midpoint = midpoint(closestCastle, secondClosest);
      double lineAngle = (Math.atan2(closestCastle.y - secondClosest.y ,
                            closestCastle.x - secondClosest.x) + Math.PI/2+6*Math.PI)%(2*Math.PI);
      Point endpoint1 = new Point((int)(midpoint.x + -1000*Math.cos(lineAngle)),
                                  (int)(midpoint.y + -1000*Math.sin(lineAngle)));
      Point endpoint2 = new Point((int)(midpoint.x +  1000*Math.cos(lineAngle)),
                                  (int)(midpoint.y +  1000*Math.sin(lineAngle)));
      Line2D.Double perpBisector = new Line2D.Double(endpoint1, endpoint2);

      Rectangle r = dilatePolygon((Polygon)countryPolygons.get(i), 33.0/31).getBounds();
      if(!perpBisector.intersects(r)) {
        continue loop; //not part of a border
      }

      int connType = countrysideConnectionType[closestIndex][secondClosestIndex];
      if(connType == 0) {
        connType = 1+rand.nextInt(2);
        countrysideConnectionType[secondClosestIndex][closestIndex] = connType;
        countrysideConnectionType[closestIndex][secondClosestIndex] = connType;
      }
      borders[closestIndex][secondClosestIndex].add((Polygon)countryPolygons.get(i));
      borders[secondClosestIndex][closestIndex].add((Polygon)countryPolygons.get(i));
    }

    //now, for land bridge connections remove all countries on the border except an isthmus
    //remove all border countries for penalty bridge connections
    for(int i = 0; i < numCastles; i += 1) {
      for(int j = i+1; j < numCastles; j += 1) {
        if(countrysideConnectionType[i][j] == 1) { //land bridge
          //keep is whether we keep or remove a given hexagon:
          boolean[] keep = new boolean[borders[i][j].size()];
          for(int k = 0; k < keep.length; k += 1) {
            keep[k] = false;
          }
          loop:
          for(int k = 0; k < 1; k += 1) {
            int bridgeSeed = rand.nextInt(borders[i][j].size());
            keep[bridgeSeed] = true;
            //don't retain a polygon already removed by another connection:
            if(!countryPolygons.contains(borders[i][j].get(bridgeSeed))) {
              k -= 1;
              continue loop;
            }

            //keep a hexagon and all those that touch it
            Vector conns = (Vector)connections.get(countryPolygons.indexOf(borders[i][j].get(bridgeSeed)));
            for(int l = 0; l < conns.size(); l += 1) {
              int index =  borders[i][j].indexOf((Polygon)conns.get(l));
              if(index != -1) keep[index] = true;
            }
          }

          for(int k = 0; k < borders[i][j].size(); k += 1) {
            if(!keep[k]) removeHexagon((Polygon)borders[i][j].get(k));
          }
        } else if(countrysideConnectionType[i][j] == 2) { //penalty bridge, remove everything
          for(int k = 0; k < borders[i][j].size(); k += 1) {
            removeHexagon((Polygon)borders[i][j].get(k));
          }
        }
      }

    }

    //make bridges. the bridge's location is initial the midpoint of the castles it connects
    //then we choose the two hexagons a bridge at that point would connect to, and move the 
    //bridge center to their midpoint. repeat to get a well-placed bridge. then make it
    for(int i = 0; i < numCastles; i += 1) {
      for(int j = i + 1; j < numCastles; j += 1) {
        if(countrysideConnectionType[i][j] == 2) {

          Point bridgeCenter = midpoint(castlePoints[i], castlePoints[j]);

          int closestToI = -1, closestToJ = -1;
          int iDist = 10000;
          int jDist = 10000;
          for(int iter = 0; iter < 5; iter += 1) {

            for(int k = 0; k < countryPolygons.size(); k += 1) {
              int closestCastle = closestCastle((Polygon)countryPolygons.get(k));

              int bDist = (int)dist(bridgeCenter, polygonCenter((Polygon)countryPolygons.get(k)));
              if(closestCastle == i && bDist < iDist) {
                iDist = bDist;
                closestToI = k;
              }
              if(closestCastle == j && bDist < jDist) {
                jDist = bDist;
               closestToJ = k;
              }
            }
            bridgeCenter = midpoint(polygonCenter((Polygon)countryPolygons.get(closestToI)),
                                    polygonCenter((Polygon)countryPolygons.get(closestToJ)));
          }
          makeBridgeBetween((Polygon)countryPolygons.get(closestToI),
                            (Polygon)countryPolygons.get(closestToJ));
        }
      }
    }

    //remove any isolated hexagons or hexagons in the wrong continent
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      Vector conns = (Vector)connections.get(i);
      boolean hasNeighborInContinent = false;
      int iClosestCastle = closestCastle((Polygon)countryPolygons.get(i));
      for(int j = 0; j < conns.size(); j += 1) {
        if(countryPolygons.contains(conns.get(j)) &&
           iClosestCastle == closestCastle((Polygon)conns.get(j))) {
          hasNeighborInContinent = true;
        }
      }

      if(!hasNeighborInContinent) {
        removeHexagon((Polygon)countryPolygons.get(i));
        i -= 1;
      }
    }

    //draw land borders between continents
    //for each pair of polygons
    //  if they are in two different continents and two of their sides are right next to each other
    //  draw a thick white line along those sides
    foreG.setColor(Color.white);
    foreG.setStroke(new BasicStroke(5));
    for(int ip = 0; ip < countryPolygons.size(); ip += 1) {
      Polygon p = (Polygon)countryPolygons.get(ip);
      int pClosestCastle = closestCastle(p);
      for(int jn = 0; jn < ((Vector)connections.get(ip)).size(); jn += 1) {
        Polygon n = (Polygon)(((Vector)connections.get(ip)).get(jn));
        if(countryPolygons.contains(n) && pClosestCastle != closestCastle(n)) { //countryside border
          for(int i = 0; i < p.npoints; i += 1) {
            for(int j = 0; j < n.npoints; j += 1) {
              if(dist(new Point(p.xpoints[i], p.ypoints[i]),
                      new Point(n.xpoints[j], n.ypoints[j])) < 10) {
                for(int iOff = -1; iOff <= 1; iOff += 2) {
                  int i2 = (i+iOff+p.npoints)%p.npoints;
                  for(int jOff = -1; jOff <= 1; jOff += 2) {
                    int j2 = (j+jOff+n.npoints)%n.npoints;
                    if(dist(new Point(p.xpoints[i2], p.ypoints[i2]),
                            new Point(n.xpoints[j2], n.ypoints[j2])) < 10) {
                      foreG.draw(new Line2D.Double((p.xpoints[i]+n.xpoints[j])/2, height - (p.ypoints[i]+n.ypoints[j])/2,
                                                   (p.xpoints[i2]+n.xpoints[j2])/2, height - (p.ypoints[i2]+n.ypoints[j2])/2));
                      out.println("<line><position>" + (p.xpoints[i]+n.xpoints[j])/2 +"," + (p.ypoints[i]+n.ypoints[j])/2 + " " +
                                                    (p.xpoints[i2]+n.xpoints[j2])/2 + "," + (p.ypoints[i2]+n.ypoints[j2])/2 +
                                         "</position><above>true</above></line>");

                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  }

  //create the polygons and connections of a bridge between hex a and hex b.
  private void makeBridgeBetween(Polygon a, Polygon b) {
    //line between hexagons' midpoints
    Line2D.Double line = drawLineBetween(a, b);

    //don't make tiny bridges
    if(dist(new Point((int)line.x1, (int)line.y1), new Point((int)line.x2, (int)line.y2)) < 20) {
      return;
    }

    double lineAngle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
    double ang = lineAngle + Math.PI/2;
    double midX = (line.x2 + line.x1)/2;
    double midY = (line.y2 + line.y1)/2;

    //if a contracted version of the bridge intersects a polygon that is not
    //a or b, don't draw the bridge, as it will look funny
    Line2D.Double shortenedSide1 =
        new Line2D.Double(midX - 20*Math.cos(ang) - (line.x2 - line.x1)/4,
                          midY - 20*Math.sin(ang) - (line.y2 - line.y1)/4,
                          midX - 20*Math.cos(ang) + (line.x2 - line.x1)/4,
                          midY - 20*Math.sin(ang) + (line.y2 - line.y1)/4);
    Line2D.Double shortenedSide2 =
        new Line2D.Double(midX + 20*Math.cos(ang) - (line.x2 - line.x1)/4,
                          midY + 20*Math.sin(ang) - (line.y2 - line.y1)/4,
                          midX + 20*Math.cos(ang) + (line.x2 - line.x1)/4,
                          midY + 20*Math.sin(ang) + (line.y2 - line.y1)/4);
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      Polygon p = (Polygon)countryPolygons.get(i);
      if(p != a && p != b && (lineIntersectsPolygon(shortenedSide1, p) ||
                              lineIntersectsPolygon(shortenedSide2, p) ||
                              p.contains(shortenedSide1.x1, shortenedSide1.y1) ||
                              p.contains(shortenedSide1.x2, shortenedSide1.y2) ||
                              p.contains(shortenedSide2.x1, shortenedSide2.y1) ||
                              p.contains(shortenedSide2.x2, shortenedSide2.y2)))
        return; //bridge intersects some other polygon, don't make it
    }


    Polygon aPart = new Polygon(); //side touching a
    aPart.addPoint((int)(line.x1 + 20*Math.cos(ang)), (int)(line.y1 + 20*Math.sin(ang)));
    aPart.addPoint((int)(line.x1 - 20*Math.cos(ang)), (int)(line.y1 - 20*Math.sin(ang)));
    aPart.addPoint((int)(midX - 20*Math.cos(ang)), (int)(midY - 20*Math.sin(ang)));
    aPart.addPoint((int)(midX + 20*Math.cos(ang)), (int)(midY + 20*Math.sin(ang)));

    Polygon bPart = new Polygon(); //side touching b
    bPart.addPoint((int)(midX - 20*Math.cos(ang)), (int)(midY - 20*Math.sin(ang)));
    bPart.addPoint((int)(midX + 20*Math.cos(ang)), (int)(midY + 20*Math.sin(ang)));
    bPart.addPoint((int)(line.x2 + 20*Math.cos(ang)), (int)(line.y2 + 20*Math.sin(ang)));
    bPart.addPoint((int)(line.x2 - 20*Math.cos(ang)), (int)(line.y2 - 20*Math.sin(ang)));

    //draw brown railings on the bridges
    foreG.setColor(new Color(139, 69, 19).darker()); //brown
    foreG.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
    Point[] postBases1 = new Point[5];
    Point[] postBases2 = new Point[5];
    for(int i = 0; i < 3; i += 1) {
      postBases1[i] = new Point((int)(aPart.xpoints[0] + i*(line.x2-line.x1)/2),
                                (int)(aPart.ypoints[0] + i*(line.y2-line.y1)/2));
      postBases2[i] = new Point((int)(aPart.xpoints[1] + i*(line.x2-line.x1)/2),
                                (int)(aPart.ypoints[1] + i*(line.y2-line.y1)/2));
      foreG.drawLine(postBases1[i].x, height-postBases1[i].y, postBases1[i].x, height-(postBases1[i].y + 15));
      foreG.drawLine(postBases2[i].x, height-postBases2[i].y, postBases2[i].x, height-(postBases2[i].y + 15));
    }
    foreG.drawLine(postBases1[0].x, height-(postBases1[0].y+7), postBases1[1].x, height-(postBases1[1].y + 15));
    foreG.drawLine(postBases1[2].x, height-(postBases1[2].y+7), postBases1[1].x, height-(postBases1[1].y + 15));
    foreG.drawLine(postBases2[0].x, height-(postBases2[0].y+7), postBases2[1].x, height-(postBases2[1].y + 15));
    foreG.drawLine(postBases2[2].x, height-(postBases2[2].y+7), postBases2[1].x, height-(postBases2[1].y + 15));

    //add the polygons and connections
    bridgePolygons.add(aPart);
    bridgePolygons.add(bPart);
    Vector aConns = new Vector(), bConns = new Vector();
    aConns.add(a);
    aConns.add(bPart);
    bConns.add(b);
    bConns.add(aPart);
    bridgeConnections.add(aConns);
    bridgeConnections.add(bConns);
    ((Vector)connections.get(countryPolygons.indexOf(a))).add(aPart);
    ((Vector)connections.get(countryPolygons.indexOf(b))).add(bPart);
  }

  //output the xml of all countryside hexagon continents
  private void writeHexagons() {
    //for each castle, write the hexagons it owns
    //bonus = borders-1

    boolean[] taken = new boolean[countryPolygons.size()];
    for(int i = 0; i < taken.length; i += 1) taken[i] = false;

    for(int c = 0; c < numCastles; c += 1) {
      Point center = polygonCenter(castleCenters[c]);
      out.write("<continent>\n" +
	"  <continentname>" + countrysideNames[c] + "</continentname>\n" + 
        "  <labellocation>" + center.x + "," + (center.y-40) + "</labellocation>\n");

      int numBorders = 0;

      loop:
      for(int i = 0; i < countryPolygons.size(); i += 1) { //write out each country
        if(taken[i] || closestCastle((Polygon)countryPolygons.get(i)) != c) {
          continue loop;
        }

        taken[i] = true;

        out.write("  <country>\n" +
		"    <id>" + i + "</id>\n" +
		"    <name>" + makeHexagonName() + "</name>\n");
        Vector integerConnections = new Vector();
        boolean border = false;
        for(int j = 0; j < ((Vector)connections.get(i)).size(); j += 1) {
          if(allPolygons.contains(((Vector)connections.get(i)).get(j)) &&
             (closestCastle((Polygon)((Vector)connections.get(i)).get(j)) != c ||
              bridgePolygons.contains(((Vector)connections.get(i)).get(j)))) 
            border = true;

          int index = allPolygons.indexOf(((Vector)connections.get(i)).get(j));
          if(index != -1) {
            integerConnections.add(new Integer(index));
          }
        }
        if(border) {
          numBorders += 1;
        }

        writeConnections(integerConnections);
        writePolygon((Polygon)countryPolygons.get(i));
//		writeScenario();
		// 1.01 addition:
        out.write("  <initialOwner>"+((scenarioOwnerCount++)%6)+"</initialOwner>\n");
//        out.write("  <initialArmies>2</initialArmies>\n");
        out.write("  </country>\n");

      }
      out.write("<bonus>" + Math.round(Math.min(numBorders-1, 6)) + "</bonus>\n");
      out.write("</continent>\n");
    }

  }

  //write the xml for the castles
  //bonus = 6
  private void writeCastles() {
    for(int c = 0; c < numCastles; c += 1) {
      Point center = polygonCenter(castleCenters[c]);
      out.write("<continent>\n" +
	"  <continentname>" + castleNames[c] + "</continentname>\n" +
	"  <bonus>" + 6 + "</bonus>\n" +
        "  <labellocation>" + center.x + "," + (center.y+20) + "</labellocation>\n");

      for(int i = 0; i < 4; i += 1) {
        //put army location in a good spot
        int centerx = (int)(polygonCenter(castleCenters[c]).x + (cIR + cOR)/2*Math.sqrt(2)*Math.cos(Math.PI/4 + Math.PI/2*i));
        int centery = (int)(polygonCenter(castleCenters[c]).y + (cIR + cOR)/2*Math.sqrt(2)*Math.sin(Math.PI/4 + Math.PI/2*i));
        out.write("  <country>\n" +
		"    <id>" + (countryPolygons.size() + c*4 + i) + "</id>\n" +
		"    <name>" + makeWallName(i) + "</name>\n" +
		"    <armylocation>" + centerx + "," + centery + "</armylocation>\n");
        Vector integerConnections = new Vector();
        for(int j = 0; j < castleConnections[c][i].size(); j += 1) {
          int index = allPolygons.indexOf(castleConnections[c][i].get(j));
          if(index != -1) {
            integerConnections.add(new Integer(index));
          }
        }

        writeConnections(integerConnections);
        writePolygon(castles[c][i]);
        out.write("  <initialOwner>"+((scenarioOwnerCount++)%6)+"</initialOwner>\n");
        out.write("  </country>\n");
      }
      out.write("</continent>\n");
    }
  }

  //write the xml for the bridges
  private void writeBridges() {
    for(int i = 0; i < bridgePolygons.size(); i += 2) { //write out each country

      out.write("<continent>\n" +
	"  <continentname>Bridge</continentname>\n" +
	"  <bonus>" + -2 + "</bonus>\n");

      for(int j = 0; j < 2; j += 1) { //two halves per bridge
        out.write("  <country>\n" +
		"    <id>" + (countryPolygons.size() + numCastles*4 + i+j) + "</id>\n" +
		"    <name>Bridge</name>\n");
        Vector integerConnections = new Vector();
        for(int k = 0; k < ((Vector)bridgeConnections.get(i+j)).size(); k += 1) {
          int index = allPolygons.indexOf(((Vector)bridgeConnections.get(i+j)).get(k));
          if(index != -1) {
            integerConnections.add(new Integer(index));
          }
        }
        writeConnections(integerConnections);
        writePolygon((Polygon)bridgePolygons.get(i+j));
        out.write("  <initialOwner>"+((scenarioOwnerCount++)%6)+"</initialOwner>\n");
        out.write("  </country>\n");
      }

      out.write("</continent>\n");

    }

  }

  //finish off the theme and write it to a folder
  private void doTheme() {

    //draw castle images inside castles
    for(int i = 0; i < numCastles; i += 1) {
      drawCastleAt(polygonCenter(castleCenters[i]));
    }

    //draw houses & churches dotting the countryside
    drawCountrysideDecorations();

    backG.setColor(Color.black);
    backG.setStroke(new BasicStroke(1));
    backG.drawLine(0, 0, 0, height-1);
    backG.drawLine(1, 0, 1, height-1);
    backG.drawLine(width-1, 0, width-1, height);
    backG.drawLine(width-2, 0, width-2, height);
    backG.drawLine(width-3, 0, width-3, height);


    //now handle the file stuff
    try {
      //first delete any old theme directories
      File support = new File(new File(m.getMapGeneratorPath()).getParent());
      File[] supportChildren = support.listFiles();
      File themes = null;
      for(int i = 0; i < supportChildren.length; i += 1) {
        if(supportChildren[i].getName().toLowerCase().equals("themes")) {
          themes = supportChildren[i];
        }
      }
      File[] themesChildren = themes.listFiles();
      for(int i = 0; i < themesChildren.length; i += 1) {
        if(themesChildren[i].getName().indexOf("CastleInfinity") != -1) {
          File[] oldThemeFiles = themesChildren[i].listFiles();
          for(int j = 0; j < oldThemeFiles.length; j += 1) {
            oldThemeFiles[j].delete();
          }
          themesChildren[i].delete();
        }

      }


      //then write one for this map
      new File(themes.getPath() + File.separator + choice + seed).mkdir();
      ImageIO.write(background, "png", new File(themes + File.separator + choice+seed + File.separator + "background.png"));
      ImageIO.write(foreground, "png", new File(themes + File.separator + choice+seed + File.separator + "overground.png"));
    } catch(Exception e) {
      debug(e.toString());
    }

  }

  //draw houses and such in the countryside
  private void drawCountrysideDecorations() {
    //get the images from the Castle Lux SI foreground
    BufferedImage house1 = siForeground.getSubimage(305, 190, 40, 40);
    BufferedImage house2 = siForeground.getSubimage(775, 193, 35, 35);
    BufferedImage house3 = siForeground.getSubimage(790, 228, 35, 35);
    BufferedImage house4 = siForeground.getSubimage(65, 391, 40, 40);
    BufferedImage house5 = siForeground.getSubimage(420, 200, 52, 40);
    BufferedImage[] houses = new BufferedImage[]{house1, house2, house3, house4, house5};
    double[] maxRadii = new double[]{15, 21, 21, 15, 12};

    boolean[] used = new boolean[countryPolygons.size()];
    for(int i = 0; i < used.length; i += 1) used[i] = false;

    loop:
    for(int i = 0; i < countryPolygons.size()/2; i += 1) {
      int index;
      do {
        index = rand.nextInt(countryPolygons.size());
      } while(used[index]);

      used[index] = true;

      int houseType = rand.nextInt(houses.length);

      Point center = polygonCenter((Polygon)countryPolygons.get(index));
      double radius = 10 + rand.nextDouble()*(maxRadii[houseType]-10);
      double angle = rand.nextDouble()*Math.PI*2;
      center.x += (int)(radius*Math.cos(angle));
      center.y += (int)(radius*Math.sin(angle));
      for(int c = 0; c < numCastles; c += 1) {
        if(dist(polygonCenter(castleCenters[c]), center) < Math.sqrt(2)*cOR + 15)
          continue loop;
      }
      BufferedImage house = houses[houseType];
      foreG.drawImage(house, center.x-house.getWidth()/2, height - (center.y + house.getWidth()/2), this);
    }
  }


  private static final Point[] siCastlePoints = new Point[]{new Point(260, 100)};
  private static final int siCastleWidth = 40;
  private static final int siCastleHeight = 40;

  //draw the image of a castle at point c
  private void drawCastleAt(Point c) {
    int i = rand.nextInt(siCastlePoints.length);
    backG.setColor(Color.green.darker().darker());
    backG.fillRect(c.x - cOR, height - (c.y + cOR), 2*cOR, 2*cOR);
    foreG.drawImage(siForeground.getSubimage(siCastlePoints[i].x-siCastleWidth,
                    siCastlePoints[i].y-siCastleHeight, 2*siCastleWidth, 2*siCastleHeight), c.x-siCastleWidth, height - (c.y+siCastleHeight), this);
  }

  //remove a hexagon from the map
  private void removeHexagon(Polygon h) {
    int index = countryPolygons.indexOf(h);
    if(index != -1) {
      countryPolygons.remove(index);
      connections.remove(index);
    }
  }

  //can a hexagon be the center of a castle? only if it's far enough from the edge
  private boolean canBeCastleCenter(int index) {
    Point center = polygonCenter((Polygon)countryPolygons.get(index));

    if(center.x > hexRad*3 && center.y > hexRad*3 &&
       center.x < width-hexRad*3 && center.y < height-hexRad*3) {
      return true;
    } else {
      return false;
    }
  }

  //find the index of the closest castle to a polygon
  private int closestCastle(Polygon p) {
    int closestCastle = -1;
    double closestDist = 100000;
    for(int i = 0; i < numCastles; i += 1) {
      double dist = dist(polygonCenter(castleCenters[i]), polygonCenter(p));
      if(dist < closestDist) {
        closestDist = dist;
        closestCastle = i;
      }
    }
    return closestCastle;
  }

  //test if a line intersects any of a polygon's edges
  private boolean lineIntersectsPolygon(Line2D.Double l, Polygon p) {
    for(int i = 0; i < p.npoints; i += 1) {
      Line2D.Double l2 = new Line2D.Double(p.xpoints[i], p.ypoints[i], 
                              p.xpoints[(i+1)%p.npoints], p.ypoints[(i+1)%p.npoints]);
      if(l.intersectsLine(l2)) return true;
    }
    return false;
  }

  //draw Polygon p with g, but flip it upside-down first
  //theme and xml have different coordinate systems
  private void fillPolygon(Graphics g, Polygon p) {
    int[] ypoints = new int[p.npoints];
    for(int i = 0; i < p.npoints; i += 1) {
      ypoints[i] = height - p.ypoints[i];
    }
    g.fillPolygon(p.xpoints, ypoints, p.npoints);
  }     

  private Point midpoint(Point a, Point b) {
    return new Point((a.x + b.x)/2, (a.y + b.y)/2);
  }

  private static final String[] suffixes = new String[]{"ville", "ton", " town", " city", " village"};
  //generate a random name for a hexagon country from our list of players
  private String makeHexagonName() {
    return names[rand.nextInt(names.length)] + suffixes[rand.nextInt(suffixes.length)];
  }

  //random castle name
  private String makeCastleName() {
    return new String(new char[]{Character.toUpperCase(consonants[rand.nextInt(numConsonants)]),
                                  vowels[rand.nextInt(numVowels)],
                                  consonants[rand.nextInt(numConsonants)],
                                  vowels[rand.nextInt(numVowels)],
                                  consonants[rand.nextInt(numConsonants)],
                                  vowels[rand.nextInt(numVowels)],
                                  consonants[rand.nextInt(numConsonants)]}) + " Castle";
  }

  //random countryside name
  private String makeCountrysideName(String castleName) {
    String[] types = new String[]{"Lowlands", "Highlands", "Forest", "Countryside", "Plains", "Hills", "Valley"};
    return castleName + " " + types[rand.nextInt(types.length)];
  }

  private String makeWallName(int wall) {
    switch(wall) {
      case 0:
        return "NE Wall";
      case 1:
        return "NW Wall";
      case 2:
        return "SW Wall";
      case 3:
        return "SE Wall";
    }
    return "Castle Wall";
  }

  //make a hexagon centered on point p
  private Polygon hexagonAround(Point p) {
    int[] xpoints = new int[6];
    int[] ypoints = new int[6];
    for(int i = 0; i < 6; i += 1) {
      xpoints[i] = (int)(p.x + (hexRad-1)*Math.cos(Math.PI/6 + i*Math.PI/3));
      ypoints[i] = (int)(p.y + (hexRad-1)*Math.sin(Math.PI/6 + i*Math.PI/3));
    }
    return new Polygon(xpoints, ypoints, 6);
  }

  //find the center of gravity of a polygon's points
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



  //draw a line between the polygons with endpoints on their perimeters
  //we do this through a binary search for their boundaries
  private Line2D.Double drawLineBetween(Polygon a, Polygon b) {
    Point ca = polygonCenter(a);
    Point cb = polygonCenter(b);

    double theta = Math.atan2(ca.y - cb.y, ca.x - cb.x);

    double radius = dist(ca, cb);
    double movement = radius/2;
    for(int i = 0; i < 20; i += 1) {
      ca = new Point((int)(cb.x + radius*Math.cos(theta)), (int)(cb.y + radius*Math.sin(theta)));
      if(a.contains(ca)) {
        radius -= movement;
      } else {
        radius += movement;
      }
      movement /= 2;
    }
    radius += 5; //make sure the line isn't detached
    ca = new Point((int)(cb.x + radius*Math.cos(theta)), (int)(cb.y + radius*Math.sin(theta)));

    theta += Math.PI;

    radius = dist(ca, cb);
    movement = radius/2;
    for(int i = 0; i < 20; i += 1) {
      cb = new Point((int)(ca.x + radius*Math.cos(theta)), (int)(ca.y + radius*Math.sin(theta)));
      if(b.contains(cb)) {
        radius -= movement;
      } else {
        radius += movement;
      }
      movement /= 2;
    }
    radius += 5; //make sure the line isn't detached
    cb = new Point((int)(ca.x + radius*Math.cos(theta)), (int)(ca.y + radius*Math.sin(theta)));

    return new Line2D.Double(ca, cb);
  }

  //write the xml representation of a country polygon
  private void writePolygon(Polygon p) {
    out.write("    <polygon>");
    for(int i = 0; i < p.npoints; i += 1) {
      out.write("" + p.xpoints[i] + "," + p.ypoints[i] + " ");
    }
    out.write("</polygon>\n");

  }

  //write the xml representation of a country's adjoining list
  private void writeConnections(Vector conns) {
    out.write("    <adjoining>");
    for(int i = 0; i < conns.size()-1; i += 1) {
      out.write("" + (Integer)conns.get(i) + ",");
    }
    if(conns.size() > 0) out.write("" + (Integer)conns.get(conns.size()-1));
    out.write("</adjoining>\n");
  }

  //write the xml representation of a line
  private void writeLine(Line2D.Double l) {
    out.write("<line><position>" + (int)l.x1 + "," + (int)l.y1 + " " +
                                   (int)l.x2 + "," + (int)l.y2 + "</position></line>\n");
  }

  //dilate p by ratio ratio
  private Polygon dilatePolygon(Polygon p, double ratio) {
    Point c = polygonCenter(p);
    int[] xpoints = new int[p.npoints];
    int[] ypoints = new int[p.npoints];
    for(int i = 0; i < p.npoints; i += 1) {
       xpoints[i] = (int)(c.x + ratio*(p.xpoints[i] - c.x));
       ypoints[i] = (int)(c.y + ratio*(p.ypoints[i] - c.y));
    }
    return new Polygon(xpoints, ypoints, p.npoints);
  }

  //distance between two points
  private double dist(Point a, Point b) {
    return Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y));
  }

  //interface function. should be true on release
  public boolean canCache() {
    return false;
  }

  public String description() {
    return "CastleInfinity is a random map generator that makes maps in the style of Castle Lux SI, complete with a similar theme. Note that the randomly generated themes will only be seen by the host in network games.\n\nCredit for the graphics from Castle Lux SI goes to Mark Bauer.";  
  }

  //user size choices
  static final String CHOICE_SMALL = "CastleInfinity - small";
  static final String CHOICE_NORMAL = "CastleInfinity - normal";
  static final String CHOICE_BIG = "CastleInfinity - big";

  public java.util.List getChoices() {
    Vector v = new Vector();
    v.add(CHOICE_SMALL);
    v.add(CHOICE_NORMAL);
    v.add(CHOICE_BIG);
    return v;
  }

  //interface function; unused
  public String message(String message, Object data) {
   	if ("scenarioPlayerCount".equals(message))
		{
		return "6";
		}

    return "";
  }

  public String name() {
    return "CastleInfinity";
  }

  public float version() {
    return 1.0f;
  }

  private void debug(String s) {
    System.out.println(":" + s);
  }

  //list of player names for naming hexagon countries, taken from http://sillysoft.net/lux/rankings/wins/alltime.php on June 14, 2006
  String[] names = new String[]{"shopi","Loki","magpie","upeng2005","jOnNiE","Smedz","paranoiarodeo","mercer","obiwan","General K","tfPunx","Punkee.Munkee",
"MrBuckin the Impaler","Brewster","Jerry T","DR.Zuss","documan","what? oh yeah.","MASSHOLE","Darth Rellek","Mouldy Dog","PJR","'84 Tigers","Dad",
"JadePathMan","dustin","Gabo","michelle","Alexander_the_Great","futurist","Gryffindor","KingPatrick","shalafarky","bstevens","vargas","Yo Daddy",
"gary the cheater","shock-n-ya'll","stevedip","Grozoth","Mud","fink","Natya","mood in dhingo","fishflakes","Preacherman","mikey","Zo‘","kevquincey","Baden","Chumdinger","Nietzsche",
"General Zod","Pod","Jman","Fane","microtonal","stanski","furball","Golgi","King of Lux","glowextreme","SecondTermMistake","Sexy Lady","Lord of War",
"SET","The Adrianator","Erik","ryanfaintich","h0b0","rob","zimbabwe","pcristov","SnyperEye","Kef","underdog","AquaRegia","TERMINATOR","Gav","Atom","DL",
"D.I.T","Rocco Rompomuro","Autoplay","SexBomba","pillow","Jormanainen","marvin","Sire","tmiso","Knut Olaf","Charles de Gaulle","Muskie","primaimm",
"Laney6969","Pwn","the tide","Big R","WaR_LoRd","One Big Wave","magpie","upeng2005","paranoiarodeo","General K","Jerry T","Smedz","DR.Zuss","General Zod",
"'84 Tigers","Darth Rellek","MrBuckin the Impaler","Mouldy Dog","SET","jOnNiE","Nietzsche","ryanfaintich","Mud","Fane","Gabo","Yo Daddy","Grozoth","fink",
"shock-n-ya'll","furball","michelle","One Big Wave","pillow","SecondTermMistake","Lord of War","microtonal","glowextreme","Alexander_the_Great","hoodie",
"SnyperEye","Baden","stevedip","Punkee.Munkee","AquaRegia","futurist","Laney6969","kevquincey","PSU7685","documan","D.I.T","pcristov","marvin","WaR_LoRd",
"Usternchen","the tide","MinorKing","Tracker","stanski","Sire","dustin","pale kate","Gav","mercer","MadDaz","PJR","Golgi","I'm Your Huckleberry",
"SexBomba","primaimm","mood in dhingo","Muskie","Mattman160","cheech99","Black Pope","matthiasmay","Kleddiator","Toddly","Vidar","what? oh yeah.",
"Brewster","Drifter","underdog","Sexy Lady","Iggy","Mach","TheMant","gibraltar monkey","Xanadu","mic","Loki","JGRNAUT","bennok","Scepter","Cpt Mandrake",
"Gryffindor","nevir","Firefly","Sidhe","KingPatrick","Frodo","Soca Warrior","Blind Willie","boris","Blitzspeare","h0b0","Dangerous Beans","dustin","dustin","dustin","dustin"};

  //a method we have to have to draw images
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {return false;}
}