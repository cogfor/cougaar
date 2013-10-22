/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import org.cougaar.core.blackboard.TimestampEntry;
import org.cougaar.core.component.Service;
import org.cougaar.core.util.UID;

/** 
 * This service tracks timestamps for the ADD ({@link
 * BlackboardService#publishAdd}) and the more recent modification
 * ({@link BlackboardService#publishChange}) of {@link
 * org.cougaar.core.util.UniqueObject}s.
 * <p>
 * These timestamps are not persisted, and upon rehydration the
 * creation time of the objects will be the agent restart time.
 *
 * @property org.cougaar.core.blackboard.timestamp
 * Enable blackboard timestamps, defaults to false. 
 *
 * @see org.cougaar.core.blackboard.TimestampSubscription
 */
public interface BlackboardTimestampService extends Service {

  /**
   * @see #getTimestampEntry get the creation time
   */
  long getCreationTime(UID uid);

  /**
   * @see #getTimestampEntry get the modification time
   */
  long getModificationTime(UID uid);

  /**
   * Get the TimestampEntry for the local blackboard UniqueObject 
   * with the given UID.
   * <p>
   * The timestamps are measured in milliseconds, and matches the
   * transaction close times of the blackboard subscriber that 
   * performed the "publishAdd()" or "publishChange()".
   * <p>
   * The underlying (UID, TimestampEntry) map is dynamically 
   * maintained by a separate subscriber.  These methods are 
   * thread safe.  Clients should be aware that multiple calls
   * to "getTimestampEntry(..)" may return different responses.
   * <p>
   * The service provider of this service may restrict the set
   * of UniqueObjects covered by this service.  UniqueObjects
   * that are not covered have null TimestampEntry values.
   *
   * @return the TimestampEntry for the UniqueObject with the 
   *    specified UID, or null if not known.
   */
  TimestampEntry getTimestampEntry(UID uid);

}
