package com.sillysoft.lux.agent;

import com.sillysoft.lux.*;
import com.sillysoft.lux.util.*;
import java.util.*;
import java.io.*;

//
//	Defender.java
//	Lux
//

public class Defender implements LuxAgent
{
    protected int ID; // This agent's ownerCode:
    protected Board board;
    protected Country[] countries;
    protected Random rand;
    protected int numCountries;
    protected int numContinents;
    protected int numPlayers;
    protected int moveTheseArmies = 0;
    protected Country[] homeCountries;
    protected Country[] targetCountries;
    protected boolean pickCountries = false;
    protected int armiesAvailable = 0;
    protected int lastAttacker = -1;
    
    // This number determines how cautious the bot is
      // 0.0 = attacks when 50% sure
      // 0.5 = attacks when 75% sure
      // 1.0 = attacks whens 99% sure
    protected float caution = 0.5f;
    
    // Defender variables
    protected boolean knowsHome = false;
    protected boolean winPref = false;
    protected boolean popPref = false;
    protected boolean killPref = false;
    protected boolean farmPref = false;
    protected int chatPref = 1; // The % chance that Defender will talk in the chat box
    protected int defaultChat = 1;
    protected String helpMessage;
    protected String statusMessage;
    static boolean helpMsg = false; 

    public Defender()
    {
        rand = new Random();
        getHelpMessage();
    }
    
    public void setPrefs(int newID, Board theboard)
    {
        ID = newID;
        board = theboard;
        countries = board.getCountries();
        numCountries = countries.length;
        numContinents = board.getNumberOfContinents();
        numPlayers = board.getNumberOfPlayers();
        Country [] temp = getOurCountries(countries);
        if (temp.length > 0)
        {
            homeCountries = temp;
            knowsHome = true;
        }
        
        // Get user preferences
        getPrefs();
    }
    
    public String name()
    {
        return "Defender";
    }
    
    public float version()
    {
        return 2.2f;
    }

    public String description()
    {
        return "Defender works hard to protect the countries it starts with.";
    }

    public void debug(Object text)
    {
        System.out.println("("+ID+") "+board.getPlayerName(ID)+": "+text);
        System.out.flush();
    }

    public int pickCountry()
    {
        pickCountries = true;
        
        // Put the available continents into a list
        List availableContinents = new ArrayList();
        for (int i = 0; i < numContinents; i++)
        {
            boolean available = false;
            CountryIterator theseCountries = new ContinentIterator(i, countries);
            while (theseCountries.hasNext())
            {
                Country thisCountry = theseCountries.next();
                if (thisCountry.getOwner() == -1)
                {
                    available = true;
                    break;
                }
            }
            if (available)
            {
                availableContinents.add(i);
            }
        }
        
        // Put the continents we have at least one country in or has a negative bonus in this list
        List placedHere = new ArrayList();
        for (int i = 0; i < numContinents; i++)
        {
            boolean placed = false;
            CountryIterator theseCountries = new ContinentIterator(i, countries);
            if (board.getContinentBonus(i) < 0
              || !availableContinents.contains(i))
            {
                placed = true;
            }
            else
            {
                while (theseCountries.hasNext())
                {
                    Country thisCountry = theseCountries.next();
                    if (thisCountry.getOwner() == ID)
                    {
                        placed = true;
                        break;
                    }
                }
            }
            if (placed)
            {
                placedHere.add(i);
            }
        }
        
        if (placedHere.size() != numContinents)
        {
            // Fint the best available continent
            float maxRating = -1000000;
            int bestContinent = -1;
            for (int i = 0; i < numContinents; i++)
            {
                float thisRating = getRating(i);
                if (!placedHere.contains(i)
                  && thisRating > maxRating)
                {
                    maxRating = thisRating;
                    bestContinent = i;
                }
            }
            pickCountries = false;
            return getInnermostUnownedCountry(bestContinent).getCode();
        }
        else
        {
            // Picked at least one country in every good continent, so pick in the best continent available
            float maxRating = -1000000;
            int bestContinent = -1;
            for (int i = 0; i < numContinents; i++)
            {
                float thisRating = getRating(i);
                if (availableContinents.contains(i)
                  && thisRating > maxRating)
                {
                    maxRating = thisRating;
                    bestContinent = i;
                }
            }
            pickCountries = false;
            return getInnermostUnownedCountry(bestContinent).getCode();
        }
    }

    public void placeInitialArmies(int numberOfArmies)
    {
        placeArmies(numberOfArmies);
    }

    public void cardsPhase(Card[] cards )
    {
    }

    public void placeArmies(int numberOfArmies)
    {
        startMessage();
        if (board.getTurnCount() > 1)
        {
            chat();
        }
        
        //debug("placeArmies("+numberOfArmies+")");
        armiesAvailable = numberOfArmies;
        if (winPref
          && canWinGame())
        {
            // Place to win game
            numberOfArmies = placeToAttack(countries, numberOfArmies);
            homeCountries = countries;
        }
        else if (killPref)
        {
            // Place to kill a player
            //debug("  placeToKill("+numberOfArmies+")");
            numberOfArmies = placeToKill(numberOfArmies);
        }
        else
        {
            // Get list of home countries
            if (!knowsHome)
            {
                getHomeCountries();
            }
            
            // Place to defend home countries
            if (homeCountries != null)
            {
                //debug("  placeToDefend(homeCountries, "+numberOfArmies+")");
                numberOfArmies = placeToDefend(homeCountries, numberOfArmies);
                if (numberOfArmies < 1)
                {
                    return;
                }
            }
        }
        
        // Place to attack target countries
        getTargetCountries();
        if (targetCountries != null)
        {
            //debug("  placeToAttack(targetCountries, "+numberOfArmies+")");
            numberOfArmies = placeToAttack(targetCountries, numberOfArmies);
            if (numberOfArmies < 1)
            {
                return;
            }
        }

        if (popPref)
        {
            // Place to pop continents
            //debug("  placeToPop("+numberOfArmies+")");
            numberOfArmies = placeToPop(numberOfArmies);
            if (numberOfArmies < 1)
            {
                return;
            }
        }

        // Place remaining armies on the most vulnerable home borders
        Country[] homeBorders = getBorders(getOurCountries(homeCountries));
        //debug("  placeOnMostVulnerable(getBorders(homeCountries), "+numberOfArmies+")");
        placeOnMostVulnerable(homeBorders, numberOfArmies);

        // If we don't own any home countries place where our biggest army is located
        placeOnBiggestArmy(countries, numberOfArmies);
    }

    public void attackPhase()
    {
        //debug("attackPhase()");
        if(killPref)
        {
            //debug("  killPlayers()");
            killPlayers();
        }
        getTargetCountries();
        if (targetCountries != null)
        {
            //debug("  attackCountries(targetCountries)");
            attackCountries(targetCountries);
        }
        if (popPref)
        {
            //debug("  popContinents()");
            popContinents();
        }
        if (farmPref)
        {
            //debug("  attackForCard()");
            attackForCard();
        }
    }

    public int moveArmiesIn(int countryCodeAttacker, int countryCodeDefender)
    {
        // If defender doesn't have any hostile neighbors, don't move any in
        if (getEnemyAttackers(countries[countryCodeDefender]).length < 1)
	{
	    return 0;
	}

	// If attacker doesn't have any hostile neighbors, move all but 2 out
	else if (getEnemyAttackers(countries[countryCodeAttacker]).length < 1)
	{
	    int moveThis = countries[countryCodeAttacker].getArmies() - 2;
	    return moveThis;
	}
        
        return moveTheseArmies;
    }

    public void fortifyPhase()
    {
        fortifyToDefend(homeCountries);
    }

    public String youWon()
    {
	// For variety we store a bunch of answers and pick one at random to return.
	String[] answers = new String[] {
            "How in the HELL did Defender beat you?",
            "Just so you know, you lost to an easy bot..."
        };
	return answers[ rand.nextInt(answers.length) ];
    }
    
    public String message( String message, Object data )
    {
        if ("chat".equals(message))
        {
            handleChatMessage(data);
        }
        
        if ("attackNotice".equals(message))
        {
            List dataList = (List) data;
            lastAttacker = countries[((Integer)dataList.get(0)).intValue()].getOwner();
        }
        
        if ("youLose".equals(message))
        {
            sendLosingChat(data);
        }

        // Add countries we've lost in the first round to our list of home countries
        if ( !knowsHome
          && "attackNotice".equals(message)
          && board.getTurnCount() == 1)
        {
            List dataList = (List) data;
            int attackingCountryCode = ((Integer)dataList.get(0)).intValue();
            int defendingCountryCode = ((Integer)dataList.get(1)).intValue();
            
            List homeList = new ArrayList();
            if(homeCountries != null)
            {
                for (int i = 0; i <homeCountries.length; i++)
                {
                    homeList.add(homeCountries[i]);
                }
            }
            homeList.add(countries[defendingCountryCode]);
            
            // Remove duplicate continents
            Set set = new HashSet();
            set.addAll(homeList);
            if(set.size() < homeList.size())
            {
                homeList.clear();
                homeList.addAll(set);
            }
            homeCountries = convertListToCountryArray(homeList);
        }

        return null;
    }




//
// BOT SPECIFIC METHODS
//


    public void getHomeCountries()
    {
        // Don't look if we already know our home countries
        if(knowsHome)
        {
            return;
        }
        
        // Add any existing home countries
        List homeList = new ArrayList();
        if(homeCountries != null)
        {
            for (int i = 0; i < homeCountries.length; i++)
            {
                homeList.add(homeCountries[i]);
            }
        }
        
        // Add all countries we own
        for (int i = 0; i < numCountries; i++)
        {
            if (countries[i].getOwner() == ID)
            {
                homeList.add(countries[i]);
            }
        }
        homeCountries = convertListToCountryArray(homeList);
        knowsHome = true;
    }
    
    public void getTargetCountries()
    {
        List targetList = new ArrayList();
        if (homeCountries == null)
        {
            getHomeCountries();
        }
        for (int i = 0; i < homeCountries.length; i++)
        {
            if (homeCountries[i].getOwner() != ID)
            {
                targetList.add(homeCountries[i]);
            }
        }
        targetCountries = convertListToCountryArray(targetList);
    }  
    
    public void getHelpMessage()
    {
        helpMessage = newline+
          "## Defender Help ##"+newline+
          "Type the following commands in the chat box to change how Defender will play this game."+newline+
          "The default settings can be set in Defender.txt, located in the Lux > Support > Agents folder."+newline+
          ""+newline+
          "  Defender help  \t-->  This message will appear in the chat"+newline+
          "  Defender status\t-->  View the current Defender settings"+newline+
          "  Defender reset \t-->  Reset all options to defaults"+newline+
          "  Defender win   \t-->  Defender will try to win the game"+newline+
          "  Defender nowin \t-->  Defender will not try to win the game"+newline+
          "  Defender pop   \t-->  Defender will pop continents"+newline+
          "  Defender nopop \t-->  Defender will not pop continents"+newline+
          "  Defender kill     \t-->  Defender will kill players for cards"+newline+
          "  Defender nokill\t-->  Defender will not kill players for cards"+newline+
          "  Defender farm  \t-->  Defender will farm for cards"+newline+
          "  Defender nofarm\t-->  Defender will not farm for cards"+newline+
          "  Defender chat  \t-->  Defender will chat"+newline+
          "  Defender nochat\t-->  Defender will not chat"+newline+
          ""+newline+
          "http://sillysoft.net/wiki/?Defender"+newline
          ;
    }
    
    public void getStatusMessage()
    {
        String winText;
        String popText;
        String killText;
        String farmText;
        String chatText;
        
        if (winPref == false)
        {
            winText = "Defender will not try to win the game";
        }
        else
        {
            winText = "Defender will try to win the game";
        }
        
        if (popPref == false)
        {
            popText = "Defender will not pop continents";
        }
        else
        {
            popText = "Defender will pop continents";
        }
        
        if (killPref == false)
        {
            killText = "Defender will not kill players for cards";
        }
        else
        {
            killText = "Defender will kill players for cards";
        }
        
        if (farmPref == false)
        {
            farmText = "Defender will not farm for cards";
        }
        else
        {
            farmText = "Defender will farm for cards";
        }
        
        if (chatPref == 0)
        {
            chatText = "Defender will not chat";
        }
        else
        {
            chatText = "Defender will chat "+chatPref+"% of the time";
        }
        
        statusMessage = newline+
          "## Defender Status ##"+newline+
          ""+newline+
          winText+newline+
          popText+newline+
          killText+newline+
          farmText+newline+
          chatText+newline
          ;
    }
    
