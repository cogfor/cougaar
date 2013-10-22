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

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.log.Logging;

/**
 * A low-priority "heartbeat" function that prints a period
 * every few seconds when nothing else is happening.
 *
 * @property org.cougaar.core.agent.heartbeat
 * Unless disabled, the node will provide a heartbeat to the vm.
 *
 * @property org.cougaar.core.agent.idleInterval 
 * How long between idle detection and heartbeat cycles (prints '.');
 *
 * @property org.cougaar.core.agent.idle.verbose
 * If <em>true</em>, will print elapsed time (seconds) since
 * the agent's start every idle.interval millis.
 *
 * @property org.cougaar.core.agent.idle.verbose.interval=60000
 * The number of milliseconds between verbose idle reports.
 *
 * @property org.cougaar.core.agent
 * quiet Makes standard output as quiet as possible.  
 * If Heartbeat is running, will not print dots.
 */
public final class Heartbeat 
{

  private static int idleInterval = 5*1000;
  private static boolean idleVerbose = false; // don't be verbose
  private static long idleVerboseInterval = 60*1000L; // 1 minute
  private static long maxIdleInterval;

  static {
    idleInterval = SystemProperties.getInt("org.cougaar.core.agent.idleInterval", idleInterval);
    maxIdleInterval = (idleInterval+(idleInterval/10));
    idleVerbose = SystemProperties.getBoolean("org.cougaar.core.agent.idle.verbose", idleVerbose);
    idleVerboseInterval = SystemProperties.getInt("org.cougaar.core.agent.idle.verbose.interval",
                                                (int)idleVerboseInterval);
  }


  private long firstTime;
  private long lastVerboseTime;

  private static long lastHeartbeat = 0L;
  private static long idleTime = 0L;

  private Schedulable schedulable;

  /** Only node can construct a Heartbeat */
  Heartbeat() {
    firstTime = System.currentTimeMillis();
    lastVerboseTime = firstTime;
  }


  synchronized void start(ServiceBroker sb) {
    if (schedulable != null) throw new RuntimeException("Attempted to restart Heartbeat!");

    ThreadService tsvc = sb.getService(this, ThreadService.class, null);
    //Want this thread to be lowest priority, i.e. run only after
    // every thing else has run. With the thread lanes, the best we can do
    // is run in the default pool.
    schedulable = tsvc.getThread(this, new Beater(), "Heartbeat");
    schedulable.schedule(idleInterval);
    sb.releaseService(this, ThreadService.class, tsvc);
  }
  
  synchronized void stop() throws SecurityException {
    if (schedulable == null) throw new RuntimeException("Attempted to stop a stopped Heartbeat!");
    schedulable.cancel();
  }

  private class Beater implements Runnable {
    public void run() {
      // initialize the values
      firstTime = System.currentTimeMillis();
      lastVerboseTime = firstTime;

      showProgress(".");
      // if heartbeat actually gets to run at least every 5.5 seconds,
      // we'll consider the VM idle. 

      //TBD: The logic of this test assumes that this schedulable will
      // be run only after all other work has completed. This is not
      // the case with a thread service thread which does not have low
      // priority.  A better test would be to query the thread control
      // service for the "load average" There seems to be no client
      // for idle time, so we will leave the old logic in place as
      // documenation for when this functionality will be revisited
      long t = System.currentTimeMillis();
      if (lastHeartbeat!=0) {
          long delta = t-lastHeartbeat;
          if (delta <= maxIdleInterval) {
	      // we're pretty much idle
	      idleTime += delta;
          } else {
	      idleTime = 0;
          }
      }
      lastHeartbeat = t;
      
      if (idleVerbose) {
          long delta = t-lastVerboseTime;
          if (delta >= idleVerboseInterval) {
	      showProgress("("+Long.toString(((t-firstTime)+500)/1000)+")");
	      lastVerboseTime=t;
          }
      }
      schedulable.schedule(idleInterval);
    }
  }




  private static void showProgress(String p) {
    Logging.printDot(p);
  }

  /**
   * @return an estimate of how long in milliseconds the VM has been 
   * approximately idle.
   */
  public long getIdleTime() { 
    long delta = System.currentTimeMillis() - lastHeartbeat;
    if (delta <= maxIdleInterval) {
      return idleTime+delta;
    } else {
      return 0;
    }
  }

}

