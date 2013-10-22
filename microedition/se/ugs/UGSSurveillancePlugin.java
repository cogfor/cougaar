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

/*
  This class is the Surveillance Manager for the NSOF demo.
  It is based upon the SurveillancePlugin.
*/
import org.cougaar.planning.plugin.legacy.SimplePlugin;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.OrganizationPG;
import org.cougaar.microedition.shared.Constants;

import java.util.*;
import java.lang.*;


public class UGSSurveillancePlugin extends SimplePlugin
{
  static boolean underway = false;
  static boolean firsttimedetection = false;
  static boolean waypointsset = false;

  private String robotstartlook = Constants.Robot.SEARCHFRONT;

  IncrementalSubscription robotOrgs;
  IncrementalSubscription reportTargetTasks;
  IncrementalSubscription reportPositionTasks;
  IncrementalSubscription reportTargetAllocs;
  IncrementalSubscription reportPositionAllocs;
  IncrementalSubscription setWaypointTasks;
  IncrementalSubscription setGoTask; //PSP originates this task to put things in motion

//  DCL myDCL=new DCL();

  class PositionCoordinate
  {
    public double latitude; //decimal degrees, North positive
    public double longitude; //decimal degrees, East positive

    public PositionCoordinate()
    {
      latitude = 0.0;
      longitude = 0.0;
    }
    public PositionCoordinate(double lat, double lon)
    {
      latitude = lat;
      longitude = lon;
    }
  }

  class RBCoordinate
  {
    public double range; //meters
    public double bearing; //degrees CW from True North

    public RBCoordinate()
    {
      range = 0.0;
      bearing = 0.0;
    }

    public RBCoordinate(double r, double b)
    {
      range = r;
      bearing = b;
    }
  }

  //this predicate asks for all organizations registered as surveillance providers
  UnaryPredicate SurveillanceProviderPredicate()
  {
    UnaryPredicate myPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        boolean ret = false;
        if (o instanceof Organization)
        {
          Organization org = (Organization)o;
          OrganizationPG orgPG = org.getOrganizationPG();
          ret = orgPG.inRoles(Role.getRole(Constants.Robot.roles[Constants.Robot.SURVEILLANCEPROVIDER]));
        }
        return ret;
      }
    };
    return myPred;
  }

  UnaryPredicate ReportPositionAllocsPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        return false;
      }
    };
    return newPred;
  }

  UnaryPredicate ReportTargetTasksPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Task)
          return ((Task)o).getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTTARGET]);
        return false;
      }
    };
    return newPred;
  }

  UnaryPredicate GetGoTaskPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Task)
          return ((Task)o).getVerb().equals(Constants.Robot.verbs[Constants.Robot.STARTSYSTEM]);
        return false;
      }
    };
    return newPred;
  }

  UnaryPredicate ReportTargetAllocsPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTTARGET]);
        return false;
      }
    };
    return newPred;
  }

  UnaryPredicate ReportPositionTasksPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Task)
          return ((Task)o).getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        return false;
      }
    };
    return newPred;
  }

  UnaryPredicate SetWaypointTasksPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Task)
          return ((Task)o).getVerb().equals(Constants.Robot.verbs[Constants.Robot.SETWAYPOINT]);
        return false;
      }
    };
    return newPred;
  }
  // subscribe to tasks and organizations
  protected void setupSubscriptions()
  {

    System.out.println("nsofSurveillancePlugin::setupSubscriptions");

    robotOrgs = (IncrementalSubscription)subscribe(SurveillanceProviderPredicate());
    reportTargetTasks = (IncrementalSubscription)subscribe(ReportTargetTasksPredicate());
    reportTargetAllocs = (IncrementalSubscription)subscribe(ReportTargetAllocsPredicate());
    reportPositionTasks = (IncrementalSubscription)subscribe(ReportPositionTasksPredicate());
    reportPositionAllocs = (IncrementalSubscription)subscribe(ReportPositionAllocsPredicate());
//    setWaypointTasks = (IncrementalSubscription)subscribe(SetWaypointTasksPredicate());

    setGoTask = (IncrementalSubscription)subscribe(GetGoTaskPredicate());

  }

  private void makeReportPositionTask(Asset o)
  {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]));

    publishAdd(t);

    AllocationResult estAR = null;
    Allocation a =
      theLDMF.createAllocation(t.getPlan(), t, o, estAR, Role.ASSIGNED);
    publishAdd(a);
  }

  private void makeTargetingTask(Asset o, PositionCoordinate pos, RBCoordinate rb)
  {
    System.out.println("Make Targeting Tasks...");

    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.REPORTTARGET]));

