/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.persist.Persistence;
import org.cougaar.util.CallerTracker;
import org.cougaar.util.LockFlag;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * The standard implementation of the {@link
 * org.cougaar.core.service.BlackboardService}.
 *
 * @property org.cougaar.core.blackboard.enforceTransactions
 * Set to <em>false</em> to disable checking for clients
 * of BlackboardService publishing changes to the blackboard outside
 * of a transaction.
 *
 * @property org.cougaar.core.blackboard.debug
 * Set to true to additional checking on blackboard transactions.  
 * For instance, it will attempt to look for changes to blackboard
 * objects which have not been published at transaction close time.
 *
 * @note Although Subscriber directly implements all the methods of
 * BlackboardService, it declines to implement the interface to avoid
 * the Subscriber class itself <em>and all extending classes</em> from
 * being Services.
 *
 * @property org.cougaar.core.blackboard.timestamp
 * Set to true to enable EnvelopeMetrics and TimestampSubscriptions
 * (defaults to false).
 *
 * @property org.cougaar.core.blackboard.trackPublishers
 * Set to true to enable PublisherSubscriptions
 * (defaults to false).
 */
public class Subscriber {
  private static final Logger logger = Logging.getLogger(Subscriber.class);

  private static final boolean isEnforcing =
    SystemProperties.getBoolean(
        "org.cougaar.core.blackboard.enforceTransactions",
        true);

  private static final boolean warnUnpublishChanges = 
    SystemProperties.getBoolean("org.cougaar.core.blackboard.debug");

  private static final boolean enableTimestamps = 
    SystemProperties.getBoolean("org.cougaar.core.blackboard.timestamp") ||
    SystemProperties.getBoolean("org.cougaar.core.blackboard.trackPublishers");

  private BlackboardClient theClient = null;
  private Distributor theDistributor = null;
  private String subscriberName = "";
  private boolean shouldBePersisted = true;
  private boolean firstTransactionComplete = false;

  protected Subscriber(){} 

  /**
   * Create a subscriber that provides subscription services 
   * to a client and send outgoing messages to a Distributor.
   * Plugin clients will use this API.
   */
  public Subscriber(BlackboardClient client, Distributor distributor) {
    this(
        client, 
        distributor, 
        ((client != null) ? client.getBlackboardClientName() : null));
  }

  public Subscriber(BlackboardClient client, Distributor distributor, String subscriberName) {
    setClientDistributor(client,distributor);
    setName(subscriberName);
  }

  public void setClientDistributor(BlackboardClient client, Distributor newDistributor)
  {
    theClient = client;
    if (theDistributor != newDistributor) {
      if (theDistributor != null) {
        theDistributor.unregisterSubscriber(this);
      }
      theDistributor = newDistributor;
      if (theDistributor != null) {
        theDistributor.registerSubscriber(this);
      }
    }
  }

  public void setName(String newName) {
    subscriberName = newName;
  }

  public String getName() {
    return subscriberName;
  }

  public boolean shouldBePersisted() {
    return shouldBePersisted;
  }

  public void setShouldBePersisted(boolean shouldBePersisted) {
    this.shouldBePersisted = shouldBePersisted;
  }

  boolean isReadyToPersist() {
    return firstTransactionComplete;
  }

  public void setReadyToPersist() {
    theDistributor.discardRehydrationInfo(this);
    firstTransactionComplete = true;
  }

  public boolean didRehydrate() {
    boolean result = theDistributor.didRehydrate(this);
    return result;
  }

  public void persistNow() {
    boolean inTransaction = (transactionLock.getBusyFlagOwner() == Thread.currentThread());
    if (inTransaction) closeTransaction();
    theDistributor.persistNow();
    if (inTransaction) openTransaction();
  }

  public Persistence getPersistence() {
    return theDistributor.getPersistence();
  }

