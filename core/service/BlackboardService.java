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

package org.cougaar.core.service;

import java.util.Collection;

import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.util.UnaryPredicate;

/**
 * This service provides transactional publish/subscribe access to
 * the local agent's blackboard.
 * <p>
 * Most developers should extend {@link
 * org.cougaar.core.plugin.ComponentPlugin} to handle threading and
 * opening/closing transactions.
 */
public interface BlackboardService extends Service {
  //
  //Subscriber/ Subscription stuff
  //

  Subscriber getSubscriber();
    
  /**
   * Request a subscription to all objects for which 
   * isMember.execute(object) is true.  The returned Collection
   * is a transactionally-safe set of these objects which is
   * guaranteed not to change out from under you during run()
   * execution.
   * 
   * subscribe() may be called any time after 
   * load() completes.
   */
  <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember);

  /**
   * like subscribe(UnaryPredicate), but allows specification of
   * some other type of Collection object as the internal representation
   * of the collection.
   * Alias for getSubscriber().subscribe(UnaryPredicate, Collection);
   */
  <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember, Collection<T> realCollection);

  /**
   * Alias for getSubscriber().subscribe(UnaryPredicate, boolean);
   */
  <T> Subscription<T> subscribe(UnaryPredicate<T> isMember, boolean isIncremental);
  
  /**
   * Alias for <code>getSubscriber().subscribe(UnaryPredicate, Collection, boolean);</code>
   * @param isMember a predicate to execute to ascertain
   * membership in the collection of the subscription.
   * @param realCollection a container to hold the contents of the subscription.
   * @param isIncremental should be true if an incremental subscription is desired.
   * An incremental subscription provides access to the incremental changes to the subscription.
   * @return the Subsciption.
   * @see org.cougaar.core.blackboard.Subscriber#subscribe
   * @see org.cougaar.core.blackboard.Subscription
   */
  <T> Subscription<T> subscribe(UnaryPredicate<T> isMember, Collection<T> realCollection, boolean isIncremental);

  /**
   * Primary subscribe method, which registers a new subscription.
   */
  <S extends Subscription> S subscribe(S subscription);

  /**
   * Issue a query against the logplan.  Similar in function to
   * opening a new subscription, getting the results and immediately
   * closing the subscription, but can be implemented much more efficiently.
   * Note: the initial implementation actually does exactly this.
   */
  <T> Collection<T> query(UnaryPredicate<T> isMember);

  /**
   * Cancels the given Subscription which must have been returned by a
   * previous invocation of subscribe().  Alias for
   * <code> getSubscriber().unsubscribe(Subscription)</code>.
   * @param subscription the subscription to cancel
   * @see org.cougaar.core.blackboard.Subscriber#unsubscribe
   */
  void unsubscribe(Subscription<?> subscription);
  
  int getSubscriptionCount();
  
  int getSubscriptionSize();

  int getPublishAddedCount();

  int getPublishChangedCount();

  int getPublishRemovedCount();
  
  /**
   * @return true iff collection contents have changed since the last 
   * transaction.
   */
  boolean haveCollectionsChanged();

  //
  // Blackboard changes publishing
  //

  void publishAdd(Object o);
  
  void publishRemove(Object o);

  void publishChange(Object o);
  
  /**
   * mark an element of the Plan as changed.
   * Behavior is not defined if the object is not a member of the plan.
   * There is no need to call this if the object was added or removed,
   * only if the contents of the object itself has been changed.
   * The changes parameter describes a set of changes made to the
   * object beyond those tracked automatically by the object class
   * (see the object class documentation for a description of which
   * types of changes are tracked).  Any additional changes are
   * merged in <em>after</em> automatically collected reports.
   * @param changes a set of ChangeReport instances or null.
   */
  void publishChange(Object o, Collection<? extends ChangeReport> changes); 

  // 
  // aliases for Transaction handling 
  //

  /**
   * Open a transaction by grabbing the transaction lock and updating
   * the subscriptions.  This method blocks waiting for the
   * transaction lock.
   */
  void openTransaction();

  /**
   * Attempt to open a transaction by attempting to grab the 
   * transaction lock and updating the collections (iff we got the 
   * lock).
   *
   * This is equivalent to the old (misnamed) tryLockSubscriber method
   * in PluginWrapper.
   *
   * @return true IFF a transaction was opened.
   */
  boolean tryOpenTransaction();

  /**
   * Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction(), flushing the change
   * tracking (delta) lists of any open subscriptions.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   */
  void closeTransaction() throws SubscriberException;
    
  /**
   * Close a transaction opened by openTransaction() or a successful
   * tryOpenTransaction(), but don't reset subscription changes or
   * clear delta lists.  In effect, defers changes tracked until next
   * transaction.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   */
  void closeTransactionDontReset();

  /**
   * Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction().
   * @param resetp IFF true, all subscriptions will have
   * their resetChanges() method called to clear any delta lists, etc.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   * @deprecated Use {@link #closeTransactionDontReset closeTransactionDontReset}
   * This method becomes private after deprecation period expires.
   */
  void closeTransaction(boolean resetp) throws SubscriberException;


  /**
   * Check to see if a transaction is open and owned by the current thread.
   * There is no method to check to see if the subscribe has a transaction 
   * open which is owned by a different thread, since that would not be a safe 
   * operation.
   * @return true IFF the current thread already has an open transaction.
   * @note This method should really only be used in assertions and the like, since
   * code should generally know exactly when it is or is not inside an open transaction.
   */
  boolean isTransactionOpen();
    
  //
  // plugin hooks
  //

  /**
   * called when the client (Plugin) requests that it be waked again.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   */
  void signalClientActivity();

  /** register a watcher of subscription activity */
  SubscriptionWatcher registerInterest(SubscriptionWatcher w);

  /** register a watcher of subscription activity */
  SubscriptionWatcher registerInterest();

  /** stop watching subscription activity */
  void unregisterInterest(SubscriptionWatcher w) throws SubscriberException;

  //
  // persistence hooks
  //

  /**
   * indicate that this blackboard service information should (or should not)
   * be persisted.
   */
  void setShouldBePersisted(boolean value);
  /** @return the current value of the persistence setting */
  boolean shouldBePersisted();

