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

package org.cougaar.core.service;

import java.util.Map;
import java.util.Set;

import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service controls the execution time in {@link
 * AlarmService#currentTimeMillis} and {@link
 * AlarmService#addAlarm}.
 * <p>
 * Although the standard implementation for "setSocietyTime"
 * uses a node-level {@link
 * org.cougaar.core.node.service.NaturalTimeService}
 * and "list all nodes" naming service targets list, some effort
 * has been made to allow separate execution times both between
 * nodes and in the agents within nodes.
 */
public interface DemoControlService extends Service {

  /**
   * Get the local node's execution rate.
   */
  double getExecutionRate();

  /**
   * Schedule a thread to set the local agent's time at
   * real-time <i>changeTime</i> to an execution-time of
   * <i>changeTime + offset</i> and <i>rate</i>.
   */
  void setLocalTime(long offset, double rate, long changeTime);

  /**
   * Backwards-compatible variations of {@link
   * #setLocalTime(long,double,long)}.
   * <p> 
   * These methods use <tt>ExecutionTimer.create</tt> to create an
   * <tt>ExecutionTimer.Change</tt>, which may mangle the timestamps
   * when converting a "time" to an "offset".
   *
   * @see #setLocalTime(long,double,long) 
   */
  void setNodeTime(long time, double rate, long changeTime);
  void advanceNodeTime(long period, double rate);
  void setNodeTime(long time, double rate);

  /**
   * Non-blocking method to set the node time at the specified nodes.
   *
   * @param offset offset of real-time at the changeTime
   * @param rate time rate multiplier (e.g. 2 seconds per second)
   * @param changeTime real-time when the change should take place,
   * which should allow enough time for messaging.
   * @param cb optional callback to monitor progress
   * @return true if completed in this thread, which only happens
   * if the targets list is empty or just the local node, otherwise
   * the callback is required to monitor the asynchronous progress.
   */
  boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Set targets,
      Callback cb);

  /**
   * Mostly non-blocking setSocietyTime to all nodes listed in the
   * naming service, but the naming service lookup could block or
   * return a partial list.
   *
   * @see #setSocietyTime(long,double,long,Set,DemoControlService.Callback)
   * @see org.cougaar.core.wp.ListAllNodes 
   */
  boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Callback cb);

  /**
   * Blocking setSocietyTime variations with timeouts.
   * <p>
   * These are identical to the above Callback variations
   * with a simple Callback that blocks on an
   * "Object.wait(timeout)".
   * <p>
   * Callers should pass a timeout that's &lt;= to the
   * changeTime, since usually you'd want to stop blocking
   * if a target was overly slow or unreachable.
   *
   * @param timeout time to wait in milliseconds, or zero
   * to block forever -- usually changeTime makes sense. 
   * @return true if all advances completed before the timeout,
   * otherwise false. 
   * @see #setSocietyTime(long,double,long,Set,DemoControlService.Callback)
   */
  boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Set targets,
      long timeout);
  /**
   * @see #setSocietyTime(long,double,long,DemoControlService.Callback)
   */ 
  boolean setSocietyTime(
      long offset, double rate, long changeTime,
      long timeout);

  /**
   * A blocking setSocietyTime that uses the changeTime
   * as the timeout -- this is the basis for all the other
   * non-Callback/timeout setSocietyTime variations.
   *
   * @see #setSocietyTime(long,double,long,Set,DemoControlService.Callback)
   */
  boolean setSocietyTime(
      long offset, double rate, long changeTime);

  /**
   * Backwards-compatible blocking variations of {@link
   * #setSocietyTime(long,double,long)}.
   * <p>
   * For legacy reasons these return "void" instead of a "boolean".
   * Also, they use <tt>ExecutionTimer.create</tt> to create an
   * <tt>ExecutionTimer.Change</tt>, which may mangle the timestamps
   * when converting a "time" to an "offset".
   *
   * @see #setSocietyTime(long,double,long,Set,DemoControlService.Callback)
   */
  void setSocietyTime(long time);
  void setSocietyTime(long time, boolean forceRunning);
  void setSocietyTimeRate(double rate);
  void advanceSocietyTime(long period);
  void advanceSocietyTime(long period, boolean forceRunning);
  void advanceSocietyTime(long period, double rate);
  void advanceSocietyTime(ExecutionTimer.Change[] changes);

  /**
   * An optional {@link
   * #setSocietyTime(long,double,long,Set,DemoControlService.Callback)}
   * progress monitor supplied by the service user.
   */ 
  public interface Callback {
    /**
     * On the <tt>setSocietyTime</tt> call, the Set of targets
     * that will be advanced.
     */
    public void sendingTimeAdvanceTo(Set addrs);
    /**
     * An "ack" from a target, with the round-trip-time.
     */
    public void updatedTime(MessageAddress addr, long rtt);
    /**
     * All targets have been advanced. 
     */
    public void completed(Map addrsToRTT);
  }
}
