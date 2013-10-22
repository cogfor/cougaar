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
import org.cougaar.core.service.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.microedition.se.domain.*;

/**
 * A test Plugin to test interoperatbility with Cougaar ME.
 * It asks all known micro clusters for the temperature.
 */
public class TempAllocatorPlugin extends SimplePlugin {

  IncrementalSubscription assetSub, allocSub;
  String Name = "Temperature";
  Logfile lf = null;

  final boolean isThermometer(MicroAgent mc) {
    return (mc.getMicroAgentPG().getCapabilities().toLowerCase()
              .indexOf("TemperatureProvider".toLowerCase())>-1);
  }

  /**
   * Subscribe to MicroAgents and my own allocations.
   */
  protected void setupSubscriptions() {

    boolean log = false;

    Vector parameters = getParameters();
    Enumeration pnum = parameters.elements();
    while (pnum.hasMoreElements()) {
      String param = (String)pnum.nextElement();
      if (param.equalsIgnoreCase("log")) log = true;
      if (param.toLowerCase().indexOf("name") < 0)
        continue;
      int indx = param.indexOf("=");
      if (indx < 0)
        continue;
      Name = (param.substring(indx+1)).trim();
      System.out.println("Name: " + Name);
    }

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAgent) {
           MicroAgent mc = (MicroAgent)o;
           return isThermometer(mc);
        }
        return false;
      }});

    allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          return a.getTask().getVerb().equals("Measure") &&
                (a.getTask().getPrepositionalPhrase(Name) != null);
        }
        return false;
      }});

    if (log)  {
      String agentName = "unknown";
      AgentIdentificationService ais =
        (AgentIdentificationService) getBindingSite().getServiceBroker().getService(this, AgentIdentificationService.class, null);
      if (ais != null) {
        agentName = ais.getName();
      }

      lf = new Logfile(agentName+"-"+Name+".csv");
    }

    System.out.println(Name+"AllocationPlugin:setupSubscriptions()");

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
      System.out.println(Name+"AllocationPlugin:allocate()");
    }

    //
    // Look at allocation results to see what the temperature is.
    //
    Enumeration allos = allocSub.getChangedList();
    while (allos.hasMoreElements()) {
      Allocation alloc = (Allocation)allos.nextElement();
      AllocationResult ar = alloc.getReceivedResult();

      double tmp = ar.getValue(0);
      System.out.println(Name+" is: "+tmp);
      Task t = alloc.getTask();
      PrepositionalPhrase p = t.getPrepositionalPhrase(Name);
      String ido = p.getIndirectObject().toString();
      if (tmp < (double)50.0 && ido.equals("Fahrenheit"))
        continue;
      if (tmp >= (double)50.0 && ido.equals("Celsius"))
        continue;
      if (tmp < (double)50.0) {
        System.out.println("changing to Fahrenheit");
        ((NewPrepositionalPhrase)p).setIndirectObject("Fahrenheit");
      }
      else {
        System.out.println("changing to Celsius");
        ((NewPrepositionalPhrase)p).setIndirectObject("Celsius");
      }
      ((NewTask)t).setPrepositionalPhrase(p);
      publishChange(t);

      if (lf != null)
        lf.log(ar.getValue(0));
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
    npp.setPreposition(Name);
    npp.setIndirectObject("Celsius");
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
