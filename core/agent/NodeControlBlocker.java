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

package org.cougaar.core.agent;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.NullService;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component blocks the {@link NodeControlService} if the
 * agent's address does not match the node's address.  This is
 * typically one of the first component loaded in all agents.
 */
public final class NodeControlBlocker
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private ServiceProvider ncsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    MessageAddress localAgent = find_local_agent();
    MessageAddress localNode = find_local_node();
    boolean isNode = 
      (localAgent == null ||
       localAgent.equals(localNode));

    if (!isNode) {
      // block the NodeControlService!
      ncsp = new BlockSP();
      if (!sb.addService(NodeControlService.class, ncsp)) {
        throw new RuntimeException("Unable to block NodeControlService");
      }

      // verify
      NodeControlService ncs = sb.getService(this, NodeControlService.class, null); 
      if (ncs != null) {
        throw new RuntimeException("Could not block NodeControlService");
      }
    }
  }

  @Override
public void unload() {
    super.unload();

    if (ncsp != null) {
      sb.revokeService(NodeControlService.class, ncsp);
      ncsp = null;
    }
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

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }

  private static final class BlockSP
    implements ServiceProvider {
      // we are not required to implement our service API if the
      // instance implements NullService
      private final Service NULL = new NullService() {};

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (NodeControlService.class.isAssignableFrom(serviceClass)) {
          return NULL; // service blocker!
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
