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

import org.cougaar.util.CircularQueue;

/**
 * The simplest thread service implementation, which runs its {@link
 * Schedulable}s serially, uses this utility class to hold a set of {@link
 * Schedulable}s in proper sequence.
 */
final class SerialThreadQueue {
	
    private final CircularQueue<TrivialSchedulable> schedulables;
    private final Object lock;

    SerialThreadQueue() {
	schedulables = new CircularQueue<TrivialSchedulable>();
	lock = new Object();
    }

    int iterateOverThreads(ThreadStatusService.Body body) {
	int count = 0;
	TrivialSchedulable[] objects = new TrivialSchedulable[schedulables.size()];
	synchronized (lock) {
	    schedulables.toArray(objects);
	}
	for (TrivialSchedulable sched : objects) {
	    try {
		body.run("root", sched);
		count++;
	    } catch (Throwable t) {
		// ignore
	    }
	}
	return count;
    }

    Object getLock() {
	return lock;
    }

    void enqueue(TrivialSchedulable sched) {
	sched.setState(CougaarThread.THREAD_PENDING);
	synchronized (lock) {
	    if (!schedulables.contains(sched)) {
		schedulables.add(sched);
		lock.notify();
	    }
	}
    }

    // caller synchronizes
    boolean isEmpty() {
	return schedulables.isEmpty();
    }

    // caller synchronizes
    TrivialSchedulable next()  {
	if (schedulables.isEmpty())
	    return null;
	else
	    return schedulables.next();
    }
}

