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

// Later this will move elsewhere...
package org.cougaar.core.thread;

import org.cougaar.core.service.ThreadService;
import org.cougaar.util.CircularQueue;

/**
 * This utility class embads a {@link CircularQueue} in its
 * own {@link Schedulable}, the body of which processes elements on the queue
 * for up to 500ms or until the queue is empty, whichever comes
 * first.  Every addition to the queue (re)starts the Schedulable.
 */
public class RunnableQueue implements Runnable {
    private static final long  MAX_RUNTIME = 500;
    private final CircularQueue<Runnable> queue;
    private final Schedulable sched;
    
    public RunnableQueue(ThreadService svc, String name) {
	queue = new CircularQueue<Runnable>();
	sched = svc.getThread(this, this, name);
    }

    public void add(Runnable runnable) {
	synchronized (queue) {
	    queue.add(runnable);
	}
	sched.start();
    }

    public void run() {
	long start = System.currentTimeMillis();
	Runnable next = null;
	boolean restart = false;
	while (true) {
	    synchronized (queue) {
		if (queue.isEmpty()) {
		    break;
		}
		if (System.currentTimeMillis()-start > MAX_RUNTIME) {
		    // Spent too long in this thread but the queue
		    // isn't empty yet.  Start a new thread.
		    restart = true;
		    break;
		}
		next = queue.next();
	    }
	    next.run();
	}
	if (restart) {
	    sched.start();
	}
    }
}

