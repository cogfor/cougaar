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

package org.cougaar.planning.plugin.legacy;

import java.util.Collection;
import java.util.Date;

import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.planning.ldm.ClusterServesPlugin;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.util.UnaryPredicate;

/** 
 * An interface for getting at the (normally) protected Plan API 
 * methods of a Plugin.  Essentially all the of the protected Plan 
 * API methods of PluginAdapter can be accessed via these public
 * methods.
 * @see PluginAdapter#getDelegate()
 **/

public interface PluginDelegate {
  BlackboardService getBlackboardService();
  /** Alias for getBlackboardService() **/
  BlackboardService getSubscriber();
  ClusterServesPlugin getCluster();
  LDMServesPlugin getLDM();
  PlanningFactory getFactory();
  Factory getFactory(String domainname);
  MessageAddress getMessageAddress();
  void openTransaction();
  boolean tryOpenTransaction();
  void closeTransaction() throws SubscriberException;
  void closeTransactionDontReset() throws SubscriberException ;
  /** @deprecated Use {@link #closeTransactionDontReset closeTransactionDontReset}
   **/
  void closeTransaction(boolean resetp) throws SubscriberException ;
  boolean wasAwakened();
  void wake();
  long currentTimeMillis();
  Date getDate();
  Subscription subscribe(UnaryPredicate isMember);
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection);
  Subscription subscribe(UnaryPredicate isMember, boolean isIncremental);
  Subscription subscribe(UnaryPredicate isMember, Collection realCollection, boolean isIncremental);
  void unsubscribe(Subscription collection);
  Collection query(UnaryPredicate isMember);
  void publishAdd(Object o);
  void publishRemove(Object o);
  void publishChange(Object o);
  void publishChange(Object o, Collection changes);
  Collection getParameters();
  boolean didRehydrate();
  ServiceBroker getServiceBroker();

  /** Attempt to stake a claim on a logplan object, essentially telling 
   * everyone else that you and only you will be disposing, modifying, etc.
   * it.
   * Calls Claimable.tryClaim if the object is Claimable.
   * @return true IFF success.
   **/
  boolean claim(Object o);

  /** Release an existing claim on a logplan object.  This is likely to
   * thow an exception if the object had not previously been (successfully) 
   * claimed by this plugin.
   **/
  void unclaim(Object o);
}
