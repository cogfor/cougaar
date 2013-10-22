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

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This is the standard implementation of {@link RightsSelector}.  It
 * uses a round-robin approach to offer rights equally among its
 * own {@link Scheduler}'s thread and those of its children.
 */
class RoundRobinSelector implements RightsSelector {
    private static final Logger logger = Logging.getLogger(RoundRobinSelector.class);
    // Holds the next index of the round-robin selection.  A value of
    // -1 refers to the local queue, rather than any of the children.
    private int currentIndex = -1;
    protected PropagatingScheduler scheduler;

    public void setScheduler(PropagatingScheduler scheduler) {
	this.scheduler = scheduler;
    }

    private SchedulableObject checkNextPending(List<TreeNode> children) {
	// Conceptually this should be synchronized on 'children'.
	// Unfortunately the nature of what it's doing makes that
	// impossible.  The result is that this code will in some
	// circumstances run while an 'add' call is in progress on the
	// list.  This will show up as a null in the list.  Just
	// ignore it and hope for the best...In theory all it means
	// is that the newly added child will miss its first turn.
	SchedulableObject handoff = null;
	int child_count = children.size();
	if (currentIndex == -1) {
	    handoff = scheduler.popQueue();
	    currentIndex = child_count == 0 ? -1 : 0;
	} else {
	    TreeNode child_node =children.get(currentIndex++);
	    if (currentIndex == child_count){
		currentIndex = -1;
	    }
	    if (child_node == null) {
		logger.warn(scheduler + "has a null child");
		return null;
	    }

	    Scheduler child = child_node.getScheduler(scheduler.getLane());
	    if (!scheduler.allowRightFor(child)) {
		return null;
	    }

	    handoff = child.getNextPending();
	    // We're the parent of the Scheduler to which the handoff
	    // is given.  Increase the local count.
	    if (handoff != null) {
		scheduler.incrementRunCount(child);
	    }
	}
	return handoff;
    }

    public SchedulableObject getNextPending() {
	int initialIndex = currentIndex;
	List<TreeNode> children = scheduler.getTreeNode().getChildren();
	SchedulableObject handoff = null;
	do {
	    handoff = checkNextPending(children);
	    if (handoff != null) {
		return handoff;
	    }
	} while (currentIndex != initialIndex);
	return null;
    }
}
