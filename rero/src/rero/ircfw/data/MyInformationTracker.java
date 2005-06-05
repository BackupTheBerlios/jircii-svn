package rero.ircfw.data;

/* keep "my" information in IDL up to date
   my information includes things such as my nickname and all that jazz...  */

import rero.ircfw.*;
import rero.ircfw.interfaces.FrameworkConstants;

import rero.util.*;

import java.util.*;
import java.util.regex.*;

public class MyInformationTracker extends DataEventAction implements FrameworkConstants
{
    private static String  supportPattern = ":.*? 005 .*? (.*?) :.*";
    private static Pattern isSupport      = Pattern.compile(supportPattern);

    public boolean isEvent(HashMap data)
    {
        String temp = (String)data.get($EVENT$);

        if ("001".equals(temp)) { return true; }
        if ("305".equals(temp)) { return true; } /* back from being away */
        if ("306".equals(temp)) { return true; } /* set as away */
        if ("005".equals(temp)) { return true; }

        return false;
    }

    public void process(HashMap data)
    {
        if ( "001".equals(data.get($EVENT$)) )
        {
            dataList.setMyNick((String)data.get($TARGET$));
        }

        if ("305".equals(data.get($EVENT$)))
        {
            dataList.getMyUserInformation().setBack();
        }

        if ("306".equals(data.get($EVENT$)))
        {
            dataList.getMyUserInformation().setAway();
        }

        if ("005".equals(data.get($EVENT$)))
        {
            StringParser parser = new StringParser(data.get($RAW$).toString(), isSupport);
            if (parser.matches())
            {
               String[] temp = parser.getParsedString(0).split(" ");
               for (int x = 0; x < temp.length; x++)
               {
                  String key, value;

                  if (temp[x].indexOf('=') > -1)
                  {
                     key    = temp[x].substring(0, temp[x].indexOf('='));
                     value  = temp[x].substring(key.length() + 1, temp[x].length());

                     dataList.addSupportInfo(key, value);

                     if (key.equals("PREFIX"))
                     {
                        String chars = value.substring(1, value.indexOf(')'));
                        String modes = value.substring(chars.length() + 2, value.length());

                        dataList.setPrefixInfo(chars, modes);
                     }
                  }
                  else
                  {
                     dataList.addSupportInfo(temp[x], "true");
                  }
               }
            }
        }

    }
}

