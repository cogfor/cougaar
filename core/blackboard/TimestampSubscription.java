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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * A {@link Subscription} that tracks {@link UniqueObject} publishAdd
 * and most recent publishChange timestamps.
 * <p>
 * These timestamps are not persisted, and upon rehydration the
 * creation time of the objects will be the agent restart time.
 * 
 * @see Subscriber required system property that must be enabled
 * @see #getTimestampEntry access to the (UID, TimestampEntry) timestamp data
 */
public class TimestampSubscription 
extends Subscription 
{

  // if this timestamp subscription is not shared, this list
  // buffers the removed entries until after the plugin 
  // transaction completes.
  //
  // this list is locked by the transaction.
  private final List removedList;

  // a map if (UID, TimestampEntry) pairs
  //
  // the map is locked to allow multiple reader threads to
  // access the "get*(UID)" methods.  Even if the subscription
  // is not shared, it must be locked to allow distributor
  // updates during a transaction.
  private final Map map;

  // the "apply(..)" timestamp from the transaction close time
  // of the TimestampedEnvelope that is being processed.
  //
  // only one "apply(..)" can occur at a time, so this is thread 
  // safe.
  private long time;

  /**
   * Equivalent to<code>new TimestampSubscription(p, true)</code>.
   */
  public TimestampSubscription(UnaryPredicate p) {
    this(p, true);
  }

  /**
   * @param p the predicate should only accept UniqueObjects; 
   *    all non-UniqueObjects and UniqueObjects with null UIDs 
   *    are ignored.
   * @param isShared if true, removals are done immediately,
   *    otherwise they are done at the end of the plugin's
   *    transaction.
   */
  public TimestampSubscription(
      UnaryPredicate p, boolean isShared) {
    super(p);
    map = new HashMap(13);
    removedList = (isShared ? null : new ArrayList(11));
  }

  //
  // all the methods from Subscription are provided.
  //

  /**
   * @see #getTimestampEntry get the creation time
   */
  public long getCreationTime(UID uid) {
    TimestampEntry entry = getTimestampEntry(uid);
    return 
      ((entry != null) ? 
       entry.getCreationTime() : 
       TimestampEntry.UNKNOWN_TIME);
  }

  /**
   * @see #getTimestampEntry get the modification time
   */
  public long getModificationTime(UID uid) {
    TimestampEntry entry = getTimestampEntry(uid);
    return 
      ((entry != null) ? 
       entry.getModificationTime() : 
       TimestampEntry.UNKNOWN_TIME);
  }

  /**
   * Get the TimestampEntry for the local blackboard object with the 
   * given UID.
   * <p>
   * The object must match this subscription's predicate.
   * <p>
   * The timestamps are measured in milliseconds, and matches the
   * transaction close times of the subscriber that performed
   * the "publishAdd()" or "publishChange()".
   * <p>
   * This method is thread-safe to allow multiple clients to
   * access the underlying (UID, TimestampEntry) map.  The map is
   * also updated <i>during</i> the subscriber's transaction.  
   * Multiple calls to "getTimestampEntry()", even within the same
   * subscriber transaction, may return different results.
   *
   * @return the TimestampEntry, or null if not known.
   */
  public TimestampEntry getTimestampEntry(UID uid) {
    synchronized (map) {
      return (TimestampEntry) map.get(uid);
    }
  }

  @Override
protected void privateAdd(Object o, boolean isVisible) {
    // always fill in the map, even if (!isVisible)
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        TimestampEntry entry = new TimestampEntry(time, time);
        synchronized (map) {
          map.put(uid, entry);
        }
      }
    }
  }

  @Override
protected void privateChange(Object o, List changes, boolean isVisible) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      if (uid != null) {
        TimestampEntry newEntry = new TimestampEntry(time, time);
        synchronized (map) {
          TimestampEntry prevEntry = (TimestampEntry) map.put(uid, newEntry);
          if (prevEntry != null) {
            // typical case.  replace an existing entry.
            long creationTime = prevEntry.getCreationTime();
            // assert (creationTime <= time);
            //
            // this "private_*" call saves us an extra "map.get(..)".
            // it is safe only within this "map.put(..)" situation
            newEntry.private_setCreationTime(creationTime);
          }
        }
      }
    }
  }

  @Override
protected void privateRemove(Object o, boolean isVisible) {
    if (removedList == null) {
      removeEntry(o);     // remove immediately
    } else {
      removedList.add(o); // wait until transaction close
    }
  }

  private void removeEntry(Object o) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject) o).getUID();
      synchronized (map) {
        map.remove(uid);
      }
    }
  }

  @Override
protected void resetChanges() {
    super.resetChanges();
    if (removedList != null) {
      // process removals
      int n = removedList.size();
      if (n > 0) {
        for (int i = 0; i < n; i++) {
          removeEntry(removedList.get(i));
        }
        removedList.clear();
      }
    }
  }

  @Override
public boolean apply(Envelope envelope) {
    if (envelope instanceof TimestampedEnvelope) {
      TimestampedEnvelope te = (TimestampedEnvelope) envelope;
      long closeTime = te.getTransactionCloseTime();
      if (closeTime != TimestampEntry.UNKNOWN_TIME) {
        this.time = closeTime;
        return super.apply(envelope);
      }
    } else if (envelope instanceof InitializeSubscriptionEnvelope) {
      super.apply(envelope);
    }
    // FIXME should we still "apply(..)" with the current time?
    return false;
  }

}
