package rero.gui.input;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import text.*;

import rero.config.*;

public class InputField extends JTextField implements KeyListener, ActionListener, MouseListener, ClientStateListener
{
    protected InputList  list = null;
    protected Border     defaultBorder;
    protected LinkedList listeners;   
    protected UserInputEvent event;

    protected InputBorder indent;

    public void mouseClicked(MouseEvent ev)
    {
       if (ev.getButton() == MouseEvent.BUTTON1 && ev.isShiftDown() && indent != null)
       {
          AttributedText temp = indent.getAttributes().getAttributesAt(ev.getX() - defaultBorder.getBorderInsets(this).left);

          if (temp != null)
          {
             if (temp.backIndex != -1 && ev.isControlDown())
             {
                ModifyColorMapDialog.showModifyColorMapDialog((JComponent)ev.getSource(), temp.backIndex);
             }
             else
             {
                ModifyColorMapDialog.showModifyColorMapDialog((JComponent)ev.getSource(), temp.foreIndex);
             }
             repaint();
          }
       }
    }

    public boolean isFocusable()
    {
       return true;
    }

    public void mouseEntered(MouseEvent ev) { }
    public void mouseExited(MouseEvent ev) { }
    public void mousePressed(MouseEvent ev) { }
    public void mouseReleased(MouseEvent ev) { }

    public InputField()
    {
       setUI(new javax.swing.plaf.basic.BasicTextFieldUI());

       setOpaque(false);

       defaultBorder = BorderFactory.createEmptyBorder(1, TextSource.UNIVERSAL_TWEAK, 1, 1); // a 1 pixel empty border all around;
       setBorder(defaultBorder);
/*
       setBackground(null); // suggested by Sun as a fix to a background being painted problem 
                            // in the GTK+ look and feel, unfortunately it doesn't work... maybe it will when 1.5 comes out      
*/

       addActionListener(this);
       addKeyListener(this);

       listeners = new LinkedList();

       event = new UserInputEvent();
       event.source = this;

       indent = null;

       addMouseListener(this);

       rehashColors();

       ClientState.getClientState().addClientStateListener("ui.editcolor", this);
       ClientState.getClientState().addClientStateListener("ui.font", this);
    }

    public void propertyChanged(String name, String parms)
    {
       rehashColors();
    }

    public void rehashColors()
    {
       Color temp = ClientState.getClientState().getColor("ui.editcolor", ClientDefaults.ui_editcolor);

       setForeground(temp);
       setCaretColor(temp.brighter()); 

       setFont(ClientState.getClientState().getFont("ui.font", ClientDefaults.ui_font));

       revalidate();
    }

    public void actionPerformed(ActionEvent ev)
    {
       event.text = ev.getActionCommand();

       InputList temp = new InputList();
       temp.text = event.text;

       if (event.text.length() <= 0)
       {
          fireInputEvent(); // fire an empty input event, it helps sometimes
          return;
       }

       if (list != null)
       {
          temp.prev = list;
          temp.next = list.next;

          if (list.next != null)
          {
             list.next.prev = temp;
          }

          list.next = temp;
       }

       list = temp;

       fireInputEvent();
    }
 
    public void addInputListener(InputListener l)
    {
       // we use addFirst for the following reasons...  generally input fields will have two listeners
       // the client itself will be listening and then there is a sort of master listener for all of the scripts.
       // the client itself will of course register the listener first
       // the master listener for scripts will register its listener second
       // by firing listeners in a last in first fired manner the scripts will get a chance to halt the processing
       // of the input event.   These kinds of things can be tough to keep track of so that is why I write this
       // comment.

       listeners.addFirst(l);
    }

    public void fireInputEvent()
    {
       ListIterator i = listeners.listIterator();
       while (i.hasNext())
       {
          InputListener temp = (InputListener)i.next();
          temp.onInput(event);
       }

       setText("");
       event.reset();
    }

    public String getIndent()
    {
       if (indent != null)
       {
          return indent.getText(); 
       }

       return "";
    }

    public void setIndent(String text)
    {
       if (text != null)
       {
          indent = new InputBorder(text);
          setBorder(new CompoundBorder(defaultBorder, indent));
       }
       else
       {
          setBorder(defaultBorder);
          indent = null;
       }
    }

    public void keyTyped(KeyEvent e)
    {
       //
       // deal with problem of windows binging when hitting backspace in an empty buffer
       //
       if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE && getText().length() == 0)
       {
          e.consume();
       }
    }

    public void keyPressed(KeyEvent e)
    {
       //
       // special built in control codes..
       //
       if (e.getModifiers() == 2)
       {
          int caretpos = getCaretPosition() + 1;

          switch (e.getKeyCode())
          {
             case 75: // control-k color
               setText(getText().substring(0, getCaretPosition()) + AttributedString.color + getText().substring(getCaretPosition(), getText().length()));
               setCaretPosition(caretpos);
               e.consume();
               return;
             case 85: // control-u underline
               setText(getText().substring(0, getCaretPosition()) + AttributedString.underline + getText().substring(getCaretPosition(), getText().length()));
               setCaretPosition(caretpos);
               e.consume();
               return;
             case 66: // control-b bold
               setText(getText().substring(0, getCaretPosition()) + AttributedString.bold + getText().substring(getCaretPosition(), getText().length()));
               setCaretPosition(caretpos);
               e.consume();
               return;
             case 79: // control-o cancel
               setText(getText().substring(0, getCaretPosition()) + AttributedString.cancel + getText().substring(getCaretPosition(), getText().length()));
               setCaretPosition(caretpos);
               e.consume();
               return;
             case 82: // control-r reverse
               setText(getText().substring(0, getCaretPosition()) + AttributedString.reverse + getText().substring(getCaretPosition(),getText().length()));
               setCaretPosition(caretpos);
               e.consume();
               return;
             default:
          }
       }

       if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiers() != 0)
       {
          event.text = getText();
          fireInputEvent();
          e.consume();
          return;
       }

       //
       // deal with arrow up
       //
       if (e.getKeyCode() == KeyEvent.VK_UP)
       {
          if (list != null)
          {
             setText(list.text);

             if (list.prev != null)
             {
                list = list.prev;
             }
          }          
          e.consume();
       }
   
       // deal with arrow down
       if (e.getKeyCode() == KeyEvent.VK_DOWN)
       {
          if (list != null && list.next != null)
          {
             list = list.next;
             setText(list.text);
          }          
          else
          {
             setText("");
          }
          e.consume();
       }

       // deal with ^K and other built in shortcuts
    }

    public void keyReleased(KeyEvent e) 
    { 
    }

    public void paint(Graphics g)
    {
       TextSource.initGraphics(g);
       super.paint(g);
    }

    protected Document createDefaultModel()
    {
       return new InputDocument();
    }

    class InputDocument extends PlainDocument
    {
       public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
       {
           if (str.indexOf('\n') == -1)
           {
//              super.insertString(offs, str, a);
              super.insertString(offs, str, a);
              return;
           }

           while (str.indexOf('\n') > -1)
           {
              event.text = str.substring(0, str.indexOf('\n'));
              fireInputEvent();

              if (str.indexOf('\n') == str.length())
              {
                 return;
              }
              else
              {
                 str = str.substring(str.indexOf('\n') + 1, str.length());
              }
           }

           if (str.length() > 0)
           {
              event.text = str;
              fireInputEvent();
           }
       }
    }
}
