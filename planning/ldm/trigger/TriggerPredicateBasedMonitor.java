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


package org.cougaar.planning.ldm.trigger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.plugin.legacy.PluginDelegate;
import org.cougaar.util.UnaryPredicate;

/**
 * A TriggerPredicateBasedMonitor is a kind of monitor that generates a
 * subscription for objects
 */

public class TriggerPredicateBasedMonitor implements TriggerMonitor {
  
  transient private IncrementalSubscription my_subscription;
  private UnaryPredicate my_predicate;
  transient private List assobjects = null;

  public TriggerPredicateBasedMonitor(UnaryPredicate predicate) {
    my_predicate = predicate;
    my_subscription = null;
  }

  public UnaryPredicate getPredicate() { return my_predicate; }

  public void EstablishSubscription(IncrementalSubscription subscription) {
    my_subscription = subscription;
  }

  public IncrementalSubscription getSubscription() {
    return my_subscription;
  }

  public Object[] getAssociatedObjects() {
    if (assobjects == null) {
      assobjects = new ArrayList();
    }
    assobjects.clear();
    // Pull objects out of subscription
    if (my_subscription != null) {
      // check for changes
      Enumeration clist = my_subscription.getChangedList();
      while (clist.hasMoreElements()){
        Object subobj =  clist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(subobj) ) {
          assobjects.add(subobj);
        }
      }
      // check for additions
      Enumeration alist = my_subscription.getAddedList();
      while (alist.hasMoreElements()){
        Object subobj = alist.nextElement();
        // make sure that this object isn't already in the list, we don't need it 
        // twice if it happened to get added and changed before we got a chance to run.
        if ( ! assobjects.contains(subobj) ) {
          assobjects.add(subobj);
        }
      }
       
    }
    //System.err.println("Returning "+assobjects.size()+" objects to be tested");      
    return assobjects.toArray();
  }

  public boolean ReadyToRun(PluginDelegate pid) { 
    // Check if subscription has changes  (don't need pid for right now)
    if ( (my_subscription != null) && (my_subscription.hasChanged()) ) {
      return true;
    }
    return false;
  }

  public void IndicateRan(PluginDelegate pid) {
    // Probably nothing to do in this case
  }

  

}
