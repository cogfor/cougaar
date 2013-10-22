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
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.wp.AddressEntry;

/**
 * This Servlet displays a summary of all interactions between any
 * Agent on this Node and the Agents they talk to (remote or loca).
 */
public class RemoteAgentServlet
    extends MetricsServlet
{

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static final String TOPOLOGY = "topology";
    private static String localHost;

    static {
	try {
	    InetAddress localAddr = InetAddress.getLocalHost();
	    localHost = localAddr.getHostAddress();
	} catch (java.net.UnknownHostException uhe) {
	    localHost = "127.0.0.1";
	}
    }

    private AgentStatusService agentStatusService; 

    public RemoteAgentServlet(ServiceBroker sb) {
	super(sb);

	agentStatusService = sb.getService(this, AgentStatusService.class, null);
    }



    @Override
   public String getPath() {
	return "/metrics/remote/agents";
    }

    @Override
   public String getTitle () {
	return "Remote Agent Status for Node " + getNodeID();
    }

    private String canonicalizeAddress(String hostname) {
	try {
	    InetAddress addr = InetAddress.getByName(hostname);
	    return addr.getHostAddress();
	} catch (java.net.UnknownHostException uhe) {
	    return hostname;
	}
    }

    @Override
   public void printPage(HttpServletRequest request, PrintWriter out) {
	if (agentStatusService == null) return;
	// Get list of All Agents in society
	Set matches = agentStatusService.getRemoteAgents();
	if (matches == null) return;

	// Space between the intro and the content
	out.print("<br><br>");

	//Header Row
	out.print("<table border=3 cellpadding=2 rules=groups>\n");
	out.print("<colgroup span=1><colgroup span=3><colgroup span=1><colgroup span=2><colgroup span=1><colgroup span=2>");
	out.print("<tr>");
	out.print("<th>AGENTS</th>");
	out.print("<th>SpokeTo</th>");
	out.print("<th>HeardFrom</th>");
	out.print("<th>SpokeErr</th>");
	out.print("<th>Queue</th>");
	out.print("<th>MsgTo</th>");
	out.print("<th>MsgFrom</th>");
	out.print("<th>eMJIPS</th>");
	out.print("<th>mKbps</th>");
	out.print("<th>eKbps</th>");
	out.print("</tr>");

	//Rows
	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    MessageAddress agent = (MessageAddress) itr.next();
	    String name = agent.getAddress();

	    AgentStatusService.AgentState state = 
		agentStatusService.getRemoteAgentState(agent);
	    String agentHost = null;
	    try {
		// -1 means don't block
		AddressEntry entry = wpService.get(name, TOPOLOGY, -1);
		if (entry == null) {
		    agentHost = localHost;
		} else {
		    agentHost = entry.getURI().getHost();
		}
		agentHost = canonicalizeAddress(agentHost);
	    } catch (Exception ex1) {
	    }
	    
	    String agentPath = "Agent(" +name+ ")"+PATH_SEPR;
	    String destPath="Node("+getNodeID()+")"+PATH_SEPR+
		"Destination("+name+")"+PATH_SEPR;
	    String ipFlowPath="IpFlow(" +localHost+ "," +agentHost+ ")"
		+PATH_SEPR;
	    // Get Metrics
	    Metric eMJIPS = metricsService.getValue(agentPath+
						    "EffectiveMJips");

	    int qLength = 0;
	    double qCredibility = NO_CREDIBILITY;
	    String qProvenance = "none";
	    if (state != null) {
		qLength = state.queueLength;
		qCredibility = SECOND_MEAS_CREDIBILITY;
		qProvenance = "AgentStatusService";
	    }

	    Metric queue = new MetricImpl(new Integer(qLength), 
					  qCredibility,
					  "none",
					  qProvenance);

	    Metric msgTo = metricsService.getValue(destPath
						   +MSG_TO+
						   "(" +_10_SEC_AVG+ ")");
	    Metric msgFrom = metricsService.getValue(destPath
						     +MSG_FROM+
						     "(" +_10_SEC_AVG+ ")");
// 	    Metric bytesTo= metricsService.getValue(destPath
// 						    +BYTES_TO+
// 						    "(" +_10_SEC_AVG+ ")");
// 	    Metric bytesFrom = metricsService.getValue(destPath
// 						       +BYTES_FROM+
// 						       "(" +_10_SEC_AVG+ ")");
	    Metric eMbps = metricsService.getValue(ipFlowPath+
						   "CapacityUnused");
	    Metric mMbps = metricsService.getValue(ipFlowPath+
						   "CapacityMax");

	    Metric heard = metricsService.getValue(agentPath+
						   "LastHeard");
	    if (heard ==null) {
		heard = new MetricImpl(new Double(0.00), 0,"units","test");
	    }

	    Metric spoke = metricsService.getValue(agentPath+
						   "LastSpoke");
	    if (spoke ==null) {
		spoke = new MetricImpl(new Double(0.00), 0,"units","test");
	    } 

	    Metric error = metricsService.getValue(agentPath+
						   "LastSpokeError");
	    if (error ==null) {
		error = new MetricImpl(new Double(0.00), 0,"units","test");
	    } 

	    if (msgTo ==null) 
		msgTo = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (msgFrom ==null) 
		msgFrom = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (eMbps ==null) 
		eMbps = new MetricImpl(new Double(0.00), 0,"units","test");

	    if (mMbps ==null) 
		mMbps = new MetricImpl(new Double(0.00), 0,"units","test");



	    //output Row
	    out.print("<tr><td>");
	    out.print(name);
	    out.print("</td>");
	    ServletUtilities.valueTable(spoke, 0.0, 30.0, true, f3_0, out);
	    ServletUtilities.valueTable(heard, 0.0, 30.0, true, f3_0, out);
	    ServletUtilities.valueTable(error, 0.0, 60.0, false, f3_0, out);
	    ServletUtilities.valueTable(queue, 0.0, 1.0, true,  f4_2, out);
	    ServletUtilities.valueTable(msgTo, 0.0, 1.0, true, f4_2, out);
	    ServletUtilities.valueTable(msgFrom, 0.0, 1.0, true, f4_2, out);
	    ServletUtilities.valueTable(eMJIPS, 10.0, 400.0, false, f3_0, out);
	    ServletUtilities.valueTable(mMbps, 0.0, 0.10, false, f6_3, out);
	    ServletUtilities.valueTable(eMbps, 0.0, 0.10, false, f6_3, out);
	    out.print("</tr>\n");
	}
	out.print("</table>");
    }
}
