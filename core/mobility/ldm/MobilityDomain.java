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

package org.cougaar.core.mobility.ldm;

import java.util.Collection;
import java.util.Collections;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.UIDService;

/**
 * This component is the mobility domain, which just has the
 * {@link MobilityFactory} and no {@link 
 * org.cougaar.core.domain.LogicProvider}s.
 * <p>
 * The original idea was to add security by restricting access to
 * the {@link MobilityFactory}, which is required to create a
 * {@link AgentControl}.
 */
public class MobilityDomain extends DomainAdapter {

  private static final String MOBILTY_NAME = "mobility";

  @Override
public String getDomainName() {
    return MOBILTY_NAME;
  }

  public Collection getAliases() {
    return Collections.singleton(getDomainName());
  }

  @Override
protected void loadFactory() {
    UIDService uidService = getUIDService();
    MessageAddress nodeId = getNodeId();
    MessageAddress agentId = getAgentId();
    MobilityFactory f = new MobilityFactoryImpl(uidService, nodeId, agentId);

    setFactory(f);
  }

  @Override
protected void loadXPlan() {
    // no xplan
  }

  private UIDService getUIDService() {
    ServiceBroker sb = getServiceBroker();
    UIDService uidService = 
      sb.getService(
       this,
       UIDService.class,
       null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain uid service");
    }
    return uidService;
  }

  private MessageAddress getAgentId() {
    // get the agentId
    ServiceBroker sb = getServiceBroker();
    AgentIdentificationService agentIdService = 
      sb.getService(
       this,
       AgentIdentificationService.class,
       null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    MessageAddress agentId = agentIdService.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }
    return agentId;
  }

  private MessageAddress getNodeId() {
    // get the nodeId
    ServiceBroker sb = getServiceBroker();
    NodeIdentificationService nodeIdService = 
      sb.getService(
       this,
       NodeIdentificationService.class,
       null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    MessageAddress nodeId = nodeIdService.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }
    return nodeId;
  }

  // zero LPs
  @Override
protected void loadLPs() {
  }
  @Override
public void invokeMessageLogicProviders(DirectiveMessage message) {
  }
  @Override
public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
  }
  @Override
public void invokeRestartLogicProviders(MessageAddress cid) {
  }

}
