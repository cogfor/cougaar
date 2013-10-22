/* 
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.core.plugin.deletion;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PluginAlarm;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component removes {@link Deletable} blackboard objects
 * according to policy and their age.
 * <p>
 * How to Configure the DeletionPlugin:
 * <p>
 * The DeletionPlugin runs according to a prescribed schedule to find
 * Deletable objects on the blackboard and delete them if they are ready
 * to be deleted. These actions are controlled by a
 * DelectionSchedulePolicy and one or more DeletionPolicy objects on the
 * blackboard. The DeletionPlugin guarantees that there is always a
 * DefaultDeletionPolicy present by creating on at startup (or restart)
 * if there is not one already present.
 * <p>
 * DeletionSchedulePolicy:
 * <p>
 * The deletion schedule is characterized primarily by a period (how
 * often) and a phase (at what time of day). The public interface of the
 * standard DeletionSchedulePolicy allows all aspects of the schedule to
 * be altered including adding specific times for deletion to occur. The
 * current DeletionPlugin implementation insures that exactly one
 * DeletionSchedulePolicy exists on the blackboard creating one if
 * necessary and deleting extraneous ones. If an existing policy is
 * found, it is left as is. Subclasses could choose to insure that the
 * periodic schedule parameters agree with the values specified as plugin
 * parameters.
 * <p>
 * DefaultDeletionPolicy:
 * <p>
 * The DeletionPlugin also guarantees that there is exactly one default
 * DeletionPolicy on the blackboard. If there are none, one is created
 * using plugin parameters. If there are multiples (should never happen),
 * extras are deleted.
 * <p>
 * DeletionPlugin Parameters:
 * <p>
 * The DeletionPlug accepts four named parameters as follows:
 * <pre>
 * deletionDelay=&lt;long default 15 days&gt;
 * deletionPeriod=&lt;long default 7 days&gt;
 * deletionPhase=&lt;long 0 (midnight)&gt;
 * archivingEnabled=&lt;boolean&gt;
 * </pre> 
 * <p>
 * The archivingEnable parameter causes a persistence snapshot to be
 * taken for archiving purposes prior to doing deletions.
 */
public class DeletionPlugin extends ComponentPlugin {

  @Override
protected void setupSubscriptions() {
    long deletionDelay = DEFAULT_DELETION_DELAY;
    long deletionPeriod = DEFAULT_DELETION_PERIOD;
    long deletionPhase = DEFAULT_DELETION_PHASE;
    archivingEnabled = DEFAULT_ARCHIVING_ENABLED;
    for (Iterator i = getParameters().iterator(); i.hasNext();) {
      String param = (String) i.next();
      deletionDelay =
        parseLongParameter(param, DELETION_DELAY_PREFIX, deletionDelay);
      deletionPeriod =
        parseLongParameter(param, DELETION_PERIOD_PREFIX, deletionPeriod);
      deletionPhase =
        parseLongParameter(param, DELETION_PHASE_PREFIX, deletionPhase);
      archivingEnabled =
        parseBooleanParameter(
			      param,
			      ARCHIVING_ENABLED_PREFIX,
			      archivingEnabled);
    }
    deletionPolicies =
      (CollectionSubscription) blackboard.subscribe(
						    deletionPolicyPredicate,
						    false);
    checkDefaultDeletionPolicy(deletionDelay);
    deletionSchedulePolicies =
      (CollectionSubscription) blackboard.subscribe(
						    deletionSchedulePolicyPredicate,
						    false);
    checkDeletionSchedulePolicies(deletionPeriod, deletionPhase);
    getBlackboardService().setShouldBePersisted(false);
    // All subscriptions are created as needed
    setAlarm();
  }

  /**
   * Runs only when the alarm expires.
   *
   * The procedure is:
   * Find new allocations for tasks that deletable and mark the allocations
   * Find tasks with deletable dispositions and mark them
   * Find deletable tasks that are subtasks of an expansion and
   * remove them from the expansion and remove them from the
   * logplan.
   */
  @Override
public void execute() {
    scenarioNow = currentTimeMillis();
    systemNow = System.currentTimeMillis();
    if (alarm.hasExpired()) { // Time to make the donuts
      if (logger.isDebugEnabled())
        logger.debug("Time to make the donuts");
      if (archivingEnabled) {
        try {
          getBlackboardService().persistNow(); // Record our state
        } catch (PersistenceNotEnabledException pnee) {
          pnee.printStackTrace();
          logger.error("Archiving disabled");
          archivingEnabled = false;
        }
      }
      checkDeletables();
      setAlarm();
    }
  }

