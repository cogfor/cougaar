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

package org.cougaar.core.agent.service.alarm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Implement a basic timer class (not to be confused with
 * {@link java.util.Timer}) that invokes Alarm "expire()" methods
 * when they are due.
 * <p>
 * The base class operated on System time, but subclasses may operate
 * on different scales.
 * <p>
 * Visible feedback may be controlled by standard logging for class:<pre>
 * org.cougaar.core.agent.service.alarm.Timer:
 * WARN also enables logging of when (real-time only) alarms are more than Epsilon millis late 
 * INFO also enables logging of when alarms take more than Epsilon millis to ring
 * DEBUG also enables reports of every alarm ringing.
 * </pre> 
 * <p> 
 * Subclasses may override the feedback printed.
 *
 * @property org.cougaar.core.agent.service.alarm.Timer.epsilon=10000 milliseconds
 * considered a relatively long time for alarm delivery.
 * @property org.cougaar.core.agent.service.alarm.Timer.useSchedulable=true Set to false
 * to use in-band delivery of alarm sounding rather than using a schedulable to wrap the
 * delivery.
 */
public abstract class Timer implements Runnable {
  protected final static Logger log = Logging.getLogger(Timer.class);

  protected static final long EPSILON = 
    SystemProperties.getLong(
        "org.cougaar.core.agent.service.alarm.Timer.epsilon",
        10*1000L);
  protected static final boolean USE_SCHEDULABLE = 
    SystemProperties.getBoolean(
        "org.cougaar.core.agent.service.alarm.Timer.useSchedulable",
        true);

  /** all alarms */
  // this could be optimized to use a heap
  private final ArrayList alarms = new ArrayList();

  /** Pending Periodic Alarms.  
   * PeriodicAlarms which have gone off but
   * need to be added back on.  These are collected and added
   * back in a second pass so that we don't get terrible behavior
   * if someone abuses a periodic alarm
   */
  // only modified in the run loop
  private final ArrayList ppas = new ArrayList();

  /** Pending Alarms.  
   * alarms which need to be rung, but we haven't gotten around to yet.
   */
  // only modified in the run loop thread
  private final ArrayList pas = new ArrayList();


  private static final Comparator COMPARATOR = new Comparator() {
      public int compare(Object a, Object b) {
        long ta = ((Alarm)a).getExpirationTime();
        long tb = ((Alarm)b).getExpirationTime();
        if (ta>tb) return 1;
        if (ta==tb) return 0;
        return -1;
      }};

  protected final Object sem = new Object();

  private Schedulable schedulable;
  private ThreadService threadService = null;

  public Timer() {}

  public void start(ThreadService tsvc) {
    threadService = tsvc;
    schedulable = tsvc.getThread(this, this, getName()); // lane?
    // should be nothing on the queue yet, so no need to start.
    // On the other hand, starting is harmless, since it will quit 
    // immediately if the queue is empty.
    // For now we don't do a "schedulable.start()"
    if (log.isDebugEnabled()) {
      log.debug("Started");
    } 
  }

  public void stop() {
    if (log.isDebugEnabled()) {
      log.debug("Stop timer");
    }
    synchronized (sem) {
      Iterator it = alarms.iterator();
      while (it.hasNext()) {
        Alarm alarm = (Alarm) it.next();
        if (alarm == null) {
          continue;
        }
        it.remove();
      }
    }
    //schedulable.cancel();
    //schedulable = null;
    //threadService = null;
  }

  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  public void addAlarm(Alarm alarm) {
    if (log.isDebugEnabled()) {
      log.debug("addAlarm("+alarm+")");
    }
    synchronized (sem) {
      insert(alarm);
    }
    requestRun();
  }

  public void cancelAlarm(Alarm alarm) {
    if (log.isDebugEnabled()) {
      log.debug("cancelAlarm("+alarm+")");
    }
    synchronized (sem) {
      alarms.remove(alarm);
    }
    requestRun();
  }

  protected double getRate() {
    return 1.0;
  }

  /**
   * Override this to specify time before next rate change. It is
   * always safe to underestimate.
   */
  protected long getMaxWait() {
    return 10000000000L;        // A long time
  }

  protected String getName() {
    return "Timer";
  }

  protected void requestRun() {
    schedulable.start();
  }

  protected void report(Alarm alarm) {
    if (log.isDebugEnabled()) {
      log.debug("Ringing "+alarm);
    }
  }

  // must be called within sync(sem) 
  private Alarm peekAlarm() {
    if (alarms.isEmpty())
      return null;
    else
      return (Alarm) alarms.get(0);
  }

