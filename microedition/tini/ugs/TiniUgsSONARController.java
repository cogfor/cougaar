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

import org.cougaar.microedition.tini.DS2450;
import java.util.*;
import org.cougaar.microedition.asset.*;
import com.ibutton.adapter.*;
import com.ibutton.container.*;
import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.plugin.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.shared.Constants;

public class TiniUgsSONARController extends ControllerResource {

  static int sonarIdx = 0 ;
  static int sonarChan = 0 ;
  static int debugLevel=10;

  private double sensorthreshold = 2.0; //average
  private double maxave = 0.0;
  private DS2450 ds2450=null;

  int position=0;
  int historySize=10;
  double [] historyQueue = null;
  boolean lookforpeak = false;
  boolean upswingseen = false;

  public void modifyControl(String controlparameter, String controlparametervalue)
  {
    if(controlparameter.equalsIgnoreCase("threshold"))
    {
      Double temp = new Double(controlparametervalue);
      sensorthreshold = temp.doubleValue();
      System.out.println("TiniSONARController: threshold set: " +sensorthreshold);
    }
  }

  private boolean isStarted = false;
  public boolean isUnderControl()
  {
    return isStarted;
  }

  public void startControl()
  {
    for (int idx=0; idx<historySize; idx++)
    {
        historyQueue[idx] = 0.0;
    }
    isStarted = true;
  }

  public void stopControl()
  {
    isStarted = false;
  }

  public void getValues(long [] values)
  {
    values[0] = (long)(scalingFactor*1);
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.DETECTION;
  }

  public int getNumberAspects()
  {
    return 1;
  }

  public boolean getSuccess()
  {
    return true;
  }

  public boolean conditionChanged() {
    return conditionChangedNoMemory();
  }

  public boolean conditionChangedNoMemory()
  {
      double value=getDS2450Value();
      boolean thisTime = (value > sensorthreshold);
      System.out.println("conditionChangedNoMem-- ds2450Value "+value+ " "+thisTime+" "+sensorthreshold);
      return thisTime;
  }


  /**
   * Constructor.  Sets name default.
   */
  public TiniUgsSONARController() {}

  /**
   * Set parameters with values from my node and initialize resource.
   */
  public void setParameters(java.util.Hashtable t) {
    setName("TiniSONARController");
    historyQueue=new double[historySize];
    for (int idx=0; idx<historySize; idx++)
    {
        historyQueue[idx] = 0.0;
    }
    initDS2450();

  }


/**
 * Initialize DS2450 object for reading and setting items on the one wire bus.
 * Note that DS2450.adoutputenable must be false (as it is by default
 * in the DS2450 object).
 **/
  private void initDS2450() {
    initDS2450(null);
  }

/**
 * Initialize DS2450 object for reading and setting items on the one wire bus.
 * Note that DS2450.adoutputenable must be false (as it is by default
 * in the DS2450 object).
 * @param owAddr initialize the item at the given address;  if the given address
 *               is null, then initialize each of the devices found
 **/
  private void initDS2450(String owAddr) {
    try
    {
      int adindex =sonarIdx;
      int adchan = sonarChan;
      boolean adoutputstate = true;  // true = not conducting to ground, logic 1

      if (debugLevel > 30) System.out.println("TiniSONARController.initDS2450 Starting....");
      if (owAddr==null) {
        ds2450 = new DS2450();
      } else {
        ds2450 = new DS2450(getName(), owAddr);
      }
      if (debugLevel > 30) System.out.println("TiniSONARController.initDS2450 A/Ds Initialized.");

      // set the output pin low
      boolean adoutputenable = false;
      ds2450.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugLevel > 30) System.out.println("TiniSONARController.initDS2450 "+ds2450.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured: ");
      if (debugLevel > 30) System.out.println("TiniSONARController.initDS2450 "+"adoutputenable: "+adoutputenable+" adoutputstate: "+adoutputstate);
    }
    catch (Exception ex)
    {
      if (debugLevel > -20) System.err.println("initDS2450 caught Exception: "+ex);
     ex.printStackTrace();
    }
    catch (Throwable t)
    {
      if (debugLevel > -20) System.err.println("initDS2450 caught Throwable: "+t);
    }
  }

  /**
   * Set the DS2450 channel to be used.  This must be consistent with the wiring
   * of the hardware.
   */
  public void setChan(int c) {
    chan = sonarChan = c;
    System.out.println("TiniSONARController: setting channel to "+sonarChan);
    if (c != sonarChan)
      throw new IllegalArgumentException(getName() + ": bad chan: " + c + "; using: " + sonarChan);
  }

  public void setUnits(String u) {}

  /**
   * A prior implementation of a method to read the voltage from the DS2450 device.
   * Probably obsolete now.
   */
  public double getLMValue() {
    byte[]   state;
    double range = (-999.*2.);
    TINIExternalAdapter adapter = new TINIExternalAdapter();
    double curVoltage = -999.;
    try {
      adapter.beginExclusive(true);
      adapter.targetFamily(0x20);
      iButtonContainer20 aD = (iButtonContainer20)adapter.getFirstiButton();

      if (aD != null) {
        state = aD.readDevice();
        range = aD.getADRange(getChan(), state);
        if (range < 5.0) {
          aD.setADRange(getChan(), 5.12, state);
          aD.writeDevice(state);
        }
        aD.doADConvert(getChan(), state);
        curVoltage = aD.getADVoltage(getChan(), state);
      }
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    } finally {
      adapter.endExclusive();
    }

    System.out.println(getName() + " Reading: Ch" + getChan() + " = " + (range-curVoltage) + " V");
    return range-curVoltage;
  }

  /**
   * Read voltage from DS2450 device using the DS2450 object.
   */
  public double getDS2450Value() {
    if (debugLevel > 30) System.out.println("TiniSONARController.getDS2450Value and ds2450 is "+ds2450);
    return (ds2450==null) ? 0.0 : ds2450.readVoltage(sonarIdx, sonarChan);
  }
}
