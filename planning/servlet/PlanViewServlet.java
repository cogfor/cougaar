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

package org.cougaar.planning.servlet;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
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
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.cougaar.core.blackboard.PublisherInfo;
import org.cougaar.core.blackboard.PublisherSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.asset.AggregateAsset;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetGroup;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.TypeIdentificationPG;
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
import org.cougaar.util.StackElements;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

/**
 * A <code>Servlet</code> that generates HTML views of an Agent's Blackboard.
 * <p>
 * This Servlet allows the user to: <ol>
 *   <li> List Tasks, PlanElements, Assets, UniqueObjects</li>
 *   <li> View detailed information for Tasks, PlanElements, 
 *        etc.</li>
 *   <li> Search for an item by UID</li>
 *   <li> Illustrates the use of URLs to allow the user to 
 *        seamlessly jump between views of separate blackboards</li>
 *   <li> Provide XML views of data (using XMLize)</li>
 * </ol>.
 * <p>
 * This is a very large Servlet and is overly-complex to be
 * a good example for future Servlet developers.  Consider
 * using this simple example:
 * <pre>
 *    public class HelloServlet extends HttpServlet {
 *      private SimpleServletSupport support;
 *      public void setSimpleServletSupport(SimpleServletSupport support) {
 *        this.support = support;
 *      }
 *      public void doGet(
 *          HttpServletRequest req,
 *          HttpServletResponse res) throws IOException {
 *        PrintWriter out = req.getWriter();
 *        out.print("Hello from agent "+support.getEncodedAgentName());
 *      }
 *    }
 * </pre>
 * <p>
 * <pre>
 * @property org.cougaar.planning.servlet.planview.preds
 *    Name of the example predicate file that is loading into
 *    the "Advanced Search" page, which defaults to 
 *    "default.preds.dat"
 * </pre>
 */
