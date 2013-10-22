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

package org.cougaar.core.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;

/**
 * This component provides {@link javax.servlet.Servlet} access
 * to the {@link DemoControlService} for setting the demo (aka
 * "execution") time of the local node.
 * <p>
 * One plugin paramter if present and Boolean <tt>true</tt> will
 * cause the plugin to print the system and demo time every 10
 * secs (as measured by system and demo time)
 *
 * @see org.cougaar.core.service.DemoControlService
 */
public class DemoTimeControlPlugin extends ComponentPlugin {

  private boolean debug = false;

  long lastScenarioTime = 0;
  long lastRealTime = 0;

  ServletService servletService;
  DemoControlService demoControlService;
  LoggingService loggingService;

  DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private String printTime(String pfx) {
    long scenarioNow = alarmService.currentTimeMillis();
    long realNow = System.currentTimeMillis();
    long scenarioDelta = scenarioNow - lastScenarioTime;
    long realDelta = realNow - lastRealTime;

    String ret =
      "\n"
        + pfx
        + "Time at "
        + this.getAgentIdentifier().getAddress()
        + ".  Scenario Time is "
        + scenarioNow
        + "["
        + scenarioDelta
        + "]"
        + "{"
        + fmt.format(new Date(scenarioNow))
        + "}"
        + " Real Time is "
        + realNow
        + "["
        + realDelta
        + "]"
        + "{"
        + fmt.format(new Date(realNow))
        + "}"
        + "\nOffset = "
        + (scenarioNow - realNow);

    System.err.println(ret);
    lastScenarioTime = scenarioNow;
    lastRealTime = realNow;
    return ret;
  }

  private class MyAlarm implements Alarm {
    long exp;
    boolean realTime;
    public MyAlarm(long delta, boolean realTime) {
      if (realTime)
        this.exp = System.currentTimeMillis() + delta;
      else
        this.exp = alarmService.currentTimeMillis() + delta;

      this.realTime = realTime;
    }
    public long getExpirationTime() {
      return exp;
    }
    public void expire() {
      printTime(realTime ? "Real " : "Demo ");
      if (realTime)
        alarmService.addRealTimeAlarm(new MyAlarm(10000, true));
      else
        alarmService.addAlarm(new MyAlarm(10000, false));

    }
    @Override
   public String toString() {
      return "<" + exp + ">";
    }
    public boolean cancel() {
      return false;
    }
    /**
     * @see org.cougaar.core.agent.service.alarm.Alarm#hasExpired()
     */
    public boolean hasExpired() {
      return false;
    }

  }

  /**
   * @see org.cougaar.core.blackboard.BlackboardClientComponent#setupSubscriptions()
   */
  @Override
protected void setupSubscriptions() {
    Collection params = this.getParameters();
    if (params.contains("true"))
      debug = true;
    if (debug) {
      System.err.println(
        "timeControl plugin loaded at scenario time:" + alarmService.currentTimeMillis());
      printTime("setup");
    }

    try {
      servletService.register("/timeControl", new MyServlet());
    } catch (Exception ex) {
      System.err.println("Unable to register timeControl servlet");
      ex.printStackTrace();
    }

  }

  /**
   * @see org.cougaar.core.blackboard.BlackboardClientComponent#execute()
   */
  @Override
protected void execute() {
    if (debug) {
      printTime("execute");
      alarmService.addRealTimeAlarm(new MyAlarm(10000, true));
      alarmService.addRealTimeAlarm(new MyAlarm(10000, false));
    }
  }

