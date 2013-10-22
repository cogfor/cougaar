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
package org.cougaar.microedition.se.ssw;

import java.util.*;

import org.cougaar.core.plugin.*;
import org.cougaar.core.util.*;
import org.cougaar.core.domain.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.service.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.microedition.se.domain.*;

/**
 * This plugin works with the com.bbn.sensorweb.sensor.x10.MotionDetectorTaskingPlugin by
 * looking for new MicroAgents with DoorWindowSensor capabilities, and allocating tasks to them.
 * It expects the MicroAgents' capabilities to have a colon-separated format like:
 * <pre>
 *              DoorWindowSensor:ReportState0:0.0:1.0:0.0
		DoorWindowSensor:ReportState1:1.0:1.0:0.0
		DoorWindowSensor:ReportState2:2.0:1.0:0.0
 * </pre>
 * This example shows 3 motion detectors.  The fields on each line are:
 * <nl><li>The constant "DoorWindowSensor" all else is ignored
 * <li>The Verb name to use for the task.  Should match the "command" for ControlPlugin.
 * <li>The sensor's X position
 * <li>The sensor's Y position
 * <li>The sensor's Z position
 */
public class DoorWindowSensorPlugin extends ComponentPlugin {

  IncrementalSubscription assetSub, allocSub;

  /** Holds value of property loggingService. */
  private LoggingService loggingService;
  
  /** Holds value of property domainService. */
  private DomainService domainService;
  
  /**
   * Subscribe to MicroAgents and my own allocations.
   */
  protected void setupSubscriptions() {

    assetSub = (IncrementalSubscription)getBlackboardService().subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {return o instanceof MicroAgent;}});

  }

  /**
   * Handle new micro agents and changes to my allocations
   */
  protected void execute() {

    //
    // Allocate all tasks to a micro agent
    //
    Enumeration micros = assetSub.getAddedList();
    while (micros.hasMoreElements()) {
      MicroAgent micro = (MicroAgent)micros.nextElement();
      loggingService.info("Got a new micro asset: "+micro);
      makeTasks(micro);
    }
    micros = assetSub.getRemovedList();
    while (micros.hasMoreElements()) {
      MicroAgent micro = (MicroAgent)micros.nextElement();
      loggingService.info("Deleted a micro asset: "+micro);
      removeTasks(micro);
    }
  }

  /**
   * Gin-up new task(s).
   */
  private void makeTasks(MicroAgent micro) {
    RootFactory factory = getDomainService().getFactory();


    // set up the task prepositions based on the microagent description
    MicroAgentPG mapg = micro.getMicroAgentPG();
    StringTokenizer toker = new StringTokenizer(mapg.getCapabilities(), ":\n");
    
    int detectorNumber = 0;
    while (toker.hasMoreTokens()) {
        String type = toker.nextToken().trim();
        if (!"DoorWindowSensor".equals(type)) {
            if (loggingService.isDebugEnabled())
                loggingService.debug("Ignoring sensor type: "+type);
            continue; // this will eat all uninteresting data records
        }

        NewTask t = factory.newTask();
        t.setPlan(factory.getRealityPlan());
        String verb = toker.nextToken().trim();
        t.setVerb(Verb.getVerb(verb));
        
        NewPrepositionalPhrase pp = factory.newPrepositionalPhrase();
        pp.setPreposition("DoorWindowSensor");
        pp.setIndirectObject(Boolean.TRUE);
        t.addPrepositionalPhrase(pp);
        
        pp = factory.newPrepositionalPhrase();
        pp.setPreposition("X");
        String strVal = toker.nextToken().trim();
        pp.setIndirectObject(new Double(strVal));
        t.addPrepositionalPhrase(pp);
        
        pp = factory.newPrepositionalPhrase();
        pp.setPreposition("Y");
        strVal = toker.nextToken().trim();
        pp.setIndirectObject(new Double(strVal));
        t.addPrepositionalPhrase(pp);
    
        pp = factory.newPrepositionalPhrase();
        pp.setPreposition("Z");
        strVal = toker.nextToken().trim();
        pp.setIndirectObject(new Double(strVal));
        t.addPrepositionalPhrase(pp);
    
        pp = factory.newPrepositionalPhrase();
        pp.setPreposition("ObserverID");
        pp.setIndirectObject("DoorWindowSensor-"+mapg.getName()+"-"+detectorNumber++);
        t.addPrepositionalPhrase(pp);
        if (loggingService.isDebugEnabled())
            loggingService.debug("Publishing a new task: "+t);

        getBlackboardService().publishAdd(t);
        Allocation allo = makeAllocation(t, micro);
        getBlackboardService().publishAdd(allo);
    }
  }

  private void removeTasks(MicroAgent micro) {
      RoleSchedule rs = micro.getRoleSchedule();
      ArrayList toBeDeleted = new ArrayList(rs.size());
      for(Enumeration re = rs.getRoleScheduleElements(); re.hasMoreElements();) {
          Allocation alloc = (Allocation)re.nextElement();
          Task t = alloc.getTask();
          toBeDeleted.add(t);
      }
      for(Iterator iter = toBeDeleted.iterator(); iter.hasNext();) 
          getBlackboardService().publishRemove(iter.next());
  }
  /**
   * Gin-up an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task t, MicroAgent micro) {
    RootFactory factory = getDomainService().getFactory();
    AllocationResult estAR = null;
    Allocation allocation =
      factory.createAllocation(t.getPlan(), t, micro, estAR, Role.ASSIGNED);
    return allocation;
  }
  
  /** Getter for property loggingService.
   * @return Value of property loggingService.
   */
  public LoggingService getLoggingService() {
      return loggingService;
  }
  
  /** Setter for property loggingService.
   * @param loggingService New value of property loggingService.
   */
  public void setLoggingService(LoggingService loggingService) {
      this.loggingService = loggingService;
  }
  
  /** Getter for property domainService.
   * @return Value of property domainService.
   */
  public DomainService getDomainService() {
      return this.domainService;
  }
  
  /** Setter for property domainService.
   * @param domainService New value of property domainService.
   */
  public void setDomainService(DomainService domainService) {
      this.domainService = domainService;
  }
  
}
