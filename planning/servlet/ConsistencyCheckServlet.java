/*
 *
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

package org.cougaar.planning.servlet;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.UniqueObjectSet;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.servlet.ComponentServlet;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.LogPlan;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.planning.ldm.lps.ConsistencyChecker;
import org.cougaar.planning.ldm.measure.AbstractMeasure;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AllocationforCollections;
import org.cougaar.planning.ldm.plan.AspectScorePoint;
import org.cougaar.planning.ldm.plan.AspectScoreRange;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Composition;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.ItineraryElement;
import org.cougaar.planning.ldm.plan.Location;
import org.cougaar.planning.ldm.plan.LocationRangeScheduleElement;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.MPTask;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.PlanElementSet;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleElement;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.plugin.util.PluginHelper;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.PropertyTree;
import org.cougaar.util.Sortings;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A <code>Servlet</code> that checks the blackboard for plan
 * object consistency, such as checking that the blackboard
 * contains the parent tasks.
 * <p>
 * Load in all agents &amp; nodes as:<pre>
 *   &lt;component class="org.cougaar.planning.servlet.ConsistencyCheckServlet"&gt;
 *     &lt;argument&gt;/consistency&lt;/argument&gt;
 *   &lt;/component&gt;
 * </pre> 
 */
public class ConsistencyCheckServlet
extends ComponentServlet 
{

  private BlackboardQueryService blackboard;

  public void setBlackboardQueryService(
      BlackboardQueryService blackboard) {
    this.blackboard = blackboard;
  }

  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException
  {
    (new Worker(request, response)).execute();
  }

  /** This inner class does all the work. */
  private class Worker {
    // from the "doGet(..)":
    private HttpServletRequest request;
    private HttpServletResponse response;

    // TBA url parameters

    // writer from the request
    private PrintWriter out;

    private UniqueObjectSet taskSet;
    private PlanElementSet planElementSet;

    private Logger logger;
    private LogPlan logplan;

    public Worker(
        HttpServletRequest request,
        HttpServletResponse response) {
      this.request = request;
      this.response = response;
    }

    public void execute() throws IOException {
      parseParams();
      writeResponse();
    }

    private void parseParams() {
      // no parameters for now
      //value = getParameter("name", "default");
    }

    private String getParameter(String name, String defaultValue) {
      String value = request.getParameter(name);
      if (value != null) {
        value = value.trim();
        if (value.length() == 0) {
          value = null;
        }
      }
      if (value == null) {
        value = defaultValue;
      }
      return value;
    }

    private void writeResponse() throws IOException {
      response.setContentType("text/html");
      this.out = response.getWriter();

      String title = getEncodedAgentName()+" Consistency Checker";
      out.println(
          "<html><head><title>"+
          title+
          "</title></head><body bgcolor=\"white\">"+
          "<h1>"+title+"</h1><p>");

      out.println("<h2>Validating Tasks["+getTaskCount()+"]:</h2><hr>");
      boolean tasksOkay = validateTasks();
      out.println("<br><hr><p>");

      out.println(
          "<h2>Validating PlanElements["+getPlanElementCount()+
          "]:</h2><hr>");
      boolean planElementsOkay = validatePlanElements();
      out.println("<br><hr>");

      boolean allOkay = (tasksOkay && planElementsOkay);

      out.println(
          "<h2>"+(allOkay ? "All" : "Not")+" Consistent</h2>");

      String color = (allOkay ? "green" : "red");
      out.println(
          "<table width=\"100%\" bgcolor=\""+color+
          "\"><tr><td>&nbsp;</td></tr></table>");

      out.println(
          "</body></html>");
    }

    private void findTasks() {
      if (taskSet != null) {
        return;
      }
      UnaryPredicate taskP =
        new UnaryPredicate() {
          public boolean execute(Object o) {
            return (o instanceof Task);
          }
        };
      Collection col = blackboard.query(taskP);
      taskSet = new UniqueObjectSet();
      taskSet.addAll(col);
    }

    private int getTaskCount() {
      findTasks();
      return taskSet.size();
    }

    private Task findTask(UID uid) {
      findTasks();
      return (Task) taskSet.findUniqueObject(uid);
    }

    private void findPlanElements() {
      if (planElementSet != null) {
        return;
      }
      UnaryPredicate planElementP = new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof PlanElement);
        }
      };
      Collection col = blackboard.query(planElementP);
      planElementSet = new PlanElementSet();
      planElementSet.addAll(col);
    }

    private int getPlanElementCount() {
      findPlanElements();
      return planElementSet.size();
    }

    private PlanElement findPlanElement(Task task) {
      findPlanElements();
      return planElementSet.findPlanElement(task);
    }

    private boolean validateTasks() {
      boolean ret = true;
      for (Iterator iter = taskSet.iterator();
          iter.hasNext();
          ) {
        Task t = (Task) iter.next();
        if (!validateTask(t)) {
          ret = false;
        }
      }
      return ret;
    }

    private boolean validateTask(Task t) {
      return 
        ConsistencyChecker.isTaskConsistent(
            getAgentIdentifier(),
            getLogger(),
            getLogPlan(),
            t,
            "Task found");
    }

    private boolean validatePlanElements() {
      boolean ret = true;
      for (Iterator iter = planElementSet.iterator();
          iter.hasNext();
          ) {
        PlanElement pe = (PlanElement) iter.next();
        if (!validatePlanElement(pe)) {
          ret = false;
        }
      }
      return ret;
    }

    private boolean validatePlanElement(PlanElement pe) {
      return 
        ConsistencyChecker.isPlanElementConsistent(
            getAgentIdentifier(),
            getLogger(),
            getLogPlan(),
            pe,
            true);
    }

    private Logger getLogger() { 
      if (logger != null) {
        return logger;
      }
      logger = new LoggerAdapter() {
        public boolean isEnabledFor(int level) {
          return true;
        }
        public void log(int level, String message, Throwable t) {
          out.println(
              "<b>"+convertIntToString(level)+"</b>"+
              " - "+encodeHTML(message, false));
          if (t != null) {
            out.println("<pre>");
            t.printStackTrace(out);
            out.println("</pre>");
          }
          out.println("<br>");
        }
        public void printDot(String dot) {
        }
        private String convertIntToString(int level) {
          switch (level) {
            case Logger.DETAIL: return "DETAIL";
            case Logger.DEBUG: return "DEBUG";
            case Logger.INFO:  return "INFO";
            case Logger.WARN:  return "WARN";
            case Logger.ERROR: return "ERROR";
            case Logger.SHOUT: return "SHOUT";
            case Logger.FATAL: return "FATAL";
            default: return null;
          }
        }
      };
      return logger;
    }
    private LogPlan getLogPlan() {
      if (logplan != null) {
        return logplan;
      }
      logplan = new LogPlan() {
        public PlanElement findPlanElement(Task task) {
          return Worker.this.findPlanElement(task);
        }
        public Task findTask(UID uid) {
          return Worker.this.findTask(uid);
        }
        public Task findTask(Task task) {
          return (Task) taskSet.findUniqueObject(task.getUID());
        }
        // unsupported operations:
        private void die() {
          throw new UnsupportedOperationException();
        }
        public void setupSubscriptions(Blackboard blackboard) { die(); }
        public PlanElement findPlanElement(String task) { die(); return null; }
        public PlanElement findPlanElement(UID uid) { die(); return null; }
        public Task findTask(String id) { die(); return null; }
        public Asset findAsset(Asset asset) { die(); return null; }
        public Asset findAsset(String id) { die(); return null; }
        public void incAssetCount(int inc) { die(); }
        public void incPlanElementCount(int inc) { die(); }
        public void incTaskCount(int inc) { die(); }
        public void incWorkflowCount(int inc) { die(); }
      };
      return logplan;
    }
  }

  private static String encodeHTML(String s, boolean noBreakSpaces) {
    StringBuffer buf = null;  // In case we need to edit the string
    int ix = 0;               // Beginning of uncopied part of s
    for (int i = 0, n = s.length(); i < n; i++) {
      String replacement = null;
      switch (s.charAt(i)) {
        case '"': replacement = "&quot;"; break;
        case '<': replacement = "&lt;"; break;
        case '>': replacement = "&gt;"; break;
        case '&': replacement = "&amp;"; break;
        case ' ': if (noBreakSpaces) replacement = "&nbsp;"; break;
      }
      if (replacement != null) {
        if (buf == null) buf = new StringBuffer();
        buf.append(s.substring(ix, i));
        buf.append(replacement);
        ix = i + 1;
      }
    }
    if (buf != null) {
      buf.append(s.substring(ix));
      return buf.toString();
    } else {
      return s;
    }
  }
}
