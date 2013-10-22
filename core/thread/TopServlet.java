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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.servlet.ServletFrameset;


/**
 * This servlet displays a more or less current list of {@link
 * Schedulable}s, both running and queued, at all levels and for all
 * lanes, in a way that's vaguely remniscent of the unix 'top'
 * command.  It's created by {@link TopPlugin} and uses the {@link
 * ThreadStatusService} to get its snapshot lists.  The access path is
 * <b>/threads/top</b>.
 */
final class TopServlet extends ServletFrameset {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static final long THRESHOLD = 1000;
    private ThreadStatusService statusService;
    private ThreadControlService controlService;

    private static class Record {
	Record(String scheduler, Schedulable schedulable, boolean queued) {
	    this.scheduler = scheduler;
	    this.schedulable = schedulable;
	    this.queued = queued;
	    elapsed = System.currentTimeMillis() - schedulable.getTimestamp();

	}

	String scheduler;
	Schedulable schedulable;
	long elapsed;
	boolean queued;
    }



    // Higher times appear earlier in the list
    private Comparator<Record> comparator = new Comparator<Record>() {
	    public int compare(Record r, Record s) {
		if (r.elapsed == s.elapsed) {
		    return 0;
		} else if (r.elapsed < s.elapsed) {
		    return 1;
		} else {
		    return -1;
		}
	    }

	    @Override
      public boolean equals(Object x) {
		return x == this;
	    }
	};



    public TopServlet(ServiceBroker sb)  {
	super(sb);

	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);

	if (ncs == null) {
	    throw new RuntimeException("Unable to obtain service");
	}

	ServiceBroker rootsb = ncs.getRootServiceBroker();

	statusService = rootsb.getService(this, ThreadStatusService.class, null);
	controlService = rootsb.getService(this, ThreadControlService.class, null);

	if (statusService == null) {
	    throw new RuntimeException("Unable to obtain service");
	}
    }


    private void printHeaders(PrintWriter out) {
	out.print("<tr>");
	out.print("<th align=left><b>State</b></th>");
	out.print("<th align=left><b>Blocking</b></th>");
	out.print("<th align=left><b>Time(ms)</b></th>");
	out.print("<th align=left><b>Level</b></th>");
	out.print("<th align=left><b>Lane</b></th>");
	out.print("<th align=left><b>Thread</b></th>");
	out.print("<th align=left><b>Client</b></th>");
	out.print("</tr>");
    }

    private void printCell(String data, boolean queued, PrintWriter out) {
	out.print("<td>");
	if (queued) out.print("<i>");
	out.print(data);
	if (queued) out.print("</i>");
	out.print("</td>");
    }

    private void printCell(long data, boolean queued, PrintWriter out) {
	out.print("<td align=right>");
	if (queued) out.print("<i>");
	out.print(data);
	if (queued) out.print("</i>");
	out.print("</td>");
    }


    private void printRecord(Record record,
			     PrintWriter out) {

	if (record.elapsed > THRESHOLD) {
	    out.print("<tr bgcolor=\"#ffeeee\">"); // pale-pink background
	} else {
	    out.print("<tr>");
	}
	printCell(record.queued ? "queued" : "running", record.queued, out);

	String b_string = 
	    SchedulableStatus.statusString(record.schedulable.getBlockingType(),
					   record.schedulable.getBlockingExcuse());

	printCell(b_string, record.queued, out);

	printCell(record.elapsed, record.queued, out);
	printCell(record.scheduler, record.queued, out);
	printCell(record.schedulable.getLane(), record.queued, out);
	printCell(record.schedulable.getName(), record.queued, out);
	printCell(record.schedulable.getConsumer().toString(), record.queued, out);
	out.print("</tr>");
    }

    @Override
   public void printBottomPage(HttpServletRequest request, PrintWriter out) {
	// lane key
	out.print("Lane 0: Best Effort");
	out.print("<br>Lane 1: Will Block");
	out.print("<br>Lane 2: CPU Intensive");
	out.print("<br>Lane 3: Well Behaved");
    }

    private void printSummary(List status, PrintWriter out) {
	int running = 0;
	int queued = 0;
	int total = status.size();
	int[] run_counts = new int[ThreadService.LANE_COUNT];

	Iterator itr = status.iterator();
	while (itr.hasNext()) {
	    Record record = (Record) itr.next();
	    if (record.queued) {
		++queued;
	    } else {
		++running;
		++run_counts[record.schedulable.getLane()];
	    }
	}

	out.print("<br><br><b>");
	out.print(total );
	out.print(" thread");
	if (total != 1) out.print('s');
	out.print(": " );
	out.print(queued);
	out.print(" queued, ");
	out.print(running);
	out.print(" running");
	if (controlService != null) {
	    out.print(", running/max (per lane):");
	    for (int i=0; i<ThreadService.LANE_COUNT; i++) {
		out.print(i==0 ? " " : ", ");
		out.print(run_counts[i]);
		out.print("/");
		out.print(controlService.maxRunningThreadCount(i));
	    }
	}
	out.print("</b>");
    }



    // Implementations of ServletFrameset's abstract methods

    @Override
   public String getPath() {
	return "/threads/top";
    }

    @Override
   public String getTitle() {
	return "Threads";
    }

    @Override
   public void printPage(HttpServletRequest request, PrintWriter out) {
	final List<Record> status = new ArrayList<Record>();
	ThreadStatusService.Body body = new ThreadStatusService.Body () {
		public void run(String scheduler, Schedulable schedulable)
		{
		    Record record = null;
		    int state = schedulable.getState();
		    if (state == CougaarThread.THREAD_PENDING) {
			record = new Record(scheduler, schedulable, true);
		    } else if (state == CougaarThread.THREAD_RUNNING) {
			record = new Record(scheduler, schedulable, false);
		    } else {
			return; // ignore this one
		    }

		    status.add(record);
		}
	    };
	
	statusService.iterateOverStatus(body);

	printSummary(status, out);

	if (status.size() == 0) {
	    // Nothing more to print
	    return;
	}

	out.print("<hr>");
	// Sort the records by time
	java.util.Collections.sort(status, comparator);

	out.print("<table>");
	printHeaders(out);

	for (Record record : status) {
	    printRecord(record, out);
	}
	
	out.print("</table>");
    }

}
