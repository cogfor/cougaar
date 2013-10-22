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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

/**
 * This Plugin periodically dumps Agent load data (1sec load average)
 * for each local Agent to the file
 * <code>&lt;node&gt;-agent-data.log</code>, where &lt;node&gt; is the
 * name of the Node.
 */
public class AgentLoadLoggerPlugin 
    extends ComponentPlugin
    implements Constants
{
    private AgentContainer agentContainer;
    private MetricsService metricsService;
    private String node;
    private String filename = "agent-data.log";
    private PrintWriter out;
    private DecimalFormat formatter = new DecimalFormat("00.00");
    private boolean first_time = true;
    private ArrayList agents;
    private long start;

    private class Poller implements Runnable {
	public void run() {
	    if (first_time) 
		collectNames();
	    else
		dump();
	    first_time = false;
	}
    }

    public AgentLoadLoggerPlugin() {
	super();
    }


    private void collectNames() {
	start = System.currentTimeMillis();
	Set localAgents = getLocalAgents();
	if (localAgents == null) return;
	
	agents = new ArrayList();
	out.print("Time");
	for (Iterator itr = localAgents.iterator(); itr.hasNext(); ) {
            MessageAddress addr = (MessageAddress) itr.next();
	    String name = addr.getAddress();
	    out.print('\t');
	    out.print(name);
	    agents.add(name);
	}
	out.println("");
	out.flush();
    }

    private void dumpAgentData(String name) {
	String path  ="Agent(" +name+ ")" +PATH_SEPR+ 
	    CPU_LOAD_AVG +"("+ _1_SEC_AVG +")";	
	Metric metric = metricsService.getValue(path);
	double value = metric.doubleValue();
	String formattedValue = formatter.format(value);
	out.print('\t');
	out.print(formattedValue);
    }

    private long relativeTimeMillis() {
	return System.currentTimeMillis()-start;
    }

    private void dump() {
	out.print(relativeTimeMillis()/1000.0);
	Iterator itr = agents.iterator();
	while (itr.hasNext()) {
	    String name = (String) itr.next();
	    dumpAgentData(name);
	}
	out.println("");
	out.flush();
    }

    @Override
   public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
        if (ncs != null) {
            agentContainer = ncs.getRootContainer();
            sb.releaseService(this, NodeControlService.class, ncs);
        }

	metricsService = sb.getService(this, MetricsService.class, null);

	NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
 	node = nis.getMessageAddress().toString();


	ThreadService tsvc = sb.getService(this, ThreadService.class, null);

	filename = node+"-"+filename;
	
	try {
	    FileWriter fw = new FileWriter(filename);
	    out = new PrintWriter(fw);
	} catch (java.io.IOException ex) {
	    ex.printStackTrace();
	    return;
	}

	Poller poller = new Poller();
	Schedulable sched = tsvc.getThread(this, 
							   poller,
							   "LoadPoller");
	sched.schedule(60000, 500);
	sb.releaseService(this, ThreadService.class, tsvc);
	
    }

    /**
     * @return the message addresses of the agents on this
     * node, or null if that information is not available.
     */
    protected final Set getLocalAgents() {
        if (agentContainer == null) {
            return null;
        } else {
            return agentContainer.getAgentAddresses();
        }
    }

    @Override
   protected void setupSubscriptions() {
    }
  
    @Override
   protected void execute() {
	//System.out.println("Uninteresting");
    }

}
