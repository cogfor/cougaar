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

package org.cougaar.planning.plugin.deletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.deletion.DeletionPlugin;
import org.cougaar.core.plugin.deletion.DeletionPolicy;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.Constraint;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewConstraint;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.NewWorkflow;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PlanElementSet;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.Filters;
import org.cougaar.util.UnaryPredicate;

/**
 * DeletionPlugin provides generic deletion services to a agent.
 * These consist of:
 *
 * Identification of deletable Allocations to non-org assets
 * Identification of tasks having deletable dispositions (PlanElements)
 * Removal of deletable subtasks from Expansions
 * Identification of Aggregations to deletable tasks
 **/

public class TaskDeletionPlugin extends DeletionPlugin {
  private UnaryPredicate deletablePlanElementsPredicate;
  private class DeletablePlanElementsPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      if (o instanceof PlanElement) {
        PlanElement pe = (PlanElement) o;
        if (isTimeToDelete(pe)) {
          if (pe instanceof Allocation) {
            AllocationforCollections alloc = (AllocationforCollections) pe;
            Asset asset = alloc.getAsset();
            ClusterPG cpg = asset.getClusterPG();
            if (cpg == null)
              return true; // Can't be remote w/o ClusterPG
            MessageAddress destination = cpg.getMessageAddress();
            if (destination == null) {
              return true; // Can't be remote w null destination
            }
            UID remoteUID = alloc.getAllocationTaskUID();
            boolean remoteIsDeleted = alloc.isAllocationTaskDeleted();
            return remoteUID == null || remoteIsDeleted;
            // Can delete if remote task is deleted or non-existent
          }
          if (pe instanceof Expansion) {
            Expansion exp = (Expansion) pe;
            return !(exp.getWorkflow().getTasks().hasMoreElements());
          }
          return false;
        }
      }
      return false;
    }
  }

  private List deletablePlanElementFilter(UnaryPredicate deletablePred, Collection planElements) {
    return new ArrayList(Filters.filter(planElements, deletablePred));
  }

  /**
   * Setup subscriptions. We maintain no standing subscriptions, but
   * we do have parameters to initialize -- the period between
   * deletion activities and the deletion time margin.
   **/
  protected void setupSubscriptions() {
    super.setupSubscriptions();
    deletablePlanElementsPredicate = new DeletablePlanElementsPredicate();
  }

  private static final UnaryPredicate planElementPredicate =
    new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof PlanElement;
    }
  };

  private class PESet {
    private PlanElementSet planElementSet;
    public PlanElement findPlanElement(UID uid) {
      if (planElementSet == null) {
        queryBlackBoard();
      }
      return planElementSet.findPlanElement(uid);
    }

    private void queryBlackBoard() {
      Collection planElements = getBlackboardService().query(planElementPredicate);
      planElementSet = new PlanElementSet();
      planElementSet.addAll(planElements);
    }

    public Collection toCollection() {
      List planElementList;
      if (planElementSet == null) {
        queryBlackBoard();
      }
      if (planElementSet != null) {
        planElementList = Arrays.asList(planElementSet.toArray());
      } else {
        planElementList = new ArrayList();
      }
      return planElementList;
    }

    public void clear() {
      planElementSet = null;
    }
  }

  private Set getDeletablePlanElements() {
    if (deletablePlanElements == null) {
      deletablePlanElements = new PlanElementSet();
      Collection c = deletablePlanElementFilter(deletablePlanElementsPredicate, peSet.toCollection());
      if (c.size() > 0) {
        if (logger.isDebugEnabled())
          logger.debug("Found " + c.size() + " deletable PlanElements");
        deletablePlanElements.addAll(c);
      }
    }
    return deletablePlanElements;
  }

  private PESet peSet = new PESet();
  private PlanElementSet deletablePlanElements = null;
  protected int wakeCount = 0;
  private int numDeletedTasks = 0;
  /**
  * Called from execute when the alarm expires.
  *
  * The procedure is:
  * Find new allocations for tasks that deletable and mark the allocations
  * Find tasks with deletable dispositions and mark them
  * Find deletable tasks that are subtasks of an expansion and
  * remove them from the expansion and remove them from the
  * logplan.
  **/
  protected void checkDeletables() {
    numDeletedTasks = 0;
    checkDeletablePlanElements();
    if (logger.isDebugEnabled()) {
      if (++wakeCount > 4) {
        wakeCount = 0;
        printAllPEs();
      }
    }
    peSet.clear();
    deletablePlanElements = null;
    super.checkDeletables();
    if (logger.isInfoEnabled()) {
      if (numDeletedTasks > 0) {
        logger.info(","+getAgentIdentifier()+","+new Date(currentTimeMillis())+","+
		     numDeletedTasks+", tasks deleted this cycle");
      }
    }
  }

  /**
   * Check all plan elements that are superficially deletable (as
   * determined by the deletable plan elements predicate). Starting
   * from each such plan element, we work backward, toward the root
   * tasks, looking for a deletable tasks. All the methods beginning
   * with "check" work back toward the roots. The methods beginning
   * with "delete" actually delete the objects.
   **/
  private void checkDeletablePlanElements() {
    Set s = getDeletablePlanElements();
    for (Iterator i = s.iterator(); i.hasNext();) {
      checkPlanElement((PlanElement) i.next());
    }
  }

  private void printAllPEs() {
    Collection c = blackboard.query(new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof PlanElement;
      }
    });
    if (!c.isEmpty()) {
      logger.debug("Undeletable Tasks");
      for (Iterator i = c.iterator(); i.hasNext();) {
        PlanElement pe = (PlanElement) i.next();
        String reason = canDelete(pe);
        if (reason == null) {
          logger.debug(
            pe.getTask().getUID()
              + " "
              + pe.getTask().getVerb()
              + ": Deletable");
        } else {
          logger.debug(
            pe.getTask().getUID()
              + " "
              + pe.getTask().getVerb()
              + ": "
              + reason);
        }
      }
    }
  }

  private String canDelete(PlanElement pe) {
    if (!isTimeToDelete(pe))
      return "Not time to delete";
    if (pe instanceof Allocation) {
      AllocationforCollections alloc = (AllocationforCollections) pe;
      Asset asset = alloc.getAsset();
      ClusterPG cpg = asset.getClusterPG();
      if (cpg != null) {
        MessageAddress destination = cpg.getMessageAddress();
        if (destination != null) {
          if (alloc.getAllocationTaskUID() == null) {
            return "Awaiting remote task creation";
          }
          if (!alloc.isAllocationTaskDeleted()) {
            return "Remote task not deleted";
          }
        }
      }
    }
    if (pe instanceof Expansion) {
      Expansion exp = (Expansion) pe;
      if (exp.getWorkflow().getTasks().hasMoreElements()) {
        return "Expands to non-empty workflow";
      }
    }
    Task task = pe.getTask();
    if (task instanceof MPTask) {
      MPTask mpTask = (MPTask) task;
      for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements();) {
        Task parent = (Task) e.nextElement();
        PlanElement ppe = parent.getPlanElement();
        // This is always an Aggregation
        String parentReason = canDelete(ppe);
        if (parentReason != null)
          return "Has undeletable parent: " + parentReason;
      }
    } else {
      UID ptuid = task.getParentTaskUID();
      if (ptuid != null) {
        PlanElement ppe = peSet.findPlanElement(ptuid);
        if (ppe != null) {
          if (!(ppe instanceof Expansion)) {
            String parentReason = canDelete(ppe);
            if (parentReason != null)
              return "Has undeletable parent: " + parentReason;
          }
        }
      }
    }
    return null;
  }

  /**
   * Check one plan element. The plan is already superficially
   * deletable, but cannot actually be deleted unless its task
   * is deletable.
   **/
  private void checkPlanElement(PlanElement pe) {
    if (isDeleteAllowed(pe)) {
      delete(pe.getTask());
    }
  }

  /**
   * A plan element is ready to delete if is time to delete the plan
   * element and if its task can be deleted without messing things
   * up. Its task can be deleted if it has no parent, is a subtask
   * of an expansion, or if its parent can be deleted.
   **/
  private boolean isDeleteAllowed(PlanElement pe) {
    if (pe == null)
      return true; // Hmmmmm, can this happen?
    Task task = pe.getTask();
    if (task instanceof MPTask) {
      MPTask mpTask = (MPTask) task;
      for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements();) {
        Task parent = (Task) e.nextElement();
        PlanElement ppe = parent.getPlanElement();
        // This is always an Aggregation
        if (!isDeleteAllowed(ppe))
          return false;
      }
      return true;
    } else {
      UID ptuid = task.getParentTaskUID();
      if (ptuid == null)
        return true; // Can always delete a root task
      PlanElement ppe = peSet.findPlanElement(ptuid);
      if (ppe == null) { // Parent is in another agent
        return true; // It's ok to delete it
      }
      if (ppe instanceof Expansion) {
        return true; // Can always delete a subtask
      } else {
        // Otherwise, can only delete if the pe can be deleted
        return getDeletablePlanElements().contains(ppe) && isDeleteAllowed(ppe);
      }
    }
  }

  //      private void delete(PlanElement pe) {
  //          delete(pe.getTask());
  //      }

  private void delete(Task task) {
    if (logger.isDebugEnabled())
      logger.debug("Deleting " + task);
    ((NewTask) task).setDeleted(true); // Prevent LP from propagating deletion
    if (task instanceof MPTask) {
      // Delete multiple parent tasks
      MPTask mpTask = (MPTask) task;
      if (logger.isDebugEnabled())
        logger.debug("Task is MPTask, deleting parents");
      for (Enumeration e = mpTask.getParentTasks(); e.hasMoreElements();) {
        Task parent = (Task) e.nextElement();
        delete(parent); // ppe is always an Aggregation
      }
      if (logger.isDebugEnabled())
        logger.debug("All parents deleted");
    } else {
      if (logger.isDebugEnabled())
        logger.debug("Checking parent");
      UID ptuid = task.getParentTaskUID();
      if (ptuid == null) {
        if (logger.isDebugEnabled())
          logger.debug("Deleting root " + task.getUID());
        deleteRootTask(task);
        numDeletedTasks++;
      } else {
        PlanElement ppe = peSet.findPlanElement(ptuid);
        if (ppe == null) { // Parent is in another agent
          // Delete the task
          if (logger.isDebugEnabled())
            logger.debug(
              "Parent " + ptuid + " is remote, deleting task" + task.getUID());
          deleteReceivedTask(task);
          numDeletedTasks++;
        } else {
          if (ppe instanceof Expansion) {
            if (logger.isDebugEnabled())
              logger.debug(
                "Parent is expansion of "
                  + ptuid
                  + ", deleting subtask "
                  + task.getUID());
            deleteSubtask((Expansion) ppe, task);
            numDeletedTasks++;
          } else {
            if (logger.isDebugEnabled())
              logger.debug("Parent is other, propagating");
            delete(ppe.getTask());
            // Not sure this is possible, but parallels "isDeleteAllowed"
          }
        }
      }
    }
  }

  private void deleteRootTask(Task task) {
    blackboard.publishRemove(task);
  }

  private void deleteReceivedTask(Task task) {
    blackboard.publishRemove(task);
  }

  /**
   * Delete a subtask of an expansion. Find all constraints where
   * the subtask is the constraining task and replace the constraint
   * with an absolute constraint against the constraining value.
   **/
  private void deleteSubtask(Expansion exp, Task subtask) {
    NewWorkflow wf = (NewWorkflow) exp.getWorkflow();
    List constraintsToRemove = new ArrayList();
    for (Enumeration e = wf.getTaskConstraints(subtask);
      e.hasMoreElements();
      ) {
      NewConstraint constraint = (NewConstraint) e.nextElement();
      if (constraint.getConstrainingTask() == subtask) {
        double value = constraint.computeValidConstrainedValue();
        constraint.setConstrainingTask(null);
        constraint.setAbsoluteConstrainingValue(value);
      } else if (constraint.getConstrainedTask() == subtask) {
        constraintsToRemove.add(constraint);
      }
    }
    wf.removeTask(subtask);
    for (Iterator i = constraintsToRemove.iterator(); i.hasNext();) {
      wf.removeConstraint((Constraint) i.next());
    }
    if (!wf.getTasks().hasMoreElements() && isTimeToDelete(exp)) {
      checkPlanElement(exp); // Ready to be deleted.
    }
    if (logger.isDebugEnabled())
      logger.debug("Deleting subtask " + subtask);
    blackboard.publishRemove(subtask);
  }

  private boolean isTimeToDelete(PlanElement pe) {
    long et = computeExpirationTime(pe);
    //  	if (logger.isDebugEnabled()) logger.debug("Expiration time is " + new java.util.Date(et));
    boolean result = et == 0L || et < scenarioNow;
    //          if (result) {
    //              if (logger.isDebugEnabled()) logger.debug("isTimeToDelete: " + new java.util.Date(et));
    //          }
    return result;
  }

  private long computeExpirationTime(PlanElement pe) {
    double et;
    Task task = pe.getTask();
    AllocationResult ar = pe.getReportedResult();
    if (ar == null) {
      ar = pe.getEstimatedResult();
    }
    et = PluginHelper.getEndTime(ar);
    if (Double.isNaN(et))
      try {
        et = PluginHelper.getEndTime(task);
      } catch (RuntimeException re) {
        et = Double.NaN;
      }
    if (Double.isNaN(et))
      et = PluginHelper.getStartTime(ar);
    if (Double.isNaN(et))
      try {
        et = PluginHelper.getStartTime(task);
      } catch (RuntimeException re) {
        et = Double.NaN;
      }
    if (Double.isNaN(et))
      return Long.MAX_VALUE; //return 0L;
    for (Iterator i = deletionPolicies.iterator(); i.hasNext();) {
      DeletionPolicy policy = (DeletionPolicy) i.next();
      if (policy.getPredicate().execute(task)) {
        return ((long) et) + policy.getDeletionDelay();
      }
    }
    return 0L;
    // Should not get here; DefaultDeletionPolicy should always apply
  }
}
