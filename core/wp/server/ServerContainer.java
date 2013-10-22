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

package org.cougaar.core.wp.server;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;

/**
 * This component is an empty container for the white pages server
 * components.
 */
public class ServerContainer
extends ContainerSupport
{
  public static final String INSERTION_POINT = 
    Agent.INSERTION_POINT + ".WPServer";

  private LoggingService logger;
  private MessageAddress agentId;

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
    }
  }

  @Override
protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  @Override
protected ComponentDescriptions findInitialComponentDescriptions() {
    String cname = agentId.toString();
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = sb.getService(this, ComponentInitializerService.class, null);
    try {
      return new ComponentDescriptions(
          cis.getComponentDescriptions(cname, specifyContainmentPoint()));
    } catch (ComponentInitializerService.InitializerException cise) {
      if (logger.isInfoEnabled()) {
        logger.info("\nUnable to add "+cname+"'s plugins ",cise);
      }
      return null;
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }
  }
}
