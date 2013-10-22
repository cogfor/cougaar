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


/**  SubTaskResults
   * Allows access to the sub-task's allocation result information
   * used to aggregate this Expansion's latest reported allocationresult
   *
   *
   **/

public class SubTaskResult implements java.io.Serializable {
  
  Task t;
  AllocationResult ar;
  boolean changed;
  
  /** Simple Constructor for saving state of a single sub-task's results
    * when the AllocationResult Aggregator is run.  The boolean changed
    * keeps track of whether this allocationresult changed to cause the
    * re-aggregation.
    * @param task  the subtask of the workflow
    * @param haschanged  whether this is a new allocationresult causing the recalculation
    * @param result the AllocationResult used to Aggregate the results of the workflow
    */
  public SubTaskResult (Task task, boolean haschanged, AllocationResult result) {
    this.t = task;
    this.changed = haschanged;
    this.ar = result;
  }
  
  /** @return Task  The sub-task this information is about. **/
  public Task getTask() { return t; }
  /** @return AllocationResult  The AllocationResult for this sub-task used by the Aggregator **/
  public AllocationResult getAllocationResult() { return ar; }
  /** @return boolean  Whether this was a new AllocationResult that caused the re-aggregation **/
  public boolean hasChanged() { return changed; }
}
