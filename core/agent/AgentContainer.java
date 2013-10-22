/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * The AgentContainer manages all agents on the node.
 */
public interface AgentContainer {

  /**
   * Equivalent to
   *   <code>(getAgentDescription(agentId) != null)</code>
   *
   * @return true if the agent is on the local node
   */
  boolean containsAgent(MessageAddress agentId);

  /**
   * Equivalent to
   *   <code>getLocalAgentDescriptions().keySet()</code>
   *
   * @return a Set of all local agent MessageAddresses
   */
  Set getAgentAddresses();

  /**
   * Equivalent to
   *   <code>getLocalAgentDescriptions().get(agentId)</code>
   *
   * @return null if the agent is not on the local node,
   *    or the description is not known.
   */
  ComponentDescription getAgentDescription(
      MessageAddress agentId);

  /**
   * Get an unmodifiable map of local agent MessageAddress to the 
   * ComponentDescriptions.
   *
   * @return a Map&lt;MessageAddress&gt;&lt;ComponentDescriptions&gt;
   *   for the local agents
   */
  Map getAgents();

  List getComponents();

  /**
   * Add a new agent to the local node.
   */
  void addAgent(MessageAddress agentId);

  /**
   * <i>deprecated</i>, use one of the above "addAgent" method.
   */
  void addAgent(MessageAddress agentId, StateTuple tuple);

  /**
   * Add a component to this agent container. Only certain components
   * are allowed using this method. In particular, agents cannot be
   * added.
   */
  boolean add(Object o);

  /**
   * Remove an agent that's on the local node.
   * 
   * @throws RuntimeException if the agent is not on the
   *    local node, or it can't be removed.
   */
  void removeAgent(MessageAddress agentId);

  /**
   * Remove component from this agent container. Only components added
   * with the add method can be removed this way. Agents cannot be
   * removed with this method.
   */
  boolean remove(Object o);
}
