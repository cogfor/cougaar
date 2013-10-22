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
import org.cougaar.core.domain.RootPlan;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;

public class WorkflowAllocationLP
implements LogicProvider, EnvelopeLogicProvider
{
  private final RootPlan rootplan;
  private final LogPlan logplan;

  public WorkflowAllocationLP(
      RootPlan rootplan,
      LogPlan logplan) {
    this.rootplan = rootplan;
    this.logplan = logplan;
  }

  public void init() {
  }

  /** @param o EnvelopeTuple
   *        where Envelope.Tuple.object
   *            == PlanElement object ADDED TO LOGPLAN containing Expansion
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    // drop changes
    if (o.isAdd()) {
      Object obj = o.getObject();
      if ((obj instanceof Expansion)) {
        processExpansion((Expansion) obj);
      }
    }
  }

  private void processExpansion(Expansion exp) {
    Workflow work = exp.getWorkflow();
    rootplan.add(work);

      // Add each subtask of the workflow to the blackboard so
      // that allocators can allocate against single tasks
      // (and we don't need pass-thru expanders
    Enumeration tasks = work.getTasks();
    while (tasks.hasMoreElements()) {
      Task t = (Task) tasks.nextElement();
      if (t != null) {
        if (logplan.findTask(t) == null)
          rootplan.add(t);
      }
    }
  }
}
