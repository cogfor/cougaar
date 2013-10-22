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

package org.cougaar.core.blackboard;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeBusyService;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceObject;
import org.cougaar.core.persist.PersistenceSubscriberState;
import org.cougaar.core.persist.RehydrationResult;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.QuiescenceReportForDistributorService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * The Distributor coordinates blackboard transactions, subscriber
 * updates, and persistence.
 *
 * @property org.cougaar.core.agent.keepPublishHistory
 *   if set to <em>true</em>, enables tracking of
 *   all publishes.  Extremely expensive.
 * @property org.cougaar.core.agent.singleTransactionModel
 *   Enables a blackboard/agent run model where only one
 *   transaction may be open at a given time.
 */
final class Distributor {

  /*
   * Design summary:
   *
   * The distributor uses two locks:
   *   distributorLock
   *   transactionLock
   * The distributorLock guards against "distribute()" and
   * blackboard modification.  The transactionLock guards against
   * start/finish transaction and persistence.  Only one distribute
   * can occur at a time, and it can't occur at the same time as a
   * persist.  Only one persist can occur at a time, and there must
   * be no active transactions (to guard against object
   * modifications during serialization), but we can be suspended.
   * Transactions are allowed when we're not persisting or
   * suspended.  It's okay for transactions to start/finish while
   * a distribute is taking place, to provide parallelism.
   *
   * Persistence runs in either a lazy or non-lazy mode, where the
   * best-supported mode is lazy.  Lazy persistence occurs
   * periodically (33 seconds) if the blackboard has been modified
   * during that time, and only if the persistence implementation
   * indicates that it is "getPersistenceTime()".
   *
   * Old notes, partially accurate:
   *
   * Synchronization methodology. The distributor distributes three
   * kinds of things: envelopes, messages, and timers. All three are
   * synchronized on the distributor object (this) so only one of
   * these can be in progress at a time. The distributor must have
   * unfettered access to the subscribers meaning that the subscribers
   * cannot themselves by locked while awaiting access to the
   * distributor. Each distribution may generate a persistence
   * delta. Persistence deltas are not generated unless there are no
   * open transactions. Normally, subscribers are allowed to open
   * transactions except if sufficient time has elapsed since the
   * previous persistence delta requiring that a persistence delta
   * must be generated. A persistence delta must also be generated if
   * there are no open transactions and nothing has been distributed
   * to any subscriber.
   * @property org.cougaar.core.blackboard.persistenceReservationTimeout
   * specifies the maximum delay allowed before using a persistence
   * reservation. An agent exceeding this delay will have to request a
   * new reservation. Default value is 120000 milliseconds. A value of
   * zero disables the reservation mechanism.
   */

  /** The maximum interval between persistence deltas. */
  private static final long MAX_PERSIST_INTERVAL = 37000L;
  private static final long TIMER_PERSIST_INTERVAL = 33000L;

  /** The maximum delay allowed before using a persistence reservation */
  private static final String PERSISTENCE_RESERVATION_TIMEOUT_PROP =
    "org.cougaar.core.blackboard.persistenceReservationTimeout";
  private static final long PERSISTENCE_RESERVATION_TIMEOUT =
    SystemProperties.getLong(PERSISTENCE_RESERVATION_TIMEOUT_PROP, 120000L);

  private static final String PERSIST_AT_STARTUP_PROP =
    "org.cougaar.core.blackboard.persistAtStartup";
  private static final boolean PERSIST_AT_STARTUP =
    SystemProperties.getBoolean(PERSIST_AT_STARTUP_PROP);

