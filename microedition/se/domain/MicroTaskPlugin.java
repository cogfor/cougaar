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

package org.cougaar.microedition.se.domain;

import java.util.*;
import java.io.*;

import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.component.*;

import org.cougaar.microedition.shared.*;

/**
 * Infrastructure plugin for task commmunications between big Cougaar and
 * Cougaar Micro Edition.
 */
public class MicroTaskPlugin extends ComponentPlugin implements MessageListener, ServiceAvailableListener
{
  private IncrementalSubscription sub;
  private IncrementalSubscription taskSub;

  /** Holds value of property mEMessageService. */
  private MEMessageService mEMessageService;  

  /** Holds value of property loggingService. */
  private LoggingService loggingService;
  
  /**
   * Subscribe to allocations to MicroAgents
   */
  protected void setupSubscriptions() {
    
    getBindingSite().getServiceBroker().addServiceListener(this);
    sub = (IncrementalSubscription)getBlackboardService().subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation a = (Allocation)o;
          return a.getAsset() instanceof MicroAgent;
        }
        return false;
      }});

    taskSub = (IncrementalSubscription)getBlackboardService().subscribe(new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Task)
          return true;
        return false;
      }});

      Collection v = getParameters();
      if (loggingService.isDebugEnabled()) {
          loggingService.debug("PARAMS: "+v);
      }
  }

  /**
   * Called when objects in the PLAN change
   */
  protected void execute() {
    // send new or changed allocations to the micro asset.
    Collection newlyAllocedTasks = new Vector();
    Collection allocs = new Vector();
    if (sub.getAddedCollection() != null)
      allocs.addAll(sub.getAddedCollection());
    Iterator iter = allocs.iterator();
    while (iter.hasNext()) {
      Allocation alloc = (Allocation)iter.next();
      newlyAllocedTasks.add(alloc.getTask());
      processAllocation(alloc, "add");
    }

    Collection callocs = new Vector();
    if (taskSub.getChangedCollection() != null)
      callocs.addAll(taskSub.getChangedCollection());
    Iterator citer = callocs.iterator();
    while (citer.hasNext()) {
      Task task = (Task)citer.next();
      if (!newlyAllocedTasks.contains(task)) {  // only send changed tasks if they're not
        processTask(task, "change");            // associated with a new allocation
      }
    }

    Collection dallocs = new Vector();
    if (sub.getRemovedCollection() != null)
      dallocs.addAll(sub.getRemovedCollection());
    Iterator diter = dallocs.iterator();
    while (diter.hasNext())
      processAllocation((Allocation)diter.next(), "remove");
  }

  private void processTask(Task task, String op) {
    Allocation allo = (Allocation)task.getPlanElement();
    if (allo == null)
      return;
    Asset asset = allo.getAsset();
      MicroTask mt = mEMessageService.newMicroTask(task);
      try {
        mEMessageService.getMessageTransport().sendTo((MicroAgent)asset, mt, op);
      } catch (java.io.IOException ioe)
      {
        loggingService.error("IOException sending message to MicroAgent", ioe);
      }
  }

  /**
   * Transmit an allocation to the micro agent.
   */
  private void processAllocation(Allocation allo, String op) {
    Asset asset = allo.getAsset();
      if (loggingService.isDebugEnabled()) loggingService.debug("MicroTaskPlugin: Allocation to MicroAgent: " + asset);
      // encode task to send to micro agent
      MicroTask mt = mEMessageService.newMicroTask(allo.getTask());

      try {
        mEMessageService.getMessageTransport().sendTo((MicroAgent)asset, mt, op);
      } catch (java.io.IOException ioe)
      {
        loggingService.error("IOException sending message to MicroAgent", ioe);
      }
  }

  /**
   * Receive a message from a micro-agent.  For now, assume it's a MicroTask.
   */
  public boolean deliverMessage(String msg, String src, String srcAddress, OutputStream client, InputStream in) {
    if (msg.indexOf("<MicroTask") >= 0) {
     TaskDecoder td = new TaskDecoder();
     MicroTask mt = td.decode(msg);
     getBlackboardService().openTransaction();
     // lookup the original task
     Task t = lookupTask(mt.getUniqueID());
     if (loggingService.isDebugEnabled()) loggingService.debug("MicroTaskPlugin: Delivering task: " + t);
     if (t == null) {
       loggingService.error("MicroTaskPlugin: Error finding task \""+mt.getUniqueID()+"\"");
       getBlackboardService().closeTransaction();
       return true;
     }

     // update the reported result
     Allocation alloc = (Allocation)t.getPlanElement();
     AllocationResult ar = decodeAllocationResult(mt);
     if ((ar == null) || (alloc == null)) {
      getBlackboardService().closeTransaction();
      if (loggingService.isDebugEnabled()) loggingService.debug("MicroTaskPlugin: allocation not updated.  ar="+ar+": alloc="+alloc);
      return true;
     }
     ((PlanElementForAssessor)alloc).setReceivedResult(ar);

     if (loggingService.isDebugEnabled()) loggingService.debug("MicroTaskPlugin: Changing alloc: "+alloc);
     getBlackboardService().publishChange(alloc);
     getBlackboardService().closeTransaction();
    }
    return true;
  }

  /**
   * Unwrap an allocation result from a MicroTask's MicroAllocationResult.
   */
  private AllocationResult decodeAllocationResult(MicroTask mt) {
    if (mt.getAllocation() == null)
      return null;
    if (mt.getAllocation().getReportedResult() == null)
      return null;
    double rating = mt.getAllocation().getReportedResult().getConfidenceRating();
    boolean success = mt.getAllocation().getReportedResult().isSuccess();
    int [] aspects = mt.getAllocation().getReportedResult().getAspects();
    /*
     * Convert Long array (in thousandths) to double array
     */
    long [] thousandths = mt.getAllocation().getReportedResult().getValues();
    double [] values = new double[thousandths.length];
    for (int i=0; i<thousandths.length; i++)
      values[i] = thousandths[i] / 1000.0;
    AllocationResult ar = new AllocationResult(rating, success, aspects, values);
    return ar;
  }

  /**
   *  Lookup a task in the PLAN by UID.
   */
  private Task lookupTask(String UID) {
    Task ret = null;
    Collection tasks_col = getBlackboardService().query(new TaskPredicate(UID));
    Iterator iter = tasks_col.iterator();
    if (iter.hasNext())
      ret = (Task)iter.next();
    return ret;
  }

  protected void registerMessageListener() {
    mEMessageService.getMessageTransport().addMessageListener(this);
  }
  
  /** Getter for property mEMessageService.
   * @return Value of property mEMessageService.
   */
  public MEMessageService getMEMessageService() {
      return mEMessageService;
  }
  
  /** Setter for property mEMessageService.
   * @param mEMessageService New value of property mEMessageService.
   */
  public void setMEMessageService(MEMessageService mEMessageService) {
      this.mEMessageService = mEMessageService;
      if (mEMessageService != null)
        registerMessageListener();
  }
  
  public void serviceAvailable(ServiceAvailableEvent sae) {
      if (sae.getService().isAssignableFrom(MEMessageService.class)) {
          setMEMessageService((MEMessageService) sae.getServiceBroker().getService(this, MEMessageService.class, null));
      }
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
  
}

/**
 * Predicate class for looking up tasks by UID.
 */
class TaskPredicate implements UnaryPredicate {
  private String UID;
  public TaskPredicate (String UID) {
    this.UID = UID;
  }
  public boolean execute (Object o) {
    if (o instanceof Task) {
      Task t = (Task)o;
      return t.getUID().toString().equals(UID);
    }
    return false;
  }
  
  
}
