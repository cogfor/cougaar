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

package org.cougaar.core.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.ChangeEnvelopeTuple;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.RelayLP;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is the minimal "root" domain that creates the
 * basic {@link RootPlan} for other {@link Domain}s and loads the
 * {@link RelayLP}.
 * <p>
 * Instead of using the {@link DomainAdapter} as a base class, this
 * simple domain directly implements the domain API for efficiency
 * and illustative purposes.
 */
public final class RootDomain
extends GenericStateModelAdapter
implements Component, Domain 
{
  private ServiceBroker sb;
  private DomainRegistryService domainRegistryService;
  private MessageAddress self;
  private RootPlan rootplan;
  private RelayLP relayLP;

  public void setParameter(Object o) {
    String domainName = (String) (((List) o).get(0));
    if (!getDomainName().equals(domainName)) {
      throw new IllegalArgumentException(
          "Invalid Domain name parameter - " +
          " specified " + domainName + 
          " should be " + getDomainName());
    }
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setAgentIdentificationService(
      AgentIdentificationService ais) {
    if (ais != null) {
      this.self = ais.getMessageAddress();
    }
  }

  private class RootPlanServiceProvider implements ServiceProvider {
    private Service theInstance = new RootPlanImpl();
    /* (non-Javadoc)
     * @see org.cougaar.core.component.ServiceProvider#getService(org.cougaar.core.component.ServiceBroker, java.lang.Object, java.lang.Class)
     */
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      return theInstance;
    }

    /* (non-Javadoc)
     * @see org.cougaar.core.component.ServiceProvider#releaseService(org.cougaar.core.component.ServiceBroker, java.lang.Object, java.lang.Class, java.lang.Object)
     */
    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
    }
  }
  
  @Override
public void load() {
    super.load();
    sb.addService(RootPlan.class, new RootPlanServiceProvider());
    rootplan = sb.getService(this, RootPlan.class, null);
    relayLP = new RelayLP(rootplan, self);
    domainRegistryService = sb.getService(
       this, DomainRegistryService.class, null);
    if (domainRegistryService != null) {
      domainRegistryService.registerDomain(this);
    }
  }

  @Override
public void unload() {
    super.unload();
    if (domainRegistryService != null) {
      domainRegistryService.unregisterDomain(this);
      sb.releaseService(
          this, DomainRegistryService.class, domainRegistryService);
      domainRegistryService = null;
    }
  }

  public String getDomainName() {
    return "root";
  }

  public XPlan getXPlan() {
    return rootplan;
  }

  public Factory getFactory() {
    return null;
  }

  public void invokeMessageLogicProviders(DirectiveMessage message) {
    Directive[] directives = message.getDirectives();
    for (int index = 0; index < directives.length; index++) {
      Directive directive = directives[index];
      invokeMessageLogicProviders(directive);
    }
  }

  private void invokeMessageLogicProviders(Directive directive) {
    Collection changeReports = null;
    if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
      DirectiveMessage.DirectiveWithChangeReports dd = 
        (DirectiveMessage.DirectiveWithChangeReports) directive;
      changeReports = dd.getChangeReports();
      directive = dd.getDirective();
    }
    relayLP.execute(directive, changeReports);
  }

  public void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    if (isPersistenceEnvelope) {
      return;
    }
    Collection changeReports = null;
    if (tuple instanceof ChangeEnvelopeTuple) {
      changeReports = ((ChangeEnvelopeTuple) tuple).getChangeReports();
    }
    relayLP.execute(tuple, changeReports);
  }

  public void invokeRestartLogicProviders(MessageAddress cid) {
    relayLP.restart(cid);
  }

  public void invokeABAChangeLogicProviders(Set communities) {
    relayLP.abaChange(communities);
  }
}
