/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.community;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.FindCommunityCallback;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.Arguments;

/**
 * This component advertises a {@link CommunityService} implementation that
 * uses the {@link WhitePagesService} to supports basic join, leave, and
 * search operations.
 * <p>
 * This implementation is sufficient to support {@link AttributeBasedAddress}
 * (ABA) relay targets.  Hierarchical communities are not supported.
 * Community members must explicitly join their community at startup, as
 * illustrated in the {@link JoinCommunity} helper plugin.
 */
public class CommunityServiceProvider extends ComponentSupport {

  // the wp is cached, so we poll at a relatively fast rate.
  private static final long DEFAULT_REFRESH_PERIOD = 11000;

  // FIXME "search" can't return null on a cache miss, so we wait
  // a short while to help avoid false "empty" results
  private static final long DEFAULT_WP_TIMEOUT = 2000;

  // our marker key for listing community names, as opposed to members
  private static final String ALL = "*";

  private static final String WP_SUFFIX = ".communities";
  private static final String WP_TYPE = "community";

  // our marker entry for in-progress lookups
  private static final Set PENDING = Collections.singleton("PENDING");

  private Arguments args = Arguments.EMPTY_INSTANCE;
  private long refreshPeriod;
  private long wpTimeout;

  private LoggingService log;
  private ThreadService threadService;
  private WhitePagesService wp;

  private ServiceBroker rootsb;
  private ServiceProvider sp;
  private Schedulable thread;

  private final Object lock = new Object();
  private final Map cache = new LinkedHashMap();
  private final List listeners = new ArrayList();

  public void setParameter(Object o) {
    args = new Arguments(o);
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }
  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }
  public void setWhitePagesService(WhitePagesService wp) {
    this.wp = wp;
  }

  @Override
public void load() {
    super.load();

    refreshPeriod = args.getLong("refreshPeriod", DEFAULT_REFRESH_PERIOD);
    wpTimeout = args.getLong("wpTimeout", DEFAULT_WP_TIMEOUT);

    ServiceBroker sb = getServiceBroker();

    // optional root-level sb
    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
      sb.releaseService(this, NodeControlService.class, ncs);
    }

    // advertise our service
    sp = new TrivialServiceProvider(new CommunityServiceImpl());
    ServiceBroker the_sb = (rootsb == null ? sb : rootsb);
    the_sb.addService(CommunityService.class, sp);
  }
  
  @Override
public void start() {
    super.start();

    // start our refresh thread
    Runnable runner =
      new Runnable() {
        public void run() {
          refresh();
        }
      };
    thread = threadService.getThread(
        this,
        runner,
        "CommunityService refresh poller");
    thread.schedule(refreshPeriod);
  }

  @Override
public void stop() {
    super.stop();

    // stop our refresh thread
    if (thread != null) {
      thread.cancel();
      thread = null;
    }
  }

  @Override
