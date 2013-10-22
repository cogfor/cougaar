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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.Parameters;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a base class for (client) bootstrap discoverers.
 * <p>
 * Per-protocol subclasses call {@link #addPoller} to create
 * an inner Poller class per bootstrap location (e.g. a
 * poller for URL http://foo.com:123 and http://bar.com:456).
 * These pollers are told to start/stop according to the
 * DiscoveryService, which is controlled by the BootstrapService.
 * Pollers are also periodically told to "lookup", to find
 * bundles at the remote locations.
 */
public abstract class DiscoveryBase
extends GenericStateModelAdapter
implements Component
{
  protected Config config;

  protected ServiceBroker sb;

  protected LoggingService log;
  protected MessageAddress agentId;
  protected ThreadService threadService;

  private DiscoveryService discoveryService;

  protected String agentName;

  // Map<Object, Poller>
  protected final Map table = new HashMap();

  public void setParameter(Object o) {
    configure(o);
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  protected String getConfigPrefix() {
    return "org.cougaar.core.wp.bootstrap.discovery.";
  }

  protected void configure(Object o) {
    if (config == null) {
      config = new Config(o, getConfigPrefix());
    }
  }

  @Override
public void load() {
    super.load();

    configure(null);

    // which agent are we in?
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);
    agentName = agentId.getAddress();

    // register our discovery client
    discoveryService = sb.getService(
       this, DiscoveryService.class, null);
    if (discoveryService == null) {
      throw new RuntimeException(
          "Unable to obtain DiscoveryService");
    }
  }

  @Override
public void unload() {
    super.unload();

    if (discoveryService != null) {
      sb.releaseService(
          this, DiscoveryService.class, discoveryService);
      discoveryService = null;
    }

    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (log != null) {
      sb.releaseService(
          this, LoggingService.class, log);
      log = null;
    }
  }

  /**
   * Returns all bundles found at the bootstrap location.
   */
  protected abstract Map lookup(Object bootObj);

  /**
   * Create a poller for a bootstrap location.
   * <p>
   * This method can be overridden to use a custom Poller class. 
   */
  protected Poller createPoller(Object bootObj) {
    return new Poller(bootObj);
  }

  /**
   * Get the delay for the initial lookup, where subsequent failed
   * lookups double the delay until it reaches the
   * {@link #getMaxDelay}.
   */
  protected long getMinDelay() {
    return config.minDelay;
  }
  protected long getMaxDelay() {
    return config.maxDelay;
  }

  protected Object getKey(Object bootObj) {
    return bootObj;
  }

  // this is called by the subclass
  protected void addPoller(Object bootObj) {
    Object key = getKey(bootObj);
    if (key == null) {
      return;
    }
    synchronized (table) {
      Poller p = (Poller) table.get(key);
      if (p != null) {
        p.destroyLater();
      }
      p = createPoller(bootObj);
      p.initialize();
      table.put(key, p);
    }
  }

  // this is called by the subclass
  protected void removePoller(Object bootObj) {
    Object key = getKey(bootObj);
    if (key == null) {
      return;
    }
    synchronized (table) {
      Poller p = (Poller) table.remove(key);
      if (p == null) {
        return;
      }
      p.destroyLater();
    }
  }

  /**
   * This manages the lookup and verification for a single bootEntry.
   */
  protected class Poller implements Runnable {

    protected static final int LOOKUP = 1;
    protected static final int CANCEL = 2;

    //
    // construction-time finals:
    //

    /**
     * Our symbolic bootstrapped entry, e.g.<pre>
     *   (WP, -RMI_REG, rmi://foo.com:123/AgentX)
     * </pre>
     * where the name is "WP" and the id is "AgentX"
     */
    protected final Object bootObj;

    /** our thread */
    protected Schedulable thread;

    /**
     * Each poller gets a separate discovery service, since
     * the "update" map is per-service-instance.
     */
    protected final DiscoveryService.Client dsc =
      new DiscoveryService.Client() {
        public void startSearching() {
          startLater();
        }
        public void stopSearching() {
          stopLater();
        }
      };
    protected DiscoveryService ds;

    private final List queue = new ArrayList();
    private final List tmp = new ArrayList();

    protected boolean active;
    protected long delay;
    protected long wakeTime;

    public Poller(Object bootObj) {
      this.bootObj = bootObj;
    }

    protected void initialize() {
      thread = threadService.getThread(
          DiscoveryBase.this, 
          this, 
          "White pages bootstrap discovery for "+bootObj,
          ThreadService.WILL_BLOCK_LANE);

      ds = sb.getService(
            dsc, DiscoveryService.class, null);
      if (ds == null) {
        throw new RuntimeException(
            "Unable to obtain DiscoveryService");
      }
    }

    protected void start() {
    }

    protected void stop() {
    }

    protected void destroy() {
    }

    /**
     * Lookup the Map of Bundles at the bootstrap address.
     */
    protected Map lookup() {
      return DiscoveryBase.this.lookup(bootObj);
    }

    protected void doLookup() {
      Map found;
      try {
        found = lookup();
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Lookup "+bootObj+" failed", e);
        }
        // try again later
        return;
      }

      if (log.isInfoEnabled()) {
        log.info("Lookup "+bootObj+" found "+found);
      }

      ds.update(found);
    }

    protected long getNextWakeTime(long now) {
      long min = getMinDelay();
      if (delay < min) {
        delay = min;
      } else {
        delay <<= 1;
        long max = getMaxDelay();
        if (delay > max) {
          delay = max;
        }
      }
      return now + delay;
    }


    // queuing -- we don't want to block the calling thread!

    public void startLater() {
      enqueue(Start.getInstance());
    }
    public void stopLater() {
      enqueue(Stop.getInstance());
    }
    public void destroyLater() {
      enqueue(Destroy.getInstance());
    }
    private void enqueue(Object o) {
      synchronized (queue) {
        queue.add(o);
      }
      thread.start();
    }
    public void run() {
      // process queue
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
        } else if (o instanceof Destroy) {
          destroyNow();
        } else if (log.isErrorEnabled()) {
          log.error("Invalid queue element: "+o);
        }
      }
      tmp.clear();

      // process wake timer
      maybeWakeNow();
    }
    private void startNow() {
      if (active) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Starting "+bootObj);
      }
      active = true;
      delay = 0;
      wakeTime = 0;
      start();
    }
    private void stopNow() {
      if (!active) {
        return;
      }
      if (log.isInfoEnabled()) {
        log.info("Stopping "+bootObj);
      }
      active = false;
      delay = 0;
      wakeTime = 0;
      thread.cancelTimer();
      stop();
    }
    private void destroyNow() {
      if (active) {
        stopNow();
      }
      if (log.isInfoEnabled()) {
        log.info("Destroying "+bootObj);
      }
      if (ds != null) {
        sb.releaseService(
            dsc, DiscoveryService.class, ds);
        ds = null;
      }
      destroy();
    }
    private void maybeWakeNow() {
      // process wake timer
      if (!active) {
        if (log.isDetailEnabled()) {
          log.detail("not waking now, not active");
        }
        return;
      }
      long now = System.currentTimeMillis();
      if (wakeTime < 0 || wakeTime > now) {
        if (log.isDetailEnabled()) {
          log.detail("now("+now+") is not wakeTime("+wakeTime+")");
        }
        return; 
      }
      doLookup();
      wakeLater(now); 
    }

    private void wakeLater(long now) {
      wakeTime = getNextWakeTime(now);
      if (log.isDetailEnabled()) {
        log.detail("will wake in "+delay+" millis, at "+wakeTime);
      }
      thread.schedule(delay);
    }
  }

  //
  // queue entries for our poller
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
  private static class Destroy implements QueueElement {
    private static final Destroy INSTANCE = new Destroy();
    public static Destroy getInstance() { return INSTANCE; }
  }

  /** config options */
  protected static class Config {
    public final long minDelay;
    public final long maxDelay;

    public Config(Object o, String prefix) {
      Parameters p = new Parameters(o, prefix);
      minDelay = p.getLong("minLookup", 8000);
      maxDelay = p.getLong("maxLookup", 120000);
      if (minDelay <= 0 || minDelay > maxDelay) {
        throw new RuntimeException(
            "Invalid delay (min="+minDelay+", max="+maxDelay+")");
      }
    }
  }
}
