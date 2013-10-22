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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;

/**
 * This Plugin provides the {@link AgentLoadService} for other Plugins
 * in the same Agent.  The service implementation is an inner class.
 * In the <a
 * href="../../../../../OnlineManual/MetricsService/sensors.html">Sensor
 * Data Flow</a> pattern this class plays the role of <b>Sensor</b>
 * for load data (CPU load average, CPU) for Agents, Nodes and
 * Services.
 *
 * @see AgentLoadRatePlugin
 * @see org.cougaar.core.qos.metrics.AgentLoadServlet
 */
public class AgentLoadSensorPlugin
    extends ComponentPlugin
    implements ThreadListener, Constants, ServiceProvider
{
    private class ServiceImpl implements AgentLoadService {
	public Collection snapshotRecords() {
	    return snapshot();
	}
    }

    private class ConsumerRecord extends AgentLoadService.AgentLoad {

	ConsumerRecord(String name) {
	    this.name = extractObjectName(name);
	}

	private String extractObjectName(String rawName) {
	    if (rawName.startsWith("NodeTotal_")) {
		return rawName;
	    } else if (rawName.startsWith("Node")) {
		// Root Scheduler of the ThreadService
		return "Service" +KEY_SEPR+ "NodeRoot";
	    } else if (rawName.startsWith("Agent")){
		// Agent-level Scheduler.
		// We assume 'Agent_AgentName'
		return rawName.substring(6);
	    } else {
		// Some other Scheduler (eg MTS or MetricService)
		return "Service" +KEY_SEPR+ rawName;
	    }
	}	
	
	synchronized void incrementOutstanding() {
	    accumulate();
	    ++outstanding;
	    // Can't get the specific max, so no applicable test
	}

	synchronized boolean decrementOutstanding() {
	    // The given consumer Scheduler may have running threads when
	    // this listener starts listening.  When those threads stop,
	    // the count will go negative.  Ignore those.
	    if (outstanding == 0) return false;

	    accumulate();
	    --outstanding;
	    if (outstanding < 0) 
		loggingService.debug("Agent outstanding =" +outstanding+ 
				   " when rights returned for " +name);
	    return true;
	}

	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (timestamp > 0) {
		double deltaT = now - timestamp;
		loadMjipsIntegrator += deltaT* outstanding *effectiveMJIPS();
		loadAvgIntegrator += deltaT * outstanding;
	    } 
	    timestamp = now;
	}

	synchronized AgentLoadService.AgentLoad snapshot() {
	    accumulate();
	    AgentLoadService.AgentLoad result = 
		new AgentLoadService.AgentLoad();
	    result.name = name;
	    result.outstanding = outstanding;
	    result.loadAvgIntegrator = loadAvgIntegrator;
	    result.loadMjipsIntegrator = loadMjipsIntegrator;
	    return result;
	}
    }



    private static int number_of_cpus = 1; // default, if no metrics service
    private static double capacity_mjips = 1.0; // ditto

    private int total;
    private Map<String,ConsumerRecord> records = new HashMap<String,ConsumerRecord>();
    private ConsumerRecord nodeRecord;

    private LoggingService loggingService;
    private AgentLoadService serviceImpl;

    public AgentLoadSensorPlugin() {
    }

    // Component
    @Override
   public void load() {
	super.load();
	

	ServiceBroker sb = getServiceBroker();


	loggingService = sb.getService(this, LoggingService.class, null);

	NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
 	MessageAddress my_node = nis.getMessageAddress();
	sb.releaseService(this, NodeIdentificationService.class, nis);

	MetricsService metricsService = sb.getService(this, MetricsService.class, null);

	nodeRecord = new ConsumerRecord("NodeTotal_" + my_node);


	String path = "Node(" +my_node+ ")" +PATH_SEPR+ "Jips";
	Observer mjips_obs = new Observer() {
		public void update (Observable observable, Object value) {
		    Metric m = (Metric) value;
		    capacity_mjips = m.doubleValue()/1000000.0;
		}
	    };
	metricsService.subscribeToValue(path,  mjips_obs);


	path = "Node(" +my_node+ ")" +PATH_SEPR+ "Count";
	Observer cpu_obs = new Observer() {
		public void update (Observable observable, Object value) {
		    Metric m = (Metric) value;
		    // force to be at least 1 cpu
		    number_of_cpus =  Math.max(1,m.intValue());
		}
	    };
	metricsService.subscribeToValue(path, cpu_obs);


	sb.releaseService(this, MetricsService.class, metricsService);

	serviceImpl = new ServiceImpl();
	// We provide AgentLoadService
	sb.addService(AgentLoadService.class, this);


	// We need the root ServiceBroker's ThreadListenerService
	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);

	if (ncs != null) {
	    ServiceBroker rootsb = ncs.getRootServiceBroker();
	    sb.releaseService(this, NodeControlService.class, ncs);

	    ThreadListenerService tls = rootsb.getService(this, ThreadListenerService.class, null);
	    if (tls != null) {
		for(int lane = 0; lane < ThreadService.LANE_COUNT; lane++) {
		    tls.addListener(this,lane);
		}
		rootsb.releaseService(this, ThreadListenerService.class, tls);
	    }
	} else {
	    throw new RuntimeException("AgentLoadSensor can only be used in NodeAgents");
	}

    }


    // Plugin
    @Override
   protected void setupSubscriptions() {
    }
  
    @Override
   protected void execute() {
    }


    // ServiceProvider
    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) {
	if (serviceClass == AgentLoadService.class) {
	    return serviceImpl;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service) {
    }


    // Local utility methods
    private double effectiveMJIPS() {
	return capacity_mjips / Math.max(1,(total/number_of_cpus));
    }


    private List<AgentLoadService.AgentLoad> snapshot() {
	List<AgentLoadService.AgentLoad> result = 
	    new ArrayList<AgentLoadService.AgentLoad>();
	result.add(nodeRecord.snapshot());
	synchronized (records) {
	    for (ConsumerRecord record : records.values()) {
		result.add(record.snapshot());
	    }
	}
	return result;
    }

    private ConsumerRecord findRecord(String name) {
	ConsumerRecord rec = null;
	synchronized (records) {
	    rec = records.get(name);
	    if (rec == null) {
		rec = new ConsumerRecord(name);
		records.put(name, rec);
	    }
	}
	return rec;
    }



    // ThreadListener
    public void threadQueued(Schedulable schedulable, 
			     Object consumer) {
    }
    public void threadDequeued(Schedulable schedulable, 
			       Object consumer) {
    }
    public void threadStarted(Schedulable schedulable, 
			      Object consumer){
    }
    public void threadStopped(Schedulable schedulable, 
			      Object consumer)
    {
    }

    public synchronized void rightGiven(String scheduler) {
	ConsumerRecord rec = findRecord(scheduler);
	rec.incrementOutstanding();
	nodeRecord.incrementOutstanding();
	++total;
    }
		
    public synchronized void rightReturned(String scheduler) {
	ConsumerRecord rec = findRecord(scheduler);

	// The given consumer Scheduler may have running threads when
	// this listener starts listening.  When those threads stop,
	// the count will go negative.  Ignore those.
	if (rec.decrementOutstanding()) {
	    --total;
	    nodeRecord.decrementOutstanding();
	}
   }
}
