package rero.client.functions;

import sleep.engine.*;
import sleep.runtime.*;
import sleep.interfaces.*;

import rero.client.*;
import rero.ircfw.data.*;
import rero.ircfw.*;

import sleep.bridges.BridgeUtilities;

import java.util.*;

public class ChannelOperators extends Feature implements Predicate, Function, Loadable
{
   protected InternalDataList data;
 
   public void init()
   {
      getCapabilities().getScriptCore().addBridge(this);
      
      data = (InternalDataList)getCapabilities().getDataStructure("clientInformation");
   }

   public boolean scriptLoaded(ScriptInstance script)
   {
      String[] contents = new String[] { 
          "ison",      /** predicates */
          "isop",
          "ishalfop",
          "isvoice",
          "isnormal",
          "ismode",
          "hasmode",
          "&getUsers",  /** functions that return an array value */
          "&getOps",
          "&getHalfOps",
          "&getVoiced",
          "&getNormal",
          "&getTopic",  /** functions that return a normal scalar value */
          "&getKey",
          "&getMode",
          "&getLimit",
          "&getModeFor"
      };

      for (int x = 0; x < contents.length; x++)
      {
         script.getScriptEnvironment().getEnvironment().put(contents[x], this);
      }       

      return true;
   }

   public boolean scriptUnloaded(ScriptInstance script)
   {
      return true;
   }

   public Scalar evaluate(String function, ScriptInstance script, Stack locals)
   {
      if (function.equals("&getModeFor"))
      {
          String  _nick    = BridgeUtilities.getString(locals, "");
          String  _channel = BridgeUtilities.getString(locals, "");
          Channel channel  = data.getChannel(_channel);
 
          if (data.getUser(_nick) == null)
              return SleepUtils.getEmptyScalar();

          return SleepUtils.getScalar(data.getPrefixInfo().toString(data.getUser(_nick).getModeFor(channel)));
      }

      if (locals.size() != 1)
      {
         return SleepUtils.getEmptyScalar();
      }

      String  _channel = ((Scalar)locals.pop()).getValue().toString();
      Channel channel = data.getChannel(_channel);
  
      if (channel == null)
      {
         return null;
      }

      if (function.equals("&getTopic"))
      {
         return SleepUtils.getScalar(channel.getTopic());
      }

      if (function.equals("&getMode"))
      {
         return SleepUtils.getScalar(channel.getMode().toString());
      }

      if (function.equals("&getKey"))
      {
         return SleepUtils.getScalar(channel.getKey());
      }

      if (function.equals("&getLimit"))
      {
         return SleepUtils.getScalar(channel.getLimit());
      }

      Stack rv = new Stack();
      Iterator i = null;

      if (function.equals("&getUsers"))
      {
         return SleepUtils.getArrayWrapper(channel.getAllUsers());
      }

      if (function.equals("&getOps"))
      {
         return SleepUtils.getArrayWrapper(data.getUsersWithMode(_channel, 'o'));
      }

      if (function.equals("&getHalfOps"))
      {
         return SleepUtils.getArrayWrapper(data.getUsersWithMode(_channel, 'h'));
      }

      if (function.equals("&getVoiced"))
      {
         return SleepUtils.getArrayWrapper(data.getUsersWithMode(_channel, 'v'));
      }

      if (function.equals("&getNormal"))
      {
         return SleepUtils.getArrayWrapper(data.getUsersWithMode(_channel, ' '));
      }

      return null;
   }

   public boolean decide(String predicate, ScriptInstance script, Stack terms)
   {
      if (terms.size() != 2)
      {
         return false;
      }

      String channel = ((Scalar)terms.pop()).getValue().toString();  
      String nick    = ((Scalar)terms.pop()).getValue().toString();
 
      if (data.getChannel(channel) == null)
      {
         return false;
      }

      if (predicate.equals("ismode")) 
      {
         for (int x = 0; x < nick.length(); x++)
         {
            if (! data.getChannel(channel).getMode().isSet(nick.charAt(x)) )
            {
               return false;
            }
         }
         return true;
      }

      if (!data.isUser(nick))
      {
         return false;
      }

      if (predicate.equals("ison"))
      {
         return data.isOn(data.getUser(nick), data.getChannel(channel));
      } 

      int temp = data.getUser(nick).getModeFor(data.getChannel(channel));
     
      if (predicate.equals("isop"))
      {
         return data.getPrefixInfo().isMode(temp, 'o');
      }

      if (predicate.equals("ishalfop"))
      {
         return data.getPrefixInfo().isMode(temp, 'h');
      }

      if (predicate.equals("isvoice"))
      {
         return data.getPrefixInfo().isMode(temp, 'v');
      }

      if (predicate.equals("isnormal"))
      {
         return temp == 0;
      }

      if (predicate.equals("hasmode"))
      {
         return temp != 0;
      }

      return false;
   }
}
