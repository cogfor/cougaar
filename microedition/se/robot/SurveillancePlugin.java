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
import org.cougaar.util.UnaryPredicate;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.glm.ldm.asset.OrganizationPG;
import org.cougaar.microedition.shared.*;
import java.util.*;
import java.lang.*;
import org.cougaar.microedition.se.domain.*;


public class SurveillancePlugin extends SimplePlugin
{
  private int minimumdetectors = 2;
  private boolean underway = false;
  private boolean firsttimedetection = false;
  private boolean waypointsset = false;
  PositionCoordinate waypointposition;

  //private boolean waypointsset = true;
  //PositionCoordinate waypointposition = new PositionCoordinate(38.872797222, -77.084000000);

  IncrementalSubscription robotOrgs;
  IncrementalSubscription reportTargetTasks;
  IncrementalSubscription reportPositionTasks;
  IncrementalSubscription reportTargetAllocs;
  IncrementalSubscription reportPositionAllocs;
  IncrementalSubscription setWaypointTasks;
  IncrementalSubscription setGoTask; //PSP originates this task to put things in motion
  IncrementalSubscription launchTask; //PSP originates this task to put things in motion

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

  UnaryPredicate LaunchWeaponTaskPredicate()
  {
    UnaryPredicate newPred = new UnaryPredicate()
    {
      public boolean execute(Object o)
      {
        if (o instanceof Task)
          return ((Task)o).getVerb().equals(Constants.Robot.verbs[Constants.Robot.LAUNCHWEAPON]);
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

  private int extractinteger(String param)
  {
    int indx = param.indexOf("=");
    if (indx < 0)
    {
      System.out.println("\n !!!! Error in extractparameter for "+param);
      return 0;
    }

    String valstring = (param.substring(indx+1)).trim();
    Integer temp = new Integer(valstring);
    return(temp.intValue());
  }

  // subscribe to tasks and organizations
  protected void setupSubscriptions()
  {

    System.out.println("SurveillancePlugin::setupSubscriptions");
    Vector parameters = getParameters();
    Enumeration pnum = parameters.elements();
    while (pnum.hasMoreElements())
    {
      String param = (String)pnum.nextElement();
      if (param.toLowerCase().indexOf("minimumdetectors") >= 0)
      {
         minimumdetectors = extractinteger(param);
         System.out.println("SurveillancePlugin minimumdetectors "+minimumdetectors);
      }
    }

    robotOrgs = (IncrementalSubscription)subscribe(SurveillanceProviderPredicate());
    reportTargetTasks = (IncrementalSubscription)subscribe(ReportTargetTasksPredicate());
    reportTargetAllocs = (IncrementalSubscription)subscribe(ReportTargetAllocsPredicate());
    reportPositionTasks = (IncrementalSubscription)subscribe(ReportPositionTasksPredicate());
    reportPositionAllocs = (IncrementalSubscription)subscribe(ReportPositionAllocsPredicate());
    setWaypointTasks = (IncrementalSubscription)subscribe(SetWaypointTasksPredicate());

    setGoTask = (IncrementalSubscription)subscribe(GetGoTaskPredicate());
    launchTask = (IncrementalSubscription)subscribe(LaunchWeaponTaskPredicate());

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

    if(pos != null && rb != null)
    {
      Vector prepositions = new Vector();

      NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LATPREP]);
      npp.setIndirectObject(String.valueOf(pos.latitude));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LONPREP]);
      npp.setIndirectObject(String.valueOf(pos.longitude));
      prepositions.add(npp);

      npp = theLDMF.newPrepositionalPhrase();
      npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.BEARINGPREP]);
      npp.setIndirectObject(String.valueOf(rb.bearing));
      prepositions.add(npp);

      ((NewTask)t).setPrepositionalPhrases(prepositions.elements());
      System.out.println("...with prepositions "
                                  +String.valueOf(pos.latitude)+" "
                                  +String.valueOf(pos.longitude)+" "
                                  +String.valueOf(rb.bearing));
    }
    else
    {
      System.out.println("...without prepositions ");
    }

    publishAdd(t);

    AllocationResult estAR = null;
    Allocation a =
      theLDMF.createAllocation(t.getPlan(), t, o, estAR, Role.ASSIGNED);
    publishAdd(a);
  }

  private NewTask createLaunchSubTask(Task parenttask, long utctimelaunch)
  {
    NewTask subTask;

    subTask = theLDMF.newTask();
    subTask.setPlan(theLDMF.getRealityPlan());
    subTask.setVerb(Verb.getVerb(Constants.Robot.verbs[Constants.Robot.SETLAUNCHTIME]));

    Vector prepositions = new Vector();

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.TIMEPREP]);

    Long launchtime = new Long(utctimelaunch);
    String launchtimeprepvalue = launchtime.toString();
    System.out.println("Launch time = " +launchtime);

    npp.setIndirectObject(launchtimeprepvalue);
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LATPREP]);
    npp.setIndirectObject(String.valueOf(waypointposition.latitude));
    prepositions.add(npp);

    npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition(Constants.Robot.prepositions[Constants.Robot.LONPREP]);
    npp.setIndirectObject(String.valueOf(waypointposition.longitude));
    prepositions.add(npp);

    subTask.setPrepositionalPhrases(prepositions.elements());

    subTask.setParentTask(parenttask);
    subTask.setPlan(parenttask.getPlan());
    subTask.setDirectObject(parenttask.getDirectObject());

    return subTask;

  }

  private void makeWaypointTask(Asset o, PositionCoordinate pos)
  {
    System.out.println("Making Waypoint Task...");

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

  private void setTargetingTasks(PositionCoordinate pc, RBCoordinate rb)
  {
    // assign new reporttarget tasks for non detecting robots
    System.out.println("SurveillancePlugin:: Set targeting tasks..");
    Enumeration e = robotOrgs.elements();
    while (e.hasMoreElements())
    {
      Asset o = (Asset)e.nextElement();

      if (detectors.contains(o)) // for detectors dir=>none (suspend searching).
        continue; // don't start a targeting task on detector.

      makeTargetingTask(o, pc, rb);
    }
  }

  private void setWaypoint(PositionCoordinate pc)
  {
    // assign new reporttarget tasks for non detecting robots
    System.out.println("SurveillancePlugin:: Set Waypoint tasks..");
    Enumeration e = robotOrgs.elements();
    while (e.hasMoreElements())
    {
      Asset o = (Asset)e.nextElement();

      makeWaypointTask(o, pc);
    }
  }

  private void removeTargetingTasks()
  {
    System.out.println("SurveillancePlugin::remove target reporting tasks");
    Enumeration e;

    for (e=reportTargetTasks.elements();e.hasMoreElements();)
    {
      publishRemove((Task)e.nextElement());
    }
  }

  private void removeWaypointTasks()
  {
    System.out.println("SurveillancePlugin::remove waypoint tasks");
    Enumeration e;

    for (e=setWaypointTasks.elements();e.hasMoreElements();)
    {
      publishRemove((Task)e.nextElement());
    }
  }

  private void removePositioningTasks()
  {
    System.out.println("SurveillancePlugin::remove position reporting tasks");
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
          System.out.println("SurveillancePlugin::stopping targeting task on detector " +t.getUID());
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
          System.out.println("SurveillancePlugin::stopping positioning task on detector " +t.getUID());
          publishRemove(t);
        }
      }
    }
  }

  private void restartRobots()
  {
   System.out.println("SurveillancePlugin::restartDetectorRobots");
   Enumeration e = robotOrgs.elements();
   while (e.hasMoreElements())
   {
     Asset o = (Asset)e.nextElement();
     makeReportPositionTask(o);
     makeTargetingTask(o, null, null);
   }
   underway = true;
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

  protected void execute ()
  {
    Enumeration e;

    e = robotOrgs.getAddedList();
    if(e.hasMoreElements() == true)
    {
      Asset o = (Asset)e.nextElement();
      System.out.println("Robot " +o.toString() + "seen");
    }


    //check if the go signal was issued
    e = setGoTask.getAddedList();
    if(e.hasMoreElements() == true)
    {
      Task gotask = (Task)e.nextElement();
      PrepositionalPhrase prep = gotask.getPrepositionalPhrase(Constants.Robot.verbs[Constants.Robot.SETWAYPOINT]);
      if (prep!=null)
      {
        String coordtext =(String)prep.getIndirectObject();
        System.out.println("Manually setting waypoint " +coordtext);
        StringTokenizer st = new StringTokenizer(coordtext, ",");
        if (st.hasMoreTokens())
        {
            double lat = Double.parseDouble(st.nextToken());
            double lon = Double.parseDouble(st.nextToken());
            //System.out.println("Waypoint " +lat+" "+lon);
            waypointposition = new PositionCoordinate(lat, lon);
            waypointsset = true;
            setWaypoint(waypointposition); //send waypoints to robots
        }
      }
      else
      {
        if(!underway)
        {
          System.out.println("Got the GO signal...");
          //launch the robots we can see
          Enumeration eorgs = robotOrgs.elements();
          while (eorgs.hasMoreElements())
          {
            Asset o = (Asset)eorgs.nextElement();
            //System.out.println("\nStarting REPORT POSITION and TARGETING tasks on " + o.toString());
            makeReportPositionTask(o);
            makeTargetingTask(o, null, null);
          }
          underway = true;
        }
      }
    }
    else
    {
      //check if the Go signal was rescinded
      e = setGoTask.getRemovedList();
      if(e.hasMoreElements() == true)
      {
        System.out.println("Got the STOP signal...");
        removeTargetingTasks();
        removePositioningTasks();
        removeWaypointTasks();

        //clean up
        detectors.removeAllElements();
        firsttimedetection = false;
        underway = false;
        waypointsset = false;
        return;
      }
    }

    e = launchTask.getAddedList();
    if(e.hasMoreElements() == true && waypointsset == true)
    {
      Task ltask = (Task)e.nextElement();
      System.out.println("Launch weapon task added...expanding");

      NewWorkflow nwf = theLDMF.newWorkflow();
      nwf.setParentTask(ltask);

      long utctime = System.currentTimeMillis() + 60*1000; //add a minute
      Enumeration eorgs = robotOrgs.elements();
      while (eorgs.hasMoreElements())
      {
        Asset o = (Asset)eorgs.nextElement();
        NewTask subTask = createLaunchSubTask(ltask, utctime);
        subTask.setWorkflow(nwf);
        nwf.addTask(subTask);
        Allocation a = theLDMF.createAllocation(subTask.getPlan(), subTask, o, null, Role.ASSIGNED);
        publishAdd(subTask);
        publishAdd(a);
      }

      Expansion expansion =
                theLDMF.createExpansion(ltask.getPlan(), ltask, nwf, null);
      publishAdd(expansion);
    }

    // see if any Robots have reported a target
    e = reportTargetAllocs.getChangedList();
    if (!e.hasMoreElements())
      return;  //no change in status. Return.

    // at least one robot has something...
    while (e.hasMoreElements())
    {
      // see who has a result
      Allocation a = (Allocation)e.nextElement();
      AllocationResult ar = a.getReceivedResult();
      if (ar != null)
      {
        //which robot was this that detected
        Organization detector = (Organization)a.getAsset();

        NewRobotPG rpg = (NewRobotPG)GetRobotStatus(detector);

        //no guarantee that the RobotAllocator Plug In has set this before
        //Surveillance plug in has been notified.
        double absbear = ar.getValue(Constants.Aspects.BEARING);
        rpg.setBearing(absbear);
        rpg.setDetection((ar.getValue(Constants.Aspects.DETECTION) > 0.001));

        if(rpg.getDetection())
        {
          System.out.println("\nTarget reported at " +rpg.getBearing() +" degrees True from Robot " +detector.toString());

          // save identity of detecting robot(s)
          if (!detectors.contains(detector))
             detectors.add(detector);
        }
      }
    }


    if(detectors.size() >= minimumdetectors && waypointsset == false)
    {
      PositionCoordinate tl = new PositionCoordinate();
      if(computeTargetLocation(tl) == true)
      {
        //removePositioningTasks();
        //removeTargetingTasks(); //no more looking around
        waypointposition = tl;
        waypointsset = true;
        setWaypoint(waypointposition); //send waypoints to robots
      }
      else if(minimumdetectors == 1) //usually used to test end game with one robot
      {
        waypointsset = true;
        setWaypoint(tl); //send fake lat lon to robot
      }
      else
      {
        System.out.println("Unable to triangulate. System stopped.");
        //removePositioningTasks();
        removeTargetingTasks();
        removeWaypointTasks();

        //clean up
        detectors.removeAllElements();
        firsttimedetection = false;
        underway = false;
        waypointsset = false;
      }
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
    //System.out.println("FindIntersection: "+y1+" "+x1+" "+alpha);
    //System.out.println("FindIntersection: "+y2+" "+x2+" "+beta);

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
