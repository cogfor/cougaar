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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component gathers and integrates freeze information from agents
 * in a society to determine the "completion" of the freeze operation.
 * In most agents, it gathers the information and forwards the frozen
 * status of the agent to another agent. This process continues
 * through a hierarchy of such plugins until the plugin at the root of
 * the tree is reached. When the root determines that ice has been
 * acheived, that is reflected in the freeze control servlet
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public abstract class FreezeSourcePlugin extends FreezePlugin {
  protected UIDService uidService;
  private Subscription relaySubscription;
  private FreezeRelaySource relay; // The relay we sent

  @Override
public void unload() {
    if (uidService != null) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    super.unload();
  }

  @Override
public void setupSubscriptions() {
    super.setupSubscriptions();
    ServiceBroker sb = getServiceBroker();
    uidService = sb.getService(this, UIDService.class, null);
    relaySubscription = blackboard.subscribe(sourceRelayPredicate);
  }

  @Override
public void execute() {
    if (relaySubscription.hasChanged()) {
      if (relay != null) {
        setUnfrozenAgents(relay.getUnfrozenAgents());
      }
    }
  }

  protected abstract Set getTargetNames();

  protected abstract void setUnfrozenAgents(Set unfrozenAgents);

  protected synchronized void freeze() {
    if (relay != null) return;  // Already frozen
    if (logger.isDebugEnabled()) logger.debug("freeze");
    MessageAddress me = getAgentIdentifier();
    Set names = getTargetNames();
    Set targets = new HashSet(names.size());
    for (Iterator i = names.iterator(); i.hasNext(); ) {
      MessageAddress cid = MessageAddress.getMessageAddress((String) i.next());
      if (!cid.equals(me)) targets.add(cid);
    }
    relay = new FreezeRelaySource(targets);
    relay.setUID(uidService.nextUID());
    blackboard.publishAdd(relay);
    relaySubscription = blackboard.subscribe(new UnaryPredicate() {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
          return o == relay;
        }
      });
    setUnfrozenAgents(names);
  }

  protected synchronized void thaw() {
    if (relay == null) return;  // not frozen
    if (logger.isDebugEnabled()) logger.debug("thaw");
    blackboard.publishRemove(relay);
    blackboard.unsubscribe(relaySubscription);
    relay = null;
  }
}

