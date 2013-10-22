/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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

package org.cougaar.core.node.service.jvmdump;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Date;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.JvmStackDumpService;
import org.cougaar.core.servlet.BaseServletComponent;

/**
 * This component is a {@link Servlet} that registers with the path
 * "/jvmdump"* and allows the client to invoke the "dumpStack()"
 * method.
 * <p>
 * The servlet requires a "?action=Dump" URL parameter.
 * If not provided, the user is prompted in an HTML form.
 */
public class JvmStackDumpServlet extends BaseServletComponent {

  private String hostId;
  private MessageAddress nodeId;
  private MessageAddress agentId;

  private JvmStackDumpService jsds;

  @Override
protected String getPath() {
    return "/jvmdump";
  }

  @Override
public void load() {
    // get the jvm stack dump service
    jsds = getService(
        this, JvmStackDumpService.class, null);
    // get the host name
    // get the agent id
    AgentIdentificationService ais = getService(
       this, AgentIdentificationService.class, null);
    if (ais != null) {
      agentId = ais.getMessageAddress();
      releaseService(
          this, AgentIdentificationService.class, ais);
    }
    // get the node id
    NodeIdentificationService nis = getService(
       this, NodeIdentificationService.class, null);
    if (nis != null) {
      InetAddress hostAddr = nis.getInetAddress();
      hostId = hostAddr != null ? hostAddr.getHostName() : null;
      nodeId = nis.getMessageAddress();
      releaseService(
          this, NodeIdentificationService.class, nis);
    }
    super.load();
  }

  @Override
public void unload() {
    super.unload();
    if (jsds != null) {
      releaseService(
          this, JvmStackDumpService.class, jsds);
      jsds = null;
    }
  }

  @Override
protected Servlet createServlet() {
    return new JvmStackDumpServletImpl();
  }

  private class JvmStackDumpServletImpl extends HttpServlet {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Override
   public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {

      // check for the "?action=Dump" parameter
      if (!"Dump".equals(req.getParameter("action"))) {
        String osName = SystemProperties.getProperty("os.name");
        boolean isWindows = 
          (osName != null && osName.indexOf("Windows") >= 0);

        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "Request a JVM stack dump to be sent to"+
            " standard-out.  This is equivalent to a "+
            (isWindows ? 
             "\"CTRL-BREAK\" on Windows" :
             "\"CTRL-\\\" on Unix")+
            ".<p>\n"+
            "Confirm by pressing the \"Dump\" button below:\n");
        printForm(out, req);
        out.println("</body></html>");
        return;
      }

      // check for service existence
      if (jsds == null) {
        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "<b>ERROR:</b><p>\n"+
            "The JvmStackDumpService is not available in this"+
            " servlet's service broker.\n"+
            "</body></html>");
        return;
      }

      boolean didIt = jsds.dumpStack();

      // check the response
      if (!didIt) {
        String osName = SystemProperties.getProperty("os.name");
        boolean isWindows = 
          (osName != null && osName.indexOf("Windows") >= 0);

        res.setContentType("text/html");
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter out = res.getWriter();
        startHtml(out);
        out.println(
            "<b>ERROR:</b><p>\n"+
            "The JvmStackDumpService's \"dumpStack()\" method"+
            " returned false.<p>\n"+
            "This indicates a possible JNI library path problem"+
            " in this node's "+
            (isWindows ? "%PATH%" : "$LD_LIBRARY_PATH")+
            ".<p>\n"+
            "For further information, please see the core"+
            " javadocs for "+
            "\"org.cougaar.core.node.service.jvmdump\".\n"+
            "</body></html>");
        return;
      }

      // success!
      res.setContentType("text/html");
      PrintWriter out = res.getWriter();
      startHtml(out);
      out.println(
          "<b>SUCCESS:</b><p>\n"+
          "The JVM stack has been successfully dumped to"+
          " standard-out.<p>\n"+
          "Invoke another stack dump?<p>");
        printForm(out, req);
        out.println("</body></html>");
    }

    private void startHtml(PrintWriter out) {
      out.println(
          "<html><head><title>"+
          nodeId+" JVM stack dump</title></head>"+
          "<body>\n"+
          "<h2>JVM stack dump servlet</h2><p>\n"+
          "<table>"+
          "<tr><td rowspan=5>&nbsp;&nbsp;&nbsp;</td></tr>"+
          "<tr><td><i>Host:</i></td><td>"+hostId+"</td></tr>"+
          "<tr><td><i>Node:</i></td><td>"+nodeId+"</td></tr>"+
          "<tr><td><i>Agent:</i></td><td>"+agentId+"</td></tr>"+
          "<tr><td><i>Date:</i></td><td>"+(new Date())+"</td></tr>"+
          "</table><p>");
    }

    private void printForm(
        PrintWriter out, HttpServletRequest req) {
      out.println(
          "<form method=\"GET\" action=\""+
          req.getRequestURI()+
          "\">\n"+
          "<input type=\"submit\" name=\"action\""+
          " value=\"Dump\"><br>\n"+
          "</form>\n");
    }
  }

}
