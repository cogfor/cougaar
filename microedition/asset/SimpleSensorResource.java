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
package org.cougaar.microedition.asset;

public class SimpleSensorResource extends SensorResource
{
  private long sensorvalue;
  private boolean sensorvalueinit = false;

  public void setValue(long value)
  {
    sensorvalue = value;
    sensorvalueinit = true;
    if (debugging) System.out.println("Sensor "+getName()+" set to "+value);
  }

  public long getValue()
  {
    return sensorvalue;
  }
  public void setChan(int c)
  {
    chan = c;
  }

  public void setUnits(String u)
  {
    units = u;
  }

  public boolean isValueInitialized()
  {
    return sensorvalueinit;
  }

  public SimpleSensorResource(String ssname, String unitsname)
  {
    System.out.println("SimpleSensorResource "+ssname+" created.");

    setName(ssname);
    units = unitsname;
  }

  public void setDebugging(boolean on) {
    debugging = on;
  }
}
