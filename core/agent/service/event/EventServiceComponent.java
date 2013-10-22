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

package org.cougaar.core.agent.service.event;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;
import org.cougaar.util.log.Logging;

/**
 * This component advertises the EventService, which is implemented
 * as a wrapper around the LoggingService with an "EVENT." category
 * prefix.
 */
public final class EventServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private static final String EVENT_PREFIX = "EVENT.";

  private ServiceBroker sb;
  private String prefix = "";
  private EventServiceProvider sp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    if (ais != null) {
      prefix = ais.getMessageAddress()+": ";
      //    } else { -- Revocation - Nothing to do.
    }
  }

  @Override
public void load() {
    super.load();

    // if we're in the node-agent then advertise at the root
    // level, to allow use by all node-level components
    //
    // note that the other agengs will override this node-level
    // service to make the prefix match their agent's id, as
    // opposed to the node's id.
    NodeControlService nodeControlService = sb.getService(
       this, NodeControlService.class, null);
    if (nodeControlService != null) {
      ServiceBroker rootsb =
        nodeControlService.getRootServiceBroker();
      sb.releaseService(
          this, NodeControlService.class, nodeControlService);
      sb = rootsb;
    }

    // create and advertise our service
    if (sp == null) {
      sp = new EventServiceProvider();
      sb.addService(EventService.class, sp);
    }
  }

  @Override
public void unload() {
    // revoke our service
    if (sp != null) {
      sb.revokeService(EventService.class, sp);
      sp = null;
    }
    super.unload();
  }

  private class EventServiceProvider implements ServiceProvider {

    private final LoggerFactory lf = LoggerFactory.getInstance();

    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (EventService.class.isAssignableFrom(serviceClass)) {
        String name = EVENT_PREFIX + Logging.getKey(requestor);
        Logger l = lf.createLogger(name);
        return new EventServiceImpl(prefix, l);
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

  private static class EventServiceImpl
    implements EventService {
    private final String prefix;
    private final Logger l;
    public EventServiceImpl(String prefix, Logger l) {
      this.prefix = prefix;
      this.l = l;
    }
    public boolean isEventEnabled() {
      return l.isEnabledFor(Logger.INFO); 
    }
    public void event(String s) {
      event(s, null);
    }
    public void event(String s, Throwable t) {
      l.log(Logger.INFO, prefix+s, t);
    }
  }
}
