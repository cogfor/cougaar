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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A Timer for execution time (artificial "execution" or "scenario"
 * time), which is a modifiable offset from real time with a rate
 * multiplier.
 * <p>
 * Control the advancement of Execution time. Execution time is a
 * monotonically increasing value whose rate can be controlled through
 * the API implemented in this class. Execution time is nominally the
 * same everywhere in the society. Small descrepancies may exist when
 * the parameters of the exeuction time advancement are altered.
 * Execution time is represented by a rate of advancement and an
 * offset. The rate of advancment may be zero (time stands still) or
 * any positive value. Excessively high rates may lead to anomalous
 * behavior.
 * <p>
 * The equation of time is Te = Ts * Km + Ko where:<pre>
 *  Te is Execution time
 *  Ts is system time (System.currentTimeMillis())
 *  Km is the (positive) rate of advancement
 *  Ko is the offset
 * </pre>
 * <p>
 * The System.currentTimeMillis() is presumed to be in sync in all
 * agents within a few milliseconds by using NTP (Network Time
 * Protocol). The maximum offset of the system clocks limits the
 * maximum value that Km can have without introducing serious
 * anomalies.
 * <p>
 * It is necessary for execution time to be monotonic. Monotonicity
 * must be achieved even in the face of delays in propagating changes
 * in the parameters of execution time advancement. The message that
 * is used to alter the execution time advancement parameters
 * specifically allows for the change to occur at some future time and
 * it is expected that that will be the norm, but if the message
 * transmission is delayed or if insufficient time is allowed, the
 * equation of time must be altered to assure monotonicity.
 * Furthermore, a succession of time advancement must ultimately
 * result in all agents using the same time parameters. This is
 * achieved by defining a sorting order on the time parameters that
 * establishes a dominance relation between all such parameter sets.
 * <p>
 * Time change messages contain:<pre>
 *   New rate
 *   New offset
 *   Changeover (real) time
 * </pre>
 * <p> 
 * The changeover time is the first order sorting factor. If the changeover
 * times are equal, the parameters yielding the highest execution time value
 * at the changeover time dominate. If the execution times at the
 * changeover time are equal, the change with the highest offset
 * dominates.  
 * <p> 
 * The changeover time in this message is used as an offset from the
 * current system time --- at current system time + changeover the new parameters
 * will take effect.  Normally, though, an absolute real time sync point should be
 * specified to ensure that all nodes change parameters at the same time (again,
 * assuming all host clocks are synchronized using NTP).  An absolute real time
 * change time can be specified using the ExecutionTimer.Parameters.create method,
 * and setting changeIsAbsolute = true.
 * <p>
 * We want the execution timer to have the ability to initialize the natural-time
 * clock to a particular time. This is
 * problematic since we represent execution time as an offset from
 * system time. Computing that offset consistently across all
 * agents means that we have to compute a time (in the past) at
 * which the parameters became effective and then compute the offset
 * relative to that.
 * <p>
 * If org.cougaar.core.society.startTime is provided, we can 
 * synchronize from a common baseline point to reasonable accuracy.
 * Otherwise, there is no way for all agents to reliably
 * compute the same value, so we'll assume simultaneous starts.
 * 
 * @property org.cougaar.core.agent.startTime The date to use as the
 * start of execution for demonstration purposes.  Accepts date/time
 * in the form of <em>MM/dd/yyy_H:mm:ss</em>, <em>MM/dd/yyy H:mm:ss</em>,
 * or <em>MM/dd/yyy</em>.  The time sequence is optional and defaults to
 * midnight on the specified date. 
 * agentStartTime must be in GMT.  Note that if society.startTime is
 * not fully specified, a multi-node society can have significant 
 * natural-time clock skew across the members.
 *
 * @property org.cougaar.core.society.startTime The real date-time stamp
 * when the society was started.  If supplied, can be used to synchronize
 * the execution times of nodes which were started at different real times.
 * society.startTime must be in GMT and ought to be generally
 * slightly in the past.  Format example: "09/12/2003 13:00:00"
 *
 * @property org.cougaar.core.society.timeOffset Specify an offset (in milliseconds)
 * from real time to use as execution time.  This is an alternative to
 * using agent.startTime and society.startTime (if timeOffset is specified, then
 * these other properties are ignored).  Typical usage would be to specify
 * the execution-time offset when (re)starting a node to match the other nodes
 * in the society.
 */
public class ExecutionTimer extends Timer {
  private static final Logger logger = Logging.getLogger("org.cougaar.core.agent.service.alarm.ExecutionTimer");

  public static final long DEFAULT_CHANGE_DELAY = 10000L;

