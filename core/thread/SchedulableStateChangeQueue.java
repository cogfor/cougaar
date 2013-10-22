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

package org.cougaar.core.thread;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.CircularQueue;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

final class SchedulableStateChangeQueue extends Thread {

    // At least one thread must be a non-daemon thread, otherwise the JVM
    // will exit.  We'll mark this thread as our non-daemon "keep-alive".
    private static final boolean DAEMON =
        SystemProperties.getBoolean("org.cougaar.core.thread.daemon");

    private static SchedulableStateChangeQueue singleton;

    static void startThread()  {
	singleton = new SchedulableStateChangeQueue();
	singleton.start();
    }

    static void stopThread() {
	SchedulableStateChangeQueue instance = singleton;
	if (instance == null) {
	    return;
	}
	singleton = null;
	instance.quit();
	try {
	    instance.join();
	} catch (InterruptedException ie) {
	    // don't care
	}
    }
    
    static void pushStart(SchedulableObject sched) {
	push(sched, SchedulableLifecyle.Start);
    }
    
    static void pushReclaim(SchedulableObject sched) {
	push(sched, SchedulableLifecyle.Reclaim);
    }
    
    private static void push(SchedulableObject schedulable, SchedulableLifecyle operation) {
	SchedulableStateChangeQueue instance = singleton;
        if (instance == null) {
            Logger logger = Logging.getLogger(SchedulableStateChangeQueue.class);
            if (logger.isWarnEnabled()) {
                logger.warn("Ignoring enqueue request on stopped thread");
            }
            return;
        }
        // XXX: Creating a new queue entry every time is potentially expensive
        instance.add(new QueueEntry(schedulable, operation));
    }


    private final CircularQueue<QueueEntry> queue;
    private final Object lock;
    private boolean should_stop;

    private SchedulableStateChangeQueue() {
	super("Thread Start/Stop Queue");
	setDaemon(DAEMON);
	queue = new CircularQueue<QueueEntry>();
	lock = new Object();
    }
    
    private void quit() {
	synchronized (lock) {
	    should_stop = true;
	    lock.notify();
	}
    }
    
    private void add(QueueEntry entry) {
	synchronized (lock) {
	    // TODO turn the validity check into an assertion that only runs during testing
	    if (validToAdd(entry)) {
		queue.add(entry);
		lock.notify();
	    }
	}	
    }

    /**
     * Verify that the Schedulable isn't already on the
     * queue.  This is potentially expensive and in theory
     * should never happen.  But it does, so until we know
     * why, we need to check.
     */
   private boolean validToAdd(QueueEntry entry) {
	SchedulableObject schedulable = entry.schedulable;
	for (QueueEntry e : queue) {
	    if (e.schedulable == schedulable) {
		Logger logger = Logging.getLogger(SchedulableStateChangeQueue.class);
		logger.error(schedulable + " is already in the queue with "
			+ e.operation +   ", new op is " + entry.operation);
		// XXX: Figure out why this happens !!
		return false;
	    }
	}
	return true;
    }

    @Override
   public void run() {
	while (true) {
	    QueueEntry entry = null;
	    synchronized (lock) {
		while (true) {
		    if (should_stop) {
			return;
		    }
		    if (!queue.isEmpty()) {
			break;
		    }
		    try { 
			lock.wait();
		    } catch (InterruptedException ex) {
		    }
		}
		entry = queue.next();
	    }
	    entry.doWork();
	}
    }
    
    private static class QueueEntry {
	final SchedulableObject schedulable;
	final SchedulableLifecyle operation;
	
	QueueEntry(SchedulableObject schedulable, SchedulableLifecyle op) {
	    this.schedulable = schedulable;
	    this.operation = op;
	}
	
	void doWork() {
	    operation.doWork(schedulable);
	}
    }
}
