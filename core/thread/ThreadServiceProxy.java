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

import org.cougaar.core.service.ThreadService;

/**
 * The default implementation of {@link ThreadService}.
 */
final class ThreadServiceProxy 	implements ThreadService
{
    private TreeNode treeNode;

    ThreadServiceProxy(TreeNode treeNode) 
    {
	this.treeNode = treeNode;
    }

    TreeNode getTreeNode() {
	return treeNode;
    }




    public Schedulable getThread(Object consumer, Runnable runnable) {
	return new SchedulableObject(treeNode, runnable, null, consumer,
				     treeNode.getDefaultLane());
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name) 
    {
	return new SchedulableObject(treeNode, runnable, name, consumer,
				     treeNode.getDefaultLane());
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name,
				 int lane) 
    {
	if (lane < 0 || lane >= treeNode.getLaneCount())
	    throw new RuntimeException("Lane is out of range: " +lane);
	return new SchedulableObject(treeNode, runnable, name, consumer,
				     lane);
    }

}
