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

package org.cougaar.planning.plugin.util;

import java.util.Enumeration;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.AbstractAsset;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.Context;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;

/**
 * Provides utility methods for building Expander Plugins.
 */
public class ExpanderHelper {

  /**
   * Checks if the Task is of specified OFTYPE.
   */
  public static boolean isOfType( Task t, String p, String typeid ) {
    return typeid.equals(getOfType(t, p));
  }

  public static String getOfType(Task t, String p) {
    PrepositionalPhrase pPhrase = t.getPrepositionalPhrase(p);
    if (pPhrase != null) {
      Object indirectobj = pPhrase.getIndirectObject();
      if (indirectobj instanceof AbstractAsset) {
	AbstractAsset aa = (AbstractAsset) indirectobj;
	return aa.getTypeIdentificationPG().getTypeIdentification();
      }
    }
    return null;
  }

  /**
   * Takes "a" subtask, generates a workflow for that subtask. This newly created
   * Expansion is wired properly and returned.
   * @deprecated use PluginHelper.wireExpansion(Task parent, NewTask subTask, PlanningFactory ldmf) instead
   */
  public static Expansion wireExpansion(Task parent, NewTask subTask, PlanningFactory ldmf){

    NewWorkflow wf = ldmf.newWorkflow();

    Task t = parent;

    wf.setParentTask( t );
    subTask.setWorkflow( wf );
    wf.addTask( subTask );

    //End of creating NewWorkflow. Start creating an Expansion.
    // pass in a null estimated allocationresult for now
    Expansion exp = ldmf.createExpansion( t.getPlan(),t, wf, null );

    // Set the Context of the subTask to be that of the parent, unless it has already been set
    if ((Task)subTask.getContext() == null) {
      subTask.setContext(parent.getContext());
    }

    return exp;
  }

  /**
   * Takes a Vector of subtasks, generates a workflow for those subtasks. This newly created
   * Expansion is wired properly and returned.
   * @deprecated use PluginHelper.wireExpansion(Task parentTask, Vector subTasks, PlanningFactory ldmf) instead.
   */
  public static Expansion wireExpansion( Vector subTasks, PlanningFactory ldmf, Task parentTask, NewWorkflow wf ) {
    wf.setParentTask( parentTask );

    Context context = parentTask.getContext();
    for (Enumeration esubTasks = subTasks.elements(); esubTasks.hasMoreElements(); ) {
      Task myTask = (Task)esubTasks.nextElement();
      ((NewTask)myTask).setWorkflow( (Workflow)wf );
      wf.addTask( myTask );
      // Set the Context of the subtask if it hasn't already been set
      if (myTask.getContext() == null) {
	((NewTask)myTask).setContext(context);
      }
    }

    //End of creating NewWorkflow. Start creating an Expansion.
    // pass in a null estimated allocationresult for now
    Expansion exp = ldmf.createExpansion( parentTask.getPlan(), parentTask, (Workflow)wf, null );

    return exp;
  }

  /** Publish a new Expansion and its subtasks.
   * e.g.
   *   publishAddExpansion(getSubscriber(), myExpansion);
   * @deprecated use PluginHelper.publishAddExpansion(Subscriber sub, PlanElement exp) instead
   **/
  public static void publishAddExpansion(Subscriber sub, PlanElement exp) {
    sub.publishAdd(exp);

    for (Enumeration esubTasks = ((Expansion)exp).getWorkflow().getTasks(); esubTasks.hasMoreElements(); ) {
      Task myTask = (Task)esubTasks.nextElement();
      sub.publishAdd(myTask);
    }
  }


  /** Takes a subscription, gets the changed list and updates the changedList.
   * @deprecated use PluginHelper.updateAllocationResult(IncrementalSubscription sub) instead
   */
  public static void updateAllocationResult ( IncrementalSubscription sub ) {

    Enumeration changedPEs = sub.getChangedList();
    while ( changedPEs.hasMoreElements() ) {
      PlanElement pe = (PlanElement)changedPEs.nextElement();
      if (pe.getReportedResult() != null) {
        //compare entire pv arrays
        AllocationResult repar = pe.getReportedResult();
        AllocationResult estar = pe.getEstimatedResult();
        if ( (estar == null) || (!repar.isEqual(estar)) ) {
          pe.setEstimatedResult(repar);
          sub.getSubscriber().publishChange( pe, null );
        }
      }
    }
  }

    /**
     * @deprecated use PluginHelper.createEstimatedAllocationResult(Task t, PlanningFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, PlanningFactory ldmf) {
      return createEstimatedAllocationResult(t, ldmf, 0.0);
    }
    /**
     * @deprecated use PluginHelper.createEstimatedAllocationResult(Task t, PlanningFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, PlanningFactory ldmf, double confrating) {
	Enumeration preferences = t.getPreferences();
        Vector aspects = new Vector();
        Vector results = new Vector();
        while (preferences != null && preferences.hasMoreElements()) {
          Preference pref = (Preference) preferences.nextElement();
          int at = pref.getAspectType();
          aspects.addElement(new Integer(at));
          ScoringFunction sf = pref.getScoringFunction();
          // allocate as if you can do it at the "Best" point
          double myresult = ((AspectScorePoint)sf.getBest()).getValue();
          results.addElement(new Double(myresult));
        }
        int[] aspectarray = new int[aspects.size()];
        double[] resultsarray = new double[results.size()];
        for (int i = 0; i < aspectarray.length; i++)
          aspectarray[i] = (int) ((Integer)aspects.elementAt(i)).intValue();
        for (int j = 0; j < resultsarray.length; j++ )
          resultsarray[j] = (double) ((Double)results.elementAt(j)).doubleValue();

        AllocationResult myestimate = ldmf.newAllocationResult(confrating, true, aspectarray, resultsarray);
        return myestimate;
    }
}