  /**
   * Move inboxes into subscriptions.
   */
  protected boolean privateUpdateSubscriptions() {
    boolean changedp = false;
    synchronized (subscriptions) {
      transactionAllowsQuiescence = inboxAllowsQuiescence;
      transactionEnvelopes = flushInbox();
      try {
        for (int i = 0, n = subscriptions.size(); i < n; i++) {
          Subscription subscription = (Subscription) subscriptions.get(i);
          for (int j = 0, l = transactionEnvelopes.size(); j<l; j++) {
            Envelope envelope = (Envelope) transactionEnvelopes.get(j);
            try {
              changedp |= subscription.apply(envelope);
            } catch (PublishException pe) {
              Logger logger =  Logging.getLogger(Subscriber.class);
              String message = pe.getMessage();
              logger.error(message);

              BlackboardClient currentClient = null;
              //                 if (envelope instanceof OutboxEnvelope) {
              //                   OutboxEnvelope e = (OutboxEnvelope) envelope;
              //                   currentClient = e.theClient;
              //                 }
//              if (currentClient == null) {
                currentClient = BlackboardClient.current.getClient();
//              }
              String thisPublisher = null;
              if (currentClient != null) {
                thisPublisher = currentClient.getBlackboardClientName();
              }
              if (envelope instanceof Blackboard.PlanEnvelope) {
                if (thisPublisher == null) {
                  thisPublisher = "Blackboard";
                } else {
                  thisPublisher = "Blackboard after " + thisPublisher;
                }
              } else if (thisPublisher == null) {
                thisPublisher = "Unknown";
              }
              pe.printStackTrace(" This publisher: " + thisPublisher);
              if (!pe.priorStackUnavailable) {
                if (pe.priorStack == null) {
                  System.err.println("Prior publisher: Unknown");
                }
              } else {
                if (pe.priorStack == null) {
                  System.err.println("Prior publisher: Not set");
                } else {
                  pe.priorStack.printStackTrace();
                }
              }
            } 
            catch (RuntimeException ire) {
              BlackboardClient currentClient = BlackboardClient.current.getClient();
              String thisPublisher = null;
              if (currentClient != null) {
                thisPublisher = currentClient.getBlackboardClientName();
              }
              logger.error(
                  "Exception while applying envelopes in "+
                  currentClient+"/"+thisPublisher, ire);
            }
          }
        }
      } catch (RuntimeException re) {
        re.printStackTrace();
      }
    }
    return changedp;
  }

  /**
   * Report changes that the plugin published.
   * These changes are represented by the outbox.
   */
  protected Envelope privateGetPublishedChanges() {
    Envelope box = flushOutbox();
    if (transactionEnvelopes != null) {
      recycleInbox(transactionEnvelopes);
      transactionEnvelopes = null;
      transactionAllowsQuiescence = true;
    } else {
      recycleInbox(flushInbox());
    }
    if (enableTimestamps &&
        (box instanceof TimestampedEnvelope)) {
      TimestampedEnvelope te = (TimestampedEnvelope) box;
      te.setName(getName());
      te.setTransactionOpenTime(openTime);
      te.setTransactionCloseTime(System.currentTimeMillis());
    }
    return box;
  }

  /**
   * Accessors to persist our inbox state
   */
  public List getTransactionEnvelopes() {
    return transactionEnvelopes;
  }

  public List getPendingEnvelopes() {
    return pendingEnvelopes;
  }

  //////////////////////////////////////////////////////
  //              Subscriptions                       //
  //////////////////////////////////////////////////////
  private int publishAddedCount;
  private int publishChangedCount;
  private int publishRemovedCount;

  // now unused
  public int getSubscriptionCount() {
    // synchronized (subscriptions) {}
    return subscriptions.size();
  }
  
  // unused?
  public int getSubscriptionSize() {
    int size = 0;
    // synchronized (subscriptions) {} 
    int l = subscriptions.size();
    for (int i = 0; i < l; i++) {
      Object s = subscriptions.get(i);
      if (s instanceof CollectionSubscription) {
        size += ((CollectionSubscription)s).size();
      }
    }
    return size;
  }

  public int getPublishAddedCount() {
    return publishAddedCount;
  }

  public int getPublishChangedCount() {
    return publishChangedCount;
  }

  public int getPublishRemovedCount() {
    return publishRemovedCount;
  }


  /** our set of active subscriptions. Access must be synchronized on self. */
  protected final List subscriptions = new ArrayList(5);

  protected void resetSubscriptionChanges() {
    synchronized (subscriptions) {
      int l = subscriptions.size();
      for (int i=0; i<l; i++) {
        Subscription s = (Subscription) subscriptions.get(i);
        s.resetChanges();
      }
      resetHaveCollectionsChanged();
    }
  }

