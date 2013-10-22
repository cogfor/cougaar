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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.lang.reflect.Constructor;

import javax.naming.directory.ModificationItem;

import org.cougaar.community.BlackboardClient;
import org.cougaar.community.CommunityDescriptor;
import org.cougaar.community.CommunityUpdateListener;
import org.cougaar.community.RelayAdapter;
import org.cougaar.community.AbstractCommunityService;
import org.cougaar.community.CommunityServiceConstants;
import org.cougaar.community.CommunityResponseImpl;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.FindCommunityCallback;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.UnaryPredicate;

/**
 * Concrete implementation of CommunityManager interface that uses Blackboard
 * Relays to perform communication with remote nodes/agents.
 */
public class DefaultCommunityManagerImpl
    extends AbstractCommunityManager
    implements CommunityServiceConstants {

  protected BindingSite bindingSite;
  protected MyBlackboardClient myBlackboardClient;

  protected Set managedCommunities = Collections.synchronizedSet(new HashSet());
  protected Set communitiesToCheck = Collections.synchronizedSet(new HashSet());

  // Helper class for distributing Community updates
  protected CommunityDistributer distributer;

  // Services used
  protected AbstractCommunityService communityService;
  protected WhitePagesService whitePagesService;

  // This agent
  protected MessageAddress agentId;
  protected CommunityUpdateListener updateListener;

  protected String priorManager = null;

  protected long verifyInterval = DEFAULT_VERIFY_MGR_INTERVAL;

  protected boolean includeDescriptorInResponse =
      DEFAULT_INCLUDE_DESCRIPTOR_IN_RESPONSE;

  /**
   * Construct CommunityManager component capable of communicating with remote
   * agents via Blackboard Relays.
   * @param bs       BindingSite
   * @param acs      CommunityService reference
   * @param cul      Listener for local updates
   */
  public DefaultCommunityManagerImpl(BindingSite bs,
                                     AbstractCommunityService acs,
                                     CommunityUpdateListener cul) {
    this.bindingSite = bs;
    ServiceBroker sb = getServiceBroker();
    agentId = getAgentId();
    agentName = agentId.toString();
    logger = (LoggingService)sb.getService(this, LoggingService.class, null);
    communityService = acs;
    whitePagesService =
        (WhitePagesService) sb.getService(this, WhitePagesService.class, null);
    myBlackboardClient = new MyBlackboardClient(bs);
    getSystemProperties();
    accessManager = getCommunityAccessManager();
    distributer = new CommunityDistributer(bs,
                                           true,
                                           cul,
                                           communities);
  }

  /**
   * Create a new CommunityAccessManager.
   * @return CommunityAccessManager
   */
  protected CommunityAccessManager getCommunityAccessManager() {
    ServiceBroker sb = getServiceBroker();
    CommunityAccessManager cam = null;
    String accessManagerClassname =
        System.getProperty(COMMUNITY_ACCESS_MANAGER_PROPERTY,
                           DEFAULT_COMMUNITY_ACCESS_MANAGER_CLASSNAME);
    try {
      Class accessManagerClass = Class.forName(accessManagerClassname);
      Class args[] = new Class[]{ServiceBroker.class};
      Constructor constructor =
          accessManagerClass.getConstructor(args);
      cam = (CommunityAccessManager)constructor.newInstance(new Object[]{sb});
    } catch (Exception ex) {
      logger.error("Exception creating CommunityAccessManager: "
                   + ex.getMessage() +
                   ", reverting to org.cougaar.community.manager.CommunityAccessManager");
      cam = new CommunityAccessManager(sb);
    }
    return cam;
  }

  public void manageCommunity(Community community) {
    super.manageCommunity(community);
  }
  
  public void manageCommunity(Community community, Callback callback) {
    super.manageCommunity(community, callback);
  }

  protected void getSystemProperties() {
    try {
      verifyInterval =
          Long.parseLong(System.getProperty(VERIFY_MGR_INTERVAL_PROPERTY,
                                            Long.toString(DEFAULT_VERIFY_MGR_INTERVAL)));
      includeDescriptorInResponse =
          Boolean.valueOf(System.getProperty(INCLUDE_DESCRIPTOR_IN_RESPONSE_PROPERTY,
                                            Boolean.toString(DEFAULT_INCLUDE_DESCRIPTOR_IN_RESPONSE))).booleanValue();
    } catch (Exception ex) {
      if (logger.isWarnEnabled()) {
        logger.warn(agentName + ": Exception setting parameter from system property", ex);
      }
    }
  }

  protected MessageAddress getAgentId() {
    AgentIdentificationService ais =
        (AgentIdentificationService)getServiceBroker().getService(this,
        AgentIdentificationService.class, null);
    MessageAddress addr = ais.getMessageAddress();
    getServiceBroker().releaseService(this, AgentIdentificationService.class, ais);
    return addr;
  }

  protected ServiceBroker getServiceBroker() {
    return bindingSite.getServiceBroker();
  }

  /**
   * Processes Requests received via Relay.
   * @param req Request
   */
  protected void processRequest(Request req) {
    if (logger.isDetailEnabled()) {
      logger.detail(agentId + ": processRequest: " + req);
    }
    String source = req.getSource().toString();
    String communityName = req.getCommunityName();
    int reqType = req.getRequestType();
    Entity entity = req.getEntity();
    ModificationItem[] attrMods = req.getAttributeModifications();
    CommunityResponseImpl resp = (CommunityResponseImpl)handleRequest(source,
                                                                      communityName,
                                                                      reqType,
                                                                      entity,
                                                                      attrMods);

    if (!includeDescriptorInResponse && reqType != GET_COMMUNITY_DESCRIPTOR) {
      // Don't include community in response, instead rely on CommunityDistributer to send
      // This decreases messaging overhead (primarily in serialization) and thus
      //    improves overally scalability
      resp.setContent(null);
    }
    req.setResponse(resp);
    myBlackboardClient.publish(req, BlackboardClient.CHANGE);
  }

  /**
   * Tests whether this agent is the manager for the specified community.
   * @param communityName String
   * @return boolean
   */
  protected boolean isManager(String communityName) {
    return (managedCommunities.contains(communityName) &&
            communities.containsKey(communityName) &&
            distributer.contains(communityName));
  }

  /**
   * Add agents to distribution list for community updates.
   * @param communityName Name of community
   * @param targets Set of agent names (String) to add to distribution
   */
  protected void addTargets(String communityName, Set targets) {
    distributer.addTargets(communityName, targets);
  }

  /**
   * Remove agents from distribution list for community updates.
   * @param communityName Name of community
   * @param targets Set of agent names (String) to remove from distribution
   */
  protected void removeTargets(String communityName, Set targets) {
    distributer.removeTargets(communityName, targets);
  }

  /**
   * Send updated Community info to agents on distribution.
   * @param communityName Name of community
   */
  protected void distributeUpdates(String communityName) {
    distributer.update(communityName);
  }

  /**
   * Get name of community manager.
   * @param communityName String
   * @param fmcb FindManagerCallback
   */
  public void findManager(String                      communityName,
                          final FindCommunityCallback fmcb) {

    communityService.findCommunity(communityName, fmcb, 0);
  }

  /**
   * Asserts community manager role.
   * @param communityName Community to manage
   */
  protected void assertCommunityManagerRole(String communityName) {
    assertCommunityManagerRole(communityName, false);
  }
  
  protected void assertCommunityManagerRole(String communityName,
      Callback callback) {
    assertCommunityManagerRole(communityName, false, callback);
  }

  /**
   * Asserts community manager role by binding address to community name in
   * White Pages
   * @param communityName Community to manage
   * @param override      If true any existing binding will be removed
   *                      and replaced with new
   */
  protected void assertCommunityManagerRole(String communityName,
                                            boolean override) {
    assertCommunityManagerRole(communityName, override, null);
  }
  
  /**
   * Asserts community manager role by binding address to community name in
   * White Pages
   * 
   * @param communityName
   *          Community to manage
   * @param override
   *          If true any existing binding will be removed and replaced with new
   * @param callback
   *          Invoked when the assertion is completed
   */
  protected void assertCommunityManagerRole(String communityName,
      boolean override, Callback callback) {
    if (logger.isDetailEnabled()) {
      logger.detail(agentName + ": assertCommunityManagerRole: agent="
          + agentId.toString() + " community=" + communityName);
    }
    try {
      bindCommunityManager(communityName, override, callback);
      communitiesToCheck.add(communityName);
      myBlackboardClient.startVerifyManagerCheck();
    } catch (Throwable ex) {
      if (logger.isWarnEnabled()) {
        logger.warn(agentName
            + ": Unable to (re)bind agent as community manager:" + " error="
            + ex + " agent=" + agentId + " community=" + communityName);
      }
    }

  }

  /**
   * Return current time as a long.
   * @return  Current time
   */
  private long now() {
    return System.currentTimeMillis();
  }

  /** Create a wp entry for white pages binding
   * @param communityName Name of community to bind
   * @return AddressEntry for new manager binding.
   * @exception Exception Unable to create AddressEntry
   */
  private AddressEntry createManagerEntry(String communityName) throws Exception {
    URI uri = URI.create("agent:///"+agentId);
    AddressEntry entry =
      AddressEntry.getAddressEntry(communityName+".comm", "community", uri);
    return entry;
  }

  /**
   * Bind this agent to community name in White Pages.
   * @param communityName String  Name of community to bind
   * @param override boolean  Override existing binding
   * @throws Exception
   */
  private void bindCommunityManager(final String communityName,
                                    final boolean override, final Callback callback) throws Exception {
    final AddressEntry communityAE = createManagerEntry(communityName);
    Callback cb = new Callback() {
      public void execute(Response resp) {
        Response.Bind bindResp = (Response.Bind)resp;
        if (resp.isAvailable()) {
          if (logger.isDebugEnabled())
            logger.debug(agentName + ": bind: " +
                          " success=" + resp.isSuccess() +
                          " didBind=" + bindResp.didBind());
          if (bindResp.didBind()) {
            distributer.add(communityName,
                            Collections.singleton(agentId.toString()));
            if (logger.isDebugEnabled()) {
              logger.debug(agentName + ": Managing community " +
                           communityName);
            }
            managedCommunities.add(communityName);
          } else {
            if (logger.isDetailEnabled())
              logger.detail(
                  agentName + ": Unable to bind agent as community manager:" +
                  " agent=" + agentId +
                  " community=" + communityName +
                  " entry=" + communityAE +
                  " attemptingRebind=" + override);
            if (override) {
              rebindCommunityManager(communityAE, communityName);
            }
          }
          resp.removeCallback(this);
          if (callback != null) {
            callback.execute(resp);
          }
        }
      }
    };
    whitePagesService.bind(communityAE, cb);
  }

  private void rebindCommunityManager(AddressEntry ae,
                                      final String communityName) {
    Callback cb = new Callback() {
      public void execute(Response resp) {
        Response.Bind bindResp = (Response.Bind) resp;
        if (resp.isAvailable()) {
          if (logger.isDebugEnabled())
            logger.debug(agentName+": rebind: " +
                        " success=" + resp.isSuccess() +
                        " didBind=" + bindResp.didBind());
          if (bindResp.didBind()) {
            logger.debug(agentName+": Managing community (rebind)" + communityName);
            managedCommunities.add(communityName);
          } else {
            if (logger.isDebugEnabled())
              logger.debug(agentName+": Unable to rebind agent as community manager:" +
                           " agent=" + agentId +
                           " community=" + communityName);
          }
          resp.removeCallback(this);
        }
      }
    };
    whitePagesService.rebind(ae, cb);
  }

  private static final UnaryPredicate communityDescriptorPredicate = 
    new CommunityDescriptorPredicate();
  private static final class CommunityDescriptorPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof RelayAdapter &&
              ((RelayAdapter)o).getContent() instanceof CommunityDescriptor);
    }
  }

  /**
   * Check WPS binding to verify that the state of this community manager
   * is in sync with the WPS bindings.
   */
  private void verifyManagerRole() {
    Set l = new HashSet();
    synchronized (communitiesToCheck) {
      l.addAll(communitiesToCheck);
    }
    for (Iterator it = l.iterator(); it.hasNext(); ) {
      final String communityName = (String)it.next();
      // See if WP binding lists this agent as manager for each name
      // in communityNames collection
      FindCommunityCallback fmcb = new FindCommunityCallback() {
        public void execute(String mgrName) {
          if (logger.isDetailEnabled()) {
            logger.detail(agentName + ": verifyWpsBinding:" +
                          " community=" + communityName +
                          " current=" + mgrName +
                          " prior=" + priorManager);
          }
          if (isManager(communityName) && mgrName == null) {
            assertCommunityManagerRole(communityName, true); // reassert mgr role
          } else if (!isManager(communityName) && agentName.equals(mgrName)) {
            if (logger.isDebugEnabled()) {
              logger.debug(agentName + ": New WP binding:" +
                          " community=" + communityName +
                          " prior=" + priorManager +
                          " new=" + mgrName);
            }
            managedCommunities.add(communityName);
            distributer.add(communityName,
                            Collections.singleton(agentId.toString()));
            myBlackboardClient.startVerifyManagerCheck();
          } else if (isManager(communityName) && !agentName.equals(mgrName)) {
            if (logger.isDebugEnabled()) {
              logger.debug(agentName + ": No longer bound in WP:" +
                           " community=" + communityName +
                           " prior=" + agentName +
                           " new=" + mgrName);
            }
            managedCommunities.remove(communityName);
            distributer.remove(communityName);
          }
          priorManager = mgrName;
        }
      };
      findManager(communityName, fmcb);
    }
  }

  /**
   * Predicate used to select community manager Requests sent by remote
   * agents.
   */
  private IncrementalSubscription requestSub;
  private static final UnaryPredicate requestPredicate = new RequestPredicate();
  private static final class RequestPredicate implements UnaryPredicate {
    public boolean execute (Object o) {
      return (o instanceof Request);
    }
  };

  class MyBlackboardClient extends BlackboardClient {

    private BBWakeAlarm verifyMgrAlarm;

    public MyBlackboardClient(BindingSite bs) {
      super(bs);
    }

    protected void startVerifyManagerCheck() {
      if (verifyMgrAlarm == null) {
        verifyMgrAlarm = new BBWakeAlarm(now() + verifyInterval);
        alarmService.addRealTimeAlarm(verifyMgrAlarm);
      }
    }

    public void setupSubscriptions() {
      // Subscribe to CommunityManagerRequests
      requestSub =
          (IncrementalSubscription)blackboard.subscribe(requestPredicate);

      // Re-publish any CommunityDescriptor Relays found on BB
      if (blackboard.didRehydrate()) {
        Collection cds = blackboard.query(communityDescriptorPredicate);
        for (Iterator it = cds.iterator(); it.hasNext(); ) {
          RelayAdapter ra = (RelayAdapter)it.next();
          CommunityDescriptor cd = (CommunityDescriptor)ra.getContent();
          if (logger.isInfoEnabled()) {
            logger.info(agentName +
                        ": Found CommunityDescriptor Relay: community=" +
                        cd.getCommunity());
          }
          communities.put(cd.getName(), cd.getCommunity());
          distributer.add(ra);
          assertCommunityManagerRole(cd.getName());
        }
      }

    }

    public void execute() {

      super.execute();

      // On verifyMgrAlarm expiration check WPS binding to verify that
      // manager roles for this agent
      if (verifyMgrAlarm != null && verifyMgrAlarm.hasExpired()) {
        verifyManagerRole();
        verifyMgrAlarm = new BBWakeAlarm(now() + verifyInterval);
        alarmService.addRealTimeAlarm(verifyMgrAlarm);
      }

      // Get CommunityManagerRequests sent by remote agents
      Collection communityManagerRequests = requestSub.getAddedCollection();
      for (Iterator it = communityManagerRequests.iterator(); it.hasNext(); ) {
        Request req = (Request)it.next();
        // Process requests sent from remote agents only
        if (!agentName.equals(req.getSource().toString())) {
          processRequest(req);
        }
      }
    }

  }

}
