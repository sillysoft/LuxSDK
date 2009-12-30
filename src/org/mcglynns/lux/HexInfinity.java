//////////////////////
//HexInfinity Lux Map Generator
//Generates maps for Lux like those of the Hex series
//Greg McGlynn
//////////////////////

package org.mcglynns.lux;

import com.sillysoft.lux.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

public class HexInfinity implements LuxMapGenerator {

  static Rectangle playerInfoRect;

  //used in making names
  static char[] consonants = new char[]{'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
                                        'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'z'};
  static char[] vowels = new char[]{'a', 'e', 'i', 'o', 'u'};
  static int numConsonants = consonants.length;
  static int numVowels = vowels.length;

  Random rand;

  int width, height, numContinents;

  Vector[] continents;
  Vector connections;
  Vector countryPolygons;
  Point[] contCenters;

  int[] contIndices;

  static final double tRad = 30;
  static final double tHeight = 3*tRad/2;
  static final double tWidth = Math.sqrt(3)*tRad;

  PrintWriter out;

  public boolean generate(PrintWriter out, String choice, int seed, MapLoader loader) {

    rand = new Random(seed);

    this.out = out;

    int variation;
    if(choice == CHOICE_SMALL) {
      width = 600;
      height = 400;
      numContinents = 8;
      variation = 1;
    } else if(choice == CHOICE_MEDIUM) {
      width = 750;
      height = 500;
      numContinents = 12;
      variation = 2;
    } else if(choice == CHOICE_LARGE) {
      width = 900;
      height = 600;
      numContinents = 18;
      variation = 3;
    } else { //(choice == CHOICE_HUGE)
      width = 1000;
      height = 700;
      numContinents = 28;
      variation = 4;
    }
    



    numContinents += rand.nextInt(2*variation + 1) - variation;

    contCenters = new Point[numContinents];

    continents = new Vector[numContinents];

    loader.setLoadText("Creating "+choice + " #" + seed+" map....");

    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              "<theme>Air</theme>\n" +
              "<author>HexInfinity Generator (by Greg McGlynn)</author>\n" +
              "<email>greg@mcglynns.org</email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + choice + " #" + seed + "</title>\n" +
              "<description>This map was made by the HexInfinity LuxMapGenerator. Type: " + choice +
                       ",  Number of continents: " + numContinents  + "</description>\n");

	int lineOffsetY = 116;
	int playerInfoRectWidth = 283;
	if (System.getProperty("mrj.version") == null)
		{ // windows
		lineOffsetY = 120;
		playerInfoRectWidth = 285;
		}


    if(loader.isPlayerInfoInsideMap()) {
      playerInfoRect = new Rectangle(0, height-145, playerInfoRectWidth, 145);
      out.write("<line><position>0," + (height-lineOffsetY) + " "+playerInfoRectWidth+"," + (height-lineOffsetY) + "</position><width>3</width><above>true</above></line>\n");
      out.write("<line><position>"+playerInfoRectWidth+"," + height + " "+playerInfoRectWidth+"," + (height-lineOffsetY) + "</position><width>3</width><above>true</above></line>\n");
    } else {
      playerInfoRect = new Rectangle(0, 0, 0, 0);
    }

    debug("making seeds");
    makeContinentSeeds();

    growContinents();

    countryPolygons = new Vector();
    connections = new Vector();
    contIndices = new int[numContinents+1];
    for(int i = 0; i < numContinents; i += 1) {
      contIndices[i] = countryPolygons.size();
      for(int j = 0; j < continents[i].size(); j += 1) {
        countryPolygons.add((Polygon)continents[i].get(j));
        connections.add(new Vector());
      }

      for(int j = contIndices[i]; j < countryPolygons.size(); j += 1) {
        for(int k = contIndices[i]; k < countryPolygons.size(); k += 1) {
          if(j != k && dist(polygonCenter((Polygon)countryPolygons.get(j)),
                            polygonCenter((Polygon)countryPolygons.get(k))) < tRad*1.5) {
            ((Vector)connections.get(j)).add(new Integer(k));
          }
        }
      }
    }
    contIndices[numContinents] = countryPolygons.size();

    debug("connecting continents");
    Vector lines = makeConnectionsBetweenContinents();
    debug("writing...");

    for(int i = 0; i < numContinents; i += 1) {
      writeContinent(i);
    }

    for(int i = 0; i < lines.size(); i += 1) {
      writeLine((Line2D.Double)lines.get(i));
    }

    out.write("</luxboard>\n");

    return true;
  }

