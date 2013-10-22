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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Required transaction support to record a thread's active
 * {@link Subscriber} and maintain attached ChangeReports.
 * <p>
 * May be extended to add additional functionality to a Subscriber.
 * @see org.cougaar.core.blackboard.Subscriber#newTransaction()
 */
public class Transaction {
  // instantiable stuff
  protected Subscriber subscriber;

  public Transaction(Subscriber s) {
    subscriber = s;
  }

  /** a map of object to List (of outstanding changes) */
  private Map _map = null;
  private final Map map() {
    if (_map == null) {
      _map = new HashMap(5);
    }
    return _map;
  }

  /**
   * Note a ChangeReport for the next {@link #getChangeReports}.
   * <p> 
   * May be called by anyone (inside a Transaction) wishing to
   * publish a detailed ChangeReport on an object.
   *
   * @see ActiveSubscriptionObject#changingInBlackboard 
   */
  public final static void noteChangeReport(Object o, ChangeReport cr) {
    Transaction t = getCurrentTransaction();
    if (t != null) {
      t.private_noteChangeReport(o,cr);
    } else {
      System.err.println("Warning: ChangeReport added out of transaction on "+o+
                         ":\n\t"+cr);
      Thread.dumpStack();
    }
  }

  /**
   * Note a Collection of ChangeReports for the next {@link
   * #getChangeReports}.
   * <p> 
   * May be called by anyone (inside a Transaction) wishing to publish a 
   * detailed set of ChangeReports on an object.
   *
   * @see ActiveSubscriptionObject#changingInBlackboard 
   */
  public final static void noteChangeReport(Object o, Collection c) {
    Transaction t = getCurrentTransaction();
    if (t != null) {
      t.private_noteChangeReport(o,c);
    } else {
      System.err.println("Warning: ChangeReport added out of transaction on "+o+
                         ":\n\t"+c);
      Thread.dumpStack();
    }
  }

  private final synchronized void private_noteChangeReport(Object o, ChangeReport r) {
    Map m = map();
    List changes = (List)m.get(o);
    if (changes == null) {
      changes = new ArrayList(3);
      m.put(o,changes);
    } 
    changes.add(r);
  }

  /** Bulk version of noteChangeReport */
  private final synchronized void private_noteChangeReport(Object o, Collection r) {
    Map m = map();
    List changes = (List)m.get(o);
    if (changes == null) {
      changes = new ArrayList(r.size());
      m.put(o,changes);
    } 
    changes.addAll(r);
  }

  /**
   * Used by {@link Subscriber} to take {@link ChangeReport}s noted
   * by {@link #noteChangeReport(Object,ChangeReport)} and merge them
   * with the optional change reports specified in the plugin's {@link
   * org.cougaar.core.service.BlackboardService#publishChange}.
   * <p>
   * Calling this method should atomically return the List and
   * clear the stored value.  Implementations may also want to 
   * keep track of the changing thread to avoid changing the object
   * simultaneously in different transactions. <p>
   * <p>
   * Plugins must <em>never</em> call this or changes will not be propagated.
   * <p>
   * The List returned (if any) may not be reused, as the infrastructure
   * can and will modify it for its purposes.
   *
   * @return A List of ChangeReport instances or null.  Implementations
   * are encouraged to return null if no trackable changes were made, rather
   * than an empty List.
   * @see ChangeReport
   */
  public synchronized final List getChangeReports(Object o) {
    // be careful not to create map unless we need to...
    if (_map== null) return null;
    List l = (List)_map.get(o);
    if(l!=null) {
      _map.remove(o);
    }
    return l;
  }

  /**
   * Called by {@link Subscriber} to check for changes made to objects
   * which hadn't actually been publishChanged.
   */
  synchronized final Map getChangeMap() {
    Map m = _map;
    _map = null;
    return m;
  }


  private static final ThreadLocal transactionTable = new ThreadLocal();

  /** Register a transaction as open */
  public final static void open(Transaction t) {
    Transaction o = (Transaction) transactionTable.get();
    if (o != null) {
      throw new RuntimeException("Attempt to open a nested transaction:\n"+
                                 "\tPrevious was: "+o+"\n"+
                                 "\tNext is: "+t);
    }
    transactionTable.set(t);
  }

  /** Register a transaction as closed */
  public final static void close(Transaction t) {
    Transaction o = (Transaction) transactionTable.get();
    if (o != t) {
      throw new RuntimeException("Attempt to close a transaction inappropriately:\n"+
                                 "\tPrevious was: "+o+"\n"+
                                 "\tNext is: "+t);
    }
    transactionTable.set(null);
  }

  /** get the current Transaction.  */
  public final static Transaction getCurrentTransaction() {
    return (Transaction) transactionTable.get();
  }
}
