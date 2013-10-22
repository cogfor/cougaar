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

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RestartLogicProviderHelper;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;


/** RemoteAllocationLP class provides the logic to capture 
 * Allocations against remote agents 
 *
 **/

public class RemoteAllocationLP
implements LogicProvider, EnvelopeLogicProvider, RestartLogicProvider
{
   // Tasks older than this are not sent to other agents.
  private static final long VALID_TASK_TIME_OFFSET = 86400000L;

  private static final Logger logger = Logging.getLogger(RemoteAllocationLP.class);

  private final RootPlan rootplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;
  private final AlarmService alarmService;
  private final LogPlan logplan;
//   private final Workflow specialWorkflow = new SpecialWorkflow();

  // Whether to confirm the Task being allocated exists before sending the allocation. Defaults to true.
  private static final boolean CHECKALLOC = PropertyParser.getBoolean("org.cougaar.planning.ldm.lps.RemoteAllocationLP.checkBadTask", true);

  public RemoteAllocationLP(
      RootPlan rootplan,
      PlanningFactory ldmf,
      MessageAddress self,
      AlarmService alarmService,
      LogPlan logplan)
  {
    this.rootplan = rootplan;
    this.ldmf = ldmf;
    this.self = self;
    this.alarmService = alarmService;
    this.logplan = logplan;
    // logger is static final now
    //logger = new LoggingServiceWithPrefix(logger, self + ": ");
  }

  public void init() {
  }

  private long currentTimeMillis() {
    return alarmService.currentTimeMillis();
  }

  private void examineTask(Object obj, Collection changes) {
    if (obj instanceof Task) {
      Task task = (Task) obj;
      PlanElement pe = task.getPlanElement();
      if (logger.isDebugEnabled()) {
        logger.debug("examineTask " + task + ", pe=" + pe);
      }
      examine(pe, changes);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("examine(non)Task " + obj);
      }
    }
  }

  private void examine(Object obj, Collection changes) {
    if (! (obj instanceof Allocation)) return;
    AllocationforCollections all = (AllocationforCollections) obj;
    Task task = all.getTask();

    // Confirm that the Task exists. If it does not, then 
    // do not send this. 
    if (CHECKALLOC) {
      if (logplan.findTask(task) == null) {
	// The Task being allocated is not on the Blackboard any more. Note that this will INFO about
	// Allocations to local Assets as well as remote Agents
	if (logger.isInfoEnabled())
	  logger.info(self + ": RemoteAllocationLP: Allocation of rescinded Task: " + task + " using new Allocation " + all);
	return;
      }
    }

    Asset asset = all.getAsset();
    ClusterPG cpg = asset.getClusterPG();
    if (cpg == null) return;
    MessageAddress destination = cpg.getMessageAddress();
    if (destination == null) return;
    if (!taskShouldBeSent(task)) {
      if (logger.isDebugEnabled()) {
        logger.debug("shouldNotBeSent: " + task);
      }
      return; // In past
    }

    // see if we're reissuing the task... if so, we'll just use it.
    UID copyUID = all.getAllocationTaskUID();
    boolean isDeleted = all.isAllocationTaskDeleted();
    Task copytask = prepareRemoteTask(task, destination, copyUID, isDeleted);
    ((AllocationforCollections)all).setAllocationTask(copytask);
    rootplan.change(all, changes); 

    // Give the task directive to the blackboard for transmission
    sendTask(copytask, changes);
  }

  private void sendTask(Task copytask, Collection changes) {
//     if (copytask.getWorkflow() == null) {
//       NewTask nt = (NewTask) copytask;
//       nt.setWorkflow(specialWorkflow);
//     }
    if (logger.isDebugEnabled()) {
      logger.debug("Send task: " + copytask);
    }
    rootplan.sendDirective(copytask, changes);
  }

  /**
   * Handle one EnvelopeTuple. Call examine to check for objects that
   * are Allocations to a remote agent.
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (o.isAdd()) {
      examine(obj, changes);
    } else if (o.isChange()) {
      examineTask(obj, changes);
    } else if (o.isBulk()) {
      Collection c = (Collection) obj;
      for (Iterator e = c.iterator(); e.hasNext(); ) {
        examine(e.next(), changes);
      }
    }
  }

  /**
   * If a agent restarts, we resend all the tasks we sent before in
   * case they have been lost or are out of date.
   **/
  public void restart(final MessageAddress cid) {
    if (logger.isInfoEnabled()) {
      logger.info(
        "Reconcile with "+
        (cid==null?"all agents":cid.toString()));
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof Allocation) {
          Allocation alloc = (Allocation) o;
          Asset asset = alloc.getAsset();
          ClusterPG cpg = asset.getClusterPG();
          if (cpg == null) return false;
          MessageAddress destination = cpg.getMessageAddress();
          return 
            RestartLogicProviderHelper.matchesRestart(
              self, cid, destination);
        }
        return false;
      }
    };
    Enumeration en = rootplan.searchBlackboard(pred);
    while (en.hasMoreElements()) {
      AllocationforCollections alloc = (AllocationforCollections) en.nextElement();
      UID remoteTaskUID = alloc.getAllocationTaskUID();
      Task localTask = alloc.getTask();
      if (remoteTaskUID != null && taskShouldBeSent(localTask)) {
        Asset asset = alloc.getAsset();
        ClusterPG cpg = asset.getClusterPG();
        MessageAddress destination = cpg.getMessageAddress();
        Task remoteTask = prepareRemoteTask(localTask, destination, remoteTaskUID, false);
        if (logger.isInfoEnabled()) {
          logger.info(
              "Resend" + (cid == null ? "*" : "")
              + " task to " + remoteTask.getDestination()
              + " with remoteUID=" + remoteTaskUID
              + " " + localTask);
        }
        sendTask(remoteTask, null);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info("Reconciled");
    }
  }

  private boolean taskShouldBeSent(Task task) {
    double et;
    try {
      et = PluginHelper.getEndTime(task);
    } catch (RuntimeException re) {
      et = Double.NaN;
    }
    if (Double.isNaN(et))
      try {
        et = PluginHelper.getStartTime(task);
      } catch (RuntimeException re) {
        et = Double.NaN;
      }
    if (Double.isNaN(et)) return true; // Can't tell, send it

    // Require end time to be later than 1 day before now. In other words,
    // Task must be at least 1 day in the past for it to be dropped.
    long minValidTaskTime = currentTimeMillis() - VALID_TASK_TIME_OFFSET;
    boolean shouldSend = ((long) et) >= minValidTaskTime;

    if (!shouldSend && logger.isInfoEnabled()) {
      Date thePref = new Date((long)et);
      long myCurrentTime = currentTimeMillis();
      Date myCurrentTimeDate = new Date(myCurrentTime);
      logger.info(self + ": " + task + " has end time pref earlier than minValidTime (now - 1 day) of " + thePref + ". Will not Allocate task due more than 1 day ago! Now is: " + myCurrentTimeDate);
    }

    return shouldSend;
  }

  private Task prepareRemoteTask(Task task, MessageAddress dest, UID uid, boolean isDeleted) {
    NewTask nt;
    /*
    if (task instanceof MPTask) {
      nt = ldmf.newMPTask();
      ((NewMPTask)nt).setParentTasks(((MPTask)task).getParentTasks());
    }
    */
    nt = ldmf.newTask(uid);
    nt.setDeleted(isDeleted);
    nt.setParentTask(task);             // set ParenTask to original task

    // redundant: ldmf initializes it.
    //nt.setSource(self);

    // FIXME MIK WARNING! WARNING!
    // as a hack, we've made setDestination bark if it isn't the current
    // agent (suspicious use).  In order to prevent the below from 
    // generating barkage, we've got a (privately) muzzle...
    //nt.setDestination(dest);
    // 
    ((org.cougaar.planning.ldm.plan.TaskImpl)nt).privately_setDestination(dest);
    nt.setVerb(task.getVerb());
    nt.setDirectObject(task.getDirectObject());
    nt.setPrepositionalPhrases(task.getPrepositionalPhrases());
    Date commitmentDate = task.getCommitmentDate();
    if (commitmentDate != null) nt.setCommitmentDate(commitmentDate);
    // no workflow
    synchronized (task) {
      nt.setPreferences(task.getPreferences());
    }
    nt.setPriority(task.getPriority());
    nt.setPlan(task.getPlan());
    nt.setAuxiliaryQueryTypes(task.getAuxiliaryQueryTypes());
    nt.setContext(task.getContext());

    return nt;
  }
}
