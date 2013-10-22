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

import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Package-private implementation of {@link MoveAgent}.
 * <p>
 * Backwards compatibility for the old MoveAgent API.
 */
class MoveAgentAdapter
extends AgentControlImpl 
implements MoveAgent {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private Status myStatus;

  public MoveAgentAdapter(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      Ticket ticket) {
    super(uid, ownerUID, source, target, ticket);
  }

  public MoveAgentAdapter(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      Ticket ticket) {
    super(uid, null, source, target, ticket);
  }

  public Ticket getTicket() {
    return (Ticket) getAbstractTicket();
  }

  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(Status xStatus) {
    if (xStatus != null) {
      setStatus(
          ((xStatus.getCode() == Status.OKAY) ? 
           (MOVED) : 
           FAILURE),
          xStatus.getThrowable());
    }
  }

  @Override
public void setStatus(int status, Throwable stack) {
    super.setStatus(status, stack);
    setMyStatus(status, stack);
  }

  @Override
public int updateResponse(
      MessageAddress t, Object response) {
    int ret = super.updateResponse(t, response);
    if (ret != Relay.NO_CHANGE) {
      setMyStatus(getStatusCode(), getFailureStackTrace());
    }
    return ret;
  }

  private void setMyStatus(int status, Throwable stack) {
    if (status == NONE) {
      myStatus = null;
    } else if (status == MOVED) {
      myStatus = new Status(
          Status.OKAY, 
          ("Agent arrived at time "+System.currentTimeMillis()), 
          stack);
    } else {
      myStatus = new Status(
          Status.FAILURE, 
          ("Failed at time "+System.currentTimeMillis()),
          stack);
    }
  }

}
