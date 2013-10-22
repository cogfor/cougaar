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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ServletService;

public class MetricsRSSFeedServlet
    extends HttpServlet
    implements Constants
{
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static final DateFormat DateFormatter = 
	DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);

    private MetricsService metricsService;
    private MessageAddress nodeID;
    private AgentContainer agentContainer;

    public MetricsRSSFeedServlet(ServiceBroker sb) {
	// Register our servlet with servlet service
	ServletService servletService = sb.getService(this, ServletService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain ServletService");
	}
	try {
	    servletService.register(getPath(), this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +getPath()+ ">: " +e.getMessage());
	}
   
	// get metrics service
	try {
	    metricsService = sb.getService(this, MetricsService.class, null);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to get MetricsService at path <"
				       +getPath()+ ">: " +e.getMessage());
	}


	NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
	nodeID = nis.getMessageAddress();
	sb.releaseService(this, NodeIdentificationService.class, nis);

	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
        if (ncs != null) {
            agentContainer = ncs.getRootContainer();
            sb.releaseService(this, NodeControlService.class, ncs);
        }

    }
  
    public String getPath() {
	return "/metrics/society/rss";
    }

    public String getTitle () {
	return "Really Simple Syndication feed";
    }
  
  
    void printItem(String path, String description, PrintWriter out) {
	out.print("<item>\n");
	out.print("<title>");
	out.print(path);
	out.print("</title>\n");
	out.print("<description>");
	out.print(description);
	out.print("</description>\n");
	out.print("</item>\n");
    }

    void printItem(String path, Metric metric, PrintWriter out) {
	out.print("<item>\n");
	out.print("<title>");
	out.print(path);
	out.print("</title>\n");


	
	if (metric == null) {
	    out.print("<description>");
	    out.print("null" );
	    out.print("</description>\n");
	} else {
	    Object data = metric.getRawValue();
	    out.print("<description>");
	    out.print(" Type=");
	    if (data instanceof Float || data instanceof Double)  
		out.print("Double");
	    else if (data instanceof Number)
		out.print("Long");
	    else if (data instanceof Boolean)  
		out.print("Boolean");
	    else if (data instanceof String)  
		out.print("String");
	    else if (data instanceof Character)
		out.print("Character");

	    out.print(" Value=");
	    out.print(data.toString());

	    out.print(" Credibility=");
	    out.print(metric.getCredibility());

	    if (metric.getUnits() != null) {
		out.print(" Units=");
		out.print(metric.getUnits());
	    }

	    out.print(" Halflife=");
	    out.print(metric.getHalflife());
	    out.print("</description>\n");

	    if (metric.getProvenance() != null) {
		out.print("<source>");
		out.print(metric.getProvenance());
		out.print("</source>\n");
	    }


	    // pubDate
	    out.print("<pubDate>");
	    Date timestamp = new Date(metric.getTimestamp());
	    // out.print(timestamp.toGMTString());
	    out.print(DateFormatter.format(timestamp));
	    out.print("</pubDate>\n");

	}

	out.print("</item>\n");
    }

    public void printChannel(String url, List paths, PrintWriter out) 
    {
	// build string of xml metrics
	out.print("<?xml version=\"1.0\"?>\n");
	out.print("<rss version=\"2.0\">\n<channel>\n");
	out.print("<description>");
	out.print("Agent load");
	out.print("</description>");
	out.print("<title>");
	out.print("Load data for Node " +nodeID);
	out.print("</title>");
	out.print("<link>");
	out.print(url);
	out.print("</link>");
	out.print("<ttl>10</ttl>\n");
	if (paths != null) {
	    int count = paths.size();
	    for (int i=0; i<count; i++) {
		String path = (String) paths.get(i);
		try {
		    Metric pathMetric = metricsService.getValue(path);
		    printItem(path, pathMetric, out);
		} catch(Exception e) {
		    printItem(path, e.toString(), out);
		}
	    }
	}

	out.print("</channel>\n</rss>\n");
    }
  
    void addPaths(String type, MessageAddress agentID, List paths)
    {
	String path = type + "(" +agentID+ ")" + PATH_SEPR;
	paths.add(path +CPU_LOAD_AVG+ "(" +_10_SEC_AVG+ ")");
	paths.add(path +CPU_LOAD_MJIPS+ "(" +_10_SEC_AVG+ ")");
	paths.add(path
		  +MSG_IN+ 
		  "("+_10_SEC_AVG+")");
	paths.add(path
		  +MSG_OUT+
		  "("+_10_SEC_AVG+")");
	paths.add(path
		  +BYTES_IN+
		  "(" +_10_SEC_AVG+")");
	paths.add(path
		  +BYTES_OUT+
		  "(" +_10_SEC_AVG+")");
	paths.add(path + PERSIST_SIZE_LAST );
    }

    public void printPage(HttpServletRequest request, PrintWriter out) {
    
	StringBuffer url_buf = request.getRequestURL();
	String url = url_buf.toString();

	ArrayList paths = new ArrayList();

	Set agents = agentContainer.getAgentAddresses();
	Iterator itr = agents.iterator();
	while (itr.hasNext()) {
	    MessageAddress agentID = (MessageAddress) itr.next();
	    addPaths("Agent", agentID, paths);
	}

	try {
	    // parses params
	    printChannel(url, paths, out);
	} catch(Exception e) {
	    // also log here
	    System.out.println("Exception: " + e);
	    e.printStackTrace();
	}
    }


    // servlet requirement - pass to our print method to handle
    @Override
   public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {
	PrintWriter out = response.getWriter();
	printPage(request, out);
    }
}