  //output the xml of a continent
  //start and end are indices in the polygon and connection Vectors
  private void writeContinent(int cont) {
    int start = contIndices[cont];
    int end = contIndices[cont+1];

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

    int bonus = (int)Math.round((numBorders + countries/3.0)/2);

    out.write("<continent>\n" +
	"  <continentname>" + continentName + "</continentname>\n" +
	"  <bonus>" + bonus + "</bonus>\n");

    for(int i = start; i < end; i += 1) { //write out each country
      out.write("  <country>\n" +
		"    <id>" + i + "</id>\n" +
		"    <name>" + makeCountryName(continentName) + "</name>\n");
      out.write("    <initialOwner>" + rand.nextInt(6) + "</initialOwner>");

      writeConnections((Vector)connections.get(i));
      writePolygon((Polygon)countryPolygons.get(i));
      out.write("  </country>\n");
    }
    out.write("</continent>\n");

  }

  private void makeContinentSeeds() {
    double minDist = 100;

    contCenters = new Point[numContinents];

    contplacement:
    while(true) {  //loops until we have a valid arrangement

      for(int i = 0; i < numContinents; i += 1) { //place the conts one at a time
        boolean success = false; //whether we successfully placed this cont

        placetry:
        for(int k = 0; k < 15; k += 1) { //try to place this cont 15 times
          contCenters[i] = new Point(50+rand.nextInt(width-100), 50+rand.nextInt(height-100));

          if(playerInfoRect.contains(contCenters[i])) continue placetry;

          //expandedI creates a buffer between continents
          for(int j = 0; j < i; j += 1) {
            if(dist(contCenters[j], contCenters[i]) < minDist)  //overlap
              continue placetry; //try again...
          }
          success = true; //no intersections
          break; //success in placing this cont
        }
        if(!success) continue contplacement; //couldn't place this cont, start over
      }

      break; //made it through all conts: done

    }

    for(int i = 0; i < numContinents; i += 1) {
      continents[i] = new Vector();
      continents[i].add(upTriangleAround(contCenters[i]) );
    }
  }

  private void growContinents() {
    for(int i = 0; i < 50; i += 1) {
      for(int j = 0; j < numContinents; j += 1) {
        addOneCountryToContinent(rand.nextInt(numContinents));
      }
    }
  }

  private boolean addOneCountryToContinent(int cont) {
    trytoplace:
    for(int i = 0; i < 40; i += 1) {
      int parentIndex = rand.nextInt(continents[cont].size());
      Polygon parent = (Polygon)continents[cont].get(parentIndex);
      Polygon child = makeAdjoiningTriangle(parent);

      for(int j = 0; j < numContinents; j += 1) {
        for(int k = 0; k < continents[j].size(); k += 1) {
          //debug("child = " + child + ", continents[j] = " + continents[j] + ", continents = " = continents);
          if(trianglesIntersect(dilatePolygon(child, (cont == j ? 1.0: 1.2)),
                                dilatePolygon((Polygon)continents[j].get(k), (cont==j ? 1.0 : 1.2))) ||
             !polygonFitsOnScreen(child) ||
             (j != cont && dist(polygonCenter(child), polygonCenter((Polygon)continents[j].get(k))) < tRad*2))
            continue trytoplace;
        }
      }

      //valid, add it
      continents[cont].add(child);
      return true;
    }
    return false;
  }

  private Polygon makeAdjoiningTriangle(Polygon p) {
    Polygon ret = new Polygon();
    if(trianglePointsUp(p)) {
      switch(rand.nextInt(3)) {
        case 0:
          ret.addPoint(p.xpoints[0], (int)(p.ypoints[0]-2*tHeight));
          ret.addPoint(p.xpoints[1], p.ypoints[1]);
          ret.addPoint(p.xpoints[2], p.ypoints[2]);
          ret.translate(0, -5);
          return ret;
        case 1:
          ret.addPoint(p.xpoints[1], p.ypoints[1]);
          ret.addPoint(p.xpoints[1]-(int)(tWidth/2), p.ypoints[0]);
          ret.addPoint(p.xpoints[0], p.ypoints[0]);
          ret.translate(-5, 0);
          return ret;
        case 2:
          ret.addPoint(p.xpoints[2], p.ypoints[2]);
          ret.addPoint(p.xpoints[0], p.ypoints[0]);
          ret.addPoint(p.xpoints[2]+(int)(tWidth/2), p.ypoints[0]);
          ret.translate(5, 0);
          return ret;
      }
    } else {
      switch(rand.nextInt(3)) {
        case 0:
          ret.addPoint(p.xpoints[0], p.ypoints[0]+(int)(2*tHeight));
          ret.addPoint(p.xpoints[1], p.ypoints[1]);
          ret.addPoint(p.xpoints[2], p.ypoints[2]);
          ret.translate(0, 5);
          return ret;
        case 1:
          ret.addPoint(p.xpoints[1], p.ypoints[1]);
          ret.addPoint(p.xpoints[1]-(int)(tWidth/2), p.ypoints[0]);
          ret.addPoint(p.xpoints[0], p.ypoints[0]);
          ret.translate(-5, 0);
          return ret;
        case 2:
          ret.addPoint(p.xpoints[2], p.ypoints[2]);
          ret.addPoint(p.xpoints[0], p.ypoints[0]);
          ret.addPoint(p.xpoints[2]+(int)(tWidth/2), p.ypoints[0]);
          ret.translate(5, 0);
          return ret;
      }
    }

    return null;
  }

