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

package org.cougaar.core.mobility.ldm;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * A {@link Factory} for creating {@link AgentControl} objects.
 */
public interface MobilityFactory extends Factory {

  /**
   * Get a new ticket identifier for use when creating
   * a Ticket.
   */
  Object createTicketIdentifier();

  /**
   * Create a request that an agent be added/removed/moved, 
   * which is sent directly to the specified target.
   * <p>
   * Add requests must specify the node address as the 
   * target.
   * <p>
   * Remove requests must specify either the agent or its
   * node as the target.
   * <p>
   * Move requests must specify either null, the agent,
   * or the node running the agent that is to be moved.
   * If the agent is specified, the request first
   * passes through the agent (RedirectMovePlugin).
   */
  AgentControl createAgentControl(
      UID ownerUID,
      MessageAddress target,
      AbstractTicket abstractTicket);


  // old API


  /**
   * Create an agent move request -- this has been
   * replaced by the "AgentControl" APIs, but is still
   * supported.
   * 
   * @param ticket must have a ticket identifier that
   *    was created by the "createTicketIdentifier()"
   *    method.
   *
   * @see AgentControl new API
   */
  MoveAgent createMoveAgent(Ticket ticket);

}
