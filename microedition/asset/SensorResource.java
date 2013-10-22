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

import java.util.*;

/**
 * Base class for all SensorResources.
 */
public abstract class SensorResource extends ResourceAdapter {

  protected int chan = 0;
  protected String units = "";
  protected int scalingFactor = 1000;

  public abstract void setChan(int c);
  public abstract void setUnits(String u);
  public abstract long getValue();

  public SensorResource() {}

  public void init() {
    try {
      Hashtable t = getParameters();
      if (t == null)
        return;
      if (t.containsKey("units"))
        setUnits((String)t.get("units"));
      if (t.containsKey("chan"))
        setChan(Integer.parseInt((String)t.get("chan")));
    } catch (Exception e) {System.out.println("caught " + e);}
  }

  public int getChan() {
    return chan;
  }

  public String getUnits() {
    return units;
  }

  public long getScalingFactor() {
    return scalingFactor;
  }

}
