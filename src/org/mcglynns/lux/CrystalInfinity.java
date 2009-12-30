

import com.sillysoft.lux.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.geom.Line2D;



public class CrystalInfinity implements LuxMapGenerator {


  static final String CHOICE_SMALL = "CrystalInfinity - small";
  static final String CHOICE_MEDIUM = "CrystalInfinity - medium";
  static final String CHOICE_LARGE = "CrystalInfinity - large";
  static final String CHOICE_EPIC = "CrystalInfinity - epic";



  int width, height;
  String choice;
  int seed;
  Random rand;


  int randTilingSymmetry() {
    switch(rand.nextInt(7)) {
      case 0:
      case 1:
      case 2:
      case 3:
        return 5;

      case 4:
      case 5:
        return 7;

      default:
        return 9;

    }
  }


  public boolean generate(PrintWriter out, String choice, int seed, MapLoader loader) {

    System.out.println("CrystalInfinity: starting to create map \"" + choice + " #" + seed + "\"...");

    rand = new Random(seed);
    this.choice = choice;
    this.seed = seed;

    int numRegions;

    if(choice.equals(CHOICE_SMALL)) {
      width = 500;
      height = 300;
      numRegions = 2 + rand.nextInt(2);
    } else if(choice.equals(CHOICE_MEDIUM)) {
      width = 650;
      height = 450;
      numRegions = 3 + rand.nextInt(2);
    } else if(choice.equals(CHOICE_LARGE)) {
      width = 800;
      height = 600;
      numRegions = 4 + rand.nextInt(3);
    } else { //choice.equals(CHOICE_EPIC))
      width = 1000;
      height = 800;
      numRegions = 6 + rand.nextInt(5);
    }



    Vector<Rhombus> rhombi = new Vector<Rhombus>();
    Vector<Rhombus> interRegionAdjoinings = new Vector<Rhombus>();
    int numContinents = 1;


    int tilingSymmetry = randTilingSymmetry();

    Vector<Vector<Rhombus>> regions = new Vector<Vector<Rhombus>>();

    double[] seedX = new double[numRegions];
    double[] seedY = new double[numRegions];

    double minDist = -1;

    System.out.println("CrystalInfinity: seeding regions...");

    while(minDist < 150) {
      for(int i = 0; i < numRegions; i += 1) {
        seedX[i] = width*rand.nextDouble()-width/2;
        seedY[i] = height*rand.nextDouble()-width/2;
      }

      minDist = 1000000;
      for(int i = 0; i < numRegions; i += 1) {
        for(int j = 0; j < numRegions; j += 1) {
          if(i == j) continue;
          double dist = Math.sqrt(sq(seedX[i] - seedX[j]) + sq(seedY[i] - seedY[j]));
          if(dist < minDist) minDist = dist;
        }
      }
    }



    boolean[][] connected = new boolean[numRegions][numRegions];



    for(int i = 0; i < numRegions; i += 1) {
      for(int j = 0; j < numRegions; j += 1) {
        connected[i][j] = false;
      }
    }

    System.out.println("CrystalInfinity: growing regions...");

    for(int i = 0; i < numRegions; i += 1) {
      Vector<Rhombus> region = createInfiniteTiling(tilingSymmetry);
      trimTiling(region);

      for(int j = region.size()-1; j >= 0; j -= 1) {
        Rhombus r = region.get(j);
        for(int k = 0; k < 4; k += 1) {
          int closest = closestPoint(r.xpoints[k], r.ypoints[k], seedX, seedY);
          if(closest != i) {
            region.remove(j);
            if(k != 0) {
              connected[i][closest] = true;
              connected[closest][i] = true;
            }
            break;
          }
        }
      }

      pruneTiling(region);

      ensureContiguous(region, out);

      if(region.size() == 1) region.remove(0);

      regions.add(region);
    }



    for(int i = 0; i < numRegions; i += 1) {
      for(int j = i+1; j < numRegions; j += 1) {
        if(connected[i][j]) {
          connectRegions(regions.get(i), regions.get(j), interRegionAdjoinings);
        }
      }
    }

    for(int i = 0; i < numRegions; i += 1) {
      rhombi.addAll(regions.get(i));
    }

    System.out.println("CrystalInfinity: creating continents...");

    numContinents = createContinents(rhombi, tilingSymmetry, numContinents);

    iraloop:
    for(int i = interRegionAdjoinings.size()-2; i >= 0; i -= 2) {
      Rhombus r1 = interRegionAdjoinings.get(i);
      Rhombus r2 = interRegionAdjoinings.get(i+1);
      if(r1 == null || r2 == null) continue;

      Line2D l = new Line2D.Double(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY());
      for(int j = 0; j < rhombi.size(); j += 1) {
        Rhombus r = rhombi.get(j);
        if(r == r1 || r == r2) continue;
        if(intersects(r, l)) {
          interRegionAdjoinings.remove(i+1);
          interRegionAdjoinings.remove(i);
          out.write("<line><position>" + (int)r1.centerX() + "," + (int)r1.centerY() + " " + (int)r2.centerX() + "," + (int)r2.centerY() + "</position><above>true</above><color>0.0/1.0/0.0</color></line>\n");
          continue iraloop;
        }
      }
    }




    //write the XML!!

    System.out.println("CrystalInfinity: writing XML...");

    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<luxboard>\n" +
              "<version>1.0</version>\n" +
              "<width>" + width + "</width>\n" +
              "<height>" + height + "</height>\n" +
              "<theme>" + choice+seed + "</theme>\n" +
              "<author>CrystalInfinity Generator (by Greg McGlynn)</author>\n" +
              "<email>greg@mcglynns.org</email>\n" +
              "<webpage>www.sillysoft.net</webpage>\n" +
              "<title>" + choice + "</title>\n" +
              "<description></description>\n");

