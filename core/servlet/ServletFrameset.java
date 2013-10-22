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

package org.cougaar.core.servlet;

import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ServletService;

/**
 * Base class for serlvets that generate a per-agent data page.
 * <p>
 * The page is split into three frames, with the agent selection
 * at the top, data in the middle, and a refresh button at the
 * bottom.  The user can select a different agent and press the
 * refresh button to refresh the data page.  The bottom frame
 * also allows the user to set a timer-based data refresh, where
 * zero stops the timer.
 *
 * @see org.cougaar.core.servlet.ServletMultiFrameset adds a
 *   second drop-down list for additional data filtering
 */
public abstract class ServletFrameset
extends HttpServlet
{

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

// url paramter
  protected static final String FRAME="frame";

  // values for the FRAME url parameter
  protected static final String DATA_FRAME="dataFrame";
  protected static final String BOTTOM_FRAME="bottomFrame";
  protected static final String AGENT_FRAME="agentFrame";

  protected static final String BOTTOM_FORM="bottomForm";

  // url parameter for periodic data refresh
  protected static final String REFRESH_FIELD_PARAM = "refresh";

  private MessageAddress nodeAddr;

  public ServletFrameset(ServiceBroker sb) {
    // which node are we in?
    NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
    nodeAddr = nis.getMessageAddress();
    sb.releaseService(this, NodeIdentificationService.class, nis);

    // Register our servlet.
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
  }

  /** @return the local node's address */
  protected final MessageAddress getNodeID() {
    return nodeAddr;
  }

  /** Get the page title */
  public abstract String getTitle();

  /** Get the servlet path */
  public abstract String getPath();

  /** Print the data contents. */
  public abstract void printPage(
      HttpServletRequest request, PrintWriter out);

  /** Print additional text for the bottom frame */
  public void printBottomPage(
      HttpServletRequest request, PrintWriter out) {
  }

  //
  // The rest is fine for most subclasses
  //

  protected int topPercentage() {
    return 10;
  }

  protected int dataPercentage() {
    return 80;
  }

  protected int bottomPercentage() {
    return 10;
  }

  protected String getMiddleFrame() {
    return DATA_FRAME;
  }

  private void printRefreshForm(
      HttpServletRequest request, PrintWriter out) {

    // get the refresh value and let the user select a new value
    int refreshSeconds = 0;
    String refresh  = request.getParameter(REFRESH_FIELD_PARAM);
    if (refresh != null) {
      try {
        refreshSeconds = Integer.parseInt(refresh);
      } catch (Exception e) {
	// If no good time specified, use default of 0
      }
    }

    out.print(
        "<table><tr>"+
        "<td valign=\"middle\">Refresh (in seconds):</td>"+
        "<td valign=\"middle\"><input type=\"text\" size=3 name=\""+
        REFRESH_FIELD_PARAM+
        "\""+
        " value=\"");
    out.print(refreshSeconds);
    out.print(
        "\""+
        "></td>"+
        "<td valign=\"middle\">"+
        "<input type=\"submit\" name=\"action\""+
        " value=\"Refresh\"></td>"+
        "</tr></table>");
  }

  protected void writeJavascript(PrintWriter out) {
    out.print(
        "<script language=\"JavaScript\">\n"+
        "<!--\n"+
        "function mySubmit() {\n"+
        "  // make sure an agent was selected\n"+
        "  var topObj = top."+
        AGENT_FRAME+
        ".document.agent.name;\n"+
        "  var encAgent = topObj.value;\n"+
        "  if (encAgent.charAt(0) == '.') {\n"+
        "    alert(\"Please select an agent name\")\n"+
        "    return false;\n"+
        "  }\n"+
        "  document."+BOTTOM_FORM+".target=\""+DATA_FRAME+"\"\n"+
        "  document."+BOTTOM_FORM+".action=\"/$\"+encAgent+\""+
        getPath()+"\"\n"+
        "  return true\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n");
  }

  // not abstract, but a no-op by default
  protected void printAdditionalBottomFields(
      PrintWriter out) {
  }

  private void printBottomFrame(
      HttpServletRequest request, PrintWriter out) {
    out.print("<html><HEAD>");

    // write javascript
    writeJavascript(out);

    // begin form
    out.print(
        "<form name=\""+BOTTOM_FORM+"\" method=\"get\" "+
        "onSubmit=\"return mySubmit()\">\n"+
        "<input type=hidden name=\""+FRAME+
        "\" value=\""+DATA_FRAME+"\">\n");

    printAdditionalBottomFields(out);

    printRefreshForm(request, out);

    // end form
    out.print("</form>");

    printBottomPage(request, out);

    out.print("</body></html>\n");
  }

  private void printDataFrame(
      HttpServletRequest request, PrintWriter out) {
    
    out.print("<html><head>");

    // get the refresh value and make this page refresh
    String refresh  = request.getParameter(REFRESH_FIELD_PARAM);
    if (refresh != null) {
      int refreshSeconds = 0;
      try {
        refreshSeconds = Integer.parseInt(refresh);
      } catch (Exception e) {
	// If not good time specified, use the default of 0
      }
      if (refreshSeconds > 0 ) {
        out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
        out.print(refreshSeconds);
        out.print("\">");
      }
    }

    out.print("<title>");
    out.print(getTitle());
    out.print("</title></head><body><h1>");
    out.print(getTitle());
    out.print("</h1>");

    out.print("Node: "+nodeAddr+"<br>");
    out.print("Date: ");
    out.print(new java.util.Date());

    printPage(request, out);

    out.print("</body></html>\n");
  }

  private void printOuterFrame(
      HttpServletRequest request, PrintWriter out) {
    // Header
    out.print("<html><head><title>");
    out.print(getTitle());
    out.print("</title></head>");

    // Frameset
    out.print("<frameset rows=\"");
    out.print(topPercentage());
    out.print("%,");
    out.print(dataPercentage());
    out.print("%,");
    out.print(bottomPercentage());
    out.print("%\">\n");

    // Top
    out.print("<frame src=\"/agents?format=select&suffix=");
    out.print(nodeAddr);
    out.print("\" name=\"agentFrame\">\n");

    String middleFrame = getMiddleFrame();

    // Middle
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(middleFrame);
    out.print("\" name=\"");
    out.print(middleFrame);
    out.print("\">\n");

    // Bottom
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(BOTTOM_FRAME);
    out.print("\" name=\"");
    out.print(BOTTOM_FRAME);
    out.print("\">\n");

    // End frameset
    out.print("</frameset>\n");

    // Frameless browser hack
    out.print("<noframes>Please enable frame support</noframes>");

    // End
    out.print("</html>\n");
  }

  protected void printFrame(
      String frame,
      HttpServletRequest request,
      PrintWriter out) {
    if (DATA_FRAME.equals(frame)) {
      printDataFrame(request, out);
    } else if (BOTTOM_FRAME.equals(frame)) {
      printBottomFrame(request, out);
    } else {
      printOuterFrame(request, out);
    }
  }

  @Override
public final void doGet(
      HttpServletRequest request, HttpServletResponse response) 
    throws java.io.IOException {
      response.setContentType("text/html");
      try {
        PrintWriter out = response.getWriter();
        String frame = request.getParameter(FRAME);
        printFrame(frame, request, out);
        out.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

}