    public void handleChatMessage (Object data)
    {
        List dataList = (List) data;
        String from = (String) dataList.get(0);
        String chatText = (String) dataList.get(1);
        if ("Defender win".equalsIgnoreCase(chatText))
        {
            winPref = true;
            debug("Defender winGame = true");
            if (onlyBot())
            {
                board.sendEmote("will now try to win the game");
            }
        }
        else if ("Defender nowin".equalsIgnoreCase(chatText))
        {
            winPref = false;
            debug("Defender winGame = false");
            if (onlyBot())
            {
                board.sendEmote("will not try to win the game");
            }
        }
        else if ("Defender pop".equalsIgnoreCase(chatText))
        {
            popPref = true;
            debug("Defender popContinents = true");
            if (onlyBot())
            {
                board.sendEmote("will now pop continents");
            }
        }
        else if ("Defender nopop".equalsIgnoreCase(chatText))
        {
            popPref = false;
            debug("Defender popContinents = false");
            if (onlyBot())
            {
                board.sendEmote("will not pop continents");
            }
        }
        else if ("Defender kill".equalsIgnoreCase(chatText))
        {
            killPref = true;
            debug("Defender killForCards = true");
            if (onlyBot())
            {
                board.sendEmote("will now kill players for cards");
            }
        }
        else if ("Defender nokill".equalsIgnoreCase(chatText))
        {
            killPref = false;
            debug("Defender killForCards = false");
            if (onlyBot())
            {
                board.sendEmote("will not kill players for cards");
            }
        }
        else if ("Defender farm".equalsIgnoreCase(chatText))
        {
            farmPref = true;
            debug("Defender farmForCards = true");
            if (onlyBot())
            {
                board.sendEmote("will now farm for cards");
            }
        }
        else if ("Defender nofarm".equalsIgnoreCase(chatText))
        {
            farmPref = false;
            debug("Defender farmForCards = false");
            if (onlyBot())
            {
                board.sendEmote("will not farm for cards");
            }
        }
        if ("Defender chat".equalsIgnoreCase(chatText))
        {
            chatPref = defaultChat;
            if (onlyBot())
            {
                board.sendEmote("will now chat");
            }
        }
        if ("Defender nochat".equalsIgnoreCase(chatText))
        {
            chatPref = 0;
            if (onlyBot())
            {
                board.sendEmote("will not chat");
            }
        }
        if ("Defender reset".equalsIgnoreCase(chatText))
        {
            getPrefs();
            if (onlyBot())
            {
                board.sendEmote("has been reset to the default settings");
            }
        }
        else if ("Defender status".equalsIgnoreCase(chatText))
        {
            if (onlyBot())
            {
                debug("Defender was asked for status");
                getStatusMessage();
                board.sendChat(statusMessage);
            }
        }
        else if ("Defender help".equalsIgnoreCase(chatText))
        {
            if (onlyBot())
            {
                debug("Defender was asked for help");
                board.sendChat(helpMessage);
            }
        }
        else if (chatText.length() > 16)
        {
            if ("are you a truck?".equalsIgnoreCase(chatText.substring(chatText.length()-16))
              && onlyBot())
            {
                board.sendChat(newline+newline+newline+"No.");
            }
        }
    }
    
    public boolean onlyBot()
    {
        for (int i = 0; i < ID; i ++)
        {
            if (i == ID)
            {
                break;
            }
            if (board.getAgentName(i) == "Defender")
            {
                return false;
            }
        }
        
        return true;
    } // Returns true if there are no earlier instances of Defender in this game
    
    public void sendLosingChat(Object data)
    {
        String thisPlayer = board.getPlayerName(((Integer)data).intValue());
        int randomNum = rand.nextInt(10);
        
        if (randomNum > 4)
        {
            // For variety we store a bunch of retorts and pick one at random to return.
            String[] retorts = new String[] {
                "Damn it "+thisPlayer+"! Did you have to kill me?",
                "Ooo... "+thisPlayer+" likes to pick on easy bots.",
                "So tell me "+thisPlayer+", did that inflate your ego even more?",
                "Damn, I forgot to put on the PackTurtle Wax again!  You're lucky "+thisPlayer+"!",
                "Where did I put that Asshat Repellent?  It was made for players like "+thisPlayer+"...",
                "Now where did I put that Pickle Juice Sport? I bet "+thisPlayer+" stole it..."
            };

            board.sendChat(retorts[rand.nextInt(retorts.length)], this);
        }
        else
        {
            // For variety we store a bunch of emotes and pick one at random to return.
            String[] emotes = new String[] {
                "adds "+thisPlayer+" to his list of st00pid players",
                "mumbles about "+thisPlayer+"'s lucky dice",
                "curses "+thisPlayer+" and chugs the rest of his Drifter's Special Reserve Hard Lemonade",
                "throws his bottle of Cheesebeer at "+thisPlayer,
                "hands "+thisPlayer+" a copy of \"Zen and the Art of Suicide\""
            };

            board.sendEmote(emotes[rand.nextInt(emotes.length)], this);
        }
        
    } // Complains about the lost
    
    public void chat()
    {
        int randomNum = rand.nextInt(100);
        if (randomNum < chatPref
          && lastAttacker > -1)
        {
            String thisPlayer = board.getPlayerName(lastAttacker);
            randomNum = rand.nextInt(100);
            
            // Player specific chats
            if (playerChats(thisPlayer, randomNum))
            {
                return;
            }
            
            // General chats
            if (randomNum > 25)
            {
                // For variety we store a bunch of chats and pick one at random to return.
                String[] chats = new String[] {
                    "Okay "+thisPlayer+", just keep attacking me and see what happens...",
                    "Feel better "+thisPlayer+"?",
                    "So tell me "+thisPlayer+", do you like picking on easy bots?",
                    "Hey "+thisPlayer+", shouldn't you be at your Luxoholics Anonymous meeting?",
                    "So "+thisPlayer+", what are you compensating for?",
                    thisPlayer+", why don't you go read the forums for awhile and let your brain rot...",
                    "So tell me "+thisPlayer+", are you st00pid, or just plain stewpid?",
                    "An asshat and "+thisPlayer+" walk into a bar,  but I repeat myself...",
                    "Hey "+thisPlayer+", are you sure your real name isn't Richard?  Because you're sure playing like a dick...",
                    "Hey "+thisPlayer+", ask me if I'm a truck...",
                    "What happened "+thisPlayer+"?  Did your finger slip?",
                    "You know something "+thisPlayer+"?  You're almost as annoying as Nef...",
                    thisPlayer+", are you always this stupid or are you making a special effort today?",
                    thisPlayer+" doesn't know the meaning of fear, but then again "+thisPlayer+" doesn't know the meaning of most words...",
                    "I don't know what makes you so dumb "+thisPlayer+", but it really works!",
                    "Hey "+thisPlayer+", were your parents siblings?",
                    thisPlayer+", "+thisPlayer+", "+thisPlayer+"... when will you ever learn?",
                    "Go ahead "+thisPlayer+", tell us everything you know.  It'll only take ten seconds...",
                    "I bet "+thisPlayer+"'s mother has a loud bark!",
                    "I'm busy now "+thisPlayer+", can I ignore you some other time?",
                    "If ignorance is bliss, "+thisPlayer+" must be the happiest person alive!",
                    "Keep talking "+thisPlayer+", someday you'll say something intelligent.",
                    "Hey everybody, learn from "+thisPlayer+"'s parents' mistake...  use birth control!",
                    "You know "+thisPlayer+", they say two heads are better than one.  In your case one would have been better than none."
                };

                board.sendChat(chats[rand.nextInt(chats.length)], this);
            }
            else
            {
                // For variety we store a bunch of emotes and pick one at random to return.
                String[] emotes = new String[] {
                    "looks up the definition of asshat, and sees a picture of "+thisPlayer,
                    "mumbles about "+thisPlayer+" playing like a n00b",
                    "sips his Drifter's Special Reserve Hard Lemonade",
                    "looks for his container of PackTurtle Wax",
                    "throws some *Manimal Snacks at "+thisPlayer,
                    "ponders smashing "+thisPlayer+"'s will to live",
                    "thinks "+thisPlayer+" is acting like a Nazi, then remembers Godwin's Law and curses himself",
                    "has a feeling that "+thisPlayer+"'s birth certificate is an apology from the condom factory"
                };

                board.sendEmote(emotes[rand.nextInt(emotes.length)], this);
            }
        }
    }
    
    public boolean playerChats(String thisPlayer, int randomNum)
    {
        // dustin
        if ("dustin".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Whoah there Quasar!  Just because you created Lux doesn't mean you can just attack for no reason...", this);
            return true;
        }
        if ("dustin".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("wonders when dustin will stop using loaded dice", this);
            return true;
        }
        
        // el toro
        if ("el toro".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("KILL THE BULL!", this);
            return true;
        }
        if ("el toro".equals(thisPlayer)
          && randomNum < 10)
        {   
            board.sendEmote("mumbles something about mierda del toro", this);
            return true;
        }
        
        // BarStar
        if ("BarStar".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Hey Bar, so what's it like being married to el toro?", this);
            return true;
        }
        if ("BarStar".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("ponders about revealing BarStar's secret Dill Pickle obsession", this);
            return true;
        }
        
        // Packman
        if ("Packman".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Another st00pid move by Pack... the Packers must be losing again.", this);
            return true;
        }
        if ("Packman".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("wonders if Packman will nominate Defender for the Nobles Society", this);
            return true;
        }
        
        // Black Pope
        if ("Black Pope".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("What the hell BP?  I thought priests were supposed to be peaceful!", this);
            return true;
        }
        if ("Black Pope".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("checks the Black Pope-O-Meter", this);
            return true;
        }

        // Drifter
        if ("Drifter".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Hey Drifter, why don't you stop attackng me and go back to the strip club...", this);
            return true;
        }
        if ("Drifter".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("wonders how Drifter makes his Special Reserve Hard Lemonade so yellow", this);
            board.sendEmote("shudders", this);
            return true;
        }
        
        // AquaRegia
        if ("AquaRegia".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("No matter how many times you attack me, Aqua, I'm still your wife's favorite...", this);
            return true;
        }
        if ("AquaRegia".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("has a great idea for when to shove that red sharpie", this);
            return true;
        }
        
        // mbauer
        if ("mbauer".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Hey Mark!  Whatever happened to ANTI-HOLIES UNITE?", this);
            return true;
        }
        if ("mbauer".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("likes the way mb looks in the pink boa", this);
            return true;
        }
        
        // Preacherman
        if ("Preacherman".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Preach, why don't you stop hitting me and start removing all the spam in the forums.  Isn't there an easy way to delete all of your posts?", this);
            return true;
        }
        if ("Preacherman".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("thinks Preach is jealous because Nef is getting old & wrinkly", this);
            return true;
        }
        
        // MissChaos
        if ("MissChaos".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Hey MC, why don't you stop attacking me and start hitting on me instead?", this);
            return true;
        }
        if ("MissChaos".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("thinks MC is jealous because Paul is getting old & wrinkly", this);
            return true;
        }
        
        // Kef
        if ("Kef".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("Damn Kef, shouldn't you be spamming the forums?", this);
            return true;
        }
        if ("Kef".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("thinks everybody should gang up on the Belgian", this);
            return true;
        }
        
        // General K
        if ("General K".equals(thisPlayer)
          && randomNum > 90)
        {
            board.sendChat("TURN OFF THE DAMN CAPSLOCK GK!!!", this);
            return true;
        }
        if ("General K".equals(thisPlayer)
          && randomNum < 10)
        {
            board.sendEmote("mutters about the damn Kapitalist", this);
            return true;
        }

        return false;
    }
    
    public void startMessage()
    {
        if (!helpMsg)
        {
            board.sendChat("Type 'Defender help' for info on how to customize Defender");
            helpMsg = true;
        } 
    }




//
// BOARD INFORMATION
//


    public int getTotalArmies()
    {
        int result= 0;
        for(int i = 0; i < countries.length; i++)
        {
            result += countries[i].getArmies();
        }
        return result;
    } // Returns the total number of armies currently on the board
    
    public int getTotalArmies(int thisPlayer)
    {
        int result= 0;
        for(int i = 0; i < countries.length; i++)
        {
            if (countries[i].getOwner() == thisPlayer)
            {
                result += countries[i].getArmies();
            }
        }
        return result;
    } // Returns the total number of armies owned by this player currently on the board
    
    public int getTotalEnemyArmies(int thisPlayer)
    {
        int result= 0;
        for (int i = 0; i < countries.length; i++)
        {
            if (countries[i].getOwner() != thisPlayer)
            {
                result += countries[i].getArmies();
            }
        }
        return result;
    } // Returns the total number of armies not owned by this player currently on the board
    
    public int getTotalIncome()
    {
        int result = (int) numCountries / 3;
        for(int i = 0; i < numContinents; i++)
        {
            result += board.getContinentBonus(i);
        }
        return result;
    } // Returns the total possible income on this map
    
    public float getAverageEnemyIncome(int thisPlayer)
    {
        float result = 0f;
        for (int i = 0; i < numPlayers; i++)
        {
            if (i != thisPlayer)
            {
                result += (float)board.getPlayerIncome(i);
            }
        }
        result = result / ( (float) board.getNumberOfPlayersLeft() - 1f );
        return result;
    } // Returns the average income for enemies of this player

    public int[] getOwnedContinents(int thisPlayer)
    {
        List ownedContinents = new ArrayList();
        for (int i = 0; i < numContinents; i++)
        {
            if (BoardHelper.playerOwnsContinent(thisPlayer, i, countries))
            {
                ownedContinents.add(i);
            }
        }
        int[] result = convertListToIntArray(ownedContinents);
        return result;
    } // Returns an int[] of all continents owned by this player
    