public class PlanViewServlet
extends ComponentPlugin
{

  // this matches the Subscriber system property that's required to capture
  // the additional blackboard detail.
  private static final boolean ENABLE_STACKS =
    Boolean.getBoolean("org.cougaar.core.blackboard.trackPublishers");

  private BlackboardQueryService blackboardQueryService;
  private ServletService servletService;
  private String myPath;
  private PublisherSubscription sub;

  // *not* the one in core -- see below for our private adapter class.
  private SimpleServletSupport support;

  // set out path
  public void setParameter(Object o) {
    if (o instanceof String) {
      myPath = (String) o;
    } else if (o instanceof List) {
      List l = (List) o;
      if (l.size() > 0) {
        Object o1 = l.get(0);
        if (o1 instanceof String) {
          myPath = (String) o1;
        }
      }
    }
  }

  public void setBlackboardQueryService(
      BlackboardQueryService blackboardQueryService) {
    this.blackboardQueryService = blackboardQueryService;
  }

  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  public void load() {
    super.load();

    this.support = new SimpleServletSupport();

    HttpServlet servlet = new HttpServlet() {
      public void doGet(
          HttpServletRequest request,
          HttpServletResponse response) throws IOException, ServletException {
        PlanViewServlet.this.doGet(request, response);
      }
    };
    try {
      servletService.register(myPath, servlet);
    } catch (Exception e) {
      throw new RuntimeException("Unable register "+myPath, e);
    }
  }

  protected void setupSubscriptions() {
    if (ENABLE_STACKS) {
      UnaryPredicate pred = new UniqueObjectPredicate();
      sub = (PublisherSubscription) 
        getBlackboardService().subscribe(new PublisherSubscription(pred));
    }
  }
  protected void execute() {
    // ignore changes
  }

  private static final class UniqueObjectPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof UniqueObject);
    }
  }

  // adapted from old "SimpleServletSupport":
  private class SimpleServletSupport {
    private final String encAgentName;
    public SimpleServletSupport() {
      this.encAgentName = encodeAgentName(getAgentIdentifier().getAddress());
    }
    public String getPath() {
      return myPath;
    }
    public String getEncodedAgentName() {
      return encAgentName;
    }
    public String encodeAgentName(String name) {
      try {
        return URLEncoder.encode(name, "UTF-8");
      } catch (java.io.UnsupportedEncodingException e) {
        // should never happen
        throw new RuntimeException("Unable to encode to UTF-8?");
      }
    }
    public Collection queryBlackboard(UnaryPredicate pred) {
      return blackboardQueryService.query(pred);
    }

    // note that our sub is thread-safe and can be used outside of a
    // blackboard transaction
    public PublisherInfo getInfo(UID uid) {
      return (sub == null ? null : sub.getInfo(uid));
    }
  }

  public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException, ServletException
  {
    // create a new "PlanViewer" context per request
    PlanViewer pv = new PlanViewer(support);
    pv.execute(request, response);  
  }

  /**
   * This inner class does all the work.
   * <p>
   * A new class is created per request, to keep all the
   * instance fields separate.  If there was only one
   * instance then multiple simultaneous requests would
   * corrupt the instance fields (e.g. the "out" stream).
   * <p>
   * This acts as a <b>context</b> per request.
   */
  private static class PlanViewer {

    // some constants:
    private static final String PREDS_FILENAME_PROPERTY =
      "org.cougaar.planning.servlet.planview.preds";
    private static final String DEFAULT_PREDS_FILENAME = 
      "default.preds.dat";
    private static final boolean DEBUG = false;
    private static final int DEFAULT_LIMIT = 100;

    //
    // parameters from the URL:
    //

    /**
     * "mode" constants, which control which page to generate.
     */
    public static final String MODE = "mode";
    public static final int MODE_FRAME                        =  0;
    public static final int MODE_ALL_TASKS                    =  1;
    public static final int MODE_AGENTS                       =  2;
    public static final int MODE_TASK_DETAILS                 =  3;
    public static final int MODE_TASKS_SUMMARY                =  4;
    public static final int MODE_PLAN_ELEMENT_DETAILS         =  5;
    public static final int MODE_ALL_PLAN_ELEMENTS            =  6;
    public static final int MODE_ASSET_DETAILS                =  7;
    public static final int MODE_ALL_ASSETS                   =  8;
    public static final int MODE_SEARCH                       =  9;
    public static final int MODE_XML_HTML_DETAILS             = 10;
    public static final int MODE_XML_RAW_DETAILS              = 11;
    public static final int MODE_ALL_UNIQUE_OBJECTS           = 12;
    public static final int MODE_WELCOME                      = 13;
    public static final int MODE_WELCOME_DETAILS              = 14;
    public static final int MODE_TASK_DIRECT_OBJECT_DETAILS   = 15;
    public static final int MODE_ASSET_TRANSFER_ASSET_DETAILS = 16;
    public static final int MODE_XML_HTML_ATTACHED_DETAILS    = 17;
    public static final int MODE_XML_RAW_ATTACHED_DETAILS     = 18;
    public static final int MODE_ADVANCED_SEARCH_FORM         = 19;
    public static final int MODE_ADVANCED_SEARCH_RESULTS      = 20;
    public static final int MODE_STACK_DETAILS                = 21;
    private int mode = -1;

    // filter by uid
    public static final String ITEM_UID = "uid";
    private String itemUID;

    // filter by task verb
    public static final String VERB = "verb";
    private String verbFilter;

    // limit quantity of data
    public static final String LIMIT = "limit";
    private boolean limit;

    // predicate
    public static final String PREDICATE = "pred";
    private String pred;

    // view parsed predicate for debugging
    public static final String PREDICATE_DEBUG = "predDebug";
    private boolean predDebug;

    // sort results by UID
    public static final String SORT_BY_UID = "sortByUID";
    private boolean sortByUID;

    // sort results by END_TIME preference
    public static final String SORT_BY_END_TIME = "sortByEndTime";
    private boolean sortByEndTime;

    // writer from the request
    private PrintWriter out;

    // since "PlanViewer" is a static inner class, here
    // we hold onto the support API.
    //
    // this makes it clear that PlanViewer only uses
    // the "support" from the outer class.
    private SimpleServletSupport support;

    public PlanViewer(SimpleServletSupport support) {
      this.support = support;
    }

    /**
     * Main method.
     */
    public void execute(
        HttpServletRequest request, 
        HttpServletResponse response) throws IOException, ServletException 
    {
      this.out = response.getWriter();

      // create a URL parameter visitor
      ServletUtil.ParamVisitor vis = 
        new ServletUtil.ParamVisitor() {
          public void setParam(String name, String value) {
            if (name.equalsIgnoreCase(MODE)) {
              try {
                mode = Integer.parseInt(value);
              } catch (Exception eBadNumber) {
                System.err.println("INVALID MODE: "+value);
                mode = MODE_FRAME;
              }
            } else if (name.equalsIgnoreCase(ITEM_UID)) {
              if (value != null) {
                try {
                  itemUID = URLDecoder.decode(value, "UTF-8");
                } catch (Exception eBadEnc) {
                  System.err.println("INVALID UID: "+value);
                }
              }
            } else if (name.equalsIgnoreCase(VERB)) {
              verbFilter = value;
            } else if (name.equalsIgnoreCase(LIMIT)) {
              limit = "true".equalsIgnoreCase(value);
            } else if (name.equalsIgnoreCase(PREDICATE)) {
              pred = value;
            } else if (name.equalsIgnoreCase(PREDICATE_DEBUG)) {
              predDebug = 
                ((value != null) ?  
                 value.equalsIgnoreCase("true") : 
                 true);
            } else if (name.equalsIgnoreCase(SORT_BY_UID)) {
	      sortByUID = 
		((value != null) ?  
                 value.equalsIgnoreCase("true") : 
                 true);
            } else if (name.equalsIgnoreCase(SORT_BY_END_TIME)) {
	      sortByEndTime = 
		((value != null) ?  
                 value.equalsIgnoreCase("true") : 
                 true);
            }
          }
        };

      // visit the URL parameters
      ServletUtil.parseParams(vis, request);

      try {
        // decide which page to generate
        switch (mode) {
          default:
            if (DEBUG) {
              System.err.println("DEFAULT MODE");
            }
          case MODE_FRAME:
            displayFrame();
            break;
          case MODE_WELCOME:
            displayWelcome();
            break;
          case MODE_WELCOME_DETAILS:
            displayWelcomeDetails();
            break;
          case MODE_ALL_TASKS:
            displayAllTasks();
            break;
          case MODE_TASK_DETAILS:
            displayTaskDetails();
            break;
          case MODE_TASKS_SUMMARY:
            displayTasksSummary();
            break;
          case MODE_PLAN_ELEMENT_DETAILS:
            displayPlanElementDetails();
            break;
          case MODE_ALL_PLAN_ELEMENTS:
            displayAllPlanElements();
            break;
          case MODE_ASSET_DETAILS:
          case MODE_TASK_DIRECT_OBJECT_DETAILS:
          case MODE_ASSET_TRANSFER_ASSET_DETAILS:
            displayAssetDetails();
            break;
          case MODE_ALL_ASSETS:
            displayAllAssets();
            break;
          case MODE_AGENTS:
          case MODE_SEARCH:
            displaySearch();
            break;
          case MODE_XML_HTML_DETAILS:
          case MODE_XML_HTML_ATTACHED_DETAILS:
          case MODE_XML_RAW_DETAILS:
          case MODE_XML_RAW_ATTACHED_DETAILS:
            displayUniqueObjectDetails();
            break;
          case MODE_ALL_UNIQUE_OBJECTS:
            displayAllUniqueObjects();
            break;
          case MODE_ADVANCED_SEARCH_FORM:
            displayAdvancedSearchForm();
            break;
          case MODE_ADVANCED_SEARCH_RESULTS:
            displayAdvancedSearchResults();
            break;
          case MODE_STACK_DETAILS:
            displayStackDetails();
            break;
        }
      } catch (Exception e) {
        System.err.println(
            "/$"+
            support.getEncodedAgentName()+
            support.getPath()+
            " Exception: ");
        e.printStackTrace();
        out.print(
            "<html><body><h1>"+
            "<font color=red>Unexpected Exception!</font>"+
            "</h1><p><pre>");
        e.printStackTrace(out);
        out.print("</pre></body></html>");
        out.flush();
      }
    }

    /** BEGIN DISPLAY ROUTINES **/

    /**
     * displayFrame.
     */
    private void displayFrame()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Frame");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Cougaar PlanViewer"+
          "</title>\n"+
          "</head>\n"+
          "<frameset cols=\"25%,75%\">\n"+
          "<frameset rows=\"40%,60%\">\n"+
          "<frameset rows=\"25%,75%\">\n"+
          "<frame"+
          " src=\""+
          "/agents?format=select&suffix="+
          support.getEncodedAgentName()+
          "\" name=\"agentFrame\">\n"+
          "<frame src=\"/$"+
          support.getEncodedAgentName()+
          support.getPath()+
          "?"+
          MODE+
          "="+
          MODE_SEARCH+
          "\" name=\"searchFrame\">\n"+
          "</frameset>\n");
      //
      // Show blank WelcomeDetails page in itemFrame, since user
      // probably didn't specify $encodedAgentName in URL.
      //
      out.print("<frame src=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print(
          "?"+
          MODE+
          "="+
          MODE_WELCOME_DETAILS+
          "\" name=\"itemFrame\">\n"+
          "</frameset>\n"+
          "<frame src=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      // 
      // Show blank Welcome page in tablesFrame, since user
      // probably didn't specify $encodedAgentName in URL.
      //
      out.print(
          "?"+
          MODE+
          "="+
          MODE_WELCOME+
          "\" name=\"tablesFrame\">\n"+
          "</frameset>\n"+
          "<noframes>\n"+
          "<h2>Frame Task</h2>\n"+
          "<p>"+
          "This document is designed to be viewed using the frames feature. "+
          "If you see this message, you are using a non-frame-capable web "+
          "client.\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayWelcome.
     */
    private void displayWelcome()
    {
      if (DEBUG) {
        System.out.println("Display Welcome");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "COUGAAR PlanView"+
          "</title>\n"+
          "</head>\n"+
          "<body ");
      out.print(
          "bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<font size=small color=mediumblue>No Agent selected.</font>\n"+
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayWelcomeDetails.
     */
    private void displayWelcomeDetails()
    {
      if (DEBUG) {
        System.out.println("Display Welcome Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Item Details View"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<font size=small color=mediumblue>No Item selected.</font>\n"+
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayTaskDetails.
     */
    private void displayTaskDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Task Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Children Task View"+
          "</title>"+
          "</head>\n"+
          "<body  bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to agent
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n"+
          "Task<br>");
      // find task
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      if (baseObj instanceof Task) {
        printTaskDetails((Task)baseObj);
      } else {
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No Task selected.");
        } else if (baseObj == null) {
          out.print("No Task matching \"");
          out.print(itemUID);
          out.print("\" found.");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not a Task: ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllTasks.
     */
    private void displayAllTasks()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All Tasks");
      }
      // find tasks
      Collection col;
      if (verbFilter != null) {
        col = findTasksWithVerb(verbFilter);
      } else {
        col = findAllTasks();
      }
      int numTasks = col.size();
      Iterator tasksIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Tasks");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Tasks"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<p>"+
          "<center>");
      if (limit && (numTasks > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numTasks);
      out.print(
          "</b> Task");
      if (numTasks != 1) {
        out.print("s");
      }
      if (verbFilter != null) {
        out.print(" with verb ");
        out.print(verbFilter);
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      if (limit && (numTasks > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all tasks.
        printLinkToAllTasks(
            verbFilter, 0, numTasks, true);
        out.print("</center>\n");
      }
      // print table headers
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=8>"+
          "<font size=+1 color=mediumblue><b>Tasks</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td rowspan=2><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td rowspan=2><font color=mediumblue><b>Verb</b></font></td>\n"+
          "<td colspan=4>"+
          "<font color=mediumblue><b>Direct Object</b></font>"+
          "</td>\n"+
          "<td rowspan=2>"+
          "<font color=mediumblue><b>Prepositional Phrases</b></font>"+
          "</td>\n"+
          "<td rowspan=2>"+
          "<font color=mediumblue><b>Preference Best Values</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td><font color=mediumblue><b>TypeID</b></font></td>\n"+
        "<td><font color=mediumblue><b>ItemID</b></font></td>\n"+
        "<td><font color=mediumblue><b>Quantity</b></font></td>\n"+
        "</tr>\n");
      if (numTasks > 0) {
        // print table rows
        int rows = 0;
        while (tasksIter.hasNext()) {
          Task task = (Task)tasksIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          printLinkToLocalTask(task);
          out.print(
              "</td>\n"+
              "<td>\n");
          // show verb
          Verb v = task.getVerb();
          if (v != null) {
            out.print(v.toString());
          } else {
            out.print("<font color=red>missing verb</font>");
          }
          out.print("</td>\n");
          // show direct object
          printTaskDirectObjectTableRow(task);
          // show prepositional phrases
          out.print(
              "<td>"+
              "<font size=-1>");
          Enumeration enprep = task.getPrepositionalPhrases();
          while (enprep.hasMoreElements()) {
            PrepositionalPhrase pp = 
              (PrepositionalPhrase)enprep.nextElement();
            String prep = pp.getPreposition();
            out.print("<font color=mediumblue>");
            out.print(prep);
            out.print("</font> ");
            printObject(pp.getIndirectObject());
            out.print("<br>");
          }
          out.print("</font></td><td><font size=-1>");
          Enumeration enpref = task.getPreferences();
          while (enpref.hasMoreElements()) {
            Preference pref = (Preference) enpref.nextElement();
            AspectValue av = pref.getScoringFunction().getBest().getAspectValue();
            String type = AspectValue.aspectTypeToString(av.getAspectType());
            out.print(encodeHTML(type + ": " + av.toString(), true));
            out.print("<br>");
          }
          out.print(
              "</font>"+
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllTasks(
              verbFilter, 0, numTasks, true);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        // end table
        out.print(
            "</table>\n"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No Tasks");
        if (verbFilter != null) {
          out.print(" with verb ");
          out.print(verbFilter);
        }
        out.print(" found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayTaskSummary.
     */
    private void displayTasksSummary()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Tasks Summary");
      }
      // find tasks
      boolean oldSortByUID = sortByUID;
      sortByUID = false;
      boolean oldSortByEndTime = sortByEndTime;
      sortByEndTime = false;
      Collection col = findAllTasks();
      sortByUID = oldSortByUID;
      sortByEndTime = oldSortByEndTime;
      int numTasks = col.size();
      Iterator tasksIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Tasks");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Tasks Summary"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      printLinkToAllTasks(
          null, 0, numTasks, false);
      out.print("</center>\n");
      if (numTasks > DEFAULT_LIMIT) {
        // give limit option
        out.print("<center>");
        printLinkToAllTasks(
            null, DEFAULT_LIMIT, numTasks, false);
        out.print("</center>\n");
      }
      // begin table
      out.print(
          "<p>\n"+
          "<table align=center border=1 cellpadding=1 cellspacing=1\n"+
          " width=75% bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>Tasks Summary</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>Verb</font></b></td>\n"+
          "<td><font color=mediumblue><b>Count</font></b></td>\n"+
          "</tr>\n");
      // table rows
      if (numTasks != 0) {
        // count by verb
        HashMap tasksInfoMap = new HashMap();
        while (tasksIter.hasNext()) {
          Task task = (Task)tasksIter.next();
          Verb verb = task.getVerb();
          VerbSummaryInfo info = 
            (VerbSummaryInfo)tasksInfoMap.get(verb);
          if (info == null) {
            info = new VerbSummaryInfo(verb);
            tasksInfoMap.put(verb, info);
          }
          ++info.counter;
        }
        // sort by verb
        Collection sortedInfosCol =
          Sortings.sort(
              tasksInfoMap.values(),
              SummaryInfo.LARGEST_COUNTER_FIRST_ORDER);
        Iterator sortedInfosIter = sortedInfosCol.iterator();
        // print rows
        while (sortedInfosIter.hasNext()) {
          VerbSummaryInfo info = (VerbSummaryInfo)sortedInfosIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          // link to all tasks with verb
          printLinkToAllTasks(
              info.verb.toString(), 0, info.counter, false);
          if (info.counter > DEFAULT_LIMIT) {
            // link to limited number of tasks with verb
            out.print(" (");
            printLinkToAllTasks(
                info.verb.toString(), DEFAULT_LIMIT, info.counter, false);
            out.print(")");
          }
          out.print(
              "</td>\n"+
              "<td align=right>");
          out.print(info.counter);
          out.print(
              "</td>\n"+
              "</tr>\n");
        }
      }
      // end table
      out.print("</table>\n");
      if (numTasks == 0) {
        out.print(
            "<center>"+
            "<font color=mediumblue >\n"+
            "No Tasks found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
    /**
     * displayPlanElementDetails.
     */
    private void displayPlanElementDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay PlanElement Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "PlanElement View"+
          "</title>"+
          "</head>\n"+
          "<body  bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to agent
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n");
      // find plan element
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      if (baseObj instanceof PlanElement) {
        printPlanElementDetails((PlanElement)baseObj);
      } else {
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No PlanElement selected.");
        } else if (baseObj == null) {
          out.print("No PlanElement matching \"");
          out.print(itemUID);
          out.print("\" found.");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not a PlanElement: ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllPlanElements.
     */
    private void displayAllPlanElements()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All PlanElements");
      }
      Collection col = findAllPlanElements();
      int numPlanElements = col.size();
      Iterator peIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched PlanElements");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " PlanElements"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numPlanElements > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numPlanElements);
      out.print(
          "</b> PlanElement");
      if (numPlanElements != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>");
      if (limit && (numPlanElements > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all pes
        printLinkToAllPlanElements(
            0, numPlanElements, false);
        out.print("</center>");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>PlanElements</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td><font color=mediumblue><b>Type</b></font></td>\n"+
          "</tr>\n");
      if (numPlanElements > 0) {
        // print table rows
        int rows = 0;
        while (peIter.hasNext()) {
          PlanElement pe = (PlanElement)peIter.next();
          out.print(
              "<tr>\n"+
              "<td>\n");
          printLinkToPlanElement(pe);
          out.print(
              "</td>\n"+
              "<td>\n");
          int peType = getItemType(pe);
          if (peType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[peType]);
          } else {
            out.print("<font color=red>");
            if (pe != null) {
              out.print(pe.getClass().getName());
            } else {
              out.print("null");
            }
            out.print("</font>");
          }
          out.print(
              "</td>"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllPlanElements(
              0, numPlanElements, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No PlanElements found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAssetDetails.
     */
    private void displayAssetDetails()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Asset Details");
      }
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>"+
          "Asset View"+
          "</title>"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<b>");
      // link to agent
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n"+
          "Asset<br>");
      // find "base" UniqueObject with the specifed UID
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      Asset asset = null;
      // get asset
      switch (mode) {
        case MODE_ASSET_DETAILS:
          // asset itself
          if (baseObj instanceof Asset) {
            asset = (Asset)baseObj;
          }
          break;
        case MODE_TASK_DIRECT_OBJECT_DETAILS:
          // asset attached to Task
          if (baseObj instanceof Task) {
            asset = ((Task)baseObj).getDirectObject();
          }
          break;
        case MODE_ASSET_TRANSFER_ASSET_DETAILS:
          // asset attached to AssetTransfer
          if (baseObj instanceof AssetTransfer) {
            asset = ((AssetTransfer)baseObj).getAsset();
          }
          break;
        default:
          break;
      }
      if (asset != null) {
        printAssetDetails(baseObj, asset);
      } else {
        String baseType;
        switch (mode) {
          case MODE_ASSET_DETAILS:
            baseType = "Asset";
            break;
          case MODE_TASK_DIRECT_OBJECT_DETAILS:
            baseType = "Task";
            break;
          case MODE_ASSET_TRANSFER_ASSET_DETAILS:
            baseType = "AssetTransfer";
            break;
          default:
            baseType = "<font color=red>Error</font>";
            break;
        }
        out.print(
            "<p>"+
            "<font size=small color=mediumblue>");
        if (itemUID == null) {
          out.print("No ");
          out.print(baseType);
          out.print(" selected.");
        } else if (baseObj == null) {
          out.print("No ");
          out.print(baseType);
          out.print(" matching \"");
          out.print(itemUID);
          out.print("\" found in ");
          out.print(support.getEncodedAgentName());
          out.print(".");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(itemUID);
          out.print("\" is not of type ");
          out.print(baseType);
          out.print(": ");
          out.print(baseObj.getClass().getName());
        }
        out.print(
            "</font>"+
            "<p>\n");
      }
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displayAllAssets.
     */
    private void displayAllAssets()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All Assets");
      }
      Collection col = findAllAssets();
      int numAssets = col.size();
      Iterator assetIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Assets");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Assets"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numAssets > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numAssets);
      out.print(
          "</b> Asset");
      if (numAssets != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>");
      if (limit && (numAssets > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all assets
        printLinkToAllAssets(
            0, numAssets, false);
        out.print("</center>");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=4>"+
          "<font size=+1 color=mediumblue><b>Assets</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</font></b></td>\n"+
          "<td><font color=mediumblue><b>TypeID</font></b></td>\n"+
          "<td><font color=mediumblue><b>ItemID</font></b></td>\n"+
          "<td><font color=mediumblue><b>Quantity</font></b></td>\n"+
          "</tr>\n");
      if (numAssets > 0) {
        // print table rows
        int rows = 0;
        while (assetIter.hasNext()) {
          Asset asset = (Asset)assetIter.next();
          out.print("<tr>\n");
          printAssetTableRow(asset);
          out.print("</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            // restart table
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllAssets(
              0, numAssets, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No Assets found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    /**
     * displaySearch.
     * <p>
     * Uses JavaScript to set the FORM action, since the user selects
     * the agent _after_ page load and the action must point to the
     * correct Agent's URL.
     */
    private void displaySearch()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Form");
      }
      out.print(
          "<html>\n"+
          "<head>"+
          "<title>Logplan Search</title>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var obj = top.agentFrame.document.agent.name;\n"+
          "  var encAgent = obj.value;\n"+
          "  if (encAgent.charAt(0) == '.') {\n"+
          "    alert(\"Please select an agent name\")\n"+
          "    return false;\n"+
          "  }\n"+
          "  document.myForm.name.value = encAgent;\n"+
          "  var type = document.myForm.formType.selectedIndex\n"+
          "  var uid = trim(document.myForm."+
          ITEM_UID+
          ".value)\n"+
          "  if (uid.length > 0) {\n"+
          "    document.myForm.target=\"itemFrame\"\n"+
          "    if (type == 0) {\n"+
          "      document.myForm."+
          MODE+
          ".value= \""+
          MODE_TASK_DETAILS+
          "\"\n"+
          "    } else if (type == 1) {\n"+
          "      document.myForm."+
          MODE+
        ".value= \""+
        MODE_PLAN_ELEMENT_DETAILS+
        "\"\n"+
        "    } else if (type == 2) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ASSET_DETAILS+
        "\"\n"+
        "    } else {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_XML_HTML_DETAILS+
        "\"\n"+
        "    }\n"+
        "    if (uid.charAt(0) == '/') {\n"+
        "      document.myForm."+
        ITEM_UID+
        ".value = encAgent + uid\n"+
        "    } else if (uid != document.myForm."+
        ITEM_UID+
        ".value) {\n"+
        "      document.myForm."+
        ITEM_UID+
        ".value = uid\n"+
        "    }\n"+
        "  } else {\n"+
        "    document.myForm.target=\"tablesFrame\"\n"+
        "    if (type == 0) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_TASKS_SUMMARY+
        "\"\n"+
        "    } else if (type == 1) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_PLAN_ELEMENTS+
        "\"\n"+
        "    } else if (type == 2) {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_ASSETS+
        "\"\n"+
        "    } else {\n"+
        "      document.myForm."+
        MODE+
        ".value= \""+
        MODE_ALL_UNIQUE_OBJECTS+
        "\"\n"+
        "    }\n"+
        "  }\n"+
        "  document.myForm.action=\"/$\"+encAgent+\"");
      out.print(support.getPath());
      out.print("\"\n"+
          "  return true\n"+
          "}\n"+
          "\n"+
          "// javascript lacks String.trim()?\n"+
          "function trim(val) {\n"+
          "  var len = val.length\n"+
          "  if (len == 0) {\n"+
          "    return \"\"\n"+
          "  }\n"+
          "  var i\n"+
          "  for (i = 0; ((i < len) && (val.charAt(i) == ' ')); i++) {}\n"+
          "  if (i == len) {\n"+
          "    return \"\";\n"+
          "  }\n"+
          "  var j \n"+
          "  for (j = len-1; ((j > i) && (val.charAt(j) == ' ')); j--) {}\n"+
          "  j++\n"+
          "  if ((i == 0) && (j == len)) {\n"+
          "    return val\n"+
          "  }\n"+
          "  var ret = val.substring(i, j)\n"+
        "  return ret\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<noscript>\n"+
        "<b>This page needs Javascript!</b><br>\n"+
        "Consult your browser's help pages..\n"+
        "</noscript>\n"+
        "<form name=\"myForm\" method=\"get\" onSubmit=\"return mySubmit()\">\n"+
        "<input type=\"hidden\" name=\""+
        MODE+
        "\" value=\"fromJavaScript\">\n"+
        "<input type=\"hidden\" name=\""+
        LIMIT+
        "\" value=\"true\">\n"+
        "<select name=\"formType\">\n"+
        "  <option selected value=\"0\">Tasks</option>\n"+
        "  <option value=\"1\">PlanElements</option>\n"+
        "  <option value=\"2\">Assets</option>\n"+
        "  <option value=\"3\">UniqueObjects</option>\n"+
        "</select><br>\n"+
        "UID:<input type=\"text\" name=\""+
        // user should enter an encoded UID
        ITEM_UID+
        "\" size=12><br>\n"+
        "Sort results by UID<input type=\"checkbox\" name=\"sortByUID\" value=\"true\"><br>\n"+
        "Sort tasks by End Time<input type=\"checkbox\" name=\"sortByEndTime\" value=\"true\"><br>\n"+
        "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
        "<p>\n"+
        // link to advanced search
        "<a href=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print(
          "?"+
          MODE+
          "="+
          MODE_ADVANCED_SEARCH_FORM+
          "\" target=\"advSearch\">Advanced search</a>"+
          "</form>\n"+
          "</body>\n"+
          "</html>\n");
    }

    /**
     * displayUniqueObjectDetails.
     */
    private void displayUniqueObjectDetails()
    {
      boolean asHTML;
      boolean isAttached;
      switch (mode) {
        default:
          // error, but treat as "MODE_XML_HTML_DETAILS"
        case MODE_XML_HTML_DETAILS:
          asHTML = true;
          isAttached = false;
          break;
        case MODE_XML_HTML_ATTACHED_DETAILS:
          asHTML = true;
          isAttached = true;
          break;
        case MODE_XML_RAW_DETAILS:
          asHTML = false;
          isAttached = false;
          break;
        case MODE_XML_RAW_ATTACHED_DETAILS:
          asHTML = false;
          isAttached = true;
          break;
      }
      if (DEBUG) {
        System.out.println(
            "\nDisplay UniqueObject "+
            (asHTML ? "HTML" : "Raw")+                     
            (isAttached ? " Attached" : "")+
            " Details");
      }
      // find base object using the specified UID
      UniqueObject baseObj = 
        findUniqueObjectWithUID(itemUID);
      // get the attached object
      Object attachedObj;
      if (isAttached) {
        // examine baseObj to find attached xml
        // 
        // currently only a few cases are supported:
        //   Asset itself
        //   Task's "getDirectObject()"
        //   AssetTransfer's "getAsset()"
        if (baseObj instanceof Asset) {
          // same as above "MODE_XML_[HTML|RAW]_DETAILS"
          attachedObj = baseObj;
        } else if (baseObj instanceof Task) {
          attachedObj = ((Task)baseObj).getDirectObject();
        } else if (baseObj instanceof AssetTransfer) {
          attachedObj = ((AssetTransfer)baseObj).getAsset();
        } else {
          // error
          attachedObj = null;
        }
      } else {
        // the base itself
        attachedObj = baseObj;
      }
      Object xo = attachedObj;
      if (asHTML) {
        // print as HTML
        out.print("<html>\n<head>\n<title>");
        out.print(itemUID);
        out.print(
            " View</title>"+
            "</head>\n<body bgcolor=\"#F0F0F0\">\n<b>");
        // link to agent
        printLinkToTasksSummary();
        out.print(
            "</b><br>\n"+
            "UniqueObject<br>");
        if (xo != null) {
          // link to non-html view of object
          out.print("<p>");
          printLinkToXML(xo, false);
          if (ENABLE_STACKS) {
            out.print(
                "<br><p>"+
                "<nowrap>Publisher:&nbsp;");
            UID uid = UID.toUID(itemUID);
            printLinkToStack(uid, support.getInfo(uid));
            out.print("</nowrap>");
          }
          out.print("<br><hr><br><pre>\n");
          // print HTML-wrapped XML
          printXMLDetails(xo, true);
          out.print("\n</pre><br><hr><br>\n");
        } else {
          out.print("<p><font size=small color=mediumblue>");
          if (itemUID == null) {
            out.print("No UniqueObject selected.");
          } else if (baseObj == null) {
            out.print("No UniqueObject matching \"");
            out.print(itemUID);
            out.print("\" found.");
          } else if (attachedObj == null) {
            out.print("UniqueObject with UID \"");
            out.print(itemUID);
            out.print("\" of type ");
            out.print(baseObj.getClass().getName());
            out.print(" has null attached Object.");
          } else {
            out.print("UniqueObject with UID \"");
            out.print(itemUID);
            out.print("\" of type ");
            out.print(baseObj.getClass().getName());
            out.print(" has no XML attached Object: ");
            out.print(attachedObj.getClass().getName());
            out.print(" (internal error?)");
          }
          out.print("</font><p>\n");
        }
        out.print("</body></html>\n");
      } else {
        // print raw XML
        printXMLDetails(xo, false);
      }
      out.flush();
    }

    private static Comparator typeUniqueObjectComparator =
      new Comparator() {
        public int compare(Object o1, Object o2) {
          String s1 = getItemTypeString(o1);
          String s2 = getItemTypeString(o2);
          int diff = s1.compareTo(s2);
          if (diff == 0) {
            Comparable c1 = ((UniqueObject) o1).getUID();
            Comparable c2 = ((UniqueObject) o2).getUID();
            diff = c1.compareTo(c2);
          }
          return diff;
        }
      };
      
    /**
     * displayAllUniqueObjects.
     */
    private void displayAllUniqueObjects()
    {
      if (DEBUG) {
        System.out.println("\nDisplay All UniqueObjects");
      }
      List col = new ArrayList(findAllUniqueObjects());
      Collections.sort(col, typeUniqueObjectComparator);
      int numUniqueObjects = col.size();
      Iterator uoIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched UniqueObjects");
      }
      // begin page
      out.print(
          "<html>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " UniqueObjects"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\">\n"+
          "<center>");
      if (limit && (numUniqueObjects > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numUniqueObjects);
      out.print(
          "</b> UniqueObject");
      if (numUniqueObjects != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      if (limit && (numUniqueObjects > DEFAULT_LIMIT)) {
        out.print("<center>");
        // link to all uniqueObjects.
        printLinkToAllUniqueObjects(
            0, numUniqueObjects, false);
        out.print("</center>\n");
      }
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>UniqueObjects</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</b></font></td>\n"+
          "<td><font color=mediumblue><b>Type</b></font></td>\n"+
          "</tr>\n");
      if (numUniqueObjects > 0) {
        // print table rows
        int rows = 0;
        while (uoIter.hasNext()) {
          UniqueObject uo = (UniqueObject)uoIter.next();
          int itemType = getItemType(uo);
          out.print(
              "<tr>\n"+
              "<td>");
          switch (itemType) {
            case ITEM_TYPE_ALLOCATION:
            case ITEM_TYPE_EXPANSION:
            case ITEM_TYPE_AGGREGATION:
            case ITEM_TYPE_DISPOSITION:
            case ITEM_TYPE_ASSET_TRANSFER:
              printLinkToPlanElement((PlanElement)uo);
              break;
            case ITEM_TYPE_TASK:
              printLinkToLocalTask((Task)uo);
              break;
            case ITEM_TYPE_ASSET:
              // found this asset in local blackboard
              printLinkToLocalAsset((Asset)uo);
              break;
            case ITEM_TYPE_WORKFLOW:
            default:
              // xml for a local UniqueObject
              printLinkToXML(uo, true);
              break;
          }
          out.print(
              "</td>\n"+
              "<td>");
          if (itemType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[itemType]);
          } else {
            out.print("<font color=red>");
            out.print(uo.getClass().getName());
            out.print("</font>");
          }
          out.print(
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
        if (limit && (rows == DEFAULT_LIMIT)) {
          // link to unlimited view
          out.print(
              "<p>"+
              "<center>");
          printLinkToAllUniqueObjects(
              0, numUniqueObjects, false);
          out.print(
              "<br>"+
              "</center>\n");
        }
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No UniqueObjects found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    // keep a Map of ordered (name, value) pairs
    private static PropertyTree TEMPLATE_PREDS = null;
    private static synchronized final PropertyTree getTemplatePreds() { 
      if (TEMPLATE_PREDS == null) {
        String fname = System.getProperty(PREDS_FILENAME_PROPERTY);
        if (fname == null) {
          fname = DEFAULT_PREDS_FILENAME;
        }
        try {
          InputStream in = ConfigFinder.getInstance().open(fname);
          TEMPLATE_PREDS = PredTableParser.parse(in);
        } catch (IOException ioe) {
          System.err.println("Unable to open predicate file \""+fname+"\":");
          TEMPLATE_PREDS = new PropertyTree(1);
          TEMPLATE_PREDS.put("Unable to load \\\\"+fname+"\\\"", "");
        }
      }
      return TEMPLATE_PREDS;
    }

    private void displayAdvancedSearchForm()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Advanced Search Form");
      }
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var encAgent = document.myForm.formAgent.value\n"+
          "  document.myForm.action=\"/$\"+encAgent+\"");
      out.print(support.getPath());
      out.print(
          "\"\n"+
          "  return true\n"+
          "}\n"+
          "\n"+
          "function setPred() {\n"+
          "  var i = document.myForm.formPred.selectedIndex\n"+
          "  var s\n"+
          "  switch(i) {\n"+
          "    default: alert(\"unknown (\"+i+\")\"); break\n");
      PropertyTree templatePreds = getTemplatePreds();
      int nTemplatePreds = templatePreds.size();
      for (int i = 0; i < nTemplatePreds; i++) {
        out.print("case ");
        out.print(i);
        out.print(": s=\"");
        out.print(templatePreds.getValue(i));
        out.print("\"; break\n");
      }
      out.print(
          "  }\n"+
          "  document.myForm.pred.value=s\n"+
          "}\n"+
          "// -->\n"+
          "</script>\n"+
          "<head>\n"+
          "<title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Advanced Search Form"+
          "</title>\n"+
          "</head>\n"+
          "<body bgcolor=\"#F0F0F0\" "+
          " onload=\"setPred()\">\n"+
          "<font size=+1><b>Advanced Search</b></font><p>"+
          // should add link here for usage!!!
          "<noscript>\n"+
          "<b>This page needs Javascript!</b><br>\n"+
          "Consult your browser's help pages..\n"+
          "<p><p><p>\n"+
          "</noscript>\n"+
          "<form name=\"myForm\" method=\"get\" "+
          "target=\"predResults\" onSubmit=\"return mySubmit()\">\n"+
          "Search agent <input type=\"text\" name=\"formAgent\" value=\"");
      out.print(support.getEncodedAgentName());
      out.print("\"><br>\n");
      if (nTemplatePreds > 0) {
        out.print(
            "<b>Find all </b>"+
            "<select name=\"formPred\" "+
            "onchange=\"setPred()\">\n");
        for (int i = 0; i < nTemplatePreds; i++) {
          out.print("<option>");
          out.print(templatePreds.getKey(i));
          out.print("</option>\n");
        }
        out.print(
            "</select><br>\n");
      }
      out.print(
          "<input type=\"checkbox\" name=\""+
          LIMIT+
          "\" value=\"true\" checked>"+
          "limit to "+
          DEFAULT_LIMIT+
          " matches<br>\n"+
          "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
          "<p><hr>\n"+
          "<input type=\"checkbox\" name=\""+
          PREDICATE_DEBUG+
          "\" value=\"true\">View parsed predicate<br>\n"+
          "<textarea name=\""+
          PREDICATE+
          "\" rows=15 cols=70>\n"+
          "</textarea><br>\n"+
        "<input type=\"hidden\" name=\""+
        MODE+
        "\" value=\""+
        MODE_ADVANCED_SEARCH_RESULTS+
        "\">\n"+
        "<br><hr>\n"+
        "</form>\n"+
        "<i><b>Documentation</b> is available in the \"contract\" "+
        "guide and javadocs, as "+
        "/src/org/cougaar/lib/contract/lang/index.html"+
        "</i>"+
        "</body>"+
        "</html>\n");
      out.flush();
    }

    private void displayAdvancedSearchResults()
    {
      if (DEBUG) {
        System.out.println("\nDisplay Advanced Search Results");
      }

      String inputPred = pred;

      out.print("<html><head><title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Advanced Search Results</title><head>\n"+
          "<body bgcolor=\"#F0F0F0\"><p>\n"+
          "Search <b>");
      out.print(support.getEncodedAgentName());
      out.print("</b> using Lisp-style predicate: <br><pre>\n");
      out.print(inputPred);
      out.print("</pre><p>\n<hr><br>\n");

      // parse the input to create a unary predicate
      UnaryPredicate parsedPred;
      try {
        parsedPred = UnaryPredicateParser.parse(inputPred);
      } catch (Exception parseE) {
        // display compile error
        out.print(
            "<font color=red size=+1>Parsing failure:</font>"+
            "<p><pre>");
        out.print(parseE.getMessage());
        out.print("</pre></body></html>");
        out.flush();
        return;
      }

      if (parsedPred == null) {
        // empty string?
        out.print(
            "<font color=red size=+1>Given empty string?</font>"+
            "</body></html>");
        out.flush();
        return;
      }

      if (predDebug) {
        // this is useful in general, but clutters the screen...
        out.print("Parsed as:<pre>\n");
        out.print(parsedPred);
        out.print("</pre><br><hr><br>\n");
      }

      Collection col = searchUsingPredicate(parsedPred);
      int numObjects = col.size();
      Iterator oIter = col.iterator();
      if (DEBUG) {
        System.out.println("Fetched Matching Objects["+numObjects+"]");
      }
      out.print(
          "<b>Note:</b> "+
          "links below will appear in the \"");
      out.print(support.getPath());
      out.print(
          "\" lower-left \"details\" "+
          "frame<p>"+
          "<center>");
      if (limit && (numObjects > DEFAULT_LIMIT)) {
        out.print("Showing first <b>");
        out.print(DEFAULT_LIMIT);
        out.print("</b> of ");
      }
      out.print("<b>");
      out.print(numObjects);
      out.print("</b> Object");
      if (numObjects != 1) {
        out.print("s");
      }
      out.print(" at ");
      out.print(support.getEncodedAgentName());
      out.print("</center>\n");
      out.print(
          "\n<table align=center border=1 cellpadding=1\n"+
          " cellspacing=1 width=75%\n"+
          " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
          "<tr>\n"+
          "<td colspan=2>"+
          "<font size=+1 color=mediumblue><b>Matching Objects</b></font>"+
          "</td>\n"+
          "</tr>\n"+
          "<tr>\n"+
          "<td><font color=mediumblue><b>UID</font></b></td>\n"+
          "<td><font color=mediumblue><b>Type</font></b></td>\n"+
          "</tr>\n");
      if (numObjects > 0) {
        // print table rows
        int rows = 0;
        while (oIter.hasNext()) {
          Object o = oIter.next();
          int itemType = getItemType(o);
          out.print(
              "<tr>\n"+
              "<td>");
          switch (itemType) {
            case ITEM_TYPE_ALLOCATION:
            case ITEM_TYPE_EXPANSION:
            case ITEM_TYPE_AGGREGATION:
            case ITEM_TYPE_DISPOSITION:
            case ITEM_TYPE_ASSET_TRANSFER:
              printLinkToPlanElement((PlanElement)o);
              break;
            case ITEM_TYPE_TASK:
              printLinkToLocalTask((Task)o);
              break;
            case ITEM_TYPE_ASSET:
              // found this asset in local blackboard
              printLinkToLocalAsset((Asset)o);
              break;
            case ITEM_TYPE_WORKFLOW:
            default:
              // xml for a local UniqueObject
              printLinkToXML(o, true);
              break;
          }
          out.print(
              "</td>\n"+
              "<td>");
          if (itemType != ITEM_TYPE_OTHER) {
            out.print(ITEM_TYPE_NAMES[itemType]);
          } else {
            out.print("<font color=red>");
            out.print(o.getClass().getName());
            out.print("</font>");
          }
          out.print(
              "</td>\n"+
              "</tr>\n");
          if ((++rows % DEFAULT_LIMIT) == 0) {
            if (limit) {
              // limit to DEFAULT_LIMIT
              break;
            }
            // restart table
            out.print("</table>\n");
            out.flush();
            out.print(
                "<table align=center border=1 cellpadding=1\n"+
                " cellspacing=1 width=75%\n"+
                " bordercolordark=#660000 bordercolorlight=#cc9966>\n");
          }
        }
        // end table
        out.print("</table>\n");
      } else {
        out.print(
            "</table>"+
            "<center>"+
            "<font color=mediumblue>\n"+
            "No matching Objects found in ");
        out.print(support.getEncodedAgentName());
        out.print(
            "\n...try again"+
            "</font>"+
            "</center>\n");
      }
      // end page
      out.print(
          "</body>"+
          "</html>\n");
      out.flush();
    }

    private void displayStackDetails() {
      out.print("<html><head><title>");
      out.print(support.getEncodedAgentName());
      out.print(
          " Stack Details</title></head>\n"+
          "<body bgcolor=\"#F0F0F0\"><p>\n"+
          "<b>");
      // link to agent
      printLinkToTasksSummary();
      out.print(
          "</b><br>\n");
      if (itemUID == null) {
        out.print("<font color=red>Missing UID</font>");
      } else {
        UniqueObject uo = findUniqueObjectWithUID(itemUID);
        out.print(
            "Publisher info for "+
            getItemTypeString(uo)+
            " "+
            itemUID+
            "<p>");
        UID uid = UID.toUID(itemUID);
        PublisherInfo info = support.getInfo(uid);
        if (info == null) {
          out.print(
            "<font color=red>Unable to find info for "+
            uid.toString()+
            "</font>");
        } else {
          // print publisher
          String publisher = info.getPublisher();
          out.print(
              "<font color=mediumblue><nowrap>Publisher:&nbsp;");
          if (publisher == null) {
            out.print("<font color=red>Unknown</font>");
          } else {
            out.print(encodeHTML(publisher, true));
          }
          out.print(
              "</nowrap></font>"+
              "<p>\n");
          // print add
          printStack(info.getAddStack(), uid, "publishAdd");
          // print changes
          Set change_stacks = info.getChangeStacks();
          int n = (change_stacks == null ? 0 : change_stacks.size());
          if (n > 0) {
            Iterator iter = change_stacks.iterator();
            for (int i = 0; i < n; i++) {
              StackElements se = (StackElements) iter.next();
              String id = "publishChange["+i+" / "+n+"]";
              printStack(se, uid, id);
            }
            if (n > 1) {
              out.print(
                  "<p>"+
                  "<b>Note</b>:"+
                  " Only unique publishChange stacks are shown, and they"+
                  " are listed in the order that they were <i>first</i>"+
                  " observed.");
            }
          }
        }
      }
      out.print(
          "</body>\n"+
          "</html>\n");
      out.flush();
    }

    /** END DISPLAY ROUTINES **/

    /** BEGIN PRINT ROUTINES **/

    /**
     * printTaskDetails.
     *
     * Includes support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     *
     */
    private void printTaskDetails(Task task)
    {
      out.print(
          "<ul>\n"+
          "<li>"+
          "<font size=small color=mediumblue>UID= ");
      // show uid
      UID tu;
      String tuid;
      if (((tu = task.getUID()) != null) &&
          ((tuid = tu.toString()) != null)) {
        out.print(tuid);
      } else {
        out.print("</font><font color=red>missing</font>");
      }
      out.print(
          "</font>"+
          "</li>\n");
      printListItemForStack(tu);
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>Verb= ");
      // show verb
      Verb verb = task.getVerb();
      if (verb != null) {
        out.print(verb.toString());
      } else {
        out.print("</font><font color=red>missing");
      }
      out.print(
          "</font>"+
          "</li>\n"+
          "<li>"+
          "<font size=small color=mediumblue>"+
          "DirectObject= ");
      // link to Task's direct object
      printLinkToTaskDirectObject(task);
      out.print(
          "</font>"+
          "</li>\n"+
          "<li>"+
          "<font size=small color=mediumblue>"+
          "PlanElement= ");
      // link to plan element
      PlanElement pe = task.getPlanElement();
      printLinkToPlanElement(pe);
      out.print(
          " (");
      int peType = getItemType(pe);
      if (peType != ITEM_TYPE_OTHER) {
        out.print(ITEM_TYPE_NAMES[peType]);
      } else {
        out.print("<font color=red>");
        if (pe != null) {
          out.print(pe.getClass().getName());
        } else {
          out.print("null");
        }
        out.print("</font>");
      }
      out.print(
          ")"+
          "</font>"+
          "</li>");
      // show parent task(s) by UID
      if (task instanceof MPTask) {
        out.print(
            "<li>\n"+
            "<font size=small color=mediumblue>"+
            "ParentTasks<br>\n"+
            "<ol>\n");
        /********************************************************
         * Only want UIDs, so easy fix when getParentTasks is   *
         * replaced with getParentTaskUIDs.                     *
         ********************************************************/
        Enumeration parentsEn = ((MPTask)task).getParentTasks();
        while (parentsEn.hasMoreElements()) {
          Task pt = (Task)parentsEn.nextElement();
          out.print("<li>");
          // parents of an MPTask are always local
          printLinkToLocalTask(pt);
          out.print("</li>\n");
        }
        out.print(
            "</ol>\n"+
            "</font>\n"+
            "</li>\n");
      } else {
        out.print(
            "<li>\n"+
            "<font size=small color=mediumblue>"+
            "ParentTask= \n");
        printLinkToParentTask(task);
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show preferences
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Preferences"+
          "</font>"+
          "<ol>\n");
      Enumeration enpref = task.getPreferences();
      while (enpref.hasMoreElements()) {
        Preference pref = (Preference)enpref.nextElement();
        int type = pref.getAspectType();
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        out.print(AspectValue.aspectTypeToString(type));
        out.print("= ");
        ScoringFunction sf = pref.getScoringFunction();
        AspectScorePoint best = sf.getBest();
        double bestVal = best.getValue();
        String bestString;
        if ((type == AspectType.START_TIME) || 
            (type == AspectType.END_TIME)) {
          if ((type == AspectType.END_TIME) &&
              (sf instanceof ScoringFunction.VScoringFunction)) {
            bestString = 
              "<br>" + 
              "Earliest " + getTimeString(getEarlyDate (sf)) + 
              "<br>" + 
              "Best " + getTimeString((long)bestVal) +
              "<br>" + 
              "Latest " + getTimeString(getLateDate (sf));
          } else {
            bestString = getTimeString((long)bestVal);
          }
        } else {
          bestString = Double.toString(bestVal);
        }
        out.print(bestString);
        out.print(
            "</li>"+
            "</font>\n");
      }
      out.print(
          "</ol>"+
          "</li>\n"+
          "<li>\n"+
          "<font size=small color=mediumblue>"+
          "PrepositionalPhrases<br>\n"+
          "<ol>\n");
      // show prepositional phrases
      Enumeration enprep = task.getPrepositionalPhrases();
      while (enprep.hasMoreElements()) {
        PrepositionalPhrase pp = 
          (PrepositionalPhrase)enprep.nextElement();
        out.print("<li>");
        if (pp != null) {
          String prep = pp.getPreposition();
          out.print("<i>");
          out.print(prep);
          out.print(" </i>");
          Object indObj = pp.getIndirectObject();
          if (!(indObj instanceof Schedule)) {
            // typical case
            printObject(indObj);
          } else {
            // display full schedule information
            Schedule sc = (Schedule)indObj;
            out.print(
                "Schedule:<ul>\n"+
                "<li>Type: ");
            out.print(sc.getScheduleType());
            if (sc.isEmpty()) {
              out.print("</li>\n<li><font color=red>empty</font>");
            } else {
              out.print("</li>\n<li>StartTime= ");
              out.print(getTimeString(sc.getStartTime()));
              out.print("</li>\n<li>EndTime= ");
              out.print(getTimeString(sc.getEndTime()));
              out.print("</li>\n");
              out.print("<li>Elements:");
              out.print("\n<ol>\n");
              Iterator iterator = new ArrayList(sc).iterator();
              while (iterator.hasNext()) {
                ScheduleElement se = (ScheduleElement)iterator.next();
                out.print(
                    "<li>StartTime= ");
                out.print(getTimeString(se.getStartTime()));
                out.print("<br>EndTime= ");
                out.print(getTimeString(se.getEndTime()));
                if (se instanceof LocationRangeScheduleElement) {
                  LocationRangeScheduleElement locSE = 
                    (LocationRangeScheduleElement)se;
                  out.print("<br>StartLocation= ");
                  out.print(locSE.getStartLocation());
                  out.print("<br>EndLocation= ");
                  out.print(locSE.getEndLocation());
                  if (locSE instanceof ItineraryElement) {
                    out.print("<br>Verb= ");
                    out.print(((ItineraryElement)locSE).getRole());
                  }
                } else if (se instanceof LocationScheduleElement) {
                  out.print("<br>Location= ");
                  out.print(((LocationScheduleElement)se).getLocation());
                }
                out.print("</li>\n");
              } 
              out.print("</ol>\n");
            }
            out.print("</li>\n</ul>\n");
          }
        } else {
          out.print("<font color=red>null</font>");
        }
        out.print("</li>");
      }
      out.print(
          "</font>"+
          "</ol>\n"+
          "</li>\n"+
          "</ul>\n");
      // link to XML view
      out.print("<font size=small color=mediumblue>");
      // this task is local
      printLinkToXML(task, true);
      out.print("</font>");
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static long getEarlyDate(ScoringFunction vsf) {
      Enumeration validRanges = getValidEndDateRanges(vsf);
      while (validRanges.hasMoreElements()) {
        AspectScoreRange range = 
          (AspectScoreRange)validRanges.nextElement();
        return 
          ((AspectScorePoint)range.getRangeStartPoint()
           ).getAspectValue().longValue();
      }
      // should be TimeSpan.MIN_VALUE!
      return 0;
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static long getLateDate(ScoringFunction vsf) {
      Enumeration validRanges = getValidEndDateRanges(vsf);
      while (validRanges.hasMoreElements()) {
        AspectScoreRange range = 
          (AspectScoreRange)validRanges.nextElement();
        if (!validRanges.hasMoreElements())
          return ((AspectScorePoint)range.getRangeEndPoint()
              ).getAspectValue().longValue();
      }
      return TimeSpan.MAX_VALUE;
    }

    /* Needed for support of printing early-best-latest END_TIMEs */
    private static Calendar cal = java.util.Calendar.getInstance();

    /* Needed for support of printing early-best-latest END_TIMEs */
    private static Date endOfRange;
    static {
      cal.set(2200, 0, 0, 0, 0, 0);
      cal.set(Calendar.MILLISECOND, 0);
      endOfRange = (Date) cal.getTime();
    }

    /**
     * Part of support for printing early-best-latest dates
     * for END_TIMEs with VScoringFunctions.
     */
    private static Enumeration getValidEndDateRanges(ScoringFunction sf) {
      Enumeration validRanges = 
        sf.getValidRanges(
            TimeAspectValue.create(AspectType.END_TIME, 0l),
            TimeAspectValue.create(AspectType.END_TIME, endOfRange));
      return validRanges;
    }

    /**
     * printPlanElementDetails.
     *
     * PlanElements are always in the blackboard and have UIDs, so we
     * don't need a "baseObj" (e.g. the Task that this PlanElement
     * is attached to).
     */
    private void printPlanElementDetails(PlanElement pe)
    {
      int peType = getItemType(pe);
      // show type
      if (peType != ITEM_TYPE_OTHER) {
        out.print(ITEM_TYPE_NAMES[peType]);
      } else {
        out.print(
            "<font color=red>");
        out.print(pe.getClass().getName());
        out.print(
            "</font>\n");
      }
      out.print("<ul>\n");
      // show UID
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "UID= ");
      UID peu = pe.getUID();
      out.print((peu != null) ? peu.toString() : "null");
      out.print(
          "</font>"+
          "</li>\n");
      printListItemForStack(peu);
      // show task
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Task= ");
      printLinkToLocalTask(pe.getTask());
      out.print(
          "</font>"+
          "</li>\n");
      // show plan
      Plan plan = pe.getPlan();
      if (plan != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Plan= ");
        out.print(plan.getPlanName());
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show allocation results
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Allocation Results</font>\n"+
          "<ul>\n");
      AllocationResult ar;
      if ((ar = pe.getEstimatedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Estimated</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getReportedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Reported</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getReceivedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Received</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      if ((ar = pe.getObservedResult()) != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Observed</font>");
        printAllocationResultDetails(ar);
        out.print(
            "</li>\n");
      }
      out.print(
          "</ul>"+
          "</li>\n");
      // show PE subclass information
      switch (peType) {
        case ITEM_TYPE_ALLOCATION:
          printAllocationDetails((Allocation)pe);
          break;
        case ITEM_TYPE_EXPANSION:
          printExpansionDetails((Expansion)pe);
          break;
        case ITEM_TYPE_AGGREGATION:
          printAggregationDetails((Aggregation)pe);
          break;
        case ITEM_TYPE_DISPOSITION:
          printDispositionDetails((Disposition)pe);
          break;
        case ITEM_TYPE_ASSET_TRANSFER:
          printAssetTransferDetails((AssetTransfer)pe);
          break;
        default: // other
          out.print(
              "<li>"+
              "<font color=red>"+
              "No details for class ");
          out.print(pe.getClass().getName());
          out.print("</font></li>");
          break;
      }
      out.print("</ul>\n");
      // link to XML view
      out.print("<font size=small color=mediumblue>");
      // planElements are always local
      printLinkToXML(pe, true);
      out.print("</font>");
    }

    /**
     * printAllocationResultDetails.
     */
    private void printAllocationResultDetails(AllocationResult ar)
    {
      out.print(
          "<ul>\n"+
          "<font size=small color=mediumblue>"+
          "<li>"+
          "isSuccess= ");
      // show isSuccess
      out.print(ar.isSuccess());
      out.print(
          "</li>"+
          "</font>\n"+
          "<font size=small color=mediumblue>"+
          "<li>"+
          "Confidence= ");
      // show confidence rating
      out.print(ar.getConfidenceRating());
      out.print(
          "</li>"+
          "</font>\n");
      // for all (type, result) pairs
      AspectValue[] ava = ar.getAspectValueResults();
      for (int i = 0; i < ava.length; i++) {
        AspectValue avi = ava[i];
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        // show type
        out.print(
            AspectValue.aspectTypeToString(
              avi.getType()));
        out.print("= ");
        // show value
        if (avi instanceof TimeAspectValue) {
          // print the date in our format
          long time = ((TimeAspectValue) avi).timeValue();
          out.print(getTimeString(time));
        } else {
          out.print(avi);
        }
        out.print(
            "</li>"+
            "</font>\n");
      }
      // show phased details
      if (ar.isPhased()) {
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>"+
            "isPhased= true"+
            "</li>"+
            "</font>\n");
        // user likely not interested in phased results
      }
      out.print(
          "</ul>\n");
    }

    /**
     * printAllocationDetails.
     */
    private void printAllocationDetails(Allocation ac)
    {
      // show asset
      Asset asset = ac.getAsset();
      if (asset != null) {
        // link to allocated asset
        ClusterPG clusterPG = asset.getClusterPG();
        MessageAddress agentID;
        String remoteAgentID =
          ((((clusterPG = asset.getClusterPG()) != null) &&
            ((agentID = clusterPG.getMessageAddress()) != null)) ?
           agentID.toString() :
           null);
        boolean isRemoteAgent = (remoteAgentID != null);
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>");
        out.print(isRemoteAgent ? "Agent" : "Asset");
        out.print("= ");
        // allocations are always to an asset in the local blackboard
        printLinkToLocalAsset(asset);
        out.print(
            "</font>"+
            "</li>\n");
        if (isRemoteAgent) {
          // link to task in other agent
          String encRemoteAgentID = 
            support.encodeAgentName(remoteAgentID);
          UID allocTaskUID = ((AllocationforCollections)ac).getAllocationTaskUID();
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AllocTask= ");
          printLinkToTask(
              allocTaskUID, 
              encRemoteAgentID);
          out.print(
              "</font>"+
              "</li>\n");
        }
      } else {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Asset= </font>"+
            "<font color=red>null</font>"+
            "</li>\n");
      }
    }

    /**
     * printExpansionDetails.
     */
    private void printExpansionDetails(Expansion ex)
    {
      // link to child tasks
      out.print(
          "<li>"+
          "<font size=small color=black>"+
          "<i>Child Tasks</i>"+
          "</font>"+
          "<ol>\n");
      Enumeration en = ex.getWorkflow().getTasks();
      while (en.hasMoreElements()) {
        Task tsk = (Task)en.nextElement();
        out.print(
            "<font size=small color=mediumblue>"+
            "<li>");
        // expanded task is always local
        printLinkToLocalTask(tsk);
        out.print(
            "</li>"+
            "</font>");
      }
      out.print(
          "</ol>"+
          "</li>\n");
    }

    /**
     * printAggregationDetails.
     */
    private void printAggregationDetails(Aggregation agg)
    {
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "MPTask= ");
      Composition comp = agg.getComposition();
      if (comp != null) {
        // link to composed mp task
        Task compTask = comp.getCombinedTask();
        // composed task is always local
        printLinkToLocalTask(compTask);
      } else {
        out.print("<font color=red>null Composition</font>");
      }
      out.print(
          "</font>\n"+
          "</li>\n");
    }

    /**
     * printDispositionDetails.
     */
    private void printDispositionDetails(Disposition d)
    {
      // nothing to say?
      out.print(
          "<font size=small color=mediumblue>"+
          "Success= ");
      out.print(d.isSuccess());
      out.print("</font>\n");
    }

    /**
     * printAssetTransferDetails.
     */
    private void printAssetTransferDetails(AssetTransfer atrans)
    {
      // show attached asset
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Asset= ");
      printLinkToAssetTransferAsset(atrans);
      out.print(
          "</font>"+
          "</li>\n");
      // show role
      Role role = atrans.getRole();
      if (role != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Role= ");
        out.print(role.getName());
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show assignor
      MessageAddress assignor = atrans.getAssignor();
      if (assignor != null) {
        String name = assignor.toString();
        String encName = 
          ((name != null) ?
           (support.encodeAgentName(name)) :
           (null));
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Assignor= ");
        printLinkToTasksSummary(encName);
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show assignee
      Asset assignee = atrans.getAssignee();
      if (assignee != null) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Assignee= ");
        // assignee asset is always in the local blackboard
        printLinkToLocalAsset(assignee);
        out.print(
            "</font>"+
            "</li>\n");
      }
    }

    /**
     * printAssetDetails.
     */
    private void printAssetDetails(
        UniqueObject baseObj, Asset asset)
    {
      if (asset instanceof AssetGroup) {
        // recursive for AssetGroups!
        List assets = ((AssetGroup)asset).getAssets();
        int nAssets = ((assets != null) ? assets.size() : 0);
        out.print("AssetGroup[");
        out.print(nAssets);
        out.print("]:\n<ol>\n");
        for (int i = 0; i < nAssets; i++) {
          Asset as = (Asset)assets.get(i);
          out.print("<li>\n");
          if (as != null) {
            // recurse!
            //
            // unable to show XML for elements, so pass null baseObj
            printAssetDetails(null, as);
          } else {
            out.print("<font color=red>null</font>");
          }
          out.print("\n</li>\n");
        }
        out.print("</ol>\n");
        if (baseObj != null) {
          // link to HTML-encoded XML view
          out.print("<font size=small color=mediumblue>");
          printLinkToAttachedXML(
              baseObj,
              asset,
              true);
        }
        return;
      }
      // if asset is an aggregate, info_asset is the
      // aggregate's asset which contains Type and Item info.
      Asset info_asset = asset;
      int quantity = 1;
      boolean isAggregateAsset = (asset instanceof AggregateAsset);
      if (isAggregateAsset) {
        do {
          AggregateAsset agg = (AggregateAsset)info_asset;
          quantity *= (int)agg.getQuantity();
          info_asset = agg.getAsset();
        } while (info_asset instanceof AggregateAsset);
        if (info_asset == null) {
          // bad!  should throw exception, but I doubt this will
          // ever happen...
          info_asset = asset;
        }
      }
      out.print("<ul>\n");
      if (isAggregateAsset) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "Quantity= ");
        // show quantity
        out.print(quantity);
        out.print(
            "</font>"+
            "</li>\n");
      } else {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "UID= ");
        // show UID
        UID u = asset.getUID();
        String foundUID = ((u != null) ? u.toString() : "null");
        out.print(foundUID);
        out.print(
            "</font>"+
            "</li>\n");
        printListItemForStack(asset.getUID());
      }
      // show class
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Class= ");
      out.print(info_asset.getClass().getName());
      out.print(
          "</font>"+
          "</li>\n");
      // show type id info
      TypeIdentificationPG tipg = info_asset.getTypeIdentificationPG();
      if (tipg != null) {
        String tiid = tipg.getTypeIdentification();
        if (tiid != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "TypeID= ");
          out.print(tiid);
          out.print(
              "</font>"+
              "</li>");
        }
        String tin = tipg.getNomenclature();
        if (tin != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "TypeNomenclature= ");
          out.print(tin);
          out.print(
              "</font>"+
              "</li>");
        }
        String tiati = tipg.getAlternateTypeIdentification();
        if (tiati != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AlternateTypeID= ");
          out.print(tiati);
          out.print(
              "</font>"+
              "</li>");
        }
      } else {
        out.print(
            "<li>"+
            "<font color=red>"+
            "TypeID missing"+
            "</font>"+
            "</li>\n");
      }
      // show item id
      ItemIdentificationPG iipg = info_asset.getItemIdentificationPG();
      if (iipg != null) {
        String iiid = iipg.getItemIdentification();
        if (iiid != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "ItemID= ");
          out.print(iiid);
          out.print(
              "</font>"+
              "</li>");
        }
        String iin = iipg.getNomenclature();
        if (iin != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "ItemNomenclature= ");
          out.print(iin);
          out.print(
              "</font>"+
              "</li>");
        }
        String iiati = iipg.getAlternateItemIdentification();
        if (iiati != null) {
          out.print(
              "<li>"+
              "<font size=small color=mediumblue>"+
              "AlternateItemID= ");
          out.print(iiati);
          out.print(
              "</font>"+
              "</li>");
        }
      } else {
        out.print(
            "<li>"+
            "<font color=red>"+
            "ItemID missing"+
            "</font>"+
            "</li>\n");
      }
      // show role schedule
      RoleSchedule rs;
      Schedule sc;
      if (((rs = asset.getRoleSchedule()) != null) &&
          ((sc = rs.getAvailableSchedule()) != null) &&
          !sc.isEmpty() ) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "RoleSchedule<br>\n"+
            "Start= ");
        out.print(getTimeString(sc.getStartTime()));
        out.print("<br>End= ");
        out.print(getTimeString(sc.getEndTime()));
        out.print("<br>\n");
        Enumeration rsEn = rs.getRoleScheduleElements();
        if (rsEn.hasMoreElements()) {
          out.print(
              "RoleScheduleElements<br>\n"+
              "<ol>\n");
          do {
            PlanElement pe = (PlanElement)rsEn.nextElement();
            out.print("<li>");
            // planElements are always local
            printLinkToPlanElement(pe);
            out.print("</li>\n");
          } while (rsEn.hasMoreElements());
          out.print("</ol>\n");
        } else {
          out.print("RoleScheduleElements: none<br>\n");
        }
        Iterator iterator = new ArrayList(sc).iterator();
        if (iterator.hasNext()) {
          out.print(
              "AvailableScheduleElements<br>\n"+
              "<ol>\n");
          while (iterator.hasNext()) {
            ScheduleElement se = (ScheduleElement)iterator.next();
            out.print(
                "<li>Start= ");
            out.print(getTimeString(se.getStartTime()));
            out.print("<br>End= ");
            out.print(getTimeString(se.getEndTime()));
            out.print("</li>\n");
          } 
          out.print("</ol>\n");
        } else {
          out.print("AvailableScheduleElements: none<br>\n");
        }
        out.print(
            "</font>"+
            "</li>\n");
      }
      // show location schedule
      LocationSchedulePG locSchedPG;
      Schedule locSched;
      if (((locSchedPG = asset.getLocationSchedulePG()) != null) &&
          ((locSched = locSchedPG.getSchedule()) != null) &&
          (!(locSched.isEmpty()))) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "LocationSchedule<br>\n"+
            "Start= ");
        out.print(getTimeString(locSched.getStartTime()));
        out.print("<br>End= ");
        out.print(getTimeString(locSched.getEndTime()));
        out.print("<br>\n");
        Enumeration locSchedEn = locSched.getAllScheduleElements();
        if (locSchedEn.hasMoreElements()) {
          out.print(
              "LocationScheduleElements<br>\n"+
              "<ol>\n");
          do {
            ScheduleElement se = (ScheduleElement)locSchedEn.nextElement();
            out.print(
                "<li>Start= ");
            out.print(getTimeString(se.getStartTime()));
            out.print("<br>End= ");
            out.print(getTimeString(se.getEndTime()));
            if (se instanceof LocationScheduleElement) {
              LocationScheduleElement lse = (LocationScheduleElement)se;
              Location loc = lse.getLocation();
              if (loc != null) {
                out.print("<br>Location= \"");
                out.print(loc);
                out.print("\"");
              }
            }
            out.print("</li>\n");
          } while (locSchedEn.hasMoreElements());
          out.print("</ol>\n");
        } else {
          out.print("LocationScheduleElements: none<br>\n");
        }
        out.print(
            "</font>"+
            "</li>\n");
      }
      // PGs?
      out.print("</ul>");
      if (baseObj != null) {
        // link to HTML-encoded XML view
        out.print("<font size=small color=mediumblue>");
        printLinkToAttachedXML(
            baseObj,
            asset,
            true);
        out.print("</font>");
      } else {
        // likely recursed on an AssetGroup, and the top-level group
        //   had a "View XML" link.
      }
    }

    /**
     * printAssetTableRow.
     *
     * Asset that is in the local blackboard and has a UID.  Treat this
     * as an Asset attached to itself.
     */
    private void printAssetTableRow(Asset asset)
    {
      printAttachedAssetTableRow(
          asset,
          asset,
          MODE_ASSET_DETAILS);
    }

    /**
     * printTaskDirectObjectTableRow.
     */
    private void printTaskDirectObjectTableRow(Task task)
    {
      printAttachedAssetTableRow(
          task,
          ((task != null) ? task.getDirectObject() : null),
          MODE_TASK_DIRECT_OBJECT_DETAILS);
    }

    /**
     * printAttachedAssetTableRow.
     * <p>
     * Print asset information in three table columns:<br>
     * <ol>
     *   <li>UID</li>
     *   <li>TypeID</li>
     *   <li>ItemID</li>
     *   <li>Quantity</li>
     * </ol>
     * Be sure to have a corresponding table!
     *
     * @see #printTaskDirectObjectTableRow
     */
    private void printAttachedAssetTableRow(
        UniqueObject baseObj, Asset asset, int baseMode)
    {
      if ((baseObj == null) ||
          (asset == null)) {
        out.print(
            "<td colspan=4>"+
            "<font color=red>null</font>"+
            "</td>\n");
      } else if (asset instanceof AssetGroup) {
        // link to asset group
        //   "UID" of the baseObj, and a link using that UID
        //   "TypeID" is a bold "AssetGroup"
        //   "ItemID" is "N/A"
        //   "Quantity" is the number of items in the group
        out.print("<td>");
        printLinkToAttachedAsset(baseObj, asset, baseMode);
        out.print(
            "</td>\n"+
            "<td>"+
            "<b>AssetGroup</b>"+
            "</td>\n"+
            "<td>"+
            "N/A"+
            "</td>\n"+
            "<td align=right>");
        List assets = ((AssetGroup)asset).getAssets();
        int nAssets = ((assets != null) ? assets.size() : 0);
        out.print(nAssets);
        out.print(
            "</td>\n");
      } else {
        // if asset is an aggregate, info_asset is the
        // aggregate's asset which contains Type and Item info.
        Asset info_asset;
        int quantity;
        if (asset instanceof AggregateAsset) {
          info_asset = asset;
          quantity = 1;
          do {
            AggregateAsset agg = (AggregateAsset)info_asset;
            quantity *= (int)agg.getQuantity();
            info_asset = agg.getAsset();
          } while (info_asset instanceof AggregateAsset);
          if (info_asset == null) {
            out.print(
                "<td colspan=4>"+
                "<font color=red>null</font>"+
                "</td>\n");
            return;
          }
        } else {
          info_asset = asset;
          if (asset instanceof AssetGroup) {
            List assets = ((AssetGroup)asset).getAssets();
            quantity = ((assets != null) ? assets.size() : 0);
          } else {
            quantity = 1;
          }
        }
        // link to asset
        out.print("<td>");
        printLinkToAttachedAsset(baseObj, asset, baseMode);
        out.print(
            "</td>\n"+
            "<td>");
        // show type id
        TypeIdentificationPG tipg = info_asset.getTypeIdentificationPG();
        if (tipg != null) {
          out.print(
              tipg.getTypeIdentification());
        } else {
          out.print("<font color=red>missing typeID</font>");
        }
        out.print(
            "</td>\n"+
            "<td>");
        // show item id
        ItemIdentificationPG iipg = info_asset.getItemIdentificationPG();
        if (iipg != null) {
          out.print(
              iipg.getItemIdentification());
        } else {
          out.print("<font color=red>missing itemID</font>");
        }
        out.print(
            "</td>\n"+
            "<td align=right>");
        // show quantity
        out.print(quantity);
        out.print("</td>\n");
      }
    }

    /**
     * printXMLDetails.
     * <p>
     * Prints XML for given Object.
     * <p>
     * Considered embedding some Applet JTree viewer, e.g.<br>
     * <code>ui.planviewer.XMLViewer</code>
     * but would need separate Applet code.
     * <p>
     * Also considered using some nifty javascript XML tree viewer, e.g.<br>
     * <code>http://developer.iplanet.com/viewsource/smith_jstree/smith_jstree.html</code><br>
     * but would take some work...
     * <p>
     * @param printAsHTML uses XMLtoHTMLOutputStream to pretty-print the XML
     */
    private void printXMLDetails(Object xo, boolean printAsHTML){
      try {
         Document doc = new DocumentImpl();
         Element element = XMLize.getPlanObjectXML(xo, doc);
         
         StringWriter stringWriter = new StringWriter();
         XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stringWriter);
         
         xmlWriter.writeStartDocument();
         writeNode(element, xmlWriter);
         xmlWriter.writeEndDocument();
         xmlWriter.flush();
         xmlWriter.close();
         String xml = stringWriter.toString();
         
         TransformerFactory factory = TransformerFactory.newInstance();
         Transformer transformer = factory.newTransformer();
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
         StringWriter formattedStringWriter = new StringWriter();
         transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(formattedStringWriter));
         xml = formattedStringWriter.toString();
         
         PrintWriter pout2 = new PrintWriter(new XMLtoHTMLOutputStream(out));
         out.print("<pre>\n");
         pout2.append(xml);
         out.print("\n</pre>\n");
         pout2.flush();
      } catch (Exception e) {
        if (printAsHTML) {
          out.print("\nException!\n\n");
          e.printStackTrace(out);
        }
      }
    }

   private void writeNode(Node element, XMLStreamWriter xmlWriter) throws XMLStreamException {
      switch (element.getNodeType()) {
         case Node.TEXT_NODE:
            xmlWriter.writeCharacters(element.getTextContent());
            break;
            
         default:
            xmlWriter.writeStartElement(element.getNodeName());
            NodeList childNodes = element.getChildNodes();
            for (int i=0; i<childNodes.getLength(); i++) {
               Node child = childNodes.item(i);
               writeNode(child, xmlWriter);
            }
            xmlWriter.writeEndElement();
      }
   }

    private void printListItemForStack(UID uid) {
      if (ENABLE_STACKS) {
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "<nowrap>Publisher:&nbsp;");
        printLinkToStack(uid, support.getInfo(uid));
        out.print(
            "</nowrap></font>"+
            "</li>\n");
      }
    }

    private void printStack(StackElements stack, UID uid, String type) {
      if (stack == null) {
        out.print(
            "<font color=red>Unable to find "+type+" stack for "+
            uid.toString()+
            "</font>");
      } else {
        out.print(
            "<font color=mediumblue>"+type+":</font>\n"+
            "<font color=green><pre>");
        Throwable t = stack.getThrowable();
        if (t == null) {
          out.print("null");
        } else {
          StackTraceElement[] trace = t.getStackTrace();
          boolean show = false;
          for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if (!show) {
              String cl = ste.getClassName();
              if (cl != null && cl.startsWith("org.cougaar.core.blackboard.")) {
                // hide blackboard internals
                continue;
              }
              show = true;
            }
            out.print("\tat " + ste+"\n");
          }
        }
        out.print("</pre></font>\n");
      }
    }

    /** END PRINT ROUTINES **/

    /** BEGIN PRINTLINK ROUTINES **/

    /**
     * print link to task summary at this agent.
     */

    private void printLinkToTasksSummary()
    {
      printLinkToTasksSummary(
          support.getEncodedAgentName());
    }

    /**
     * print link to task summary for given agent
     */
    private void printLinkToTasksSummary(
        String encodedAgentName)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        // link to agent
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_TASKS_SUMMARY);
        out.print("\" target=\"tablesFrame\">");
        out.print(support.getEncodedAgentName());
        out.print(
            "</a>");
      } else {
        out.print("<font color=red>Unknown agent</font>");
      }
    }

    /** simple flags for parameter checking **/
    private static final byte _FLAG_LIMIT   = (1 << 0);
    private static final byte _FLAG_VERB    = (1 << 1);
    private static final byte _FLAG_VERBOSE = (1 << 2);
    private static final byte _FLAG_SORT    = (1 << 3);

    /**
     * printLinkToAllTasks for the local agent.
     */
    private void printLinkToAllTasks(
        String verb, int limit, int numTasks, boolean verbose)
    {
      printLinkToAllTasks(
          support.getEncodedAgentName(),
          verb, limit, numTasks, verbose);
    }

    /**
     * printLinkToAllTasks.
     */
    private void printLinkToAllTasks(
        String encodedAgentName,
        String verb, int limit, int numTasks, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_TASKS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verb != null) {
          out.print(
              "&"+
              VERB+
              "=");
          out.print(verb);
          flags |= _FLAG_VERB;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
	if (sortByUID)
	  out.print ("&" + SORT_BY_UID + "=true");
	if (sortByEndTime)
	  out.print ("&" + SORT_BY_END_TIME + "=true");

        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numTasks);
            out.print("</b> Tasks at ");
            out.print(encodedAgentName);
            break;
          case (_FLAG_LIMIT | _FLAG_VERB):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERB | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numTasks);
            out.print("</b> Tasks with verb ");
            out.print(verb);
            out.print("at ");
            out.print(encodedAgentName);
            break;
          case (_FLAG_VERB):
            out.print(verb);
            break;
          case (_FLAG_VERB | _FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numTasks);
            out.print("</b> Tasks with verb ");
            out.print(verb);
            out.print(" at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numTasks);
            out.print("</b> Tasks at ");
            out.print(support.getEncodedAgentName());
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown agent</font>");
      }
    }

    /**
     * printLinkToAllPlanElements for the local agent.
     */
    private void printLinkToAllPlanElements(
        int limit, int numPlanElements, boolean verbose)
    {
      printLinkToAllPlanElements(
          support.getEncodedAgentName(),
          limit, numPlanElements, verbose);
    }

    /**
     * printLinkToAllPlanElements.
     */
    private void printLinkToAllPlanElements(
        String encodedAgentName,
        int limit, int numPlanElements, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_PLAN_ELEMENTS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numPlanElements);
            out.print("</b> PlanElements at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numPlanElements);
            out.print("</b> PlanElements at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown agent</font>");
      }
    }

    /**
     * printLinkToAllAssets for the local agent.
     */
    private void printLinkToAllAssets(
        int limit, int numAssets, boolean verbose)
    {
      printLinkToAllAssets(
          support.getEncodedAgentName(),
          limit, numAssets, verbose);
    }

    /**
     * printLinkToAllAssets.
     */
    private void printLinkToAllAssets(
        String encodedAgentName,
        int limit, int numAssets, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_ASSETS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numAssets);
            out.print("</b> Assets at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numAssets);
            out.print("</b> Assets at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown agent</font>");
      }
    }

    /**
     * printLinkToAllUniqueObjects for the local agent.
     */
    private void printLinkToAllUniqueObjects(
        int limit, int numUniqueObjects, boolean verbose)
    {
      printLinkToAllUniqueObjects(
          support.getEncodedAgentName(),
          limit, numUniqueObjects, verbose);
    }

    /**
     * printLinkToAllUniqueObjects.
     */
    private void printLinkToAllUniqueObjects(
        String encodedAgentName,
        int limit, int numUniqueObjects, boolean verbose)
    {
      if (encodedAgentName != null) {
        out.print("<a href=\"/$");
        out.print(encodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_ALL_UNIQUE_OBJECTS);
        // set flags
        byte flags = 0;
        if (limit > 0) {
          out.print(
              "&"+
              LIMIT+
              "=true");
          flags |= _FLAG_LIMIT;
        }
        if (verbose) {
          flags |= _FLAG_VERBOSE;
        }
        out.print("\" target=\"tablesFrame\">");
        // print over-customized output .. make parameter?
        switch (flags) {
          case (_FLAG_LIMIT):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b>");
            break;
          case (_FLAG_LIMIT | _FLAG_VERBOSE):
            out.print("View first <b>");
            out.print(limit);
            out.print("</b> of <b>");
            out.print(numUniqueObjects);
            out.print("</b> UniqueObjects at ");
            out.print(encodedAgentName);
            break;
          default:
          case (0):
          case (_FLAG_VERBOSE):
            out.print("View all <b>");
            out.print(numUniqueObjects);
            out.print("</b> UniqueObjects at ");
            out.print(encodedAgentName);
            break;
        }
        out.print("</a>");
      } else {
        out.print("<font color=red>Unknown agent</font>");
      }
    }

    /**
     * printLinkToParentTask.
     * <p>
     * Get task's parent before linking.
     */
    private void printLinkToParentTask(Task task)
    {
      UID ptU;
      String ptUID;
      if (task == null) {
        out.print("<font color=red>null</font>");
      } else if (((ptU = task.getParentTaskUID()) == null) ||
          ((ptUID = ptU.toString()) == null)) {
        out.print("<font color=red>parent not unique</font>");
      } else {
        MessageAddress tAgentID = task.getSource();
        String ptEncodedAgentName;
        if ((tAgentID == null) ||
            ((ptEncodedAgentName = tAgentID.toString()) == null)) {
          ptEncodedAgentName = support.getEncodedAgentName();
        } else {
          ptEncodedAgentName = support.encodeAgentName(ptEncodedAgentName);
        }
        out.print("<a href=\"/$");
        out.print(ptEncodedAgentName);
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_TASK_DETAILS+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(ptUID));
        out.print("\" target=\"itemFrame\">");
        out.print(ptUID);
        out.print("</a>");
      }
    }

    /**
     * printLinkToLocalTask.
     * <p>
     * Tasks that stay in the current agent.
     */
    private void printLinkToLocalTask(Task task)
    {
      printLinkToTask(
          task == null ? null : task.getUID(), 
          support.getEncodedAgentName());
    }

    /**
     * printLinkToTask.
     * <p>
     * This method attempts to works around task forwarding across
     * agents in the "Down" sense, i.e. allocations.
     */
    private void printLinkToTask(
        UID taskU, 
        String atEncodedAgentName)
    {
      String taskUID;
      if (taskU == null) {
        out.print("<font color=red>null</font>");
      } else {
        taskUID = taskU.toString();
        if (taskUID == null) {
          out.print("<font color=red>not unique</font>");
        } else {
          out.print("<a href=\"/$");
          out.print(atEncodedAgentName);
          out.print(support.getPath());
          out.print(
            "?"+
            MODE+
            "="+
            MODE_TASK_DETAILS+
            "&"+
            ITEM_UID+
            "=");
          out.print(encodeUID(taskUID));
          out.print("\" target=\"itemFrame\">");
          out.print(taskUID);
          out.print("</a>");
        }
      }
    }

    /**
     * printLinkToPlanElement.
     * <p>
     * PlanElements stay in their agent
     */
    private void printLinkToPlanElement(PlanElement pe)
    {
      UID peU;
      String peUID;
      if (pe == null) {
        out.print("<font color=red>null</font>\n");
      } else if (((peU = pe.getUID()) == null) ||
          ((peUID = peU.toString()) == null)) {
        out.print("<font color=red>not unique</font>\n");
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_PLAN_ELEMENT_DETAILS+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(peUID));
        out.print("\" target=\"itemFrame\">");
        out.print(peUID);
        out.print("</a>");
      }
    }

    /**
     * printLinkToLocalAsset.
     * <p>
     * Asset that is in the local blackboard and has a UID.  Treat this
     * as an Asset attached to itself.
     **/
    private void printLinkToLocalAsset(Asset asset)
    {
      printLinkToAttachedAsset(
          asset, 
          asset,
          MODE_ASSET_DETAILS);
    }

    /**
     * printLinkToTaskDirectObject.
     **/
    private void printLinkToTaskDirectObject(Task task)
    {
      printLinkToAttachedAsset(
          task, 
          ((task != null) ? task.getDirectObject() : null),
          MODE_TASK_DIRECT_OBJECT_DETAILS);
    }

    /**
     * printLinkToAssetTransferAsset.
     **/
    private void printLinkToAssetTransferAsset(AssetTransfer atrans)
    {
      printLinkToAttachedAsset(
          atrans, 
          ((atrans != null) ? atrans.getAsset() : null),
          MODE_ASSET_TRANSFER_ASSET_DETAILS);
    }

    /**
     * printLinkToAttachedAsset.
     *
     * @see #printLinkToTaskDirectObject
     * @see #printLinkToAssetTransferAsset
     **/
    private void printLinkToAttachedAsset(
        UniqueObject baseObj, Asset asset, 
        int baseMode)
    {
      UID baseObjU;
      String baseObjUID;
      if ((baseObj == null) ||
          (asset == null)) {
        out.print("<font color=red>null</font>");
      } else if (((baseObjU = baseObj.getUID()) == null) ||
          ((baseObjUID = baseObjU.toString()) == null)) {
        out.print("<font color=red>not unique</font>");
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            baseMode+
            "&"+
            ITEM_UID+
            "=");
        out.print(encodeUID(baseObjUID));
        out.print("\" target=\"itemFrame\">");
        String assetName;
        if (asset == baseObj) {
          // asset it it's own base
          assetName = baseObjUID;
        } else {
          UID assetU;
          // asset attached to the base UniqueObject
          if (((assetU = asset.getUID()) == null) ||
              ((assetName = assetU.toString()) == null)) {
            if (asset instanceof AggregateAsset) {
              assetName = "Non-UID Aggregate";
            } else if (asset instanceof AssetGroup) {
              assetName = "Non-UID Group";
            } else {
              assetName = "Non-UID "+asset.getClass().getName();
            }
          }
        }
        out.print(assetName);
        out.print("</a>");
      }
    }

    /**
     * printLinkToXML.
     * <p>
     * XML objects stay in agent.
     **/
    private void printLinkToXML(
        UniqueObject uo, boolean asHTML)
    {
      if (uo != null) {
        // link to HTML-encoded XML view
        printLinkToAttachedXML(
            uo,
            uo,
            asHTML);
      } else {
        out.print("<font color=red>null</font>");
      }
    }

    /**
     * printLinkToXML.
     * <p>
     * XML objects stay in agent.
     **/
    private void printLinkToXML(
        Object xo, boolean asHTML)
    {
      if (xo instanceof UniqueObject) {
        // link to HTML-encoded XML view
        printLinkToAttachedXML(
            (UniqueObject)xo,
            xo,
            asHTML);
      } else if (xo == null) {
        out.print("<font color=red>null</font>");
      } else {
        // asset not unique
        out.print("<font color=red>");
        out.print(xo.getClass().getName());
        out.print(" not a UniqueObject</font>");
      }
    }

    /**
     * printLinkToAttachedXML.
     **/
    private void printLinkToAttachedXML(
        UniqueObject baseObj, Object xo, 
        boolean asHTML)
    {
      UID baseObjU;
      String baseObjUID;
      if ((xo == null) ||
          (baseObj == null) ||
          ((baseObjU = baseObj.getUID()) == null) ||
          ((baseObjUID = baseObjU.toString()) == null)) {
        if (asHTML) {
          out.print("<font color=red>Unable to view XML</font>\n");
        } else {
          out.print("<font color=red>Raw XML unavailable</font>\n");
        }
      } else {
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "=");
        int mode =
          ((xo == baseObj) ?
           (asHTML ? 
            MODE_XML_HTML_DETAILS :
            MODE_XML_RAW_DETAILS) :
           (asHTML ? 
            MODE_XML_HTML_ATTACHED_DETAILS :
            MODE_XML_RAW_ATTACHED_DETAILS));
        out.print(mode);
        out.print(
            "&"+
            ITEM_UID+
            "=");
        String encBaseObjUID = encodeUID(baseObjUID);
        out.print(encBaseObjUID);
        out.print("\" target=\"xml_");
        out.print(encBaseObjUID);
        out.print("_page\">");
        String xoName;
        if (xo == baseObj) {
          xoName = baseObjUID;
        } else {
          if (xo instanceof UniqueObject) {
            UID xoU;
            if (((xoU = ((UniqueObject)xo).getUID()) == null) ||
                ((xoName = xoU.toString()) == null)) {
              if (xo instanceof AggregateAsset) {
                xoName = "Non-UID Aggregate";
              } else if (xo instanceof AssetGroup) {
                xoName = "Non-UID Group";
              } else {
                xoName = "Non-UID "+xo.getClass().getName();
              }
            }
          } else {
            xoName = "Non-UniqueObject "+xo.getClass().getName();
          }
        }
        if (asHTML) {
          out.print("View XML for ");
          out.print(xoName);
        } else {
          out.print("Raw XML for ");
          out.print(xoName);
        }
        out.print("</a>\n");
      }
    }

    /**
     * printLinkToStack
     */
    private void printLinkToStack(
        UID uid,
        PublisherInfo info) {
      String publisher = (info == null ? null : info.getPublisher());
      String suid;
      if (publisher == null) {
        out.print("<font color=red>Unknown</font>\n");
      } else if (
          (info.getAddStack() == null) ||
          (uid == null) ||
          ((suid = uid.toString()) == null)) {
        out.print(encodeHTML(publisher, true));
      } else {
        String encUID = encodeUID(suid);
        out.print("<a href=\"/$");
        out.print(support.getEncodedAgentName());
        out.print(support.getPath());
        out.print(
            "?"+
            MODE+
            "="+
            MODE_STACK_DETAILS+
            "&"+
            ITEM_UID+
            "="+
            encodeUID(suid)+
            "\" target=\"itemFrame\">");
        out.print(encodeHTML(publisher, true));
        out.print("</a>\n");
      }
    }

    /** END PRINTLINK ROUTINES **/

    /** BEGIN UTILITY PARSERS **/

    /**
     * printObject.
     * <p>
     * Currently used to print Preposition.getIndirectObject()
     * <p>
     * recursive for AssetGroups!
     */
    private void printObject(Object io)
    {
      try {
        if (io == null) {
          out.print("<font color=red>null</font>");
        } else if (io instanceof String) {
          out.print((String)io);
        } else if (io instanceof Location) {
          out.print("Location: \"");
          out.print(io.toString());
          out.print("\"");
        } else if (io instanceof Asset) {
          Asset as = (Asset)io;
          out.print("Asset: \"");
          TypeIdentificationPG tipg;
          String tiNomen;
          if (((tipg = as.getTypeIdentificationPG()) != null) &&
              ((tiNomen = tipg.getNomenclature()) != null)) {
            out.print(tiNomen);
          }
          out.print("(asset type=");
          out.print(as.getClass().getName());
          out.print(", asset uid=");
          UID asu;
          String uid;
          if (((asu = as.getUID()) != null) &&
              ((uid = asu.toString()) != null)) {
            out.print(uid);
          } else {
            out.print("None");
          }
          out.print(")\"");
        } else if (io instanceof Schedule) {
          out.print(io.getClass().getName());
        } else if (io instanceof MessageAddress) {
          out.print("AID: \"");
          out.print(((MessageAddress)io).toString());
          out.print("\"");
        } else if (io instanceof AssetTransfer) {
          out.print("AssetTransfer: \"");
          out.print(((AssetTransfer)io).getAsset().getName());
          out.print("\"");
        } else if (io instanceof AssetAssignment) {
          out.print("AssetAssignment: \"");
          out.print(((AssetAssignment)io).getAsset().getName());
          out.print("\"");
        } else if (io instanceof AssetGroup) {
          out.print("AssetGroup: \"[");
          List assets = ((AssetGroup)io).getAssets();
          for (int i = 0; i < assets.size(); i++) {
            Asset as = (Asset)assets.get(i);
            // recursive!
            printObject(as);
          }
          out.print("]\"");
        } else if (io instanceof AbstractMeasure) {
          String clName = ((AbstractMeasure)io).getClass().getName();
          int i = clName.lastIndexOf('.');
          if (i > 0) {
            clName = clName.substring(i+i);
          }
          out.print(clName);
          out.print(": ");
          out.print(io.toString());
        } else { 
          out.print(io.getClass().getName()); 
          out.print(": ");
          out.print(io.toString());
        }
      } catch (Exception e) {
        out.print("<font color=red>invalid</font>");
      }
    }

    /** END UTILITY PARSERS **/

    /** BEGIN BLACKBOARD SEARCHERS **/

    private UnaryPredicate getUniqueObjectWithUIDPred(final String uidFilter) {
      UID findUID = UID.toUID(uidFilter);
      return new UniqueObjectWithUIDPredicate(findUID);
    }
    private static final class UniqueObjectWithUIDPredicate 
        implements UnaryPredicate {
      private final UID findUID;
      public UniqueObjectWithUIDPredicate(UID findUID) {
        this.findUID = findUID;
      }
      public boolean execute(Object o) {
        if (o instanceof UniqueObject) {
          UID u = ((UniqueObject)o).getUID();
          return findUID.equals(u);
        }
        return false;
      }
    }

    private UnaryPredicate getTaskPred() {
      return new TaskPredicate();
    }
    private static final class TaskPredicate implements UnaryPredicate {
      public boolean execute(Object o) {
        return (o instanceof Task);
      }
    }

    private UnaryPredicate getTaskWithVerbPred(Verb v) {
      return new TaskWithVerbPredicate(v);
    }
    private static final class TaskWithVerbPredicate
        implements UnaryPredicate {
      private final Verb v;
      public TaskWithVerbPredicate(Verb v) {
        this.v = v;
      }
      public boolean execute(Object o) {
        return ((o instanceof Task) &&
            v.equals(((Task)o).getVerb()));
      }
      public String toString() {
        return getClass().getName()+"[verb="+v+"]";
      }
    }

    private UnaryPredicate getPlanElementPred() {
      return new PlanElementPredicate();
    }
    private static final class PlanElementPredicate implements UnaryPredicate {
      public boolean execute(Object o) {
        return (o instanceof PlanElement);
      }
    }

    private UnaryPredicate getAssetPred() {
      return new AssetPredicate();
    }
    public static final class AssetPredicate implements UnaryPredicate {
      public boolean execute(Object o) {
        return (o instanceof Asset);
      }
    }

    private UnaryPredicate getUniqueObjectPred() {
      return new UniqueObjectPredicate();
    }

    private Collection searchUsingPredicate(
        UnaryPredicate pred) 
    {
      Collection col = support.queryBlackboard(pred);
      if (col.size() > 1) {
        Comparator comparator = null;
        if (sortByEndTime) {
          comparator = THE_ONLY_END_TIME_COMPARATOR;
        } else if (sortByUID) {
          comparator = THE_ONLY_UID_COMPARATOR;
        }
        if (comparator != null) {
          Object[] a = col.toArray();
          Arrays.sort(a, comparator);
          return Arrays.asList(a);
        }
      }
      return col;
    }

    private static final Comparator THE_ONLY_UID_COMPARATOR = new UIDComparator ();

    private static class UIDComparator implements Comparator {
      public int compare (Object first, Object second) {
	if (first instanceof UniqueObject) {
	  if (second instanceof UniqueObject) {
	    // return the usual UID compare
	    UID u1 = ((UniqueObject) first).getUID();
	    UID u2 = ((UniqueObject) second).getUID();
	    return u1.compareTo(u2);
	  } else {
	    return -1;
	  }
	} else if (second instanceof UniqueObject) {
	  return 1;
	} else {
	  return 0;
	}
      }
    }

    private static final Comparator THE_ONLY_END_TIME_COMPARATOR = new EndTimeComparator ();

    private static class EndTimeComparator implements Comparator {
      public int compare (Object first, Object second) {
	if (first instanceof Task) {
	  if (second instanceof Task) {
	    // return the usual UID compare
            try {
              long t1 = PluginHelper.getEndTime((Task) first);
              long t2 = PluginHelper.getEndTime((Task) second);
              if (t1 < t2) return -1;
              if (t1 > t2) return 1;
            } catch (Exception e) {
              // Probably no end time preference
            }
            return THE_ONLY_UID_COMPARATOR.compare(first, second);
	  } else {
	    return -1;
	  }
	} else if (second instanceof Task) {
	  return 1;
	} else {
	  return 0;
	}
      }
    }

    private UniqueObject findUniqueObjectWithUID(
        final String itemUID)
    {
      if (itemUID == null) {
        // missing UID
        return null;
      }
      Collection col = 
        searchUsingPredicate(
            getUniqueObjectWithUIDPred(itemUID));
      if (col.size() < 1) {
        // item not found
        return null;
      }
      // take first match
      Iterator iter = col.iterator();
      UniqueObject uo = (UniqueObject)iter.next();
      if (DEBUG) {
        if (iter.hasNext()) {
          System.err.println("Multiple matches for "+itemUID+"?");
        }
      }
      return uo;
    }

    private Collection findAllTasks()
    {
      return 
        searchUsingPredicate(getTaskPred());
    }

    private Collection findTasksWithVerb(
        final String verbFilter)
    {
      if (verbFilter == null) {
        // missing verb
        return null;
      }
      Verb v = Verb.get(verbFilter);
      return 
        searchUsingPredicate(
            getTaskWithVerbPred(v));
    }

    private Collection findAllPlanElements()
    {
      return 
        searchUsingPredicate(
            getPlanElementPred());
    }

    private Collection findAllAssets()
    {
      return 
        searchUsingPredicate(
            getAssetPred());
    }

    private Collection findAllUniqueObjects()
    {
      return 
        searchUsingPredicate(
            getUniqueObjectPred());
    }

    /** END BLACKBOARD SEARCHERS **/

    /** BEGIN MISC UTILITIES **/

    /**
     * Item type codes to show interface name instead of "*Impl".
     **/
    private static final int ITEM_TYPE_ALLOCATION     = 0;
    private static final int ITEM_TYPE_EXPANSION      = 1;
    private static final int ITEM_TYPE_AGGREGATION    = 2;
    private static final int ITEM_TYPE_DISPOSITION    = 3;
    private static final int ITEM_TYPE_ASSET_TRANSFER = 4;
    private static final int ITEM_TYPE_TASK           = 5;
    private static final int ITEM_TYPE_ASSET          = 6;
    private static final int ITEM_TYPE_WORKFLOW       = 7;
    private static final int ITEM_TYPE_OTHER          = 8;
    private static String[] ITEM_TYPE_NAMES;
    static {
      ITEM_TYPE_NAMES = new String[(ITEM_TYPE_OTHER+1)];
      ITEM_TYPE_NAMES[ITEM_TYPE_ALLOCATION     ] = "Allocation";
      ITEM_TYPE_NAMES[ITEM_TYPE_EXPANSION      ] = "Expansion";
      ITEM_TYPE_NAMES[ITEM_TYPE_AGGREGATION    ] = "Aggregation";
      ITEM_TYPE_NAMES[ITEM_TYPE_DISPOSITION    ] = "Disposition";
      ITEM_TYPE_NAMES[ITEM_TYPE_ASSET_TRANSFER ] = "AssetTransfer";
      ITEM_TYPE_NAMES[ITEM_TYPE_TASK           ] = "Task";
      ITEM_TYPE_NAMES[ITEM_TYPE_ASSET          ] = "Asset";
      ITEM_TYPE_NAMES[ITEM_TYPE_WORKFLOW       ] = "Workflow";
      ITEM_TYPE_NAMES[ITEM_TYPE_OTHER          ] = null;
    }

    private static String getItemTypeString(Object item) {
      int itemType = getItemType(item);
      if (itemType == ITEM_TYPE_OTHER) return item.getClass().getName();
      return ITEM_TYPE_NAMES[itemType];
    }

    /**
     * getItemType.
     * <p>
     * Replace with synchronized hashmap lookup on obj.getClass()?
     **/
    private static int getItemType(Object obj) {
      if (obj instanceof PlanElement) {
        if (obj instanceof Allocation) {
          return ITEM_TYPE_ALLOCATION;
        } else if (obj instanceof Expansion) {
          return ITEM_TYPE_EXPANSION;
        } else if (obj instanceof Aggregation) {
          return ITEM_TYPE_AGGREGATION;
        } else if (obj instanceof Disposition) {
          return ITEM_TYPE_DISPOSITION;
        } else if (obj instanceof AssetTransfer) {
          return ITEM_TYPE_ASSET_TRANSFER;
        } else {
          return ITEM_TYPE_OTHER;
        }
      } else if (obj instanceof Task) {
        return ITEM_TYPE_TASK;
      } else if (obj instanceof Asset) {
        return ITEM_TYPE_ASSET;
      } else if (obj instanceof Workflow) {
        return ITEM_TYPE_WORKFLOW;
      } else {
        return ITEM_TYPE_OTHER;
      }
    }

    /**
     * SummaryInfo.
     * <p>
     * Counter holder
     **/
    private static class SummaryInfo {
      public int counter;
      public SummaryInfo() {
        counter = 0;
      }
      public static final Comparator LARGEST_COUNTER_FIRST_ORDER = 
        new Comparator() {
          public final int compare(Object o1, Object o2) {
            int c1 = ((SummaryInfo)o1).counter;
            int c2 = ((SummaryInfo)o2).counter;
            return ((c1 > c2) ? -1 : ((c1 == c2) ? 0 : 1));
          }
        };
    }

    /**
     * SummaryInfo.
     */
    private static class VerbSummaryInfo extends SummaryInfo {
      public Verb verb;
      public VerbSummaryInfo(Verb vb) {
        super();
        verb = vb;
      }
    }

    /**
     * Dates are formatted to "month_day_year_hour:minute[AM|PM]"
     */
    private static SimpleDateFormat myDateFormat;
    private static Date myDateInstance;
    private static java.text.FieldPosition myFieldPos;
    static {
      myDateFormat = new SimpleDateFormat("MM_dd_yyyy_h:mma");
      myDateInstance = new Date();
      myFieldPos = new java.text.FieldPosition(SimpleDateFormat.YEAR_FIELD);
    }

    /**
     * getTimeString.
     * <p>
     * Formats time to String.
     */
    private static String getTimeString(long time) {
      synchronized (myDateFormat) {
        myDateInstance.setTime(time);
        return 
          myDateFormat.format(
              myDateInstance,
              new StringBuffer(20), 
              myFieldPos
                             ).toString();
      }
    }

    /**
     * bit[] based upon URLEncoder.
     */
    static boolean[] DONT_NEED_ENCODING;
    static {
      DONT_NEED_ENCODING = new boolean[256];
      for (int i = 'a'; i <= 'z'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      for (int i = 'A'; i <= 'Z'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      for (int i = '0'; i <= '9'; i++) {
        DONT_NEED_ENCODING[i] = true;
      }
      DONT_NEED_ENCODING['-'] = true;
      DONT_NEED_ENCODING['_'] = true;
      DONT_NEED_ENCODING['.'] = true;
      DONT_NEED_ENCODING['*'] = true;

      // special-case to not encode "/"
      DONT_NEED_ENCODING['/'] = true;
    }

    /**
     * Saves some String allocations.
     */
    private static String encodeUID(String s) {
      int n = s.length();
      for (int i = 0; i < n; i++) {
//        int c = (int)s.charAt(i);
        if (!(DONT_NEED_ENCODING[i])) {
          try {
            return URLEncoder.encode(s, "UTF-8");
          } catch (Exception e) {
            throw new IllegalArgumentException(
                "Unable to encode URL ("+s+")");
          }
        }
      }
      return s;
    }

    /**
     * Encodes a string that may contain HTML syntax-significant
     * characters by replacing with a character entity.
     **/
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

    /**
     * XMLtoHTMLOutputStream.
     * <p>
     * Filter which converts XML to simple HTML.
     * Assumes &lt;pre&gt; tag surrounds this call, e.g.
     * <pre><code>
     *   String xml = "&gt;tag&lt;value&gt;/tag&lt;";
     *   PrintStream out = System.out;
     *   XMLtoHTMLOutputStream xout = new XMLtoHTMLOutputStream(out);
     *   out.print("&lt;pre&gt;\n");
     *   xout.print(xml);
     *   xout.flush();
     *   out.print("\n&lt;/pre&gt;");
     * </code></pre>
     * This keeps the spacing uniform and saves some writing.
     */
    public static class XMLtoHTMLOutputStream extends FilterWriter 
    {
      private static final char[] LESS_THAN;
      private static final char[] GREATER_THAN;
      static {
        LESS_THAN = "<font color=green>&lt;".toCharArray();
        GREATER_THAN = "&gt;</font>".toCharArray();
      }

      public XMLtoHTMLOutputStream(Writer w) {
        super(w);
      }

      public void write(String str, int off, int len) throws IOException 
      {
        int n = off+len;
        for (int i = off; i < n; i++) {
          write(str.charAt(i));
        }
      }

      public void write(char cbuf[], int off, int len) throws IOException 
      {
        int n = off+len;
        for (int i = off; i < n; i++) {
          write(cbuf[i]);
        }
      }

      public void write(int c) throws IOException {
        //
        // NOTE: "this.out" is *not* the PlanViewer's "out"!
        //
        if (c == '<') {
          this.out.write(LESS_THAN);
        } else if (c == '>') {
          this.out.write(GREATER_THAN);
        } else {
          this.out.write(c);
        }
      }
    }

    private static class UnaryPredicateParser {
      private static String CLNAME = 
        "org.cougaar.lib.contract.lang.OperatorFactoryImpl";
      private static Integer STYLE =
        new Integer(13); //paren-pretty-verbose

      private static Exception loadE;
      private static Object inst;
      private static Method meth;

      public static UnaryPredicate parse(
          String s) throws Exception {
        ensureIsLoaded();
        return (UnaryPredicate)
          meth.invoke(inst, new Object[] {STYLE, s});
      }

      private static synchronized void ensureIsLoaded() throws Exception {
        if (inst == null) {
          if (loadE == null) {
            try {
              Class cl = Class.forName(CLNAME);
              meth = cl.getMethod(
                  "create", 
                  new Class[] {Integer.TYPE, Object.class});
              inst = cl.newInstance(); 
              return;
            } catch (Exception e) {
              loadE = new RuntimeException(
                  "Unable to load "+CLNAME, e);
            }
          }
          throw loadE;
        }
      }
    }

    /** END MISC UTILITIES **/
  }
}
