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

package org.cougaar.planning.ldm.plan;

import java.util.List;

/** Composition Interface
   * An Composition represents the aggregation of multiple tasks
   * into a single task.  Compositions are referenced by Aggregation PlanElements.
   *
   *
   **/

public interface Composition
{
  
  /** Returns the Aggregation PlanElements of the Tasks that
    * are being combined
    * @return List
    * @see org.cougaar.planning.ldm.plan.Aggregation
    */
  List getAggregations();
  
  /** Convenienve method that calculates the Tasks that are 
   * being aggregated by looking at all of the Aggregations.
   * (Aggregation.getTask())
   * @return List
   * @see org.cougaar.planning.ldm.plan.Task
   **/
  List getParentTasks();
  
  /** Returns the newly created task that represents all 'parent' tasks.
    * The new task should be created as an MPTask.
    * @return Task
    * @see org.cougaar.planning.ldm.plan.MPTask
    */
  MPTask getCombinedTask();
  
  /** Allows the AllocationResult to be properly dispersed among the 
    * original (or parent) tasks.
    * @return AllocationResultDistributor
    * @see org.cougaar.planning.ldm.plan.AllocationResultDistributor    
    */
  AllocationResultDistributor getDistributor();
  
  /**Calculate seperate AllocationResults for each parent task of the Composition.
    * @return TaskScoreTable
    * @see org.cougaar.planning.ldm.plan.TaskScoreTable
    */
  TaskScoreTable calculateDistribution();
  
  /** Should all related Aggregations, and the combined task be rescinded 
   * when a single parent task and its Aggregation is rescinded.
   * When false, and a single 'parent' Aggregation is rescinded,
   * the infrastructure removes references to that task/Aggregation in the
   * Composition and the combined MPTask.  However, the Composition and combined
   * task are still valid as are the rest of the parent tasks/Aggregations that
   * made up the rest of the Composition.
   * Defaults to true.
   * set to false by NewComposition.setIsPropagating(isProp);
   **/
  boolean isPropagating();
  
}
  
