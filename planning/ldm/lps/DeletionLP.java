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

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.NewDeletion;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** RescindLogicProvider class provides the logic to capture 
 * rescinded PlanElements (removed from collection)
  *
  *
  **/

public class DeletionLP
implements LogicProvider, EnvelopeLogicProvider
{
  private static final Logger logger = Logging.getLogger(DeletionLP.class);

  private final RootPlan rootplan;
  private final PlanningFactory ldmf;
  private final MessageAddress self;

  public DeletionLP(
      RootPlan rootplan,
      PlanningFactory ldmf,
      MessageAddress self)
  {
    this.rootplan = rootplan;
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
    if (o.isRemove()) {
      Object obj = o.getObject();
      if (obj instanceof Task) {
        Task task = (Task) obj;
        if (task.isDeleted()) {
          UID ptuid = task.getParentTaskUID();
          if (ptuid != null) {
            MessageAddress dst = task.getSource();
            if (!dst.getPrimary().equals(self)) {
              // Parent task is in another agent so we do our thing
              NewDeletion nd = ldmf.newDeletion();
              nd.setTaskUID(ptuid);
              nd.setPlan(task.getPlan());
              nd.setSource(self);
              nd.setDestination(dst);
	      if (logger.isDebugEnabled()) {
		logger.debug(self + ": send Deletion to " + dst + " for task " + ptuid);
	      }
              rootplan.sendDirective(nd);
            }
          }
        }
      }
    }
  }
}
