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
import org.cougaar.microedition.shared.Constants;
import org.cougaar.microedition.se.domain.*;

/**
 **/
public class AdvancePlugin extends SimplePlugin
{
  private String myAdvanceVerb = Constants.Robot.verbs[Constants.Robot.ADVANCE];
  private static String name="AdvancePlugin";

  // Subscription for all my tasks
  private IncrementalSubscription taskSub;

  // Subscription for all of my assets
  private IncrementalSubscription assetSub;

  // Subscription for all of my allocs
  private IncrementalSubscription allocSub;

   /**
   * Establish subscriptions
   **/
  public void setupSubscriptions() {
    //System.out.println("AdvancePlugin::setupSubscriptions");

    // This predicate matches all tasks with one of myVerbs
    taskSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Task t = (Task)o;
          return t.getVerb().equals(myAdvanceVerb) ;
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
            if(a_role.equals(Constants.Robot.meRoles[Constants.Robot.LOCOMOTIONCONTROLLER]))
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
          return t.getVerb().equals(myAdvanceVerb) ;
        }
        return false;
      }
    });

  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {
    Task t;
    Enumeration micros = assetSub.elements();
    if (!micros.hasMoreElements())
    {
      //System.out.println(name+" execute: no mc resources, return");
      return; // if no assets return
    }
    MicroAgent micro = (MicroAgent)micros.nextElement();

    Enumeration tasks = taskSub.elements();
    while (tasks.hasMoreElements()) {
      t = (Task)tasks.nextElement();
      if (t.getPlanElement() != null)
        continue; // only want unallocated tasks
      //System.out.println("AdvancePlugin::allocing "+t.getVerb()+" task to micro");
      Allocation allo = makeAllocation(t, micro);
      publishAdd(allo);
    }

    Enumeration e = taskSub.getRemovedList();
    while (e.hasMoreElements())
    {
      t = (Task)e.nextElement();
      //System.out.println("AdvancePlugin::got removed task with verb "+t);
    }

    e = taskSub.getChangedList();
    while (e.hasMoreElements()) {
      t = (Task)e.nextElement();
      //System.out.println("AdvancePlugin::got changed task with verb "+t.getVerb());
    }

  }

  private void addAspectValues(Task t, Vector aspects, Vector values) {
    PlanElement pe = t.getPlanElement();
    if (pe == null)
      return;
    AllocationResult ar = pe.getReceivedResult();
    if (ar == null)
      return;
    int [] its_types = ar.getAspectTypes();
    double [] its_values = ar.getResult();
    for (int i=0; i<its_types.length; i++) {
      aspects.addElement(new Integer(its_types[i]));
      values.addElement(new Double(its_values[i]));
    }
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
