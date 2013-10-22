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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.ChangeEnvelopeTuple;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is an optional base class for {@link Domain}
 * implementations.
 */
public abstract class DomainAdapter 
  extends GenericStateModelAdapter
  implements Component, Domain 
{

  private BindingSite bindingSite;
  private ServiceBroker sb;
  private LoggingService logger;
  private XPlanService xplanService;
  private DomainRegistryService domainRegistryService;

  private final List myEnvelopeLPs = new ArrayList();
  private final List myMessageLPs = new ArrayList();
  private final List myRestartLPs = new ArrayList();
  private final List myABAChangeLPs = new ArrayList();
  
  private Factory myFactory;
  private XPlan myXPlan;

  /** returns the Domain name */
  public abstract String getDomainName();

  public void setBindingSite(BindingSite bindingSite) {
    this.bindingSite = bindingSite;
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  protected BindingSite getBindingSite() {
    return bindingSite;
  }

  protected ServiceBroker getServiceBroker() {
    return sb;
  }

  protected XPlan getXPlanForDomain(String domainName) {
    return xplanService.getXPlan(domainName);
  }

  protected XPlan getXPlanForDomain(Class domainClass) {
    return xplanService.getXPlan(domainClass);
  }

  /** returns the LoggingService */
  public LoggingService getLoggingService() {
    return logger;
  }

  /** returns the Factory for this Domain. */
  public Factory getFactory() {
    return myFactory;
  }

  /**
   * @return the XPlan instance for the domain - instance may be
   * be shared among domains
   */
  public XPlan getXPlan() {
    return myXPlan;
  }

  @Override
public void load() {
    super.load();

    LoggingService ls = sb.getService(this, LoggingService.class, null);
    if (ls != null) {
      logger = ls;
    }

    xplanService = sb.getService(this, XPlanService.class, null);
    if (xplanService == null) {
      throw new RuntimeException(
          "Unable to obtain XPlanService");
    }

    domainRegistryService = sb.getService(this, DomainRegistryService.class, null);
    if (domainRegistryService == null) {
      throw new RuntimeException(
          "Unable to obtain DomainRegistryService");
    }

    loadFactory();
    loadXPlan();
    loadLPs();

    domainRegistryService.registerDomain(this);
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

    if (xplanService != null) {
      sb.releaseService(
          this, XPlanService.class, xplanService);
      xplanService = null;
    }

    if (logger != LoggingService.NULL) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = LoggingService.NULL;
    }
  }


  /** invoke the MessageLogicProviders for this domain */
  public void invokeMessageLogicProviders(DirectiveMessage message) {
    Directive [] directives = message.getDirectives();
    for (int index = 0; index < directives.length; index ++) {
      Directive directive = directives[index];
      Collection changeReports = null;
      if (directive instanceof DirectiveMessage.DirectiveWithChangeReports) {
        DirectiveMessage.DirectiveWithChangeReports dd = 
          (DirectiveMessage.DirectiveWithChangeReports) directive;
        changeReports = dd.getChangeReports();
        directive = dd.getDirective();
      }

      synchronized (myMessageLPs) {
        for (int lpIndex = 0; lpIndex < myMessageLPs.size(); lpIndex++) {
          ((MessageLogicProvider) myMessageLPs.get(lpIndex)).execute(directive, changeReports);
        }
      }
    }
  }
    
  /** invoke the EnvelopeLogicProviders for this domain */
  public void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, boolean isPersistenceEnvelope) {
    Collection changeReports = null;
    if (tuple instanceof ChangeEnvelopeTuple) {
      changeReports = ((ChangeEnvelopeTuple) tuple).getChangeReports();
    }

    synchronized (myEnvelopeLPs) {
      for (int lpIndex = 0; lpIndex < myEnvelopeLPs.size(); lpIndex++) {
        EnvelopeLogicProvider lp = (EnvelopeLogicProvider) myEnvelopeLPs.get(lpIndex);
	if (isPersistenceEnvelope &&
            !(lp instanceof LogicProviderNeedingPersistenceEnvelopes)) {
	  continue;	// This lp does not want contents of PersistenceEnvelopes
	}
        try {
          lp.execute(tuple, changeReports);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** invoke the RestartLogicProviders for this domain */
  public void invokeRestartLogicProviders(MessageAddress cid) {
    synchronized (myRestartLPs) {
      for (int index = 0;  index < myRestartLPs.size(); index++) {
        try {
          ((RestartLogicProvider) myRestartLPs.get(index)).restart(cid);
        }
        catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    }
  }
  public void invokeABAChangeLogicProviders(Set communities) {
    synchronized (myABAChangeLPs) {
      for (int index = 0; index < myABAChangeLPs.size(); index++) {
        try {
          ((ABAChangeLogicProvider) myABAChangeLPs.get(index)).abaChange(communities);
        }
        catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** 
   * setParameter - Should only be used by the binding utilities when 
   * loading a domain from a ComponentDescription. Used as a sanity check
   * to ensure that the specified domain name matches the domain name for
   * the class.
   *
   * @param o  Expecting a List containing one non-null String that 
   * specifies the domainName. 
   */
  public void setParameter(Object o) {
    String domainName = (String) (((List) o).get(0));
    if (domainName == null) {
      throw new IllegalArgumentException("Null domain name");
    }
    
    if (!domainName.equals(getDomainName())) { 
      System.err.println("Invalid Domain name parameter - " +
                         " specified " + domainName + 
                         " should be " + getDomainName());
    }
  }

  
  /** Add a LogicProvider to the set maintained for this Domain. */
  protected void addLogicProvider(LogicProvider lp) {
    if (lp instanceof MessageLogicProvider) {
      myMessageLPs.add(lp);
    }
    if (lp instanceof EnvelopeLogicProvider) {
      myEnvelopeLPs.add(lp);
    }
    if (lp instanceof RestartLogicProvider) {
      myRestartLPs.add(lp);
    }
    if (lp instanceof ABAChangeLogicProvider) {
      myABAChangeLPs.add(lp);
    }
    
    lp.init();
  }

  protected final List getEnvelopeLPs() {
    return myEnvelopeLPs;
  }

  protected final List getMessageLPs() {
    return myMessageLPs;
  }

  protected final List getRestartLPs() {
    return myRestartLPs;
  }

  protected final List getABAChangeLPs() {
    return myABAChangeLPs;
  }

  /**
   * Load the Factory for this Domain. Should call setFactory() to set the
   * factory for this Domain
   */
  abstract protected void loadFactory();

  /**
   * Load the XPLan for this Domain. Should call setXPlan() to set the XPlan 
   * for this Domain
   */
  abstract protected void loadXPlan();
  
  /**
   * Load the LogicProviders for this Domain. Should call addLogicProvider() to
   * add each LogicProvider to the set maintained for this Domain.
   */
  abstract protected void loadLPs();

  /** set the factory for this Domain */
  protected void setFactory(Factory factory) {
    if ((myFactory != null) && logger.isDebugEnabled()) {
      // Should we even allow this?
      logger.debug("DomainAdapter: resetting Factory");
    }

    myFactory = factory;
  }

  /** set the XPlan for this Domain */
  protected void setXPlan(XPlan xPlan) {
    if ((myXPlan != null) && logger.isDebugEnabled()) {
      // Should we even allow this?
      logger.debug("DomainAdapter: resetting XPlan");
    }
    
    myXPlan = xPlan;
  }
}