    public Country[] getOurCountries(Country[] theseCountries)
    {
        List thisList = new ArrayList();
        CountryIterator ourCountries = new PlayerIterator(ID, theseCountries);
        while (ourCountries.hasNext())
        {
            thisList.add(ourCountries.next());
        }
        Country[] result = convertListToCountryArray(thisList);
        return result;
    } // Returns a Country[] of all countries we own within these countries
    
    public Country[] getPlayerCountries(int thisPlayer)
    {
        List thisList = new ArrayList();
        CountryIterator theseCountries = new PlayerIterator(thisPlayer, countries);
        while (theseCountries.hasNext())
        {
            thisList.add(theseCountries.next());
        }
        Country[] result = convertListToCountryArray(thisList);
        return result;
    } // Returns a Country[] of all countries this player owns
    
    public int getPossibleIncome(int thisPlayer)
    {
        int thisIncome = board.getPlayerIncome(thisPlayer);
        int cardValue = board.getNextCardSetValue();
        int numCards = board.getPlayerCards(thisPlayer);
        float calc;
        if (numCards == 5)
        {
            thisIncome += cardValue;
        }
        else if (numCards == 4)
        {
            calc = (float) cardValue * .817f;
            thisIncome += (int) calc;
        }
        else if (numCards == 3)
        {
            calc = (float) cardValue * .4228f;
            thisIncome += (int) calc;
        }
        return thisIncome;
    } // Returns the possible income of this player
    
    public float[] getPlayerRatings()
    {
        float[] result = new float[numPlayers];
        for(int i = 0; i < numPlayers; i++)
        {
            Country[] theseCountries = getPlayerCountries(i);
            result[i] = getRating(theseCountries);
        }
        return result;
    }
    
    public boolean canWinGame()
    {
        Country[] borderCountries = getBorders(getOurCountries(countries));
        int borderArmies = 0;
        for (int i = 0; i < borderCountries.length; i++)
        {
            borderArmies += borderCountries[i].getArmies();
        }
        if (borderArmies > getAttackCost(countries))
        {
            homeCountries = countries;
            return true;
        }
        return false;
    }




//
// COUNTRY & CONTINENT INFORMATION
//


    public float getRealAttackCost(Country thisCountry)
    {
        int targetArmies = thisCountry.getArmies();
        
        // Calculate cost
        double attackCost = ( ( 0.8534144f * targetArmies ) -  ( 0.2213413f * ( 1 - ( java.lang.Math.pow( -.525359d, (double) targetArmies) ) ) ) );
        
        // Add armies that need to stay in this country
        float result = (float) attackCost + 2;
        return result;
    } // Returns the average cost of an attack on this country
    
    public float getRealAttackCost(int thisContinent)
    {
        float attackCost = 0.0f;
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while(continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            if (thisCountry.getOwner() != ID)
            {
                attackCost += getRealAttackCost(thisCountry);
            }
        }
        
        // If we don't own a country in the continent, add the cost of the cheapest route to it
        if (BoardHelper.getPlayerArmiesInContinent(ID, thisContinent, countries) < 1)
        {
            CountryRoute thisRoute = getCheapestRoute(thisContinent);
            attackCost += getAttackCost(thisRoute);
            attackCost -= getAttackCost(thisRoute.end());
        }
        return attackCost;
    } // Returns the average cost of an attack on this continent, including the cheapest route to it if we don't border it'
    
    public float getRealAttackCost(Country[] theseCountries)
    {
        float attackCost = 0.0f;
        for(int i = 0; i < theseCountries.length; i++)
        {
            Country thisCountry = theseCountries[i];
            int targetArmies = thisCountry.getArmies();
            
            // Don't include cost if we own it
            if (thisCountry.getOwner() != ID)
            {
                attackCost += getRealAttackCost(thisCountry);
            }
        }
        return (float) attackCost;
    } // Returns the average cost of an attack on these countries
    
    public float getRealAttackCost(CountryRoute thisRoute)
    {
        Country[] theseCountries = convertRouteToCountryArray(thisRoute);
        return getRealAttackCost(theseCountries);
    } // Returns the average cost of an attack on this route
    
    public float getAttackCost(Country thisCountry)
    {
        int targetArmies;
        if (thisCountry.getOwner() == -1)
        {
            targetArmies = 1;
        }
        else
        {
            targetArmies = thisCountry.getArmies();
        }
        
        // Calculate cost
        double attackCost = ( ( 0.8534144f * targetArmies ) -  ( 0.2213413f * ( 1 - ( java.lang.Math.pow( -.525359d, (double) targetArmies) ) ) ) );
        
        // Change result based on how cautious the bot is
        attackCost += attackCost * caution;
        
        // Add armies that need to stay in this country
        float result = (float) attackCost + 2;
        return result;
    } // Returns the cost of an attack on this country based on how cautious the bot is
    
    public float getAttackCost(int thisContinent)
    {
        float attackCost = 0.0f;
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while(continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            if (thisCountry.getOwner() != ID)
            {
                attackCost += getAttackCost(thisCountry);
            }
        }
        
        // If we don't own a country in the continent, add the cost of the cheapest route to it
        if (BoardHelper.getPlayerArmiesInContinent(ID, thisContinent, countries) < 1)
        {
            CountryRoute thisRoute = getCheapestRoute(thisContinent);
            attackCost += getAttackCost(thisRoute);
            attackCost -= getAttackCost(thisRoute.end());
        }
        return attackCost;
    } // Returns the cost of an attack on this continent, including the cheapest route to it if we don't border it'
    
    public float getAttackCost(Country[] theseCountries)
    {
        float attackCost = 0.0f;
        for(int i = 0; i < theseCountries.length; i++)
        {
            Country thisCountry = theseCountries[i];
            int targetArmies = thisCountry.getArmies();
            
            // Don't include cost if we own it
            if (thisCountry.getOwner() != ID)
            {
                attackCost += getAttackCost(thisCountry);
            }
        }
        return (float) attackCost;
    } // Returns the cost of an attack on these countries

    public float getAttackCost(CountryRoute thisRoute)
    {
        Country[] theseCountries = convertRouteToCountryArray(thisRoute);
        return getAttackCost(theseCountries);
    } // Returns the cost of an attack on this route
    
    public float getAttackCost(CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        return getAttackCost(theseCountries);
    } // Returns the cost of an attack on this cluster
    
    public float getCostToKill (int thisPlayer)
    {
        if (!BoardHelper.playerIsStillInTheGame(thisPlayer, countries))
        {
            return -1;
        }
        
        float result = 0.0f;

        // Get all clusters owned by this player
        CountryClusterSet playerClusters = CountryClusterSet.getAllCountriesOwnedBy(thisPlayer, countries);
        for (int i = 0; i < playerClusters.numberOfClusters(); i++)
        {
            CountryCluster thisCluster = playerClusters.getCluster(i);

            // Add cost needed to conquer this cluster
            result += getAttackCost(thisCluster);
            
            // Add cost needed to reach this cluster
            Country[] theseBorders = getBorders(thisCluster);
            CountryRoute thisRoute = getCheapestRouteNotCountingThisPlayer(theseBorders, thisPlayer);
            if (thisRoute == null)
            {
                debug("ERROR in getCostToKill:  No route to attack");
                return 100000000;
            }
            for (int j = 0; j < thisRoute.size(); j ++)
            {
                if (thisRoute.get(j).getOwner() != ID && thisRoute.get(j).getOwner() != thisPlayer)
                {
                    result += getAttackCost(thisRoute.get(j));
                }
            }
        }
        return result;
    } // Returns the average cost to kill this player
    
    public float getCostToPop(int thisContinent)
    {
        if (continentIsOwned(thisContinent))
        {
            return getRealAttackCost(getCheapestRoute(thisContinent));
        }
        else
        {
            return -1;
        }
        
    } // Returns the average cost to pop this continent; returns -1 if it's unowned
    
    public boolean continentIsOwned(int thisContinent)
    {
        CountryIterator check = new ContinentIterator(thisContinent, countries);
        int owner = check.next().getOwner();
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while (continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            if (thisCountry.getOwner() != owner)
            {
                return false;
            }
        }
        return true;
    } // Returns whether this continent is owned by anybody
    
    public Country[] getBorders(int thisContinent)
    {
        List borderList = new ArrayList();
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while (continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            Country[] attackList = getAttackers(thisCountry);
            for (int i = 0; i < attackList.length; i++)
            {
                if (attackList[i].getContinent() != thisContinent)
                {
                    borderList.add(thisCountry);
                    break;
                }
            }
        }
        Country[] result = convertListToCountryArray(borderList);
        return result;
    } // Returns a Country[] of all countries on the border of this continent

    public Country[] getBorders(Country[] theseCountries)
    {
        List countryList = new ArrayList();
        for (int i = 0; i < theseCountries.length; i++)
        {
            countryList.add(theseCountries[i]);
        }
        List borderList = new ArrayList();
        for (int i = 0; i < countryList.size(); i++)
        {
            Country[] attackList = getAttackers((Country)countryList.get(i));
            for (int j = 0; j < attackList.length; j++)
            {
                if (!countryList.contains(attackList[j]))
                {
                    borderList.add(countryList.get(i));
                }
            }
        }
        Country[] result = convertListToCountryArray(borderList);
        return result;
    } // Returns a Country[] of all countries on the border of these countries
    
    public Country[] getBorders(CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        return getBorders(theseCountries);
    } // Returns a Country[] of all countries on the border of this cluster
    
    public int[] getBorderingContinents(int thisContinent)
    {
        List borderContinents = new ArrayList();
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while (continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            Country[] attackList = getAttackers(thisCountry);
            for (int i = 0; i < attackList.length; i++)
            {
                Country thisNeighbor = attackList[i];
                if (thisNeighbor.getContinent() != thisContinent)
                {
                    borderContinents.add(thisNeighbor.getContinent());
                }
            }
        }
        
        // Remove duplicate continents
        Set set = new HashSet();
        set.addAll(borderContinents);
        if(set.size() < borderContinents.size())
        {
            borderContinents.clear();
            borderContinents.addAll(set);
        }
        int[] result = convertListToIntArray(borderContinents);
        return result;
    } // Returns an int[] of all continents that border this continent

    public boolean weBorder(int thisContinent)
    {
        Country[] continentBorders = getBorders(thisContinent);
        for (int i = 0; i < continentBorders.length; i++)
        {
            Country thisCountry = continentBorders[i];
            Country[] attackList = getAttackers(thisCountry);
            for (int j = 0; j < attackList.length; j++)
            {
                if (attackList[j].getOwner() == ID && attackList[j].getContinent() != thisContinent)
                {
                    return true;
                }
            }
        }
        return false;
    } // Returns whether we border this continent
    
    public boolean weBorder(Country thisCountry)
    {
        Country[] attackList = getAttackers(thisCountry);
        for (int i = 0; i < attackList.length; i++)
        {
            if (attackList[i].getOwner() == ID)
            {
                return true;
            }
        }
        return false;
    } // Returns whether we border this country
    
    public boolean weBorder(CountryCluster thisCluster)
    {
        List clusterCountries = thisCluster.getList();
        for (int i = 0; i < clusterCountries.size(); i++)
        {
            Country thisCountry = (Country) clusterCountries.get(i);
            Country[] attackList = getAttackers(thisCountry);
            for (int j = 0; j < attackList.length; j++)
            {
                if (attackList[j].getOwner() == ID)
                {
                    return true;
                }
            }
        }
        return false;
    } // Returns whether we border this cluster

    public int getWeakestArmy(Country[] theseCountries)
    {
        int arms;
        int leastArmies = 10000000;
        for (int i = 0; i < theseCountries.length; i++)
        {
            Country us = theseCountries[i];
            arms = us.getArmies();
            if(arms < leastArmies)
            {
                leastArmies = arms;
            }
        }
        return leastArmies;
    } // Returns the number of armies on the weakest country in these countries
    
    public int getVulnerability(Country thisCountry)
    {
        int ourArmies = thisCountry.getArmies();
        int enemyArmies = 0;
        Country[] enemies = getEnemyAttackers(thisCountry);
        for(int i = 0; i < enemies.length; i++)
        {
            int thisEnemy = enemies[i].getArmies() + getPossibleIncome(enemies[i].getOwner());
            int maxEnemies = thisEnemy;
            Country[] enemyNeighbors = getEnemyAttackers(thisCountry);
            for (int j = 0; j < enemyNeighbors.length; j++)
            {
                int thisNeighborCalc = enemyNeighbors[j].getArmies() + getPossibleIncome(enemyNeighbors[j].getOwner()) - thisEnemy;
                if (enemyNeighbors[j].getOwner() != ID
                  && thisNeighborCalc > maxEnemies)
                {
                    maxEnemies = thisNeighborCalc;
                }
            }
            enemyArmies += maxEnemies;
        }
        int result = enemyArmies - ourArmies;
        return result;
    } // Returns the number of neighboring enemy armies minus the armies in this country
    
    public Country getMostVulnerable (Country[] theseCountries)
    {
        int max = -1000000;
        Country result = null;
        for (int i = 0; i < theseCountries.length; i++)
        {
            int thisVulnerability = getVulnerability(theseCountries[i]);
            if (thisVulnerability > max)
            {
                max = thisVulnerability;
                result = theseCountries[i];
            }
        }
        return result;
    } // Returns the most vulnerable country within these countries
    
