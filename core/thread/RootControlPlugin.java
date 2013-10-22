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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.UnaryPredicate;

/**
 * This node-level Plugin shows examples of limiting the top-level
 * thread service in two ways: it sets the global max to 2, and it
 * qualifies rights selection for children so that no child ever uses
 * more than half of the available rights.
 */
public class RootControlPlugin extends ComponentPlugin
{
    private static final int MAX_THREADS=2;

    public RootControlPlugin() {
	super();
    }


    private static class ExampleChildQualifier implements UnaryPredicate {
	/**
       * 
       */
      private static final long serialVersionUID = 1L;
   private LoggingService lsvc;

	ExampleChildQualifier(LoggingService lsvc) {
	    this.lsvc = lsvc;
	}

	public boolean execute(Object x) {
	    if (! (x instanceof Scheduler)) return false;

	    Scheduler child = (Scheduler) x;
	    int lane = child.getLane();
	    Scheduler parent = 
		child.getTreeNode().getParent().getScheduler(lane);
	    float count = child.runningThreadCount();
	    float max = parent.maxRunningThreadCount();
	    // Random test - don't let any one child use more than half
	    // the slots
	    if (count/max <= .5) {
		return true;
	    } else {
		if (lsvc.isWarnEnabled()) 
		    lsvc.warn("Attempted to use too many rights Child="+child);
		return false;
	    }
	}
    }


    @Override
   public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	LoggingService lsvc = sb.getService(this, LoggingService.class, null);
	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
	sb = ncs.getRootServiceBroker();
 	ThreadControlService tcs = sb.getService(this, ThreadControlService.class, null);
//  	RightsSelector selector = new PercentageLoadSelector(sb);
 	//tcs.setRightsSelector(selector);
	if (tcs != null) {
	    tcs.setMaxRunningThreadCount(MAX_THREADS);
	    tcs.setChildQualifier(new ExampleChildQualifier(lsvc));
	}
	
    }

    @Override
   protected void setupSubscriptions() {
    }
  
    @Override
   protected void execute() {
	System.out.println("Uninteresting");
    }

}
