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

import org.cougaar.core.service.ThreadService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * An optional utility class for use by {@link java.lang.Runnable}s
 * running in a pooled {@link org.cougaar.core.service.ThreadService}
 * thread to tell the {@link ThreadService} why they are running
 * so long (for example, due to a blocking I/O call). 
 * <p>
 * For example:<pre> 
 *    try {
 *      SchedulableStatus.beginNetIO("download url");
 *      // open a URLConnection, read from socket, etc.
 *    } finally {
 *      SchedulableStatus.endBlocking();
 *    }
 * </pre> 
 * <p>
 * This information is shown in the {@link TopPlugin}'s "/threads/top"
 * servlet.
 */
public final class SchedulableStatus
{
    public static final int NOT_BLOCKING = -1;
    public static final int OTHER = 0;
    public static final int WAIT = 1;
    public static final int FILEIO = 2;
    public static final int NETIO = 3;
    public static final int CPUINTENSIVE = 4;

    private static Logger logger = 
	Logging.getLogger("org.cougaar.core.thread.SchedulableStatus");

    private static boolean checkLegalBlocking(int type, 
					      SchedulableObject schedulable) 
    {
	int lane = schedulable.getLane();
	switch (lane) {
	case ThreadService.BEST_EFFORT_LANE:
	    return true;

	case ThreadService.WILL_BLOCK_LANE:
	    return true;

	case ThreadService.CPU_INTENSE_LANE:
	    if (type == WAIT || type == CPUINTENSIVE) return true;
	    if (logger.isWarnEnabled())
		logger.warn(schedulable.getName() +
			    " is in CPU_INTENSE_LANE but is blocking on  "
			    + statusString(type,"") );
	    return false;

	case ThreadService.WELL_BEHAVED_LANE:
	    if (type == WAIT) return true;
	    if (logger.isWarnEnabled())
		logger.warn(schedulable.getName() +
			    " is in WELL_BEHAVED_LANE but is blocking on  "
			    + statusString(type,"") );
	    return false;

	default:
	    return true;
	}
    }

    public static void beginBlocking(int type, String excuse) 
    {
	Thread thread = Thread.currentThread();
	if (thread instanceof ThreadPool.PooledThread) {
	    ThreadPool.PooledThread pthread = (ThreadPool.PooledThread) thread;
	    SchedulableObject sched = pthread.getSchedulable();
	    checkLegalBlocking(type, sched);
	    sched.setBlocking(type, excuse == null ? "No excuse given" : excuse);
	}
    }

    public static void beginWait(String excuse) {
	beginBlocking(WAIT, excuse);
    }

    public static void beginFileIO(String excuse) {
	beginBlocking(FILEIO, excuse);
    }

    public static void beginNetIO(String excuse) {
	beginBlocking(NETIO, excuse);
    }

    public static void beginCPUIntensive(String excuse) {
	beginBlocking(NETIO, excuse);
    }

    public static void endBlocking() 
    {
	Thread thread = Thread.currentThread();
	if (thread instanceof ThreadPool.PooledThread) {
	    ThreadPool.PooledThread pthread = (ThreadPool.PooledThread) thread;
	    SchedulableObject sched = pthread.getSchedulable();
	    sched.clearBlocking();
	}
    }

  public static void withBlocking(int type, String excuse, Runnable thunk) {
    try {
      beginBlocking(type, excuse);
      thunk.run();
    } finally {
      endBlocking();
    }
  }
  public static void withWait(String excuse, Runnable thunk) {
    withBlocking(WAIT, excuse, thunk);
  }
  public static void withFileIO(String excuse, Runnable thunk) {
    withBlocking(FILEIO, excuse, thunk);
  }
  public static void withNetIO(String excuse, Runnable thunk) {
    withBlocking(NETIO, excuse, thunk);
  }
  public static void withCPUIntensive(String excuse, Runnable thunk) {
    withBlocking(CPUINTENSIVE, excuse, thunk);
  }


    public static String statusString(int type, String excuse)
    {
	String string = excuse;
	switch (type) {
	case NOT_BLOCKING:
	    string = "none";
	    break;
	case OTHER:
	    break;
	case WAIT:
	    string = "Lock wait: " + string;
	    break;
	case FILEIO:	
	    string = "Disk I/O: " + string;
	    break;
	case NETIO:
	    string = "Network I/O: " + string;
	    break;
	case CPUINTENSIVE:
	    string = "CPU Intensive: " + string;
	    break;
	}
	return string;
    }

}
