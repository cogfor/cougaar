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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a base class for (server) bootstrap advertisers.
 * <p>
 * Per-protocol subclasses call {@link #addAdvertiser} to create
 * an inner Advertiser class per bootstrap location (e.g. an
 * advertiser for URL http://foo.com:123 and http://bar.com:456).
 * These advertisers are told to start/stop according to the
 * AdvertiseService.  Advertisers are also told when the local
 * node's bundles change, in case this data must be posted to
 * a remote location.
 */
public abstract class AdvertiseBase
extends GenericStateModelAdapter
implements Component
{
  protected ServiceBroker sb;

  protected LoggingService log;
  protected MessageAddress agentId;
  protected ThreadService threadService;

  private AdvertiseService advertiseService;

  protected String agentName;

  private final Object lock = new Object();

  // Map<String, Bundle>
  protected Map bundles = Collections.EMPTY_MAP;

  // Map<String, Advertiser>
  protected final Map advertisers = new HashMap();

  private final AdvertiseService.Client advertiseClient = 
    new AdvertiseService.Client() {
      public void add(String name, Bundle bundle) {
        AdvertiseBase.this.add(name, bundle);
      }
      public void change(String name, Bundle bundle) {
        AdvertiseBase.this.change(name, bundle);
      }
      public void remove(String name, Bundle bundle) {
        AdvertiseBase.this.remove(name);
      }
    };

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  @Override
public void load() {
    super.load();

    // which agent are we in?
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);
    agentName = agentId.getAddress();

    // register our advertise client
    advertiseService = sb.getService(
       advertiseClient, AdvertiseService.class, null);
    if (advertiseService == null) {
      throw new RuntimeException(
          "Unable to obtain AdvertiseService");
    }
  }

  @Override
public void unload() {
    removeAllAdvertisers(); 

    if (advertiseService != null) {
      sb.releaseService(
          advertiseClient, AdvertiseService.class, advertiseService);
      advertiseService = null;
    }

    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (log != null && log != LoggingService.NULL) {
      sb.releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }

    super.unload();
  }

  /**
   * Create a poller for a bootstrap location.
   * <p>
   * This method can be overridden to use a custom Advertiser class. 
   */
  protected abstract Advertiser createAdvertiser(Object bootObj);

  /**
   * Get the current bundles.
   */
  protected Map getBundles() {
    synchronized (lock) {
      return bundles;
    }
  }

  private void add(String name, Bundle bundle) {
    update(name, bundle);
  }
  private void change(String name, Bundle bundle) {
    update(name, bundle);
  }
  private void remove(String name) {
    update(name, null);
  }

  protected void update(String name, Bundle bundle) {
    synchronized (lock) {
      // update bundles
      Bundle b = (Bundle) bundles.get(name);
      if (bundle == null ? b == null : bundle.equals(b)) {
        // no change?
        return;
      }
      // copy-on-write
      Map nb = new HashMap(bundles);
      if (bundle == null) {
        nb.remove(name);
      } else {
        nb.put(name, bundle);
      }
      bundles = Collections.unmodifiableMap(nb);
      if (advertisers.isEmpty()) {
        return;
      }
      if (log.isDetailEnabled()) {
        log.detail("updating name="+name+" bundle="+bundle);
      }
      // tell our advertisers
      for (Iterator iter = advertisers.values().iterator();
          iter.hasNext();
          ) {
        Advertiser a = (Advertiser) iter.next();
        a.updateLater(name, bundle);
      }
    }
  }

  protected Object getKey(Object bootObj) {
    return bootObj;
  }

  // this is called by the subclass
  protected void addAdvertiser(Object bootObj) {
    Advertiser a;
    Object key = getKey(bootObj);
    if (key == null) {
      return;
    }
    synchronized (lock) {
      a = (Advertiser) advertisers.get(key);
      if (a != null) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Creating "+bootObj);
      }
      a = createAdvertiser(bootObj);
      advertisers.put(key, a);
    }
    a.startLater();
  }

  protected void removeAllAdvertisers() {
    List l;
    synchronized (lock) {
      if (advertisers.isEmpty()) {
        return;
      }
      l = new ArrayList(advertisers.values());
      advertisers.clear();
      if (log.isInfoEnabled()) {
        for (int i = 0; i < l.size(); i++) {
          log.info("Removing "+l.get(i));
        }
      }
    }
    for (int i = 0; i < l.size(); i++) {
      Advertiser a = (Advertiser) l.get(i);
      if (a != null) {
        a.stopLater();
      }
    }
  }

  // this is called by the subclass
  protected void removeAdvertiser(Object bootObj) {
    Advertiser a;
    Object key = getKey(bootObj);
    if (key == null) {
      return;
    }
    synchronized (lock) {
      a = (Advertiser) advertisers.remove(key);
      if (a == null) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Removing "+bootObj);
      }
    }
    a.stopLater();
  }

  /**
   * This manages the lookup and verification for a single bootObj.
   */
  protected abstract class Advertiser implements Runnable {

    //
    // construction-time finals:
    //

    private final Schedulable thread;

    protected final Object bootObj;

    private final List queue = new ArrayList();
    private final List tmp = new ArrayList();

    private boolean active;

    public Advertiser(Object bootObj) {
      this.bootObj = bootObj;

      this.thread = threadService.getThread(
          AdvertiseBase.this, 
          this, 
          "White pages bootstrap advertiser for "+bootObj,
          ThreadService.WILL_BLOCK_LANE);
    }

    /**
     *
     */
    public abstract void start();

    /**
     *
     */ 
    public abstract void update(String name, Bundle bundle);

    /**
     *
     */ 
    public abstract void stop();

    // queuing -- we don't want to block the calling thread!

    public void startLater() {
      enqueue(Start.getInstance());
    }
    public void stopLater() {
      enqueue(Stop.getInstance());
    }
    public void updateLater(String name, Bundle bundle) {
      enqueue(new Update(name, bundle));
    }
    private void enqueue(Object o) {
      synchronized (queue) {
        queue.add(o);
      }
      thread.start();
    }
    public void run() {
      int n;
      synchronized (queue) {
        n = queue.size();
        if (n > 0) {
          tmp.addAll(queue);
          queue.clear();
        }
      }
      for (int i = 0; i < n; i++) {
        Object o = tmp.get(i);
        if (o instanceof Start) {
          startNow();
        } else if (o instanceof Stop) {
          stopNow();
        } else if (o instanceof Update) {
          Update u = (Update) o;
          updateNow(u.getName(), u.getBundle());
        } else if (log.isErrorEnabled()) {
          log.error("Invalid queue element: "+o);
        }
      }
      tmp.clear();
    }
    private void startNow() {
      if (active) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Starting "+bootObj);
      }
      start();
      active = true;
    }
    private void stopNow() {
      if (!active) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Stopping "+bootObj);
      }
      stop();
      active = false;
    }
    private void updateNow(String name, Bundle bundle) {
      if (log.isInfoEnabled()) {
        log.info(
            "Updating "+bootObj+" with name="+name+
            " bundle="+bundle);
      }
      update(name, bundle);
    }
  }

  //
  // queue entries for our advertiser
  //

  private interface QueueElement {
  } 
  private static class Start implements QueueElement {
    private static final Start INSTANCE = new Start();
    public static Start getInstance() { return INSTANCE; }
  }
  private static class Stop implements QueueElement {
    private static final Stop INSTANCE = new Stop();
    public static Stop getInstance() { return INSTANCE; }
  }
  private static class Update implements QueueElement {
    private final String name;
    private final Bundle bundle;
    public Update(String name, Bundle bundle) {
      this.name = name;
      this.bundle = bundle;
    }
    public String getName() { return name; }
    public Bundle getBundle() { return bundle; }
    @Override
   public String toString() {
      return "(update name="+name+" bundle="+bundle+")";
    }
  }
}