  /**
   * Subscribe to a collection service with isMember, default inner
   * collection and supporting incremental change queries.
   * @note Although allowed, use of DynamicUnaryPredicate can be extremely expensive
   * and tends to create as many problems as it solves.  When in pedantic mode,
   * warning are emitted when DynamicUnaryPredicate is used. Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  @SuppressWarnings("unchecked")
  public <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember) {
    return (IncrementalSubscription<T>) subscribe(isMember, null, true);
  }

  /**
   * Subscribe to a collection service with isMember, default inner
   * collection and specifying if you want incremental change query support.
   * @note Although allowed, use of DynamicUnaryPredicate can be extremely expensive
   * and tends to create as many problems as it solves.  When in pedantic mode,
   * warning are emitted when DynamicUnaryPredicate is used. Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public <T> Subscription<T> subscribe(UnaryPredicate<T> isMember, boolean isIncremental) {
    return subscribe(isMember, null, isIncremental);
  }

  /**
   * Subscribe to a collection service with isMember, specifying inner
   * collection and supporting incremental change queries.
   * @note Although allowed, use of DynamicUnaryPredicate can be extremely expensive
   * and tends to create as many problems as it solves.  When in pedantic mode,
   * warning are emitted when DynamicUnaryPredicate is used. Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  @SuppressWarnings("unchecked")
  public <T> IncrementalSubscription<T> subscribe(UnaryPredicate<T> isMember, Collection<T> realCollection){
    return (IncrementalSubscription<T>) subscribe(isMember, realCollection, true);
  }

  /**
   * Subscribe to a collection service.
   * Tells the Distributor about its interest, but should not block,
   * even if there are lots of "back issues" to transmit.
   * This is the full form.
   * @param isMember The defining predicate for the slice of the blackboard.
   * @param realCollection The real container wrapped by the returned value.
   * @param isIncremental IFF true, returns a container that supports delta
   * lists.
   * @return The resulting Subscription
   * @see IncrementalSubscription
   * @note Although allowed, use of DynamicUnaryPredicate can be extremely expensive
   * and tends to create as many problems as it solves.  When in pedantic mode,
   * warning are emitted when DynamicUnaryPredicate is used. Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public <T> Subscription<T> subscribe(
      UnaryPredicate<T> isMember,
      Collection<T> realCollection,
      boolean isIncremental) {
    Subscription<T> sn;

    if (realCollection == null) 
      realCollection = new HashSet<T>();

    if (isIncremental) {
      sn = new IncrementalSubscription<T>(isMember, realCollection);
    } else {
      sn = new CollectionSubscription<T>(isMember, realCollection);
    }
    return subscribe(sn);
  }

  /** Primary subscribe method.  Register a new subscription. */
  public final <S extends Subscription> S subscribe(S subscription) {
    // Strictly speaking, subscribe can be done outside a transaction, but the 
    // state of filled subscription w/rt the rest of the subscriptions
    // is suspect if it isn't.
    checkTransactionOK("subscribe()");

    synchronized (subscriptions) {
      subscription.setSubscriber(this);
      subscriptions.add(subscription);
      theDistributor.fillSubscription(subscription);
    }
    setHaveNewSubscriptions();  // make sure we get counted.
    return subscription;
  }
    
  /** lightweight query of Blackboard */
  public final <T> Collection<T> query(UnaryPredicate<T> isMember) {
    checkTransactionOK("query(UnaryPredicate)");
    QuerySubscription s = new QuerySubscription(isMember);
    s.setSubscriber(this);      // shouldn't really be needed
    theDistributor.fillQuery(s);
    return s.getCollection();
  }

  private static final CallerTracker pTracker = CallerTracker.getShallowTracker(2);

  final void checkTransactionOK(String methodname, Object arg) {
    if (this instanceof Blackboard) return;               // No check for Blackboard

    if (Blackboard.PEDANTIC && arg instanceof Collection && pTracker.isNew()) {
      if (logger.isWarnEnabled())
	logger.warn("PEDANTIC: A Collection published by "+theClient, new Throwable());
    }

    if (!isMyTransaction()) {
      if (arg != null) { methodname = methodname+"("+arg+")"; }
      logger.error(toString()+"."+methodname+" called outside of transaction", new Throwable());
      //throw new RuntimeException(methodname+" called outside of transaction boundaries");
    }
  }

  final void checkTransactionOK(String methodname) {
    checkTransactionOK(methodname, null);
  }

  /**
   * Stop subscribing to a previously obtained Subscription. The
   * Subscription must have been returned from a previous call to
   * subscribe.
   * @param subscription the Subscription that is to be cancelled.
   */
  public void unsubscribe(Subscription subscription) {
    // strictly speaking, this doesn't have to be done inside a transaction, but
    // we'll check anyway to be symmetric with subscribe.
    checkTransactionOK("unsubscribe()");
    synchronized (subscriptions) {
      subscriptions.remove(subscription);
    }
  }

