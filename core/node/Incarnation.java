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

package org.cougaar.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.IdentityHashSet;

/**
 * This component provides the {@link IncarnationService} that
 * monitors agent incarnation (version) numbers in the {@link
 * WhitePagesService} and notifies clients of any changes.
 * 
 * @property org.cougaar.core.node.incarnation.period
 * Milliseconds between white pages incarnation polling to detect
 * agent restarts, defaults to 43000. 
 */
public final class Incarnation
extends GenericStateModelAdapter
implements Component
{

  private static final long RESTART_CHECK_INTERVAL = 
    SystemProperties.getLong(
        "org.cougaar.core.node.incarnation.period",
        43000L);

  private ServiceBroker sb;

  private LoggingService log;
  private ServiceBroker rootsb;
  private WhitePagesService wps;

  private IncarnationSP isp;

  private Schedulable pollThread;

  // map of agent name to an entry with the most recently observed
  // incarnation and listener callbacks
  private final Map incarnationMap = new HashMap();

  // WP callbacks for non-blocking lookups
  private final Map pendingMap = new HashMap();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);

    // get root sb
    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException(
          "Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    // get wp
    wps = sb.getService(this, WhitePagesService.class, null);
    if (wps == null) {
      throw new RuntimeException(
          "Unable to obtain WhitePagesService");
    }

    // get thread
    ThreadService threadService = sb.getService(this, ThreadService.class, null);
    Runnable pollRunner = 
      new Runnable() {
        public void run() {
          pollWhitePages();
        }
      };
    pollThread = threadService.getThread(
        this, pollRunner, "Incarnation");
    sb.releaseService(this, ThreadService.class, threadService);

    // assume we're running
    pollThread.schedule(
        RESTART_CHECK_INTERVAL,
        RESTART_CHECK_INTERVAL);

    // advertise our service
    isp = new IncarnationSP();
    rootsb.addService(IncarnationService.class, isp);
  }

  @Override
