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

import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Request an agent management operation, as defined in the
 * {@link #getAbstractTicket()}.
 * <p>
 * Abstract tickets allow add / remove / move of agents.
 *
 * @see MoveAgent backwards compatible API for agent mobility.
 */
public interface AgentControl extends UniqueObject {
  
   /**
    * Status codes:
    */
  int NONE = 1;
  int CREATED = 2;
  int ALREADY_EXISTS = 3;
  int REMOVED = 4;
  int DOES_NOT_EXIST = 5;
  int MOVED = 6;
  int ALREADY_MOVED = 7;
  int FAILURE = 8;
  
  
  /**
   * UID support from unique-object.
   */
  UID getUID();
  
  /**
   * Get the optional UID of the object that "owns" this 
   * agent control request.
   */
  UID getOwnerUID();
  
  /**
   * Get the agent that created this request.
   */
  MessageAddress getSource();
  
  /**
   * Get the agent that should perform the operation.
   * <p>
   * For agent mobility this should be eithe the mobile agent itself
   * or the node containing the mobile agent.  This must
   * agree with the ticket.
   * <p>
   * Note that the behavior is different depending upon
   * the target.  If the node is specified then the 
   * request is not passed through the agent.
   */
  MessageAddress getTarget();

  /**
   * The addresses specified in the ticket are not 
   * null.
   */
  AbstractTicket getAbstractTicket();

  /**
   * Get the move status code, which is one of the above
   * "*_STATUS" constants.
   * <p>
   * Initially the status is "NO_STATUS".
   */
  int getStatusCode();

  /**
   * Get a string representation of the status.
   */
  String getStatusCodeAsString();
  
  /**
   * If (getStatusCode() == FAILED_STATUS), this is
   * the failure exception.
   */
  Throwable getFailureStackTrace();
  
  /**
   * For infrastructure use only!  Set the status.
   */
  void setStatus(int statusCode, Throwable stack);

}
