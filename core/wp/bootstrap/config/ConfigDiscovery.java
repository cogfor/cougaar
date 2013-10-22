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

package org.cougaar.core.wp.bootstrap.config;

import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.DiscoveryService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component copies bundles found in the {@link ConfigService}
 * to the {@link DiscoveryService}.
 * <p>
 * This is the simple static bootstrap.
 */
public class ConfigDiscovery
extends GenericStateModelAdapter
implements Component, DiscoveryService.Client
{
  private ServiceBroker sb;

  private LoggingService log;
  private ThreadService threadService;
  private Schedulable thread;

  private DiscoveryService discoveryService;

  private ConfigService configService;

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

    configService = sb.getService(this, ConfigService.class, null);
    if (configService == null) {
      throw new RuntimeException("Unable to obtain ConfigService");
    }

    discoveryService = sb.getService(this, DiscoveryService.class, null);
    if (discoveryService == null) {
      throw new RuntimeException("Unable to obtain DiscoveryService");
    }
  }

  @Override
public void unload() {
    if (discoveryService != null) {
      sb.releaseService(this, DiscoveryService.class, discoveryService);
      discoveryService = null;
    }
    if (configService != null) {
      sb.releaseService(this, ConfigService.class, configService);
      configService = null;
    }
    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }

    super.unload();
  }

  public void startSearching() {
    final Map m = configService.getBundles();
    if (log.isDetailEnabled()) {
      log.detail("startSearching, ds.update("+m+")");
    }
    if (m == null || m.isEmpty()) {
      return;
    }

    discoveryService.update(m);

    // wake up before the earliest TTD, to make sure our entries stay alive in
    // the cache.  This can happen if the server is temporarily unreachable
    // for a relatively long time.
    boolean set_min_ttd = false;
    long min_ttd = -1;
    for (Iterator iter = m.values().iterator(); iter.hasNext(); ) {
      long ttd = ((Bundle) iter.next()).getTTD();
      if (ttd > 0 && (!set_min_ttd || ttd < min_ttd)) {
        set_min_ttd = true;
        min_ttd = ttd;
      }
    }
    if (set_min_ttd) {
      Runnable r = new Runnable() {
        public void run() {
          // TODO if we have a range of TTDs then we should only update the
          // map subset that will expire at this time, and set our timer to
          // wake us accordingly.  However, that's overkill -- we'll simply
          // recache all our bundles.
          if (log.isDetailEnabled()) {
            log.detail("renew config bootstrap");
          }
          discoveryService.update(m);
        }
      };
      thread = threadService.getThread(this, r, "WP config bootstrap");
      long delay = Math.max(1000, (long) (0.9 * min_ttd));
      thread.scheduleAtFixedRate(delay, delay);
    }
  }

  public void stopSearching() {
    if (log.isDetailEnabled()) {
      log.detail("stopSearching");
    }
    if (thread != null) {
      thread.cancelTimer();
      thread = null;
    }
  }
}
