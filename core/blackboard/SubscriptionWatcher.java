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

package org.cougaar.core.blackboard;

/**
 * A callback for {@link
 * org.cougaar.core.service.BlackboardService#registerInterest}
 * subscription or alarm activity.
 * <p>
 * On the {@link #signalNotify} callback, the watcher should schedule
 * itself to run in a separate thread, e.g. by using the {@link
 * org.cougaar.core.service.ThreadService}.
 * <p>
 * Most of this could likely be removed and replaced with a simple
 * {@link org.cougaar.core.thread.Schedulable#start}, since most
 * clients don't check the {@link #signalNotify} "event" and the
 * {@link Subscriber} has a {@link Subscriber#wasClientActivity}
 * method that's equivalent to {@link #clearSignal}.
 */
public class SubscriptionWatcher {

  // this class supports a wait/notify semaphore, but clients
  // are encouraged to rely on the "signalNotify" callback instead
  // of blocking.

  public final static int EXTERNAL = 1;
  public final static int INTERNAL = 2;
  public final static int CLIENT = 3;

  /** have the collections changed since we last looked? */
  protected boolean externalFlag = false;
  protected boolean internalFlag = false;
  protected boolean clientFlag = false;
  
  /**
   * Some blackboard activity has occured, so the watcher should
   * schedule a separate thread, open a transaction, check if its
   * subscriptions have changed, and finish the transaction. 
   */
  public synchronized void signalNotify(int event) {
    switch (event) {
    case EXTERNAL: externalFlag = true; break;
    case INTERNAL: internalFlag = true; break;
    case CLIENT: clientFlag = true; break;
    default: break;
    }
    notifyAll();
  }      

  /**
   * Wait for a signal to continue.  
   * @return true iff the wake signal is unconditional.
   */
  public synchronized boolean waitForSignal() {
    while (! test() ) {
      try {
        wait();
      } catch (InterruptedException ie) {}
    }
    return clearSignal();
  }

  public synchronized boolean clearSignal() {
    boolean retval = clientFlag || internalFlag;

    externalFlag = false;
    internalFlag = false;
    clientFlag = false;

    return retval;
  }

  /**
   * @return true IFF it is time to wake up.
   * by default, this will return true when any of 
   * externalFlag, internalFlag and clientFlag are true.
   */
  protected boolean test() {
    return (externalFlag || internalFlag || clientFlag);
    //return (externalFlag || clientFlag);
  }

}
