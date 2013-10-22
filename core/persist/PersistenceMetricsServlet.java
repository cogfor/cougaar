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

package org.cougaar.core.persist;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.PersistenceMetricsService;
import org.cougaar.core.service.ServletService;

/**
 * This component is a {@link javax.servlet.Servlet} that displays
 * the persistence snapshots available for the agent, and allows
 * the user to request a Full Persistence snapshot.
 */
public class PersistenceMetricsServlet extends ServiceUserPlugin {
  private static final String PERSIST_NOW = "PersistNow";

  private static final Class[] requiredServices = {
    ServletService.class,
    PersistenceMetricsService.class,
    EventService.class
  };

  private static SimpleDateFormat dateFormat;
  static {
    dateFormat = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss");
  }

  private ServletService servletService = null;
  private PersistenceMetricsService metricsService = null;
  private EventService eventService = null;
  private String agentName;

  public PersistenceMetricsServlet() {
    super(requiredServices);
  }

  protected boolean haveServices() {
    if (servletService != null) return true;
    if (acquireServices()) {
      eventService = getServiceBroker().getService(this, EventService.class, null);
      servletService = getServiceBroker().getService(this, ServletService.class, null);
      metricsService = getServiceBroker().getService(this, PersistenceMetricsService.class, null);
      try {
        servletService.register("/persistenceMetrics", new MyServlet());
      } catch (Exception e) {
        logger.error("Failed to register completionControl servlet", e);
      }
      return true;
    }
    return false;
  }

  @Override
public void setupSubscriptions() {
    agentName = getAgentIdentifier().toString();

    // haveServices will acquire the services if not done yet. Returns (ignored) boolean
    haveServices();
  }

  @Override
public void execute() {
    // haveServices will acquire the services if not done yet. Returns (ignored) boolean
    haveServices();
  }

  private static String getMedia(PersistenceMetricsService.Metric metric) {
    StringBuffer buf = new StringBuffer();
    buf.append(metric.getPersistencePluginName())
      .append("(");
    for (int i = 0, n = metric.getPersistencePluginParamCount(); i < n; i++) {
      if (i > 0) buf.append(",");
      buf.append(metric.getPersistencePluginParam(i));
    }
    buf.append(")");
    return buf.toString();
  }
  private static Long getTime(PersistenceMetricsService.Metric metric) {
    return new Long(metric.getStartTime());
  }

  private static Long getElapsed(PersistenceMetricsService.Metric metric) {
    return new Long(metric.getEndTime() - metric.getStartTime());
  }

  private static Long getCpu(PersistenceMetricsService.Metric metric) {
    return new Long(metric.getCpuTime());
  }

  private static Long getSize(PersistenceMetricsService.Metric metric) {
    return new Long(metric.getSize());
  }

  private static String getType(PersistenceMetricsService.Metric metric) {
    return metric.isFull() ? "Full" : "Delta";
  }

  private static String getName(PersistenceMetricsService.Metric metric) {
    return metric.getName();
  }

  private static class SortItem implements Comparable {
    int ix;
    Comparable key;
    public SortItem(int ix) {
      this(ix, new Integer(ix));
    }
    public SortItem(int ix, Comparable key) {
      this.ix = ix;
      this.key = key;
    }
    public int compareTo(Object o) {
      SortItem that = (SortItem) o;
      return key.compareTo(that.key);
    }
  }

  private class MyServlet extends HttpServlet {
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
    private String getSortParams(boolean currentReverse, String currentSort, String newSort) {
      String rev = "";
      if (newSort.equals(currentSort) && !currentReverse) rev = "&rev=true";
      return "sort=" + newSort + rev;
    }

