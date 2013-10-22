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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service allows a component to query the agents registered
 * with the node's {@link QuiescenceReportService} and mark agents
 * as dead (ignorable) when they have been restarted on another
 * node.
 */
public interface AgentQuiescenceStateService extends Service {
  /** Is the Node altogether quiescent */
  boolean isNodeQuiescent();

  /**
   * List the local agents with quiescence states for the Node to consider
   * @return an array of MessagAddresses registered with the Nodes QuiescenceReportService
   */
  MessageAddress[] listAgentsRegistered();
  
  /**
   * Is the named agent's quiescence service enabled (ie the Distributor is fully loaded)?
   * @param agentAddress The agent to query
   * @return true if the agent's quiescence service has been enabled and it counts towards Node quiescence
   */
  boolean isAgentEnabled(MessageAddress agentAddress);
  
  /**
   * Is the named agent quiescent?
   * @param agentAddress The agent to query
   * @return true if the Agent's Distributor is quiescent
   */
  boolean isAgentQuiescent(MessageAddress agentAddress);
  
  /**
   * Is the named agent alive for quiescence purposes, or has it been
   * marked as dead to be ignored?
   * @param agentAddress The agent to query
   * @return false if the agent is dead and should be ignored for local quiescence
   */
  boolean isAgentAlive(MessageAddress agentAddress);
  
  /**
   * Mark the named agent as dead - it has been restarted elsewhere, and should
   * be ignored locally for quiescence calculations.
   * @param agentAddress The Agent to mark as dead
   */
  void setAgentDead(MessageAddress agentAddress);
  
  /**
   * Show the QuiescenceService clients that are blocking quiescence for this agent, if any
   * @param agentAddress The Agent whose quiescence blockers to show
   */
  String getAgentQuiescenceBlockers(MessageAddress agentAddress);

  // Other options: list message numbers? 
}
