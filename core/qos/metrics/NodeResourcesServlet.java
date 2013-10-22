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
import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;

/**
 * This Servlet displays the Host resource metrics, using the Node to
 * get at it.
 */
public class NodeResourcesServlet 
    extends MetricsServlet
{
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public NodeResourcesServlet(ServiceBroker sb) {
	super(sb);
    }

    @Override
   public String getPath() {
	return "/metrics/resources";
    }

    @Override
   public String getTitle () {
	return "Resources for Node " + getNodeID();
    }


    private void outputMetric(PrintWriter out,
			      String path,
			      String name,
			      double ignore,
			      double highlight,
			      boolean greater,
			      DecimalFormat formatter) 
    {
	Metric metric = metricsService.getValue(path+name);
	if (metric == null)
	    metric= new MetricImpl(new Double(0.00), 0,"units","test");
	out.print("<tr><td>");
	out.print(name);
	out.print("</td>");
	ServletUtilities.valueTable(metric, ignore, 
			 highlight,greater,formatter, out);
	out.print("</tr>\n");	
    }

    @Override
   public void printPage(HttpServletRequest request, PrintWriter out) {
	String nodePath = "Agent(" +getNodeID()+ ")"+PATH_SEPR;
	

	// Space between the intro and the content
	out.print("<br><br>");

	//Header Row
	out.print("<table border=3 rules=groups>\n");
	out.print("<colgroup><colgroup>");
	out.print("<tr>");
	out.print("<th>RESOURCE</th>");
	out.print("<th>Value</th>");
	out.print("</tr>");

	//Rows
	outputMetric(out,nodePath,"EffectiveMJips",
		     10.0, 400.0, false, f2_0);
	outputMetric(out,nodePath,"LoadAverage",
		     0.0, 4.0, true, f4_2);
	outputMetric(out,nodePath,"Count",
		     1.0, 5.0, true, f2_0);
	outputMetric(out,nodePath,"Jips",
		     1.0, 400000000.0, false, f7_0);
	outputMetric(out,nodePath,"BogoMips",
		     1.0, 800.0, false, f7_0);
	outputMetric(out,nodePath,"Cache",
		     1.0, 256, false, f7_0);
	outputMetric(out,nodePath,"TcpInUse",
		     0.0, 50.0, true, f3_0);
	outputMetric(out,nodePath,"UdpInUse",
		     0.0, 50.0, true, f3_0);
	outputMetric(out,nodePath,"TotalMemory",
		     0.0, 256000.0, false, f7_0);
	outputMetric(out,nodePath,"FreeMemory",
		     0.0, 32000, false, f7_0);
	outputMetric(out,nodePath,"MeanTimeBetweenFailure",
		     1.0, 1000.0, false, f7_0);

	out.print("</table>");
    }
}
