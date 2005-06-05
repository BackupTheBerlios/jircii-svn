package rero.dialogs;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.util.*;
import rero.config.*;

import rero.dck.*;
import rero.dck.items.*;

public class ExternalDialog extends DMain
{
   public String getTitle()
   {
      return "Applications";
   }

   public String getDescription()
   {
      return "Configure External Applications";
   }

   public void setupDialog()
   {
      addBlankSpace();
      addBlankSpace();
      addBlankSpace();

      addFileInput("ui.openfiles", ClientDefaults.ui_openfiles, "Launch files with:  ", 'O', 25);
      addLabel("jIRCii uses the above command to send files/urls to the application registered to handle them.", 30);

      addBlankSpace();
   }    
}



