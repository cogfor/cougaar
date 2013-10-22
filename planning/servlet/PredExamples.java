/*
 *
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
package org.cougaar.planning.servlet;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;

/**
 * These are example "public static boolean" methods for use by the
 * <code>PlanViewServlet</code> as examples in the "Advanced Search".
 *
 * The file <tt>PlanViewServlet.DEFAULT_PRED_FILENAME</tt> uses these as
 * examples, such as:<pre>
 *   (org.cougaar.planning.servlet.PredExamples:examplePredicateA (this))
 * </pre>.
 */
public class PredExamples {

  /**
   * This is an example of a "public static boolean" predicate for the 
   * predicate search -- this one happens to check for a <code>Task</code>, 
   * but one could write arbitrarily complicated code here.
   *
   * This is here for the examples only!  Other "utility" predicates should
   * be placed in a different class (e.g. "SearchUtils")!
   */
  public static boolean examplePredicateA(Object o) {
    return (o instanceof Task);
  }

  /** @see #examplePredicateA(Object) */
  public static boolean examplePredicateB(
      Task t, 
      String verbStr) {
    return t.getVerb().equals(verbStr);
  }

  /** @see #examplePredicateA(Object) */
  public static boolean examplePredicateC(
      Object o, 
      String verbStr, 
      double minConf) {
    if (o instanceof Task) {
      Task t = (Task)o;
      if ((verbStr == null) ||
          (t.getVerb().equals(verbStr))) {
        PlanElement pe = t.getPlanElement();
        if (pe != null) {
          AllocationResult est = pe.getEstimatedResult();
          return 
            ((est != null) &&
             (est.getConfidenceRating() >= minConf));
        }
      }
    }
    return false;
  }
}
