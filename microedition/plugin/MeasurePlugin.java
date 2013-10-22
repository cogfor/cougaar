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
package org.cougaar.microedition.plugin;

import java.io.*;
import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;

/**
 * Generic Plugin for readings.
 */
public class MeasurePlugin extends PluginAdapter {

  UnaryPredicate getPred(final String prepPhrase) {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          if (mt.getVerb().equals("Measure")) {
            MicroPrepositionalPhrase mpp = findPreposition(mt, prepPhrase);
            return mpp != null;
          }
        }
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getResourcePred(final String prepPhrase) {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof SensorResource) {
          SensorResource sr = (SensorResource)o;
          return (prepPhrase.indexOf(sr.getName()) >= 0);
        }
        return false;
      }
    };
    return resourcePred;
  }

  int sleepTime = 5000;
  String prepPhrase = "Generic";
  String units = "";
  int chan = 0;
  SensorResource mySensorResource = null;
  Subscription sub;
  Subscription resourceSub;
  int counter = 1;
  boolean iShouldDie = false;

  public MeasurePlugin() {}

  public void setupSubscriptions() {
System.out.println("MeasurePlugin::setupSubscriptions");
    if (getParameters() != null) {
      Hashtable t = getParameters();
      System.out.println(prepPhrase+"Plugin: setupSubscriptions " + t);
      if (t.containsKey("sleeptime"))
        sleepTime = Integer.parseInt((String)t.get("sleeptime"));
      if (t.containsKey("name"))
        prepPhrase = (String)t.get("name");
      if (t.containsKey("chan"))
        chan = Integer.parseInt((String)t.get("chan"));
    }
    else {
      System.out.println(prepPhrase+"Plugin: setupSubscriptions No Params");
    }
    sub = subscribe(getPred(prepPhrase));
    sub.setSeeOwnChanges(false);
    resourceSub = subscribe(getResourcePred(prepPhrase));
  }

  synchronized void allocate(MicroTask mt) {
    long [] values = {0}; /* in thousandths */
    int [] aspects = {0};

    if (mySensorResource == null)
      return;
    for (Enumeration pp = mt.getPrepositionalPhrases().elements(); pp.hasMoreElements();) {
      MicroPrepositionalPhrase mpp = (MicroPrepositionalPhrase)pp.nextElement();
      if (mpp.getPreposition().equals(prepPhrase))
        units = mpp.getIndirectObject();
      if (mpp.getPreposition().equals("Channel"))
        chan = Integer.parseInt(mpp.getIndirectObject());
    }
    try {
      if (units != null) mySensorResource.setUnits(units);
      mySensorResource.setChan(chan);
    } catch (Exception e) {System.out.println("caught " + e);}

    int imult10 = 10;
    long mult10 = imult10;
    long val = mySensorResource.getValue();
    values[0] = val;

    // Only update if unset or changed
    if (
     (mt.getAllocation() == null) ||
     (mt.getAllocation().getReportedResult() == null) ||
     (mt.getAllocation().getReportedResult().getValues() == null) ||
     (mt.getAllocation().getReportedResult().getValues().length == 0) ||
     (values[0]/mult10 != mt.getAllocation().getReportedResult().getValues()[0]/mult10) ||
     ((counter%10)==0)
    ) {
      // make an allocation result.
      MicroAllocation ma = new MicroAllocation(null, mt);
      MicroAllocationResult mar = new MicroAllocationResult();
      ma.setReportedResult(mar);
      mar.setAspects(aspects);
      mar.setValues(values);
System.out.println("Sending "+prepPhrase+": " + values[0]);
      openTransaction();
      mt.setAllocation(ma);
      publishChange(mt);
      closeTransaction();
      counter = 1;
    }
    else
      counter++;
  }

  public void execute() {
    Enumeration enm = sub.getAddedList().elements();
    while (enm.hasMoreElements()) {
System.out.println("MeasurePlugin::got Measure "+prepPhrase+" Task");
      MicroTask mt = (MicroTask)enm.nextElement();
      monitorTask(mt, sleepTime);
    }

    if (mySensorResource == null) {
      enm = resourceSub.getAddedList().elements();
      while (enm.hasMoreElements()) {
        SensorResource sr = (SensorResource)enm.nextElement();
        if (prepPhrase.indexOf(sr.getName()) >= 0) {
System.out.println("MeasurePlugin::found "+sr.getName()+" Resource");
          mySensorResource = sr;
          mySensorResource.init();
        }
      }
    }

    // handle changed here? what to do?

    Enumeration echa = sub.getChangedList().elements();
    if (echa.hasMoreElements()) {
      MicroTask mt = (MicroTask)echa.nextElement();
      Vector v = mt.getPrepositionalPhrases();
      for (Enumeration pp = v.elements(); pp.hasMoreElements();) {
        MicroPrepositionalPhrase mpp = (MicroPrepositionalPhrase)pp.nextElement();
        if (mpp.getPreposition().equals(prepPhrase)) {
          iShouldDie = true;
          monitorTask(mt, sleepTime);
        }
      }
    }
    Enumeration edel = sub.getRemovedList().elements();
    if (edel.hasMoreElements()) {
      MicroTask mt = (MicroTask)edel.nextElement();
      Vector v = mt.getPrepositionalPhrases();
      for (Enumeration pp = v.elements(); pp.hasMoreElements();) {
        MicroPrepositionalPhrase mpp = (MicroPrepositionalPhrase)pp.nextElement();
        if (mpp.getPreposition().equals(prepPhrase))
          iShouldDie = true;
      }
    }
  }

  private void monitorTask(MicroTask mt, int st) {
    Thread t = new Thread(new Monitor(mt, st));
    t.start();
  }

  class Monitor implements Runnable {
    MicroTask task;
    int tSleepTime;
    public Monitor(MicroTask mt, int st) {
      task = mt;
      tSleepTime = st;
    }
    public void run() {
      while (true) {
        allocate(task);
        if (tSleepTime < 0)
          break;
        if (tSleepTime > 0) {
          try {Thread.sleep(tSleepTime);} catch (InterruptedException ie){}
        }
        if (iShouldDie)
          break;
      }
    }
  }
}
