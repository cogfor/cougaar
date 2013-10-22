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
 * create a Tini Thermometer.
 */
public class TiniTest extends ResourceAdapter {

  static String[] Units = { "1", "-1", "" };

  String units = "";

  public TiniTest() {}

  public void setChan(int c) {}

  public void setUnits(String u) {
    units = Units[0];
    boolean ok = false;
    for (int i=0; Units[i] != ""; i++)
      if (u.equals(Units[i]))
        ok = true;
    if (!ok)
      throw new IllegalArgumentException("bad units: " + u + "; using " + Units[0]);
    units = u;
  }

  public String getUnits() { return units; }

  public long getValue() {
    double dval = (double)-999.999;
    String unit = getUnits();
    try {
//      adapter.setSearchAlliButtons();
      if (unit.equals("1"))
        dval = (double)1.;
      else
        dval = (double)-1.;
      System.out.println(unit + " "+ getName() + " Reading: " + dval);
    } catch (Exception e) {
      System.out.println("caught exception: " + e);
    }
    return (long)dval;
  }

}
