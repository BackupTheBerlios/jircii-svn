package rero.client.output;

import rero.ircfw.*;
import rero.ircfw.data.*;

import rero.bridges.event.*;
import rero.bridges.set.*;

import rero.client.*;

import java.util.*;

import rero.dcc.*;
import rero.net.*;

import rero.util.*;

import rero.client.listeners.*;

public class ChatCapabilities extends Feature
{
   protected SocketConnection    sock;
   protected OutputCapabilities  output;
   protected DataDCC             dccData;

   public void init()
   {
      sock    = getCapabilities().getSocketConnection();
      output  = getCapabilities().getOutputCapabilities();
      dccData = (DataDCC)getCapabilities().getDataStructure("dcc");
   }

   public void sendMessage(String nickname, String message)
   {
      HashMap eventData = new HashMap();

      if (nickname.charAt(0) == '=')
      {
          String dnickname = nickname.substring(1, nickname.length());
          Chat connection = (Chat)dccData.getSpecificConnection(dnickname, ProtocolDCC.DCC_CHAT);

          if (connection == null)
          {
              eventData.put("$target", dnickname);
              eventData.put("$parms", message);
              eventData.put("$data", dnickname  + " " + message);
              output.fireSetTarget(eventData, nickname, "SEND_CHAT_ERROR");
              return;
          }

          connection.sendln(message);
      }
      else
      {
          sock.println("PRIVMSG " + nickname + " :" + message);
      }

      if (nickname.charAt(0) == '=')
      {
          eventData.put("$target", nickname.substring(1, nickname.length()));
          eventData.put("$parms", message);
          eventData.put("$data", nickname.substring(1, nickname.length()) + " " + message);

          output.fireSetTarget(eventData, nickname, "SEND_CHAT");
      }
      else if (ClientUtils.isChannel(nickname))
      {
          eventData.put("$target", nickname);
          eventData.put("$channel", nickname);
          eventData.put("$parms", message);
          eventData.put("$data", nickname + " " + message);

          output.fireSetTarget(eventData, nickname, output.chooseSet(nickname, "SEND_TEXT", "SEND_TEXT_INACTIVE"));
      }
      else
      {
          eventData.put("$target", nickname);
          eventData.put("$parms", message);
          eventData.put("$data", nickname + " " + message);

          output.fireSetQuery(eventData, nickname, nickname, "SEND_MSG");
      }
   }

   public void sendNotice(String target, String message)
   {
       sock.println("NOTICE " + target + " :" + message);

       HashMap eventData = new HashMap();

       eventData.put("$target", target);
       eventData.put("$parms", message);
       eventData.put("$data", target + " " + message);

       output.fireSetConfused(eventData, target, "notice", "SEND_NOTICE");
   }
 
   public void sendAction(String target, String message)
   {
      HashMap eventData = new HashMap();

      if (target.charAt(0) == '=')
      {
          eventData.put("$target", target.substring(1));
          eventData.put("$parms", message);
          eventData.put("$data", target.substring(1) + " " + message);

          output.fireSetTarget(eventData, target, output.chooseSet(target, "SEND_ACTION", "SEND_ACTION_INACTIVE"));
      }
      else if (ClientUtils.isChannel(target))
      {
          eventData.put("$target", target);
          eventData.put("$channel", target);
          eventData.put("$parms", message);
          eventData.put("$data", target + " " + message);

          output.fireSetTarget(eventData, target, output.chooseSet(target, "SEND_ACTION", "SEND_ACTION_INACTIVE"));
      }
      else
      {
          eventData.put("$target", target);
          eventData.put("$parms", message);
          eventData.put("$data", target + " " + message);

          output.fireSetTarget(eventData, target, "SEND_ACTION");
      }

      if (target.charAt(0) == '=')
      {
          Chat connection = (Chat)dccData.getSpecificConnection(target.substring(1), ProtocolDCC.DCC_CHAT);

          if (connection == null)
          {
              output.fireSetTarget(eventData, target, "SEND_CHAT_ERROR");
              return;
          }

          connection.sendln((char)1 + "ACTION " + message + (char)1);
      }
      else
      {
          sock.println("PRIVMSG " + target + " :" + (char)1 + "ACTION " + message + (char)1);
      }
   }

   public void sendRequest(String target, String type, String parms)
   {
       HashMap eventData = new HashMap();

       eventData.put("$target", target);
       eventData.put("$parms", parms);
       eventData.put("$type", type);
       eventData.put("$data", target + " " + type + " " + parms); 

       if (type.equals("PING") && parms.equals(""))
       {
          parms = System.currentTimeMillis() + "";
       }

       output.fireSetConfused(eventData, target, "reply", "SEND_CTCP");

       if (parms.length() > 0)
       {
          sock.println("PRIVMSG " + target + " :" + (char)1 + type.toUpperCase() + " " + parms + (char)1);
       }
       else
       {
          sock.println("PRIVMSG " + target + " :" + (char)1 + type.toUpperCase() + (char)1);
       }
   }

   public void sendReply(String target, String type, String parms)
   {
       sock.println("NOTICE " + target + " :" + (char)1 + type.toUpperCase() + " " + parms + (char)1);
   }
}