    for(int c = 0; c < numContinents; c += 1) {
      if(c == 0) {
        out.write("<continent>\n" +
                  "<continentname>Disordered Lands</continentname>\n" +
                  "<bonus>0</bonus>\n" +
                  "<labellocation>-200,-200</labellocation>\n");
      } else {
        out.write("<continent>\n" +
                  "<continentname>Crystal " +  c + "</continentname>\n");
      }

      out.write("<color>" + .333*rand.nextInt(4) + "/" + .333*rand.nextInt(4) + "/" + .333*rand.nextInt(4) + "</color>\n");

      int size = 0;
      int borders = 0;
      double centerX = 0;
      double centerY = 0;
      boolean ring = true;

      for(int i = 0; i < rhombi.size(); i += 1) {

        Rhombus r = rhombi.get(i);

        if(r.continent != c) continue;

        size += 1;

        boolean border = false;



        out.write("  <country>\n");
        out.write("    <id>" + i + "</id>\n");
        out.write("    <name>Facet " + i + "</name>\n");

        out.write("    <initialOwner>" + rand.nextInt(6) + "</initialOwner>");
        out.write("    <initialArmies>1</initialArmies>");

        out.write("    <adjoining>");

        for(int j = 0; j < interRegionAdjoinings.size(); j += 2) {
          if(interRegionAdjoinings.get(j) == r) {
            border = true;
            out.write("" + rhombi.indexOf(interRegionAdjoinings.get(j+1)) + ",");
          }
        }

        Vector<Integer> adjoining = getAdjoining(i, rhombi);


        for(int j = 0; j < adjoining.size(); j += 1) {
          if(!(j==0)) out.write(",");
          out.write("" + adjoining.get(j).toString());
          if(rhombi.get(adjoining.get(j)).continent != c) border = true;
          if(rhombi.get(adjoining.get(j)).continent == c) ring = false;

        }
        out.write("</adjoining>\n");


        out.write("    <polygon>");
        for(int j = 0; j < 4; j += 1) {
          out.write("" + (width/2+(int)r.xpoints[j]) + "," + (height/2+(int)r.ypoints[j]) + " ");
        }
        out.write("</polygon>\n");

        centerX += r.centerX();
        centerY += r.centerY();


        out.write("  </country>\n\n");

        if(border) borders += 1;
      }

      if(size != tilingSymmetry) ring = false;

      if(c != 0) {
        if(ring) out.write("<bonus>" + size/2 + "</bonus>\n");
        else     out.write("<bonus>" + borders + "</bonus>\n");
      }
      
      if(ring) {
        centerX /= size;
        centerY /= size;
        out.write("<labellocation>" + (int)(width/2+centerX) + "," + (int)(width/2+centerY-40) + "</labellocation>\n");
      }

      out.write("</continent>\n");
    }



    System.out.println("CrystalInfinity: creating theme...");

    doXMLTheme(rhombi, interRegionAdjoinings, out);


    doImageTheme(rhombi, loader);


    out.write("</luxboard>\n");


    System.out.println("CrystalInfinity: finished creating map \"" + choice + " #" + seed + "\"");

