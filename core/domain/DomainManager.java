/*
 * <copyright>
 *  
 *  Copyright 2001-2007 BBNT Solutions, LLC
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainForBlackboardService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;

/**
 * This component is a container for {@link Domain}s.
 *
 * @property org.cougaar.core.load.planning
 *   If enabled, the domain manager will load the planning-specific
 *   PlanningDomain.  See bug 2522.  Defaults to <em>true</em>.
 *
 * @property org.cougaar.core.domain.config.enable
 *   Enable the "-Dorg.cougaar.core.domain.config.filename" option
 *   to read the <i>LDMDomains.ini</i> file.  Defaults to <em>true</em>.
 *
 * @property org.cougaar.core.domain.config.filename
 *   The domain manager will read the specified ".ini" configuration
 *   file (using the config finder) to load domains.  See bug 2977.
 *   Defaults to <i>LDMDomains.ini</i>.
 */
public class DomainManager 
extends ContainerSupport
{

  private static final boolean READ_CONFIG_FILE = 
    SystemProperties.getBoolean(
        "org.cougaar.core.domain.config.enable",
        true);

  private static final String FILENAME = 
    SystemProperties.getProperty(
        "org.cougaar.core.domain.config.filename",
        "LDMDomains.ini");

  private static final boolean LOAD_PLANNING =
    SystemProperties.getBoolean("org.cougaar.core.load.planning", true);

  private final static String PREFIX = "org.cougaar.domain.";
  private final static int PREFIXLENGTH = PREFIX.length();


  /** Insertion point for a DomainManager, defined relative to its parent, Agent. */
  public static final String INSERTION_POINT = 
    Agent.INSERTION_POINT + ".DomainManager";
  private final static String CONTAINMENT_POINT = INSERTION_POINT;

  // set parameter defaults
  private boolean readConfigFile = READ_CONFIG_FILE;
  private String filename = FILENAME;
  private boolean loadPlanning = LOAD_PLANNING;

  private final Object lock = new Object();
  private List delayedXPlans = Collections.EMPTY_LIST;
  private List domains = Collections.EMPTY_LIST;
  private Blackboard blackboard = null;

  private MessageAddress self;
  private AgentIdentificationService agentIdService;
  private LoggingService loggingService = LoggingService.NULL;

  private DomainRegistryServiceProvider domainRegistrySP;
  private XPlanServiceProvider xplanSP;
  private DomainServiceProvider domainSP;
  private DomainForBlackboardServiceProvider domainForBlackboardSP;

  public void setParameter(Object o) {
    List l = (List) o;
    for (int i = 0; i < l.size(); i++) {
      String si = (String) l.get(i);
      int sep = si.indexOf('=');
      if (sep <= 0) {
        throw new IllegalArgumentException(
            "Expecting a \"name=value\" parameter, not "+si);
      }
      String name = si.substring(0, sep).trim();
      String value = si.substring(sep+1).trim();
      if ("load_planning".equals(name)) {
        loadPlanning = "true".equals(value);
      } else if ("read_config_file".equals(name)) {
        readConfigFile = "true".equals(value);
      } else if ("filename".equals(name)) {
        filename = value;
      } else {
        throw new IllegalArgumentException("Unknown parameter name: "+name);
      }
    }
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais != null) {
      this.self = ais.getMessageAddress();
    }
  }

  @Override
