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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;

/**
 * Base class for serlvets that have multiple frames with an
 * agent selection list.
 */
public abstract class ServletMultiFrameset
extends ServletFrameset
{

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// values for the FRAME url parameter
  private static final String INFO_FRAME="infoFrame";
  private static final String SELECT_FRAME="selectFrame";
  private static final String RELOAD_FRAME="reloadFrame";

  private static final String SELECT_FORM="selectForm";

  // url parameter for the selected data
  private static final String INFO_BASE="base";
  private static final String SELECT_NAME="name";

  public ServletMultiFrameset(ServiceBroker sb) {
    super(sb);
  }

  /**
   * Get the contents for the second drop-down list.
   * <p>
   * The only other information of interest is the local agent's
   * name, which is available from the <code>getNodeID()</code>
   * method.
   */
  protected abstract List getDataSelectionList(
      HttpServletRequest request);

  /**
   * Print the data for the dataFrame.
   * <p>
   * @param selectName the selected name from the secondary drop-down
   */
  protected abstract void printPage(
      String selectName,
      HttpServletRequest request,
      PrintWriter out);

  //
  // The rest is fine for most subclasses
  //

  @Override
public final void printPage(
      HttpServletRequest request, PrintWriter out) {
    String selectName = request.getParameter(SELECT_NAME);
    printPage(selectName, request, out);
  }

  @Override
protected final String getMiddleFrame() {
    return INFO_FRAME;
  }

  @Override
protected final void writeJavascript(PrintWriter out) {
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
        "  // see if the top name matches the middle frames\n"+
        "  var baseObj = document."+BOTTOM_FORM+"."+INFO_BASE+";\n"+
        "  var baseAgent = baseObj.value;\n"+
        "  if (baseAgent != null &&\n"+
        "      baseAgent != encAgent) {\n"+
        "    // reset the info frame to match the top agent selection\n"+
      "    document."+BOTTOM_FORM+"."+INFO_BASE+".value=encAgent;\n"+
      "    document."+BOTTOM_FORM+"."+FRAME+".value=\""+INFO_FRAME+"\";\n"+
      "    document."+BOTTOM_FORM+".target=\""+INFO_FRAME+"\";\n"+
      "    document."+BOTTOM_FORM+".action=\"/$\"+encAgent+\""+
      getPath()+"\";\n"+
      "    return true;\n"+
      "  }\n"+
      "  document."+BOTTOM_FORM+"."+FRAME+".value=\""+RELOAD_FRAME+"\";\n"+
      "  document."+BOTTOM_FORM+".target=\""+DATA_FRAME+"\"\n"+
      "  document."+BOTTOM_FORM+".action=\"/$\"+encAgent+\""+
      getPath()+"\"\n"+
      "  return true\n"+
      "}\n"+
      "// -->\n"+
      "</script>\n");
  }

  /**
   * We save the info frame's view to avoid reloading both the
   * selection list and data frame if the user hasn't changed
   * the agent selection.
   */
  @Override
