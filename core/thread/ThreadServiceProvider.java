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

import java.util.Iterator;
import java.util.List;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.StateModelException;

/**
 * This component is the {@link ServiceProvider} for the {@link
 * ThreadService}, {@link ThreadControlService}, {@link
 * ThreadListenerService}, and {@link ThreadStatusService}.
 *
 */
public final class ThreadServiceProvider 
    extends GenericStateModelAdapter
    implements ServiceProvider, Component
{

    private static final String SERVICE_TYPE_PROPERTY = 
	"org.cougaar.thread.service.type";

    private static ThreadPool[] pools;
    private static int[] lane_sizes = new int[ThreadService.LANE_COUNT];

    private static synchronized void makePools() 
    {
	if (pools != null) return;

	pools = new ThreadPool[ThreadService.LANE_COUNT];
	int initializationCount = 10; // could be a param
	for (int i=0; i<pools.length; i++)
	    pools[i] = new ThreadPool(lane_sizes[i], initializationCount,
				      "Pool-"+i);
    }

    private static synchronized void stopPools()
    {
        if (pools == null) return;
	for (int i=0; i<pools.length; i++) {
          pools[i].stopAllThreads();
        }
        pools = null;
    }

    static final boolean validateThreadStatusServiceClient(Object client)
    {
	return
	    (client instanceof TopServlet) ||
	    (client instanceof RogueThreadDetector) ||
	    (client instanceof org.cougaar.core.node.StateDumpServiceComponent);
    }



    private ServiceBroker sb;
    private boolean isRoot;
    private ThreadListenerProxy listenerProxy;
    private ThreadControlService controlProxy;
    private ThreadServiceProxy proxy;
    private ThreadStatusService statusProxy;
    private String name;
    private int laneCount = ThreadService.LANE_COUNT;
    private TrivialThreadServiceProvider threadServiceProvider;
    
    public ThreadServiceProvider() 
    {
    }

    public void setServiceBroker(ServiceBroker sb)
    {
        this.sb = sb;
    }

    @Override
   public void load() 
    {
	super.load();
	

	ServiceBroker the_sb = sb;
	isRoot = !the_sb.hasService(ThreadService.class);

	String type = SystemProperties.getProperty(SERVICE_TYPE_PROPERTY, 
					 "hierarchical");
	if (type.equals("trivial")) {
	    if (isRoot)	{
          threadServiceProvider = new TrivialThreadServiceProvider();
          threadServiceProvider.setServiceBroker(the_sb);
          threadServiceProvider.initialize();
          threadServiceProvider.load();
          threadServiceProvider.start();
        }
	    return;
	} else if (type.equals("single")) {
	    if (isRoot) {
          threadServiceProvider = new SingleThreadServiceProvider();
          threadServiceProvider.setServiceBroker(the_sb);
          threadServiceProvider.initialize();
          threadServiceProvider.load();
          threadServiceProvider.start();
        }
	    return;
	}

	// Hierarchical service

	makePools();

	// check if this component was added with parameters
        if (name == null) {
	    // Make default values from position in containment hierarcy
	    AgentIdentificationService ais = the_sb.getService(this, AgentIdentificationService.class, null);
	    MessageAddress agentAddr = ais.getMessageAddress();
	    the_sb.releaseService(this, AgentIdentificationService.class, ais);
	    
	    NodeIdentificationService nis = the_sb.getService(this, NodeIdentificationService.class, null);
	    MessageAddress nodeAddr = nis.getMessageAddress();
	    the_sb.releaseService(this, NodeIdentificationService.class, nis);

	    name = 
		isRoot ?
		"Node "+nodeAddr :
		"Agent_"+agentAddr;
        }

	if (isRoot) {
	    /*Starter.startThread();
	    Reclaimer.startThread();*/
	    SchedulableStateChangeQueue.startThread();
	    // use the root service broker
	    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
	    the_sb = ncs.getRootServiceBroker();

	}
	
	ThreadService parent = the_sb.getService(this, ThreadService.class, null);
	final TreeNode node = makeProxies(parent);
	provideServices(the_sb);
	if (isRoot) {
	    statusProxy = new ThreadStatusService() {
		    public int iterateOverStatus(ThreadStatusService.Body body) 
		    {
			return 
			    node.iterateOverQueuedThreads(body) +
			    node.iterateOverRunningThreads(body);
		    }
		};
	    the_sb.addService(ThreadStatusService.class, this);
	}
    }

    /**
     * Gracefully unload this component.
     * <p>
     * @see org.cougaar.util.GenericStateModelAdapter#unload()
     */
    @Override
   public synchronized void unload() throws StateModelException {
      
      if (threadServiceProvider == null) {
          // Unload hierarchical thread service and Timers
          if (isRoot) {
              TreeNode.releaseTimer();
              SchedulableStateChangeQueue.stopThread();
              stopPools();
          }
      } else {
          // Unload singleton ThreadServiceProvider and Timer
          TreeNode.releaseTimer();
          threadServiceProvider.halt();
          threadServiceProvider.unload();
          threadServiceProvider = null;
      }

      super.unload();
    }

    private void setParameterFromString(String property) 
    {
	int sepr = property.indexOf('=');
	if (sepr < 0) return;
	String key = property.substring(0, sepr);
	String value = property.substring(++sepr);
	int lane_index, lane_max;

	if (key.equals("name")) {
	    name = value;
	} else if (key.equals("isRoot")) {
	    isRoot = value.equalsIgnoreCase("true");
	} else if (key.equals("BestEffortAbsCapacity")) {
	    lane_index = ThreadService.BEST_EFFORT_LANE;
	    lane_max = Integer.parseInt(value);
	    lane_sizes[lane_index] = lane_max;
	} else if (key.equals("WillBlockAbsCapacity")) {
	    lane_index = ThreadService.WILL_BLOCK_LANE;
	    lane_max = Integer.parseInt(value);
	    lane_sizes[lane_index] = lane_max;
	} else if (key.equals("CpuIntenseAbsCapacity")) {
	    lane_index = ThreadService.CPU_INTENSE_LANE;
	    lane_max = Integer.parseInt(value);
	    lane_sizes[lane_index] = lane_max;
	} else if (key.equals("WellBehavedAbsCapacity")) {
	    lane_index = ThreadService.WELL_BEHAVED_LANE;
	    lane_max = Integer.parseInt(value);
	    lane_sizes[lane_index] = lane_max;
	} // add more later
    }

    public void setParameter(Object param) 
    {
	if (param instanceof List) {
	    Iterator itr = ((List) param).iterator();
	    while(itr.hasNext()) {
		setParameterFromString((String) itr.next());
	    }
	} else if (param instanceof String) {
	    setParameterFromString((String) param);
	}
    }

    private Scheduler makeScheduler(Object[] args,
				    int lane)
				   
    {
	Scheduler scheduler =  new PropagatingScheduler(listenerProxy);

	scheduler.setLane(lane);
	scheduler.setAbsoluteMax(lane_sizes[lane]);
	return scheduler;
    }

    private TreeNode makeProxies(ThreadService parent) 
    {
	listenerProxy = new ThreadListenerProxy(laneCount);

	Object[] actuals = { listenerProxy };
	Scheduler[] schedulers = new Scheduler[laneCount];
	for (int i=0; i<schedulers.length; i++) {
	    schedulers[i] = makeScheduler(actuals, i);
	}
	

	ThreadServiceProxy parentProxy = (ThreadServiceProxy) parent;
	TreeNode node = new TreeNode(schedulers, pools, name, parentProxy);
	proxy = new ThreadServiceProxy(node);
	controlProxy = new ThreadControlServiceProxy(node);
	listenerProxy.setTreeNode(node);
	return node;
    }

    private void provideServices(ServiceBroker the_sb) 
    {
	the_sb.addService(ThreadService.class, this);
	the_sb.addService(ThreadControlService.class, this);
	the_sb.addService(ThreadListenerService.class, this);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == ThreadService.class) {
	    return proxy;
	} else if (serviceClass == ThreadControlService.class) {
	    // Later this will be tightly restricted
	    return controlProxy;
	} else if (serviceClass == ThreadListenerService.class) {
	    return listenerProxy;
	} else if (serviceClass == ThreadStatusService.class) {
	    if (validateThreadStatusServiceClient(requestor))
		return statusProxy;
	    else
		return null;
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

