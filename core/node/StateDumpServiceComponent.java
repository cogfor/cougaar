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

package org.cougaar.core.node;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageQueueDumpService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.SuicideService;
import org.cougaar.core.thread.CougaarThread;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadStatusService;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public final class StateDumpServiceComponent
    extends ParameterizedComponent
    implements ServiceProvider
{
    private static Logger logger = Logging.getLogger(StateDumpServiceComponent.class);
    private ServiceBroker sb, rootsb;
    private Impl impl;
    private int waitTime;

    public StateDumpServiceComponent()
    {
    }


    public void setNodeControlService(NodeControlService ncs) { 
	rootsb = (ncs == null)?null:ncs.getRootServiceBroker();
    }

    @Override
   public void load()
    {
	super.load();
	impl = new Impl(sb);
	rootsb.addService(StateDumpService.class, this);
    }

    @Override
   public void start()
    {
	super.start();
	waitTime = (int) getParameter("waitTime", 10000);
	impl.threadStatus = sb.getService(this, ThreadStatusService.class, null);
	// test for forcing Suicide 
	int dieTime = (int) getParameter("dieTime", 0);
	if (dieTime > 0) {
	    java.util.Timer timer = new java.util.Timer();
	    java.util.TimerTask task = new java.util.TimerTask() {
		    @Override
         public void run() {
			SuicideService svc = sb.getService(this, SuicideService.class, null);
			Throwable thr = new Error("Pointless suicide");
			svc.die(StateDumpServiceComponent.this, thr);
		    }
		};
	    timer.schedule(task, dieTime);
	}
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor,
			     Class serviceClass)
    {
	if (serviceClass == StateDumpService.class)
	    return impl;
	else
	    return null;
    }


    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


    public final void setBindingSite(BindingSite bs) {
	this.sb = bs.getServiceBroker();
    }

    private class Impl
	implements StateDumpService, ThreadStatusService.Body
    {
	ThreadStatusService threadStatus;
	MessageQueueDumpService mqds;
	ServiceBroker sb;

	Impl(ServiceBroker sb)
	{
	    this.sb = sb;
	}

	private String getTopJavaPid(Properties props)
	{
	    String last_pid = props.getProperty("PPid");
	    String previous_pid =  props.getProperty("Pid");
	    try {
		FileInputStream fis = new FileInputStream("/proc/" 
							  +last_pid+
							  "/status");
		props.load(fis);
		String name = props.getProperty("Name");
		if (name.equals("java")) {
		    return getTopJavaPid(props);
		} else {
		    return previous_pid;
		}
	    } catch (Exception ex) {
		logger.error(null, ex);
		return null;
	    }
	}

	private String getTopJavaPid()
	{
	    Properties props = new Properties();
	    props.setProperty("PPid", "self");
	    return getTopJavaPid(props);
	}

	private void dumpThreads()
	{
	    // Only works in Linux
	    String os_name = SystemProperties.getProperty("os.name");
	    if (!os_name.equals("Linux")) {
		logger.warn("Can't find pid to dump threads in " + os_name);
		return;
	    }
	    logger.warn("Dumping Java Threads");
	    String pid = getTopJavaPid();
	    if (pid != null) {
		String command = "kill -3 " + pid;
		logger.warn(command);
		try {
		    Runtime.getRuntime().exec(command);
		} catch (Exception ex) {
		    logger.error(null, ex);
		}
	    }
	}

	public void run(String scheduler, Schedulable schedulable)
	{
	    int state = schedulable.getState();
	    String state_string = null;
	    if (state == CougaarThread.THREAD_PENDING)
		state_string = "Queued";
	    else if (state == CougaarThread.THREAD_RUNNING)
		state_string  = "Running";
	    else
		return; // skip dormant Schedulables

	    long elapsed = 
		System.currentTimeMillis()-schedulable.getTimestamp();

	    logger.warn("Schedulable " +
			" " + state_string +
			" " + elapsed +
			" " + scheduler +
			" " + schedulable.getLane() +
			" " + schedulable.getName()+
			" " + schedulable.getConsumer()
			);
	}

	private void dumpSchedulables()
	{
	    logger.warn("Dumping Schedulables");
	    int count = threadStatus.iterateOverStatus(this);
	    logger.warn("Dumped " +count+ " Schedulables");
	}

	private void dumpQueues()
	{
	    if (mqds == null) {
		mqds = sb.getService(this, MessageQueueDumpService.class, null);
		if (mqds == null) {
		    logger.warn("Couldn't get MessageQueueDumpService");
		    return;
		}
	    }

	    logger.warn("Dumping Message Queues");
	    int count=mqds.dumpQueues(logger);
	    logger.warn("Dumped " +count+ " Messages");
	}

	private void dumpMemoryProfile() 
	{
	    PrintStream out = System.out;
	    try {
		Class cl = Class.forName("org.cougaar.profiler.Dump");
		Class[] params = {PrintStream.class};
		Object[] args = {out};
		java.lang.reflect.Method m = cl.getMethod("dumpTo", params );
		m.invoke(null, args);
	    } catch (Exception e) {
		logger.warn("Memory Profiler not found: "+e);
	    }
        }

	private void dumpObject(UniqueObject object)
	{
	    logger.warn(object.toString());
	}

	private void dumpBlackboard()
	{
	    BlackboardQueryService svc = sb.getService(this, BlackboardQueryService.class, null);
	    if (svc != null) {
		UnaryPredicate pred = new UnaryPredicate() {
			/**
          * 
          */
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
			    return (o instanceof UniqueObject);
			}
		    };
		Collection objects = svc.query(pred);
		Iterator itr = objects.iterator();
		logger.warn("Dumping " +objects.size()+ " Blackboard objects");
		while (itr.hasNext()) {
		    UniqueObject object = (UniqueObject) itr.next();
		    dumpObject(object);
		}
	    } else {
		logger.warn("Couldn't get BlackboardQueryService");
	    }
	}

	public void dumpState()
	{
	    try {
		dumpSchedulables();
		dumpQueues();
		dumpBlackboard();
		dumpMemoryProfile();
		dumpThreads();
	    } catch (Throwable any) {
		logger.error(null, any);
	    }
	    // Give dumpers time to catch up
	    logger.warn("waiting " + waitTime/1000+ " seconds");
	    try { Thread.sleep(waitTime); }
	    catch (InterruptedException ex) {}
	}
    }

}

