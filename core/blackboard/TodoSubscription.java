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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/**
 * A subscription that queues objects, such as Alarms and other non-blackboard
 * callbacks, for processing in the plugin's "execute()" thread.
 * <p>
 * TodoSubscriptions are typically used for Alarm callbacks, for example:
 * <pre>
 *   public class MyPlugin extends ComponentPlugin {
 *
 *     private TodoSubscription expiredAlarms;
 *
 *     protected void setupSubscriptions() {
 *       expiredAlarms = (TodoSubscription) 
 *         blackboard.subscribe(new TodoSubscription("x"));
 *
 *       // wake up in 10 seconds
 *       getAlarmService().addRealTimeAlarm(
 *           new MyAlarm(System.currentTimeMillis() + 10000));
 *     }
 *
 *     protected void execute() {
 *       if (expiredAlarms.hasChanged()) {
 *         System.out.println("Due alarms: "+expiredAlarms.getAddedCollection());
 *       }
 *     }
 *
 *     private class MyAlarm extends AlarmBase {
 *       // optionally add fields here, e.g. data or a Runnable
 *       public MyAlarm(long time) { super(time); }
 *
 *       // don't do work in the "onExpire()" method, since it's the
 *       // AlarmService's callback thread.  Instead, put this alarm on our
 *       // "todo" queue, which will asynchronously call our "execute()".
 *       public void onExpire() { expiredAlarms.add(this); }
 *     }
 *   }
 * </pre>
 * <p>
 * This is analogous to Swing's "invokeLater" pattern, for example:
 * <pre>
 *    SwingUtilities.invokeLater(new Runnable() {
 *      public void run() { ... }
 *    });
 * </pre>
 * where the Runnable is put on a queue and will be "run()" in the Swing
 * thread.
 */
public class TodoSubscription<E> extends Subscription<E> {

  // this is never used, but is required by our super class
  private static final UnaryPredicate FALSE_P =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) { return false; }
    };

  private final String name;
  private Collection<E> active = null; 

  /**
   * Create a TodoSubscription with the given non-null, unique name.
   * <p>
   * The name can be an arbitrary, non-null name, so long as the set of
   * names is unique within each plugin instance.
   * <p>
   * We need the name to support agent mobility and persistence in cases where
   * a plugin has more than one TodoSubscription.
   * <p>
   * For example, say a plugin has two TodoSubscriptions:<pre>
   *   protected void setupSubscriptions() {
   *     alpha = (TodoSubscription) blackboard.subscribe(new TodoSubscription("x"));
   *     beta = (TodoSubscription) blackboard.subscribe(new TodoSubscription("y"));
   *   }
   * </pre>
   * and we persist just <i>after</i> calling:<pre>
   *   alpha.add(new Foo(1234));
   * </pre>
   * but <i>before</i> we've had a chance to "execute()".  This pending object
   * will be persisted as a pending "inbox envelope" and rehydrated when we
   * restart the agent.  Our "setupSubscriptions()" will be called again, and we
   * want to make sure that the object is put on the "alpha" 
   * {@link #getAddedCollection} and not on "beta".
   * <p>
   * We need the name to match the rehydrated "inbox envelope" with the above<pre>
   *    ... new TodoSubscription("x") ...
   * </pre>
   * since we persist the envelopes, <i>not</i> the subscription instances.
   * Besides, the "new TodoSubscription" is a brand new subscription instance,
   * so without the name, we'd have no way to figure out that these two
   * instances represent the same "todo".
   */
  public TodoSubscription(String name) {
    super(FALSE_P);
    this.name = name;
    if (name == null) {
      throw new IllegalArgumentException("Null name");
    }
  }

  /**
   * Create the backing collection for reuse in {@link #getAddedCollection},
   * which defaults to an ArrayList.
   * <p>
   * Another useful option is a LinkedHashSet, which filters out duplicates.
   */
  protected Collection<E> createCollection() {
    return new ArrayList<E>(5);
  }

  /**
   * Add an object to the queue of pending objects that will be visible in the
   * next blackboard transaction's {@link #getAddedCollection}, and request an
   * asynchronous plugin "execute()" cycle.
   * <p>
   * This is analogous to a blackboard "publishAdd", except that the object is
   * only visible to this subscription and will not be persisted.
   *
   * @param o the object to put on the queue, which can be any non-null object
   * (either data or a Runnable)
   */
  public void add(E o) {
    if (o == null) throw new NullPointerException();
    addLater(o, false);
  }

  /** @see #add(Object) */
  public void addAll(Collection<E> c) { 
    if (!c.isEmpty()) {
      addLater(c, true);
    }
  }

  private void addLater(Object o, boolean isBulk) {
    subscriber.receiveEnvelopes(
        Collections.singletonList(
          new TodoEnvelope(name, o, isBulk)),
        true);
  }

  private boolean addNow(Object o, boolean isBulk) {
    if (o == null) return false;
    if (active == null) {
      active = createCollection();
    }
    if (!isBulk) return active.add((E) o);
    boolean ret = false;
    Collection c = (Collection) o;
    for (Iterator iter = c.iterator(); iter.hasNext(); ) {
      Object oi = iter.next();
      if (oi != null) {
        ret |= active.add((E) oi);
      }
    }
    return ret;
  }

  /**
   * @return an enumeration of the objects that have been added
   * since the last transaction.
   */
  public Enumeration<E> getAddedList() {
    checkTransactionOK("getAddedList()");
    if (active == null || active.isEmpty()) return Empty.elements();
    return new Enumerator<E>(active);
  }

  /**
   * @return a possibly empty collection of objects that have been
   * added since the last transaction. Will not return null.
   */
  public Collection<E> getAddedCollection() {
    if (active == null) return Collections.emptySet();
    return active;
  }

  @Override
protected void resetChanges() {
    super.resetChanges();
    if (active != null) {
      active.clear();
    }
  }

  /** For infrastructure use only */
  @Override
public void fill(Envelope envelope) {
    // we only care about rehydrated "pending envelopes"
    apply(envelope);
  }

  /** For infrastructure use only */
  @Override
public boolean apply(Envelope envelope) {
    if (envelope instanceof TodoEnvelope) {
      TodoEnvelope te = (TodoEnvelope) envelope;
      if (name.equals(te.getName())) {
        boolean ret = addNow(te.getObject(), te.isBulk);
        if (ret) {
          setChanged(true);
        }
        return ret;
      }
    }
    return false;
  }

  // we override "apply", so these methods are never called.
  @Override
protected void privateAdd(Object o, boolean isVisible) { die(); }
  @Override
protected void privateRemove(Object o, boolean isVisible) { die(); }
  @Override
protected void privateChange(Object o, List changes, boolean isVisible) {
    die();
  }
  private void die() { throw new InternalError(); }


  private static final class TodoEnvelope extends Envelope {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final String name;
    private final Object o;
    private final boolean isBulk;

    public TodoEnvelope(String name, Object o, boolean isBulk) {
      this.name = name;
      this.o = o;
      this.isBulk = isBulk;
    }

    @Override
   public Envelope newInstance() {
      return new TodoEnvelope(name, o, isBulk);
    }

    public String getName() { return name; }
    public Object getObject() { return o; }
    
    @SuppressWarnings("unused")
   public boolean isBulk() { return isBulk; }

    @Override
   public String toString() { return "TodoEnvelope for "+name; }
  }
}
