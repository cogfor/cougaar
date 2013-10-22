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

import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.Verb;

/**
 * Not typically used factory for Predicates. Intended for use from
 * script-based plugins. It's better usage to create your own
 * predicates directly from your plugin.
 */
public class PredicateFactory {
    
  public static NewExpandableTasksPredicate newExpandableTasksPredicate() {
    ExpandableTasksPredicate et = new ExpandableTasksPredicate();
    return ( ( NewExpandableTasksPredicate ) et );
  }

  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewExpandableTasksPredicate newExpandableTasksPredicate( String ve, PlanningFactory ldmf ) {
    Verb newVerb = Verb.get( ve );
    ExpandableTasksPredicate et = new ExpandableTasksPredicate( newVerb );
    return ( ( NewExpandableTasksPredicate ) et );
  }	

  public static NewAllocatableWFPredicate newAllocatableWFPredicate() {
    AllocatableWFPredicate atp = new AllocatableWFPredicate();
    return ( ( NewAllocatableWFPredicate ) atp );
  }

  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewAllocatableWFPredicate newAllocatableWFPredicate( String ve, PlanningFactory ldmf ) {
    Verb newVerb = Verb.get( ve );
    AllocatableWFPredicate et = new AllocatableWFPredicate( newVerb );
    return ( ( NewAllocatableWFPredicate ) et );
  }	


  public static NewAllocationsPredicate newAllocationsPredicate() {
    AllocationsPredicate ap = new AllocationsPredicate();
    return ( ( NewAllocationsPredicate ) ap );
  }
    
  /** for use from scripts. Discouraged to use from plugins directly */
  public static NewAllocationsPredicate newAllocationsPredicate( String ve, PlanningFactory ldmf ) {
    Verb newVerb = Verb.get( ve );
    AllocationsPredicate ap = new AllocationsPredicate( newVerb );
    return ( ( NewAllocationsPredicate ) ap );
  }	
}