    return true;
  }





  //////Tiling methods: ///////////////////////

  void trimTiling(Vector<Rhombus> rhombi) {

    for(int i = rhombi.size()-1; i >= 0; i -= 1) {
      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        if(Math.abs(r.xpoints[j]) > width/2-10 || Math.abs(r.ypoints[j]) > height/2-10) {
          rhombi.remove(i);
          break;
        }
      }
    }


    //align vertices thatshould be identical
    for(int i = 0; i < rhombi.size(); i += 1) {
      Rhombus r1 = rhombi.get(i);
      for(int j = 0; j < rhombi.size(); j += 1) {
        if(i == j) continue;
        Rhombus r2 = rhombi.get(j);
        for(int m = 0; m < 4; m += 1) {
          for(int n = 0; n < 4; n += 1) {
            if(   Math.abs(r1.xpoints[m] - r2.xpoints[n]) < 8 &&
                  Math.abs(r1.ypoints[m] - r2.ypoints[n]) < 8) {
              double midX = (r1.xpoints[m] + r2.xpoints[n])/2;
              double midY = (r1.ypoints[m] + r2.ypoints[n])/2;
              r1.xpoints[m] = r2.xpoints[n] = midX;
              r1.ypoints[m] = r2.ypoints[n] = midY;
            }
          }
        }
      }
    }


    loopq:
    for(int i = rhombi.size() - 1; i >= 0; i -= 1) {
      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        for(int k = 0; k < 4; k += 1) {
          if(j == k) continue;
          if(  Math.abs(r.xpoints[j] - r.xpoints[k]) < 5 &&
               Math.abs(r.ypoints[j] - r.ypoints[k]) < 5) {
            rhombi.remove(r);
            continue loopq;
          }
        }
      }
    }
  }



  void pruneTiling(Vector<Rhombus> rhombi) {
    //remove rhombi that are isolated
    for(int i = rhombi.size()-1; i >= 0; i -= 1) {
      if(getAdjoining(i, rhombi).size() == 0) rhombi.remove(i);
    }
  }



  void translate(Vector<Rhombus> rhombi, double origX, double origY) {
    //center the region on (origX, origY)
    for(int i = 0; i < rhombi.size(); i += 1) {
      for(int j = 0; j < 4; j += 1) {
        rhombi.get(i).xpoints[j] += origX;
        rhombi.get(i).ypoints[j] += origY;
      }
    }
  }




  void ensureContiguous(Vector<Rhombus> rhombi, PrintWriter out) {
    int[] rep = new int[rhombi.size()];

    for(int i = 0; i < rep.length; i += 1) {
      rep[i] = i;
    }

    for(int z = 0; z < 1000; z += 1) {
      for(int i = 0; i < rep.length; i += 1) {
        Vector<Integer> adjoining = getAdjoining(i, rhombi);
        for(int j = 0; j < adjoining.size(); j += 1) {
          int adjIndex = adjoining.get(j).intValue();
          if(rep[adjIndex] < rep[i]) rep[i] = rep[adjIndex];
        }
      }
    }

    int[] count = new int[rep.length];

    for(int i = 0; i < rep.length; i += 1) {
      count[rep[i]] += 1;
    }

    int biggestRep = -1;
    int biggestCount = -1;

    for(int i = 0; i < count.length; i += 1) {
      if(count[i] > biggestCount) {
        biggestCount = count[i];
        biggestRep = i;
      }
    }

    for(int i = rhombi.size()-1; i >= 0; i -= 1) {
      if(rep[i] != biggestRep) rhombi.remove(i);
    }


  }


  Vector<Integer> getAdjoining(int x, Vector<Rhombus> rhombi) {
    Vector<Integer> adjoining = new Vector<Integer>();

    Rhombus rhombus = rhombi.get(x);

    for(int i = 0; i < rhombi.size(); i += 1) {
      if(i == x) continue;

      int numSharedVertices = 0;
      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        for(int k = 0; k < 4; k += 1) {
          if(     Math.abs(rhombus.xpoints[j]-r.xpoints[k]) < 10 &&
                  Math.abs(rhombus.ypoints[j]-r.ypoints[k]) < 10) {
            numSharedVertices += 1;
          }
        }
      }
      if(numSharedVertices >= 2) {
        adjoining.add(new Integer(i));
      }
    }

    return adjoining;
  }






  ////Cont creation methods:///////////////////////////////

  int createContinents(Vector<Rhombus> rhombi, int tilingSymmetry, int contCount) {

    //int contLimit = contCount + Math.max(1, rhombi.size()/12);

    //create star continents first
    for(int i = 0; i < rhombi.size(); i += 1) {
      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        double x = r.xpoints[j];
        double y = r.ypoints[j];

        int newConts = createStarContinent(rhombi, x, y, contCount, tilingSymmetry);
        if(newConts > 0) {
          contCount += newConts;
          break;
        }
      }
    }


    //create other continents
    for(int z = 0; z < 10000; z += 1) {

      Rhombus r = rhombi.get(rand.nextInt(rhombi.size()));
      int vertex = rand.nextInt(4);
      double x = r.xpoints[vertex];
      double y = r.ypoints[vertex];

      if(createContinent(rhombi, x, y, contCount)) {
        contCount += 1;
      }

    }

    return contCount;
  }


  int createStarContinent(Vector<Rhombus> rhombi, double x, double y, int contID, double tilingSymmetry) {
    Vector<Rhombus> c = new Vector<Rhombus>();

    double trueX = 0, trueY = 0;

    for(int i = 0; i < rhombi.size(); i += 1) {
      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        if(   Math.abs(r.xpoints[j] - x) < 10 &&
              Math.abs(r.ypoints[j] - y) < 10) {

          if(r.continent != 0) return 0;

          c.add(r);

          trueX += r.xpoints[j];
          trueY += r.ypoints[j];

          break;
        }
      }
    }


    if(c.size() % tilingSymmetry != 0) return 0;

    trueX /= c.size();
    trueY /= c.size();



    Rhombus[] countries = new Rhombus[c.size()];
    for(int i = 0; i < countries.length; i += 1) {
      countries[i] = c.get(i);
      if(!equals(countries[i].distanceFrom(trueX, trueY), countries[0].distanceFrom(trueX, trueY), 5)) return 0;
    }

    //star!
    for(int i = 0; i < countries.length; i += 1) {
      countries[i].continent = contID;
    }


    if(rand.nextInt(2) == 0) return 1;

    Set<Rhombus> surroundings = new HashSet<Rhombus>();

    double surroundDist = 0;

    for(int i = 0; i < countries.length; i += 1) {
      Vector<Integer> neighbors = getAdjoining(rhombi.indexOf(countries[i]), rhombi);
      for(int j = 0; j < neighbors.size(); j += 1) {
        Rhombus r = rhombi.get(neighbors.get(j).intValue());

        if(r.continent == 0) {
          surroundings.add(r);

          if(surroundDist == 0) {
            surroundDist = r.distanceFrom(trueX, trueY);
          } else {
            if(!equals(surroundDist, r.distanceFrom(trueX, trueY), 5)) return 1;
          }
        }
      }
    }

    if(surroundings.size() != countries.length)  return 1;


    //proper symmetrical surroundings!
    Vector<Rhombus> v = new Vector<Rhombus>(surroundings);
    for(int i = 0; i < v.size(); i += 1) {
      v.get(i).continent = (contID + 1);
    }

    return 2;

  }


  boolean createContinent(Vector<Rhombus> rhombi, double x, double y, int contID) {
    Vector<Rhombus> c = new Vector<Rhombus>();

    double trueX = 0, trueY = 0;

    for(int i = 0; i < rhombi.size(); i += 1) {


      Rhombus r = rhombi.get(i);
      for(int j = 0; j < 4; j += 1) {
        if(   Math.abs(r.xpoints[j] - x) < 10 &&
              Math.abs(r.ypoints[j] - y) < 10) {
          //rhombus touches (x, y)

          if(r.continent != 0) return false;

          //continents must not share an edge:
          Vector<Integer> adjoining = getAdjoining(i, rhombi);
          for(int k = 0; k < adjoining.size(); k += 1) {
            if(rhombi.get(adjoining.get(k).intValue()).continent != 0) return false;
          }

          c.add(r);

          trueX += r.xpoints[j];
          trueY += r.ypoints[j];



          break;
        }
      }
    }

    if(c.size() < 3) return false;

    trueX /= c.size();
    trueY /= c.size();

    Rhombus[] countries = new Rhombus[c.size()];
    double[] radii = new double[c.size()];
    double[] angles = new double[c.size()];
    for(int i = 0; i < countries.length; i += 1) {
      countries[i] = c.get(i);
      radii[i] = countries[i].distanceFrom(trueX, trueY);
      angles[i] = Math.atan2(countries[i].centerY() - trueY, countries[i].centerX() - trueX);
    }

    //now we have a vector of all countries touching this vertex, none of which belong to any continent
    //check for two-fold symmetry of the constellation
    //we have two-fold symmetry iff:
    //  -there is an axis of symmetry running through the center of at least one rhombus
    //  -if a rhombus is not on the axis of symmetry it has a pair opposite it
    for(int a = 0; a < countries.length; a += 1) { //axis of symmetry
      double axis = angles[a];

      boolean symmetrical = true;
      for(int i = 0; i < countries.length; i += 1) {
        if(i == a) continue;

        boolean hasReflection = false;

        for(int j = 0; j < countries.length; j += 1) {
          if(i == j) continue;

          if(equals(radii[i], radii[j], 5) &&
             equals(angleSeparation(angles[i], axis), angleSeparation(angles[j], axis), Math.PI/10)) {
            hasReflection = true;
            break;
          }
        }

        //reflection of self
        if(equals(angleSeparation(angles[i], axis), Math.PI, Math.PI/10)) hasReflection = true;

        if(!hasReflection) { //not symmetrical about this axis
          symmetrical = false;
          break;
        }
      }


      if(symmetrical) { //victory!
        for(int i = 0; i < countries.length; i += 1) {
          countries[i].continent = contID;
        }

        return true;
      }
    }

    //no axis worked. no continent
    return false;
  }



  //makes an interregion connection between two noncontiguous regions
  void connectRegions(Vector<Rhombus> region1, Vector<Rhombus> region2, Vector<Rhombus> interRegionAdjoinings) {

    Rhombus closest1 = null, closest2 = null;
    double closestDist = 999999999;

    for(int i = 0; i < region1.size(); i += 1) {
      Rhombus r1 = region1.get(i);
      for(int j = 0; j < region2.size(); j += 1) {
        Rhombus r2 = region2.get(j);

        double dist = r1.distanceFrom(r2.centerX(), r2.centerY());

        if(dist < closestDist) {
          closestDist = dist;
          closest1 = r1;
          closest2 = r2;
        }
      }
    }

    interRegionAdjoinings.add(closest1);
    interRegionAdjoinings.add(closest2);
    interRegionAdjoinings.add(closest2);
    interRegionAdjoinings.add(closest1);
  }





  //Theme methods:////////////////////////

  void doXMLTheme(Vector<Rhombus> rhombi, Vector<Rhombus> interRegionAdjoinings, PrintWriter out) {

    //draw the continental borders in XML lines
    for(int i = 0; i < rhombi.size(); i += 1) {
      Rhombus r = rhombi.get(i);

      if(r.continent == 0) continue;

      Vector<Integer> adjoining = getAdjoining(i, rhombi);


      for(int j = 0; j < 4; j += 1) {
        double x1 = width/2+r.xpoints[j];
        double y1 = height/2+r.ypoints[j];
        double x2 = width/2+r.xpoints[(j+1)%4];
        double y2 = height/2+r.ypoints[(j+1)%4];

        boolean intercontinental = true;

        //inter- or intra-continental border?
        for(int k = 0; k < adjoining.size(); k += 1) {
          Rhombus r2 = rhombi.get(adjoining.get(k).intValue());
          if(r2.continent == r.continent) {
            if(Line2D.linesIntersect(r.xpoints[j], r.ypoints[j], r.xpoints[(j+1)%4], r.ypoints[(j+1)%4],
                                     r.centerX(), r.centerY(), r2.centerX(), r2.centerY())) {
              intercontinental = false;
            }
          }
        }


        if(!intercontinental) {
//          out.write("<line><position>" + (int)x1 + "," + (int)y1 + " " + (int)x2 + "," + (int)y2 + "</position>" +
//                    "<width>2</width><above>true</above><color>0.0/0.0/0.0</color></line>");
        } else {

          double angle = Math.atan2(y2 - y1, x2 - x1);

          double oX = 1.5*Math.cos(angle+Math.PI/2);
          double oY = 1.5*Math.sin(angle+Math.PI/2);

          //contract the line a bit
          x1 += 2.3*Math.cos(angle);
          x2 -= 2.3*Math.cos(angle);
          y1 += 2.3*Math.sin(angle);
          y2 -= 2.3*Math.sin(angle);

          out.write("<line><position>" + (int)(x1+oX) + "," + (int)(y1+oY) + " " + (int)(x2+oX) + "," + (int)(y2+oY) +
              "</position><width>3</width><above>true</above><color>0.0/0.0/0.0</color></line>\n");

          //contract the line a bit
          x1 -= 2.3*Math.cos(angle);
          x2 += 2.3*Math.cos(angle);
          y1 -= 2.3*Math.sin(angle);
          y2 += 2.3*Math.sin(angle);

          out.write("<line><position>" + (int)(x1-oX) + "," + (int)(y1-oY) + " " + (int)(x2-oX) + "," + (int)(y2-oY) +
              "</position><width>3</width><above>true</above><color>1.0/1.0/1.0</color></line>\n");
        }
      }
    }


    for(int i = 0; i < interRegionAdjoinings.size(); i += 2) {
      Rhombus r1 = interRegionAdjoinings.get(i);
      Rhombus r2 = interRegionAdjoinings.get(i+1);
      if(r1 == null || r2 == null) continue;
      out.write("<line><position>" +(int)(width/2+r1.centerX()) + "," + (int)(height/2+r1.centerY()) + " " +
                                    (int)(width/2+r2.centerX()) + "," + (int)(height/2+r2.centerY()) +
                "</position><color>0.0/0.0/0.0</color><width>7</width></line>\n");
      out.write("<line><position>" +(int)(width/2+r1.centerX()) + "," + (int)(height/2+r1.centerY()) + " " +
                                    (int)(width/2+r2.centerX()) + "," + (int)(height/2+r2.centerY()) +
                "</position><color>1.0/1.0/1.0</color><width>1</width></line>\n");
    }
  }







  void doImageTheme(Vector<Rhombus> rhombi, MapLoader loader) {

    //get new theme images
    BufferedImage foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D foreG = (Graphics2D)foreground.getGraphics();
    foreG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);


    //do the facets on the continental rhombi
    for(int i = 0; i < rhombi.size(); i += 1) {
      Rhombus r = rhombi.get(i);

      if(r.continent == 0) continue;

      for(int j = 0; j < 4; j += 1) {

        //the intensity (alpha) of a facet is based on how perpendicular it is
        //to the direction of incoming light (which comes in diagonally from above)
        double angle = Math.atan2(r.ypoints[(j+1)%4] - r.ypoints[j],
                                  r.xpoints[(j+1)%4] - r.xpoints[j]);
        double angleFromLight = Math.abs(angle - 3*Math.PI/4);
        if(angleFromLight > Math.PI) angleFromLight = 2*Math.PI-angleFromLight;

        float alpha = (float)(Math.abs(angleFromLight - Math.PI/2)/(Math.PI/2));
        alpha *=.5;

        Color c;
        if(angleFromLight < Math.PI/2) c = new Color(1.0f, 1.0f, 1.0f, alpha);
        else                           c = new Color(0.0f, 0.0f, 0.0f, alpha);



        double xpoints[] = new double[4];
        double ypoints[] = new double[4];

        xpoints[0] = r.xpoints[j];
        ypoints[0] = r.ypoints[j];

        xpoints[1] = r.xpoints[(j+1)%4];
        ypoints[1] = r.ypoints[(j+1)%4];

        xpoints[2] = (xpoints[1] + r.centerX())/2;
        ypoints[2] = (ypoints[1] + r.centerY())/2;

        xpoints[3] = (xpoints[0] + r.centerX())/2;
        ypoints[3] = (ypoints[0] + r.centerY())/2;

        int[] ixpoints = new int[4];
        int[] iypoints = new int[4];
        for(int k = 0; k < 4; k += 1) {
          ixpoints[k] = width/2 + (int)xpoints[k];
          iypoints[k] = height - (height/2 + (int)ypoints[k]);
        }

        Polygon p = new Polygon(ixpoints, iypoints, 4);

        foreG.setColor(c);
        foreG.fillPolygon(p);
      }
    }





    try {
      //first delete any old theme directories
      File support = new File(new File(loader.getMapGeneratorPath()).getParent());
      File[] supportChildren = support.listFiles();
      File themes = null;
      for(int i = 0; i < supportChildren.length; i += 1) {
        if(supportChildren[i].getName().toLowerCase().equals("themes")) {
          themes = supportChildren[i];
        }
      }

      File[] themesChildren = themes.listFiles();
      for(int i = 0; i < themesChildren.length; i += 1) {
        if(themesChildren[i].getName().toLowerCase().indexOf("crystalinfinity") != -1) {
          File[] oldThemeFiles = themesChildren[i].listFiles();
          for(int j = 0; j < oldThemeFiles.length; j += 1) {
            oldThemeFiles[j].delete();
          }
          themesChildren[i].delete();
        }
      }

      //the background:
      BufferedImage woodImage = ImageIO.read(new File(support + File.separator + "MapGenerators" + File.separator + "Wood.jpg"));

      //then write one for this map
      new File(themes.getPath() + File.separator + choice + seed).mkdir();
      ImageIO.write(foreground, "png", new File(themes + File.separator + choice+seed + File.separator + "overground.png"));
      ImageIO.write(woodImage,  "jpg", new File(themes + File.separator + choice+seed + File.separator + "background.jpg"));
      ImageIO.write(woodImage,  "jpg", new File(themes + File.separator + choice+seed + File.separator + "foreground.jpg"));
    } catch(Exception e) {
      e.printStackTrace();
    }

  }





  //a core method: creates a large region of a generalized Penrose tiling
  Vector<Rhombus> createInfiniteTiling(int symmetry) {


    final int gridSize = 21;

    MultigridLine[][] multigrid = new MultigridLine[symmetry][gridSize];

    double totalOffset;


    double theOffset = rand.nextDouble()-.5;

    //asymmetrical
    totalOffset = 0;
    for(int i = 0; i < symmetry - 1; i += 1) {
      double offset = rand.nextDouble() - .5;
      multigrid[i][0] = new MultigridLine(i*(2*Math.PI/symmetry), offset-gridSize/2);
      totalOffset += offset;
    }
    multigrid[symmetry-1][0] = new MultigridLine((symmetry-1)*2*Math.PI/symmetry, -totalOffset -gridSize/2);




    for(int i = 0; i < symmetry; i += 1) {
      for(int j = 1; j < gridSize; j += 1) {
        multigrid[i][j] = new MultigridLine(multigrid[i][j-1].angle, multigrid[i][j-1].offset + 1);
      }
    }


    for(int i = 0; i < symmetry; i += 1) {
      for(int j = 0; j < gridSize; j += 1) {
        for(int x = i+1; x < symmetry; x += 1) {
          if(x == i) continue;
          for(int y = 0; y < gridSize; y += 1) {
            MultigridLine.addIntersection(multigrid[i][j], multigrid[x][y]);
          }
        }
      }
    }

    MultigridVertex centralVertex = multigrid[0][gridSize/2].vertices.get(multigrid[0][gridSize/2].vertices.size()/2);

    for(int i = 0; i < symmetry; i += 1) {
      for(int j = 0; j < gridSize; j += 1) {
        multigrid[i][j].connectVertices();
      }
    }




    Rhombus centralRhombus = centralVertex.unitRhombus();

    Vector<Rhombus> rhombi = new Vector<Rhombus>();
    Vector<MultigridVertex> vertices = new Vector<MultigridVertex>();
    Vector<Rhombus> rhombiToAdd = new Vector<Rhombus>();

    vertices.add(centralVertex);
    rhombi.add(centralRhombus);

    loop:
    while(rhombi.size() < 500) {
      for(int i = vertices.size()-1; i >= 0; i -= 1) {

        MultigridVertex vertex = vertices.get(i);
        Rhombus rhombus = rhombi.get(i);



        for(int j = 0; j < 4; j += 1) {
          MultigridVertex neighbor = vertex.connectedVertices.get(j);
          if(neighbor.connectedVertices.size() != 4 || vertices.contains(neighbor)) continue;
          Rhombus nRhombus = neighbor.attachRhombus(vertex, rhombus);
          vertices.add(neighbor);
          rhombi.add(nRhombus);
        }
      }
    }


    double originX = centralRhombus.centerX();
    double originY = centralRhombus.centerY();

    /*
    //make the origin (0, 0)
    double originX = 0, originY = 0;
    for(int i = 0; i < symmetry; i += 1) {
      MultigridVertex centralVertexI = multigrid[i][gridSize/2].vertices.get(multigrid[i][gridSize/2].vertices.size()/2);
      int index = vertices.indexOf(centralVertexI);
      originX += rhombi.get(index).centerX();
      originY += rhombi.get(index).centerY();
    }
    originX /= symmetry;
    originY /= symmetry;     */

    //now translate the rhombi so that the origin is at (0, 0)

    for(int i = 0; i < rhombi.size(); i += 1) {
      rhombi.get(i).translate(-originX+15, -originY);
    }


    return rhombi;
  }



  /////Util. methods://///////////
  double sq(double x) { return x*x; }

  boolean equals(double x, double y, double margin) {
    return (Math.abs(x-y) < margin);
  }

  double angleSeparation(double a, double b) {
    double ret = Math.abs(a - b);
    if(ret > Math.PI) ret = 2*Math.PI - ret;
    return ret;
  }

  int closestPoint(double x, double y, double[] xpoints, double[] ypoints) {
    double closestDist = Double.MAX_VALUE;
    int ret = -1;
    for(int i = 0; i < xpoints.length; i += 1) {
      double dist = Math.sqrt(sq(x-xpoints[i]) + sq(y-ypoints[i]));
      if(dist < closestDist) {
        closestDist = dist;
        ret = i;
      }
    }

    return ret;
  }

  boolean intersects(Rhombus r, Line2D l) {
    for(int i = 0; i < 4; i += 1) {
      Line2D lr = new Line2D.Double(r.xpoints[i], r.ypoints[i], r.xpoints[(i+1)%4], r.ypoints[(i+1)%4]);
      if(l.intersectsLine(lr)) return true;
    }

    return false;
  }




  //debugging method
  String cross(double xx, double yy) {
    int x = (int)xx;
    int y = (int)yy;
    return "<line><position>" + x + "," + (y-30) + " " + x + "," + (y+30) + "</position><above>true</above><width>5</width></line>\n" +
           "<line><position>" + (x-30) + "," + y + " " + (x+30) + "," + y + "</position><above>true</above><width>5</width></line>\n";
  }





















  //LuxMapGenerator methods://////////////////////////////////////////////////////

  public String message(String message, Object data) { 
  	if ("scenarioPlayerCount".equals(message))
		{
		return "6";
		}

	return null;
	}

  public String name() { return "CrystalInfinity"; }

  public java.util.List getChoices() {
    Vector choices = new Vector();
    choices.add(CHOICE_SMALL);
    choices.add(CHOICE_MEDIUM);
    choices.add(CHOICE_LARGE);
    choices.add(CHOICE_EPIC);
    return choices;
  }

  public float version() { return 1.1f; }

  public boolean canCache() { return false; }

  public String description() { return "CrystalInfinity: Generates maps in the style of Sir Holo's Quasicrystal maps."; }

}












