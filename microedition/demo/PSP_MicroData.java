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

package org.cougaar.microedition.demo;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

import org.cougaar.lib.planserver.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.microedition.se.domain.*;

public class PSP_MicroData extends PSP_BaseAdapter implements PlanServiceProvider, KeepAlive, UseDirectSocketOutputStream, UISubscriber
{

/**
 * This predicate matches what I want
 */
  class Gimme implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Allocation a = (Allocation)o;
          if (a.getTask().getVerb().equals("Measure"))
            return (
              (a.getTask().getPrepositionalPhrase("Temperature") != null) ||
              (a.getTask().getPrepositionalPhrase("Light") != null) ||
              (a.getTask().getPrepositionalPhrase("Value") != null)
            );
      }
      return false;
    }
  }

  /**
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_MicroData()
  {
    super();
  }

  public PSP_MicroData(String pkg, String id) throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

  /**
   *
   * Periodically sends HTML update to client
   **/
  int iterationCounter =0;
  public void execute(
      PrintStream cout,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    //
    // Look at allocation results to see what the temperature is.
    //
    out = cout;
    Subscription subscription = psc.getServerPluginSupport().subscribe(this, new Gimme());
    while (true) {
      try {Thread.sleep(Long.MAX_VALUE);} catch (Exception e) {}
    }
  }

  PrintStream out;

  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  /**
   * Any PlanServiceProvider must be able to provide DTD of its
   * output IFF it is an XML PSP... ie.  returnsXML() == true;
   * or return null
   **/
  public String getDTD()  {
    return null;
  }

  /**
   * The UISubscriber interface. (not needed)
   */
  public void subscriptionChanged(Subscription subscription) {

    Collection container = ((IncrementalSubscription)subscription).getChangedCollection();
    if (container == null) {
      container = ((IncrementalSubscription)subscription).getAddedCollection();
      if (container == null)
        return;
    }
    else
      container.addAll(((IncrementalSubscription)subscription).getAddedCollection());

    Iterator iterate = container.iterator();
    while (iterate.hasNext()) {
      Allocation alloc = (Allocation)iterate.next();
      if (alloc == null) break;
      String src = alloc.getAsset().getItemIdentificationPG().getItemIdentification();
      AllocationResult ar = alloc.getReceivedResult();
      if (ar == null) continue;
      if (alloc.getTask().getPrepositionalPhrase("Temperature") != null)
        out.println(src+":Temperature:"+ar.getValue(0));
      else if (alloc.getTask().getPrepositionalPhrase("Light") != null)
        out.println(src+":Light:"+ar.getValue(0));
      else if (alloc.getTask().getPrepositionalPhrase("Value") != null)
        out.println(src+":PDA:"+ar.getValue(0));

      out.flush();
    }
  }
}