public void load() {
    ServiceBroker sb = getServiceBroker();
    ServiceBroker csb = getChildServiceBroker();

    LoggingService ls = sb.getService(this, LoggingService.class, null);
    if (ls != null) {
      loggingService = ls;
    }

    xplanSP = new XPlanServiceProvider();
    csb.addService(XPlanService.class, xplanSP);

    domainSP = new DomainServiceProvider();
    sb.addService(DomainService.class, domainSP);

    domainRegistrySP = new DomainRegistryServiceProvider();
    csb.addService(
        DomainRegistryService.class,
        domainRegistrySP);

    domainForBlackboardSP = new DomainForBlackboardServiceProvider();
    sb.addService(
        DomainForBlackboardService.class, 
        domainForBlackboardSP);

    // display the agent id
    if (loggingService.isDebugEnabled()) {
      loggingService.debug(
          "DomainManager "+this+" loading Domains for agent "+self);
    }

    super.load();
  }

  private Iterator delayedXPlansIterator() {
    synchronized (lock) {
      // the list is immutable, since we replace it when modified
      return delayedXPlans.iterator();
    }
  }

  private XPlan getXPlan(String domainName) {
    Domain d = getDomain(domainName);
    return (d == null ? null : d.getXPlan());
  }

  private XPlan getXPlan(Class domainClass) {
    Domain d = getDomain(domainClass);
    return (d == null ? null : d.getXPlan());
  }

  private void registerDomain(Domain d) {
    String domainName = d.getDomainName();
    XPlan xplan = d.getXPlan();
    synchronized (lock) {
      Domain origD = getDomain(domainName);
      if (origD != null) {
        if (loggingService.isWarnEnabled()) {
          loggingService.warn(
              "Domain \""+domainName+"\" multiply defined!"+
              " Already loaded "+origD+".  Ignoring "+d);
        }
        return;
      }
      // replace the list, treat it as immutable.
      // this makes the blackboard access more efficient
      List l = new ArrayList(domains.size()+1);
      l.addAll(domains);
      l.add(d);
      domains = l;
      // add xplan
      if (xplan instanceof SupportsDelayedLPActions &&
          (!delayedXPlans.contains(xplan))) {
        l = new ArrayList(delayedXPlans.size()+1);
        l.addAll(delayedXPlans);
        l.add(xplan);
        delayedXPlans = l;
      }
    }

    if (loggingService.isDebugEnabled()) {
      loggingService.debug("Registering "+domainName);
    }

    if ((xplan != null) && (blackboard != null)) {
      xplan.setupSubscriptions(blackboard);
    }
  }

  private void unregisterDomain(Domain d) {
    String domainName = d.getDomainName();
    synchronized (lock) {
      // find index
      int i;
      int n;
      for (i = 0, n = domains.size(); i < n; i++) {
        Domain di = (Domain) domains.get(i);
        if (di.getDomainName().equals(domainName)) {
          break;
        }
      }
      if (i >= n) {
        // not registered?
        return;
      }
      // remove entry, replace list
      // fix domains
      List l = new ArrayList(n-1);
      for (int j = 0; j < i; j++) {
        l.add(domains.get(j));
      }
      for (int j = i+1; j < n; j++) {
        l.add(domains.get(j));
      }
      domains = l;
      // fix delayed xplans
      XPlan xplan = d.getXPlan();
      if (xplan instanceof SupportsDelayedLPActions &&
          delayedXPlans.contains(xplan)) {
        l = new ArrayList(delayedXPlans);
        l.remove(xplan);
        delayedXPlans = l;
      }
    }
  }

  private Iterator domainIterator() {
    synchronized (lock) {
      // the list is immutable, since we replace it when modified
      return domains.iterator();
    }
  }

  private Domain getDomain(String name) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      if (d.getDomainName().equals(name)) {
        return d;
      }
    }
    return null;
  }

  private Domain getDomain(Class clazz) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      if (d.getClass().equals(clazz)) {
        return d;
      }
    }
    return null;
  }

  private void setBlackboard(Blackboard blackboard) {
    if (this.blackboard != null) {
      if (loggingService.isWarnEnabled()) {
        loggingService.warn(
            "DomainManager: ignoring duplicate call to setBlackboard. " +
            "Blackboard can only be set once.");
      }
      return;
    }
    this.blackboard = blackboard;
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      XPlan xplan = d.getXPlan();
      if (xplan != null) {
        xplan.setupSubscriptions(blackboard);
      }
    }
  }

  private Factory getFactory(String domainName) {
    Domain d = getDomain(domainName);
    return (d == null ? null : d.getFactory());
  }

  private Factory getFactory(Class domainClass) {
    Domain d = getDomain(domainClass);
    return (d == null ? null : d.getFactory());
  }

  /** return a List of all domain-specific factories */
  private List getFactories() {
    ArrayList factories = new ArrayList(size());
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      Factory f = d.getFactory();
      if (f != null) {
        factories.add(f);
      }
    }
    return factories;
  }

  private void invokeDelayedLPActions() {
    for (Iterator iter = delayedXPlansIterator(); iter.hasNext(); ) {
      SupportsDelayedLPActions xplan = 
        (SupportsDelayedLPActions) iter.next();
      xplan.executeDelayedLPActions();
    }
  }

  /** invoke EnvelopeLogicProviders across all currently loaded domains */
  private void invokeEnvelopeLogicProviders(
      EnvelopeTuple tuple, boolean persistenceEnv) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      d.invokeEnvelopeLogicProviders(tuple, persistenceEnv);
    }
  }

  /** invoke MessageLogicProviders across all currently loaded domains */
  private void invokeMessageLogicProviders(DirectiveMessage message) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      d.invokeMessageLogicProviders(message);
    }
  }

  /** invoke RestartLogicProviders across all currently loaded domains */
  private void invokeRestartLogicProviders(MessageAddress cid) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      d.invokeRestartLogicProviders(cid);
    }
  }

  /** invoke ABAChangeLogicProviders across all currently loaded domains */
  private void invokeABAChangeLogicProviders(Set communities) {
    for (Iterator iter = domainIterator(); iter.hasNext(); ) {
      Domain d = (Domain) iter.next();
      d.invokeABAChangeLogicProviders(communities);
    }
  }

  //
  // binding services
  //

  @Override