  //connect up the continents so that any country can reach any other country.
  //the algorithm is to make the shortest valid connection at each step until
  //all continents are reacheable. we return a Vector of lines that should be
  //drawn representing the connections we made
  private Vector makeConnectionsBetweenContinents() {
    //an array saying whether there is a direct connection between any two continents
    boolean[][] contsConnected = new boolean[numContinents][numContinents];
    for(int i = 0; i < numContinents; i += 1) {
      for(int j = 0; j < numContinents; j += 1) {
        contsConnected[i][j] = false;
      }
    }

    boolean[][] countriesConnected = new boolean[connections.size()][connections.size()];
    for(int i = 0; i < connections.size(); i += 1) {
      for(int j = 0; j < connections.size(); j += 1) {
        countriesConnected[i][j] = false;
      }
    }

    Vector lines = new Vector();

    //connect continents until they are all connected
    boolean more = true;
    while(more && !allContsReachableFrom(0, contsConnected, numContinents)) {
//      more = false;
      double shortestGap = 10000;
      int contA = rand.nextInt(numContinents);
//      debug("contIndices[contA+1] - contIndices[contA] = " + (contIndices[contA+1] - contIndices[contA]));
      int countryA = contIndices[contA] + rand.nextInt(contIndices[contA+1] - contIndices[contA]);
      int contB = -1;
      int countryB = -1;

      int xxx = 0;
      for(int i = 0; i < numContinents; i += 1) {
        if(i != contA) {
          for(int j = contIndices[i]; j < contIndices[i+1]; j += 1) {
            if(!countriesConnected[countryA][j] && countriesConnectable(countryA, j)) {
//              debug("xxx = " +  j);
              xxx += 1;
              double gap = dist(polygonCenter((Polygon)countryPolygons.get(countryA)),
                                polygonCenter((Polygon)countryPolygons.get(j)));
//              debug("gap = " + gap);
              if(gap < shortestGap) {
                shortestGap = gap;
                contB = i;
                countryB = j;
              }
            }
          }
        }
      }

      if(countryB != -1) {
        lines.add(connectCountries(countryA, countryB));
        countriesConnected[countryA][countryB] = true;
        countriesConnected[countryB][countryA] = true;
        contsConnected[contA][contB] = true;
        contsConnected[contB][contA] = true;
      }
    }

    //kill dead ends
    for(int a = 0; a < numContinents; a += 1) {
      int connectedConts = 0;
      for(int i = 0; i < numContinents; i += 1) {
        if(i != a && contsConnected[i][a]) {
          connectedConts += 1;
        }
      }

      if(connectedConts == 1) {
        newconnection:
        for(int t = 0; t < 10; t += 1) {
          double shortestGap = 10000;
          int contA = a;
          int countryA = contIndices[contA] + rand.nextInt(contIndices[contA+1] - contIndices[contA]);
          int contB = -1;
          int countryB = -1;

          for(int i = 0; i < numContinents; i += 1) {
            if(i != contA) {
              for(int j = contIndices[i]; j < contIndices[i+1]; j += 1) {
                if(!contsConnected[contA][i] && !countriesConnected[countryA][j] && countriesConnectable(countryA, j)) {
                  double gap = dist(polygonCenter((Polygon)countryPolygons.get(countryA)),
                                    polygonCenter((Polygon)countryPolygons.get(j)));
                  if(gap < shortestGap) {
                    shortestGap = gap;
                    contB = i;
                    countryB = j;
                  }
                }
              }
            }
          }
          if(countryB != -1) {
            lines.add(connectCountries(countryA, countryB));
            countriesConnected[countryA][countryB] = true;
            countriesConnected[countryB][countryA] = true;
            contsConnected[contA][contB] = true;
            contsConnected[contB][contA] = true;
            break newconnection;
          }
        }
      }
    }

    return lines;
  }