//    if(pos != null && rb != null)
//    {
      Vector prepositions = new Vector();

      NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LATPREP]);
//      npp.setIndirectObject(String.valueOf(pos.latitude));
      npp.setIndirectObject("0");
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LONPREP]);
//      npp.setIndirectObject(String.valueOf(pos.longitude));
      npp.setIndirectObject(String.valueOf(0));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.BEARINGPREP]);
//      npp.setIndirectObject(String.valueOf(rb.bearing));
      npp.setIndirectObject(String.valueOf(0));
      prepositions.add(npp);

      ((NewTask)t).setPrepositionalPhrases(prepositions.elements());
      System.out.println("...with prepositions "
                                  +String.valueOf(0)+" "
                                  +String.valueOf(0)+" "
                                  +String.valueOf(0));
//      System.out.println("...with prepositions "
//				  +String.valueOf(pos.latitude)+" "
//				  +String.valueOf(pos.longitude)+" "
//				  +String.valueOf(rb.bearing));
//    }
//    else
//    {
//      System.out.println("...without prepositions ");
//    }

    publishAdd(t);

    AllocationResult estAR = null;
    Allocation a =
      theLDMF.createAllocation(t.getPlan(), t, o, estAR, Role.ASSIGNED);
    publishAdd(a);
  }

  private void makeWaypointTask(Asset o, PositionCoordinate pos)
  {
    System.out.println("Makeing Waypoint Task...");

    if(pos == null)
    {
      System.out.println("makeWaypointTask error. No position specified!");
      return;
    }

    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.SETWAYPOINT]));

    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LATPREP]);
    npp.setIndirectObject(String.valueOf(pos.latitude));
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LONPREP]);
    npp.setIndirectObject(String.valueOf(pos.longitude));
    prepositions.add(npp);

    ((NewTask)t).setPrepositionalPhrases(prepositions.elements());
    System.out.println("...with prepositions "
                                +String.valueOf(pos.latitude)+" "
                                +String.valueOf(pos.longitude));

    publishAdd(t);

    AllocationResult estAR = null;
    Allocation a =
      theLDMF.createAllocation(t.getPlan(), t, o, estAR, Role.ASSIGNED);
    publishAdd(a);
  }

  private RobotPG GetRobotStatus(Asset o)
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

  private UGSPG GetUGSPG(Asset o)
  {
    Enumeration pgs = o.getOtherProperties();
    while (pgs.hasMoreElements())
    {
      PropertyGroup pg = (PropertyGroup)pgs.nextElement();
      if (pg instanceof UGSPG)
      {
        UGSPG upg = (UGSPG)pg;
        return upg;
      }
    }
    return null;
  }

  private void setTargetingTasks(PositionCoordinate pc, RBCoordinate rb)
  {
    // assign new reporttarget tasks for non detecting robots
    System.out.println("nsofSurveillancePlugin:: Set targeting tasks..");
    Enumeration e = robotOrgs.elements();
    while (e.hasMoreElements())
    {
      Asset o = (Asset)e.nextElement();

      if (detectors.contains(o)) // for detectors dir=>none (suspend searching).
        continue; // don't start a targeting task on detector.

      makeTargetingTask(o, pc, rb);
    }
  }

  private void setWaypointTasks(PositionCoordinate pc)
  {
    // assign new reporttarget tasks for non detecting robots
    System.out.println("nsofSurveillancePlugin:: Set Waypoint tasks..");
    Enumeration e = robotOrgs.elements();
    while (e.hasMoreElements())
    {
      Asset o = (Asset)e.nextElement();

      makeWaypointTask(o, pc);
    }
  }

  private void removeTargetingTasks()
  {
    System.out.println("nsofSurveillancePlugin::remove target reporting tasks");
    Enumeration e;

    for (e=reportTargetTasks.elements();e.hasMoreElements();)
    {
      publishRemove((Task)e.nextElement());
    }
  }

  private void removeWaypointTasks()
  {
    System.out.println("nsofSurveillancePlugin::remove waypoint tasks");
    Enumeration e;

    for (e=setWaypointTasks.elements();e.hasMoreElements();)
    {
      publishRemove((Task)e.nextElement());
    }
  }

  private void removePositioningTasks()
  {
    System.out.println("nsofSurveillancePlugin::remove position reporting tasks");
    Enumeration e;

    for (e=reportPositionTasks.elements();e.hasMoreElements();)
      publishRemove((Task)e.nextElement());
  }

  private void stopDetectorRobots()
  {
    Enumeration e=null;

    e = reportTargetAllocs.elements();
    if (e.hasMoreElements())
    {
      while (e.hasMoreElements())
      {
        Allocation a = (Allocation)e.nextElement();
        Organization o = (Organization)a.getAsset();
        if (detectors.contains(o)) // for detectors dir=>none (suspend searching).
        {
          Task t = a.getTask();
          System.out.println("nsofSurveillancePlugin::stopping targeting task on detector " +t.getUID());
          publishRemove(t);
        }
      }
    }

    e = reportPositionAllocs.elements();
    if (e.hasMoreElements())
    {
      while (e.hasMoreElements())
      {
        Allocation a = (Allocation)e.nextElement();
        Organization o = (Organization)a.getAsset();
        if (detectors.contains(o)) // for detectors dir=>none (suspend searching).
        {
          Task t = a.getTask();
          System.out.println("nsofSurveillancePlugin::stopping positioning task on detector " +t.getUID());
          publishRemove(t);
        }
      }
    }
  }

  private void restartDetectorRobots()
  {
   System.out.println("nsofSurveillancePlugin::restartDetectorRobots");
   Enumeration e = robotOrgs.elements();
    while (e.hasMoreElements())
    {
       Asset o = (Asset)e.nextElement();
       if (detectors.contains(o))
       {
          makeReportPositionTask(o);
          makeTargetingTask(o, null, null);
       }
    }
  }


  private boolean computeTargetLocation(PositionCoordinate t)
  {
    if(detectors.size() < 2)
    {
      System.out.println("computeTargetLocation hasn't enough detectors!!");
      return false;
    }

    //get first detecting robot
    Organization o = (Organization)detectors.elementAt(0);
    RobotPG r1 = GetRobotStatus(o);

    //get second detecting robot
    o = (Organization)detectors.elementAt(1);
    RobotPG r2 = GetRobotStatus(o);

    if (FindIntersection(r1.getLatitude(), r1.getLongitude(), r1.getBearing(),
                         r2.getLatitude(), r2.getLongitude(), r2.getBearing(),
                         t))
    {
      System.out.println("Location of Target: " +t.latitude +" " +t.longitude);
      return true;
    }
    else
    {
      System.out.println("Location Not Possible");
      return false;
    }
  }

  //place to hold robot Orgs that have detected the target
  static Vector detectors = new Vector();
  long bogusTime=0;

  private void makeRobotPropertyGroup(Organization org) {
    NewRobotPG new_robot_pg = (NewRobotPG)theLDMF.createPropertyGroup("RobotPG");
    new_robot_pg.setLatitude(-999.0);
    new_robot_pg.setLongitude(-999.0);
    new_robot_pg.setVelocity(-999.0);
    new_robot_pg.setHeading(-999.0);
    new_robot_pg.setDetection(false);
    new_robot_pg.setImageAvailable(false);
    new_robot_pg.setFlashlightOn(false);
    org.addOtherPropertyGroup(new_robot_pg);
    publishChange(org);
  }

  private void makeUGSPropertyGroup(Organization org) {
    int length=100;
    NewUGSPG upg = (NewUGSPG)theLDMF.createPropertyGroup("UGSPG");
    upg.setName(org.getItemIdentificationPG().getItemIdentification());
    upg.setBearing(new double[length]);
    upg.setLat(-999.99);
    upg.setLon(-999.99);
    upg.setStatus(status.setAlive(true).getValue());
    upg.setEntries(0);
    upg.setLen(length);
    upg.setDetTime(new double[length]);
    org.addOtherPropertyGroup(upg);
    publishChange(org);
  }

  Status status=new Status();
  class Status {

      final String ALIVE_ONLINE="ALIVE_ONLINE;";
      final String ALIVE_OFFLINE="ALIVE_OFFLINE;";
      String statusAlive=ALIVE_OFFLINE;
      final String REP_POS_ON="REP_POS_TASK_ON;";
      final String REP_POS_OFF="REP_POS_TASK_OFF;";
      String statusRepPosValue=REP_POS_OFF;
      final String REP_TAR_ON="REP_TAR_TASK_ON;";
      final String REP_TAR_OFF="REP_TAR_TASK_OFF;";
      String statusRepTarValue=REP_TAR_OFF;
      final String TAR_AR_RCVD_ON="TAR_AR_RCVD_ON;";
      final String TAR_AR_RCVD_OFF="TAR_AR_RCVD_OFF;";
      String statusTarARRecvValue=TAR_AR_RCVD_OFF;
      final String POS_AR_RCVD_ON="POS_AR_RCVD_ON;";
      final String POS_AR_RCVD_OFF="POS_AR_RCVD_OFF;";
      String statusPosARRecvValue=POS_AR_RCVD_OFF;

      String status=getValue();

      Status setAlive(boolean val) {
        if (val) statusAlive=ALIVE_ONLINE;
        else statusAlive=ALIVE_OFFLINE;
        status = getValue();
        return this;
      }
      Status setPosTask(boolean val) {
        if (val) statusRepPosValue=REP_POS_ON;
        else statusRepPosValue=REP_POS_OFF;
        status = getValue();
        return this;
      }
      Status setPosAR(boolean val) {
        if (val) statusPosARRecvValue=POS_AR_RCVD_ON;
        else statusPosARRecvValue=POS_AR_RCVD_OFF;
        status = getValue();
        return this;
      }
      Status setTarTask(boolean val) {
        if (val) statusRepTarValue=REP_TAR_ON;
        else statusRepTarValue=REP_TAR_OFF;
        status = getValue();
        return this;
      }
      Status setTarAR(boolean val) {
        if (val) statusTarARRecvValue=TAR_AR_RCVD_ON;
        else statusTarARRecvValue=TAR_AR_RCVD_OFF;
        status = getValue();
        return this;
      }

      String getValue() {
        return statusAlive
                +statusRepPosValue
                +statusRepTarValue
                +statusTarARRecvValue
                +statusPosARRecvValue
                ;
      }
      public String toString() { return status; }
  }
  public void execute ()
  {
    Enumeration e;

    e = robotOrgs.getAddedList();
    while(e.hasMoreElements())
    {
//      Asset o = (Asset)e.nextElement();
      Organization o = (Organization)e.nextElement();
      System.out.println("UGS " +o.toString() + " seen");
      RobotPG rpg = GetRobotStatus(o);
      if (rpg==null) {
        System.out.println("UGS " +o.toString() + " needs a RPG property group. Creating it.");
        makeRobotPropertyGroup(o);
      }
      UGSPG upg = GetUGSPG(o);
      if (upg==null) {
        System.out.println("UGS " +o.toString() + " needs a UGS property group. Creating it.");
        makeUGSPropertyGroup(o);
      }
      makeReportPositionTask(o);
      System.out.println("UGS " +o.toString() + " ready.");
    }

    if(!underway)
    {
      //check if the go signal was issue
      e = setGoTask.getAddedList();
      if(e.hasMoreElements())
      {
        System.out.println("Got the GO signal...");
        //launch the robots we can see
        e = robotOrgs.elements();
        while (e.hasMoreElements())
        {

          Asset o = (Asset)e.nextElement();

          System.out.println("\nStarting REPORT POSITION and TARGETING tasks on " + o.toString());
//	  makeReportPositionTask(o);
          makeTargetingTask(o, null, null);

          // requested hack to vary times of turning active
         // if (o.toString().indexOf("1")>-1 || o.toString().indexOf("3")>-1)
            updateUGSStatus(o, status.setTarTask(true));
        }
        underway = true;
      }
      else
      {
        // return; //no go signal was issued
      }
    }
    else
    {
      //check if the Go signal was rescinded
      e = setGoTask.getRemovedList();
      if(e.hasMoreElements())
      {
        System.out.println("Got the STOP signal...");
        removeTargetingTasks();
        removePositioningTasks();
        //removeWaypointTasks();
        e = robotOrgs.elements();
        while (e.hasMoreElements())
        {
          Asset o = (Asset)e.nextElement();
          updateUGSStatus(o, status.setTarTask(false));
        }

        //clean up
        detectors.removeAllElements();
        firsttimedetection = false;
        underway = false;
        waypointsset = false;
        return;
      }
    }

/* -- */
    //this for debugging to see if robot is saying stuff
    e = reportPositionAllocs.getChangedList();
    if (e.hasMoreElements())
    {
      while (e.hasMoreElements())
      {
        Allocation a = (Allocation)e.nextElement();
        AllocationResult ar = a.getReceivedResult();
        if (ar != null)
        {
          updateUGSPosition(a, ar);
          NewRobotPG rloc = (NewRobotPG)GetRobotStatus(a.getAsset());
//	  System.out.println("nsofSurveillancePlugin:: UGS " + a.getAsset().toString() + " position "
//	                      +rloc.getLatitude() + " " + rloc.getLongitude());
          System.out.println("nsofSurveillancePlugin:: UGS " + a.getAsset().toString() + " position "
                              +ar.getValue(Constants.Aspects.LATITUDE) + " " + ar.getValue(Constants.Aspects.LONGITUDE));
          if (rloc !=null) {
            rloc.setLatitude(ar.getValue(Constants.Aspects.LATITUDE));
            rloc.setLongitude(ar.getValue(Constants.Aspects.LONGITUDE));
          System.out.println("nsofSurveillancePlugin:: UGS " + /* a.getAsset().toString() + */ " position "
                              +rloc.getLatitude() + " " + rloc.getLongitude());
          }
        }
      }
    }
/* -- */

    // see if any Robots have reported a target
    System.out.println("processingAdded targetAllocs");
    processReportedTargetAllocResults(reportTargetAllocs.getAddedList());
    System.out.println("processingChanged targetAllocs");
    processReportedTargetAllocResults(reportTargetAllocs.getChangedList());

//    if (!e.hasMoreElements())
//      return;  //no change in status. Return.

    // at least one ugs has something...
  }

  private void processReportedTargetAllocResults(Enumeration e) {
    while (e.hasMoreElements())
    {
      // see who has a result
      Allocation a = (Allocation)e.nextElement();
      AllocationResult ar = a.getReceivedResult();
      if (ar != null)
      {
        addUGSDetection(a, ar);
      }
    }
  }
  private double getDetection(AllocationResult ar) {
    double detection;
      if (!ar.isDefined(Constants.Aspects.DETECTION)) {
        System.out.println("WARNING: Received ReportTarget AllocationResult which does not specify the DETECTION aspect value.");
        detection=0.9;
      } else {
        detection=ar.getValue(Constants.Aspects.DETECTION);
      }
      return detection;
  }

  private Date getDetectionTime(AllocationResult ar) {
    Date retval;
//      if (!ar.isDefined(Constants.Aspects.DETTIME)) {
      if (!ar.isDefined(Constants.Aspects.DETECTION_TIME)) {
        System.out.println("WARNING: Received ReportTarget AllocationResult which does not specify the DETECTION aspect value.");
        retval=new Date();
      } else {
        retval=new Date((long)(ar.getValue(Constants.Aspects.DETECTION_TIME)));
      }
      return retval;
  }