protected String specifyContainmentPoint() {
    return CONTAINMENT_POINT;
  }

  @Override
protected ComponentDescriptions findInitialComponentDescriptions() {
    // display the agent id
    String cname = agentIdService.getMessageAddress().toString();
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = sb.getService(this, ComponentInitializerService.class, null);
    try {
      List l = new ArrayList(5);

      // setup the root domain
      addDomain(
          l,
          "root", 
          "org.cougaar.core.domain.RootDomain");

      if (loadPlanning) {
        // setup the planning domain
        addDomain(
            l,
            "planning", 
            "org.cougaar.planning.ldm.PlanningDomain");
      }

      /* read domain file */ 
      initializeFromProperties(l);
      initializeFromConfigFiles(l);

      /* read agent.ini */
      try {
        String cp = specifyContainmentPoint();
        ComponentDescription[] cds =
          cis.getComponentDescriptions(cname, cp);
        for (int i = 0; i < cds.length; i++) {
          l.add(cds[i]);
        }      
      } catch (ComponentInitializerService.InitializerException cise) {
        if (loggingService.isWarnEnabled()) {
          loggingService.warn(
              "Cannot find DomainManager configuration for "+cname,
              cise);
        }
      }

      return new ComponentDescriptions(l);
    } catch (Exception e) {
      loggingService.error("Unable to add "+cname+"'s child Components", e);
      return null;
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }
  }

  @Override
