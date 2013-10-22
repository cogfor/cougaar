/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.thread;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * The simplest thread service implementation, which runs its {@link
 * Schedulable}s serially, uses a small Collection of
 * SerialThreadRunners to do that work.  Each uses its own native Java
 * Thread.  The {@link Schedulable}s are held in order in a {@link
 * SerialThreadQueue}.
 */
final class SerialThreadRunner
{
  private Thread thread;

  private TrivialSchedulable current;

  private SerialThreadQueue queue;

  /** Boolean value used to notify the Body thread when
    * the SerialThreadRunner is unloaded. */
  private boolean isRunning;
  
  private Logger logger = Logging.getLogger(getClass().getName());

    SerialThreadRunner(SerialThreadQueue queue) 
    {
    this.queue = queue;
    thread = new Thread(new Body(), "Serial Thread Runner");
    thread.setDaemon(true);
    isRunning = true;
    thread.start();
  }


    Thread getThread()
    {
    return thread;
  }

    int iterateOverThreads(ThreadStatusService.Body body)
    {
    TrivialSchedulable sched = current;
    if (sched != null) {
      try {
        body.run("root", sched);
      } catch (Throwable t) {
        logger.error("ThreadStatusService error in body", t);
        return 0;
      }
    }
    return 1;
  }


    private void dequeue() 
    {
    while (true) {
      synchronized (queue.getLock()) {
        current = queue.next();
      }
	    if (current == null) return;

      current.setState(CougaarThread.THREAD_RUNNING);
      current.getRunnable().run();
      current.thread_stop(); // sets the state to DORMANT
      current = null;
    }
  }

    private class Body implements Runnable 
    {
	public void run() 
	{
      Object lock = queue.getLock();
      // Loop until the SerialThreadRunner is unloaded.
      while (isRunning) {
        dequeue();
        synchronized (lock) {
          while (queue.isEmpty()) {
            try {
              lock.wait();
              break;
            } catch (InterruptedException ex) {
            }
          }
        }
      }
    }
  }

  /**
   * Gracefully shuts down this thread.
   */
  protected void stop() {
    isRunning = false;
    Object lock = queue.getLock();
    synchronized(lock) {
      lock.notify();
    }
    thread = null;
    queue = null;
  }

}
