/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.util.Set;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;

/**
 * This service is used by agents to note when they are busy in
 * an action that may block communication, such as persistence.
 * <p>
 * This service is advertised in the root service broker for
 * access by all agents on the node.
 */
public interface NodeBusyService extends Service {
  /**
   * The component using this must supply AgentIdentificationService
   * capable of correctly identifying the agent using the service
   * before it can call the setAgentBusy method.
   */
  void setAgentIdentificationService(AgentIdentificationService ais);
  /**
   * The agent using this service must have already identified itself
   * using the setAgentIdentificationService before calling this
   * method. This avoids the possibilility of misrepresenting the
   * agent.
   */
  void setAgentBusy(boolean busy);
  /**
   * Anybody can check if an agent is busy.
   */
  boolean isAgentBusy(MessageAddress agent);
  /**
   * @return an unmodifiable set of busy agents (MessageAddress elements).
   */
  Set getBusyAgents();
}