public void unload() {
    ServiceBroker sb = getServiceBroker();
    ServiceBroker csb = getChildServiceBroker();

    sb.revokeService(
        DomainForBlackboardService.class, 
        domainForBlackboardSP);
    csb.revokeService(
        DomainRegistryService.class,
        domainRegistrySP);
    sb.revokeService(DomainService.class, domainSP);
    csb.revokeService(XPlanService.class, xplanSP);

    csb = null;

    super.unload();

    if (loggingService != LoggingService.NULL) {
      sb.releaseService(
          this, LoggingService.class, loggingService);
      loggingService = LoggingService.NULL;
    }

    if (agentIdService != null) {
      sb.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  //
  // service providers
  //

  private class DomainRegistryServiceProvider
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (DomainRegistryService.class.isAssignableFrom(
                serviceClass)) {
          return new DomainRegistryServiceImpl();
        }
        return null;
      }

      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service)  {
        if (service instanceof DomainRegistryServiceImpl) {
          DomainRegistryServiceImpl srv = 
            (DomainRegistryServiceImpl) service;
          srv.onRelease();
        }
      }

      private class DomainRegistryServiceImpl
        implements DomainRegistryService {
          public void registerDomain(Domain d) {
            DomainManager.this.registerDomain(d);
          }
          public void unregisterDomain(Domain d) {
            DomainManager.this.unregisterDomain(d);
          }
          private void onRelease() {
            // unregister all registered domains?
          }
        }
    }

  private class XPlanServiceProvider
    implements ServiceProvider {
      private final XPlanServiceImpl xps = new XPlanServiceImpl();

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (XPlanService.class.isAssignableFrom(serviceClass)) {
          return xps;
        }
        return null;
      }

      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }

      private class XPlanServiceImpl
        implements XPlanService {
          public XPlan getXPlan(String domainName) {
            return DomainManager.this.getXPlan(domainName);
          }
          public XPlan getXPlan(Class domainClass) {
            return DomainManager.this.getXPlan(domainClass);
          }
        }
    }

  private class DomainServiceProvider
    implements ServiceProvider {
      private final DomainServiceImpl ds = new DomainServiceImpl();

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (DomainService.class.isAssignableFrom(serviceClass)) {
          return ds;
        }
        return null;
      }

      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service)  {
      }

      private class DomainServiceImpl implements DomainService {
        public Factory getFactory(String domainName) {
          return DomainManager.this.getFactory(domainName);
        }
        public Factory getFactory(Class domainClass) {
          return DomainManager.this.getFactory(domainClass);
        }
        public List getFactories() {
          return DomainManager.this.getFactories();
        }
      }
    }

  private class DomainForBlackboardServiceProvider 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (DomainForBlackboardService.class.isAssignableFrom(
              serviceClass)) {
          Blackboard bb = (Blackboard) requestor;
          return new DomainForBlackboardServiceImpl(bb);
        }
        return null;
      }

      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service)  {
        if (service instanceof DomainForBlackboardServiceImpl) {
          DomainForBlackboardServiceImpl srv = 
            (DomainForBlackboardServiceImpl) service;
          srv.onRelease();
        }
      }

      private class DomainForBlackboardServiceImpl
        implements DomainForBlackboardService {
          public DomainForBlackboardServiceImpl(Blackboard bb) {
            // ignore
          }
          // blackboard registration
          public void setBlackboard(Blackboard blackboard) {
            DomainManager.this.setBlackboard(blackboard);
          }
          // copy of DomainService
          public Factory getFactory(String domainName) {
            return DomainManager.this.getFactory(domainName);
          }
          public Factory getFactory(Class domainClass) {
            return DomainManager.this.getFactory(domainClass);
          }
          public List getFactories() {
            return DomainManager.this.getFactories();
          }
          // new stuff for the blackboard
          public void invokeDelayedLPActions() {
            DomainManager.this.invokeDelayedLPActions();
          }
          public void invokeEnvelopeLogicProviders(
              EnvelopeTuple tuple, boolean persistenceEnv) {
            DomainManager.this.invokeEnvelopeLogicProviders(
                tuple, persistenceEnv);
          }
          public void invokeMessageLogicProviders(DirectiveMessage message) {
            DomainManager.this.invokeMessageLogicProviders(message);
          }
          public void invokeRestartLogicProviders(MessageAddress cid) {
            DomainManager.this.invokeRestartLogicProviders(cid);
          }
          public void invokeABAChangeLogicProviders(Set communities) {
            DomainManager.this.invokeABAChangeLogicProviders(communities);
          }
          // cleanup
          private void onRelease() {
            // set the domain manager's blackboard to null?
          }
        }  
    }

  // 
  // other services
  //
  
  @Override
