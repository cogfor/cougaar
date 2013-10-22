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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.DynamicUnaryPredicate;
import org.cougaar.util.StackElements;
import org.cougaar.util.UnaryPredicate;

/** 
 * A subscription that only tracks add/change/remove deltas.
 * <p>
 * See {@link CollectionSubscription} for a system property that identifies
 * subscriptions that could potentially be changed to DeltaSubscriptions.
 * <p>
 * We subclass {@link IncrementalSubscription} to both reuse code and to make
 * it easy for existing plugins to switch over to this class.  For example, the
 * plugin won't need to change its field declaration from:<pre>
 *    private IncrementalSubscription sub;
 * </pre>
 * to be "DeltaSubscription".  The only change is the subscribe line:<pre>
 *      sub = (IncrementalSubscription) 
 *        blackboard.subscribe(new DeltaSubscription(pred));
 * </pre>
 * The downside is a tiny cost per subscription for the pointer to the unused
 * "real" backing collection.
 *
 * @property org.cougaar.core.blackboard.DeltaSubscription.enable
 * Enable DeltaSubscription memory saving.  This property exists so we can
 * easily profile the memory advantage of DeltaSubscriptions when compared to
 * using regular IncrementalSubscriptions.
 * (defaults to true)
 *
 * @property org.cougaar.core.blackboard.DeltaSubscription.recordStack
 * Track the creation point of every DeltaSubscription, so if it is misused
 * as a CollectionSubscription the runtime exception will point to the original
 * "subscribe" call.
 * (defaults to false)
 */
public class DeltaSubscription extends IncrementalSubscription {

  private static final String ENABLED_PROP =
    "org.cougaar.core.blackboard.DeltaSubscription.enable";
  private static final boolean ENABLED =
    SystemProperties.getBoolean(ENABLED_PROP, true);

  private static final String RECORD_STACK_PROP =
    "org.cougaar.core.blackboard.DeltaSubscription.recordStack";
  private static final boolean RECORD_STACK =
    SystemProperties.getBoolean(RECORD_STACK_PROP);

  private static final Collection BLOCKER_COLLECTION =
    new BlockerCollection();

  public DeltaSubscription(UnaryPredicate p) {
    super(p, makeCollection());
    if (ENABLED && p instanceof DynamicUnaryPredicate) {
      throw new IllegalArgumentException("Dynamic predicate: "+p);
    }
  }
  
  private static Collection makeCollection() {
    if (!ENABLED) {
      return new HashSet();
    } else if (RECORD_STACK) {
      return new TrackedCollection();
    } else {
      return BLOCKER_COLLECTION;
    }
  }

  @Override
public String toString() {
    return "Delta Subscription";
  }

  private static class BlockerCollection implements Collection {
    // allow our CollectionSubscription to call these methods
    public boolean add(Object o) { return false; }
    public boolean remove(Object o) { return false; }

    // block the rest
    public int size() { die(); return 0; }
    public boolean isEmpty() { die(); return false; }
    public boolean contains(Object o) { die(); return false; }
    public Iterator iterator() { die(); return null; }
    public Object[] toArray() { die(); return null; }
    public Object[] toArray(Object a[]) { die(); return null; }
    public boolean containsAll(Collection c) { die(); return false; }
    public boolean addAll(Collection c) { die(); return false; }
    public boolean removeAll(Collection c) { die(); return false; }
    public boolean retainAll(Collection c) { die(); return false; }
    public void clear() { die(); }
    @Override
   public boolean equals(Object o) { die(); return false; }
    @Override
   public int hashCode() { die(); return 0; }

    protected void die() {
      // create error message
      StringBuffer buf = new StringBuffer();
      buf.append(
          "Invalid use of a DeltaSubscription as a Collection."+
          "\nTo disable delta subscriptions, set -D"+ENABLED_PROP+"=false");
      Throwable t = getStack();
      if (t == null) {
        buf.append(
            "\nFor subscription creation stack info, run again with "+
            "-D"+RECORD_STACK_PROP+"=true");
      } else {
        buf.append("\nCreated at");
        StackTraceElement[] ste = t.getStackTrace();
        for (int i = 0; i < ste.length; i++) {
          buf.append("\n\tat ").append(ste[i]);
        }
        buf.append("\nObserved at:");
      }
      String msg = buf.toString();

      // throw exception
      throw new UnsupportedOperationException(msg);
    }

    protected Throwable getStack() {
      return null;
    }
  }

  private static class TrackedCollection extends BlockerCollection {
    private static final Map stacks = new WeakHashMap();
    // keep the StackElements and not just the throwable, otherwise our cache
    // won't work (since it's weak)
    private final StackElements stack;
    public TrackedCollection() {
      this.stack = captureStack();
    }
    // enhance the "die()" message with our allocation point
    @Override
   protected Throwable getStack() {
      return stack.getThrowable();
    }
    private static StackElements captureStack() {
      StackElements se = new StackElements(new Throwable());
      synchronized (stacks) {
        StackElements cached_se = (StackElements) stacks.get(se);
        if (cached_se == null) {
          stacks.put(se, se);
        } else {
          se = cached_se;
        }
      }
      return se;
    }
  }
}
