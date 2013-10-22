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

import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.plugin.util.PluginHelper;
import org.cougaar.util.UnaryPredicate;
import java.util.*;
import org.cougaar.microedition.shared.Constants;

/**
 * Uses values from a "Value" task (created elsewhere) to control
 * the velocity of "Advance" tasks.
 * One integer parameter is the multiplier to convert the PDA value to
 * robot velocity.
 **/
public class PDAMotionControlPlugin extends SimplePlugin
{
  private String myVerb = Constants.Robot.verbs[Constants.Robot.ADVANCE];

  // Subscription for all my 'Advance' tasks
  private IncrementalSubscription taskSub;

  // Subscription for all of my 'Value' allocs
  private IncrementalSubscription allocSub;

  // this number is multiplied with the PDA value to set the speed of the robot
  private int velocityMultiplier = 25;

   /**
   * Establish subscriptions
   **/
  public void setupSubscriptions() {
    System.out.println("PDAMotionControlPlugin::setupSubscriptions");

    Enumeration enm = getParameters().elements();
    while (enm.hasMoreElements()) {
      String param = (String)enm.nextElement();
      velocityMultiplier = Integer.parseInt(param);
      System.out.println("PDAMotionControlPlugin: velocityMultiplier set to: "+velocityMultiplier);
    }
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

  /**
   * Predicate that matches all allocations of "Value" tasks
   */
  allocSub = (IncrementalSubscription)subscribe(new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Allocation) {
        Allocation a = (Allocation)o;
        return a.getTask().getVerb().equals("Measure");
      }
      return false;
    }
  });

  }

  /**
   * Top level plugin execute loop.
   **/
  public void execute() {

    // look for changes 'Value' allocations
    Enumeration e = allocSub.getChangedList();
    while (e.hasMoreElements()) {
      Allocation sa = (Allocation)e.nextElement();
//      System.out.println("PDAMotionControl: changed alloc:"+sa);
      if (sa.getReceivedResult() != null) {
        double value = sa.getReceivedResult().getValue(0); // value lives in aspect '0'
//        if (removeOldAdvanceTasks(value)) {
//          System.out.println("PDAMotionControl: New value:"+value);
//          createAdvanceTask(value);
//        }
        updateAdvanceTasks(value);
      }
    }
  }

  private void updateAdvanceTasks(double value) {
    if (taskSub.isEmpty()) {
      createAdvanceTask(value);
    }
    String newValue = Integer.toString((int)value * velocityMultiplier);
    Enumeration e = taskSub.elements();
    while (e.hasMoreElements()) {
      Task task = (Task)e.nextElement();
      PrepositionalPhrase pp = task.getPrepositionalPhrase("velocity");
      if (! pp.getIndirectObject().equals(newValue)) {
        System.out.println("PDAMotionControl: changing task:"+task.getUID().toString());
        ((NewPrepositionalPhrase)pp).setIndirectObject(newValue);
        publishChange(task);
      }
    }
  }

  private boolean removeOldAdvanceTasks(double value) {
    boolean ret = taskSub.isEmpty();
    String newValue = Integer.toString((int)value * velocityMultiplier);
    Enumeration e = taskSub.elements();
    while (e.hasMoreElements()) {
      Task task = (Task)e.nextElement();
      PrepositionalPhrase pp = task.getPrepositionalPhrase("velocity");
      if (! pp.getIndirectObject().equals(newValue)) {
        System.out.println("PDAMotionControl: removing old advance task");
        publishRemove(task);
        ret = true;
      }
    }
    return ret;
  }

  private void createAdvanceTask(double value) {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(myVerb));

    int velocity = (int)value * velocityMultiplier;
    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition("velocity");
    npp.setIndirectObject(Integer.toString(velocity));
    t.setPrepositionalPhrase(npp);

    System.out.println("PDAMotionControlPlugin: new advance task:"+t);
    publishAdd(t);
  }

}
