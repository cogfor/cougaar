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

public class UGSProxy {

  double actualLength=18; //inches (about)
  double actualWidth=6; // inches (about)
  //double length=actualLength*2;
  double length=actualLength/2;
  double width=actualWidth*2;

  // --------
  String id=this.toString();
  double lon=20, lat=20;
  Vector bearingLines=new Vector();
  Vector bearings=new Vector();
  Vector detTimes=new Vector();
  // ---------

  double heading=0;
  double headingLineLength=length;
  double bearingLineLength=length*200;

  String status;
  HeadingLine headingLine;
//  LightCircle lightCircle;
  PictureAvailableRectangle pictureAvailableRectangle;
//  BearingLine bearingLine=new BearingLine();

  public double getLat() {return lat; }
  public double getLon() {return lon; }

  public UGSProxy(String id, double lon, double lat) {
    this.id=id;
    headingLine=new HeadingLine(lon, lat, headingLineLength); // move down after all other updates are upgraded
    updateLocation(lon, lat);
  }

  public UGSProxy(String theId, double lat, double lon, Vector theBearings, Vector theDetTimes, String theStatus) {
//    System.out.println("RPctor : "+id+", "+lon+", "+lat+", b:"+theBearings+", dt:"+theDetTimes);
    System.out.print("n "+id+" "); System.out.flush();
    id=theId;
    bearings=theBearings;
    detTimes=theDetTimes;
    headingLine=new HeadingLine(lon, lat, headingLineLength);
    updateLocation(lon, lat);
    pictureAvailableRectangle.setPictureAvailable(false);
    status=theStatus;
  }

  boolean isPictureAvailable() { return pictureAvailableRectangle.isPictureAvailable(); }
  void setPictureAvailable(boolean value) { pictureAvailableRectangle.setPictureAvailable(value); }


  // top-level
  public String getId() { return id; }
  public void  setId(String value) { id=value; }
  private int centerX() { return (int) (lon + width/2.0) ;}
  private int centerY() { return (int) (lat + length/2.0) ;}


  boolean bearingDisplay=true;
  public void setBearingsDisplay(boolean value)  {
    bearingDisplay=value;
  }
  public boolean getBearingsDisplay() { return bearingDisplay; }
  public void toggleBearingsDisplay() { bearingDisplay=!bearingDisplay; }