  /*
   * Inbox invariants:
   * pendingEnvelopes accumulates new envelopes for the next transaction (always).
   * transactionEnvelopes has the previous pendingEnvelopes during a
   * transaction, null otherwise.
   * idleEnvelopes has an empty list when no transaction is active.
   *
   * The list cycle around from idle to pending to transaction back to
   * idle. idle and transaction are never null at the same time; one
   * of them always has the list the pendingEnvelopes does not have
   *
   */
  private List pendingEnvelopes = new ArrayList();     // Envelopes to be added at next transaction
  private List transactionEnvelopes = null;            // Envelopes of current transaction
  private List idleEnvelopes = new ArrayList();        // Alternate list
  private final Object inboxLock = new Object();       // For synchronized access to inboxes
  private boolean inboxAllowsQuiescence = true;        // True if inbox allows quiescence
  private boolean transactionAllowsQuiescence = true;  // True if inbox being processed allowed quiescence.

  /**
   * Called by non-client methods to add an envelope to our inboxes.
   * This is complicated because we wish to avoid holding envelopes
   * when there is no possibility of their ever being used (no
   * subscriptions). A simple test of the number of subscriptions is
   * insufficient because, if a transaction is open, new subscriptions
   * may be created that, in later transactions, need to receive the
   * envelopes. So the test includes a test of transactions being
   * open. We use transactionLock.tryGetBusyFlag() because we can't
   * block and the fact that the lock is busy, is a sufficient
   * indication that we must put the new envelopes into the inbox. It
   * may turn out that the inbox did not need to be stuffed (because
   * there will not be any subscriptions), but this is handled when
   * the transaction is closed where the inbox is emptied if there are
   * no subscriptions.
   */
  public void receiveEnvelopes(List envelopes, boolean envelopeQuiescenceRequired) {
    boolean signalActivity = false;
    synchronized (inboxLock) {
      boolean notBusy = transactionLock.tryGetBusyFlag(); 
      // if notBusy, then the client isn't running (and wont) until we're done.
      // if !notBusy, then the client IS running so we need to dump the envelopes
      //  in regardless (because it might add a watcher or subscription
      boolean hasWatchers; synchronized (watchers) { hasWatchers = !watchers.isEmpty(); }
      boolean hasSubscriptions = !subscriptions.isEmpty();
      if (hasSubscriptions || (hasWatchers && !notBusy)) {
        pendingEnvelopes.addAll(envelopes);
        if (envelopeQuiescenceRequired) { inboxAllowsQuiescence = false; }
        signalActivity = true;
      } else {
        if (logger.isInfoEnabled() && !hasSubscriptions && !notBusy && !hasWatchers) {
          logger.info(
              this + ".receiveEnvs: Fix for bug 3328 means"+
              " we're not distributing the outbox here cause no watchers.");
        }
      }
      if (notBusy) transactionLock.freeBusyFlag();
    }
    if (signalActivity) signalExternalActivity();
  }

  public boolean isBusy() {
    synchronized (inboxLock) {
      return (pendingEnvelopes.size() > 0);
    }
  }

  public boolean isQuiescent() {
    synchronized (inboxLock) {
      return (inboxAllowsQuiescence && transactionAllowsQuiescence);
    }
  }

  private List flushInbox() {
    synchronized (inboxLock) {
      List result = pendingEnvelopes;
      pendingEnvelopes = idleEnvelopes;
      idleEnvelopes = null;
      inboxAllowsQuiescence = true;
      return result;
    }
  }

  private void recycleInbox(List old) {
    old.clear();
    idleEnvelopes = old;
  }

  /**
   * outbox data structure - an Envelope used to encapsulate 
   * outgoing changes to collections.
   */
  private Envelope outbox = createEnvelope();

  protected Envelope flushOutbox() {
    if (outbox.size() == 0) return null;
    Envelope result = outbox;
    outbox = createEnvelope();
    return result;
  }

// This won't work with persistence turned on. Don't _ever_ use operationally (ray)
//   public static class OutboxEnvelope extends Envelope {
//     public OutboxEnvelope(BlackboardClient client) {
//       theClient = client;
//     }
//     public BlackboardClient theClient;
//   }

  /** factory method for creating Envelopes of the correct type */
  protected Envelope createEnvelope() {
    if (enableTimestamps) {
      return new TimestampedEnvelope();
    } else {
      return new Envelope();
    }
// return new OutboxEnvelope(getClient());  // for debugging
  }

  // might want to make the syncs finer-grained
  /**
   * called whenever the client adds an object to a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   */
  protected EnvelopeTuple clientAddedObject(Object o) {
    // attempt to claim the object
    claimObject(o);

    return outbox.addObject(o);
  }

