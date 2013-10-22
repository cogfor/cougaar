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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.Timer;

/**
 * A base class for timer service implementations.
 */
public abstract class TimeServiceBase {

  private final String name;

  /** map of &lt;Alarm,AlarmWrapper&gt; */
  private final Map alarms = new HashMap(11);

  public TimeServiceBase(Object requestor) {
    name = (this.getClass().getName())+" for "+(requestor.toString());
  }

  @Override
public String toString() { return name; }

  /**
   * @return a {@link Timer}, not a {@link java.util.Timer}!
   */
  protected abstract Timer getTimer();

  public long currentTimeMillis() {
    return getTimer().currentTimeMillis();
  }

  public void addAlarm(Alarm alarm) {
    getTimer().addAlarm(wrap(alarm));
  }

  public void cancelAlarm(Alarm alarm) {
    Alarm w = find(alarm);
    if (w != null) {
      getTimer().cancelAlarm(w);
    }
  }

  /** clear out any saved state, e.g. remove outstanding alarms */
  protected void clear() {
    synchronized (alarms) {
      for (Iterator it = alarms.values().iterator(); it.hasNext(); ) {
        Alarm w = (Alarm) it.next();
        // should the Alarms themselves be cancelled?  I'm guessing not
        getTimer().cancelAlarm(w);
      }
    }
  }

  /** create an AlarmWrapper around an Alarm, and remember it */
  protected Alarm wrap(Alarm a) {
    Alarm w = new AlarmWrapper(a);
    synchronized (alarms) {
      alarms.put(a,w);
    }
    return w;
  }

  /** drop an Alarm (not an AlarmWrapper) from the remembered alarms */
  protected void forget(Alarm a) {
    synchronized (alarms) {
      alarms.remove(a);
    }
  }

  /** Find the remembered AlarmWrapper matching a given Alarm */
  protected AlarmWrapper find(Alarm a) {
    synchronized (alarms) {
      return (AlarmWrapper) alarms.get(a);
    }
  }

  protected class AlarmWrapper implements Alarm {
    private Alarm alarm;
    AlarmWrapper(Alarm alarm) {
      this.alarm = alarm;
    }

    public long getExpirationTime() {
      return alarm.getExpirationTime();
    }

    public boolean hasExpired() {
      return alarm.hasExpired();
    }

    // called by Timer to notify that the alarm has rung
    public void expire() {
      forget(alarm);
      alarm.expire();
    }

    // called by client to notify that the alarm should be cancelled
    // usually just sets hasExpired
    public boolean cancel() {
      forget(alarm);
      return alarm.cancel();
    }

    @Override
   public String toString() {
      return 
        "AlarmWrapper("+alarm+") of "+
        (TimeServiceBase.this.toString());
    }
  }
}
