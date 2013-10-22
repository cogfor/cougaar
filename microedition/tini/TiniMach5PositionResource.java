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

package org.cougaar.microedition.tini;

import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.Constants;
import java.util.*;
import java.io.*;
import java.lang.*;
import javax.comm.*;


/*

This structure keeps track of where the Mach 5 is in a relative coordinate frame.
It assumes that the relative coordinate frame is defined by the robots position and
orientation in a absolute coordinate frame at start up. For example, the robotbase
will start at Xrel = Yrel = 0 and ThetaRel = 90.0

*/


public class TiniMach5PositionResource extends ControllerResource implements LocationResource {

  public TiniMach5PositionResource()
  {

  }

  private TiniMach5LocomotionResource mach5resource = null;

  public void setLocomotionResource(TiniMach5LocomotionResource res)
  {
    mach5resource = res;
  }

  private boolean debug = false;

  //these variables define the position, orientation of the relative coordinate
  //frame (of the robot) with respect to an absolute from (geographic)
  private double surveyheading = 0.0;
  private double surveylatitude = 0.0;
  private double surveylongitude = 0.0;
  private double shsin = 0.0;
  private double shcos = 1.0;
  private double [] poscoord = new double[3];

  /**
   *  The only paramater is "port" which defaults to "serial1"
   */
  public void setParameters(Hashtable params)
  {
    setName("TiniMach5PositionResource");

    setScalingFactor((long)Constants.Geophysical.DEGTOBILLIONTHS);

    if (params != null)
    {
      debug = (params.get("debug") != null);

      if (params.get("SurveyLatitude") != null)
      {
	String pstr = (String)params.get("SurveyLatitude");
        Double temp = new Double(pstr);
        surveylatitude = temp.doubleValue();
	surveylatitude *= (Math.PI/180.0); //radians
      }
      if (params.get("SurveyLongitude") != null)
      {
	String pstr = (String)params.get("SurveyLongitude");
        Double temp = new Double(pstr);
        surveylongitude = temp.doubleValue();
	surveylongitude *= (Math.PI/180.0); //radians
      }
      if (params.get("SurveyHeading") != null)
      {
	String pstr = (String)params.get("SurveyHeading");
        Double temp = new Double(pstr);
        surveyheading = temp.doubleValue();
	shcos = TiniTrig.tinicos((-1.0*Math.PI/180)*surveyheading);
	shsin = TiniTrig.tinisin((-1.0*Math.PI/180)*surveyheading);
      }
    }
  }

  public void getCoordinates( double [] coord)
  {
    if(mach5resource == null) return;

    //read the wheels to tell me where you are at
    //use the same command to keep it going
    Mach5RelativePositionData upd = mach5resource.ReadCurrentState();

    //rotate the coordinate frame to align with absolute coordinate frame
    double xpp = upd.Xrel*shcos - upd.Yrel*shsin;
    double ypp = upd.Xrel*shsin + upd.Yrel*shcos;

    xpp = xpp/1000.0;
    ypp = ypp/1000.0; //meters

    double lat = surveylatitude + ypp/Constants.Geophysical.EARTH_RADIUS_METERS;
    double lon = surveylongitude + xpp / (Constants.Geophysical.EARTH_RADIUS_METERS * TiniTrig.tinicos(0.5*(surveylatitude + lat)));

    lat *= (180.0/Math.PI); //convert to degrees
    lon *= (180.0/Math.PI); //convert to degrees

    double h = (Math.PI/2.0) - upd.Thetarel;
    h *= (180.0/Math.PI);

    //adjust by heading of coordinate frame
    h += surveyheading;
    if(h < 0.0) h += 360.0;
    if(h >= 360.0) h -=360.0;

    coord[0] = lat;
    coord[1] = lon;
    coord[2] = h;

  }

  public double getLatitude()
  {
    if(mach5resource == null)
      return surveylatitude;

    //read the wheels to tell me where you are at
    //use the same command to keep it going
    Mach5RelativePositionData upd = mach5resource.ReadCurrentState();

    //rotate the coordinate frame to align with absolute coordinate frame
    double xpp = upd.Xrel*shcos - upd.Yrel*shsin;
    double ypp = upd.Xrel*shsin + upd.Yrel*shcos;

    xpp = xpp/1000.0;
    ypp = ypp/1000.0; //meters

    double lat = surveylatitude + ypp/Constants.Geophysical.EARTH_RADIUS_METERS;
    lat *= (180.0/Math.PI); //convert to degrees

    return lat;
  }

  public double getLongitude()
  {
    if(mach5resource == null)
      return surveylongitude;

    //read the wheels to tell me where you are at
    //use the same command to keep it going
    Mach5RelativePositionData upd = mach5resource.ReadCurrentState();

    //rotate the coordinate frame to align with absolute coordinate frame
    double xpp = upd.Xrel*shcos - upd.Yrel*shsin;
    double ypp = upd.Xrel*shsin + upd.Yrel*shcos;

    xpp = xpp/1000.0;
    ypp = ypp/1000.0; //meters

    double lat = surveylatitude + ypp/Constants.Geophysical.EARTH_RADIUS_METERS;
    double lon = surveylongitude + xpp / (Constants.Geophysical.EARTH_RADIUS_METERS * TiniTrig.tinicos(0.5*(surveylatitude + lat)));

    lon *= (180.0/Math.PI); //convert to degrees

    return lon;
  }

  public double getAltitude()
  {
    return 0.0;
  }

  public double getHeading()
  {
    if(mach5resource == null)
      return surveyheading;

    Mach5RelativePositionData upd = mach5resource.ReadCurrentState();

    //convert h math coordinates to heading coordinates
    double h = (Math.PI/2.0) - upd.Thetarel;
    h *= (180.0/Math.PI);

    //adjust by heading of coordinate frame
    h += surveyheading;
    if(h < 0.0) h += 360.0;
    if(h >= 360.0) h -=360.0;

    return h;
  }

  public Date getDate()
  {
    return new Date();
  }

  public void getValues(long [] values)
  {
    getCoordinates(poscoord);
    values[0] = (long)(scalingFactor*poscoord[0]);
    values[1] = (long)(scalingFactor*poscoord[1]);
    values[2] = (long)(scalingFactor*poscoord[2]);
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.LATITUDE;
    aspects[1] = Constants.Aspects.LONGITUDE;
    aspects[2] = Constants.Aspects.HEADING;
  }

  public int getNumberAspects()
  {
    return 3;
  }

  public void setChan(int c) {}
  public void setUnits(String u) {}
  public boolean conditionChanged() {return true;} //always report heading

  private boolean isundercontrol = false;

  public void startControl()
  {
    isundercontrol = true;
  }

  public void stopControl()
  {
    isundercontrol = false;
  }

  public boolean isUnderControl()
  {
    return isundercontrol;
  }

  public void modifyControl(String controlparameter, String controlparametervalue)
  {

  }
}