//  public void displayTracks(DCL dcl) {
//    Iterator trackIter=dcl.tracksIterator();
//    while (trackIter.hasNext()) {
//  //      DCL.Track track = (DCL.Track)trackIter.next();
//      Track track = (Track)trackIter.next();
//      System.out.println(track);
//    }
//  }

  private void addUGSDetection(Allocation a, AllocationResult ar) {
        //which robot was this that detected
        Organization detector = (Organization)a.getAsset();
        NewUGSPG upg = (NewUGSPG)GetUGSPG(detector);

        // the next line is a hack which was requested
        updateUGSStatus(detector, status.setTarTask(true));

        //no guarantee that the RobotAllocator Plug In has set this before
        //Surveillance plug in has been notified.
        double arDetection=getDetection(ar);
        Date arDetTime=getDetectionTime(ar);
        boolean detected=(arDetection > 0.001);
        if (detected) {
          System.out.println("\nTarget reported at " +ar.getValue(Constants.Aspects.BEARING) +" degrees from UGS " +detector.toString()+" "+arDetection+" "+arDetTime);

          if (upg!=null) {
            double[] bearings=upg.getBearing();
            double[] detTime=upg.getDetTime();
            int len=upg.getLen();
            int entries=upg.getEntries();
            if (entries<len) {
              bearings[entries]=ar.getValue(Constants.Aspects.BEARING);
//              detTime[entries]=bogusTime;
              detTime[entries]=arDetTime.getTime();
              entries++;
              upg.setBearing(bearings);
              upg.setDetTime(detTime);
              upg.setEntries(entries);
              upg.setStatus(status.setTarAR(true).getValue());
            }

            double[] tmpBearings=upg.getBearing();
            int myEntry=upg.getEntries()-1;
            double lastBearing = tmpBearings[myEntry];
            System.out.println("\nUGSPG Target reported at " +lastBearing +" degrees from UGS " +detector.toString());
            System.out.println("\nUGSPG Bearings: " +upg.getBearing() +".");
            System.out.println("\nUGSPG Times:    " +upg.getDetTime() +".");

/*
            bogusTime++;
            // myDCL.addDetection(upg.getLat(), upg.getLon(), lastBearing, bogusTime);
            myDCL.addDetection(upg.getLat(), upg.getLon(), lastBearing, arDetTime.getTime());
            if (bogusTime < 20 || bogusTime%60 == 0) displayTracks(myDCL);
*/
          }

        } else {
          System.out.println("\nNon-Detection Target reported at " +ar.getValue(Constants.Aspects.BEARING) +" degrees from UGS " +detector.toString());
        }


  }

      private void updateUGSPosition(Allocation a, AllocationResult ar) {
          NewUGSPG ugspg = (NewUGSPG)GetUGSPG(a.getAsset());
          System.out.println("nsofSurveillancePlugin:: UGS " + a.getAsset().toString() + " position "
                              +ar.getValue(Constants.Aspects.LATITUDE) + " " + ar.getValue(Constants.Aspects.LONGITUDE));
          if (ugspg !=null) {
            ugspg.setLat(ar.getValue(Constants.Aspects.LATITUDE));
            ugspg.setLon(ar.getValue(Constants.Aspects.LONGITUDE));
            updateUGSStatus(a.getAsset(), status.setPosAR(true));

          System.out.println("nsofSurveillancePlugin:: UGS " + /* a.getAsset().toString() + */ " position "
                              +ugspg.getLat() + " " + ugspg.getLon());

          }


      }

      private void updateUGSStatus(Asset asset, Status newStatus) {
          NewUGSPG ugspg = (NewUGSPG)GetUGSPG(asset);
          System.out.println("nsofSurveillancePlugin:: updateUGSStatus ");
          if (ugspg !=null) {
            System.out.print("nsofSurveillancePlugin:: updateUGSStatus "
                      + asset + " Status change from "
                              +ugspg.getStatus()); System.out.flush();
            ugspg.setStatus(newStatus.getValue());
            System.out.println(" to (" + status+") "+ugspg.getStatus());
          }

      }

  public double normalizeAngle(double a) {
    double twopi = 2.*Math.PI;
    while (a < 0.0)
      a += twopi;
    while (a >= twopi)
      a -= twopi;
    return a;
  }

  public double sign(double x)
  {
    return (x<0?-1.0:1.0);
  }

  public boolean FindIntersection(double y1, double x1, double alpha,
                                  double y2, double x2, double beta,
                                  PositionCoordinate pc)
  {
    // using linear algebra version from Bob Bieri
    pc.latitude = -999.0;
    pc.longitude = -999.0;
    alpha = Math.toRadians(alpha);
    beta = Math.toRadians(beta);

    double ca = Math.cos(alpha);
    double sa = Math.sin(alpha);
    double cb = Math.cos(beta);
    double sb = Math.sin(beta);
    double Ma, Mb;

    if (sa == 0.) {
      if (sb == 0.)
        return false;
      else {
        pc.longitude = x1;
        Mb = cb/sb;
        pc.latitude = y2 + Mb*(pc.longitude-x2);
      }
    }
    else if (sb == 0) {
      pc.longitude = x2;
      Ma = ca/sa;
      pc.latitude = y1 + Ma*(pc.longitude-x1);
    }
    else {
      Ma = ca/sa;
      Mb = cb/sb;
      double b1 = y1 - Ma*x1;
      double b2 = y2 - Mb*x2;
      if (Ma == Mb) {
        pc.longitude = -999.;
        pc.latitude = -999.;
        return false;
      }
      else {
        pc.longitude = (b2-b1) / (Ma-Mb);
        pc.latitude = (Mb*b1 - Ma*b2) / (Mb-Ma);
      }
    }
    if (  sign(pc.latitude-y1) != sign(ca) ||
          sign(pc.longitude-x1) != sign(sa) ||
          sign(pc.latitude-y2) != sign(cb) ||
          sign(pc.longitude-x2) != sign(sb) ) {
      pc.longitude = -999.;
      pc.latitude = -999.;
      return false;
    }
    return true;
  }
}
