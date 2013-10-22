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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.SuicideService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This component advertises the {@link SuicideService}.
 */
public final class SuicideServiceComponent
extends GenericStateModelAdapter implements Component {

  private static final Logger log = Logging.getLogger(SuicideService.class);

  private ServiceBroker rootsb;

  private ServiceProvider ssp;
  private SuicideService ssi;
  private static SuicideService singleton;

  private SuicideMonitor monitor;

  public void setNodeControlService(NodeControlService ncs) { 
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  @Override
public void load() {
    super.load();
    ssp = new SuicideServiceProvider();
    ssi = new SuicideServiceImpl();
    setSingleton(ssi);
    rootsb.addService(SuicideService.class, ssp);
    if (log.isDebugEnabled()) { log.error("SuicideService loaded"); }
  }

  @Override
public void start() {
    super.start();
    if (SuicideService.isSuicideEnabled) {
      if (SuicideService.isProactiveEnabled) {
        monitor = new SuicideMonitor();
        monitor.start();
      }
    }
  }
  @Override
public void stop() {
    if (SuicideService.isSuicideEnabled) {
      if (SuicideService.isProactiveEnabled) {
        monitor.stop();
        monitor = null;
      }
    }
    super.stop();
  }
  @Override
public void unload() {
    rootsb.revokeService(SuicideService.class, ssp);
    ssp = null;
    ssi = null;
    super.unload();
  }

  private class SuicideServiceProvider implements ServiceProvider {
    public Object getService(ServiceBroker xsb, Object requestor, Class serviceClass) {
      if (serviceClass == SuicideService.class) {
        return ssi;
      }
      return null;
    }
    public void releaseService(ServiceBroker xsb, Object requestor, Class serviceClass, Object service) {}
  }

  private class SuicideServiceImpl implements SuicideService {
    public void die(Object component, Throwable error) {
      if (error == null) throw new IllegalArgumentException("May not Suicide without an Error");
      try {
        String s = (component==null)?"anonymous":(component.toString());
        log.fatal("Suicide from "+s, error);
	StateDumpService sds = rootsb.getService(this, StateDumpService.class, null);
	if (sds != null) sds.dumpState();
      } finally {
        if (SuicideService.isSuicideEnabled) {
          System.exit(SuicideService.EXIT_CODE);
        }
      }
    }
  }

  
  public static synchronized SuicideService getSuicideService() { return singleton; }
  private static synchronized void setSingleton(SuicideService ss) {
    if (singleton != null) {
      log.error("Multiple SuicideService instances loaded into VM", new Throwable());
    } else {
      singleton = ss;
    }
  }

  private class SuicideMonitor implements Runnable {
    Thread t;
    Runtime runtime;
    long thresh;
    long period;

    SuicideMonitor() {
      runtime = Runtime.getRuntime();
      double lm = SuicideService.lowMem;
      if (lm >= 1.0) {
        thresh = (long) (lm*1024);
      } else {
        thresh = (long) (runtime.maxMemory()*lm);
      }
      period = (long) (SuicideService.proactivePeriod * 1000L);
    }

    void start() {
      t = new Thread(this);
      t.start();
    }
    void stop() {
      t.interrupt();
      t = null;
    }

    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          Thread.sleep(period);
          checkForOOM();
        }
      } catch (InterruptedException ie) {
        Thread.interrupted();
        // exit thread...
      }
    }
    
    void checkForOOM() {
      long fm = runtime.freeMemory();
      long mm = runtime.maxMemory();
      long tm = runtime.totalMemory();
      long headroom = fm+(mm-tm); // how much heap space might be left
      if (headroom < thresh) {
        ssi.die(null, new Error("Looming OutOfMemoryError (only "+headroom+" of "+mm+" remaining, total="+tm+", free="+fm+")"));
      }
    }
  }
}
