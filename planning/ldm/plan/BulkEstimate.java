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

/** BulkEstimate Interface
  * A BulkEstimate is similar to but not a subclass of PlanElement.
  * A BulkEstimate allows for a Plugin to specify a Task with a collection
  * of Preference sets and get back a collection of AllocationResults.
  * Each AllocationResult will represent the results of allocating the Task
  * with one of the Preference sets.
  *
  **/

public interface BulkEstimate {
	/** @return Task  The task to be allocated */
	Task getTask();
	
	/** @return List  The collection of preference sets.  Each set will be
	 * represented by a Preference Array.
	 */
	List getPreferenceSets();
	
	/** @return AllocationResult[]  The Array of AllocationResults. 
	 * Note that this collection will be changing until isComplete()
	 */
	AllocationResult[] getAllocationResults();
	
	/** @return boolean  Will be set to true once all of the AllocationResults
	 *  for each preference set have been gathered.
	 */
	boolean isComplete();
	
	/** @return double  The confidence rating of each AllocationResult that
	 * must be reached before the result is valid and the next preference set 
	 * can be tested.  The confidence rating should be between 0.0 and 1.0 with 
	 * 1.0 being the most complete of allocations.
	 */
	double getConfidenceRating();
	
}
