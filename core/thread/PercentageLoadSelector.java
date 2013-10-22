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

import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.PropertyParser;

/**
 * A sample {@link RightsSelector} that attempts to select among the
 * children in such a way as to match a set of target percentages.
 * 
 * @property org.cougaar.thread.targets Specifies a file which
 * contains percentage targets for the children.
 */
public class PercentageLoadSelector
    	extends RoundRobinSelector
    	implements ThreadListener {

    private static final String TARGETS_PROP = "org.cougaar.thread.targets";
    private Map<String,ConsumerRecord> records = new HashMap<String,ConsumerRecord>();
    private Properties properties = new Properties();
    private Comparator<Scheduler> comparator;
    private TreeSet<Scheduler> orderedChildren;


    public PercentageLoadSelector(ServiceBroker sb) {
	String propertiesFilename = SystemProperties.getProperty(TARGETS_PROP);
	if (propertiesFilename != null) {
	    try {
		FileInputStream fos = new FileInputStream(propertiesFilename);
		properties.load(fos);
		fos.close();
	    } catch (java.io.IOException ex) {
		ex.printStackTrace();
	    }
	}

	comparator = new DistanceComparator();


	ThreadListenerService tls = sb.getService(this, ThreadListenerService.class, null);
	if (tls != null) tls.addListener(this);

	ThreadService tsvc = sb.getService(this, ThreadService.class, null);
	Runnable body = new SnapShotter();
	Schedulable sched = tsvc.getThread(this, body);
	sched.schedule(5000, 1000);
	sb.releaseService(this, ThreadService.class, tsvc);
    }

    ConsumerRecord findRecord(String name) {
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


    public void threadQueued(Schedulable schedulable, Object consumer)  {
    }
    
    public void threadDequeued(Schedulable schedulable, Object consumer) {
    }
    
    public void threadStarted(Schedulable schedulable, Object consumer) {
    }
    
    public void threadStopped(Schedulable schedulable, Object consumer) {
    }

    public void rightGiven(String consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.outstanding;
    }
		
    public void rightReturned(String consumer) {
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.outstanding;
   }



    private double getSchedulerDistance(Scheduler scheduler) {
	ConsumerRecord rec = findRecord(scheduler.getName());
	if (rec != null) {
	    return rec.distance();
	} else {
	    return 1;
	}
    }

    // RightsSelector

    // Too inefficient to use but simple to write...
    private void rankChildren() {
	TreeSet<Scheduler> result = new TreeSet<Scheduler>(comparator);
        List<TreeNode> children = scheduler.getTreeNode().getChildren();
	for (TreeNode child : children) {
	    result.add(child.getScheduler(scheduler.getLane()));
	}
	result.add(scheduler);
	orderedChildren = result;
    }

    @Override
   public SchedulableObject getNextPending() {
	if (orderedChildren == null) {
	    // Snapshotter hasn't run yet.  Round-robin instead.
	    return super.getNextPending();
	}
	// Choose the one with the largest distance()
	Iterator<Scheduler> itr = orderedChildren.iterator();
	Scheduler next = null;
	SchedulableObject handoff = null;
	while(itr.hasNext()) {
	    next = itr.next();
	    // The list contains the scheduler itself as well as its
	    // children, In the former case we can't call
	    // getNextPending since that will recurse forever.  We
	    // need the super method, conveniently available as
	    // getNextPendingSuper.
	    if (next == scheduler) {
		handoff = scheduler.popQueue();
	    } else {
		handoff = next.getNextPending();
	    }
	    if (handoff != null) {
		// If we're the parent of the Scheduler to which the
		// handoff is given, increase the local count.
		if (next != scheduler) {
		    scheduler.incrementRunCount(next);
		}
		return handoff;
	    }
	}
	return null;
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
	    rankChildren();
	}
    }


    private class DistanceComparator implements Comparator<Scheduler> {
	private int hashCompare(Object o1, Object o2) {
	    if (o1.hashCode() < o2.hashCode())
		return -1;
	    else
		return 1;
	}

	public int compare(Scheduler o1, Scheduler o2) {
	    if (o1 == o2) return 0;

	    Scheduler x = o1;
	    Scheduler y = o2;
	    double x_distance = getSchedulerDistance(x);
	    double y_distance = getSchedulerDistance(y);
			

	    // Smaller distances are less preferable
	    if (x_distance < y_distance)
		return 1;
	    else if (x_distance > y_distance) 
		return -1;
	    else 
		return hashCompare(o1, o2);
	}

    }

    private class ConsumerRecord {
	String name;
	int outstanding;
	long timestamp;
	double accumulator;
	long snapshot_timestamp;
	double snapshot_accumulator;
	double rate;
	double targetPercentage;

	ConsumerRecord(String name) {
	    this.name = name;
	    targetPercentage = 	
		PropertyParser.getDouble(properties, name, .05);
	    System.err.println(name+ " target=" +targetPercentage);
	}

	synchronized void snapshot() {
	    long now = System.currentTimeMillis();
	    rate = (accumulator-snapshot_accumulator)/
		(now-snapshot_timestamp);
	    snapshot_timestamp = now;
	    snapshot_accumulator = accumulator;
	    System.out.println(name+ " rate=" +rate);
	}

	double distance() {
	    return (targetPercentage-rate)/targetPercentage;
	}

	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (timestamp > 0) {
		double deltaT = now - timestamp;
		accumulator += deltaT * outstanding;
	    } 
	    timestamp = now;
	}
    }


}
