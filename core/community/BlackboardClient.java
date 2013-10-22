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

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;

/**
 * BlackboardClient base class used by CommunityService components that require
 * blackboard access.  Primarily used to send Relays between agents and
 * remote community manager.
 */
public class BlackboardClient extends BlackboardClientComponent {

  // Supported BB operations
  public static final int ADD    = 0;
  public static final int CHANGE = 1;
  public static final int REMOVE = 2;

  protected LoggingService logger;
  protected long TIMER_INTERVAL = 5 * 1000;
  private BBWakeAlarm wakeAlarm;
  private final List addQueue = new ArrayList(5);
  private final List changeQueue = new ArrayList(5);
  private final List removeQueue = new ArrayList(5);

  private ServiceAvailableListener serviceListener;

  public BlackboardClient(BindingSite bs) {
    try {
      setBindingSite(bs);
      if (servicesAvailable()) {
        init();
      } else {
        serviceListener = new ServiceAvailableListener() {
          public void serviceAvailable(ServiceAvailableEvent sae) {
            //if (sae.getService().equals(BlackboardService.class)) {
              init();
            //}
          }
        };
        getServiceBroker().addServiceListener(serviceListener);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  protected AlarmService getAlarmService() {
    if (alarmService == null) {
      setAlarmService((AlarmService)getServiceBroker().getService(this, AlarmService.class, null));
    }
    return alarmService;
  }

  private boolean servicesAvailable() {
    ServiceBroker sb = getServiceBroker();
    boolean servicesAvailable =
        sb.hasService(org.cougaar.core.service.AlarmService.class) &&
        sb.hasService(org.cougaar.core.service.SchedulerService.class) &&
        sb.hasService(org.cougaar.core.service.AgentIdentificationService.class) &&
        sb.hasService(org.cougaar.core.service.LoggingService.class) &&
        sb.hasService(org.cougaar.core.service.BlackboardService.class);
    return servicesAvailable;
  }

  static Object lock = new Object();
  /**
   * Set essential services and invoke GenericStateModel methods.
   */
  private void init() {
    synchronized (lock) {
      if (servicesAvailable() && getModelState() == -1) {
        //System.out.println("init: " + agentId);
        ServiceBroker sb = getServiceBroker();
        setSchedulerService(
            (SchedulerService)sb.getService(this, SchedulerService.class, null));
        setAgentIdentificationService(
            (AgentIdentificationService)sb.getService(this,
            AgentIdentificationService.class, null));
        logger =
            (LoggingService)sb.getService(this, LoggingService.class, null);
        logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger,
            agentId + ": ");
        setAlarmService((AlarmService)sb.getService(this, AlarmService.class, null));
        setBlackboardService(
            (BlackboardService)sb.getService(this, BlackboardService.class, null));
        blackboard = (BlackboardService)sb.getService(this, BlackboardService.class, null);
        initialize();
        load();
        start();
        if (serviceListener != null) {
          sb.removeServiceListener(serviceListener);
        }
      }
    }
  }

  public void publish(Object obj, int type) {
    if (logger.isDetailEnabled()) {
      logger.detail("publish: type=" + type + " obj=" + obj);
    }
    switch (type) {
      case ADD:
        synchronized (addQueue) {addQueue.add(obj);}
        break;
      case CHANGE:
        synchronized (changeQueue) {changeQueue.add(obj);}
        break;
      case REMOVE:
        synchronized (removeQueue) {removeQueue.add(obj);}
        break;
    }
    if (blackboard != null) {
      blackboard.signalClientActivity();
    } else {
      //if (logger.isDetailEnabled()) {
        logger.info("Blackboard not available, retrying in " + TIMER_INTERVAL +
                      "ms");
      //}
      AlarmService as = getAlarmService();
      if (as != null) {
        // Start timer to check service availability later
        wakeAlarm = new BBWakeAlarm(System.currentTimeMillis() + TIMER_INTERVAL);
        as.addRealTimeAlarm(wakeAlarm);
      }
    }
  }

  /**
   * Process queued requests.
   */
  private void fireAll() {
    if (logger.isDetailEnabled()) {
      logger.detail("fireall:" +
                    " add(" + addQueue.size() + ")" +
                    " change(" + changeQueue.size() + ")" +
                    " remove(" + removeQueue.size() + ")");
    }
    fire(addQueue, ADD);
    fire(changeQueue, CHANGE);
    fire(removeQueue, REMOVE);
  }

  private void fire(List queue, int type) {
    int n;
    List l;
    synchronized (queue) {
      n = queue.size();
      if (n <= 0 || blackboard == null) { return; }
      l = new ArrayList(queue);
      queue.clear();
    }
    for (int i = 0; i < n; i++) {
      switch (type) {
        case ADD:
          blackboard.publishAdd(l.get(i));
          if (logger.isDebugEnabled()) {
            logger.debug("publishAdd: " + l.get(i));
          }
          break;
        case CHANGE:
          blackboard.publishChange(l.get(i));
          if (logger.isDebugEnabled()) {
            logger.debug("publishChange: " + l.get(i));
          }
          break;
        case REMOVE:
          blackboard.publishRemove(l.get(i));
          if (logger.isDebugEnabled()) {
            logger.debug("publishRemove: " + l.get(i));
          }
          break;
      }
    }
  }

  public void setupSubscriptions() {}

  public void execute() {
    fireAll();  // Published queued requests
  }

  // Timer for periodically checking blackboard availability.
  // Blackboard activity is signaled once the blackboard service is available
  // to check for queued requests
  public class BBWakeAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public BBWakeAlarm(long expirationTime) {expiresAt = expirationTime;}
    public long getExpirationTime() {return expiresAt;}
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        if (blackboard != null) {
          blackboard.signalClientActivity();
        } else { // Not ready yet, wait for awhile
          wakeAlarm = new BBWakeAlarm(System.currentTimeMillis() + TIMER_INTERVAL);
          alarmService.addRealTimeAlarm(wakeAlarm);
        }
      }
    }
    public boolean hasExpired() {return expired;}
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }

}
