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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component loads the initial set of agents into the node and
 * persists the names of dynamically added/removed agents.
 *
 * @property org.cougaar.core.node.ignoreRehydratedAgentList
 *   Ignore the list of agents from the rehydrated state of the
 *   NodeAgent, if any. Defaults to false. Set to true to disable
 *   this feature and always use the list of agents from the
 *   ComponentInitializerService.
 */
public final class AgentLoader
extends GenericStateModelAdapter
implements Component
{

  public static final String IGNORE_REHYDRATED_AGENT_LIST_PROP =
    "org.cougaar.core.node.ignoreRehydratedAgentList";

  private static final boolean ignoreRehydratedAgentDescs =
    SystemProperties.getBoolean(IGNORE_REHYDRATED_AGENT_LIST_PROP);

  private ServiceBroker sb;

  private List initialAgents;

  private LoggingService log;

  private ServiceBroker rootsb;
  private AgentContainer agentContainer;

  private MessageAddress localAgent;

  private PersistenceService ps;
  private PersistenceClient pc;

  private BlackboardForAgent bb;

  private RegisterAgentServiceProvider rasp;

  private boolean addingAgents;
  private List initialDescs;

  private final Set activeAgentAddrs = new HashSet();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  /**
   * Expecting a list of agent names.
   */
  public void setParameter(Object o) {
    List l = (List) o;
    int n = (l == null ? 0 : l.size());
    for (int i = 0; i < n; i++) {
      Object o1 = l.get(i);
      if (!(o1 instanceof String)) {
        throw new IllegalArgumentException(
            "List element["+i+"/"+n+"] is "+
            (o1 == null ? "null" : o1.getClass().getName()));
      }
      if (initialAgents == null) {
        initialAgents = new ArrayList();
      } else if (initialAgents.contains(o1)) {
        // n^2 duplicate check, but n is small...
        continue;
      }
      initialAgents.add(o1);
    }
  }

  /**
   * Add Agents and their child Components (Plugins, etc) to this Node.
   * <p>
   * This first checks the persistence snapshot to resume the agents
   * that were running.  If there is no snapshot (or it has been
   * disabled) then the component initializer service is used, which
   * reads the list of agents from the configuration files (INI/XML/DB).
   * <p>
   * Note that the agents are added in bulk, which loads them in
   * sequence in our thread.
   */
  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    agentContainer = ncs.getRootContainer();
    sb.releaseService(this, NodeControlService.class, ncs);

    localAgent = find_local_agent();

    register_persistence();

    bb = sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null && log.isWarnEnabled()) {
      log.warn("Unable to obtain BlackboardForAgent");
    }

    // advertise our agent add/remove listener
    rasp = new RegisterAgentServiceProvider();
    rootsb.addService(RegisterAgentService.class, rasp);

    ComponentDescription[] agentDescs = null;

    // rehydrate list of agent descriptions
    Object o = rehydrate();
    if (o instanceof List) {
      List l = (List) o;
      agentDescs = (ComponentDescription[]) 
        l.toArray(
            new ComponentDescription[l.size()]);
      if (log.isInfoEnabled()) {
        log.info(
            "Persistence snapshot contains a list of "+
            agentDescs.length+" agents");
      }
    }
    o = null;

    if (agentDescs != null && ignoreRehydratedAgentDescs) {
      if (log.isInfoEnabled()) {
        log.info(
            "Ignoring rehydrated list of "+
            agentDescs.length + " agents");
      }
      agentDescs = null;
    }

    // Look for agents in the ComponentInitializerService
    if (agentDescs == null) {
      agentDescs = readAgentsFromConfig();
    }

    addAgents(agentDescs);
  }

  @Override
