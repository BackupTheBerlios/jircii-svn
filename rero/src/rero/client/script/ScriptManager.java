package rero.client.script;

import rero.client.*;
import rero.config.*;
import rero.script.*;

import sleep.error.*;
import sleep.runtime.*;

import java.io.*;
import java.util.*;

public class ScriptManager extends Feature implements ClientStateListener, RuntimeWarningWatcher
{
   protected ScriptLoader loader;
   protected Hashtable    environment; // shared environment...
   protected boolean      lock     = false;

   private static boolean SILENT   = false;  // for /reload mainly...

   public void init()
   {
      loader      = (ScriptLoader)getCapabilities().getDataStructure(DataStructures.ScriptLoader);
      environment = (Hashtable)getCapabilities().getDataStructure(DataStructures.SharedEnv);

      ClientState.getClientState().addClientStateListener("script.files", this);
   }

   public void storeDataStructures(WeakHashMap data)
   {
      data.put(DataStructures.ScriptManager, this);
   }

   public void hashScripts()
   {
      if (lock) 
         return;

      Set unload, load, configured;

      configured = new LinkedHashSet();
      configured.addAll(ClientState.getClientState().getStringList("script.files").getList());

      load   = loader.getScriptsToLoad(configured);
      unload = loader.getScriptsToUnload(configured);

      load.remove("menus");   
      load.remove("default");
      load.remove("lame");

      unload.remove("menus");   
      unload.remove("lame");   
      unload.remove("default");

      Iterator i = unload.iterator();
      while (i.hasNext())
      {
         String tt = (String)i.next();
         loader.unloadScript(tt);

         if (ClientState.getClientState().isOption("script.verboseLoad", ClientDefaults.script_verboseLoad))
         {
            getCapabilities().getUserInterface().printStatus("Successfully unloaded script " + tt);
         }
      }

      i = load.iterator();
      while (i.hasNext())
      {
         internalScriptLoad((String)i.next());
      }
   }

   public void loadTheme(String filename)
   {
      if ((new File(filename)).exists())
      {
         internalScriptLoad(filename);
         loader.unloadScript(filename);
      }
      else
      {
         getCapabilities().getUserInterface().printStatus("Error loading script: " + new File(filename).getAbsolutePath() + " does not exist");
      }
   }

   public void addScript(String filename)
   {
      if ((new File(filename)).exists())
      {
         StringList temp = ClientState.getClientState().getStringList("script.files");
         String     fn   = (new File(filename)).getAbsolutePath();

         if (!temp.getList().contains(fn))
         {
            temp.add(fn);
            temp.save();
            ClientState.getClientState().sync();
         }
         else
         {
            getCapabilities().getUserInterface().printStatus("Script file " + fn + " is already loaded.  Grabbing a beer instead");
         }
      }
      else
      {
         getCapabilities().getUserInterface().printStatus("Error loading script: " + new File(filename).getAbsolutePath() + " does not exist");
      }
   }

   public void evalScript(String code)
   {
      try
      {
         getCapabilities().getUserInterface().printActive(((ScriptInstance)loader.getScripts().getFirst()).getScriptEnvironment().evaluateExpression(code).toString()); 
      }
      catch (YourCodeSucksException ex)
      {
         formatCodeException("/eval input", ex);
      }
   }

   public String evalString(String code)
   {
      try
      {
         return ((ScriptInstance)loader.getScripts().getFirst()).getScriptEnvironment().evaluateExpression(code).toString(); 
      }
      catch (YourCodeSucksException ex)
      {
         formatCodeException("/eval input", ex);
      }
      return code;
   }

   public void reloadScript(String filename)
   {
      boolean flag = true;

      Iterator i = findScripts(filename, ClientState.getClientState().getStringList("script.files").getList()).iterator();
      while (i.hasNext())
      {
         flag = false;

         String scriptf = (String)i.next();

//         if (ClientState.getClientState().isOption("script.verboseLoad", ClientDefaults.script_verboseLoad))
//         {
//            getCapabilities().getUserInterface().printStatus("Attempting to reload script " + scriptf);
//         }

         removeScript(scriptf);
         addScript(scriptf);
//         loader.unloadScript(scriptf);
//         internalScriptLoad(scriptf);        
      }

      if (flag)
         getCapabilities().getUserInterface().printStatus("Error (re)loading script: " + filename + " isn't loaded");
   }

   private LinkedList findScripts(String filename, LinkedList theList)
   {
      LinkedList goodBye = new LinkedList();

      Iterator i = theList.iterator();
      while (i.hasNext())
      {
         File temp = new File((String)i.next());
         if (temp.getName().equals(filename) || temp.getAbsolutePath().equals(filename))
         {
            goodBye.add(temp.getAbsolutePath());
         }
      }

      return goodBye;
   }

