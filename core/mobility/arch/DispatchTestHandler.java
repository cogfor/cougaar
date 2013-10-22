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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.MobilityClient;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mobility.service.LocalMoveState;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.GenericStateModel;

/**
 * Handle a request to restart the agent on the local node, cycling
 * it as if it was moved. 
 */
public class DispatchTestHandler extends AbstractHandler {

  private GenericStateModel model;
  private ComponentDescription desc;
  private MobilityClient stateProvider;
  private LocalMoveState localMoveState;

  public DispatchTestHandler(
      MobilitySupport support,
      GenericStateModel model,
      ComponentDescription desc,
      MobilityClient stateProvider,
      LocalMoveState localMoveState) {
    super(support);
    this.model = model;
    this.desc = desc;
    this.stateProvider = stateProvider;
    this.localMoveState = localMoveState;
  }

  @Override
public void run() {
    dispatchTest();
  }

  private void dispatchTest() {

    Object state;
    boolean didSuspend = false;
    try {

      checkTicket();

      if (log.isInfoEnabled()) {
        log.info(
            "Begin local restart of agent "+id+" on node "+
            nodeId);
      }

      onDispatch();

      suspendAgent();
      didSuspend = true;

      state = getAgentState();
      state = testSerial(state, "state");

    } catch (Exception e) {
      // obtaining the state shouldn't mangle the agent, so we'll
      // attempt to resume the agent from its suspension.

      if (log.isErrorEnabled()) {
        log.error("Failed agent "+id+" forced restart (capture)", e);
      }
      if (didSuspend) {
        model.resume();
        if (log.isErrorEnabled()) {
          log.error("Resumed agent "+id);
        }
      }
      onFailure(e);
      return;
    }

    try {
      stopAgent();
      unloadAgent();
      removeAgent();
    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error("Failed agent "+id+" forced restart (remove)", e);
      }

      onFailure(e);
      return;
    }

    localMoveState.setState(state);
    state = null;

    try {
      addAgent(desc);

      onArrival();
    } catch (Exception e) {

      if (log.isErrorEnabled()) {
        log.error("Failed agent "+id+" forced restart (add)", e);
      }

      onFailure(e);
      return;
    }


    if (log.isInfoEnabled()) {
      log.info(
          "Agent "+id+" has successfully restarted on node "+
          nodeId);
    }

  }

  private void checkTicket() {

    // check for restart
    if (!(moveTicket.isForceRestart())) {
      throw new MobilityException(
          "Test dispatch on a non force-restart?");
    }

    // check for local destination
    MessageAddress destNode = moveTicket.getDestinationNode();
    if ((destNode != null) &&
        (!(destNode.equals(nodeId)))) {
      throw new MobilityException(
          "Test dispatch on a non-local destination "+destNode);
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
      // FIXME now what?
      //
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

  private void stopAgent() {
    if (log.isInfoEnabled()) {
      log.info("Stop      agent "+id);
    }
    model.stop();
    if (log.isInfoEnabled()) {
      log.info("Stopped   agent "+id);
    }
  }

  private void unloadAgent() {
    if (log.isInfoEnabled()) {
      log.info("Unload    agent "+id);
    }
    model.unload();
    if (log.isInfoEnabled()) {
      log.info("Unloaded  agent "+id);
    }
  }

  @Override
protected void addAgent(ComponentDescription desc) {
    if (log.isInfoEnabled()) {
      log.info("Add       agent "+id);
    }
    super.addAgent(desc);
    if (log.isInfoEnabled()) {
      log.info("Added     agent "+id);
    }
  }

  @Override
protected void removeAgent() {
    if (log.isInfoEnabled()) {
      log.info("Remove    agent "+id);
    }
    super.removeAgent();
    if (log.isInfoEnabled()) {
      log.info("Removed   agent "+id);
    }
  }

  private Object testSerial(Object o, String type) {
    try {
      if (log.isInfoEnabled()) {
        log.info("Serialize    "+type+" of agent "+id);
      }
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(bos);
      os.writeObject(o);
      os.flush();
      if (log.isInfoEnabled()) {
        log.info("Serialized   "+type+" of agent "+id);
      }
      o = null;
      if (log.isInfoEnabled()) {
        log.info("Deserialize  "+type+" of agent "+id);
      }
      byte[] b = bos.toByteArray();
      ByteArrayInputStream bis = new ByteArrayInputStream(b);
      ObjectInputStream is = new ObjectInputStream(bis);
      Object newO = is.readObject();
      if (log.isInfoEnabled()) {
        log.info("Deserialized "+type+" of agent "+id);
      }
      return newO;
    } catch (Exception e) {
      throw new MobilityException(
          "Serialize/Deserialize ("+type+") test on agent "+
          id+
          " description failed, will attempt resume",
          e);
    }
  }

  @Override
public String toString() {
    return "Move (dispatch-test) of agent "+id;
  }
}
