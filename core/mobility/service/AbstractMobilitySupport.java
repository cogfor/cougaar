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

package org.cougaar.core.mobility.service;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.arch.MobilitySupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Implementation of the {@link MobilitySupport} interface. 
 */
abstract class AbstractMobilitySupport
implements MobilitySupport 
{

  protected final MessageAddress id;
  protected final MessageAddress nodeId;
  protected final MoveTicket moveTicket;
  protected final LoggingService log;

  public AbstractMobilitySupport(
      MessageAddress id,
      MessageAddress nodeId,
      MoveTicket moveTicket,
      LoggingService log) {
    this.id = id;
    this.nodeId = nodeId;
    this.moveTicket = moveTicket;
    this.log = log;
  }

  // fields

  public LoggingService getLog() {
    return log;
  }

  public MessageAddress getId() {
    return id;
  }

  public MessageAddress getNodeId() {
    return nodeId;
  }

  public MoveTicket getTicket() {
    return moveTicket;
  }

  // model-reg

  // agent-container

  public abstract void addAgent(ComponentDescription desc);

  public abstract void removeAgent();

  @Override
public String toString() {
    return "Mobility support for agent "+id+" on "+nodeId;
  }

}
