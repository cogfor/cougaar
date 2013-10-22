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

import org.cougaar.microedition.shared.Constants;
import org.cougaar.microedition.se.domain.MicroAgent;


/**
 * Plugin to control targeting.
 */
public class TargetingAllocatorPlugin extends SimplePlugin {

  static final private String myVerb
    = Constants.Robot.verbs[Constants.Robot.DETECTTARGET];
  static final private String turretVerb
    = Constants.Robot.verbs[Constants.Robot.ROTATETURRET];
  static final private String sonarVerb
    = Constants.Robot.verbs[Constants.Robot.REPORTDETECTION];

  private MicroAgent mySONARSensorMC = null;
  private IncrementalSubscription taskSub;
  private IncrementalSubscription resourceSub;
  private IncrementalSubscription allocSub;
  private double bearing = -1 ;
  private double peakbearing = 0.0;
  private double peakdetection = -1.0;

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

  /**
   * Return a UnaryPredicate which is true for SONARSensor MicroAgents.
   */
  UnaryPredicate getResourcePred() {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        boolean ret=false;
        if (o instanceof MicroAgent) {
           MicroAgent mc = (MicroAgent)o;
           ret = isTurretController(mc) || isSonarSensor(mc);
        }
        return ret;
      }
    };
    return resourcePred;
  }

  final boolean isTurretController(MicroAgent mc) {
    return (mc.getMicroAgentPG().getCapabilities().toLowerCase()
      .indexOf(Constants.Robot.meRoles[Constants.Robot.TURRETCONTROLLER].toLowerCase())>-1);
  }

  final boolean isSonarSensor(MicroAgent mc) {
    return (mc.getMicroAgentPG().getCapabilities().toLowerCase()
      .indexOf(Constants.Robot.meRoles[Constants.Robot.SONARSENSOR].toLowerCase())>-1);
  }

  UnaryPredicate getAllocPred() {
    UnaryPredicate myAllocPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation
             && ((Allocation)o).getEstimatedResult()!=null ) {
          Task mt = ((Allocation)o).getTask();
          return mt.getVerb().equals(sonarVerb) ||
                 mt.getVerb().equals(turretVerb);
        }
        return false;
      }
    };
    return myAllocPred;
  }

  public void setupSubscriptions()
  {
    taskSub = (IncrementalSubscription)subscribe(getTaskPred());
    resourceSub = (IncrementalSubscription)subscribe(getResourcePred());
    allocSub = (IncrementalSubscription)subscribe(getAllocPred());
  }

  Vector removedTasks=new Vector();
  /**
   * Handle addition of SONARSensor resource microcluster and added or
   * removed tasks of interest.
   */
  public void execute()
  {
    Enumeration allocEnum;

    allocEnum = allocSub.getChangedList();
    while (allocEnum.hasMoreElements())
    {
      processAllocation((Allocation)allocEnum.nextElement());
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
      Enumeration resourceEnum = resourceSub.elements();
      while (resourceEnum.hasMoreElements()) {
        MicroAgent mc = (MicroAgent)resourceEnum.nextElement();
        updateResource(mc, mt, true);
      }
    }
  }

  private void removeTask(Task t, Allocation alloc)
  {
    if (!removedTasks.contains(t))
    {
      removedTasks.add(t);
      publishRemove(t);
    }
  }

  private boolean heedsonar = false; //true when turret bearings are during scan period

  private void processAllocation(Allocation alloc)
  {
     //System.out.println("TargetAllocationPlugin: processAllocation");
  // if the reported result is from a turret, update bearing
  // if the reported result is from sonarsensor, then update allocationResults on taskSub elements
    MicroAgent mc=(MicroAgent)alloc.getAsset();

    AllocationResult ar = alloc.getReportedResult();

    if (isTurretController(mc))
    {
       try
       {
        if (ar!=null)
        {
          if (ar.isSuccess())
          {
            //System.out.println("TargettingPlugin:  Turret reports bearing");
            bearing = ar.getValue(Constants.Aspects.BEARING);
            heedsonar = true;
          }
          else
          {
            //scanning finished. Tell the brain that nothing happened
            //System.out.println("TargettingPlugin:  Turret reports scan finish");
            bearing = ar.getValue(Constants.Aspects.BEARING);
            heedsonar = false;

            AllocationResult myar = null;

            if(peakdetection > 0.0)
            {
              //there was a peak above threshold, report it
              System.out.println("TargetingAllocator Plugin: reporting peakbearing " +peakbearing);
              myar=makeAllocationResult(peakbearing, true);
              peakdetection = -1.0; //reset
            }
            else
            {
              myar=makeAllocationResult(bearing, false);
            }

            Enumeration taskEnum = taskSub.elements();
            while (taskEnum.hasMoreElements())
            {
              Task mt = (Task)taskEnum.nextElement();
              Allocation myAlloc=((Allocation)mt.getPlanElement());
              myAlloc.setEstimatedResult(myar);
              publishChange(myAlloc);
            }
          }
        }
      }
      catch (Exception ex)
      {
        System.out.println("Turret gave allocationResult without a bearing");
      }
    }
    else if (isSonarSensor(mc))
    {
      if(heedsonar == true)
      {
        double detection = ar.getValue(Constants.Aspects.DETECTION);
        System.out.println("TargettingPlugin:  sonar reports peak value " +detection+" at bearing "+bearing);

        if(detection > peakdetection)
        {
          peakdetection = detection;
          peakbearing = bearing;
        }
/*
        AllocationResult myar=makeAllocationResult(bearing, true);

        Enumeration taskEnum = taskSub.elements();
        while (taskEnum.hasMoreElements())
        {
          Task mt = (Task)taskEnum.nextElement();
          Allocation myAlloc=((Allocation)mt.getPlanElement());
          myAlloc.setEstimatedResult(myar);
          publishChange(myAlloc);
        }
*/
      }
    }
  }

  private AllocationResult makeAllocationResult(double bearing, boolean wasSuccessful)
  {
      int []aspect_types = {Constants.Aspects.BEARING, Constants.Aspects.DETECTION};
      double []results = { bearing, ((wasSuccessful) ? 1 : 0)};
      return theLDMF.newAllocationResult(1.0, wasSuccessful,
        aspect_types, results);
  }

  private void updateResource(MicroAgent mc, Task mt, boolean wantOn)
  {

    NewTask subTask = null;

    if (mc == null)
    {
      System.err.println("execute->updateResource called when resource is NULL.");
      return;
    }

    SubTasks st;
    if (inProgress.containsKey(mt))
    {
      st=(SubTasks)inProgress.get(mt);
    }
    else
    {
      st=new SubTasks();
      inProgress.put(mt, st);
    }

    if (isTurretController(mc))
    {
      subTask = createTask(mc, turretVerb); //creates a new task
      st.setTurretTask(subTask);
      inProgress.put(mt, st);
    }
    else if (isSonarSensor(mc))
    {
      subTask = createTask(mc, sonarVerb); //creates a new task
      st.setSensorTask(subTask);
      inProgress.put(mt, st);
    }

    //pass on prepositional phrases to sub tasks
    if (mt.getPrepositionalPhrases() != null)
    {
      Vector prepositions = new Vector();
      Enumeration enum= mt.getPrepositionalPhrases();
      while(enum.hasMoreElements())
      {
        PrepositionalPhrase preps=(PrepositionalPhrase)enum.nextElement();
        if (preps!=null) prepositions.add(preps);
      }
      subTask.setPrepositionalPhrases(prepositions.elements());
    }

    // update allocation
    if (mt.getPlanElement()==null)
    {
      Allocation alloc = makeAllocation(mt, mc);
      publishAdd(alloc);
      publishChange(mt);
    }
  }


  Vector subtasks=new Vector();

  private NewTask createTask(MicroAgent mc, String verb)
  {
    NewTask mt = theLDMF.newTask();
    mt.setPlan(theLDMF.getRealityPlan());
    mt.setVerb(Verb.getVerb(verb));
    publishAdd(mt);
    Allocation alloc=makeAllocation(mt, mc);
    publishAdd(alloc);
    subtasks.add(alloc);
    return mt;
  }

  Hashtable inProgress=new Hashtable();

  private void processRemovedTask(Task mt)
  {
    //System.out.println("TargetingAllocatorPlugin: processRemoveTask");

    SubTasks st=(SubTasks)inProgress.remove(mt);
    if(st != null)
    {
      //System.out.println("TargetingAllocatorPlugin: subtask "+st.getTurretTask());
      publishRemove(st.getTurretTask());
      //System.out.println("TargetingAllocatorPlugin: subtask "+st.getSensorTask());
      publishRemove(st.getSensorTask());
    }
  }

  private Task makeTask(String verbText) {
    NewTask t = theLDMF.newTask();
    t.setPlan(theLDMF.getRealityPlan());
    t.setVerb(Verb.getVerb(verbText));
    return t;
  }

  /**
   * Create an allocation of this task to this asset
   */
  private Allocation makeAllocation(Task task, MicroAgent micro)
  {
    //System.out.println("TargetAllocationPlugin: makeAllocation");
    AllocationResult estAR = makeAllocationResult(-1, false);
    Allocation allocation =
     theLDMF.createAllocation(task.getPlan(), task, micro, estAR, Role.ASSIGNED);
    return allocation;
  }


  class SubTasks
  {
    Task turretTask;
    Task sensorTask;

    void setTurretTask(Task t) { turretTask=t; }
    void setSensorTask(Task t) { sensorTask=t; }
    Task getTurretTask() { return turretTask; }
    Task getSensorTask() { return sensorTask; }

    public String toString() {
      String tur="null";
      String sen="null";
      if (turretTask!=null) tur=turretTask.getUID()+" "+turretTask.getVerb().toString();
      if (sensorTask!=null) sen=sensorTask.getUID()+" "+sensorTask.getVerb().toString();
      return "SubTasks: Sensor: "+sen+" Turret: "+tur;
    }
  }
}