  //can we connect contA and contB? no if the resulting line would cross another continent
  private boolean countriesConnectable(int countryA, int countryB) {
    Line2D.Double l = new Line2D.Double(polygonCenter((Polygon)countryPolygons.get(countryA)),
                                        polygonCenter((Polygon)countryPolygons.get(countryB)));
    for(int i = 0; i < countryPolygons.size(); i += 1) {
      if(i != countryA && i != countryB) {
        Polygon p = (Polygon)countryPolygons.get(i);
        p = dilatePolygon(p, 1.2);
        Line2D.Double l1 = new Line2D.Double(p.xpoints[0], p.ypoints[0], p.xpoints[1], p.ypoints[1]);
        Line2D.Double l2 = new Line2D.Double(p.xpoints[1], p.ypoints[1], p.xpoints[2], p.ypoints[2]);
        Line2D.Double l3 = new Line2D.Double(p.xpoints[2], p.ypoints[2], p.xpoints[0], p.ypoints[0]);
        if(l.intersectsLine(l1) || l.intersectsLine(l2) || l.intersectsLine(l3)) return false;
      }
    }
    return true;
  }

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
  private Line2D.Double connectCountries(int country1, int country2) {
    ((Vector)connections.get(country1)).add(new Integer(country2));
    ((Vector)connections.get(country2)).add(new Integer(country1));

    return drawLineBetween((Polygon)countryPolygons.get(country1),
                           (Polygon)countryPolygons.get(country2));
  }


  private boolean trianglesIntersect(Polygon a, Polygon b) {
    if(a.contains(polygonCenter(b))) return true;
    if(b.contains(polygonCenter(a))) return true;
    if(b.contains(a.xpoints[0], a.ypoints[0])) return true;
    if(b.contains(a.xpoints[1], a.ypoints[1])) return true;
    if(b.contains(a.xpoints[2], a.ypoints[2])) return true;
    if(a.contains(b.xpoints[0], b.ypoints[0])) return true;
    if(a.contains(b.xpoints[1], b.ypoints[1])) return true;
    if(a.contains(b.xpoints[2], b.ypoints[2])) return true;

    return false;
  }

  //write the xml representation of a country polygon
  private void writePolygon(Polygon p) {
    out.write("<polygon>");
    for(int i = 0; i < p.npoints; i += 1) {
      out.write("" + p.xpoints[i] + "," + p.ypoints[i] + " ");
    }
    out.write("</polygon>\n");

  }

  //write the xml representation of a country's adjoining list
  private void writeConnections(Vector conns) {
    out.write("<adjoining>");
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


  private Polygon upTriangleAround(Point p) {
    int[] xpoints = new int[3];
    int[] ypoints = new int[3];
    xpoints[0] = p.x;
    ypoints[0] = p.y + (int)tRad;
    xpoints[1] = p.x - (int)(tRad*Math.sqrt(3)/2);
    ypoints[1] = p.y - (int)(tRad/2);
    xpoints[2] = p.x + (int)(tRad*Math.sqrt(3)/2);
    ypoints[2] = p.y - (int)(tRad/2);

    return new Polygon(xpoints, ypoints, 3);
  }

  private Polygon downTriangleAround(Point p) {
    int[] xpoints = new int[3];
    int[] ypoints = new int[3];
    xpoints[0] = p.x;
    ypoints[0] = p.y - (int)tRad;
    xpoints[1] = p.x - (int)(tRad*Math.sqrt(3)/2);
    ypoints[1] = p.y + (int)(tRad/2);
    xpoints[2] = p.x + (int)(tRad*Math.sqrt(3)/2);
    ypoints[2] = p.y + (int)(tRad/2);

    return new Polygon (xpoints, ypoints, 3);
  }

  private boolean trianglePointsUp(Polygon p) {
    Point c = polygonCenter(p);
    int pointsAbove = 0;
    for(int i = 0; i < 3; i += 1) {
      if(p.ypoints[i] > c.y) pointsAbove += 1;
    }
    if(pointsAbove == 1) return true;
    return false;
  }

  private boolean polygonFitsOnScreen(Polygon p) {
    for(int i = 0; i < p.npoints; i += 1) {
      if(playerInfoRect.contains(p.xpoints[i], p.ypoints[i]) ||
         p.xpoints[i] < 0 || p.xpoints[i] > width ||
         p.ypoints[i] < 0 || p.ypoints[i] > height) {
           return false;
      }
    }

    return true;
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

  public boolean canCache() {
    return false;
  }

  public String description() {
    return "Generates maps like those of the Hex series.";
  }

  private static final String CHOICE_SMALL = "HexInfinity - small";
  private static final String CHOICE_MEDIUM = "HexInfinity - medium";
  private static final String CHOICE_LARGE = "HexInfinity - large";
  private static final String CHOICE_HUGE = "HexInfinity - huge";

  public java.util.List getChoices() {
    Vector v = new Vector();
    v.add(CHOICE_SMALL);
    v.add(CHOICE_MEDIUM);
    v.add(CHOICE_LARGE);
    v.add(CHOICE_HUGE);
    return v;
  }


  public float version() {
    return 1.2f;
  }

  public String name() {
    return "HexInfinity";
  }

  public String message(String message, Object data) {
  	if ("scenarioPlayerCount".equals(message))
		{
		return "6";
		}
  
    return "";
  }

  private void debug(String s) {
    //System.out.println("" + s);
  }
}
