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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.StandardVariableEvaluator;
import org.cougaar.core.qos.metrics.VariableEvaluator;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.service.PlaybookReadService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 * Sets OperatingModes for components based on plays in the playbook
 * and current conditions. Runs periodically and selects new plays
 * according to the prevailing {@link Condition}s.
 **/
public class AdaptivityEngine extends ServiceUserPlugin {
  private static final long MISSING_CONDITION_DELAY = 60000;

  /**
   * A listener that listens to itself. It responds true when it is
   * itself the object of a subscription change.
   **/
  private static class Listener extends OperatingModeService.ListenerAdapter
    implements ConditionService.Listener,
               PlaybookReadService.Listener,
               OperatingModeService.Listener,
               UnaryPredicate
  {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return (this == o);
    }

    @Override
   public boolean wantAdds() {
      return true;
    }
  }

  private PlayHelper helper;
  private PlaybookReadService playbookService;
  private OperatingModeService operatingModeService;
  private ConditionService conditionService;
  private UIDService uidService;
  private MetricsService metricsService;
  private VariableEvaluator variableEvaluator;

  private static Class[] requiredServices = {
    PlaybookReadService.class,
    OperatingModeService.class,
    ConditionService.class,
    UIDService.class,
    MetricsService.class
  };

  private Subscription conditionListenerSubscription;
  private Subscription playbookListenerSubscription;
  private Subscription operatingModeListenerSubscription;

  private Map smMap = new HashMap();

  /**
   * Keeps track of the remote operating mode constraints we have
   * created by name.
   **/
  private Map romcMap = new HashMap();

  /**
   * Keeps a copy of romcMap while updating romcMap. Declared as
   * instance variable to avoid consing a new one every time.
   **/
  private Map tempROMCMap = new HashMap();

  /**
   * The names of the changed remote operating mode constraints.
   * Declared as instance variable to avoid consing a new one every
   * time.
   **/
  private Set romcChanges = new HashSet();

  private Play[] plays;

  private List missingConditions = new ArrayList();

  private long missingConditionTime =
    System.currentTimeMillis() + MISSING_CONDITION_DELAY;

  private Listener playbookListener = new Listener();

  private Listener conditionListener = new Listener();

  private Listener operatingModeListener = new Listener();

  public AdaptivityEngine() {
    super(requiredServices);
  }

  /**
   * Test if the services we need to run have all been acquired. We
   * use the non-null value of the primary service (playbookService)
   * to indicate that all services have been acquired. If
   * playbookService has not been set, we use the
   * super.acquireServices to perform the test of whether all services
   * are available or not.
   **/
  protected boolean haveServices() {
    if (playbookService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      playbookService = sb.getService(this, PlaybookReadService.class, null);
      operatingModeService = sb.getService(this, OperatingModeService.class, null);
      conditionService = sb.getService(this, ConditionService.class, null);
      uidService = sb.getService(this, UIDService.class, null);
      metricsService = sb.getService(this, MetricsService.class, null);
      variableEvaluator = new StandardVariableEvaluator(sb);

      conditionService.addListener(conditionListener);
      operatingModeService.addListener(operatingModeListener);
      playbookService.addListener(playbookListener);

      helper = new PlayHelper(logger, operatingModeService, conditionService, blackboard, uidService, smMap);
      return true;
    }
    return false;
  }

  /**
   * Cleanup before we stop -- release all services.
   **/
  @Override
public void stop() {
    ServiceBroker sb = getServiceBroker();
    if (playbookService != null) {
      playbookService.removeListener(playbookListener);
      sb.releaseService(this, PlaybookReadService.class, playbookService);
      playbookService = null;
    }
    if (conditionService != null) {
      conditionService.removeListener(conditionListener);
      sb.releaseService(this, ConditionService.class, conditionService);
      conditionService = null;
    }
    if (operatingModeService != null) {
      sb.releaseService(this, OperatingModeService.class, operatingModeService);
      operatingModeService = null;
    }
    super.stop();
  }

  /**
   * Setup subscriptions to listen for playbook and condition changes.
   * The current implementation responds immedicately to changes. An
   * alternative would be to introduce delays before responding to
   * reduce chaotic behavior.
   **/
  @Override
public void setupSubscriptions() {
    Iterator iter = getParameters().iterator();
    if (iter.hasNext()) {
      String param = (String) iter.next();
      try {
        long missingConditionDelay = Long.parseLong(param);
        if (missingConditionDelay >= 5000L) {
          missingConditionTime = System.currentTimeMillis() + missingConditionDelay;
        } else {
          logger.error("Bogus missing condition delay is less than 5000");
        }
      } catch (Exception e) {
        logger.error("Error parsing missing condition delay", e);
      }
    }
    playbookListenerSubscription = blackboard.subscribe(playbookListener);
    conditionListenerSubscription = blackboard.subscribe(conditionListener);
    operatingModeListenerSubscription = blackboard.subscribe(operatingModeListener);
    blackboard.publishAdd(conditionListener);
    blackboard.publishAdd(playbookListener);
  }

  /**
   * The normal plugin execute. Wakes up whenever the playbook is
   * changed or whenever a Condition is changed. Also wakes up if the
   * base class has set a timer waiting for all services to be
   * acquired. If the playbook has changed we refetch the new set of
   * plays and fetch the conditions required by those new plays. If
   * the playbook has not changed, but the conditions have, we refetch
   * the required conditions. If all required conditions are
   * available, the operating modes are updated from the current
   * plays.
   **/
  @Override
