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

import java.util.Collection;

/** NewComposition Interface
   * Used to build complete Composition objects.
   *
   *
   **/

public interface NewComposition extends Composition {
  
  /** Set the Aggregation PlanElements of the tasks being combined
    * @param aggs  The Aggregations
    * @see org.cougaar.planning.ldm.plan.Aggregation
    */
  void setAggregations(Collection aggs);
  
  /** Add a single Aggregation to the existing collection
   */
  void addAggregation(Aggregation singleagg);
  
  /** Set the newly created task that represents all 'parent' tasks.
    * @param newTask
    * @see org.cougaar.planning.ldm.plan.Task
    */
  void setCombinedTask(MPTask newTask);
  
  /** Allows the AllocationResult to be properly dispersed among the 
    * original (or parent) tasks.
    * @param distributor
    * @see org.cougaar.planning.ldm.plan.AllocationResultDistributor
    */
  void setDistributor(AllocationResultDistributor distributor);
  
  /** Tells the infrastructure that all members of this composition should
   * be rescinded when one of the Aggregations is rescinded, this includes all
   * of the Aggregations (one for each parent task), the combined task and 
   * planelements against the combined task.
   * If flag is set to False, the infrastructure does NOT rescind the other
   * Aggregations or the combined task.  It only removes the reference of the
   * rescinded Aggregation and its task (a parent task) from the composition
   * and the combined task.
   * @param isProp
   **/
  void setIsPropagating(boolean isProp);
  
  /** @deprecated  Use setIsPropagating(boolean isProp) - defaults to true**/
  void setIsPropagating();
  
}
