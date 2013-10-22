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

package  org.cougaar.core.qos.metrics;

import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ServletService;


/**
 * This Servlet provides url access to the MetricsUpdateService.  The
 * required URI parameters are <code>key</code> and <code>value</code>.
 */
public class MetricsWriterServlet
    extends HttpServlet
    implements Constants
{
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private MetricsUpdateService metricsUpdateService;
  
    public MetricsWriterServlet(ServiceBroker sb) {
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
	    metricsUpdateService = sb.getService(this, MetricsUpdateService.class, null);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to get MetricsUpdateService: "
				       +e.getMessage());
	}
    }
  
    public String getPath() 
    {
	return "/metrics/writer";
    }
    
    public String getTitle () 
    {
	return "Remote Metrics Writer for Node";
    }
    
    /*
     * Parses params, and send out either a propertylist of
     * metrics(java version), or a string of xml-formatted text,
     * through a serialized byte array.
     */
    public void printPage(HttpServletRequest request, PrintWriter out) 
    {
	try {		  
	    
	    // parses params
	    String key = request.getParameter("key");
	    String value = request.getParameter("value");
	    
	    if (key==null || value==null) {
		out.print("Key or Value is null");
		return;
	    }
	    
	    Metric metric = new MetricImpl(Double.parseDouble(value),
					   USER_DEFAULT_CREDIBILITY,
					   null,
					   request.getRemoteHost());
	    metricsUpdateService.updateValue(key, metric);
	    out.print("Key: " + key + ", Value: " + value+ 
		      "From: " + request.getRemoteHost() +"\n");
	} catch(Exception e) {
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
