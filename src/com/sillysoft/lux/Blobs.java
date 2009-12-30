package com.sillysoft.lux;

import com.sillysoft.lux.util.*;
import java.util.*;
import java.io.PrintWriter;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

//
//  Blobs.java
//  Lux
//
//  Copyright (c) 2002-2007 Sillysoft Games. 
//	http://sillysoft.net
//	lux@sillysoft.net
//
//	This source code is licensed free for non-profit purposes. 
//	For other uses please contact lux@sillysoft.net
//

/**
  Blobs is an implementation of the MapGenerator interface.
<br>	It is used to create the built-in random maps in Lux.
<br>  
<br>	Sometimes described as "potato blob" output. The general strategy is as follows:
<br>	
<br>		- Pick a width and height to use as the 2D field.
<br>		- Pick a random point inside the field. This will serve as a centerpoint of a country.
<br>		- Using polar co-ordinates, a randomly changing radius is drawn around the center to select the points that form into a country.
<br>		-- Pick a point near outside of the country we just drew.
<br>		-- Use this point as a centerpoint of another country. When a newly created point falls inside a previous shape move the point to follow outside the border of that country.
<br>		-- Repeat picking nearby points and drawing countries for a while. This will form a continent of countries.
<br>		- Then start fresh by picking a brand new point on the field. If inside another country pick again. Start a country around it and more countries near it. Repeat this a few times to grow some continents.
<br>		
<br>		- After the desired number of countries have been created then countries distance are compared. If they are close enough they connect.
<br>		
<br>		- More connections (along with lines indicating them) are then made over the board so that all the countries are connected.				
								
												*/

