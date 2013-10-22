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

import java.util.*;
import gov.nasa.jpl.sensorweb.Datatypes;

import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.microedition.se.domain.*;


/**
 **/
public class ReadHeatIndexPlugin extends SimplePlugin
{
  private String taskVerb = "ReportHeatIndex";
  private String microAgentRole = "HeatIndexProvider";

  private IncrementalSubscription assetSub;
  private IncrementalSubscription allocSub;

  private MicroAgent heatindexprovider = null;
  private NewTask reportheatindex = null;

  private boolean debugging = false;

  public void setupSubscriptions()
  {

    if(debugging)
        System.out.println("ReadHeatIndexPlugin: setupSubscriptions");

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAgent) {
          MicroAgent m = (MicroAgent)o;
          String possible_roles = m.getMicroAgentPG().getCapabilities();
          StringTokenizer st = new StringTokenizer(possible_roles, ",");
          while (st.hasMoreTokens())
          {
            String a_role = st.nextToken();
            if(a_role.equals(microAgentRole))
                 return true;
          }
        }
        return false;
      }});


    // Predicate that matches all allocations of my tasks
    allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          Task t=a.getTask();
          return t.getVerb().equals(taskVerb) ;
        }
        return false;
      }
    });

  }

  public void execute()
  {

    //This section looks for a new micro asset and assigns it a task
    Enumeration micros = assetSub.getAddedList();
    if (micros.hasMoreElements())
    {
      if(debugging)
        System.out.println("ReadHeatIndexPlugin: A heat index provider agent has been identified.");

      heatindexprovider = (MicroAgent)micros.nextElement();

      //create a task for this agent
      if(reportheatindex != null)
      {
        publishRemove(reportheatindex);
      }

      reportheatindex = theLDMF.newTask();
      reportheatindex.setPlan(theLDMF.getRealityPlan());
      reportheatindex.setVerb(Verb.getVerb(taskVerb));
      publishAdd(reportheatindex);

      Allocation allo = theLDMF.createAllocation(reportheatindex.getPlan(),
                                                  reportheatindex,
                                                  heatindexprovider,
                                                  null, Role.ASSIGNED);
      publishAdd(allo);

    }

    //this section keeps track of assets that went away
    micros = assetSub.getRemovedList();
    if (micros.hasMoreElements())
    {
      MicroAgent magent = (MicroAgent)micros.nextElement();
      if(magent == heatindexprovider)
      {
        if(debugging)
          System.out.println("ReadHeatIndexPlugin: Heat index provider agent was removed.");

        //create a task for this agent
        if(reportheatindex != null)
        {
          publishRemove(reportheatindex);
          reportheatindex = null;
        }

        heatindexprovider = null;

      }
    }

    //this section monitors allocation results
    Enumeration allocEnum = allocSub.getChangedList();
    while (allocEnum.hasMoreElements())
    {
      Allocation alloc = (Allocation)allocEnum.nextElement();
      AllocationResult ar = alloc.getReportedResult();

      if (ar==null)
      {
        if(debugging) System.out.println("ReadHeatIndexPlugin allocation result null!");
        continue;
      }

      if (ar.isSuccess())
      {
        int podid = (int)(ar.getValue((int)Datatypes.IDENTIFY)*1000.0);
        long recordtime = (long)(ar.getValue((int)Datatypes.RECORDTIME)*1000.0);
        double heatindex = (ar.getValue((int)Datatypes.HEAT_INDEX)*1000.0);

        if(debugging)
          System.out.println("ReadHeatIndexPlugin allocation result is " +podid+" "+recordtime+" "+heatindex);


        HeatIndexRecord hirec = new HeatIndexRecord(podid, new Date(recordtime), heatindex);
        publishAdd(hirec);
      }
    }
  }
}
