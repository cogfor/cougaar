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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.cougaar.util.log.Logger;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;

import org.cougaar.community.CommunityServiceConstants;
import org.cougaar.community.CommunityImpl;
import org.cougaar.community.CommunityResponseImpl;
import org.cougaar.community.CommunityUtils;

/**
 * Base class for CommunityManager that can manager one or more communities.  
 * Handles requests to join and leave a community.  Disseminates community
 * descriptors to community members and other interested agents.
 */
public abstract class AbstractCommunityManager
    implements CommunityManager, CommunityServiceConstants {

  protected Logger logger;
  protected Map communities = Collections.synchronizedMap(new HashMap());
  protected String agentName;
  protected CommunityAccessManager accessManager;

  /**
   * Adds a community to be managed by this community manager.
   * @param community Community to manage
   */
  public void manageCommunity(Community community) {
    manageCommunity(community, null);
  }
  
  /**
   * Adds a community to be managed by this community manager.
   * 
   * @param community
   *          Community to manage
   * @param callback
   *          Callback to invoke on completion
   */
  public void manageCommunity(final Community community, final Callback callback) {
    if (logger.isDebugEnabled()) {
      logger.debug(agentName + ": manageCommunity: community="
          + community.getName());
    }
    final String communityName = community.getName();

    if (!isManager(communityName)) {
      assertCommunityManagerRole(communityName, new Callback() {

        public void execute(Response resp) {
          Response.Bind respBind = (Response.Bind) resp;      
          if (respBind.didBind()) {
            if (logger.isDebugEnabled()) {
              logger.debug(agentName + ": addCommunity:" + " name="
                  + community.getName());
            }
            if (community.getAttributes() == null) {
              community.setAttributes(new BasicAttributes());
            }
            CommunityUtils.setAttribute(community.getAttributes(),
                "CommunityManager", agentName);
            communities.put(communityName, ((CommunityImpl) community).clone());
            Set targets = new HashSet();
            for (Iterator it = community.getEntities().iterator(); it.hasNext();) {
              targets.add(((Entity) it.next()).getName());
            }
            addTargets(communityName, targets);
          }
          if (callback != null) {
            callback.execute(resp);
          }
        }

      });

    }
  }

  public CommunityResponse processRequest(String             source,
                                          String             communityName,
                                          int                reqType,
                                          Entity             entity,
                                          ModificationItem[] attrMods) {
    CommunityResponseImpl resp = (CommunityResponseImpl)
        handleRequest(source, communityName, reqType, entity, attrMods);
    if (resp.getContent() != null) {
      //resp.setContent(((CommunityImpl) resp.getContent()).clone());
      resp.setContent(resp.getContent());
    }
    return resp;
  }

  protected synchronized CommunityResponse handleRequest(String             source,
                                          String             communityName,
                                          int                reqType,
                                          Entity             entity,
                                          ModificationItem[] attrMods) {
    if (logger.isDebugEnabled()) {
      logger.debug(agentName+": processRequest:" +
                   " source=" + source +
                   " community=" + communityName +
                   " reqType=" + reqType +
                   " entity=" + entity +
                   " attrMods=" + attrMods);
    }
    if (accessManager != null &&
        !accessManager.authorize(communityName,
                                 source,
                                 reqType,
                                 entity != null ? entity.getName()
                                                : communityName,
                                 attrMods)) {
      if (logger.isWarnEnabled()) {
        logger.warn(agentName + ": Authorization Failure:" +
                                " community=" + communityName +
                                " source=" + source +
                                " request=" + reqType +
                                " target=" + entity);
      }
      return new CommunityResponseImpl(CommunityResponse.FAIL, null);
    } else {
      if (isManager(communityName)) {
        CommunityImpl community = (CommunityImpl)communities.get(communityName);
        boolean result = true;
        switch (reqType) {
          case JOIN:
            if (entity != null) {
              String entitiesBeforeAdd = "";
              if (logger.isDetailEnabled()) {
                entitiesBeforeAdd = entityNames(community.getEntities());
              }
              community.addEntity(entity);
              if (logger.isDebugEnabled()) {
                logger.debug(agentName + ": Add entity:" +
                             " community=" + community.getName() +
                             " entity=" + entity +
                             " members=" + community.getEntities().size());
              }
              if (logger.isDetailEnabled()) {
                logger.detail(agentName + ": Add entity:" +
                              " community=" + community.getName() +
                              " entity=" + entity.getName() +
                              " before=" + entitiesBeforeAdd +
                              " after=" +
                              entityNames(community.getEntities()));
              }
              addTargets(communityName, Collections.singleton(source));
              distributeUpdates(communityName);
            } else {
              result = false;
            }
            break;
          case LEAVE:
            if (entity != null && community.hasEntity(entity.getName())) {
              String entitiesBeforeRemove = "";
              if (logger.isDetailEnabled()) {
                entitiesBeforeRemove = entityNames(community.getEntities());
              }
              community.removeEntity(entity.getName());
              if (logger.isDebugEnabled()) {
                logger.debug(agentName + ": Remove entity:" +
                             " community=" + community.getName() +
                             " entity=" + entity +
                             " members=" + community.getEntities().size());
              }
              if (logger.isDetailEnabled()) {
                logger.detail(agentName + ": Remove entity:" +
                              " community=" + community.getName() +
                              " entity=" + entity.getName() +
                              " before=" + entitiesBeforeRemove +
                              " after=" +
                              entityNames(community.getEntities()));
              }
              distributeUpdates(communityName);
            } else {
              result = false;
            }
            break;
          case GET_COMMUNITY_DESCRIPTOR:
            addTargets(communityName, Collections.singleton(source));
            break;
          case MODIFY_ATTRIBUTES:
            if (logger.isDebugEnabled()) {
              logger.debug(agentName + ": Modify attributes:" +
                           " community=" + community.getName() +
                           " source=" + source +
                           " affectedEntity=" + entity);
            }
            if (entity == null ||
                community.getName().equals(entity.getName())) {
              // modify community attributes
              Attributes attrs = community.getAttributes();
              if (logger.isDetailEnabled()) {
                logger.debug(agentName + ": Modifying community attributes:" +
                             " community=" + community.getName() +
                             " before=" + attrsToString(attrs));
              }
              applyAttrMods(attrs, attrMods);
              if (logger.isDetailEnabled()) {
                logger.debug(agentName + ": Modifying community attributes:" +
                             " community=" + community.getName() +
                             " after=" + attrsToString(attrs));
              }
              distributeUpdates(communityName);
            } else {
              // modify attributes of a community entity
              entity = community.getEntity(entity.getName());
              if (entity != null) {
                Attributes attrs = entity.getAttributes();
                if (logger.isDetailEnabled()) {
                  logger.detail(agentName + ": Modifying entity attributes:" +
                                " community=" + community.getName() +
                                " entity=" + entity.getName() +
                                " before=" + attrsToString(attrs));
                }
                applyAttrMods(attrs, attrMods);
                if (logger.isDetailEnabled()) {
                  logger.detail(agentName + ": Modifying entity attributes:" +
                                " community=" + community.getName() +
                                " entity=" + entity.getName() +
                                " after=" + attrsToString(attrs));
                }
                distributeUpdates(communityName);
              }
            }
            break;
        }
        community.setLastUpdate(System.currentTimeMillis());
        return new CommunityResponseImpl(result
                                         ? CommunityResponse.SUCCESS
                                         : CommunityResponse.FAIL,
                                         result ? community : null);
      } else {
        if (logger.isDetailEnabled()) {
          logger.detail(agentName + ": Not community manager:" +
                        " community=" + communityName +
                        " source=" + source +
                        " request=" + reqType);
        }
        return new CommunityResponseImpl(CommunityResponse.TIMEOUT, null);
      }
    }
  }

  /**
   * Apply attribute modifications.
   * @param attrs Attributes to be modified
   * @param mods  Changes
   */
  protected void applyAttrMods(Attributes attrs, ModificationItem[] mods) {
    for (int i = 0; i < mods.length; i++) {
      switch (mods[i].getModificationOp()) {
        case DirContext.ADD_ATTRIBUTE:
          Attribute newAttr = mods[i].getAttribute();
          if (newAttr == null) {
            continue;
          }
          Attribute oldAttr = attrs.get(newAttr.getID());
          if (oldAttr == null) {
            attrs.put(newAttr);
          }
          else {
            try {
              NamingEnumeration en = newAttr.getAll();
              while (en.hasMore()) {
                oldAttr.add(en.next());
              }
            } catch (NamingException e) {
              if (logger.isWarnEnabled()) {
                logger.warn("Unable to add attribute", e);
              }
            }
          }
          break;
        case DirContext.REPLACE_ATTRIBUTE:
          attrs.remove(mods[i].getAttribute().getID());
          attrs.put(mods[i].getAttribute());
          break;
        case DirContext.REMOVE_ATTRIBUTE:
          attrs.remove(mods[i].getAttribute().getID());
          break;
      }
    }
  }

  // Converts a collection of Entities to a compact string representation of names
  protected String entityNames(Collection entities) {
    StringBuffer sb = new StringBuffer("[");
    for (Iterator it = entities.iterator(); it.hasNext(); ) {
      Entity entity = (Entity) it.next();
      sb.append(entity.getName() + (it.hasNext() ? "," : ""));
    }
    return (sb.append("]").toString());
  }

  /**
   * Creates a string representation of an Attribute set.
   * @param attrs Attributes to convert to String
   * @return String representation of attributes
   */
  protected String attrsToString(Attributes attrs) {
    StringBuffer sb = new StringBuffer("[");
    try {
      for (NamingEnumeration en = attrs.getAll(); en.hasMore(); ) {
        Attribute attr = (Attribute) en.next();
        sb.append(attr.getID() + "=(");
        for (NamingEnumeration enum1 = attr.getAll(); enum1.hasMore(); ) {
          sb.append( (String) enum1.next());
          if (enum1.hasMore())
            sb.append(",");
          else
            sb.append(")");
        }
        if (en.hasMore())
          sb.append(",");
      }
      sb.append("]");
    }
    catch (NamingException ne) {}
    return sb.toString();
  }

  /**
   * Tests whether this agent is the manager for the specified community.
   * @param communityName String
   * @return boolean
   */
  abstract protected boolean isManager(String communityName);

  /**
   * Add agents to distribution list for community updates.
   * @param communityName Name of community
   * @param targets Set of agent names to add to distribution
   */
  abstract protected void addTargets(String communityName, Set targets);

  /**
   * Remove agents from distribution list for community updates.
   * @param communityName Name of community
   * @param targets Set of agent names to remove
   */
  abstract protected void removeTargets(String communityName, Set targets);

  /**
   * Send updated Community info to agents on distribution.
   * @param communityName Name of community
   */
  abstract protected void distributeUpdates(String communityName);

  /**
   * Asserts community manager role.
   * @param communityName Community to manage
   */
  abstract protected void assertCommunityManagerRole(String communityName);
  
  /**
   * asserts a community manager role in the WP and invokes a callback when the bind
   * is complete
   * 
   * @param communityName Community to manage
   * @param callback callback to invoke on completion
   */
  abstract protected void assertCommunityManagerRole(String communityName,
      Callback callback);

}
