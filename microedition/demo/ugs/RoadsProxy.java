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

public class RoadsProxy {

//  double actualLength=18; //inches (about)
//  double actualWidth=6; // inches (about)
//  double length=actualLength;
//  double width=actualWidth*2;

  // --------
  String id=this.toString();
  Vector markers=new Vector();
  double[] lats;
  double[] lons;
  Color myColor=Color.cyan;
  // ---------

//  double heading=0;
//  double headingLineLength=length;
//  double bearingLineLength=length*100;


//  HeadingLine headingLine;
//  PictureAvailableRectangle pictureAvailableRectangle;

  public RoadsProxy(String theId, Vector theLats, Vector theLons) {
    System.out.println("RoadsProxy ctor : "+id);
    id=theId;
//    myColor=nextColor();
    update(theLats, theLons);
  }

//  Color[] PosibleColors = { Color.red, Color.blue, Color.yellow, Color.cyan,  };
//  static int curColor=0;
//  private Color nextColor() {
//    Color ret=PosibleColors[curColor];
//    curColor = ++curColor % PosibleColors.length;
//    return ret;
//  }

  // top-level
  public String getId() { return id; }
  public void  setId(String value) { id=value; }
//  private int centerX() { return (int) (lon + width/2.0) ;}
//  private int centerY() { return (int) (lat + length/2.0) ;}


  boolean roadDisplay=true;
  public void setRoadDisplay(boolean value)  {
    roadDisplay=value;
  }
  public boolean getRoadDisplay() { return roadDisplay; }
  public void toggleRoadDisplay() { roadDisplay=!roadDisplay; }


  Stroke myStroke=new BasicStroke(5);
  /**
   * Newer better draw which delegates tot he UGSDemoPanel
   */
  public void draw(UGSDemoPanel p, Graphics2D g2) {
    synchronized(p) {


    /* Draw rectangle at robot location.  Fill the rectangle if an image
     * is available.
     */
    if (getRoadDisplay()) {
      int entries=lats.length;
      Color origColor=g2.getColor();
      g2.setColor(myColor);
      Stroke origStroke = g2.getStroke();
      g2.setStroke(myStroke);
      p.drawPolyLine(g2, lats, lons, entries, false);
      g2.setStroke(origStroke);
//      p.drawString(g2, lats[entries-1], lons[entries-1], 0, (int)length, heading, getId());

//      for (Iterator miter=markers.iterator(); miter.hasNext(); ) {
//        ((CircleMarker)miter.next()).draw(p, g2);
//      }
      g2.setColor(origColor);
    }

    }
  }

  // HeadingLine pass-thrus
//  private double headingX() { return headingLine.headingX() ;}
//  private double headingY() { return headingLine.headingY();}

  // update methods
  void update(Vector theLats, Vector theLons) {
    System.out.println("road update : "+id+", "+theLats+", "+theLons);
    Iterator latIter=theLats.iterator();
    Iterator lonIter=theLons.iterator();
//    markers.clear();
    lats=new double[theLats.size()];
    lons=new double[theLons.size()];
    for (int idx=0; latIter.hasNext() && lonIter.hasNext(); idx++) {
      lats[idx]=((java.lang.Double)latIter.next()).doubleValue();
      lons[idx]=((java.lang.Double)lonIter.next()).doubleValue();
//      markers.add(new CircleMarker(lons[idx], lats[idx], width, width));
    }
    if (latIter.hasNext()||lonIter.hasNext()) {
      System.out.println("Warning: latIter and lonIter should have same number of items "+theLats.size()+", "+theLons.size());
    }
    System.out.print("."); System.out.flush();
  }


  public String toString() {
    return " road "+id+" lons: "+lons+", lats: "+lats;
  }
}