  protected void updateTime(String advance, String rate, String changeTime) {
    if (loggingService.isDebugEnabled()) {
      loggingService.debug("Time to advance = "+advance+"; New Rate = "+rate+"; Change Time is "+changeTime);
    }
    if ((advance == null) && (rate == null))
      return;

    long l_advance = 0;
    if (advance != null) {
      try {
        l_advance = Long.parseLong(advance);
      } catch (NumberFormatException nfe) {
        System.err.println("Bad advance");
        nfe.printStackTrace();
      }
    }
    double d_rate = 1.0;
    if (rate != null) {
      try {
        d_rate = Double.parseDouble(rate);
      } catch (NumberFormatException nfe) {
        System.err.println("Bad rate");
        nfe.printStackTrace();
      }
    }

    long newTime = alarmService.currentTimeMillis() + l_advance;
    long quantization = 1L;
    if (l_advance >= 86400000L) {
      // Quantize to nearest day if step is a multiple of one day
      if ((l_advance % 86400000L) == 0L) {
        quantization = 86400000L;
      }
    } else if ((l_advance % 3600000L) == 0L) {
      // Quantize to nearest hour if step is a multiple of one hour
      quantization = 3600000;
    }
    newTime = (newTime / quantization) * quantization;

    if (changeTime != null && changeTime.length() > 0) {
      long l_changeTime = 0;
      try {
         l_changeTime = Long.parseLong(changeTime);
      } catch (NumberFormatException nfe) {
         System.err.println("Bad change time");
         nfe.printStackTrace();
      }
      demoControlService.setNodeTime(newTime, d_rate, l_changeTime);
    } else {
      demoControlService.setNodeTime(newTime, d_rate);
    }
  }

  protected void doit(HttpServletRequest req, HttpServletResponse res)
    throws IOException {

    updateTime(
      req.getParameter("timeAdvance"),
      req.getParameter("executionRate"),
      req.getParameter("changeTime"));

    PrintWriter out = res.getWriter();
    out.println("<html><head></head><body>");

    out.println("<FORM METHOD=\"GET\" ACTION=\"timeControl\">");
    out.println("<table>");
    out.println(
      "<tr><td>Scenario Time</td><td>"
        + fmt.format(new Date(alarmService.currentTimeMillis()))
        + "</td></tr>");
out.println(
"<tr><td>Test Time</td><td>"
 + System.currentTimeMillis()
 + "</td></tr>");
    out.println(
      "<tr><td>Time Advance (millisecs)</td><td><input type=\"text\" name=\"timeAdvance\" size=10 value=\""
        + 0
        + "\"> <i>1 Day = 86400000</i></td></tr>");
    out.println(
      "<tr><td>Execution Rate</td><td><input type=\"text\" name=\"executionRate\" size=10 value=\""
        + demoControlService.getExecutionRate()
        + "\"> <i>(required)</i></td></tr>");
    out.println(
      "<tr><td>Real Time for Change to take Effect</td><td><input type=\"text\" name=\"changeTime\" size=10 value=\""
        + ""
        + "\"> </td></tr>");
    out.println("</table>");
    out.println("<INPUT TYPE=\"submit\" Value=\"Submit\">");
    out.println("</FORM>");
    out.println("</body>");
    //    demoControlService.advanceTime(0, 2.0);
  }

  private class MyServlet extends HttpServlet {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
      doit(req, res);
    }
    @Override
   public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
      doit(req, res);
    }
  }

  /**
   * Returns the servletService.
   * @return ServletService
   */
  public ServletService getServletService() {
    return servletService;
  }

  /**
   * Sets the servletService.
   * @param servletService The servletService to set
   */
  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  /**
   * Returns the demoControlService.
   * @return DemoControlService
   */
  public DemoControlService getDemoControlService() {
    return demoControlService;
  }

  /**
   * Sets the demoControlService.
   * @param demoControlService The demoControlService to set
   */
  public void setDemoControlService(DemoControlService demoControlService) {
    this.demoControlService = demoControlService;
  }

  /**
   * Returns the loggingService.
   * @return LoggingService
   */
  public LoggingService getLoggingService() {
    return loggingService;
  }

  /**
   * Sets the loggingService.
   * @param loggingService The loggingService to set
   */
  public void setLoggingService(LoggingService loggingService) {
    this.loggingService = loggingService;
  }

}
