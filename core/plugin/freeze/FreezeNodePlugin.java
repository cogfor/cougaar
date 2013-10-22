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

package org.cougaar.core.plugin.freeze;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;

/**
 * This component gathers and integrates freeze information from
 * agents in a node to determine the "freeze" of the current
 * tasks. It continually determines the worst laggard in the node and
 * forwards that one laggard to the society root.
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public class FreezeNodePlugin extends FreezeSourcePlugin {
  private IncrementalSubscription relaySubscription;
  private AgentContainer agentContainer;

  @Override
public void load() {
    super.load();

    NodeControlService ncs = getServiceBroker().getService(
       this, NodeControlService.class, null);
    if (ncs != null) {
      agentContainer = ncs.getRootContainer();
      getServiceBroker().releaseService(
          this, NodeControlService.class, ncs);
    }
  }

  @Override
public void setupSubscriptions() {
    super.setupSubscriptions();
    relaySubscription = blackboard.subscribe(targetRelayPredicate);
  }

  /**
   * If the relay subscription becomes empty with thaw our children
   * (rescind our relay). If the relay subscription becomes non-empty,
   * we freeze our children (send a relay). Redundant freezes and
   * thaws are filtered by our base class.
   */
  @Override
public void execute() {
    if (relaySubscription.hasChanged()) {
      if (relaySubscription.isEmpty()) {
        thaw();                 // Thaw if frozen
      } else {
        freeze();
      }
    }
    super.execute();
  }

  // Implement abstract methods
  /**
   * Get the names of our target agents.
   * @return the names of agents in this node
   */
  @Override
protected Set getTargetNames() {
    // get local agent addresses
    Set addrs;
    if (agentContainer == null) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to list local agents on node "+
            getAgentIdentifier());
      }
      addrs = Collections.EMPTY_SET;
    } else {
      addrs = agentContainer.getAgentAddresses();
    }
    // flatten to names, which the parent then converts back.
    // we could fix parent to ask for "getTargetAddresses()"
    Set names = new HashSet(addrs.size());
    for (Iterator i = addrs.iterator(); i.hasNext(); ) {
      MessageAddress a = (MessageAddress) i.next();
      names.add(a.getAddress());
    }
    return names;
  }

  /**
   * Our children have become frozen, so we tell our parent(s) we are frozen, too
   */
  @Override
protected void setUnfrozenAgents(Set unfrozenAgents) {
    if (logger.isDebugEnabled()) logger.debug("unfrozen " + unfrozenAgents);
    for (Iterator i = relaySubscription.iterator(); i.hasNext(); ) {
      FreezeRelayTarget relay = (FreezeRelayTarget) i.next();
      relay.setUnfrozenAgents(unfrozenAgents);
      blackboard.publishChange(relay);
    }
  }
}
