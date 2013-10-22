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
package org.cougaar.microedition.demo.ugs;

import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Arc2D.*; // cannot use Double or it conflicts w/ Line2D.Double
import java.awt.geom.AffineTransform;
import java.awt.*;
import java.util.*;

/**
 */

public class TracksProxy {

  double actualLength=18; //inches (about)
  double actualWidth=6; // inches (about)
  //double length=actualLength*2;
  double length=actualLength;
  double width=actualWidth*2;

  // --------
  String id=this.toString();
//  double lon=0, lat=0;
  Vector markers=new Vector();
  double[] lats;
  double[] lons;
  Color myColor=Color.red;
  // ---------

  double heading=0;
  double headingLineLength=length;
  double bearingLineLength=length*100;


  HeadingLine headingLine;
//  LightCircle lightCircle;
  PictureAvailableRectangle pictureAvailableRectangle;
//  BearingLine bearingLine=new BearingLine();

//  public double getLat() {return lat; }
//  public double getLon() {return lon; }

//  public TracksProxy(String id, double lon, double lat) {
//    this.id=id;
//    headingLine=new HeadingLine(lon, lat, headingLineLength); // move down after all other updates are upgraded
//    updateLocation(lon, lat);
//  }

  public TracksProxy(String theId, Vector theLats, Vector theLons) {
    System.out.println("TracksProxy ctor : "+id);
    id=theId;
    myColor=nextColor();
    update(theLats, theLons);
  }

  Color[] PosibleColors = { Color.red, Color.blue, Color.gray, Color.yellow, Color.cyan,  };
  static int curColor=0;
  private Color nextColor() {
    Color ret=PosibleColors[curColor];
    curColor = ++curColor % PosibleColors.length;
    return ret;
  }

  // top-level
  public String getId() { return id; }
  public void  setId(String value) { id=value; }
//  private int centerX() { return (int) (lon + width/2.0) ;}
//  private int centerY() { return (int) (lat + length/2.0) ;}


  boolean trackDisplay=false;
  public void setTrackDisplay(boolean value)  {
    trackDisplay=value;
  }
  public boolean getTrackDisplay() { return trackDisplay; }
  public void toggleTrackDisplay() { trackDisplay=!trackDisplay; }

  Stroke myStroke=new BasicStroke(3);
  /**
   * Newer better draw which delegates tot he UGSDemoPanel
   */
  public void draw(UGSDemoPanel p, Graphics2D g2) {
    synchronized(p) {


    /* Draw rectangle at robot location.  Fill the rectangle if an image
     * is available.
     */
    if (getTrackDisplay()) {
      int entries=lats.length;
      Color origColor=g2.getColor();
      g2.setColor(myColor);
      Stroke origStroke = g2.getStroke();
      g2.setStroke(myStroke);
      p.drawPolyLine(g2, lats, lons, entries, true);
      g2.setStroke(origStroke);
      p.drawString(g2, lats[entries-1], lons[entries-1], 0, (int)length, heading, getId());

      for (Iterator miter=markers.iterator(); miter.hasNext(); ) {
        ((CircleMarker)miter.next()).draw(p, g2);
      }
      g2.setColor(origColor);
    }

//    // Draw heading line
//    p.drawLine(g2, lat, lon, (int)headingLineLength, heading);
//    // draw flashlight circle
//    lightCircle.draw(p, g2);


    }
  }

  // HeadingLine pass-thrus
  private double headingX() { return headingLine.headingX() ;}
  private double headingY() { return headingLine.headingY();}

  // BearingLine pass-thrus

//  void setBearing(double bear) {
//    bearingLine=new BearingLine(centerX(), centerY(), bear, bearingLineLength);
//  }
//
//  void clearBearing() {
//    if (bearingLine!=null) bearingLine.clearBearing();
//  }