  /**
   * called whenever the client removes an object from a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   */
  protected EnvelopeTuple clientRemovedObject(Object o) {
    // attempt to unclaim the object
    unclaimObject(o);

    return outbox.removeObject(o);
  }

  /**
   * called whenever the client changes an object in a collection
   * to notify the rest of the world of the change.
   * Actual Changes to the collection only happen via this api.
   */
  protected EnvelopeTuple clientChangedObject(Object o, List<? extends ChangeReport> changes) {
    return outbox.changeObject(o, changes);
  }
  
  /**
   * Add an object to the blackboard.
   * <p> 
   * Behavior is not defined if the object was already on the blackboard.
   * @note Although strictly allowed, it takes special care to properly publish a
   * raw Collection object to the Blackboard.  Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public final void publishAdd(Object o) {
    checkTransactionOK("add", o);

    if (theDistributor.history != null) theDistributor.history.publishAdd(o);
    if (o instanceof ActiveSubscriptionObject ) {
      ((ActiveSubscriptionObject)o).addingToBlackboard(this, false);
      if (!ActiveSubscriptionObject.deferCommit) {
        ((ActiveSubscriptionObject)o).addingToBlackboard(this, true);
      }
    }

    if (o instanceof Publishable) {
      //List crs =  // var unused
      Transaction.getCurrentTransaction().getChangeReports(o); // side effects
    }

    // if we made it this far publish the object and return true.
    clientAddedObject(o);
    publishAddedCount++;
  }
  
  /**
   * Remove an object from the blackboard.
   * <p> 
   * Behavior is not defined if the object was not already on the blackboard.
   *
   * @note Although strictly allowed, it takes special care to properly publish a
   * raw Collection object to the Blackboard.  Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public final void publishRemove(Object o) {
    checkTransactionOK("remove", o);

    if (theDistributor.history != null) theDistributor.history.publishRemove(o);
    if (o instanceof ActiveSubscriptionObject ) {
      ((ActiveSubscriptionObject)o).removingFromBlackboard(this, false);
      if (!ActiveSubscriptionObject.deferCommit) {
        ((ActiveSubscriptionObject)o).removingFromBlackboard(this, true);
      }
    }

    if (o instanceof Publishable) {
      List crs = Transaction.getCurrentTransaction().getChangeReports(o);
      if (warnUnpublishChanges) {
        if (crs != null && crs.size()>0) {
	  if (logger.isWarnEnabled())
	    logger.warn("Warning: publishRemove("+o+") is dropping outstanding changes:\n\t"+crs);
        }
      }
    }

    clientRemovedObject(o);
    publishRemovedCount++;
  }

  /**
   * Convenience function for publishChange(o, null).
   * @note Although strictly allowed, it takes special care to properly publish a
   * raw Collection object to the Blackboard.  Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public final void publishChange(Object o) {
    publishChange(o, null);
  }

  /**
   * Mark an object on the blackboard as changed.
   * <p> 
   * Behavior is not defined if the object is not on the blackboard.
   * <p> 
   * There is no need to call this if the object was added or removed,
   * only if the contents of the object itself has been changed.
   * The changes parameter describes a set of changes made to the
   * object beyond those tracked automatically by the object class
   * (see the object class documentation for a description of which
   * types of changes are tracked).  Any additional changes are
   * merged in <em>after</em> automatically collected reports.
   * @param changes a set of ChangeReport instances or null.
   * @note Although strictly allowed, it takes special care to properly publish a
   * raw Collection object to the Blackboard.  Disable Blackboard.PEDANTIC to quiet
   * such warnings if you are sure you want to do this.
   * @see Blackboard#PEDANTIC
   */
  public final void publishChange(Object o, Collection<? extends ChangeReport> changes) {
    checkTransactionOK("change", o);    

    if (theDistributor.history != null) theDistributor.history.publishChange(o);
    if (o instanceof ActiveSubscriptionObject ) {
      ((ActiveSubscriptionObject)o).changingInBlackboard(this, false);
      if (!ActiveSubscriptionObject.deferCommit) {
        ((ActiveSubscriptionObject)o).changingInBlackboard(this, true);
      }
    }

    List crs = null;
    if (o instanceof Publishable) {
      crs = Transaction.getCurrentTransaction().getChangeReports(o);
    }

    // convert null or empty changes to the "anonymous" list
    if (isZeroChanges(changes)) {
      if (isZeroChanges(crs)) {
        crs = AnonymousChangeReport.LIST;
      } else {
	// use crs as-is
      }
    } else {
      if (isZeroChanges(crs)) {
        crs = new ArrayList(changes);
      } else {
        crs.addAll(changes);
      }
    }

    // if we made it this far publish the change and return true.
    clientChangedObject(o, crs);
    publishChangedCount++;
  }

