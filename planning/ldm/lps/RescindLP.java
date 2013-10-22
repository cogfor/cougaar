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

package org.cougaar.planning.ldm.lps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AssetRescind;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.AssignedAvailabilityElement;
import org.cougaar.planning.ldm.plan.Composition;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.WorkflowImpl;
import org.cougaar.util.Enumerator;
import org.cougaar.util.MutableTimeSpan;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
 *
 * Attempts to do a complete LogPlan rescind walk, not depending on
 * being re-called to do the "next" level of rescind.
 * @property org.cougaar.planning.ldm.lps.RescindLP.checkBadTask defaults to true: When true, check for consistent Task & PEs on publishAdd/Remove
 * @property org.cougaar.planning.ldm.lps.RescindLP.removeBadTask. When this & checkBadTask are true, will also remove bad Tasks/PEs if the above checks suggest it. Defaults to true.
 **/
public class RescindLP
  implements LogicProvider, EnvelopeLogicProvider {

  private static final Logger logger = Logging.getLogger(RescindLP.class);

  // If true, check for link consistency on Task/PE add/remove
  private static final boolean CHECKCONSISTENCY = PropertyParser.getBoolean("org.cougaar.planning.ldm.lps.RescindLP.checkBadTask", true);

  // If true and find inconsistencies above, clean up
  // where possible
  private static final boolean DOREMOVES = PropertyParser.getBoolean("org.cougaar.planning.ldm.lps.RescindLP.removeBadTask", true);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  //private List conflictlist = new ArrayList();

  public RescindLP(
		   RootPlan rootplan,
		   LogPlan logplan,
		   PlanningFactory ldmf,
		   MessageAddress self) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
    this.self = self;
  }

  public void init() {
  }

  /**
   *  @param o  EnvelopeTuple
   *             where Envelope.Tuple.object is an ADDED PlanElement which contains
   *                             an Allocation to an agent asset.
   * Do something if the test returned true i.e. it was a PlanElement being removed
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    Object obj = o.getObject();
    if (o.isRemove()) {
      if (obj instanceof Task) {  // task
        taskRemoved((Task) obj);
      }else  if (obj instanceof PlanElement) {                    // PE
        planElementRemoved((PlanElement) obj);
      }
    } else if (o.isAdd()) {
      if (obj instanceof DeferredRescind) {
        processDeferredRescind((DeferredRescind) obj);
      } else if (obj instanceof PlanElement) {
        planElementAdded((PlanElement) obj);
      } else if (obj instanceof Task) {
	taskAdded((Task) obj);
      }
    } else if (o.isChange()) {
      if (obj instanceof Task)
	taskChanged((Task) obj);
    }
  }

  private void taskChanged(Task t) {
    // we really dont care. But use this opportunity to check for consistent blackboard objects
    // You should not publishChange a Task that is "inconsistent" (ie no parent). 

    // FIXME: Does this check things that could be broken temporarily for a task thats changing?
    // Will the deferred rescind do the right thing?
    if (CHECKCONSISTENCY) {
      if (!isTaskConsistent(t, "Changed Task")) {
	// create & publish new DeferredRescind
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Adding deferred rescind of inconsistent task " + t);
	rootplan.add(new DeferredRescind(t, "Changed Task"));
      }
    }
  }

  private void taskAdded(Task t) {
    // If the task has no parent, maybe it was just removed.
    // Do a DeferredRescind(checking) and see if that's still true.
    // If so, remove this task.
    
    // Also, if the task results from an Expansion or Aggregation, but that PE is not there, do similar.
    if (CHECKCONSISTENCY) {
      if (!isTaskConsistent(t, "Added Task")) {
	// create & publish new DeferredRescind
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Adding deferred rescind of inconsistent task " + t);
	rootplan.add(new DeferredRescind(t, "Added Task"));
      }
    }
  }

  // The type is a string indicating the kind of task we are checking - or more appropriately, when:
  // Added Task, Changed Task, Added PE's Task, Removed PE's Task.
  private boolean isTaskConsistent(Task t, String type) {
    return ConsistencyChecker.isTaskConsistent(
        self,
        logger,
        logplan,
        t,
        type);
  }

  private void planElementAdded(PlanElement pe) {
    if (!ConsistencyChecker.isPlanElementConsistent(
          self,
          logger,
          logplan,
          pe,
          CHECKCONSISTENCY)) {
      removePlanElement(pe, true);

      // Unless we're doing debug logging, skip out here.
      // Note that this means an inconsistent PE
      // will not have its Tasks consistency checked
      if (! logger.isDebugEnabled())
	return;
    }

    if (! CHECKCONSISTENCY)
      return;

    // If the task referenced by this PlanElement is inconsistent, remove it.
    Task task = pe.getTask(); 
    if (!isTaskConsistent(task, "Added PE's Task")) {
      // create & publish new DeferredRescind
      if (logger.isDebugEnabled())
	logger.debug(self + ": Adding deferred rescind of inconsistent task " + task);
      rootplan.add(new DeferredRescind(task, "Added PE's Task"));
    }
  }

  private void removeTaskFromWorkflow(Task t) {
    if (t == null)
      return;
    WorkflowImpl w = (WorkflowImpl)t.getWorkflow();
    if (w == null)
      return;

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

    if (hasSub) {
      if (logger.isInfoEnabled())
	logger.info(self + " removing task from workflow. Task: " + t);
      w.removeTask(t);
    }
  }

  private void processDeferredRescind(DeferredRescind deferredRescind) {
    if (deferredRescind.tr != null) {
      UID rtuid = deferredRescind.tr.getTaskUID();
      Task t = logplan.findTask(rtuid);
      if (t != null) {
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Found task for DeferredRescind. Removing " + t);
	removeTask(t);
	rootplan.remove(deferredRescind);
      } else {
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Never found task for DeferredRescind. Giving up on " + rtuid);
	rootplan.remove(deferredRescind);
      }
    } else if (deferredRescind.t != null) {
      // Check consistency as above.
      // assert CHECKCONSISTENCY == true
      if (!isTaskConsistent(deferredRescind.t, deferredRescind.type)) {
	if (DOREMOVES) {
	  if (logger.isInfoEnabled())
	    logger.warn(self + ": " + deferredRescind.type + " inconsistent after deferral, removing: " + deferredRescind.t);
	  // FIXME: remove parent task & PE too?
	  // If task is in a workflow, must first remove it from the workflow
	  removeTaskFromWorkflow(deferredRescind.t);
	  removeTask(deferredRescind.t);
	} else {
	  if (logger.isInfoEnabled())
	    logger.warn(self + ": " + deferredRescind.type + " inconsistent after deferral, NOT REMOVING: " + deferredRescind.t);
	  rootplan.remove(deferredRescind);
	}
      } else {
	if (logger.isInfoEnabled())
	  logger.info(self + ": " + deferredRescind.type + " was not, now is consistent after deferral. Leaving: " + deferredRescind.t);
	rootplan.remove(deferredRescind);
      }

    }
  }

  /** remove PE and any cascade objects */
  private void removePlanElement(PlanElement pe, boolean force) {
    if (pe != null) {
      // FIXME: Should this be lp.find(pe.getTask()) == pe??
      if (force || logplan.findPlanElement(pe.getTask()) != null) {
	rootplan.remove(pe);
	//      planElementRemoved(pe);
      }
    }
  }

  /** rescind the cascade of any PE (does not remove the PE) */
  private void planElementRemoved(PlanElement pe) {
    // Is the PE on the LogPlan or the Task? - if it is, re remove it
    Task t = pe.getTask();
    if (CHECKCONSISTENCY && t != null) {
      // First, check that the PEs Task is consistent - plan to remove it if not
      if (!isTaskConsistent(t, "Removed PE's Task")) {
	// create & publish new DeferredRescind
	if (logger.isDebugEnabled())
	  logger.debug(self + ": Adding deferred rescind of inconsistent task " + t);
	rootplan.add(new DeferredRescind(t, "Removed PE's Task"));
      }

      // Now check that the various pointers to the PE agree
      PlanElement bPE = logplan.findPlanElement(t);
      PlanElement tPE = t.getPlanElement();
      if (bPE == pe) {
	// removed PE still listed as on the LogPlan under it's task
	if (tPE == pe) {
	  // removed PE still pointed to by it's task
	  // So the PE is on the LogPlan
	  // and the task points to it - it's as though
	  // it never happened at all. Leave it?
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan and Task points to it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    // FIXME: Log what I expect to happen?
	    return;
	  }
	} else if (tPE == null) {
	  // the ASO link fixing happened by the PE
	  // is still on the LogPlan.
	  // Re-remove the PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan though Task points to no PE. Re removing it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    return;
	  }
	} else {
	  // the ASO link fixing happened by the PE and another
	  // PE is pointed to?
	  // is still on the LogPlan.
	  // Re-remove the PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE still on LogPlan though Task doesn't point to it. Re removing it. PE: " + pe.getUID() + ":" + pe + ", Task: " + t + ". Task points to PE: " + tPE.getUID() + ":" + tPE);
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    // Do the follow-on the next time around...
	    return;
	  }
	}
      } else if (tPE != bPE) {
	if (tPE == pe) {
	  // The PE is gone from the LogPlan, but
	  // the task still points to it. As though
	  // the ASO stuff didn't happen but the LogPlan stuff did.
	  // Note that bboard may have null or different PE
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": Removed PE not on LogPlan but Task still points to it. Re-removing. PE: " + pe.getUID() + ":" + pe + ", Task: " + t + ". Logplan has " + (bPE == null ? "null" : (bPE.getUID() + ":" + bPE)));
	  if (DOREMOVES) {
	    removePlanElement(pe, true);
	    return;
	  }
	} else {
	  // Removed PE not on LogPlan and not pointed to.
	  // But it's task also doesn't point to the PE 
	  // that is on the LogPlan under it's UID
	  // Task's PE may be null or different
	  if (logger.isInfoEnabled())
	    logger.info(self + " Removed PE was removed, but it's Task points to different PE than what LogPlan has under its UID. Task: " + t + ", LogPlan's PE: " + (bPE == null ? "null" : (bPE.getUID() + ":" + bPE)) + ", Task's PE: " + (tPE == null ? "null" : (tPE.getUID() + ":" + tPE)));
	}
      }
    } // end of error check PE removal

    //logger.printDot("p");
    if (pe instanceof Allocation) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      allocationRemoved((Allocation) pe);
    } else if (pe instanceof Expansion) {
      // Do nothing
    } else if (pe instanceof AssetTransfer) {
      // remove planelement from the asset's roleschedule
      //removePERS(pe);
      assetTransferRemoved((AssetTransfer) pe);
    } else if (pe instanceof Aggregation) {
      // Do nothing
    } else if (pe instanceof Disposition) {
      // do nothing since its the end of the line
    } else {
      logger.error(self + ": Unknown planelement "+pe, new Throwable());
    }
  }

  /** rescind the cascade of an allocation */
  private void allocationRemoved(Allocation all) {
    //logger.printDot("a");
    Asset a = all.getAsset();
    ClusterPG cpg = a.getClusterPG();
    if (cpg != null) {
      MessageAddress cid = cpg.getMessageAddress();
      if (cid != null) {
        UID remoteUID = ((AllocationforCollections) all).getAllocationTaskUID();
        if (remoteUID != null) {
	  if (logger.isDebugEnabled())
	    logger.debug(self + ": Removed Allocation, so will propagate and rescind alloc task. Alloc: " + all + ", alloc task: " + remoteUID);
          TaskRescind trm = ldmf.newTaskRescind(remoteUID, cid);
          ((AllocationforCollections) all).setAllocationTaskUID(null);
          rootplan.sendDirective((Directive) trm);
        }
      }
    }
  }

  /** remove a task and any PE addressing it */
  private void removeTask(Task task) {
    if (task != null) {
      rootplan.remove(task);
    }
  }

  /** remove the PE associated with a task (does not remove the task) */
  private void taskRemoved(Task task) {
    if (CHECKCONSISTENCY) {
      // Is the task on the LogPlan? If it is, re remove it    
      Task bT = logplan.findTask(task.getUID());
      if (bT == task) {
	if (DOREMOVES) {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": removed Task still on LogPlan? Re-removing " + task);
	  removeTask(task);
	  return; // Do the follow-on cleanup the next time around
	} else {
	  if (logger.isWarnEnabled())
	    logger.warn(self + ": removed Task still on LogPlan? " + task);
	}
      } else if (bT != null) {
	if(logger.isWarnEnabled())
	  logger.warn(self + ": removed Task UID listed as on LogPlan, but it's a different object. Removed " + task + " and LogPlan has " + bT);
	// FIXME: Remove task? bT?
	return;
      }
    } // end of consistency checks

    // get the planelement with this task
    PlanElement taskpe = task.getPlanElement();

    // FIXME: Does taskpe point to this task? Doe bpe point to this task?
    Task taskpetask = null;
    Task bpetask = null;

    PlanElement bpe = null;
    if (CHECKCONSISTENCY) {
      if (taskpe != null)
	taskpetask = taskpe.getTask();
      bpe = logplan.findPlanElement(task);
      if (bpe != null)
	bpetask = bpe.getTask();
      if (taskpe != bpe) {
	// Task and logplan reference different PEs for this Task
	if (taskpe == null) {
	  // Task has no PE link (as though the PE had already been removed)
	  // But the logplan has a PE still
	  // Presumably a publishRemove(PE) just happened in another transaction,
	  // but has not finished.
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Removed Task has no PE, but LogPlan still lists a PE under this UID. Task: " + task + ", LogPlan's PE: " + bpe.getUID() + ":" + bpe + ". LogPlan PE's Task: " + bpetask);
	} else if (bpe == null) {
	  // Presumably a publishAdd(PE) just happened in another transaction,
	  // but has not finished
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Removed task lists a PE, but LogPlan has no PE under this UID. Task: " + task + ", Task's PE: " + taskpe.getUID() + ":" + taskpe + ". Task's PE's Task: " + taskpetask);
	} else {
	  if (logger.isWarnEnabled()) {
	    logger.warn(self + ": Removed task lists a different PE than LogPlan has. Task: " + task + ", Task's PE: " + taskpe.getUID() + ":" + taskpe + ", LogPlan's PE: " + bpe.getUID() + ":" + bpe);
            if (logger.isInfoEnabled()) {
	      logger.info(self + "..... Task's PE's Task: " + taskpetask + ", LogPlan's PE's Task: " + bpetask);
            }
	  }
	}
      } else {
	// taskpe == bpe
	if (bpe == null) {
	  // Removed task has no PE and neither does LogPlan
	} else if (taskpetask != task) {
	  if (logger.isInfoEnabled())
	    logger.info(self + ": Removed Task lists same PE as LogPlan has for it, but the PE points to a different Task. Task: " + task + ", Task's PE: " + taskpe.getUID() + ":" + taskpe + ". Task's PE's Task: " + taskpetask);
	}
      } // end of consistency checks
      
      // rescind (or remove) this planelement from the logplan
      if (DOREMOVES && bpe != null) {
	removePlanElement(bpe, false);
      }
    }
    
    // If we didn't just remove the only PE above, remove
    // the pe referenced by the task
    if (taskpe != bpe && taskpe != null) {
      // A false below would mean that if bpe == null then taskpe won't
      // be removed. That's old behavior.
      // Use DOREMOVES to toggle this.
      removePlanElement(taskpe, DOREMOVES);
    }
  }

  /** remove the cascade associated with an AssetTransfer **/
  private void assetTransferRemoved(AssetTransfer at) {
    // create an AssetRescind message
    Schedule rescindSchedule;


    //Remove info from local assets
    Asset localAsset = logplan.findAsset(at.getAsset());
    if (localAsset == null) {
      logger.error(self + ": Rescinded transferred asset - " + 
		   at.getAsset() + " - not found in logplan.");
      return;
    }


    if ((at.getAsset() instanceof HasRelationships) &&
        (at.getAssignee() instanceof HasRelationships)) {
      rescindSchedule = ldmf.newAssignedRelationshipSchedule();
      RelationshipSchedule transferSchedule = 
        ((HasRelationships)at.getAsset()).getRelationshipSchedule();

      for (Iterator iterator = new ArrayList(transferSchedule).iterator();
           iterator.hasNext();) {
        Relationship relationship = (Relationship)iterator.next();
        ((NewSchedule)rescindSchedule).add(ldmf.newAssignedRelationshipElement(relationship));
      }
      
      HasRelationships localAssignee = (HasRelationships)logplan.findAsset(at.getAssignee());
      if (localAssignee == null) {
        logger.error(self + ": Rescinded assignee - " + 
		     at.getAssignee() + " - not found in logplan.");
        return;
      }

      // Update local relationship schedules
      RelationshipSchedule localSchedule = 
        ((HasRelationships) localAsset).getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);        
      
      localSchedule = localAssignee.getRelationshipSchedule();
      localSchedule.removeAll(transferSchedule);
      
      // Update asset avail
      // Remove all current entries denoting asset avail to assignee
      // Will add in new entry based on the current relationship schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule) localAsset.getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        Collection remove = assetAvailSchedule.filter(new UnaryPredicate() {
	    public boolean execute(Object o) {
	      return ((o instanceof AssignedAvailabilityElement) &&
		      (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	    }  
	  });
        assetAvailSchedule.removeAll(remove);
      } // end sync block

      // Get all relationships with asset
      RelationshipSchedule relationshipSchedule = 
        (localAssignee).getRelationshipSchedule();
      Collection collection = 
        relationshipSchedule.getMatchingRelationships((HasRelationships) localAsset,
                                                      new MutableTimeSpan());
      
      // If any relationships, add a single avail element with the 
      // min start and max end
      if (collection.size() > 0) {
        Schedule schedule = ldmf.newSchedule(new Enumerator(collection));
        
        // Add a new avail element
        synchronized (assetAvailSchedule) {
          assetAvailSchedule.add(ldmf.newAssignedAvailabilityElement((Asset)localAssignee,
                                                                     schedule.getStartTime(),
                                                                     schedule.getEndTime()));
        }
      }

      rootplan.change(localAsset, null);
      rootplan.change(localAssignee, null);
    } else {
      rescindSchedule = at.getSchedule();

      // Update asset avail - remove all current entries which match the rescind
      // schedule
      NewSchedule assetAvailSchedule = 
        (NewSchedule)((Asset)localAsset).getRoleSchedule().getAvailableSchedule();
      final Asset assignee = at.getAssignee();
      synchronized (assetAvailSchedule) {
        //final Asset asset = (Asset)localAsset;
        Collection assignedAvailSchedule = assetAvailSchedule.filter(new UnaryPredicate() {
	    public boolean execute(Object o) {
	      return ((o instanceof AssignedAvailabilityElement) &&
		      (((AssignedAvailabilityElement)o).getAssignee().equals(assignee)));
	    }  
	  });
        
        //iterate over rescind schedule and remove matching avail elements
        for (Iterator iterator = rescindSchedule.iterator();
             iterator.hasNext();) {
          ScheduleElement rescind = (ScheduleElement)iterator.next();
    
          Iterator localIterator = assignedAvailSchedule.iterator();
      
          //boolean found = false;
          while (localIterator.hasNext()) {
            ScheduleElement localAvailability = 
              (ScheduleElement)localIterator.next();

            if ((rescind.getStartTime() == localAvailability.getStartTime()) &&
                (rescind.getEndTime() == localAvailability.getEndTime())) {
              assignedAvailSchedule.remove(localAvailability);
              break;
            }
          }
        }
      }
      rootplan.change(localAsset, null);
    }
   
    AssetRescind arm = ldmf.newAssetRescind(at.getAsset(), 
                                            at.getAssignee(),
                                            rescindSchedule);
    rootplan.sendDirective((Directive)arm);
  }
  
  /** remove the plan element from the asset's roleschedule **/
  private void removePERS(PlanElement pe) {
    /*
      boolean conflict = false;
    */
    Asset rsasset = null;
    if (pe instanceof Allocation) {
      Allocation alloc = (Allocation) pe;
      rsasset = alloc.getAsset();
      /*
	if ( alloc.isPotentialConflict() ) {
        conflict = true;
	}
      */
    } else if (pe instanceof AssetTransfer) {
      AssetTransfer at = (AssetTransfer) pe;
      rsasset = at.getAsset();
      /*
	if ( at.isPotentialConflict() ) {
        conflict = true;
	}
      */
    }
    if (rsasset != null) {
      if (logger.isDebugEnabled()) {
	logger.debug(self + " RESCIND REMOVEPERS called for: " + rsasset);
      }
      /*
	RoleScheduleImpl rsi = (RoleScheduleImpl) rsasset.getRoleSchedule();
	// if the pe had a conflict re-check the roleschedule
	if (conflict) {
        checkConflictFlags(pe, rsi);
	}
      */
    } else {
      if (logger.isWarnEnabled())
	logger.warn(self + " Could not remove rescinded planelement");
    }
  }
  
  /*
    // if the rescinded pe had a potential conflict re-set the conflicting pe(s)
    private void checkConflictFlags(PlanElement pe, RoleSchedule rs) {
    // re-set any existing items in the conflict list.
    conflictlist.clear();
    AllocationResult estar = pe.getEstimatedResult();
    
    // make sure that the start time and end time aspects are defined.
    // if they aren't, don't check anything
    // (this could happen with a propagating failed allocation result).
    if ( (estar.isDefined(AspectType.START_TIME) ) && (estar.isDefined(AspectType.END_TIME) ) ) {
    Date sdate = new Date( ((long)estar.getValue(AspectType.START_TIME)) );
    Date edate = new Date( ((long)estar.getValue(AspectType.END_TIME)) );
    
    // check for encapsulating schedules of other plan elements.
    OrderedSet encap = rs.getEncapsulatedRoleSchedule(sdate, edate);
    Enumeration encapconflicts = encap.elements();
    while (encapconflicts.hasMoreElements()) {
    PlanElement conflictpe = (PlanElement) encapconflicts.nextElement();
    // make sure its not our pe.
    if ( !(conflictpe == pe) ) {
    conflictlist.add(conflictpe);
    }
    }
    
    // check for ovelapping schedules of other plan elements.
    OrderedSet overlap = rs.getOverlappingRoleSchedule(sdate, edate);
    Enumeration overlapconflicts = overlap.elements();
    while (overlapconflicts.hasMoreElements()) {
    PlanElement overconflictpe = (PlanElement) overlapconflicts.nextElement();
    // once again, make sure its not our pe.
    if ( !(overconflictpe == pe) ) {
    conflictlist.add(overconflictpe);
    }
    }
    }
    
    if ( ! conflictlist.isEmpty() ) {
    ListIterator lit = conflictlist.listIterator();
    while ( lit.hasNext() ) {
    RoleScheduleConflicts conpe = (RoleScheduleConflicts) lit.next();
    // re-set this pe's conflict flag to false
    conpe.setPotentialConflict(false);
    // set the check flag to true so that the RoleScheduleConflictLP will
    // run again on the publish change in case this pe had conflicts with
    // other pe's (besides the one that was just rescinded)
    conpe.setCheckConflicts(true);
    rootplan.change(conpe);
    }
    }
    }
  */

  public static class DeferredRescind implements java.io.Serializable {
    public TaskRescind tr = null;
    public Task t = null;
    public int tryCount = 0;
    public String type = null;
    public DeferredRescind(TaskRescind tr) {
      this.tr = tr;
    }
    // Used for deferring the rescind of an inconsistent task
    public DeferredRescind(Task t, String type) {
      this.t = t;
      this.type = type;
    }
  }

}
