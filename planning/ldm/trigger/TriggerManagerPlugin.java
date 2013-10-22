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


package org.cougaar.planning.ldm.trigger;

import java.util.Enumeration;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.plugin.legacy.Assessor;
import org.cougaar.planning.plugin.legacy.PluginDelegate;
import org.cougaar.planning.plugin.legacy.SimplifiedFatPlugin;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin manages triggers that have been created in a given
 * agent.  It sets up a subscription for all policies, and
 * subscribes to them. It then generates subscriptions for
 * predicate-based Monitors and sets up timers for time-based
 * Monitors. When the execute method is called, it Executes any
 * trigger that is ready to run.
 **/
public class TriggerManagerPlugin extends SimplifiedFatPlugin 
  implements Assessor 
{

  /** Will never wait less than this many msecs for time-based alarms. **/
  private static long minWaitTime = 1000L;
  
  private static int verbosity = 0;
  static {
    verbosity = Integer.getInteger("org.cougaar.trigger.verbosity",0).intValue();
    minWaitTime = Long.getLong("org.cougaar.trigger.minWait",1000L).longValue();
  }

  private IncrementalSubscription triggerSub;
  private Alarm alarm = null;

  public void setupSubscriptions() {
    getBlackboardService().setShouldBePersisted(false);
    // Subscribe to all triggers, to maintain list
    triggerSub = (IncrementalSubscription)subscribe( triggerPred );
  }

  public void execute() {
    // Check if the trigger subscription has fired
    if (triggerSub.hasChanged()) {
      // initialize any new triggers
      Enumeration newtriggers = triggerSub.getAddedList();
      while (newtriggers.hasMoreElements()) {
        Trigger anewtrigger = (Trigger) newtriggers.nextElement();
        TriggerMonitor newmonitor = anewtrigger.getMonitor();

        // If its a TriggerPredicateBasedMonitor, establish its subscription
        if (newmonitor instanceof TriggerPredicateBasedMonitor) {
          TriggerPredicateBasedMonitor mon = 
            (TriggerPredicateBasedMonitor) newmonitor;
          UnaryPredicate anewpred = mon.getPredicate();
          IncrementalSubscription anewsub =
           (IncrementalSubscription) subscribe(anewpred);
          mon.EstablishSubscription(anewsub);
        }
        if (verbosity>1) System.err.println("Added trigger "+anewtrigger+" at "+getMessageAddress());
      }

      Enumeration removedtriggers = triggerSub.getRemovedList();
      while (removedtriggers.hasMoreElements()) {
        Trigger rtrigger = (Trigger) removedtriggers.nextElement();
        TriggerMonitor rmonitor = rtrigger.getMonitor();
        if (rmonitor instanceof TriggerPredicateBasedMonitor) {
          Subscription unsub = 
            ((TriggerPredicateBasedMonitor)rmonitor).getSubscription();
          unsubscribe(unsub);
          if (verbosity>1) System.err.println("TriggerManager cleaning up "+rtrigger);
        }
      }  
    }

    // run all that need running
    long waittime = runTriggers();

    if (waittime < 0) {
      // nothing needs wake service. 
      if (alarm != null) {
        alarm.cancel();         // cancel the outstanding alarm
        alarm = null;
      }
    } else {
      if (waittime < minWaitTime) waittime=minWaitTime;
      if (alarm != null && !alarm.hasExpired()) { // have an alarm still waiting?
        if (verbosity>1) System.err.println("TriggerManager Cancelling "+alarm);
        alarm.cancel();         // cancel it
      }

      if (verbosity>1) System.err.println("TriggerManager sleeping for "+waittime);
      alarm = wakeAfterRealTime(waittime);
    }
  }

  /** check all likely triggers, running those that are ready.
   * returns the next alarm interval to wait for time-based triggers.
   * @return msec to wait for time-based triggers or -1 if there are none.
   * Any time-based triggers which should already have expired will be reset to 0.
   */
  private long runTriggers() {
    PluginDelegate delegate = getDelegate();
    long minwait = Long.MAX_VALUE;
    boolean anywait = false;

    for (Enumeration rlit = triggers(); rlit.hasMoreElements();) {
      Trigger trig = (Trigger)rlit.nextElement();

      if (trig.ReadyToRun(delegate)) {
        if (verbosity>1) System.err.println("TriggerManager Running "+trig);
        trig.Execute(delegate);
      }

      // check times - we do this in the loop to prevent consuming all
      // cpu time servicing short alarms.
      TriggerMonitor mon = trig.getMonitor();
      if (mon instanceof TriggerTimeBasedMonitor) {
        TriggerTimeBasedMonitor tmon = (TriggerTimeBasedMonitor) mon;
        long left = tmon.getRemainingTime();
        if (verbosity>1) System.err.println("Trigger "+trig+" has "+left+"msec left");
        if (left<0) left=0;
        if (left < minwait) minwait=left;
        anywait=true;
      }
    }
    return anywait?minwait:-1;
  }

  private Enumeration triggers() {
    return triggerSub.elements();
  }

  private static UnaryPredicate triggerPred = 
    new UnaryPredicate() {
        public boolean execute( Object o ) {
          return ( o instanceof Trigger );
        }};
  
}
