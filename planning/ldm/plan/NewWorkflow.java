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

/** NewWorkflow extends Workflow with set methods and other methods useful
 *  for building valid objects.
 **/

public interface NewWorkflow 
  extends Workflow 
{

  /** setParentTask allows you to set the parent task
    * or base task of a Worflow.
    * <PRE> myworkflow.setParentTask(mytask); </PRE>
    * @param parentTask  Should be a Task that is the task that was expanded to create the Workflow.
    **/
		
  void setParentTask(Task parentTask);
  
  /** setTasks allows you to pass in the Tasks that were created
  	* during the expansion of the parentTask.  These Task objects make up
  	* a Workflow (along with Constraint objects).
  	* <PRE> myworkflow.setTasks(myenumoftasks); </PRE>
  	* @param tasks - Enumeration{Task} tasks that make up the workflow
  	**/
  	
  void setTasks(Enumeration tasks);
  
  /** addTask allows you to add a Task to a Workflow.
  	* <PRE> myworkflow.addTask(mynewtask); </PRE>
  	* @param newTask -  Task to add
  	**/
  	
  void addTask(Task newTask);
  
  /** Remove the specified Task from the Workflow's sub-task collection  
    * @param remTask The Task to be removed.
    **/
  void removeTask(Task remTask);
  
  /** setConstraints allows you to add an Enumeration
  	* of Constraints to a Workflow.  Each Constraint has
  	* a relationship with a pair of Tasks in a Workflow.
  	* <PRE> myworkflow.setConstraints(myenumofconstraints);</PRE>
  	* @param enumofConstraints - Enumeration{Constraint} constraints to add to workflow
  	**/
  	
  void setConstraints(Enumeration enumofConstraints);
  
  /** addConstraint allows you to add a Constraint to a Workflow.
   * <PRE> myworkflow.addConstraint(mynewconstraint); </PRE>
   * @param newConstraint  - Constraint to add
   **/
  	
  void addConstraint(Constraint newConstraint);

  /** removeConstraint allows you to remove a Constraint from a Workflow.
   * <PRE> myworkflow.removeConstraint(constraint); </PRE>
   * @param constraint  - Constraint to remove
   **/
  	
  void removeConstraint(Constraint constraint);

  
  /** sets a specific compute algorithm to use while computing the aggregated
   * allocation results of the workflow.  If this method is not called, the 
   * allocationresult will be aggregated using the default 
   * AllocationResultAggregator (Sum). 
   * @param aragg The AllocationResultAggregator to use.
   * @see org.cougaar.planning.ldm.plan.AllocationResultAggregator
   */
  void setAllocationResultAggregator(AllocationResultAggregator aragg);

  /** Tells the infrastructure whether subtasks of the workflow
   * should be rescinded when the Expansion of the workflow is rescinded.
   * True means the infrastructure automatically performs the subtask rescinds.
   * False means that the Plugin is responsible for rescinding the subtasks
   * of the workflow or reattaching the workflow and its subtasks to 
   * a new parent task and its Expansion.  
   * @param isProp 
   **/
  void setIsPropagatingToSubtasks(boolean isProp);
  
  /** @deprecated  Use setIsPropagatingToSubtasks(boolean isProp) - 
    * default value is true
    **/
  void setIsPropagatingToSubtasks();

}