  public static class Parameters implements Comparable, java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final long theOffset;             // The offset (Ko)
    public final double theRate;             // The advancement rate (Km)
    public final long theChangeTime;         // The changeover time

    public Parameters(double aRate, long anOffset, long aChangeTime) {
      theRate = aRate;
      theOffset = anOffset;
      theChangeTime = aChangeTime;
    }

    public long computeTime(long now) {
      return (long) (now * theRate) + theOffset;
    }

    public int compareTo(Object o) {
      Parameters other = (Parameters) o;
      long diff = this.theChangeTime - other.theChangeTime;
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      diff = this.computeTime(this.theChangeTime) - other.computeTime(other.theChangeTime);
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      diff = this.theOffset - other.theOffset;
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      return 0;
    }

    @Override
   public String toString() {
      return ("Time = "
              + new Date(computeTime(theChangeTime)).toString()
              + "*"
              + theRate
              + "@"
              + new Date(theChangeTime).toString());
    }
  }

  /**
   * Description of a Parameters change (relative to some implicit Parameters)
   */
  public static class Change {
    public final long theOffsetDelta;       // Step in the offset
    public final double theRate;             // New rate is absolute
    public final long theChangeTimeDelta; // Change time relative to some other Parameters
    public Change(double aRate, long anOffsetDelta, long aChangeTimeDelta) {
      theOffsetDelta = anOffsetDelta;
      theRate = aRate;
      theChangeTimeDelta = aChangeTimeDelta;
    }
  }

  /**
   * An array of Parameters. The array is sorted in the order in which
   * they are to be applied. The 0-th element is the current setting.
   */
  Parameters[] theParameters = new Parameters[] {
    new Parameters(1.0, 0L, 0L),
    null,                       // Allow up to five new parameters
    null,
    null,
    null,
  };
  private int theParameterCount = 1;
  
  long theCurrentExecutionTime;    // This assures monotonicity

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter.
   */
  public Parameters create(long millis,
                                        boolean millisIsAbsolute,
                                        double newRate)
  {
    synchronized (sem) {
      return create(millis, millisIsAbsolute, newRate, false, DEFAULT_CHANGE_DELAY);
    }
  }

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter. The new rate is 0.0 if running
   * is false, the current rate if running is true and the current
   * rate is greater than 0.0 or 1.0 if running is true and the
   * current rate is stopped.
   */
  public Parameters create(long millis,
                           boolean millisIsAbsolute,
                           double newRate,
                           boolean forceRunning)
  {
    synchronized (sem) {
      return create(millis, millisIsAbsolute, newRate, forceRunning, DEFAULT_CHANGE_DELAY);
    }
  }

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter. The new rate is 0.0 if running
   * is false, the current rate if running is true and the current
   * rate is greater than 0.0 or 1.0 if running is true and the
   * current rate is stopped.
   * @deprecated Use the version that allows specifying absolute change time instead
   */
  public Parameters create(long millis,
                           boolean millisIsAbsolute,
                           double newRate,
                           boolean forceRunning,
                           long changeDelay)
  {
    synchronized (sem) {
      long changeTime = getNow() + changeDelay;
      return create(millis, millisIsAbsolute, newRate, forceRunning, changeTime, theParameters[0]);
    }
  }

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter. The new rate is 0.0 if running
   * is false, the current rate if running is true and the current
   * rate is greater than 0.0 or 1.0 if running is true and the
   * current rate is stopped.  
   * The new parameters can go into effect at a time relative to
   * the current system time (changeIsAbsolute == false), or at a
   * specific system time (changeIsAbsolute == true)
   */
  public Parameters create(long millis,
                           boolean millisIsAbsolute,
                           double newRate,
                           boolean forceRunning,
                           long changeTime,
			   boolean changeIsAbsolute)
  {
    synchronized (sem) {
      if (!changeIsAbsolute)
        changeTime = getNow() + changeTime;
      return create(millis, millisIsAbsolute, newRate, forceRunning, changeTime, theParameters[0]);
    }
  }

  private Parameters create(long millis, boolean millisIsAbsolute,
                            double newRate, boolean forceRunning,
                            long changeTime,
                            Parameters relativeTo)
  {
    long valueAtChangeTime = relativeTo.computeTime(changeTime);
    if (Double.isNaN(newRate)) newRate = relativeTo.theRate;
    if (Double.isInfinite(newRate)) {
      throw new IllegalArgumentException("Illegal infinite rate");
    }
    if (newRate < 0.0) {
      throw new IllegalArgumentException("Illegal negative rate: " + newRate);
    }
    if (forceRunning) {
      if (newRate == 0.0) {
        newRate = relativeTo.theRate;
      }
      if (newRate == 0.0) {
        newRate = 1.0;
      }
    }
    if (millisIsAbsolute) {
      millis = millis - valueAtChangeTime;
    }
    if (millis < 0L) {
      throw new IllegalArgumentException("Illegal negative advancement:" + millis);
    }
    long newOffset = valueAtChangeTime + millis - (long) (changeTime * newRate);
    return new Parameters(newRate, newOffset, changeTime);
  }

  /**
   * Creates a series of changes. The first change is relative to the
   * current Parameters, the subsequent changes are relative to the
   * previous change.
   */
  public Parameters[] create(Change[] changes) {
    synchronized (sem) {
      Parameters[] result = new Parameters[changes.length];
      Parameters prev = theParameters[0];
      long changeTime = getNow();
      for (int i = 0; i < changes.length; i++) {
        Change change = changes[i];
        if (change.theChangeTimeDelta <= 0.0) {
          throw new IllegalArgumentException("Illegal non-positive change time delta: "
                                             + change.theChangeTimeDelta);
        }
        changeTime += change.theChangeTimeDelta;
        result[i] = create(change.theOffsetDelta, false,
                           change.theRate, false,
                           changeTime, prev);
        prev = result[i];
      }
      return result;
    }
  }

  /**
   * Get the current real time and insure that the current parameters
   * are compatible with the real time being returned. The pending
   * parameters become current if their time of applicability has been
   * reached.
   */
  private long getNow() {
    long now = System.currentTimeMillis();
    while (theParameterCount > 1 && theParameters[1].theChangeTime <= now) {
      System.arraycopy(theParameters, 1, theParameters, 0, --theParameterCount);
      theParameters[theParameterCount] = null;
    }
    return now;
  }

  /**
   * Get the current execution time in millis.
   */
  @Override
