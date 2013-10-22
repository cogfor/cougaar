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

import java.util.Vector;

/** AllocationResultDistributor is a class which specifies how allocation results
  * should be distributed amongst 'parent' tasks of a Composition.
  * Distributes all aspect values amongst all parent tasks, divides COST and 
  * QUANTITY aspects evenly among all parent tasks.
  * Distributes all AuxiliaryQueryTypes and data to all parent tasks.
  * @see org.cougaar.planning.ldm.plan.AllocationResult
  **/

public interface AllocationResultDistributor
  extends AspectType // for Constants
{
  
  /** Calculate seperate AllocationResults for each parent task of 
   * the Composition.
   * @param parents Vector of Parent Tasks.
   * @param aggregateAllocationResult The allocationResult of the subtask.
   * @return distributedresults
   * @see org.cougaar.planning.ldm.plan.Composition
   * @see org.cougaar.planning.ldm.plan.TaskScoreTable
   * @see org.cougaar.planning.ldm.plan.AllocationResult
   */
  TaskScoreTable calculate(Vector parents, AllocationResult aggregateAllocationResult);
  
  /* static accessor for a default distributor */
  AllocationResultDistributor DEFAULT = new DefaultDistributor();
  
  // implementation of the default distributor
  /** Default distributor makes the best guess computation possible
   * without examining the details of the parent or sub tasks.
   * In particular all result values are copied to the values passed
   * to the parent, except for COST and QUANTITY, whose values are
   * distributed equally among the parents. This may or may not be
   * the right thing, depending on what sort of tasks are being 
   * aggregated.
   **/

  class DefaultDistributor
    implements AllocationResultDistributor 
  {
    public DefaultDistributor() {}
    public TaskScoreTable calculate(Vector parents, AllocationResult ar) {
      int l = parents.size();

      if (l == 0 || ar == null) return null;

      AspectValue[] avs = ar.getAspectValueResults(); // get a new AVR copy

      for (int x = 0; x<avs.length ; x++) {
        AspectValue av = avs[x];
        int type = av.getType();
        // if the aspect is COST or QUANTITY divide evenly across parents
        if ( (type == COST) || (type == QUANTITY) ) {
          avs[x] = av.dupAspectValue(av.floatValue() / l);
        } else {
          // it is ok already - just propagate it through
        }
      }
      
      AllocationResult newar = new AllocationResult(ar.getConfidenceRating(),
                                                    ar.isSuccess(),
                                                    avs);
      // fill in the auxiliaryquery info
      // each of the new allocationresults(for the parents) will have the SAME
      // auxiliaryquery info that the allocationresult (of the child) has.  
      for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
        String info = ar.auxiliaryQuery(aq);
        if (info != null) {
          newar.addAuxiliaryQueryInfo(aq, info);
        }
      }
      
      AllocationResult results[] = new AllocationResult[l];
      for (int i = 0; i<l; i++) {
        results[i] = newar;
      }

      Task tasks[] = new Task[l];
      parents.copyInto(tasks);

      return new TaskScoreTable(tasks, results);
    }
  } // end of DefaultDistributor inner class
  
}
