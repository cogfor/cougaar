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

import java.util.Iterator;
import java.util.LinkedList;

import org.cougaar.util.log.Logger;

/**
 * The ReservationManager coordinates {@link
 * org.cougaar.core.blackboard.Distributor} persistence to ensure
 * that only one agent can persist at a time, and that an agent
 * preparing to persist will not block other agents from
 * persisting.
 * <p> 
 * Persistence reservations indicate that a persistence instance
 * wishes to take a snapshot of its agent. The reservations are held
 * in a queue (FIFO). When a persistence instance reaches the head of
 * the queue it has exclusive use of the persistence mechanism. The
 * reservation will only be held for a certain interval and if not
 * exercised or re-confirmed within that interval, it is cancelled.
 * During this interval, the agent should be getting itself into a
 * well-defined state so the persistence snapshot will be valid.
 * <p>
 * If at any time after reaching the head of the queue (and trying to
 * reach a well-defined state), an agent discovers that its
 * reservation has been cancelled, it should abandon its attempt to
 * reach a well-defined state, continue execution, and try again
 * later.
 * <p>
 * If a ReservationManager is created with a timeout of 0, the manager
 * is effectively disabled. This means that all requests and commits
 * are satisfied unconditionally, and waitFor and release return
 * immediately and do nothing. Also no storage is allocated.
 */
public class ReservationManager {
  private LinkedList queue = null;
  private long timeout;
  private boolean committed;

  private class Item {
    private Object obj;
    private long expires;
    public Item(Object p, long now) {
      obj = p;
      updateTimestamp(now);
    }

    public boolean hasExpired(long now) {
      return expires <= now;
    }

    public void updateTimestamp(long now) {
      expires = now + timeout;
    }

    @Override
   public String toString() {
      return obj.toString();
    }
  }

  public ReservationManager(long timeout) {
    this.timeout = timeout;
    if (timeout > 0L) {
      queue = new LinkedList();
    }
  }

  public synchronized boolean request(Object p) {
    if (queue == null) return true;
    long now = System.currentTimeMillis();
    Item item = findOrCreateItem(p, now);
    if (!committed) removeExpiredItems(now);
    boolean result = item == queue.getFirst();
    return result;
  }

  private Item findOrCreateItem(Object p, long now) {
    Item item = findItem(p);
    if (item == null) {
      item = new Item(p, now);
      queue.add(item);
    } else {
      item.updateTimestamp(now);
    }
    return item;
  }

  public synchronized void waitFor(Object p, Logger logger) {
    if (queue == null) return;
    while (true) {
      long now = System.currentTimeMillis();
      Item item = findOrCreateItem(p, now);
      if (!committed) removeExpiredItems(now);
      if (item == queue.getFirst()) return;
      try {
        Item firstItem = (Item) queue.getFirst();
        long delay = firstItem.expires - now;
        if (logger != null && logger.isInfoEnabled()) {
          logger.info("waitFor " + delay + " for " + firstItem);
        }
        if (delay <= 0) {
          wait();               // Must be committed, wait for release
        } else {
          wait(delay);          // Uncommitted, wait for timeout or release.
        }
        if (logger != null && logger.isInfoEnabled()) {
          logger.info("waitFor wait finished");
        }
      } catch (InterruptedException ie) {
      }
    }
  }

  public synchronized boolean commit(Object p) {
    if (queue == null) return true;
    if (request(p)) {
      committed = true;
      return true;
    }
    return false;
  }

  public synchronized void release(Object p) {
    if (queue == null) return;
    Item item = findItem(p);
    if (item != null) {
      queue.remove(item);
      committed = false;
      notifyAll();
    }
  }

  private void removeExpiredItems(long now) {
    for (Iterator i = queue.iterator(); i.hasNext(); ) {
      Item item = (Item) i.next();
      if (item.hasExpired(now)) {
        i.remove();
      }
    }
  }

  private Item findItem(Object p) {
    for (Iterator i = queue.iterator(); i.hasNext(); ) {
      Item item = (Item) i.next();
      if (item.obj == p) {
        return item;
      }
    }
    return null;
  }
}
