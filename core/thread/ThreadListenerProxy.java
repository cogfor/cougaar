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

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.service.ThreadListenerService;

/**
 * Implementation of {@link ThreadListenerService}.
 */
final class ThreadListenerProxy implements ThreadListenerService
{
    private List<List<ThreadListener>> listenersList;
    private TreeNode node;

    ThreadListenerProxy(int laneCount) {
	listenersList = new ArrayList<List<ThreadListener>>(laneCount);
	for (int i=0; i<laneCount; i++)
	    listenersList.add(new ArrayList<ThreadListener>());
    }
		    
    void setTreeNode(TreeNode node) {
	this.node = node;
    }

    List<ThreadListener> getListeners(SchedulableObject schedulable) {
	return listenersList.get(schedulable.getLane());
    }

    List<ThreadListener> getListeners(Scheduler scheduler) {
	return listenersList.get(scheduler.getLane());
    }

    synchronized void notifyQueued(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	List<ThreadListener> listeners = getListeners(schedulable);
        for (ThreadListener listener : listeners) {
	    listener.threadQueued(schedulable, consumer);
	}
    }

    synchronized void notifyDequeued(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	List<ThreadListener> listeners = getListeners(schedulable);
	for (ThreadListener listener : listeners) {
	    listener.threadDequeued(schedulable, consumer);
	}
    }

    synchronized void notifyStart(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	List<ThreadListener> listeners = getListeners(schedulable);
	for (ThreadListener listener : listeners) {
	    listener.threadStarted(schedulable, consumer);
	}
    }

    synchronized void notifyEnd(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	List<ThreadListener> listeners = getListeners(schedulable);
	for (ThreadListener listener : listeners) {
	    listener.threadStopped(schedulable, consumer);
	}
    }

    synchronized void notifyRightGiven(Scheduler scheduler) {
	String id = scheduler.getName();
	List<ThreadListener> listeners = getListeners(scheduler);
	for (ThreadListener listener : listeners) {
	    listener.rightGiven(id);
	}
    }

    synchronized void notifyRightReturned(Scheduler scheduler) {
	String id = scheduler.getName();
	List<ThreadListener> listeners = getListeners(scheduler);
	for (ThreadListener listener : listeners) {
	    listener.rightReturned(id);
	}
    }

    public synchronized void addListener(ThreadListener listener,
					 int lane) {
	if (lane < 0 || lane >= listenersList.size()) {
	    throw new RuntimeException("Lane is out of range: " +lane);
	}
	listenersList.get(lane).add(listener);
    }


    public synchronized void removeListener(ThreadListener listener,
					    int lane)  {
	if (lane < 0 || lane >= listenersList.size()) {
	    throw new RuntimeException("Lane is out of range: " +lane);
	}
	listenersList.get(lane).remove(listener);
    }


    public synchronized void addListener(ThreadListener listener) {
	listenersList.get(node.getDefaultLane()).add(listener);
    }


    public synchronized void removeListener(ThreadListener listener) {
	listenersList.get(node.getDefaultLane()).remove(listener);
    }

}
