/*
 *
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

package org.cougaar.planning.ldm.lps;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.UniqueObjectSet;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.measure.AbstractMeasure;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Composition;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.ItineraryElement;
import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.planning.ldm.plan.LocationRangeScheduleElement;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PlanElementSet;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.PropertyTree;
import org.cougaar.util.Sortings;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Perform some basic consistency checking on Tasks and PlanElements
 */
public class ConsistencyChecker {

  /**
   * @param type the kind of task we are checking - or more
   * appropriately, when:<pre>
   * Added Task, Changed Task, Added PE's Task, Removed PE's Task.
   * </pre>
   */
  public static boolean isTaskConsistent(
      MessageAddress self,
      Logger logger,
      LogPlan logplan,
      Task t,
      String type) {

    // New local tasks should have a parent on the BBoard
    // That parent when tracing down should claim this task 
    // If the parent was expanded, then it's Exp should be same
    // as the task's workflow's Exp, and similarly the Workflows
    // should be the same. And the Exp should be on the BBoard
    // 1: pT on bb (if local task claiming a pT)
    // 2: pT has PE
    // 3: pT's PE is on bb
    // 4: If pT is Exp, it's workflow == t.getWorkflow != null
    // 5: if had workflow, it claims this task
    // 6: else if pT's PE is All,  it's getAllocationTaskUID() == t.getUID()
    // 7: else if pT's PE is Agg???? This task should be the MPTask
    // Note: Could put public helper in TaskImpl that takes
    // result of logplan.findTask(pUID), logplan.findPlanElement(parent)
    // -- or a ref to the logplan I suppose --,
    // and the self MessageAddress
    // Maybe cleaner that way?

    // If using Debug logging, then do all checks
    // even after one fails
    boolean debug = false;
    if (logger.isDebugEnabled())
      debug = true;
    boolean result = true;


    // Infrastructure could have removed this task already,
    // leaving it in an inconsistent state not worth checking
    Task bT = logplan.findTask(t.getUID());
    if (bT == null) {
      if (logger.isDebugEnabled())
	logger.debug(self + ": " + type + " removed by the time I looked: " + t);
      return true;
    } else if (bT != t) {
      if (logger.isInfoEnabled())
	logger.info(self + ": " + type + " doesn't match what logplan had for this UID. Checking task: " + t + ". LogPlan has: " + bT);
      if (! debug)
	return false;
      else
	result = false;
    }

    UID pUID = t.getParentTaskUID();
    MessageAddress dest = t.getSource();
    Workflow w = t.getWorkflow();
    // If pUID is non-null && local, 
    if (pUID != null && (self == dest || self.equals(dest.getPrimary()))) {
      // 1: this UID should be on the LogPlan.
      Task parent = logplan.findTask(pUID);
      if (parent == null) {
	// PROBLEM: Local task claims parent not on LogPlan
	if (logger.isInfoEnabled())
	  logger.info(self + ": " + type + " (local)'s parent not found on LogPlan: " + t);
	// Should later remove this task
	if (! debug)
	  return false;
	else
	  result = false;

	// Local task with parent avail
	if (w == null) {
	  // Added task whose parent is missing with no workflow.
	  // Nothing more to check.
	  return result;
	} else {
	  // Look at the workflow: Does it contain me?
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration en = w.getTasks();
	    while (en.hasMoreElements()) {
	      Task subT = (Task)en.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }
	  
	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": " + type + "'s workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	  
	  // Does it point to the same task as I point to?
	  Task wPTask = w.getParentTask();
	  if (wPTask != null) {
	    if (wPTask.getUID().equals(pUID)) {
	      parent = wPTask;
	      if (logger.isDebugEnabled())
		logger.debug(self + ": " + type + " whose parent was missing had a workflow that still pointed to the parent task. Task: " + t);
	    } else {
	      if (logger.isInfoEnabled())
		logger.info(self + ": " + type + " whose parent was missing had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	      // So the parent was not on the LogPlan, and the workflow
	      // pointed to a different parent. Bail out.
	      // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	      return false;
	    }
	  } else {
	    // local task with no parent but workflow also missing parent
	    // is a rescind in progress? From above,
	    // this task will be marked inconsistent already
	    if (logger.isInfoEnabled())
	      logger.info(self + ": " + type + " with missing parent whose workflow's parent link is null. Task: " + t);
	    return false;
	  }
	} // missing parent but had a workflow
      } // end of block handling missing parent task

      // Get here means the local parent was found.
	
      // 2: It should also have a non-null PE
      PlanElement ppe = parent.getPlanElement();
      PlanElement bppe = logplan.findPlanElement(parent);
      if (ppe == null) {
	// problem
	if (logger.isInfoEnabled()) 
	  logger.info(self + ": " + type + "'s parent has no PE. Task: " + t + ". Logplan lists pe: " + (bppe != null ? (bppe.getUID() + ":") : "") + bppe);
	// Should later remove both this task and the parent!!! FIXME!!!
	// Or maybe the parent is OK?
	if (! debug)
	  return false;
	else
	  result = false;

	// If the parent has no plan element, what else can I check?
	if (w == null) {
	  // Added task whose parent's planelement is missing with no workflow.
	  // Nothing more to check.
	  if (logger.isInfoEnabled())
	    logger.info(self + " " + type + " whose parent task's planelement is missing and has no workflow. Task: " + t + ", parent: " + parent);
	  return result;
	} else {
	  // Look at the workflow: Does it contain me?
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration en = w.getTasks();
	    while (en.hasMoreElements()) {
	      Task subT = (Task)en.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }
	  
	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": " + type + "'s workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	  
	  // Does it point to the same task as I point to?
	  Task wPTask = w.getParentTask();
	  if (wPTask != null) {
	    if (wPTask == parent) {
	      if (logger.isDebugEnabled())
		logger.debug(self + ": " + type + " whose parent's PE was missing had a workflow that still pointed to the parent task. Task: " + t);
	    } else {
	      if (logger.isInfoEnabled())
		logger.info(self + ": " + type + " whose parent's PE was missing had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	      // So the parent's PE was not on the LogPlan, and the workflow
	      // pointed to a different parent. Bail out.
	      // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	      return false;
	    }
	  } else {
	    // This task's workflow didn't point back to the task's parent!
	    if (logger.isInfoEnabled())
	      logger.info(self + ": " + type + " had parent missing PE & a workflow without a parent task. Task: " + t + ", workflow: " + w);
	    return false;
	  }
	} // missing parent's PE but had a workflow
	return false;
      } // handle no PE from parent task
      // At this point we've handled missing parent or parent missing PE.
      // Below we'll handle non-local task or not pointing
      // to a parent
      
      // 3: That PE should be on the LogPlan
      if (bppe != ppe) {
	// problem
	if (logger.isInfoEnabled())
	  logger.info(self + ": " + type + "'s parent's PE not on LogPlan consistently. Task: " + t + ", parent's PE: " + (ppe!= null ? (ppe.getUID()+":"+ppe) : ppe.toString()) + ", but LogPlan says: " + (bppe != null ? (bppe.getUID()+":"+bppe) : bppe.toString()));
	// Should later remove both this task and the parent!!! FIXME!!!
	// Should also probably remove both the planElement's referred
	// to here.... FIXME!!!
	// Or maybe the parent is OK, but the task and PEs are not?
	if (! debug)
	  return false;
	else
	  result = false;
      }

      if (ppe == null && bppe != null) {
	// If we get here the parent listed no PE,
	// but the blackboard PE is not. Use that to continue to debug
	ppe = bppe;
      }

      // That PE should be an Expansion or Aggregation (maybe Alloc too?)
      // 4: If the PE is an Expansion, then t.getWorkflow below 
      // should be non-null and == exp.getWorkflow()
      if (ppe instanceof Expansion) {
	Workflow pw = ((Expansion)ppe).getWorkflow();
	if (pw == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent's expansion had no workflow. Task: " + t + ", Expansion: " + ppe.getUID() + ":" + ppe);
	  // Should remove the task, parent, and Expansion? 
	  // Or maybe just the task? FIXME!!!
	  if (! debug)
	    return false;
	  else
	    result = false;
	}

	if (w == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent was Expanded, but this task has no workflow. Task: " + t);
	  // Task is clearly bad. But is parent OK? FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	} 

	if (w != pw) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent's expansion's workflow not same as this task's workflow. Task: " + t + " claims workflow: " + w + ", but parent has workflow: " + pw);
	  // Added task is bad. parent may be OK though? FIXME!
	  // All sub's of the added task's workflow are also suspect
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
	
	// 4.5: Extra check.
	if (w != null && w.getParentTask() == null) {
	  if (logger.isInfoEnabled()) 
	    logger.info(self + ": " + type + "'s workflow's parent is null. Task: " + t + ", workflow: " + w);
	  // The task and all subs of the workflow are bad. FIXME
	  // But the parent task pointed to this workflow, so is the 
	  // parent task also bad?
	  if (! debug)
	    return false;
	  else
	    result = false;
	}

	if (w != null && w.getParentTask() != parent) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent not same as " + type + "'s workflow's parent. Task: " + t + ", workflow: " + w);
	  // The workflow is pointed 2 from 2 directions, but it's upwards
	  // pointer is bad. Huh?
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
	
	if (w != null) {
	  // 5: Confirm that workflow has this subtask
	  boolean hasSub = false;
	  synchronized (w) {
	    Enumeration en = w.getTasks();
	    while (en.hasMoreElements()) {
	      Task subT = (Task)en.nextElement();
	      if (subT == t) {
		hasSub = true;
		break;
	      }
	    }
	  }

	  if (! hasSub) {
	    if (logger.isInfoEnabled()) 
	      logger.info(self + ": " + type + "'s workflow does not contain this task. Task: " + t + ", workflow: " + w);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	}
	
	// end of parent was expanded check
      } else if (ppe instanceof Allocation) {
	// 6: ppe Allocation must have this t's UID as allocTaskUID
	UID aUID = ((AllocationforCollections)ppe).getAllocationTaskUID();
	if (aUID == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent's allocation says AllocTask is null? Task: " + t + ", parent's alloc: " + ppe.getUID() + ":" + ppe);
	  // Task is bad. Allocation & parent may be OK FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	} else if (aUID != t.getUID()) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent's allocation's allocTask not same as this task. Task: " + t + ", allocation: " + ppe.getUID() + ":" + ppe);
	  // Task is bad. Alloc & parent may be OK - FIXME
	  if (! debug)
	    return false;
	  else
	    result = false;
	}
      } else if (ppe instanceof Aggregation) {
	// 7: If ppe is Aggregation?
	// This task should be the MPTask
	Composition c = ((Aggregation)ppe).getComposition();
	if (c == null) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + "'s parent's Aggregation PE had no composition. Task: " + t + " aggregation: " + ppe.getUID() + ":" + ppe);
	  if (! debug)
	    return false;
	  else
	    result = false;
	} else {
	  // Could check that composition has the parent task as a parent
	  MPTask mpt = c.getCombinedTask();
	  if (mpt == null) {
	    if (logger.isInfoEnabled())
	      logger.info(self + ": " + type + "'s parent's aggregation's composition had no CombinedTask. Task: " + t + ", aggregation: " + ppe.getUID() + ":" + ppe);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  } else if (mpt != t) {
	    if (logger.isInfoEnabled())
	      logger.info(self + ": " + type + "'s parent's aggregation's MPTask not same as this task. Task: " + t + ", mptask: " + mpt + ", aggregation: " + ppe.getUID() + ":" + ppe);
	    if (! debug)
	      return false;
	    else
	      result = false;
	  }
	}
      } // switch on type of parent PE
    } else if (w != null) {
      // task with no parent or parent is remote
      // Had no parent but it says it has a workflow?
      if (logger.isInfoEnabled())
	logger.info(self + ": " + type + " had no or non-local parent. Task Source: " + dest + ". For comparison, dest: " + t.getDestination() + ". But it has a workflow! Task: " + t + ", workflow: " + w);
      // Keep going? Does the workflow have a parent? Does
      // that parent exist? If so, maybe remove that parent
      // so it propogates back down to the Expansion & clears out
      // the workflow and removes the task?
      // Does the workflow contain this task?
      if (! debug)
	return false;
      else
	result = false;

      // Look at the workflow: Does it contain me?
      boolean hasSub = false;
      synchronized (w) {
	Enumeration en = w.getTasks();
	while (en.hasMoreElements()) {
	  Task subT = (Task)en.nextElement();
	  if (subT == t) {
	    hasSub = true;
	    break;
	  }
	}
      }
      
      if (! hasSub) {
	if (logger.isInfoEnabled()) 
	  logger.info(self + ": " + type + "'s workflow does not contain this task. Task: " + t + ", workflow: " + w);
	if (! debug)
	  return false;
	else
	  result = false;
      }
      
      // Does it point to the same task as I point to?
      Task wPTask = w.getParentTask();
      if (wPTask != null) {
	if (wPTask.getUID().equals(pUID)) {
	  if (logger.isDebugEnabled())
	    logger.debug(self + ": " + type + " whose parent was missing or remote had a workflow that still pointed to the parent task. Task: " + t + ", workflow: " + w);
	} else {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": " + type + " whose parent was missing or remote had a workflow that pointed to a different parent task. Task: " + t + ", workflow: " + w);
	  // So the parent was not on the LogPlan, and the workflow
	  // pointed to a different parent. Bail out.
	  // FIXME: Could check to see if that wf's parent task refers back to this workflow and/or is on the bboard & of course it's planelement
	}
      } else {
	if (logger.isInfoEnabled())
	  logger.info(self + ": " + type + " with remote or missing parent had a workflow with no parent. Task: " + t + ", workflow " + w);
      }
    } // missing or remote parent but had a workflow

    // Task with no or non-local parent and no workflow. Just fine.
    return result;
  }

  /**
   * @return true if the PlanElement should exist on the blackboard,
   * or false if it should be publishRemove'd
   */
  public static boolean isPlanElementConsistent(
      MessageAddress self,
      Logger logger,
      LogPlan logplan,
      PlanElement pe,
      boolean checkConsistency) {
    boolean result = true;

    Task task = pe.getTask();
    // Could check that the PE is still on the LogPlan at this point...
    if (logplan.findTask(task) == null) {
      if (logger.isDebugEnabled()) {
	logger.debug(self + ": Removing added planelement [task not found in the logplan] for " + task + " as " + pe.getUID() + ":" + pe);
      }
      //removePlanElement(pe, true);
      result = false;

      // FIXME: Log that I expect removal of any child task from this PE?

      // Unless we're doing debug logging, skip out here.
      if (! logger.isDebugEnabled())
	return result;
    }

    if (!checkConsistency)
      return result;

    // With the ASO fix, the below is hopefully extra:
    PlanElement bPE = logplan.findPlanElement(task);
    PlanElement tPE = task.getPlanElement();
    if (bPE == null) {
      // added PE not on LogPlan
      // This is OK if the PE were since removed. 
      if (tPE == null) {
	// And it's task doesn't point to it
	// In other words, it's been removed
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Added PE not on Task or LogPlan. Removed? " + pe.getUID() + ":" + pe);
      } else if (tPE != pe) {
	// And it's task points to a different PE
	// w/o ASO change, another thread may be adding a new PE
	// after this PE was removed. With that change,
	// this is an error.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE not on LogPlan (nothing is), but task points to another PE. Is another add in progress after this was removed? PE: " + pe.getUID() + ":" + pe + ", task: " + task + ", Task's new PE: " + tPE.getUID() + ":" + tPE);
      }
    } else if (bPE != pe) {
      // added PE not on LogPlan, another is.
      // This is OK if another add has already completed
      // We must watch though, w/o ASO change, for inconsistent pointers
      if (bPE == tPE) {
	// And its task points to this other PE
	// Does the other PE point to this task? Presumably?
	// This could happen if this PE were removed
	// And another added before this LP ran.

	// This is a strange thing for a PE to do, but i guess so...
	// I see lots of these allocate to InventoryAssets, PackaedPOL,
	// MaintainedItem -- diff UID, alloc to same NSN
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Added PE apparently since removed & replaced. Added: " + pe.getUID() +":" + pe + " replaced with " + bPE.getUID() + ":" + bPE + " for task " + task);
      } else if (tPE == null) {
	// Added PE's task points to no PE, but has a different
	// PE on the LogPlan. This shouldn't happen
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE not on task or LogPlan. Removed? Task points to no PE, but LogPlan points to a different PE!?! Task: " + task + ", LogPlan's PE: " + bPE.getUID() +":" + bPE);
      } else if (tPE == pe) {
	// Added PE's task points to it (ie no publishRemove called), 
	// but has a different PE on the LogPlan - ie
	// another publishAdd finished after this one. Maybe
	// Add 1 starts, then remove, then add #2 finishes, then add #1
	// finishes, then this LP runs?
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE is ref'ed by its task, but LogPlan has another. Added PE: " + pe.getUID() + ":" + pe + ", Task: " + task + ", LogPlan's PE: " + bPE.getUID() + ":" + bPE);
      } else {
	// Added PE's Task points to 1 PE, LogPlan to another, and neither
	// are the PE that was just added!
	if (logger.isWarnEnabled())
	  logger.warn(self + ": Added PE not ref'ed, and Task and LogPlan point to different PEs. Task " + task + ", Task's PE: " + tPE.getUID() + ":" + tPE + ", LogPlan's PE: " + bPE.getUID() + ":" + bPE);
      }
    } else {
      // LogPlan has this PE. Does the task?
      if (tPE == null) {
	// w/o ASO change it means a remove is in progress
	// With that change, this is an error.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE on LogPlan but Task has none. Is PE removal in progress? PE: " + pe.getUID() +":" + pe + ", Task: " + task);
      } else if (tPE != pe) {
	// Huh? W/o ASO change this may mean the first add was in
	// progress, a remove started but didn't finish,
	// then another add started (changing the task pointer),
	// and only now is the first add finishing.
	if (logger.isInfoEnabled())
	  logger.info(self + ": Added PE on LogPlan but Task already points to a different PE. Another remove and add both in progress? PE: " + pe.getUID() + ":" + pe + ", Task: " + task + ", Task's PE: " + tPE.getUID() + ":" + tPE);
      }
      // Task has what LogPlan has which is this PE. Normal case.
    }

    // Other to do: If it's an Expansion, does it have a workflow?
    
    // if !isTaskConsistent(task) then deferRescind(task)

    return result; 
  }
}
