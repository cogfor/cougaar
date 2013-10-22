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
import org.cougaar.microedition.asset.*;
import org.cougaar.microedition.shared.Constants;

/**
 * Represents Flashlight resources controlled by TINI boards.
 */
public class TiniFlashlight extends ControllerResource {

  private boolean isOnNow=false;
  private boolean wasOnThen = false;
  private boolean changecondition = false;
  private DS2450 ds2450;
  private int ds2450Index=0, ds2450Channel=3;
  private String owAddr;
  private int debugLevel = 0;

  private static final int FLASHLIGHT_OFF = 0;
  private static final int FLASHLIGHT_ON = 1;
  private static final int FLASHLIGHT_FLASHING = 2;
  private static final int FLASHLIGHT_TOGGLE = 3;
  private int flashlightstate = FLASHLIGHT_OFF;


  /**
   * Set parameters with values from my node and initialize TiniFlashlight.
   */
  public void setParameters(Hashtable t)
  {
    setName("TiniFlashlight");
    super.setParameters(t);
    if (t != null && t.containsKey("owaddr"))
      setAddr((String)t.get("owaddr"));

    /*
    if (owAddr==null) {
      System.err.println("TiniFlashlight requires a non-null owaddr from a parameter in the xml file.");
    } else {
      initDS2450(owAddr);
    }
    */
    initDS2450();


    //testing
    try
    {
      setOn(true);
      Thread.sleep(1000);
      setOn(false);
    }
    catch (Exception e) {}
  }

  /**
   * Set the address of the DS2450.
   */
  private void setAddr(String addr) { owAddr=addr; }


  /**
   * Read One Wire Bus to determine if flashlight is on.
   */
  public boolean isOn() {

    if (ds2450!=null) {
      if (debugLevel > 20) {
        System.out.println("TiniFlashlight.isOnNow calling "
          +"ds2450.readOutput(ds2450Index, ds2450Channel) with values:");
        System.out.println("ds2450.readOutput("+ds2450Index+", "+ds2450Channel+")");
      }
      isOnNow=!ds2450.readOutput(ds2450Index, ds2450Channel);  // output is false when light is on
      if (debugLevel > 20) System.out.println("TiniFlashlight.isOnNow Returned from ds2450.readOutput");
    } else {
      isOnNow=false;
	System.err.println("TiniFlashlight.isOnNow but ds2450 is null -- returning false ");
    }
    return isOnNow;
  }

  /**
    Attempts to set the Flashlight to the value indicated.
    @value true for on; false for off
    @return actual value after the method completes execution
  */
  public boolean setOn(boolean value)
  {
    //System.out.println("TiniFlashlight: setOn("+value+")");
    // setPinTo returns the value of the pin after the call (should be same as value)
    isOnNow=setPinTo(value);
    //System.out.println("TiniFlashlight: Leaving setOn("+value+") and flashlight isOn() returns "+isOn());

    if(isOnNow != wasOnThen)
      changecondition = true;

    return isOnNow;
  }

  /**
   * Constructor.
   */
  public TiniFlashlight() { }

  /**
   * Set one wire bus to the given value.
   * @return value read from the bus after attempting to set it.
   */
  private boolean  setPinTo(boolean value) {
    boolean outputenable=true;
    boolean pinValue=!value;  // set pin to false to turn light on
    if (ds2450!=null) {
      if (debugLevel > 30) {
        System.out.println("TiniFlashlight.setPinTo calling "
          +"ds2450.configureADOutput(ds2450Index, ds2450Channel, outputenable, value) with values:");
        System.out.println("ds2450.configureADOutput("+ds2450Index+", "+ds2450Channel+", "+outputenable+", "+pinValue+")");
      }

      ds2450.configureADOutput(ds2450Index, ds2450Channel, outputenable, pinValue);

      if (debugLevel > 30) System.out.println("Returned from ds2450.configureADOutput");
    } else {
      if (debugLevel > -20) System.out.println("TiniFlashlight.setPinTo("+value+") but ds2450 is null ");
    }
    return isOn();
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
      int adindex =ds2450Index;
      int adchan = ds2450Channel;
      boolean adoutputstate = true;  // true = not conducting to ground, logic 1

      if (debugLevel > 30) System.out.println("TiniFlashlight.initDS2450 Starting....");
      if (owAddr==null) {
        ds2450 = new DS2450();
      } else {
        ds2450 = new DS2450(getName(), owAddr);
      }
      if (debugLevel > 30) System.out.println("TiniFlashlight.initDS2450 A/Ds Initialized.");

      // set the output pin low
      boolean adoutputenable = false;
      ds2450.configureADOutput(adindex, adchan, adoutputenable, adoutputstate);
      if (debugLevel > 30) System.out.println("TiniFlashlight.initDS2450 "+ds2450.readDeviceName(adindex) + ", A/D " + adindex + ", channel " + adchan + " reconfigured: ");
      if (debugLevel > 30) System.out.println("TiniFlashlight.initDS2450 "+"adoutputenable: "+adoutputenable+" adoutputstate: "+adoutputstate);
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

  public void getValues(long [] values)
  {
    wasOnThen = isOnNow;
    values[0] = (long)(1.0*scalingFactor);
  }

  public void getValueAspects(int [] aspects)
  {
    aspects[0] = Constants.Aspects.FLASHLIGHT;
  }

  public int getNumberAspects()
  {
    return 1;
  }

  public void setChan(int c) {}
  public void setUnits(String u) {}
  public boolean conditionChanged()
  {
    if(flashlightstate == FLASHLIGHT_FLASHING)
      setOn(!isOn());
    if(flashlightstate == FLASHLIGHT_ON)
      setOn(true);
    if(flashlightstate == FLASHLIGHT_OFF)
      setOn(false);
    if(flashlightstate == FLASHLIGHT_TOGGLE)
    {
       if(isOn())
       {
	 setOn(false);
	 flashlightstate = FLASHLIGHT_OFF;
       }
       else
       {
	 setOn(true);
	 flashlightstate = FLASHLIGHT_ON;
       }
    }
    return false;
  }

  public void startControl()
  {
    switch(flashlightstate)
    {
      case FLASHLIGHT_ON:
      case FLASHLIGHT_FLASHING:
	   setOn(true);
	   break;
      case FLASHLIGHT_TOGGLE:
	   if(isOn())
	   {
	     setOn(false);
	     flashlightstate = FLASHLIGHT_OFF;
	   }
	   else
	   {
	     setOn(true);
	     flashlightstate = FLASHLIGHT_ON;
	   }
	   break;
      default:
           setOn(false);
    }
  }

  public void stopControl()
  {
    //setOn(false);
  }

  public boolean isUnderControl()
  {
    return false;
  }

  public boolean getSuccess() { return true; }

  public void modifyControl(String controlparameter, String controlparametervalue)
  {
    if(controlparameter.equalsIgnoreCase("LightingMode"))
    {
      if(controlparametervalue.equalsIgnoreCase("on"))
	flashlightstate = FLASHLIGHT_ON;
      else if(controlparametervalue.equalsIgnoreCase("flashing"))
	flashlightstate = FLASHLIGHT_FLASHING;
      else if(controlparametervalue.equalsIgnoreCase("toggle"))
	flashlightstate = FLASHLIGHT_TOGGLE;
      else
	flashlightstate = FLASHLIGHT_OFF;
    }
    System.out.println("TiniFlashlight state " +flashlightstate);
  }
}

