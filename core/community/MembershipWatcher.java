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

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.FindCommunityCallback;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.cougaar.util.log.LoggerFactory;
import org.cougaar.util.log.Logger;

/**
 * This class listens for community change events and updates local
 * CommunityMembership object to reflect current state.  The
 * CommunityMembership is used to rejoin communities on a restart and to
 * periodically verify that this agents view of the world is in sync with
 * that of applicable community managers.
 */
public class MembershipWatcher {

  protected String thisAgent;
  protected CommunityMemberships myCommunities;
  protected CommunityService communityService;
  protected Logger logger;
  protected List managedCommunities = Collections.synchronizedList(new ArrayList());;
  protected List pendingOperations = Collections.synchronizedList(new ArrayList());

  public MembershipWatcher(String agentName,
                           CommunityService commSvc,
                           CommunityMemberships memberships) {
    this.thisAgent = agentName;
    this.myCommunities = memberships;
    this.communityService = commSvc;
    this.logger =
        LoggerFactory.getInstance().createLogger(MembershipWatcher.class);
  }

  public MembershipWatcher(String agentName,
                           CommunityService commSvc) {
    this.thisAgent = agentName;
    this.myCommunities = new CommunityMemberships();
    this.communityService = commSvc;
    this.logger =
        LoggerFactory.getInstance().createLogger(MembershipWatcher.class);
  }

  public void setMemberships(CommunityMemberships memberships) {
    this.myCommunities = memberships;
  }

  public synchronized void validate() {
    if (logger.isDebugEnabled()) {
      logger.debug(thisAgent + ": validate community memberships: " + thisAgent +
                   " myCommunities=" + myCommunities);
    }
    for (Iterator it = myCommunities.listCommunities().iterator(); it.hasNext(); ) {
      final String communityName = (String)it.next();
      Collection entities = myCommunities.getEntities(communityName);
      for (Iterator it1 = entities.iterator(); it1.hasNext(); ) {
        Entity entity = (Entity)it1.next();        
        if ((entity.getName().equals(thisAgent)) && !pendingOperations.contains(communityName)) {
          checkCommunity(communityName, entity, true);
        }
      }
    }
    Collection parents =
        communityService.listParentCommunities(null, (CommunityResponseListener)null);
    parents.removeAll(myCommunities.listCommunities());
    for (Iterator it1 = parents.iterator(); it1.hasNext(); ) {
      String parentName = (String)it1.next();
      Community parentCommunity = communityService.getCommunity(parentName, null);
      if (parentCommunity != null &&
          parentCommunity.hasEntity(thisAgent) &&
          !pendingOperations.contains(parentName)) {
        // a problem occurs when a remote agent (AgentA) joins a community on behalf
        // of another agent (AgentB).  In this case AgentB's myCommunities
        // is not aware that it has joined the community and attempts to 
        // leave the community at this point.  At some point in the future
        // AgentA will see that AgentB has left the community and because
        // AgentA's myCommunity list says that AgentB should be in the community
        // it will try to re-join the community for AgentB.  Updating the 
        // myCommunities list at this point resolves the problem but I am
        // not sure if it is the correct solution
        if (!myCommunities.contains(parentName, thisAgent)) {
          myCommunities.add(parentName, new AgentImpl(thisAgent));
        }
        checkCommunity(parentName, new AgentImpl(thisAgent), true);
      }
    }

  }

  public void addPendingOperation(String communityName) {
    if (!pendingOperations.contains(communityName)) {
      pendingOperations.add(communityName);
    }
  }

  public void removePendingOperation(String communityName) {
    if (pendingOperations.contains(communityName)) {
      pendingOperations.remove(communityName);
    }
  }

