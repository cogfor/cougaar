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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mobility.MobilityClient;
import org.cougaar.core.mobility.MobilityService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceObject;
import org.cougaar.core.persist.PersistenceServiceForAgent;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.IdentityHashSet;

/**
 * This component rehydrates the agent persistence object
 * and acts as a proxy for agent mobility state capture.
 * 
 * @see RehydrateLate 
 */
public final class RehydrateEarly
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;

  private MessageAddress localAgent;
  private MessageAddress localNode;

  private AgentStateModelService agentModel;

  private PersistenceServiceForAgent psfa;
  private PersistenceClient pc;

  private final Set moveWatchers = new IdentityHashSet();

  private MobilityNotificationSP mnsp;
  private RehydrateLoadSP rlsp;

  private PersistenceObject persistenceObject;

  private boolean rehydrated;

  private boolean is_moving;

  private MobilityService mobilityService;
  private MobilityClient mobilityClient;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    localAgent = find_local_agent();

    get_logger();

    localNode = find_local_node();

    register_persistence();

    register_mobility();

    // rehydrate from mobile state (if available)
    load_early();

    mnsp = new MobilityNotificationSP();
    sb.addService(MobilityNotificationService.class, mnsp);

    rlsp = new RehydrateLoadSP();
    sb.addService(RehydrateLoadService.class, rlsp);

    // called later via RehydrateLoadService:
    //load_late();
  }

  @Override
public void start() {
    super.start();

    // once loaded we revoke our load_late service
    if (rlsp != null) {
      sb.revokeService(RehydrateLoadService.class, rlsp);
      rlsp = null;
    }
  }

  @Override
public void suspend() {
    super.suspend();

    if (is_moving) {
      // only capture our mobile state if we're moving, as opposed to
      // agent/node shutdown 
      is_moving = false;
      persist_blackboard();
    }

    if (psfa != null) {
      psfa.suspend();
    }
  }

  @Override
