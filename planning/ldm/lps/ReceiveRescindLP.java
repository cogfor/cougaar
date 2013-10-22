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

import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskRescind;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
  * take an incoming Rescind Directive and
  * perform Modification to the LOGPLAN
  * 
  *  1. Rescind Task - removes the task and any plan elements which
  *   address the that task.  Any cascade effect is then handled by
  *   RescindLP.
  **/
public class ReceiveRescindLP
implements LogicProvider, MessageLogicProvider
{
  private static final Logger logger = Logging.getLogger(ReceiveRescindLP.class);

  private final RootPlan rootplan;
  private final LogPlan logplan;

  public ReceiveRescindLP(
      RootPlan rootplan,
      LogPlan logplan) {
    this.rootplan = rootplan;
    this.logplan = logplan;
  }

  public void init() {
  }

  /**
   *  perform updates -- per Rescind ALGORITHM --
   *
   **/
  public void execute(Directive dir, Collection changes) {
    // drop changes
    if (dir instanceof TaskRescind) {
      receiveTaskRescind((TaskRescind) dir);
    }
  }

  private void receiveTaskRescind(TaskRescind tr) {
    UID tuid = tr.getTaskUID();
    logger.printDot("R");

    // just rescind the task; let the RescindLP handle the rest
    //
    Task t = logplan.findTask(tuid);
    if (t != null) {
      if (logger.isDebugEnabled())
	logger.debug("Rescinding task " + t);
      rootplan.remove(t);
    } else {
      if (logger.isDebugEnabled()) {
	logger.debug("Couldn't find task to rescind: " + tuid);
      }
      rootplan.add(new RescindLP.DeferredRescind(tr));
    }
  }
}
