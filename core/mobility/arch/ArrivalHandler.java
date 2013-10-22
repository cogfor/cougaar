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

import org.cougaar.core.component.ComponentDescription;

/**
 * Handle a request to accept a mobile agent at the target node. 
 */
public class ArrivalHandler extends AbstractHandler {

  private ComponentDescription desc;

  public ArrivalHandler(
      MobilitySupport support,
      ComponentDescription desc) {
    super(support);
    this.desc = desc;
  }

  @Override
public void run() {
    arrival();
  }

  private void arrival() {

    // FIXME race condition between move & agent-add!

    // FIXME do handshake

    if (log.isInfoEnabled()) {
      log.info(
          "Received request to add agent "+id+
          ", which is moving from node "+
          moveTicket.getOriginNode()+
          " to local node "+nodeId);
    }

    try {

      addAgent(desc);

    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to add moved agent "+id+" to node "+nodeId, 
            e);
      }

      sendNack(e);

      return;
    }

    try {

      sendAck();

    } catch (Exception e) {

      // too late now!

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to send acknowledgement for"+
            " move of agent "+id+" to node "+nodeId, e);
      }

    }

    if (log.isInfoEnabled()) {
      log.info(
          "Sent acknowledgement back to node "+
          moveTicket.getOriginNode()+
          ": agent "+id+" has successfully moved to node "+
          nodeId);
    }

  }

  @Override
protected void addAgent(ComponentDescription desc) {
    if (log.isInfoEnabled()) {
      log.info("Add   agent "+id);
    }
    super.addAgent(desc);
    if (log.isInfoEnabled()) {
      log.info("Added agent "+id);
    }
  }

  @Override
public String toString() {
    return "Move (arrival) of agent "+id;
  }
}
