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

package org.cougaar.core.mobility.arch;

import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mts.MessageAddress;

/**
 * Handle a trivial request for move of an agent that's already at
 * the target node.
 */
public class DispatchNoopHandler extends AbstractHandler {

  public DispatchNoopHandler(MobilitySupport support) {
    super(support);
  }

  @Override
public void run() {
    dispatchNoop();
  }

  private void dispatchNoop() {

    try {

      if (log.isInfoEnabled()) {
        log.info("Initiate agent "+id+" \"no-op\" dispatch");
      }

      checkTicket();

      // simply inform the agent
      onDispatch();
      onArrival();

    } catch (Exception e) {

      // the agent should be okay

      if (log.isErrorEnabled()) {
        log.error("Failed agent "+id+" \"no-op\" dispatch", e);
      }
      onFailure(e);
      return;
    }

    if (log.isInfoEnabled()) {
      log.info("Completed agent "+id+" \"no-op\" dispatch");
    }

  }

  private void checkTicket() {

    if (log.isDebugEnabled()) {
      log.debug(
          "Check dispatch ticket on node "+nodeId+
          " of agent "+id+" and ticket "+moveTicket);
    }

    // check for non-restart
    if (moveTicket.isForceRestart()) {
      throw new MobilityException(
          "Noop dispatch on a force-restart?");
    }

    // check for local
    MessageAddress destNode = moveTicket.getDestinationNode();
    if ((destNode != null) &&
        (!(destNode.equals(nodeId)))) {
      throw new MobilityException(
          "Noop dispatch on a non-local destination "+destNode);
    }

    // check agent assertion
    MessageAddress mobileAgent = moveTicket.getMobileAgent();
    if ((mobileAgent != null) &&
        (!(mobileAgent.equals(id)))) {
      throw new MobilityException(
          "Move agent "+id+
          " doesn't match ticket agent "+mobileAgent);
    }

    // check origin assertion
    MessageAddress originNode = moveTicket.getOriginNode();
    if ((originNode != null) &&
        (!(originNode.equals(nodeId)))) {
      throw new MobilityException(
          "Move origin "+nodeId+" for "+id+
          " doesn't match ticket origin "+originNode);
    }
  }

  @Override
public String toString() {
    return "Move (dispatch-noop) of agent "+id;
  }
}