class MultigridLine {
  public double offset;
  public double angle;
  public double m, b; //from y = mx + b

  Vector<MultigridVertex> vertices = new Vector<MultigridVertex>();


  public MultigridLine(double angle, double offset) {
    this.angle = angle;
    this.offset = offset;
    m = Math.tan(angle);
    b = offset*(Math.sin(angle+Math.PI/2) + Math.cos(angle+Math.PI/2)/Math.tan(angle+Math.PI/2));
  }



  public static void addIntersection(MultigridLine a, MultigridLine b) {
    MultigridVertex v = new MultigridVertex(a, b);
    a.addVertex(v);
    b.addVertex(v);
  }


  void addVertex(MultigridVertex v) {
    for(int i = 0; i < vertices.size(); i += 1) {
      if(vertices.get(i).x > v.x) {
        vertices.add(i, v);
        return;
      }
    }

    vertices.add(v); //v.x is > than all others
  }

  public void connectVertices() {
    for(int i = 0; i < vertices.size() - 1; i += 1) {
      MultigridVertex.addConnection(vertices.get(i), vertices.get(i+1));
    }
  }

}


class MultigridVertex {
  MultigridLine a, b;
  double x, y;

  Vector<MultigridVertex> connectedVertices = new Vector<MultigridVertex>();
  Vector<Double> angles = new Vector<Double>();


