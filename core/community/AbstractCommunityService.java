/*
 * <copyright>
 * 
 * Copyright 2001-2004 Mobile Intelligence Corp under sponsorship of the Defense
 * Advanced Research Projects Agency (DARPA).
 * 
 * You can redistribute this software and/or modify it under the terms of the
 * Cougaar Open Source License as published on the Cougaar Open Source Website
 * (www.cougaar.org).
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * </copyright>
 */
package org.cougaar.community;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.cougaar.community.manager.CommunityManager;
import org.cougaar.community.manager.Request;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.community.Agent;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.FindCommunityCallback;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.util.log.Logger;

/**
 * Base class for implementations of CommunityService API. Methods that are used
 * for remote operations are abstract enabling the use of different
 * communication mechanisms.
 */
public abstract class AbstractCommunityService implements CommunityService,
    java.io.Serializable {

  protected Logger log;

  protected static CommunityCache cache; // Shared community cache

  protected CommunityManager communityManager;

  protected String agentName;

  protected CommunityUpdateListener communityUpdateListener;

  protected CommunityMemberships myCommunities;

  protected MembershipWatcher membershipWatcher;

  /**
   * Request to create a community. If the specified community does not exist it
   * will be created and the caller will become the community manager. It the
   * community already exists a Community reference is obtained from its
   * community manager and returned.
   * 
   * @param communityName
   *          Name of community to create
   * @param attrs
   *          Attributes to associate with new community
   * @param crl
   *          Listener to receive response
   */
  public void createCommunity(String communityName, Attributes attrs,
      CommunityResponseListener crl) {
    joinCommunity(communityName, getAgentName(), AGENT, new BasicAttributes(),
        true, attrs, crl);
  }

  /**
   * Request to get a Community instance from local cache. If community is found
   * in cache a reference is returned by method call. If the community is not
   * found in the cache a null value is returned and the Community reference is
   * requested from the community manager. After the Community instance has been
   * obtained from the community manager the supplied CommunityResponseListener
   * callback is invoked to notify the requester. Note that the supplied
   * callback is not invoked if a non-null value is returned.
   * 
   * @param communityName
   *          Community of interest
   * @param crl
   *          Listener to receive response after remote fetch
   * @return Community instance if found in cache or null if not found
   */
  public Community getCommunity(String communityName,
      CommunityResponseListener crl) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": getCommunity:" + " community=" + communityName
          + " inCache=" + cache.contains(communityName));
    }
    if (cache.contains(communityName)) {
      Community value = cache.get(communityName);
      if (value == null) {
        crl.getResponse(new CommunityResponseImpl(CommunityResponse.FAIL, null));
      }
      return value;
    } else {
      queueCommunityRequest(communityName, Request.GET_COMMUNITY_DESCRIPTOR,
          null, null, crl, -1, // No timeout
          0); // no delay
      return null;
    }
  }

  /**
   * Request to modify an Entity's attributes.
   * 
   * @param communityName
   *          Name of community
   * @param entityName
   *          Name of affected Entity or null if modifying community attributes
   * @param mods
   *          Attribute modifications
   * @param crl
   *          Listener to receive response
   */
  public void modifyAttributes(String communityName, String entityName,
      ModificationItem[] mods, CommunityResponseListener crl) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": modifyAttributes:" + " community="
          + communityName + " entity=" + entityName + " mods=" + mods);
    }
    Entity entity = entityName != null ? new EntityImpl(entityName) : null;
    final CommunityResponseListener wcrl = wrapResponse(
        Request.MODIFY_ATTRIBUTES, crl, communityName, entity);
    queueCommunityRequest(communityName, Request.MODIFY_ATTRIBUTES, entity,
        mods, wcrl, -1, // no timeout
        0);
  }

  /**
   * Request to join a named community. If the specified community does not
   * exist it may be created in which case the caller becomes the community
   * manager. It the community doesn't exist and the caller has set the
   * "createIfNotFound flag to false the join request will be queued until the
   * community is found.
   * 
   * @param communityName
   *          Community to join
   * @param entityName
   *          New member name
   * @param entityType
   *          Type of member entity to create (AGENT or COMMUNITY)
   * @param entityAttrs
   *          Attributes for new member
   * @param createIfNotFound
   *          Create community if it doesn't exist, otherwise wait
   * @param newCommunityAttrs
   *          Attributes for created community (used if createIfNotFound set to
   *          true, otherwise ignored)
   * @param crl
   *          Listener to receive response
   */
  public void joinCommunity(final String communityName,
      final String entityName, final int entityType,
      final Attributes entityAttrs, boolean createIfNotFound,
      final Attributes newCommunityAttrs, final long timeout,
      final CommunityResponseListener crl) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": joinCommunity: " + " community=" + communityName
          + " entity=" + entityName + " entityAttrs=" + entityAttrs
          + " createIfNotFound=" + createIfNotFound + " timeout=" + timeout);
    }
    switch (entityType) {
      case AGENT:
        final Agent agent = new AgentImpl(entityName != null ? entityName
            : agentName, entityAttrs);
        final CommunityResponseListener wcrl = wrapResponse(Request.JOIN, crl,
            communityName, agent);
        if (createIfNotFound) {
          if (createIfNotFound && !entityName.equals(agentName)) {
            if (log.isWarnEnabled()) {
              log.warn("Cannot create community by proxy. " + agentName
                  + " cannot create community and set " + entityName
                  + " as the manager.  Will try to join " + communityName
                  + " for " + entityName + " if the community already exists");
            }
            queueCommunityRequest(communityName, Request.JOIN, agent, null, wcrl,
                timeout, 0);
          }
          FindCommunityCallback fmcb = new FindCommunityCallback() {

            public void execute(String name) {
              if (name == null) { // community not found
                Community community = new CommunityImpl(communityName,
                    newCommunityAttrs);
                communityManager.manageCommunity(community, new Callback() {

                  public void execute(Response resp) {
                    Response.Bind respBind = (Response.Bind) resp;
                    if (respBind.didBind()) {
                      queueCommunityRequest(communityName, Request.JOIN, agent,
                          null, wcrl, timeout, 0);
                    } else {
                      queueCommunityRequest(communityName, Request.JOIN, agent,
                          null, wcrl, timeout, 1000);
                    }
                  }

                });
              } else {
                queueCommunityRequest(communityName, Request.JOIN, agent, null,
                    wcrl, timeout, 0);
              }
            }
          };
          findCommunity(communityName, fmcb, 0);
        } else {
          queueCommunityRequest(communityName, Request.JOIN, agent, null, wcrl,
              timeout, 0);
        }
        break;
      case COMMUNITY:
        // Submit join request to add nested community iff nested community
        // already exists
        if (createIfNotFound) {
          if (log.isWarnEnabled()) {
            log.warn("createIfNotFound true but value is ignored "
                + " for joining nested communities. " + entityName
                + " will only join " + communityName + " if it already exists");
          }
        }
        FindCommunityCallback fmcb = new FindCommunityCallback() {

          public void execute(final String nestedCommunityMgr) {
            if (nestedCommunityMgr != null) { // community found
              Community member = new CommunityImpl(entityName, entityAttrs);
              CommunityResponseListener wcrl = wrapResponse(Request.JOIN, crl,
                  communityName, member);
              queueCommunityRequest(communityName, Request.JOIN, member, null,
                  wcrl, timeout, 0);
              // Add "Parent" attribute to nested community
              Community nestedCommunity = getCommunity(entityName,
                  new CommunityResponseListener() {

                    public void getResponse(CommunityResponse resp) {
                      if (resp.getStatus() == CommunityResponse.SUCCESS) {
                        Community nestedCommunity = (Community) resp
                            .getContent();
                        addParentAttribute(nestedCommunity, communityName);
                      }
                    }
                  });
              if (nestedCommunity != null) {
                addParentAttribute(nestedCommunity, communityName);
              }
            } else {
              // Failed request, nested community does not exist
              crl.getResponse(new CommunityResponseImpl(CommunityResponse.FAIL,
                  null));
            }
          }
        };
        findCommunity(entityName, fmcb, 0); // Look for nested community
        break;
      default:
        // Failed request, unknown entity type
        crl
            .getResponse(new CommunityResponseImpl(CommunityResponse.FAIL, null));
    }
  }

  /**
   * Request to join a named community. If the specified community does not
   * exist it may be created in which case the caller becomes the community
   * manager. It the community doesn't exist and the caller has set the
   * "createIfNotFound flag to false the join request will be queued until the
   * community is found.
   * 
   * @param communityName
   *          Community to join
   * @param entityName
   *          New member name
   * @param entityType
   *          Type of member entity to create (AGENT or COMMUNITY)
   * @param entityAttrs
   *          Attributes for new member
   * @param createIfNotFound
   *          Create community if it doesn't exist, otherwise wait
   * @param newCommunityAttrs
   *          Attributes for created community (used if createIfNotFound set to
   *          true, otherwise ignored)
   * @param crl
   *          Listener to receive response
   */
  public void joinCommunity(final String communityName,
      final String entityName, final int entityType,
      final Attributes entityAttrs, boolean createIfNotFound,
      final Attributes newCommunityAttrs, final CommunityResponseListener crl) {
    joinCommunity(communityName, entityName, entityType, entityAttrs,
        createIfNotFound, newCommunityAttrs, -1, crl);
  }

  // Scratchpad for pending callbacks to prevent multiple invocations
  private Set pendingCallbacks = Collections.synchronizedSet(new HashSet());

  /**
   * Wrap response enabling to be automatically resent in the event of a
   * timeout.
   * 
   * @param type
   *          int
   * @param crl
   *          CommunityResponseListener
   * @param communityName
   *          String
   * @param entity
   *          Entity
   * @return CommunityResponseListener
   */
  private CommunityResponseListener wrapResponse(final int type,
      final CommunityResponseListener crl, final String communityName,
      final Entity entity) {
    if (crl == null)
      return null;
    membershipWatcher.addPendingOperation(communityName);
    return new CommunityResponseListener() {

      public void getResponse(final CommunityResponse resp) {
        switch (resp.getStatus()) {
          case CommunityResponse.SUCCESS:
            if (resp.getContent() == null) {
              final Community community = cache.get(communityName);
              if (type == CommunityServiceConstants.JOIN) {
                if (community != null && community.hasEntity(entity.getName())) {
                  myCommunities.add(communityName, entity);
                  membershipWatcher.removePendingOperation(communityName);
                  ((CommunityResponseImpl) resp).setContent(community);
                  crl.getResponse(resp);
                } else {
                  CommunityChangeListener ccl = new CommunityChangeListener() {

                    public String getCommunityName() {
                      return communityName;
                    }

                    public void communityChanged(CommunityChangeEvent cce) {
                      synchronized (pendingCallbacks) {
                        if (cce.getCommunity().hasEntity(entity.getName())
                            && pendingCallbacks.contains(this)) {
                          removeListener(this);
                          myCommunities.add(communityName, entity);
                          membershipWatcher
                              .removePendingOperation(communityName);
                          ((CommunityResponseImpl) resp).setContent(cce
                              .getCommunity());
                          crl.getResponse(resp);
                          pendingCallbacks.remove(this);
                        }
                      }
                    }
                  };
                  pendingCallbacks.add(ccl);
                  addListener(ccl);
                }
              } else if (type == CommunityServiceConstants.LEAVE) {
                if (community != null && !community.hasEntity(entity.getName())) {
                  myCommunities.remove(communityName, entity.getName());
                  membershipWatcher.removePendingOperation(communityName);
                  ((CommunityResponseImpl) resp).setContent(community);
                  crl.getResponse(resp);
                } else {
                  CommunityChangeListener ccl = new CommunityChangeListener() {

                    public String getCommunityName() {
                      return communityName;
                    }

                    public void communityChanged(CommunityChangeEvent cce) {
                      if (!cce.getCommunity().hasEntity(entity.getName())
                          && pendingCallbacks.contains(this)) {
                        removeListener(this);
                        myCommunities.remove(communityName, entity.getName());
                        membershipWatcher.removePendingOperation(communityName);
                        ((CommunityResponseImpl) resp).setContent(cce
                            .getCommunity());
                        crl.getResponse(resp);
                        pendingCallbacks.remove(this);
                      }
                    }
                  };
                  pendingCallbacks.add(ccl);
                  addListener(ccl);
                }
              } else {
                if (community != null) {
                  ((CommunityResponseImpl) resp).setContent(community);
                  crl.getResponse(resp);
                } else {
                  CommunityChangeListener ccl = new CommunityChangeListener() {

                    public String getCommunityName() {
                      return communityName;
                    }

                    public void communityChanged(CommunityChangeEvent cce) {
                      if (pendingCallbacks.contains(this)) {
                        ((CommunityResponseImpl) resp).setContent(cce
                            .getCommunity());
                        crl.getResponse(resp);
                        pendingCallbacks.remove(this);
                        removeListener(this);
                      }
                    }
                  };
                  pendingCallbacks.add(ccl);
                  addListener(ccl);
                }
              }
            } else { // content != null
              if (type == CommunityServiceConstants.JOIN) {
                myCommunities.add(communityName, entity);
              } else if (type == CommunityServiceConstants.LEAVE) {
                myCommunities.remove(communityName, entity.getName());
              }
              membershipWatcher.removePendingOperation(communityName);
              crl.getResponse(resp);
            }
            break;
          case CommunityResponse.FAIL:
            membershipWatcher.removePendingOperation(communityName);
            crl.getResponse(resp);
            break;
          case CommunityResponse.TIMEOUT:
            if (log.isWarnEnabled()) {
              log.warn("Community request timeout:" + " type=" + type
                  + " entity=" + entity + " community=" + communityName);
            }
            // retry ??
            /*
             * queueCommunityRequest(communityName, type, entity, null,
             * wrapResponse(type, crl, communityName, entity), -1, 10 * 1000); //
             * Requeue with delay
             */

            break;

        }
      }
    };
  }

  /**
   * Request to leave named community.
   * 
   * @param communityName
   *          Community to leave
   * @param entityName
   *          Entity to remove from community
   * @param crl
   *          Listener to receive response
   */
  public void leaveCommunity(final String communityName,
      final String entityName, final CommunityResponseListener crl) {
    leaveCommunity(communityName, entityName, -1, crl);
  }

  /**
   * Request to leave named community.
   * 
   * @param communityName
   *          Community to leave
   * @param entityName
   *          Entity to remove from community
   * @param timeout
   *          How long to attempt operation before giving up
   * @param crl
   *          Listener to receive response
   */
  public void leaveCommunity(final String communityName,
      final String entityName, final long timeout,
      final CommunityResponseListener crl) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": leaveCommunity: " + " community="
          + communityName + " entity=" + entityName);
    }
    final Entity member = new EntityImpl(entityName);
    final CommunityResponseListener wcrl = wrapResponse(Request.LEAVE, crl,
        communityName, member);
    Community community = getCommunity(communityName, null);
    if (community != null && community.hasEntity(entityName)) {
      Entity entity = community.getEntity(entityName);
      if (entity instanceof Agent) {
        // Leave request for this agent
        queueCommunityRequest(communityName, Request.LEAVE, member, null, wcrl,
            timeout, 0);
      } else { // Entity is a community
        findCommunity(communityName, new FindCommunityCallback() {

          public void execute(String managerName) {
            if (agentName.equals(managerName)) {
              queueCommunityRequest(communityName, Request.LEAVE, member, null,
                  wcrl, timeout, 0);
              removeParentAttribute(getCommunity(entityName, null),
                  communityName);
            } else {
              // Failed request, requestor not community manager
              crl.getResponse(new CommunityResponseImpl(CommunityResponse.FAIL,
                  null));
            }
          }
        }, timeout);
      }
    } else {
      // Failed request, nested community or entity does not exist
      crl.getResponse(new CommunityResponseImpl(CommunityResponse.FAIL, null));
    }
  }

  /**
   * Adds "Parent=XXX" attribute to nested community.
   * 
   * @param community
   *          Community
   * @param parent
   *          String
   */
  protected void addParentAttribute(Community community, String parent) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": addParentAttribute: community="
          + community.getName());
    }
    Attributes attrs = community.getAttributes();
    Attribute attr = attrs.get("Parent");
    if (attr == null || !attr.contains(parent)) {
      int type = attr == null ? DirContext.ADD_ATTRIBUTE
          : DirContext.REPLACE_ATTRIBUTE;
      ModificationItem[] mods = new ModificationItem[] { new ModificationItem(
          type, new BasicAttribute("Parent", parent))};
      queueCommunityRequest(community.getName(), Request.MODIFY_ATTRIBUTES,
          null, mods, null, -1, 0);
    }
  }

  /**
   * Removes "Parent=XXX" attribute from nested community.
   * 
   * @param community
   *          Community
   * @param parent
   *          String
   */
  protected void removeParentAttribute(Community community, String parent) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": removeParentAttribute: community="
          + community.getName());
    }
    Attributes attrs = community.getAttributes();
    Attribute attr = attrs.get("Parent");
    if (attr != null && attr.contains(parent)) {
      ModificationItem[] mods = new ModificationItem[] { new ModificationItem(
          DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("Parent", parent))};
      queueCommunityRequest(community.getName(), Request.MODIFY_ATTRIBUTES,
          null, mods, null, -1, 0);
    }
  }

  /**
   * Initiates a community search operation. The results of the search are
   * immediately returned as part of the method call if the search can be
   * resolved using locally cached data. However, if the search requires
   * interaction with a remote community manager a null value is returned by the
   * method call and the search results are returned via the
   * CommunityResponseListener callback after the remote operation has been
   * completed. In the case where the search can be satisified using local data
   * (i.e., the method returns a non-null value) the CommunityResponseListener
   * is not invoked.
   * 
   * @param communityName
   *          Name of community to search
   * @param searchFilter
   *          JNDI compliant search filter
   * @param recursiveSearch
   *          True for recursive search into nested communities [false = search
   *          top community only]
   * @param resultQualifier
   *          Type of entities to return in result [ALL_ENTITIES, AGENTS_ONLY,
   *          or COMMUNITIES_ONLY]
   * @param crl
   *          Callback object to receive search results
   * @return Collection of Entity objects matching search criteria if available
   *         in local cache. A null value is returned if cache doesn't contained
   *         named community.
   */
  public Collection searchCommunity(final String communityName,
      final String searchFilter, final boolean recursiveSearch,
      final int resultQualifier, final CommunityResponseListener crl) {
    Collection results = null;
    if (communityName == null) {
      // Search all parent communities found in local cache
      results = cache.search(searchFilter);
      if (results == null)
        results = Collections.EMPTY_SET;
    } else {
      if (cache.contains(communityName)) {
        results = cache.search(communityName, searchFilter, resultQualifier,
            recursiveSearch);
      } else {
        final Set matches = new HashSet();
        FindCommunityCallback fmcb = new FindCommunityCallback() {

          public void execute(String name) {
            if (name != null) { // community exists, get a copy in local cache
              CommunityResponseListener getCommunityListener = new CommunityResponseListener() {

                public void getResponse(CommunityResponse resp) {
                  if (resp.getStatus() == CommunityResponse.SUCCESS) {
                    matches.addAll(cache.search(communityName, searchFilter,
                        resultQualifier, recursiveSearch));
                  }
                  if (crl != null) {
                    crl.getResponse(new CommunityResponseImpl(
                        CommunityResponse.SUCCESS, matches));
                  }
                }
              };
              if (getCommunity(communityName, getCommunityListener) != null) {
                matches.addAll(cache.search(communityName, searchFilter,
                    resultQualifier, recursiveSearch));
                crl.getResponse(new CommunityResponseImpl(
                    CommunityResponse.SUCCESS, matches));
              }
            } else { // community doesn't exist
              crl.getResponse(new CommunityResponseImpl(
                  CommunityResponse.SUCCESS, Collections.EMPTY_SET));
            }
          }
        };
        findCommunity(communityName, fmcb, -1);
      }
    }
    if (log.isDebugEnabled()) {
      boolean inCache = cache.contains(communityName);
      log.debug(agentName + ": searchCommunity:" + " community="
          + communityName + " filter=" + searchFilter + " recursive="
          + recursiveSearch + " qualifier=" + resultQualifier + " inCache="
          + inCache + " results="
          + (results != null ? Integer.toString(results.size()) : null));
    }
    return results;
  }

  /**
   * Returns an array of community names of all communities of which caller is a
   * member.
   * 
   * @param allLevels
   *          Set to false if the list should contain only those communities in
   *          which the caller is explicitly referenced. If true the list will
   *          also include those communities in which the caller is implicitly a
   *          member as a result of community nesting.
   * @return Array of community names
   */
  public String[] getParentCommunities(boolean allLevels) {
    return (String[]) cache.getAncestorNames(getAgentName(), allLevels)
        .toArray(new String[0]);
  }

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member. If the member name is null the immediate
   * parent communities for calling agent are returned. If member is the name of
   * a nested community the names of all immediate parent communities is
   * returned. The results are returned directly if the member name is null or
   * if a copy of the specified community is available in local cache.
   * Otherwise, the results will be returned in the CommunityResponseListener
   * callback in which case the method returns a null value.
   * 
   * @param member
   *          Member name (either null or name of a nested community)
   * @param crl
   *          Listner to receive results if remote lookup is required
   * @return A collection of community names if operation can be resolved using
   *         data from local cache, otherwise null
   */
  public Collection listParentCommunities(String member, String filter,
      CommunityResponseListener crl) {
    if (log.isDebugEnabled()) {
      log.debug("listParentCommunities:" + " member=" + member + " filter="
          + filter + " hasCRL=" + (crl != null));
    }
    return listParentCommunities(member, crl);
  }

  /**
   * Add listener for CommunityChangeEvents.
   * 
   * @param l
   *          Listener
   */
  public void addListener(CommunityChangeListener l) {
    if (log.isDebugEnabled())
      log.debug(agentName + ": Adding CommunityChangeListener:" + " community="
          + l.getCommunityName());
    cache.addListener(l);
  }

  /**
   * Remove listener for CommunityChangeEvents.
   * 
   * @param l
   *          Listener
   */
  public void removeListener(CommunityChangeListener l) {
    if (l != null) {
      cache.removeListener(l);
    }
  }

  /**
   * Performs attribute based search of community entities. This is a general
   * purpose search operation using a JNDI search filter. This method is
   * non-blocking. An empty Collection will be returned if the local cache is
   * empty. Updated search results can be obtained by using the addListener
   * method to receive change notifications.
   * 
   * @param communityName
   *          Name of community to search
   * @param filter
   *          JNDI search filter
   * @return Collection of MessageAddress objects
   */
  public Collection search(final String communityName, final String filter) {
    if (log.isDebugEnabled()) {
      boolean inCache = cache.contains(communityName);
      if (log.isDebugEnabled()) {
        log.debug(agentName + ": search" + " community=" + communityName
            + " filter=" + filter + " inCache=" + inCache);
      }
      if (inCache) {
        if (log.isDetailEnabled()) {
          log.detail(cache.get(communityName).toXml());
        }
      }
    }
    final Collection matches = new HashSet();
    if (cache.contains(communityName)) {
      matches.addAll(getMatches(communityName, filter));
    } else {
      Community community = getCommunity(communityName,
          new CommunityResponseListener() {

            public void getResponse(CommunityResponse resp) {
              if (resp.getContent() != null) {
                matches.addAll(getMatches(communityName, filter));
              }
            }
          });
      if (community != null) {
        matches.addAll(getMatches(communityName, filter));
      }
    }
    return matches;
  }

  /**
   * Returns a collection of MessageAddress objects for all agents from
   * specified community that match search filter.
   * 
   * @param communityName
   *          String
   * @param filter
   *          String
   * @return Collection Collection of MessageAddress objects
   */
  protected Collection getMatches(String communityName, String filter) {
    Collection matches = new HashSet();
    if (cache.contains(communityName)) {
      Collection searchResults = cache.search(communityName, filter,
          Community.AGENTS_ONLY, true);
      for (Iterator it = searchResults.iterator(); it.hasNext();) {
        Entity e = (Entity) it.next();
        if (e instanceof Agent) {
          matches.add(MessageAddress.getMessageAddress(e.getName()));
        }
      }
    }
    return matches;
  }

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member.
   * 
   * @param member
   *          Member name
   * @return A collection of community names
   */
  public Collection listParentCommunities(String member) {
    if (log.isDebugEnabled()) {
      log.debug("listParentCommunities:" + " member=" + member);
    }
    return cache.getAncestorNames(member, false);
  }

  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member and satisfy a given set of attributes.
   * 
   * @param member
   *          Member name
   * @param filter
   *          Search filter defining community attributes
   * @return A collection of community names
   */
  public Collection listParentCommunities(String member, String filter) {
    if (log.isDebugEnabled()) {
      log.debug("listParentCommunities:" + " member=" + member + " filter="
          + filter);
    }
    List matches = new ArrayList();
    Collection parentNames = listParentCommunities(member);
    Set communitiesMatchingFilter = cache.search(filter);
    if (communitiesMatchingFilter != null) {
      for (Iterator it = communitiesMatchingFilter.iterator(); it.hasNext();) {
        Community community = (Community) it.next();
        if (parentNames.contains(community.getName())) {
          matches.add(community.getName());
        }
      }
    }
    return matches;
  }

  /**
   * Handle response to community request returned by manager.
   * 
   * @param resp
   *          CommunityResponse from manager
   * @param listeners
   *          CommunityResponseListeners to be notified
   */
  protected void handleResponse(final String communityName,
      final CommunityResponse resp, final Set listeners) {
    if (log.isDebugEnabled()) {
      log.debug(agentName + ": handleResponse: " + resp);
    }
    int status = resp.getStatus();
    Object content = resp.getContent();
    switch (status) {
      case CommunityResponse.SUCCESS:
        if (content != null && content instanceof Community) {
          Community community = (Community) content;
          // Update cached copy of community
          if (communityUpdateListener != null) {
            communityUpdateListener.updateCommunity(community);
          }
          // Replace community object in response with local reference from
          // cache
          ((CommunityResponseImpl) resp).setContent(cache.get(community
              .getName()));
          sendResponse(resp, listeners);
        } else {
          // Notify all listeners
          sendResponse(resp, listeners);
        }
        break;
      case CommunityResponse.TIMEOUT:
        // TODO: Retry??
        sendResponse(resp, listeners);
        break;
      case CommunityResponse.FAIL:
        sendResponse(resp, listeners);
        break;
    }
  }

  abstract public Collection listAllCommunities();

  abstract public void listAllCommunities(CommunityResponseListener crl);

  abstract protected void queueCommunityRequest(String communityName,
      int requestType, Entity entity, ModificationItem[] attrMods,
      CommunityResponseListener crl, long timeout, long delay);

  abstract protected String getAgentName();

  abstract protected void sendResponse(CommunityResponse resp, Set listeners);

  /**
   * Invokes callback when specified community is found.
   * 
   * @param communityName
   *          Name of community
   * @param fccb
   *          Callback invoked after community is found or timeout has lapsed
   * @param timeout
   *          Length of time (in milliseconds) to wait for community to be
   *          located. A value of -1 disables the timeout.
   */
  abstract public void findCommunity(String communityName,
      FindCommunityCallback fccb, long timeout);

}
