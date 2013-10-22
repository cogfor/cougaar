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
package org.cougaar.microedition.se.domain;

import java.io.*;
import java.util.*;

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.se.domain.*;

/**
 */
public class MicroWebServerPlugin extends SimplePlugin
{

  UnaryPredicate getAgentsPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
        if(o instanceof MicroAgent)
          return true;
        return false;
      }
    };
    return myPred;
  }

  private static final String provideserverporttask = "ProvideWebServerPortIdentification";
  private static final int portidaspect = 100;
  private boolean debugging = true;
  private NameTablePair pubhash = null;

  UnaryPredicate getPortIDAllocationPred()
  {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Task mt = ((Allocation)o).getTask();
          return mt.getVerb().equals(provideserverporttask);
        }
        return false;
      }
    };
    return myPred;
  }

  private IncrementalSubscription agentSub;
  private IncrementalSubscription portidallocSub;

  public void setupSubscriptions()
  {
    System.out.println("MicroWebServerPlugin::setupSubscriptions");

    agentSub = (IncrementalSubscription)subscribe(getAgentsPred());
    portidallocSub = (IncrementalSubscription)subscribe(getPortIDAllocationPred());

    pubhash = new NameTablePair("MicroWebServerPorts", new Hashtable());

    publishAdd(pubhash);

  }

  public void execute()
  {

    //Examine new microagents in the society
    Enumeration enm = agentSub.getAddedList();
    while(enm.hasMoreElements())
    {
      MicroAgent magent = (MicroAgent)enm.nextElement();
      MicroAgentPG mapg = magent.getMicroAgentPG();

      if(pubhash.table.containsKey(mapg.getName()))
        continue; //don't spawn a task for this, it's already been done

      Integer temportid = new Integer(0);
      pubhash.table.put(mapg.getName(), temportid);
      publishChange(pubhash);

      NewTask t = theLDMF.newTask();
      t.setPlan(theLDMF.getRealityPlan());
      t.setVerb(Verb.getVerb(provideserverporttask));
      publishAdd(t);

      Allocation allocation =
                 theLDMF.createAllocation(t.getPlan(), t, magent, null, Role.ASSIGNED);

      publishAdd(allocation);

       if(debugging)
       {
          System.out.println("MicroWebServerPlugin:: New MicroAgent: "+mapg.getName());
          System.out.println("                  Task allocated: "+t.getUID());
       }
    }

    enm = agentSub.getRemovedList();
    while(enm.hasMoreElements())
    {
      MicroAgent magent = (MicroAgent)enm.nextElement();
      MicroAgentPG mapg = magent.getMicroAgentPG();

      if(pubhash.table.containsKey(mapg.getName()))
      {
        pubhash.table.remove(mapg.getName());
        publishChange(pubhash);
      }
    }

    //Examine allocation results for port id reporting
    enm = portidallocSub.getChangedList();
    while(enm.hasMoreElements())
    {
      Allocation ma = (Allocation)enm.nextElement();

      String assetname = null;
      MicroAgent magent = (MicroAgent)ma.getAsset();
      if(magent != null)
      {
        MicroAgentPG mapg = magent.getMicroAgentPG();
        if(mapg != null) assetname = mapg.getName();
      }

      AllocationResult mar = ma.getReportedResult();
      if (mar == null) continue;

      if(debugging)
      {
        System.out.println("MicroWebServerPlugin: Allocation changed on task   : " +ma.getTask().getUID());
        System.out.println("MicroWebServerPlugin: Allocation changed from asset: " +assetname);
      }

      try
      {
        int value = (int)mar.getValue(portidaspect);
        if(assetname != null)
        {
          Integer serverport = new Integer(value);
          if(pubhash.table.containsKey(assetname))
          {
            if(debugging)
            {
              System.out.println("Setting local hashtable: key = " +assetname);
              System.out.println("Setting local hashtable: obj = " +serverport);
            }
            pubhash.table.put(assetname, serverport);
            publishChange(pubhash);
          }
        }
      }
      catch(Exception e)
      {
        System.out.println("MicroWebServerPlugin: unable to obtain allocation result");
      }
    }
  }
}
