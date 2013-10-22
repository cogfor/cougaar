/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.planning.ldm.plan;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;

import org.cougaar.core.blackboard.ActiveSubscriptionObject;
import org.cougaar.core.blackboard.BlackboardException;
import org.cougaar.core.blackboard.Claimable;
import org.cougaar.core.blackboard.PublishableAdapter;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.Transaction;
import org.cougaar.core.persist.ActivePersistenceObject;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** PlanElement Implementation
 * PlanElements represent the association of a Plan, a Task,
 * and a Disposition (where a Disposition is either
 * an Allocation, an Expansion, an Aggregation, or an AssetTransfer).
 * A Disposition (as defined above) are subclasses of PlanElement.
 * PlanElements make a Plan.  For Example, a task "move 15 tanks..." with
 * an Allocation(an Asset, estimated penalty and estimated schedule) 
 * of 15 HETs could represent a PlanElement.
 **/

public abstract class PlanElementImpl 
  extends PublishableAdapter
  implements PlanElement, NewPlanElement, PEforCollections, ScheduleElement, ActiveSubscriptionObject, ActivePersistenceObject, BeanInfo
{

  private static final long serialVersionUID = -3303746652987764635L;

  protected transient Task task;   // changed to transient : Persistence
  //protected Plan plan;
  
  private UID uid;

  /**
   * There are four allocation results:
   * estAR is set by the plugin often as a copy of the reported
   * rcvAR is computed from downstream results (e.g. by workflow aggregators)
   * obsAR is set from observed events (event monitor)
   * repAR is the merge of obsAR and rcvAR and is lazily evaluated
   **/
  protected AllocationResult obsAR = null;
  protected AllocationResult repAR = null;
  protected AllocationResult rcvAR = null;
  protected AllocationResult estAR = null;

  protected transient boolean notify = false;

  private static final Logger logger = Logging.getLogger(PlanElement.class);

  //no-arg constructor
  public PlanElementImpl() {}

  public PlanElementImpl(UID uid) {
    this.uid = uid;
  }     
        
  //constructor that takes both a plan and a task object
  public PlanElementImpl (Plan p, Task t) {
    //plan = p;
    setTask(t);
  }
  
  public void setUID(UID uid) { this.uid = uid;}
  public UID getUID() { return uid; }
        
  //PlanElement interface implementations

  /**
   * @return Plan  the plan this planelement is a part of
   **/
  public Plan getPlan() {
    return PlanImpl.REALITY;
  }
               
  /** This returns the Task of the PlanElement.
   * @return Task
   **/
        
  public Task getTask() {
    return task;
  }

  // ClaimableHolder interface implementation
  public Claimable getClaimable() {
    Task t = getTask();
    if (t != null && t instanceof Claimable) {
      return ((Claimable) t);
    }
    return null;
  }

  // NewPlanElement interface implementations
 
  /** This sets the Task of the PlanElement. 
   * Also sets the planelement  of the task
   * @param t
   **/
        
  public void setTask(Task t) {
    if (task != null) {
      logger.error("planelement.setTask from "+task+" to "+t, new Throwable());
    }
    task = t;
  }

  /**
   * Sets the Task of the PlanElement. This method differs from
   * setTask in that it is expected that the PlanElement is already
   * attached to a Task so the Task and PlanElement are rewired
   * accordingly.
   * @param t - The new Task that the PlanElement is referencing.
   **/
  public void resetTask(Task t) {
    Task oldTask = getTask();
    logger.error("planelement.resetTask from "+oldTask+" to "+t, new Throwable());
    if (oldTask != null) {
      ((TaskImpl) oldTask).privately_resetPlanElement();
    }
    setTask(t);
  }
  
  /** @param p - set the plan this planelement is a part of */
  public void setPlan(Plan p) {
    //plan = p;
  }
  
  /** Returns the estimated allocation result that is related to performing
   * the Task.
   * @return AllocationResult
   **/
  
  public AllocationResult getEstimatedResult() {
    return estAR;
  }
  
  /** Returns the reported allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getReportedResult() {
    if (repAR == null) {
      if (rcvAR == null) {
        repAR = obsAR;
      } else if (obsAR == null) {
        repAR = rcvAR;
      } else {
        repAR = new AllocationResult(obsAR, rcvAR);
      }
    }
    return repAR;
  }
  
  /** Returns the received allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getReceivedResult() {
    return rcvAR;
  }
  
  /** Returns the observed allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getObservedResult() {
    return obsAR;
  }

  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   **/
  public void setEstimatedResult(AllocationResult estimatedresult) {
    estAR = estimatedresult;
    Transaction.noteChangeReport(this,new PlanElement.EstimatedResultChangeReport());
    setNotification(true);
  }
  
  /**
   * CALLED BY INFRASTRUCTURE ONLY - AFTER RESULTS HAVE BEEN COMPUTED ACROSS TASKS.
   * @param rcvres the new received AllocationResult object associated with this pe 
   */
  public void setReceivedResult(AllocationResult rcvres) {
    rcvAR = rcvres;
    repAR = null;               // Need to recompute this
    Transaction.noteChangeReport(this,new PlanElement.ReportedResultChangeReport());
  }

  /** @deprecated use setReceivedResult **/
  public void setReportedResult(AllocationResult repres) {
    throw new UnsupportedOperationException("Use setReceivedResult instead");
  }
  
  /**
   * Set or update the observed AllocationResult. Should be called
   * only by the event monitor.
   * @param obsres the new observed AllocationResult object associated with this pe 
   **/
  public void setObservedResult(AllocationResult obsres) {
    obsAR = obsres;
    repAR = null;               // Need to recompute this
    Transaction.noteChangeReport(this, new PlanElement.ObservedResultChangeReport());
    Transaction.noteChangeReport(this, new PlanElement.ReportedResultChangeReport());
  }
  
  // implement TimeSpan

  public long getStartTime() {
    AllocationResult ar = estAR;
    if (ar != null) {
      if (ar.isDefined(AspectType.START_TIME)) {
        return (long) ar.getValue(AspectType.START_TIME);
      }
    }
    return MIN_VALUE;
  }

  public long getEndTime() {
    AllocationResult ar = estAR;
    if (ar != null) {
      if (ar.isDefined(AspectType.END_TIME)) {
        return (long) ar.getValue(AspectType.END_TIME);
      }
    }
    return MAX_VALUE;
  }

  public boolean shouldDoNotification() {
    return notify;
  }
  public void setNotification(boolean v) {
    notify = v;
  }
  
  // ScheduleElement implementation
  /** Start date is a millisecond-precision, inclusive time of start.
   * @return Date Start time for the task 
   **/
  public Date getStartDate() { return new Date(getStartTime()); }
	
  /** End Date is millisecond-precision, <em>exclusive</em> time of end.
   * @return Date End time for the task 
   **/
  public Date getEndDate() { return new Date(getEndTime()); }
	
  /** is the Date on or after the start time and strictly before the end time?
   *  @return boolean whether the date is included in this time interval.  
   **/
  public boolean included(Date date) {
    return included(date.getTime());
  }
	
  /** is the time on or after the start time and strictly before the end time?
   * @return boolean whether the time is included in this time interval 
   **/
  public boolean included(long time) {
    return ( (time >= getStartTime()) && (time < getEndTime()) );
  }

  /** Does the scheduleelement overlap (not merely abut) the schedule?
   * @return boolean whether schedules overlap 
   **/
  public boolean overlapSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime < getEndTime() &&
             tetime > getStartTime() );
  }


  /** Does the scheduleElement meet/abut the schedule?
   **/
  public boolean abutSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime == getEndTime() ||
             tetime == getStartTime() );
  }


  // If the planelement is either an allocation or an assettransfer, add the 
  // planelement to the respective Asset's RoleSchedule.
  protected void addToRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.add(this);
    } else {
      System.err.println("\n WARNING - could not add PlanElement to roleschedule");
    }
  }
  protected void removeFromRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.remove(this);
    } else {
      System.err.println("\n WARNING - could not remove PlanElement from roleschedule");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
 
    stream.defaultWriteObject();
 
    stream.writeObject(task);
    if (stream instanceof org.cougaar.core.persist.PersistenceOutputStream) {
        stream.writeObject(myAnnotation);
    }
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    task = (Task)stream.readObject();
    if (stream instanceof org.cougaar.core.persist.PersistenceInputStream) {
        myAnnotation = (Annotation) stream.readObject();
    }
    pcs = new PropertyChangeSupport(this);
  }

  public String toString() {
    return "[PE #"+task.getUID()+" -> "+"]";
  }

  // ActiveSubscriptionObject
  public void addingToBlackboard(Subscriber s, boolean commit) {
    Blackboard.getTracker().checkpoint(commit, getTask(), "getPlanElement");
    if (!commit) return;

    Task t = getTask();
    Date comdate = t.getCommitmentDate();
    if (comdate != null) {
      // make sure the current planning time is before commitment time
      long curTime = s.getClient().currentTimeMillis();
      // Could allow a 5 second buffer perhaps?
      // IE: if (curTime > comdate.getTime() + 5000)
      if ( curTime  > comdate.getTime() ) {
        // its after the commitment time - shouldn't publish the object
	// But for now we do so anyhow
        logger.warn("publishAdd of "+this + " " + (curTime - comdate.getTime()) + " millis past commitmenttime "+comdate + " at curTime: " + (new Date(curTime)) + " by Subscriber " + s);
      }
    }

    PlanElement existingPE = t.getPlanElement();
    BlackboardException e = null;
    if (existingPE == null) {
      ((TaskImpl)t).privately_setPlanElement(this);
    } else if (existingPE == this) {
      e =  new BlackboardException("publishAdd of miswired PlanElement (task already wired to this PE): " + this);
    } else {
      e =  new BlackboardException("publishAdd of miswired PlanElement (task already has other PE): " + existingPE);
    }
    if (e != null) {
      logger.error("PlanElement.addingToBlackboard", e);
      throw e;
    }
  }
  public void changingInBlackboard(Subscriber s, boolean commit) {}
  public void removingFromBlackboard(Subscriber s, boolean commit) {
    Blackboard.getTracker().checkpoint(commit, getTask(), "getPlanElement");
    if (!commit) return;

    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();
  }

  // ActivePersistenceObject
  public boolean skipUnpublishedPersist(Logger logger) {
    logger.error("Omitting PlanElement not on blackboard: " + this);
    return true;
  }
  public void checkRehydration(Logger logger) {
    /*  // currently a no-op
    if (this instanceof AssetTransfer) {
    } else {
      Task task = getTask();
      if (task != null) {
        PlanElement taskPE = task.getPlanElement();
        if (taskPE != this) {
          //            if (logger.isWarnEnabled()) logger.warn("Bad " + getClass().getName() + ": getTask()=" + task + " task.getPlanElement()=" + taskPE);
        }
      } else {
        //          if (logger.isWarnEnabled()) logger.warn("Bad " + getClass().getName() + ": getTask()=null");
      }
    }
    */
  }

  public void postRehydration(Logger logger) {
    if (logger.isDebugEnabled()) {
      logger.debug("Rehydrated plan element: " + this);
    }

    TaskImpl task = (TaskImpl) getTask();
    if (task != null) {
      PlanElement taskPE = task.getPlanElement();
      if (taskPE != this) {
        if (taskPE != null) {
          task.privately_resetPlanElement();
          logger.warn("resetPlanElement of "+task+" to "+this+" (was "+taskPE+")");
        }

        task.privately_setPlanElement(this); // These links can get severed during rehydration
      }
    }
  }

  /** reset asset role-schedules post-rehydration **/
  protected void fixAsset(Asset asset) {
    // Compute role-schedules
    RoleScheduleImpl rsi = (RoleScheduleImpl) asset.getRoleSchedule();
    rsi.add(this);
  }

  // Should match BasePersistence.hc(o), without compile dependency
  protected static String hc(Object o) {
    return (Integer.toHexString(System.identityHashCode(o)) +
            " " +
            (o == null ? "<null>" : o.toString()));
  }

  //
  // annotation
  //
  private transient Annotation myAnnotation = null;
  public void setAnnotation(Annotation pluginAnnotation) {
    myAnnotation = pluginAnnotation;
  }
  public Annotation getAnnotation() {
    return myAnnotation;
  }

  //dummy PropertyChangeSupport for the Jess Interpreter.
  public transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
      pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
      pcs.removePropertyChangeListener(pcl);
  }

  // beaninfo - duplicate of SelfDescribingBeanInfo because
  // java doesn't allow multiple inheritence of implementation.

  public BeanDescriptor getBeanDescriptor() { return null; }
  public int getDefaultPropertyIndex() { return -1; }
  public EventSetDescriptor[] getEventSetDescriptors() { return null; }
  public int getDefaultEventIndex() { return -1; }
  public MethodDescriptor[] getMethodDescriptors() { return null; }
  public BeanInfo[] getAdditionalBeanInfo() { return null; }
  public java.awt.Image getIcon(int iconKind) { return null; }
  private static final PropertyDescriptor[] _emptyPD = new PropertyDescriptor[0];
  public PropertyDescriptor[] getPropertyDescriptors() { 
    Collection pds = new ArrayList();
    try {
      addPropertyDescriptors(pds);
    } catch (IntrospectionException ie) {
      System.err.println("Warning: Caught exception while introspecting on "+this.getClass());
      ie.printStackTrace();
    }
    return (PropertyDescriptor[]) pds.toArray(_emptyPD);
  }
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    c.add(new PropertyDescriptor("uid", PlanElementImpl.class, "getUID", null));
    //c.add(new PropertyDescriptor("plan", PlanElementImpl.class, "getPlan", null));
    c.add(new PropertyDescriptor("task", PlanElementImpl.class, "getTask", null));
    c.add(new PropertyDescriptor("estimatedResult", PlanElementImpl.class, "getEstimatedResult", "setEstimatedResult"));
    c.add(new PropertyDescriptor("reportedResult", PlanElementImpl.class, "getReportedResult", null));
  }
}