    public Country[] getAttackers(Country thisCountry)
    {
        List attackList = new ArrayList();

        // for all the countries, can they see the target?
        for (int i = 0; i < countries.length; i++)
        {
            if (countries[i].canGoto(thisCountry))
            {
                attackList.add(countries[i]);
            }
        }
        Country[] result = convertListToCountryArray(attackList);
	return result;
    } // Returns a Country[] of all countries that can attack this country
    
    public Country[] getFriendlyAttackers(Country thisCountry)
    {
        List attackList = new ArrayList();
        int thisPlayer = thisCountry.getOwner();

        // for all the countries, can they see the target?
        for (int i = 0; i < countries.length; i++)
        {
            if (countries[i].getOwner() == thisPlayer && countries[i].canGoto(thisCountry))
            {
                attackList.add(countries[i]);
            }
        }
        Country[] result = convertListToCountryArray(attackList);
	return result;
    } // Returns a Country[] of all countries that can attack this country owned by the owner of this country
    
    public Country[] getEnemyAttackers(Country thisCountry)
    {
        List attackList = new ArrayList();
        int thisPlayer = thisCountry.getOwner();

        // for all the countries, can they see the target?
        for (int i = 0; i < countries.length; i++)
        {
            if (countries[i].getOwner() != thisPlayer && countries[i].canGoto(thisCountry))
            {
                attackList.add(countries[i]);
            }
        }
        Country[] result = convertListToCountryArray(attackList);
	return result;
    } // Returns a Country[] of all countries that can attack this country not owned by the owner of this country
    
    public int getShortestDistance(Country thisCountry, Country[] theseCountries)
    {
        int shortestDistance = 0;
        
       // Return -1 if there are no countries
        if (theseCountries.length < 1)
        {
            return -1;
        }
        
        // Go through each owned country and calculate the shortest distance
        // We keep track of which countries we have already seen (so we don't
        // consider the same country twice). We do it with a boolean array, with
        // a true/false value for each of the countries:
        boolean[] haveSeenAlready = new boolean[countries.length];
        for (int k = 0; k < countries.length; k++)
        {
            haveSeenAlready[k] = false;
        }
        
        // Create a Q (with a history) to store the country-codes and their cost so far:
        CountryPathStack Q = new CountryPathStack();
        
        // We explore from all the borders
        int testCode, distanceSoFar;
        int[] testCodeHistory;
        for (int l = 0; l < theseCountries.length; l++)
        {
            testCode = theseCountries[l].getCode();
            distanceSoFar = 0;
            testCodeHistory = new int[1];
            testCodeHistory[0] = testCode;
            haveSeenAlready[testCode] = true;
            
            Q.pushWithValueAndHistory(theseCountries[l], 0, testCodeHistory );
        }
        
        // So now we have all the borders in the Q (all with cost 0),
        // expand every possible outward path (in the order of cost).
        // eventually we should find thisCountry, then we return that path's size
        while ( true )
        {
            distanceSoFar = Q.topValue();
            testCodeHistory = Q.topHistory();
            testCode = Q.pop();
            
            if ( countries[testCode] == thisCountry )
            {
                
                // we have found the shortest distance!
                shortestDistance = testCodeHistory.length - 1;
                return shortestDistance;
            }
            Country[] neighbors = getAttackers(countries[testCode]);
            for (int i = 0; i < neighbors.length; i++)
            {
                if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
                {
                    
                    // Create the new node's history array. (It is just
                    // testCode's history with its CC added at the beginning):
                    int[] newHistory = new int[ testCodeHistory.length + 1 ];
                    newHistory[0] = neighbors[i].getCode();
                    for (int j = 1; j < newHistory.length; j++)
                    {
                        newHistory[j] = testCodeHistory[j-1];
                    }
                    Q.pushWithValueAndHistory(
                      neighbors[i],
                      distanceSoFar + 1,
                      newHistory );
                    haveSeenAlready[ neighbors[i].getCode() ] = true;
                }
            }
            if ( Q.isEmpty() )
            {
                debug("ERROR in getShortestDistance -> can't pop");
                return 1000000;
            }
        }
    } // Returns the shortest distance between this country and any one of these countries
    
    public int getShortestDistanceOwned(Country thisCountry, Country[] theseCountries)
    {
        int shortestDistance = 0;
        Country[] ourCountries = getOurCountries(countries);
        
       // Return -1 if there are no countries
        if (theseCountries.length < 1)
        {
            return-1;
        }
        
        // Go through each owned country and calculate the shortest distance
        // We keep track of which countries we have already seen (so we don't
        // consider the same country twice). We do it with a boolean array, with
        // a true/false value for each of the countries:
        boolean[] haveSeenAlready = new boolean[countries.length];
        for (int k = 0; k < countries.length; k++)
        {
            haveSeenAlready[k] = false;
        }
        
        // Create a Q (with a history) to store the country-codes and their cost so far:
        CountryPathStack Q = new CountryPathStack();
        
        // We explore from all the borders
        int testCode, distanceSoFar;
        int[] testCodeHistory;
        for (int l = 0; l < theseCountries.length; l++)
        {
            testCode = theseCountries[l].getCode();
            distanceSoFar = 0;
            testCodeHistory = new int[1];
            testCodeHistory[0] = testCode;
            haveSeenAlready[testCode] = true;
            
            Q.pushWithValueAndHistory(theseCountries[l], 0, testCodeHistory );
        }
        
        // So now we have all the borders in the Q (all with cost 0),
        // expand every possible outward path (in the order of cost).
        // eventually we should find thisCountry, then we return that path's size
        while ( true )
        {
            distanceSoFar = Q.topValue();
            testCodeHistory = Q.topHistory();
            testCode = Q.pop();
            
            if ( countries[testCode] == thisCountry )
            {
                
                // we have found the shortest distance!
                shortestDistance = testCodeHistory.length - 1;
                return shortestDistance;
            }
            Country[] neighbors = getFriendlyAttackers(countries[testCode]);
            for (int i = 0; i < neighbors.length; i++)
            {
                if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
                {
                    
                    // Create the new node's history array. (It is just
                    // testCode's history with its CC added at the beginning):
                    int[] newHistory = new int[ testCodeHistory.length + 1 ];
                    newHistory[0] = neighbors[i].getCode();
                    for (int j = 1; j < newHistory.length; j++)
                    {
                        newHistory[j] = testCodeHistory[j-1];
                    }
                    Q.pushWithValueAndHistory(
                      neighbors[i],
                      distanceSoFar + 1,
                      newHistory );
                    haveSeenAlready[ neighbors[i].getCode() ] = true;
                }
            }
            if ( Q.isEmpty() )
            {
                return -1;
            }
        }
    } // Returns the shortest distance between this country and any one of these countries only going through countries we own
    
    public int getShortestDistanceToBorder(Country thisCountry, Country[] theseCountries)
    {
        int shortestDistance = 0;
        
        // Find all borders
        Country[] borders = getBorders(theseCountries);
        
        // Return -1 if there are no border countries
        if (borders.length < 1)
        {
            return -1;
        }
        return getShortestDistance(thisCountry, borders);
    } // Returns the shortest distance between this country and a border in these countries
    
    public int getShortestDistanceToBorder(Country thisCountry, CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        return getShortestDistanceToBorder(thisCountry, theseCountries);
    } // Returns the shortest distance between this country and a border in this cluster
    
