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

import java.util.List;

/**
 * This experimenatal extension of {@link Scheduler} is not currently used.
 */
public class RightsPropagatingScheduler extends Scheduler
{
    private static final long MaxTime = 20; // ms
    private int ownedRights = 0;
    private long lastReleaseTime = 0;

    public RightsPropagatingScheduler(ThreadListenerProxy listenerProxy)
    {
	super(listenerProxy);
    }

    
    @Override
   boolean requestRights(Scheduler requestor) {
	TreeNode parent_node = getTreeNode().getParent();
	boolean result;
	if (parent_node == null) {
	    // This is the root
	    result = super.requestRights(requestor);
	} else if (ownedRights > 0) {
	    return false;
	} else {
	    Scheduler parent = parent_node.getScheduler(getLane());
	    result = parent.requestRights(this);
	}
	synchronized (this) { if (result) ++ownedRights; }
	return result;
    }

    
    @Override
   void releaseRights(Scheduler consumer) { 
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    super.releaseRights(consumer);
	} else {
	    long now = System.currentTimeMillis();
	    if (now - lastReleaseTime > MaxTime) {
		releaseToParent(consumer);
	    } else {
		offerRights(consumer);
	    }
	}
   }

    private void releaseToParent(Scheduler consumer) {
	TreeNode parent_node = getTreeNode().getParent();
	Scheduler parent = parent_node.getScheduler(getLane());
	parent.releaseRights(this);
	lastReleaseTime = System.currentTimeMillis();
	synchronized (this) { --ownedRights; }
    }

    private synchronized void offerRights(Scheduler consumer) {
	SchedulableObject handoff = getNextPending();
	if (handoff != null) {
	    handoff.thread_start();
	} else {
	    releaseToParent(consumer);
	}
    }




    // Holds the next index of the round-robin selection.  A value of
    // -1 refers to the local queue, a value >= 0 refers to the
    // corresponding child.
    private int currentIndex = -1;

    private SchedulableObject checkNextPending(List<TreeNode> children) {
	SchedulableObject handoff = null;
	if (currentIndex == -1) {
	    handoff = super.getNextPending();
	    currentIndex = children.size() == 0 ? -1 : 0;
	} else {
	    TreeNode child_node =children.get(currentIndex++);
	    if (currentIndex == children.size()) currentIndex = -1;
	    Scheduler child = child_node.getScheduler(getLane());
	    handoff = child.getNextPending();
	}
	return handoff;
    }

    @Override
   SchedulableObject getNextPending() {
	int initialIndex = currentIndex;
	List<TreeNode> children = getTreeNode().getChildren();
	SchedulableObject handoff = null;
	// repeat-until
	while (true) {
	    handoff = checkNextPending(children);
	    if (handoff != null) return handoff;
	    if (currentIndex == initialIndex) break;
	}
	return null;
    }

}
