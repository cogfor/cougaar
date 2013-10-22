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


/**
 * The standard hiearchical thread service implementation uses this
 * extension of {@link Scheduler} to handle the propagation of rights.
 */
public class PropagatingScheduler extends Scheduler {
    private RightsSelector selector;

    public PropagatingScheduler(ThreadListenerProxy listenerProxy) {
	super(listenerProxy);

	// Default selector
	selector = new RoundRobinSelector();
	selector.setScheduler(this);
    }

    
    @Override
   public void setRightsSelector(RightsSelector selector) {
	this.selector = selector;
	selector.setScheduler(this);
    }

    @Override
   boolean requestRights(Scheduler requestor) {
	if (!allowRightFor(requestor)) {
	    return false;
	}
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    return super.requestRights(requestor);
	} else {
	    synchronized (this) {
		if (!checkLocalRights()) {
		    return false;
		}
		++rightsRequestCount;
	    }
	    Scheduler parent = parent_node.getScheduler(getLane());
	    boolean ok = parent.requestRights(this);
	    // If our parent gave us a right, increase our local count
	    synchronized (this) {
		if (ok) {
		    incrementRunCount(this);
		}
		--rightsRequestCount;
	    }
	    return ok;
	}
    }

    
    @Override
   void releaseRights(Scheduler consumer) { 
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    super.releaseRights(consumer);
	} else {
	    // In this simple scheduler, layers other than root always
	    // give up the right at this point (root may hand it off).
	    decrementRunCount(this);
	    Scheduler parent = parent_node.getScheduler(getLane());
	    parent.releaseRights(this);
	}
    }
    
    @Override
   SchedulableObject getNextPending() {
	return selector.getNextPending();
    }

}