    public int getShortestDistanceToBorder(Country thisCountry, int thisContinent)
    {
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        List thisList = new ArrayList();
        while (continentCountries.hasNext())
        {
            thisList.add(continentCountries.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        return getShortestDistanceToBorder(thisCountry, theseCountries);
    } // Returns the shortest distance between this country and a border in this continent
    
    public Country getInnermostCountry(int thisContinent)
    {
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        List thisList = new ArrayList();
        while (continentCountries.hasNext())
        {
            thisList.add(continentCountries.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        return getInnermostCountry(theseCountries);
    } // Returns the innermost country in this continent that has the fewest neighbors
    
    public Country getInnermostCountry(CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        return getInnermostCountry(theseCountries);
    } // Returns the innermost country in this cluster that has the fewest neighbors
    
    public Country getInnermostCountry(Country[] theseCountries)
    {   // Find countries with the longest distance to a border
        Country result = null;
        List innermostCountries = new ArrayList();
        int longestDistance = -1;
	for (int i = 0; i < theseCountries.length; i++)
        {
            Country thisCountry = theseCountries[i];
            int thisDistance = getShortestDistanceToBorder(thisCountry, theseCountries);
            if (thisDistance == longestDistance)
            {
                innermostCountries.add(thisCountry);
            }
            else if ( thisDistance > longestDistance)
            {
                innermostCountries.clear();
                innermostCountries.add(thisCountry);
                longestDistance = thisDistance;
            }
        }
        if (innermostCountries.size() == 1)
        {
            result = (Country) innermostCountries.get(0);
            return result;
        }
        
        // Find the countries with the fewest neighbors
        List temp = new ArrayList();
        int minNeighbors = 1000000;
        for (int j = 0; j < innermostCountries.size(); j++)
        {
            Country thisCountry = (Country) innermostCountries.get(j);
            int numNeighbors = getAttackers(thisCountry).length;
            if ( numNeighbors == minNeighbors)
            {
                temp.add(thisCountry);
            }
            else if (numNeighbors < minNeighbors)
            {
                temp.clear();
                temp.add(thisCountry);
                minNeighbors = numNeighbors;
            }
        }
        if (temp.size() == 1)
        {
            result = (Country) temp.get(0);
            return result;
        }
        
        // Find country with innermost neighbors
        int lowestCalc = 1000000;
        for (int k = 0; k < temp.size(); k++)
        {
            Country thisCountry = (Country) temp.get(k);
            int thisCalc = 0;
            Country[] innermostNeighbors = getAttackers(thisCountry);
            for (int l = 0; l < innermostNeighbors.length; l++)
            {
                thisCalc += getShortestDistanceToBorder(innermostNeighbors[l], theseCountries);
            }
            if (thisCalc < lowestCalc)
            {
                lowestCalc = thisCalc;
                result = thisCountry;
            }
        }
        
        return result;
    } // Returns the innermost country in these countries that has the fewest neighbors
    
    public Country getInnermostCountryWeOwn(int thisContinent)
    {
        CountryIterator continentCountries = new ContinentIterator(thisContinent, new PlayerIterator(ID, countries));
        List thisList = new ArrayList();
        while (continentCountries.hasNext())
        {
            thisList.add(continentCountries.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        if (theseCountries.length > 0)
        {
            return getInnermostCountry(theseCountries);
        }
        else
        {
            return null;
        }
    } // Returns the innermost country in this continent that we own and has the fewest neighbors
    
    public Country getInnermostCountryWeOwn(Country[] theseCountries)
    {
        CountryIterator ourCountries = new PlayerIterator(ID, theseCountries);
        List thisList = new ArrayList();
        while (ourCountries.hasNext())
        {
            thisList.add( ourCountries.next());
        }
        Country[] newCountries = convertListToCountryArray(thisList);
        return getInnermostCountry(newCountries);
    } // Returns the innermost country in this continent that we own and has the fewest neighbors
    
    public Country getInnermostUnownedCountry(int thisContinent)
    {
        CountryIterator theseCountries = new ContinentIterator(thisContinent, countries);
        List thisList = new ArrayList();
        while (theseCountries.hasNext())
        {
            Country thisCountry = theseCountries.next();
            if (thisCountry.getOwner() == -1)
            {
                thisList.add(thisCountry);
            }
        }
        if (thisList.size() < 1)
        {
            return null;
        }
        Country[] newCountries = convertListToCountryArray(thisList);
        return getInnermostCountry(newCountries);
    } // Returns the innermost country in this continent that is unowned and has the fewest neighbors
    
    public Country getInnermostCountryWeBorder(int thisContinent)
    {
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        List thisList = new ArrayList();
        while (continentCountries.hasNext())
        {
            Country thisCountry = continentCountries.next();
            if (thisCountry.getOwner() != ID && weBorder(thisCountry))
            {
                thisList.add(thisCountry);
            }
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        if (theseCountries.length > 0)
        {
            return getInnermostCountry(theseCountries);
        }
        else
        {
            return null;
        }
        
    } // Returns the innermost enemy country in this continent that we border and has the fewest neighbors
    
    public Country getInnermostCountryWeBorder(Country[] theseCountries)
    {
        List thisList = new ArrayList();
        for (int i = 0; i < theseCountries.length; i++)
        {
            Country thisCountry = theseCountries[i];
            if (thisCountry.getOwner() != ID && weBorder(thisCountry))
            {
                thisList.add(thisCountry);
            }
        }
        Country[] newCountries = convertListToCountryArray(thisList);
        if (newCountries.length > 0)
        {
            return getInnermostCountry(newCountries);
        }
        else
        {
            return null;
        }
    } // Returns the innermost enemy country in these countries that we border and has the fewest neighbors
    
    public Country getInnermostCountryToFortify(Country[] theseCountries)
    {
        CountryIterator ourCountries = new PlayerIterator(ID, theseCountries);
        List thisList = new ArrayList();
        while (ourCountries.hasNext())
        {
            Country thisCountry = ourCountries.next();
            if(thisCountry.getMoveableArmies() > 0 
              && thisCountry.getArmies() > 2
              && getShortestDistanceToBorder(thisCountry, theseCountries) > 0)
            {
                thisList.add(thisCountry);
            }
        }
        
        Country[] newCountries = convertListToCountryArray(thisList);
        return getInnermostCountry(newCountries);
    } // Returns the innermost country in this continent that we own and has the fewest neighbors
    
    public Country[] getConnectedCountries(Country thisCountry, CountryCluster thisCluster)
    {
        List clusterCountries = thisCluster.getList();
        List connectedCountries = new ArrayList();
        CountryIterator neighbors = new NeighborIterator(thisCountry, countries);
        while (neighbors.hasNext())
        {
            Country thisNeighbor = neighbors.next();
            if (thisNeighbor.getOwner() != ID
              && clusterCountries.contains(thisNeighbor))
            {
                connectedCountries.add(thisNeighbor);
            }
        }
        
        // Keep track of which countries we have looked at
        List countriesChecked = new ArrayList();
        while (countriesChecked.size() < connectedCountries.size())
        {
            for (int i = 0; i < connectedCountries.size(); i++)
            {
                if (!countriesChecked.contains(connectedCountries.get(i)))
                {
                    CountryIterator connectedNeighbors = new NeighborIterator((Country)connectedCountries.get(i), countries);
                    countriesChecked.add(connectedCountries.get(i));
                    while (connectedNeighbors.hasNext())
                    {
                        Country thisNeighbor = connectedNeighbors.next();
                        if (thisNeighbor.getOwner() != ID
                          && clusterCountries.contains(thisNeighbor)
                          && !connectedCountries.contains(thisNeighbor))
                        {
                            connectedCountries.add(thisNeighbor);
                        }
                    }
                }
            }
        }
        return convertListToCountryArray(connectedCountries);
    } // Returns a Country[] of all countries within this cluster that we can reach from this country
    
    public int getBiggestArmy(int thisPlayer, Country[] theseCountries)
    {
        int maxArmies = 0;
        for (int i = 0; i < theseCountries.length; i++)
        {
            if (theseCountries[i].getOwner() == thisPlayer
              && theseCountries[i].getArmies() > maxArmies)
            {
                maxArmies = theseCountries[i].getArmies();
            }
        }
        return maxArmies;
    } // Returns the biggest army in these countries owned by this player
        
    public Country getBiggestArmyLocation(int thisPlayer, Country[] theseCountries)
    {
        int maxArmies = 0;
        Country result = null;
        for (int i = 0; i < theseCountries.length; i++)
        {
            if (theseCountries[i].getOwner() == thisPlayer
              && theseCountries[i].getArmies() > maxArmies)
            {
                result = theseCountries[i];
                maxArmies = result.getArmies();
            }
        }
        return result;
    } // Returns the country owned by this player with the biggest army in these countries
    
    public int getIncome(Country[] theseCountries)
    {
        int income = theseCountries.length / 3;
        List countryList = convertCountryArrayToList(theseCountries);
        for (int i = 0; i < numContinents; i++)
        {
            CountryIterator continentCountries = new ContinentIterator(i, countries);
            boolean ownContinent = true;
            while (continentCountries.hasNext())
            {
                if (!countryList.contains(continentCountries.next()))
                {
                   ownContinent = false;
                   break;
                }
            }
            if (ownContinent)
            {
                income += board.getContinentBonus(i);
            }
        }
        return income;
    }
    
    public float getRating(Country[] theseCountries)
    {
        if (theseCountries.length < 1)
        {
            return -1;
        }
        int income = getIncome(theseCountries);
        int numBorders = getBorders(theseCountries).length;
        float cost = getAttackCost(theseCountries);
        float result;
        if (income < 0)
        {
            result = income;
        }
        else if(numBorders < 1)
        {
            result = income + (200 / cost);
        }
        else if (cost < 1)
        {
            result = income + (15 / numBorders) + 200;
        }
        else
        {
            result = income + (15 / numBorders) + (200 / cost);
        }
        return result;
    }
    
    public float getRating(int thisContinent)
    {
        int income = board.getContinentBonus(thisContinent);
        int numBorders = getBorders(thisContinent).length;
        float cost = 0;
        if (pickCountries)
        {
            CountryIterator theseCountries = new ContinentIterator(thisContinent, countries);
            while (theseCountries.hasNext())
            {
                cost += getAttackCost(theseCountries.next());
            }
        }
        else
        {
            cost = getAttackCost(thisContinent);
        }
        if (cost < 1)
        {
            cost = 1;
        }
        if (numBorders < 1)
        {
            numBorders = 1;
        }
        float result;
        if (income < 0)
        {
            result = income;
        }
        else
        {
            result = income + (15 / numBorders) + (200 / cost);
        }
        return result;
    }
    
    public boolean weOwn(int thisContinent)
    {
        CountryIterator theseCountries = new ContinentIterator(thisContinent, countries);
        while (theseCountries.hasNext())
        {
            if (theseCountries.next().getOwner() != ID)
            {
                return false;
            }
        }
        return true;
    } // Returns true if we own this continent
    
    public boolean weCanConquer(int thisContinent)
    {
        int armiesNeeded = getArmiesNeededToConquer(thisContinent);
        if (armiesAvailable > armiesNeeded)
        {
            return true;
        }
        return false;
    } // Returns true if we can conquer this continent
    
    public boolean weCanConquer(Country[] theseCountries)
    {
        int armiesNeeded = getArmiesNeededToConquer(theseCountries);
        if (armiesAvailable > armiesNeeded)
        {
            return true;
        }
        return false;
    } // Returns true if we can conquer these countries
    
    public int getArmiesNeededToConquer(Country[] theseCountries)
    {
        int armiesNeeded = 0;

        //Find all enemy clusters
        CountryClusterSet targetSet = CountryClusterSet.getAllCountriesNotOwnedBy(ID, theseCountries);
        
        // Find armies needed to attack all clusters that we border
        for (int i = 0; i < targetSet.numberOfClusters(); i++)
        {
            CountryCluster thisCluster = targetSet.getCluster(i);
            Country placeHere = null;
            armiesNeeded += getAttackCost(thisCluster);
            if (weBorder(thisCluster))
            {
                // Find armies needed to attack the countries in this cluster
                List clusterList = thisCluster.getList();
                List listCountries = new ArrayList();
                Country[] ourCountries = getOurCountries(countries);
                for (int j= 0; j <ourCountries.length; j++)
                {
                    listCountries.add(ourCountries[j]);
                }
                for (int j = 0; j < clusterList.size(); j++)
                {
                    listCountries.add(clusterList.get(j));
                }
                Country[] allCountries = convertListToCountryArray(listCountries);
                
                // Find innermost country that we border
                int longestDistance = -1;
                Country defender = null;
                for (int j = 0; j < clusterList.size(); j++)
                {
                    Country thisCountry = (Country) clusterList.get(j);
                    
                    // Fixes a bug if there are no borders in allCountries
                    if (getBorders(allCountries).length < 1)
                    {
                        allCountries = convertListToCountryArray(clusterList);
                    }
                    int distance = getShortestDistanceToBorder(thisCountry, allCountries);
                    if (distance > longestDistance && weBorder(thisCountry))
                    {
                        longestDistance = distance;
                        defender = thisCountry;
                    }
                }
                // Find neighbors that have the fewest enemy borders
                List potentialAttackers = new ArrayList();
                Country[] neighbors = getEnemyAttackers(defender);
                int minBorders = 1000000;
                for (int j = 0; j < neighbors.length; j++)
                {
                    Country thisCountry = neighbors[j];
                    if (thisCountry.getOwner() == ID)
                    {
                        int numBorders = getEnemyAttackers(thisCountry).length;
                        if (numBorders == minBorders)
                        {
                            potentialAttackers.add(thisCountry);
                        }
                        else if (numBorders < minBorders)
                        {
                            minBorders = numBorders;
                            potentialAttackers.clear();
                            potentialAttackers.add(thisCountry);
                        }
                    }
                }

                // Find biggest potential attacker
                int biggestArmy = 0;
                for (int j = 0; j < potentialAttackers.size(); j++)
                {
                    Country checkCountry = (Country) potentialAttackers.get(j);
                    if (checkCountry.getArmies() > biggestArmy)
                    {
                        biggestArmy = checkCountry.getArmies();
                        placeHere = checkCountry;
                    }
                }
                int thisVulnerability = getVulnerability(placeHere);
                if (thisVulnerability > 0)
                {
                    armiesNeeded += thisVulnerability;
                }
            }
            
            // We don't border this cluster, so place at the start of the cheapest route to this cluster
            else
            {
                CountryRoute thisRoute = getCheapestRoute(thisCluster);
                int thisVulnerability = getVulnerability(thisRoute.get(0));
                if (thisVulnerability > 0)
                {
                    armiesNeeded += thisVulnerability;
                }
            }
        }
        if (armiesNeeded < 1)
        {
            armiesNeeded = 0;
        }
        return armiesNeeded;
    } // Returns the number of armies we need to place in order to conquer these countries
    
    public int getArmiesNeededToConquer(int thisContinent)
    {
        List thisList = new ArrayList();
        CountryIterator theseCountries = new ContinentIterator(thisContinent, countries);
        while(theseCountries.hasNext())
        {
            thisList.add(theseCountries.next());
        }
        return getArmiesNeededToConquer(convertListToCountryArray(thisList));
    } // Returns the number of armies we need to place in order to conquer this continent




//
// ROUTE METHODS
//


    public CountryRoute getCheapestRoute(Country[] theseCountries)
    {
	// We keep track of which countries we have already seen (so we don't
	// consider the same country twice). We do it with a boolean array, with
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
        {
            haveSeenAlready[i] = false;
        }

	// Create a Q (with a history) to store the country-codes and their cost
	// so far:
	CountryPathStack Q = new CountryPathStack();

	// We explore from all the borders of <cluster>
	int testCode, armiesSoFar;
	int[] testCodeHistory;
        Country[] borders = getBorders(theseCountries);
	
        // Return null if there are no border countries
        if (borders.length < 1)
        {
            return null;
        }
        for (int i = 0; i < borders.length; i++)
        {
            testCode = borders[i].getCode();
            armiesSoFar = borders[i].getArmies();
            testCodeHistory = new int[1];
            testCodeHistory[0] = testCode;
            haveSeenAlready[testCode] = true;
            Q.pushWithValueAndHistory(
              countries[borders[i].getCode()], 
              armiesSoFar, 
              testCodeHistory );
        }

	// So now we have all the continent borders in the Q (all with cost 0),
	// expand every possible outward path (in the order of cost).
	// eventually we should find a country owned by <owner>,
	// then we return that path's history
	while ( true )
        {
            armiesSoFar = Q.topValue();
            testCodeHistory = Q.topHistory();
            testCode = Q.pop();
            if ( countries[testCode].getOwner() == ID )
            {
                // Convert it to a CountryRoute
		CountryRoute bestRoute = new CountryRoute(testCodeHistory, countries);
		return bestRoute;
            }
            Country[] neighbors = getAttackers(countries[testCode]);
            for (int i = 0; i < neighbors.length; i++)
            {
                if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
                {
                    // Create the new node's history array. (It is just
                    // testCode's history with its CC added at the beginning):
                    int[] newHistory = new int[ testCodeHistory.length + 1 ];
                    newHistory[0] = neighbors[i].getCode();
                    for (int j = 1; j < newHistory.length; j++)
                    {
                        newHistory[j] = testCodeHistory[j-1];
                    }
                    Q.pushWithValueAndHistory(
                      neighbors[i],
                      // If the neighbor is owned by the proper person then minus
                      // its armies from the value so if gets pulled off the Q next.
                      // Without this there is a bug
                      armiesSoFar + (neighbors[i].getOwner() == ID ? -neighbors[i].getArmies() : neighbors[i].getArmies()),
                      newHistory );
                    haveSeenAlready[ neighbors[i].getCode() ] = true;
                }
            }
            if ( Q.isEmpty() )
            {
                System.out.println(
                  "ERROR in getCheapestRoute -> can't pop");
                return null;
            }
        }
    } // Return cheapest route to a border country in these countries
    
    public CountryRoute getCheapestRoute(int thisContinent)
    {
        List thisList = new ArrayList();
        CountryIterator continentCountries = new ContinentIterator(thisContinent, countries);
        while (continentCountries.hasNext())
        {
            thisList.add(continentCountries.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        return getCheapestRoute(theseCountries);
    } // Return cheapest route to a border country in this continent
    
    public CountryRoute getCheapestRoute(Country thisCountry)
    {
        Country[] theseCountries = new Country[1];
        theseCountries[0] = thisCountry;
        return getCheapestRoute(theseCountries);
    } // Return the cheapest route to this Country
    
    public CountryRoute getCheapestRoute(CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        return getCheapestRoute(theseCountries);
    } // Return cheapest route to a border country in this cluster
    
    public CountryRoute getCheapestRouteNotCountingThisPlayer(Country[] theseCountries, int thisPlayer)
    {
        // We keep track of which countries we have already seen (so we don't
	// consider the same country twice). We do it with a boolean array, with
	// a true/false value for each of the countries:
	boolean[] haveSeenAlready = new boolean[countries.length];
	for (int i = 0; i < countries.length; i++)
		{
		haveSeenAlready[i] = false;
		}

	// Create a Q (with a history) to store the country-codes and their cost
	// so far:
	CountryPathStack Q = new CountryPathStack();

	// We explore from all the borders of <cluster>
	int testCode, armiesSoFar;
	int[] testCodeHistory;
        Country[] borders = getBorders(theseCountries);
	
        // Return null if there are no border countries
        if (borders.length < 1)
        {
            return null;
        }
        
        for (int i = 0; i < borders.length; i++)
		{
		testCode = borders[i].getCode();
		armiesSoFar = borders[i].getArmies();
		testCodeHistory = new int[1];
		testCodeHistory[0] = testCode;
		haveSeenAlready[testCode] = true;

		Q.pushWithValueAndHistory(
 					countries[borders[i].getCode()], armiesSoFar, testCodeHistory );
		}

	// So now we have all the continent borders in the Q (all with cost 0),
	// expand every possible outward path (in the order of cost).
	// eventually we should find a country owned by <owner>,
	// then we return that path's history
	while ( true )
		{
		armiesSoFar = Q.topValue();
		testCodeHistory = Q.topHistory();
		testCode = Q.pop();
		if ( countries[testCode].getOwner() == ID )
			{

			// we have found the best path. Convert it to a CountryRoute
			CountryRoute bestRoute = new CountryRoute(testCodeHistory, countries);
			return bestRoute;
			}
		Country[] neighbors = countries[testCode].getAdjoiningList();
		for (int i = 0; i < neighbors.length; i++)
			{
			if ( ! haveSeenAlready[ neighbors[i].getCode() ] )
				{

				// Create the new node's history array. (It is just
				// testCode's history with its CC added at the beginning):
				int[] newHistory = new int[ testCodeHistory.length + 1 ];
				newHistory[0] = neighbors[i].getCode();
				for (int j = 1; j < newHistory.length; j++)
					{
					newHistory[j] = testCodeHistory[j-1];
					}
				
                                // If the neighbor is owned by the proper person then minus
                                // its armies from the value so if gets pulled off the Q next.
				// Without this there is a bug
					
                                if (neighbors[i].getOwner() == ID)
                                {
                                    armiesSoFar -= -neighbors[i].getArmies();
                                }
                                else if (neighbors[i].getOwner() != thisPlayer)
                                {
                                    armiesSoFar += neighbors[i].getArmies();
                                }
                                Q.pushWithValueAndHistory(
					neighbors[i],
					armiesSoFar,
					newHistory );
				haveSeenAlready[ neighbors[i].getCode() ] = true;
				}
			}
		if ( Q.isEmpty() )
			{
			System.out.println(
				"ERROR in getCheapestRouteNotCountingThisPlayer -> can't pop");
			return null;
			}
		}
    } // Return cheapest route to a border country in these countries, not counting the cost to go through this player's countries




//
// PLACING METHODS
//


    public int placeToAttack(Country[] theseCountries, int availableArmies)
    {
        if (theseCountries == null)
        {
            return availableArmies;
        }
        //Find all enemy clusters
        CountryClusterSet targetSet = CountryClusterSet.getAllCountriesNotOwnedBy(ID, theseCountries);
        
        // Place to attack all clusters that we border
        for (int i = 0; i < targetSet.numberOfClusters(); i++)
        {
            CountryCluster thisCluster = targetSet.getCluster(i);
            Country placeHere = null;
            if (weBorder(thisCluster))
            {
                // Place to attack the countries in this cluster
                List clusterList = thisCluster.getList();
                List listCountries = new ArrayList();
                Country[] ourCountries = getOurCountries(countries);
                for (int j= 0; j <ourCountries.length; j++)
                {
                    listCountries.add(ourCountries[j]);
                }
                for (int j = 0; j < clusterList.size(); j++)
                {
                    listCountries.add(clusterList.get(j));
                }
                Country[] allCountries = convertListToCountryArray(listCountries);
                
                // Find innermost country that we border
                int longestDistance = -1;
                Country defender = null;
                for (int j = 0; j < clusterList.size(); j++)
                {
                    Country thisCountry = (Country) clusterList.get(j);
                    
                    // Fixes a bug if there are no borders in allCountries
                    if (getBorders(allCountries).length < 1)
                    {
                        allCountries = convertListToCountryArray(clusterList);
                    }
                    int distance = getShortestDistanceToBorder(thisCountry, allCountries);
                    if (distance > longestDistance && weBorder(thisCountry))
                    {
                        longestDistance = distance;
                        defender = thisCountry;
                    }
                }
                // Find neighbors that have the fewest enemy borders
                List potentialAttackers = new ArrayList();
                Country[] neighbors = getEnemyAttackers(defender);
                int minBorders = 1000000;
                for (int j = 0; j < neighbors.length; j++)
                {
                    Country thisCountry = neighbors[j];
                    if (thisCountry.getOwner() == ID)
                    {
                        int numBorders = getEnemyAttackers(thisCountry).length;
                        if (numBorders == minBorders)
                        {
                            potentialAttackers.add(thisCountry);
                        }
                        else if (numBorders < minBorders)
                        {
                            minBorders = numBorders;
                            potentialAttackers.clear();
                            potentialAttackers.add(thisCountry);
                        }
                    }
                }

                // Find biggest potential attacker
                int biggestArmy = 0;
                for (int j = 0; j < potentialAttackers.size(); j++)
                {
                    Country checkCountry = (Country) potentialAttackers.get(j);
                    if (checkCountry.getArmies() > biggestArmy)
                    {
                        biggestArmy = checkCountry.getArmies();
                        placeHere = checkCountry;
                    }
                }
            }
            
            // We don't border this cluster, so place at the start of the cheapest route to this cluster
            else
            {
                CountryRoute thisRoute = getCheapestRoute(thisCluster);
                placeHere = thisRoute.get(0);
            }
            int placeThis = Math.min((int) getAttackCost(thisCluster), availableArmies);
            board.placeArmies(placeThis, placeHere);
        }
        return availableArmies;
    } // Place enough armies to conquer these countries; returns the number armies available
    
    public int placeToDefend(Country[] theseCountries, int availableArmies)
    {
        if (availableArmies < 1)
        {
            return 0;
        }
        
        Country[] ourCountries = getOurCountries(theseCountries);
        Country[] borderCountries = getBorders(ourCountries);
        if (ourCountries.length < 1 || borderCountries.length < 1)
        {
            return availableArmies;
        }
        
        // Make sure every border has at least 6 armies
        availableArmies = placeMinimum(borderCountries, availableArmies, 6);
        if (availableArmies < 1)
        {
            return 0;
        }
        
        // Make sure every country has at least 2 armies
        availableArmies = placeMinimum(ourCountries, availableArmies, 2);
        if (availableArmies < 1)
        {
            return 0;
        }
        
        if (borderCountries.length < 1)
        {
            debug("ERROR! - no border countries in placeToDefend");
            return availableArmies;
        }

        // Place on the most vulnerable borders until none are vulnerable
        int mostVulnerability = getVulnerability(getMostVulnerable(borderCountries));
        while ( mostVulnerability > 0)
        {
            if (availableArmies < 1)
                {
                    return 0;
                }
            Country mostVulnerable = getMostVulnerable(borderCountries);
            board.placeArmies(1, mostVulnerable);
            availableArmies --;
            mostVulnerability ++;
        }
        return availableArmies;
    } // Place armies to defend these countries; returns the number of armies left over
    
    public int placeMinimum(Country[] theseCountries, int availableArmies, int minimum)
    {
        if (availableArmies < 1)
        {
            return 0;
        }
        
        Country[] ourCountries = getOurCountries(theseCountries);
        int weakest = getWeakestArmy(ourCountries);
        while (weakest < minimum)
        {
            for (int i = 0; i < ourCountries.length; i++)
            {
                if (availableArmies < 1)
                {
                    return 0;
                }
                if (ourCountries[i].getArmies() < minimum)
                {
                    board.placeArmies(1, ourCountries[i]);
                    availableArmies --;
                }
            }
            weakest ++;
        }
        return availableArmies;
    } // Place armies one at a time on the weakest countries until all countries have at least the minimum; returns the number of armies left over
    
    public void placeOnWeakest(Country[] theseCountries, int availableArmies)
    {
        if (availableArmies < 1)
        {
            return;
        }
        
        Country[] ourCountries = getOurCountries(theseCountries);
        int weakest = getWeakestArmy(ourCountries);
        while (availableArmies > 0)
        {
            for (int i = 0; i < ourCountries.length; i++)
            {
                if (availableArmies < 1)
                {
                    return;
                }
                if (ourCountries[i].getArmies() == weakest)
                {
                    board.placeArmies(1, ourCountries[i]);
                    availableArmies --;
                }
            }
            weakest ++;
        }
    } // Place armies one at a time on the weakest countries
    
    public void placeOnWeakestBorder(Country[] theseCountries, int availableArmies)
    {
        if (availableArmies < 1)
        {
            return;
        }
        
        Country[] ourCountries = getOurCountries(theseCountries);
        Country[] borders = getBorders(ourCountries);
        if (borders.length < 1)
        {
            return;
        }
        int weakest = getWeakestArmy(borders);
        while (availableArmies > 0)
        {
            for (int i = 0; i < borders.length; i++)
            {
                if (availableArmies < 1)
                {
                    return;
                }
                if (borders[i].getArmies() == weakest)
                {
                    board.placeArmies(1, borders[i]);
                    availableArmies --;
                }
            }
            weakest ++;
        }
    } // Place armies one at a time on the weakest border countries
    
    public void placeOnMostVulnerable(Country[] theseCountries, int availableArmies)
    {
        if (availableArmies < 1)
        {
            return;
        }
        Country[] ourCountries = getOurCountries(theseCountries);
        if (ourCountries.length < 1)
        {
            return;
        }
        while (availableArmies > 0)
        {
            Country placeHere = getMostVulnerable(ourCountries);
            board.placeArmies(1, placeHere);
            availableArmies --;
        }
    } // Place armies one at a time on the most vulnerable countries
    
    public void placeOnBiggestArmy(Country[] theseCountries, int availableArmies)
    {
        if (availableArmies < 1)
        {
            return;
        }
        Country[] ourCountries = getOurCountries(theseCountries);
        Country placeHere = getBiggestArmyLocation(ID, ourCountries);
        board.placeArmies(availableArmies, placeHere);
    } // Place all available armies where the largest army is located
    
    public int placeToPop(int availableArmies)
    {
        if (availableArmies < 1)
        {
            return 0;
        }

        // List the benefits of popping each continent
        int[] popBenefit = new int[numContinents];
        for (int i = 0; i < numContinents; i++)
        {
            int cost = (int) getCostToPop(i);
            if (cost > 0)
            {
                popBenefit[i] =  board.getContinentBonus(i) - cost;
            }
            else
            {
                popBenefit[i] = -1;
            }
        }
        
        // Create array to see if we've looked at this continent yet
        boolean[] checked = new boolean[numContinents];
        for (int j = 0; j < numContinents; j++)
        {
            checked[j] = false;

        }
        
        // Place armies to pop the best continents
        int numChecked = 0;
        while (numChecked < numContinents)
        {
            // Find best continent to pop
            int maxBenefit = -1000000;
            int bestContinent = -1;
            for (int k = 0; k < numContinents; k++)
            {
                if (!checked[k]
                  && popBenefit[k] > maxBenefit)
                {
                    maxBenefit = popBenefit[k];
                    bestContinent = k;
                }
            }
            
            // If the best continent has no benefit, stop placing armies
            if (maxBenefit < 0)
            {
                return availableArmies;
            }
            
            // Place armies to pop the best continent
            int placeThis = Math.min(availableArmies, (int) getCostToPop(bestContinent));
            Country placeHere = getCheapestRoute(bestContinent).start();
            board.placeArmies(placeThis, placeHere);
            checked[bestContinent] = true;
            availableArmies -= placeThis;
            if(availableArmies < 1)
            {
                return 0;
            }
            numChecked++;
        }
        return availableArmies;
    } // If any continents are worth popping, place armies to do so, starting with the best continents to pop
    
    public int placeToKill(int availableArmies)
    {
        if (availableArmies < 1)
        {
            return 0;
        }

        // List the benefits of killing each player
        int[] killBenefit = new int[numPlayers];
        int ourCards = board.getPlayerCards(ID);
        int cardValue = board.getNextCardSetValue();
        for (int i = 0; i < numPlayers; i++)
        {
            int thisPlayerCards = board.getPlayerCards(i);
            int cost = (int) getCostToKill(i);
            int numCards = ourCards + thisPlayerCards;
            float calc = 0.0f;
            if (thisPlayerCards == 5)
            {
                calc= cardValue;
            }
            else if (thisPlayerCards == 4)
            {   
                calc = (float) cardValue * .817f;
            }
            else if (thisPlayerCards == 3)
            {
                calc = (float) cardValue * .4228f;
            }
            int calcBenefit = (int) calc - cost;
            if (cost > 0
              && numCards > 5)
            {
                killBenefit[i] =  cardValue - cost;
            }
            else
            {
                killBenefit[i] = -1;
            }
            if (cost > 0
              && calc > cost
              && calcBenefit > killBenefit[i])
            {
                killBenefit[i] = calcBenefit;
            }
        }
        
        // Create array to see if we've looked at this player yet
        boolean[] checked = new boolean[numPlayers];
        for (int j = 0; j < numPlayers; j++)
        {
            checked[j] = false;
        }
        
        // Place armies to kill the best players
        int numChecked = 0;
        while (numChecked < numPlayers)
        {
            // Find best player to kill
            int maxBenefit = -1000000;
            int bestPlayer = -1;
            for (int k = 0; k < numPlayers; k++)
            {
                if (!checked[k]
                  && k == ID)
                {
                    checked[k] = true;
                    numChecked ++;
                }
                else if (!checked[k]
                  && killBenefit[k] > maxBenefit)
                {
                    maxBenefit = killBenefit[k];
                    bestPlayer = k;
                }
            }
                        
            // If the best player has no benefit, stop placing armies
            if (maxBenefit < 0)
            {
                return availableArmies;
            }
            
            // Get all clusters owned by this player
            CountryClusterSet playerClusters = CountryClusterSet.getAllCountriesOwnedBy(bestPlayer, countries);
            for (int l = 0; l < playerClusters.numberOfClusters(); l++)
            {
                CountryCluster thisCluster = playerClusters.getCluster(l);
                Country[] theseBorders = getBorders(thisCluster);
                CountryRoute thisRoute = getCheapestRouteNotCountingThisPlayer(theseBorders, bestPlayer);
                int cost = (int) getAttackCost(thisCluster);
                for (int m = 0; m < thisRoute.size(); m++)
                {
                    Country thisCountry = thisRoute.get(m);
                    if (thisCountry.getOwner() != ID
                      && thisCountry.getOwner() != bestPlayer)
                    {
                        cost += getAttackCost(thisCountry);
                    }
                }
                Country placeHere = thisRoute.start();
                int placeThis = Math.min(availableArmies, cost);
                board.placeArmies(placeThis, placeHere);
                availableArmies -= placeThis;
                if(availableArmies < 1)
                {
                    return 0;
                }
            }
            checked[bestPlayer] = true;
            numChecked++;
        }
        return availableArmies;
    } // If any players are worth killing, place armies to do so, starting with the best players to kill




//
// ATTACK METHODS
//


    public boolean safeAttack(Country attacker, Country defender, int moveThis)
    {
        /*
        debug("    safeAttack(attacker, defender, moveThis)");
        debug("      @ attacker: "+attacker);
        debug("      @ defender: "+defender);
        debug("      @ moveThis: "+moveThis);
        */
        moveTheseArmies = moveThis;
      
        while (
          attacker.getArmies() > 3
          && attacker.getOwner() != defender.getOwner()
          && attacker.getArmies() > getAttackCost(defender) 
          )
        {
            board.attack(attacker, defender, false);
        }
        if (defender.getOwner() == ID)
        {
            return true;
        }
        else
        {
            return false;
        }
    } // Attacks if attacker has enough armies and moves this number of armies in; returns true if successful
    
    public void halfAttack(Country attacker, Country defender)
    {
        //debug("    halfAttack(attacker, defender)");
        //debug("      @ attacker: "+attacker);
        //debug("      @ defender: "+defender);
        while (
          attacker.getArmies() > 3
          && defender.getArmies() > 2
          && attacker.getOwner() != defender.getOwner()
          )
        {
            board.attack(attacker, defender, false);
        }
    } // Attacks until defender has less than three armies
    
    public boolean attackRoute(CountryRoute thisRoute, int armiesAtEnd)
    {
        // Only attack if we have enough armies to conquer this route with this number of armies at the end
        if (thisRoute.get(0).getArmies() < (getAttackCost(thisRoute) + armiesAtEnd))
        {
            return false;
        }

        // Attack first country in route
        int moveThis = Math.min((int) getAttackCost(thisRoute) - (int) getAttackCost(thisRoute.get(1)) + armiesAtEnd, thisRoute.get(0).getArmies() - 2);
        if(!safeAttack(thisRoute.get(0), thisRoute.get(1), moveThis))
        {
            return false;
        }
        
        // Attack remaining countries in route
        int size = thisRoute.size() - 2;
        for (int i = 0; i < size; i++)
        {
            moveThis = thisRoute.get(i+1).getArmies() - 2;
            if(!safeAttack(thisRoute.get(i+1), thisRoute.get(i+2), moveThis))
            {
                return false;
            }
        }
        return true;
    } // Attacks this route, leaving this number of armies at the end; returns true if the route is conquered

    public boolean attackCountries(Country[] theseCountries)
    {
        boolean result = true;

        //Find all enemy clusters
        CountryClusterSet targetSet = CountryClusterSet.getAllCountriesNotOwnedBy(ID, theseCountries);
        
        // Attack all clusters that we border
        for (int i = 0; i < targetSet.numberOfClusters(); i++)
        {
            CountryCluster thisCluster = targetSet.getCluster(i);
            if (weBorder(thisCluster)
              &&!attackCluster(thisCluster))
            {
                result = false;
            }
        }
        
        // Attack all clusters we don't border, starting with the easiest clusters to reach
        targetSet = CountryClusterSet.getAllCountriesNotOwnedBy(ID, theseCountries);
        while (targetSet.size() > 0)
        {
            float cheapestCost = 1000000000f;
            CountryCluster targetCluster = null;
            for (int i = 0; i < targetSet.numberOfClusters(); i++)
            {
                CountryCluster thisCluster = targetSet.getCluster(i);
                float thisCost = getAttackCost(getCheapestRoute(thisCluster));
                if (thisCost < cheapestCost)
                {
                    cheapestCost = thisCost;
                    targetCluster = thisCluster;
                }
            }
            
            // Attack cheapest cluster to reach
            if (!attackCluster(targetCluster))
            {
                return false;
            }
            targetSet = CountryClusterSet.getAllCountriesNotOwnedBy(ID, theseCountries);
        }
        return result;
    } // Attacks all unowned countries in this Country[]; returns true if successful
    
    public boolean attackCluster(CountryCluster thisCluster)
    {
        int moveThis;
        int armiesNeeded;

        // If we don't border this cluster, get the cheapest route and attack it
        if (!weBorder(thisCluster)) 
        {
            CountryRoute thisRoute = getCheapestRoute(thisCluster);
            if (thisRoute == null)
            {
                debug("ERROR! - We couldn't get a route to this cluster since it doesn't have any borders!");
                return false;
            }
            armiesNeeded = (int) getAttackCost(thisCluster);
            if (!attackRoute(thisRoute, armiesNeeded))
            {
                // Since we couldn't conquer route, return false
                return false;
            }
        }
        
        // Attack the countries in this cluster
        List clusterList = thisCluster.getList();
        List listCountries = new ArrayList();
        Country[] ourCountries = getOurCountries(countries);
        for (int i= 0; i <ourCountries.length; i++)
        {
            listCountries.add(ourCountries[i]);
        }
        for (int i = 0; i < clusterList.size(); i++)
        {
            listCountries.add(clusterList.get(i));
        }
        Country[] allCountries = convertListToCountryArray(listCountries);
        
        // If there are no borders in allCountries, then our goal is to conquer the board, so use the innermost country in this cluster
        if (getBorders(allCountries).length < 1)
        {
            allCountries = new Country[1];
            allCountries[0] = getInnermostCountry(thisCluster);
        }
        
        List failedAttackers = new ArrayList();
        List failedDefenders = new ArrayList();
        while(clusterList.size() > 0)
        {
            // Find neighboring countries in this cluster with the cheapest cost to attack its connected countries
            List potentialDefenders = new ArrayList();
            for (int i = 0; i < clusterList.size(); i++)
            {
                float minCost = 100000000f;
                if ( !failedDefenders.contains(clusterList.get(i))
                  && weBorder((Country) clusterList.get(i))
                  && getAttackCost(getConnectedCountries((Country)clusterList.get(i), thisCluster)) == minCost)
                {
                    potentialDefenders.add((Country) clusterList.get(i));
                }
                else if ( !failedDefenders.contains(clusterList.get(i))
                  && weBorder((Country) clusterList.get(i))
                  && getAttackCost(getConnectedCountries((Country)clusterList.get(i), thisCluster)) < minCost)
                {
                    potentialDefenders.clear();
                    potentialDefenders.add((Country) clusterList.get(i));
                }
            }
            if (potentialDefenders.size() < 1)
            {
                return false;
            }
            Country defender = null;
            int maxDistance = -1;
            for (int i = 0; i < potentialDefenders.size(); i++)
            {
                int thisDistance = getShortestDistanceToBorder((Country)potentialDefenders.get(i), allCountries);
                if (thisDistance > maxDistance)
                {
                    maxDistance = thisDistance;
                    defender = (Country) potentialDefenders.get(i);
                }
            }
            
            // Find neighbors that have the fewest enemy borders
            List potentialAttackers = new ArrayList();
            Country[] neighbors = getEnemyAttackers(defender);
            int minBorders = 1000000;
            for (int i = 0; i < neighbors.length; i++)
            {
                Country thisCountry = neighbors[i];
                if (thisCountry.getOwner() == ID
                  && !failedAttackers.contains(thisCountry))
                {
                    int numBorders = getEnemyAttackers(thisCountry).length;
                    if (numBorders == minBorders)
                    {
                        potentialAttackers.add(thisCountry);
                    }
                    else if (numBorders < minBorders)
                    {
                        minBorders = numBorders;
                        potentialAttackers.clear();
                        potentialAttackers.add(thisCountry);
                    }
                }
            }
            
            // If there are no potential attackers, we've failed
            if (potentialAttackers.size() < 1)
            {
                failedDefenders.add(defender);
            }

            // If there's only one potential attacker, safeAttack
            else if (potentialAttackers.size() == 1)
            {
                // Move enough armies needed to conquer all enemy countries connected to the defender
                moveThis = 0;
		
		// Find connected countries within this cluster
                Country[] connectedCountries = getConnectedCountries(defender, thisCluster);

		//  Calculate the cost needed to conquer the connected countries
                moveThis += getAttackCost(connectedCountries);
                
                if(!safeAttack((Country) potentialAttackers.get(0), defender, moveThis))
                {
                    // Can't conquer country with this attacker, so try again with others
                    failedAttackers.add(potentialAttackers.get(0));
                }
                else
                {
                    clusterList.remove(defender);
                }
            }
            
            // Since there are several potential attackers, halfAttack with all but the biggest army, then safeAttack with the biggest
            else
            {
                int biggestArmy = 0;
                Country max = null;
                for (int i = 0; i < potentialAttackers.size(); i++)
                {
                    Country checkCountry = (Country) potentialAttackers.get(i);
                    if (checkCountry.getArmies() > biggestArmy)
                    {
                        biggestArmy = checkCountry.getArmies();
                        max = checkCountry;
                    }
                }
                potentialAttackers.remove(max);
                for (int i = 0; i < potentialAttackers.size(); i++)
                {
                    Country halfAttacker = (Country) potentialAttackers.get(i);
                    halfAttack(halfAttacker, defender);
                    failedAttackers.add(halfAttacker);
                }
                
                // Move enough armies needed to conquer all enemy countries connected to the defender
                moveThis = 0;
		
		// Find connected countries within this cluster
                Country[] connectedCountries = getConnectedCountries(defender, thisCluster);

		//  Calculate the cost needed to conquer the connected countries
                moveThis += getAttackCost(connectedCountries);
		
                if (!safeAttack(max, defender, moveThis))
                {
                    // Can't conquer country with this attacker, so try again with others
                    failedAttackers.add(max);
                }
                else
                {
                    clusterList.remove(defender);
                }
            }
        }
        return true;
    } // Attacks all unowned countries in this cluster; returns true if successful
    
    public boolean attackCountry(Country defender)
    {
        List failedAttackers = new ArrayList();
        while(defender.getOwner() != ID)
        {
            // Find neighbors that have the fewest enemy borders
            List potentialAttackers = new ArrayList();
            Country[] neighbors = getEnemyAttackers(defender);
            int minBorders = 1000000;
            for (int i = 0; i < neighbors.length; i++)
            {
                Country thisCountry = neighbors[i];
                if (thisCountry.getOwner() == ID
                  && !failedAttackers.contains(thisCountry))
                {
                    int numBorders = getEnemyAttackers(thisCountry).length;
                    if (numBorders == minBorders)
                    {
                        potentialAttackers.add(thisCountry);
                    }
                    else if (numBorders < minBorders)
                    {
                        minBorders = numBorders;
                        potentialAttackers.clear();
                        potentialAttackers.add(thisCountry);
                    }
                }
            }
            
            // If there are no potential attackers, we've failed
            if (potentialAttackers.size() < 1)
            {
                return false;
            }

            // If there's only one potential attacker, safeAttack
            if (potentialAttackers.size() == 1)
            {
                if(!safeAttack((Country) potentialAttackers.get(0), defender, 0))
                {
                    // Can't conquer country with this attacker, so try again with others
                    failedAttackers.add(potentialAttackers.get(0));
                }
            }
            
            // Since there are several potential attackers, halfAttack with all but the biggest army, then safeAttack with the biggest
            else
            {
                int biggestArmy = 0;
                Country max = null;
                for (int i = 0; i < potentialAttackers.size(); i++)
                {
                    Country checkCountry = (Country) potentialAttackers.get(i);
                    if (checkCountry.getArmies() > biggestArmy)
                    {
                        biggestArmy = checkCountry.getArmies();
                        max = checkCountry;
                    }
                }
                potentialAttackers.remove(max);
                for (int i = 0; i < potentialAttackers.size(); i++)
                {
                    Country halfAttacker = (Country) potentialAttackers.get(i);
                    halfAttack(halfAttacker, defender);
                    failedAttackers.add(halfAttacker);
                }
                
                if (!safeAttack(max, defender, 0))
                {
                    // Can't conquer country with this attacker, so try again with others
                    failedAttackers.add(max);
                }
            }
        }
        return true;
    } // Attacks this country; returns true if successful
    
    public boolean attackContinent (int thisContinent)
    {
        List thisList = new ArrayList();
        CountryIterator iter = new ContinentIterator(thisContinent, countries);
        while (iter.hasNext())
        {
            thisList.add(iter.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        return attackCountries(theseCountries);
    } // Attacks all unowned countries in this continent; returns true if successful
    
    public void popContinents()
    {
        // List the benefits of popping each continent
        int[] popBenefit = new int[numContinents];
        for (int i = 0; i < numContinents; i++)
        {
            int cost = (int) getCostToPop(i);
            if (cost > 0)
            {
                popBenefit[i] =  board.getContinentBonus(i) - cost;
            }
            else
            {
                popBenefit[i] = -1;
            }
        }
        
        // Create array to see if we've looked at this continent yet
        boolean[] checked = new boolean[numContinents];
        for (int j = 0; j < numContinents; j++)
        {
            checked[j] = false;

        }
        
        // Pop the best continents first
        int numChecked = 0;
        while (numChecked < numContinents)
        {
            // Find best continent to pop
            int maxBenefit = -1000000;
            int bestContinent = -1;
            for (int k = 0; k < numContinents; k++)
            {
                if (!checked[k]
                  && popBenefit[k] > maxBenefit)
                {
                    maxBenefit = popBenefit[k];
                    bestContinent = k;
                }
            }
            
            // If the best continent has no benefit, stop attacking
            if (maxBenefit < 0)
            {
                return;
            }
            
            // Get route to pop the best continent
            CountryRoute targetRoute = getCheapestRoute(bestContinent);
            attackRoute(targetRoute, 0);
            checked[bestContinent] = true;
            numChecked++;
        }
    } // Pops any continents worth popping
    
    public void killPlayers()
    {
        boolean killedPlayer = false;
        do
        {
            // List the benefits of killing each player
            int[] killBenefit = new int[numPlayers];
            int ourCards = board.getPlayerCards(ID);
            int cardValue = board.getNextCardSetValue();
            for (int i = 0; i < numPlayers; i++)
            {
                int thisPlayerCards = board.getPlayerCards(i);
                int cost = (int) getCostToKill(i);
                int numCards = ourCards + thisPlayerCards;
                float calc = 0.0f;
                if (thisPlayerCards == 5)
                {
                    calc= cardValue;
                }
                else if (thisPlayerCards == 4)
                {   
                    calc = (float) cardValue * .817f;
                }
                else if (thisPlayerCards == 3)
                {
                    calc = (float) cardValue * .4228f;
                }
                int calcBenefit = (int) calc - cost;
                if (cost > 0
                  && numCards > 5)
                {
                    killBenefit[i] =  cardValue - cost;
                }
                else
                {
                    killBenefit[i] = -1;
                }
                if (cost > 0
                  && calc > cost
                  && calcBenefit > killBenefit[i])
                {
                    killBenefit[i] = calcBenefit;
                }
            }
        
            // Find best player to kill
            int maxBenefit = -1000000;
            int bestPlayer = -1;
            for (int k = 0; k < numPlayers; k++)
            {
                if (k != ID
                  && killBenefit[k] > maxBenefit)
                {
                    maxBenefit = killBenefit[k];
                    bestPlayer = k;
                }
            }
            
            // If the best player has no benefit, stop killing
            if (maxBenefit < 0)
            {
                return;
            }
            
            // Try to kill the best player
            if(killThisPlayer(bestPlayer))
            {
                killedPlayer = true;
            }
	    else
	    {
	        killedPlayer = false;
	    }
        } while (killedPlayer);
    } // Kills any players worth killing
    
    public boolean killThisPlayer (int thisPlayer)
    {
        List thisList = new ArrayList();
        CountryIterator iter = new PlayerIterator(thisPlayer, countries);
        while (iter.hasNext())
        {
            thisList.add(iter.next());
        }
        Country[] theseCountries = convertListToCountryArray(thisList);
        return attackCountries(theseCountries);
    } // Kills this player; returns true if successful
    
    public void attackForCard()
    {
        if (!board.tookOverACountry()
          && board.useCards())
        {
            // Calculate benefit getting a card
            int cash = board.getNextCardSetValue();
            float benefit = (float) cash / 3;
            
            // Find the cheapest country we can take over
            Country[] ourBorders = getBorders(getOurCountries(countries));
            List targetList = new ArrayList();
            for(int i = 0; i < ourBorders.length; i++)
            {
                int tempArmies = ourBorders[i].getArmies() - 4;
                Country[] temp = getEnemyAttackers(ourBorders[i]);
                for(int j = 0; j < temp.length; j++)
                {
                    if (!targetList.contains(temp[j])
                      && tempArmies > getRealAttackCost(temp[j]))
                    {
                        targetList.add(temp[j]);
                    }
                }
            }
            if (targetList.size() < 1)
            {
                return;
            }
            
            int minArmies = 1000000;
            Country defender = null;
            float cost = 0f;
            for (int i = 0; i < targetList.size(); i++)
            {
                Country temp = (Country) targetList.get(i);
                float tempCost = getRealAttackCost(temp);
                int tempArmies = temp.getArmies();
                if (tempCost < benefit
                  && tempArmies < minArmies)
                {
                    minArmies = tempArmies;
                    defender = temp;
                }
            }
            if (defender != null)
            {
                attackCountry(defender);
            }
        }
    } // Attack the easiest country if it's worth it to get another card
    





//
// FORTIFY METHODS
//


    public void fortifyToDefend (Country[] theseCountries)
    {
        if (theseCountries != null)
        {
            CountryClusterSet targetSet = CountryClusterSet.getAllCountriesOwnedBy(ID, theseCountries);
            int numClusters = targetSet.numberOfClusters();
            for (int i = 0; i < numClusters; i++)
            {
                CountryCluster currentCluster = targetSet.getCluster(i);
                fortifyToDefend(currentCluster);
            }
        }
   } // Fortify armies towards the borders of these countries
   
    public void fortifyToDefend(CountryCluster thisCluster)
    {
        Country[] theseCountries = convertListToCountryArray(thisCluster.getList());
        Country[] theseBorders = getBorders(theseCountries);
        fortifyToBorders(theseCountries, theseBorders);
   } // Fortify armies towards the borders of this cluster
    
    public void fortifyToBorders(Country[] theseCountries, Country[] theseBorders)
    {
        Country[] ourCountries = getOurCountries(theseCountries);
        
        // Fortify armies towards the borders
        int longestDistance = 0;
        for (int i = 0; i < ourCountries.length; i++)
        {
            int thisDistance = getShortestDistanceOwned(ourCountries[i], theseBorders);
            if (thisDistance > longestDistance)
            {
                longestDistance = thisDistance;
            }
        } 
        
        while (longestDistance > 0)
        {
            for (int j = 0; j < ourCountries.length; j++)
            {
                Country thisCountry = ourCountries[j];                
                if (getShortestDistanceOwned(thisCountry, theseBorders) == longestDistance
                  && thisCountry.getMoveableArmies() > 0 
                  && thisCountry.getArmies() > 2)
                {
                    // Find the countries thisCountry can fortify to
		    CountryIterator neighbors = new NeighborIterator( thisCountry, new PlayerIterator(ID, countries) );
		    Country bestPlaceToMove = null;
		    int shortestDistance = 1000000;
		    while (neighbors.hasNext())
                    {
			Country thisNeighbor = neighbors.next();
			
			// Fortify 1 army to all neighbors that are closer to a border and we can fortify
                        if (getShortestDistanceOwned(thisNeighbor, theseBorders) < longestDistance 
                          && thisCountry.getMoveableArmies() > 0 
                          && thisCountry.getArmies() > 2)
                        {
			    board.fortifyArmies( 1, thisCountry, thisNeighbor );
			    
			    // Check to see if this is the best Neighbor to place remaing armies
			    if (getShortestDistanceOwned(thisNeighbor, theseBorders) < shortestDistance)
                            {
				shortestDistance = getShortestDistanceOwned(thisNeighbor, theseBorders);
				bestPlaceToMove = thisNeighbor;
                            }
                        }
                    }
		    
		    // Move remaining armies to the best neighbor
		    int fortifyThis = -1;
		    if (thisCountry.getArmies() > 2)
                    {
			fortifyThis = Math.min(thisCountry.getArmies()-2, thisCountry.getMoveableArmies());
                    }
		    if (fortifyThis > 0)
                    {
			board.fortifyArmies( fortifyThis, thisCountry, bestPlaceToMove );
                    }
                }
            }
            longestDistance --;
        }
        
        // Distribute armies around the borders
        for (int j = 0; j < theseBorders.length; j++)
        {
            Country thisCountry = theseBorders[j];
            if (thisCountry.getMoveableArmies() > 0)
            {
                CountryIterator borderNeighbors = new NeighborIterator(thisCountry, countries);
                while (borderNeighbors.hasNext())
                {
                    Country thisNeighbor = borderNeighbors.next();
                    int difference = thisCountry.getArmies() - thisNeighbor.getArmies();
                    if ( difference > 1 
                      && thisCountry.getOwner() == ID
                      && thisNeighbor.getOwner() == ID
                      && thisCountry.getArmies() > 2
                      && thisCountry.getMoveableArmies() > 0
                      && getEnemyAttackers(thisNeighbor).length > 0)
                    {
                        // Move half the difference
                        int fortifyThis = difference / 2;
                        board.fortifyArmies( fortifyThis, thisCountry, thisNeighbor);
                    }
                }
            }
        }
    } // Fortify armies towards these borders, leaving at least two in each country, and distributes the armies around the border based on vulnerability




//
// CONVERSION UTILITIES
//


    public Country[] convertListToCountryArray(List thisList)
    {
        Country[] result = new Country[thisList.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = (Country) thisList.get(i);
        }
        return result;
    } // Converts a list of countries into a Country[]
    
    public Country[] convertRouteToCountryArray(CountryRoute thisRoute)
    {
        Country[] result = new Country[thisRoute.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = thisRoute.get(i);
        }
        return result;
    } // Converts a CountryRoute into a Country[]
    
    public List convertCountryArrayToList(Country[] theseCountries)
    {
        List result = new ArrayList();
        for (int i = 0; i < theseCountries.length; i++)
        {
            result.add(theseCountries[i]);
        }
        return result;
    } // Convewrts a Country[] into a list of countries
    
    public int[] convertListToIntArray(List thisList)
    {
        int[] result = new int[thisList.size()];
        for (int i = 0; i < thisList.size(); i++)
        {
            Integer objectInteger = (Integer) thisList.get(i);
            result[i] = objectInteger.intValue();
        }
        return result;
    } // Converts a list of numbers into an int[]




//
// FILE UTILITIES
//


    public static String newline = System.getProperty("line.separator");
    
    public void createPrefs()
    {
        try
        {
            FileWriter writer = new FileWriter(board.getAgentPath() + File.separator + "Defender.txt");
              writer.write(
                "# Defender 2.2 Preferences"+newline+
                ""+newline+
                "# This file will be read by Defender at the start of every game."+newline+
                "# Replace false with true to enable each option."+newline+
                ""+newline+
                "winGame = false"+newline+
                "# Determines whether Defender will try to win the game if he gets an enormous advantage."+newline+
                ""+newline+
                "popContinents = false"+newline+
                "# Determines whether Defender will try to pop continents."+newline+
                ""+newline+
                "killForCards = false"+newline+
                "# Determines whether Defender will try to kill players for cards."+newline+
                ""+newline+
                "farmForCards = false"+newline+
                "# Determines whether Defender will try to farm easy countries for cards."+newline+
                ""+newline+
                "chat% = 5"+newline+
                "# Determines what percentage of Defender's moves will he type something in the chat"+newline+
                "# Replace 5 with an integer between 0 and 100"+newline
                );
              writer.flush();
              writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public void getPrefs()
    {
        String line = null;
        try
        {
            FileReader fr = new FileReader(board.getAgentPath() + File.separator + "Defender.txt");
            BufferedReader br = new BufferedReader(fr);
            
            line = new String();
            while((line = br.readLine()) != null)
            {
                if (line.startsWith("winGame = true"))
                {
                    winPref = true;
                }
                else if (line.startsWith("popContinents = true"))
                {
                    popPref = true;
                }
                else if (line.startsWith("killForCards = true"))
                {
                    killPref = true;
                }
                else if(line.startsWith("farmForCards = true"))
                {
                    farmPref = true;
                }
                else if(line.startsWith("chat% = "))
                {
                    chatPref = Integer.parseInt(line.substring(8));
                    defaultChat = chatPref;
                }
            }
        } 
        catch (IOException e)
        {
            //e.printStackTrace();
            String error = e.toString();
            if (error.startsWith("java.io.FileNotFoundException"))
            {
                createPrefs();
            }
        }
    }

}