  /**
   * Newer better draw which delegates tot he UGSDemoPanel
   */
  public void draw(UGSDemoPanel p, Graphics2D g2) {
    synchronized(p) {


    /* Draw rectangle at robot location.  Fill the rectangle if an image
     * is available.
     */
     // System.out.println("UGSProxy.draw with status of "+status);
     boolean active=false;
    if (status!=null && status.indexOf("REP_TAR_TASK_ON")>-1) active=true;

     boolean detections=false;
    if (status!=null && status.indexOf("TAR_AR_RCVD_ON")>-1) detections=true;

    String sensorLabel=getId()+((active)?" [Active]":" [INACTIVE]");

    p.drawRect(g2, lat, lon, (int)width, (int)length, heading, detections);
    p.drawString(g2, lat, lon, 0, (int)length, heading, sensorLabel);

//    // Draw heading line
//    p.drawLine(g2, lat, lon, (int)headingLineLength, heading);
//    // draw flashlight circle
//    lightCircle.draw(p, g2);

    if (getBearingsDisplay()) {
      for (Iterator bit=bearingLines.iterator(); bit.hasNext();) {
        ((BearingLine)bit.next()).draw(p, g2);
      }
//    bearingLine.draw(p, g2);
    }

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
  void update(double lat, double lon, Vector theBearings, Vector theDetTimes, String newStatus) {
//    System.out.println("RPctor : "+id+", "+lon+", "+lat+", b:"+theBearings+", dt:"+theDetTimes);
    bearings=theBearings;
    detTimes=theDetTimes;
    System.out.print("u"); System.out.flush();
    updateLocation(lon,lat);
    status=newStatus;

//    setLightOn(false);
//    pictureAvailableRectangle.setPictureAvailable(false);
  }
  void updateLocation(double lon, double lat) {
    this.lon=lon;
    this.lat=lat;
    updateBearingLines();
    updateHeadingLine();
//    updateLightCircle();
    updatePictureAvailableRectangle();
  }
  private void updateHeadingLine() {
    if (headingLine!=null)
      headingLine.setLine(centerX(), centerY());
  }
  private void updateBearingLines() {
    bearingLines.clear();
    if (bearings!=null) {
      for(Iterator bit=bearings.iterator(); bit.hasNext();){
        bearingLines.add(new BearingLine(lon, lat,
          ((java.lang.Double)bit.next()).doubleValue(), bearingLineLength));
      }
    }
  }
//  private void updateLightCircle() {
//    double lcWidth=width;
//    double lcLength=width;
//    if (lightCircle==null) {
//      lightCircle=new LightCircle(lon, lat, lcWidth, lcLength);
//    } else {
//      lightCircle.updateLocation(lon,lat);
//    }
//  }
  private void updatePictureAvailableRectangle() {
    pictureAvailableRectangle=new PictureAvailableRectangle(lon, lat, width, length);
  }

  public void stepForward(int dist) {
     // System.out.println("in stepForward "+this);
    updateLocation(lon, lat - dist);
  }
  public void setHeading(double degrees) {
    heading=degrees;
  }
//  public  boolean isLightOn() { return (lightCircle !=null && lightCircle.isLightOn()); }
//  public  void setLightOn(boolean value) { lightCircle.setLightOn(value); }


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

  class BearingLine extends Line2D.Double {
    double lon1, lat1, blon, blat;
    boolean hasBearing=false;
    double bearing=45;
    double bearingLineLength;
    BearingLine() { }
    BearingLine(double lon, double lat, double bearing, double length) {
        bearingLineLength=length;
        lon1=lon;
        lat1=lat;
        this.bearing=bearing;
        super.setLine(lon,  lat,  lon,  lat-bearingLineLength);
        Line2D.Double tmpLine=new Line2D.Double(lon,  lat,  lon,  lat-bearingLineLength);
        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(bearing), lon, lat);
        txShape = at.createTransformedShape(tmpLine);
        Rectangle2D b2d = at.createTransformedShape(tmpLine).getBounds2D();
        blon=b2d.getMinX();
        blat=b2d.getMinY();
        hasBearing=true;

    }
  Shape txShape=null;
    double bearingX() { return blon ;}
    double bearingY() { return blat ;}
    double getBearing() { return bearing ;}
    void draw(Graphics2D g2, AffineTransform at) {
      if (hasBearing) {
        AffineTransform bat = new AffineTransform();
        bat.rotate(Math.toRadians(bearing), lon1, lat1);

        Stroke initStroke=g2.getStroke();
        Stroke dashStroke=new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, (new float[] { 10f }), 0 );
        g2.setStroke(dashStroke);

        g2.draw(bat.createTransformedShape(this));

        g2.setStroke(initStroke);

      }
    }

    public void clearBearing() { hasBearing=false; }
    /**
     * Newer better draw which delegates tot he UGSDemoPanel
     */
    public void draw(UGSDemoPanel p, Graphics2D g2) {
      synchronized(p) {
      if (hasBearing) {
        p.drawDashedLine(g2, lat, lon, (int)bearingLineLength, bearing);
      }
      }
    }
    public void draw(UGSDemoPanel p, Graphics2D g2, AffineTransform at) {
      synchronized(p) {
      if (hasBearing) {
        AffineTransform bat = new AffineTransform();
        bat.rotate(Math.toRadians(bearing), lon1, lat1);
        // bat.concatenate(at);
        Stroke initStroke=g2.getStroke();
        Stroke dashStroke=new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, (new float[] { 10f }), 0 );
        g2.setStroke(dashStroke);
        p.draw(this, g2, bat);
        g2.setStroke(initStroke);
      }
      }
    }

  }
  public String toString() {
    return " ugs "+id+" lon: "+lon+", lat: "+lat;
  }
}