public long currentTimeMillis() {
    synchronized (sem) {
      long now = getNow();
      long newTime = theParameters[0].computeTime(now);
      if (newTime > theCurrentExecutionTime) {
        theCurrentExecutionTime = newTime; // Only advance time, never decreases
      }
      return theCurrentExecutionTime;
    }
  }

  /**
   * Insert new Parameters into theParameters. If the new parameters
   * apply before the last element of theParameters, ignore them.
   * Otherwise, append the new parameters overwriting the last parameters if necessary.
   */
  public void setParameters(Parameters parameters) {
    synchronized (sem) {
      getNow();                   // Bring parameters up-to-now
      if (parameters.compareTo(theParameters[theParameterCount - 1]) > 0) {
        if (theParameterCount < theParameters.length) {
          if (logger.isInfoEnabled()) {
            logger.info("Setting parameters " + theParameterCount + " to " + parameters);
          }
          theParameters[theParameterCount++] = parameters;
        } else {
          if (logger.isInfoEnabled()) {
            logger.info("Setting parameters " + (theParameterCount-1) + " to " + parameters);
          }
          theParameters[theParameterCount - 1] = parameters;
        }
      }
    }
    requestRun();
  }

  @Override
protected long getMaxWait() {
    if (theParameterCount > 1) {
      return theParameters[1].theChangeTime - System.currentTimeMillis();
    } else {
      return 100000000L;
    }
  }

  @Override
