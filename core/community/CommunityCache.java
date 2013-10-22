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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

import javax.naming.directory.Attributes;

/**
 * Maintains a local cache of Community objects.
 */
public class CommunityCache implements CommunityServiceConstants {

  protected Logger logger = LoggerFactory.getInstance().createLogger(CommunityCache.class);
  protected Map communities = Collections.synchronizedMap(new HashMap());
  protected Map listenerMap = Collections.synchronizedMap(new HashMap());
  protected ThreadService threadService;
  protected long expirationPeriod = DEFAULT_CACHE_EXPIRATION;

  private static DateFormat df = new SimpleDateFormat("HH:mm:ss,SSS");

  public CommunityCache(ThreadService ts) {
    this.threadService = ts;
    getSystemProperties();
  }

  public CommunityCache(ThreadService ts, long expiration) {
    this(ts);
    this.expirationPeriod = expiration;
  }

  protected void getSystemProperties() {
    try {
      expirationPeriod =
          Long.parseLong(System.getProperty(CACHE_EXPIRATION_PROPERTY,
                                            Long.toString(DEFAULT_CACHE_EXPIRATION)));
    } catch (Exception ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Exception setting parameter from system property", ex);
      }
    }
  }

  public synchronized Community get(String name) {
    //TODO: Add authorization check
    Community community = null;
    CacheEntry ce = (CacheEntry)communities.get(name);
    if (ce != null) {
      if (isExpired(ce)) {
        // Flush cache entry if expired
        flushCacheEntry(ce);
      } else {
        community = (CommunityImpl)ce.community;
      }
    }
    return community;
  }

  /**
   * Searches all communities in cache for a community matching search filter
   * @param filter JNDI-compliant search filter
   * @return Set of Community object matching search criteria
   */
  public synchronized Set search(String filter) {
    //TODO: Add authorization check
    if (communities.isEmpty())
      return null;
    Set matches = new HashSet();
    try {
      Filter f = new SearchStringParser().parse(filter);
      for (Iterator it = communities.values().iterator(); it.hasNext(); ) {
        CacheEntry ce = (CacheEntry)it.next();
        CommunityImpl community = ce.community;
        if (f.match(community.getAttributes()))
          matches.add(community);
      }
    }
    catch (Exception ex) {
      System.out.println("Exception in search, filter=" + filter);
      ex.printStackTrace();
    }
    if (logger.isDetailEnabled())
      logger.detail("search: matches=" + CommunityUtils.entityNames(matches));
    return matches;
  }

  /**
   * Searches community for all entities matching search filter
   * @param communityName  Name of community to search
   * @param filter JNDI-compliant search filter
   * @param qualifier Restict returned Entities to AGENTS_ONLY, COMMUNITIES_ONLY,
   *     or ALL_ENTITIES (refer to org.cougaar.core.service.community.Community
   *     for values)
   * @param recursive Controls whether search includes nested communities if any
   * @return Set of Entity objects matching search criteria
   */
  public Set search(String  communityName,
                       String  filter,
                       int     qualifier,
                       boolean recursive) {
    Community community = get(communityName);
    if (logger.isDetailEnabled()) {
      logger.detail("search:" +
                   " community=" + communityName +
                   " filter=" + filter +
                   " qualifier=" + community.qualifierToString(qualifier) +
                   " recursive=" + recursive);
    }
    if (community == null) return Collections.EMPTY_SET;
    if (recursive) {
      Set matches = new HashSet();
      recursiveSearch(community, filter, qualifier, matches, new HashSet());
      return matches;
    } else {  // Recursive search
      return community.search(filter, qualifier);
    }
  }

  /*
   * Recursive search of community map for all ancestors of a specified entity.
   */
  private synchronized void findAncestors(String entityName, Set ancestors, boolean recursive) {
    Collection allCommunities = communities.values();
    if (logger.isDetailEnabled()) {
      logger.detail("findAncestors:" +
                    " entity=" + entityName +
                    " ancestors=" + ancestors +
                    " communities=" + allCommunities.size());
    }
    for (Iterator it = allCommunities.iterator(); it.hasNext();) {
      CacheEntry ce = (CacheEntry)it.next();
      Community community = ce.community;
      if (community.hasEntity(entityName)) {
        String parent = community.getName();
        ancestors.add(parent);
        if (recursive) findAncestors(parent, ancestors, recursive);
      }
    }
  }

  public synchronized void update(Community community) {
    //TODO: Add authorization check
    CommunityImpl ci = (CommunityImpl)community;
    CacheEntry ce = (CacheEntry)communities.get(community.getName());
    if (ce != null) {
      if (ci.getLastUpdate() >= ce.community.getLastUpdate()) {
        ce.timeStamp = now();
        //CommunityImpl prior = ce.community;
        //ce.community = (CommunityImpl)community;
        if (logger.isDebugEnabled()) {
          logger.debug("update:" +
                       " community=" + community.getName() +
                       " prior=" + (ce.community == null ? -1 : ce.community.getEntities().size()) +
                       " updated=" + ce.community.getEntities().size() +
                       " expires=" + (expirationPeriod == NEVER
                                      ? "NEVER"
                                      : df.format(new Date(ce.timeStamp + expirationPeriod))));
        }
        if (logger.isDetailEnabled()) {
          logger.detail(this.toString());
        }
        fireChangeNotifications(ce.community, community);
      }
    } else {
      ce = new CacheEntry(now(), (CommunityImpl)ci.clone());
      communities.put(community.getName(), ce);
      if (logger.isDebugEnabled()) {
        logger.debug("add:" +
                     " community=" + community.getName() +
                     " prior=null" +
                     " updated=" + ce.community.getEntities().size() +
                     " expires=" + (expirationPeriod == NEVER
                                    ? "NEVER"
                                    : df.format(new Date(ce.timeStamp + expirationPeriod))));
      }
      if (logger.isDetailEnabled()) {
        logger.detail(this.toString());
      }
      fireChangeNotifications(ce.community, null);
    }
  }

  public synchronized String toString() {
    return "CommunityCache: contents=" + communities.keySet().toString();
  }

  public synchronized String toXML() {
    //TODO: Add authorization check
    StringBuffer sb = new StringBuffer();
    for (Iterator it = communities.values().iterator(); it.hasNext();) {
      CacheEntry ce = (CacheEntry)it.next();
      sb.append(ce.community.toXml());
    }
    return sb.toString();
  }

  public synchronized Set listAll() {
    //TODO: Add authorization check
    return new HashSet(communities.keySet());
  }

   /**
   * Searches cache for all ancestors of specified entity.
   * @param entityName Entity name
   * @param recursive If true all ancestors are retrieved, if false only immediate
   *                  parents
   * @return List of communities having specified community as a descendent
   */
  public Set getAncestorNames(String entityName, boolean recursive) {
    //TODO: Add authorization check
   Set ancestors = new HashSet();
    if (logger.isDetailEnabled()) {
      logger.detail("getAncestorNames:" +
                    " entity=" + entityName +
                    " recursive=" + recursive +
                    " ancestors=" + ancestors);
    }
    findAncestors(entityName, ancestors, recursive);
    return ancestors;
  }

  /**
   * Add listener to be notified when a change occurs to community.
   * @param l  Listener to be notified
   */
  public void addListener(CommunityChangeListener l) {
    if (l != null) addListener(l.getCommunityName(), l);
  }

  /**
   * Removes listener from change notification list.
   * @param l  Listener to be removed
   */
  public boolean removeListener(CommunityChangeListener l) {
    if (l != null) {
      String communityName = l.getCommunityName();
      if (l == null) {
        communityName = "ALL_COMMUNITIES";
      }
      if (logger.isDetailEnabled()) {
        logger.detail("removeListener: community=" + communityName);
      }
      synchronized (listenerMap) {
        Set listeners = (Set)listenerMap.get(communityName);
        if (listeners != null && listeners.contains(l)) {
          listeners.remove(l);
          return true;
        }
      }
    }
    return false;
  }

  public synchronized boolean contains(String name) {
    boolean containsCurrentEntry = false;
    CacheEntry ce = (CacheEntry)communities.get(name);
    if (ce != null) {
      if (isExpired(ce)) {
        // Flush cache entry if expired
        flushCacheEntry(ce);
      } else {
        containsCurrentEntry = true;
      }
    }
    return containsCurrentEntry;
  }

  public synchronized Community remove(String communityName) {
    if (logger.isDebugEnabled()) {
      logger.debug("remove:" +
                   " community=" + communityName);
    }
    CacheEntry ce = (CacheEntry)communities.remove(communityName);
    return (ce == null ? null : ce.community);
  }

  private void fireChangeNotifications(Community current, Community updated) {
    if (logger.isDetailEnabled()) {
      logger.detail("fireChangeNotifications: community=" + (updated == null ? "null" : updated.getName()));
    }
    //Community community = get(updated.getName()); // a copy for Change Event
    if (updated == null) {  // new community
      notifyListeners(new CommunityChangeEvent(current,
                                               CommunityChangeEvent.ADD_COMMUNITY,
                                               current.getName()));
      for (Iterator it = current.getEntities().iterator(); it.hasNext();) {
        Entity entity = (Entity)it.next();
        notifyListeners(new CommunityChangeEvent(current,
                                                 CommunityChangeEvent.ADD_ENTITY,
                                                 entity.getName()));
      }
    } else {

      // Updated community attributes
      if (!attributesEqual(current.getAttributes(), updated.getAttributes())) {
        current.setAttributes((Attributes)updated.getAttributes().clone());
        notifyListeners(new CommunityChangeEvent(current,
                                                 CommunityChangeEvent.COMMUNITY_ATTRIBUTES_CHANGED,
                                                 current.getName()));
      }

      // Added Entities
      Collection addedEntities =
          listAddedEntities(current.getEntities(), updated.getEntities());
      for (Iterator it = addedEntities.iterator(); it.hasNext();) {
        String entityName = (String)it.next();
        current.addEntity(updated.getEntity(entityName));
        notifyListeners(new CommunityChangeEvent(current,
                                                 CommunityChangeEvent.ADD_ENTITY,
                                                 entityName));
      }

      // Removed Entities
      Collection removedEntities =
          listRemovedEntities(current.getEntities(), updated.getEntities());
      for (Iterator it = removedEntities.iterator(); it.hasNext();) {
        String entityName = (String)it.next();
        current.removeEntity(entityName);
        notifyListeners(new CommunityChangeEvent(current,
                                                 CommunityChangeEvent.REMOVE_ENTITY,
                                                 entityName));
      }

      // Entities with changed attributes
      for (Iterator it = current.getEntities().iterator(); it.hasNext();) {
        Entity curEntity = (Entity)it.next();
        Entity updatedEntity = updated.getEntity(curEntity.getName());
        if (updatedEntity != null &&
            !attributesEqual(curEntity.getAttributes(), updatedEntity.getAttributes())) {
          curEntity.setAttributes((Attributes)updatedEntity.getAttributes().clone());
          notifyListeners(new CommunityChangeEvent(current,
                                                   CommunityChangeEvent.ENTITY_ATTRIBUTES_CHANGED,
                                                   curEntity.getName()));
        }
      }
    }
  }

  private boolean attributesEqual(Attributes attrs1, Attributes attrs2) {
    return (attrs1 == null && attrs2 == null) ||
           attrs1 != null && attrs1.equals(attrs2);
  }

  private Collection listAddedEntities(Collection prior, Collection current) {
    Collection added = CommunityUtils.getEntityNames(current);
    added.removeAll(CommunityUtils.getEntityNames(prior));
    return added;
  }

  private Collection listRemovedEntities(Collection prior, Collection current) {
    Collection removed = CommunityUtils.getEntityNames(prior);
    removed.removeAll(CommunityUtils.getEntityNames(current));
    return removed;
  }

  private long now() {
    return System.currentTimeMillis();
  }

  private boolean isExpired(CacheEntry ce) {
    return (expirationPeriod != NEVER && (ce.timeStamp + expirationPeriod) < now());
  }

  private void flushCacheEntry(CacheEntry ce) {
    if (logger.isInfoEnabled()) {
      logger.info("flushEntry: community=" + ce.community.getName());
    }
    remove(ce.community.getName());
  }

  /*
   * Determines if a local copy exists for all nested communities from a
   * specified root community.
   */
  private synchronized boolean allDescendentsFound(Community community) {
    Collection nestedCommunities =
        community.search("(Role=Member)", Community.COMMUNITIES_ONLY);
    for (Iterator it = nestedCommunities.iterator(); it.hasNext();) {
      Community nestedCommunity = (Community)it.next();
      if (!communities.containsKey(nestedCommunity.getName()) ||
          !allDescendentsFound(nestedCommunity)) return false;
    }
    return true;
  }

  private void recursiveSearch(Community community,
                               String filter,
                               int qualifier,
                               Set matches,
                               Set visited) {
    if (community != null) {
      visited.add(community.getName());  // avoid endless loop caused by circular references
      Collection entities = community.search(filter, qualifier);
      if (logger.isDetailEnabled()) {
        logger.detail("recursiveSearch:" +
                      " community=" + community.getName() +
                      " filter=" + filter +
                      " qualifier=" + qualifier +
                      " matches=" + CommunityUtils.entityNames(entities));
      }
      matches.addAll(entities);
      for (Iterator it = community.getEntities().iterator(); it.hasNext(); ) {
        Entity entity = (Entity)it.next();
        if (entity instanceof Community) {
          String nestedCommunityName = entity.getName();
          Community nestedCommunity = get(nestedCommunityName);
          if (nestedCommunity != null && !visited.contains(nestedCommunity.getName())) {
            recursiveSearch(nestedCommunity, filter, qualifier, matches, visited);
          }
        }
      }
    }
  }

  /**
   * Invoke callback on each CommunityListener associated with named
   * community and its ancestors.  Provide community reference in callback
   * argument.
   * @param cce CommunityChangeEvent to fire
   */
  private void notifyListeners(CommunityChangeEvent cce) {
    Set listenerSet = new HashSet();
    Set affectedCommunities = new HashSet();
    affectedCommunities.add(cce.getCommunityName());
    affectedCommunities.addAll(getAncestorNames(cce.getCommunityName(), true));
    for (Iterator it = affectedCommunities.iterator(); it.hasNext();) {
      Set listeners = getListeners((String)it.next());
      for (Iterator it1 = listeners.iterator(); it1.hasNext();) {
        listenerSet.add((CommunityChangeListener)it1.next());
      }
    }
    Set listeners = getListeners("ALL_COMMUNITIES");
    for (Iterator it = listeners.iterator(); it.hasNext();) {
      listenerSet.add((CommunityChangeListener)it.next());
    }
    if (logger.isDetailEnabled()) {
      logger.detail("notifyListeners:" +
                   " community=" + cce.getCommunityName() +
                   " changeType=" + CommunityChangeEvent.getChangeTypeAsString(cce.getType()) +
                   " whatChanged=" + cce.getWhatChanged() +
                   " numListeners=" + listenerSet.size());
    }
    fireCommunityChangeEvent(listenerSet,  cce);
  }

  private void fireCommunityChangeEvent(CommunityChangeListener l,
                                          CommunityChangeEvent cce) {
    Set listeners = new HashSet();
    listeners.add(l);
    fireCommunityChangeEvent(listeners, cce);
  }

  private void fireCommunityChangeEvent(final Set listeners,
                                          final CommunityChangeEvent cce) {
    if (threadService != null) { // use Cougaar threads
      threadService.getThread(this, new Runnable() {
        public void run() {
          for (Iterator it = listeners.iterator(); it.hasNext(); ) {
            ((CommunityChangeListener)it.next()).communityChanged(cce);
          }
        }
      } , "CommunityNotificationThread").start();
    } else {  // Use regular Java threads
      new Thread("CommunityNotificationThread") {
        public void run() {
          for (Iterator it = listeners.iterator(); it.hasNext(); ) {
            ((CommunityChangeListener)it.next()).communityChanged(cce);
          }
        }
      }.start();
    }
  }

  private synchronized void addListener(String communityName, CommunityChangeListener l) {
    if (l != null) {
      String cname = (communityName != null ? communityName : "ALL_COMMUNITIES");
      if (logger.isDetailEnabled()) {
        logger.detail("addListeners:" +
                     " community=" + cname);
      }
      synchronized (listenerMap) {
        Set listeners = (Set)listenerMap.get(cname);
        if (listeners == null) {
          listeners = new HashSet();
          listenerMap.put(cname, listeners);
        }
        listeners.add(l);
        // If listener is interested in communities which are already in cache
        // send an initial event
        if (cname.equals("ALL_COMMUNITIES")) {
          for (Iterator it = communities.values().iterator(); it.hasNext();) {
            CacheEntry ce = (CacheEntry)it.next();
            Community community = ce.community;
            fireCommunityChangeEvent(l, new CommunityChangeEvent(community,
                                                                 CommunityChangeEvent.ADD_COMMUNITY,
                                                                 community.getName()));
            for (Iterator it1 = community.getEntities().iterator(); it1.hasNext();) {
              Entity entity = (Entity)it1.next();
              fireCommunityChangeEvent(l, new CommunityChangeEvent(community,
                                                                   CommunityChangeEvent.ADD_ENTITY,
                                                                   entity.getName()));
            }
          }
        } else {
          Community community = get(cname);
          if (community != null) {
            fireCommunityChangeEvent(l, new CommunityChangeEvent(community,
                                                                 CommunityChangeEvent.ADD_COMMUNITY,
                                                                 community.getName()));
            for (Iterator it = community.getEntities().iterator(); it.hasNext(); ) {
              Entity entity = (Entity)it.next();
              fireCommunityChangeEvent(l, new CommunityChangeEvent(community,
                                                                   CommunityChangeEvent.ADD_ENTITY,
                                                                   entity.getName()));
            }
          }
        }
      }
    }
  }

  /**
   * Gets listeners.
   * @param communityName Name of communtiy
   * @return Set of CommunityListeners.
   */
  protected Set getListeners(String communityName) {
    synchronized (listenerMap) {
      if (!listenerMap.containsKey(communityName)) {
        listenerMap.put(communityName, new HashSet());
      }
      return new HashSet((Set)listenerMap.get(communityName));
    }
  }

  class CacheEntry {
    private long timeStamp;
    private CommunityImpl community;
    CacheEntry(long timeStamp, CommunityImpl community) {
      this.timeStamp = timeStamp;
      this.community = community;
    }
  }

}
