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

package org.cougaar.core.wp.server;

import java.util.Map;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service sends "forward"s to replicate leases between servers.
 * <p>
 * This API hides the MTS and messaging details.  In particular,
 * the transport selects which WP server(s) the lookups should
 * be sent to, aggregates results if necessary, and retries any
 * failed deliveries.
 * <p>
 * The service requestor must implement the Client API.
 */
public interface ForwardService extends Service {

  /**
   * Forward entries to all our peer servers.
   * <p>
   * The map keys are Strings and the values are Forward objects.
   * <p>
   * Like the ModifyService, the "forwardAnswer" can send back a
   * LeaseNotKnown response if the UID is not known (e.g. due to a
   * server restart), in which case the server should send the full
   * Record.  A LeaseDenied is never sent because this is typically
   * a race condition -- the remote server's "forward" is likely
   * on the wire and will correct the sender's tables.  Lastly, a
   * success Lease is not necessary since a lack of acknowledgement
   * is assumed to be an acceptance of the forwarded data.
   * <p>
   * Clients may see temporary inconsistencies due to propagation 
   * delays, but these should be minimal.  Races between clients
   * are resolved by deconflicting the entries based upon the
   * optional "version" entries in each record.
   * <p>
   * Larger inconsistencies may occur due to network partitions or
   * server crashes.  These conflicts are eventually remedied with
   * the server-side deconfliction code and periodic lease
   * renewals.  Additionally, as an optional optimization, a server
   * can detect another server's crash and forward a full copy of
   * its data to that server, by using the other "forward" method.
   * <p>
   * It's fine to submit a singleton map, but for efficiency a
   * client can use this API to batch requests.
   */
  void forward(Map m, long ttd);

  /**
   * Reply to a "forwardAnswer" LeaseNotKnown by sending a Forward
   * to our peer.
   */
  void forward(MessageAddress target, Map m, long ttd);

  interface Client {
    /** Receive the answer to a forward request. */
    void forwardAnswer(MessageAddress addr, long baseTime, Map m);
  }
}