public void unload() {
    if (pollThread != null) {
      pollThread.cancelTimer();
      pollThread = null;
    }
    if (isp != null) {
      sb.revokeService(IncarnationService.class, isp);
      isp = null;
    }
    if (wps != null) {
      sb.releaseService(
          this, WhitePagesService.class, wps);
      wps = null;
    }
    super.unload();
  }

  private long getIncarnation(MessageAddress agentId) {
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        return 0;
      }
      return e.getIncarnation();
    }
  }

  private int updateIncarnation(MessageAddress agentId, long inc) {
    if (inc <= 0) {
      // invalid incarnation (wp cache miss?)
      return 0;
    }
    List callbacks;
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        // no subscribers for this information!
        return 0;
      }
      long cachedInc = e.getIncarnation();
      if (inc == cachedInc) {
        // no change
        return 0;
      }
      if (inc < cachedInc) {
        // stale incarnation
        return -1; 
      }
      // increase
      if (log.isInfoEnabled()) {
        log.info(
            "Update agent "+agentId+
            " from "+cachedInc+
            " to "+inc);
      }
      e.setIncarnation(inc);
      if (cachedInc == 0) {
        // first time, don't invoke callbacks
        return 0;
      }
      // get callbacks
      callbacks = e.getCallbacks();
    }
    // invoke callbacks in the caller's thread
    int n = (callbacks == null ? 0 : callbacks.size());
    for (int i = 0; i < n; i++) {
      IncarnationService.Callback cb = (IncarnationService.Callback)
        callbacks.get(i);
      if (log.isDebugEnabled()) {
        log.debug(
            "Invoking callback("+agentId+", "+inc+")["+
            i+" / "+n+"]: "+cb);
      }
      cb.incarnationChanged(agentId, inc);
    }
    return 1;
  }

  private boolean subscribe(
      MessageAddress agentId,
      IncarnationService.Callback cb,
      long initialInc) {
    if (agentId == null || cb == null) {
      throw new IllegalArgumentException(
          "null "+(agentId == null ? "addr" : "cb"));
    }
    if (agentId.isGroupAddress()) {
      // ignore multicast addresses and the like
      return false;
    }
    long inc = initialInc;
    while (true) {
      long cachedInc;
      synchronized (incarnationMap) {
        Entry e = (Entry) incarnationMap.get(agentId);
        if (e == null) {
          if (log.isInfoEnabled()) {
            log.info("Adding "+agentId);
          }
          e = new Entry();
          incarnationMap.put(agentId, e);
        }
        cachedInc = e.getIncarnation();
        if (inc <= 0 || inc == cachedInc) {
          // okay to add now
          boolean ret = e.addCallback(cb);
          if (log.isDetailEnabled()) {
            log.detail(
                "addCallback("+
                agentId+", "+cb+", "+inc+")="+ret);
          }
          return ret;
        }
      }
      // inc != cachedInc, so we must bring them into sync,
      // but don't invoke callbacks while holding the lock!
      if (inc >= cachedInc) {
        updateIncarnation(agentId, inc);
        continue;
      }
      if (log.isDebugEnabled()) {
        log.debug("Invoking callback("+agentId+", "+inc+"):"+ cb);
      }
      cb.incarnationChanged(agentId, cachedInc);
      inc = cachedInc;
    }
  }

  private boolean unsubscribe(
      MessageAddress agentId,
      IncarnationService.Callback cb) {
    if (agentId == null || cb == null) {
      throw new IllegalArgumentException(
          "null "+(agentId == null ? "addr" : "cb"));
    }
    synchronized (incarnationMap) {
      Entry e = (Entry) incarnationMap.get(agentId);
      if (e == null) {
        return false;
      }
      if (!e.removeCallback(cb)) {
        return false;
      }
      if (!e.hasCallbacks()) {
        incarnationMap.remove(agentId);
        if (log.isInfoEnabled()) {
          log.info("Removing "+agentId);
        }
      }
      return true;
    }
  }

  /**
   * Periodically called to poll for remote agent incarnation
   * changes.
   */
  private void pollWhitePages() {
    if (log.isDebugEnabled()) {
      log.debug("pollWhitePages");
    }
    // snapshot the agent names
    Set agentIds;
    synchronized (incarnationMap) {
      if (incarnationMap.isEmpty()) {
        return; // nothing to do
      }
      agentIds = new HashSet(incarnationMap.keySet());
    }
    // update the latest incarnations from the white pages
    for (Iterator iter = agentIds.iterator();
        iter.hasNext();
        ) {
      MessageAddress agentId = (MessageAddress) iter.next();
      long currentInc = lookupIncarnation(agentId);
      updateIncarnation(agentId, currentInc);
    }
  }

  /**
   * White pages lookup to get the latest incarnation number for
   * the specified agent.
   *
   * @return -1 if the WP lacks an entry, -2 if a WP background
   * lookup is in progress, or &gt; 0 for a valid incarnation
   */
  private long lookupIncarnation(MessageAddress agentId) {
    AddressEntry entry;

    // runs in the pollThread, so no locking required
    BlockingWPCallback callback = (BlockingWPCallback)
      pendingMap.get(agentId);
    if (callback == null) {
      // no pending callback yet.
      callback = new BlockingWPCallback();
      wps.get(agentId.getAddress(), "version", callback);
      if (callback.completed) {
        // cache hit
        entry = callback.entry;
      } else {
        // no cache hit.  Remember the callback.
        pendingMap.put(agentId, callback);
        return -2;
      }
    } else if (callback.completed) {
      // pending callback completed
      entry = callback.entry;
      pendingMap.remove(agentId);
    } else {
      // pending callback not completed yet
      return -2;
    }

    if (entry == null) {
      // log this?
      // return error code
      return -1;
    }

    // parse the entry
    String path = entry.getURI().getPath();
    int end = path.indexOf('/', 1);
    String incn_str = path.substring(1, end);
    return Long.parseLong(incn_str);
  }

  /** an incarnationMap entry */
  private static final class Entry {

    // comparator that puts non-comparables first
    private static final Comparator CALLBACK_COMP =
      new Comparator() {
        public int compare(Object o1, Object o2) {
          if (o1 instanceof Comparable) {
            if (o2 instanceof Comparable) {
              return ((Comparable) o1).compareTo(o2);
            } else {
              return 1;
            }
          } else if (o2 instanceof Comparable) {
            return -1;
          } else {
            return 0;
          }
        }
      };

    // cached incarnation.
    private long inc;

    // our callbacks
    private Set callbacks;

    public long getIncarnation() {
      return inc;
    }

    public void setIncarnation(long currentInc) {
      inc = currentInc;
    }

    public boolean hasCallbacks() {
      return (callbacks != null && !callbacks.isEmpty());
    }

    public boolean addCallback(IncarnationService.Callback cb) {
      if (callbacks == null) {
        // use a regular IdentityHashSet and sort on "update",
        // instead of using a more expensive TreeSet.
        callbacks = new IdentityHashSet();
      }
      return callbacks.add(cb);
    }

    public boolean removeCallback(IncarnationService.Callback cb) {
      if (callbacks == null) {
        return false;
      }
      return callbacks.remove(cb);
    }

    public List getCallbacks() {
      if (callbacks.isEmpty()) {
        return null;
      }
      List ret = new ArrayList(callbacks);
      Collections.sort(ret, CALLBACK_COMP);
      return ret;
    }

    @Override
   public String toString() {
      return
        "(entry inc="+inc+" callbacks["+
        (callbacks == null ? 0 : callbacks.size())+
        "]="+callbacks+")";
    }
  }

  private final class IncarnationSP
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (IncarnationService.class.isAssignableFrom(serviceClass)) {
          return new Impl();
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
        if (!(service instanceof Impl)) {
          return;
        }
        ((Impl) service).unsubscribeAll();
      }

      private class Impl implements IncarnationService {
        // map from agentId to callbacks
        private final Map subs = new HashMap();

        public long getIncarnation(MessageAddress addr) {
          MessageAddress agentId = addr.getPrimary();
          return Incarnation.this.getIncarnation(agentId);
        }

        public int updateIncarnation(MessageAddress addr, long inc) {
          MessageAddress agentId = addr.getPrimary();
          return Incarnation.this.updateIncarnation(agentId, inc);
        }

        public Map getSubscriptions() {
          synchronized (subs) {
            return
              (subs.isEmpty() ?
               Collections.EMPTY_MAP :
               (new HashMap(subs)));
          }
        }

        public boolean subscribe(MessageAddress addr, Callback cb) {
          return subscribe(addr, cb, 0);
        }

        public boolean subscribe(
            MessageAddress addr,
            Callback cb,
            long initialInc) {
          MessageAddress agentId = addr.getPrimary();
          synchronized (subs) {
            Set s = (Set) subs.get(agentId);
            if (s == null) {
              s = new IdentityHashSet();
              subs.put(agentId, s);
            }
            if (!s.add(cb)) {
              // already have this subscription
              return false;
            }
            return Incarnation.this.subscribe(agentId, cb, initialInc);
          }
        }

        public boolean unsubscribe(MessageAddress addr, Callback cb) {
          MessageAddress agentId = addr.getPrimary();
          synchronized (subs) {
            Set s = (Set) subs.get(agentId);
            if (s == null) {
              return false;
            }
            if (!s.remove(cb)) {
              return false;
            }
            return Incarnation.this.unsubscribe(agentId, cb);
          }
        }

        private void unsubscribeAll() {
          synchronized (subs) {
            for (Iterator iter = subs.entrySet().iterator();
                iter.hasNext();
                ) {
              Map.Entry me = (Map.Entry) iter.next();
              MessageAddress agentId = (MessageAddress) me.getKey();
              Set s = (Set) me.getValue();
              for (Iterator i2 = s.iterator();
                  i2.hasNext();
                  ) {
                Callback cb = (Callback) i2.next();
                Incarnation.this.unsubscribe(agentId, cb);
              }
            }
            subs.clear();
          }
        }
      }
    }

  // replace with a Latch?
  private class BlockingWPCallback implements Callback {
    AddressEntry entry;
    boolean completed = false;

    public void execute(Response response) {
      completed = true;
      if (response.isSuccess()) {
        if (log.isDetailEnabled()) {
          log.detail("wp response: "+response);
        }
        entry = ((Response.Get) response).getAddressEntry();
      } else {
        if (log.isDetailEnabled()) {
          log.detail("wp error: "+response);
        }
      }
    }
  }

}
