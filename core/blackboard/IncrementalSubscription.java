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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;
import org.cougaar.util.UnaryPredicate;

/**
 * A {@link CollectionSubscription} that records add/change/remove
 * deltas.
 */
public class IncrementalSubscription<E> extends CollectionSubscription<E> {

  public IncrementalSubscription(UnaryPredicate<E> p, Collection<E> c) {
    super(p, c);
  }

  private Set<E> myAddedSet = null; 
  private List<E> myRemovedList = null;

  @Override
protected void resetChanges() {
    super.resetChanges();
    if (myAddedSet != null) 
      myAddedSet.clear();
    if (myRemovedList != null)
      myRemovedList.clear();
  }

  /**
   * @return an enumeration of the objects that have been added
   * since the last transaction.
   */
  public Enumeration<E> getAddedList() {
    checkTransactionOK("getAddedList()");
    if (myAddedSet == null || myAddedSet.isEmpty()) 
      return Empty.elements();
    return new Enumerator<E>(myAddedSet);
  }

  /**
   * @return a possibly empty collection of objects that have been
   * added since the last transaction. Will not return null.
   */
  public Collection<E> getAddedCollection() {
    if (myAddedSet == null)
      return Collections.emptySet();
    return myAddedSet;
  }

  /**
   * @return a enumeration of the objects that have been removed
   * since the last transaction.
   */
  public Enumeration<E> getRemovedList() {
    checkTransactionOK("getRemovedList()");
    if (myRemovedList == null || myRemovedList.isEmpty())
      return Empty.elements();
    return new Enumerator<E>(myRemovedList);
  }

  /**
   * @return a possibly empty collection of objects that have been
   * removed since the last transaction.  Will not return null.
   */
  public Collection<E> getRemovedCollection() {
    if (myRemovedList == null)
      return Collections.emptyList();
    return myRemovedList;
  }

  /**
   * @return an enumeration of the objects that have been changed
   * since the last transaction.
   * @see #getChangeReports(Object)
   */
  public Enumeration<E> getChangedList() {
    checkTransactionOK("getChangedList()");
    return super.privateGetChangedList();
  }

  /**
   * @return a possibly empty collection of objects that have been
   * changed since the last transaction. Will not return null.
   * @see #getChangeReports(Object) 
   */
  public Collection<E> getChangedCollection() {
    return super.privateGetChangedCollection();
  }

  /** override this for sorted sets */
  protected Set<E> createAddedSet() {
    return new HashSet<E>(5);
  }

  /** called by privateAdd */
  private void addToAddedList( E o ) {
    if (myAddedSet == null) myAddedSet = createAddedSet();
    myAddedSet.add(o);
  }
  /** called by privateRemove */
  private void addToRemovedList( E o ) {
    if (myRemovedList == null) myRemovedList = new ArrayList<E>(3);
    myRemovedList.add( o );
  }

  @Override
protected void privateAdd(E o, boolean isVisible) { 
    super.privateAdd(o, isVisible);
    if (isVisible) {
      setChanged(true);
      addToAddedList(o);
    }
  }
  @Override
protected void privateRemove(E o, boolean isVisible) {
    super.privateRemove(o, isVisible);
    if (isVisible) {
      setChanged(true);
      addToRemovedList(o);
    }
  }

  @Override
protected void privateChange(E o, List<ChangeReport> changes, boolean isVisible) {
    if (isVisible) {
      setChanged(true);
      super.privateChange(o, changes, true);
    }
  }
}
