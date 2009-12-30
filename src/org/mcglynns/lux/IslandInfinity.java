//////////////////////////////////////////
//   IslandInfinity.java
//  Lux Map Generator
//  Makes maps consisting of groups of connected islands
//
//  Version: 1.0
//  Date last modified: 5/28/2006
//  By: Greg McGlynn
//////////////////////////////////////////

package org.mcglynns.lux;

import com.sillysoft.lux.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class IslandInfinity implements LuxMapGenerator {

  Random rand;

  //used in making names
  static char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
                                        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'};
  static char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
  static int numConsonants = consonants.length;
  static int numVowels = vowels.length;

  Vector countryPolygons; //shapes of the countrys
  Vector connections;     //vector of vectors of integers that say which countries a country connects to

  //the game calls this when it wants a map
  //write a map xml to out
  public boolean generate(PrintWriter out, String choice, int seed, MapLoader m) {
    //seed must completely determine our map
    rand = new Random(seed);

    countryPolygons = new Vector();
    connections = new Vector();

    //set size & conts based on user choice
    int width = 600;
    int height = 400;
    int numContinents = 6;
    if(choice == CHOICE_BIG) {
      width = 700;
      height = 480;
      numContinents = 10;
    } else if(choice == CHOICE_HUGE) {
      width = 800;
      height = 550;
      numContinents = 17;
    } else if (choice == CHOICE_REALLYHUGE) {
      width = 1100;
      height = 700;
      numContinents = 25;
    }
    int contVariation = numContinents/6; //numContinents can vary (+/- 1/6)
    numContinents += rand.nextInt(2*contVariation+1)-contVariation;

    //the header of our xml file
    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              "<theme>Ocean</theme>\n" +
              "<author>IslandInfinity Generator (by Greg McGlynn)</author>\n" +
              "<email>greg@mcglynns.org</email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + choice + " #" + seed + "</title>\n" +
              "<description>This map was made by the IslandInfinity LuxMapGenerator. Type: " + choice +
                       ",  Number of continents: " + numContinents  + "</description>\n");


    debug("placing continents...");
    //determine where our continents will go
    Rectangle[] contBounds = placeContinents(numContinents, width, height);
    Point[] contCenters = getContCenters(contBounds);

    int[] contIndices = new int[numContinents+1]; //the starting index of each cont's countries in the Vectors
    for(int i = 0; i < numContinents; i += 1) {
      debug("new cont: " + i);
      contIndices[i] = countryPolygons.size();
      addNewContinent(contBounds[i]); //determine the shapes and connections of the cont's countries
    }
    contIndices[numContinents] = countryPolygons.size();

    debug("Making connections...");
    //connect up the continents:
    Vector lines = makeConnectionsBetweenContinents(contIndices, contCenters, contBounds, numContinents);

    //now write the continents to out
    for(int i = 0; i < numContinents; i += 1) {
      debug("writing cont: " + i);
      int start = contIndices[i];
      int end = contIndices[i+1];

      writeContinent(start, end, out);
    }

    debug("drawing lines");
    //write the lines representing intercontinent connections
    for(int i = 0; i < lines.size(); i += 1) {
      writeLine(out, (Line2D.Double)lines.get(i));
    }

    //done
    debug("done");
    out.write("</luxboard>");

    return true;
  }

  //this function places numContiennts rectangles in a box of size width-height
  //so that none overlap. we use this to choose where our continents will go
  private Rectangle[] placeContinents(int numContinents, int width, int height) {
    //minimum and maximum sizes of the rectangles
    int minWidth = 75;
    int minHeight = 60;
    int maxWidth = 150;
    int maxHeight = 120;

    contplacement:
    while(true) {  //loops until we have a valid arrangement
      Rectangle[] ret = new Rectangle[numContinents];

      for(int i = 0; i < numContinents; i += 1) { //place the conts one at a time
        boolean success = false; //whether we successfully placed this cont

        placetry:
        for(int k = 0; k < 15; k += 1) { //try to place this cont 15 times
          int contWidth = rand.nextInt(maxWidth-minWidth) + minWidth;
          int contHeight = rand.nextInt(maxHeight-minHeight) + minHeight;
          int x = rand.nextInt(width - contWidth);
          int y = rand.nextInt(height - contHeight);
          ret[i] = new Rectangle(x, y, contWidth, contHeight);

          //expandedI creates a buffer between continents
          Rectangle expandedI = new Rectangle(x-20, y-20, contWidth+40, contHeight+40);
          for(int j = 0; j < i; j += 1) {
            if(ret[j].intersects(expandedI))  //overlap
              continue placetry; //try again...
          }
          success = true; //no intersections
          break; //success in placing this cont
        }
        if(!success) continue contplacement; //couldn't place this cont, start over
      }

      return ret; //made it through all conts: done

    }
  }

  //determines the centerpoint of each continent
  private Point[] getContCenters(Rectangle[] contBounds) {
    Point[] ret = new Point[contBounds.length];
    for(int i = 0; i < ret.length; i += 1) {
      ret[i] = new Point(contBounds[i].x + contBounds[i].width/2,
                         contBounds[i].y + contBounds[i].height/2);
    }
    return ret;
  }

  //pick a name for a continent (consonant - vowel - consonant)
  private String makeContinentName() {
    return new String(new char[]{Character.toUpperCase(consonants[rand.nextInt(numConsonants)]),
                                  vowels[rand.nextInt(numVowels)],
                                  consonants[rand.nextInt(numConsonants)]});
  }

  //pick a name for a country (contName - vowel - consonant)
  private String makeCountryName(String continentName) {
    return continentName + new String(new char[]{vowels[rand.nextInt(numVowels)],
                                                 consonants[rand.nextInt(numConsonants)]});
  }

  //output the xml of a continent
  //start and end are indices in the polygon and connection Vectors
  private void writeContinent(int start, int end, PrintWriter out) {

    String continentName = makeContinentName();
    int countries = end-start;

    int numBorders = 0;
    for(int i = start; i < end; i += 1) {
      Vector theseConns = (Vector)connections.get(i);
      boolean isABorder = false;
      for(int j = 0; j < theseConns.size(); j += 1) {
        int connIndex = ((Integer)theseConns.get(j)).intValue();
        if(connIndex < start || connIndex >= end) {
          isABorder = true;
        }
      }
      if(isABorder) numBorders += 1;
    }

    int bonus = numBorders;

    out.write("<continent>\n" +
	"  <continentname>" + continentName + "</continentname>\n" +
	"  <bonus>" + bonus + "</bonus>\n");

    for(int i = start; i < end; i += 1) { //write out each country
      out.write("  <country>\n" +
		"    <id>" + i + "</id>\n" +
		"    <name>" + makeCountryName(continentName) + "</name>\n");
      writeConnections(out, (Vector)connections.get(i));
      writePolygon(out, (Polygon)countryPolygons.get(i));
      out.write("  </country>\n");
    }
    out.write("</continent>\n");

  }

  //continent shapes
  static final int CONT_RECT = 0;
  static final int CONT_ELLIPSE = 1;
  static final int numContTypes = 2;

  //set up a continent's interior: countries and connections
  private void addNewContinent(Rectangle bounds) {
    //pick a shape:
    int contType = rand.nextInt(numContTypes);

    int indexStart = countryPolygons.size(); //starting index in the Vectors

    //countries in continent = area/2000 +/- 1
    int area = bounds.width*bounds.height;
    int countries = area/2000 + rand.nextInt(3)-1;
    if(countries < 1) countries = 1;

    for(int i = 0; i < countries; i += 1) {
      connections.add(new Vector());
    }

    //each country has a "center." the country consists of the locus of points
    //within the continent boundary and closer to the country's center than to
    //any other country's center. this means that boundaries consist of the cont
    //boundaries and the perpendicular bisectors of segments between close centers

    //pick center locations, ensuring that no two are two close,
    //as this leads to tiny countries
    Point[] centers = new Point[countries];
    double minDist = Math.sqrt(area/(double)countries - 750);
    boolean done = false;
    centerplacement:
    while(!done) {
      for(int i = 0; i < countries; i += 1) {
        centers[i] = getPointInContinent(bounds, contType);
        for(int j = 0; j < i; j += 1) {
          if(dist(centers[i], centers[j]) < minDist) continue centerplacement;
        }
      }
      done = true;
    }

    //now create the country polygons. these consist of 100 points. the points have
    //2*pi/100 radians between them angularly, but can be at different radii.
    Polygon[] polygons = new Polygon[countries];
    for(int i = 0; i < countries; i += 1) {

      polygons[i] = new Polygon();
      for(double theta = 0; theta < 2*Math.PI; theta += Math.PI/50) {
        Point borderPoint = getPointOnContinentBorder(centers[i], theta, bounds, contType);

        int lastNeighbor = -1;
        //now cycle through the other centers and make sure this point is closer to
        //this center than any other
        for(int p = 0; p < countries; p += 1) {
          if(dist(borderPoint, centers[p]) < dist(borderPoint, centers[i])) {
            lastNeighbor = p;

            int radius = (int)dist(borderPoint, centers[i]);
            int movement = radius/2;
            for(int j = 0; j < 40; j += 1) {
              borderPoint = new Point((int)(centers[i].x + radius*Math.cos(theta)),
                                      (int)(centers[i].y + radius*Math.sin(theta)));
              if(dist(borderPoint, centers[p]) < dist(borderPoint, centers[i])) {
                radius -= movement;
              } else {
                radius += movement;
              }
              movement /= 2;
            }

          }

        }
        //if this point is a boundary between two countries, make a connection between them
        if(lastNeighbor != -1) {
          ((Vector)connections.get(indexStart + lastNeighbor)).add(new Integer(indexStart + i));
          ((Vector)connections.get(indexStart + i)).add(new Integer(indexStart + lastNeighbor));
        }

        polygons[i].addPoint(borderPoint.x, borderPoint.y);
      }
//      if(((Vector)connections.get(i)).size() == 0) debug("didn't connect " + i + " to anything");
//      else debug("" + i + " connects to " + ((Integer)((Vector)connections.get(i)).get(0)).intValue());

      countryPolygons.add(polygons[i]);

    }
  }

  //pick a random point within the given cont shape
  private Point getPointInContinent(Rectangle bounds, int contType) {
    if(contType == CONT_RECT) {
      return new Point(bounds.x + rand.nextInt(bounds.width),
                       bounds.y + rand.nextInt(bounds.height));
    } else if(contType == CONT_ELLIPSE) {
      while(true) {
        Point ret = new Point(bounds.x + rand.nextInt(bounds.width),
                              bounds.y + rand.nextInt(bounds.height));
        if(new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height).contains(ret)) return ret;
      }
    }

    debug("UNKNOWN CONT TYPE");
    return null;
  }

  //find the point on the continent border with the given angle to center.
  //we do this by a binary search for the border
  private Point getPointOnContinentBorder(Point center, double theta, Rectangle border, int contType) {

    if(!border.contains(center)) debug("border doesn't contain center!");

    if(contType == CONT_RECT || contType == CONT_ELLIPSE) {
      RectangularShape rs = null;
      if(contType == CONT_RECT) {
        rs = border;
      } else if(contType == CONT_ELLIPSE) {
        rs = new Ellipse2D.Double(border.x, border.y, border.width, border.height);
      }

      int radius = 1000;
      Point ret = null;
      int movement = 500;
      for(int i = 0; i < 20; i += 1) {
        ret = new Point((int)(center.x + radius*Math.cos(theta)), (int)(center.y + radius*Math.sin(theta)));

        if(rs.contains(ret)) {
          radius += movement;
        } else {
          radius -= movement;
        }
        movement /= 2;
      }

      return ret;
    }

    debug("GPOCB: unkown cont type");
    return null;

  }

  //connect up the continents so that any country can reach any other country.
  //the algorithm is to make the shortest valid connection at each step until
  //all continents are reacheable. we return a Vector of lines that should be
  //drawn representing the connections we made
  private Vector makeConnectionsBetweenContinents(
              int[] contIndices, Point[] contCenters, Rectangle[] contBounds, int numContinents) {
    //an array saying whether there is a direct connection between any two continents
    boolean[][] contsConnected = new boolean[numContinents][numContinents];
    for(int i = 0; i < numContinents; i += 1) {
      for(int j = 0; j < numContinents; j += 1) {
        contsConnected[i][j] = false;
      }
    }

    Vector lines = new Vector();

    //connect continents until they are all connected
    while(!allContsReachableFrom(0, contsConnected, numContinents)) {
      double shortestGap = 10000;
      int contA = -1;
      int contB = -1;
      for(int i = 0; i < numContinents; i += 1) {
        for(int j = 0; j < numContinents; j += 1) {
          if(!contsConnected[i][j] && contsConnectable(i, j, contBounds, contCenters, numContinents)) {
            double gap = dist(contCenters[i], contCenters[j]);
            if(gap < shortestGap) {
              shortestGap = gap;
              contA = i;
              contB = j;
            }
          }
        }
      }
      contsConnected[contA][contB] = true;
      contsConnected[contB][contA] = true;
      lines.add(connectContinents(contA, contB, contIndices));
    }

    for(int i = 0; i < numContinents; i += 1) {
      int connectedConts = 0;
      for(int j = 0; j < numContinents; j += 1) {
        if(contsConnected[i][j]) connectedConts += 1;
      }

      debug("connectedConts[" + i + "] = " + connectedConts);

      if(connectedConts == 2) { //dead end
        double shortestGap = 10000;
        int contB = -1;
        for(int j = 0; j < numContinents; j += 1) {
          if(!contsConnected[i][j] && contsConnectable(i, j, contBounds, contCenters, numContinents)) {
            double gap = dist(contCenters[i], contCenters[j]);
            if(gap < shortestGap) {
              shortestGap = gap;
              contB = j;
            }
          }
        }

        debug("XXXXXXXXXXXX");
        if(contB != -1) {
          lines.add(connectContinents(i, contB, contIndices));
          contsConnected[i][contB] = true;
          contsConnected[contB][i] = true;
        }
      }
    }

    return lines;
  }

  //can we connect contA and contB? no if the resulting line would cross another continent
  private boolean contsConnectable(int contA , int contB, Rectangle[] contBounds, Point[] contCenters, int numContinents) {
    Line2D.Double l = new Line2D.Double(contCenters[contA], contCenters[contB]);
    for(int i = 0; i < numContinents; i += 1) {
      if(i != contA && i != contB) {
        if(l.intersects(contBounds[i])) return false;
      }
    }
    return true;
  }

  //can all continents be reached from start?
  private boolean allContsReachableFrom(int start, boolean[][] contsConnected, int numContinents) {
    boolean[] reachedAlready = new boolean[numContinents];
    for(int i = 0; i < numContinents; i += 1) {
      reachedAlready[i] = false;
    }
    reachedAlready[start] = true;

    int numReached = 1;
    for(int i = 0; i < numContinents; i += 1) {
      if(contsConnected[start][i] && i != start) {
        reachedAlready = whichContsReachableFrom(i, contsConnected, reachedAlready, numContinents);
      }
    }
    for(int i = 0; i < numContinents; i += 1) {
      if(!reachedAlready[i]) return false;
    }
    return true;
  }

  //which continents can be reached from start?
  private boolean[] whichContsReachableFrom(
               int start, boolean[][] contsConnected, boolean[] reachedAlready, int numContinents) {
    int numReached = 1;
    reachedAlready[start] = true;
    for(int i = 0; i < numContinents; i += 1) {
      if(contsConnected[start][i] && !reachedAlready[i]) {
        reachedAlready = whichContsReachableFrom(i, contsConnected, reachedAlready, numContinents);
      }
    }
    return reachedAlready;
  }

  //connect cont1 and cont2, adding the connection to the connections Vector
  //and returning the line to draw on the board
  private Line2D.Double connectContinents(int cont1, int cont2, int[] contIndices) {
    int bestPolygon1 = -1;
    int bestPolygon2 = -1;
    double bestLength = 10000;
    for(int j = contIndices[cont1]; j < contIndices[cont1+1]; j += 1) {
      for(int k = contIndices[cont2]; k < contIndices[cont2+1]; k += 1) {
        Point a = polygonCenter((Polygon)countryPolygons.get(j));
        Point b = polygonCenter((Polygon)countryPolygons.get(k));
        double dist = dist(a, b);
        if(dist < bestLength) {
          bestLength = dist;
          bestPolygon1 = j;
          bestPolygon2 = k;
        }
      }
    }

    ((Vector)connections.get(bestPolygon1)).add(new Integer(bestPolygon2));
    ((Vector)connections.get(bestPolygon2)).add(new Integer(bestPolygon1));

    return drawLineBetween((Polygon)countryPolygons.get(bestPolygon1),
                           (Polygon)countryPolygons.get(bestPolygon2));
  }

  //write the xml representation of a country polygon
  private void writePolygon(PrintWriter out, Polygon p) {
    out.write("<polygon>");
    for(int i = 0; i < p.npoints; i += 1) {
      out.write("" + p.xpoints[i] + "," + p.ypoints[i] + " ");
    }
    out.write("</polygon>\n");

  }

  //write the xml representation of a country's adjoining list
  private void writeConnections(PrintWriter out, Vector conns) {
    out.write("<adjoining>");
    for(int i = 0; i < conns.size()-1; i += 1) {
      out.write("" + (Integer)conns.get(i) + ",");
    }
    if(conns.size() > 0) out.write("" + (Integer)conns.get(conns.size()-1));
    out.write("</adjoining>\n");
  }

  //write the xml representation of a line
  private void writeLine(PrintWriter out, Line2D.Double l) {
    out.write("<line><position>" + (int)l.x1 + "," + (int)l.y1 + " " +
                                   (int)l.x2 + "," + (int)l.y2 + "</position></line>\n");
  }

  //distance between two points
  private double dist(Point a, Point b) {
    return Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y));
  }

  //find the center of gravity of a polygon
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

  //interface function. should be true on release
  public boolean canCache() {
    return true;
  }

  public String description() {
    return "Makes maps consisting of island continents.";
  }

  //user size choices
  static final String CHOICE_NORMAL = "IslandInfinity - Hawaii";
  static final String CHOICE_BIG = "IslandInfinity - normal";
  static final String CHOICE_HUGE = "IslandInfinity - big";
  static final String CHOICE_REALLYHUGE = "IslandInfinity - huge";

  public java.util.List getChoices() {
    Vector v = new Vector();
    v.add(CHOICE_NORMAL);
    v.add(CHOICE_BIG);
    v.add(CHOICE_HUGE);
    v.add(CHOICE_REALLYHUGE);
    return v;
  }

  //interface function; unused
  public String message(String message, Object data) {
  	if ("scenarioPlayerCount".equals(message))
		{
		return "6";
		}
	return null;
  }

  public String name() {
    return "IslandInfinity";
  }

  public float version() {
    return 1.1f;
  }

  private void debug(String s) {
//    System.out.println(":" + s);
  }
}