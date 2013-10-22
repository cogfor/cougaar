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

/**
 * Interface for an object telling the AlarmService to wake a client
 * in the future.
 *
 * @see org.cougaar.core.service.AlarmService
 */
public interface Alarm {
  /**
   * @return absolute time (in milliseconds) that the Alarm should
   * go off.  This value must be implemented as a fixed value.
   */
  long getExpirationTime();
  
  /**
   * Called by the agent's alarm time when clock-time &gt;= getExpirationTime().
   * <p> 
   * The system will attempt to Expire the Alarm as soon as possible on 
   * or after the ExpirationTime, but cannot guarantee any specific
   * maximum latency.
   * NOTE: this will be called in the thread of the cluster clock.  
   * Implementations should make certain that this code does not block
   * for a significant length of time.
   * If the alarm has been canceled, this should be a no-op.
   */
  void expire();

  /**
   * @return true IFF the alarm has rung (expired) or was canceled.
   */
  boolean hasExpired();

  /**
   * This method can be called by a client to cancel the alarm.
   * May or may not remove the alarm from the queue, but should
   * prevent expire from doing anything.
   * @return false IF the the alarm has already expired or was already canceled.
   */
  boolean cancel();
}
