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

import java.io.Serializable;

import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A request that an agent be moved from its current node 
 * to a different node.
 * <p>
 * This has been replaced by the "AgentControl" API, but 
 * there's no rush to switch to the new API.
 *
 * @see AgentControl
 */
public interface MoveAgent extends UniqueObject {

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Address of the agent that requested the move.
   */
  MessageAddress getSource();

  /**
   * Get the move ticket.
   * <p>
   * If the ticket's "getMobileAgent()" is null then
   * the requesting agent will be moved (a "rover" agent).
   * 
   * @see Ticket
   */
  Ticket getTicket();

  // maybe add "abort" here

  /**
   * Get the current status to the request.
   */
  Status getStatus();

  /**
   * For infrastructure use only!.
   * Allows the infrastructure to set the status.
   * Only valid if (local-agent == req.agentToMove)
   */
  void setStatus(Status status);


  /**
   * Immutable class that represents the dynamic status 
   * of the move request.
   */
  final class Status implements Serializable {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   /**
     * Status codes:
     */
    public static final int OKAY = 200;
    public static final int FAILURE = 500;
    // add more status-codes here

    private final int code;
    private final String message;
    private final Throwable throwable;

    public Status(int code, String message) {
      this(code, message, null);
    }

    public Status(int code, String message, Throwable throwable) {
      this.code = code;
      this.message = message;
      this.throwable = throwable;
    }

    public int getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public Throwable getThrowable() {
      return throwable;
    }

    public String getCodeAsString() {
      switch (code) {
        case OKAY: return "Okay ("+OKAY+")";
        case FAILURE: return "Failure ("+FAILURE+")";
        default: return "Unknown ("+code+")";
      }
    }

    @Override
   public int hashCode() {
      int hc = code;
      if (message != null) hc += message.hashCode();
      return hc;
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Status)) {
        return false;
      } else {
        Status s = (Status) o;
        return 
          ((code == s.code) &&
           ((message == null) ? 
            (s.message == null) :
            (message.equals(s.message))) &&
           ((throwable == null) ? 
            (s.throwable == null) :
            (throwable.equals(s.throwable))));
      }
    }
    @Override
   public String toString() {
      return 
        (getCodeAsString()+" "+
         message+
         ((throwable != null) ? (throwable.getMessage()) : ""));
    }
  }

}
