/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.freeze;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.ListAllNodes;

/**
 * This component initiates a society-wide freeze for assessment.
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public class FreezeSocietyPlugin extends FreezeSourcePlugin {
  private ServletService servletService = null;
  private WhitePagesService wps = null;
  protected String status = "Running";
  private DemoControlService demoControlService;
  private double savedRate = 1.0;
  private double newRate = Double.NaN;

  public void setDemoControlService(DemoControlService dcs) {
    demoControlService = dcs;
  }

  @Override
public void load() {
    super.load();

    wps = getServiceBroker().getService(
       this, WhitePagesService.class, null);
  }

  @Override
public void setupSubscriptions() {
    super.setupSubscriptions();
    try {
      servletService = getServiceBroker().getService(this, ServletService.class, null);
      servletService.register("/freezeControl", new FreezeControlServlet());
    } catch (Exception e) {
      logger.error("Failed to register freezeControl servlet", e);
    }
  }

  @Override
protected synchronized void setUnfrozenAgents(Set unfrozenAgents) {
    if (logger.isDebugEnabled()) logger.debug("unfrozen" + unfrozenAgents);
    if (unfrozenAgents.isEmpty()) {
      status = "Frozen";
      savedRate = demoControlService.getExecutionRate();
      setRate(0.0);
    } else {
      status = "Freezing " + unfrozenAgents;
    }
  }

  private synchronized void setRate(double newRate) {
    this.newRate = newRate;
    demoControlService.setSocietyTimeRate(newRate);
  }

  @Override
protected synchronized void thaw() {
    super.thaw();
    setRate(savedRate);
  }

  @Override
protected Set getTargetNames() {
    Set names;
    try {
      names = ListAllNodes.listAllNodes(wps); // not scalable!
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to list all node names", e);
      }
      names = Collections.EMPTY_SET;
    }
    return names;
  }

  private static final SimpleDateFormat timeFormat =
    new SimpleDateFormat("yyyy/MM/dd HHmm zzz");

  private static String formatTime(long time) {
    return timeFormat.format(new Date(time));
  }

  private class FreezeControlServlet extends HttpServlet {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, false);
    }
    @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
      doPostOrGet(request, response, true);
    }
    protected void doPostOrGet(HttpServletRequest request, HttpServletResponse response, boolean doUpdate)
      throws IOException
    {
      String submit = request.getParameter("submit");
      String rate = request.getParameter("rate");
      boolean freeze = "Freeze".equals(submit);
      boolean thaw = "Thaw".equals(submit);
      boolean setRate = "Set Execution Rate".equals(submit) || submit==null;
      double erate = demoControlService.getExecutionRate();
      String tstatus = status;
      if (freeze) {
        blackboard.openTransaction();
        synchronized (FreezeSocietyPlugin.this) {
          freeze();
          status = "Freezing...";
          tstatus = status;
        }
        blackboard.closeTransactionDontReset();
      } else if (thaw) {
        blackboard.openTransaction();
        synchronized (FreezeSocietyPlugin.this) {
          thaw();
          status = "Running";
          tstatus = status;
        }
        blackboard.closeTransactionDontReset();
      } else if (setRate) {
        try {
          setRate(Double.parseDouble(rate));
        } catch (Exception e) {
        }
      }
      response.setContentType("text/html");
      if (tstatus.startsWith("Freezing")) {
        response.setHeader("Refresh", "1");
      }
      if (Math.abs(erate - newRate) / (erate + newRate) > 0.0001) {
        rate = String.valueOf(erate) + "->" + String.valueOf(newRate);
        response.setHeader("Refresh", "1");
      } else {
        rate = String.valueOf(erate);
      }
      PrintWriter out = response.getWriter();
      out.println("<html>");
      out.println("  <head>");
      out.println("    <title>Society Freeze Control</title>");
      out.println("  </head>");
      out.println("  <body>");
      out.println("    <h1>Society Freeze Control</h1>");
      out.println("    <form action=\"freezeControl\" method=\"post\">");
      // The following kludge the browser bugs that include the first
      // submit button if <enter> is typed in a text field.
      out.println("     <input type=\"image\" border=\"0\" name=\"refresh\" width=1 height=1 src=\"transprent-1x1.gif\"><br>");
      out.println(tstatus + "<br>");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Freeze\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Thaw\"><br>");
      out.println("Execution Time: " + formatTime(currentTimeMillis()) + "<br>");
      out.println("Execution Rate: ");
      out.println("<input type=\"text\" name=\"rate\" value=\"" + rate + "\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Set Execution Rate\"><br>");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      out.println("    </form>");
      out.println("  </body>");
      out.println("</html>");
    }
  }
}
      
