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

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.microedition.shared.Constants;
import org.cougaar.microedition.se.domain.*;

/**
 * Plugin for robot to use to control flashlight.
 */
public class FlashlightAllocatorPlugin extends SimplePlugin {


  /**
   * Return a UnaryPredicate which is true for ControlFlashlight tasks.
   */
  UnaryPredicate getTaskPred() {
    UnaryPredicate taskPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        boolean ret=false;
        if (o instanceof Task) {
          Task mt = (Task)o;
          ret= (mt.getVerb().equals(Constants.Robot.verbs[Constants.Robot.CONTROLFLASHLIGHT]));
        }
        return ret;
      }
    };
    return taskPred;
  }

  /**
   * Return a UnaryPredicate which is true for Flashlight MicroAgents.
   */
  UnaryPredicate getResourcePred() {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o)
      {
        if (o instanceof MicroAgent)
        {
          MicroAgent m = (MicroAgent)o;
          String possible_roles = m.getMicroAgentPG().getCapabilities();
          StringTokenizer st = new StringTokenizer(possible_roles, ",");
          while (st.hasMoreTokens())
          {
            String a_role = st.nextToken();
            if(a_role.equals(Constants.Robot.meRoles[Constants.Robot.FLASHLIGHTCONTROLLER]))
                 return true;
          }
        }
        return false;
      }
    };
    return resourcePred;
  }

  private MicroAgent myFlashlightMC = null;
  private IncrementalSubscription taskSub;
  private IncrementalSubscription resourceSub;
  private String name="FlashlightAllocatorPlugin";
  static int debugLevel=10;

  public void setupSubscriptions() {
    if (getParameters() != null) {
      if (debugLevel > 10) System.out.println(name+" FlashlightAllocatorPlugin: setupSubscriptions " + getParameters());
    }
    else {
      if (debugLevel > 10) System.out.println(name+" FlashlightAllocatorPlugin: setupSubscriptions No Params");
    }
    taskSub = (IncrementalSubscription)subscribe(getTaskPred());
    resourceSub = (IncrementalSubscription)subscribe(getResourcePred());
  }

  /**
   * Handle addition of flashlight resource microcluster and added or removed tasks of interest.
   */
  public void execute() {
      if (debugLevel > 5) System.out.println(name+" FlashlightAllocatorPlugin: execute ");

    if (myFlashlightMC == null) {
      Enumeration flashlightEnum = resourceSub.getAddedList();
      if (debugLevel > 10) System.out.println(name+" FlashlightAllocatorPlugin: execute flashlightEnum.hasMoreElements() "+flashlightEnum.hasMoreElements());
      while (flashlightEnum.hasMoreElements()) {
        MicroAgent fl = (MicroAgent)flashlightEnum.nextElement();
        initFlashlight(fl);
      }
    }

    Enumeration taskEnum = taskSub.getAddedList();
     if (debugLevel > 10) System.out.println(name+" FlashlightAllocatorPlugin: execute taskEnum.hasMoreElements() "+taskEnum.hasMoreElements());
    while (taskEnum.hasMoreElements()) {
      Task mt = (Task)taskEnum.nextElement();
      updateFlashlightMC(mt, true);
    }

    Enumeration edel = taskSub.getRemovedList();
     if (debugLevel > 10) System.out.println(name+" FlashlightAllocatorPlugin: execute edel.hasMoreElements() "+edel.hasMoreElements());
    while (edel.hasMoreElements()) {
      Task mt = (Task)edel.nextElement();
      updateFlashlightMC(mt, false);
    }
  }

  /**
   * Initialize flashlight resource.
   */
  private void initFlashlight(MicroAgent fl) {
    if (myFlashlightMC != null) {
      if (debugLevel > 10) System.err.println("FlashlightAllocatorPlugin: execute->initFlashlight called when myFlashlightMC is already set.");
    }
    myFlashlightMC=fl;
  }

  /**
   * Allocate task to flashlight resource.
   */
  synchronized private void updateFlashlightMC(Task mt, boolean wantOn) {
    long [] values = {0};
    int [] aspects = {0};

    if (myFlashlightMC == null)
      return;

    if (wantOn) {
      Allocation alloc=makeAllocation(mt, myFlashlightMC);
      publishAdd(alloc);
      publishChange(mt);
    }
  }

  /**
   * Create an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task task, MicroAgent micro) {
    AllocationResult estAR = null;
    Allocation allocation =
     theLDMF.createAllocation(task.getPlan(), task, micro, estAR, Role.ASSIGNED);
    return allocation;
  }


}
