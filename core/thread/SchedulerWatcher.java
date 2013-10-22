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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.qos.metrics.Constants;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricImpl;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;

/**
 * This class listens for events on the closest ThreadService,
 * collects information about every consumer, and periodically uploads
 * that information the the metrics service.  It's typically
 * instantiated by the {@link AgentControlPlugin}, which implies that
 * it's watching the ThreadService for an Agent.
 */
public class SchedulerWatcher
    implements ThreadListener, Constants {
    private static final double CREDIBILITY = SECOND_MEAS_CREDIBILITY;
    private static final String PROVENANCE = "SchedulerWatcher";

    private String agentName;

    private class ConsumerRecord {
	// Static
	String name;
	String prefix;
	//instantanous
	int outstanding;  
	int pending;
	//Accumalators
	long accumalate_timestamp;
	long queued;   // exit counts
	long ran;      // exit counts
	// Integrators
	double sumPending;
	double sumOutstanding;
	//Rate Snapshots
	long  snapshot_timestamp;
	double snapshot_sumOutstanding;
	double snapshot_sumPending;
	double snapshot_ran;
	double snapshot_queued;
	//Rates
	double utilization;
	double runs_per_sec;
	double avg_cpu_per_run;
	double avg_latency_per_run;
	double avg_wait_per_run;

	ConsumerRecord(Object consumer) {
	    // System.err.println("%%%% new ConsumerRecord for " +consumer);
	    this.name = consumer.toString();
	    this.prefix = "Agent" +KEY_SEPR+ agentName
		+KEY_SEPR+ "Plugin" +KEY_SEPR+ this.name +KEY_SEPR;
	}


	
	synchronized void snapshot() {
	    // Calculate Deltas
	    long now = System.currentTimeMillis();
	    double deltaSumOutstanding = (sumOutstanding 
					  - snapshot_sumOutstanding);
	    double deltaSumPending = (sumPending - snapshot_sumPending);
	    double deltaRuns = (ran - snapshot_ran);
	    double deltaQueued = (queued - snapshot_queued);
	    double deltaTime = (now - snapshot_timestamp);
	    double deltaLatency = deltaSumPending + deltaSumOutstanding;


	    // Calculate Rates
	    utilization = deltaSumOutstanding/deltaTime;
	    runs_per_sec = 1000 *( deltaRuns/deltaTime);

	    avg_latency_per_run = 0;
	    avg_cpu_per_run = 0;
	    avg_wait_per_run = 0;
	    if(deltaRuns > 0) {
		avg_latency_per_run = deltaLatency/deltaRuns;
		avg_cpu_per_run = deltaSumOutstanding/deltaRuns;
	    }
	    if(deltaQueued > 0) {
		avg_wait_per_run = deltaSumPending/deltaQueued;
	    }

	    // Save SnapShot
	    snapshot_timestamp = now;
	    snapshot_sumOutstanding = sumOutstanding;
	    snapshot_sumPending = sumPending;
	    snapshot_ran = ran;
	    snapshot_queued = queued;

	    sendData(utilization, "utilization");
	    sendData(runs_per_sec, "runspersec");
	    sendData(avg_cpu_per_run, "avgcpuperrun");
	    sendData(avg_latency_per_run, "avglatencyperrun");
	    sendData(avg_wait_per_run, "avgwaitperrun");
	}

	private void sendData(double value, String tag) {
 	    Metric metric = new MetricImpl(new Double(value),
					   CREDIBILITY,
					   "",
					   PROVENANCE);
// 	    metricsUpdateService.updateValue(prefix+tag, PROVENANCE, metric);
 	    metricsUpdateService.updateValue(prefix+tag, metric);
	    //	    System.out.println("Metric:"+prefix+tag + "="+metric);
	}


	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (accumalate_timestamp > 0) {
		double deltaT = now - accumalate_timestamp;
		sumOutstanding += deltaT * outstanding;
		sumPending += deltaT * pending;
	    } 
	    accumalate_timestamp = now;
	}
    }

    private class SnapShotter implements Runnable {
	public void run() {
	    synchronized (records) {
		Iterator itr = records.values().iterator();
		while (itr.hasNext()) {
		    ConsumerRecord rec = (ConsumerRecord) itr.next();
		    rec.snapshot();
		}
	    }
	}
    }


    private Map<Object,ConsumerRecord> records = new HashMap<Object,ConsumerRecord>();
    private MetricsUpdateService metricsUpdateService;


    public SchedulerWatcher(ServiceBroker sb, String agent) {
	agentName = agent;
	metricsUpdateService = sb.getService(this, MetricsUpdateService.class, null);
	ThreadListenerService tls = sb.getService(this, ThreadListenerService.class, null);
	if (tls != null) tls.addListener(this);

	ThreadService tsvc = sb.getService(this, ThreadService.class, null);
	Runnable body = new SnapShotter();
	Schedulable sched = tsvc.getThread(this, body);
	sched.schedule(5000, 1000);
	sb.releaseService(this, ThreadService.class, tsvc);
    }

    ConsumerRecord findRecord(Object consumer) {
	ConsumerRecord rec = null;
	synchronized (records) {
	    rec = records.get(consumer);
	    if (rec == null) {
		rec = new ConsumerRecord(consumer);
		records.put(consumer, rec);
	    }
	}
	return rec;
    }


    public void threadQueued(Schedulable schedulable, 
			     Object consumer)  {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.pending;
    }

    public void threadDequeued(Schedulable schedulable, 
			       Object consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.pending;
	++rec.queued;
    }

    public void threadStarted(Schedulable schedulable, 
			      Object consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.outstanding;
    }

    public void threadStopped(Schedulable schedulable, 
			      Object consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.outstanding;
	++rec.ran;
    }

    public void rightGiven(String consumer) {
    }
		
    public void rightReturned(String consumer) {
    }
}
