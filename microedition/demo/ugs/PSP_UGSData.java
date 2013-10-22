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

package org.cougaar.microedition.demo.ugs;

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
import org.cougaar.planning.ldm.asset.*;

import org.cougaar.microedition.se.domain.*;

/**
 * This PSP responds with the vital statistics of each robot reporting to
 * this agent.
 */
public class PSP_UGSData extends PSP_BaseAdapter implements PlanServiceProvider, UseDirectSocketOutputStream, UISubscriber
{

/**
 * This predicate matches what I want
 */
  class Gimme implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof Asset) {
        Asset asset = (Asset)o;
        PropertyGroup pg = asset.searchForPropertyGroup(UGSPG.class);
        return pg != null;
      }
      return false;
    }
  }

  /**
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_UGSData()
  {
    super();
  }

  public PSP_UGSData(String pkg, String id) throws RuntimePSPException
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
   * Sends HTML update to client
   **/
  public void execute(
      PrintStream cout,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    //
    // Look at allocation results to see what the temperature is.
    //
    Collection bots = psc.getServerPluginSupport().queryForSubscriber( new Gimme());
    Iterator bot_iter = bots.iterator();
    // dumpXml(bot_iter);
    dumpText(bot_iter, cout);
  }

  private void dumpText(Iterator bot_iter, PrintStream cout) {
    while (bot_iter.hasNext()) {
      dumpRecordText((Asset)bot_iter.next(), cout);
    }
  }

  private void dumpXml(Iterator bot_iter, PrintStream cout) {
      cout.println("<?xml version=\"1.0\"?>");
      cout.println("<collection>");
    while (bot_iter.hasNext()) {
      dumpRecordXml((Asset)bot_iter.next(), cout);
    }
      cout.println("</collection>");
  }

  private void dumpRecordXml(Asset bot, PrintStream out) {
    out.println("<UGS>");
    out.println("<ID>\t"+bot.getItemIdentificationPG().getItemIdentification()+"</ID>");
    UGSPG rpg = (UGSPG)bot.searchForPropertyGroup(UGSPG.class);
    out.println("<lat>\t"+rpg.getLat()+"</lat>");
    out.println("<lon>\t"+rpg.getLon()+"</lon>");
    int entries=rpg.getEntries();
    double[] bearings=rpg.getBearing();
    double[] detTimes=rpg.getDetTime();
    for (int idx=entries-1; idx>-1; idx--) {
      out.println("<BPair> <!-- =====\t============ -->");
      out.println("<bearing>\t"+bearings[idx]+"</bearing>");
      out.println("<detTime>\t"+detTimes[idx]+"</detTime>");
      out.println("</BPair> <!-- =====\t============ --><BR></BR>");
    }
    out.println("</UGS>");
    out.println();
  }

  private void dumpRecordText(Asset bot, PrintStream out) {
    out.println("ID\t"+bot.getItemIdentificationPG().getItemIdentification());
    UGSPG rpg = (UGSPG)bot.searchForPropertyGroup(UGSPG.class);
    out.println("lat\t"+rpg.getLat());
    out.println("lon\t"+rpg.getLon());
    out.println("status\t"+rpg.getStatus());
    int entries=rpg.getEntries();
    double[] bearings=rpg.getBearing();
    double[] detTimes=rpg.getDetTime();
    for (int idx=entries-1; idx>-1; idx--) {
      out.println("bearing\t"+bearings[idx]);
      out.println("detTime\t"+detTimes[idx]);
      out.println("<!--=================--><BR></BR>");
    }
    out.println();
  }

  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
//    return true;
  }

  public boolean returnsHTML() {
    return true;
//    return false;
  }

  /**
   * Any PlanServiceProvider must be able to provide DTD of its
   * output IFF it is an XML PSP... ie.  returnsXML() == true;
   * or return null
   **/
  public String getDTD()  {
    return null;
//    return "";
  }

  /**
   * The UISubscriber interface. (not needed)
   */
  public void subscriptionChanged(Subscription subscription) {
  }
}
