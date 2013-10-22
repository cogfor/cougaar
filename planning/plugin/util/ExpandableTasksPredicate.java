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

package org.cougaar.planning.plugin.util;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.UnaryPredicate;

/**
 * Not usually used predicate for Tasks of certain Verb with no Expansion
 * @see PredicateFactory
 */
public class ExpandableTasksPredicate implements UnaryPredicate, NewExpandableTasksPredicate {
    
  private Verb myVerb;

  public ExpandableTasksPredicate() {
  }

  /** Overloaded constructor for using from the scripts. 
   *  Discouraged to use from plugins directly.
   */
  public ExpandableTasksPredicate( Verb ver ) {
    myVerb = ver;
  }

  public void setVerb( Verb vb ) {
    myVerb = vb;
  }
    
  public boolean execute(Object o) {
    if ( o instanceof Task ) {
      Task t = ( Task ) o;
      // WARNING: Predicates that look at these slots on a Task
      // will behave oddly when the task later has these slots filled in-
      // later changes to the Task will not trigger the subscription
      if ( (t.getWorkflow() == null) &&
	   (t.getPlanElement() == null) &&
	   (t.getVerb().equals( myVerb ) )  ) {
	return true;
      }
    }
    return false;
  }
}
