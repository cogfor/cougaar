/*
 * <copyright>
 *
 *  Copyright 2001-2004 Mobile Intelligence Corp
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

package org.cougaar.community.manager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.community.CommunityImpl;
import org.cougaar.community.CommunityDescriptor;
import org.cougaar.community.RelayAdapter;
import org.cougaar.community.CommunityUpdateListener;
import org.cougaar.community.BlackboardClient;
import org.cougaar.community.CommunityServiceConstants;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;

import org.cougaar.core.util.UID;

/**
 * Helper class used to distribute new/updated CommunityDescriptor objects to
 * interested nodes and agents.
 */
public class CommunityDistributer implements CommunityServiceConstants {

  private long updateInterval;
  private long cacheExpiration;
  private boolean nodesOnly = true;

  private WhitePagesService whitePagesService;
  private ServiceBroker serviceBroker;
  private UIDService uidService;
  private LoggingService logger;
  private CommunityUpdateListener updateListener;
  private MyBlackboardClient blackboardClient;
  private BindingSite bindingSite;
  private MessageAddress agentId;

  private Map communities;

  // Map of DescriptorEntry objects.  Allows multiple communities to be
  // managed.
  private Map descriptors = Collections.synchronizedMap(new HashMap());
  class DescriptorEntry {
    String name;
    RelayAdapter ra;
    Set nodeTargets = Collections.synchronizedSet(new HashSet());
    Set unresolvedAgents = Collections.synchronizedSet(new HashSet());
    long lastSent = 0;
    boolean didChange = true;
    boolean doRemove = false;
    DescriptorEntry(String name) {
      this.name = name;
    }
  }

  /**
   * Constructor.
   * @param bs  BindingSite from CommunityManager.
   * @param nodesOnly       True if CommunityDescriptors are only sent to node
   *                        agents
   * @param cul             Listener object to receive community descriptor updates
   * @param communities     Communities managed by CommunityManager
   *
   */
  public CommunityDistributer(BindingSite             bs,
                              boolean                 nodesOnly,
                              CommunityUpdateListener cul,
                              Map                     communities) {
    this.communities = communities;
    this.bindingSite = bs;
    this.nodesOnly = nodesOnly;
    this.updateListener = cul;
    this.blackboardClient = new MyBlackboardClient(bs);
    this.serviceBroker = getServiceBroker();
    this.agentId = getAgentId();
    this.logger =
        (LoggingService)serviceBroker.getService(this, LoggingService.class, null);
    this.whitePagesService =
      (WhitePagesService)serviceBroker.getService(this, WhitePagesService.class, null);
    getSystemProperties();
    initUidService();
  }

  protected void getSystemProperties() {
    try {
      updateInterval =
          Long.parseLong(System.getProperty(UPDATE_INTERVAL_PROPERTY,
                                            Long.toString(DEFAULT_UPDATE_INTERVAL)));
      cacheExpiration =
          Long.parseLong(System.getProperty(CACHE_EXPIRATION_PROPERTY,
                                            Long.toString(DEFAULT_CACHE_EXPIRATION)));
    } catch (Exception ex) {
      if (logger.isWarnEnabled()) {
        logger.warn(agentId + ": Exception setting parameter from system property", ex);
      }
    }
  }

  protected ServiceBroker getServiceBroker() {
    return bindingSite.getServiceBroker();
  }

  protected MessageAddress getAgentId() {
    AgentIdentificationService ais =
        (AgentIdentificationService)getServiceBroker().getService(this,
        AgentIdentificationService.class, null);
    MessageAddress addr = ais.getMessageAddress();
    getServiceBroker().releaseService(this, AgentIdentificationService.class, ais);
    return addr;
  }

