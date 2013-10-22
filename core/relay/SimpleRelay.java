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

package org.cougaar.core.relay;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A blackboard object API to send a query object to a remote agent,
 * which can optionally reply back to the sender.
 * <p>
 * Multiple rounds of query/reply are supported.
 *
 * @see SimpleRelaySource source-side relay implementation 
 */
public interface SimpleRelay extends UniqueObject {

  // from UniqueObject 
  UID getUID();
  void setUID(UID uid);

  /** The agent that created the query */
  MessageAddress getSource();

  /** The agent that will set the reply */
  MessageAddress getTarget();

  /** Get the query contents */
  Object getQuery();

  /** 
   * Change the query, which is optional.
   * <p> 
   * Only the agent who's address matches "getSource()" may call
   * this method, which must be followed by a blackboard
   * "publishChange" to resend the query to the target.
   * <p>
   * Note that the relay implementation may batch changes and
   * only send the latest change.
   *
   * @param query the immutable query object 
   */ 
  void setQuery(Object query);

  /**
   * Get the remote reply to the query, if any.
   * <p>
   * Blackboard interactions are asynchronous, so the plugin should
   * use a subscription to wake when the SimpleRelay has changed.
   */
  Object getReply();

  /** 
   * Change the reply, which is optional.
   * <p>
   * Only the agent who's address matches "getTarget()" should call
   * this method, which must be followed by a blackboard
   * "publishChange" to resend the reply to the target.
   * <p>
   * Note that the relay implementation may batch changes and
   * only send the latest change.
   *
   * @param reply the immutable reply object 
   */ 
  void setReply(Object reply);

}