  private final boolean isZeroChanges(final Collection<? extends ChangeReport> c) {
    return
      ((c == null) || 
       (c == AnonymousChangeReport.LIST) || 
       (c == AnonymousChangeReport.SET) || 
       (c.isEmpty()));
  }


  /**
   * A extension subscriber may call this method to execute bulkAdd transactions.
   * This is protected because it is of very limited to other than persistance plugins.
   *  Note that Blackboard does something like
   * this by hand constructing an entire special-purpose envelope.  This, however, is
   * for use in-band, in-transaction.  
   *  The Collection passed MUST be immutable, since there may be many consumers,
   * each running at different times.
   */
  protected EnvelopeTuple bulkAddObject(Collection c) {
    checkTransactionOK("bulkAdd", c);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(c);

    return t;
  }

  /**
   * Safer version of bulkAddObject(Collection).
   * Creates a Collection from the Enumeration and passes it into
   * the envelope.
   */
  protected EnvelopeTuple bulkAddObject(Enumeration en) {
    checkTransactionOK("bulkAdd", en);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(en);

    return t;
  }

  protected EnvelopeTuple bulkAddObject(Iterator en) {
    checkTransactionOK("bulkAdd", en);    

    EnvelopeTuple t;
    t = outbox.bulkAddObject(en);

    return t;
  }

  //
  // Transaction handling.
  //
  /*
   * It would be nice if we could merge the Transaction object with the
   * older open/close transaction code somehow - there is some redundancy
   * and the current parallel implementations are somewhat confusing.
   */


  /**
   * The transaction lock.  At most one watcher per subscriber gets to
   * have an open transaction at one time.  We could support multiple
   * simultaneously open transactions with multiple subscribers, but
   * this is a feature for another day.
   */
  private LockFlag transactionLock = new LockFlag();
  
  /**
   * The current in-force transaction instance.
   * The is only kept around as a check to pass to Transaction.close()
   * in order to make sure we're closing the right one.
   * In particular, we cannot use this in the publishWhatever methods 
   * because the Blackboard methods are executing in the wrong thread.
   */
  private Transaction theTransaction = null;

  /**
   * Overridable by extending classes to specify more featureful
   * Transaction semantics.
   */
  protected Transaction newTransaction() {
    return new Transaction(this);
  }

  /**
   * Open a transaction by grabbing the transaction lock and updating
   * the subscriptions.  This method blocks waiting for the
   * transaction lock.
   */
  public final void openTransaction() {
    transactionLock.getBusyFlag();
    finishOpenTransaction();
  }

  private long openTime = setTransactionOpenTime();

  protected final boolean isTimestamped() {
    return enableTimestamps;
  }

  protected final long setTransactionOpenTime() {
    if (enableTimestamps) {
      return (openTime = System.currentTimeMillis());
    } else {
      return -1;
    }
  }

  /**
   * Common routine for both openTransaction and tryOpenTransaction
   * does everything except getting the transactionLock busy flag.
   */
  private void finishOpenTransaction() {
    int count = transactionLock.getBusyCount();
    if (count > 1) {
      if (isEnforcing) {
        logger.error("Opened nested transaction (level="+count+")", new Throwable());
      }
      return;
    }

    startTransaction();

    theDistributor.startTransaction();

    setTransactionOpenTime();
    if (privateUpdateSubscriptions()) {
      setHaveCollectionsChanged();
    }
    if (haveNewSubscriptions()) {
      setHaveCollectionsChanged();
      resetHaveNewSubscriptions();
    }
    noteOpenTransaction(this);
  }

  protected final void startTransaction() {
    theTransaction = newTransaction();
    Transaction.open(theTransaction);
  }

  private boolean _haveNewSubscriptions = false;
  private boolean haveNewSubscriptions() { return _haveNewSubscriptions; }
  private void setHaveNewSubscriptions() { _haveNewSubscriptions = true; }
  private void resetHaveNewSubscriptions() { _haveNewSubscriptions = false; }

  /**
   * Keep track of whether or not the collections have changed 
   * since the previous openTransaction.
   */
  private boolean _haveCollectionsChangedSinceLastTransaction = false;

  /** set haveCollectionsChanged() */
  private void setHaveCollectionsChanged() {
    _haveCollectionsChangedSinceLastTransaction = true;
  }

  /** set haveCollectionsChanged() */
  private void resetHaveCollectionsChanged() {
    _haveCollectionsChangedSinceLastTransaction = false;
  }

