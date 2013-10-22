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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link NodeBusyService} to all
 * agents.
 */
public final class NodeBusyComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private LoggingService log;

  private ServiceProvider nbsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    nbsp = new NodeBusyServiceProvider(log);
    rootsb.addService(NodeBusyService.class, nbsp);
  }

  @Override
public void unload() {
    super.unload();

    rootsb.revokeService(NodeBusyService.class, nbsp);
    nbsp = null;
  }

  private static class NodeBusyServiceProvider
    implements ServiceProvider {
      private final LoggingService log;
      private final Set busyAgents = new HashSet();

      public NodeBusyServiceProvider(LoggingService log) {
        this.log = log;
      }

      public Object getService(
          ServiceBroker xsb, Object requestor, Class serviceClass) {
        if (serviceClass != NodeBusyService.class) {
          throw new IllegalArgumentException(
              "Can only provide NodeBusyService!");
        }
        return new NodeBusyService() {
          MessageAddress me = null;
          public void setAgentIdentificationService(
              AgentIdentificationService ais) {
            me = ais.getMessageAddress();
          }
          public void setAgentBusy(boolean busy) {
            if (me == null) {
              throw new RuntimeException(
                  "AgentIdentificationService has not been set");
            }
            if (log.isDebugEnabled()) {
              log.debug("setAgentBusy(" + me + ", " + busy + ")");
            }
            if (busy) {
              busyAgents.add(me);
            } else {
              busyAgents.remove(me);
            }
          }
          public boolean isAgentBusy(MessageAddress agent) {
            return busyAgents.contains(agent);
          }
          public Set getBusyAgents() {
            return Collections.unmodifiableSet(busyAgents);
          }
        };
      }

      public void releaseService(
          ServiceBroker xsb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