  // must be called within sync(sem) 
  private Alarm nextAlarm() {
    if (alarms.isEmpty()) return null;
    Alarm top = (Alarm) alarms.get(0);
    if (top != null) 
      alarms.remove(0);
    if (alarms.isEmpty()) return null;
    return (Alarm) alarms.get(0);
  }

  // must be called only within a sync(sem)
  private void insert(Alarm alarm) {
    if (log.isDebugEnabled()) {
      log.debug("insert("+alarm+")");
    }
    boolean added = false;
    // find the right insertion point
    ListIterator i = alarms.listIterator(0);
    if (i.hasNext()) {
      while (i.hasNext()) {
        Alarm cur = (Alarm) i.next();
        // stop if the alarm is < the current insertion point
        if (COMPARATOR.compare(alarm, cur) < 0) {
          i.previous();         // back up one step
          i.add(alarm);         // add before cur
          added = true;
          break;               
        }
      }
    }
    if (!added) {
      // no elements were greater, add at end
      alarms.add(alarm);
    }
    if (log.isDetailEnabled()) {
      synchronized (sem) {
        log.detail("Alarms = "+alarms);
      }
    }
  }

  // only called by scheduler
  public void run() {
    if (log.isDetailEnabled()) {
      log.detail("run");
    }
    schedulable.cancelTimer(); // cancel any outstanding timed restarts
    while (true) {
      long time;
      synchronized (sem) {
        Alarm top = peekAlarm();

        if (top == null) {
          // no pending events?
          // new events will restart us
          return;
        }

        // figure out how long to wait
        { 
          long delta = top.getExpirationTime() - currentTimeMillis();
          double rate = getRate();
          long maxWait = getMaxWait();
          if (rate > 0.0) {
            delta = Math.min((long) (delta / rate), maxWait);
          } else {            // Time is standing still
            delta = maxWait;  // Wait until next significant change in timer
          }
          if (log.isDetailEnabled()) {
            log.detail("delta is "+delta+" for top "+top);
          }
          if (delta > 0) {
            if (delta < 100) delta=100; // min of .1 second wait time
            schedulable.schedule(delta);   // restart after delta ms
            return;
            // sem.wait(delta);
          }
        }

        // fire some alarms
        top = peekAlarm();
        time = currentTimeMillis();
        while (top != null && 
               time >= top.getExpirationTime()) {
          pas.add(top);
          top = nextAlarm();
        }

        if (log.isDetailEnabled()) {
          log.detail("pas is "+pas);
        }

      } // sync(sem)
      
      // now ring any outstanding alarms: outside the sync
      // just in case an alarm ringer tries setting another alarm!
      {
        int l = pas.size();
        for (int i = 0; i<l; i++) {
          Alarm top = (Alarm) pas.get(i);
          try {
            ring(top);
          } catch (Throwable e) {
            log.error("Alarm "+top+" generated Exception", e);
            // cancel error generating alarms to be certain.
            top.cancel();
          }
          
          // handle periodic alarms
          if (top instanceof PeriodicAlarm) {
            ppas.add(top);      // consider adding it back later
          }
        }
        pas.clear();
      }

      // back in sync, reset any periodic alarms
      synchronized (sem) {
        // reset periodic alarms
        int l = ppas.size();
        for (int i=0; i<l; i++) {
          PeriodicAlarm ps = (PeriodicAlarm) ppas.get(i);
          ps.reset(time);       // reset it
          if (!ps.hasExpired()) { // if it hasn't expired, add it back to the queue
            insert(ps);
          }
        }
        ppas.clear();
      } // sync(sem)
    } // infinite loop
  }

  private void ring(final Alarm alarm) {
    if (alarm.hasExpired()) {
      // already cancelled
      return; 
    }
    if (!USE_SCHEDULABLE && threadService == null) { 
      // ring in our thread
      reallyRing(alarm);
      return;
    }
    // ring in pooled thread
    Schedulable quasimodo = threadService.getThread(
        this,
        new Runnable() {
          public void run() {
            reallyRing(alarm);
          }
        },
       "Alarm Ringer");
    quasimodo.start();
  }

  private void reallyRing(Alarm alarm) {
    report(alarm);
    long dt = 0L;
    try {
      dt = System.currentTimeMillis(); // real start time
      alarm.expire();
      dt = System.currentTimeMillis() - dt; // real delta time
      //
      if (dt > EPSILON) {
        if (log.isWarnEnabled()) {
          log.warn("Alarm "+alarm+" blocked for "+dt+"ms while ringing");
        }
      }
    } finally {
      // see if the alarm has been evil and as has opened a transaction
      // but neglected to close it
      if (org.cougaar.core.blackboard.Subscriber.abortTransaction()) {
        log.error("Alarm "+alarm+" failed to close it's transaction");
      }
    }
  }
}