public void unload() {
    super.unload();

    if (rasp != null) {
      rootsb.revokeService(RegisterAgentService.class, rasp);
      rasp = null;
    }

    if (bb != null) {
      sb.releaseService(this, BlackboardForAgent.class, bb);
      bb = null;
    }

    unregister_persistence();
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
  
  private ComponentDescription[] readAgentsFromConfig() {
    ComponentDescription[] agentDescs = null;

    // backwards compatibility for non-XML configs!
    try {
      ComponentInitializerService cis = sb.getService(this, ComponentInitializerService.class, null);
      boolean oldStyle = !cis.includesDefaultComponents();
      if (oldStyle) {
        if (log.isInfoEnabled()) {
          log.info("Asking component initializer service for agents");
        }
        // get the agents - this gives _anything_ below AgentManager,
        // so must extract out just the .Agent's later
        agentDescs =
          cis.getComponentDescriptions(
              localAgent.getAddress(),
              "Node.AgentManager");
        if (agentDescs == null) {
          agentDescs = new ComponentDescription[0];
        }
      }
      sb.releaseService(this, ComponentInitializerService.class, cis);
      if (oldStyle) {
        return agentDescs;
      }
    } catch (Exception e) {
      throw new Error("Couldn't initialize list of agents", e);
    }

    if (initialAgents == null) {
      if (log.isWarnEnabled()) {
        log.warn("Node "+localAgent+" contains zero agents");
      }
      return new ComponentDescription[0];
    }

    int n = initialAgents.size();

    if (log.isInfoEnabled()) {
      log.info("Will add agents["+n+"]: "+initialAgents);
    }

    ComponentDescription[] ret = 
      new ComponentDescription[n];

    for (int i = 0; i < n; i++) {
      String name = (String) initialAgents.get(i);
      ComponentDescription desc = 
        new ComponentDescription(
            "org.cougaar.core.agent.AgentImpl("+name+")",
            "Node.AgentManager.Agent",
            "org.cougaar.core.agent.AgentImpl",
            null, //codebase
            Collections.singletonList(name), //params
            null, //certificate
            null, //lease
            null, //policy
            ComponentDescription.PRIORITY_COMPONENT);
      ret[i] = desc;
    }

    return ret;
  }

  private void addAgents(ComponentDescription[] agentDescs) {
    ComponentDescriptions cds = new ComponentDescriptions(agentDescs);
    List cdcs = 
      cds.extractInsertionPointComponent(
          "Node.AgentManager.Agent");
    if (log.isDebugEnabled()) {
      log.debug("Adding "+cdcs.size()+" agents: "+cdcs);
    }

    addingAgents = true;
    initialDescs = cdcs;

    for (int i = 0, n = cdcs.size(); i < n; i++) {
      ComponentDescription cd = (ComponentDescription)
        cdcs.get(i);
      try {
        agentContainer.add(cd);
      } catch (Exception e) {
        log.error(
            "Unable to add agent "+cd.getParameter()+
            ", not loading agents: "+cdcs.subList(i, n));
        break;
      }
    }

    addingAgents = true;
    initialDescs = null;
  }

  private Object captureState() {
    if (addingAgents) {
      if (log.isInfoEnabled()) {
        int n = (initialDescs == null ? 0 : initialDescs.size());
        log.info(
            "Asked to \"captureState\" while loading,"+
            " which would find a partial list of agents,"+
            " so instead return our initial agent list["+n+"]");
        if (log.isDebugEnabled()) {
          log.debug("initialDescs["+n+"]="+initialDescs);
        }
      }
      return initialDescs;
    }

    // FIXME replace with just the agent addrs?
    //
    // this should be possible once there's a fixed agent 
    // class and agent parameters are limited to the addr.

    // get the map of (addr -> desc)
    Map agents = agentContainer.getAgents();
    // remove ourselves
    agents.remove(localAgent); 
    // convert to desc list
    List ret = new ArrayList(agents.values()); 

    return ret;
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName();
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          Object o = captureState();
          // must return mutable list!
          List l = new ArrayList(1);
          l.add(o);
          return l;
        }
      };
    ps = 
      sb.getService(
          pc, PersistenceService.class, null);
  }

  private void unregister_persistence() {
    if (ps != null) {
      sb.releaseService(
          pc, PersistenceService.class, ps);
      ps = null;
      pc = null;
    }
  }

  private void persistNow() {
    if (addingAgents) {
      if (log.isInfoEnabled()) {
        log.info("Still loading, delaying persistNow until active");
      }
      return;
    }

    if (bb == null) {
      if (log.isInfoEnabled()) {
        log.info("Unable to persistNow, BlackboardService is null");
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Asking our blackboard to persist, to trigger a"+
          " full node-agent snapshot that will contain the"+
          " modified list of agents running on this node");
    }
    bb.persistNow();
    if (log.isInfoEnabled()) {
      log.info("Completed persistNow()");
    }
  }

  private Object rehydrate() {
    RehydrationData rd = ps.getRehydrationData();
    if (rd == null) {
      if (log.isInfoEnabled()) {
        log.info("No rehydration data found");
      }
      return null;
    }

    // extract our ComponentDescriptions
    List l = rd.getObjects();
    rd = null;
    int lsize = (l == null ? 0 : l.size());
    if (lsize < 1) {
      if (log.isInfoEnabled()) {
        log.info("Invalid rehydration list? "+l);
      }
      return null;
    }
    Object o = l.get(0);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("Null rehydration state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Found rehydrated state");
      if (log.isDetailEnabled()) {
        log.detail("state is "+o);
      }
    }

    return o;
  }

  private void add(MessageAddress addr) {
    if (localAgent.equals(addr)) {
      // ignore self
      return;
    }

    boolean changed;
    synchronized (activeAgentAddrs) {
      changed = activeAgentAddrs.add(addr);
    }

    if (changed) {
      persistNow();
    }
  }

  private void remove(MessageAddress addr) {
    boolean changed;
    synchronized (activeAgentAddrs) {
      changed = activeAgentAddrs.remove(addr);
    }

    if (changed) {
      persistNow();
    }
  }

  private class RegisterAgentServiceProvider 
    implements ServiceProvider {

      private final RegisterAgentService myService =
        new RegisterAgentService() {
          public void addAgent(MessageAddress addr) {
            add(addr);
          }
          public void removeAgent(MessageAddress addr) {
            remove(addr);
          }
        };

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass == RegisterAgentService.class) {
          return myService;
        } else {
          throw new IllegalArgumentException(
              "Can only provide RegisterAgentService!");
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
