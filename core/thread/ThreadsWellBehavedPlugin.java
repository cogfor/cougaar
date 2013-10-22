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

import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadService;

/**
 * This class marks the agent as wellbehaved. This means that all the
 * plugins in the agent will not block on IO or Network and will not
 * hog the CPU. This Plugin calls the ThreadControlService and sets
 * the default Thread Lane to be WELL_BEHAVED. After load time, the
 * plugin should do nothing. 
 * 
 * This is designed to be an Agent Plugin
 * 
 * JAZ this has not been tested with mobility, yet. Also, what about
 * when this plugin loads, maybe some threads will already be started.
 */
public class ThreadsWellBehavedPlugin
    extends ParameterizedComponent // not really a Plugin
{
    private ServiceBroker sb;

    public ThreadsWellBehavedPlugin() {
	super();
    }

    public void setServiceBroker(ServiceBroker sb) {
        this.sb = sb;
    }

    @Override
   public void load() {
	super.load();
	long defaultLane = getParameter("defaultLane", 
					  ThreadService.WELL_BEHAVED_LANE);
	ServiceBroker sb = getServiceBroker();
	ThreadControlService tsvc = sb.getService(this, ThreadControlService.class, null);
	LoggingService lsvc = sb.getService(this, LoggingService.class, null);
	tsvc.setDefaultLane((int)defaultLane);
	if (lsvc.isDebugEnabled()) 
	    lsvc.debug("Default Lane Set to " + defaultLane +
		       " got back" + tsvc.getDefaultLane());
	
	sb.releaseService(this, ThreadControlService.class, tsvc);
	sb.releaseService(this, LoggingService.class, lsvc);
    }

    public ServiceBroker getServiceBroker() {
	return sb;
    }

}
