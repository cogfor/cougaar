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

package org.cougaar.core.qos.metrics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;


/**
 * The test Plugin will dump the time history of an Agent's resource
 * load into the logging system.  
 */
public class AgentLoadTracePlugin 
    extends ComponentPlugin
    implements Constants
{
    private LoggingService loggingService = null;    
    private MetricsService metricsService;
    private DecimalFormat formatter = new DecimalFormat("###,#00.0#");
    private boolean first_time = true;
    private ArrayList agents;
    private static final int BASE_PERIOD = 10; //10SecAVG
    private AgentStatusService agentStatusService=null; 
    

    private class Poller implements Runnable {
	public void run() {	    
	    if (first_time) {
		
		collectNames();
		// log data key
		if (loggingService.isInfoEnabled()) {
		    loggingService.info("--- Data Key ---\n" + 
					"Timestamp, " +
					"AgentName, " +
					"CPULoad, " +
					"CPULoadJips, " +
					"MsgIn, " +
					"MsgOut, " +
					"bytesIn, " +
					"BytesOut, " +
					"PersistSize" );
		}	    
	    }
	    else
		dump();
	    first_time = false;
	}
    }

    public AgentLoadTracePlugin() {
	super();
    }
    

    private void collectNames() {
	Set localAgents = getLocalAgents();
	if (localAgents == null) { 
	    return;
	}
	agents = new ArrayList();
	for (Iterator itr = localAgents.iterator(); itr.hasNext(); ) {
            MessageAddress addr = (MessageAddress) itr.next();
	    String name = addr.getAddress();
	    agents.add(name);
	}
    }

    private void logAgentMetrics(String name) {
	if(name == null) {
	    loggingService.debug("Agent is null, cannot retrieve any metrics!");
	}
	String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	
	Metric cpuLoad = metricsService.getValue(agentPath
						 +CPU_LOAD_AVG+
						 "(" +_10_SEC_AVG+ ")");
	double cpuLoadV = cpuLoad.doubleValue();
	    
	Metric cpuLoadJips = metricsService.getValue(agentPath
						     +CPU_LOAD_MJIPS+
						     "(" +_10_SEC_AVG+")");
	double cpuLoadJipsV = cpuLoadJips.doubleValue();

	Metric msgIn = metricsService.getValue(agentPath
					       +MSG_IN+
					       "(" +_10_SEC_AVG+")");
	double msgInV = msgIn.doubleValue();

	Metric msgOut = metricsService.getValue(agentPath
						+MSG_OUT+
						"(" +_10_SEC_AVG+")");
	double msgOutV = msgOut.doubleValue();

	Metric bytesIn = metricsService.getValue(agentPath
						 +BYTES_IN+
						 "(" +_10_SEC_AVG+")");
	double bytesInV = bytesIn.doubleValue();

	Metric bytesOut = metricsService.getValue(agentPath
						  +BYTES_OUT+
						  "(" +_10_SEC_AVG+")");
	double bytesOutV = bytesOut.doubleValue();

	Metric persistSize = metricsService.getValue(agentPath
						     +PERSIST_SIZE_LAST );
	double persistSizeV = persistSize.doubleValue();

	long now =  System.currentTimeMillis();
	    
	if (loggingService.isInfoEnabled()) {
	    loggingService.info(now + ", "+ 
				name  +","+
				formatter.format(cpuLoadV) +","+
				formatter.format(cpuLoadJipsV)  +","+
				formatter.format(msgInV) +","+
				formatter.format(msgOutV) +","+
				formatter.format(bytesInV) +","+
				formatter.format(bytesOutV) +","+
				formatter.format(persistSizeV) );
	}
    }
    
    private void dump() {
	Iterator itr = agents.iterator();
	while (itr.hasNext()) {
	    String name = (String) itr.next();
	    logAgentMetrics(name);
	}
    }

    @Override
   public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	metricsService = sb.getService(this, MetricsService.class, null);

	loggingService = sb.getService(this, LoggingService.class, null);
	
	agentStatusService = sb.getService(this, AgentStatusService.class, null);
	
	ThreadService tsvc = sb.getService(this, ThreadService.class, null);

	Poller poller = new Poller();
	Schedulable sched = tsvc.getThread(this, 
					   poller,
					   "LoadPoller");
	//sched.schedule(60000, 500);
	sched.schedule(5000, BASE_PERIOD*1000);
	sb.releaseService(this, ThreadService.class, tsvc);
	
    }

    /**
     * @return the message addresses of the agents on this
     * node, or null if that information is not available.
     */
    protected final Set getLocalAgents() {
        // instead of asking the NodeControlService's AgentContainer for:
        //   agentContainer.getAgentAddresses()
	// we try getting agents from agentstatus service
	if(agentStatusService == null) {
	    if(loggingService.isDebugEnabled())
		loggingService.debug("No LocalAgents from AgentStatusService");
	    return null;
	} else {
	    return agentStatusService.getLocalAgents();
	}
    }

    @Override
   protected void setupSubscriptions() {
    }
  
    @Override
   protected void execute() {
    }

}
