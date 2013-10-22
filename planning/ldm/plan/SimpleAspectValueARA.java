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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import org.cougaar.planning.ldm.asset.Asset;

/** Simple Aggregator for AllocationResults specified with AspectValues.
  * Particularly useful for results with TypedQuantityAspects.
  * Does the right computation for workflows which are made up of
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
  * TYPED_QUANTITY is summed for each type(asset).
  *
  * For AuxiliaryQuery information, if all the query values are the same
  * across subtasks or one subtask has query info it will be place in the 
  * aggregate result.  However, if there are conflicting query values, no
  * information will be put in the aggregated result.
  * 
  * returns null when there are no subtasks or any task has no result.
  *
  *
  **/
  public class SimpleAspectValueARA implements AllocationResultAggregator {
    public AllocationResult calculate(Workflow wf, TaskScoreTable tst, AllocationResult currentar) {
      List aggregAR = new ArrayList();
      boolean suc = true;
      double rating = 0.0;
      int count = 0;
      
      Enumeration tasks = wf.getTasks();
      if (tasks == null || (! tasks.hasMoreElements())) return null;
      
      String auxqsummary[] = new String[AuxiliaryQueryType.AQTYPE_COUNT];
      // initialize all values to UNDEFINED for comparison purposes below.
      final String UNDEFINED = "UNDEFINED";
      for (int aqs = 0; aqs < auxqsummary.length; aqs++) {
        auxqsummary[aqs] = UNDEFINED;
      }
      

      while (tasks.hasMoreElements()) {
        Task t = (Task) tasks.nextElement();
        count++;
        AllocationResult ar = tst.getAllocationResult(t);
        if (ar == null) {
          return null; // bail if undefined
        }

        suc = suc && ar.isSuccess();
        rating += ar.getConfidenceRating();
        
        AspectValue[] someresults = ar.getAspectValueResults();
        
        for (int b = 0; b < someresults.length; b++) {
          AspectValue sumav = null;
          AspectValue srav = someresults[b];
          int thisat = srav.getAspectType();
          for (ListIterator lit = aggregAR.listIterator(); lit.hasNext(); ) {
            AspectValue thisav = (AspectValue) lit.next();
            if (thisav.getAspectType() == thisat) {
              if (thisat != TYPED_QUANTITY) {
                sumav = thisav;
              } else {
                // check the asset
                Asset tqasset = ((TypedQuantityAspectValue)thisav).getAsset();
                if ( tqasset.equals( ((TypedQuantityAspectValue)srav).getAsset() ) ) {
                  sumav = thisav;
                }
              }
            }
          }
          // if we still don't have a matching sum AspectValue, make a new one for this aspect
          if (sumav == null) {
            sumav = srav; // First time, just clone the value
            aggregAR.add(sumav);
          } else {
            switch (thisat) {
            case START_TIME:
              double nst = Math.min(sumav.getValue(), srav.getValue());
              sumav = sumav.dupAspectValue(nst);
              break;
            case END_TIME:
            case DANGER:
            case RISK:
              double newmaxv = Math.max(sumav.getValue(), srav.getValue());
              sumav = sumav.dupAspectValue(newmaxv);
              break;
            default:
              // if its anything else its a simple summation, even for TYPED_QUANTITY
              double newsumv = sumav.getValue() + srav.getValue();
              sumav = sumav.dupAspectValue(newsumv);
            }
          }
        } // end of for loop for allocationresult aspecttypes
          
        // Sum up the auxiliaryquery data.  If there are conflicting data
        // values, send back nothing for that type.  If only one subtask
        // has information about a querytype, send it back in the 
        // aggregated result.
        for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
          String data = ar.auxiliaryQuery(aq);
          if (data != null) {
            String sumdata = auxqsummary[aq];
            if (sumdata.equals(UNDEFINED)) {
              // there's not a value yet, so use this one.
              auxqsummary[aq] = data;
            } else if (! data.equals(sumdata)) {
              // there's a conflict, pass back null
              auxqsummary[aq] = null;
            }
          }
        }
        
        
      } // end of while looping through tasks.
      
      // now make AspectVales for DURATION and CUSTOMER_SATISFACTION
      double overallstart = 0.0;
      double overallend = 0.0;
      for (ListIterator l = aggregAR.listIterator(); l.hasNext(); ) {
        AspectValue timeav = (AspectValue) l.next();
        int timeavaspect = timeav.getAspectType();
        if ( timeavaspect == START_TIME ) {
          overallstart = timeav.getValue();
        } else if ( timeavaspect == END_TIME ) {
          overallend = timeav.getValue();
        }
      }
      // if the start time and end time are defined, find or create a duration aspectvalue
      AspectValue theduration = null;
      if ( (overallstart != 0.0) && (overallend != 0.0) ) {
        for (ListIterator litdur = aggregAR.listIterator(); litdur.hasNext(); ) {
          AspectValue durav = (AspectValue) litdur.next();
          if ( DURATION == durav.getAspectType() ) {
            theduration = durav;
            // reset the value
            durav = durav.dupAspectValue(overallend - overallstart);
          }
        }
        // if we didn't find a duration av, create one
        if (theduration == null) {
          AspectValue duration = AspectValue.newAspectValue(AspectType.Duration, overallend - overallstart);
          aggregAR.add(duration);
        }
      }
      
      // if there is a CUSTOMER_SATISFACTION av, divide the total sum by the number of tasks
      for (ListIterator litcs = aggregAR.listIterator(); litcs.hasNext(); ) {
        AspectValue csav = (AspectValue) litcs.next();
        if (CUSTOMER_SATISFACTION == csav.getAspectType() ) {
          csav = csav.dupAspectValue(csav.getValue() / count);
        }
      }

      rating /= count;

      boolean delta = true;
      if (currentar != null) {
        AspectValue[] currentavs = currentar.getAspectValueResults();
        List currentlist = Arrays.asList(currentavs);
        if ( currentlist.equals(aggregAR) &&
             currentar.getConfidenceRating() == rating &&
             currentar.isSuccess() == suc ) {
          // there are no changes
          delta = false;
        }
      }

      if (delta) {
        AspectValue[] newreturnresults = (AspectValue[])aggregAR.toArray(new AspectValue[aggregAR.size()]);
        AllocationResult artoreturn = new AllocationResult(rating, suc, newreturnresults);
        // fill in the auxquery stuff
        for (int aqt = 0; aqt < auxqsummary.length; aqt++) {
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
