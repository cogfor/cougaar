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

package org.cougaar.community;

import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Community;

import org.cougaar.community.CommunityResponseImpl;

import org.cougaar.community.requests.AddChangeListener;
import org.cougaar.community.requests.CommunityRequest;
import org.cougaar.community.requests.GetCommunity;
import org.cougaar.community.requests.JoinCommunity;
import org.cougaar.community.requests.LeaveCommunity;
import org.cougaar.community.requests.ListParentCommunities;
import org.cougaar.community.requests.ModifyAttributes;
import org.cougaar.community.requests.RemoveChangeListener;
import org.cougaar.community.requests.SearchCommunity;

import org.cougaar.community.init.CommunityInitializerService;
import org.cougaar.community.init.CommunityConfig;
import org.cougaar.community.init.EntityConfig;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

/**
 * Plugin used by an agent to automatically create/join its initial
 * communities during startup.  The communities to create/join are defined
 * in the communities.xml file found on the Cougaar config path.  This
 * plugin is listens for CommunityRequests published to blackboard and
 * invokes applicable CommunityService methods.
 */
public class CommunityPlugin extends ComponentPlugin {

  // Services used
  protected LoggingService logger;
  protected CommunityService communityService;

  protected String thisAgent;

  /**
   * Get required services and join startup communities.
   */
  protected void setupSubscriptions() {
    logger =
      (LoggingService)getBindingSite().getServiceBroker().getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentId + ": ");
    communityService =
      (CommunityService) getBindingSite().getServiceBroker().getService(this, CommunityService.class, null);
    thisAgent = agentId.toString();

    if (!blackboard.didRehydrate()) {
      // Initial start
      joinStartupCommunities(getCommunityConfigs());
    }

