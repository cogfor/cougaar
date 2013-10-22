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

import java.io.Serializable;
import java.util.HashMap;
/** AllocationResultAggregator is a class which specifies how allocationresults
 * should be aggregated.  Currenlty used by Workflow.computeAllocationResult
 * @see org.cougaar.planning.ldm.plan.AllocationResult
 **/

public interface AllocationResultAggregator 
  extends Serializable, AspectType // for Constants
{
  double SIGNIFICANT_CONFIDENCE_RATING_DELTA = 0.0001;
  
  /** @return AllocationResult The computed AllocationResult
   * @param wf The workflow that is using this aggregator to compute it's AllocationResult
   * @param tst The TaskScoreTable associated with this workflow
   * @param currentar The current AllocationResult
   * @see Workflow
   * @see TaskScoreTable
   **/
  AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar);
  
  /** static accessor for a default/generic AllocationResultAggregator that does
   * a simple summation of all measures (including extensions) found in all allocationresults in the
   * subtasks of the workflow.
   */
  AllocationResultAggregator SUM = new Sum();

  AllocationResultAggregator DEFAULT = new DefaultARA();
  
  // implementation of a default/generic AllocationResultAggregator
  class Sum implements AllocationResultAggregator {
    public Sum() {}
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      // write new implementation - stub for now.
      return null;
    }
  } // end of Sum class

      
  /** Does the right computation for workflows which are made up of
   * equally important tasks with no inter-task constraints.
   * START_TIME is minimized.
   * END_TIME is maximized.
   * DURATION is overall END_TIME - overall START_TIME.
   * COST is summed.
   * DANGER is maximized.
   * RISK is maximized.
   * QUANTITY is summed.
   * INTERVAL is summed.
   * TOTAL_QUANTITY is summed.
   * TOTAL_SHIPMENTS is summed.
   * CUSTOMER_SATISFACTION is averaged.
   * READINESS is minimized.
   * Any extended aspect types are ignored.
   * 
   * For AuxiliaryQuery information, if all the query values are the same
   * across subtasks or one subtask has query info it will be place in the 
   * aggregate result.  However, if there are conflicting query values, no
   * information will be put in the aggregated result.
   * 
   * returns null when there are no subtasks or any task has no result.
   **/
  class DefaultARA implements AllocationResultAggregator {
    private static final String UNDEFINED = "UNDEFINED";

    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      double acc[] = new double[AspectType._ASPECT_COUNT];
      acc[START_TIME] = Double.MAX_VALUE;
      acc[END_TIME] = 0.0;
      // duration is computed from end values of start and end
      acc[COST] = 0.0;
      acc[DANGER] = 0.0;
      acc[RISK] = 0.0;
      acc[QUANTITY] = 0.0;
      acc[INTERVAL] = 0.0;
      acc[TOTAL_QUANTITY] = 0.0;
      acc[TOTAL_SHIPMENTS] = 0.0;
      acc[CUSTOMER_SATISFACTION] = 1.0; // start at best
      acc[READINESS] = 1.0;

      boolean ap[] = new boolean[AspectType._ASPECT_COUNT];

      boolean suc = true;
      double rating = 0.0;
      
      if (tst == null) return null;
      int tstSize = tst.size();
      if (tstSize == 0) return null;
      
      String auxqsummary[] = new String[AuxiliaryQueryType.AQTYPE_COUNT];
      // initialize all values to UNDEFINED for comparison purposes below.
      int aql = auxqsummary.length;
      for (int aqs = 0; aqs < aql; aqs++) {
        auxqsummary[aqs] = UNDEFINED;
      }

      int hash = 0;
      for (int i = 0; i < tstSize; i++) {
        AllocationResult ar = tst.getAllocationResult(i);
        if (ar == null) return null; // bail if undefined

        suc = suc && ar.isSuccess();
        rating += ar.getConfidenceRating();
        
        int[] definedaspects = ar.getAspectTypes();
        int al = definedaspects.length;
        for (int b = 0; b < al; b++) {
          // accumulate the values for the defined aspects
          switch (definedaspects[b]) {
          case START_TIME:
            acc[START_TIME] = Math.min(acc[START_TIME], ar.getValue(START_TIME));
            ap[START_TIME] = true;
            hash |= (1<<START_TIME);
            break;
          case END_TIME: 
            acc[END_TIME] = Math.max(acc[END_TIME], ar.getValue(END_TIME));
            ap[END_TIME] = true;
            hash |= (1<<END_TIME);
            break;
            // compute duration later
          case COST: 
            acc[COST] += ar.getValue(COST);
            ap[COST] = true;
            hash |= (1<<COST);
            break;
          case DANGER:
            acc[DANGER] = Math.max(acc[DANGER], ar.getValue(DANGER));
            ap[DANGER] = true;
            hash |= (1<<DANGER);
            break;
          case RISK: 
            acc[RISK] = Math.max(acc[RISK], ar.getValue(RISK));
            ap[RISK] = true;
            hash |= (1<<RISK);
            break;
          case QUANTITY:
            acc[QUANTITY] += ar.getValue(QUANTITY);
            ap[QUANTITY] = true;
            hash |= (1<<QUANTITY);
            break;
            // for now simply add the repetitve task values
          case INTERVAL: 
            acc[INTERVAL] += ar.getValue(INTERVAL);
            ap[INTERVAL] = true;
            hash |= (1<<INTERVAL);
            break;
          case TOTAL_QUANTITY: 
            acc[TOTAL_QUANTITY] += ar.getValue(TOTAL_QUANTITY);
            ap[TOTAL_QUANTITY] = true;
            hash |= (1<<TOTAL_QUANTITY);
            break;
          case TOTAL_SHIPMENTS:
            acc[TOTAL_SHIPMENTS] += ar.getValue(TOTAL_SHIPMENTS);
            ap[TOTAL_SHIPMENTS] = true;
            hash |= (1<<TOTAL_SHIPMENTS);
            break;
            //end of repetitive task specific aspects
          case CUSTOMER_SATISFACTION:
            acc[CUSTOMER_SATISFACTION] += ar.getValue(CUSTOMER_SATISFACTION);
            ap[CUSTOMER_SATISFACTION] = true;
            hash |= (1<<CUSTOMER_SATISFACTION);
            break;
          case READINESS:
            acc[READINESS] = Math.min(acc[READINESS], ar.getValue(READINESS));
            ap[READINESS] = true;
            hash |= (1<<READINESS);
            break;
	  }
        }
        
        // Sum up the auxiliaryquery data.  If there are conflicting data
        // values, send back nothing for that type.  If only one subtask
        // has information about a querytype, send it back in the 
        // aggregated result.
        for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
          String data = ar.auxiliaryQuery(aq);
          if (data != null) {
            String sumdata = auxqsummary[aq];
            // if sumdata = null, there has already been a conflict.
            if (sumdata != null) {
              if (sumdata.equals(UNDEFINED)) {
                // there's not a value yet, so use this one.
                auxqsummary[aq] = data;
              } else if (! data.equals(sumdata)) {
                // there's a conflict, pass back null
                auxqsummary[aq] = null;
              }
            }
          }
        }

      } // end of looping through all subtasks
      
      // compute duration IFF defined.
      if (ap[START_TIME] && ap[END_TIME]) {
        acc[DURATION] = acc[END_TIME] - acc[START_TIME];
        ap[DURATION] = true;
        hash |= (1<<DURATION);
      } else {
        // redundant
        acc[DURATION] = 0.0;
        ap[DURATION] = false;
      }

      if (tstSize>0) {
        acc[CUSTOMER_SATISFACTION] /= tstSize;
        rating /= tstSize;
      }

      boolean delta = false;
      
      // only check the defined aspects and make sure that the currentar is not null
      if (currentar == null) {
        delta = true;		// if the current ar == null then set delta true
      } else {
        int[] caraspects = currentar.getAspectTypes();
        if (caraspects.length != acc.length) {
          //if the current ar length is different than the length of the new
          // calculations (acc) there's been a change
          delta = true;
        } else {
          int il = caraspects.length;
          for (int i = 0; i < il; i++) {
            int da = caraspects[i];
            if (ap[da] && acc[da] != currentar.getValue(da)) {
              delta = true;
              break;
            }
          }
        }
      
        if (!delta) {
	  if (currentar.isSuccess() != suc) {
	    delta = true;
	  } else if (Math.abs(currentar.getConfidenceRating() - rating) > SIGNIFICANT_CONFIDENCE_RATING_DELTA) {
	    delta = true;
	  }
        }
      }

      if (delta) {
        int keys[] = _STANDARD_ASPECTS;
        int al = AspectType._ASPECT_COUNT;

        // see if we should compress the results array
        int lc = 0;
        for (int b = 0; b < al; b++) {
          if (ap[b]) lc++;
        }
      
        if (lc < al) {            // need to compress the arrays
          double nv[] = new double[lc];

          // slow, big, general case
          /*
          {
            int nk[] = new int[lc];
            int i = 0;
            for (int b = 0; b<al; b++) {
              if (ap[b]) {
                nv[i] = acc[b];
                nk[i] = keys[b];
                i++;
              }
            }
            acc = nv;
            keys = nk;
          }
          */

          // lazy cache the key patterns
          synchronized (hack) {
            Integer ihash = new Integer(hash);
            KeyHolder kh = (KeyHolder) hack.get(ihash);
            if (kh == null) {
              //System.err.println("Caching key "+hash);
              int nk[] = new int[lc];
              int i = 0;
              for (int b = 0; b<al; b++) {
                if (ap[b]) {
                  nv[i] = acc[b];
                  nk[i] = keys[b];
                  i++;
                }
              }
              acc = nv;
              keys = nk;
              kh = new KeyHolder(nk);
              hack.put(ihash,kh);
            } else {
              keys = kh.keys;
              int i = 0;
              for (int b = 0; b<al; b++) {
                if (ap[b]) {
                  nv[i] = acc[b];
                  i++;
                }
              }
              acc = nv;
            }
          }

        }

        AllocationResult artoreturn = new AllocationResult(rating, suc, keys, acc);

        for (int aqt = 0; aqt < aql; aqt++) {
          String aqdata = auxqsummary[aqt];
          if ( (aqdata !=null) && (aqdata != UNDEFINED) ) {
            artoreturn.addAuxiliaryQueryInfo(aqt, aqdata);
          }
        }
        return artoreturn;
      } else {
        return currentar;
      }
    }
  }

  int[] K02 = new int[] {0,2};
  int[] K012 = new int[] {0,1,2};
  int[] K0126 = new int[] {0,1,2,6};

  HashMap hack = new HashMap();
  
  final class KeyHolder {
    public int[] keys;
    public KeyHolder(int keys[]) {
      this.keys = keys;
    }
  }
}