  // update methods
  void update(Vector theLats, Vector theLons) {
    System.out.println("track update : "+id+", "+theLats+", "+theLons);
    Iterator latIter=theLats.iterator();
    Iterator lonIter=theLons.iterator();
    markers.clear();
    lats=new double[theLats.size()];
    lons=new double[theLons.size()];
    for (int idx=0; latIter.hasNext() && lonIter.hasNext(); idx++) {
      lats[idx]=((java.lang.Double)latIter.next()).doubleValue();
      lons[idx]=((java.lang.Double)lonIter.next()).doubleValue();
      markers.add(new CircleMarker(lons[idx], lats[idx], width, width));
    }
    if (latIter.hasNext()||lonIter.hasNext()) {
      System.out.println("Warning: latIter and lonIter should have same number of items "+theLats.size()+", "+theLons.size());
    }
    System.out.print("."); System.out.flush();
  }

//  void updateLocation(double lon, double lat) {
//    this.lon=lon;
//    this.lat=lat;
//    updateBearingLines();
//    updateHeadingLine();
//    updateLightCircle();
//    updatePictureAvailableRectangle();
//  }
//  private void updateHeadingLine() {
//    if (headingLine!=null)
//      headingLine.setLine(centerX(), centerY());
//  }
//  private void updateBearingLines() {
//    bearingLines.clear();
//    if (bearings!=null) {
//      for(Iterator bit=bearings.iterator(); bit.hasNext();){
//        bearingLines.add(new BearingLine(lon, lat,
//          ((java.lang.Double)bit.next()).doubleValue(), bearingLineLength));
//      }
//    }
//  }
//  private void updateLightCircle() {
//    double lcWidth=width;
//    double lcLength=width;
//    if (lightCircle==null) {
//      lightCircle=new LightCircle(lon, lat, lcWidth, lcLength);
//    } else {
//      lightCircle.updateLocation(lon,lat);
//    }
//  }
//  private void updatePictureAvailableRectangle() {
//    pictureAvailableRectangle=new PictureAvailableRectangle(lon, lat, width, length);
//  }

//  public void stepForward(int dist) {
//     // System.out.println("in stepForward "+this);
//    updateLocation(lon, lat - dist);
//  }
  public void setHeading(double degrees) {
    heading=degrees;
  }
//  public  boolean isLightOn() { return (lightCircle !=null && lightCircle.isLightOn()); }
//  public  void setLightOn(boolean value) { lightCircle.setLightOn(value); }


  class CircleMarker extends Arc2D.Double {

    boolean filled=true;
    double lon,  lat,  w,  h;
    final static int type=Arc2D.CHORD;

    CircleMarker(double lon, double lat, double w, double h) {
        this.lon=lon;
        this.lat=lat;
        this.w=w;
        this.h=h;
        System.out.println("CircleMarker ctor.");
    }
    void updateLocation(double lon, double lat) {
        this.lon=lon;
        this.lat=lat;
        this.w=w;
        this.h=h;
    }

    boolean isFilled() { return filled; }
    void setFilled(boolean value) {
      filled=value;
      // System.out.println("setting filled "+value);
    }
    void draw(Graphics2D g2, AffineTransform at) {
      if (isFilled()) {
        g2.fill(at.createTransformedShape(this));
      } else {
        g2.draw(at.createTransformedShape(this));
      }
    }
    /**
     * Newer better draw which delegates tot he UGSDemoPanel
     * Draw filled circle if light is on.
     * Draw empty circle if light is off.
     */
    public void draw(UGSDemoPanel p, Graphics2D g2) {
      synchronized(p) {
        int radius=(int)w;
//        int offset=(int)headingLineLength;
        int offset=(int) -w/2;
        p.drawCircle(g2, lat, lon, (int) radius, offset, heading, isFilled());
      }
    }
  }

  class LightCircle extends Arc2D.Double {

    boolean lightOn=false;
    double lon,  lat,  w,  h;
    final static int type=Arc2D.CHORD;

    LightCircle(double lon, double lat, double w, double h) {
        this.lon=lon;
        this.lat=lat;
        this.w=w;
        this.h=h;
        System.out.println("LC ctor.");

        // following is just for testing
        // lightOn = testVar;
        // testVar=!testVar;
    }
    void updateLocation(double lon, double lat) {
        this.lon=lon;
        this.lat=lat;
        this.w=w;
        this.h=h;
    }

    boolean isLightOn() { return lightOn; }
    void setLightOn(boolean value) {
      lightOn=value;
      // System.out.println("LC setting light "+value);
    }
    void draw(Graphics2D g2, AffineTransform at) {
      if (isLightOn()) {
        g2.fill(at.createTransformedShape(this));
      } else {
        g2.draw(at.createTransformedShape(this));
      }
    }
    /**
     * Newer better draw which delegates tot he UGSDemoPanel
     * Draw filled circle if light is on.
     * Draw empty circle if light is off.
     */
    public void draw(UGSDemoPanel p, Graphics2D g2) {
      synchronized(p) {
        int radius=(int)w;
        int offset=(int)headingLineLength;
        // if light is on then set fill on
        p.drawCircle(g2, lat, lon, (int) radius, offset, heading, isLightOn());
      }
    }
    public void draw_prev(UGSDemoPanel p, Graphics2D g2, AffineTransform at) {
      synchronized(p) {
      if (isLightOn()) {
        p.fill(this, g2, at);
      } else {
        p.draw(this, g2, at);
      }
      }
    }
  }

