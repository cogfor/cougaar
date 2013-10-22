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
package org.cougaar.microedition.se.robot;

import java.util.*;

import org.cougaar.planning.plugin.legacy.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.microedition.se.domain.*;

/**
 * A test Plugin to test interoperatbility with Cougaar ME.
 * It asks all known micro clusters for the temperature.
 */
public class ValueAllocatorPlugin extends SimplePlugin {

  IncrementalSubscription assetSub, allocSub;
  double rescindAt = Double.NEGATIVE_INFINITY;

  /**
   * Subscribe to MicroAgents and my own allocations.
   */
  protected void setupSubscriptions() {

    Enumeration pnum = getParameters().elements();
    while (pnum.hasMoreElements()) {
      String param = (String)pnum.nextElement();
      if (param.toLowerCase().indexOf("rescind") < 0)
        continue;
      int indx = param.indexOf("=");
      if (indx < 0)
        continue;
      String rescindLevel = (param.substring(indx+1)).trim();
      rescindAt = Double.parseDouble(rescindLevel);
      System.out.println("Rescinding tasks with value less than: " + rescindLevel);
    }

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {return o instanceof MicroAgent;}});


    allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          return a.getTask().getVerb().equals("Measure") &&
                (a.getTask().getPrepositionalPhrase("Value") != null);
        }
        return false;
      }});

  }

  /**
   * Handle new micro clusters and changes to my allocations
   */
  protected void execute() {

    //
    // Allocate a temperature measure task to all micro clusters
    //
    Enumeration micros = assetSub.getAddedList();
    while (micros.hasMoreElements()) {
      MicroAgent micro = (MicroAgent)micros.nextElement();
      Task t = makeTask();
      publishAdd(t);
      Allocation allo = makeAllocation(t, micro);
      publishAdd(allo);
    }

    //
    // Look at allocation results to see what the temperature is.
    //
    Enumeration allos = allocSub.getChangedList();
    while (allos.hasMoreElements()) {
      Allocation alloc = (Allocation)allos.nextElement();
      AllocationResult ar = alloc.getReceivedResult();
      double val = ar.getValue(0);
      System.out.println("Value is: "+val);
      if (val < rescindAt) {
        System.out.println("removing Value Task");
        publishRemove(alloc.getTask());
      }
    }
  }

  /**
   * Gin-up an new temperature task.
   */
  private Task makeTask() {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb("Measure"));

    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition("Value");
    npp.setIndirectObject("Button");
    prepositions.add(npp);
    t.setPrepositionalPhrases(prepositions.elements());

    return t;
  }

  /**
   * Gin-up an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task t, MicroAgent micro) {
    AllocationResult estAR = null;
    Allocation allocation =
      theLDMF.createAllocation(t.getPlan(), t, micro, estAR, Role.ASSIGNED);
    return allocation;
  }
}
