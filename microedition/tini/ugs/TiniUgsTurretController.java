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
package org.cougaar.microedition.tini.ugs;

import java.lang.Thread;

import com.dalsemi.system.*;
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.Constants;

/**
 * Asset for turret control.
 */

public class TiniUgsTurretController extends ControllerResource
{
  private static final double DELTADEG = 3.6; //degrees
  private double bearing = 0.0;
  private double scanspeed = 0.0;
  private double startbearing = 0.0;
  private double stopbearing = 0.0;

  private BitPort in1; // TX1 (12) Input #1
  private BitPort in2; // CTX (8) Input #2
  private boolean debugging = false;
  private int currentIn1State = -1;
  private int currentIn2State = -1;

  public TiniUgsTurretController()
  {
    //DON'T DO ANYTHING HERE. USE SET PARAMETERS TO INITIALIZE
  }

  public void setParameters(java.util.Hashtable t)
  {

    setName("TiniTurretController");
    in1 = new BitPort(BitPort.Port5Bit3); // TX1 (12) Input #1
    in2 = new BitPort(BitPort.Port5Bit0); // CTX (8) Input #2
  }

  /**
   * Returns true if the state of the controller was changed.
   */
  // control1 = direction
  // control2 = step on falling edge
  private boolean setControlBits(int control1, int control2)
  {
    // Don't set bits if they are already set right
    if ((currentIn1State == control1) && (currentIn2State == control2))
    {
      return false;
    }
    if (debugging) System.out.println("Setting control bits: "+control1+":"+control2);
    if (control1 == 0)
      in1.clear();
    else
      in1.set();
    if (control2 == 0)
      in2.clear();
    else
      in2.set();
    currentIn1State = control1;
    currentIn2State = control2;
    return true;
  }

  public void getValues(long [] values)
  {
    values[0] = (long)(scalingFactor*bearing);
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

  public boolean conditionChanged()
  {
    return true;
  }

  private boolean isundercontrol = false;

  public void startControl()
  {
    Thread stepmotorthread = new Thread(new StepMotorControl());
    stepmotorthread.start();
    isundercontrol = true;
  }

  public void stopControl()
  {
    endmotorcontrolthread = true;
    isundercontrol = false;
  }

  public boolean isUnderControl()
  {
    return isundercontrol;
  }

  public void modifyControl(String controlparameter, String controlparametervalue)
  {
    try
    {
      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.VELOCITYPREP]))
      {
	Double temp = new Double(controlparametervalue);
	scanspeed = temp.doubleValue();
	System.out.println("TiniTurretController: scan speed: " +scanspeed+" deg per sec");
      }

      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.STARTANGLEPREP]))
      {
	Double temp = new Double(controlparametervalue);
	startbearing = (long)temp.doubleValue();
	System.out.println("TiniTurretController: start bearing: " +startbearing+" degrees");
      }
      if(controlparameter.equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.STOPANGLEPREP]))
      {
	Double temp = new Double(controlparametervalue);
	stopbearing = (long)temp.doubleValue();
	System.out.println("TiniTurretController: stop bearing: " +stopbearing +" degrees");
      }
    }
    catch (Exception ex) {}
  }

  private boolean endmotorcontrolthread = false;
  private static final int DIRECTION_CLOCKWISE = 0;
  private static final int DIRECTION_COUNTERCLOCKWISE = 1;

  class StepMotorControl implements Runnable
  {

    public void run()
    {
      if(debugging) System.out.println("StepMotorControl: thread running");

      int direction = DIRECTION_CLOCKWISE; //start clockwise

      setControlBits(1, 1); //prep

      while (true)
      {
	setControlBits(direction, 0); //move an increment
	setControlBits(direction, 1);

	bearing += ((1 - 2*direction) * DELTADEG);

	//System.out.println("Bearing Value : " +bearing);

	if(bearing >= stopbearing)
	{
	  System.out.println("Bearing Value : " +bearing);
	  direction = DIRECTION_COUNTERCLOCKWISE;
	}
	if(bearing <= startbearing)
	{
	  System.out.println("Bearing Value : " +bearing);
	  direction = DIRECTION_CLOCKWISE;
	}

        try
	{
	  //approximate speed
	  //msecs = 1000 msec/sec * sec/command
	  long msecs = (long)((1000.0)/(Math.abs(scanspeed)/DELTADEG));
	  if(msecs > 0)
	    Thread.sleep(msecs);
	}

	catch (Exception ie)
	{
	  System.err.println("ControlPlugin Thread: Exception caught " +ie);
	  ie.printStackTrace();
	}

	//stop control of resource if I'm still associated with it.
	if(endmotorcontrolthread == true)
	{
	  break;
	}
      } //end while
    }
  }
}