public void unload() {
    super.unload();

    if (mnsp != null) {
      sb.revokeService(MobilityNotificationService.class, mnsp);
      mnsp = null;
    }

    unregister_mobility();
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

  private void get_logger() {
    log = sb.getService(this, LoggingService.class, null);
    String prefix = localAgent+": ";
    log = LoggingServiceWithPrefix.add(log, prefix);
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

  private void load_early() {
    if (persistenceObject == null) {
      if (log.isInfoEnabled()) {
        log.info("No mobile persistence data found");
      }
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Rehydrating mobile persistence data before loading"+
          " high priority components");
    }
    rehydrate(persistenceObject);
    persistenceObject = null;
    rehydrated = true;
  }

  private void load_late() {
    if (rehydrated) {
      if (log.isDebugEnabled()) {
        log.debug("Already rehydrated from mobile state");
      } 
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Rehydrating now that we've loading our high priority"+
          " components");
    }
    rehydrate(null);
    rehydrated = true;
  }

  private void set_state(Object o) {
    if (!(o instanceof PersistenceObject)) {
      if (log.isErrorEnabled()) {
        log.error(
            "Invalid setState("+
            (o == null ?
             "null" :
             o.getClass().getName()+" "+o)+
            ")");
      }
      return;
    }

    // fill in our mobile persistence state
    persistenceObject = (PersistenceObject) o;
    if (log.isInfoEnabled()) {
      log.info(
          "Set persistence state ("+
          (persistenceObject == null ?
           "null" :
           persistenceObject.getClass().getName())+
          ")");
    }
  }

  private Object get_state() {
    if (log.isInfoEnabled()) {
      log.info(
          "Get persistence state ("+
          (persistenceObject == null ?
           "null" :
           persistenceObject.getClass().getName())+
          ")");
    }

    // we should be suspended now!
    return persistenceObject;
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName()+"_agent";
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          // we trigger rehydration but don't have state
          //
          // must return a mutable list!
          return new ArrayList(0);
        }
      };
    psfa = 
      sb.getService(
          pc, PersistenceServiceForAgent.class, null);
  }

  private void unregister_persistence() {
    if (psfa != null) {
      sb.releaseService(
          pc, PersistenceServiceForAgent.class, psfa);
      psfa = null;
      pc = null;
    }
  }

  private void register_mobility() {
    if (localAgent == null || localAgent.equals(localNode)) {
      if (log.isDebugEnabled()) {
        log.debug("NodeAgent "+localNode+" not registering for mobility");
      }
      return;
    }

    // get the agent state model, required for mobility
    agentModel = sb.getService(this, AgentStateModelService.class, null); 
    if (agentModel == null) {
      throw new RuntimeException(
          "Unable to obtain AgentStateModelService");
    }

    // create our mobility proxy for the agent
    mobilityClient = new MobilityClient() {

      public MessageAddress getAgentIdentifier() {
        return localAgent;
      }

      public void onDispatch(MessageAddress destinationNode) {
        announce_move(destinationNode);
      }

      public void setState(Object o) {
        set_state(o);
      }
      public Object getState() {
        return get_state();
      }

      // forward all agent state transitions
      public void initialize()   { agentModel.initialize(); }
      public void load()         { agentModel.load(); }
      public void start()        { agentModel.start(); }
      public void suspend()      { agentModel.suspend(); }
      public void resume()       { agentModel.resume(); }
      public void stop()         { agentModel.stop(); }
      public void halt()         { agentModel.halt(); }
      public void unload()       { agentModel.unload(); }
      public int getModelState() { return agentModel.getModelState(); }
    };

    mobilityService = sb.getService(mobilityClient, MobilityService.class, null);
    if (mobilityService == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Unable to obtain MobilityService"+
            ", mobility is disabled");
      }
    }
  }

  private void unregister_mobility() {
    if (mobilityService != null) {
      sb.releaseService(
          mobilityClient,
          MobilityService.class,
          mobilityService);
      mobilityService = null;
      mobilityClient = null;
    }

    if (agentModel != null) {
      sb.releaseService(
          this, AgentStateModelService.class, agentModel); 
      agentModel = null;
    }
  }

  private void rehydrate(
      PersistenceObject pObj) {
    psfa.rehydrate(pObj);
  }

  private void persist_blackboard() {
    // suspending after "announce_move", so capture blackboard state as
    // our mobile persistenceObject.
    BlackboardForAgent bb = sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null) {
      throw new RuntimeException(
          "Unable to obtain BlackboardForAgent"+
          ", required for mobility persist");
    }
    persistenceObject = bb.getPersistenceObject();
    sb.releaseService(this, BlackboardForAgent.class, bb);
    bb = null;
  }

  private void announce_move(MessageAddress destinationNode) {
    is_moving = true;

    if (log.isInfoEnabled()) {
      log.info("Moving agent from "+localNode+" to "+destinationNode);
    }

    // probably safe, but synchronize & copy anyways
    List l;
    synchronized (moveWatchers) {
      l = new ArrayList(moveWatchers);
    }
    for (int i = 0, n = l.size(); i < n; i++) {
      MobilityNotificationClient mnc = (MobilityNotificationClient) l.get(i);
      mnc.movingTo(destinationNode);
    }
  }

  private void add_move_watcher(MobilityNotificationClient mnc) {
    synchronized (moveWatchers) {
      moveWatchers.add(mnc);
    }
  }
  private void remove_move_watcher(MobilityNotificationClient mnc) {
    synchronized (moveWatchers) {
      moveWatchers.remove(mnc);
    }
  }

  private final class MobilityNotificationSP
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!(MobilityNotificationService.class.isAssignableFrom(
                serviceClass))) {
          return null;
        }
        if (!(requestor instanceof MobilityNotificationClient)) {
          throw new IllegalArgumentException(
              "Requestor must implement MobilityNotificationClient");
        }
        MobilityNotificationClient mnc = 
          (MobilityNotificationClient) requestor;
        add_move_watcher(mnc);
        return new MobilityNotificationS(mnc);
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
        if (service instanceof MobilityNotificationS) {
          MobilityNotificationClient mnc = 
            ((MobilityNotificationS) service).mnc;
          remove_move_watcher(mnc);
        }
      }
    }

  private static final class MobilityNotificationS
    implements MobilityNotificationService {
      public final MobilityNotificationClient mnc;
      public MobilityNotificationS(
          MobilityNotificationClient mnc) {
        this.mnc = mnc;
      }
      // nothing to do
    }

  private final class RehydrateLoadSP
    implements ServiceProvider {
      private final RehydrateLoadService rls;
      public RehydrateLoadSP() {
        rls = new RehydrateLoadService() {
          public void rehydrate() {
            load_late();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (RehydrateLoadService.class.isAssignableFrom(serviceClass)) {
          return rls;
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
