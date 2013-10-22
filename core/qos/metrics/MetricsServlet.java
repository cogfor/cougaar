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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.servlet.ServletFrameset;

/**
 * Abstract parent class of many Metrics-related servlets.  It
 * provides some useful text formatters, access to services and lists
 * of local agents, and a standard bottom frame in a FrameSet.
 */
public abstract class MetricsServlet 
    extends ServletFrameset
    implements Constants
{

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   protected WhitePagesService wpService;
    protected MetricsService metricsService;
    protected final DecimalFormat f4_2 = new DecimalFormat("#0.00");
    protected final DecimalFormat f6_3 = new DecimalFormat("##0.000");
    protected final DecimalFormat f2_0 = new DecimalFormat("#0");
    protected final DecimalFormat f3_0 = new DecimalFormat("##0");
    protected final DecimalFormat f4_0 = new DecimalFormat("###0");
    protected final DecimalFormat f7_0 = new DecimalFormat("#######0");
    
    private AgentContainer agentContainer;

    public MetricsServlet(ServiceBroker sb) 
    {
	super(sb);

	wpService = sb.getService(this, WhitePagesService.class, null);

	NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
        if (ncs != null) {
            agentContainer = ncs.getRootContainer();
            sb.releaseService(this, NodeControlService.class, ncs);
        }

	metricsService = sb.getService(this, MetricsService.class, null);


    }

    /**
     * @return the message addresses of the agents on this
     * node, or null if that information is not available.
     */
    protected final Set getLocalAgents() 
    {
        if (agentContainer == null) {
            return null;
        } else {
            return agentContainer.getAgentAddresses();
        }
    }


    @Override
   public void printBottomPage(HttpServletRequest request,
				PrintWriter out)
    {
	out.print("<p><b>Color key</b>");
	ServletUtilities.colorTest(out);
    }

    @Override
   public int dataPercentage() 
    {
	return 70;
    }

    @Override
   public int bottomPercentage() 
    {
	return 20;
    }

}
