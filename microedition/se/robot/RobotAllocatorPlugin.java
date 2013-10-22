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

import org.cougaar.planning.plugin.legacy.*;
import org.cougaar.core.util.*;
import org.cougaar.core.blackboard.*;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;

import org.cougaar.util.*;

import org.cougaar.glm.ldm.asset.*;

import org.cougaar.microedition.shared.tinyxml.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.se.domain.*;

import java.util.*;
import java.io.*;


/**
 */
public class RobotAllocatorPlugin extends SimplePlugin {

// Subscription for all Robot assets
  private IncrementalSubscription robotOrgs;
  IncrementalSubscription reportPositionAllocs;
  IncrementalSubscription reportTargetAllocs;
  IncrementalSubscription advanceAllocs;

/**
 */
  protected void setupSubscriptions() {
    System.out.println("RobotAllocatorPlugin::setupSubscriptions");

    theLDMF.addPropertyGroupFactory(new org.cougaar.microedition.se.domain.PropertyGroupFactory());

    robotOrgs = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        boolean ret = false;
        if (o instanceof Organization) {
          Organization org = (Organization)o;
          OrganizationPG orgPG = org.getOrganizationPG();
          ret = orgPG.inRoles(Role.getRole(Constants.Robot.roles[Constants.Robot.SURVEILLANCEPROVIDER]));
        }
        return ret;
      }
    });

    reportPositionAllocs = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTPOSITION]);
        return false;
      }
    });

    reportTargetAllocs = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.REPORTTARGET]);
        return false;
      }
    });

    advanceAllocs = (IncrementalSubscription)subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation)
            return ((Allocation)o).getTask().getVerb().equals(Constants.Robot.verbs[Constants.Robot.ADVANCE]);
        return false;
      }
    });

  }

  /**
   */
  protected void execute () {
//    System.out.println("RobotPlugin::execute");

    // process new organizations
    Enumeration org_enum = robotOrgs.getAddedList();
    while (org_enum.hasMoreElements()) {
//System.out.println("RobotPluginPlugin::getting new Robot Org");
      Organization o = (Organization)org_enum.nextElement();
      o.addOtherPropertyGroup(getRobotPG());
      publishChange(o);
    }

    Enumeration alloc_enum = reportPositionAllocs.getChangedList();
    while (alloc_enum.hasMoreElements()) {
      Allocation a = (Allocation)alloc_enum.nextElement();
      AllocationResult ar = a.getReceivedResult();
      if (ar == null)
        continue;
      if (ar.isSuccess() == false)
        continue;
//System.out.println("RobotPluginPlugin::getting changed report position allocs");
      Organization o = (Organization)a.getAsset();
      Enumeration pgs = o.getOtherProperties();
      while (pgs.hasMoreElements()) {
        PropertyGroup pg = (PropertyGroup)pgs.nextElement();
        if (pg instanceof RobotPG) {
          NewRobotPG rpg = (NewRobotPG)pg;
          double lat = ar.getValue(Constants.Aspects.LATITUDE);
          double lon = ar.getValue(Constants.Aspects.LONGITUDE);
          double heading = ar.getValue(Constants.Aspects.HEADING);

          //originally reported in billionths. MicroTask plugin adjusted by 1000.0
          lat = lat*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);
          lon = lon*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);
          heading = heading*(Constants.Geophysical.BILLIONTHSTODEG*1000.0);

          rpg.setLatitude(lat);
          rpg.setLongitude(lon);
          rpg.setHeading(heading);
//	  System.out.println("RobotAllocatorPlugin: Robot Lat, Lon, Head: "
//	                       +rpg.getLatitude() +" " +rpg.getLongitude() +" " +rpg.getHeading());
//          o.addOtherPropertyGroup(rpg);
          publishChange(o);
        }
      }
    }

    alloc_enum = reportTargetAllocs.getChangedList();
    while (alloc_enum.hasMoreElements()) {
      Allocation a = (Allocation)alloc_enum.nextElement();
      AllocationResult ar = a.getReceivedResult();
      if (ar == null)
        continue;
      if (ar.isSuccess() == false)
        continue;
//System.out.println("RobotPluginPlugin::getting changed report target allocs");
      Organization o = (Organization)a.getAsset();
      Enumeration pgs = o.getOtherProperties();
      while (pgs.hasMoreElements()) {
        PropertyGroup pg = (PropertyGroup)pgs.nextElement();
        if (pg instanceof RobotPG) {
          NewRobotPG rpg = (NewRobotPG)pg;
          double absbear = ar.getValue(Constants.Aspects.BEARING);
          double det = ar.getValue(Constants.Aspects.DETECTION);
          rpg.setBearing(absbear);
          rpg.setDetection((det > 0.001)); //0.0 = false, 1.0 = true
//          o.addOtherPropertyGroup(rpg);
          publishChange(o);
//System.out.println("RobotPluginPlugin::"+Constants.Robot.verbs[Constants.Robot.REPORTTARGET]+ar.getValue(Constants.Aspects.BEARING));
        }
      }
    }
  }

  /**
   */
  private RobotPG getRobotPG() {
    NewRobotPG new_robot_pg = (NewRobotPG)theLDMF.createPropertyGroup("RobotPG");
    new_robot_pg.setLatitude(-999.0);
    new_robot_pg.setLongitude(-999.0);
    new_robot_pg.setVelocity(-999.0);
    new_robot_pg.setHeading(-999.0);
    new_robot_pg.setDetection(false);
    new_robot_pg.setImageAvailable(false);
    new_robot_pg.setFlashlightOn(false);
    return new_robot_pg;
  }

}