  protected void checkCommunity(final String  communityName,
                                final Entity  entity,
                                final boolean isMember) {
    if (logger.isDetailEnabled()) {
      logger.detail(thisAgent+": checkCommunityMembership:" +
                  " community=" + communityName +
                  " entity=" + entity +
                  " isMember=" + isMember);
    }
    FindCommunityCallback fccb = new FindCommunityCallback() {
      public void execute(String managerName) {
        if (managerName != null) { // Community exists
          Community community = communityService.getCommunity(communityName,
            new CommunityResponseListener() {
              public void getResponse(CommunityResponse resp) {
                Object obj = resp.getContent();
                if (obj != null && !(obj instanceof Community)) {
                  logger.warn(thisAgent+": Invalid response object, type=" +
                              obj.getClass().getName());
                } else {
                  Community community = (Community)obj;
                  if (isMember &&
                      (community == null || !community.hasEntity(entity.getName()))) {
                    rejoin(communityName, entity);
                  } else if (!isMember &&
                             community != null &&
                             community.hasEntity(entity.getName())) {
                    leave(communityName, entity.getName());
                  } else if (isMember && community.hasEntity(entity.getName())) {
                    verifyAttributes(communityName,
                                     entity.getName(),
                                     community.getEntity(entity.getName()).getAttributes(),
                                     entity.getAttributes());
                  }
                }
              }
            });
            if (community != null) {
              if (isMember && !community.hasEntity(entity.getName())) {
                rejoin(communityName, entity);
              } else if (!isMember && community.hasEntity(entity.getName())) {
                leave(communityName, entity.getName());
              } else if (isMember && community.hasEntity(entity.getName())) {
                verifyAttributes(communityName,
                                 entity.getName(),
                                 community.getEntity(entity.getName()).getAttributes(),
                                 entity.getAttributes());
              }
            }
        } else { // Community doesn't exist
          rejoin(communityName, entity);
        }
      }
    };
    communityService.findCommunity(communityName, fccb, 0);
  }

  protected void verifyAttributes(String communityName,
                                  String entityName,
                                  Attributes attrsFromComm,
                                  Attributes attrsFromLocalCopy) {
    ModificationItem attrDelta[] =
        getAttrDelta(attrsFromComm, attrsFromLocalCopy);
    if (attrDelta.length > 0) {
      if (logger.isInfoEnabled()) {
        logger.info(thisAgent+": Correcting attributes:" +
                    " community=" + communityName +
                    " entity=" + entityName +
                    " numAttrsCorrected=" + attrDelta.length +
                    " commAttrs=" + CommunityUtils.attrsToString(attrsFromComm) +
                    " localAttrs=" + CommunityUtils.attrsToString(attrsFromLocalCopy));
      }
      communityService.modifyAttributes(communityName,
                                        entityName,
                                        attrDelta,
                                        null);
    }
  }

  protected void rejoin(final String communityName, Entity entity) {
    if (logger.isDebugEnabled()) {
      logger.debug(thisAgent+": Re-joining community:" +
                  " community=" + communityName +
                  " entity=" + entity.getName());
    }
    addPendingOperation(communityName);
    int type = entity instanceof Agent
                  ? CommunityService.AGENT
                  : CommunityService.COMMUNITY;
    communityService.joinCommunity(communityName,
                                   entity.getName(),
                                   type,
                                   entity.getAttributes(),
                                   false,
                                   null,
      new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          removePendingOperation(communityName);
          if (logger.isDetailEnabled()) {
            logger.detail(thisAgent + ": Join status=" + resp);
          }
        }
    });
  }

  protected void leave(final String communityName, String entityName) {
    if (logger.isDebugEnabled()) {
      logger.debug(thisAgent+": Leaving community:" +
                  " community=" + communityName +
                  " entity=" + entityName);
    }
    addPendingOperation(communityName);
    communityService.leaveCommunity(communityName,
                                   entityName,
      new CommunityResponseListener() {
        public void getResponse(CommunityResponse resp) {
          if (logger.isDetailEnabled()) {
            logger.detail("Leave status=" + resp);
          }
          removePendingOperation(communityName);
        }
    });
  }

  protected ModificationItem[] getAttrDelta(Attributes attrsFromComm,
                                            Attributes attrsFromLocalCopy) {
    List mods = new ArrayList();
    if (attrsFromLocalCopy != null && attrsFromLocalCopy.size() > 0) {
      NamingEnumeration ne = attrsFromLocalCopy.getAll();
      try {
        while (ne.hasMore()) {
          Attribute attr2 = (Attribute)ne.next();
          Attribute attr1 = attrsFromComm.get(attr2.getID());
          if (attr1 == null) {
            mods.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, attr2));
          } else {
            if (!attr2.equals(attr1)) {
              mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attr2));
            }
          }
        }
      } catch (NamingException nex) {
        if (logger.isWarnEnabled()) {
          logger.warn(nex.getMessage());
        }
      }
    }
    return (ModificationItem[])mods.toArray(new ModificationItem[]{});
  }

}