  /**
   * Initialize UIDService using ServiceAvailableListener if service not
   * immediately available.
   */
  private void initUidService() {
    ServiceBroker sb = getServiceBroker();
    if (sb.hasService(org.cougaar.core.service.UIDService.class)) {
      uidService = (UIDService)sb.getService(this, UIDService.class, null);
    } else {
      sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent sae) {
          if (sae.getService().equals(UIDService.class)) {
            uidService = (UIDService)getServiceBroker().getService(this, UIDService.class, null);
          }
        }
      });
    }
  }

  /**
   * Get Unique identifier.
   * @return Unique ID
   */
  protected UID getUID() {
    return uidService != null ? uidService.nextUID() : null;
  }

  /**
   * Publishes pending CommunityDescriptors.
   */
  private void publishDescriptors() {
    if (logger.isDetailEnabled()) {
      logger.detail("publishDescriptors");
    }
    long now = now();
    List l;
    synchronized (descriptors) {
      l = new ArrayList(descriptors.values());
    }
    for (Iterator it = l.iterator(); it.hasNext();) {
      DescriptorEntry de = (DescriptorEntry) it.next();
      //CommunityImpl community =
      //    (CommunityImpl)((CommunityImpl)communities.get(de.name)).clone();
      CommunityImpl community = (CommunityImpl)communities.get(de.name);
      community.setLastUpdate(now);
      ((CommunityDescriptorImpl)de.ra.getContent()).community = community;
      if (de.lastSent == 0) {
        if (!de.nodeTargets.isEmpty()) {
          updateTargets(de.ra, nodesOnly ? de.nodeTargets : de.ra.getInterestedAgents());
          de.didChange = false;
          de.lastSent = now;
          if (blackboardClient != null) {
            blackboardClient.publish(de.ra, BlackboardClient.ADD);
            if (logger.isDebugEnabled()) {
              logger.debug("publishAdd: " + de.ra +
                           " targets=" + de.ra.getTargets().size() +
                           " size=" + ((CommunityDescriptor)de.ra.getContent()).getCommunity().getEntities().size());
            }
          }
          if (de.nodeTargets.contains(agentId)) {
            //updateListener.updateCommunity((CommunityImpl)community.clone());
            updateListener.updateCommunity(community);
          }
        }
      } else {
        if ((de.didChange && (now > (de.lastSent + updateInterval))) ||
            (cacheExpiration != NEVER && (now > (de.lastSent + (cacheExpiration / 2))))) {
          // publish changed descriptor
          updateTargets(de.ra, nodesOnly ? de.nodeTargets : de.ra.getInterestedAgents());
          de.didChange = false;
          de.lastSent = now;
          if (blackboardClient != null) {
            blackboardClient.publish(de.ra, BlackboardClient.CHANGE);
            if (logger.isDebugEnabled()) {
              logger.debug("publishChange: " + de.ra +
                           " targets=" + de.ra.getTargets().size() +
                           " size=" + ((CommunityDescriptor)de.ra.getContent()).getCommunity().getEntities().size());
            }
          }
          if (de.nodeTargets.contains(agentId)) {
            //updateListener.updateCommunity((CommunityImpl)community.clone());
            updateListener.updateCommunity(community);
          }
        } else {
          if (de.doRemove) { // remove descriptor
            if (blackboardClient != null) {
              blackboardClient.publish(de.ra, BlackboardClient.REMOVE);
            }
            if (de.nodeTargets.contains(agentId)) {
              //updateListener.removeCommunity((CommunityImpl)community.clone());
              updateListener.removeCommunity(community);
            }
            descriptors.remove(de.name);
            if (logger.isDebugEnabled()) {
              logger.debug("publishRemove: " + de.ra);
            }
          }
        }
      }
    }
  }

  /**
   * Enable automatic update of CommunityDescriptors for named community.
   * @param communityName  Community to update
   * @param agents     Initial set of targets
   */
  protected void add(String communityName, Set agents) {
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de == null) {
      de = new DescriptorEntry(communityName);
      CommunityDescriptorImpl cd = new CommunityDescriptorImpl(agentId, null, getUID());
      de.ra = new RelayAdapter(agentId, cd, cd.getUID());
      descriptors.put(communityName, de);
      addTargets(communityName, agents);
    }
    blackboardClient.startTimer();
  }

  /**
   * Enable automatic update of CommunityDescriptors for named community.
   * @param ra  RelayAdapter associated with previously created CommunityDescriptor
   */
  protected void add(RelayAdapter ra) {
    CommunityDescriptorImpl cd = (CommunityDescriptorImpl)ra.getContent();
    String communityName = cd.getName();
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de == null) {
      de = new DescriptorEntry(communityName);
      de.ra = ra;
      descriptors.put(communityName, de);
      addTargets(communityName, ra.getInterestedAgents());
    }
    blackboardClient.startTimer();
  }

  protected boolean contains(String communityName) {
    return descriptors.containsKey(communityName);
  }

  protected Set getTargets(String communityName) {
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null && de.ra != null) {
      return de.ra.getTargets();
    } else {
      return Collections.EMPTY_SET;
    }
  }

  /**
   * Adds new targets to receive CommunityDescriptor updates.
   * @param communityName  Community
   * @param targets  New targets
   */
  protected void addTargets(String communityName, Set targets) {
    if (logger.isDebugEnabled()) {
      logger.debug("addTargets:" +
                   " community=" + communityName +
                   " agents=" + targets);
    }
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null) {
      de.ra.getInterestedAgents().addAll(targets);
      Set agentsToAdd = new HashSet(targets);
      for (Iterator it = agentsToAdd.iterator(); it.hasNext(); ) {
        String targetName = (String)it.next();
        findNodeTargets(MessageAddress.getMessageAddress(targetName), communityName);
      }
    }
  }

  /**
   * Removes targets to receive CommunityDescriptor updates.
   * @param communityName  Community
   * @param agentNames  Targets to remove
   */
  protected void removeTargets(String communityName, Set agentNames) {
    if (logger.isDetailEnabled()) {
      logger.detail("removeTargets:" +
                   " community=" + communityName +
                   " agents=" + agentNames);
    }
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null) {
      de.ra.getInterestedAgents().removeAll(agentNames);
    }
  }

  /**
   * Disables CommunityDescriptor updates for named community.  If a
   * CommunityDescriptor Relay was previously published it is rescinded via
   * a blackboard publishRemove.
   * @param communityName  Name of community
   */
  protected void remove(String communityName) {
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null) {
      de.doRemove = true;
    }
  }

  /**
   * Update Relay target set.
   * @param ra      Relay to update
   * @param targets Targets
   */
  private void updateTargets(RelayAdapter ra, Set targets) {
    Set targetsToAdd = new HashSet();
    synchronized (targets) {
      targetsToAdd.addAll(targets);
    }
    for (Iterator it = targetsToAdd.iterator(); it.hasNext();) {
      MessageAddress target = (MessageAddress)it.next();
      if (!ra.getTargets().contains(target)) {
        ra.addTarget(target);
      }
    }
    resolveAgents();
  }

  /**
   * Notify targets of a change in community state.
   * @param communityName  Name of changed community
   */
  protected void update(String communityName) {
    if (logger.isDetailEnabled()) {
      logger.detail("update:" +
                   " community=" + communityName);
    }
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null) {
      de.didChange = true;
      blackboardClient.timer.expire();
    }
  }

  /**
   * Notify targets of a change in community state.
   * @param communityName  Name of changed community
   * @param type  Type of change
   * @param what  Entity affected by change
   * @deprecated
   */
  protected void update(String communityName, int type, String what) {
    if (logger.isDetailEnabled()) {
      logger.detail("update:" +
                   " community=" + communityName +
                   " type=" + CommunityChangeEvent.getChangeTypeAsString(type) +
                   " whatChanged=" + what);
    }
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    if (de != null) {
      de.didChange = true;
      blackboardClient.timer.expire();
    }
  }

  /**
   * Get CommunityDescriptor associated with named community.
   * @param communityName Name of community
   * @return  CommunityDescriptor for community
   */
  protected CommunityDescriptor get(String communityName) {
    DescriptorEntry de = (DescriptorEntry)descriptors.get(communityName);
    return de != null ? (CommunityDescriptor)de.ra.getContent() : null;
  }

  /**
   * Find an agents node by looking in WhitePages.  Add node address to
   * Relay target set.
   * @param agentId  MessageAddress of agent
   * @param communityName Name of associated community
   */
  private void findNodeTargets(final MessageAddress agentId,
                               final String communityName) {
    if (logger.isDetailEnabled()) {
      logger.detail("findNodeTargets:" +
                   " community=" + communityName +
                   " agent=" + agentId);
    }
    Callback cb = new Callback() {
      public void execute(Response resp) {
        if (resp.isAvailable()) {
          if (resp.isSuccess()) {
            AddressEntry entry = ((Response.Get)resp).getAddressEntry();
            try {
              if (entry != null) {
                URI uri = entry.getURI();
                MessageAddress node = MessageAddress.getMessageAddress(uri.
                    getPath().substring(1));
                DescriptorEntry de = (DescriptorEntry) descriptors.get(
                    communityName);
                if (de != null) {
                  if (!de.nodeTargets.contains(node)) {
                    de.nodeTargets.add(node);
                    de.didChange = true;
                  }
                }
              } else {
                if (logger.isDetailEnabled()) {
                  logger.detail("AddressEntry is null: agent=" + agentId);
                }
                DescriptorEntry de = (DescriptorEntry) descriptors.get(
                    communityName);
                if (de != null) {
                  de.unresolvedAgents.add(agentId);
                }
              }
            } catch (Exception ex) {
              if (logger.isErrorEnabled()) {
                logger.error("Exception in addNodeToTargets:", ex);
              }
            } finally {
              resp.removeCallback(this);
            }
          }
        }
      }
    };
    whitePagesService.get(agentId.toString(), "topology", cb);
  }

  // Find node for any agents which are still unresolved.
  private void resolveAgents() {
    List l;
    synchronized (descriptors) {
      l = new ArrayList(descriptors.values());
    }
    for (Iterator it = l.iterator(); it.hasNext();) {
      DescriptorEntry de = (DescriptorEntry) it.next();
      if (!de.unresolvedAgents.isEmpty()) {
        List agents;
        synchronized (descriptors) {
          agents = new ArrayList(de.unresolvedAgents);
          de.unresolvedAgents.clear();
        }
        for (Iterator it1 = agents.iterator(); it1.hasNext();) {
          findNodeTargets((MessageAddress)it1.next(), de.name);
        }
      }
    }
  }

  /**
   * Returns current time as a long.
   * @return long Current time
   */
  private long now() {
    return System.currentTimeMillis();
  }

  class MyBlackboardClient extends BlackboardClient {

    private BBWakeAlarm timer;

    public MyBlackboardClient(BindingSite bs) {
      super(bs);
    }

    protected void startTimer() {
      if (timer == null) {
        AlarmService as = getAlarmService();
        if (as != null) {
          timer = new BBWakeAlarm(now() + updateInterval);
          as.addRealTimeAlarm(timer);
        }
      }
    }

    public void setupSubscriptions() {
    }

    public void execute() {
      super.execute();
      if (timer != null && timer.hasExpired()) {
        publishDescriptors();
        timer = new BBWakeAlarm(now() + updateInterval);
        alarmService.addRealTimeAlarm(timer);
      }
    }
  }

}