public synchronized void execute() {
    boolean debug = logger.isDebugEnabled();
    if (debug) {
      if (conditionListenerSubscription.hasChanged()) logger.debug("Condition changed");
      if (operatingModeListenerSubscription.hasChanged()) logger.debug("OperatingMode changed");
      if (playbookListenerSubscription.hasChanged()) logger.debug("Playbook changed");
    }
    if (haveServices()) {
      if (plays == null || playbookListenerSubscription.hasChanged()) {
        plays = playbookService.getCurrentPlays();
        if (debug) logger.debug("got " + plays.length + " plays");
        if (debug) logger.debug("getting conditions");
        getConditions();
      } else if (conditionListenerSubscription.hasChanged()) {
        getConditions();
        if (debug) logger.debug("got " + smMap.size() + " conditions");
      } else if (operatingModeListenerSubscription.hasChanged()) {
	if (debug) logger.debug("operating mode subscription changed");
      } else if (timerExpired()) {
        if (debug) logger.debug("missing condition timer expired");
      } else {
        if (debug) logger.debug("nothing changed");
      }
      if (debug) logger.debug("updateOperatingModes");
      updateOperatingModes();
      if (missingConditions.size() > 0 && logger.isWarnEnabled()) {
        long timeLeft = missingConditionTime - System.currentTimeMillis();
        if (timeLeft <= 0L) {
          for (Iterator i = missingConditions.iterator(); i.hasNext(); ) {
            String msg = (String) i.next();
            logger.warn(msg);
          }
          missingConditions.clear();
        } else {
          cancelTimer();
          resetTimer(timeLeft);
        }
      }
    }
  }

  /**
   * Scan the current plays for required conditions and stash them in
   * smMap for use in running the plays. Non-existent conditions that
   * look like measurements available from the MetricsService are
   * converted to a MetricsCondition. The name of such a condition is:
   * Metrics:{<type>:}{<scope>:}<metrics formula name>. Allowed types
   * are: double, long, integer, string, and boolean. If the type is
   * omitted, "double" is assumed. The type should match the context
   * in which the condition is being used. If the scope is omitted, it
   * is assumed to be this agent. Otherwise, scopes conform to the
   * Metrics path specification with the following enhancement: The
   * arglist of a scope can itself be a scope. This is interpreted to
   * mean the scope containing the inner scope. E.g.: Node(Agent(foo))
   * is the scope in the node of the agent named foo.
   **/
  private void getConditions() {
    smMap.clear();
    for (int i = 0; i < plays.length; i++) {
      Play play = plays[i];
      for (Iterator x = play.getIfClause().iterator(); x.hasNext(); ) {
        Object o = x.next();
        if (o instanceof String) {
          String name = (String) o;
          if (!smMap.containsKey(name)) {
            Condition sm = conditionService.getConditionByName(name);
            if (sm == null) {
              if (name.startsWith(MetricsCondition.METRICS_PREFIX)) {
                try {
                  sm = MetricsCondition.create(name, metricsService, variableEvaluator);
                } catch (Exception e) {
                  if (logger.isWarnEnabled()) logger.warn(e.getMessage(), e);
                }
              } else {
                if (logger.isInfoEnabled()) logger.info("No condition named " + name);
              }
            }
            if (sm != null) {
              smMap.put(name, sm);
            }
          }
        }
      }
    }
  }

  /**
   * Update all operating modes based on conditions and the playbook.
   * The real work is done in the {@link PlayHelper}.
   **/
  private void updateOperatingModes() {
    tempROMCMap.putAll(romcMap);
    missingConditions.clear();
    helper.updateOperatingModes(plays, romcMap, romcChanges, missingConditions);
    for (Iterator i = romcChanges.iterator(); i.hasNext(); ) {
      String operatingModeName = (String) i.next();
      if (romcMap.containsKey(operatingModeName)) {
        // Now present. Was it added or changed
        if (tempROMCMap.containsKey(operatingModeName)) {
          // Was previously present so must have changed
          blackboard.publishChange(romcMap.get(operatingModeName));
        } else {
          // Was not previously present so must have been added
          blackboard.publishAdd(romcMap.get(operatingModeName));
        }
      } else {
        // No longer present. Must have been removed.
        blackboard.publishRemove(tempROMCMap.get(operatingModeName));
      }
    }
    tempROMCMap.clear();
  }
}
