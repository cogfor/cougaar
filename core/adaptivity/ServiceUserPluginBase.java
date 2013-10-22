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

package org.cougaar.core.adaptivity;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;

/**
 * Convenience base class for plugins that need to acquire services
 * that may not be immediately available when first started. Records
 * the service classes needed and attempts acquire them on request.
 * <p>Also provides timer services.
 **/
public abstract class ServiceUserPluginBase extends ComponentPlugin {
  private Class[] serviceClasses;

  private boolean[] serviceAcquired;

  private boolean allServicesAcquired = false;

  private Alarm timer;

  /**
   * Everybody needs a logger, so we provide it here.
   **/
  protected LoggingService logger;

  /**
   * Constructor
   * @param serviceClasses the service classes needed for this plugin
   * to operate.
   **/
  protected ServiceUserPluginBase(Class[] serviceClasses) {
    this.serviceClasses = serviceClasses;
    this.serviceAcquired = new boolean[serviceClasses.length];
  }

  /**
   * Override to get a logger on load
   **/
  @Override
public void load() {
    super.load();
    logger = getServiceBroker().getService(this, LoggingService.class, null);
  }

  /**
   * Override to release a logger on load
   **/
  @Override
public void unload() {
    if (logger != null) {
      getServiceBroker().releaseService(this, LoggingService.class, logger);
      logger = null;
    }
    super.unload();
  }

  /**
   * Test if all services specified in the constructor are available.
   * Sub-classes should call this method from their setupSubscriptions
   * and execute methods until it returns true. Once this method
   * returns true, the services should be requested and normal
   * operation started. Once all the services are available, this
   * method simply returns true. See <code>haveServices()</code> in
   * {@link ConditionServiceProvider#execute} for a typical
   * usage pattern.
   **/
  protected boolean acquireServices() {
    cancelTimer();
    if (!allServicesAcquired) {
      allServicesAcquired = true; // Assume we will get them all
      ServiceBroker sb = getServiceBroker();
      for (int i = 0; i < serviceClasses.length; i++) {
        if (!serviceAcquired[i]) {
          Service svc = (Service) sb.getService(this, serviceClasses[i], null);
          if (svc == null) {
            allServicesAcquired = false;
            break;
          }
          if (logger.isDebugEnabled()) logger.debug(serviceClasses[i] + " acquired");
          serviceAcquired[i] = true;
          sb.releaseService(this, serviceClasses[i], svc);
        }
      }
      if (!allServicesAcquired) {
        startTimer(1000L);
      }
    }
    return allServicesAcquired;
  }

  /**
   * Test if the timer has expired.
   * @return false if the timer is not running or has not yet expired
   * else return true.
   **/
  protected boolean timerExpired() {
    return timer != null && timer.hasExpired();
  }

  /**
   * Schedule a update wakeup after some interval of time
   * @param delay how long to delay before the timer expires.
   **/
  protected void startTimer(final long delay) {
    if (timer != null) return;  // update already scheduled
    if (logger.isDebugEnabled()) logger.debug("Starting timer " + delay);
    timer = new Alarm() {
      long expirationTime = System.currentTimeMillis() + delay;
      boolean expired = false;
      public long getExpirationTime() {return expirationTime;}
      public synchronized void expire() {
        if (!expired) {
          expired = true;
          org.cougaar.core.service.BlackboardService bbs = getBlackboardService();
          if (bbs != null)
            bbs.signalClientActivity();
        }
      }
      public boolean hasExpired() { return expired; }
      public synchronized boolean cancel() {
        boolean was = expired;
        expired=true;
        return was;
      }
    };
    getAlarmService().addRealTimeAlarm(timer);
  }

  /**
   * Cancel the timer.
   **/
  protected void cancelTimer() {
    if (timer == null) return;
    if (logger.isDebugEnabled()) logger.debug("Cancelling timer");
    timer.cancel();
    timer = null;
  }
}
