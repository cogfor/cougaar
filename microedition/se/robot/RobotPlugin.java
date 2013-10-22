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

import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.se.domain.*;

import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.OrganizationPG;



/**
 * Plugin to control targeting.
 */
public class RobotPlugin extends SimplePlugin {

  static final private String myVerb
    = Constants.Robot.verbs[Constants.Robot.REPORTTARGET];
  static final private String LatPrep
    = Constants.Robot.prepositions[Constants.Robot.LATPREP];
  private String latPrepValue="Unknown";
  static final private String LonPrep
    = Constants.Robot.prepositions[Constants.Robot.LONPREP];
  private String lonPrepValue="Unknown";

  static final private String targetingVerb
    = Constants.Robot.verbs[Constants.Robot.DETECTTARGET];
  static final private String advanceVerb
    = Constants.Robot.verbs[Constants.Robot.ADVANCE];
  static final private String targetingPrep
    = Constants.Robot.prepositions[Constants.Robot.TURRETDIRECTIONPREP];

  private String targetingPrepValue = Constants.Robot.SEARCHFRONT;


  private IncrementalSubscription taskSub;
  private IncrementalSubscription reportPositionAllocs;
  private IncrementalSubscription allocSub;
  private IncrementalSubscription allocAdvanceSub;
  private IncrementalSubscription wayPointTaskSub;

  static int debugLevel=0;
  private double firstdetectionbearing = 0.0; //from true North
  private double detectionbearing = 0.0; //from true North
  private double detectiondifference = 45.0;
  private boolean idetected = false;

  private double robotlat = 0.0;
  private double robotlon = 0.0;
  private double robotheading = 0.0;
  private boolean robotpositionverified = false;
  private double destinationheading = 0.0; //degrees
  private double advanceincrement = 3000.0; //millimeters
  private double initialadvanceincrement = 3000.0; //used until robot heading is verified
  private double surveyspeed = 500.0; //millimeters per second
  private double waypointspeed = 500.0;
  private boolean haltadvance = false;
  private boolean waypointwasset = false;
  private boolean gottawhiff = false;
  private double surveyscanspeed = 3.6; //degrees per second (positive = CW)
  private double initstartangle = -135.0;
  private double initstopangle = 135.0; //as measured from front of robot (CW = positive)
  private double finesearchspeed = 3.6; // degrees per second
  private double finesearchwedge = 180.0; //degrees
  private double closerlook = 0.0;
  private double getcloserspeed = 500.0;

  private NewTask flashlighttask = null;
  private NewTask imagingtask = null;

