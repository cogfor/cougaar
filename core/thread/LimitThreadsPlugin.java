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

/**
 * This Component limits the number of threads that an agent can use
 * to run its schedulables in parallel.  This should loaded into all
 * agents.  Note that if the more than the given number of threads are
 * already running when this loads, the limiting won't become
 * effective until some of them stop.
 * 
 * JAZ this has not been tested with mobility yet.
 */
public class LimitThreadsPlugin
    extends ParameterizedComponent // not really a Plugin
{
    private static final long MAX_THREADS = 5;
    private ServiceBroker sb;

    public LimitThreadsPlugin() 
    {
	super();
    }

    public void setServiceBroker(ServiceBroker sb) 
    {
	this.sb = sb;
    }

    @Override
   public void load() 
    {
	super.load();
					  
	ThreadControlService tsvc = sb.getService(this, ThreadControlService.class, null);
	LoggingService lsvc = sb.getService(this, LoggingService.class, null);


	int lane = (int) getParameter("lane", tsvc.getDefaultLane());
	int maxThreads = (int) getParameter("maxThreads", MAX_THREADS);
	tsvc.setMaxRunningThreadCount(maxThreads, lane);

	if (lsvc.isDebugEnabled()) 
	    lsvc.debug("Setting Max Threads to " +maxThreads+ 
		       "for lane " + lane);
	
	sb.releaseService(this, ThreadControlService.class, tsvc);
	sb.releaseService(this, LoggingService.class, lsvc);
    }

}
