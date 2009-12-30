package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;

//
//  Chimera.java
//  Lux
//
//  Created by Dustin Sacks on 8/26/04.
//  Copyright (c) 2002-2007 Sillysoft Games. All rights reserved.
//

import java.util.*;
import java.io.*;

public class Chimera implements LuxAgent
{

// We use a backing agent to send many commands to.
// The type of backing agent is randomly picked at creation
protected LuxAgent backer;

// Use a static rand so that many instances will get different agent types
private static Random staticRand = new Random();

// We need a reference to the Board to send the dieing emote
private Board board;

// The list of possible agent types. Will get loaded from the prefs file.
static protected String[] possibleAgentTypes;

static protected String optionsFile = "Chimera.txt";

public Chimera()
	{}

public void setPrefs(int ID, Board board )
	{
	createOptionsFileIfNeeded();
	loadOptionsFileIfNeeded();
	
	this.board = board;
		
	// Try a maximum of 10 times to load an agent
	for (int i = 0; i < 10 && backer == null; i++)
		{
		try
			{
			String tryAgent = possibleAgentTypes[ staticRand.nextInt(possibleAgentTypes.length) ];
//			System.out.println("Try to load agent type "+tryAgent);
			backer = board.getAgentInstance(tryAgent);
			}
		catch (Throwable e)
			{
//			System.out.println("Chimera could not load a "+tryAgent+". Will try another type...");
			}
		}

	if (backer == null)
		backer = new Cluster();
		
//	System.out.println("Chimera has loaded a "+backer.name()); 
	
	backer.setPrefs(ID, board);
	}

public String name()
	{	return "Chimera";	}

public String realName()
	{	return backer.name();	}
	
public float version()
	{	return 1.2f;	}
public String description()
	{	return "Chimera has many different heads.";	}

public String message( String message, Object data )
	{	
	if ("youLose".equals(message))
		{
		board.sendEmote("reveals the shattered core of a "+backer.name(), this);
		}

	return backer.message(message, data);
	}
	
public String youWon()
	{ return backer.youWon()+"\n("+backer.name()+")"; }

// For all of the gameplay methods we just pass them to our backer:
public int pickCountry()
	{	return backer.pickCountry();	}
public void placeInitialArmies( int numberOfArmies )
	{	backer.placeInitialArmies(numberOfArmies);	}
public int moveArmiesIn( int countryCodeAttacker, int countryCodeDefender )
	{	return backer.moveArmiesIn(countryCodeAttacker, countryCodeDefender);	}
public void fortifyPhase()
	{	backer.fortifyPhase();	}
public void cardsPhase( Card[] cards )
	{	backer.cardsPhase(cards);	}
public void placeArmies( int numberOfArmies )
	{	backer.placeArmies(numberOfArmies);	}
public void attackPhase()
	{	backer.attackPhase();	}

protected void createOptionsFileIfNeeded()
	{
	File file = new File(board.getAgentPath() + optionsFile);
	if (! file.exists())
		{
		createOptionsFile();
		}
	}

protected void createOptionsFile()
    {
	try
        {
		PrintWriter p = new PrintWriter(new FileWriter(board.getAgentPath() + optionsFile));
		
		p.println("# Chimera configuration file");
		p.println("# Lines beginning with # are comments and will be ignored.");
		p.println();
		p.println("# List the bots you want Chimera to randomly select from, one per line:");
		p.println();
		
		
		p.println("Bort");
		p.println("Boscoe");
		p.println("BotOfDoom");
		p.println("Brainiac");
		p.println("EvilPixie");
		p.println("Killbot");
		p.println("Nefarious");
		p.println("Quo");
		p.println("Reaper");
		p.println("Sparrow");
		p.println("Trotsky");
		
		p.close();    
		}
	catch (IOException e)
		{
		System.out.println(name() + " createKeywordsFile() exception " + e);
		}
	}
	
protected void loadOptionsFileIfNeeded()
	{
	if (possibleAgentTypes == null)
		{
		// Try to load it in from the file
		BufferedReader inputStream = null;
        try
			{
            inputStream = new BufferedReader(new FileReader(board.getAgentPath() + optionsFile));
			
			Vector agentList = new Vector();
			String line;
			while ((line = inputStream.readLine()) != null)
				{
				if (! line.startsWith("#") && ! line.trim().equals(""))
					{
					agentList.add(line);
					}
				}
				
			possibleAgentTypes = new String[agentList.size()];
			for (int i = 0; i < possibleAgentTypes.length; i++)
				possibleAgentTypes[i] = (String) agentList.get(i);
			}
        catch (IOException e)
			{
            System.out.println(name() + " loadOptionsFileIfNeeded() exception " + e);
			}

		
        if (possibleAgentTypes == null)
			{
			// Set to the default set
			possibleAgentTypes = new String[] {
				"Boscoe",
				"EvilPixie",
				"Killbot",
				"Quo",
				"Nefarious",
				"BotOfDoom",
				"Brainiac",
				"Trotsky",
				"Reaper",
				"Bort",
				"Sparrow"
				};
			}
		
		}
	}

}