  protected void checkDeletables() {
    Collection deletables = blackboard.query(deletablePredicate);
    for (Iterator i = deletables.iterator(); i.hasNext();) {
      Deletable element = (Deletable) i.next();
      policiesLoop:
      for (Iterator j = deletionPolicies.iterator(); j.hasNext();) {
        DeletionPolicy policy = (DeletionPolicy) j.next();
        if (policy.getPredicate().execute(element)) {
          long deletionTime =
            element.getDeletionTime() + policy.getDeletionDelay();
          long now = element.useSystemTime() ? systemNow : scenarioNow;
          if (deletionTime < now) {
            element.setDeleted();
            blackboard.publishRemove(element);
          }
          break policiesLoop;
        }
      }
    }
  }

  protected static final SimpleDateFormat deletionTimeFormat;
  static {
    deletionTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
  }
  protected static UnaryPredicate truePredicate = new UnaryPredicate() {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      return true;
    }
  };
  
  private static UnaryPredicate deletablePredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
	if (o instanceof Deletable) {
	  Deletable d = (Deletable) o;
	  return d.isDeletable();
	}
	return false;
      }
    };

  protected static final String DELETION_DELAY_PREFIX = "deletionDelay=";

  protected static final String DELETION_PERIOD_PREFIX = "deletionPeriod=";

  protected static final String DELETION_PHASE_PREFIX = "deletionPhase=";

  protected static final String ARCHIVING_ENABLED_PREFIX = "archivingEnabled=";

  protected static final long DEFAULT_DELETION_PERIOD = 7 * 86400000L;

  protected static final long DEFAULT_DELETION_DELAY = 15 * 86400000L;

  protected static final long DEFAULT_DELETION_PHASE = 0L;

  protected static final boolean DEFAULT_ARCHIVING_ENABLED = true;

  protected static final long subscriptionExpirationTime = 10L * 60L * 1000L;

  protected boolean archivingEnabled = true;

  protected Alarm alarm;

  protected long scenarioNow;
  protected long systemNow;

  protected DeletionSchedulePolicy theDeletionSchedulePolicy;

  protected UnaryPredicate deletionPolicyPredicate = new UnaryPredicate() {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
	return o instanceof DeletionPolicy;
      }
    };

  protected UnaryPredicate deletionSchedulePolicyPredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
	return o instanceof DeletionSchedulePolicy;
      }
    };

  protected SortedSet deletionPolicySet = new TreeSet(new Comparator() {
      public int compare(Object o1, Object o2) {
	DeletionPolicy p1 = (DeletionPolicy) o1;
	DeletionPolicy p2 = (DeletionPolicy) o2;
	int diff = p1.getPriority() - p2.getPriority();
	if (diff != 0)
	  return diff;
	String n1 = p1.getName();
	String n2 = p2.getName();
	if (n1 != n2) {
	  if (n1 == null)
	    return -1;
	  if (n2 == null)
	    return +1;
	  diff = n1.compareTo(n2);
	  if (diff != 0)
	    return diff;
	}
	return o1.hashCode() - o2.hashCode();
      }
    });

  protected CollectionSubscription deletionPolicies;

  protected CollectionSubscription deletionSchedulePolicies;

  protected LoggingService logger;

  protected UIDService uidService;

  public void setLoggingService(LoggingService ls) {
    logger = ls;
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  @Override
public void load() {
    super.load();
    if (!(logger instanceof LoggingServiceWithPrefix)) {
      logger =
        LoggingServiceWithPrefix.add(
				     logger,
				     getAgentIdentifier().toString() + ": ");
    }
  }
  
  /**
   * The known unit names
   */
  private static Map intervals = new HashMap(11);
  static {
    intervals.put("seconds", new Long(1000L));
    intervals.put("minutes", new Long(1000L * 60L));
    intervals.put("hours", new Long(1000L * 60L * 60L));
    intervals.put("days", new Long(1000L * 60L * 60L * 24L));
    intervals.put("weeks", new Long(1000L * 60L * 60L * 24L * 7L));
  }

  private static long parseInterval(String param) {
    param = param.trim();
    int spacePos = param.indexOf(' ');
    long mul = 1L;
    if (spacePos >= 0) {
      String units = param.substring(spacePos + 1).toLowerCase();
      param = param.substring(0, spacePos);
      Long factor = (Long) intervals.get(units);
      if (factor != null) {
        mul = factor.longValue();
      }
    }
    return Long.parseLong(param) * mul;
  }

  protected long parseLongParameter(String param, String prefix, long dflt) {
    if (param.startsWith(prefix)) {
      try {
        return parseInterval(param.substring(prefix.length()));
      } catch (Exception e) {
        if (logger.isWarnEnabled())
          logger.warn("Could not parseInterval " + param);
        return dflt;
      }
    }
    return dflt;
  }

  protected boolean parseBooleanParameter(
					  String param,
					  String prefix,
					  boolean dflt) {
    if (param.startsWith(prefix)) {
      return Boolean.valueOf(param.substring(prefix.length())).booleanValue();
    }
    return dflt;
  }

  /**
   * Check to see if the DefaultDeletionPolicy is present and matches the
   * current deletionDelay, If a DefaultDeletionPolicy is found for
   * which the deletionDelay does not match the current deletionDelay,
   * it is removed. If no DefaultDeletionPolicy having the correct
   * deletionDelay is found, a new one created and added.
   */
  private void checkDefaultDeletionPolicy(long deletionDelay) {
    for (Iterator i = deletionPolicies.iterator(); i.hasNext();) {
      DeletionPolicy policy = (DeletionPolicy) i.next();
      if (SimpleDeletionPolicy.isDefaultDeletionPolicy(policy)) {
        if (policy.getDeletionDelay() == deletionDelay) {
          return; // ok
        }
        blackboard.publishRemove(policy);
      }
    }
    SimpleDeletionPolicy policy =
      new SimpleDeletionPolicy(
			       "Default Deletion Policy",
			       truePredicate,
			       deletionDelay,
			       DeletionPolicy.MIN_PRIORITY);
    uidService.registerUniqueObject(policy);
    blackboard.publishAdd(policy);
  }

  /**
   * Check to see if the default schedule policy is present. If a
   * DeletionSchedulePolicy is found then it is left as is. If no
   * DeletionSchedulePolicy is found, a new one created and added.
   * If multiple schedule policies are found, all but one is deleted.
   * Subclasses may wish to set the periodic schedule parameters to
   * match the specified values, but the base implementation only uses
   * the values if a new policy must be created.
   */
  protected void checkDeletionSchedulePolicies(
    long deletionPeriod,
    long deletionPhase) {
    for (Iterator i = deletionSchedulePolicies.iterator(); i.hasNext();) {
      DeletionSchedulePolicy policy = (DeletionSchedulePolicy) i.next();
      if (theDeletionSchedulePolicy == null) {
        theDeletionSchedulePolicy = policy;
      } else {
        // Remove extraneous policies
        blackboard.publishRemove(policy);
      }
    }
    if (theDeletionSchedulePolicy == null) {
      DeletionSchedulePolicy policy =
        new DeletionSchedulePolicy(deletionPeriod, deletionPhase);
      blackboard.publishAdd(policy);
      theDeletionSchedulePolicy = policy;
    }
  }

  private Alarm createAlarm(long time) {
    return new PluginAlarm(time) {
	@Override
   public BlackboardService getBlackboardService() {
	  return blackboard;
	}
      };
  }


  /**
   * Set the alarm so that it expires when the time is next congruent to the
   * deletionPhase modulo the deletionPeriod
   */
  protected void setAlarm() {
    long now = currentTimeMillis();
    long nextAlarm = theDeletionSchedulePolicy.getNextDeletionTime(now);
    if (logger.isDebugEnabled())
      logger.debug(getAgentIdentifier()+" Make the donuts in " + (nextAlarm - now) + "msec.");
    alarm = createAlarm(nextAlarm);
    getAlarmService().addAlarm(alarm);
  }
}