    protected void doPostOrGet(HttpServletRequest request, HttpServletResponse response, boolean doUpdate)
      throws IOException
    {
      PrintWriter out = response.getWriter();
      out.println("<html>");
      out.println(" <head>");
      out.println("  <title>Persistence Metrics For " + agentName + "</title>");
      out.println(" </head>");
      out.println(" <body>");
      String submit = request.getParameter("submit");
      if (PERSIST_NOW.equals(submit)) {
        try {
          blackboard.persistNow();

	  // Send event allowing automated users to move on
	  if (eventService != null) {
	    eventService.event("Did Full Persist.");
	  }
        } catch (PersistenceNotEnabledException pnee) {
          out.println(pnee);
        }
      }
      String sort = request.getParameter("sort");
      boolean rev = "true".equals(request.getParameter("rev"));
      out.println("  <h1>Persistence Metrics For " + agentName + "</h1>");
      out.println("  <form method=\"GET\">");
      out.println("   <input type=\"submit\" name=\"submit\" value=\"" + PERSIST_NOW + "\">");
      out.println("  </form>");
      out.println("  <table border=1>");
      out.println("   <tr>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "time") + "\">Time</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "media") + "\">Media</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "type") + "\">Type</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "id") + "\">Id</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "elapsed") + "\">Elapsed (ms)</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "cpu") + "\">CPU (ms)</a></td>");
      out.println("    <td><A href=\"?" + getSortParams(rev, sort, "size") + "\">Bytes</a></td>");
      out.println("   </tr>");
      PersistenceMetricsService.Metric[] metrics = metricsService.getAll(PersistenceMetricsService.ALL);
      response.setContentType("text/html");
      SortItem[] items = new SortItem[metrics.length];
      if ("time".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getTime(metrics[i]));
        }
      } else if ("media".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getMedia(metrics[i]));
        }
      } else if ("type".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getType(metrics[i]));
        }
      } else if ("id".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getName(metrics[i]));
        }
      } else if ("elapsed".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getElapsed(metrics[i]));
        }
      } else if ("cpu".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getCpu(metrics[i]));
        }
      } else if ("size".equals(sort)) {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i, getSize(metrics[i]));
        }
      } else {
        for (int i = 0; i < metrics.length; i++) {
          items[i] = new SortItem(i);
        }
      }        
      Arrays.sort(items);
      for (int i = 0; i < items.length; i++) {
        SortItem item = items[rev ? items.length - 1 - i : i];
        printMetric(out, metrics[item.ix], null);
      }
      out.println("<tr></tr>");
      printMetric(out, metricsService.getAverage(PersistenceMetricsService.FULL),
                  "Average(" + metricsService.getCount(PersistenceMetricsService.FULL) + ") Full");
      printMetric(out, metricsService.getAverage(PersistenceMetricsService.DELTA),
                  "Average" + metricsService.getCount(PersistenceMetricsService.DELTA) + ") Delta");
      printMetric(out, metricsService.getAverage(PersistenceMetricsService.ALL),
                  "Average" + metricsService.getCount(PersistenceMetricsService.ALL) + ") All");
      out.println("  </table>");
      out.println(" </body>");
      out.println("</html>");
    }
  }

  private void printMetric(PrintWriter out, PersistenceMetricsService.Metric metric, String kind) {
    out.println("   <tr>");
    if (kind != null) {
      out.println("    <td><p align=\"right\">" + dateFormat.format(new Date()) + "</p></td>");
      out.println("    <td colspan=3><p align=\"left\" >" + kind + "</p></td>");
    } else {
      out.println("    <td><p align=\"right\">" + dateFormat.format(new Date(metric.getStartTime())) + "</p></td>");
      out.println("    <td><p align=\"left\" >" + getMedia(metric) + "</p></td>");
      out.println("    <td><p align=\"left\" >" + getType(metric) + "</p></td>");
      out.println("    <td><p align=\"left\" >" + getName(metric) + "</p></td>");
    }
    out.println("    <td><p align=\"right\">" + getElapsed(metric) + "</p></td>");
    out.println("    <td><p align=\"right\">" + getCpu(metric) + "</p></td>");
    out.println("    <td><p align=\"right\">" + getSize(metric) + "</p></td>");
    out.println("   </tr>");
  }
}
