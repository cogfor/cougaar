/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component an optional proxy to the {@link WhitePagesService}
 * resolver that obtains the local agent's name and tags all requests
 * with that name.
 * <p>
 * This should be loaded into all agents.  All WhitePagesService
 * requests from within the agent will passed through a proxy
 * service.  From the node's point of view there will only be one
 * client per agent (ie. this proxy).
 * <p>
 * The proxy is disabled if this component is loaded into the
 * node agent. 
 */
public class ResolverProxy
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;

  private LoggingService logger;
  private MessageAddress agentId;
  private MessageAddress nodeId;
  private WhitePagesService proxyWP;

  private WhitePagesSP proxySP;

  private final ResolverClient myClient =
    new ResolverClient() {
      public String getAgent() {
        return (agentId == null ? null : agentId.getAddress());
      }
    };

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  @Override
public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver proxy");
    }

    // which agent are we in?
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    // which node are we in?
    NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
    nodeId = nis.getMessageAddress();
    sb.releaseService(this, NodeIdentificationService.class, nis);

    if (agentId == null ||
        agentId.equals(nodeId)) {
      // we're in the node agent
      return;
    }

    // get the node's wp service
    proxyWP = sb.getService(myClient, WhitePagesService.class, null);
    if (proxyWP == null) {
      throw new RuntimeException(
          "Unable to obtain the WhitePagesService,"+
          " proxy failed for agent "+agentId+" on node "+nodeId);
    }

    // advertize our service proxy
    proxySP = new WhitePagesSP();
    sb.addService(WhitePagesService.class, proxySP);

    if (logger.isInfoEnabled()) {
      logger.info(
          "Loaded white pages resolver proxy for agent "+
          agentId+" on node "+nodeId);
    }
  }

  @Override
public void unload() {
    super.unload();

    // revoke white pages service
    if (proxySP != null) {
      sb.revokeService(WhitePagesService.class, proxySP);
      proxySP = null;
    }

    if (proxyWP != null) {
      sb.releaseService(myClient, WhitePagesService.class, proxyWP);
      proxyWP = null;
    }

    if (logger != null) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
    }
  }

  private class WhitePagesSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
          // return our proxyWP, where myClient identifies our agent
          if (logger.isDetailEnabled()) {
            logger.detail(
                "giving "+agentId+" proxy WP to "+requestor);
          }
          return proxyWP;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
