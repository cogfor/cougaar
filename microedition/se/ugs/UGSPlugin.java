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
import java.util.*;

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
public class UGSPlugin extends SimplePlugin {

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


  static final private String myTurretVerb
    = Constants.Robot.verbs[Constants.Robot.ROTATETURRET];


  private IncrementalSubscription targetSub;
  private IncrementalSubscription positionSub;
  private IncrementalSubscription targetingResourceSub;
  private IncrementalSubscription targetingAllocSub;
  private IncrementalSubscription turretResourceSub;
  private IncrementalSubscription turretAllocSub;
//  private IncrementalSubscription turretTaskSub;

  static int DEBUG_LOW=60;
  static int DEBUG_MED=80;
  static int DEBUG_HI=100;

  static int debugLevel=110;
  private double bearing = -1 ;
  private double oldBearing = -1 ;
  private double detection = -0.98765 ;
  private double oldDetection = -0.98765 ;
  private double lat=41.5;
  private double lon=-100.75;
  long sleepTime=4000;
  private double sweepDegrees=30;
  private double bearingResolution=3.6;

  Hashtable turretTasks=new Hashtable();

  /**
   * Return a UnaryPredicate which is true for tasks witht the given verb.
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

  UnaryPredicate getTargetingMCPred() {
    return getMCPred(Constants.Robot.meRoles[Constants.Robot.SONARSENSOR]);
  }

  UnaryPredicate getTurretMCPred() {
    return getMCPred(Constants.Robot.meRoles[Constants.Robot.TURRETCONTROLLER]);
  }

  UnaryPredicate getMCPred(final String myRole) {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroAgent) {
          MicroAgent m = (MicroAgent)o;
          String possible_roles = m.getMicroAgentPG().getCapabilities();
          StringTokenizer st = new StringTokenizer(possible_roles, ",");
          while (st.hasMoreTokens())
          {
            String a_role = st.nextToken();
            if(a_role.equals(myRole))
                 return true;
          }
        }
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getTurretAllocPred() {
    return getAllocPred(myTurretVerb);
  }
  UnaryPredicate getTargetingAllocPred() {
    return getAllocPred(myTargetingVerb);
  }
  UnaryPredicate getAllocPred(final String myVerb) {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          return a.getTask().getVerb().equals(myVerb);
        }
        return false;
      }
    };
    return myPred;
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
        oldBearing = bearing=Double.parseDouble(param);  // startingBearing
      if (idx==3)
        sleepTime=(long)Double.parseDouble(param);
      if (idx==4)
        sweepDegrees=(long)Double.parseDouble(param); // degrees to ending bearing
    }

    targetingResourceSub = (IncrementalSubscription)subscribe(getTargetingMCPred());
    targetingAllocSub = (IncrementalSubscription)subscribe(getTargetingAllocPred());
    targetSub = (IncrementalSubscription)subscribe(getTaskPred(myTargetingVerb));

    turretResourceSub = (IncrementalSubscription)subscribe(getTurretMCPred());
    turretAllocSub = (IncrementalSubscription)subscribe(getTurretAllocPred());
//    turretTaskSub = (IncrementalSubscription)subscribe(getTaskPred(myTurretVerb));

    positionSub = (IncrementalSubscription)subscribe(getTaskPred(myPositionVerb));
  }

  private MicroAgent getMCResource(MicroAgent mc, IncrementalSubscription sub) {
    MicroAgent mcRet=mc;
      if (mc==null) {
        Enumeration mcs = sub.elements();
        if (mcs.hasMoreElements()) {
          mc = (MicroAgent)mcs.nextElement();
          if (debugLevel > 60) System.out.println("nsofUGSPlugin Execute -- have resource "+mc.getUID()+" named "+mc.getName());
        }
      }
      return mc;
  }

  MicroAgent targetingResource=null;
  MicroAgent turretResource=null;
  public void execute() {
    if (debugLevel > 60) System.out.println("nsofUGSPlugin Execute ");

    targetingResource=getMCResource(targetingResource, targetingResourceSub);
    turretResource=getMCResource(turretResource, turretResourceSub);


    Enumeration enm;
    Enumeration tasks = targetSub.elements();
    while (tasks.hasMoreElements()) {
      Task t = (Task)tasks.nextElement();
      if (t.getPlanElement() != null) {
        //System.out.println("nsofUGSPlugin Execute -- task has planElement "+t);
        continue; // only want unallocated tasks
      }
      if (targetingResource == null) {
        System.out.println("nsofUGSPlugin Execute -- cannot allocate task because targetingResource is null ");
        // continue; // only want unallocated tasks
      } else {
        System.out.println("nsofUGSPlugin Execute -- allocing task to micro");
        Allocation allo = makeAllocation(t, targetingResource);
        publishAdd(allo);
      }

      if (turretResource!=null) {
        Task turretTask=makeTurretTask(myTurretVerb);
        publishAdd(turretTask);
        Allocation turAllo = makeAllocation(turretTask, turretResource);
        publishAdd(turAllo);
        turretTasks.put(t.getUID(), turretTask);
        System.out.println("nsofUGSPlugin Execute -- alloced turretTask to turretResource "+turretTask);
      } else {
        System.out.println("nsofUGSPlugin Execute -- turretResource is null -- not sending task");

      }
    }

    // get updates to bearing first
    processTurretAllocations();

    // then check for detections and update allocationResults with bearing & detection
    processDetectionAllocations();

    enm = positionSub.elements();
    while (enm.hasMoreElements()) {
      Task t=(Task)enm.nextElement();
      if (t.getPlanElement() != null) {
        //System.out.println("nsofUGSPlugin Execute -- pos task has planElement "+t);
        continue; // only want unallocated tasks
      }
      System.out.println("nsofUGSPlugin Execute -- calling processNewPositionTask");
      processNewPositionTask(t);
    }


    enm = targetSub.getRemovedList();
    while (enm.hasMoreElements()) {
      processRemovedTargetingTask((Task)enm.nextElement());
    }


    if (debugLevel > 60) System.out.println("nsofUGSPlugin Leaving execute now");
  }

  private void processTurretAllocations() {
    System.out.println("nsofUGSPlugin Execute -- processTurretAllocations-changed");
    processTurretAllocations(turretAllocSub.getChangedList());
    System.out.println("nsofUGSPlugin Execute -- processTurretAllocations-added");
    processTurretAllocations(turretAllocSub.getAddedList());
  }

  private void processTurretAllocations(Enumeration e) {
    while (e.hasMoreElements()) {
      System.out.println("nsofUGSPlugin Execute -- processTurretAllocation got an alloc");
      Allocation sa = (Allocation)e.nextElement();
      AllocationResult recvAR=sa.getReceivedResult();
      if (recvAR!=null&&recvAR.isDefined(Constants.Aspects.BEARING)) {
        bearing =
          recvAR.getValue(Constants.Aspects.BEARING)  ;
          System.out.print("new ");
      } else {
        System.out.println(" turret recvAR without bearing");
      }
      bearing = bearing+(Math.random()*bearingResolution);
      System.out.println("current bearing "+bearing);
    }
  }


  private void processDetectionAllocations() {
    System.out.println("nsofUGSPlugin Execute --processDetectionAllocations - changed");
    processDetectionAllocations(targetingAllocSub.getChangedList());
    System.out.println("nsofUGSPlugin Execute --processDetectionAllocations - added");
    processDetectionAllocations(targetingAllocSub.getAddedList());
  }
  private void processDetectionAllocations(Enumeration e) {
//    System.out.println("nsofUGSPlugin Execute --processDetectionAllocations");
//    Enumeration e = targetingAllocSub.getChangedList();
    while (e.hasMoreElements()) {
    System.out.println("nsofUGSPlugin Execute --processDetectionAllocations got a alloc -- xfering rec to est");
      Allocation sa = (Allocation)e.nextElement();
      AllocationResult recvAR=sa.getReceivedResult();
      oldDetection=detection;
      if (recvAR!=null&&recvAR.isDefined(Constants.Aspects.DETECTION)) {
        detection =
          recvAR.getValue(Constants.Aspects.DETECTION)  ;
      } else {
        System.out.println("no recvAR or no detection aspect--probably a new alloc--returning");
        return;
      }
      System.out.println("detection "+detection);
      System.out.println("oldDetection: "+oldDetection);
      System.out.println("bearing "+bearing);
      System.out.println("oldBearing: "+oldBearing);
      if (notEqual(detection, oldDetection)||notEqual(bearing, oldBearing)) {
        oldBearing=bearing;
        Date now=new Date();
        System.out.println("updating AR to: "+bearing+", "+detection+", "+now);
//        sa.setEstimatedResult(sa.getReceivedResult());
        sa.setEstimatedResult(makeAllocationResult(bearing,detection, now.getTime()));
        publishChange(sa);
      }
    }
  }

  private double getAspect(AllocationResult ar, int aspectType, double defaultVal) {
    double retVal=defaultVal;
//      if (!ar.isDefined(Constants.Aspects.DETECTION)) {
      if (!ar.isDefined(aspectType)) {
        if (debugLevel > DEBUG_HI) System.out.println("WARNING: Received AllocationResult which does not specify value for aspect "+aspectType+"--using default of "+retVal+".");
      } else {
        retVal=ar.getValue(aspectType);
      }
      return retVal;
  }

  PeriodicDetection pd;
  Thread thd;
  Hashtable detectors=new Hashtable();

  private void processRemovedTargetingTask(Task task) {
    System.out.println("processRemovedTargetingTask "+task);
    Task turretTask = (Task)turretTasks.get(task.getUID());
    System.out.println("processRemovedTargetingTask turretTask: "+turretTask);
    if (turretTask!=null) {
      System.out.println("processRemovedTargetingTask turretTask being removed...");
      publishRemove(turretTask);
      turretTasks.remove(task.getUID());
    }
  }

  private boolean notEqual(double d1, double d2) {
    return !(d1==d2
      || ((d1 < d2 + .0001) && (d1 > d2- .0001)));
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
      double theBearing=bearing ;
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
      System.out.println("nsofUGSPlugin Reporting Detection at "+theBearing+" to allocation for task "+task.getUID());
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
      System.out.println("nsofUGSPlugin Reporting Position to allocation for task "+task.getUID());
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
  private AllocationResult makeAllocationResult(double bearing,
    double detection) {
      int []aspect_types
        = {Constants.Aspects.BEARING, Constants.Aspects.DETECTION};
      double []results = { bearing, detection};
      return theLDMF.newAllocationResult(1.0, detection>.5,
        aspect_types, results);
  }

  private AllocationResult makeAllocationResult(double bearing,
    double detection, long dtime) {
      int []aspect_types
        = {Constants.Aspects.BEARING, Constants.Aspects.DETECTION, Constants.Aspects.DETECTION_TIME};
      double []results = { bearing, detection, dtime};
      return theLDMF.newAllocationResult(1.0, detection>.5,
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


  private Allocation makeAllocation(Task t, MicroAgent micro) {
    AllocationResult estAR = null;
    Allocation allocation =
      theLDMF.createAllocation(t.getPlan(), t, micro, estAR, Role.ASSIGNED);
    return allocation;
  }

//
//  private void addPreposition(NewTask t, String prep, String val) {
//    if (prep==null || val==null) return ;
//    NewPrepositionalPhrase npp= theLDMF.newPrepositionalPhrase();
//    npp.setPreposition(prep);
//    npp.setIndirectObject(val);
//    t.setPrepositionalPhrase(npp);
//  }
//
  private Task makeTurretTask(String verbText) {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(verbText));


      Vector prepositions = new Vector();

      NewPrepositionalPhrase npp;

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.BEARINGPREP]);
      npp.setIndirectObject(String.valueOf(bearing));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.VELOCITYPREP]);
      npp.setIndirectObject(String.valueOf(15));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.STARTANGLEPREP]);
      npp.setIndirectObject(String.valueOf(bearing));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.STOPANGLEPREP]);
      npp.setIndirectObject(String.valueOf(bearing+sweepDegrees));
      prepositions.add(npp);


      ((NewTask)t).setPrepositionalPhrases(prepositions.elements());
      System.out.println("...with preposition "
                                  +String.valueOf(0));



    return t;
  }
}

