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
import com.ibutton.adapter.*;
import com.ibutton.container.*;

/**
 * create a Tini Lightmeter.
 */
public class TiniLightmeter extends LightmeterResource {

  static int lightChan = 3;

  public TiniLightmeter() {}

  public void setChan(int c) {
    chan = lightChan;
    if (c != lightChan)
      throw new IllegalArgumentException(getName() + ": bad chan: " + c + "; using: " + lightChan);
  }

  public void setUnits(String u) {}

  public long getValue() {
    byte[]   state;
    double range = (-999.*2.);
    TINIExternalAdapter adapter = new TINIExternalAdapter();
    double curVoltage = -999.;
    try {
      adapter.beginExclusive(true);
      adapter.targetFamily(0x20);
//      adapter.setSearchAlliButtons();
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
      adapter.endExclusive();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
      adapter.endExclusive();
    }

    System.out.println(getName() + " Reading: Ch" + getChan() + " = " + (range-curVoltage) + " V");
    long scaledValue = (long)((range-curVoltage) * (double)getScalingFactor());
    return scaledValue;
  }
}