    // Subscribe to CommunityRequests
    communityRequestSub =
      (IncrementalSubscription)blackboard.subscribe(communityRequestPredicate);

  }

  public void execute() {

    // Get community requests published by local components.
    Collection communityRequests = communityRequestSub.getAddedCollection();
    for (Iterator it = communityRequests.iterator(); it.hasNext(); ) {
      CommunityRequest cr = (CommunityRequest) it.next();
      processCommunityRequest(cr);
    }
  }

  /**
   * Get CommunityConfigs using CommunityInitializerService.
   * @return Collection
   */
  private Collection getCommunityConfigs() {
    Collection communityConfigs = null;
    ServiceBroker sb = getBindingSite().getServiceBroker();
    CommunityInitializerService cis = (CommunityInitializerService)
      sb.getService(this, CommunityInitializerService.class, null);
    try {
      communityConfigs = cis.getCommunityDescriptions(null);
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Unable to obtain community information for agent " +
                    thisAgent);
      }
    } finally {
      sb.releaseService(this, CommunityInitializerService.class, cis);
    }
    return communityConfigs;
  }

  /**
   * Builds a collection of CommunityConfig objects for all parent communities.
   * The community config information is retrieved from the
   * CommunityInitializerService.  The source of the community config data is
   * typically an XML file (named "communities.xml" in config path).
   * @param communityConfigs CommunityConfigs associated with society
   * @return Collection of CommunityConfig objects defining parent communities
   */
  private Collection findMyStartupCommunities(Collection communityConfigs) {
    Collection startupCommunities = new Vector();
    for (Iterator it = communityConfigs.iterator(); it.hasNext();) {
      CommunityConfig cc = (CommunityConfig)it.next();
      EntityConfig ec = cc.getEntity(thisAgent);
      if (ec != null) {
        Attributes attrs = ec.getAttributes();
        Attribute roles = attrs.get("Role");
        if (roles != null && roles.contains("Member")) {
          startupCommunities.add(cc);
        }
      }
    }
    return startupCommunities;
  }

  /**
   * Join (and optionally create) all parent communities.  If the parent
   * community does not exist this agent may attempt to create it
   * based on community attributes set in community configuration.
   * The agent will attempt to create the community if the
   * community attribute "CommunityManager=" contains its name.
   * If this attribute is not set the agent will also attempt to become the
   * manager if the entity attribute "CanBeManager=" is undefined or set to true.
   * @param communityConfigs  List of all CommunityConfig objects associated
   *                          with startup communities.
   */
  private void joinStartupCommunities(final Collection communityConfigs) {
    Collection myStartupCommunities = findMyStartupCommunities(communityConfigs);
    for (Iterator it = myStartupCommunities.iterator(); it.hasNext(); ) {
      final CommunityConfig cc = (CommunityConfig) it.next();
      final String communityName = cc.getName();
      final EntityConfig ec = cc.getEntity(thisAgent);
      Set designatedManagers = getDesignatedManagers(cc);
      if (logger.isDetailEnabled()) {
        logger.detail("joinStartupCommunity :" +
                     " agent=" + thisAgent +
                     " community=" + communityName +
                     " designatedManagers=" + designatedManagers +
                     " canBeManager=" + canBeManager(ec));
      }

      // Submit join request to add self to community
      final boolean createIfNotFound = (designatedManagers.contains(ec.getName()) ||
                                       (designatedManagers.isEmpty() &&
                                        canBeManager(ec)));
      CommunityResponseListener crl = new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          if (logger.isDebugEnabled()) {
            logger.debug("joinCommunity:" +
                        " community=" + communityName +
                        " agent=" + agentId +
                        " result=" + resp.getStatusAsString());
          }
          switch (resp.getStatus()) {
            case CommunityResponse.SUCCESS:
              Community community = (Community)resp.getContent();
              if (hasAttribute(community.getAttributes(), "CommunityManager", thisAgent)) {
                Map parentCommunities = listParents(communityConfigs, communityName);
                if (logger.isDebugEnabled()) {
                  logger.debug("Managing community " + communityName +
                               " parents=" + parentCommunities);
                }
                if (parentCommunities != null) {
                  joinParentCommunities(parentCommunities, communityName);
                }
              }
              break;
          }
        }
      };
      communityService.joinCommunity(communityName,
                                     ec.getName(),
                                     CommunityService.AGENT,
                                     ec.getAttributes(),
                                     createIfNotFound,
                                     (createIfNotFound ? cc.getAttributes() : null),
                                     crl);
    }
  }

  /**
   * Adds a nested community to its parent community.
   * @param parentCommunities Collection
   * @param nestedCommunity String
   */
  protected void joinParentCommunities(Map   parentCommunities,
                                       final String nestedCommunity) {
    for (Iterator it = parentCommunities.entrySet().iterator(); it.hasNext();) {
      Map.Entry me = (Map.Entry)it.next();
      final String communityName = (String)me.getKey();
      final Attributes memberAttrs = (Attributes)me.getValue();
      communityService.joinCommunity(communityName,
                                     nestedCommunity,
                                     CommunityService.COMMUNITY,
                                     memberAttrs,
                                     false,
                                     null,
                                     new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          if (logger.isDebugEnabled()) {
            logger.debug("joinParentCommunity:" +
                         " community=" + communityName +
                         " nestedCommunity=" + nestedCommunity +
                         " result=" + resp.getStatusAsString());
          }
        }
      });
    }
  }

  /**
   * Create all communities that contain the specified community as
   * a member.
   * @param communityConfigs Collection
   * @param communityName String
   * @return Map containing parent community name and member attributes
   */
  private Map listParents(Collection communityConfigs, String communityName) {
    Map parents = new HashMap();
    for (Iterator it = communityConfigs.iterator(); it.hasNext(); ) {
      CommunityConfig cc = (CommunityConfig)it.next();
      EntityConfig ec = cc.getEntity(communityName);
      if (ec != null) {
        Attributes attrs = ec.getAttributes();
        Attribute type = attrs.get("EntityType");
        if (type != null && type.contains("Community")) {
          parents.put(cc.getName(), attrs);
        }
      }
    }
    return parents;
  }

  /**
   * Retrieves designated community manager(s) from community configuration.
   * @param cc Config data associated with parent community
   * @return Set of entity names
   */
  private Set getDesignatedManagers(CommunityConfig cc) {
    Set managers = new HashSet();
    try {
      Attributes attrs = cc.getAttributes();  // get community attributes
      if (attrs != null) {
        Attribute attr = attrs.get("CommunityManager");
        if (attr != null &&
            attr.size() > 0 &&
            ((String)attr.get()).trim().length() > 0) { // is a manager specified?
          for (NamingEnumeration ne = attr.getAll(); ne.hasMoreElements();) {
            managers.add(ne.next());
          }
        }
      }
    } catch (NamingException ne) {}
    return managers;
  }

  /**
   * Determines if specified agent can become a community manager.  A value
   * of true is returned if the agent is a member of community and the
   * attribute "CanBeManager=" is either undefined or is not equal to false.
   * @param ec Config data associated with agent
   * @return true if can be manager
   */
  private boolean canBeManager(EntityConfig ec) {
    Attributes attrs = ec.getAttributes();  // get agent attributes
    if (attrs == null)
      return false;  // no attributes, can't be a member or manager
    if (!hasAttribute(attrs, "Role", "Member") ||
        !hasAttribute(attrs, "EntityType", "Agent"))
      return false;
    Attribute attr = attrs.get("CanBeManager");
    if (attr == null) {
      return true;  // default to true if attr not specified
    } else {
      return (!attr.contains("No") && !attr.contains("False"));
    }
  }

  /**
   * Handle CommunityRequests published to blackboard by invoking appropriate
   * CommunityService method.
   * @param cr CommunityRequest
   */
  private void processCommunityRequest (CommunityRequest cr) {
    if (logger.isDebugEnabled()){
      logger.debug("Received CommunityRequest: " + cr);
    }
    if (cr instanceof GetCommunity) {
      GetCommunity gcd = (GetCommunity)cr;
      Community community =
          communityService.getCommunity(gcd.getCommunityName(),
                                        new ResponseHandler(cr));
      if (community != null) {
        cr.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS,
                                                 community));
        blackboard.publishChange(cr);
      }
    } else if (cr instanceof ModifyAttributes) {
      ModifyAttributes ma = (ModifyAttributes)cr;
      communityService.modifyAttributes(ma.getCommunityName(),
                                        ma.getEntityName(),
                                        ma.getModifications(),
                                        new ResponseHandler(cr));
    } else if (cr instanceof JoinCommunity) {
      JoinCommunity jc = (JoinCommunity)cr;
      communityService.joinCommunity(jc.getCommunityName(),
                                     jc.getEntityName(),
                                     jc.getEntityType(),
                                     jc.getEntityAttributes(),
                                     jc.createIfNotFound(),
                                     jc.getCommunityAttributes(),
                                     new ResponseHandler(cr));
    } else if (cr instanceof LeaveCommunity) {
      LeaveCommunity lc = (LeaveCommunity)cr;
      communityService.leaveCommunity(lc.getCommunityName(),
                                      lc.getEntityName(),
                                     new ResponseHandler(cr));
    } else if (cr instanceof SearchCommunity) {
      SearchCommunity sc = (SearchCommunity)cr;
      Collection results =
          communityService.searchCommunity(sc.getCommunityName(),
                                           sc.getFilter(),
                                           sc.isRecursiveSearch(),
                                           sc.getQualifier(),
                                           new ResponseHandler(cr));
      if (results != null) {
        cr.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS,
                                                 results));
        blackboard.publishChange(cr);
      }
    } else if (cr instanceof ListParentCommunities) {
      ListParentCommunities lpc = (ListParentCommunities)cr;
      Collection parentNames =
          communityService.listParentCommunities(lpc.getCommunityName(),
                                                 new ResponseHandler(cr));
      if (parentNames != null) {
        cr.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS,
                                                 parentNames));
        blackboard.publishChange(cr);
      }
    } else if (cr instanceof AddChangeListener) {
      AddChangeListener acl = (AddChangeListener)cr;
      communityService.addListener(acl.getChangeListener());
      cr.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS, null));
      blackboard.publishChange(cr);
    } else if (cr instanceof RemoveChangeListener) {
      RemoveChangeListener rcl = (RemoveChangeListener)cr;
      communityService.removeListener(rcl.getChangeListener());
      cr.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS, null));
      blackboard.publishChange(cr);
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn("Received unknown CommunityRequest - " + cr);
      }
    }
  }

  private boolean hasAttribute(Attributes attrs, String id, String value) {
    if (attrs != null) {
      Attribute attr = attrs.get(id);
      return (attr != null && attr.contains(value));
    }
    return false;
  }

  private IncrementalSubscription communityRequestSub;
  private static final UnaryPredicate communityRequestPredicate =
    new CommunityRequestPredicate();
  private static class CommunityRequestPredicate implements UnaryPredicate {
    public boolean execute (Object o) {
      return (o instanceof CommunityRequest);
    }
  }

// Updates CommunityRequest with response from CommunityService and
// publishes change to requester.
class ResponseHandler implements CommunityResponseListener {
  CommunityRequest req;
  ResponseHandler(final CommunityRequest cr) {
    req = cr;
    if (cr instanceof SearchCommunity) {
      final SearchCommunity sc = (SearchCommunity)cr;
      communityService.addListener(new CommunityChangeListener() {
        public String getCommunityName() { return cr.getCommunityName(); }
        public void communityChanged(CommunityChangeEvent cce) {
          Collection results =
              communityService.searchCommunity(sc.getCommunityName(),
                                               sc.getFilter(),
                                               sc.isRecursiveSearch(),
                                               sc.getQualifier(),
                                               null);
          if (results != null) {
            req.setResponse(new CommunityResponseImpl(CommunityResponse.SUCCESS,
                                                      results));
            blackboard.openTransaction();
            blackboard.publishChange(req);
            blackboard.closeTransaction();
          }
        }
      });
    }
  }

  public void getResponse(CommunityResponse resp) {
    req.setResponse(resp);
    blackboard.openTransaction();
    blackboard.publishChange(req);
    blackboard.closeTransaction();
  }
}

}
