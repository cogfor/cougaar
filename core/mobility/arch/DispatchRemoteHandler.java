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
import org.cougaar.core.mobility.MobilityClient;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.GenericStateModel;

/**
 * Handle a request to send an agent to a remote node. 
 * <p>
 * This does half the work, since the ack/nack will be pending. 
 */
public class DispatchRemoteHandler extends AbstractHandler {

  private GenericStateModel model;
  private ComponentDescription desc;
  private MobilityClient stateProvider;

  public DispatchRemoteHandler(
      MobilitySupport support,
      GenericStateModel model,
      ComponentDescription desc,
      MobilityClient stateProvider) {
    super(support);
    this.model = model;
    this.desc = desc;
    this.stateProvider = stateProvider;
  }

  @Override
public void run() {
    dispatchRemote();
  }

  private void dispatchRemote() {

    Object state;
    boolean didSuspend = false;
    try {

      checkTicket();

      if (log.isInfoEnabled()) {
        log.info(
            "Begin move of agent "+id+" from "+
            nodeId+" to "+moveTicket.getDestinationNode()+
            ", move id is "+moveTicket.getIdentifier());
      }

      onDispatch();

      suspendAgent();
      didSuspend = true;

      state = getAgentState();

    } catch (Exception e) {

      // obtaining the state shouldn't mangle the agent, so we'll
      // attempt to resume the agent from its suspension.

      if (log.isErrorEnabled()) {
        log.error(
            "Unable to move agent "+id, e);
      }
      if (didSuspend) {
        model.resume();
      }

      onFailure(e);

      return;
    }

    try {

      sendTransfer(desc, state);
      state = null; 

    } catch (Exception e) {
      state = null; 

      // not sure if the message was sent...
      //
      // attempt to resume the agent from its suspension.

      if (log.isErrorEnabled()) {
        log.error(
            "Failed message delivery for agent transfer", e);
      }

      model.resume();

      onFailure(e);

      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Transfering agent "+id+" to node "+
          moveTicket.getDestinationNode()+
          ", waiting for an acknowledgement from node "+
          moveTicket.getDestinationNode());
    }

  }

  private void checkTicket() {

    // check for non-local destination
    MessageAddress destNode = moveTicket.getDestinationNode();
    if ((destNode == null) ||
        (destNode.equals(nodeId))) {
      throw new InternalError(
          "Remote dispatch on a local destination "+destNode);
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

  private void suspendAgent() {
    if (log.isInfoEnabled()) {
      log.info("Suspend   agent "+id);
    }

    model.suspend();

    if (log.isInfoEnabled()) {
      log.info("Suspended agent "+id);
    }
  }

  private Object getAgentState() {
    // capture the agent state

    if (stateProvider == null) {
      if (log.isWarnEnabled()) {
        log.warn("Agent "+id+" has no state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Capture  state for agent "+id);
    }

    Object state;
    try {
      state = stateProvider.getState();
    } catch (Exception e) {
      throw new MobilityException(
            "Unable to capture state for agent "+id+
            ", will attempt resume", e);
    }

    if (log.isInfoEnabled()) {
      // FIXME maybe not log this -- state may be very verbose!
      log.info("Captured state for agent "+id+": "+state);
    }

    return state;
  }

  @Override
public String toString() {
    return "Move (dispatch-remote) of agent "+id;
  }
}
