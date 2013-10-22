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

import java.io.PrintWriter;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;

/**
 * This Servlet collects cpu and message metrics for each local Agent
 * and displays the result in a nicely formatted web page.  In the <a
 * href="../../../../../../OnlineManual/MetricsService/sensors.html">Sensor
 * Data Flow</a> pattern this class plays the role of <b>Servlet</b>
 * for load data (CPU load average, CPU) and messages sizes and counts
 * for Agents, Nodes and Services.
 *
 * @see org.cougaar.core.thread.AgentLoadRatePlugin
 * @see org.cougaar.core.thread.AgentLoadSensorPlugin
 * @see AgentStatusService
 * @see AgentStatusRatePlugin
 */
public class AgentLoadServlet 
    extends MetricsServlet
{
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private AgentStatusService agentStatusService; 

    public AgentLoadServlet(ServiceBroker sb) {
	super(sb);

	agentStatusService = sb.getService(this, AgentStatusService.class, null);
    }

    @Override
   public String getPath() {
	return "/metrics/agent/load";
    }

    @Override
   public String getTitle () {
	return "Agent Load for Node " + getNodeID();
    }

    public void printRow(MessageAddress name, 
			 String type,
			 PrintWriter out) 
    {
	String sizeFormula = null;
	String label = name.getAddress();

	if (type.equals("Agent")) {
	    sizeFormula = PERSIST_SIZE_LAST;
	} else {
	    sizeFormula = "VMSize";
	}

	String path = type + "(" +name+ ")" + PATH_SEPR;
	// Get Metrics
	Metric cpuLoad = metricsService.getValue(path
						 +CPU_LOAD_AVG+
						 "(" +_10_SEC_AVG+ ")");
	
	Metric cpuLoadJips = 
	    metricsService.getValue(path
				    +CPU_LOAD_MJIPS+
				    "(" +_10_SEC_AVG+ ")");
	Metric msgIn = metricsService.getValue(path
					       +MSG_IN+ 
					       "("+_10_SEC_AVG+")");
	Metric msgOut = metricsService.getValue(path
						+MSG_OUT+
						"("+_10_SEC_AVG+")");
	Metric bytesIn = metricsService.getValue(path
						 +BYTES_IN+
						 "(" +_10_SEC_AVG+")");
	Metric bytesOut = metricsService.getValue(path
						  +BYTES_OUT+
						  "(" +_10_SEC_AVG+")");
	Metric size = metricsService.getValue(path + sizeFormula );


	//output Row
	out.print("<tr><td>");
	out.print(label);
	out.print("</td>");
	ServletUtilities.valueTable(cpuLoad, 0.0, 1.0,true, f4_2, out);
	ServletUtilities.valueTable(cpuLoadJips, 0.0, 200,true, f6_3, out);
	ServletUtilities.valueTable(msgIn, 0.0, 1.0, true, f4_2, out);
	ServletUtilities.valueTable(msgOut, 0.0, 1.0, true, f4_2, out);
	ServletUtilities.valueTable(bytesIn, 0.0, 10000, true, f7_0, out);
	ServletUtilities.valueTable(bytesOut, 0.0, 10000, true, f7_0, out);
	ServletUtilities.valueTable(size, 0.0, 10000, true, f7_0, out);
	out.print("</tr>\n");
    }


    private void printServiceRow(String key, PrintWriter out)
    {
	out.print("<tr><td>");
	out.print(key);
	out.print("</td>");
	String load_path = "Service(" +key+ ")" +PATH_SEPR+ 
	    CPU_LOAD_AVG +"("+ _10_SEC_AVG +")";
	Metric load = metricsService.getValue(load_path);
	ServletUtilities.valueTable(load, 0.0, 1.0,true, f4_2, out);
	String jips_path = "Service(" +key+ ")" +PATH_SEPR+ 
	    CPU_LOAD_MJIPS +"("+ _10_SEC_AVG +")";
	Metric jips = metricsService.getValue(jips_path);
	ServletUtilities.valueTable(jips, 0.0, 500.0,true, f6_3, out);
	out.print("</tr>\n");
    }

    @Override
   public void printPage(HttpServletRequest request, PrintWriter out) {
	// Get list of All Agents On this Node
	if (agentStatusService == null) return;
	java.util.Set localAgents = agentStatusService.getLocalAgents();
	if (localAgents == null) return;

	// Space between the intro and the content
	out.print("<br><br>");

	//Header Row
	out.print("<table border=\"3\" cellpadding=\"2\"  rules=\"groups\">\n");
	out.print("<colgroup> <colgroup span=\"2\"><colgroup span=\"4\"><colgroup>");
	out.print("<tr><b>");
	out.print("<th><b>NODE</b></th>");
	out.print("<th><b>CPUloadAvg</b></th>");
	out.print("<th><b>CPUloadMJIPS</b></th>");
	out.print("<th><b>MsgIn</b></th>");
	out.print("<th><b>MsgOut</b></th>");
	out.print("<th><b>BytesIn</b></th>");
	out.print("<th><b>BytesOut</b></th>");
	out.print("<th><b>Size</b></th>");
	out.print("</b></tr>");


	// Node data
	printRow(getNodeID(), "Node", out);

	out.print("<tbody>");
	out.print("<tr><b>");
	out.print("<th><b>AGENTS</b></th>");
	out.print("</b></tr>");

	//Agent Rows
	for (Iterator itr = localAgents.iterator(); itr.hasNext(); ) {

	    // Get Agent
	    MessageAddress addr = (MessageAddress) itr.next();
	    
	    // Print Agents Load
	    printRow(addr, "Agent", out);
	}
	
	
	//Service table

	out.print("<tbody>");
	out.print("<tr><b>");
	out.print("<th><b>SERVICES</b></th>");
	out.print("</b></tr>");

	printServiceRow("MTS", out);
	printServiceRow("Metrics", out);
	printServiceRow("NodeRoot", out);


	out.print("</table>");

    }


}
