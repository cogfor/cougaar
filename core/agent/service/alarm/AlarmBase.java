/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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
 * A standard {@link Alarm} base class.
 *
 * @see #onExpire See the "onExpire()" method.
 */
public abstract class AlarmBase implements Alarm {
  private final long expirationTime;
  private boolean expired = false;

  /** Construct an alarm to expire */
  public AlarmBase(long time) {
    expirationTime = time;
  }

  public final long getExpirationTime() {
    return expirationTime;
  }

  /**
   * This callback method is invoked when the alarm expires.
   *
   * @see org.cougaar.core.blackboard.TodoSubscription blackboard-based plugins
   * should use a TodoSubscription to queue the expired alarm for processing
   * in the "execute()" thread, as opposed to the AlarmService callback thread.
   */
  public abstract void onExpire();

  public final void expire() {
    synchronized (this) {
      if (expired) return;
      expired = true;
    }
    onExpire();
  }

  public final synchronized boolean hasExpired() {
    return expired;
  }

  public final synchronized boolean cancel() {
    boolean was = expired;
    expired = true;
    return was;
  }

  @Override
public String toString() {
    return "(alarm expired="+hasExpired()+" expireTime="+expirationTime+")";
  }
}
