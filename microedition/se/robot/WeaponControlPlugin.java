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

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.se.domain.*;

/**
 **/
public class WeaponControlPlugin extends SimplePlugin
{
  private IncrementalSubscription taskSub;
  private IncrementalSubscription assetSub;
  private IncrementalSubscription reportPositionAllocs;

  UnaryPredicate reportPositionPred()
  {
    UnaryPredicate upred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        return false;
      }
    };
    return upred;
  }

  public void setupSubscriptions()
  {

    taskSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Task t = (Task)o;
          return t.getVerb().equals(Constants.Robot.verbs[Constants.Robot.SETLAUNCHTIME]);
        }
        return false;
      }
    });

    assetSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAgent) {
          MicroAgent m = (MicroAgent)o;
          String possible_roles = m.getMicroAgentPG().getCapabilities();
          StringTokenizer st = new StringTokenizer(possible_roles, ",");
          while (st.hasMoreTokens())
          {
            String a_role = st.nextToken();
            if(a_role.equals(Constants.Robot.meRoles[Constants.Robot.WEAPONPROVIDER]))
                 return true;
          }
        }
        return false;
      }});
  }

  public void execute()
  {
    Task t;
    Enumeration micros = assetSub.elements();
    if (!micros.hasMoreElements()) {
      //System.out.println("WeaponControlPlugin execute: no mc resources, return");
      return; // if no assets return
    }
    MicroAgent micro = (MicroAgent)micros.nextElement();

    Enumeration tasks = taskSub.elements();
    while (tasks.hasMoreElements())
    {
      t = (Task)tasks.nextElement();
      if (t.getPlanElement() != null)
        continue; // only want unallocated tasks
      //System.out.println("WeaponControlPlugin::allocing "+t.getVerb()+" task to micro");

      processLaunchTask(t, micro);

    }
  }

  private void processLaunchTask(Task t, MicroAgent micro)
  {
    String latString = "none";
    String lonString = "none";
    String toaString = "none";
    for (Enumeration enum= t.getPrepositionalPhrases();
            enum.hasMoreElements(); )
    {
      PrepositionalPhrase preps=(PrepositionalPhrase)enum.nextElement();
      if (preps!=null)
      {
        if (preps.getPreposition().equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.LATPREP]))
           latString=(String)preps.getIndirectObject();
        if (preps.getPreposition().equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.LONPREP]))
          lonString=(String)preps.getIndirectObject();
        if (preps.getPreposition().equalsIgnoreCase(Constants.Robot.prepositions[Constants.Robot.TIMEPREP]))
          toaString=(String)preps.getIndirectObject();
      }
    } // end-for

    if(latString.equalsIgnoreCase("none") || lonString.equalsIgnoreCase("none") || toaString.equalsIgnoreCase("none"))
    {
      System.err.println("WeaponControlPlugin: Unable to get lat lon from prepositions");
      return;
    }
    else
    {
      System.out.println("WeaponControlPlugin: Target Lat, Lon: " +latString+" "+lonString+" "+toaString);
      Collection posallocs = query(reportPositionPred());
      Iterator iter = posallocs.iterator();
      while(iter.hasNext())
      {
        Allocation a = (Allocation)iter.next();
        AllocationResult ar = a.getReceivedResult();
        //if (ar != null && ar.isSuccess() == true)
        if (ar != null)
        {
          double mylat = ar.getValue(Constants.Aspects.LATITUDE);
          double mylon = ar.getValue(Constants.Aspects.LONGITUDE);

          //originally reported in billionths. MicroTask plugin adjusted by 1000.0
          mylat = mylat*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);
          mylon = mylon*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);

          System.out.println("WeaponControlPlugin: My Lat, Lon: " +mylat+" "+mylon);
          RBCoordinate rb = EmitterLocator.FindRangeBearing(mylat, mylon,
                                    Double.parseDouble(latString),
                                    Double.parseDouble(lonString));

          System.out.println("WeaponControlPlugin: Target Range: " +rb.range);

          //expand task
          NewTask subTask;

          NewWorkflow nwf = theLDMF.newWorkflow();
          nwf.setParentTask(t);
          subTask = theLDMF.newTask();
          subTask.setPlan(theLDMF.getRealityPlan());
          subTask.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.ENGAGEWEAPON]));

          //set prepositions.
          Vector prepositions = new Vector();

          NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
          npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.TIMEPREP]);
          npp.setIndirectObject(toaString);
          prepositions.add(npp);

          npp = theLDMF.newPrepositionalPhrase();
          npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.RANGEPREP]);
          npp.setIndirectObject(String.valueOf(rb.range));
          prepositions.add(npp);

          npp = theLDMF.newPrepositionalPhrase();
          npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.BEARINGPREP]);
          npp.setIndirectObject(String.valueOf(rb.bearing));
          prepositions.add(npp);

          subTask.setPrepositionalPhrases(prepositions.elements());
          subTask.setParentTask(t);
          subTask.setWorkflow(nwf);
          subTask.setPlan(t.getPlan());
          subTask.setDirectObject(t.getDirectObject());
          //publishAdd(subTask);
          nwf.addTask(subTask);

          Expansion expansion =
            theLDMF.createExpansion(t.getPlan(), t, nwf, null);
          publishAdd(expansion);

          //System.out.println("WeaponControlPlugin: Allocating task to micro");

          Allocation allocation =
              theLDMF.createAllocation(subTask.getPlan(), subTask, micro, null, Role.ASSIGNED);

          publishAdd(allocation);

          break;
        }
      }
    }
  }
}
