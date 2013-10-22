/*
 * <copyright>
 * 
 * Copyright 1997-2001 BBNT Solutions, LLC.
 * under sponsorship of the Defense Advanced Research Projects
 * Agency (DARPA).
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.microedition.demo;

import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.util.Iterator;
import java.awt.geom.AffineTransform;
import java.awt.Shape;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import javax.swing.Box;
import java.awt.geom.Rectangle2D;

import org.cougaar.microedition.shared.*;

/**
 * Panel for drawing robot locations.
 */
public class RobotDemoPanel extends JPanel {

  double lat1, lon1, lat2, lon2  ;
  AffineTransform at;
  MouseHandler myMouseHandler=new MouseHandler();

  Border blackline;
  Border paneEdge;

  class MouseHandler extends MouseAdapter
  {
    int MOUSE_REGION=20;

    public void mouseClicked(MouseEvent e)
    {
      double x, y;
      double lat, lon;

      System.out.println("Mouse clicked at: "+e.getPoint());
      x = e.getX();
      y = e.getY();

      PositionCoordinate pc = inverseTransform(e.getPoint());
      System.out.println("Transformed to: "+pc.latitude+" "+pc.longitude);

      if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)
      {
	RobotProxy rp;

	for (Iterator iter=RobotDemoUI.getRobotInfo(); iter.hasNext();)
	{
	  rp=(RobotProxy)iter.next();
	  RobotDemoPanel src=(RobotDemoPanel)e.getSource();
	  Point p = src.transformCoordinates(rp.getLat(), rp.getLon());
	  double rpx=p.getX();
	  double rpy=p.getY();
	  if (x <= rpx+MOUSE_REGION && x >= rpx-MOUSE_REGION)
	  {
	    if (y <= rpy+MOUSE_REGION && y >= rpy-MOUSE_REGION*2)
	    {
	      if(e.isControlDown())
	      {
	        System.out.println("Get Image from Robot "+rp.getId());
	        RobotImageDisplay rid=new RobotImageDisplay(rp.getId());
	        rid.setVisible(true);
	      }
	      else
	      {
		System.out.println("Activate Laser Beam "+rp.getId());
		toggleLight(rp);
	      }
	    }
	  }
	}
      }

      if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
      {
	String urlBase="";
	String urlSuffix="";
	try
	{
	  urlBase=System.getProperty("startSystemUrlBase");
	  urlSuffix=System.getProperty("waypointSuffix");
	}
	catch(Exception ex)
	{
	  System.err.println("Error:  Check waypoint URL 1");
	}

	String fullUrl=urlBase+urlSuffix+Double.toString(pc.latitude)+","+Double.toString(pc.longitude);

	System.out.println("Retrieving data from url: ["+fullUrl+"]");

	try
	{
	  BufferedReader br=new BufferedReader(new InputStreamReader(new URL(fullUrl).openStream()));
	  for (String line = br.readLine(); line!=null; line = br.readLine())
	      System.out.println(line);
	}
        catch (Exception ex)
	{
	  System.err.println("Error:  Check waypoint URL 2");
	}
      }
    } // end mouseclicked

