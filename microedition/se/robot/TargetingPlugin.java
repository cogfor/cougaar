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
public class TargetingPlugin extends SimplePlugin
{
  String myVerb = Constants.Robot.verbs[Constants.Robot.REPORTTARGET];

  // Subscription for all myVerb tasks
  private IncrementalSubscription taskSub;

  // Subscription for all of my assets
  private IncrementalSubscription assetSub;

  // Subscription for all of my subtasks
  private IncrementalSubscription allocSub;

   /**
   * Establish subscriptions
   **/
  public void setupSubscriptions() {
System.out.println("TargetingPlugin::setupSubscriptions");

  // This predicate matches all tasks with myVerb
  taskSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task)o;
        return t.getVerb().equals(myVerb);
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
            if(a_role.equals(Constants.Robot.meRoles[Constants.Robot.TARGETINGCONTROLLER]))
                 return true;
          }
        }
        return false;
      }});

  /**
   * Predicate that matches all subtasks of my expansions
   */
  allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Allocation a = (Allocation)o;
        return a.getTask().getVerb().equals(myVerb);
      }
      return false;
    }
  });

  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {

    Enumeration micros = assetSub.elements();
    if (!micros.hasMoreElements())
      return; // if no assets return
    MicroAgent micro = (MicroAgent)micros.nextElement();

    Enumeration tasks = taskSub.elements();
    while (tasks.hasMoreElements()) {
      Task t = (Task)tasks.nextElement();
      if (t.getPlanElement() != null)
        continue; // only want unallocated tasks
System.out.println("TargetingPlugin::allocing task to micro");
      Allocation allo = makeAllocation(t, micro);
      publishAdd(allo);
    }

    Enumeration e = allocSub.getChangedList();
    while (e.hasMoreElements()) {
//System.out.println("TargetingPlugin::xfering rec to est");
      Allocation sa = (Allocation)e.nextElement();
      if (!sa.getReceivedResult().equals(sa.getEstimatedResult())) {
        sa.setEstimatedResult(sa.getReceivedResult());
        publishChange(sa);
      }
    }

    e = taskSub.getRemovedList();
    while (e.hasMoreElements()) {
System.out.println("TargetingPlugin::got removed task");
      e.nextElement();
    }

    e = taskSub.getChangedList();
    while (e.hasMoreElements()) {
System.out.println("TargetingPlugin::got changed task");
      e.nextElement();
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
