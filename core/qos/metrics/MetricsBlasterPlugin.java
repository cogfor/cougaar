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

import java.util.Observable;
import java.util.Observer;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

/**
 * This test Plugin publishes data into the MetricsUpdateService at
 * high rates and subscribes to formulas using that data.  The
 * arguments are <code>key</code> (the publish key) and
 * <code>path</code> (the subscription path).
 */
public class MetricsBlasterPlugin
    extends org.cougaar.core.plugin.ParameterizedPlugin
    implements Observer
{
    
    private MetricsUpdateService update;
    private MetricsService svc;
    private ThreadService tsvc;

    private String key,path;
	
    private long callbackCount =0;
    private long blastCount=0;
    private long lastCallbackDelay=0;
    private long lastPrintTime=0;

    private int restCount = 0;
    private Schedulable blastTask;
    private Schedulable restTask;
	

    private void dumpCounters(long now) {
	if (1000 <  (now - lastPrintTime)){
	    System.out.println("blast count=" +blastCount+
			       " callback count=" +callbackCount+
			       " Last delay=" +lastCallbackDelay);
	    lastPrintTime=now;
	}
    }

    @Override
   public void execute() {
    }

    @Override
   public void setupSubscriptions() {
    }


    @Override
   public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	update = sb.getService(this, MetricsUpdateService.class, null);
	svc = sb.getService(this, MetricsService.class, null);
	tsvc = sb.getService(this, ThreadService.class, null);

	path = getParameter("path");
	if (path != null) {
	    svc.subscribeToValue(path, this);
	    System.out.println("Subscribed to " +path);


	    key = getParameter("key");
	    if (key !=null) {
		System.out.println("Blasting to " +key);
		Blast blast = new Blast();
		blastTask = tsvc.getThread(this, blast, "Blaster");
		blastTask.schedule(10000);
	    }

	}
    }


    public void update(Observable o, Object arg) {
	callbackCount++;
	long now = System.currentTimeMillis();
	long value = ((Metric) arg).longValue();
	lastCallbackDelay = now - value;
    }

    private class Blast implements Runnable {
	public void run() {
	    System.out.println("Starting Blaster");
	    long startTime =  System.currentTimeMillis();
	    long startBlast = blastCount;
	    long startCallback = callbackCount;

	    long now = startTime;
	    // Blast for 5 seconds and then stop
	    while (5000 > (now-startTime)) {
		now =  System.currentTimeMillis();
		Metric m = new MetricImpl(new Long(now),
					  0.3,
					  "", "MetricsTestAspect");
		update.updateValue(key, m);
		blastCount++;
		dumpCounters(now);
	    }
	    float deltaBlasts = (blastCount-startBlast);
	    float deltaCallback = (callbackCount-startCallback);
	    long deltaTime = now - startTime;
	    float blastRate = deltaBlasts/deltaTime;
	    float callbackRate = deltaCallback/deltaTime;
	    float callbackPercent = deltaBlasts > 0 ? 
		(100*deltaCallback)/deltaBlasts :
		0.0f;
	    System.out.println("Stopped Blaster:" +
			       "blasts/millisec =" +  blastRate +
			       "callback/millisec =" +  callbackRate +
			       " Callback % =" + callbackPercent);

	    restCount = 0;
	    Rest rest = new Rest();
	    restTask = tsvc.getThread(this, rest, "Rest");
	    restTask.schedule(0, 1000);
	}
    }

    private class Rest implements Runnable {
	public void run() {
	    dumpCounters(System.currentTimeMillis());
	    if (restCount++ == 10) {
		restTask.cancelTimer();
		blastTask.start();
	    }
	}
    }


}
