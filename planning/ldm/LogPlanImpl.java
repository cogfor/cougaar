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

import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.UniqueObjectSet;
import org.cougaar.core.domain.XPlan;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetSet;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PlanElementSet;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.util.UnaryPredicate;

/**
 * Implementation of "planning" LogPlan.
 */
public class LogPlanImpl
implements LogPlan, XPlan
{
  private Blackboard blackboard;

  private static final UnaryPredicate planElementP = new PlanElementPredicate();
  private static class PlanElementPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof PlanElement);
    }
  }

  /** is this a task object? **/
  private static final UnaryPredicate taskP = new TaskPredicate();
  private static class TaskPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Task);
    }
  }

  /** is this an asset? **/
  private static final UnaryPredicate assetP = new AssetPredicate();
  private static class AssetPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Asset);
    }
  }

  /**
   * Private container for PlanElements only.  Supports fast lookup of
   * Task->PlanElement.
   **/
  PlanElementSet planElementSet = new PlanElementSet();
  private CollectionSubscription planElementCollection;

  UniqueObjectSet taskSet = new UniqueObjectSet();
  private CollectionSubscription taskCollection;

  AssetSet assetSet = new AssetSet();
  private CollectionSubscription assetCollection;

  public void setupSubscriptions(Blackboard blackboard) {
    this.blackboard = blackboard;
    planElementCollection = new CollectionSubscription(planElementP, planElementSet);
    blackboard.subscribe(planElementCollection);

    taskCollection = new CollectionSubscription(taskP, taskSet);
    blackboard.subscribe(taskCollection);

    assetCollection = new CollectionSubscription(assetP, assetSet);
    blackboard.subscribe(assetCollection);
  }

  public PlanElement findPlanElement(Task task) {
    return planElementSet.findPlanElement(task);
  }

  /** @deprecated Use findPlanElement(UID uid) instead. **/
  public PlanElement findPlanElement(String id) {
    return planElementSet.findPlanElement(UID.toUID(id));
  }

  public PlanElement findPlanElement(UID uid) {
    return planElementSet.findPlanElement(uid);
  }

  public Task findTask(Task task) {
    return (Task) taskSet.findUniqueObject(task.getUID());
  }

  /** @deprecated Use findTask(UID uid) instead. **/
  public Task findTask(String id) {
    return findTask(UID.toUID(id));
  }

  public Task findTask(UID uid) {
    return (Task) taskSet.findUniqueObject(uid);
  }

  public Asset findAsset(Asset asset) {
    return assetSet.findAsset(asset);
  }

  public Asset findAsset(String id) {
    return assetSet.findAsset(id);
  }

  /** Counters for different types of logplan objects for metrics **/
  private int planelemCnt = 0;
  private int workflowCnt = 0;
  private int taskCnt = 0;
  private int assetCnt = 0;

  // Accessors for metrics counts
  public int getLogPlanCount() {
    return assetCnt + taskCnt + workflowCnt + planelemCnt;
  }

  public int getAssetCount() {
    return assetSet.size();
  }

  public int getTaskCount() {
    return taskSet.size();
  }

  public int getPlanElementCount() {
    return planElementSet.size();
  }

  private static final UnaryPredicate workflowPredicate = new WorkflowPredicate();
  private static class WorkflowPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Workflow);
    }
  }

  public int getWorkflowCount() {
    // no subscription for workflows?
    return blackboard.countBlackboard(workflowPredicate);
  }

  // Increment counts by given amount
  public void incAssetCount(int inc) {
      assetCnt += inc;
  }

  public void incTaskCount(int inc) {
      taskCnt += inc;
  }

  public void incPlanElementCount(int inc) {
      planelemCnt += inc;
  }

  public void incWorkflowCount(int inc) {
      workflowCnt += inc;
  }
}
