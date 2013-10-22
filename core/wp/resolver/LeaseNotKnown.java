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

/**
 * An "unknown lease uid" response from the {@link ClientTransport}'s
 * {@link ModifyService}, indicating to the {@link LeaseManager} that
 * an attempt to renew a lease failed because the server doesn't know
 * a lease with the specified UID.
 * <p>
 * If a client attempts to renew a Lease by passing the UID, and
 * the server doesn't know the UID, then this response is
 * returned to request the full Record of data.
 * <p>
 * This is used to cover two cases:<ol>
 *   <li>The server expired the lease due to lack of renewal
 *       (e.g. network partition) and the client must remind
 *       the server of the data</li>
 *   <li>The server has crashed and must be reconciled.</li>
 * </ol>
 * <p>
 * The Record should use the same UID as the Lease.  This will
 * ensure that duplicate or out-of-order messages will not
 * cause problems.
 */
public final class LeaseNotKnown implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;

  public LeaseNotKnown(UID uid) {
    this.uid = uid;
    // validate
    String s = 
      ((uid == null) ? "null uid" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  public UID getUID() {
    return uid;
  }

  @Override
public String toString() {
    return "(lease-not-known uid="+uid+")";
  }
}
