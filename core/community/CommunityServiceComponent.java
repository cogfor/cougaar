/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.community;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.community.CommunityService;

/**
 * Agent-level component that loads the CommunityService provider and adds
 * initial community relationships for agent to Name Server.
 */

public class CommunityServiceComponent extends ComponentSupport {

  public CommunityServiceComponent() {
    super();
  }

  /**
   * Initializes CommunityService and adds initial community
   * relationships for this agent to Name Server.
   */
  public void load() {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    MessageAddress agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);
    CommunityService cs = loadCommunityService(agentId);
    super.load();
  }

  /**
   * Creates a CommunityService instance and adds to agent ServiceBroker.
   * @param agentId  Name of Agent
   * @return CommunityService reference
   */
  private CommunityService loadCommunityService(MessageAddress agentId) {
    ServiceBroker sb = getBindingSite().getServiceBroker();
    CommunityServiceProvider csp =
        new CommunityServiceProvider(getBindingSite(), agentId);
    sb.addService(CommunityService.class, csp);
    return (CommunityService)sb.getService(this, CommunityService.class,
      new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {}
    });
  }

}
