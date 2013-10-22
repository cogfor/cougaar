/* Modified by cougaar
 * <copyright>
 *  Copyright 2006 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

/*
  File: Semaphore.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
   5Aug1998  dl               replaced int counters with longs
  24Aug1999  dl               release(n): screen arguments
*/

package org.cougaar.community.util;

/**
 * Copied from <code>EDU.oswego.cs.dl.util.concurrent.Semaphore</code> to
 * avoid the extra "sys/concurrent.jar" dependency.
 */
public class Semaphore {
  /** current number of available permits **/
  protected long permits_;

  /** 
   * Create a Semaphore with the given initial number of permits.
   * Using a seed of one makes the semaphore act as a mutual exclusion lock.
   * Negative seeds are also allowed, in which case no acquires will proceed
   * until the number of releases has pushed the number of permits past 0.
  **/
  public Semaphore(long initialPermits) {  permits_ = initialPermits; }


  /** Wait until a permit is available, and take one **/
  public void acquire() throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    synchronized(this) {
      try {
        while (permits_ <= 0) wait();
        --permits_;
      }
      catch (InterruptedException ex) {
        notify();
        throw ex;
      }
    }
  }

  /** Wait at most msecs millisconds for a permit. **/
  public boolean attempt(long msecs) throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();

    synchronized(this) {
      if (permits_ > 0) { 
        --permits_;
        return true;
      }
      else if (msecs <= 0)   
        return false;
      else {
        try {
          long startTime = System.currentTimeMillis();
          long waitTime = msecs;
          
          for (;;) {
            wait(waitTime);
            if (permits_ > 0) {
              --permits_;
              return true;
            }
            else { 
              waitTime = msecs - (System.currentTimeMillis() - startTime);
              if (waitTime <= 0) 
                return false;
            }
          }
        }
        catch(InterruptedException ex) { 
          notify();
          throw ex;
        }
      }
    }
  }

  /** Release a permit **/
  public synchronized void release() {
    ++permits_;
    notify();
  }


  /** 
   * Release N permits. <code>release(n)</code> is
   * equivalent in effect to:
   * <pre>
   *   for (int i = 0; i &lt; n; ++i) release();
   * </pre>
   * <p>
   * But may be more efficient in some semaphore implementations.
   * @exception IllegalArgumentException if n is negative.
   **/
  public synchronized void release(long n) {
    if (n < 0) throw new IllegalArgumentException("Negative argument");

    permits_ += n;
    for (long i = 0; i < n; ++i) notify();
  }

  /**
   * Return the current number of available permits.
   * Returns an accurate, but possibly unstable value,
   * that may change immediately after returning.
   **/
  public synchronized long permits() {
    return permits_;
  }
}