  private static final UnaryPredicate anythingP = 
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return (o != null);
      }
    };

  private static final String SINGLE_TRANSACTION_PROP = 
    "org.cougaar.core.agent.singleTransactionModel";
  /** The default setting for single transaction model */
  public static final boolean DEFAULT_SINGLE_TRANSACTION = false;
  private static final boolean SINGLE_TRANSACTION = 
    SystemProperties.getBoolean(SINGLE_TRANSACTION_PROP, DEFAULT_SINGLE_TRANSACTION);

  //
  // these are set in the constructor and are final:
  //

  /** The publish history is available for subscriber use. */
  public final PublishHistory history =
    (SystemProperties.getBoolean("org.cougaar.core.agent.keepPublishHistory") ?
     new PublishHistory() :
     null);

  /** the name of this distributor */
  private final String name;

  /** NodeBusyService so we can indicate when we are busy persisting */
  private NodeBusyService nodeBusyService;

  // blackboard, noted below.

  /** the logger, which is thread safe */
  private final Logger logger;

  //
  // these are set immediately following the constructor, and
  // are effectively final:
  //

  /** True if using lazy persistence */
  private boolean lazyPersistence = true;

  /** The object we use to persist ourselves. */
  private Persistence persistence = null;

  /** If persistence is non-null, is it a dummy? */
  private boolean dummyPersistence;

  /** The reservation manager for persistence */
  private static final ReservationManager persistenceReservationManager =
      new ReservationManager(PERSISTENCE_RESERVATION_TIMEOUT);

  //
  // lock for open/close transaction and persistence:
  //

  private final Object transactionLock = new Object();

  /**
   * when singleTransactionModel is enabled, the transactionMutex is
   * locked for the duration of the transaction.  
   */
  private final Object transactionMutexLock = new Object();
  private boolean transactionMutexInUse = false;

  /**
   * Acquire the transaction mutex.  No-op if not running in SINGLE_TRANSACTION mode.
   */
  protected final void acquireTransactionMutex() {
    if (SINGLE_TRANSACTION) {
      synchronized (transactionMutexLock) {
        try {
          while (transactionMutexInUse) { transactionMutexLock.wait(); }
          transactionMutexInUse = true;
        } catch (InterruptedException ie) {
          transactionMutexLock.notify();
          logger.error("Interrupted while acquiring transactionMutex", ie);
        }
      }
    }
  }

  /**
   * Release the transaction mutex. No-op if not running in SINGLE_TRANSACTION mode.
   */
  protected final void releaseTransactionMutex() {
    if (SINGLE_TRANSACTION) {
      synchronized (transactionMutexLock) {
        transactionMutexInUse = false;
        transactionMutexLock.notify(); 
      }
    }
  }

  // the following are locked under the transactionLock:

  private static final int PERSIST_ACTIVE  = (1<<0);
  private static final int PERSIST_PENDING = (1<<1);
  private static final int SUSPENDED       = (1<<2);
  private int persistFlags = 0;
  private int transactionCount = 0;

  // temporary list for use within "doPersist":
  private final List subscriberStates = new ArrayList();

  //
  // lock for distribute and blackboard access:
  //

  private final Object distributorLock = new Object();

  // the following are locked under the distributorLock:

  /** our blackboard */
  private final Blackboard blackboard;

  /** True if rehydration occurred at startup */
  private boolean didRehydrate = false;

  /** Tuples that have been distributed during a persistence ecoch */
  private Map epochTuples;

  /** The message manager for this agent */
  private MessageManager myMessageManager = null;

  /** Periodic persistence timer */
  private Schedulable distributorTimer = null;

  private final Subscribers subscribers = new Subscribers();

  /** The time that we last persisted */
  private long lastPersist = System.currentTimeMillis();

  /**
   * Do we need to persist sometime; changed state has not
   * been persisted
   */
  private boolean needToPersist = false;

  // temporary lists, for use within "distribute()":
  private final List outboxes = new ArrayList();
  private final List messagesToSend = new ArrayList();

  // temporary list, for use within "receiveMessages()":
  private final List directiveMessages = new ArrayList();

  //
  // periodic (lazy) persistence timer
  //

  private final Object timerLock = new Object();

  // the following are locked under the timerLock:

  private boolean timerActive;

  //
  // These are partially locked, and may cause bugs in
  // the future.  In practice they seem to be fine:
  //

  /** Envelopes distributed since the last rehydrated delta */
  private List postRehydrationEnvelopes = null;

  /** All objects published prior to the last rehydrated delta */
  private PersistenceEnvelope rehydrationEnvelope = null;

  private QuiescenceMonitor quiescenceMonitor;
  private boolean quiescenceReportEnabled = false; // Not yet enabled

  private ServiceBroker sb;
  private QuiescenceReportForDistributorService quiescenceReportService;

  // Subscribers who should rehydrate before enabling the QuiescenceService
  // that is, the set of subscribers remaining to rehydrate for
  // which quiescence is required
  private Set subscribersToRehydrate = Collections.EMPTY_SET;

  /** Isolated constructor */
  public Distributor(Blackboard blackboard, ServiceBroker sb, String name) {
    this.blackboard = blackboard;
    this.name = (name != null ? name : "Anonymous");
    Logger l = Logging.getLogger(getClass());
    this.logger = new LoggingServiceWithPrefix(l, this.name + ": ");
    if (logger.isInfoEnabled()) {
      logger.info("Distributor started");
    }

    if (logger.isDebugEnabled()) {
      logger.debug("transaction options: "+
                   "deferCommit="+ ActiveSubscriptionObject.deferCommit+
                   ", singleTransaction="+ SINGLE_TRANSACTION);
    }


    this.sb = sb;
    nodeBusyService = sb.getService(this, NodeBusyService.class, null);
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    nodeBusyService.setAgentIdentificationService(ais);
    quiescenceReportService = sb.getService(this, QuiescenceReportForDistributorService.class, null);
    quiescenceReportService.setAgentIdentificationService(ais);
    quiescenceMonitor = new QuiescenceMonitor(quiescenceReportService, logger);
  }

  /**
   * Called by the blackboard immediately after the constructor,
   * and only once.
   */
  void setPersistence(Persistence newPersistence, boolean lazy) {
    assert persistence == null : "persistence already set";
    persistence = newPersistence;
    lazyPersistence = lazy;
    dummyPersistence = 
      (persistence != null && persistence.isDummyPersistence());
    initializeEpochEnvelopes();
  }

  /**
   * Called by Subscriber to link into Blackboard persistence
   * mechanism
   */
  Persistence getPersistence() {
    return persistence;
  }

  /**
   * Called by subscriber to discard rehydration info.
   */
  void discardRehydrationInfo(Subscriber subscriber) {
    synchronized (distributorLock) {
      if (rehydrationEnvelope != null) {
	// Only need to check subscriber count to enable quiescence
	if (! quiescenceReportEnabled) {
	  // Remove this subscriber from those we want to rehydrate before
	  // registering with the quiescence service.
	  // Later, in distribute, we'll see if this was in fact the last
	  // relevant subscriber to be rehydrated
	  PersistenceSubscriberState pss = persistence.getSubscriberState(subscriber);
	  if (pss != null) {
	    String key = pss.getKey();
	    boolean didRemove = subscribersToRehydrate.remove(key);
	    if (didRemove && logger.isDebugEnabled())
	      logger.debug(".discard: Rehydrated q-relevant subscriber " + key);
	  }
	}

	// Now dump its subscriber state
	// (needs to be after above so we can get the key...)
	persistence.discardSubscriberState(subscriber);

	// If there are no more persistence subscriber states left, dump
	// the rehydration information -- we're done with it
	if (! persistence.hasSubscriberStates()) {
	  if (logger.isDebugEnabled())
	    logger.debug(".discard: No sub states left at all. discarding rehydration info");
	  // discard rehydration info:
	  rehydrationEnvelope = null;
	  postRehydrationEnvelopes = null;
	}
      }
    }
  }

  public boolean didRehydrate(Subscriber subscriber) {
    if (!didRehydrate) return false;
    return (persistence.getSubscriberState(subscriber) != null);
  }

  /**
   * Pass thru to blackboard to safely return blackboard object
   * counts.
   * Used by BlackboardMetricsService
   * @param cl The class type
   */
  public int getBlackboardCount(Class cl) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      return blackboard.countBlackboard(cl);
    }
  }

  /**
   * Pass thru to blackboard to safely return blackboard object
   * counts.
   * Used by BlackboardMetricsService
   * @param predicate The objects to count in the blackboard
   * @return int The count of objects that match the predicate
   *   currently in the blackboard
   */
  public int getBlackboardCount(UnaryPredicate predicate) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      return blackboard.countBlackboard(predicate);
    }
  }

  /**
   * Pass thru to blackboard to safely return the size of the
   * blackboard collection.
   */
  public int getBlackboardSize() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      return blackboard.getBlackboardSize();
    }
  }

  /**
   * Rehydrate this blackboard. If persistence is off, just create a
   * MessageManager that does nothing. If persistence is on, try to
   * rehydrate from existing persistence deltas. The result of this is
   * a List of undistributed envelopes and a MessageManager. There
   * might be no MessageManager in the result signifying that either
   * there were no persistence deltas or that lazyPersistence was on
   * so the message manager did not need to be saved. In either case
   * the existence of an appropriate message manager is assured. The
   * undistributed envelopes might be null signifying that there was
   * no persistence deltas in existence. This is reflected in the
   * value of the didRehydrate flag.
   */
  private void rehydrate(Object state) {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (persistence == null) {
      myMessageManager = new MessageManagerImpl(false);
      return;
    }

    // Get the rehydration data.  Note that the passed "state" is
    // always null, so we can't use it to check for dummyPersistence.
    rehydrationEnvelope = new PersistenceEnvelope();
    RehydrationResult rr =
      persistence.rehydrate(rehydrationEnvelope, state);
    if (rr.quiescenceMonitorState != null) {
      quiescenceMonitor.setState(rr.quiescenceMonitorState);
    }

    // Distributor tracks the subscribers who have yet to rehydrate
    // However, we only really care about those for whom we require
    // quiescence
    subscribersToRehydrate = persistence.getSubscriberStateKeys();
    if (logger.isDebugEnabled())
      logger.debug("Initial number of subscribers: " + subscribersToRehydrate.size());
    // Synchronize so that a subscriber who already 
    // is in discardRehydrationInfo doesnt cause a ConcurrentModExc
    synchronized (distributorLock) {
      Iterator iter = subscribersToRehydrate.iterator();
      List toRemove = new ArrayList();
      while (iter.hasNext()) {
        String key = (String)iter.next();
        if (! quiescenceMonitor.isQuiescenceRequired(key)) {
          if (logger.isDebugEnabled())
            logger.debug("Ignoring subscriber " + key);
          toRemove.add(key);
        } else {
          if (logger.isDebugEnabled())
            logger.debug("NOT Ignoring subscriber " + key);
        }
      }
      subscribersToRehydrate.removeAll(toRemove);
      if (logger.isDebugEnabled())
        logger.debug("Trimmed number of subscribers: " + subscribersToRehydrate.size());
    } // end synchronized(distributorLock)

    if (lazyPersistence) {    // Ignore any rehydrated message manager
      myMessageManager = new MessageManagerImpl(false);
    } else {
      myMessageManager = rr.messageManager;
      if (myMessageManager == null) {
        myMessageManager = new MessageManagerImpl(true);
      }
    }
    if (rr.undistributedEnvelopes != null) {
      didRehydrate = true;
      postRehydrationEnvelopes = new ArrayList();
      postRehydrationEnvelopes.addAll(rr.undistributedEnvelopes);
      addEpochEnvelopes(rr.undistributedEnvelopes);
    }
  }

  private MessageManager getMessageManager() {
    return myMessageManager;
  }

  /**
   * Rehydrate a new subscription. New subscriptions that correspond
   * to persisted subscriptions are quietly fed the
   * rehydrationEnvelope which has all the objects that had been
   * distributed prior to the last rehydrated delta. Objects in the
   * rehydrationEnvelope do not waken the subscriber; they are simply
   * added to the subscription container. Next, the envelopes that
   * were pending in the inbox of the subscriber at the last delta are
   * fed to the subscription. The subscriber _will_ be notified of
   * these. Finally, all envelopes in postRehydrationEnvelopes are fed
   * to the subscription. The subscriber will be notified of these as
   * well.
   */
  private void rehydrateNewSubscription(
      Subscription s,
      List persistedTransactionEnvelopes,
      List persistedPendingEnvelopes) {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    s.fill(rehydrationEnvelope);
    if (persistedTransactionEnvelopes != null) {
      for (Iterator iter = persistedTransactionEnvelopes.iterator();
          iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    if (persistedPendingEnvelopes != null) {
      for (Iterator iter = persistedPendingEnvelopes.iterator();
          iter.hasNext(); ) {
        s.fill((Envelope) iter.next());
      }
    }
    for (Iterator iter = postRehydrationEnvelopes.iterator();
        iter.hasNext(); ) {
      s.fill((Envelope) iter.next());
    }
  }

  /**
   * Start the distribution thread.
   * Note that although Distributor is Runnable, it does not
   * extend Thread, rather, it maintains its own thread state
   * privately.
   */
  public void start(
      MessageSwitchService msgSwitch, Object state) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      rehydrate(state);
      getMessageManager().start(msgSwitch, didRehydrate);
      needToPersist = true;
    }
    // persist now, to claim the persistence ownership
    // and save our component hierarchy
    if (PERSIST_AT_STARTUP) {
      persist(false, false);
    }
    createTimer();
    enableTimer();
  }

  private void createTimer() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // disable the timer if it already exists
    disableTimer();
    // start a disabled periodic timer
    synchronized (distributorLock) {
      if (lazyPersistence && 
          !dummyPersistence &&
          distributorTimer == null) {
        Runnable task =
          new Runnable() {
            public void run() {
              synchronized (timerLock) {
                if (timerActive) {
                  timerPersist();
                }
              }
            }
          };
        ThreadService tsvc = sb.getService(this, ThreadService.class, null);
        distributorTimer = tsvc.getThread(this, task, "Persistence Timer",
            ThreadService.WILL_BLOCK_LANE);
        sb.releaseService(this, ThreadService.class, tsvc);
        distributorTimer.schedule(TIMER_PERSIST_INTERVAL,
            TIMER_PERSIST_INTERVAL);
      }
    }
  }

  private void enableTimer() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (timerLock) {
      timerActive = true;
    }
  }

  private void disableTimer() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // if the timer is running then this will block
    synchronized (timerLock) {
      timerActive = false;
    }
  }

  private void stopTimer() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // disable the timer if it's running
    disableTimer();
    // stop the timer thread.  This isn't necessary unless we're
    // really "stop()"ing the distributor, but it's useful to
    // hide this thread when the timer is disabled.
    synchronized (distributorLock) {
      if (lazyPersistence) {
        if (distributorTimer != null) {
          distributorTimer.cancelTimer();
          distributorTimer = null;
        }
      }
    }
  }

  /**
   * Complete any active persists, lockout transactions and
   * timer-based persists, and only allow external state
   * captures via "persistNow()" and "getPersistenceObject()".
   */
  public void suspend() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (logger.isInfoEnabled()) {
      logger.info("Suspending");
    }
    // stop our timer
    disableTimer();
    // pretend that we're getting ready to persist to lockout
    // all transactions.  Then set the SUSPENDED flag, which
    // acts as another block for "startTransaction".
    // Then we release our active persistence lock to allow
    // externally-signaled persistence while still preventing
    // transaction-based persistence.
    synchronized (transactionLock) {
      // wait for persistence to complete
      lockoutPersistence();
      // block transactions
      lockoutTransactions();
      // keep the transactions blocked
      setSuspended(true);
      // allow persistence
      resumePersistence();
      resumeTransactions();
    }
    if (logger.isInfoEnabled()) {
      logger.info("Suspended");
    }
  }

  public void resume() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    // see suspend for comments
    if (logger.isInfoEnabled()) {
      logger.info("Resuming");
    }
    synchronized (transactionLock) {
      setSuspended(false);
    }
    enableTimer();
    if (logger.isInfoEnabled()) {
      logger.info("Resumed");
    }
  }

  /**
   * Stop the distribution thread.
   * @see #start
   */
  public void stop() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    quiescenceReportService.setQuiescenceReportEnabled(false); // Finished here
    sb.releaseService(this, QuiescenceReportForDistributorService.class, quiescenceReportService);
    stopTimer();
    synchronized (distributorLock) {
      getMessageManager().stop();
    }
  }

  //
  // Subscriber Services
  //

  /**
   * Register a Subscriber with the Distributor.  Future envelopes are
   * distributed to all registered subscribers.
   */
  public void registerSubscriber(Subscriber subscriber) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      subscribers.register(subscriber);
    }
  }

  /**
   * Unregister subscriber with the Distributor. Future envelopes are
   * not distributed to unregistered subscribers.
   */
  public void unregisterSubscriber(Subscriber subscriber) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      subscribers.unregister(subscriber);
    }
  }

  /**
   * Provide a new subscription with its initial fill. If the
   * subscriber of the subscription was persisted, we fill from the
   * persisted information (see rehydrateNewSubscription) otherwise
   * we fill from the Blackboard (blackboard.fillSubscription).
   */
  public void fillSubscription(Subscription subscription) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      Subscriber subscriber = subscription.getSubscriber();
      PersistenceSubscriberState subscriberState = null;
      if (didRehydrate) {
        if (subscriber.isReadyToPersist()) {
          if (logger.isInfoEnabled()) {
            logger.info(
                "No subscriber state for late subscribe of " +
                subscriber.getName());
          }
        } else {
          subscriberState =
            persistence.getSubscriberState(subscriber);
        }
      }
      if (subscriberState != null &&
          subscriberState.pendingEnvelopes != null) {
        rehydrateNewSubscription(subscription,
            subscriberState.transactionEnvelopes,
            subscriberState.pendingEnvelopes);
      } else {
        blackboard.fillSubscription(subscription);
      }

      // distribute the initialize envelope -- used for subs
      // to know when their first transaction is done
      /*
         {
      // option 1
      distribute(new InitializeSubscriptionEnvelope(subscription), null);
      }
       */
      // blackboard subscribes don't need an ISE to fill
      if (subscriber != blackboard) {
        // option 2
        Subscriber s = subscription.getSubscriber();
        List l = new ArrayList(1);
        l.add(new InitializeSubscriptionEnvelope(subscription));
        s.receiveEnvelopes(l, true);    // queue in the right spot.
                                        // Assume at least one of the
                                        // sources of the objects that
                                        // filled the subscription
                                        // requires quiescence.
      }
    }
  }

  public void fillQuery(Subscription subscription) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (distributorLock) {
      blackboard.fillQuery(subscription);
    }
  }

  /**
   * The main workhorse of the distributor. Distributes the contents
   * of an outbox envelope to everybody.
   *
   * If needToPersist is true and it is time to persist, we set the
   * PERSIST_PENDING flag to prevent any further openTransactions from
   * happening. Then we distribute the outbox and consequent
   * envelopes. If anything is distributed, we set the needToPersist
   * flag. Any messages generated by the Blackboard are gathered and
   * given to the message manager for eventual transmission. Finally,
   * the generation of a persistence delta is considered.
   * @return true if a persistance snapshot should be taken
   */
  private boolean distribute(Envelope outbox, BlackboardClient client) {
    return distribute(outbox, client, quiescenceMonitor.isQuiescenceRequired(client));
  }

  private boolean distribute(
      Envelope outbox, BlackboardClient client, boolean clientQuiescenceRequired) {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    boolean result = false;
    if (outbox != null &&
        logger.isDetailEnabled() &&
        client != null) {
      logEnvelope(outbox, client);
    }
    if (persistence != null) {
      if (needToPersist) {
        if (timeToPersist()) {
          result = true;
        }
      }
    }
    blackboard.prepareForEnvelopes();
    boolean haveSomethingToDistribute = false;
    // nest loops in case delayed actions cascade into more
    // lp actions.
    while (outbox != null && outbox.size() > 0) {
      while (outbox != null && outbox.size() > 0) {
        outboxes.add(outbox);
        outbox = blackboard.receiveEnvelope(outbox);
        haveSomethingToDistribute = true;
      }

      // outbox should be empty at this point.
      // execute any pending DelayedLPActions
      outbox = blackboard.executeDelayedLPActions();
    }
    Blackboard.getTracker().clearLocalSet();

    //      while (outbox != null && outbox.size() > 0) {
    //        outboxes.add(outbox);
    //        outbox = blackboard.receiveEnvelope(outbox);
    //        haveSomethingToDistribute = true;
    //      }

    /**
     * busy indicates that we have found evidence that things are
     * still happening or are about to happen in this agent.
     */
    boolean busy = haveSomethingToDistribute;
    boolean newSubscribersAreQuiescent = true; // Until proven otherwise
    if (persistence != null) {
      if (!needToPersist && haveSomethingToDistribute) {
        needToPersist = true;
      }
    }
    if (logger.isDebugEnabled()) {
      if (haveSomethingToDistribute) {
        logger.debug("quiescence"
                     + (clientQuiescenceRequired ? "" : " not")
                     + " required for outbox of "
                     + client.getBlackboardClientName());
      }
    }
    for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
      Subscriber subscriber = (Subscriber) iter.next();
      if (subscriber == blackboard) continue;
      boolean subscriberBusy = false;
      if (haveSomethingToDistribute) {
        subscriber.receiveEnvelopes(outboxes, clientQuiescenceRequired);
        subscriberBusy = true;
      } else if (subscriber.isBusy()) {
        subscriberBusy = true;
      }
      if (subscriberBusy) {
        busy = true;
      }

      if (quiescenceReportEnabled) {
	// Check if at least one subscriber we care about is
	// now non-quiescent
	BlackboardClient inboxClient = subscriber.getClient();
	if (quiescenceMonitor.isQuiescenceRequired(inboxClient)) {
	  if (newSubscribersAreQuiescent && !subscriber.isQuiescent()) {
	    if (logger.isDebugEnabled()) {
	      logger.debug(
                  "There is at least one q-relevant subscriber,"+
                 " so this distribute prevents quiescence.");
	    }
	    if (logger.isDetailEnabled())
	      logger.detail("       First such subscriber: " + inboxClient.getBlackboardClientName());
	    newSubscribersAreQuiescent = false;
	  }
	}
      }
    }
    // Fill messagesToSend
    blackboard.appendMessagesToSend(messagesToSend);
    if (messagesToSend.size() > 0) {
      for (Iterator i = messagesToSend.iterator(); i.hasNext(); ) {
        DirectiveMessage msg = (DirectiveMessage) i.next();
	// If the publisher is q-relevant, then we number the message
	// to require a succesful receipt for quiescence
        if (clientQuiescenceRequired) quiescenceMonitor.numberOutgoingMessage(msg);
        if (logger.isDetailEnabled()) {
          Directive[] dirs = msg.getDirectives();
          for (int j = 0; j < dirs.length; j++) {
            logger.detail("SEND   " + dirs[j]);
          }
        }
      }
      getMessageManager().sendMessages(messagesToSend.iterator());
    }
    messagesToSend.clear();
    if (persistence != null) {
      if (postRehydrationEnvelopes != null) {
        postRehydrationEnvelopes.addAll(outboxes);
      }
      addEpochEnvelopes(outboxes);
      if (!needToPersist && getMessageManager().needAdvanceEpoch()) {
        needToPersist = true;
      }
      if (!busy && transactionCount > 1) {
        // This is not the last transaction, still busy
        busy = true;
      }
      if (needToPersist) {
        if (!busy) {
          result = true;
        }
      }
    }
    outboxes.clear();

    // Update the cumulative quiescence of all the subscriber inboxes
    // based on this distribute: Non-q if a Q-relevant comp published something
    // to this agent, and we have at least one q-relevant subscriber
    if (quiescenceReportEnabled)
      quiescenceMonitor.setSubscribersAreQuiescent(newSubscribersAreQuiescent);

    // If we have not yet enabled the QRS, check to see if this 
    // call to distribute means the last relevant subscriber
    // has rehydrated. If so, enable the QuiescenceReportService.
    // Note that discardRehydrationInfo is where we remove the subscribers
    // from the list we want to rehydrate first.

    // If this distribute is happening at the close of the first transaction
    // of the last q-relevant subscriber, then we enable the QRS -- but _after_
    // we've updated the Quiescence state based on the results of
    // this distribute -- otherwise, the close of the first transaction
    // of the last q-relevant subscriber would always make you non-q on rehydrate
    if (!quiescenceReportEnabled) {
      int nLeft = subscribersToRehydrate.size();
      if (nLeft == 0) {
	if (logger.isDebugEnabled())
	  logger.debug(".distribute: No more q-relevant subscribers to rehydrate. Enabling QRS.");
        quiescenceReportService.setQuiescenceReportEnabled(true); // All ready to go
        quiescenceReportEnabled = true;
      } else if (logger.isDebugEnabled()) {
	if (nLeft == 1) 
	  logger.debug(
              ".distribute: sole remaining q-relevant subscriber"+
              " left to rehydrate: " + subscribersToRehydrate);
	else
	  logger.debug(".distribute: " + nLeft + " q-relevant subscribers left to rehydrate");
      }
    } // end check to enable QRS

    return result;
  } // end of distribute()

  private void initializeEpochEnvelopes() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (dummyPersistence) {
      return;
    }
    //epochEnvelopes = new ArrayList();
    // this could use a regular equals-based map, but for persistence
    // consistency we use an =='s based identity map.
    epochTuples = new IdentityHashMap();
  }

  private void addEpochEnvelopes(List envelopes) {
    assert Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (dummyPersistence) {
      return;
    }
    //epochEnvelopes.addAll(rr.undistributedEnvelopes);
    for (int i = 0, n = envelopes.size(); i < n; i++) {
      Envelope e = (Envelope) envelopes.get(i);
      List tuples = e.getRawDeltas();
      for (int j = 0, m = tuples.size(); j < m; j++) {
        EnvelopeTuple tuple = (EnvelopeTuple) tuples.get(j);
        addEpochTuple(tuple);
      }
    }
  }

  private void addEpochTuple(EnvelopeTuple tuple) {
    assert Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    assert dummyPersistence;
    Object o = tuple.getObject();
    if (tuple.isBulk()) {
      Collection c = (Collection) o;
      for (Iterator iter = c.iterator(); iter.hasNext(); ) {
        Object o2 = iter.next();
        EnvelopeTuple oldTuple = (EnvelopeTuple)
          epochTuples.get(o2);
        if (oldTuple == null) {
          // N + A => A (common)
          epochTuples.put(o2, new AddEnvelopeTuple(o2));
        } else {
	  // Other theoretical cases - not handled
          // A + A => A (error?)
          // C + A => C (error?)
          // R + A => R (error?)
        }
      }
    } else {
      EnvelopeTuple oldTuple = (EnvelopeTuple)
        epochTuples.get(o);
      if (oldTuple == null) {
        // N + A => A  (common, ~25%)
        // N + C => C  (rare)
        // N + R => R  (rare)
        epochTuples.put(o, tuple); 
      } else if (oldTuple.isAdd()) {
        if (tuple.isRemove()) {
          // A + R => N  (common, ~10%)
          epochTuples.remove(o); 
	} else {
	  // Other theoretical cases, not handled
	  // A + A => A  (error?)
	  // A + C => A  (common, ~60%)
        }
      } else if (oldTuple.isChange()) {
        if (tuple.isAdd()) {
          // C + A => C  (error?)
        } else {
          // C + C => C  (common, ~2%)
          // C + R => R  (rare)
          epochTuples.put(o, tuple); 
        }
      } else {
	// Other theoretical cases, not handled
	// R + A => R  (error?)
	// R + C => R  (error?  probably a race)
	// R + R => R  (error?)
      }
    }
  }

  private List getEpochEnvelopes() {
    assert Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (dummyPersistence) {
      // we haven't been collecting tuples, since persistence is
      // disabled, but we must capture the full blackboard for a
      // forced persist (e.g. mobility).
      QuerySubscription everything = new QuerySubscription(anythingP);
      blackboard.fillQuery(everything);
      Envelope envelope = new Envelope();
      envelope.bulkAddObject(everything.getCollection());
      List ret = new ArrayList(1);
      ret.add(envelope);
      return ret;
    }
    //return epochEnvelopes;
    // create a list with a single envelope that contains all our
    // epochTuples, where the list's "clear()" clears our tuple
    // map for early GC.
    final Envelope e = new Envelope();
    e.getRawDeltas().addAll(epochTuples.values());
    List ret = 
      new AbstractList() {
        private Envelope envelope = e;
        @Override
      public int size() {
          return (envelope == null ? 0 : 1);
        }
        @Override
      public Object get(int index) {
          if (index != 0 || envelope == null) {
            throw new IndexOutOfBoundsException(
                "Index: "+index+", Size: "+size());
          }
          return envelope;
        }
        @Override
      public void clear() {
          if (envelope != null) {
            envelope = null;
            epochTuples.clear();
          }
        }
      };
    return ret;
  }

  private void clearEpochEnvelopes() {
    assert Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (dummyPersistence) {
      return;
    }
    //epochEnvelopes.clear();
    epochTuples.clear();
  }

  public void restartAgent(MessageAddress cid) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    boolean persistWanted = false;
    try {
      startTransaction();
      synchronized (distributorLock) {
        try {
          blackboard.startTransaction();
          blackboard.restart(cid);
          Envelope envelope =
            blackboard.receiveMessages(Collections.EMPTY_LIST);
          persistWanted = distribute(envelope, blackboard.getClient(), true);
        } finally {
          blackboard.stopTransaction();
        }
      }
      if (persistWanted) maybeSetPersistPending();
    } finally {
      finishTransaction();
    }
  }

  /**
   * Process directive and ack messages from other agents. Acks
   * are given to the message manager. Directive messages are passed
   * through the message manager for validation and then given to
   * the Blackboard for processing. Envelopes resulting from that
   * processing are distributed.
   */
  public void receiveMessages(List messages) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    try {
      // Do one of the incoming messages come from a q-relevant component?
      boolean messagesRequireQuiescence = false;
      boolean persistWanted = false;
      startTransaction(); // Blocks if persistence active

      synchronized (distributorLock) {
        for (Iterator msgs = messages.iterator(); msgs.hasNext(); ) {
          Object m = msgs.next();
          if (m instanceof DirectiveMessage) {
            DirectiveMessage msg = (DirectiveMessage) m;
            int code = getMessageManager().receiveMessage(msg);
            if ((code & MessageManager.RESTART) != 0) {
              try {
                blackboard.startTransaction();
                blackboard.restart(msg.getSource());
              } finally {
                blackboard.stopTransaction();
              }
            }
            if ((code & MessageManager.IGNORE) == 0) {
              if (logger.isDetailEnabled()) {
                Directive[] dirs = msg.getDirectives();
                for (int i = 0; i < dirs.length; i++) {
                  logger.detail("RECV   " + dirs[i]);
                }
              }
              directiveMessages.add(msg);
              messagesRequireQuiescence |=
                quiescenceMonitor.numberIncomingMessage(msg);
            }
          } else if (m instanceof AckDirectiveMessage) {
            AckDirectiveMessage msg = (AckDirectiveMessage) m;
            int code = getMessageManager().receiveAck(msg);
            if ((code & MessageManager.RESTART) != 0) {
              // Remote agent has restarted
              blackboard.restart(msg.getSource());
            }
          }
        }
        // We nominally ack the messages here so the persisted
        // state will include the acks. The acks are not actually
        // sent until the persistence delta is concluded.
        getMessageManager().acknowledgeMessages(
            directiveMessages.iterator());

        try {
          blackboard.startTransaction();
          Envelope envelope = blackboard.receiveMessages(
              directiveMessages);
          persistWanted = distribute(envelope, blackboard.getClient(), messagesRequireQuiescence);
        } finally {
          blackboard.stopTransaction();
        }
        directiveMessages.clear();
      }
      if (persistWanted) maybeSetPersistPending();
    } finally {
      finishTransaction();
    }
  }

  public void invokeABAChangeLPs(Set communities) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (logger.isDebugEnabled()) {
      if (Thread.holdsLock(distributorLock)) {
        logger.debug("Distributor.invokeABAChangeLPs invoked inside distributorLock",
                     new Throwable());
      }
      if (Thread.holdsLock(transactionLock)) {
        logger.debug("Distributor.invokeABAChangeLPs invoked inside transactionLock",
                     new Throwable());
      }
    }
    boolean persistWanted = false;
    synchronized (distributorLock) {
      try {
        blackboard.startTransaction();
        blackboard.invokeABAChangeLPs(communities);
        Envelope envelope =
          blackboard.receiveMessages(Collections.EMPTY_LIST);
        persistWanted = distribute(envelope, blackboard.getClient(), true);
      } finally {
        blackboard.stopTransaction();
      }
    }
    if (persistWanted) maybeSetPersistPending();
  }

  /**
   * Generate a persistence delta if possible and necessary. It is
   * possible if the transaction count is zero and necessary if either
   * PERSIST_PENDING is true or needToPersist is true and we are not
   * busy. This second clause is needed so we don't end up being idle
   * with needToPersist being true.
   */
  private PersistenceObject doPersistence(
      boolean persistedStateNeeded, boolean full) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    assert transactionCount == 1 : transactionCount;
    assert persistFlags != 0 : persistFlags;
    nodeBusyService.setAgentBusy(true);
    List epochEnvelopes;
    synchronized (distributorLock) {
      epochEnvelopes = getEpochEnvelopes();
      for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
        Subscriber subscriber = (Subscriber) iter.next();
        if (subscriber.isReadyToPersist()) {
          subscriberStates.add(new PersistenceSubscriberState(subscriber));
        }
      }
    }
    PersistenceObject result;
    synchronized (getMessageManager()) {
      getMessageManager().advanceEpoch();
      result = persistence.persist(
          epochEnvelopes,
          Collections.EMPTY_LIST,
          subscriberStates,
          persistedStateNeeded,
          full,
          lazyPersistence ? null : getMessageManager(),
          quiescenceMonitor.getState());
    }
    synchronized (distributorLock) {
      clearEpochEnvelopes();
      subscriberStates.clear();
      needToPersist = false;
      lastPersist = System.currentTimeMillis();
    }
    setPersistPending(false);
    nodeBusyService.setAgentBusy(false);
    return result;
  }

  private boolean timeToLazilyPersist() {
    if (dummyPersistence) {
      return false;
    }
    long overdue =
      System.currentTimeMillis() -
      persistence.getPersistenceTime();
    return overdue > 0L;
  }

  private boolean timeToPersist() {
    assert  Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (dummyPersistence) {
      return false;
    }
    long nextPersistTime =
      Math.min(
          lastPersist + MAX_PERSIST_INTERVAL,
          persistence.getPersistenceTime());
    return (System.currentTimeMillis() >= nextPersistTime);
  }

  /**
   * Transaction control
   */
  private static final String START_EXCUSE = "Waiting to Start Transaction";
  public void startTransaction() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);

    acquireTransactionMutex();

    synchronized (transactionLock) {
      while (persistFlags != 0) {
        try {
	    SchedulableStatus.beginWait(START_EXCUSE);
	    transactionLock.wait();
        } catch (InterruptedException ie) {
        } finally {
	    SchedulableStatus.endBlocking();
	}
      }
      transactionCount++;
    }
  }

  public void finishTransaction(
      Envelope outbox, BlackboardClient client) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    boolean persistWanted = false;
    synchronized (distributorLock) {
      persistWanted = distribute(outbox, client);
    }
    if (persistWanted) maybeSetPersistPending();
    finishTransaction();
  }

  private void finishTransaction() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    boolean doIt = false;
    synchronized (transactionLock) {
      if ((persistFlags & PERSIST_PENDING) != 0) {
        if (transactionCount == 1) {
          // transactionCount == 1 implies ((persistFlags & PERSIST_ACTIVE) == 0)
          if (logger.isInfoEnabled()) {
            logger.info("Persist started (finish transaction)");
          }
          assert ((persistFlags & PERSIST_ACTIVE) == 0);
          doIt = true;
        } else {
          if (logger.isInfoEnabled()) {
            logger.info("Persist deferred, "
                        + transactionCount
                        + " transactions open");
          }
        }
      }
    }

    releaseTransactionMutex();

    if (doIt) {
      doPersistence(false, false);
      if (logger.isInfoEnabled()) {
        logger.info("Persist completed (finish transaction)");
      }
      if (logger.isInfoEnabled()) logger.info("reservation release");
      persistenceReservationManager.release(persistence);
    }
    synchronized (transactionLock) {
      transactionCount--;
      assert transactionCount >= 0 : transactionCount;
      transactionLock.notifyAll();
    }
  }

  /**
   * Force a persistence delta to be generated.
   */
  public void persistNow() {
    persist(false, true);
  }

  /**
   * Force a (full) persistence delta to be generated and
   * return result
   */
  public PersistenceObject getPersistenceObject() {
    return persist(true, true);
  }

  /**
   * Generate a persistence delta and (maybe) return the data of
   * that delta.
   * <p>
   * This code parallels that of start/finish transaction except
   * that:<pre>
   *   distribute() is not called
   *   we wait for all transactions to close
   * </pre><p>
   * A "PERSIST_ACTIVE" flag is used to guarantee that only
   * one persist occurs at a time.
   *
   * @param isStateWanted true if the data of a full persistence
   *   delta is wanted
   * @return a state Object including all the data from a full
   * persistence delta if isStateWanted is true, null if
   *   isStateWanted is false.
   */
  private PersistenceObject persist(boolean isStateWanted, boolean full) {
      assert !Thread.holdsLock(distributorLock);
      assert !Thread.holdsLock(transactionLock);
      if (persistence == null ||
          (dummyPersistence && !isStateWanted)) {
        return null;
      }
      while (true) {            // Loop until we succeed and return a result
        synchronized (transactionLock) {
          // First we must wait for any other persistence activity to cease
          lockoutPersistence();
        }
        // Then we have to wait for our reservation to become ripe
        if (logger.isInfoEnabled()) {
          logger.info("reservation waitfor");
        }
        persistenceReservationManager.waitFor(persistence, logger);
        synchronized (transactionLock) {
          // Now we wait for all transactions to finish
          lockoutTransactions();
        }
        try {
          // We commit to doing persistence, but we may have lost our reservation
          if (persistenceReservationManager.commit(persistence)) {
            if (logger.isInfoEnabled()) {
              logger.info("Persist started (persist)");
            }
            PersistenceObject result = 
              doPersistence(
                  isStateWanted,
                  (full || dummyPersistence));
            if (logger.isInfoEnabled()) logger.info("reservation release");
            persistenceReservationManager.release(persistence);
            if (logger.isInfoEnabled()) {
              logger.info("Persist completed (persist)");
            }
            return result;
          } else {
            // Lost our reservation. Back out of transaction counting and start over
            if (logger.isInfoEnabled()) {
              logger.info("Reservation lost, starting over");
            }
          }
        } finally {
          synchronized (transactionLock) {
            resumePersistence();
            resumeTransactions();
          }
        }
      }
    }

  private static final String LOCKOUT_EXCUSE = "Waiting for active persist to complete";
  private void lockoutPersistence() {
    assert !Thread.holdsLock(distributorLock);
    assert Thread.holdsLock(transactionLock);
    if ((persistFlags & PERSIST_ACTIVE) != 0) {
      if (logger.isInfoEnabled()) {
        logger.info(LOCKOUT_EXCUSE);
      }
      do {
        try {
	    SchedulableStatus.beginWait(LOCKOUT_EXCUSE);
	    transactionLock.wait();
        } catch (InterruptedException ie) {
        } finally {
	    SchedulableStatus.endBlocking();
	}
       } while ((persistFlags & PERSIST_ACTIVE) != 0);
    }
    persistFlags |= PERSIST_ACTIVE;
    // don't care about PERSIST_PENDING or SUSPENDED.
    //
    // To persist we still need to lockout transactions
  }

  private void resumePersistence() {
    assert !Thread.holdsLock(distributorLock);
    assert Thread.holdsLock(transactionLock);
    assert ((persistFlags & PERSIST_ACTIVE) != 0);
    persistFlags &= ~PERSIST_ACTIVE;
    // we need to notify the transaction lock.  
    //
    // This is always called just before resuming transactions,
    // which will notify the lock, so we'll let that method
    // do the notification.
  }
  private static final String TRANSACTION_EXCUSE = "Waiting for transaction to close";
  private void lockoutTransactions() {
    assert !Thread.holdsLock(distributorLock);
    assert Thread.holdsLock(transactionLock);
    if (logger.isInfoEnabled()) {
      logger.info("Waiting for " + transactionCount +
                  " transactions to close");
    }
    transactionCount++;
    assert transactionCount >= 1 : transactionCount;
    while (transactionCount > 1) {
      try {
	  SchedulableStatus.beginWait(TRANSACTION_EXCUSE);
	  transactionLock.wait();
      } catch (InterruptedException ie) {
      } finally {
	  SchedulableStatus.endBlocking();
      }
     }
    assert transactionCount == 1 : transactionCount;
    // if we've locked out persistence then it's now save to persist
  }

  private void resumeTransactions() {
    assert !Thread.holdsLock(distributorLock);
    assert Thread.holdsLock(transactionLock);
    assert transactionCount >= 1 : transactionCount;
    transactionCount--;
    assert transactionCount == 0 : transactionCount;
    transactionLock.notifyAll();
  }

  private void maybeSetPersistPending() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (!lazyPersistence || timeToLazilyPersist()) {
      if (persistenceReservationManager.request(persistence)) {
        if (logger.isInfoEnabled()) logger.info("reservation request succeeded");
        setPersistPending(true);
      } else if ((persistFlags & PERSIST_PENDING) != 0) {
        // Whoops. We lost our reservation
        if (logger.isInfoEnabled()) logger.info("reservation request failed");
        setPersistPending(false); // Need to start all over
      }
    }
  }

  private void setPersistPending(boolean on) {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    synchronized (transactionLock) {
      if (on) {
        persistFlags |= PERSIST_PENDING;
      } else if ((persistFlags & PERSIST_PENDING) != 0) {
        persistFlags &= ~PERSIST_PENDING;
        transactionLock.notifyAll();
      }
    }
  }

  private void setSuspended(boolean on) {
    assert !Thread.holdsLock(distributorLock);
    assert Thread.holdsLock(transactionLock);
    if (on) {
      persistFlags |= SUSPENDED;
    } else if ((persistFlags & SUSPENDED) != 0) {
      persistFlags &= ~SUSPENDED;
      transactionLock.notifyAll();
    }
  }

  private void timerPersist() {
    assert !Thread.holdsLock(distributorLock);
    assert !Thread.holdsLock(transactionLock);
    if (lazyPersistence && !timeToLazilyPersist()) {
      return;
    }
    synchronized (distributorLock) {
      if (!needToPersist) {
        return;
      }
    }
    synchronized (transactionLock) {
      if ((persistFlags & PERSIST_ACTIVE) != 0) {
        return;
      }
      if ((persistFlags & PERSIST_PENDING) != 0 &&
          transactionCount >= 1) {
        return;
      }
    }
    // there's a small window here where we may do an unnecessary
    // second persist, but that would be harmless and quick
    persist(false, false);
  }

  private void logEnvelope(Envelope envelope, BlackboardClient client) {
    if (!logger.isDetailEnabled()) return;
    boolean first = true;
    for (Iterator tuples = envelope.getAllTuples(); tuples.hasNext(); ) {
      if (first) {
        logger.detail(
            "Outbox of " + client.getBlackboardClientName());
        first = false;
      }
      EnvelopeTuple tuple = (EnvelopeTuple) tuples.next();
      if (tuple.isBulk()) {
        for (Iterator objects =
            ((BulkEnvelopeTuple) tuple).getCollection().iterator();
            objects.hasNext(); ) {
          logger.detail("BULK   " + objects.next());
        }
      } else {
        String kind = "";
        if (tuple.isAdd()) {
          kind = "ADD    ";
        } else if (tuple.isChange()) {
          kind = "CHANGE ";
        } else {
          kind = "REMOVE ";
        }
        logger.detail(kind + tuple.getObject());
      }
    }
  }

  public String getName() { return name; } // agent name

  @Override
