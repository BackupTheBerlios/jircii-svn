package rero.gui;

import rero.client.output.*;

import java.util.*;

import rero.ircfw.interfaces.ChatListener;
import rero.client.*;
import rero.gui.*;

import rero.util.*;

import rero.bridges.event.*;

import text.event.*;

public class ClickableURLHandler extends Feature implements ClickListener
{
    public void wordClicked(ClickEvent ev)
    {
        String item = ev.getClickedText().toLowerCase();

        if (item.matches("^\\(*(http|https|ftp)://.*"))
        {
            ClientUtils.openURL(extractURL(ev.getClickedText()));
            ev.consume();
            ev.acknowledge();
        } else if (item.matches("^www\\..*"))
        {
            String location = extractURL(ev.getClickedText());
            ClientUtils.openURL("http://" + location);
            ev.consume();
            ev.acknowledge();
        } else if (item.length() > 2 && ClientUtils.isChannel(item) && getCapabilities().isConnected() && !ev.getClickedText().endsWith("."))
        {
            getCapabilities().sendln("JOIN " + ev.getClickedText());
            ev.consume();
            ev.acknowledge();
        } else if (item.length() > 2 && ClientUtils.isChannel(item.substring(1, item.length())) && getCapabilities().isConnected())
        {
            getCapabilities().sendln("JOIN " + ev.getClickedText().substring(1, item.length() - 1));
            ev.consume();
            ev.acknowledge();
        }
    }

    private static String extractURL(String url)
    {
        if (url.charAt(0) == '(')
        {
            url = url.substring(1, url.length() - 1);
        }

        return url;
    }

    public void init()
    {
    }
}