    private void toggleLight(RobotProxy rp)
    {
      String fullUrl;
      String urlBase="";
      String urlSuffix="";
      boolean fromState=rp.isLightOn();
      boolean toState=!rp.isLightOn();
      System.out.println("Toggling light from "+fromState+" to "+toState+".");
      rp.setLightOn(toState);
      String robotId=rp.getId();

      try {
        urlBase=System.getProperty("lightUrlBase");
      }
      catch(Exception ex) {
        System.err.println("Error:  Need to set lightUrlBase");
      }
      try {
        urlSuffix=System.getProperty("lightUrlSuffix");
      }
      catch(Exception ex) {
        System.err.println("Error:  Need to set lightUrlBase");
      }
      //urlBase="http://localhost:5555/ControlLight.PSP"+"?robotId=";
      //urlSuffix="?on=";
      fullUrl=urlBase+"$"+robotId+urlSuffix+robotId+"?on=true";
      System.out.println("Retrieving data from url: ["+fullUrl+"]");
      BufferedReader br=null;
      try {
        br=new BufferedReader(
          new InputStreamReader(new URL(fullUrl).openStream()));
        for (String line = br.readLine(); line!=null; line = br.readLine()) {
          System.out.println(line);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        try {
          if (br!=null) {
            br.close();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    } // end toggleLight
  } // end class

  public RobotDemoPanel() {
    addMouseListener(myMouseHandler);
  }
  public RobotDemoPanel(double lat1, double lon1,
                        double lat2, double lon2) {
    this.lat1=lat1;
    this.lon1=lon1;
    this.lat2=lat2;
    this.lon2=lon2;
    addMouseListener(myMouseHandler);

    addBorder();

  }

  public void addBorder() {
    Border myBorder=BorderFactory.createLineBorder(Color.black, 5);
    String title="(Lat: "+lat1+", Lon: "+lon1+")";
    TitledBorder titled1 = BorderFactory.createTitledBorder(
                              myBorder, title);
    titled1.setTitleJustification(TitledBorder.LEFT);
    titled1.setTitlePosition(TitledBorder.ABOVE_TOP);

    String title2=" (Lat: "+lat2+", Lon: "+lon2+")";
    TitledBorder titled2 = BorderFactory.createTitledBorder(
                              titled1, title2);
    titled2.setTitleJustification(TitledBorder.RIGHT);
    titled2.setTitlePosition(TitledBorder.BELOW_BOTTOM);

    setBorder(titled2);
  }

  private void adjustForBorder(Graphics2D g2) {
    Rectangle r=getVisibleRect();
    Insets delta = this.getInsets();
    //System.out.println("Rectangle2D r2=new Rectangle2D.Double("+r.getX()+ "+"+delta.left+", "+r.getY()+"+"+delta.top+", "
    //  +r.getWidth()+"-"+delta.right+", "+r.getHeight()+"-"+delta.bottom+");");

    // The following is better on Windows, but what about Linux?
    //    Rectangle2D r2=new Rectangle2D.Double(r.getX()+delta.left-4, r.getY()+delta.top-4,
    //      r.getWidth()-(delta.right+delta.left)+8,r.getHeight()-(delta.top+delta.bottom)+6);
    Rectangle2D r2=new Rectangle2D.Double(r.getX()+delta.left, r.getY()+delta.top,
      r.getWidth()-(delta.right+delta.left),r.getHeight()-(delta.top+delta.bottom));

    g2.clip(r2);
  }

  public void draw(Shape s, Graphics2D g2, AffineTransform at) {
    synchronized(this) {
    if (at==null) {
      g2.draw(s);
    } else {
      g2.draw(at.createTransformedShape(s));
    }
    }
  }


  Point transformCoordinates(double lat, double lon)
  {
    double x=getWidth()/(lon2-lon1)*(lon-lon1);
    double y=getHeight()/(lat2-lat1)*(lat-lat1);

    Point p = new Point();
    p.setLocation(x, y);

    return p;
  }

  PositionCoordinate inverseTransform(Point pt)
  {

    double w=getWidth();
    double h=getHeight();

    double lat= (pt.getY()*(lat2-lat1)/h)+lat1;
    double lon= (pt.getX()*(lon2-lon1)/w)+lon1;

    PositionCoordinate p = new PositionCoordinate(lat, lon);
    return p;
  }

  void drawRect(Graphics2D g2, double lat, double lon, int w, int h, double orientation) {
    drawRect(g2, lat, lon, w, h, orientation, false);
  }
  void fillRect(Graphics2D g2, double lat, double lon, int w, int h, double orientation) {
    drawRect(g2, lat, lon, w, h, orientation, true);
  }
  void drawRect(Graphics2D g2, double lat, double lon, int w, int h, double orientation, boolean fill) {
    performTransformedAction(g2,
        new DrawRectAction(lat, lon, w, h, orientation, fill));
  }

  void drawString(Graphics2D g2, double lat, double lon,
    int xOffset, int yOffset, double orientation, String label) {
    performTransformedAction(g2,
        new DrawStringAction(lat, lon, xOffset, yOffset, orientation, label));
  }


  void drawLine(Graphics2D g2, double lat, double lon, int len, double orientation) {
    drawLine(g2, lat, lon, len, orientation, false);
  }
  void drawDashedLine(Graphics2D g2, double lat, double lon, int len, double orientation) {
    drawLine(g2, lat, lon, len, orientation, true);
  }
  void drawLine(Graphics2D g2, double lat, double lon, int len, double orientation, boolean wantDashes) {
    performTransformedAction(g2,
        new DrawLineAction(lat, lon, len, orientation, wantDashes));
  }


  /**
   * Abstract base class for actions which are performed by RobotDemoPanel on
   * transformed objects.
   */
  abstract class TransformAction {
    abstract public void execute(Graphics2D g2, int x, int y);
    abstract public double getLat();
    abstract public double getLon();
    abstract public double getOrientation();
  }

  class DrawCircleAction extends TransformAction {
    double lat, lon;
    int dia, offset;
    double orientation;
    boolean fill;
    DrawCircleAction(double lati, double loni, int diai, int offseti,
      double orientationi, boolean filli) {
      lat=lati; lon=loni; dia=diai; offset=offseti;
      orientation=orientationi; fill=filli;
    }
    public double getLat() {return lat; }
    public double getLon() { return lon; }
    public double getOrientation() { return orientation; }
    public void execute(Graphics2D g2, int x, int y) {
      if (fill) {
        g2.fillArc(x-dia/2,y-offset-dia, dia, dia,0, 360);
      } else {
        g2.drawArc(x-dia/2,y-offset-dia, dia, dia,0, 360);
      }
    }
  }
  class DrawRectAction extends TransformAction {
    double lat, lon;
    int w, h;
    double orientation;
    boolean fill;
    DrawRectAction(double lati, double loni, int wi, int hi, double orientationi, boolean filli) {
      lat=lati; lon=loni; w=wi; h=hi;
      orientation=orientationi; fill=filli;
    }
    public double getLat() {return lat; }
    public double getLon() { return lon; }
    public double getOrientation() { return orientation; }
    public void execute(Graphics2D g2, int x, int y) {
      if (fill) {
        g2.fillRect(x-w/2, y-h/2, w, h);
      } else {
        g2.drawRect(x-w/2, y-h/2, w, h);
      }
    }
  }
  class DrawStringAction extends TransformAction {
    double lat, lon;
    int xOffset, yOffset;
    double orientation;
    String label;
    int xHedge=0;
    int yHedge=0;

    DrawStringAction(double lat, double lon,
      int xOffset, int yOffset, double orientation, String label) {
      this.lat=lat; this.lon=lon; this.xOffset=xOffset; this.yOffset=yOffset;
      this.orientation=orientation; this.label=label;
      try {      xHedge=Integer.parseInt(System.getProperty("drawName.xHedge"));
      } catch (Exception ex) {    }
      try {      yHedge=Integer.parseInt(System.getProperty("drawName.yHedge"));
      } catch (Exception ex) {    }
    }
    public double getLat() {return lat; }
    public double getLon() { return lon; }
    public double getOrientation() { return orientation; }
    public void execute(Graphics2D g2, int x, int y) {
        g2.drawString(label, x+xOffset+xHedge, y+yOffset+yHedge);
    }
  }
  class DrawLineAction extends TransformAction {
    double lat, lon;
    int len;
    double orientation;
    boolean wantDashes;
    DrawLineAction(double lati, double loni, int leni, double orientationi, boolean dashes) {
      lat=lati; lon=loni; len=leni;
      orientation=orientationi; wantDashes=dashes;
    }
    public double getLat() {return lat; }
    public double getLon() { return lon; }
    public double getOrientation() { return orientation; }
    public void execute(Graphics2D g2, int x, int y) {
      Stroke initStroke=g2.getStroke();
      if (wantDashes) {
        Stroke dashStroke=new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, (new float[] { 10f }), 0 );
        g2.setStroke(dashStroke);
      }
      g2.drawLine(x, y, x, y-len);
      if (wantDashes) {
        g2.setStroke(initStroke);
      }
    }
  }

  void performTransformedAction(Graphics2D g2, TransformAction ta)
  {
    int x,y;
    double lat=ta.getLat();
    double lon=ta.getLon();
    double orientation=ta.getOrientation();

    Point p = transformCoordinates(lat, lon);
    x=(int)p.getX();
    y=(int)p.getY();

    //System.out.println("Transformed lat, lon of ("+lat+", "+lon+") to [x,y] of ["+x+", "+y+"]");
    AffineTransform at = new AffineTransform();
    at.rotate(Math.toRadians(orientation), x, y);
    AffineTransform orig=g2.getTransform();
    g2.setTransform(at);

    ta.execute(g2, x, y);

    g2.setTransform(orig);
  }

  void drawCircle(Graphics2D g2, double lat, double lon, int dia, int offset,
      double orientation) {
      drawCircle(g2, lat, lon, dia, offset, orientation, false);
  }

  void fillCircle(Graphics2D g2, double lat, double lon, int dia, int offset,
      double orientation) {
      drawCircle(g2, lat, lon, dia, offset, orientation, true);
  }

  void drawCircle(Graphics2D g2, double lat, double lon, int dia, int offset,
      double orientation, boolean fill) {
      performTransformedAction(g2,
        new DrawCircleAction(lat, lon, dia, offset, orientation, fill));
  }

  public void fill(Shape s, Graphics2D g2, AffineTransform at) {
    synchronized(this) {
      if (at==null) {
        g2.fill(s);
      } else {
        g2.fill(at.createTransformedShape(s));
      }
    }
  }

  private void fillBackground(Graphics2D g2) {
    Color bg=g2.getBackground();
    Color fg=g2.getColor();
    Color bground=bg;

    try {
      float r=Float.parseFloat(System.getProperty("background.r"));
      float g=Float.parseFloat(System.getProperty("background.g"));
      float b=Float.parseFloat(System.getProperty("background.b"));
      bground=new Color(r, g, b);
    } catch (Exception ex) {
    }

    g2.setColor(bground);
    g2.fill(g2.getClip());
    g2.setColor(fg);
  }

  boolean invalidLat(double theLat)
  {
      return (theLat < -90 || theLat > 90);
  }

  boolean invalidLon(double theLon)
  {
      return (theLon > 180 || theLon < -180);
  }

//  public void paintComponent(Graphics g) {
  public void paint(Graphics g)
  {
    synchronized(this)
    {
//    super.paintComponent(g);
      super.paint(g);

      //System.out.println("rbpanel paint");
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


      adjustForBorder(g2);
      fillBackground(g2);

      RobotProxy rp;
      for (Iterator iter=RobotDemoUI.getRobotInfo(); iter.hasNext();)
      {
	rp=(RobotProxy)iter.next();
	if (rp.getId()!=null && !invalidLat(rp.getLat()) && !invalidLon(rp.getLon()))
	{
	  rp.draw(this, g2);
	}
      }
    }
  }
}