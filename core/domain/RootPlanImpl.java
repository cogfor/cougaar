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

package org.cougaar.core.domain;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.blackboard.ABATranslation;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.PublishHistory;
import org.cougaar.core.blackboard.UniqueObjectSet;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * Standard implementation of {@link RootPlan}. 
 */
public class RootPlanImpl
implements RootPlan, SupportsDelayedLPActions
{
  private Blackboard blackboard;

  /** is this a UniqueObject? */
  private static final UnaryPredicate uniqueObjectP =
    new UniqueObjectPredicate();
  private static final class UniqueObjectPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return (o instanceof UniqueObject) && (((UniqueObject) o).getUID() != null);
    }
  }

  /** Private container for UniqueObject lookup.  */
  private UniqueObjectSet uniqueObjectSet = new UniqueObjectSet();
  private CollectionSubscription uniqueObjectCollection;

  public void setupSubscriptions(Blackboard blackboard) {
    this.blackboard = blackboard;
    uniqueObjectCollection = new CollectionSubscription(uniqueObjectP, uniqueObjectSet);
    blackboard.subscribe(uniqueObjectCollection);
  }

  public UniqueObject findUniqueObject(UID uid) {
    return uniqueObjectSet.findUniqueObject(uid);
  }

  // Implementation of BlackboardServesLogProvider

  /**
   * Apply predicate against the entire "Blackboard".
   * User provided predicate
   */
  public Enumeration searchBlackboard(UnaryPredicate predicate) {
    return blackboard.searchBlackboard(predicate);
  }

  public int countBlackboard(UnaryPredicate predicate) {
    return blackboard.countBlackboard(predicate);
  }

  /**
   * Add Object to the RootPlan Collection
   * (All subscribers will be notified)
   */
  public void add(Object o) {
    blackboard.add(o);
  }

  /**
   * Removed Object to the RootPlan Collection
   * (All subscribers will be notified)
   */
  public void remove(Object o) {
    blackboard.remove(o);
  }

  /**
   * Change Object to the RootPlan Collection
   * (All subscribers will be notified)
   */
  public void change(Object o) {
    blackboard.change(o, null);
  }

  /**
   * Change Object to the RootPlan Collection
   * (All subscribers will be notified)
   */
  public void change(Object o, Collection changes) {
    blackboard.change(o, changes);
  }

  /**
   * Alias for sendDirective(Directive, null);
   */
  public void sendDirective(Directive dir) {
    blackboard.sendDirective(dir, null);
  }

  /**
   * Reliably send a directive.
   * <p> 
   * The message transport takes pains to retransmit this message
   * until it is acknowledged, even if agents crash.  When a crashed
   * agent recovers, the blackboard invokes the {@link
   * RestartLogicProvider}s.
   */
  public void sendDirective(Directive dir, Collection changes) {
    blackboard.sendDirective(dir, changes);
  }

  public PublishHistory getHistory() {
    return blackboard.getHistory();
  }

  //
  // DelayedLPAction support
  //
  
  private Object dlpLock = new Object();
  private HashMap dlpas = new HashMap(11);
  private HashMap dlpas1 = new HashMap(11);

  public void executeDelayedLPActions() {
    synchronized (dlpLock) {
      // loop in case we get cascades somehow (we don't seem to)
      while (dlpas.size() > 0) {
        // flip the map
        HashMap pending = dlpas;
        dlpas = dlpas1;
        dlpas1 = pending;

        // scan the pending map
        for (Iterator i = pending.values().iterator(); i.hasNext(); ) {
          DelayedLPAction dla = (DelayedLPAction) i.next();
          try {
            dla.execute(this);
          } catch (RuntimeException re) {
            System.err.println("DelayedLPAction "+dla+" threw: "+re);
            re.printStackTrace();
          }
        }

        // clear the pending queue before iterating.
        pending.clear();
      }
    }
  }
  
  public void delayLPAction(DelayedLPAction dla) {
    synchronized (dlpLock) {
      DelayedLPAction old = (DelayedLPAction) dlpas.get(dla);
      if (old != null) {
        old.merge(dla);
      } else {
        dlpas.put(dla,dla);
      }
    }
  }

  public ABATranslation getABATranslation(AttributeBasedAddress aba) {
    return blackboard.getABATranslation(aba);
  }
}