public class Blobs implements LuxMapGenerator
{
private MapLoader loader;
private Random rand;	// the random number generator
private boolean playerInfoInsideMap; // Switch to give room for the PI

private int topx, topy;		// the maximum drawing bounds

// These variables control how smooth the random shapes are.
private int variance = 3; 					// The maximum distance away nearNumber() will return
private double fullCircle = 2*Math.PI;
private double thetaStep = fullCircle/20; 	// The step size of the angle
private int minRadius = 13; 					// the minimum the radius is allowed to be when drawing shapes


// To encapsulate the getShpaeAround functions we store the array of shapes:
private GeneralPath[] shapes;
private Rectangle2D[] shapeBounds; // we keep a cache of the bounds of each shape
private int shapeCount;
private Vector lines;	// the lines that the LuxView will have to draw

private boolean[] connected;	// When making connections, we remember what is connected so far

private int numCountries;
private Country[] countries;
private int[] contCodes;		// the continent code for each country

private int numContinents;
private int[] contBonus;	// each continent has a bonus value associated with it

// Because there doen't seem to be a way to get the actual points of an GeneralPath in java, we keep a vector of points for each shape. One Vector for each shape. They will be filled with Point2Ds.
private Vector[] points;

private Hashtable allPoints;	// the points are the keys, thenumber of shapes touching that point is the value

private int[][] distanceMemory;	// the closest distance between shapes

private String boardSize;   // a value from the choices array

private static List choices;

public String name()
	{
	return "Blobs";
	}

public float version()
	{
	return 2.0f;
	}

public String description()
	{
	return "Blobs is the class that Lux uses to generate the tiny-huge maps.";
	}

public List getChoices()
	{
	if (choices == null)
		{
		choices = new Vector();
		choices.add(name()+" - tiny");
		choices.add(name()+" - small");
		choices.add(name()+" - medium");
		choices.add(name()+" - large");
		choices.add(name()+" - huge");
		}
	return choices;
	}

public boolean generate(PrintWriter out, String choice, int seed, MapLoader loader)
	{
	this.loader = loader;
	boardSize = choice;
	rand = new Random(seed);
	playerInfoInsideMap = loader.isPlayerInfoInsideMap();

	generateBoard();
	this.loader = null;
	boolean finished = saveBoard(out, boardSize+" ID#"+seed);
	if (! finished)
		return generate(out, choice, seed+1, loader);
	else
		return true;
	}

public boolean canCache()
	{
	return false;
	}

/**
The main method that controls the generation of a map.
Note that rand and boardSize must be set before this is called.		*/
public void generateBoard()
	{
	//debug("Starting generateBoard. boardSize: "+boardSize);
	/********************
	To create a random board the following things must be done:
		-> clear the CreateBoard and initialize vars
		-> Determine the number of countries
		and For each country:
			-> Generate a shape
			-> determine the adjoinging list
			-> set the continent code

		-> choose the bonus values for each continent
		-> make any extra lines to connect countries
	********************/

	// The number of countries depend on the size of the board:
	numCountries = getNumberOfCountriesForSize(boardSize);

	initialize();

	topx = getWidthForSize(boardSize);
	topy = getHeightForSize(boardSize);

	// Do the shapes now.
	// Pick random points and draw shapes around them.
	while (shapeCount < numCountries)
		{
		generateNugget();
		}

	// so all the shapes are picked. 
	loader.setLoadText("adding easy connections");

	// add connections between shapes that are very close together
	connectShapesAt(5);	// this will cause them to be in the same continent

	loader.setLoadText("choosing continent nuggets");

	// The shapes have now been completed. Some close connection have been made.
	// Here we expand those trees into continents.

	// we mark the countries we have assigned to conts
	contCodes = new int[numCountries];
	connected = new boolean[numCountries];	//used to remember who has been given a continent (markConnectedFrom() and others use this array)
	for (int i = 0; i < numCountries; i++) 
		{
		connected[i] = false;
		contCodes[i] = -1;
		}

	// try and make continents have about this number of countries
	int averageContinentCountries = 5;
	numContinents = (int)Math.ceil((double)numCountries/(double)averageContinentCountries);
	// NOTE: numContinents may become smaller, if we don't find that many nuggets

	// the method: cycle through the countries and find the biggest tree without a cont code
	// give that tree a continent code
	// repeat till all countries have a cont code or we have chosen the desired number of conts
	for (int nextContCode = 0; nextContCode < numContinents; nextContCode++)
		{
		int biggestTreeSize = 0;
		int inBiggestTree = -1;

		for (int i = 0; i < numCountries; i++) 
			{
			if (contCodes[i] == -1)
				{
				Vector tree = getTouching(i);
				if (tree.size() > biggestTreeSize)
					{
					biggestTreeSize = tree.size();
					inBiggestTree = i;
					}
				}
			}

		if (inBiggestTree != -1)
			{
// xxxx this would be the place to break up big continents into smaller ones...
			// so mark the tree as the nugget to start this cont:
			markConnectedFrom(inBiggestTree);
			markContinentCodeFrom(inBiggestTree, nextContCode);
			}
		else
			{
			// then everything has been given a continent.
			numContinents = nextContCode;	// this will break us out of thr for loop
			}
		}

// Now numContinents is garanteed to be its final value.

String currentLoadText = new String ("building up nuggets");
loader.setLoadText(currentLoadText);

// So now we have numContinents nuggets to build on.
// cycle through the countries, making connections from unassigned countries, until everything has a continent.

while ( ! isFullyConnected() ) // NOTE: the isFullyConnected() function just tests that everything has been marked as connected. the graph will not be fully connected when the while loop exits.
	{
	// pick a random country that still has no continent.
	int from = rand.nextInt(numCountries);
	while (contCodes[from] != -1)
		from = (from+1) % numCountries;

	// now connect it to the closest possible country.
	int closestShape = -1;
	int closestDistance = 1000000;
	for (int j = 0; j < countries.length; j++)
		{
		if (from != j && ! countries[from].canGoto(j))
			{
			int distance = distanceBetween(from, j);
			if (distance < closestDistance && lineCanExistBetween(from,j) )
				{
				closestDistance = distance;
				closestShape = j;
				}
			}
		}

	if (closestShape != -1)
		{
		// then connect the unassigned country to it's closest neighbor
		makeCountriesTouch(from, closestShape);
		addLineBetweenShapes(from, closestShape);

		// if we connected it to a shape with a contCode then we get that contCode too.
		if (contCodes[closestShape] != -1)
			{
			markConnectedFrom(from);
			markContinentCodeFrom(from, contCodes[closestShape]);
			}
		}
	else
		{
		System.out.println("ERROR in Blobs.generateBoard() -> (closestShape == -1) while building up nuggets");
		System.out.println("	-> from = "+from);

		// HACK: create a new continent with this shape...
		markConnectedFrom(from);
		markContinentCodeFrom(from, numContinents);
		numContinents++;
		}
	currentLoadText = currentLoadText+".";
	loader.setLoadText(currentLoadText);
	}

// good. the world is now fully divided into continents.

// since the continents are now finalized we can create bonus values for them all
// for now the bonus is just the # of countries in the cont.
contBonus = new int[numContinents];
for (int i = 0; i < numContinents; i++)
	{
	int size = BoardHelper.getContinentSize( i, countries );
	contBonus[i] = size;
	}

currentLoadText = new String("fully connecting");
loader.setLoadText(currentLoadText);

// now the only thing left to do is ensure that the graph is fully connected.
// we want to do it using the smallest possible connections.

// first add all the tiny edges possible:
connectShapesAt(12);

// now the harder part:
// we must clear the connected memory
for (int i = 0; i < numCountries; i++)
	connected[i] = false;
markConnectedFrom(0);


// first connect continents that are close
connectContinentsAt(50);

// Now we must fullt connect the graph.
// Start by getting the connected graph from shape 0.
Vector tree = getTouching(0);
// And add non-reachable nodes until everything is connected...
while (tree.size() < numCountries)
	{
	// In each iteration we should connect the closest shape that is not in <tree> to a shape in <tree>
	int closestShapeFrom = -1, closestShapeTo = -1;
	int closestDistance = 1000000;

	for (int i = 0; i < tree.size(); i++)
		{
		int in = ((Country)tree.get(i)).getCode();
		for (int out = 0; out < numCountries; out++)
			{
			if ( ! tree.contains( countries[out] ) )
				{
				// then consider connecting <out> to <in>
				int dist = distanceBetween(in,out);
				if (dist < closestDistance && lineCanExistBetween(in,out) )
					{
					closestDistance = dist;
					closestShapeFrom = in;
					closestShapeTo = out;
					}
				}
			}
		}

	if (closestShapeFrom == -1)
		{
		// this should never happen...
		System.out.println("ERROR in Blobs.generateBoard() -> (closestShapeFrom == -1) while fully connecting");
		break;	// the board won't be fully connected, but what else can we do?
		}
	else
		{
		makeCountriesTouch(closestShapeFrom, closestShapeTo);
		addLineBetweenShapes(closestShapeFrom, closestShapeTo);
		tree = getTouching(0);

		// some user feedback
		currentLoadText = currentLoadText+".";
		loader.setLoadText(currentLoadText);
		}

	}
	// now our graph is fully connected
	// whew

	// in order to make a more globe-like world we would also like to make a 
	// connection crossing over the edges of the board
	// (like the alaska-russia connection in Risk).
	double lowWrapDistance = 1000000;
	int lowShape = -1, highShape = -1;
	for (int i = 0; i < numCountries; i++)
		{
		for (int j = 0; j < numCountries; j++)
			{
			double dist = wrappedDistance(i,j);
			if (dist < lowWrapDistance)
				{
				lowWrapDistance = dist;
				if (shapeBounds[i].getX() < shapeBounds[j].getX())
					{
					lowShape = i;
					highShape = j;
					}
				else
					{
					lowShape = j;
					highShape = i;
					}
				}
			}
		}


	// Now we have the shapes to connect
	makeCountriesTouch(lowShape, highShape);

	// now we just have to find the shortest wrapped line and add it...
	Point2D lowPoint = null, highPoint = null;
	double smallestDist = 1000000;
	for (int i = 0; i < points[lowShape].size(); i++)
		for (int j = 0; j < points[highShape].size(); j++)
			{
			// pretend the low-shape is actually really high
			Point2D mapHigherPoint = new Point2D.Double(((Point2D)points[lowShape].get(i)).getX()+topx, ((Point2D)points[lowShape].get(i)).getY());
			double dist = mapHigherPoint.distance((Point2D)points[highShape].get(j));
			if (dist < smallestDist)
				{
				smallestDist = dist;
				lowPoint = (Point2D)points[lowShape].get(i);
				highPoint = (Point2D)points[highShape].get(j);
				}
			}

	// so we have the points, map them and add the lines...
	Point2D mapHigherPoint = new Point2D.Double(lowPoint.getX()+topx, lowPoint.getY());
	Point2D mapLowerPoint = new Point2D.Double(highPoint.getX()-topx, highPoint.getY());

	lines.add( new Line2D.Float(highPoint, mapHigherPoint) );
	lines.add( new Line2D.Float(mapLowerPoint, lowPoint) );

	//report for testing purposes
	//debug("GenReport -> size: "+boardSize+", numShapes: "+numCountries+", conts: "+numContinents);
	}

private int getNumberOfCountriesForSize(String boardSize)
	{
	if ( boardSize.contains("tiny") )
		{
		return 8 + rand.nextInt(11); // 8 to 18
		}
	else if ( boardSize.contains("small") )
		{
		return 15 + rand.nextInt(11); // 15 to 25
		}
	else if ( boardSize.contains("large") )
		{
		return 25 + rand.nextInt(21); // 25 to 45
		}
	else if ( boardSize.contains("huge") )
		{
		return 40 + rand.nextInt(31); // 40 to 70
		}
	else
		{	// it's medium (or undefined)
		return 20 + rand.nextInt(16); // 20 to 35
		}
	}
	
public static int getWidthForSize(String boardSize)
	{
	if (boardSize.contains("tiny"))
		{
		return 600;
		}
	else if (boardSize.contains("small"))
		{
		return 675;
		}
	else if (boardSize.contains("large"))
		{
		return 1000;
		}
	else if (boardSize.contains("huge"))
		{
		return 1300;
		}

	return 780;
	}

public static int getHeightForSize(String boardSize)
	{
	if (boardSize.contains("tiny"))
		{
		return 300;
		}
	else if (boardSize.contains("small"))
		{
		return 400;
		}
	else if (boardSize.contains("large"))
		{
		return 600;
		}
	else if (boardSize.contains("huge"))
		{
		return 700;
		}
	return 500;
	}

// gives the approximate wrapped distance between shapes i and j
private double wrappedDistance( int i, int j)
	{
	int lower, higher;	// one shape will be near the left and onr near the right side of the board

	if (shapeBounds[i].getX() < shapeBounds[j].getX())
		{
		lower = i;
		higher = j;
		}
	else
		{
		lower = j;
		higher = i;
		}

	// wrap a point on the bounds of the low shape to a position higher than the edge of the board
	Point2D mapHigherPoint = new Point2D.Double(shapeBounds[lower].getX()+topx, shapeBounds[lower].getY());
	Point2D highPoint = new Point2D.Double(shapeBounds[higher].getX()+shapeBounds[higher].getWidth(), shapeBounds[higher].getY());

	return mapHigherPoint.distance(highPoint);
	}

// initialize must ONLY be called after numCountries has been set
private void initialize()
	{
	shapeCount = 0;

	// we set up a variety of data structures here
	shapes = new GeneralPath[numCountries];
	shapeBounds = new Rectangle2D[numCountries];
	points = new Vector[numCountries];	// each shape also gets a vector of Point's that it is made of
	allPoints = new Hashtable();
	countries = new Country[numCountries];

	lines = new Vector();	// it holds ExtraLines

	distanceMemory = new int[numCountries][numCountries];
	// distanceMemory[][] is our memory of computed distances
	// it shouldn't be used until all the shapes are finished

	for (int i = 0; i<numCountries; i++)
		{
		points[i] = new Vector();
		countries[i] = new Country(i, -1, this);	// they have no continentCode yet

		for (int j = 0; j < numCountries; j++)
			distanceMemory[i][j] = -1;
		}
	}

// connects any shapes whose distance < <level>
private void connectShapesAt(int level)
	{
	for (int i = 0; i < numCountries; i++)
		for (int j = 0; j < numCountries; j++)
			{
			if (i != j && !countries[i].canGoto(countries[j]) && distanceBetween(i,j) < level)
				{
				makeCountriesTouch(i, j);
				addLineBetweenShapes(i, j);
				}
			}
	}

/** Connects any shapes that are in different continents and whose distance is below 'level'.	*/
private void connectContinentsAt(int level)
	{
	for (int i = 0; i < numCountries; i++)
		for (int j = 0; j < numCountries; j++)
			{
			if (i != j && !countries[i].canGoto(countries[j]) && countries[i].getContinent() != countries[j].getContinent() && distanceBetween(i,j) < level && lineCanExistBetween(i,j))
				{
				makeCountriesTouch(i, j);
				addLineBetweenShapes(i, j);
				}
			}
	}

/** Generates a couple shapes close together. */
private void generateNugget()
	{
	// pick a random point:
	double x = 30 + rand.nextInt(topx-60);
	double y = 30 + rand.nextInt(topy-60);

	// allow room for player info (if needed):
	if (pointIntersectsPlayerInfo(x, y))
		{	// start over
		System.out.println("Starting generateNugget() over again due to PI area intersection");
		generateNugget();
		return;
		}
		
	if (! createShapeAt( x, y ))
		{
		// bad shape. restart the nugget somewhere else
		generateNugget();
		return;
		}
	//debug("generateNugget put first shape at ("+x+", "+y+")");

	// Otherwise a shape was created.
	// now make some other shapes near to this nugget...
	int nuggetBase = shapeCount-1;
	Vector nuggetShapeIndexes = new Vector();
	nuggetShapeIndexes.add(new Integer(nuggetBase));
	int desiredContSize = 2 + rand.nextInt(8);

	for (int tries = 0; nuggetShapeIndexes.size() < desiredContSize && tries < 30 && shapeCount < numCountries; tries++)
		{
		// to start with, we try shapes close to the first shape
		int closeToShape = nuggetBase;

		Point2D randPoint = (Point2D)points[closeToShape].get( rand.nextInt(points[closeToShape].size()) );
		if (shapes[closeToShape].contains( randPoint.getX()-5, randPoint.getY() ) )
			{
			x = randPoint.getX()+(minRadius*2);
			y = randPoint.getY();
			}
		else
			{
			x = randPoint.getX()-(minRadius*2);
			y = randPoint.getY();
			}

		// we have the new point. try a shape there:
		if (createShapeAt( x, y ))
			{
			// good shape
			nuggetShapeIndexes.add(new Integer(shapeCount-1));
			}
		}
	//debug(" -> placed "+(shapeCount-nuggetBase)+" shapes total in this nugget");
	}

private boolean createShapeAt( double x, double y )
	{
	generateNextShapeAroundPoint(x, y);

	if (shapes[shapeCount] != null)
		shapeBounds[shapeCount] = shapes[shapeCount].getBounds2D();

	// keep it if a shape was drawn around this point and it is not too small and not too big
	if (shapes[shapeCount] != null && 
		shapeBounds[shapeCount].getWidth() > 40 && 
		shapeBounds[shapeCount].getHeight() > 40 && 
		shapeBounds[shapeCount].getWidth() < topx/3 && 
		shapeBounds[shapeCount].getHeight() < topy/3) 
		{
		// because generateShapeAroundPoint() only sets up adjoingingLists one-way, we must add the reverse connections here:
		addReverseLinks(countries[shapeCount]);
		// Add the shapes point to the allPoints hashtable
		for (int p = 0; p < points[shapeCount].size(); p++)
			{
			Object key = allPoints.get(points[shapeCount].get(p));
			if (key == null)
				allPoints.put(points[shapeCount].get(p), new Integer(1));
			else
				{
				allPoints.put(points[shapeCount].get(p), new Integer( ((Integer)key).intValue() + 1 ));
				}
			}

		shapeCount++;
		loader.setLoadText("creating board. shapeCount -> "+shapeCount+"/"+numCountries);
		return true;
		}
	else
		{
		// we are ditching the shape, so clear the adjoingList:
		countries[shapeCount].clearAdjoiningList(this);
		points[shapeCount] = new Vector();
		return false;
		}
	}


/** Generate the next country shape around the given point. It will fill in shapes[shapeCount] - possibly with null if it fails to make a shape. */
public void generateNextShapeAroundPoint( double pointx, double pointy )
	{
	Point2D shapeOrigin = new Point2D.Double(pointx, pointy);
	if ( isInShapes( shapeOrigin ) != -1)
		{
		//debug(" abort -> started in another shape");
		shapes[shapeCount] = null;
		return; // we don't want to start inside another shape
		}

	//debug("generateNextShapeAroundPoint called. shape="+shapeCount+", shapeOrigin="+shapeOrigin);

	// We will return <shape>. create it here:
	shapes[shapeCount] = new GeneralPath();
	GeneralPath shape = shapes[shapeCount];

	// The first point in the shape is a special case
	double theta = 0.001;
	double radius = 35 + rand.nextInt(30); // between 35 and 65
	Point2D p = pointFromPolar(shapeOrigin, theta, radius);

	// Make sure that putting the first point there won't jump over another shape
	Point2D mid = getMiddlePoint(shapeOrigin, p);
	if ( isInShapes(mid) != -1 )
		{
		p = mid;
		radius = shapeOrigin.distance( p );
		}

	// Bring it closer until there is no conflict.
	while ( radius >= minRadius && isInShapes(p) != -1 )
		{
		radius -= 5;
		p = pointFromPolar(shapeOrigin, theta, radius);
		}

	if (radius < minRadius)
		{
		shapes[shapeCount] = null;
		return;
		}

	if (! validPoint(p) )
		{
		shapes[shapeCount] = null;
		return;
		}

	// So now we have a good first point
	points[shapeCount].add(p);
	shape.moveTo((float)p.getX(), (float)p.getY());

	// used for radius convergence
	double initialRadius = radius;

	// Start drawing the circle...
	while (theta < 6.2 && theta != 0)	// this is close to 2 pi
		{
		// Set the radius and theta to the correct values for the last point added
		radius = shapeOrigin.distance( shape.getCurrentPoint() );
		if (radius < minRadius)
			{
			//debug("ABORT -> radius was smaller than min");
			// then we just placed a point with an invalid radius. abort
			shapes[shapeCount] = null;
			return;
			}
		theta = calcTheta(shapeOrigin, shape.getCurrentPoint());

		if (theta == -1)
			{
			//debug(" abort -> calcTheta returned -1 (at start of circle loop)");
			shapes[shapeCount] = null;
			return;
			}

		// and get the next point to add
		theta += thetaStep;
		radius = Math.max( nearNumber(radius), minRadius); // a slightly random radius, above the minimum

		if (theta > fullCircle-thetaStep || theta == 0)
			{
			// then we are done going around the circle. break out of the loop
			break;
			}

		if (theta > 4.712)	// 4.712 ~= to PI*1.5
			{
			// then we are in the last quarter of the circle shape.
			// ensure that the radius eventually converges to what it was for the first point.
			// The maximum difference is limited to 1 per 2 degrees away from the start
			// (ie when theta is 270 degrees, it is 90 degrees away from the start and the max difference is 45)
			double maxDifference = (double)(((fullCircle-theta)*360)/(2*fullCircle));
			maxDifference = Math.abs(maxDifference);
			if (initialRadius - radius > maxDifference)
				{
				//debug("converging radius1 from "+radius+" to "+(initialRadius - maxDifference));
				//debug("-> theta="+theta+", maxDifference="+maxDifference+", initialRadius="+initialRadius);
				radius = initialRadius - maxDifference;
				}
			else if (radius - initialRadius > maxDifference)
				{
				//debug("converging radius2 from "+radius+" to "+(initialRadius + maxDifference));
				radius = initialRadius + maxDifference;
				}
			}

		p = pointFromPolar(shapeOrigin, theta, radius);

		// Test to see if the new point conflicts with other shapes.
		int conflict = isInShapes(p);

		if (conflict == -1) 
			{
			// then there was no conflict with the point itself. 
			// but drawing the line to the point could still cause overlap. test for that here.
			mid = getMiddlePoint(shape.getCurrentPoint(), p);
			conflict = isInShapes(mid);
			}

		if ( conflict == -1 )
			{
			if (! validPoint(p) )
				{
				shapes[shapeCount] = null;
				return;
				}
//debug("      drawing the shape. adding a point with radius="+radius+", theta="+theta);
			// there was no intersection with a shape. thus p is a valid point. Add it to the shape.
			points[shapeCount].add(p);
			shape.lineTo((float)p.getX(), (float)p.getY());
			}
		else
			{
			// Then there was a conflict. Now the fun really begins

			// Since the point p has hit inside a shape, these two shapes will touch:
			countries[shapeCount].addToAdjoiningList( countries[conflict], this );

			// Make this shape follow the outline of the conflict shape for a bit.
			// We start by finding the border-point closest to shape's last point:
			int borderPoint = getClosestBorderNotInShape(shape.getCurrentPoint(), conflict, shapeCount);
			if (borderPoint == -1)
				{
				//debug("getClosestBorderNotInShape returned -1. abort the shape");
				shapes[shapeCount] = null;
				return;
				}

			// make p be the same point as the closest borderPoint and add it to the shape 
			p = (Point2D)points[conflict].get(borderPoint);
			points[shapeCount].add(p);
			shape.lineTo((float)p.getX(), (float)p.getY());

//debug(" --> CONFLICT with shape "+conflict+", shifting to borderPoint "+borderPoint);

			// So we have matched up one border point.
			// continue to follow the border until we hit an exit condition

			// Note: within the loop we need to know the radius and theta of the last point.
			// Set the initial values here 
			double lastRadius = shapeOrigin.distance(p);
			double lastTheta = calcTheta(shapeOrigin, p );

			if (lastTheta == -1)
				{
//debug(" abort -> calcTheta returned -1");
				shapes[shapeCount] = null;
				return;
				}

			boolean keepFollowingBorder = true;
			while ( keepFollowingBorder && theta < fullCircle-thetaStep && theta != 0 )
				{
				borderPoint--;	// this will make us choose the previous border point.
				if (borderPoint == -1)
					{
					borderPoint = points[conflict].size()-1; 
					//debug("wrapping to first border point");
					}

				p = (Point2D)points[conflict].get(borderPoint);

				// But we don't want to follow the border too far.
				// We only allow theta to backtrack if the radius also gets smaller

				// calculate the radius and theta:
				radius = shapeOrigin.distance(p);
				theta = calcTheta(shapeOrigin, p);

				if (theta == -1)
					{
					//debug(" abort -> calcTheta returned -1 (in 2nd place)");
					shapes[shapeCount] = null;
					return;
					}

				if (theta < lastTheta && lastRadius < radius) {
					// then theta is backtracking while the radius gets bigger.
					// this is an exit condition for following the border
					// NOTE: we don't even bother to add this point
					keepFollowingBorder = false;
//debug("exit condition 1");
					}
				else {
					// add it to the shape
					lastTheta = theta;
					lastRadius = radius;
					points[shapeCount].add(p);
					shape.lineTo((float)p.getX(), (float)p.getY());
//debug(" ---> keepFollowingBorder tick. borderPoint="+borderPoint);

					// If the point we just added is also in 2 or more other shapes then 
					// stop following this border
					Object key = allPoints.get(p);
					if (key != null && ((Integer)key).intValue() > 1)
						{
						//debug("exit condition 2");
						keepFollowingBorder = false;
						}
					}
				}	// end of keepFollowingBorder loop


			// So we are done following that shape for now.
			// We will continue drawing the circle now.

			}	// end of dealing with conflict point
		}	// end of stepTheta for-loop

	// So now we are done drawing our circle-ish shape.
	// the only thing left to do is close it.
	shape.closePath();

	// check if we swallowed an entire country
	// check against a box that is slightly bigger then the new shape
	Rectangle2D eatCheck = shape.getBounds2D();
	eatCheck = new Rectangle2D.Double(eatCheck.getX()-2, eatCheck.getY()-2, eatCheck.getWidth()+4, eatCheck.getHeight()+4);
	for (int i = 0; i < shapeCount; i++)
		{
		// check if we swallowed an entire country
		if (eatCheck.contains(shapeBounds[i]))
			{
			// then we have eaten shape i. abort this shape.
			//debug("oops, shapeCount = "+shapeCount+" almost swallowed "+i);
			shapes[shapeCount] = null;
			return;
			}
		}

	eatCheck = shape.getBounds2D();
	for (int i = 0; i < shapeCount; i++)
		{
		// check if we swallowed an entire country
		if (shapeBounds[i].contains( eatCheck ))
			{
			// then we have eaten shape i. abort this shape.
			//debug("oops, shapeCount = "+shapeCount+" was almost created inside "+i);
			shapes[shapeCount] = null;
			return;
			}
		}
		
	if (rectIntersectsPlayerInfo(eatCheck))
		{
		// then we intersect the player info inside the map. abort this shape.
		debug("shapeCount = "+shapeCount+" ("+shapes[shapeCount]+") aborted for being in the player info area");
		shapes[shapeCount] = null;
		return;
		}
		
	return;	// hooray
	}

private Point2D pointFromPolar( Point2D origin, double theta, double radius)
	{
	return new Point2D.Double(origin.getX() + (double)(Math.cos(theta)*radius), origin.getY() + (double)(Math.sin(theta)*radius));
	}

// returns -1 if there was an error
private double calcTheta( Point2D origin, Point2D p )
	{
	double lastTheta;
	double ydiff = p.getY() - origin.getY();
	double xdiff = p.getX() - origin.getX();
	double yabs = Math.abs(ydiff);
	double radius = p.distance( origin );
	if (radius <= 0)
		return -1;

	if (ydiff >= 0 && xdiff >= 0)
		lastTheta = Math.asin(yabs/radius); // quadrant 1
	else if (ydiff >= 0)
		lastTheta = Math.PI - Math.asin(yabs/radius); // quadrant 2
	else if (ydiff < 0 && xdiff < 0)
		lastTheta = Math.PI + Math.asin(yabs/radius); // quadrant 3
	else
		lastTheta = Math.PI*2 - Math.asin(yabs/radius); // quadrant 4

	return lastTheta;
	}

/** Ensure that all the countries that 'c' connects to also connect to 'c'.		*/
private void addReverseLinks(Country c)
	{
	Country[] adList = c.getAdjoiningList();
	if (adList != null)
		for (int i = 0; i < adList.length; i++)
			adList[i].addToAdjoiningList( c, this );
	}

private int getClosestBorderNotInShape(Point2D p, int shape, int notShape)
	{
	int closestPoint = -1;
	double dist,closestDistance = 100000000;
	for (int i = 0; i < points[shape].size(); i++)
		{
		Point2D next = (Point2D) points[shape].get(i);
		dist = p.distance( next );
		if (dist < closestDistance)
			{
			// We must check to make sure that this point is not in <notShape>
			boolean usePoint = true;
			for (int j = 0; j < points[notShape].size() && usePoint; j++)
				{
				Point2D point = (Point2D) points[notShape].get(j);
				if (next.getX() == point.getX() && next.getY() == point.getY())
					usePoint = false;
				}
			if (usePoint)
				{
				closestDistance = dist;
				closestPoint = i;
				}
			}
		}

	if (closestPoint == -1) {
		// this should never happen, because one of the points must be closest
		System.out.println("Error in CreateBoard.getClosestBorderNotInShape p="+p+", shape="+shape);
		}

	return closestPoint;
	}

/** Find the point in 'shape" closest to 'p'.	*/
private int getClosestBorder(Point2D p, int shape)
	{
	int closestPoint = -1;
	double dist,closestDistance = 100000000;
	for (int i = 0; i < points[shape].size(); i++)
		{
		Point2D next = (Point2D) points[shape].get(i);
		if (next.getX() != p.getX() || next.getY() != p.getY())	// this ensures that we don't return a point that is equal to <p>
			{
			dist = p.distance( next );
			if (dist < closestDistance)
				{
				closestDistance = dist;
				closestPoint = i;
				}
			}
		}

	if (closestPoint == -1) {
		// this should never happen, because one of the points must be closest
		System.out.println("Error in CreateBoard.getClosestBorder p="+p+", shape="+shape);
		}

	return closestPoint;
	}

/** Return a vector with all the Country's that can be reached from 'from'. */
private Vector getTouching(int from)
	{
	Vector touches = new Vector();
	touches.add( countries[from] );

	for ( int i = 0; i < touches.size(); i++ )
		{
		// load its neighbors to the Que
		Country[] lookTouches = ((Country)touches.get(i)).getAdjoiningList();
		if (lookTouches != null)
			{
			for (int n = 0; n < lookTouches.length; n++)
				{
				if ( ! touches.contains(lookTouches[n]) )
					{
					touches.add(lookTouches[n]);
					}
				}
			}
		}
	return touches;
	}

// this marks the connected[] array as true for any countries reachable from <from>
 private void markConnectedFrom(int from)
	{
	// We mark everything that this guy touches as connected:
	Vector toLookAt = new Vector();
	toLookAt.add( countries[from] );

	while ( toLookAt.size() > 0 )
		{
		// mark the shape at the front of the Que
		connected[ ((Country)toLookAt.get(0)).getCode() ] = true;
		// load its neighbors to the Que
		Country[] lookTouches = ((Country)toLookAt.get(0)).getAdjoiningList();
		if (lookTouches != null)
			{
			for (int i = 0; i < lookTouches.length; i++)
				{
				if ( ! connected[ lookTouches[i].getCode() ] )
					toLookAt.add(lookTouches[i]);
				}
			}
		// and pop the front
		toLookAt.removeElementAt(0);
		}
	}

private void markContinentCodeFrom(int from, int code)
	{
	// We mark everything that this guy touches as connected:
	Vector touches = getTouching(from);

	for (int i = 0; i < touches.size(); i++)
		{
		countries[((Country)touches.get(i)).getCode()].setContinentCode(code, this);
		contCodes[((Country)touches.get(i)).getCode()] = code;
		}
	}

private boolean isFullyConnected()
	{
	for (int i = 0; i < connected.length; i++)
		if (! connected[i])
			return false;

	return true;
	}

/** Return the ID number of the shape that the point is inside. If the point is free then it returns -1. */
private int isInShapes( Point2D p )
	{
	for (int i = 0; i < shapeCount; i++)
		{
		if (shapeBounds[i].contains(p) && shapes[i].contains(p))
			{
			return i;
			}
		}

	return -1;
	}

/** Return a random number near to 'near', depending on the class-variable 'variance'.	*/
public double nearNumber (double near )
	{
	// This gives a double with mean 0.0 and standard deviation 1.0
	double next = rand.nextGaussian();
	// But we want a variance of up to <variance>:
	next *= variance;
	// And centered around near:
	next += near;

	return (double) next;
	}

/** Returns false if the point is too close to the borders of the drawing area. */
private boolean validPoint( Point2D point )
	{
	double x = point.getX();
	double y = point.getY();

	int minX = 20, minY = 15, maxX = topx-20, maxY = topy-40;

	if (x < minX || y < minY || x > maxX || y > maxY)
		return false;

	return true;
	}

/** Tests to see if a line can safely be drawn between the two shapes.
Will return false if the line would cross any shapes or other lines.	*/
public boolean lineCanExistBetween( int s1, int s2 )
	{
	Point2D p1 = null;
	Point2D p2 = null;

	int smallDist = distanceBetween(s1, s2);
	int dist;
	// now find the points that are that distance apart (they would form the line)
	for (int i = 0; i < points[s1].size() && p1==null; i++)
		for (int j = 0; j < points[s2].size() && p1==null; j++)
			{
			dist = (int)((Point2D)points[s1].get(i)).distance((Point2D)points[s2].get(j));
			if (dist == smallDist)
				{
				p1 = (Point2D)points[s1].get(i);
				p2 = (Point2D)points[s2].get(j);
				}
			}

	// First check it against the other lines:
	for (int i = 0; i < lines.size(); i++)
		{
		Line2D check = (Line2D)lines.get(i);
		if ( check.intersectsLine(p1.getX(), p1.getY(), p2.getX(), p2.getY()) )
			{
			return false;
			}
		}

	// Dissallow lines that are inside the bounding box of other shapes.
	// this is an upper bound for intersecting the shapes themselves
	for (int i = 0; i < shapeCount; i++)
		{
		if (i != s1 && i != s2 && shapeBounds[i].intersectsLine(p1.getX(), p1.getY(), p2.getX(), p2.getY()))
			return false;
		}

	return true;
	}

/** Returns the point in between pa and p2. */
public Point2D getMiddlePoint( Point2D p1, Point2D p2 )
	{
	return new Point2D.Double( (p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2 );
	}

private void addLineBetweenShapes(int from, int to)
	{ 
	int smallDist = distanceBetween(from, to), dist;
	// now find the points that are that distance apart and connect them:
	for (int i = 0; i < points[from].size(); i++)
		{
		for (int j = 0; j < points[to].size(); j++)
			{
			dist = (int)((Point2D)points[from].get(i)).distance((Point2D)points[to].get(j));
			if (dist == smallDist)
				{
				lines.add( new Line2D.Double( (Point2D)points[from].get(i),  (Point2D)points[to].get(j)) );
				return;
				}
			}
		}
	}

/** Add the connection to the adjoiningLists. */
private void makeCountriesTouch(int from, int to )
	{
	countries[from].addToAdjoiningListBoth( countries[to], this );
	}

/** Computes the cloest distance between the two finished GeneralPaths. */
private int distanceBetween(int from, int to)
	{
	if (distanceMemory[from][to] == -1)
		{
		double smallDist = 1000000, dist;
		for (int i = 0; i < points[from].size(); i++)
			for (int j = 0; j < points[to].size(); j++)
				{
				dist = ((Point2D)points[from].get(i)).distance((Point2D)points[to].get(j));
				if (dist < smallDist)
					{
					smallDist = dist;
					}
				}
		distanceMemory[from][to] = (int)smallDist;
		distanceMemory[to][from] = (int)smallDist;
		}
	return distanceMemory[from][to];
	}

private String getRandomTheme()
	{
	int code = rand.nextInt(11);
	if (code < 5)
		return "Ocean";
	if (code < 8)
		return "Space";
	if (code < 10)
		return "Air";
	else
		return "Huh";
	}

public boolean saveBoard(PrintWriter file, String mapName)
	{
	if (numContinents < 2)
		{
		// fail on boards with only one continent
		System.out.println("Blobs is failing with only 1 continent created");
		return false;
		}

	file.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<luxboard>");
	file.println("<version>1.1</version>");
	file.println("<width>"+topx+"</width>");
	file.println("<height>"+topy+"</height>");

	file.println("<title>"+mapName+"</title>");
	file.println("<theme>"+getRandomTheme()+"</theme>");
	file.println("<author>Lux version "+loader.getLuxVersion()+"</author>");
	file.println("<email>lux@sillysoft.net</email>");
	file.println("<webpage>http://sillysoft.net</webpage>");
	file.println("<description>A randomly generated blob world.</description>");
	file.println("<playerInfoLocation>-2,-2</playerInfoLocation>");
	file.println("");

	ContinentIterator continentIter;
	int scenarioOwnerCount = getScenarioPlayerCountForSize(boardSize)-1;
	for (int i = 0; i < numContinents; i++)
		{
		file.println("<continent>");
		file.println("<bonus>"+contBonus[i]+"</bonus>");
		// Output all the countries in this continent
		continentIter = new ContinentIterator(i, countries);
		while (continentIter.hasNext())
			{
			Country c = continentIter.next();
			int code = c.getCode();
			// Build the polygon co-ordinate list:
			String pointList = "";
			for (int n = 0; n < points[code].size(); n++)
				pointList = pointList+(int)((Point2D)points[code].get(n)).getX()+","+(int)((Point2D)points[code].get(n)).getY()+" ";

			// Build the adjoining list:
			Country[] neighbors = c.getAdjoiningList();
			String adList = String.valueOf(neighbors[0].getCode());
			for (int n = 1; n < neighbors.length; n++)
				adList = adList+","+neighbors[n].getCode();

			file.println("<country><id>"+code +"</id><polygon>"+pointList +"</polygon><adjoining>"+adList +"</adjoining><initialOwner>"+scenarioOwnerCount+"</initialOwner></country>");
			scenarioOwnerCount--;
			if (scenarioOwnerCount < 0)
				scenarioOwnerCount = getScenarioPlayerCountForSize(boardSize)-1;
			}

		file.println("</continent>");
		}

	for (int i = 0; i < lines.size(); i++)
		{
		Line2D line = (Line2D)lines.get(i);
		file.println("<line><position>"+(int)line.getP1().getX()+","+(int)line.getP1().getY()+" "+(int)line.getP2().getX()+","+(int)line.getP2().getY()+"</position></line>");
		}

	file.println("</luxboard>");

	file.close();

	return true;
	}

private void debug(Object text)
	{
	System.out.println(text);
//	System.out.flush();
	}
	


public int getScenarioPlayerCountForSize(String boardSize)
	{
	if (boardSize.contains("tiny"))
		return 3;
	if (boardSize.contains("small"))
		return 4;
	if (boardSize.contains("medium"))
		return 4;
	if (boardSize.contains("large"))
		return 5;
	if (boardSize.contains("huge"))
		return 6;
		
	return 0;
	}
	
public String message( String message, Object data )
	{
	if ("scenarioPlayerCount".equals(message))
		{
		// How many players does the scenario for this size have?
		String choice = (String) data;
		return ""+getScenarioPlayerCountForSize(choice);
		}

	return null;
	}	
	
private boolean pointIntersectsPlayerInfo(double x, double y)
	{
	if (playerInfoInsideMap)
		{
		return (x < 300 && y > topy - 120);
		}
	return false;
	}

private boolean rectIntersectsPlayerInfo(Rectangle2D rect)
	{
	if (playerInfoInsideMap)
		{
		Rectangle2D pi = new Rectangle2D.Double(0, topy - 120, 300, 120);
		return rect.intersects(pi);
		}
	return false;
	}
	
}
