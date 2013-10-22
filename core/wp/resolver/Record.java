/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.io.Serializable;

import org.cougaar.core.util.UID;
import org.cougaar.core.wp.Timestamp;

/**
 * A data response from a successful {@link LookupService} lookup,
 * or a request parameter to a {@link ModifyService} lease renewal.
 * <p>
 * The UID is specific to this view of the data.  If the server
 * changes the data then the server-side UID for the record will
 * also change.
 * <p>
 * This is used by the LookupService in two cases:<ol>
 *   <li>The client passed a null UID, indicating a lookup
 *       where the client lacks a cached Record.</li>
 *   <li>The client passed a non-null UID to validate that
 *       the Record hadn't changed, but the record <i>has</i>
 *       changed and the client must replace the cached
 *       data.</li>
 * </ol>  
 * <p>
 * The ttd is relative to the baseTime passed by the lookup
 * callback method.
 */
public final class Record implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
  private final long ttd;
  private final Object data;

  public Record(UID uid, long ttd, Object data) {
    this.uid = uid;
    this.ttd = ttd;
    this.data = data;
  }

  /**
   * The UID of the Record.
   * <p>
   * The UID of the server's record changes whenever the data
   * changes.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The expiration "time-to-death" relative to the base timestamp,
   * or negative if this is a proposed modification.
   */
  public long getTTD() {
    return ttd;
  }

  /**
   * The data itself.
   * <p>
   * For a "getAll" this is a Map of AddressEntry objects, and for
   * a "list" this is a Set of String names.  In case of failure
   * this may be an Exception (e.g. "access denied").
   * <p>
   * The client should send its latest view of its entries.
   * Rebound entries should show their latest value, unbound
   * entries should be excluded, and already-leased values should be
   * included.
   * <p>
   * The client should send a new Record with a new UID every time
   * it changes its local data.  Renewals and retries should send
   * the current UID.
   * <p>
   * <b>NOTE:</b>
   * This design currently doesn't support "bind" operations; it
   * upgrades all binds to rebinds.  This was done to expedite the
   * white pages implementation, but should be revisited in the near
   * future.  The problem with "bind" operations is that they are
   * difficult to replicate (is "already bound" tested at one server
   * or all servers?) and difficult to retry if a server is down
   * (what if one server says "ok" and the other says "already
   * bound?).
   * <p>
   * The proposed implementation is to tag entries with a
   * "bind-only" flag, then allow any server to reject these
   * specific entries.  If a server rejects any of the "bind-only"
   * entries, and the request contains rebind or unbind operations,
   * then the client must send a new Record modification (excluding
   * the "bind-only" operations) with a new UID.  This will
   * guarantee that duplicate or lost messages won't cause
   * confusion over bind-only operations.
   * <p>
   * The reason for throwing away the failed bind modification is
   * illustrated in this example: say the record proposed two
   * actions:<pre>
   *    <i>uid=u/1</i>
   *    bind   X=Y
   *    rebind P=Q
   * </pre>
   * The request is sent to server A, which attempts to send back
   * a partial-failure that accepts "P=Q" but rejects "X=Y" due
   * to a prior binding.  At this point the network goes down, so
   * the client retries at server B, which accepts both actions
   * and tells the client the new lease.  Server B crashes, server
   * A comes back up, and the client attempts to renew its
   * lease on uid <i>u/1</i> at server A.  Server A renews just
   * "P=Q", which is the wrong partial record instead of the full
   * record data.
   */
  public Object getData() {
    return data;
  }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Record)) {
      return false;
    }
    Record r = (Record) o;
    return 
      (uid == null ?
       r.uid == null :
       uid.equals(r.uid)) &&
      ttd == r.ttd &&
      (data == null ?
       r.data == null :
       data.equals(r.data));
  }

  @Override
public int hashCode() {
    return (uid == null ? 0 : uid.hashCode());
  }

  @Override
public String toString() {
    if (ttd < 0) {
      return 
        "(new-record uid="+uid+
        " data="+data+")";
    } else {
      return 
        "(record uid="+uid+
        " ttd="+ttd+
        " data="+data+")";
    }
  }

  public String toString(long baseTime, long now) {
    if (ttd < 0) {
      return 
        "(new-record uid="+uid+
        " data="+data+")";
    } else {
      long ttl = baseTime + ttd;
      return 
        "(record uid="+uid+
        " ttd="+ttd+
        " ttl="+Timestamp.toString(ttl, now)+
        " data="+data+")";
    }
  }
}
