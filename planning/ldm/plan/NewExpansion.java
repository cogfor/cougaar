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


/** NewExpansion Interface
   * Allows access to the single sub-task's allocation result information
   * used to aggregate this Expansion's latest reported allocationresult
   *
   *
   **/

public interface NewExpansion extends Expansion {
  
  /** Called by an Expander Plugin to get the latest copy of the allocationresults
   *  for each subtask.
   *  Information is stored in a List which contains a SubTaskResult for each subtask.  
   *  Each of the SubTaskResult objects contain the following information:
   *  Task - the subtask, boolean - whether the result changed,
   *  AllocationResult - the result used by the aggregator for this sub-task.
   *  The boolean indicates whether the AllocationResult changed since the 
   *  last time the collection was cleared by the plugin (which should be
   *  the last time the plugin looked at the list).
   *  NOTE!!! This accessor should only be called once in a plugin execute cycle as
   *  the list will be cleared as soon as this accessor is called.  The information container
   *  in the return list should be directly related to an update of the reportedAllocationResult
   *  slot on the Expansion that the plugin woke up on.
   *  @return List
   */
  SubtaskResults getSubTaskResults();
 
}
