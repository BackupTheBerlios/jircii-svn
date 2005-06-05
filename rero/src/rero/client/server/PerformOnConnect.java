package rero.client.server;

import rero.ircfw.interfaces.*;
import java.util.*;

import rero.dialogs.server.*;
import rero.dck.items.NetworkSelect;

import rero.config.*;

import rero.util.*;
import rero.client.user.*;
import rero.client.*;

import rero.client.script.*;

import rero.net.*;

/** temporary listener to halt /list replies that don't match our criteria **/
public class PerformOnConnect extends Feature implements ChatListener
{
   UserHandler user;
   String      network; // just in case...
   boolean     newConnect = false;

   public void init()
   {
      user = (UserHandler)getCapabilities().getDataStructure(DataStructures.UserHandler);
      getCapabilities().addChatListener(this);
   }

   public int fireChatEvent(HashMap eventDescription)
   {
      String event = (String)eventDescription.get("$event");

         // perform on connect code, should be in its own class...
      if (event.equals("001"))
      {
         String[] temp = eventDescription.get("$parms").toString().split(" ");

         if (temp.length >= 4)
         {
            getCapabilities().getSocketConnection().getSocketInformation().network = temp[3];
            network = temp[3];
         }
         else
         {
            getCapabilities().getSocketConnection().getSocketInformation().network = "";
            network = "";
         }

         newConnect = true; // flag this as a new connection...
      }
      else if ((event.equals("376") || event.equals("422")) && newConnect) // 422 = no motd, 376 = end of /motd
      {
         if (ClientState.getClientState().isOption("perform.enabled", false))
         {
            Server myserver = ServerData.getServerData().getServerByName(getCapabilities().getSocketConnection().getSocketInformation().hostname);
            StringList actions;

            if (myserver != null && !myserver.isRandom())
            { 
               getCapabilities().getSocketConnection().getSocketInformation().network = myserver.getNetwork();
               network = myserver.getNetwork(); 
            }

            if (ClientState.getClientState().getString("perform." + network.toLowerCase(), null) != null)
            {
               actions = ClientState.getClientState().getStringList("perform." + network.toLowerCase());
            }
            else if (ClientState.getClientState().getString("perform." + network, null) != null) 
            {
               // this is a hack to help users migrate their jIRCii perform settings...

               actions = ClientState.getClientState().getStringList("perform." + network);
            }
            else
            {
               actions = ClientState.getClientState().getStringList("perform." + NetworkSelect.ALL_NETWORKS.toLowerCase());
            }

            Iterator ii = actions.getList().iterator();
            while (ii.hasNext())
            {
               String temp = ii.next().toString();
               processInput(temp);
            }
         }

         //
         // lets not interrupt the processing for "this" server...
         //
         if (rero.test.QuickConnect.IsQuickConnect())
         {
            user.processInput(rero.test.QuickConnect.GetInformation().getCommand());
         }

         newConnect = false; // our new connection status just expired...
      }
      return EVENT_DONE;
   }

   public void processInput(String input)
   {
      if (input.indexOf('$') > -1)
      {
         String command = ((ScriptManager)getCapabilities().getDataStructure(DataStructures.ScriptManager)).evalString("\"" + input + "\"");
         user.processInput(command);         
      }
      else
      {
         user.processInput(input);
      }
   }   

   public boolean isChatEvent(String event, HashMap eventDescription)
   {
       return (event.equals("376") || event.equals("001") || event.equals("422")); /* end of /MOTD reply */
   }
}
