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
 * A "successful lease" response from the {@link ClientTransport}'s
 * {@link ModifyService}, indicating to the {@link LeaseManager}
 * that either a new {@link Record} was successfully bound or an
 * existing {@link Lease} was extended.
 * <p>
 * The LeaseManager client must renew this lease before it expires,
 * otherwise the server(s) will automatically remove it.
 * <p>
 * Renewals can pass the UID of the Record, as documented in
 * the ModifyService.
 * <p>
 * A relative "TTD" for the expiration data is also specified,
 * which is relative to the base time passed by the
 * "modifyAnswer" method.
 */
public final class Lease implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
  private final long ttd;

  public Lease(UID uid, long ttd) {
    this.uid = uid;
    this.ttd = ttd;
    // validate
    String s =
      ((uid == null) ? "null uid" :
       (ttd < 0) ? "negative ttd" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The UID of the lease, as selected by the View.
   * <p>
   * This is the "in response to" field.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The lease time-to-death relative to the base timestamp.
   * <p>
   * This is the lease expiration date.  A negative number
   * indicates a failed bind or renewal, in which case the
   * result field will be non-null.
   */
  public long getTTD() {
    return ttd;
  }

  @Override
public String toString() {
    return "(lease uid="+uid+" ttd="+ttd+")";
  }

  public String toString(long baseTime, long now) {
    long ttl = baseTime + ttd;
    return 
      "(lease uid="+uid+
      " ttd="+ttd+
      " ttl="+(0 < ttl ? Timestamp.toString(ttl, now) : "N/A")+
      ")";
  }
}
