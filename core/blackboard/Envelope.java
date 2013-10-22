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
import java.util.Iterator;
import java.util.List;

/**
 * A container for blackboard add/change/remove {@link
 * EnvelopeTuple}s of a single transaction.
 * <p>
 * An envelope is not synchronized; it is privately held by a
 * subscriber and passes through the distributor in a thread-safe
 * manner.
 * <p> 
 * Several types of transaction operations are supported:
 * <pre> 
 *   ADD        add an object to the set.
 *   REMOVE     remove an object from the set.
 *   CHANGE     mark the object as changed in the set.
 *   BULK       like ADD of a set of Objects, with the
 *                slightly different functional semantics.
 *                In particular, since these are only emitted
 *                Blackboard on initialization of subscriptions,
 *                and by PersistenceSubscribers on Blackboard
 *                rehydration, LogicProviders function differently
 *                on BULKs than ADDs, for instance, business rules
 *                which fire on new Blackboard elements and produce
 *                other Blackboard elements will not fire on BULKs
 *                because the BULK delta should already include
 *                *those* products.
 * </pre>
 */
public class Envelope implements java.io.Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
/** a set of changes to make to a subscriber's data structures */
  private final List deltas = new ArrayList(5);

  /**
   * direct access to the internal structure to allow for more efficient
   * traversal of deltas.
   */
  final List getRawDeltas() { return deltas; }

  public static final int ADD = 0;
  public static final int REMOVE = 1;
  public static final int CHANGE = 2;
  /**
   * BULK is a bulk add - then Object is a Container of objects
   * Additionally, BULK tuples are generally handled differently than 
   * ADDs by LogicProviders (See documentation on individual LPs to see
   * how they process BULK transactions.
   */
  public static final int BULK = 3;

  public static final int EVENT = 4;

  public Envelope() { }

  /** Create a copy with no deltas */
  public Envelope newInstance() {
    return new Envelope();
  }

  public final EnvelopeTuple addObject(Object o) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = newAddEnvelopeTuple(o);
    deltas.add(t);
    return t;
  }
  public final EnvelopeTuple changeObject(Object o, List changes) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = newChangeEnvelopeTuple(o, changes);
    deltas.add(t);
    return t;
  }
  public final EnvelopeTuple removeObject(Object o) {
    if (o == null) throw new IllegalArgumentException("Null Object");
    EnvelopeTuple t = newRemoveEnvelopeTuple(o);
    deltas.add(t);
    return t;
  }

  // only allow package-local subclass overrides for these:
  AddEnvelopeTuple newAddEnvelopeTuple(Object o) {
    return new AddEnvelopeTuple(o);
  }
  ChangeEnvelopeTuple newChangeEnvelopeTuple(Object o, List changes) {
    return new ChangeEnvelopeTuple(o, changes);
  }
  RemoveEnvelopeTuple newRemoveEnvelopeTuple(Object o) {
    return new RemoveEnvelopeTuple(o);
  }

  public final void addTuple(EnvelopeTuple t) {
    deltas.add(t);
  }

  /** how many elements are in the Envelope?*/
  public final int size() {
    return deltas.size();
  }

  public final Iterator getAllTuples()
  {
    return deltas.iterator();
  }

  /**
   * Equivalent to adding a homogeneous Collection of objects
   * as separate adds.  Distributor-level predicate tests will
   * assume that all objects in the Collection apply to the same
   * degree to a given predicate instance.
   * The container must be be immutable, as there is no guarantee
   * that it will be unpacked at any specific time.  If this is
   * a problem, the Enumeration form should be used.
   */
  public final EnvelopeTuple bulkAddObject(Collection c) {
    if (c == null) throw new IllegalArgumentException("Null Collection");
    EnvelopeTuple t = new BulkEnvelopeTuple(c);
    deltas.add(t);
    return t;
  }

  /**
   * Safer form of bulkAddObject does the equivalent
   * of calling bulkAddObject on a container
   * constructed by iterating over the elements of 
   * the Enumeration argument.
   */
  public final EnvelopeTuple bulkAddObject(Enumeration en) {
    List v = new ArrayList();
    while (en.hasMoreElements()) {
      v.add(en.nextElement());
    }

    EnvelopeTuple t = new BulkEnvelopeTuple(v);
    deltas.add(t);
    return t;
  }

  /**
   * Safer form of bulkAddObject does the equivalent
   * of calling bulkAddObject on a container
   * constructed by iterating over the elements of 
   * the argument.
   */
  public final EnvelopeTuple bulkAddObject(Iterator i) {
    List v = new ArrayList();
    while (i.hasNext()) {
      v.add(i.next());
    }

    EnvelopeTuple t = new BulkEnvelopeTuple(v);
    deltas.add(t);
    return t;
  }

  /**
   * Boolean used to decide on visibility of subscription modifications
   * in applyToSubscription. Overridden by PersistenceEnvelope.
   */
  protected boolean isVisible() { return true; }

  /**
   * Apply all object deltas in this envelope to the subscription.
   */
  public final boolean applyToSubscription(Subscription subscription) {
    boolean vp = isVisible();     // in case we've got *lots* of tuples.
    boolean somethingFired = false;
    // we use the List directly instead of getAllTuples to avoid iterator overhead.
    int l = deltas.size();
    for (int i = 0; i<l; i++) {
      EnvelopeTuple tuple = (EnvelopeTuple) deltas.get(i);
      somethingFired |= tuple.applyToSubscription(subscription, vp);
    }
    return vp && somethingFired;
  }

  @Override
public String toString() {
    return getClass().getName()+" ["+deltas.size()+"]";
  }
}

