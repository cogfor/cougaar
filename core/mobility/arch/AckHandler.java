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

import org.cougaar.util.GenericStateModel;

/**
 * Handle an acknowledged move. 
 */
public class AckHandler extends AbstractHandler {

  private GenericStateModel model;

  public AckHandler(
      MobilitySupport support,
      GenericStateModel model) {
    super(support);
    this.model = model;
  }

  @Override
public void run() {
    ack();
  }

  /**
   * Received an ACK response from the destination node.
   */
  private void ack() {

    // FIXME race condition between move & agent-add!

    // FIXME do handshake

    if (log.isInfoEnabled()) {
      log.info(
          "Received acknowledgement from node "+
          moveTicket.getDestinationNode()+
          " for move of agent "+id);
    }

    try {

      stopAgent();
      unloadAgent();
      removeAgent();

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error("Agent removal after move failed", e);
      }

    }

    // fill in success
    //
    // note that this travels from the origin node 
    // to the just-moved agent on the remote destination 
    // node.

    onArrival();

    // agent will be GC'ed now

    if (log.isInfoEnabled()) {
      log.info(
          "Agent "+id+" has successfully moved to node "+
          moveTicket.getDestinationNode()+
          " and has been removed from node "+nodeId);
    }

  }

  private void stopAgent() {
    if (log.isInfoEnabled()) {
      log.info("Stop     agent "+id);
    }
    model.stop();
    if (log.isInfoEnabled()) {
      log.info("Stopped  agent "+id);
    }
  }

  private void unloadAgent() {
    if (log.isInfoEnabled()) {
      log.info("Unload   agent "+id);
    }
    model.unload();
    if (log.isInfoEnabled()) {
      log.info("Unloaded agent "+id);
    }
  }

  @Override
protected void removeAgent() {
    if (log.isInfoEnabled()) {
      log.info("Remove   agent "+id);
    }
    super.removeAgent();
    if (log.isInfoEnabled()) {
      log.info("Removed  agent "+id);
    }
  }

  @Override
public String toString() {
    return "Move (ack) of agent "+id;
  }
}
