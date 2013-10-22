/* 
 * <copyright>
 * 
 *  Copyright 2004 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentQuiescenceStateService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.ComponentServlet;

/**
 * This component is a {@link javax.servlet.Servlet} that displays
 * the agents registered with the node's {@link
 * AgentQuiescenceStateService}, and allow an agent to be marked as
 * "dead" (for use when the agent has been restarted elsewhere and
 * the original instance should be ignored).
 * <p>
 * Address it at /agentQuiescenceState
 * <p> 
 * Load it into every Node at BINDER or lower priority, using the
 * insertion point
 * <code>Node.AgentManager.Agent.PluginManager.Servlet</code>
 */
public class QuiescenceStateServlet extends ComponentServlet {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private LoggingService log = null;
  private AgentQuiescenceStateService aqs = null;
  private String node = null;

  @Override
protected String getPath() {
    return "/agentQuiescenceState";
  }

  @Override
public void load() {
    log = getService(this, LoggingService.class, null);
    aqs = getService(this, AgentQuiescenceStateService.class, null);
    super.load();
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setAgentQuiescenceStateService(AgentQuiescenceStateService aqs) {
    this.aqs = aqs;
  }

  public void setNodeIdentificationService(NodeIdentificationService nis) {
    if (nis != null)
      node = nis.getMessageAddress().toString();
  }

  @Override
public void unload() {
    super.unload();
    // release logger, aqs
    if (aqs != null) {
      releaseService(
          this, AgentQuiescenceStateService.class, aqs);
      aqs = null;
    }

    if (log != null) {
      releaseService(
          this, LoggingService.class, log);
      log = null;
    }
  }


  @Override
public void doGet(
      HttpServletRequest sreq,
      HttpServletResponse sres) throws IOException {
    // create a new handler per request, so we don't mangle our
    // per-request variables
    MyHandler h = new MyHandler(log, aqs, node);
    h.execute(sreq, sres);  
  }

  @Override
public void doPut(
      HttpServletRequest sreq,
      HttpServletResponse sres) throws IOException {
    // create a new handler per request, so we don't mangle our
    // per-request variables
    MyHandler h = new MyHandler(log, aqs, node);
    h.execute(sreq, sres);  
  }

  // The servlet should throw up a table of Agents, Dead, Enabled, Quiescent, MarkDead (a button)
  // It needs both an HTML and an XML format for easy ACME use

  private static class MyHandler {

    private final LoggingService log;
    private final AgentQuiescenceStateService aqs;
    private HttpServletRequest sreq;
    private PrintWriter out;

    private static final String HTML_FORMAT = "HTML";
    private static final String XML_FORMAT = "XML";
    private String format = HTML_FORMAT;
    private MessageAddress[] agents; // List of Nodes agents
    private String nodeName; // local node
    private MessageAddress agentToKill = null; // agent to mark as dead

    public MyHandler (LoggingService log, AgentQuiescenceStateService aqs, String node) {
      this.log = log;
      this.aqs = aqs;
      this.nodeName = node;
      agents = aqs.listAgentsRegistered();
      if (log!= null && log.isDebugEnabled()) {
	log.debug(nodeName + ".QStateServlet invoked. Got list of agents of length " + agents.length);
      }
    }

    public void execute(
        HttpServletRequest sreq, 
        HttpServletResponse sres) throws IOException
      //, ServletException 
    {
      this.sreq = sreq;
      parseParams();
      if (format == XML_FORMAT) {
	// Print XML header
	sres.setContentType("text/xml");
	this.out = sres.getWriter();
	out.println("<?xml version='1.0'?>");

	performXMLRequest();
      } else {
	// Doing HTML page
	this.out = sres.getWriter();
	printHeader();
	performRequest();
	printForm();
	printFooter();
      }
      this.out.flush();
    }

    // Get parameters from the URI
    private void parseParams() {
      String form = paramSingle("format");

      // Only use the specified format if it is XML.
      // Else use default (HTML)
      if (XML_FORMAT.equalsIgnoreCase(form))
	format = XML_FORMAT;

      String deadAgent = paramSingle("dead");
      if (deadAgent != null)
	agentToKill = MessageAddress.getMessageAddress(deadAgent);
      else
	agentToKill = null;
      if (log != null && log.isDebugEnabled()) {
	log.debug(nodeName + ".QStateServlet parsed params: format=" + format + ", deadAgent= " + deadAgent);
      }
    }

    // Header of the HTML page
    private void printHeader() {
      long now = System.currentTimeMillis();
      out.println(
          "<html>\n"+
          "<head>\n"+
	  "<title>Quiescence State</title>\n"+
	  "</head>\n"+
	  "<body>"+
	  "<h2>Agents registered with " + nodeName + "'s Quiescence Service</h2>"+
	  "<p>\n"+
	  "Node " + nodeName + " is now <b>" + (aqs.isNodeQuiescent() ? "Quiescent" : "NOT Quiescent") + "</b><p>\n" +
	  "Time:  "+now+" ("+(new Date(now))+")<p>\n");
    }

    // HTML form for query generation, status display
    private void printForm() {
      // FIXME: Support multiple agent names via checkboxes or some such
      out.println(
        "<table border=1>\n"+
        "<form name=\"f\" method=\"GET\" action=\""+
        sreq.getRequestURI()+
        "\">\n"+
	"<tr><th>Agent</th><th>Quiescent?</th><th>Blockers</th><th>Enabled?</th><th>Dead?</th><th>Mark as Dead</th></tr>\n");
      // loop over Message addresses
      for (int i = 0; i < agents.length; i++) {
	printRowHTML(agents[i]);
      }
      out.println("</form>\n</table>\n");
    }

    // Show status / button for one agent
    private void printRowHTML(MessageAddress agent) {
      out.println(
		  "<tr><td>" + agent + "</td>"+
                  getColumn(aqs.isAgentQuiescent(agent), true)+
		  "<td>" + aqs.getAgentQuiescenceBlockers(agent) + "</td>" + 
                  getColumn(aqs.isAgentEnabled(agent), true)+
                  getColumn(!aqs.isAgentAlive(agent), false)+
		  "<td>" + (aqs.isAgentAlive(agent) ? getButton(agent) : "Already Dead") + "</td></tr>\n");
    }

    // Show status / button for one agent
    private void printRowXML(MessageAddress agent) {
      out.println(
		  "  <agent name=\'" + agent + "\' quiescent=\'" + aqs.isAgentQuiescent(agent) + "\' "+
		  "enabled=\'" + aqs.isAgentEnabled(agent) + "\' "+
		  "dead=\'" + ! aqs.isAgentAlive(agent) + "\'" + (aqs.isAgentQuiescent(agent) ? "" : "blockers=\'" + aqs.getAgentQuiescenceBlockers(agent) + "\'") + "/>");
    }

    // Color-coded boolean column
    private String getColumn(boolean value, boolean greenValue) {
      return "<td bgcolor=\"" + (value == greenValue ? "80FF80" : "FF8080") +
          "\">" + value + "</td>";
    }

    // Print a button to mark an agent as dead
    private String getButton(MessageAddress agent) {
      return "<input type=\"submit\" name=\"dead\" value=\"" + agent.toString() + "\">";
    }

    // Print bottom of the HTML page
    private void printFooter() {
      out.println("</body></html>");
    }

    // Do the actual action
    private void performRequest() {
      if (agentToKill == null)
	return;
      if (aqs == null)
	return;
      if (log != null && log.isDebugEnabled()) {
	log.debug(nodeName + ".QStateServlet killing agent " + agentToKill);
      }
      aqs.setAgentDead(agentToKill);
    }

    private void performXMLRequest() {
      performRequest();
      out.println("<node name=\"" + nodeName + "\" quiescent=\'" + aqs.isNodeQuiescent() + "\'>");
      // loop over Message addresses
      for (int i = 0; i < agents.length; i++) {
	printRowXML(agents[i]);
      }
      out.println("</node>\n");      
    }

    // Parse a single-valued parameter (decoding it if necc.)
    private String paramSingle(String n) {
      String s = sreq.getParameter(n);
      if (s==null || s.trim().length()==0) {
        s = null;
      } else {
	try {
	  s = URLDecoder.decode(s, "UTF-8");
	} catch (Exception e) {
          // should never happen, utf-8 is standard.
          s = null;
	}
      }

      return s;
    }
  } // end of MyHandler
}
