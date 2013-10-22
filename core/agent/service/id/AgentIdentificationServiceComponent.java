/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.id;

import java.util.List;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the AgentIdentificationService.
 */
public final class AgentIdentificationServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private MessageAddress addr;
  private AgentIdentificationService aiS;
  private AgentIdentificationServiceProvider aiSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setParameter(Object o) {
    List l = (List) o;
    String name = (String) l.get(0);
    this.addr = MessageAddress.getMessageAddress(name);
  }

  @Override
public void load() {
    super.load();

    // create a single per-agent ai service instance
    this.aiS = new AgentIdentificationServiceImpl(addr);

    // create and advertise our service
    this.aiSP = new AgentIdentificationServiceProvider();
    sb.addService(AgentIdentificationService.class, aiSP);
  }

  @Override
public void unload() {
    // revoke our service
    if (aiSP != null) {
      sb.revokeService(AgentIdentificationService.class, aiSP);
      aiSP = null;
    }
    super.unload();
  }

  private class AgentIdentificationServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (AgentIdentificationService.class.isAssignableFrom(serviceClass)) {
        return aiS;
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

  private static class AgentIdentificationServiceImpl
    implements AgentIdentificationService {
      private final MessageAddress addr;
      public AgentIdentificationServiceImpl(MessageAddress addr) {
        this.addr = addr;
        if (addr == null) {
          throw new IllegalArgumentException(
              "Agent address is null");
        }
      }
      public MessageAddress getMessageAddress() { 
        return addr;
      }
      public String getName() {
        return addr.getAddress();
      }
      @Override
      public String toString() {
        return getName();
      }
    }
}
