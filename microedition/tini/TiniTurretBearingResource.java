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

//this class keeps track of where the robot was in terms of
//relative coordinates the last time the command state changed


public class TiniTurretBearingResource extends ControllerResource
{

  /**
   * Constructor.  Sets name default.
   */
  public TiniTurretBearingResource() {  }

  private double fixedturretbearing = 0.0;

  public void setParameters(Hashtable params)
  {
    setName("TiniTurretBearingResource");
  }

  public void getValues(long [] values)
  {
    values[0] = (long)(scalingFactor*fixedturretbearing);
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.BEARING;
  }

  public int getNumberAspects()
  {
    return 1;
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
    if(controlparameter.equalsIgnoreCase("bearing"))
    {
      Double temp = new Double(controlparametervalue);
      fixedturretbearing = temp.doubleValue();
      System.out.println("TiniTurretBearingResource: modifyControl bearing value: " +fixedturretbearing);
    }

    if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.TURRETDIRECTIONPREP]))
    {
      if(controlparametervalue.equalsIgnoreCase(Constants.Robot.SEARCHRIGHT))
	System.out.println("TiniTurretBearingResource: modifyControl set RIGHT hemisphere");
      else if(controlparametervalue.equalsIgnoreCase(Constants.Robot.SEARCHLEFT))
	System.out.println("TiniTurretBearingResource: modifyControl set LEFT hemisphere");
      else
        System.out.println("TiniTurretBearingResource: modifyControl set MIDDLE hemisphere");
    }
  }
}