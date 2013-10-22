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

import java.util.Enumeration;

import org.cougaar.core.util.UniqueObject;

/**
 * Workflow Interface
 * A Workflow is the result of a Task expansion consisting primarily
 * of a partially ordered set of Task instances.  There are many sorts
 * of Workflow implementations possible, ranging from a strictly-ordered
 * vector of subtasks, to an unordered Bag, to a set of DAGs, or even 
 * a complex, temporally-ordered set.
 **/
  
public interface Workflow 
  extends UniqueObject, Annotatable
{
 	
  /**  
   * <PRE> Task parenttask = myworkflow.getParentTask(); </PRE>
   * @return Task  Return the Task for which this Workflow is an expansion.
   **/
  Task getParentTask();
 	
  /** 
   * Return an Enumeration which walks over all tasks 
   * which are members of this Workflow.
   * <PRE> Enumeration mytasks = myworkflow.getTasks(); </PRE>
   * @return Enumeration{Task}
   **/
  Enumeration getTasks();
 	
  /** 
   * Returns an Enumeration which walks over
   * all Constraints that are members of
   * this workflow.
   * <PRE> Enumeration myconstraints = myworkflow.getConstraints(); </PRE>
   * @return Enumeration{Constraint}
   **/
  Enumeration getConstraints();
   
  /** 
   * Returns an Enumeration which walks over
   * all Constraints that have a relationship
   * to the passed Task.
   * <PRE> Enumeration myconstraints = myworkflow.getTaskConstraints(mytask); </PRE>
   * @param task - The task you are checking for Constraints. 
   * @return Enumeration{Constraint}
   **/
  Enumeration getTaskConstraints(Task task);
   
  /** 
   * Returns and Enumeration which walks over
   * all Constraints that have a pair-wise 
   * relationship with two passed tasks.
   * <PRE> Enumeration myconstraints = myworkflow.getPairConstraints(mytask1, mytask2); </PRE>
   * @param constrainedTask - Task that is constrained
   * @param constrainingTask - Task that is constraining
   * @return Enumeration{Constraint}
   **/
  Enumeration getPairConstraints(Task constrainedTask, Task constrainingTask);
   
  /** Ask the workflow to compute an AllocationResult based on the 
   * AllocationResults of the Workflow's sub Tasks.  If the aggregate
   * AllocationResult is undefined (e.g. some of the sub Tasks have not yet
   * been allocated), computePenaltyValue should return null.
   *
   * @return AllocationResult - the result of aggregating the AllocationResults
   * of the Workflow using the defined (or default) AllocationResultAggregator.
   * @see org.cougaar.planning.ldm.plan.AllocationResultAggregator
   **/
  AllocationResult aggregateAllocationResults();

  /**
   * Get allocation results and change information for all subtasks
   * corresponding to the most recently computed received allocation
   * result of the corresponding expansion. The returned List contains
   * a SubTaskResult for each subtask. Each of the SubTaskResult
   * objects contains:
   * Task - the subtask,
   * boolean - whether the result changed,
   * AllocationResult - the result used by the aggregator for this sub-task.
   * The boolean indicates whether the AllocationResult changed since the 
   * last time the collection was cleared by the plugin (which should be
   * the last time the plugin looked at the list).
   * @return List of SubTaskResultObjects one for each subtask
   **/
  SubtaskResults getSubtaskResults();
  
  /** Has a constraint been violated?
    * @return boolean
    */
    boolean constraintViolation();
  
  /** Get the constraints that were violated.
    * @return Enumeration{Constraint}
    */
    Enumeration getViolatedConstraints();

  /** Should subtasks be rescinded by the infrastructure when the
   * expansion this workflow is attached to is rescinded?
   * Defaults to true.
   * Set to false (meaning the Plugin is responsible for rescinding or
   * reattaching the workflow and its subtasks to an expansion and parent task)
   * by NewWorkflow.setIsPropagatingToSubtasks(isProp);
   * @return boolean 
   **/
  boolean isPropagatingToSubtasks();

   /** Return constraint for which constraining event is defined and
    * constraining event is undefined or violated
    **/
   
  Constraint getNextPendingConstraint();

}