public double getRate() {
    synchronized (sem) {
      getNow();                   // Bring parameters up-to-now
      return theParameters[0].theRate;
    }
  }

  /**
   */
  public ExecutionTimer() {
    long offset = computeInitialOffset();
    if (offset != 0L) {
      if (logger.isWarnEnabled()) {
        logger.warn("Starting Time set to "+new Date(System.currentTimeMillis()+offset)+" offset="+offset+"ms");
      }
      theParameters[0] = new Parameters(1.0, offset, 0L);
    }
  }

  private long computeInitialOffset() {
    long offset = 0L;

    // society.timeOffset wins if specified
    {
      offset =  SystemProperties.getLong("org.cougaar.core.society.timeOffset", DATE_ERROR);
      if (offset != DATE_ERROR) {
        if (logger.isInfoEnabled()) {
          logger.info("Exact time set by society.timeOffset ("+offset+")");
        }
        return offset;
      }
    }
    offset = 0L; // reset so not == DATE_ERROR
    
    long now = System.currentTimeMillis();
    ParsedPropertyDate adt = new ParsedPropertyDate("org.cougaar.core.agent.startTime");
    ParsedPropertyDate sdt = new ParsedPropertyDate("org.cougaar.core.society.startTime");

    long target = adt.time;
    if (target == DATE_ERROR) {
      if (adt.date != DATE_ERROR) {
        if (logger.isWarnEnabled()) {
          logger.warn("Inexact agent.startTime specified:  Will default to midnight.");
        }
        target = adt.date;
      }
    }
    
    if (target != DATE_ERROR) { // fully-specified agent start time?
      if (sdt.time != DATE_ERROR) { // fully-specified society start time?
        // then we can compute exact offset
        offset = target - sdt.time;
        if (logger.isInfoEnabled()) {
          logger.info("Exact time set by agent-society times ("+offset+")");
        }
      } else {
        if (sdt.date != DATE_ERROR) {
          // useless: only partially-specified society start!
          logger.error("Ignoring partially-specified society.startTime "+new Date(sdt.date));
          // fall through...
        }
        
        // no useful society.startTime: accept the skew, but complain
        offset = target - now;
        if (logger.isWarnEnabled()) { // check in case someone turns it off
          logger.warn("Multi-node societies will have execution-time clock skew: Set org.cougaar.core.society.startTime or society.timeOffset to avoid this problem.");
        }
      }
    }
    return offset;
  }

  private static final long DATE_ERROR = Long.MIN_VALUE;

  private static class ParsedPropertyDate {
    String name;
    String value;
    long time = DATE_ERROR;
    long date = DATE_ERROR;
    ParsedPropertyDate(String propertyName) {
      this.name = propertyName;
      value = SystemProperties.getProperty(name);

      if (value != null) {
        try {
          DateFormat f;
          try {
            // try full date with "_" separator
            f = (new SimpleDateFormat("MM/dd/yyy_H:mm:ss"));
            time = f.parse(value).getTime();
          } catch (ParseException e1) {
            // try full date with " " separator
            f = (new SimpleDateFormat("MM/dd/yyy H:mm:ss"));
            time = f.parse(value).getTime();
          }
          // get midnight of specified date
          Calendar c = f.getCalendar();
          c.setTimeInMillis(time);
          c.set(Calendar.HOUR, 0);
          c.set(Calendar.MINUTE, 0);
          c.set(Calendar.SECOND, 0);
          c.set(Calendar.MILLISECOND, 0);
          date = c.getTimeInMillis();
        } catch (ParseException e) { 
          // try with just the date
          try {
            DateFormat f = (new SimpleDateFormat("MM/dd/yyy"));
            time = f.parse(value).getTime();
          } catch (ParseException e1) {
	    if (logger.isDebugEnabled())
	      logger.debug("Failed to parse property " + propertyName + " as date+time or just time: " + value, e1);
	  }
        }
      }
    }
  }

  @Override
protected String getName() {
    return "ExecutionTimer";
  }

  /* /////////////////////////////////////////////////////// 

  // point test 

  public static void main(String args[]) {
    // create a timer
    ExecutionTimer timer = new ExecutionTimer();
    timer.start();

    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    // test running advance
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000+30*1000)); // 60min+30sec
    timer.addAlarm(timer.createTestAlarm(30*60*1000)); // 30 min
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec (again)
    timer.sleep(15*1000);      // wait 10 seconds      
    System.err.println("advancing running time 60 minutes");
    timer.advanceRunningOffset(60*60*1000);
    timer.sleep(20*1000);      // wait 20sec
    System.err.println("done waiting for running time.");

    // stopped tests
    System.err.println("Trying stopped tests:");
    long t = timer.currentTimeMillis()+10*1000;
    timer.advanceStoppedTime(t);
    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(30*60*1000)); // 30 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000+30*1000)); // 60min+30sec
    timer.sleep(15*1000);      // wait 10 seconds      
    System.err.println("advancing stopped time 5 seconds");
    timer.advanceStoppedTime(t+5*1000);
    timer.sleep(1*1000);      // sleep a second
    System.err.println("advancing stopped time to 10 seconds");
    timer.advanceStoppedTime(t+10*1000);
    timer.sleep(1*1000);      // sleep a second
    System.err.println("advancing stopped time to 60 minutes");
    timer.advanceStoppedTime(t+60*60*1000);
    timer.sleep(1*1000);      // wait 20sec
    System.err.println("starting clock");
    timer.startRunning();
    timer.sleep(20*1000);      // wait 20sec
    System.err.println("done waiting for running time.");


    System.exit(0);
  }

  public void sleep(long millis) {
    try {
      synchronized(this) {
        this.wait(millis);
      }
    } catch (InterruptedException ie) {}
  }
    

  Alarm createTestAlarm(long delta) {
    return new TestAlarm(delta);
  }
  private class TestAlarm implements Alarm {
    long exp;
    public TestAlarm(long delta) { this.exp = currentTimeMillis()+delta; }
    public long getExpirationTime() {return exp;}
    public void expire() { System.err.println("Alarm "+exp+" expired.");}
    public String toString() { return "<"+exp+">";}
    public boolean cancel() {}  // doesn't support cancel
  }

  /* */
}
