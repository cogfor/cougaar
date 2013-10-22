/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.RarelyModifiedList;

/**
 * This component advertises the {@link BootstrapService}, which
 * starts/stops the discovery pollers to find bootstrap {@link
 * Bundle}s and copy them into the {@link
 * org.cougaar.core.wp.resolver.CacheManager}'s {@link HintService}.
 */
public class DiscoveryManager
extends GenericStateModelAdapter
implements Component
{

  private static final String ADD = "add";
  private static final String CHANGE = "change";
  private static final String REMOVE = "remove";
  private static final String SAME = "same";
  private static final String[] ACTIONS = new String[] {
    ADD, CHANGE, REMOVE, SAME };

  private ServiceBroker sb;

  private LoggingService log;

  private HintService hintService;

  private BootstrapSP bootstrapSP;

  private ServersSP serversSP;

  private DiscoverySP discoverySP;

  private final Object lock = new Object();

  private Set servers = Collections.EMPTY_SET;

  private final RarelyModifiedList server_listeners =
    new RarelyModifiedList();

  private int count;

  private final List discoverers = new ArrayList();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  @Override
public void load() {
    super.load();

    hintService = sb.getService(this, HintService.class, null);
    if (hintService == null) {
      throw new RuntimeException("Unable to obtain HintService");
    }

    // advertise our service
    bootstrapSP = new BootstrapSP();
    sb.addService(BootstrapService.class, bootstrapSP);
    serversSP = new ServersSP();
    sb.addService(ServersService.class, serversSP);
    discoverySP = new DiscoverySP();
    sb.addService(DiscoveryService.class, discoverySP);
  }

  @Override
public void unload() {
    if (discoverySP != null) {
      sb.revokeService(DiscoveryService.class, discoverySP);
      discoverySP = null;
    }
    if (serversSP != null) {
      sb.revokeService(ServersService.class, serversSP);
      serversSP = null;
    }
    if (bootstrapSP != null) {
      sb.revokeService(BootstrapService.class, bootstrapSP);
      bootstrapSP = null;
    }

    super.unload();
  }

  private void register(DiscoveryService.Client dsc) {
    synchronized (lock) {
      if (log.isDetailEnabled()) {
        log.detail(
            "register("+dsc+"), count="+count+
            ", discoverers["+discoverers.size()+"]="+
            discoverers);
      }
      discoverers.add(dsc);

      if (count <= 0) {
        return;
      }
      dsc.startSearching();
    }
  }

  private void unregister(DiscoveryService.Client dsc) {
    synchronized (lock) {
      if (log.isDetailEnabled()) {
        log.detail(
            "unregister("+dsc+"), count="+count+
            ", discoverers["+discoverers.size()+"]="+discoverers);
      }
      if (count > 0) {
        dsc.stopSearching();
      }

      discoverers.remove(dsc);
    }
  }


  private void startSearching() {
    synchronized (lock) {
      if (log.isDetailEnabled()) {
        log.detail(
            "startSearching(), count="+count+
            ", discoverers["+discoverers.size()+"]="+discoverers);
      }
      if (++count > 1) {
        return;
      }
      int n = discoverers.size();
      for (int i = 0; i < n; i++) {
        DiscoveryService.Client dsc = (DiscoveryService.Client)
          discoverers.get(i);
        dsc.startSearching();
      }
    }
  }

  private void stopSearching() {
    synchronized (lock) {
      if (log.isDetailEnabled()) {
        log.detail(
            "stopSearching(), count="+count+
            ", discoverers["+discoverers.size()+"]="+discoverers);
      }
      if (--count > 0) {
        return;
      }
      int n = discoverers.size();
      for (int i = 0; i < n; i++) {
        DiscoveryService.Client dsc = (DiscoveryService.Client)
          discoverers.get(i);
        dsc.stopSearching();
      }
    }
  }

  private void register(ServersService.Client client) {
    if (log.isDetailEnabled()) {
      log.detail(
          "register("+client+"), server_listeners["+
          server_listeners.size()+"]="+server_listeners);
    }
    server_listeners.add(client);
  }
  private void unregister(ServersService.Client client) {
    if (log.isDetailEnabled()) {
      log.detail(
          "unregister("+client+"), server_listeners["+
          server_listeners.size()+"]="+server_listeners);
    }
    server_listeners.remove(client);
  }

  private void add(String name, Bundle bundle) {
    update(ADD, name, bundle);
  }
  private void change(String name, Bundle bundle) {
    update(CHANGE, name, bundle);
  }
  private void remove(String name, Bundle bundle) {
    update(REMOVE, name, bundle);
  }

  private void update(Map oldMap, Map newMap) {
    if (log.isDetailEnabled()) {
      log.detail("update("+oldMap+", "+newMap+")");
    }

    // TODO what if we search longer than the bundle ttls?
    // The cache would expire these entries and we'd never renew them
    // in the hintService.  So maybe we should re-hint unchanged
    // bundles to keep the cache alive.
    //
    // For now we can probably safely assume that if the
    // bundles are useful they'll stop the search well before
    // they expire. 

    Map diff = computeDiff(oldMap, newMap);

    if (log.isDebugEnabled()) {
      log.debug(
          "update("+
          "\n  oldMap="+oldMap+
          "\n  newMap="+newMap+
          "\n) diff is {\n"+
          diff+
          "\n}");
    }

    for (int i = 0; i < ACTIONS.length; i++) {
      String action = ACTIONS[i];
      Map m = (Map) diff.get(action);
      if (m == null || m.isEmpty()) {
        continue;
      }
      for (Iterator iter = m.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Bundle bundle = (Bundle) me.getValue();
        update(action, name, bundle);
      }
    }
  }

  private void update(String action, String name, Bundle bundle) {
    if (log.isDetailEnabled()) {
      log.detail(
          "update("+action+", "+name+", "+bundle+")");
    }

    // treat ADD/CHANGE/SAME as add
    // we use SAME to keep the cache alive
    boolean add = (action != REMOVE);

    // update cache
    if (add) {
      hintService.add(name, bundle);
    } else { 
      hintService.remove(name, bundle);
    }

    // update servers list
    MessageAddress addr = Util.parseServer(bundle); 
    if (log.isDetailEnabled()) {
      log.detail("parseServer("+bundle+")="+addr);
    }
    if (addr == null) {
      return;
    }
    // assert addr == addr.getPrimary();
    synchronized (lock) {
      if (add == servers.contains(addr)) {
        // no change
        if (log.isDetailEnabled()) {
          log.detail("add("+add+") == servers.contains("+addr+")");
        }
        return;
      }
      // copy-on-write
      Set ns = new HashSet(servers);
      if (add) {
        ns.add(addr);
      } else {
        ns.remove(addr);
      }
      servers = Collections.unmodifiableSet(ns);
      if (log.isDetailEnabled()) {
        log.detail("servers["+servers.size()+"]="+servers);
      }
    }
    // tell clients
    List l = server_listeners.getUnmodifiableList();
    if (log.isDetailEnabled()) {
      log.detail("tell server_listeners["+server_listeners.size()+"]");
    }
    for (int i = 0, ln = l.size(); i < ln; i++) {
      ServersService.Client c =
        (ServersService.Client) l.get(i);
      if (log.isDetailEnabled()) {
        log.detail(
            "  client["+i+"]=("+c+")."+
            (add?"add":"remove")+"("+addr+")");
      }
      if (add) {
        c.add(addr);
      } else {
        c.remove(addr);
      }
    }
  }
  
  private static Map computeDiff(Map oldMap, Map newMap) {
    if (newMap.equals(oldMap)) {
      // same
      return Collections.singletonMap(SAME, oldMap); 
    } 

    // calculate diff
    Map added = null; 
    Map changed = null; 
    Map removed = null; 
    Map same = null; 
    for (Iterator iter = newMap.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Bundle newBundle = (Bundle) me.getValue();
      if (name == null || newBundle == null) {
        // invalid
        continue;
      }
      Bundle oldBundle = (Bundle) oldMap.get(name);
      if (oldBundle == null) {
        if (added == null) {
          added = new HashMap();
        }
        // added
        added.put(name, newBundle);
      } else if (oldBundle.equals(newBundle)) {
        if (same == null) {
          same = new HashMap();
        } 
        // same
        same.put(name, newBundle);
      } else {
        if (changed == null) {
          changed = new HashMap();
        } 
        // changed
        changed.put(name, newBundle);
      }
    }
    // removed
    if (newMap.isEmpty()) {
      removed = oldMap;
    } else { 
      removed = new HashMap(oldMap);
      removed.keySet().removeAll(newMap.keySet());
    }

    Map ret = new HashMap(4);
    ret.put(ADD, added);
    ret.put(CHANGE, changed);
    ret.put(REMOVE, removed);
    ret.put(SAME, same);
    return ret;
  }

  private Set getServers() {
    synchronized (lock) {
      return servers;
    }
  }
  
  private class BootstrapSP implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!BootstrapService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      return new BootstrapServiceImpl();
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
      if (!(service instanceof BootstrapServiceImpl)) {
        return;
      }
      BootstrapServiceImpl bsi = (BootstrapServiceImpl) service;
      bsi.stopSearching();
    }
    private class BootstrapServiceImpl implements BootstrapService {
      private final Object lock = new Object();
      private boolean running;
      public void startSearching() {
        synchronized (lock) {
          if (log.isDetailEnabled()) {
            log.detail("bsi.startSearching(), running="+running);
          }
          if (running) {
            return;
          }
          running = true;
          DiscoveryManager.this.startSearching();
        }
      }
      public void stopSearching() {
        synchronized (lock) {
          if (log.isDetailEnabled()) {
            log.detail("bsi.stopSearching(), running="+running);
          }
          if (!running) {
            return;
          }
          running = false;
          DiscoveryManager.this.stopSearching();
        }
      }
    }
  }

  private class ServersSP implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!ServersService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      ServersService.Client client =
        (requestor instanceof ServersService.Client ?
         (ServersService.Client) requestor :
         null);
      ServersServiceImpl ssi = new ServersServiceImpl(client);
      if (client != null) {
        DiscoveryManager.this.register(client);
      }
      return ssi;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
      ServersServiceImpl ssi = (ServersServiceImpl) service;
      ServersService.Client client = ssi.client;
      if (client != null) {
        DiscoveryManager.this.unregister(client);
      }
    }
    private class ServersServiceImpl
      implements ServersService {
        protected final Client client;
        public ServersServiceImpl(Client client) {
          this.client = client;
        }
        public Set getServers() {
          return DiscoveryManager.this.getServers();
        }
      }
  }

  private class DiscoverySP implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!DiscoveryService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      DiscoveryService.Client client =
        (requestor instanceof DiscoveryService.Client ?
         (DiscoveryService.Client) requestor :
         null);
      DiscoveryServiceImpl dsi = new DiscoveryServiceImpl(client);
      if (client != null) {
        DiscoveryManager.this.register(client);
      }
      return dsi;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
      if (!(service instanceof DiscoveryServiceImpl)) {
        return;
      }
      DiscoveryServiceImpl dsi = (DiscoveryServiceImpl) service;
      DiscoveryService.Client client = dsi.client;
      if (client != null) {
        DiscoveryManager.this.unregister(client);
      }
    }
    private class DiscoveryServiceImpl implements DiscoveryService {
      protected final Client client;
      private final Map m = new HashMap(3);
      public DiscoveryServiceImpl(Client client) {
        this.client = client;
      }
      public void update(Map nm) {
        Map newM = (nm == null ? Collections.EMPTY_MAP : nm);
        synchronized (m) {
          if (log.isDetailEnabled()) {
            log.detail(
                "dsi("+client+").update("+newM+") from ("+m+")");
          }
          DiscoveryManager.this.update(m, newM);
          m.clear();
          m.putAll(newM);
        }
      }
      public void add(String name, Bundle bundle) {
        synchronized (m) {
          if (log.isDetailEnabled()) {
            log.detail(
                "dsi("+client+").add("+name+", "+bundle+
                ") to ("+m+")");
          }
          DiscoveryManager.this.add(name, bundle);
          m.put(name, bundle);
        }
      }
      public void change(String name, Bundle bundle) {
        synchronized (m) {
          if (log.isDetailEnabled()) {
            log.detail(
                "dsi("+client+").change("+name+", "+bundle+
                ") to ("+m+")");
          }
          DiscoveryManager.this.change(name, bundle);
          m.put(name, bundle);
        }
      }
      public void remove(String name, Bundle bundle) {
        synchronized (m) {
          if (log.isDetailEnabled()) {
            log.detail(
                "dsi("+client+").remove("+name+", "+bundle+
                ") from ("+m+")");
          }
          DiscoveryManager.this.remove(name, bundle);
          m.remove(name);
        }
      }
    }
  }
}