  public MultigridVertex(MultigridLine a, MultigridLine b) {
    this.a = a;
    this.b = b;
    x = (b.b - a.b)/(a.m - b.m);
    y = a.m * x + a.b;
  }

  public static void addConnection(MultigridVertex a, MultigridVertex b) {
    a.addConnection(b);
    b.addConnection(a);
  }

  void addConnection(MultigridVertex v) {
    double angle = Math.atan2(v.y - y, v.x - x);
    for(int i = 0; i < angles.size(); i += 1) {
      if(angles.get(i).doubleValue() > angle) {
        angles.add(i, new Double(angle));
        connectedVertices.add(i, v);
        return;
      }
    }
    //new max angle
    angles.add(new Double(angle));
    connectedVertices.add(v);
  }


  public Rhombus unitRhombus() {
    double[] xpoints = new double[4];
    double[] ypoints = new double[4];

    double x = 0;
    double y = 0;

    xpoints[0] = x;
    ypoints[0] = y;

    double dir = Math.PI/2 + angles.get(0).doubleValue();
    x += 50*Math.cos(dir);
    y += 50*Math.sin(dir);

    xpoints[1] = x;
    ypoints[1] = y;

    dir = Math.PI/2 + angles.get(1).doubleValue();
    x += 50*Math.cos(dir);
    y += 50*Math.sin(dir);

    xpoints[2] = x;
    ypoints[2] = y;

    dir = Math.PI/2 + angles.get(2).doubleValue();
    x += 50*Math.cos(dir);
    y += 50*Math.sin(dir);

    xpoints[3] = x;
    ypoints[3] = y;


    return new Rhombus(xpoints, ypoints);
  }