  /**
   * Return a UnaryPredicate which is true for ReportDection tasks.
   */
  UnaryPredicate getTaskPred() {
    UnaryPredicate taskPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        boolean ret=false;
        if (o instanceof Task) {
          Task mt = (Task)o;
          ret= (mt.getVerb().equals(myVerb));
        }
        return ret;
      }
    };
    return taskPred;
  }

  UnaryPredicate getWayPointTaskPred() {
    UnaryPredicate taskPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        boolean ret=false;
        if (o instanceof Task) {
          Task mt = (Task)o;
          ret= (mt.getVerb().equals(Constants.Robot.verbs[Constants.Robot.SETWAYPOINT]));
        }
        return ret;
      }
    };
    return taskPred;
  }

  UnaryPredicate getAllocPred() {
    UnaryPredicate myAllocPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation
             && ((Allocation)o).getEstimatedResult()!=null ) {
          Task mt = ((Allocation)o).getTask();
          return mt.getVerb().equals(targetingVerb);
        }
        return false;
      }
    };
    return myAllocPred;
  }

  UnaryPredicate getAdvanceAllocPred() {
    UnaryPredicate myAllocPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation)
        {
          Task mt = ((Allocation)o).getTask();
          return mt.getVerb().equals(advanceVerb);
        }
        return false;
      }
    };
    return myAllocPred;
  }

  private double extractparameter(String param)
  {
    int indx = param.indexOf("=");
    if (indx < 0)
    {
      System.out.println("\n !!!! Error in extractparameter for "+param);
      return 0;
    }

    String valstring = (param.substring(indx+1)).trim();
    Double temp = new Double(valstring);
    return(temp.doubleValue());
  }

  public void setupSubscriptions()
  {
    Vector parameters = getParameters();
    Enumeration pnum = parameters.elements();
    while (pnum.hasMoreElements())
    {
      String param = (String)pnum.nextElement();
      if (param.toLowerCase().indexOf("destinationheading") >= 0)
         destinationheading = extractparameter(param);
      if (param.toLowerCase().indexOf("scanspeed") >= 0)
         surveyscanspeed = extractparameter(param);
      if (param.toLowerCase().indexOf("surveyspeed") >= 0)
         surveyspeed = extractparameter(param);
      if (param.toLowerCase().indexOf("advanceincrement") >= 0)
         advanceincrement = extractparameter(param);
    }

    taskSub = (IncrementalSubscription)subscribe(getTaskPred());
    wayPointTaskSub = (IncrementalSubscription)subscribe(getWayPointTaskPred());
    allocSub = (IncrementalSubscription)subscribe(getAllocPred());
    allocAdvanceSub = (IncrementalSubscription)subscribe(getAdvanceAllocPred());

    reportPositionAllocs = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        return false;
      }
    });
  }

  public void execute()
  {
    if (debugLevel > 60) System.out.println("RobotPlugin Execute ");

    Enumeration allocEnum;

    allocEnum = allocAdvanceSub.getChangedList();
    while (allocEnum.hasMoreElements()) {
      processAdvanceAllocation((Allocation)allocEnum.nextElement());
    }

    allocEnum = allocSub.getChangedList();
    while (allocEnum.hasMoreElements()) {
      processTargetingAllocation((Allocation)allocEnum.nextElement());
    }

    Enumeration taskEnum;
    taskEnum = taskSub.getRemovedList();
    while (taskEnum.hasMoreElements()) {
      Task mt = (Task)taskEnum.nextElement();
      processRemovedTask(mt);
    }

    taskEnum = taskSub.getAddedList();
    while (taskEnum.hasMoreElements()) {
      Task mt = (Task)taskEnum.nextElement();
      addTasks(mt);
    }

    taskEnum = wayPointTaskSub.getAddedList();
    while (taskEnum.hasMoreElements()) {
      Task mt = (Task)taskEnum.nextElement();
      makeAdvanceTask(mt);
    }

    Enumeration alloc_enum = reportPositionAllocs.getChangedList();
    while (alloc_enum.hasMoreElements()) {
      Allocation a = (Allocation)alloc_enum.nextElement();
      AllocationResult ar = a.getReceivedResult();
      if (ar == null)
        continue;
      if (ar.isSuccess() == false)
        continue;

      robotlat = ar.getValue(Constants.Aspects.LATITUDE);
      robotlon = ar.getValue(Constants.Aspects.LONGITUDE);
      robotheading = ar.getValue(Constants.Aspects.HEADING);

      //originally reported in billionths. MicroTask plugin adjusted by 1000.0
      robotlat = robotlat*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);
      robotlon = robotlon*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);
      robotheading = robotheading*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);

      robotpositionverified = true;

      //System.out.println("Robot Plugin position: " +robotlat+" "+robotlon+" "+robotheading);

    }

    if (debugLevel > 60) System.out.println("RobotPlugin Leaving execute now");
  }

  private void processAdvanceAllocation(Allocation alloc)
  {
    if(waypointwasset)
    {
      System.out.println("Waypoint Task complete...take picture now");
      makeImagingTask();
      return;
    }

    //System.out.println("RobotPlugin: Advancement task finished: create a targeting task" +alloc.getTask());

    if(gottawhiff)
    {
      makeTargetingTask(alloc, initstartangle, initstopangle, finesearchspeed);
    }
    else
    {
      makeTargetingTask(alloc, initstartangle, initstopangle, surveyscanspeed);
    }
  }

  private void processTargetingAllocation(Allocation alloc)
  {
    if(waypointwasset)
    {
      return;
    }
    //System.out.println("processTargetingAllocation " +alloc+"\n"+alloc.getTask());
    AllocationResult ar=null;
    if (alloc.getReportedResult()!=null) {
      ar=alloc.getReportedResult();
    }
    if (ar==null) {
      if (alloc.getEstimatedResult()!=null) {
        ar=alloc.getEstimatedResult();
      }
    }

    if (ar==null)
    {
      System.out.println("return due to (ar==null) ");
      return;
    }

    if (!ar.isSuccess()) //create a new advance task
    {
      if(gottawhiff)
      {
        System.out.println("RobotPlugin: whiff was false. Resuming search.");
        gottawhiff = false; //reset this if 'twere set on a first successful target allocation
        makeFlashlightTask("off");
      }
      makeAdvanceTask(alloc, destinationheading, advanceincrement, surveyspeed);
      return;
    }

    try
    {
      detectionbearing = ar.getValue(Constants.Aspects.BEARING);
      detectionbearing += robotheading; //make absolute
      if(detectionbearing >= 360.0) detectionbearing -= 360.0;
      if(detectionbearing <= -360.0) detectionbearing += 360.0;
    }
    catch (Exception ex)
    {
      System.out.println("Warning: Cannot obtain bearing from successful targeting controller allocationResult.");
      return;
    }

    if (gottawhiff == false)
    {
      gottawhiff = true; //first whiff

      System.out.println("RobotPlugin Reporting Initial Detection at "+detectionbearing);
      firstdetectionbearing = detectionbearing;

      //create another targeting task to make sure
      makeFlashlightTask("flashing");
      makeAdvanceTask(alloc, firstdetectionbearing, closerlook, getcloserspeed); //turn toward source and target again
    }
    else
    {
      //at this point robot has turned toward the firstdetectionbearing. If valid, the current
      //detection bearing and robot heading should be similar
      System.out.println("RobotPlugin Has Secondary Detection at "+detectionbearing);
      if(Math.abs(detectionbearing - robotheading) < detectiondifference)
      {
        //looks like a good one.
        idetected = true;
        stopObjective((NewWorkflow)alloc.getTask().getWorkflow());
        makeFlashlightTask("on");

        //tell houston
        AllocationResult myar=makeAllocationResult(detectionbearing, true);

        Enumeration taskEnum = taskSub.elements();
        while (taskEnum.hasMoreElements())
        {
          Task mt = (Task)taskEnum.nextElement();
          Expansion myExpansion=((Expansion)mt.getPlanElement());
          if (myExpansion!=null)
          {
            myExpansion.setEstimatedResult(myar);
            publishChange(myExpansion);
          }
          else
          {
            System.out.println("Have task with myVerb without Expansion.  verb: "+mt.getVerb()+" task: "+mt);
          }

          System.out.println("RobotPlugin Reporting Detection at "+detectionbearing+" true North");
          System.out.flush();
        }
      }
      else
      {
        //try to pinpoint again...
        System.out.println("RobotPlugin: Primary and secondary detection mismatch. Resuming search.");
        //gottawhiff = false;
        //makeFlashlightTask("off");
        //makeAdvanceTask(alloc, destinationheading, advanceincrement, surveyspeed);
        firstdetectionbearing = detectionbearing;
        gottawhiff = true;
        makeFlashlightTask("flashing");
        makeAdvanceTask(alloc, firstdetectionbearing, closerlook, getcloserspeed); //turn toward source and target again
      }
    }
  }

  private void stopObjective(NewWorkflow wkfi)
  {
    if ( wkfi == null)
    {
      System.out.println("stopObjective workflow is null");
      return;
    }

    Task t;
    Vector v = new Vector();

    Enumeration enum=wkfi.getTasks();
    while (enum.hasMoreElements())
    {
      t=(Task)enum.nextElement();
      v.add(t);
    }
    enum = v.elements();
    while (enum.hasMoreElements())
    {
      t=(Task)enum.nextElement();
      System.out.println("RobotPlugin: stopObjective removing tasks " +t);
      wkfi.removeTask(t);
      publishRemove(t);
    }

    if(flashlighttask != null)
    {
      publishRemove(flashlighttask);
      flashlighttask = null;
    }
  }

  private AllocationResult makeAllocationResult(double bearing,
    boolean wasSuccessful) {
      int []aspect_types
        = {Constants.Aspects.BEARING, Constants.Aspects.DETECTION};
      double []results = { bearing, ((wasSuccessful) ? 1 : 0)};
      return theLDMF.newAllocationResult(1.0, wasSuccessful,
        aspect_types, results);
  }

  private void processRemovedTask(Task mt)
  {
      System.out.println("Removing task "+mt.getUID()+" with verb "+mt.getVerb());
      stopObjective((NewWorkflow)mt.getWorkflow());
  }

  class SubTasks {
    Task targetingTask;
    Task advanceTask;

    void setTargetingTask(Task t) { targetingTask=t; }
    void setAdvanceTask(Task t) { advanceTask=t; }
    Task getTargetingTask() { return targetingTask; }
    Task getAdvanceTask() { return advanceTask; }

    public String toString() {
      String tar="null";
      String adv="null";
      if (targetingTask!=null) tar=targetingTask.getUID()+" "+targetingTask.getVerb().toString();
      if (advanceTask!=null) adv=advanceTask.getUID()+" "+advanceTask.getVerb().toString();
      return "SubTasks: Advance: "+adv+" targeting: "+tar;
    }
  }

  /*  ===================================================================== */
  private void makeTargetingTask(Allocation alloc, double startangle, double stopangle, double scanspeed)
  {
      NewWorkflow wf = (NewWorkflow)alloc.getTask().getWorkflow();

      if(wf == null)
        return; //can happen if advance task was created via PSP

      //remove the task
      wf.removeTask(alloc.getTask());
      publishRemove(alloc.getTask());

      Task parenttask = wf.getParentTask();

      NewTask subTask = (NewTask)makeTargetingTask(parenttask, startangle, stopangle, scanspeed);
      subTask.setParentTask(parenttask);
      subTask.setWorkflow(wf);
      subTask.setPlan(parenttask.getPlan());
      subTask.setDirectObject(parenttask.getDirectObject());
      publishAdd(subTask);
      wf.addTask(subTask);

      System.out.println("Targeting task created" +subTask);
  }

  private NewTask makeTargetingTask(Task mt, double startangle, double stopangle, double scanspeed)
  {

    NewTask subTask;

    String startAnglePrepValue = Double.toString(startangle);
    String stopAnglePrepValue = Double.toString(stopangle);
    String scanSpeedPrepValue = Double.toString(scanspeed);

    subTask = (NewTask)makeTask(targetingVerb);
    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.VELOCITYPREP]);
    npp.setIndirectObject(scanSpeedPrepValue);
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.STARTANGLEPREP]);
    npp.setIndirectObject(startAnglePrepValue);
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.STOPANGLEPREP]);
    npp.setIndirectObject(stopAnglePrepValue);
    prepositions.add(npp);

    subTask.setPrepositionalPhrases(prepositions.elements());

    return subTask;
  }

  private void allocate(Task t, Organization org) {
      Allocation alloc=makeAllocation(t, org);
      publishAdd(alloc);
  }


  /**
   * Create an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task task, Organization org) {
    AllocationResult estAR = makeAllocationResult(-1, false);
    Allocation allocation =
     theLDMF.createAllocation(task.getPlan(), task, org, estAR, Role.ASSIGNED);
    return allocation;
  }

  private void addPreposition(NewTask t, String prep, String val) {
    if (prep==null || val==null) return ;
    NewPrepositionalPhrase npp= theLDMF.newPrepositionalPhrase();
    npp.setPreposition(prep);
    npp.setIndirectObject(val);
    t.setPrepositionalPhrase(npp);
  }

  private Task makeTask(String verbText) {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(verbText));
    return t;
  }

  private void makeFlashlightTask(String fstate)
  {
    //System.out.println("Making Flashlight task..." +fstate);
/*
    if(flashlighttask != null) publishRemove(flashlighttask);

    flashlighttask = theLDMF.newTask();
    flashlighttask.setPlan(theLDMF.getRealityPlan());
    flashlighttask.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.CONTROLFLASHLIGHT]));

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition("LightingMode");
    npp.setIndirectObject(fstate);

    flashlighttask.setPrepositionalPhrase(npp);

    publishAdd(flashlighttask);
*/
  }

  private void makeImagingTask()
  {
    //System.out.println("Making Imaging task...");

    if(imagingtask != null) publishRemove(imagingtask);

    imagingtask = theLDMF.newTask();
    imagingtask.setPlan(theLDMF.getRealityPlan());
    imagingtask.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.GETIMAGE]));

    publishAdd(imagingtask);

  }

  private void addTasks(Task mt)
  {
    waypointwasset = false;
    idetected = false;

    NewTask subTask;

    NewWorkflow nwf = theLDMF.newWorkflow();
    nwf.setParentTask(mt);

    //compute latitude and longitude far in front of robot' current heading.
    //subTask = (NewTask)makeTargetingTask(mt, initstartangle, initstopangle, surveyscanspeed);
    subTask = (NewTask)makeAdvanceTask(mt, destinationheading, advanceincrement, surveyspeed);
    subTask.setParentTask(mt);
    subTask.setWorkflow(nwf);
    subTask.setPlan(mt.getPlan());
    subTask.setDirectObject(mt.getDirectObject());
    publishAdd(subTask);
    nwf.addTask(subTask);


    AllocationResult estAR = null;
    Expansion expansion =
      theLDMF.createExpansion(mt.getPlan(), mt, nwf, estAR);
    publishAdd(expansion);

  }

  private static final double rangestandoff = 2500.0; //mm to stand off from

  private void makeAdvanceTask(Task mt)
  {
    String latString = "none";
    String lonString = "none";
    for (Enumeration enum= mt.getPrepositionalPhrases();
            enum.hasMoreElements(); )
    {
      PrepositionalPhrase preps=(PrepositionalPhrase)enum.nextElement();
      if (preps!=null)
      {
        if (preps.getPreposition().equalsIgnoreCase(LatPrep))
           latString=(String)preps.getIndirectObject();
        if (preps.getPreposition().equalsIgnoreCase(LonPrep))
          lonString=(String)preps.getIndirectObject();
      }
    } // end-for

    if(latString.equalsIgnoreCase("none") || lonString.equalsIgnoreCase("none"))
    {
      System.err.println("makeAdvanceTask(mt): Unable to get lat lon from prepositions");
      return;
    }
    else
    {
      NewTask subTask;

      NewWorkflow nwf = theLDMF.newWorkflow();
      nwf.setParentTask(mt);

      RBCoordinate rb = EmitterLocator.FindRangeBearing(robotlat, robotlon,
                                Double.parseDouble(latString),
                                Double.parseDouble(lonString));

      //double mmdistance = (rb.range*1000.0) - rangestandoff;
      double mmdistance = 0.0;
      //if(rb.range < 1) mmdistance = -2000.0; //back off if too close

      double heading = rb.bearing;

      if(idetected) heading = detectionbearing;

      subTask = makeAdvanceTask(mt, heading, mmdistance, waypointspeed);

      subTask.setParentTask(mt);
      subTask.setWorkflow(nwf);
      subTask.setPlan(mt.getPlan());
      subTask.setDirectObject(mt.getDirectObject());
      publishAdd(subTask);
      nwf.addTask(subTask);

      AllocationResult estAR = null;
      Expansion expansion =
        theLDMF.createExpansion(mt.getPlan(), mt, nwf, estAR);
      publishAdd(nwf);
      publishAdd(expansion);

      waypointwasset = true;

      System.out.println("Waypoint advanced task created" +subTask);

    }
  }

  private void makeAdvanceTask(Allocation alloc, double heading, double distance, double speed)
  {
      NewWorkflow wf = (NewWorkflow)alloc.getTask().getWorkflow();
      //remove the task
      wf.removeTask(alloc.getTask());
      publishRemove(alloc.getTask());

      Task parenttask = wf.getParentTask();

      NewTask subTask = (NewTask)makeAdvanceTask(parenttask, heading, distance, speed);
      subTask.setParentTask(parenttask);
      subTask.setWorkflow(wf);
      subTask.setPlan(parenttask.getPlan());
      subTask.setDirectObject(parenttask.getDirectObject());
      publishAdd(subTask);
      wf.addTask(subTask);

      System.out.println("Advance task created" +subTask);
  }

  private NewTask makeAdvanceTask(Task mt, double heading, double distance, double speed)
  {
    NewTask subTask;

    //compute degrees rotation
    double degreesrotation = heading - robotheading;
    double mmtranslation = distance;
    if(robotpositionverified == false)
    {
      degreesrotation = 0; //reckon straight until valid heading perceived.
      mmtranslation = initialadvanceincrement;
    }
    else
    {
      //spin CW or CCW as less as possible
       if(degreesrotation < -180.0) degreesrotation += 360.0;
       if(degreesrotation >= 180.0) degreesrotation -= 360.0;
       //System.out.println("makeAdvanceTask "+degreesrotation+" "+mmtranslation+" "+speed);
    }

    String rotatePrepValue = Double.toString(degreesrotation);
    String translatePrepValue = Double.toString(mmtranslation);
    String velocityPrepValue = Double.toString(speed);

    subTask = (NewTask)makeTask(advanceVerb);

    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.VELOCITYPREP]);
    npp.setIndirectObject(velocityPrepValue);
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.ROTATEPREP]);
    npp.setIndirectObject(rotatePrepValue);
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.TRANSLATEPREP]);
    npp.setIndirectObject(translatePrepValue);
    prepositions.add(npp);

    subTask.setPrepositionalPhrases(prepositions.elements());

    //System.out.println("makeAdvanceTask "+subTask);

    return subTask;
  }
}

  class EmitterLocator
  {

    static public double normalizeAngle(double a) {
      double twopi = 2.*Math.PI;
      while (a < 0.0)
        a += twopi;
      while (a >= twopi)
        a -= twopi;
      return a;
    }


    static private RobotPG GetRobotStatus(Asset o)
    {
      Enumeration pgs = o.getOtherProperties();
      while (pgs.hasMoreElements())
      {
        PropertyGroup pg = (PropertyGroup)pgs.nextElement();
        if (pg instanceof RobotPG)
        {
          RobotPG rpg = (RobotPG)pg;
          return rpg;
        }
      }
      return null;
    }


    static public RBCoordinate FindRangeBearing(double lat1, double lon1, double lat2, double lon2)
    {
      // compute range bearing from lat1, lon1 to lat2, lon2
      RBCoordinate rbc = new RBCoordinate();

      double dlat = Math.toRadians(lat2 - lat1);
      double dlon = Math.toRadians(lon2 - lon1);

      double yd = Constants.Geophysical.EARTH_RADIUS_METERS * dlat;
      double xd = Constants.Geophysical.EARTH_RADIUS_METERS * Math.cos(0.5*(lat1 + lat2)) * dlon;

      rbc.range = Math.sqrt(xd*xd+yd*yd); //in meters
      rbc.bearing = Math.atan2(xd, yd); //in radians
      rbc.bearing = Math.toDegrees(rbc.bearing); //in degrees CW from true north

      return rbc;
    }
  }