public String toString() {
    return self+"/DomainManager";
  }


  /** 
   * Set up a Domain from the argument strings.
   *
   * @param descs a list of component-descriptions for all
   *    previously added domains
   * @param domainName the name to register the domain under.
   * @param className the name of the class to instantiate as the domain.
   */
  private void addDomain(List descs, String domainName, 
                         String className) {
    // Unique?
    ComponentDescription found_desc = null;
    for (int i = 0, n = descs.size(); i < n; i++) {
      ComponentDescription cd = (ComponentDescription) descs.get(i);
      if (cd != null && (cd.getParameter() instanceof List)) {
        List l = (List) cd.getParameter();
        if (l.size() > 0 && domainName.equals(l.get(0))) {
          found_desc = cd;
          break;
        }
      }
    }
    if (found_desc != null) {
      if (loggingService.isWarnEnabled()) {
        loggingService.warn(
            "Domain \""+domainName+"\" multiply defined!  "+
            found_desc.getClassname()+" and "+className);
      }
      return;
    }

    // pass the domain-name as a parameter
    Object parameter =
      Collections.singletonList(domainName);

    ComponentDescription desc = 
      new ComponentDescription(
          className+"("+domainName+")",
          CONTAINMENT_POINT+".Domain",
          className,
          null,  // codebase
          parameter,
          null,  // certificate
          null,  // lease
          null); // policy

    descs.add(desc);

    if (loggingService.isDebugEnabled()) {
      loggingService.debug(
          "Will add domain \""+domainName+"\" from class \""+className+"\".");
    }
  }

  private void initializeFromProperties(List descs) {
    Properties props = SystemProperties.getSystemPropertiesWithPrefix(PREFIX);
    for (Enumeration names = props.propertyNames();
        names.hasMoreElements();
        ) {
      String key = (String) names.nextElement();
      if (key.startsWith(PREFIX)) {
        String name = key.substring(PREFIXLENGTH);
        // domain names have no extra "." characters, so we can 
        // use -D arguments to control domain-related facilities.
        if (name.indexOf('.') < 0) {
          String value = props.getProperty(key);
          addDomain(descs, name, value);
        }
      }
    }
  }
  
  private void initializeFromConfigFiles(List descs) {
    if (!readConfigFile || filename == null || filename.equals("")) {
      return;
    }
    InputStream in = null;
    try {
      in = org.cougaar.util.ConfigFinder.getInstance().open(
          filename);
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);

      String line;
      int lc = 0;
      for (line = br.readLine(); line != null; line=br.readLine()) {
        lc++;
        line = line.trim();
        if (line.length() == 0) continue;
        char c;
        if ( (c = line.charAt(0)) == ';' || c == '#' ) {
          continue;
        }
        int l = line.indexOf('=');
        if (l == -1) {
          loggingService.error(filename+" syntax error: line "+lc);
          continue;
        }
        String name = line.substring(0,l).trim();
        String val = line.substring(l+1).trim();
        if (name.length()==0 || val.length()==0) {
          loggingService.error(filename+" syntax error: line "+lc);
          continue;
        }
        addDomain(descs, name, val);
      }
    } catch (Exception ex) {
      if (! (ex instanceof FileNotFoundException)) {
        loggingService.error(filename+" exception: "+ex);
        ex.printStackTrace();
      }
    } finally {
      if (in != null) {
	try {
	  in.close();
	} catch(Exception e) {
	  if (loggingService.isDebugEnabled())
	    loggingService.debug("Failed closing input stream for " + filename, e);
	}
	in = null;
      }
    }
  }
}
