/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.planning.plugin.completion;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 */
public class CompletionCalculator {
  protected static final double CONFIDENCE_THRESHHOLD = 0.89999;

  protected static final UnaryPredicate TASK_PRED = 
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Task);
      }
    };

  protected UnaryPredicate pred;

  public UnaryPredicate getPredicate() {
    if (pred == null) {
      pred = createPredicate();
    }
    return pred;
  }

  public double calculate(Collection c) {
    int n = (c != null ? c.size() : 0);
    if (n <= 0) {
      return 1.0;
    }
    double sum = 0.0;
    if (c instanceof List) {
      List l = (List) c;
      for (int i = 0; i < n; i++) {
        Object o = l.get(i);
        sum += getConfidence(o);
      }
    } else {
      Iterator x = c.iterator();
      for (int i = 0; i < n; i++) {
        Object o = x.next();
        sum += getConfidence(o);
      }
    }
    return (sum / n);
  }

  protected UnaryPredicate createPredicate() {
    // need to count all tasks, even though we're only
    // interested in the tasks with alloc results.
    //
    // If this is changed then the completion servlet
    // must also be fixed!  The servlet assumes that
    // the basic "CompletionCalculator" predicate
    // matches all tasks.
    return TASK_PRED;
  }

  protected double adjustConfRating(double confRating) {
    return Math.min(confRating/CONFIDENCE_THRESHHOLD, 1.0);
  }

  protected double getConfidence(Object o) {
    if (o instanceof Task) {
      Task task = (Task) o;
      PlanElement pe = task.getPlanElement();
      if (pe != null) {
        AllocationResult ar = pe.getEstimatedResult();
        if (ar != null) {
          return adjustConfRating(ar.getConfidenceRating());
        }
      }
    }
    return 0.0;
  }

  public boolean isConfident(double confRating) {
    return adjustConfRating(confRating) >= 1.0;
  }

  public String getConfidenceThreshholdString(boolean positive) {
    if (positive) {
      return "conf > " + CONFIDENCE_THRESHHOLD;
    } else {
      return "conf <= " + CONFIDENCE_THRESHHOLD;
    }
  }
}
