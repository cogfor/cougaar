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

import java.util.ArrayList;
import java.util.List;

/** BulkEstimate Implementation
 * A BulkEstimate is similar to but not a subclass of PlanElement.
 * A BulkEstimate allows for a Plugin to specify a Task with a collection
 * of Preference sets and get back a collection of AllocationResults.
 * Each AllocationResult will represent the results of allocating the Task
 * with one of the Preference sets.
 *
 **/

public class BulkEstimateImpl implements BulkEstimate, NewBulkEstimate {
  private Task task;
  private List prefsets;
  private AllocationResult[] theresults;
  private boolean iscomplete;
  private double conf;
	
  /** Constructor that takes the task and preference sets.
   *  @param thetask
   *  @param thepreferencesets
   *  @param confrating  the confidence rating to be reached by each allocation
   */
  public BulkEstimateImpl(Task thetask, List thepreferencesets, double confrating) {
    this.task = thetask;
    this.prefsets = new ArrayList(thepreferencesets);
    this.iscomplete = false;
    conf = confrating;
    // initialize the allocationresult array in case we want to add
    // them singularly
    this.theresults = new AllocationResult[prefsets.size()];
  }
	
  /** @return The task to be allocated */
  public Task getTask() {
    return task;
  }
	
  /** @return The collection of preference sets.  Each set will be
   * represented by a Preference Array.
   */
  public List getPreferenceSets() {
    return new ArrayList(prefsets);
  }
	
  /** @return  The Array of AllocationResults. 
   * Note that this collection will be changing until isComplete()
   */
  public AllocationResult[] getAllocationResults() {
    return (AllocationResult[])theresults.clone();
  }
		
	
  /** @return  Will be set to true once all of the AllocationResults
   *  for each preference set have been gathered.
   */
  public boolean isComplete() {
    return iscomplete;
  }
	
  /** @return  The confidence rating of each AllocationResult that
   * must be reached before the result is valid and the next preference set 
   * can be tested.  The confidence rating should be between 0.0 and 1.0 with 
   * 1.0 being the most complete of allocations.
   */
  public double getConfidenceRating() {
    return conf;
  }
	
  /** @param allresults  The complete Array of AllocationResults. */
  public void setAllocationResults(AllocationResult[] allresults) {
    // make sure that the number of results equal the number of preference sets.
    if ( allresults.length == prefsets.size() ) {
      theresults = (AllocationResult[])allresults.clone();
    } else {
      throw new IllegalArgumentException("The number of AllocationResults passed to " +
                                         "NewBulkEstimate.setAllocationResults(AllocationResult[] allresults) " +
                                         "does not match the number of prefrence sets specified by the BulkEstimate.");
    }
  }	
	
  /** set a single AllocationResult
   * @param index  The position of the result in the overall result array.
   * This position should correspond to the preference set position.
   * @param aresult
   */
  public void setSingleResult(int index, AllocationResult aresult) {
    if ( index >= 0 && index < theresults.length ) {
      theresults[index] = aresult;
    } else {
      throw new IllegalArgumentException("An out of bounds index was passed to NewBulkEstimate.setSingleResult(int index, AllocationResult aresult).");
    }
  }
	
  /** @param complete  Should be set to true once all of the AllocationResults
   *  for each preference set have been gathered.
   */
  public void setIsComplete(boolean complete) {
    iscomplete = complete;
  }

	
}
