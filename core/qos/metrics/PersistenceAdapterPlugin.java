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


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PersistenceMetricsService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

/**
 * Gathers persistence metrics from the PersistenceMetricsService and
 * publishes them as Metrics into the MetricsUpdateService.  Should be
 * loaded into every Agent.
 *
 * @see PersistenceMetricsService
 */
public class PersistenceAdapterPlugin
    extends ComponentPlugin
    implements Runnable, Constants
{
    private PersistenceMetricsService  pms;
    private LoggingService loggingService;
    private MetricsUpdateService mus;
    private Schedulable schedulable;
    private String key;

    @Override
   public void load() {
	super.load();
	
	ServiceBroker sb = getServiceBroker();
	
	loggingService = sb.getService(this, LoggingService.class, null);

	pms = sb.getService(this, PersistenceMetricsService.class, null);
	if (pms == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get PersistenceMetricsService");
	    return;
	} 

	mus = sb.getService(this, MetricsUpdateService.class, null);
	if (mus == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get MetricsUpdateService");
	    return;
	} 

	ThreadService tsvc = sb.getService(this, ThreadService.class, null);
	if (tsvc == null) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Couldn't get ThreadService");
	    return;
	} 


	key = "Agent" +KEY_SEPR+ getAgentIdentifier() +KEY_SEPR+
	    PERSIST_SIZE_LAST;

	schedulable = tsvc.getThread(this, this, "PersistenceAdapter");
	schedulable.schedule(0, 10000);
	
	sb.releaseService(this, ThreadService.class, tsvc);
			       

    }
	

    // Runnable
    public void run() {
	PersistenceMetricsService.Metric[] metrics =
	    pms.getAll(PersistenceMetricsService.FULL);
	long maxSize = 0;
	for (int i = 0, n = metrics.length; i < n; i++) {
	    maxSize = Math.max(maxSize, metrics[i].getSize());
	}

	if (maxSize > 0) {
	    Metric metric = new MetricImpl(maxSize, 
					   SECOND_MEAS_CREDIBILITY,
					   "bytes",
					   "PersistenceMetricsService");
	    mus.updateValue(key, metric);
	    if (loggingService.isDebugEnabled())
		loggingService.debug("Updating " +key+ " to " +metric);
	} else {
	    if (loggingService.isDebugEnabled())
		loggingService.debug(key + " is still 0 ");
	}
    }

    // Plugin methods
    @Override
   protected void setupSubscriptions() {
	// None in this example
    }

    @Override
   public void execute() {
	// Not relevant in this example
    }


}

