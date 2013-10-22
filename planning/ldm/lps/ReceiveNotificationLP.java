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

import org.cougaar.core.blackboard.BlackboardServesDomain;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.domain.DelayedLPAction;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Notification;
import org.cougaar.planning.ldm.plan.PEforCollections;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.planning.ldm.plan.WorkflowImpl;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Take an incoming Notification Directive and
 * perform Modification to the LOGPLAN
 **/
public class ReceiveNotificationLP
  implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveNotificationLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;

  public ReceiveNotificationLP(
      RootPlan rootplan,
      LogPlan logplan,
      PlanningFactory ldmf) {
    this.rootplan = rootplan;
    this.logplan = logplan;
    this.ldmf = ldmf;
  }

  public void init() {
  }

  /**
   *  perform updates -- per Notification ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    if (dir instanceof Notification) {
      processNotification((Notification) dir, changes);
    }
  }

  private void processNotification(Notification not, Collection changes) {
    UID tuid = not.getTaskUID();
    UID childuid = not.getChildTaskUID();
    PlanElement pe = logplan.findPlanElement(tuid);
    boolean needToRescind = (pe == null);

    if (logger.isDebugEnabled() && needToRescind)
      logger.debug("Got notification for task with no published PlanElement - will rescind the task: " + tuid);

    // verify that the pe matches the task
    if (!needToRescind &&  (pe instanceof AllocationforCollections)) {
      UID remoteTUID = ((AllocationforCollections)pe).getAllocationTaskUID();
      if (remoteTUID == null) {
        needToRescind = true;
      } else {
        if (!(remoteTUID.equals(childuid))) {
          // this was likely due to replacing the Allocation
          if (logger.isInfoEnabled()) {
            logger.info("Got a Notification for the wrong allocation:"+
                        "\n\tTask="+tuid+
                        "  ("+pe.getTask().getUID()+")"+
                        "\n\tFrom="+childuid+
                        "  ("+remoteTUID+")"+
                        "\n\tResult="+not.getAllocationResult()+"\n"+
                        "\n\tPE="+pe);
          }
          needToRescind = true; // Insure that the old child task is gone.
          return;
        }
      }
    }

    if (needToRescind) {
      TaskRescind trm = ldmf.newTaskRescind(childuid, not.getSource());
      if (logger.isDebugEnabled())
	logger.debug("Sending new TaskRescind for " + childuid);
      rootplan.sendDirective(trm, changes);
    } else {
      AllocationResult ar = not.getAllocationResult();
      propagateNotification(
          rootplan, logplan, pe, tuid, ar, childuid, changes);
    }
  }

  // default protection so that NotificationLP can call this method
  static final void propagateNotification(
      RootPlan rootplan,
      LogPlan logplan,
      UID tuid, AllocationResult result,
      UID childuid, Collection changes) {
    PlanElement pe = logplan.findPlanElement(tuid);
    if (pe != null) {
      propagateNotification(
          rootplan, logplan, pe, tuid, result, childuid, changes);
    } else {
      if (logger.isDebugEnabled()) {
	logger.debug("Received notification about unknown task: "+tuid);
      }
      // FIXME: Doesn't this mean that the downstream Task's parent is missing or screwy,
      // and the downstream task should be rescinded?
    }
  }

  // default protection so that NotificationLP can call this method
  static final void propagateNotification(
      RootPlan rootplan,
      LogPlan logplan,
      PlanElement pe,
      UID tuid, AllocationResult result,
      UID childuid, Collection changes) {

    // In general, do not pubChange PE if nothing changed. Primary job here is to propagate Received
    // result, so if that hasn't changed, don't publishChange. However, Expansions are 
    // different since tuid task is not same as childuid (see bug 3462).
    // (the big win here is less work during reconciliation when all notifications are resent)

    if ((pe instanceof Allocation) ||
        (pe instanceof AssetTransfer) ||
        (pe instanceof Aggregation)) {

      // compare getReceivedResult .isEqual with this new one -- reconciliation after restart
      // is going to resend all the ARs, and we should avoid propagating the changes
      AllocationResult currAR = pe.getReceivedResult();
      if ((result == null && currAR == null) || (result != null && result.isEqual(currAR))) {
	if (logger.isInfoEnabled()) {
	  logger.info("Not propagating unchanged ReceivedResult for PE " + pe + ", new result: " + result);
	}
	return;
      }

      ((PEforCollections) pe).setReceivedResult(result);
      if (logger.isDebugEnabled())
	logger.debug("pubChanging local PE with new ReceivedResult: " + pe);
      rootplan.change(pe, changes);
    } else if (pe instanceof Expansion) {
      // Note that below we avoid pubChanging the expansion if the newly calculated AR is same as
      // the old
      rootplan.delayLPAction(
          new DelayedAggregateResults((Expansion)pe, childuid));

      /*
      Workflow wf = ((Expansion)pe).getWorkflow();
      AllocationResult ar = wf.aggregateAllocationResults();
      if (ar != null) {
	// get the TaskScoreTable used in the aggregation
	TaskScoreTable aggTST = ((WorkflowImpl)wf).getCurrentTST();
	// get the UID of the child task that caused this aggregation
	((ExpansionImpl)pe).setSubTaskResults(aggTST,childuid);
        ((PEforCollections) pe).setReceivedResult(ar);
	rootplan.change(pe, changes);
      } // if we can't successfully aggregate the results - don't send a notification
      */
    /*
    } else if (pe instanceof Disposition) {
      // drop the notification on the floor - cannot possibly be valid
    }
    */
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("Got a Notification for an inappropriate PE:\n"+
                    "\tTask="+tuid+"\n"+
                    "\tFrom="+childuid+"\n"+
                    "\tResult="+result+"\n"+
                    "\tPE="+pe);
      }
    }
  }

  /** delay the results aggregation of an expansion until the end in case
   * we have lots of them to do.
   **/
  private final static class DelayedAggregateResults
    implements DelayedLPAction
  {
    private final Expansion pe;
    private final ArrayList ids = new ArrayList(1);
    DelayedAggregateResults(Expansion pe, UID id) {
      this.pe = pe;
      ids.add(id);
    }

    public void execute(BlackboardServesDomain bb) {
      WorkflowImpl wf = (WorkflowImpl) pe.getWorkflow();

      // compute the new result from the subtask results.
      try {
        AllocationResult ar = wf.aggregateAllocationResults(ids);
	AllocationResult currAR = pe.getReceivedResult();
	// If the newly calculated result is at all different from the previously 
	// calculated result, then make the change and publishChange the Expansion
	if ((ar == null && currAR != null) || (ar != null && !ar.isEqual(currAR))) {
	  // set the result on the Expansion
	  ((PEforCollections) pe).setReceivedResult(ar);
	  
	  // publish the change to the blackboard.
	  
	  // Note that the above setReceivedResult puts a PlanElement.ReportedResultChangeReport
	  // on this transaction.
	  bb.change(pe, null); // drop the change details.
	  //bb.change(pe, changes);
	  //Logging.printDot("=");
	} else {
	  if (logger.isInfoEnabled())
	    logger.info("NOT publishChanging Expansion " + pe + " - new ReceivedResult same as old: " + ar);
        }
      } catch (RuntimeException re) {
        logger.error("Caught exception while processing DelayedAggregateResults for "+pe, re);
      }
    }

    /** hashcode is the hashcode of the expansion **/
    public int hashCode() {
      return pe.hashCode();
    }

    /** these guys are equal iff the they have the same PE **/
    public boolean equals(Object e) {
      return (e instanceof DelayedAggregateResults &&
              ((DelayedAggregateResults)e).pe == pe);
    }

    /** merge another one into this one **/
    public void merge(DelayedLPAction e) {
      // don't bother to check, since we will only be here if this.equals(e).
      DelayedAggregateResults other = (DelayedAggregateResults) e;
      ids.addAll(other.ids);
    }
  }
}
