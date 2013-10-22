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
package org.cougaar.microedition.se.test;

import java.util.*;

import org.cougaar.planning.plugin.legacy.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.microedition.se.domain.*;

/**
 * A test Plugin to test interoperatbility with Cougaar ME.
 */
public class TestAllocatorPlugin extends SimplePlugin {

  IncrementalSubscription assetSub, allocSub;
  String verbName = "Test";

  /** Holds value of property loggingService. */
  private LoggingService loggingService;
  
  /**
   * Subscribe to MicroAgents and my own allocations.
   */
  protected void setupSubscriptions() {

    Vector parameters = getParameters();
    Enumeration pnum = parameters.elements();
    while (pnum.hasMoreElements()) {
      String param = (String)pnum.nextElement();
      if (param.toLowerCase().indexOf("name") < 0)
        continue;
      int indx = param.indexOf("=");
      if (indx < 0)
        continue;
      verbName = (param.substring(indx+1)).trim();
      loggingService.info("verbName: " + verbName);
    }

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {return o instanceof MicroAgent;}});


    allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          return a.getTask().getVerb().equals(verbName);
        }
        return false;
      }});

  }

  /**
   * Handle new micro agents and changes to my allocations
   */
  protected void execute() {

    //
    // Allocate a temperature measure task to all micro agents
    //
    Enumeration micros = assetSub.getAddedList();
    while (micros.hasMoreElements()) {
      MicroAgent micro = (MicroAgent)micros.nextElement();
      loggingService.info("Got a new micro asset: "+micro);
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
      int [] aspectTypes = ar.getAspectTypes();
      for (int i=0; i<aspectTypes.length; i++) {
        double tmp = ar.getValue(aspectTypes[i]);
        loggingService.info(verbName+" aspect "+aspectTypes[i]+" is: "+tmp);
      }
/*
      Task t = alloc.getTask();
      PrepositionalPhrase p = t.getPrepositionalPhrase(Name);
      String ido = p.getIndirectObject().toString();
      if (tmp < (double)0.0 && ido.equals("1")) {
        System.out.println("shouldn't happen");
        continue;
      }
      if (tmp > (double)0.0 && ido.equals("-1")) {
        System.out.println("shouldn't happen");
        continue;
      }
      if (tmp < (double)0.0) {
        System.out.println("changing to 1");
        ((NewPrepositionalPhrase)p).setIndirectObject("1");
      }
      else {
        System.out.println("changing to -1");
        ((NewPrepositionalPhrase)p).setIndirectObject("-1");
      }
      ((NewTask)t).setPrepositionalPhrase(p);
      publishChange(t);
 */
    }
  }

  /**
   * Gin-up an new task.
   */
  private Task makeTask() {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(verbName));

/*
    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Name);
    npp.setIndirectObject("1");
    prepositions.add(npp);
    t.setPrepositionalPhrases(prepositions.elements());
*/

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
  
  /** Getter for property loggingService.
   * @return Value of property loggingService.
   */
  public LoggingService getLoggingService() {
      return loggingService;
  }
  
  /** Setter for property loggingService.
   * @param loggingService New value of property loggingService.
   */
  public void setLoggingService(LoggingService loggingService) {
      this.loggingService = loggingService;
  }
  
}
