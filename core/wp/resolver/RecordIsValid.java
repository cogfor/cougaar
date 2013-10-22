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
 * A data response from a successful {@link ClientTransport} {@link
 * LookupService} lookup to the {@link CacheManager}, which
 * validates the client's cached {@link Record} and extends the TTL
 * for the cached data.
 * <p>
 * This is only used if the client passed a non-null UID in the
 * lookup.
 * <p>
 * The client sent the UID of the Record, and this "RecordIsValid"
 * confirms that the Record with that UID hasn't changed and
 * permits the client to cache the Record for a little longer.
 * <p>
 * It's possible for a client to evict the entry before the
 * "record is valid" is received, e.g. due to a cache size limit.
 * In this situation the client should send a new lookup (null-UID),
 * which will incur a second lookup delay.
 */
public final class RecordIsValid implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
  private final long ttd;

  public RecordIsValid(UID uid, long ttd) {
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

  /** The UID of the validated Record. */
  public UID getUID() {
    return uid;
  }

  /**
   * The expiration "time-to-death" relative to the base timestamp.
   */
  public long getTTD() {
    return ttd;
  }

  @Override
public String toString() {
    return "(record-is-valid uid="+uid+" ttd="+ttd+")";
  }

  public String toString(long baseTime, long now) {
    long ttl = baseTime + ttd;
    return 
      "(record-is-valid uid="+uid+
      " ttd="+ttd+
      " ttl="+Timestamp.toString(ttl, now)+
      ")";
  }
}