public void unload() {
    super.unload();

    // revoke our service
    if (sp != null) {
      ServiceBroker the_sb = (rootsb == null ? getServiceBroker() : rootsb);
      the_sb.revokeService(CommunityService.class, sp);
      sp = null;
    }
  }

  //
  // the following methods are called by our CommunityServiceImpl
  //

  private void add(CommunityChangeListener ccl) {
    synchronized (lock) {
      // wrap to ensure that it's well behaved
      CommunityChangeListener l = new ListenerImpl(ccl);
      if (listeners.contains(l)) return;
      if (log.isDebugEnabled()) {
        log.debug("Add listener "+l);
      }
      listeners.add(l);
    }
  }
  private void remove(CommunityChangeListener ccl) {
    synchronized (lock) {
      CommunityChangeListener l = new ListenerImpl(ccl);
      if (!listeners.contains(l)) return;
      if (log.isDebugEnabled()) {
        log.debug("Remove listener "+l);
      }
      listeners.remove(l);
    }
  }

  // called by periodic thread
  private void refresh() {
    // get list of community names that we should refresh (null means all)
    Set names;
    synchronized (lock) {
      if (listeners.isEmpty()) {
        names = Collections.EMPTY_SET;
      } else {
        names = null;
        for (int i = 0; i < listeners.size(); i++) {
          CommunityChangeListener li = (CommunityChangeListener)
            listeners.get(i);
          String ci = li.getCommunityName();
          ci = fix(ci);
          if (ci.length() == 0) {
            // all
            names = null;
            break;
          }
          if (names == null) {
            names = new LinkedHashSet();
          }
          names.add(ci);
        }
      }

      if (names != null) {
        // not all
        for (Iterator iter = cache.entrySet().iterator(); iter.hasNext(); ) {
          Map.Entry me = (Map.Entry) iter.next();
          String ci = (String) me.getKey();
          // remove entries we're not listening to (and are not pending)
          if (!names.contains(ci) && (me.getValue() != PENDING)) {
            iter.remove();
          }
        }
      }
    }

    if (names == null) {
      // get all, force lookup
      names = list(ALL, false, null);
      if (names == null) {
        // cache miss, we'll get it next time
        names = Collections.EMPTY_SET;
      }
    }

    // force lookup
    for (Iterator iter = names.iterator(); iter.hasNext(); ) {
      String ci = (String) iter.next();
      list(ci, false, null);
    }

    // wake again later
    if (thread != null) {
      thread.schedule(refreshPeriod);
    }
  }

  /**
   * @param community either ALL or a non-null, non-empty String
   * @return a Set of Strings if ALL, otherwise a Set of MessageAddresses
   *   if not ALL.  This set can be null if !useCache.
   */
  private Set list(
      final String community,
      boolean useCache,
      final CommunityResponseListener crl) {
    // check cache
    Set oldC;
    synchronized (lock) {
      oldC = (Set) cache.get(community);
      if (oldC == null) {
        // new lookup
        cache.put(community, PENDING);
      }
    }
    if (oldC != null) {
      if (oldC == PENDING) {
        // already in progress
        return (useCache ? Collections.EMPTY_SET : null); // FIXME see below.
      } else if (useCache) {
        // in cache
        return oldC;
      } else {
        // do refresh, but don't replace our cached value with PENDING
      }
    }

    if (log.isDebugEnabled()) {
      log.debug((useCache ? "Looking up" : "Refreshing")+" "+community);
    }

    // initiate lookup
    String ext = WP_SUFFIX;
    if (community != ALL) {
      ext = "." + community + WP_SUFFIX;
    }
    Callback cb =
      new Callback() {
        public void execute(Response res) {
          Set members = extractNames(res, community);
          handleSearchResult(community, members, crl);
        }
      };
    Response res = wp.submit(new Request.List(Request.NONE, ext), cb);

    // check for immediate response (i.e. in local wp cache)
    Set ret = extractNames(res, community);
    if (ret != null) return ret;

    if (!useCache) {
      // return the stale value (null or non-PENDING)
      return oldC;
    }

    // FIXME the CommunityService API should return null if there's a cache
    // miss, otherwise the caller can't distinguish between a cache miss and
    // an empty/non-existent community!
    //
    // However, the CommunityService is currently defined to return an empty
    // set on a cache miss, and the Blackboard "lookupABA(...)" method expects
    // a non-null return value.
    //
    // So, for now, we must return a non-null value.
    if (wpTimeout > 0) {
      try {
        res.waitForIsAvailable(wpTimeout);
      } catch (InterruptedException ie) {
      }
      ret = extractNames(res, community);
      if (ret != null) return ret;
    }
    return Collections.EMPTY_SET; // should be null!
  }

  // convert "[A.MyComm.communities, B.MyComm.communities]" into "[A, B]"
  //
  // also works for "[.MyComm.communities]" --> "[MyComm]"
  private static final Set extractNames(Response res, String community) {
    if (!res.isSuccess()) return null;
    Set set = ((Response.List) res).getNames();
    if (set == null) return null;
    if (set.isEmpty()) return Collections.EMPTY_SET;
    String ext = WP_SUFFIX;
    if (community != ALL) {
      ext = "." + community + WP_SUFFIX;
    }
    Set ret = new LinkedHashSet();
    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
      String s = (String) iter.next();
      if (!s.endsWith(ext)) continue;
      s = s.substring(0, s.length()-ext.length());
      Object o;
      if (community == ALL) {
        if (s.length() <= 1 || s.charAt(0) != '.') continue;
        s = s.substring(1);
        o = s;
      } else {
        o = MessageAddress.getMessageAddress(s);
      }
      ret.add(o);
    }
    return Collections.unmodifiableSet(ret);
  }

  private void handleSearchResult(
      String community,
      Set members,
      CommunityResponseListener crl) {

    // gather change listeners
    List l = Collections.EMPTY_LIST;

    if (members == null) {
      // lookup failed
      if (log.isDebugEnabled()) {
        log.debug("Lookup for "+community+" failed");
      }
    } else {
      synchronized (lock) {
        Set oldC = (Set) cache.get(community);

        boolean changed = (oldC == PENDING || !members.equals(oldC));
        if (changed) {
          // modify cache
          cache.put(community, members);
          if (community != ALL) {
            // find interested change listeners
            for (int i = 0; i < listeners.size(); i++) {
              CommunityChangeListener li = (CommunityChangeListener)
                listeners.get(i);
              String ci = li.getCommunityName();
              ci = fix(ci);
              if (ci.length() == 0 || ci.equals(community)) {
                if (l.isEmpty()) {
                  l = new ArrayList();
                }
                l.add(li);
              }
            }
          }
        }

        if (log.isDebugEnabled()) {
          log.debug(
              "Lookup for "+community+" found "+
              (!changed ? "no change" :
               (((oldC == null || oldC == PENDING) ? "initial" : "modified")+
                " membership list["+members.size()+"]:\n  "+members+
                (community == ALL ? "" :
                 ("\nTelling "+l.size()+" listener"+
                  (l.size() == 1 ? "" : "s"))))));
        }
      }
    }

    // tell search listener
    if (crl != null) {
      crl.getResponse(new CommunityResponseImpl((members != null), community));
    }

    if (l.isEmpty()) {
      // no change listeners
      return;
    }

    // create event
    CommunityChangeEvent cce = 
      new CommunityChangeEvent(new CommunityImpl(community), -1, null);

    // invoke listeners
    //
    // our listeners should be well behaved.  Minimally the Blackboard
    // does the right thing: it kicks off a pooled thread.
    for (int i = 0; i < l.size(); i++) {
      CommunityChangeListener li = (CommunityChangeListener) l.get(i);
      try {
        li.communityChanged(cce);
      } catch (Exception e) {
        log.error(
            "Removing listener "+li+" that failed on callback for "+cce, e);
        synchronized (lock) {
          listeners.remove(li);
        }
      }
    }
  }

  private void modify(
      final boolean isJoin,
      final String community,
      final String agent,
      final CommunityResponseListener crl) {
    // define wp entry
    //
    // RFE use the uri to specify meta-data about this agent, e.g.:
    //   "?type=blah"
    // and/or, since we don't have a manager, about its community, e.g.:
    //   "?parent=Foo"
    String name = agent + "." + community + WP_SUFFIX;
    URI uri = URI.create("role://member");
    AddressEntry ae = AddressEntry.getAddressEntry(name, WP_TYPE, uri);

    if (log.isDebugEnabled()) {
      log.debug(
          (isJoin ? "Adding" : "Removing")+" agent "+agent+" "+
          (isJoin ? "to" : "from")+" community "+community);
    }

    // make wp callback wrapper
    Callback cb = 
      new Callback() {
        public void execute(Response res) {
          boolean isSuccess = 
            (isJoin ?
             ((Response.Bind) res).didBind() :
             ((Response.Unbind) res).didUnbind());
          handleModifyResult(isJoin, community, agent, crl, isSuccess);
        }
      };

    if (isJoin) {
      wp.rebind(ae, cb);
    } else {
      wp.unbind(ae, cb);
    }
  }
  
  private void handleModifyResult(
      boolean isJoin,
      final String community,
      String agent,
      CommunityResponseListener crl,
      final boolean isSuccess) {

    if (log.isDebugEnabled()) {
      log.debug(
          (isSuccess ? 
           (isJoin ? "Added" : "Removed") :
           ("Unable to "+(isJoin ? "add" : "remove")))+
          " agent "+agent+" "+(isJoin ? "to" : "from")+
          " community "+community);
    }

    // update the cache
    if (isSuccess) {
      synchronized (lock) {
        Set oldC = (Set) cache.get(community);
        if (oldC != null && oldC != PENDING &&
            (isJoin != oldC.contains(agent))) {
          // it's safer to decache then to alter the cache entry.  We want our
          // cache to match whatevers in the wp, not our half-baked view of the
          // world.
          if (log.isDebugEnabled()) {
            log.debug("Decaching "+community);
          }
          cache.remove(community);
        }
      }
    }

    // tell optional listener
    if (crl != null) {
      crl.getResponse(new CommunityResponseImpl(isSuccess, community));
    }
  }


  private static final String fix(String s) {
    String ret = s;
    if (ret == null || ret.equals("*")) {
      ret = "";
    } else {
      ret = ret.trim();
    }
    return ret;
  }

  /**
   * Our service implementation.
   */
  private class CommunityServiceImpl implements CommunityService {

    public void addListener(CommunityChangeListener l) {
      if (l == null) {
        throw new IllegalArgumentException("Null listener");
      }
      add(l);
    }
    public void removeListener(CommunityChangeListener l) {
      if (l == null) {
        throw new IllegalArgumentException("Null listener");
      }
      remove(l);
    }

    public Collection search(String communityName, String filter) {
      return searchCommunity(
          communityName, filter, false, Community.AGENTS_ONLY, null);
    }
    public Collection searchCommunity(
        String communityName,
        String searchFilter,
        boolean recursiveSearch,
        int resultQualifier,
        CommunityResponseListener crl) {
      // filter must be "(Role=Member)"
      if (searchFilter != null && !searchFilter.equals("(Role=Member)")) {
        throw new UnsupportedOperationException(
            "Only supports null or \"(Role=Member)\" search filter, not "+
            searchFilter);
      }
      // trim community name
      String c = fix(communityName);
      if (c.length() == 0) {
        throw new IllegalArgumentException(
            "Invalid community name: "+communityName);
      }
      // ignore "recursiveSearch", since our "join" forces all communities to
      // be non-hierarchical anyways  i.e. no recursion necessary)
      //
      // our namespace is flat, so there are no children communities
      if (resultQualifier == Community.COMMUNITIES_ONLY) {
        return Collections.EMPTY_SET;
      }
      if (resultQualifier != Community.AGENTS_ONLY &&
          resultQualifier != Community.ALL_ENTITIES) {
        throw new IllegalArgumentException(
            "Invalid result qualifier: "+resultQualifier);
      }
      return list(c, true, crl);
    }

    /** @deprecated */
    public Collection listAllCommunities() {
      return list(ALL, true, null);
    }
    public void listAllCommunities(CommunityResponseListener crl) {
      list(ALL, true, crl);
    }

    public void joinCommunity(
        String communityName,
        String entityName,
        int entityType,
        Attributes entityAttrs,
        boolean createIfNotFound,
        Attributes newCommunityAttrs,
        CommunityResponseListener crl) {
      // trim community name
      String c = fix(communityName);
      if (c.length() == 0) {
        throw new IllegalArgumentException(
            "Invalid community name: "+communityName);
      }
      // trim agent name
      String a = fix(entityName);
      if (a.length() == 0) {
        throw new IllegalArgumentException(
            "Invalid agent name: "+entityName);
      }
      // must be of type AGENT
      if (entityType != AGENT) {
        throw new UnsupportedOperationException(
            "Only supports AGENT ("+AGENT+") entities, not "+entityType);
      }
      // must be createIfNotFound
      if (!createIfNotFound) {
        throw new UnsupportedOperationException(
            "Only supports createIfNotFound=true");
      }
      // no attrs
      if (entityAttrs != null && entityAttrs.size() > 0) {
        throw new UnsupportedOperationException(
            "Only supports null or empty agent attributes, not "+
            entityAttrs);
      }
      if (newCommunityAttrs != null && newCommunityAttrs.size() > 0) {
        throw new UnsupportedOperationException(
            "Only supports null or empty agent attributes, not "+
            newCommunityAttrs);
      }

      modify(true, c, a, crl);
    }

    public void leaveCommunity(
        String communityName,
        String entityName,
        CommunityResponseListener crl) {
      // trim community name
      String c = fix(communityName);
      if (c.length() == 0) {
        throw new IllegalArgumentException(
            "Invalid community name: "+communityName);
      }
      // trim agent name
      String a = fix(entityName);
      if (a.length() == 0) {
        throw new IllegalArgumentException(
            "Invalid agent name: "+entityName);
      }

      modify(false, c, a, crl);
    }

    //
    // The rest are not supported!
    //
    // We could support some of the "search" methods below, e.g.:
    //   getCommunity
    //   findCommunity
    //
    // Create/modify don't quite make sense, since we don't have a community
    // manager or a wp "metadata" entry that describes the overall community.
    //
    // We currently don't support hierarchical communities, so no "parents"
    // for now.  This could be added by having every child community entry
    // specify its parent, e.g.
    //   AgentX.MyChildComm.communities is role://member?parent=MyParentComm
    // However, this makes it expensive to find children.
    //

    /** @deprecated */
    public void createCommunity(
        String communityName,
        Attributes attrs,
        CommunityResponseListener crl) { die(); }
    public Community getCommunity(
        String communityName,
        CommunityResponseListener crl) { die(); return null; }
    public void modifyAttributes(
        String communityName,
        String entityName,
        ModificationItem[] mods,
        CommunityResponseListener crl) { die(); }
    public Collection listParentCommunities(
        String member,
        CommunityResponseListener crl) { die(); return null; }
    public Collection listParentCommunities(
        String member,
        String filter,
        CommunityResponseListener crl) { die(); return null; }
    public void findCommunity(
        String communityName,
        FindCommunityCallback fccb,
        long timeout) { die(); }
    /** @deprecated */
    public String[] getParentCommunities(boolean allLevels) {
      die(); return null;
    }
    /** @deprecated */
    public Collection listParentCommunities(String member) {
      die(); return null;
    }
    /** @deprecated */
    public Collection listParentCommunities(String member, String filter) {
      die(); return null;
    }

    private void die() { throw new UnsupportedOperationException(); }
  }

  /**
   * A trivial service provider implementation, for where we want to return the
   * same service instance to all clients.
   */
  private static final class TrivialServiceProvider implements ServiceProvider {
    private final Object svc;
    public TrivialServiceProvider(Object svc) {
      this.svc = svc;
    }
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      return svc;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service) {
    }
  }

  /**
   * A minimal {@link Community} implementation, required because the
   * {@link CommunityChangeEvent} constructor calls "community.getName()".
   */
  private static final class CommunityImpl implements Community {
    private final String name;
    public CommunityImpl(String name) { this.name = name; }

    public String getName() { return name; }

    // the rest are not supported:
    public void setName(String name) { die(); }
    public void setAttributes(Attributes attrs) { die(); }
    public Attributes getAttributes() { die(); return null; }
    public String toXml() { die(); return null; }
    public String toXml(String indent) { die(); return null; }
    public String attrsToString() { die(); return null; }
    //
    public Collection getEntities() { die(); return null; }
    public Entity getEntity(String name) { die(); return null; }
    public boolean hasEntity(String name) { die(); return false; }
    public void addEntity(Entity entity) { die(); }
    public void removeEntity(String entityName) { die(); }
    public Set search(String filter, int qualifier) {
      die(); return null;
    }
    public String qualifierToString(int qualifier) {
      die(); return null;
    }
    //
    private void die() { throw new UnsupportedOperationException(); }
  }

  private static final class CommunityResponseImpl implements CommunityResponse{
    private final boolean isSuccess;
    private final String community;

    public CommunityResponseImpl(boolean isSuccess, String community) {
      this.isSuccess = isSuccess;
      this.community = community;
    }
    public int getStatus() { 
      return (isSuccess ? SUCCESS : FAIL);
    }
    public String getStatusAsString() {
      return (isSuccess ? "SUCCESS" : "FAIL");
    }
    public Object getContent() {
      return new CommunityImpl(community);
    }
  }

  /**
   * This is a paraniod wrapper around the listener callback, to make
   * sure that "getCommunityName()" is well behaved.
   */
  private static final class ListenerImpl implements CommunityChangeListener {
    private final String s;
    private final CommunityChangeListener l;

    public ListenerImpl(CommunityChangeListener l) {
      this.l = l;
      this.s = fix(l.getCommunityName());
    }
    public String getCommunityName() { return s; }
    public void communityChanged(CommunityChangeEvent event) {
      l.communityChanged(event);
    }
    @Override
   public int hashCode() {
      return System.identityHashCode(l);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof ListenerImpl)) return false;
      return l == ((ListenerImpl) o).l;
    }
    @Override
   public String toString() {
      return "(listener for community "+(s.length() == 0 ? "*" : s)+")";
    }
  }
}
