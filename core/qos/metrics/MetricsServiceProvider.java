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
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;

/**
 * This Component provides trivial implementation of the
 * MetricsService, MetricsUpdateService and 
 * DataFeedRegistrationService.   Use this for a minimal COUGAAR in
 * which a real MetricsService is not required.
 */
public final class MetricsServiceProvider
    extends QosComponent
    implements ServiceProvider
{
    
    private MetricsService retriever;
    private MetricsUpdateService updater;
    private DataFeedRegistrationService registrar;

    @Override
   public void load() {
	super.load();
	updater = new NullMetricsUpdateServiceImpl() ;
	retriever = new NullMetricsServiceImpl ();
	// MetricService Implementation also implements Registration service
	registrar = (DataFeedRegistrationService) retriever;

	ServiceBroker sb = getServiceBroker();
	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
	ServiceBroker rootsb = ncs.getRootServiceBroker();

	rootsb.addService(MetricsUpdateService.class, this);
	rootsb.addService(MetricsService.class, this);
	sb.addService(DataFeedRegistrationService.class, this);
    }


    // This is done before child-components are created

    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MetricsService.class) {
	    return retriever;
	} else if (serviceClass == MetricsUpdateService.class) {
	    return updater;
	} else if (serviceClass == DataFeedRegistrationService.class) {
	    return registrar;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


}
