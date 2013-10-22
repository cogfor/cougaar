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
package org.cougaar.microedition.se.ugs;
import org.cougaar.microedition.se.domain.*;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.*;

import org.cougaar.microedition.shared.Constants;
import org.cougaar.microedition.se.domain.MicroAgent;

import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.OrganizationPG;


/**
 * Plugin to control NSOF UGS.
 */
public class UGSEmuPlugin extends SimplePlugin {

  static final private String myTargetingVerb
    = Constants.Robot.verbs[Constants.Robot.REPORTTARGET];
  static final private String LatPrep
    = Constants.Robot.prepositions[Constants.Robot.LATPREP];
  private String latPrepValue="Unknown";
  static final private String LonPrep
    = Constants.Robot.prepositions[Constants.Robot.LONPREP];
  private String lonPrepValue="Unknown";

  static final private String myPositionVerb
    = Constants.Robot.verbs[Constants.Robot.REPORTPOSITION];


  private IncrementalSubscription targetSub;
  private IncrementalSubscription positionSub;

  static int debugLevel=110;
  private double bearing = -1 ;
  private double lat=41.5;
  private double lon=-100.75;
  long sleepTime=4000;

  /**
   * Return a UnaryPredicate which is true for ReportDection tasks.
   */
  UnaryPredicate getTaskPred(final String verb) {
    UnaryPredicate myTaskPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task) {
          Task mt = (Task)o;
          return mt.getVerb().equals(verb);
        }
        return false;
      }
    };
    return myTaskPred;
  }

  public void setupSubscriptions()
  {
    Vector parameters = getParameters();
    Enumeration pnum = parameters.elements();
    for (int idx=0;pnum.hasMoreElements(); idx++)
    {
      String param = (String)pnum.nextElement();
      System.out.println("Parm: "+param);
      if (idx==0)
        lat=Double.parseDouble(param);
      if (idx==1)
        lon=Double.parseDouble(param);
      if (idx==2)
        bearing=Double.parseDouble(param);
      if (idx==3)
        sleepTime=(long)Double.parseDouble(param);
    }

    targetSub = (IncrementalSubscription)subscribe(getTaskPred(myTargetingVerb));
    positionSub = (IncrementalSubscription)subscribe(getTaskPred(myPositionVerb));
  }

  public void execute() {
    if (debugLevel > 60) System.out.println("nsofUGSEmuPlugin Execute ");

    Enumeration enm;

    enm = targetSub.getAddedList();
    while (enm.hasMoreElements()) {
      processNewTargetingTask((Task)enm.nextElement());
    }

    enm = positionSub.getAddedList();
    while (enm.hasMoreElements()) {
      processNewPositionTask((Task)enm.nextElement());
    }


    enm = targetSub.getRemovedList();
    while (enm.hasMoreElements()) {
      processRemovedTargetingTask((Task)enm.nextElement());
    }


    if (debugLevel > 60) System.out.println("nsofUGSEmuPlugin Leaving execute now");
  }

  PeriodicDetection pd;
  Thread thd;
  Hashtable detectors=new Hashtable();
  private void processNewTargetingTask(Task task) {
    System.out.println("processNewTargetingTask "+task);

    pd = new PeriodicDetection(task);
    thd=new Thread(pd);
    thd.start();
    detectors.put(pd.getId(), pd);

  }

  private void processRemovedTargetingTask(Task task) {
    System.out.println("processRemovedTargetingTask "+task);

    pd = (PeriodicDetection)detectors.remove(task.getUID().toString());
    pd.quit();
  }
  class PeriodicDetection implements Runnable {
    boolean keepGoing=true;
    Allocation alloc;
    Task task;

    public PeriodicDetection(Task t) {
      task=t;
    }
    public PeriodicDetection(Task t, Allocation a) {
      task=t;
      alloc=a;
    }
    public String getId() { return task.getUID().toString(); }
    public void run() {
      while (keepGoing) {
        System.out.println("Periodic detection reporting after a delay of "+sleepTime+"(ms).");
        try { Thread.sleep(sleepTime); } catch (Exception ex) {}
        openTransaction();
//        System.out.println("Inside Tx.");
        reportDetection();
        closeTransaction();
      }
      System.out.println("Periodic detection stopped.");
    }
    private void reportDetection() {
      double theBearing=bearing+Math.random()*15;
      AllocationResult ar=makeAllocationResult(theBearing, hasDetection());
      if (alloc==null) {
        alloc=makeAllocation(task,
              theLDMF.createPrototype("AbstractAsset", "Stuff"),
              ar);
        publishAdd(alloc);
      } else {
        alloc.setEstimatedResult(ar);
        publishChange(alloc);
      }

      System.out.println();
      System.out.println("nsofUGSEmuPlugin Reporting Detection at "+theBearing+" to allocation for task "+task.getUID());
    }
    public void quit() {
      keepGoing=false;
    }
  }

  private boolean hasDetection() {
    return true;
  }


  private void processNewPositionTask(Task task) {
    System.out.println("processNewPositionTask "+task);

    //Allocation alloc=(Allocation)task.getPlanElement();
//    if (alloc!=null) {
      AllocationResult ar=makePositionAllocationResult();
      Allocation alloc=makeAllocation(task, theLDMF.createPrototype("AbstractAsset", "Stuff"), ar);
      publishAdd(alloc);

      System.out.println();
      System.out.println("nsofUGSEmuPlugin Reporting Position to allocation for task "+task.getUID());
//    }
  }

  private AllocationResult makeAllocationResult(double bearing,
    boolean wasSuccessful) {
      int []aspect_types
        = {Constants.Aspects.BEARING, Constants.Aspects.DETECTION};
      double []results = { bearing, ((wasSuccessful) ? 1 : 0)};
      return theLDMF.newAllocationResult(1.0, wasSuccessful,
        aspect_types, results);
  }

  private AllocationResult makePositionAllocationResult() {
      int []aspect_types
        = {Constants.Aspects.LATITUDE, Constants.Aspects.LONGITUDE};
      double []results = { lat, lon };
      return theLDMF.newAllocationResult(1.0, true,
        aspect_types, results);
  }

  /**
   * Create an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task task, Asset  asset, AllocationResult estAR) {
//    AllocationResult estAR = makeAllocationResult(-1, false);
    Allocation allocation =
     theLDMF.createAllocation(task.getPlan(), task, asset, estAR, Role.ASSIGNED);
    return allocation;
  }



}