//    /** indicate that the blackboard view is ready to persist */
//    void setReadyToPersist();

  /**
   * is this BlackboardService the result of a rehydration of a persistence 
   * snapshot? 
   */
  boolean didRehydrate();

  /**
   * Take a persistence snapshot now. If called from inside a
   * transaction (the usual case for a plugin), the transaction will
   * be closed and reopened. This means that a plugin must first
   * process all of its existing envelopes before calling
   * <code>persistNow()</code> and then process a potential new set of
   * envelopes after re-opening the transaction. Otherwise, the
   * changes will be lost.
   * @exception PersistenceNotEnabledException
   */
  void persistNow() throws PersistenceNotEnabledException;

  /** Hook to allow access to Blackboard persistence mechanism */
  Persistence getPersistence();

  /**
   * BlackboardService.Delegate is an instantiable convenience class which may be
   * used by Binder writers to bind BlackboardService, either as a simple
   * protective indirection layer, or as the base class for more complex behavior.
   * This implementation merely passes through all requests to a constructor
   * specified delegate.
   */
  class Delegate implements BlackboardService {
    private final BlackboardService bs;
    public Delegate(BlackboardService bs) {
      this.bs = bs;
    }
    public Subscriber getSubscriber() { 
      return bs.getSubscriber();
    }
    public <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember) { 
      return bs.subscribe(isMember); 
    }
    public <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember, Collection<T> realCollection) {
      return bs.subscribe(isMember, realCollection);
    }
    public <T> Subscription<T> subscribe(UnaryPredicate<T> isMember, boolean isIncremental) {
      return bs.subscribe(isMember, isIncremental);
    }
    public <T> Subscription<T> subscribe(UnaryPredicate<T> isMember, Collection<T> realCollection, boolean isIncremental) {
      return bs.subscribe(isMember, realCollection, isIncremental);
    }
    public <S extends Subscription> S subscribe(S subscription) {
      return bs.subscribe(subscription);
    }
    public <T> Collection<T> query(UnaryPredicate<T> isMember) {
      return bs.query(isMember);
    }
    public void unsubscribe(Subscription<?> subscription) {
      bs.unsubscribe(subscription);
    }
    public int getSubscriptionCount() {
      return bs.getSubscriptionCount();
    }
    public int getSubscriptionSize() {
      return bs.getSubscriptionSize();
    }
    public int getPublishAddedCount() {
      return bs.getPublishAddedCount();
    }
    public int getPublishChangedCount() {
      return bs.getPublishChangedCount();
    }
    public int getPublishRemovedCount() {
      return bs.getPublishRemovedCount();
    }
    public boolean haveCollectionsChanged() {
      return bs.haveCollectionsChanged();
    }
    public void publishAdd(Object o) {
      bs.publishAdd(o);
    }
    public void publishRemove(Object o) {
      bs.publishRemove(o);
    }
    public void publishChange(Object o) {
      bs.publishChange(o);
    }
    public void publishChange(Object o, Collection<? extends ChangeReport> changes) {
      bs.publishChange(o,changes);
    }
    public void openTransaction() {
      bs.openTransaction();
    }
    public boolean tryOpenTransaction() {
      return bs.tryOpenTransaction();
    }
    public void closeTransaction() throws SubscriberException {
      bs.closeTransaction();
    }
    public void closeTransactionDontReset() throws SubscriberException {
      bs.closeTransactionDontReset();
    }
    /**
     * @deprecated Use {@link #closeTransactionDontReset closeTransactionDontReset}
     */
    public void closeTransaction(boolean resetp) throws SubscriberException {
      bs.closeTransaction(resetp);
    }
    public boolean isTransactionOpen() {
      return bs.isTransactionOpen();
    }
    public void signalClientActivity() {
      bs.signalClientActivity();
    }
    public SubscriptionWatcher registerInterest(SubscriptionWatcher w) {
      return bs.registerInterest(w);
    }
    public SubscriptionWatcher registerInterest() {
      return bs.registerInterest();
    }
    public void unregisterInterest(SubscriptionWatcher w) throws SubscriberException {
      bs.unregisterInterest(w);
    }
    public void setShouldBePersisted(boolean value) {
      bs.setShouldBePersisted(value);
    }
    public boolean shouldBePersisted() {
      return bs.shouldBePersisted();
    }
    public void persistNow() throws org.cougaar.core.persist.PersistenceNotEnabledException {
      bs.persistNow();
    }
    public boolean didRehydrate() {
      return bs.didRehydrate();
    }
    public Persistence getPersistence() {
      return bs.getPersistence();
    }
  }
}
