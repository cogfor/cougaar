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

package org.cougaar.core.plugin;

import java.util.Iterator;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ComponentRuntimeException;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.ConfigFinder;

/**
 * This component is a container for {@link ComponentPlugin} and
 * {@link org.cougaar.core.servlet.ComponentServlet} components.
 */
public class PluginManager 
extends ContainerSupport
{
  /** The insertion point for a PluginManager, defined relative to its parent, Agent. */
  public static final String INSERTION_POINT = Agent.INSERTION_POINT + ".PluginManager";

  private LoggingService logger;
  private MessageAddress agentId;
  private AgentIdentificationService agentIdService;

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
      //    } else {      // Revocation
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

  @Override
public boolean add(Object o) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" adding plugin "+o);
      }
      boolean result = super.add(o);
      if (logger.isInfoEnabled()) {
        logger.info("Agent "+agentId+" added plugin "+o);
      }
      return result;
    } catch (ComponentRuntimeException cre) {
      Throwable cause = cre.getCause();
      if (cause == null) cause = cre;
      logger.error("Failed to add "+o+" to "+this, cause);
      throw cre;
    } catch (RuntimeException re) {
      //logger.error("Failed to add "+o+" to "+this, re);
      throw re;
    }
  }

  @Override
public void unload() {
    super.unload();

    // release services
    ServiceBroker sb = getServiceBroker();
    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
  }

  // 
  // other services
  //
  
   @Override
   public void start() {
      super.start();
      for (Iterator itr = binderIterator(); itr.hasNext();) {
         Object child = itr.next();
         if (child instanceof Binder) {
            Binder binder = ((Binder) child);
            binder.startSubscriptions();
         }
      }
   }

public MessageAddress getMessageAddress() {
    return agentId;
  }
  public MessageAddress getAgentIdentifier() {
    return agentId;
  }
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance(); // FIXME replace with service
  }
  @Override
public String toString() {
    return agentId+"/PluginManager";
  }

}
