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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.DecayingHistory;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.ThreadService;

/**
 * This Plugin collects the load history for the Agent in which it's
 * loaded, and uplaads that data to the metrics service.  It
 * uses the {@link AgentLoadService} to collect the raw date and should
 * be loaded at LOW priority to ensure that the service is available.
 *
 * In the <a
 * href="../../../../../OnlineManual/MetricsService/sensors.html">Sensor
 * Data Flow</a> pattern this class plays the role of <b>Rate
 * Converter</b> for load data (CPU load average, CPU) for Agents,
 * Nodes and Services.
 *
 * @see AgentLoadSensorPlugin
 * @see org.cougaar.core.qos.metrics.AgentLoadServlet
 */
public class AgentLoadRatePlugin
    extends ComponentPlugin
    implements Runnable, Constants
{
    private static final int BASE_PERIOD = 10; //10SecAVG

    private class AgentLoadHistory extends DecayingHistory {
	private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;

	String agentKey;
	String mjipsKey;
	String loadavgKey;

	AgentLoadHistory(String name) {
	    super(10, 3, BASE_PERIOD);
	    if (name.startsWith("Service")) 	       
		agentKey = name;
	    else if (name.startsWith("NodeTotal_"))
		agentKey = "Node" +KEY_SEPR+ name.substring(10);
	    else
		agentKey = "Agent" +KEY_SEPR+ name ;
	    mjipsKey=(agentKey +KEY_SEPR+ CPU_LOAD_MJIPS).intern();
	    addKey(mjipsKey);
	    loadavgKey=(agentKey +KEY_SEPR+ CPU_LOAD_AVG).intern();
	    addKey(loadavgKey);
	}

	@Override
   public void newAddition(KeyMap keys, 
				DecayingHistory.SnapShot now_raw,
				DecayingHistory.SnapShot last_raw) 
	{
	    AgentLoadService.AgentLoad now = (AgentLoadService.AgentLoad) 
		now_raw;
	    AgentLoadService.AgentLoad last = (AgentLoadService.AgentLoad)
		last_raw;
	    double deltaT = now.timestamp -last.timestamp;
	    double deltaLoad = now.loadAvgIntegrator-last.loadAvgIntegrator;
	    double deltaMJips = 
		now.loadMjipsIntegrator-last.loadMjipsIntegrator;

	    //Must match the Metrics Constants for CPU_LOAD_AVG_1XXX_SEC_AVG
	    String lKey = keys.getKey(loadavgKey);
 	    Metric lData = new MetricImpl(new Double( deltaLoad/deltaT),
					 CREDIBILITY,
					 "threads/sec",
					 "AgentLoadSensor");
 	    metricsUpdateService.updateValue(lKey, lData);

	    //Must match the Metrics Constants for CPU_LOAD_MJIPS_1XXX_SEC_AVG
	    String mKey = keys.getKey(mjipsKey);
 	    Metric mData = new MetricImpl(new Double( deltaMJips/deltaT),
					  CREDIBILITY,
					  "mjips",
					  "AgentLoadSensor");
 	    metricsUpdateService.updateValue(mKey, mData);
	}
	
    }

    private AgentLoadService agentLoadService;
    private MetricsUpdateService metricsUpdateService;
    private Schedulable schedulable;
    private Map<String,AgentLoadHistory> histories;

    public AgentLoadRatePlugin() {
	histories = new HashMap<String,AgentLoadHistory>();
    }

    // Local
    AgentLoadHistory findOrMakeHistory(String agent) {
	AgentLoadHistory history = histories.get(agent);
	if (history == null) {
	    history = new AgentLoadHistory(agent);
	    histories.put(agent, history);
	}
	return history;
    }

    // Component
    @Override
   public void load() {
	super.load();
	
	ServiceBroker sb = getServiceBroker();
	agentLoadService = sb.getService(this, AgentLoadService.class, null);
	if (agentLoadService == null) {
	    throw new RuntimeException("Can't find AgentLoadService. This plugin must be loaded at Low priority");
	}

	metricsUpdateService = sb.getService(this, MetricsUpdateService.class, null);

	ThreadService tsvc = sb.getService(this, ThreadService.class, null);
	schedulable = tsvc.getThread(this, this, "AgentLoadRate");
	schedulable.schedule(5000, BASE_PERIOD *1000);
	sb.releaseService(this, ThreadService.class, tsvc);
    }


    // Schedulable body
    public void run() {
	Collection agentLoadSnapshot = agentLoadService.snapshotRecords();
        boolean useItr;
        Iterator itr;
        List l;
        if (agentLoadSnapshot instanceof List &&
            agentLoadSnapshot instanceof RandomAccess) {
          useItr = false;
          itr = null;
          l = (List) agentLoadSnapshot;
        } else {
          useItr = true;
          itr = agentLoadSnapshot.iterator();
          l = null;
        }
	for (int i = 0, n = agentLoadSnapshot.size(); i < n; i++) {
	    AgentLoadService.AgentLoad record = (AgentLoadService.AgentLoad)
		(useItr ? itr.next() : l.get(i));
	    String agent = record.name;
	    AgentLoadHistory history = findOrMakeHistory(agent);
	    history.add(record);
	}
    }

    // Plugin
    @Override
   protected void setupSubscriptions() {
    }
  
    @Override
   protected void execute() {
    }


}
