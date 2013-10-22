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
package org.cougaar.microedition.plugin;

import java.io.*;
import java.util.*;

import org.cougaar.microedition.util.*;
import org.cougaar.microedition.ldm.*;
import org.cougaar.microedition.shared.*;
import org.cougaar.microedition.asset.*;


/**
 */
public class ControlPlugin extends PluginAdapter {

  UnaryPredicate getTaskPred() {
    UnaryPredicate myPred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof MicroTask) {
          MicroTask mt = (MicroTask)o;
          return mt.getVerb().equals(controlCommand);
        }
        return false;
      }
    };
    return myPred;
  }

  UnaryPredicate getResourcePred() {
    UnaryPredicate resourcePred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof ControllerResource) {
          ControllerResource cr = (ControllerResource)o;
          return (resourceName.equals(cr.getName()));
        }
        return false;
      }
    };
    return resourcePred;
  }

  private Subscription taskSub;
  private Subscription resourceSub;

  int sleeptime = 1000;
  String resourceName = "ControlResource";
  String controlCommand = "ControlCommand"; //for sensor resource could be Sense
  String controlParameter = "ControlParameter";
  String controlParameterValue = "ControlParameterValue";

  ControllerResource resource = null;

  private Vector taskthreadstokill = new Vector();
  private boolean debugging = false;

  public void setupSubscriptions() {
    //System.out.println("ControlPlugin::setupSubscriptions");
    if (getParameters() != null) {
      Hashtable t = getParameters();
      if (t.containsKey("debug"))
        debugging = !t.get("debug").equals("false");
      if (t.containsKey("sleeptime"))
        sleeptime = Integer.parseInt((String)t.get("sleeptime"));
      if (t.containsKey("resource")) //e.g. TurretController
        resourceName = (String)t.get("resource");
      if (t.containsKey("command")) //e.g. RotateTurret
        controlCommand = (String)t.get("command");
      if (t.containsKey("parameter")) //e.g. TurretHemisphere
        controlParameter = (String)t.get("parameter");
      if (t.containsKey("parametervalue")) //e.g. front
        controlParameterValue = (String)t.get("parametervalue");

      if (debugging) {
          System.out.println("ControlPlugin: Sleep time (msec): "+sleeptime);
          System.out.println("ControlPlugin: resource         : "+resourceName);
          System.out.println("ControlPlugin: control command  : "+controlCommand);
          System.out.println("ControlPlugin: control parameter: "+controlParameter);
          System.out.println("ControlPlugin: parameter value  : "+controlParameterValue);
      }
    }
    else
    {
      if (debugging)System.out.println("ControlPlugin: setupSubscriptions No Parameters specified");
    }
    taskSub = subscribe(getTaskPred());

    //have to in order not to be goosed again due to task change
    taskSub.setSeeOwnChanges(false);

    resourceSub = subscribe(getResourcePred());
  }

  private synchronized void allocate(MicroTask mt, boolean needNewTransaction)
  {
    if(debugging) System.out.println("ControlPlugin: in allocate " +mt);

    if(resource == null) return;

    int size = resource.getNumberAspects();
    if (size == 0) return;

    long values[] = new long[size];
    int aspects[] = new int[size];

    resource.getValues(values);
    resource.getValueAspects(aspects);

    MicroAllocation ma = new MicroAllocation(resource, mt);
    MicroAllocationResult mar = new MicroAllocationResult();
    ma.setReportedResult(mar);
    mar.setSuccess(resource.getSuccess());
    mar.setAspects(aspects);
    mar.setValues(values);

    if (needNewTransaction) openTransaction();
    mt.setAllocation(ma);
    publishChange(ma);
    publishChange(mt);

    if (needNewTransaction) closeTransaction();

    if(debugging)
    {
      for(int i=0; i<size; i++)
        System.out.println("ControlPlugin: allocate sending value: " + values[i]);
    }
  }

  public void execute()
  {

    if (debugging)System.out.println("ControlPlugin: execute()");
    Enumeration enm = resourceSub.getAddedList().elements();
    if (resource==null && enm.hasMoreElements())
    {
      resource = (ControllerResource)enm.nextElement();
      resource.modifyControl(controlParameter,  controlParameterValue);
      if (debugging) {
          System.out.println("ControlPlugin: resource found: "+resource.getName());
          System.out.println("ControlPlugin: resource scale factor: "+resource.getScalingFactor());
          System.out.println("ControlPlugin: resource num aspects: "+resource.getNumberAspects());
      }
    }

    enm = taskSub.getAddedList().elements();
    while (enm.hasMoreElements())
    {
      if (debugging)System.out.println("ControlPlugin: got added "+controlCommand+" task");
      MicroTask mt = (MicroTask)enm.nextElement();

      if (resource!=null)
      {
	if(debugging) System.out.println("ControlPlugin: setAssociation");
	resource.setAssociation(mt.getUniqueID());
	if(debugging) System.out.println("ControlPlugin: SetControlParameter");
        SetControlParameter(mt);

	resource.startControl();

        if (sleeptime > 0) { // poll
            if(debugging) System.out.println("ControlPlugin: launch polling thread");
            Thread pt = new Thread(new PollResource(mt)/*, "Control Plugin"*/);
            pt.start();
        }
      }
      else
      {
	System.out.println("ControlPlugin: Task added without resource!!");
      }
    }

    enm = taskSub.getChangedList().elements();
    while (enm.hasMoreElements())
    {
      MicroTask mt = (MicroTask)enm.nextElement();
      if (debugging)System.out.println("ControlPlugin: got changed "+controlCommand+" task");
      SetControlParameter(mt);
    }

    enm = taskSub.getRemovedList().elements();
    while (enm.hasMoreElements()) {
      MicroTask mt = (MicroTask)enm.nextElement();
      if (debugging)System.out.println("ControlPlugin: got removed "+controlCommand+" task");
      taskthreadstokill.addElement(mt);
    }
    
    enm = resourceSub.getChangedList().elements();
    if (enm.hasMoreElements())
    {
      resource = (ControllerResource)enm.nextElement();
      if (debugging)System.out.println("ControlPlugin: resource changed: "+resource.getName());
      Enumeration tenm = taskSub.getMemberList().elements();
      while (tenm.hasMoreElements())
      {
        MicroTask mt = (MicroTask)tenm.nextElement();
        allocate(mt, false);
      }
    }
  }

  private void SetControlParameter(MicroTask mt)
  {
    if (mt.getPrepositionalPhrases() == null)
     return;

    for (Enumeration enm= mt.getPrepositionalPhrases().elements();
	  enm.hasMoreElements(); )
    {
      MicroPrepositionalPhrase preps=(MicroPrepositionalPhrase)enm.nextElement();
      if (preps!=null)
      {
	controlParameter = preps.getPreposition();
	controlParameterValue = preps.getIndirectObject();
	if(resource != null)
	{
	  resource.modifyControl(controlParameter,  controlParameterValue);
	}
      }
    } // end-for
  }

  class PollResource implements Runnable
  {
    MicroTask task;

    public PollResource(MicroTask mt)
    {
      if(debugging) System.out.println("ControlPlugin: PollResource thread created");
      task = mt;
    }

    public void run()
    {
      if(debugging) System.out.println("ControlPlugin: PollResource thread running");
      while (true)
      {
        try
	{
	  Thread.sleep(sleeptime);

	  if (taskthreadstokill.contains(task))
	  {
	     taskthreadstokill.removeElement(task);
	     if(debugging) System.out.println("ControlPlugin: thread assignd to die.");
	     break; //I've been asked to die
	  }

	  if(resource != null)
	  {
	    if(debugging) System.out.println("ControlPlugin: thread resource association: " +resource.getAssociation());
	    if(resource.getAssociation().compareTo(task.getUniqueID()) != 0)
	      break; //I'm no longer associated with the resource

	    if(resource.conditionChanged())
	      allocate(task, true);
	  }
	}

	catch (Exception ie)
	{
	  System.err.println("ControlPlugin Thread: Exception caught " +ie);
	  ie.printStackTrace();
	}

      } //end while

      //stop control of resource if I'm still associated with it.
      if(resource != null)
      {
	if(resource.getAssociation().compareTo(task.getUniqueID()) == 0)
	{
	  System.out.println("ControlPlugin: Ending the resource polling thread for task " +task.getUniqueID());
	  if (resource.isUnderControl())
	  {
	    System.out.println("ControlPlugin Stopped Control");
	    resource.stopControl();
	  }
	  resource.setAssociation(""); //erase association with resource
	}
      }
    }
  }
}
