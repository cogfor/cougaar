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
import java.util.Enumeration;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RestartLogicProviderHelper;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.NewNotification;
import org.cougaar.planning.ldm.plan.PEforCollections;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskScoreTable;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
 *
 **/
public class NotificationLP
implements LogicProvider, EnvelopeLogicProvider, RestartLogicProvider
{
  private static Logger logger = Logging.getLogger(NotificationLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  public NotificationLP(
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
   *  @param o an Envelope.Tuple.object is an ADDED 
   * PlanElement which contains an Allocation to an Organization.
   * Do something if the test returned true i.e. it was an Allocation
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if ( ( o.isAdd() && (obj instanceof PlanElement)) ||
         ( o.isChange() &&
           ( obj instanceof PlanElement ) &&
           ((PEforCollections) obj).shouldDoNotification())
         ) {
      PlanElement pe = (PlanElement) obj;
      if (logger.isDebugEnabled())
	logger.debug("Got a PE to do checkValues on: " + pe.getUID());
      checkValues(pe, changes);
    } 
  }

  public void restart(final MessageAddress cid) {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof PlanElement) {
          PlanElement pe = (PlanElement) o;
          MessageAddress source = pe.getTask().getSource();
          return RestartLogicProviderHelper.matchesRestart(self, cid, source);
        }
        return false;
      }
    };
    Enumeration en = rootplan.searchBlackboard(pred);
    while (en.hasMoreElements()) {
      PlanElement pe = (PlanElement) en.nextElement();
      checkValues(pe, null);
    }
  }

  private void checkValues(PlanElement pe, Collection changes) {
    checkValues(pe, changes, rootplan, logplan, ldmf, self);
  }

  static final void checkValues(PlanElement pe, Collection changes, RootPlan rootplan, LogPlan logplan, PlanningFactory ldmf, MessageAddress self) {
    Task task = pe.getTask();

    if (logger.isDebugEnabled()) {
      logger.debug("\n" + self + ": task = " + task );
    }

    ((PEforCollections)pe).setNotification(false);
    if (task instanceof MPTask) {
      TaskScoreTable resultsbytask = ((MPTask)task).getComposition().calculateDistribution();
      if (resultsbytask != null) {
	Enumeration etasks = ((MPTask)task).getParentTasks();
	while (etasks.hasMoreElements()) {
	  Task pt = (Task) etasks.nextElement();
	  if (pt != null) {
	    AllocationResult result = resultsbytask.getAllocationResult(pt);
	    createNotification(pt.getUID(), task, result, changes, rootplan, logplan, ldmf, self);
	  } // else no notification need be generated
	}
      }
    } else {
      UID ptuid = task.getParentTaskUID();
      if (ptuid != null) {
	AllocationResult ar = pe.getEstimatedResult();
	createNotification(ptuid, task, ar, changes, rootplan, logplan, ldmf, self);
      } // else no notification need be generated
    }
  }
  
  static final void createNotification(UID ptuid, Task t, AllocationResult ar, Collection changes, RootPlan rootplan, LogPlan logplan, PlanningFactory ldmf, MessageAddress self) {

    if (logger.isDebugEnabled()) {
      PlanElement pe = null;
      if (t != null) {
	pe = t.getPlanElement();
	if (pe != null) {
          logger.debug("Doing checkNotification for PE: " + pe);
        }
      } else {
	logger.warn("Got null Task in createNotification?!");
      }
    }

    MessageAddress dest = t.getSource();
    if (self == dest || self.equals(dest.getPrimary())) {
      // deliver intra-agent notifications directly
      ReceiveNotificationLP.propagateNotification(
          rootplan,logplan,ptuid,ar,t.getUID(), changes);
    } else {
      // need to send an actual notification
      NewNotification nn = ldmf.newNotification();
      nn.setTaskUID(ptuid);
      nn.setPlan(t.getPlan());
      nn.setAllocationResult(ar);
      // set the UID of the child task for Expansion aggregation change purposes
      nn.setChildTaskUID(t.getUID());
      if (ptuid == null) {
	logger.error("createNotification: parent task UID is null for task " +
		     t);
      }

      MessageAddress newDest = t.getSource();
      //MessageAddress newDest = pt.getDestination();
      MessageAddress newSource = self;
      
      nn.setSource(newSource);
      nn.setDestination(newDest);
      rootplan.sendDirective(nn, changes);
    }
  }
}
