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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A record of the recent publication history, enabled by the
 * {@link org.cougaar.core.blackboard.Distributor}'s
 * "keepPublishHistory" option, that can be used to help debug
 * apparently anomalous publish events.
 */
public class PublishHistory {
  /**
   * Item records the current publication history of an object as a
   * stack dump (Throwable) for each of add, change, and remove. It
   * also has the time of the last publish event.
   */
  private static class Item implements Comparable {
    public Throwable add, change, remove;
    public long lastTime;
    public void recordAdd() {
      add = new Throwable("add@" + new Date(lastTime));
    }
    public void recordChange() {
      change = new Throwable("change@" + new Date(lastTime));
    }
    public void recordRemove() {
      remove = new Throwable("remove@" + new Date(lastTime));
    }
    public int compareTo(Object o) {
      Item other = (Item) o;
      if (other == this) return 0;
      long diff = lastTime - other.lastTime;
      if (diff > 0L) return 1;
      if (diff < 0L) return -1;
      return this.hashCode() - other.hashCode();
    }
    public void dumpStacks() {
      if (add != null)
        add.printStackTrace(System.out);
      else
        System.out.println("No add recorded");
      if (change != null)
        change.printStackTrace(System.out);
      else
        System.out.println("No change recorded");
      if (remove != null)
        remove.printStackTrace(System.out);
      else
        System.out.println("No remove recorded");
    }
  }

  /**
   * WeakReference extension having a Object that is a key to the
   * map Map. The values in that Map are Ref objects referring to an
   * Item. When the Ref in the map is the only remaining reference
   * to the Item, the map entry is removed. The key Object is kept
   * in the Ref because it is much faster to remove the entry by
   * using its key than by using its value.
   */
  private static class Ref extends WeakReference {
    public Object object;
    public Ref(Object object, Item item, ReferenceQueue refQ) {
      super(item, refQ);
      this.object = object;
    }
  }

  /**
   * A set of Items sorted by time. The least recently referenced
   * Items should be at the head of the Set so they can be quickly
   * removed as time elapses.
   */
  private static SortedSet items = new TreeSet();
  private static long nextCheckItems = System.currentTimeMillis() + 60000L;

  /**
   * Update the last reference time of an Item. The Item must be
   * removed from the sorted set prior to modifying its time because
   * the time is the basis for the sorting of the Set.
   */
  private static synchronized void updateItem(Item item) {
    items.remove(item);
    item.lastTime = System.currentTimeMillis();
    items.add(item);
  }

  /** Used to form the headset of items older than a certain time */
  private static Item deleteItem = new Item();

  /**
   * Remove Items older than 1 minute.
   */
  private static synchronized void checkItems() {
    long now = System.currentTimeMillis();
    if (now < nextCheckItems) return;
    nextCheckItems = now + 60000L;
    deleteItem.lastTime = now - 60000L;
    for (Iterator i = items.headSet(deleteItem).iterator(); i.hasNext(); ) {
      i.remove();
    }
  }

  /**
   * Map from published object to Ref to Item;
   */
  private Map map = new HashMap();

  private ReferenceQueue refQ = new ReferenceQueue();

  /**
   * Get the Item corresponding to a published Object. A new item is
   * created if necessary. In all cases, the lastTime of the Item is
   * updated to now.
   */
  private Item getItem(Object o) {
    Ref ref;
    Item item;
    checkItems();
    do {
      while ((ref = (Ref) refQ.poll()) != null) {
        map.remove(ref.object);
      }
      ref = (Ref) map.get(o);
      if (ref == null) {
        item = new Item();
        ref = new Ref(o, item, refQ);
        map.put(o, ref);
      } else {
        item = (Item) ref.get();
      }
    } while (item == null);
    updateItem(item);
    return item;
  }

  /**
   * Record a stack trace in the add slot of the item corresponding
   * to a Object.
   */
  public void publishAdd(Object o) {
    getItem(o).recordAdd();
  }

  /**
   * Record a stack trace in the change slot of the item
   * corresponding to a Object.
   */
  public void publishChange(Object o) {
    getItem(o).recordChange();
  }

  /**
   * Record a stack trace in the remove slot of the item
   * corresponding to a Object.
   */
  public void publishRemove(Object o) {
    getItem(o).recordRemove();
  }
  public void dumpStacks(Object o) {
    Item item = getItem(o);
    item.dumpStacks();
  }
}