  public Rhombus attachRhombus(MultigridVertex v, Rhombus vRhombus) {
    Rhombus ourRhombus = unitRhombus();

    int ourIndexToV = -1;
    for(int i = 0; i < connectedVertices.size(); i += 1) {
      if(connectedVertices.get(i) == v) ourIndexToV = i;
    }

    int vIndexToUs = -1;
    for(int i = 0; i < v.connectedVertices.size(); i += 1) {
      if(v.connectedVertices.get(i) == this) vIndexToUs = i;
    }

    if(vIndexToUs*ourIndexToV < 0) System.out.println("ERROR 3057298953");

    int ourRhombusPointIndex = ourIndexToV;
    int vRhombusPointIndex = (vIndexToUs+1)%4;

    double dx = vRhombus.xpoints[vRhombusPointIndex] - ourRhombus.xpoints[ourRhombusPointIndex];
    double dy = vRhombus.ypoints[vRhombusPointIndex] - ourRhombus.ypoints[ourRhombusPointIndex];

    ourRhombus.translate(dx, dy);

    return ourRhombus;
  }





}



class Rhombus {
  double[] xpoints;
  double[] ypoints;

  int continent = 0;

  public Rhombus(double[] xpoints, double[] ypoints) {
    this.xpoints = xpoints;
    this.ypoints = ypoints;
  }

  public void translate(double dx, double dy) {
    for(int i = 0; i < 4; i += 1) {
      xpoints[i] += dx;
      ypoints[i] += dy;
    }
  }


  public double distanceFrom(double x, double y) {
    double cX = 0;
    double cY = 0;
    for(int i = 0; i < 4; i += 1) {
      cX += xpoints[i];
      cY += ypoints[i];
    }

    cX /= 4;
    cY /= 4;

    return Math.sqrt((cX-x)*(cX-x) + (cY-y)*(cY-y));
  }

  public double centerX() {
    double cX = 0;
    for(int i = 0; i < 4; i += 1) {
      cX += xpoints[i];
    }

    cX /= 4;
    return cX;
  }

  public double centerY() {
    double cY = 0;
    for(int i = 0; i < 4; i += 1) {
      cY += ypoints[i];
    }

    cY /= 4;
    return cY;
  }


  public Polygon getIntPolygon() {
    int[] ixpoints = new int[4];
    int[] iypoints = new int[4];
    for(int i = 0; i < 4; i += 1) {
      ixpoints[i] = (int)xpoints[i];
      iypoints[i] = (int)ypoints[i];
    }

    return new Polygon(ixpoints, iypoints, 4);
  }

}


