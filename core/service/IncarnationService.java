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

import java.util.Map;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service tracks agent incarnation numbers, which are
 * updated every time the agent restarts (excluding agent
 * mobility).
 * <p>
 * This is used to detect restarted agents and reconcile
 * with them.
 */
public interface IncarnationService extends Service {

  /**
   * @return the agent incarnation, or -1 if the agent or its
   * incarnation is not known.
   */
  long getIncarnation(MessageAddress addr);

  /**
   * Update an agent's incarnation.
   * @return 0 if unchanged, -1 if the incarnation is old,
   * or 1 if the incarnation is new (which will invoke the
   * callbacks in the caller's thread)
   */
  int updateIncarnation(MessageAddress addr, long inc);

  /**
   * Subscribe to incarnation change callbacks.
   * Equivalent to <code>subscribe(addr, cb, 0)</code>.
   * @return true if the callback was not already subscribed 
   */
  boolean subscribe(MessageAddress addr, Callback cb);

  /**
   * Subscribe to incarnation change callbacks with an initial
   * minimal incarnation value filter.
   * <p>
   * The incarnation is used to restore a subscription after
   * capturing it from {@link #getIncarnation(MessageAddress)},
   * specifically for mobile agents.
   *
   * @return true if the callback was not already subscribed 
   */
  boolean subscribe(MessageAddress addr, Callback cb, long inc);

  /**
   * Unsubscribe from incarnation change callbacks.
   * @return true if the callback was subscribed 
   */
  boolean unsubscribe(MessageAddress addr, Callback cb);

  /**
   * Get a map of our subscriptions (addrs to callbacks).
   *
   * @return Map&gt;MessageAddress, Set&gt;Callback&lt;&lt;
   */
  Map getSubscriptions();

  /**
   * Subscription callback API.
   * <p>
   * Callback instances not implementing {@link Comparable} are
   * invoked first, followed by the {@link Comparable} instances
   * in sorted order.
   */
  interface Callback {
    /**
     * The incarnation for the specified agent has changed
     * to the new <code>inc</code> value.
     */
    void incarnationChanged(MessageAddress addr, long inc);
  }
}
