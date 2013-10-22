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
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Base class for mobility-related handlers.
 */
public abstract class AbstractHandler implements Runnable {

  protected final MobilitySupport support;

  protected final MessageAddress id;
  protected final MessageAddress nodeId;
  protected final MoveTicket moveTicket;
  protected final LoggingService log;

  public AbstractHandler(MobilitySupport support) {
    this.support = support;
    // save these for easy base-class access
    this.id = support.getId();
    this.nodeId = support.getNodeId();
    this.moveTicket = support.getTicket();
    this.log = support.getLog();
  }

  public abstract void run();

  // msg-sender

  protected void sendTransfer(
      ComponentDescription desc,
      Object state) {
    support.sendTransfer(desc, state);
  }

  protected void sendAck() {
    support.sendAck();
  }

  protected void sendNack(Throwable throwable) {
    support.sendNack(throwable);
  }

  // agent-container

  protected void addAgent(ComponentDescription desc) {
    support.addAgent(desc);
  }

  protected void removeAgent() {
    support.removeAgent();
  }

  // detachable mobility-listener

  protected void onDispatch() {
    support.onDispatch();
  }

  protected void onArrival() {
    support.onArrival();
  }

  protected void onFailure(Throwable throwable) {
    support.onFailure(throwable);
  }

  // to-string

  @Override
public String toString() {
    return "Move (?) for agent "+id;
  }
}