public String toString() {
    return "<Distributor " + getName() + ">";
  }

  /**
   * Hold our set of registered Subscribers.
   * <p>
   * The Distributor must lock this object with its
   * "distributorLock".
   */
  private static class Subscribers {
    private List subscribers = new ArrayList();
    ReferenceQueue refQ = new ReferenceQueue();

    public void register(Subscriber subscriber) {
      checkRefQ();
      subscribers.add(new WeakReference(subscriber, refQ));
    }
    public void unregister(Subscriber subscriber) {
      for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
        WeakReference ref = (WeakReference) iter.next();
        if (ref.get() == subscriber) {
          iter.remove();
        }
      }
    }

    public Iterator iterator() {
      checkRefQ();

      class MyIterator implements Iterator {
        private Object n = null;
        private Iterator iter;

        public MyIterator(Iterator it) {
          iter = it;
          advance();
        }

        /**
         * Advance to the next non-null element, dropping
         * nulls along the way
         */
        private void advance() {
          while (iter.hasNext()) {
            WeakReference ref = (WeakReference) iter.next();
            n = ref.get();
            if (n == null) {
              iter.remove();
            } else {
              return;
            }
          }
          // ran off the end,
          n = null;
        }

        public boolean hasNext() {
          return (n != null);
        }
        public Object next() {
          Object x = n;
          advance();
          return x;
        }
        public void remove() {
          iter.remove();
        }
      };
      return new MyIterator(subscribers.iterator());
    }

    private void checkRefQ() {
      Reference ref;
      while ((ref = refQ.poll()) != null) {
        subscribers.remove(ref);
      }
    }
  }
}
