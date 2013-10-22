/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.planning.plugin.completion;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.relay.RelayChangeReport;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates completion information from
 * agents in a society to determin the "completion" of the current
 * tasks. In most agents, it gathers the information and forwards the
 * completion status of the agent to another agent. This process
 * continues through a hierarchy of such plugins until the plugin at
 * the root of the tree is reached. When the root determines that
 * completion has been acheived (or is never going to be achieved), it
 * advances the clock with the expectation that the advancement will
 * engender additional activity and waits for the completion of that
 * work.
 **/

public abstract class CompletionSourcePlugin extends CompletionPlugin {
  private static final double NORMAL_TASK_COMPLETION_THRESHOLD = 0.99;
  private static final double CPU_CONSUMPTION_THRESHOLD = 0.95;
  private static final long NORMAL_UPDATE_INTERVAL = 5000L;
  private static final long NORMAL_LONG_CHECK_TARGETS_INTERVAL = 120000L;
  private static final long NORMAL_SHORT_CHECK_TARGETS_INTERVAL = 15000L;
  private static final long DEFAULT_DEAD_NODE_TIMEOUT = 120000L;
  private static final long DEFAULT_TIMER_SLACK = 10000;
  private static final String UPDATE_INTERVAL_KEY = "UPDATE_INTERVAL=";
  private static final String LONG_CHECK_TARGETS_INTERVAL_KEY = "LONG_CHECK_TARGETS_INTERVAL=";
  private static final String SHORT_CHECK_TARGETS_INTERVAL_KEY = "SHORT_CHECK_TARGETS_INTERVAL=";
  private static final String TASK_COMPLETION_THRESHOLD_KEY = "TASK_COMPLETION_THRESHOLD=";
  private static final String DEAD_NODE_TIMEOUT_KEY = "DEAD_NODE_TIMEOUT=";
  private static final String TIMER_SLACK_KEY = "TIMER_SLACK=";
  private static final int SHORT_CHECK_TARGETS_MAX = 5;
  private double TASK_COMPLETION_THRESHOLD = NORMAL_TASK_COMPLETION_THRESHOLD;
  private long UPDATE_INTERVAL = NORMAL_UPDATE_INTERVAL;
  private long LONG_CHECK_TARGETS_INTERVAL = NORMAL_LONG_CHECK_TARGETS_INTERVAL;
  private long SHORT_CHECK_TARGETS_INTERVAL = NORMAL_SHORT_CHECK_TARGETS_INTERVAL;
  private long DEAD_NODE_TIMEOUT = DEFAULT_DEAD_NODE_TIMEOUT;
  private long TIMER_SLACK = DEFAULT_TIMER_SLACK;
  private static final Class[] requiredServices = {
    UIDService.class,
    DemoControlService.class,
    AlarmService.class,
  };
  protected UIDService uidService;
  protected DemoControlService demoControlService;
  protected AlarmService alarmService;
  protected long now = System.currentTimeMillis();
  // The following are all times when we need to awaken
  private long nextCheckTargetsTime = 0L;       // Time to check the list of targets
  private long nextUpdateTime = now;		// Time to check for new laggards
  private int shortCheckTargetsCount = 0;
  private CompletionRelay relay;                // The relay we sent
  private Laggard selfLaggard = null;
  private UnaryPredicate myRelayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o == relay;
      }
    };
  private Subscription responseSubscription;
  private long timerTimeout = 0L; // When the timer should expire.

  private static Class[] concatRequiredServices(Class[] a1, Class[] a2) {
    Class[] result = new Class[a1.length + a2.length];
    System.arraycopy(a1, 0, result, 0, a1.length);
    System.arraycopy(a2, 0, result, a1.length, a2.length);
    return result;
  }

  protected CompletionSourcePlugin() {
    super(requiredServices);
  }

  protected CompletionSourcePlugin(Class[] requiredServices) {
    super(concatRequiredServices(CompletionSourcePlugin.requiredServices, requiredServices));
  }

  public void suspend() {
    if (haveServices()) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, UIDService.class, uidService);
      sb.releaseService(this, DemoControlService.class, demoControlService);
      sb.releaseService(this, AlarmService.class, alarmService);
      uidService = null;
    }
    super.suspend();
  }

  protected boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = (UIDService)
        sb.getService(this, UIDService.class, null);
      demoControlService = (DemoControlService)
        sb.getService(this, DemoControlService.class, null);
      alarmService = (AlarmService)
        sb.getService(this, AlarmService.class, null);
      return true;
    }
    return false;
  }

  public void setupSubscriptions() {
    Collection params = getParameters();
    for (Iterator i = params.iterator(); i.hasNext(); ) {
      String param = (String) i.next();
      if (param.startsWith(TASK_COMPLETION_THRESHOLD_KEY)) {
        TASK_COMPLETION_THRESHOLD =
          Double.parseDouble(param.substring(TASK_COMPLETION_THRESHOLD_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + TASK_COMPLETION_THRESHOLD_KEY
                                                + TASK_COMPLETION_THRESHOLD);
        continue;
      }
      if (param.startsWith(UPDATE_INTERVAL_KEY)) {
        UPDATE_INTERVAL = Long.parseLong(param.substring(UPDATE_INTERVAL_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + UPDATE_INTERVAL_KEY
                                                + UPDATE_INTERVAL);
        continue;
      }
      if (param.startsWith(LONG_CHECK_TARGETS_INTERVAL_KEY)) {
        LONG_CHECK_TARGETS_INTERVAL = Long.parseLong(param.substring(LONG_CHECK_TARGETS_INTERVAL_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + LONG_CHECK_TARGETS_INTERVAL_KEY
                                                + LONG_CHECK_TARGETS_INTERVAL);
        continue;
      }
      if (param.startsWith(SHORT_CHECK_TARGETS_INTERVAL_KEY)) {
        SHORT_CHECK_TARGETS_INTERVAL = Long.parseLong(param.substring(SHORT_CHECK_TARGETS_INTERVAL_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + SHORT_CHECK_TARGETS_INTERVAL_KEY
                                                + SHORT_CHECK_TARGETS_INTERVAL);
        continue;
      }
      if (param.startsWith(DEAD_NODE_TIMEOUT_KEY)) {
        DEAD_NODE_TIMEOUT = Long.parseLong(param.substring(DEAD_NODE_TIMEOUT_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + DEAD_NODE_TIMEOUT_KEY
                                                + DEAD_NODE_TIMEOUT);
        continue;
      }
      if (param.startsWith(TIMER_SLACK_KEY)) {
        TIMER_SLACK = Long.parseLong(param.substring(TIMER_SLACK_KEY.length()));
        if (logger.isInfoEnabled()) logger.info("Set "
                                                + TIMER_SLACK_KEY
                                                + TIMER_SLACK);
        continue;
      }
    }
    responseSubscription = blackboard.subscribe(myRelayPredicate);
    if (haveServices()) {
      checkTargets();
      checkSelfLaggard(true);
      resetTimer(SHORT_CHECK_TARGETS_INTERVAL);
      timerTimeout = System.currentTimeMillis() + SHORT_CHECK_TARGETS_INTERVAL + TIMER_SLACK;
    } else {
      timerTimeout = 0L;
    }
  }

  public void execute() {
    if (haveServices()) {
      now = System.currentTimeMillis();
      boolean timerExpired = !hasUnexpiredTimer(); // should we execute now?
      if (!timerExpired) {
        if (timerTimeout > 0L && now > timerTimeout) {
          // this shouldn't hapen
          long actual = getTimerExpirationTime();
          logger.error("Timer failed to fire: now="+(new Date(now))+"  timeout="+(new Date(timerTimeout))+
                       "  realTrigger="+(new Date(actual)));
          timerExpired = true;
        }
      }

      if (timerExpired) {
        //cancelTimer();  // using resetTimer now, no need to cancel.
        if (now > nextCheckTargetsTime) {
          if (checkTargets()) {
            checkSelfLaggard(true);
            shortCheckTargetsCount = 0; // Reset and start over
            nextCheckTargetsTime = now + SHORT_CHECK_TARGETS_INTERVAL;
          } else if (shortCheckTargetsCount < SHORT_CHECK_TARGETS_MAX) {
            shortCheckTargetsCount++;
            nextCheckTargetsTime = now + SHORT_CHECK_TARGETS_INTERVAL;
            if (logger.isDebugEnabled()) {
              logger.debug("shortCheckTargetsCount=" + shortCheckTargetsCount);
            }
          } else {              // Switch to using the long interval
            nextCheckTargetsTime = now + LONG_CHECK_TARGETS_INTERVAL;
          }
        } else if (shortCheckTargetsCount >= SHORT_CHECK_TARGETS_MAX) {
          checkSelfLaggard(false);
          checkLaggards();
        }
        resetTimer(UPDATE_INTERVAL);
        timerTimeout = System.currentTimeMillis() + UPDATE_INTERVAL + TIMER_SLACK;
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("waiting for services");
      }
    }
  }
  private void checkSelfLaggard(boolean isLaggard) {
    if (selfLaggard == null || selfLaggard.isLaggard() != isLaggard) {
      selfLaggard = new Laggard(getAgentIdentifier(), 1.0, isLaggard ? 1.0 : 0.0, isLaggard);
      if (isLaggard) handleNewLaggard(selfLaggard);
    }
  }

  /**
   * Check if a new relay needs to be published due to a change in
   * targets. We get the set of targets by calling "getTargetNames()"
   * and compare to the set of agents that are
   * targes of the current relay. If a difference is detected, the old
   * relay is removed and a new one with the new agent set is
   * published.
   * @return true if a new relay was published (suppresses laggard checking)
   **/
  private boolean checkTargets() {
    Set targets = getTargets();
    if (relay == null) {
      relay = new CompletionRelay(null, targets, TASK_COMPLETION_THRESHOLD, CPU_CONSUMPTION_THRESHOLD);
      relay.setUID(uidService.nextUID());
      if (logger.isInfoEnabled()) logger.info("New relay for " + targets);
      blackboard.publishAdd(relay);
      return true;
    }
    if (!targets.equals(relay.getTargets())) {
      RelayChangeReport rcr = new RelayChangeReport(relay);
      relay.setTargets(targets);
      blackboard.publishChange(relay, Collections.singleton(rcr));
      if (logger.isInfoEnabled()) logger.info("Changed relay for " + targets);
      return true;
    }
    if (logger.isDebugEnabled()) logger.debug("Same relay for " + targets);
    return false;
  }

  /**
   * Identify the worst laggard and "handle" it.
   **/
  private void checkLaggards() {
    SortedSet laggards = relay.getLaggards();
    adjustLaggards(laggards);
    if (laggards.size() > 0) {
      long oldestAllowedTimestamp = now - (LaggardFilter.NON_LAGGARD_UPDATE_INTERVAL + DEAD_NODE_TIMEOUT);
      for (Iterator i = laggards.iterator(); i.hasNext(); ) {
        Laggard newLaggard = (Laggard) i.next();
        long okBy = newLaggard.getTimestamp() - oldestAllowedTimestamp;
        if (okBy > 0L) {
          if (logger.isDebugEnabled()) {
            logger.debug("checkLaggards(" + (okBy / 1000L) + ") " + newLaggard);
          }
          handleNewLaggard(newLaggard);
          break;
        } else {
          //relay.flushOutdatedLaggard(newLaggard);
          if (logger.isDebugEnabled()) {
            logger.debug("checkLaggards ignoring old " + newLaggard);
          }
        }
      }
    } else {
      handleNewLaggard(selfLaggard);
      if (logger.isDebugEnabled()) {
        logger.debug("Waiting for relay responses");
      }
    }
  }

  protected void setPersistenceNeeded() {
    if (logger.isInfoEnabled()) {
      logger.info("setPersistence()");
    }
    relay.setPersistenceNeeded();
    blackboard.publishChange(relay);
  }

  /**
   * Get the addresses of all targets
   **/
  protected abstract Set getTargets();

  protected abstract void handleNewLaggard(Laggard worstLaggard);

  protected abstract boolean adjustLaggards(SortedSet laggards);
}
