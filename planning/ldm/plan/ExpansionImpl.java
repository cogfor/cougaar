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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.ActiveSubscriptionObject;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.util.log.Logger;
 
/** ExpansionImpl.java
 * Implementation for expansion - a form of PlanElement
 *
 */
 
public class ExpansionImpl extends PlanElementImpl 
  implements Expansion, NewExpansion
{
 
  static final long serialVersionUID = 34303612634065165L;

  private transient Workflow workflow;  // changed to transient : Persistence
  
  public ExpansionImpl() {}

  /* Constructor that assumes there is not a good estimated result at this time.
   * @param p
   * @param t
   * @param wf
   * @return Expansion
   */
  public ExpansionImpl(Plan p, Task t, Workflow wf) {
    super(p, t);
    this.workflow = wf;

    setContext();
  }
  
  /* Constructor that takes an estimated result
   * @param p
   * @param t
   * @param wf
   * @param estimatedresult
   * @return Expansion
   */
  public ExpansionImpl(Plan p, Task t, Workflow wf, AllocationResult estimatedresult) {
    super(p, t);
    workflow = wf;
    estAR = estimatedresult;
    setContext();
  }
    

  /** @return Workflow - Return the Workflow that represents the expansion of the task*/
  public Workflow getWorkflow() {
    return workflow;
  }

  public void removingFromBlackboard(Subscriber s, boolean commit) {
    super.removingFromBlackboard(s, commit);

    // Task t = getTask();
    Workflow w = getWorkflow();
    if (w == null) return; // if already disconnected...

    if (ActiveSubscriptionObject.deferCommit) { /* this is an expensive test if we're going to waste it */
      if (!w.isPropagatingToSubtasks() ) { // if we're not auto-propagating
        for (Enumeration e = w.getTasks(); e.hasMoreElements(); ) {
          NewTask wfstask = (NewTask) e.nextElement();
          Blackboard.getTracker().checkpoint(commit, wfstask, "getParentTask");
        }
      }
    }

    if (!commit) return;

    if (w.isPropagatingToSubtasks() ) { // if we're auto-propagating
      WorkflowImpl wi = (WorkflowImpl) w;
      
      
      // rescind all subtasks of the workflow
      List sts = wi.clearSubTasks();    // atomic get and clear the list
      ListIterator it = sts.listIterator();
      while (it.hasNext()) {
        NewTask asub = (NewTask) it.next();
        s.publishRemove(asub);
      }
    } else {      // we're not auto-propagating
      // disconnect the WF from the parent task
      ((NewWorkflow)w).setParentTask(null);

      // FIXME: Is it really OK to re-use a workflow and its sub-tasks in a new task?


      for (Enumeration e = w.getTasks(); e.hasMoreElements(); ) {
        NewTask wfstask = (NewTask) e.nextElement();
        wfstask.setParentTask(null);
	// Let the remover clear this pointer,
	// but use it in the meantime if necessary.
	// Note that removing the subtask will then clear
	// this pointer, if not already done (via ASO method)
	//	wfstask.setWorkflow(null);
      }
      // the plugin should reattach this workflow to a parent task. 
    }
  }
  
  /** Called by an Expander Plugin to get the latest copy of the allocationresults
   *  for each subtask.
   *  Information is stored in a List which contains a SubTaskResult for each subtask.  
   *  Each of the SubTaskResult objects contain the following information:
   *  Task - the subtask, boolean - whether the result changed,
   *  AllocationResult - the result used by the aggregator for this sub-task.
   *  The boolean indicates whether the AllocationResult changed since the 
   *  last time the collection was cleared by the plugin (which should be
   *  the last time the plugin looked at the list).
   *  @return List of SubTaskResultObjects one for each subtask
   */
  public SubtaskResults getSubTaskResults() {
    return workflow.getSubtaskResults();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    synchronized (workflow) { // Protect the integrity of the workflow
      stream.defaultWriteObject();
      stream.writeObject(workflow);
    }
  }
 

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    workflow = (Workflow) stream.readObject();
  }

  /** Sets the non-null Contexts of the subtasks in the workflow to be
   * that of the parent task
   **/
  private void setContext() {
    Context context = task.getContext();
    // No sense in going through all the subtasks if the parent's Context was never set.
    if (context == null)
      return;

    // Set the Context of the subtasks to be the Context of the parent task
    for (Enumeration e = workflow.getTasks(); e.hasMoreElements();) {
      Object o = e.nextElement();
      if (o instanceof TaskImpl) {
	TaskImpl subtask = (TaskImpl)o;
	if (subtask.getContext() == null) {
	  subtask.setContext(context);
	}
      }
    }
  }
  public String toString() {
    return "[Expansion " + getUID() + " to "+workflow+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("workflow", ExpansionImpl.class, "getWorkflow", null));
  }

  /**
   * Fix an object once rehydration has completed.
   * <p>
   * This is used as a last-minute cleanup, in case the
   * object requires special deserialization work.
   */
   public void postRehydration(Logger logger) {
     super.postRehydration(logger);

     Workflow wf = getWorkflow();
     if(wf != null) {
       for (Enumeration tasks = wf.getTasks(); tasks.hasMoreElements(); ) {
         NewTask subtask = (NewTask) tasks.nextElement();
         Workflow owf = subtask.getWorkflow();
         if (owf != wf) {
           subtask.setWorkflow(wf);
           if (owf != null) {
             logger.warn("Reset task's "+subtask+" workflow from "+owf+" to "+wf);
           }
         }

         if (logger.isDebugEnabled()) {
           PlanElement subtaskPE = subtask.getPlanElement(); /*UCK*/
           if (subtaskPE == null) {
             logger.debug("Subtask " + subtask.getUID() + " not disposed");
           } else {
             logger.debug("Subtask " + subtask.getUID() + " disposed " + hc(subtaskPE));
           }
         }
       }
     }
   }
}