protected final void printAdditionalBottomFields(
      PrintWriter out) {
    out.print(
        "<input type=hidden name=\""+INFO_BASE+"\" value=\""+
        getNodeID()+
        "\">\n");
  }

  private void printSelectFrame(
      HttpServletRequest request, PrintWriter out) {

    out.print(
        "<html><body>\n"+
        "<body>\n"+
        "<form name=\""+SELECT_FORM+"\" onSubmit=\";\">\n"+
        getNodeID()+": "+
        "<select name=\""+SELECT_NAME+"\">\n");

    // get the selection list
    List l = getDataSelectionList(request);

    int n = (l == null ? 0 : l.size());
    if (n > 0) {
      Iterator iter = l.iterator();
      for (int i = 0; i < n; i++) {
        String si = (String) iter.next();
        out.print(
            "  <option>"+si+"</option>\n");
      }
    }

    out.print(
        "</select> \n"+
        "</form></body></html>\n");
  }

  /**
   * Generate a temporary page that redirects to the data page
   * with the selected name.
   * <p>
   * This page exists to work around some issues in javascript
   * security and page generation ordering.
   * <p>
   * Say that the top frame was generated on host A.  We select an
   * agent at the top, which loads data based upon an agent on host
   * B.  The secondary selection is generated on host B.  The data
   * page depends upon the selected agent name.  The bottom frame
   * can't see the selection, since it's on a different host and
   * javascript security denies this access, so the data frame itself
   * must grab the selected name.  Furthermore, the data frame
   * contents must be generated before this selection lookup, since
   * the server must generate the data contents and not the client's
   * browser.  For this reason we need an intermediate page to grab
   * the selected name and force the data page generation.
   * <p>
   * This "reload" frame is a temporary page that's briefly displayed
   * before the data frame's contents.  It grabs the selection and
   * reloads its frame with the data page.
   * <p>
   * The reload page is not used when periodically refreshing the
   * data page, so the cost is minimal.
   * <p>
   * This basic technique should be used if additional agent-local
   * selection options are added in the future.
   */
  private void printReloadFrame(
      HttpServletRequest request, PrintWriter out) {
    String refresh = request.getParameter(REFRESH_FIELD_PARAM);
    if (refresh == null || refresh.length() == 0) {
      refresh = "0";
    }
    out.print(
        "<html><body>"+
        "<script language=\"JavaScript\">\n"+
        "<!--\n"+
        "var selectObj = top."+
        INFO_FRAME+
        "."+
        SELECT_FRAME+
        ".document."+
        SELECT_FORM+
        ".name;\n"+
        "var selectName = selectObj.value;\n"+
        "if (selectName == null || selectName == '') {\n"+
        "  document.writeln(\n"+
        "      \"Select a name above and click the"+
        "Refresh button below.\");\n"+
        "} else {\n"+
        "  var newLoc = \""+
        request.getRequestURI()+
        "?"+FRAME+"="+DATA_FRAME+"&"+
        REFRESH_FIELD_PARAM+"="+refresh+"&"+
        SELECT_NAME+"=\"+selectName;\n"+
        "  location.href=newLoc;\n"+
        "}\n"+
        "-->\n"+
        "</script>\n"+
        "</body></html>\n");
  }

  /**
   * The info frameset contains the selection list and the data
   * frame.
   * <p>
   * This separate frameset allows the bottom frame to reload both
   * the selection frame and data frame when the user selects a
   * different agent.
   */
  private void printInfoFrame(
      HttpServletRequest request, PrintWriter out) {
    // Header
    out.print("<html><head><title>");
    out.print(getTitle());
    out.print("</title></head>");

    // Frameset
    out.print("<frameset rows=\"");
    out.print(topPercentage());
    out.print("%,");
    out.print(100 - topPercentage());
    out.print("%\">\n");

    // show select frame
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(SELECT_FRAME);
    out.print("\" name=\"");
    out.print(SELECT_FRAME);
    out.print("\">\n");

    // show data frame
    out.print("<frame src=\"");
    out.print(request.getRequestURI());
    out.print("?");
    out.print(FRAME);
    out.print("=");
    out.print(RELOAD_FRAME);
    out.print("\" name=\"");
    out.print(DATA_FRAME);
    out.print("\">\n");

    // End frameset
    out.print("</frameset>\n");

    // Frameless browser hack
    out.print("<noframes>Please enable frame support</noframes>");

    // End
    out.print("</html>\n");
  }

  @Override
protected final void printFrame(
      String frame,
      HttpServletRequest request,
      PrintWriter out) {
    if (SELECT_FRAME.equals(frame)){
      printSelectFrame(request, out);
    } else if (INFO_FRAME.equals(frame)){
      printInfoFrame(request, out);
    } else if (RELOAD_FRAME.equals(frame)){
      printReloadFrame(request, out);
    } else {
      super.printFrame(frame, request, out);
    }
  }
}