  /** can be called by anyone who can open a transaction to decide what to do.
   * returned value is only valid/useful inside an open transaction.
   */
  public boolean haveCollectionsChanged() {
    return _haveCollectionsChangedSinceLastTransaction;
  }

  /** Attempt to open a transaction by attempting to grab the 
   * transaction lock and updating the collections (iff we got the 
   * lock).
   *
   * This is equivalent to the old (misnamed) tryLockSubscriber method
   * in PluginWrapper.
   *
   * @return true IFF a transaction was opened.
   */
  public final boolean tryOpenTransaction() {
    if (transactionLock.tryGetBusyFlag()) {
      finishOpenTransaction();
      return true;
    }
    return false;
  }

  /**
   * Close a transaction opened by openTransaction() or a successful
   * tryOpenTransaction(), but don't reset subscription changes or
   * clear delta lists.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   */
  public final void closeTransactionDontReset() {
    closeTransaction(false);
  }

  /** check to see if we've already got an open transaction
   */
  public final boolean isTransactionOpen() {
    return (transactionLock.getBusyFlagOwner() == Thread.currentThread());
  }

  /** Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction().
   * @param resetSubscriptions IFF true, all subscriptions will have
   * their resetChanges() method called to clear any delta lists, etc.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   * @deprecated Use {@link #closeTransactionDontReset closeTransactionDontReset}
   * This method becomes private after deprecation period expires.
   */
  public final void closeTransaction(boolean resetSubscriptions)
    throws SubscriberException {
    if (transactionLock.getBusyFlagOwner() == Thread.currentThread()) {
      // only do our closeTransaction work when exiting the nest.
      if (transactionLock.getBusyCount() == 1) {
        checkUnpostedChangeReports();

        if (!isReadyToPersist()) {
          setReadyToPersist();
        }
        if (resetSubscriptions)
          resetSubscriptionChanges();
        Envelope box = privateGetPublishedChanges();
        try {
          theDistributor.finishTransaction(box, getClient());
        } finally {
          stopTransaction();
        }
      } else {
	// Nested transaction (more than 1 busy)?
	//System.err.println("Closed nested transaction.");
	if (logger.isDebugEnabled())
	  logger.debug("Closed nested transaction.");
      }        
      // If no subscriptions we will never process the inbox. Empty
      // it to conserve memory instead of waiting for
      // openTransaction
      synchronized (inboxLock) {
        if (getSubscriptionCount() == 0) {
          pendingEnvelopes.clear();
        }
        if (! transactionLock.freeBusyFlag()) {
          throw new SubscriberException("Failed to close an owned transaction");
        }
      }
    } else {
      throw new SubscriberException("Attempt to close a non-open transaction");
    }
    noteCloseTransaction(this);
  }

  protected final void stopTransaction() {
    Transaction.close(theTransaction);
    theTransaction = null;
  }

  protected final void checkUnpostedChangeReports() {
    //Map map = theTransaction.getChangeMap();
    Map map = Transaction.getCurrentTransaction().getChangeMap();
    if (warnUnpublishChanges) {
      if (map == null || map.size()==0) return;
      if (logger.isWarnEnabled())
	logger.warn("Ignoring outstanding unpublished changes:");
      for (Iterator ki = map.keySet().iterator(); ki.hasNext(); ) {
        Object o = ki.next();
        List l = (List)map.get(o);
	if (logger.isWarnEnabled())
	  logger.warn("\t"+o+" ("+l.size()+")");
        // we could just publish them with something like:
        //handleActiveSubscriptionObjects()
        //clientChangedObject(o, l);
      }
    }
  }

  /** Close a transaction opened by openTransaction() or a 
   * successful tryOpenTransaction().
   * Will reset all subscription change tracking facilities.
   * To avoid this, use closeTransactionDontReset() instead.
   * @exception SubscriberException IFF we did not own the transaction
   * lock.
   */
  public final void closeTransaction() {
    closeTransaction(true);
  }

  /** Does someone have an open transaction? */
  public final boolean isInTransaction() {
    return (transactionLock.getBusyFlagOwner() != null);
  }

  /** Do I have an open transaction?
   * This really translates to "Is is safe to make changes to my
   * collections?"
   */
  public final boolean isMyTransaction() {
    return (transactionLock.getBusyFlagOwner() == Thread.currentThread());
  }
  

  //
  // Interest Handling - extension of earlier wakeRequest and
  //   interestSemaphore code.
  //

  /** list of SubscriptionWatchers to be notified when something
   * interesting happens.  Access must be synchronized on watchers.
   */
  private final List watchers = new ArrayList(1);

