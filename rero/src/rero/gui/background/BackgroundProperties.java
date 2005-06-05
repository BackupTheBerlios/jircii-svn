package rero.gui.background;

import java.awt.*;
import java.awt.image.*;

import rero.config.*;

public class BackgroundProperties implements ClientStateListener
{
   protected String type;

   protected int bgType;
   protected int bgStyle;

   protected Image image;
   protected Image transform;

   protected Color bgColor; 
   protected float bgTint;

   protected boolean isRelative;

   public boolean isRelative()
   {
      return isRelative;
   }

   public int getType()
   {
      return bgType;
   }

   public int getStyle()
   {
      return bgStyle;
   }

   public Color getColor()
   {
      return bgColor;
   } 

   public float getTint()
   {
      return bgTint;
   }

   public BackgroundProperties(String type, int defaultType)
   {
      this(type, Color.white, defaultType);
   }

   public BackgroundProperties(String type, Color defaultColor, int defaultType)
   {
      this(type, defaultColor, defaultType, BackgroundUtil.STYLE_TILE, .5f);
   }

   public BackgroundProperties(String _type, Color defaultColor, int defaultType, int defaultStyle, float defaultTint)
   {
      type = _type;

      ClientState.getClientState().addClientStateListener(type, this);
      init(defaultColor, defaultType, defaultStyle, defaultTint);
   }

   public void propertyChanged(String property, String parms)
   {
      init(bgColor, bgType, bgStyle, bgTint);
   }

   public void init(Color defaultColor, int defaultType, int defaultStyle, float defaultTint)
   {
      image          = null;
      transform      = null;
      bgType         = ClientState.getClientState().getInteger(type + ".bgtype"  , defaultType);
      bgColor        = ClientState.getClientState().getColor(  type + ".color"   , defaultColor);
      bgTint         = ClientState.getClientState().getFloat(  type + ".tint"    , defaultTint);
      bgStyle        = ClientState.getClientState().getInteger(type + ".bgstyle" , defaultStyle);

      isRelative     = ClientState.getClientState().isOption(  type + ".relative", false);
   }

   public Image getImage(Component c)
   {
      if (image == null)
      {
         String imageName = ClientState.getClientState().getString(type + ".image", "background.jpg");
         image            = BackgroundUtil.getManagedImage(c, imageName, bgTint, bgColor);
      }

      return image;
   }

   public Image getTransformedImage()
   {
      return transform;
   }

   public void setTransformedImage(Image i)
   {
      transform = i;
   }
}
