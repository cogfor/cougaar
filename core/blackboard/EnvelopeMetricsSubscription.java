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
import java.util.Enumeration;
import java.util.List;

import org.cougaar.util.Empty;
import org.cougaar.util.Enumerator;

/**
 * A {@link Subscription} that collects {@link EnvelopeMetrics}.
 *
 * @see Subscriber the "timestamp" system property must be enabled
 */
public class EnvelopeMetricsSubscription extends Subscription {

  private final boolean includeBlackboard;
  private final List myList = new ArrayList(5);

  public EnvelopeMetricsSubscription() {
    this(true);
  }

  public EnvelopeMetricsSubscription(boolean includeBlackboard) {
    super(null);
    this.includeBlackboard = includeBlackboard;
  }

  @Override
protected void resetChanges() {
    super.resetChanges();
    myList.clear();
  }

  /**
   * @return an enumeration of EnvelopeMetrics that have been added
   * since the last transaction.
   */
  public Enumeration getAddedList() {
    checkTransactionOK("getAddedList()");
    if (myList.isEmpty()) return Empty.enumeration;
    return new Enumerator(myList);
  }

  /** 
   * @return a possibly empty collection of EnvelopeMetrics that have
   * been added since the last transaction. Will not return null.
   */
  public Collection getAddedCollection() {
    return myList;
  }

  @Override
public boolean apply(Envelope e) {
    if (!(e instanceof TimestampedEnvelope)) {
      return false;
    }
    TimestampedEnvelope te = (TimestampedEnvelope) e;
    if ((!includeBlackboard) && te.isBlackboard()) {
      return false;
    }
    EnvelopeMetrics em = new EnvelopeMetrics(te);
    myList.add(em);
    setChanged(true);
    return true;
  }

  // never called, due to "apply(..)" override:
  @Override
protected void privateAdd(Object o, boolean isVisible) { }
  @Override
protected void privateRemove(Object o, boolean isVisible) { }
  @Override
protected void privateChange(Object o, List changes, boolean isVisible) { }

}
