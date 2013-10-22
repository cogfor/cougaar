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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link LoggingService}
 * to all agents.
 *
 * @property org.cougaar.core.logging.addAgentPrefix
 *   Modify the agent-level LoggingService implementation to add an
 *   "agent: " prefix to all logging lines.  Options are:
 *     "true" to enable in both agents and node-agents,
 *     "agent" to only enable on agents,
 *     "node" to only enable on the node-agent,
 *     "false" to disable.
 *   Defaults to true.
 */
public final class LoggingServiceComponent
extends GenericStateModelAdapter
implements Component
{

  private static final String ADD_AGENT_PREFIX =
    SystemProperties.getProperty(
        "org.cougaar.core.logging.addAgentPrefix", "true");

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private ServiceProvider lsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
      sb.releaseService(this, NodeControlService.class, ncs);
    }

    String prefix;
    if (shouldPrefix()) {
      MessageAddress localAgent = find_local_agent();
      prefix = localAgent+": ";
    } else {
      prefix = null;
    }

    lsp = new LoggingServiceProvider(prefix);

    ServiceBroker the_sb = (rootsb == null ? sb : rootsb);
    the_sb.addService(LoggingService.class, lsp);
    the_sb.addService(LoggingControlService.class, lsp);
  }
  
  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  @Override
public void unload() {
    super.unload();

    ServiceBroker the_sb = (rootsb == null ? sb : rootsb);
    the_sb.revokeService(LoggingControlService.class, lsp);
    the_sb.revokeService(LoggingService.class, lsp);
    lsp = null;
  }

  private boolean shouldPrefix() {
    boolean isNode = (rootsb != null);
    return
      ADD_AGENT_PREFIX.equals("true") ||
      ADD_AGENT_PREFIX.equals("both") ||
      (isNode ?
       ADD_AGENT_PREFIX.equals("node") :
       ADD_AGENT_PREFIX.equals("agents"));
  }
}
