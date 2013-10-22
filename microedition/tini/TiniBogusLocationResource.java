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

import java.util.*;
import java.io.*;
import java.lang.*;
import javax.comm.*;
import java.text.*;

import org.cougaar.microedition.asset.LocationResource;
import org.cougaar.microedition.asset.ControllerResource;
import org.cougaar.microedition.shared.Constants;


public class TiniBogusLocationResource extends ControllerResource implements LocationResource
{

  private double longitude = 0.0;
  private double latitude = 0.0;
  private double heading = 0.0;
  private double altitude = 0.0;
  private long msecstime = 0;
  private long tincrement = 1000;
  private String myName = "";
  private Hashtable attrtable = null;

  /**
   * Constructor.  Sets name default.
   */
  public TiniBogusLocationResource() {}

  public void setName(String n) {
    myName = n;
  }

  public String getName() {
    return myName;
  }

  public Hashtable getParameters()
  {
    return attrtable;
  }

  public void setParameters(Hashtable params)
  {
    setName("TiniBogusLocationResource");
    setScalingFactor((long)Constants.Geophysical.DEGTOBILLIONTHS);
    attrtable = params;
    if (params != null)
    {
      if (params.get("Latitude") != null)
      {
	String pstr = (String)params.get("Latitude");
        Double temp = new Double(pstr);
        latitude = temp.doubleValue();
      }
      if (params.get("Longitude") != null)
      {
	String pstr = (String)params.get("Longitude");
        Double temp = new Double(pstr);
        longitude = temp.doubleValue();
      }
      if (params.get("Heading") != null)
      {
	String pstr = (String)params.get("Heading");
        Double temp = new Double(pstr);
        heading = temp.doubleValue();
      }

      if (params.get("Altitude") != null)
      {
	String pstr = (String)params.get("Altitude");
        Double temp = new Double(pstr);
        altitude = temp.doubleValue();
      }
    }
  }


  public void getValues(long [] values)
  {
    values[0] = (long)(scalingFactor*getLatitude());
    values[1] = (long)(scalingFactor*getLongitude());
    values[2] = (long)(scalingFactor*getHeading());

    values[3] = System.currentTimeMillis();
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.LATITUDE;
    aspects[1] = Constants.Aspects.LONGITUDE;
    aspects[2] = Constants.Aspects.HEADING;
    aspects[3] = Constants.Aspects.TIME;
  }

  public int getNumberAspects()
  {
    return 4;
  }

  public void setChan(int c) {}
  public void setUnits(String u) {}
  public boolean conditionChanged() {return true;}

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
    if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.ROTATEPREP]))
    {
      Double temp = new Double(controlparametervalue);
      double rotationdegrees = (long)temp.doubleValue();
      System.out.println("TiniBogusLocationResource: augment heading: " +rotationdegrees +" degrees");
      adjustHeading(rotationdegrees);
    }
  }

  public boolean getSuccess()
  {
    return true;
  }

  public double getLatitude()
  {
    return latitude;
  }

  public double getLongitude()
  {
    return longitude;
  }

  public double getAltitude()
  {
    return altitude;
  }

  public double getHeading()
  {

    return heading;
  }

  public void adjustHeading(double deg)
  {
    heading += deg;
    if(heading > 180.0) heading -= 360.0;
    if(heading < -180.0) heading += 360.0;
  }

  public Date getDate()
  {
    return new Date();
  }
}