  public final SubscriptionWatcher registerInterest(SubscriptionWatcher w) {
    if (w == null) {
      throw new IllegalArgumentException("Null SubscriptionWatcher");
    }

    synchronized (watchers) {
      watchers.add(w);
    }

    return w;
  }

  /** Allow a thread of a subscriber to register an interest in the
   * subscriber's collections.  Mainly used to allow threads to monitor
   * changes in collections - that is, the fact of change, not the details.
   * The level of support here is like the old wake and interestSemaphore
   * code.  The client of a subscriber need not register explicitly, as
   * it is done at initialization time.
   */
  public final SubscriptionWatcher registerInterest() {
    return registerInterest(new SubscriptionWatcher());
  }

  /** Allow a thread to unregister an interest registered by
   * registerInterest.  Should be done if a subordinate (watching)
   * thread exits, or a plugin unloads.
   */
  public final void unregisterInterest(SubscriptionWatcher w) throws SubscriberException {
    synchronized (watchers) {
      if (! watchers.remove(w) ) {
        throw new SubscriberException(
            "Attempt to unregisterInterest of unknown SubscriptionWatcher");
      }
    }
  }


  //
  // watcher triggers
  //

  private boolean _externalActivity = false;
  private boolean _internalActivity = false;
  private boolean _clientActivity = false;

  public boolean wasExternalActivity() { return _externalActivity; }
  public boolean wasInternalActivity() { return _internalActivity; }
  public boolean wasClientActivity() { return _clientActivity; }

  /** called when external activity changes the subscriber's collections.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   */
  public void signalExternalActivity() {
    _externalActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.EXTERNAL);
  }
  /** called when internal activity actually changes the subscriber's
   * collections. 
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   */
  public void signalInternalActivity() {
    _internalActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.INTERNAL);
  }
  /** called when the client (Plugin) requests that it be waked again.
   * by default, just calls wakeSubscriptionWatchers, but subclasses
   * may be more circumspect.
   */
  public void signalClientActivity() {
    _clientActivity = true;
    wakeSubscriptionWatchers(SubscriptionWatcher.CLIENT);
  }
  
    
  /** called to notify all SubscriptionWatchers.
   */
  private final void wakeSubscriptionWatchers(int event) {
    synchronized (watchers) {
      int l = watchers.size();
      for (int i=0; i<l; i++) {
        ((SubscriptionWatcher) (watchers.get(i))).signalNotify(event);
      }
    }
  }

  //
  // usability and debugability methods
  // 

  @Override
public String toString() {
    String cs = "(self)";
    if (theClient != this)
      cs = theClient.toString();

    return "<"+getClass().getName()+" "+this.hashCode()+" for "+cs+" and "+
      theDistributor+">";
  }

  /** utility to claim an object as ours */
  protected void claimObject(Object o) {
    if (o instanceof ClaimableHolder) {
      Claimable c = ((ClaimableHolder) o).getClaimable();
      if (c != null) {
        //System.err.println("\n->"+getClient()+" claimed "+c);
        c.setClaim(getClient());
      }
    }
  }

  /** utility to release a claim on an object */
  protected void unclaimObject(Object o) {
    if (o instanceof ClaimableHolder) {
      Claimable c = ((ClaimableHolder) o).getClaimable();
      if (c != null) {
        //System.err.println("\n->"+getClient()+" unclaimed "+c);
        c.resetClaim(getClient());
      }
    }
  }
  
  /** return the client of the the subscriber.
   * May be overridden by subclasses in case they are really
   * delegating to some other object.
   */
  public BlackboardClient getClient() {
    return theClient;
  }

  //Leftover from ancient code - now in BlackboardService... may want to 
  //deprecate next release?
  public Subscriber getSubscriber() {
    return this;
  }

  // try and save the state so that we can abort open transactions if
  // someone is bad.
  private static final ThreadLocal _openTransaction = new ThreadLocal();

  private static void noteOpenTransaction(Subscriber s) {
    _openTransaction.set(s);
  }
  private static void noteCloseTransaction(Subscriber s) {
    if (s != _openTransaction.get()) {
      Logging.getLogger(Subscriber.class).error(
          "Attempt to close a transaction from a different thread"+
         " than the one which opened it:\n\t"+s+
         "\t"+_openTransaction.get(),
         new Throwable());
    }
    _openTransaction.set(null);
  }

  public static boolean abortTransaction() {
    Subscriber s = (Subscriber)_openTransaction.get();
    if (s!=null) {
      s.closeTransaction();
      return true;
    } else {
      return false;
    }
  }

}
