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

package org.cougaar.planning.ldm;

import org.cougaar.core.domain.XPlan;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

/**
 * Planning-specify view of the blackboard.
 */
public interface LogPlan
extends XPlan
{
  /** find the PlanElement associated with a task in the LogPlan.
   * This is an optimization of searchLogPlan, since it needs to be done
   * far more often than the general case.
   **/
  PlanElement findPlanElement(Task task);

  /** like findPlanElement(Task) but looks up based on task's proxiable ID **/
  PlanElement findPlanElement(String task);
  PlanElement findPlanElement(UID uid);

  /** find the LogPlan task matching Task.  This is normally the
   * identity operation, though it may be that (via serialization and
   * task proxies) two task instances may actually refer to the same task.
   **/
  Task findTask(Task task);

  /** like findTask(Task), but looks up via proxiable id **/
  Task findTask(String id);
  Task findTask(UID uid);

  /** Find the Asset in the logplan.  This will be an identity operation
   * modulo serialization and copying.
   **/
  Asset findAsset(Asset asset);

  /** find the Asset in the logplan by its itemIdentification.
   **/
  Asset findAsset(String id);

  // Necessary for metrics count updates
  void incAssetCount(int inc);
  void incPlanElementCount(int inc);
  void incTaskCount(int inc);
  void incWorkflowCount(int inc);
}