   public void removeScript(String filename)
   {
      StringList temp = ClientState.getClientState().getStringList("script.files");
      Iterator i      = findScripts(filename, temp.getList()).iterator();

      while (i.hasNext())
      {
         String remMe = (String)i.next();
         temp.remove(remMe);
      }

      temp.save();
      ClientState.getClientState().sync(); // this will cause all the script values to be rehashed...
   }

   private static boolean lame = true;

   public void loadLameScripts()
   {
      try
      {
         if (lame)
         {
            ScriptInstance defaults = loader.loadScript("lame", ClientState.getClientState().getResourceAsStream("lame.irc"), environment);

            defaults.addWarningWatcher(this);
            defaults.runScript();

            lame = false;
         }
      }
      catch (Exception ex)
      {

      }
   }

   public void loadScripts()
   {
      //
      // do other fun stuff... i.e. script loading and such
      //
 
      try
      {
         if (ClientState.getClientState().isOption("load.default", true))
         {
            long start = System.currentTimeMillis();
            ScriptInstance defaults = loader.loadScript("default", ClientState.getClientState().getResourceAsStream("default.irc"), environment);
//            System.out.println("Default script loaded in: " + (System.currentTimeMillis() - start));

            defaults.addWarningWatcher(this);
            defaults.runScript();
         }

         if (ClientState.getClientState().isOption("load.menus", true))
         {
            long start = System.currentTimeMillis();
            ScriptInstance defaults = loader.loadScript("menus", ClientState.getClientState().getResourceAsStream("menus.irc"), environment);
//            System.out.println("Menu script loaded in: " + (System.currentTimeMillis() - start));

            defaults.addWarningWatcher(this);
            defaults.runScript();
         }

         if (ClientState.getClientState().isOption("load.lame", false))
         {
            loadLameScripts();
         }
      }
      catch (YourCodeSucksException ex)
      {
         formatCodeException("<Internal Scripts>", ex);
      }
      catch (IOException ex)
      {
         ex.printStackTrace();
      }

      Iterator i = ClientState.getClientState().getStringList("script.files").getList().iterator();
      while (i.hasNext())
      {
         internalScriptLoad((String)i.next());
      }
   }

   public void propertyChanged(String name, String value)
   {
       hashScripts(); 
   }

   private void internalScriptLoad(String scriptFile)
   {
      try
      {   
         ScriptInstance scripti = loader.loadScript(scriptFile, environment);
         scripti.addWarningWatcher(this);

         if (ClientState.getClientState().isOption("script.verboseLoad", ClientDefaults.script_verboseLoad))
         {
            getCapabilities().getUserInterface().printStatus("Successfully loaded script " + new File(scriptFile).getName());
         }

         scripti.runScript();
      }
      catch (YourCodeSucksException ex)
      {
         formatCodeException(scriptFile, ex);

         lock = true;
         removeScript(scriptFile);
         lock = false;
      }
      catch (IOException ex2)
      {
         getCapabilities().getUserInterface().printStatus("Error loading "+(new File(scriptFile)).getName()+": " + ex2.getMessage());
      }      
      catch (Exception ex3)
      {
         getCapabilities().getUserInterface().printStatus("Error loading "+(new File(scriptFile)).getName()+": " + ex3.getMessage() + " <-- could be a sleep bug, please report :)");
         ex3.printStackTrace();
      }
   }

   public void processScriptWarning(ScriptWarning warn)
   {
      if (! ClientState.getClientState().isOption("script.ignoreWarnings", ClientDefaults.script_ignoreWarnings))
      {
          String[] temp = warn.getMessage().split("\n");

          String fname = warn.getNameShort();

          getCapabilities().getUserInterface().printStatus("*** Script Warning: " + temp[0] + " at " + fname + ":" + warn.getLineNumber());

          for (int x = 1; x < temp.length; x++)
          {
             getCapabilities().getUserInterface().printStatus("     " + temp[x]);
          }
      }
   }

   private void formatCodeException(String scriptFile, YourCodeSucksException ex)
   {
      getCapabilities().getUserInterface().printStatus("*** " + ex.getErrors().size() + " error(s) loading " + scriptFile);

      Iterator i = ex.getErrors().iterator();
      while (i.hasNext())
      {
         SyntaxError anError = (SyntaxError)i.next();
         getCapabilities().getUserInterface().printStatus("Error: " + anError.getDescription() + " at line " + anError.getLineNumber());
         getCapabilities().getUserInterface().printStatus("       " + anError.getCodeSnippet());

         if (anError.getMarker() != null)
            getCapabilities().getUserInterface().printStatus("       " + anError.getMarker());
      }
   }
}

