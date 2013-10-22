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

package org.cougaar.core.mobility.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mobility.MobilityClient;
import org.cougaar.core.mobility.MobilityService;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is a base class for the node agent's {@link
 * RootMobilityPlugin}.
 * <p>
 * This component does nothing if it is loaded into regular non-node
 * agents.
 */
public abstract class AbstractMobilityPlugin 
  extends ComponentPlugin 
{

  private static final UnaryPredicate AGENT_CONTROL_PRED = 
    new AgentControlPredicate();
  private static final class AgentControlPredicate implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return (o instanceof AgentControl);
    }
  }
  
  protected MessageAddress agentId;
  protected MessageAddress nodeId;
  protected boolean isNode;

  //private IncrementalSubscription moveSub;
  private IncrementalSubscription controlSub;
  
  protected LoggingService log;

  private MobilityFactory mobilityFactory;

  // the rest is only used if (isNode == true):

  protected NodeControlService nodeControlService;
  protected ThreadService threadService;
  protected WhitePagesService whitePagesService;

  private ServiceBroker nodeSB;
  protected AgentContainer agentContainer;

  private MobilityServiceProviderImpl mobileAgentSP;

  private final List todo = new ArrayList(5);

  @Override
public void load() {
    super.load();

    // get the logger
    log = getServiceBroker().getService(
       this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // get the agentId
    AgentIdentificationService agentIdService = 
      getServiceBroker().getService(
       this,
       AgentIdentificationService.class,
       null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }

    // get the nodeId
    NodeIdentificationService nodeIdService = 
      getServiceBroker().getService(
       this,
       NodeIdentificationService.class,
       null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }

    // either running within a node-agent or leaf-agent
    isNode = (agentId.equals(nodeId));

    // get the mobility factory
    DomainService domain = getServiceBroker().getService(
       this,
       DomainService.class,
       null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain the domain service");
    }
    mobilityFactory = (MobilityFactory) 
      domain.getFactory("mobility");
    if (mobilityFactory == null) {
      if (log.isWarnEnabled()) {
        log.warn(
          "Unable to obtain the agent mobility domain"+
          " (\"mobility\"), please check your "+
          "domain configuration.");
      }
      // okay, will fail most mobility requests
    }
    getServiceBroker().releaseService(
        this, DomainService.class, domain);

    if (isNode) {
      // get control of the node
      nodeControlService = getServiceBroker().getService(
            this,
            NodeControlService.class,
            null);
      if (nodeControlService == null) {
        throw new RuntimeException(
            "Unable to obtain node-control service");
      }
      this.nodeSB = nodeControlService.getRootServiceBroker();
      this.agentContainer = nodeControlService.getRootContainer();

      // get the thread service
      threadService = getServiceBroker().getService(
            this,
            ThreadService.class,
            null);
      if (threadService == null) {
        throw new RuntimeException(
            "Unable to obtain node-level thread service");
      }

      // get the white pages service
      whitePagesService = getServiceBroker().getService(
            this,
            WhitePagesService.class,
            null);
      if (whitePagesService == null) {
        throw new RuntimeException(
            "Unable to obtain the white pages service");
      }

      // advertise our mobility service
      if (mobileAgentSP == null) {
        this.mobileAgentSP = new MobilityServiceProviderImpl();
        nodeSB.addService(MobilityService.class, mobileAgentSP);

        if (log.isDebugEnabled()) {
          log.debug("Created mobile agent registry service for "+nodeId);
        }
      } else {
        if (log.isErrorEnabled()) {
          log.error(
              "Mobile Agent registry service already created? "+
              mobileAgentSP);
        }
      }
    }
  }

  @Override
public void unload() {
    if (isNode) {
      if (mobileAgentSP != null) {
        nodeSB.revokeService(MobilityService.class, mobileAgentSP);
        mobileAgentSP = null;
      }
      if (whitePagesService != null) {
        getServiceBroker().releaseService(
            this, WhitePagesService.class, whitePagesService);
        whitePagesService = null;
      }
      if (nodeControlService != null) {
        getServiceBroker().releaseService(
            this, NodeControlService.class, nodeControlService);
        nodeControlService = null;
      }
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  @Override
protected void setupSubscriptions() {
    // subscribe to control requests that we'll execute
    controlSub = blackboard.subscribe(AGENT_CONTROL_PRED);
    
    if (isNode) {
      if (blackboard.didRehydrate()) {
        if (log.isInfoEnabled()) {
          log.info(
              "Node rehydration for agent mobility is not supported");
        }
      }
    }
  }

  @Override
protected void execute() {
    if (!isNode) return;

    // fire pending blackboard changes
    fireAll();

    // watch control requests
    if (controlSub.hasChanged()) {
      // adds
      Enumeration en = controlSub.getAddedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        addedAgentControl(control);
      }
      // changes
      en = controlSub.getChangedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        changedAgentControl(control);
      }
      // removes
      en = controlSub.getRemovedList();
      while (en.hasMoreElements()) {
        AgentControl control = (AgentControl) en.nextElement();
        removedAgentControl(control);
      }
    }
  }

  protected AgentControl findAgentControl(UID controlUID) {
    return (AgentControl) query(controlSub, controlUID);
  }


  /** control request for a local agent. */
  protected abstract void addedAgentControl(AgentControl control);

  /** a control was changed. */
  protected abstract void changedAgentControl(AgentControl control);

  /** a control was removed. */
  protected abstract void removedAgentControl(AgentControl control);

  /** an agent registers as a mobile agent in the local node. */
  protected abstract void registerAgent(
      MessageAddress id, 
      ComponentDescription desc, 
      MobilityClient agent);

  /** an agent unregisters itself from the local mobility registry. */
  protected abstract void unregisterAgent(
      MessageAddress id);


  // more node-only code:

  protected void queue(Runnable r) {
    Schedulable sched = 
      threadService.getThread(this, r, r.toString());
    sched.start();
  }

  protected AgentControl createAgentControl(
      UID moveControlUID,
      MessageAddress destNode,
      TransferTicket transferTicket) {
    if (mobilityFactory == null) {
      throw new RuntimeException(
          "The agent mobility domain is not available on node "+
          nodeId);
    }
    return
      mobilityFactory.createAgentControl(
          moveControlUID,
          destNode,
          transferTicket);
  }

  protected void fireLater(Runnable r) {
    synchronized (todo) {
      todo.add(r);
    }
    blackboard.signalClientActivity();
  }


  private static UniqueObject query(
      CollectionSubscription sub, UID uid) {
    Collection real = sub.getCollection();
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        Object o = iter.next();
        if (o instanceof UniqueObject) {
          UniqueObject uo = (UniqueObject) o;
          UID x = uo.getUID();
          if (uid.equals(x)) {
            return uo;
          }
        }
      }
    }
    return null;
  }

  private void fireAll() {
    int n;
    List l;
    synchronized (todo) {
      n = todo.size();
      if (n <= 0) {
        return;
      }
      l = new ArrayList(todo);
      todo.clear();
    }
    for (int i = 0; i < n; i++) {
      Runnable r = (Runnable) l.get(i);
      r.run();
    }
  }
  
  private class MobilityServiceProviderImpl
  implements ServiceProvider {

    // single dummy service instance
    private final MobilityService SINGLE_SERVICE_INSTANCE =
      new MobilityService() {
      };

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      // check service class
      if (serviceClass != MobilityService.class) {
        throw new IllegalArgumentException(
            "Unsupported service "+serviceClass);
      }
      // assert that the requestor is an agent
      if (!(requestor instanceof MobilityClient)) {
        throw new RuntimeException(
            "Expecting a MobilityClient requestor, not "+requestor);
      }
      MobilityClient agent = (MobilityClient) requestor;
      MessageAddress id = agent.getAgentIdentifier();

      // get the agent's description from its container
      ComponentDescription desc = 
        agentContainer.getAgentDescription(id);
      if (desc == null) {
        throw new RuntimeException(
            "Unable to get agent \""+id+"\"'s ComponentDescription"+
            " from the agent container ("+agentContainer+")");
      }

      registerAgent(id, desc, agent);

      if (log.isDebugEnabled()) {
        log.debug("Registered agent "+id+" for agent mobility");
      }

      // create a dummy service instance
      return SINGLE_SERVICE_INSTANCE;
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // check service instance
      if (service != SINGLE_SERVICE_INSTANCE) {
        throw new IllegalArgumentException(
            "Wrong service instance "+service);
      }
      MobilityClient agent = (MobilityClient) requestor;
      MessageAddress id = agent.getAgentIdentifier();
      unregisterAgent(id);
    }
  }
}
