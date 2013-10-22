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

import java.util.Comparator;

import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.UnaryPredicate;

/**
 * The implementation of {@link ThreadControlService}.  Most of the
 * real work happens in the {@link TreeNode} or the {@link Schedulable}
 * for the corresponding {@link ThreadService}.
 */
class ThreadControlServiceProxy
    implements ThreadControlService
{
    private TreeNode node;

    ThreadControlServiceProxy(TreeNode node)
    {
	this.node = node;
    }


    public int getDefaultLane()
    {
	return node.getDefaultLane();
    }

    public void setDefaultLane(int lane)
    {
	node.setDefaultLane(lane);
    }

    private void validateLane(int lane)
    {
	if (lane < 0 || lane >= node.getLaneCount())
	    throw new RuntimeException("Lane is out of range: " +lane);
    }


    public void setMaxRunningThreadCount(int count, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setMaxRunningThreadCount(count);
    }

    public void setQueueComparator(Comparator<Schedulable> comparator, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setQueueComparator(comparator);
    }

    public void setRightsSelector(RightsSelector selector, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setRightsSelector(selector);
    }

    public boolean setQualifier(UnaryPredicate predicate, int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).setQualifier(predicate);
    }

    public boolean setChildQualifier(UnaryPredicate predicate, int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).setChildQualifier(predicate);
    }

    public int runningThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).runningThreadCount();
    }

    public int pendingThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).pendingThreadCount();
    }

    public int activeThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).activeThreadCount();
    }

    public int maxRunningThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).maxRunningThreadCount();
    }




    public void setMaxRunningThreadCount(int count)
    {
	setMaxRunningThreadCount(count, node.getDefaultLane());
    }

    public void setQueueComparator(Comparator<Schedulable> comparator)
    {
	setQueueComparator(comparator, node.getDefaultLane());
    }

    public void setRightsSelector(RightsSelector selector)
    {
	setRightsSelector(selector, node.getDefaultLane());
    }

    public boolean setQualifier(UnaryPredicate predicate)
    {
	return setQualifier(predicate, node.getDefaultLane());
    }

    public boolean setChildQualifier(UnaryPredicate predicate)
    {
	return setChildQualifier(predicate, node.getDefaultLane());
    }


    public int runningThreadCount()
    {
	return runningThreadCount(node.getDefaultLane());
    }

    public int pendingThreadCount()
    {
	return pendingThreadCount(node.getDefaultLane());
    }

    public int activeThreadCount()
    {
	return activeThreadCount(node.getDefaultLane());
    }

    public int maxRunningThreadCount()
    {
	return maxRunningThreadCount(node.getDefaultLane());
    }



}
