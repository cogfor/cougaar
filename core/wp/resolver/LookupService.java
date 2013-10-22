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

import java.util.Map;

import org.cougaar.core.component.Service;

/**
 * This service is the {@link ClientTransport}'s interface to the
 * {@link CacheManager} for lookup messaging.
 * <p>
 * This API hides the MTS and messaging details.  In particular,
 * the transport selects which WP server(s) the lookups should
 * be sent to, aggregates results if necessary, and retries any
 * failed deliveries.
 * <p>
 * The service requestor must implement the Client API.
 */
public interface LookupService extends Service {

  /**
   * Lookup entries in the server(s).
   * <p>
   * The map is from String keys to UIDs.  The key is the "getAll"
   * name or "list" suffix.  If the client has a cached Record
   * from a prior lookup then it can pass the UID of that Record
   * to request a RecordIsValid response, otherwise it should pass
   * a null UID.  If the current record's UID doesn't match
   * the UID then a full Record will be sent back.
   * <p>
   * It's fine to submit a singleton map, but for efficiency a
   * client can use this API to batch requests.
   */
  void lookup(Map m);

  interface Client {
    /**
     * Respond to a lookup request.
     * <p>
     * The map is from String keys to either Record or RecordIsValid
     * objects.
     * <p>
     * RecordIsValid objects are used for lookups that specified a
     * UID to validate a cached Record, otherwise Record objects
     * are used to provide the full data (e.g. the AddressEntries
     * for a "getAll" lookup).
     * <p>
     * The values contain relative timestamp offsets for the
     * cache expiration time (e.g. "+5000 millis"), so the baseTime
     * is also specified.
     */
    void lookupAnswer(long baseTime, Map m);
  }
}