  class PictureAvailableRectangle extends Rectangle2D.Double {
    boolean avail=false;
    double lon, lat, w, h;
    PictureAvailableRectangle(double loni, double lati, double wi, double hi) {
        //super(lon,  lat,  w,  h);
      lat=lati;
      lon=loni;
      w=wi;
      h=hi;
        // following is just for testing
        // avail = testVar;
    }
    boolean isPictureAvailable() { return avail; }
    void setPictureAvailable(boolean value) { avail=value; }
    void draw(Graphics2D g2, AffineTransform at) {
      if (avail) {
        g2.fill(at.createTransformedShape(this));
      } else {
        g2.draw(at.createTransformedShape(this));
      }
    }

    /**
     * Newer better draw which delegates tot he UGSDemoPanel
     * Draw rectangle at robot location.  Fill the rectangle if an image
     * is available.
     */
    public void draw(UGSDemoPanel p, Graphics2D g2) {
      p.drawRect(g2, lat, lon, (int)w, (int)h, heading, avail);
    }
    public void draw(UGSDemoPanel p, Graphics2D g2, AffineTransform at) {
      if (avail) {
        p.fill(this, g2, at);
      } else {
        p.draw(this, g2, at);
      }
    }
  }

  class HeadingLine extends Line2D.Double {
    double lon1, lat1;
    HeadingLine(double lon, double lat, double length) {
        headingLineLength=length;
        lon1=lon;
        lat1=lat;
        super.setLine(lon,  lat,  headingX(),  headingY());
    }

    void setLine(double lon, double lat) {
      lon1=lon;
      lat1=lat;
      super.setLine(lon,  lat,  headingX(),  headingY());
    }

    double headingX() { return lon1 ;}
    double headingY() { return lat1-headingLineLength ;}
    void draw(Graphics2D g2, AffineTransform at) {
        g2.draw(at.createTransformedShape(this));
    }
    /**
     * Newer better draw which delegates tot he UGSDemoPanel
     */
    public void draw(UGSDemoPanel p, Graphics2D g2, AffineTransform at) {
      synchronized(p) {
        p.draw(this, g2, at);
      }
    }
  }

//  class BearingLine extends Line2D.Double {
//    double lon1, lat1, blon, blat;
//    boolean hasBearing=false;
//    double bearing=45;
//    double bearingLineLength;
//    BearingLine() { }
//    BearingLine(double lon, double lat, double bearing, double length) {
//        bearingLineLength=length;
//        lon1=lon;
//        lat1=lat;
//        this.bearing=bearing;
//        super.setLine(lon,  lat,  lon,  lat-bearingLineLength);
//        Line2D.Double tmpLine=new Line2D.Double(lon,  lat,  lon,  lat-bearingLineLength);
//        AffineTransform at = new AffineTransform();
//        at.rotate(Math.toRadians(bearing), lon, lat);
//        txShape = at.createTransformedShape(tmpLine);
//        Rectangle2D b2d = at.createTransformedShape(tmpLine).getBounds2D();
//        blon=b2d.getMinX();
//        blat=b2d.getMinY();
//        hasBearing=true;
//
//    }
//  Shape txShape=null;
//    double bearingX() { return blon ;}
//    double bearingY() { return blat ;}
//    double getBearing() { return bearing ;}
//    void draw(Graphics2D g2, AffineTransform at) {
//      if (hasBearing) {
//        AffineTransform bat = new AffineTransform();
//        bat.rotate(Math.toRadians(bearing), lon1, lat1);
//
//        Stroke initStroke=g2.getStroke();
//        Stroke dashStroke=new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, (new float[] { 10f }), 0 );
//        g2.setStroke(dashStroke);
//
//        g2.draw(bat.createTransformedShape(this));
//
//        g2.setStroke(initStroke);
//
//      }
//    }
//
//    public void clearBearing() { hasBearing=false; }
//    /**
//     * Newer better draw which delegates tot he UGSDemoPanel
//     */
//    public void draw(UGSDemoPanel p, Graphics2D g2) {
//      synchronized(p) {
//      if (hasBearing) {
//        p.drawDashedLine(g2, lat, lon, (int)bearingLineLength, bearing);
//      }
//      }
//    }
//    public void draw(UGSDemoPanel p, Graphics2D g2, AffineTransform at) {
//      synchronized(p) {
//      if (hasBearing) {
//        AffineTransform bat = new AffineTransform();
//        bat.rotate(Math.toRadians(bearing), lon1, lat1);
//        // bat.concatenate(at);
//        Stroke initStroke=g2.getStroke();
//        Stroke dashStroke=new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, (new float[] { 10f }), 0 );
//        g2.setStroke(dashStroke);
//        p.draw(this, g2, bat);
//        g2.setStroke(initStroke);
//      }
//      }
//    }
//
//  }
  public String toString() {
    return " track "+id+" lons: "+lons+", lats: "+lats;
  }
}