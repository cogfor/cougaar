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
import java.util.Timer;

import org.cougaar.core.service.ThreadService;

/**
 * This class is used to represent the hierarchy of the standard
 * layered {@link ThreadService}.  
 */
final class TreeNode {
    private static Timer timer;
    private static boolean isActive = true;
    
    private final List<TreeNode> children;
    private final String name;
    private final Scheduler[] schedulers;
    private final ThreadPool[] pools;
    private final TreeNode parent;
    private int defaultLane = ThreadService.BEST_EFFORT_LANE; // parameter?

    TreeNode(Scheduler[] schedulers, 
	     ThreadPool[] pools,
	     String name,
	     ThreadServiceProxy parentService)  {
	this.name = name;
	children = new ArrayList<TreeNode>();
	this.schedulers = schedulers;
	this.pools = pools;
	for (int i=0; i<schedulers.length; i++) schedulers[i].setTreeNode(this);
	TreeNode parent = 
	    parentService == null ? null : parentService.getTreeNode();
	this.parent = parent;
	if (parent != null) parent.addChild(this);
    }

    int getDefaultLane() {
	return defaultLane;
    }
    
    void setDefaultLane(int lane) {
	defaultLane = lane;
    }

    int getLaneCount() {
	return schedulers.length;
    }

    TreeNode getParent()  {
	return parent;
    }


    void addChild(TreeNode child) {
	synchronized (children) {
	    children.add(child);
	}
    }

    List<TreeNode> getChildren() {
	return children;
    }


   int iterateOverRunningThreads(ThreadStatusService.Body body) {
	int count = 0;
	for (int i=0; i<pools.length; i++) {
	    count += pools[i].iterateOverRunningThreads(body);
	}
	return count;
    }

    int iterateOverQueuedThreads(ThreadStatusService.Body body) {
	int count = 0;
	for (int i=0; i<schedulers.length; i++)
	    count += schedulers[i].iterateOverQueuedThreads(body);
	if (children != null) {
	    synchronized (children) {
		for (int i = 0, n = children.size(); i < n; i++) {
		    TreeNode child = children.get(i);
		    count += child.iterateOverQueuedThreads(body);
		}
	    }
	}
	return count;
    }


    Scheduler getScheduler(int lane) {
	return schedulers[lane];
    }

    String getName() {
	return name;
    }

    ThreadPool getPool(int lane) {
	return pools[lane];
    }

    static synchronized Timer timer()  {
        if (timer == null && isActive) {
            timer = new Timer(true);
        }
	return timer;
    }
    
    static void releaseTimer() {
      isActive = false;
      if (timer != null) {
          timer.cancel();
          timer = null;
      }
    }

}
