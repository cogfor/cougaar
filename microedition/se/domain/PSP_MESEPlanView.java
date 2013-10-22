/*
 *
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.microedition.se.domain;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import org.cougaar.microedition.shared.NameTablePair;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.measure.AbstractMeasure;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.lib.planserver.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.lib.planserver.psp.*;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.XMLSerializer;
import org.apache.xml.serialize.OutputFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <pre>
 * Provides HTML views of a Cluster's Plan, allowing user to:
 *   1) List Tasks, PlanElements, Assets, UniqueObjects
 *   2) View detailed information for Tasks, PlanElements, etc.
 *   3) Search for an item by UID
 *   4) Link PSP from cluster to cluster, allowing one to seamlessly
 *      jump between plans in a browser.
 *   5) Provide XML views of data (using org.cougaar.util.XMLify)
 * </pre>.
 * <pre>
 * Wish list:
 *   1) Provide more powerful search, either by DebugUI-style
 *      UnaryPredicate creation or (better) by new Plugin Contracts
 *      Predicate Language.
 *   2) Others?
 *
 * Original Notes:
 *   This is a demonstration/proof-of-concept PSP which illustrates a number
 *   of techniques based on PSP (server-side) layout and HTML to construct
 *   a "multi-pane" Cluster Plan View.
 *
 *   Important to note how cross-references between Cluster LPSs are
 *   specified in HTML.  No longer will browser client directly speak
 *   to any LPS other than its host.  Task drill-down and Task views to
 *   other clusters are handled via redirection at server rather than
 *   URL link from client.
 *
 *   This is required by Netscape browsers to work with
 *   multiple-form/multipe-URL data model.  Netscape browsers insist
 *   on opening new window when Host changes.
 * </pre>
 **/

public class PSP_MESEPlanView
    extends PSP_BaseAdapter
    implements PlanServiceProvider, UISubscriber
{

  /**
   * System property for the filename of the "Advanced Search" list
   * predicates -- if not specified, the filename defaults to
   * <tt>DEFAULT_PREDS_FILENAME</tt>.
   * <p>
   * This file is parsed by <code>PredTableParser</code> and must
   * conform to the format defined in the javadocs.
   *
   * @see PredTableParser
   */
  public static final String PREDS_PROPERTY = "org.cougaar.lib.psp.preds";

  /**
   * Default name for the "Advanced Search" list of predicates.
   *
   * @see #PRED_PROPERTY
   */
  public static final String DEFAULT_PREDS_FILENAME = "default.preds.dat";

  /** This image URL should be local! **/
  public static final String IMAGE_BACKGROUND_URL =
    null;
  // "https://www.alpine.bbn.com/alpine/images/alp-logo-back.gif";

  protected static final boolean DEBUG = false;

  protected static final int DEFAULT_LIMIT = 100;

  public void execute(PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu) throws Exception
  {
    MyPSPState myState = new MyPSPState(this, query_parameters, psc);
    myState.configure(query_parameters);

    // ============= MicroAgent Linkage stuff =========
    UnaryPredicate portTablePred = new UnaryPredicate()
    {
      public boolean execute(Object o) {
        boolean ret = false;
          if (o instanceof NameTablePair) {
            NameTablePair ntable = (NameTablePair)o;
            if (ntable.name.equals("MicroWebServerPorts")) {
               ret = true;
            }
          }
        return ret;
      }
    };

    porttableSub = (IncrementalSubscription)psc
        .getServerPluginSupport().subscribe(this, portTablePred);

    // ================================================

    try {
      // table of functions (make interfaces?)
      switch (myState.mode) {
        default:
          if (DEBUG) {
            System.err.println("DEFAULT MODE");
          }
        case MyPSPState.MODE_FRAME:
          displayFrame(myState, out);
          break;
        case MyPSPState.MODE_WELCOME:
          displayWelcome(myState, out);
          break;
        case MyPSPState.MODE_WELCOME_DETAILS:
          displayWelcomeDetails(myState, out);
          break;
        case MyPSPState.MODE_ALL_TASKS:
          displayAllTasks(myState, out);
          break;
        case MyPSPState.MODE_TASK_DETAILS:
          displayTaskDetails(myState, out);
          break;
        case MyPSPState.MODE_TASKS_SUMMARY:
          displayTasksSummary(myState, out);
          break;
        case MyPSPState.MODE_PLAN_ELEMENT_DETAILS:
          displayPlanElementDetails(myState, out);
          break;
        case MyPSPState.MODE_ALL_PLAN_ELEMENTS:
          displayAllPlanElements(myState, out);
          break;
        case MyPSPState.MODE_ASSET_DETAILS:
        case MyPSPState.MODE_TASK_DIRECT_OBJECT_DETAILS:
        case MyPSPState.MODE_ASSET_TRANSFER_ASSET_DETAILS:
          displayAssetDetails(myState, out);
          break;
        case MyPSPState.MODE_ALL_ASSETS:
          displayAllAssets(myState, out);
          break;
        case MyPSPState.MODE_CLUSTERS:
        case MyPSPState.MODE_SEARCH:
          displaySearch(myState, out);
          break;
        case MyPSPState.MODE_XML_HTML_DETAILS:
        case MyPSPState.MODE_XML_HTML_ATTACHED_DETAILS:
        case MyPSPState.MODE_XML_RAW_DETAILS:
        case MyPSPState.MODE_XML_RAW_ATTACHED_DETAILS:
          displayUniqueObjectDetails(myState, out);
          break;
        case MyPSPState.MODE_ALL_UNIQUE_OBJECTS:
          displayAllUniqueObjects(myState, out);
          break;
        case MyPSPState.MODE_ADVANCED_SEARCH_FORM:
          displayAdvancedSearchForm(myState, out);
          break;
        case MyPSPState.MODE_ADVANCED_SEARCH_RESULTS:
          displayAdvancedSearchResults(myState, out);
          break;
      }
    } catch (Exception e) {
      System.err.println("TASKS.PSP Exception: ");
      System.err.println(e);
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

  /** BEGIN LOGPLAN SEARCHERS **/

  protected static UnaryPredicate getUniqueObjectWithUIDPred(
      final String uidFilter)
  {
    final UID findUID = UID.toUID(uidFilter);
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        if (o instanceof UniqueObject) {
          UID u = ((UniqueObject)o).getUID();
          return
            findUID.equals(u);
        }
        return false;
      }
    };
  }

  protected static UnaryPredicate getTaskPred()
  {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Task);
      }
    };
  }

  protected static UnaryPredicate getTaskWithVerbPred(final Verb v)
  {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        return ((o instanceof Task) &&
            v.equals(((Task)o).getVerb()));
      }
    };
  }

  protected static UnaryPredicate getPlanElementPred()
  {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof PlanElement);
      }
    };
  }

  protected static UnaryPredicate getAssetPred()
  {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Asset);
      }
    };
  }

  protected static UnaryPredicate getUniqueObjectPred()
  {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof UniqueObject);
      }
    };
  }

  protected static Collection searchUsingPredicate(
      MyPSPState myState, UnaryPredicate pred)
  {
    return myState.sps.queryForSubscriber(pred);
  }

  protected static UniqueObject findUniqueObjectWithUID(
      MyPSPState myState, final String itemUID)
  {
    if (itemUID == null) {
      // missing UID
      return null;
    }
    Collection col =
      searchUsingPredicate(
          myState,
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

  protected static Collection findAllTasks(
      MyPSPState myState)
  {
    return
      searchUsingPredicate(
          myState,
          getTaskPred());
  }

  protected static Collection findTasksWithVerb(
      MyPSPState myState, final String verbFilter)
  {
    if (verbFilter == null) {
      // missing verb
      return null;
    }
    Verb v = Verb.getVerb(verbFilter);
    return
      searchUsingPredicate(
          myState,
          getTaskWithVerbPred(v));
  }

  protected static Collection findAllPlanElements(
      MyPSPState myState)
  {
    return
      searchUsingPredicate(
          myState,
          getPlanElementPred());
  }

  protected static Collection findAllAssets(
      MyPSPState myState)
  {
    return
      searchUsingPredicate(
          myState,
          getAssetPred());
  }

  protected static Collection findAllUniqueObjects(
      MyPSPState myState)
  {
    return
      searchUsingPredicate(
          myState,
          getUniqueObjectPred());
  }

  /** END LOGPLAN SEARCHERS **/

  /** BEGIN DISPLAY ROUTINES **/

  /**
   * displayFrame.
   */
  protected static void displayFrame(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay Frame");
    }
    out.print(
        "<html>\n"+
        "<head>\n"+
        "<title>"+
        "Cluster Plan View"+
        "</title>\n"+
        "</head>\n"+
        "<frameset cols=\"25%,75%\">\n"+
        "<frameset rows=\"32%,68%\">\n"+
        "<frame src=\"/$");
    out.print(myState.encodedClusterID);
    out.print(myState.psp_path);
    out.print(
        "?"+
        MyPSPState.MODE+
        "="+
        MyPSPState.MODE_SEARCH+
        "\" name=\"searchFrame\">\n");
    //
    // Show blank WelcomeDetails page in itemFrame, since user
    // probably didn't specify $encodedCluster in URL.
    //
    out.print("<frame src=\"/$");
    out.print(myState.encodedClusterID);
    out.print(myState.psp_path);
    out.print(
        "?"+
        MyPSPState.MODE+
        "="+
        MyPSPState.MODE_WELCOME_DETAILS+
        "\" name=\"itemFrame\">\n"+
        "</frameset>\n"+
        "<frame src=\"/$");
    out.print(myState.encodedClusterID);
    out.print(myState.psp_path);
    //
    // Show blank Welcome page in tablesFrame, since user
    // probably didn't specify $encodedCluster in URL.
    //
    out.print(
        "?"+
        MyPSPState.MODE+
        "="+
        MyPSPState.MODE_WELCOME+
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
  protected static void displayWelcome(
      MyPSPState myState, PrintStream out)
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
    if (IMAGE_BACKGROUND_URL != null) {
      out.print(
          "background=\""+
          IMAGE_BACKGROUND_URL+
          "\" ");
    }
    out.print(
        "bgcolor=\"#F0F0F0\">\n"+
        "<p>"+
        "<font size=small color=mediumblue>No Cluster selected.</font>\n"+
        "</body>\n"+
        "</html>\n");
    out.flush();
  }

  /**
   * displayWelcomeDetails.
   */
  protected static void displayWelcomeDetails(
      MyPSPState myState, PrintStream out)
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
  protected static void displayTaskDetails(
      MyPSPState myState, PrintStream out)
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
    // link to cluster
    printLinkToTasksSummary(myState, out);
    out.print(
        "</b><br>\n"+
        "Task<br>");
    // find task
    UniqueObject baseObj =
      findUniqueObjectWithUID(myState, myState.itemUID);
    if (baseObj instanceof Task) {
      printTaskDetails(myState, out, (Task)baseObj);
    } else {
      out.print(
          "<p>"+
          "<font size=small color=mediumblue>");
      if (myState.itemUID == null) {
        out.print("No Task selected.");
      } else if (baseObj == null) {
        out.print("No Task matching \"");
        out.print(myState.itemUID);
        out.print("\" found.");
      } else {
        out.print("UniqueObject with UID \"");
        out.print(myState.itemUID);
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
  protected static void displayAllTasks(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay All Tasks");
    }
    // find tasks
    Collection col;
    if (myState.verbFilter != null) {
      col = findTasksWithVerb(myState, myState.verbFilter);
    } else {
      col = findAllTasks(myState);
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
    out.print(myState.clusterID);
    out.print(
        " Tasks"+
        "</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<p>"+
        "<center>");
    if (myState.limit && (numTasks > DEFAULT_LIMIT)) {
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
    if (myState.verbFilter != null) {
      out.print(" with verb ");
      out.print(myState.verbFilter);
    }
    out.print(" at ");
    out.print(myState.clusterID);
    out.print("</center>\n");
    if (myState.limit && (numTasks > DEFAULT_LIMIT)) {
      out.print("<center>");
      // link to all tasks.
      printLinkToAllTasks(
          myState, out,
          myState.verbFilter, 0, numTasks, true);
      out.print("</center>\n");
    }
    // print table headers
    out.print(
        "\n<table align=center border=1 cellpadding=1\n"+
        " cellspacing=1 width=75%\n"+
        " bordercolordark=#660000 bordercolorlight=#cc9966>\n"+
        "<tr>\n"+
        "<td colspan=7>"+
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
        printLinkToLocalTask(myState, out, task);
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
        printTaskDirectObjectTableRow(myState, out, task);
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
          out.print("</font>");
          printObject(out, pp.getIndirectObject());
          out.print(",");
        }
        out.print(
            "</font>"+
            "</td>\n"+
            "</tr>\n");
        if ((++rows % DEFAULT_LIMIT) == 0) {
          if (myState.limit) {
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
      if (myState.limit && (rows == DEFAULT_LIMIT)) {
        // link to unlimited view
        out.print(
            "<p>"+
            "<center>");
        printLinkToAllTasks(
            myState, out,
            myState.verbFilter, 0, numTasks, true);
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
      if (myState.verbFilter != null) {
        out.print(" with verb ");
        out.print(myState.verbFilter);
      }
      out.print(" found in ");
      out.print(myState.clusterID);
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
  protected static void displayTasksSummary(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay Tasks Summary");
    }
    // find tasks
    Collection col = findAllTasks(myState);
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
    out.print(myState.clusterID);
    out.print(
        " Tasks Summary"+
        "</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<center>");
    printLinkToAllTasks(
        myState, out,
        null, 0, numTasks, false);
    out.print("</center>\n");
    if (numTasks > DEFAULT_LIMIT) {
      // give limit option
      out.print("<center>");
      printLinkToAllTasks(
          myState, out,
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
            myState, out,
            info.verb.toString(), 0, info.counter, false);
        if (info.counter > DEFAULT_LIMIT) {
          // link to limited number of tasks with verb
          out.print(" (");
          printLinkToAllTasks(
              myState, out,
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
      out.print(myState.clusterID);
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
  protected static void displayPlanElementDetails(
      MyPSPState myState, PrintStream out)
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
    // link to cluster
    printLinkToTasksSummary(myState, out);
    out.print(
        "</b><br>\n");
    // find plan element
    UniqueObject baseObj =
      findUniqueObjectWithUID(myState, myState.itemUID);
    if (baseObj instanceof PlanElement) {
      printPlanElementDetails(myState, out, (PlanElement)baseObj);
    } else {
      out.print(
          "<p>"+
          "<font size=small color=mediumblue>");
      if (myState.itemUID == null) {
        out.print("No PlanElement selected.");
      } else if (baseObj == null) {
        out.print("No PlanElement matching \"");
        out.print(myState.itemUID);
        out.print("\" found.");
      } else {
        out.print("UniqueObject with UID \"");
        out.print(myState.itemUID);
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
  protected static void displayAllPlanElements(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay All PlanElements");
    }
    Collection col = findAllPlanElements(myState);
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
    out.print(myState.clusterID);
    out.print(
        " PlanElements"+
        "</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<center>");
    if (myState.limit && (numPlanElements > DEFAULT_LIMIT)) {
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
    out.print(myState.clusterID);
    out.print("</center>");
    if (myState.limit && (numPlanElements > DEFAULT_LIMIT)) {
      out.print("<center>");
      // link to all pes
      printLinkToAllPlanElements(
          myState, out,
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
        printLinkToPlanElement(myState, out, pe);
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
          if (myState.limit) {
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
      if (myState.limit && (rows == DEFAULT_LIMIT)) {
        // link to unlimited view
        out.print(
            "<p>"+
            "<center>");
        printLinkToAllPlanElements(
            myState, out,
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
      out.print(myState.clusterID);
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
  protected static void displayAssetDetails(
      MyPSPState myState, PrintStream out)
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
    // link to cluster
    printLinkToTasksSummary(myState, out);
    out.print(
        "</b><br>\n"+
        "Asset<br>");
    // find "base" UniqueObject with the specifed UID
    UniqueObject baseObj =
      findUniqueObjectWithUID(myState, myState.itemUID);
    Asset asset = null;
    // get asset
    switch (myState.mode) {
      case MyPSPState.MODE_ASSET_DETAILS:
        // asset itself
        if (baseObj instanceof Asset) {
          asset = (Asset)baseObj;
        }
        break;
      case MyPSPState.MODE_TASK_DIRECT_OBJECT_DETAILS:
        // asset attached to Task
        if (baseObj instanceof Task) {
          asset = ((Task)baseObj).getDirectObject();
        }
        break;
      case MyPSPState.MODE_ASSET_TRANSFER_ASSET_DETAILS:
        // asset attached to AssetTransfer
        if (baseObj instanceof AssetTransfer) {
          asset = ((AssetTransfer)baseObj).getAsset();
        }
        break;
      default:
        break;
    }
    if (asset != null) {
      printAssetDetails(myState, out, baseObj, asset);
    } else {
      String baseType;
      switch (myState.mode) {
        case MyPSPState.MODE_ASSET_DETAILS:
          baseType = "Asset";
          break;
        case MyPSPState.MODE_TASK_DIRECT_OBJECT_DETAILS:
          baseType = "Task";
          break;
        case MyPSPState.MODE_ASSET_TRANSFER_ASSET_DETAILS:
          baseType = "AssetTransfer";
          break;
        default:
          baseType = "<font color=red>Error</font>";
          break;
      }
      out.print(
          "<p>"+
          "<font size=small color=mediumblue>");
      if (myState.itemUID == null) {
        out.print("No ");
        out.print(baseType);
        out.print(" selected.");
      } else if (baseObj == null) {
        out.print("No ");
        out.print(baseType);
        out.print(" matching \"");
        out.print(myState.itemUID);
        out.print("\" found in ");
        out.print(myState.clusterID);
        out.print(".");
      } else {
        out.print("UniqueObject with UID \"");
        out.print(myState.itemUID);
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
  protected static void displayAllAssets(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay All Assets");
    }
    Collection col = findAllAssets(myState);
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
    out.print(myState.clusterID);
    out.print(
        " Assets"+
        "</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<center>");
    if (myState.limit && (numAssets > DEFAULT_LIMIT)) {
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
    out.print(myState.clusterID);
    out.print("</center>");
    if (myState.limit && (numAssets > DEFAULT_LIMIT)) {
      out.print("<center>");
      // link to all assets
      printLinkToAllAssets(
          myState, out,
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
        printAssetTableRow(myState, out, asset);
        out.print("</tr>\n");
        if ((++rows % DEFAULT_LIMIT) == 0) {
          // restart table
          if (myState.limit) {
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
      if (myState.limit && (rows == DEFAULT_LIMIT)) {
        // link to unlimited view
        out.print(
            "<p>"+
            "<center>");
        printLinkToAllAssets(
            myState, out,
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
      out.print(myState.clusterID);
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
   * the cluster _after_ page load and the action must point to the
   * correct cluster's PSP.
   */
  protected static void displaySearch(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay Form");
    }
    out.print(
        "<html>\n"+
        "<script language=\"JavaScript\">\n"+
        "<!--\n"+
        "function mySubmit() {\n"+
        "  var tidx = document.myForm.formCluster.selectedIndex\n"+
        "  var encCluster = document.myForm.formCluster.options[tidx].value\n"+
        "  var type = document.myForm.formType.selectedIndex\n"+
        "  var uid = trim(document.myForm."+
        MyPSPState.ITEM_UID+
        ".value)\n"+
        "  if (uid.length > 0) {\n"+
        "    document.myForm.target=\"itemFrame\"\n"+
        "    if (type == 0) {\n"+
        "      document.myForm."+
        MyPSPState.MODE+
        ".value= \""+
        MyPSPState.MODE_TASK_DETAILS+
        "\"\n"+
        "    } else if (type == 1) {\n"+
        "      document.myForm."+
        MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_PLAN_ELEMENT_DETAILS+
      "\"\n"+
      "    } else if (type == 2) {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_ASSET_DETAILS+
      "\"\n"+
      "    } else {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_XML_HTML_DETAILS+
      "\"\n"+
      "    }\n"+
      "    if (uid.charAt(0) == '/') {\n"+
      "      document.myForm."+
      MyPSPState.ITEM_UID+
      ".value = encCluster + uid\n"+
      "    }\n"+
      "  } else {\n"+
      "    document.myForm.target=\"tablesFrame\"\n"+
      "    if (type == 0) {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_TASKS_SUMMARY+
      "\"\n"+
      "    } else if (type == 1) {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_ALL_PLAN_ELEMENTS+
      "\"\n"+
      "    } else if (type == 2) {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_ALL_ASSETS+
      "\"\n"+
      "    } else {\n"+
      "      document.myForm."+
      MyPSPState.MODE+
      ".value= \""+
      MyPSPState.MODE_ALL_UNIQUE_OBJECTS+
      "\"\n"+
      "    }\n"+
      "  }\n"+
      "  document.myForm.action=\"/$\"+encCluster+\"");
    out.print(myState.psp_path);
    out.print("?POST\"\n"+
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
      "<head>\n"+
      "<title>Logplan Search</title>\n"+
      "</head>\n"+
      "<body bgcolor=\"#F0F0F0\">\n"+
      "<noscript>\n"+
      "<b>This page needs Javascript!</b><br>\n"+
      "Consult your browser's help pages..\n"+
      "<p><p><p>\n"+
      "</noscript>\n"+
      "<form name=\"myForm\" method=\"post\" onSubmit=\"return mySubmit()\">\n"+
      "<input type=\"hidden\" name=\""+
      MyPSPState.MODE+
      "\" value=\"fromJavaScript\">\n"+
      "<input type=\"hidden\" name=\""+
      MyPSPState.LIMIT+
      "\" value=\"true\">\n"+
      "<select name=\"formCluster\">\n");
    // lookup all known cluster names
    Vector names = new Vector();
    myState.psc.getAllNames(names, true);
    int sz = names.size();
    for (int i = 0; i < sz; i++) {
      String n = (String)names.elementAt(i);
      out.print("  <option ");
      if (n.equals(myState.clusterID)) {
        out.print("selected ");
      }
      out.print("value=\"");
      // javascript "encode(..)" not widely supported
      out.print(encode(n));
      out.print("\">");
      out.print(n);
      out.print("</option>\n");
    }
    out.print(
        "</select><br>\n"+
        "<select name=\"formType\">\n"+
        "  <option value=\"0\">Tasks</option>\n"+
        "  <option value=\"1\">PlanElements</option>\n"+
        "  <option value=\"2\">Assets</option>\n"+
        "  <option value=\"3\">UniqueObjects</option>\n"+
        "</select><br>\n"+
        "UID:<input type=\"text\" name=\""+
        // user should enter an encoded UID
        MyPSPState.ITEM_UID+
        "\" size=12><br>\n"+
        "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
        "<p>\n"+
        // link to advanced search
        "<a href=\"/$");
    out.print(myState.encodedClusterID);
    out.print(myState.psp_path);
    out.print(
        "?"+
        MyPSPState.MODE+
        "="+
        MyPSPState.MODE_ADVANCED_SEARCH_FORM+
        "\" target=\"advSearch\">Advanced search</a>"+
        "</form>\n"+
        "</body>\n"+
        "</html>\n");
  }

  /**
   * displayUniqueObjectDetails.
   */
  protected static void displayUniqueObjectDetails(
      MyPSPState myState, PrintStream out)
  {
    boolean asHTML;
    boolean isAttached;
    switch (myState.mode) {
      default:
        // error, but treat as "MODE_XML_HTML_DETAILS"
      case MyPSPState.MODE_XML_HTML_DETAILS:
        asHTML = true;
        isAttached = false;
        break;
      case MyPSPState.MODE_XML_HTML_ATTACHED_DETAILS:
        asHTML = true;
        isAttached = true;
        break;
      case MyPSPState.MODE_XML_RAW_DETAILS:
        asHTML = false;
        isAttached = false;
        break;
      case MyPSPState.MODE_XML_RAW_ATTACHED_DETAILS:
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
      findUniqueObjectWithUID(myState, myState.itemUID);
    // get the attached object
    Object attachedObj;
    if (isAttached) {
      // examine baseObj to find attached XMLizable
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
    // cast to XMLizable
    XMLizable xo =
      ((attachedObj instanceof XMLizable) ?
       (XMLizable)attachedObj :
       null);
    if (asHTML) {
      // print as HTML
      out.print("<html>\n<head>\n<title>");
      out.print(myState.itemUID);
      out.print(
          " View</title>"+
          "</head>\n<body bgcolor=\"#F0F0F0\">\n<b>");
      // link to cluster
      printLinkToTasksSummary(myState, out);
      out.print(
          "</b><br>\n"+
          "UniqueObject<br>");
      if (xo != null) {
        // link to non-html view of object
        out.print("<p>");
        printLinkToXML(myState, out, xo, false);
        out.print("<br><hr><br><pre>\n");
        // print HTML-wrapped XML
        printXMLizableDetails(myState, out, xo, true);
        out.print("\n</pre><br><hr><br>\n");
      } else {
        out.print("<p><font size=small color=mediumblue>");
        if (myState.itemUID == null) {
          out.print("No UniqueObject selected.");
        } else if (baseObj == null) {
          out.print("No UniqueObject matching \"");
          out.print(myState.itemUID);
          out.print("\" found.");
        } else if (attachedObj == null) {
          out.print("UniqueObject with UID \"");
          out.print(myState.itemUID);
          out.print("\" of type ");
          out.print(baseObj.getClass().getName());
          out.print(" has null attached Object.");
        } else {
          out.print("UniqueObject with UID \"");
          out.print(myState.itemUID);
          out.print("\" of type ");
          out.print(baseObj.getClass().getName());
          out.print(" has non-XMLizable attached Object: ");
          out.print(attachedObj.getClass().getName());
        }
        out.print("</font><p>\n");
      }
      out.print("</body></html>\n");
    } else {
      // print raw XML
      printXMLizableDetails(myState, out, xo, false);
    }
    out.flush();
  }

  /**
   * displayAllUniqueObjects.
   */
  protected static void displayAllUniqueObjects(
      MyPSPState myState, PrintStream out)
  {
    if (DEBUG) {
      System.out.println("\nDisplay All UniqueObjects");
    }
    Collection col = findAllUniqueObjects(myState);
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
    out.print(myState.clusterID);
    out.print(
        " UniqueObjects"+
        "</title>\n"+
        "</head>\n"+
        "<body bgcolor=\"#F0F0F0\">\n"+
        "<center>");
    if (myState.limit && (numUniqueObjects > DEFAULT_LIMIT)) {
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
    out.print(myState.clusterID);
    out.print("</center>\n");
    if (myState.limit && (numUniqueObjects > DEFAULT_LIMIT)) {
      out.print("<center>");
      // link to all uniqueObjects.
      printLinkToAllUniqueObjects(
          myState, out,
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
        "<td><font color=mediumblue><b>UID</font></b></td>\n"+
        "<td><font color=mediumblue><b>Type</font></b></td>\n"+
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
            printLinkToPlanElement(myState, out, (PlanElement)uo);
            break;
          case ITEM_TYPE_TASK:
            printLinkToLocalTask(myState, out, (Task)uo);
            break;
          case ITEM_TYPE_ASSET:
            // found this asset in local LogPlan
            printLinkToLocalAsset(myState, out, (Asset)uo);
            break;
          case ITEM_TYPE_WORKFLOW:
          default:
            if (uo instanceof XMLizable) {
              // XMLizable and a local UniqueObject
              printLinkToXML(myState, out, uo, true);
            } else {
              out.print("<font color=red>No XML for ");
              UID uoU;
              String uoUID;
              if (((uoU = uo.getUID()) != null) &&
                  ((uoUID = uoU.toString()) != null)) {
                out.print(uoUID);
              } else {
                out.print("null");
              }
              out.print("</font>");
            }
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
          if (myState.limit) {
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
      if (myState.limit && (rows == DEFAULT_LIMIT)) {
        // link to unlimited view
        out.print(
            "<p>"+
            "<center>");
        printLinkToAllUniqueObjects(
            myState, out,
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
      out.print(myState.clusterID);
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
      String fname = System.getProperty("org.cougaar.lib.psp.preds");
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

  protected static void displayAdvancedSearchForm(
      MyPSPState myState, PrintStream out) {
    if (DEBUG) {
      System.out.println("\nDisplay Advanced Search Form");
    }
    out.print(
        "<html>\n"+
        "<script language=\"JavaScript\">\n"+
        "<!--\n"+
        "function mySubmit() {\n"+
        "  var tidx = document.myForm.formCluster.selectedIndex\n"+
        "  var encCluster = document.myForm.formCluster.options[tidx].value\n"+
        "  document.myForm.action=\"/$\"+encCluster+\"");
    out.print(myState.psp_path);
    out.print(
        "?POST\"\n"+
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
        "  document.myForm."+
        MyPSPState.PREDICATE_STYLE+
        ".selectedIndex=0\n"+
        "  document.myForm.pred.value=s\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "<head>\n"+
        "<title>");
    out.print(myState.clusterID);
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
        "<form name=\"myForm\" method=\"post\" "+
        "target=\"predResults\" onSubmit=\"return mySubmit()\">\n"+
        "Search cluster <select name=\"formCluster\">\n");
    // lookup all known cluster names
    Vector names = new Vector();
    myState.psc.getAllNames(names, true);
    int sz = names.size();
    for (int i = 0; i < sz; i++) {
      String n = (String)names.elementAt(i);
      out.print("  <option ");
      if (n.equals(myState.clusterID)) {
        out.print("selected ");
      }
      out.print("value=\"");
      // javascript "encode(..)" not widely supported
      out.print(encode(n));
      out.print("\">");
      out.print(n);
      out.print("</option>\n");
    }
    out.print("</select><br>\n");
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
        MyPSPState.LIMIT+
        "\" value=\"true\" checked>"+
        "limit to "+
        DEFAULT_LIMIT+
        " matches<br>\n"+
        "<input type=\"submit\" name=\"formSubmit\" value=\"Search\"><br>\n"+
        "<p><hr>\n"+
        "Style: <select name=\""+
        MyPSPState.PREDICATE_STYLE+
        "\">\n"+
        "<option selected>Lisp format</option>\n"+
        "<option>XML format</option>\n"+
        "</select>,&nbsp;\n"+
        "<input type=\"checkbox\" name=\""+
        MyPSPState.PREDICATE_DEBUG+
        "\" value=\"true\">View parsed predicate<br>\n"+
        "<textarea name=\""+
        MyPSPState.PREDICATE+
        "\" rows=15 cols=70>\n"+
        "</textarea><br>\n"+
        "<input type=\"hidden\" name=\""+
        MyPSPState.MODE+
        "\" value=\""+
        MyPSPState.MODE_ADVANCED_SEARCH_RESULTS+
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

  protected static void displayAdvancedSearchResults(
      MyPSPState myState, PrintStream out) {
    if (DEBUG) {
      System.out.println("\nDisplay Advanced Search Results");
    }

    String inputPred = myState.pred;
    int inputStyle =
      (Operator.PRETTY_FLAG |
       Operator.VERBOSE_FLAG |
       (((myState.predStyle == null) ||
         (!("xml".regionMatches(true, 0, myState.predStyle, 0, 3)))) ?
        Operator.PAREN_FLAG :
        Operator.XML_FLAG));

    out.print("<html><head><title>");
    out.print(myState.clusterID);
    out.print(
      " Advanced Search Results</title><head>\n"+
      "<body bgcolor=\"#F0F0F0\"><p>\n"+
      "Search <b>");
    out.print(myState.clusterID);
    out.print("</b> using ");
    out.print(
      (((inputStyle & Operator.PAREN_FLAG) != 0) ?
       "Lisp" : "XML"));
    out.print("-style predicate: <br><pre>\n");
    out.print(inputPred);
    out.print("</pre><p>\n<hr><br>\n");

    // get an instance of the default operator factory
    OperatorFactory operFactory = OperatorFactory.getInstance();

    // parse the input to create a unary predicate
    Operator parsedPred;
    try {
      parsedPred = operFactory.create(inputStyle, inputPred);
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

    if (myState.predDebug) {
      // this is useful in general, but clutters the screen...
      out.print("Parsed as:<pre>\n");
      out.print(parsedPred.toString(inputStyle));
      out.print("</pre><br><hr><br>\n");
    }

    Collection col = searchUsingPredicate(myState, parsedPred);
    int numObjects = col.size();
    Iterator oIter = col.iterator();
    if (DEBUG) {
      System.out.println("Fetched Matching Objects["+numObjects+"]");
    }
    out.print(
        "<b>Note:</b> "+
        "links below will appear in TASK.PSP's lower-left \"details\" "+
        "frame<p>"+
        "<center>");
    if (myState.limit && (numObjects > DEFAULT_LIMIT)) {
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
    out.print(myState.clusterID);
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
            printLinkToPlanElement(myState, out, (PlanElement)o);
            break;
          case ITEM_TYPE_TASK:
            printLinkToLocalTask(myState, out, (Task)o);
            break;
          case ITEM_TYPE_ASSET:
            // found this asset in local LogPlan
            printLinkToLocalAsset(myState, out, (Asset)o);
            break;
          case ITEM_TYPE_WORKFLOW:
          default:
            if (o instanceof XMLizable) {
              // XMLizable and a local UniqueObject
              printLinkToXML(myState, out, (XMLizable)o, true);
            } else {
              out.print("<font color=red>No XML for ");
              UID uoU;
              String uoUID;
              if (o instanceof UniqueObject) {
                if (((uoU = ((UniqueObject)o).getUID()) != null) &&
                    ((uoUID = uoU.toString()) != null)) {
                  out.print(uoUID);
                } else {
                  out.print("null-UID");
                }
              } else if (o != null) {
                out.print("non-UniqueObject");
              } else {
                out.print("null");
              }
              out.print("</font>");
            }
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
          if (myState.limit) {
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
      out.print(myState.clusterID);
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

  /** END DISPLAY ROUTINES **/

  /** BEGIN PRINT ROUTINES **/

  /**
   * printTaskDetails.
   *
   * Includes support for printing early-best-latest dates
   * for END_TIMEs with VScoringFunctions.
   *
   */
  protected static void printTaskDetails(
      MyPSPState myState, PrintStream out, Task task)
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
        "</li>\n"+
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
    printLinkToTaskDirectObject(myState, out, task);
    out.print(
        "</font>"+
        "</li>\n"+
        "<li>"+
        "<font size=small color=mediumblue>"+
        "PlanElement= ");
    // link to plan element
    //
    // ancient note:
    // obtaining PE via .getPlanElement() is not totally kosher --
    // however, since UI is user-invoked, by the time user gets around to
    // asking -- PE's should have already been
    // set for all Tasks if society reached quiesence.. if not -- there
    // are no guarantees.
    //
    PlanElement pe = task.getPlanElement();
    printLinkToPlanElement(myState, out, pe);
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
        printLinkToLocalTask(myState, out, pt);
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
      printLinkToParentTask(myState, out, task);
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
          printObject(out, indObj);
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
        "</li>\n");
    out.print("</ul>\n");
    // link to XML view
    out.print("<font size=small color=mediumblue>");
    // this task is local
    printLinkToXML(myState, out, task, true);
    out.print("</font>");
  }

  /**
   * Part of support for printing early-best-latest dates
   * for END_TIMEs with VScoringFunctions.
   */
  public static long getEarlyDate(ScoringFunction vsf) {
    Enumeration validRanges = getValidEndDateRanges(vsf);
    while (validRanges.hasMoreElements()) {
      AspectScoreRange range = (AspectScoreRange)validRanges.nextElement();
      return ((AspectScorePoint)range.getRangeStartPoint()).getAspectValue().longValue();
    }
    // should be TimeSpan.MIN_VALUE!
    return 0;
  }

  /**
   * Part of support for printing early-best-latest dates
   * for END_TIMEs with VScoringFunctions.
   */
  public static long getLateDate(ScoringFunction vsf) {
    Enumeration validRanges = getValidEndDateRanges(vsf);
    while (validRanges.hasMoreElements()) {
      AspectScoreRange range = (AspectScoreRange)validRanges.nextElement();
      if (!validRanges.hasMoreElements())
        return ((AspectScorePoint)range.getRangeEndPoint()).getAspectValue().longValue();
    }
    return TimeSpan.MAX_VALUE;
  }

  /* Needed for support of printing early-best-latest END_TIMEs */
  protected static Calendar cal = java.util.Calendar.getInstance();

  /* Needed for support of printing early-best-latest END_TIMEs */
  protected static Date endOfRange;
  static {
    cal.set(2200, 0, 0, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    endOfRange = (Date) cal.getTime();
  };

  /**
   * Part of support for printing early-best-latest dates
   * for END_TIMEs with VScoringFunctions.
   */
  protected static Enumeration getValidEndDateRanges(ScoringFunction sf) {
    Enumeration validRanges =
      sf.getValidRanges(
          new TimeAspectValue(AspectType.END_TIME, 0l),
          new TimeAspectValue(AspectType.END_TIME, endOfRange));
    return validRanges;
  }

  /**
   * printPlanElementDetails.
   *
   * PlanElements are always in the LogPlan and have UIDs, so we
   * don't need a "baseObj" (e.g. the Task that this PlanElement
   * is attached to).
   */
  protected static void printPlanElementDetails(
      MyPSPState myState, PrintStream out, PlanElement pe)
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
    // show task
    out.print(
        "<li>"+
        "<font size=small color=mediumblue>"+
        "Task= ");
    printLinkToLocalTask(myState, out, pe.getTask());
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
      printAllocationResultDetails(myState, out, ar);
      out.print(
          "</li>\n");
    }
    if ((ar = pe.getReportedResult()) != null) {
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Reported</font>");
      printAllocationResultDetails(myState, out, ar);
      out.print(
          "</li>\n");
    }
    if ((ar = pe.getReceivedResult()) != null) {
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Received</font>");
      printAllocationResultDetails(myState, out, ar);
      out.print(
          "</li>\n");
    }
    if ((ar = pe.getObservedResult()) != null) {
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Observed</font>");
      printAllocationResultDetails(myState, out, ar);
      out.print(
          "</li>\n");
    }
    out.print(
        "</ul>"+
        "</li>\n");
    // show PE subclass information
    switch (peType) {
      case ITEM_TYPE_ALLOCATION:
        printAllocationDetails(myState, out, (Allocation)pe);
        break;
      case ITEM_TYPE_EXPANSION:
        printExpansionDetails(myState, out, (Expansion)pe);
        break;
      case ITEM_TYPE_AGGREGATION:
        printAggregationDetails(myState, out, (Aggregation)pe);
        break;
      case ITEM_TYPE_DISPOSITION:
        printDispositionDetails(myState, out, (Disposition)pe);
        break;
      case ITEM_TYPE_ASSET_TRANSFER:
        printAssetTransferDetails(myState, out, (AssetTransfer)pe);
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
    printLinkToXML(myState, out, pe, true);
    out.print("</font>");
  }

  /**
   * printAllocationResultDetails.
   */
  protected static void printAllocationResultDetails(
      MyPSPState myState, PrintStream out, AllocationResult ar)
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
    int[] arTypes = ar.getAspectTypes();
    double[] arResults = ar.getResult();
    for (int i = 0; i < arTypes.length; i++) {
      out.print(
          "<font size=small color=mediumblue>"+
          "<li>");
      // show type
      int arti = arTypes[i];
      out.print(AspectValue.aspectTypeToString(arti));
      out.print("= ");
      // show value
      double arri = arResults[i];
      switch (arti) {
        case AspectType.START_TIME:
        case AspectType.END_TIME:
        case AspectType.POD_DATE:
          // date
          out.print(
              getTimeString((long)arri));
          break;
        default:
          // other
          out.print(arri);
          break;
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
  protected static void printAllocationDetails(
      MyPSPState myState, PrintStream out, Allocation ac)
  {
    // show asset
    Asset asset = ac.getAsset();
    if (asset != null) {
      // link to allocated asset
      ClusterPG clusterPG = asset.getClusterPG();
      ClusterIdentifier clusterID;
      String remoteClusterID =
        ((((clusterPG = asset.getClusterPG()) != null) &&
          ((clusterID = clusterPG.getClusterIdentifier()) != null)) ?
         clusterID.toString() :
         null);
      boolean isRemoteCluster = (remoteClusterID != null);
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>");
      out.print(isRemoteCluster ? "Cluster" : "Asset");
      out.print("= ");
      // allocations are always to an asset in the local LogPlan
      printLinkToLocalAsset(myState, out, asset);
      out.print(
          "</font>"+
          "</li>\n");
      if (isRemoteCluster) {
        // link to task in other cluster
        Task allocTask = ((AllocationforCollections)ac).getAllocationTask();
        out.print(
            "<li>"+
            "<font size=small color=mediumblue>"+
            "AllocTask= ");
        printLinkToTask(
            myState, out,
            allocTask,
            remoteClusterID, encode(remoteClusterID));
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
  protected static void printExpansionDetails(
      MyPSPState myState, PrintStream out, Expansion ex)
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
      printLinkToLocalTask(myState, out, tsk);
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
  protected static void printAggregationDetails(
      MyPSPState myState, PrintStream out, Aggregation agg)
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
      printLinkToLocalTask(myState, out, compTask);
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
  protected static void printDispositionDetails(
      MyPSPState myState, PrintStream out, Disposition d)
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
  protected static void printAssetTransferDetails(
      MyPSPState myState, PrintStream out, AssetTransfer atrans)
  {
    // show attached asset
    out.print(
        "<li>"+
        "<font size=small color=mediumblue>"+
        "Asset= ");
    printLinkToAssetTransferAsset(myState, out, atrans);
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
    ClusterIdentifier assignor = atrans.getAssignor();
    if (assignor != null) {
      String name = assignor.cleanToString();
      out.print(
          "<li>"+
          "<font size=small color=mediumblue>"+
          "Assignor= ");
      printLinkToTasksSummary(myState, out, name, encode(name));
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
      // assignee asset is always in the local LogPlan
      printLinkToLocalAsset(myState, out, assignee);
      out.print(
          "</font>"+
          "</li>\n");
    }
  }

  // ================== Stuff for MicroAgent linkage =================== //

  private static IncrementalSubscription porttableSub = null;
  /**
   * printLinkToMicroAgent.
   */
  protected static void printLinkToMicroAgent(MyPSPState myState, PrintStream out, MicroAgent asset)
  {
    String linkstring = null;
    int wsport = 0;

    MicroAgentPG mapg = asset.getMicroAgentPG();

    Iterator iter = porttableSub.getCollection().iterator();
    if (iter.hasNext())
    {
      NameTablePair webserverports = (NameTablePair)iter.next();
      if(webserverports.table.containsKey(mapg.getName()))
      {
        Integer temp = (Integer)webserverports.table.get(mapg.getName());
        wsport = temp.intValue();
        linkstring =
           "<a href=http://"+mapg.getIpAddress()+":"+wsport+"/ target=MEBlackboardView>" + mapg.getName() + "</a>";
      }
    }

    out.print("<li>"+"<font size=small color=mediumblue> ME Blackboard View: "+linkstring+"</font>"+"</li>");

  }

  /**
   * printAssetDetails.
   */
  protected static void printAssetDetails(
      MyPSPState myState, PrintStream out,
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
          printAssetDetails(myState, out, null, as);
        } else {
          out.print("<font color=red>null</font>");
        }
        out.print("\n</li>\n");
      }
      out.print("</ol>\n");
      if (baseObj != null) {
        if (asset instanceof XMLizable) {
          // link to HTML-encoded XML view
          out.print("<font size=small color=mediumblue>");
          printLinkToAttachedXML(
              myState,
              out,
              baseObj,
              (XMLizable)asset,
              true);
          out.print("</font>");
        } else {
          // asset not XMLizable
          out.print("<font color=red>Asset not XMLable</font>");
        }
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

    // this is for micro-cougaar debugging capability
    if(asset instanceof MicroAgent)
    {
      printLinkToMicroAgent(myState, out, (MicroAgent)asset);
    }

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
          printLinkToPlanElement(myState, out, pe);
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
    // PGs?
    out.print("</ul>");
    if (baseObj != null) {
      // provide XML view
      if (asset instanceof XMLizable) {
        // link to HTML-encoded XML view
        out.print("<font size=small color=mediumblue>");
        printLinkToAttachedXML(
            myState,
            out,
            baseObj,
            (XMLizable)asset,
            true);
        out.print("</font>");
      } else {
        // asset not XMLizable
        out.print("<font color=red>Asset not XMLable</font>");
      }
    } else {
      // likely recursed on an AssetGroup, and the top-level group
      //   had a "View XML" link.
    }
  }

  /**
   * printAssetTableRow.
   *
   * Asset that is in the local LogPlan and has a UID.  Treat this
   * as an Asset attached to itself.
   */
  protected static void printAssetTableRow(
      MyPSPState myState, PrintStream out, Asset asset)
  {
    printAttachedAssetTableRow(
        myState,
        out,
        asset,
        asset,
        MyPSPState.MODE_ASSET_DETAILS);
  }

  /**
   * printTaskDirectObjectTableRow.
   */
  protected static void printTaskDirectObjectTableRow(
      MyPSPState myState, PrintStream out, Task task)
  {
    printAttachedAssetTableRow(
        myState,
        out,
        task,
        ((task != null) ? task.getDirectObject() : null),
        MyPSPState.MODE_TASK_DIRECT_OBJECT_DETAILS);
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
  protected static void printAttachedAssetTableRow(
      MyPSPState myState, PrintStream out,
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
      printLinkToAttachedAsset(myState, out, baseObj, asset, baseMode);
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
      printLinkToAttachedAsset(myState, out, baseObj, asset, baseMode);
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
   * printXMLizableDetails.
   * <p>
   * Prints XML for given XMLizable Object.
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
  protected static void printXMLizableDetails(
      MyPSPState myState, PrintStream out,
      XMLizable xo, boolean printAsHTML)
  {
    try {
      // convert to XML
      Document doc = new DocumentImpl();
      Element element = xo.getXML((Document)doc);
      doc.appendChild(element);

      OutputFormat format = new OutputFormat();
      format.setPreserveSpace(true);
      format.setIndent(2);

      // print to output
      if (printAsHTML) {
        PrintWriter pout = new PrintWriter(new XMLtoHTMLOutputStream(out));
        XMLSerializer serializer = new XMLSerializer(pout, format);
        out.print("<pre>\n");
        serializer.serialize(doc);
        out.print("\n</pre>\n");
        pout.flush();
      } else {
        PrintWriter pout = new PrintWriter(out);
        XMLSerializer serializer = new XMLSerializer(pout, format);
        serializer.serialize(doc);
        pout.flush();
      }
    } catch (Exception e) {
      if (printAsHTML) {
        out.print("\nException!\n\n");
        e.printStackTrace(out);
      }
    }
  }

  /** END PRINT ROUTINES **/

  /** BEGIN PRINTLINK ROUTINES **/

  /**
   * print link to task summary at this cluster.
   */
  protected static void printLinkToTasksSummary(
      MyPSPState myState, PrintStream out)
  {
    printLinkToTasksSummary(
        myState, out, myState.clusterID, myState.encodedClusterID);
  }

  /**
   * print link to task summary for given cluster
   *
   * @param encodedClusterID the result of encode(clusterID)
   */
  protected static void printLinkToTasksSummary(
      MyPSPState myState, PrintStream out,
      String clusterID, String encodedClusterID)
  {
    if (clusterID != null) {
      out.print("<a href=\"/$");
      // link to cluster
      out.print(encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_TASKS_SUMMARY);
      out.print("\" target=\"tablesFrame\">");
      out.print(clusterID);
      out.print(
          "</a>");
    } else {
      out.print("<font color=red>Unknown cluster</font>");
    }
  }

  /** simple flags for parameter checking **/
  protected static final byte _FLAG_LIMIT   = (1 << 0);
  protected static final byte _FLAG_VERB    = (1 << 1);
  protected static final byte _FLAG_VERBOSE = (1 << 2);

  /**
   * printLinkToAllTasks for the local cluster.
   */
  protected static void printLinkToAllTasks(
      MyPSPState myState, PrintStream out,
      String verb, int limit, int numTasks, boolean verbose)
  {
    printLinkToAllTasks(
        myState, out,
        myState.clusterID, myState.encodedClusterID,
        verb, limit, numTasks, verbose);
  }

  /**
   * printLinkToAllTasks.
   */
  protected static void printLinkToAllTasks(
      MyPSPState myState, PrintStream out,
      String clusterID, String encodedClusterID,
      String verb, int limit, int numTasks, boolean verbose)
  {
    if (clusterID != null) {
      out.print("<a href=\"/$");
      out.print(encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_ALL_TASKS);
      // set flags
      byte flags = 0;
      if (limit > 0) {
        out.print(
            "?"+
            MyPSPState.LIMIT);
        flags |= _FLAG_LIMIT;
      }
      if (verb != null) {
        out.print(
            "?"+
            MyPSPState.VERB+
            "=");
        out.print(verb);
        flags |= _FLAG_VERB;
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
          out.print(numTasks);
          out.print("</b> Tasks at ");
          out.print(clusterID);
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
          out.print(clusterID);
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
          out.print(clusterID);
          break;
        default:
        case (0):
        case (_FLAG_VERBOSE):
          out.print("View all <b>");
          out.print(numTasks);
          out.print("</b> Tasks at ");
          out.print(clusterID);
          break;
      }
      out.print("</a>");
    } else {
      out.print("<font color=red>Unknown cluster</font>");
    }
  }

  /**
   * printLinkToAllPlanElements for the local cluster.
   */
  protected static void printLinkToAllPlanElements(
      MyPSPState myState, PrintStream out,
      int limit, int numPlanElements, boolean verbose)
  {
    printLinkToAllPlanElements(
        myState, out,
        myState.clusterID, myState.encodedClusterID,
        limit, numPlanElements, verbose);
  }

  /**
   * printLinkToAllPlanElements.
   */
  protected static void printLinkToAllPlanElements(
      MyPSPState myState, PrintStream out,
      String clusterID, String encodedClusterID,
      int limit, int numPlanElements, boolean verbose)
  {
    if (clusterID != null) {
      out.print("<a href=\"/$");
      out.print(encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_ALL_PLAN_ELEMENTS);
      // set flags
      byte flags = 0;
      if (limit > 0) {
        out.print(
            "?"+
            MyPSPState.LIMIT);
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
          out.print(clusterID);
          break;
        default:
        case (0):
        case (_FLAG_VERBOSE):
          out.print("View all <b>");
          out.print(numPlanElements);
          out.print("</b> PlanElements at ");
          out.print(clusterID);
          break;
      }
      out.print("</a>");
    } else {
      out.print("<font color=red>Unknown cluster</font>");
    }
  }

  /**
   * printLinkToAllAssets for the local cluster.
   */
  protected static void printLinkToAllAssets(
      MyPSPState myState, PrintStream out,
      int limit, int numAssets, boolean verbose)
  {
    printLinkToAllAssets(
        myState, out,
        myState.clusterID, myState.encodedClusterID,
        limit, numAssets, verbose);
  }

  /**
   * printLinkToAllAssets.
   */
  protected static void printLinkToAllAssets(
      MyPSPState myState, PrintStream out,
      String clusterID, String encodedClusterID,
      int limit, int numAssets, boolean verbose)
  {
    if (clusterID != null) {
      out.print("<a href=\"/$");
      out.print(encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_ALL_ASSETS);
      // set flags
      byte flags = 0;
      if (limit > 0) {
        out.print(
            "?"+
            MyPSPState.LIMIT);
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
          out.print(clusterID);
          break;
        default:
        case (0):
        case (_FLAG_VERBOSE):
          out.print("View all <b>");
          out.print(numAssets);
          out.print("</b> Assets at ");
          out.print(clusterID);
          break;
      }
      out.print("</a>");
    } else {
      out.print("<font color=red>Unknown cluster</font>");
    }
  }

  /**
   * printLinkToAllUniqueObjects for the local cluster.
   */
  protected static void printLinkToAllUniqueObjects(
      MyPSPState myState, PrintStream out,
      int limit, int numUniqueObjects, boolean verbose)
  {
    printLinkToAllUniqueObjects(
        myState, out,
        myState.clusterID, myState.encodedClusterID,
        limit, numUniqueObjects, verbose);
  }

  /**
   * printLinkToAllUniqueObjects.
   */
  protected static void printLinkToAllUniqueObjects(
      MyPSPState myState, PrintStream out,
      String clusterID, String encodedClusterID,
      int limit, int numUniqueObjects, boolean verbose)
  {
    if (clusterID != null) {
      out.print("<a href=\"/$");
      out.print(encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_ALL_UNIQUE_OBJECTS);
      // set flags
      byte flags = 0;
      if (limit > 0) {
        out.print(
            "?"+
            MyPSPState.LIMIT);
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
          out.print(clusterID);
          break;
        default:
        case (0):
        case (_FLAG_VERBOSE):
          out.print("View all <b>");
          out.print(numUniqueObjects);
          out.print("</b> UniqueObjects at ");
          out.print(clusterID);
          break;
      }
      out.print("</a>");
    } else {
      out.print("<font color=red>Unknown cluster</font>");
    }
  }

  /**
   * printLinkToParentTask.
   * <p>
   * Get task's parent before linking.
   */
  protected static void printLinkToParentTask(
      MyPSPState myState, PrintStream out, Task task)
  {
    UID ptU;
    String ptUID;
    if (task == null) {
      out.print("<font color=red>null</font>");
    } else if (((ptU = task.getParentTaskUID()) == null) ||
        ((ptUID = ptU.toString()) == null)) {
      out.print("<font color=red>parent not unique</font>");
    } else {
      ClusterIdentifier tClusterID = task.getSource();
      String ptEncodedCluster;
      if ((tClusterID == null) ||
          ((ptEncodedCluster = tClusterID.toString()) == null)) {
        ptEncodedCluster = myState.encodedClusterID;
      } else {
        ptEncodedCluster = encode(ptEncodedCluster);
      }
      out.print("<a href=\"/$");
      out.print(ptEncodedCluster);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_TASK_DETAILS+
          "?"+
          MyPSPState.ITEM_UID+
          "=");
      out.print(encode(ptUID));
      out.print("\" target=\"itemFrame\">");
      out.print(ptUID);
      out.print("</a>");
    }
  }

  /**
   * printLinkToLocalTask.
   * <p>
   * Tasks that stay in the current cluster.
   */
  protected static void printLinkToLocalTask(
      MyPSPState myState, PrintStream out, Task task)
  {
    printLinkToTask(
        myState, out,
        task,
        myState.clusterID, myState.encodedClusterID);
  }

  /**
   * printLinkToTask.
   * <p>
   * This method attempts to works around task forwarding across
   * clusters in the "Down" sense, i.e. allocations.
   *
   * @param atCluster cluster name where this task resides
   */
  protected static void printLinkToTask(
      MyPSPState myState, PrintStream out,
      Task task,
      String atCluster, String atEncodedCluster)
  {
    UID taskU;
    String taskUID;
    if (task == null) {
      out.print("<font color=red>null</font>");
    } else if (((taskU = task.getUID()) == null) ||
        ((taskUID = taskU.toString()) == null)) {
      out.print("<font color=red>not unique</font>");
    } else {
      out.print("<a href=\"/$");
      out.print(atEncodedCluster);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_TASK_DETAILS+
          "?"+
          MyPSPState.ITEM_UID+
          "=");
      out.print(encode(taskUID));
      out.print("\" target=\"itemFrame\">");
      out.print(taskUID);
      out.print("</a>");
    }
  }

  /**
   * printLinkToPlanElement.
   * <p>
   * PlanElements stay in their cluster
   */
  protected static void printLinkToPlanElement(
      MyPSPState myState, PrintStream out, PlanElement pe)
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
      out.print(myState.encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          MyPSPState.MODE_PLAN_ELEMENT_DETAILS+
          "?"+
          MyPSPState.ITEM_UID+
          "=");
      out.print(encode(peUID));
      out.print("\" target=\"itemFrame\">");
      out.print(peUID);
      out.print("</a>");
    }
  }

  /**
   * printLinkToLocalAsset.
   * <p>
   * Asset that is in the local LogPlan and has a UID.  Treat this
   * as an Asset attached to itself.
   **/
  protected static void printLinkToLocalAsset(
      MyPSPState myState, PrintStream out, Asset asset)
  {
    printLinkToAttachedAsset(
        myState,
        out,
        asset,
        asset,
        MyPSPState.MODE_ASSET_DETAILS);
  }

  /**
   * printLinkToTaskDirectObject.
   **/
  protected static void printLinkToTaskDirectObject(
      MyPSPState myState, PrintStream out, Task task)
  {
    printLinkToAttachedAsset(
        myState,
        out,
        task,
        ((task != null) ? task.getDirectObject() : null),
        MyPSPState.MODE_TASK_DIRECT_OBJECT_DETAILS);
  }

  /**
   * printLinkToAssetTransferAsset.
   **/
  protected static void printLinkToAssetTransferAsset(
      MyPSPState myState, PrintStream out, AssetTransfer atrans)
  {
    printLinkToAttachedAsset(
        myState,
        out,
        atrans,
        ((atrans != null) ? atrans.getAsset() : null),
        MyPSPState.MODE_ASSET_TRANSFER_ASSET_DETAILS);
  }

  /**
   * printLinkToAttachedAsset.
   *
   * @see #printLinkToTaskDirectObject
   * @see #printLinkToAssetTransferAsset
   **/
  protected static void printLinkToAttachedAsset(
      MyPSPState myState, PrintStream out,
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
      out.print(myState.encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "="+
          baseMode+
          "?"+
          MyPSPState.ITEM_UID+
          "=");
      out.print(encode(baseObjUID));
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
   * XML objects stay in cluster.
   **/
  protected static void printLinkToXML(
      MyPSPState myState, PrintStream out,
      UniqueObject uo, boolean asHTML)
  {
    if (uo instanceof XMLizable) {
      // link to HTML-encoded XML view
      printLinkToAttachedXML(
          myState,
          out,
          uo,
          (XMLizable)uo,
          asHTML);
    } else if (uo == null) {
      out.print("<font color=red>null</font>");
    } else {
      // uniqueObject not XMLizable
      out.print("<font color=red>No XML for ");
      UID uoU;
      String uoUID;
      if (((uoU = uo.getUID()) != null) &&
          ((uoUID = uoU.toString()) != null)) {
        out.print(uoUID);
      } else {
        out.print("null");
      }
      out.print("</font>");
    }
  }

  /**
   * printLinkToXML.
   * <p>
   * XML objects stay in cluster.
   **/
  protected static void printLinkToXML(
      MyPSPState myState, PrintStream out,
      XMLizable xo, boolean asHTML)
  {
    if (xo instanceof UniqueObject) {
      // link to HTML-encoded XML view
      printLinkToAttachedXML(
          myState,
          out,
          (UniqueObject)xo,
          xo,
          asHTML);
    } else if (xo == null) {
      out.print("<font color=red>null</font>");
    } else {
      // asset not XMLizable
      out.print("<font color=red>");
      out.print(xo.getClass().getName());
      out.print(" not a UniqueObject</font>");
    }
  }

  /**
   * printLinkToAttachedXML.
   **/
  protected static void printLinkToAttachedXML(
      MyPSPState myState, PrintStream out,
      UniqueObject baseObj, XMLizable xo,
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
      out.print(myState.encodedClusterID);
      out.print(myState.psp_path);
      out.print(
          "?"+
          MyPSPState.MODE+
          "=");
      int mode =
        ((xo == baseObj) ?
         (asHTML ?
           MyPSPState.MODE_XML_HTML_DETAILS :
           MyPSPState.MODE_XML_RAW_DETAILS) :
         (asHTML ?
           MyPSPState.MODE_XML_HTML_ATTACHED_DETAILS :
           MyPSPState.MODE_XML_RAW_ATTACHED_DETAILS));
      out.print(mode);
      out.print(
          "?"+
          MyPSPState.ITEM_UID+
          "=");
      out.print(encode(baseObjUID));
      out.print("\" target=\"xml_");
      out.print(baseObjUID);
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

  /** END PRINTLINK ROUTINES **/

  /** BEGIN UTILITY PARSERS **/

  /**
   * printObject.
   * <p>
   * Currently used to print Preposition.getIndirectObject()
   * <p>
   * recursive for AssetGroups!
   */
  protected static void printObject(
      PrintStream out, Object io)
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
      } else if (io instanceof ClusterIdentifier) {
        out.print("CID: \"");
        out.print(((ClusterIdentifier)io).toString());
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
          printObject(out, as);
        }
        out.print("]\"");
      } else if (io instanceof AbstractMeasure) {
        out.print(getMeasureName((AbstractMeasure)io));
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

  /** BEGIN MISC UTILITIES **/

  /**
   * Item type codes to show interface name instead of "*Impl".
   **/
  protected static final int ITEM_TYPE_ALLOCATION     = 0;
  protected static final int ITEM_TYPE_EXPANSION      = 1;
  protected static final int ITEM_TYPE_AGGREGATION    = 2;
  protected static final int ITEM_TYPE_DISPOSITION    = 3;
  protected static final int ITEM_TYPE_ASSET_TRANSFER = 4;
  protected static final int ITEM_TYPE_TASK           = 5;
  protected static final int ITEM_TYPE_ASSET          = 6;
  protected static final int ITEM_TYPE_WORKFLOW       = 7;
  protected static final int ITEM_TYPE_OTHER          = 8;
  protected static String[] ITEM_TYPE_NAMES;
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

  /**
   * getItemType.
   * <p>
   * Replace with synchronized hashmap lookup on obj.getClass()?
   **/
  protected static int getItemType(Object obj) {
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
  protected static class SummaryInfo {
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
  protected static class VerbSummaryInfo extends SummaryInfo {
    public Verb verb;
    public VerbSummaryInfo(Verb vb) {
      super();
      verb = vb;
    }
  }

  /**
   * Dates are formatted to "month_day_year_hour:minute[AM|PM]"
   */
  protected static SimpleDateFormat myDateFormat;
  protected static Date myDateInstance;
  protected static java.text.FieldPosition myFieldPos;
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
  protected static String getTimeString(long time) {
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
   * getMeasureName.
   * <pre>
   * Get the Measure short class name from a Measure instance, e.g.
   *   (org.cougaar.domain.planning.ldm.measure.Mass) --&gt; "Mass".intern()
   * </pre>
   */
  private static SubstringCache measureNames = new SubstringCache('.');
  public static String getMeasureName(AbstractMeasure m)
  {
    return measureNames.getPostfix(m.getClass().getName());
  }

  /**
   * SubstringCache.
   * <pre>
   * Problem:
   *   Have many strings which all start with the same prefix,
   *   separator character, but have string appended on end, e.g.
   *     "foo#a", "foo#b", "foo#c", "foo#d" ..
   *   Want to get the "foo", i.e. get the equivalent of:
   *   <code>
   *     return (s.substring(0, s.indexOf('#'))).intern();
   *   <code>
   *   but without as many string consing and interns.
   * Uses:
   *   <code>org.cougaar.core.society.UID</code> strings, classname strings, etc.
   * Solution:
   *   Keep small cache of prefixes, i.e. ("foo#", "foo").  Then can
   *   use String.startsWith() to look up our substrings, which
   *   have already been interned.
   * Notes:
   *   It's expected that the cache be <i>tiny!</i>.  Make sure
   *   that the prefix is common and the key-character is always
   *   present in the string!
   *
   *   This class does something like <code>java.util.HashMap</code>
   *   or <code>org.cougaar.util.KeyedSet</code>, but since our Map Key
   *   is exactly what we're caching, the are a loss..
   *
   *   Consider rewrite as char-by-char parser or dictionary tree?
   *
   *   Maybe this should be an <code>org.cougaar.util</code> class?
   * </pre>
   */
  protected static class SubstringCache {
    private final char separator;
    private String[] subKeys = null;
    private String[] subValues = null;
    private int nsubs = 0;

    private SubstringCache() {
      this.separator = '/'; // private, so never called.
    }

    public SubstringCache(char separator) {
      this.separator = separator;
    }

    /**
     * As in above notes -- get prefix which ends in "key".
     */
    public synchronized String getPrefix(String s)
    {
      try {
        // 99% of time "s" _should_ be listed
        for (int i = 0; i < nsubs; i++) {
          if (s.startsWith(subKeys[i])) {
            return subValues[i];
          }
        }
      } catch (Exception e) {
        // _rare_ null "s"?
        return null;
      }
      // _rarely_ see a new substring!
      String key;
      String value;
      try {
        int j = s.indexOf(separator);
        key = s.substring(0, j+1);
        value = s.substring(0, j).intern();
      } catch (Exception e) {
        // _rare_ "s" null or lacks "key"?
        return null;
      }
      add(key, value);
      return value;
    }

    /**
     * <pre>
     * Like above notes, but looks at tail string, e.g.
     *   "a#foo", "b#foo", "c#foo", ...  --&gt; "foo"
     * </pre>
     */
    public synchronized String getPostfix(String s)
    {
      try {
        // 99% of time "s" _should_ be listed
        for (int i = 0; i < nsubs; i++) {
          if (s.endsWith(subKeys[i])) {
            return subValues[i];
          }
        }
      } catch (Exception e) {
        // _rare_ null "s"?
        return null;
      }
      // _rarely_ see a new substring!
      String key;
      String value;
      try {
        int j = s.lastIndexOf(separator);
        key = s.substring(j);
        value = s.substring(j+1).intern();
      } catch (Exception e) {
        // _rare_ "s" null or lacks "key"?
        return null;
      }
      add(key, value);
      return value;
    }

    /**
     * already synchronized -- add new (key, value) to cache.
     */
    private void add(String key, String value) {
      int i = nsubs++;
      if (i == 0) {
        subKeys = new String[10];
        subValues = new String[10];
      } else {
        int oldsubKeysLength = subKeys.length;
        if (nsubs > oldsubKeysLength) {
          String[] oldsubKeys = subKeys;
          subKeys = new String[oldsubKeysLength+10];
          System.arraycopy(oldsubKeys, 0, subKeys, 0, i);
          String[] oldsubValues = subValues;
          subValues = new String[oldsubKeysLength+10];
          System.arraycopy(oldsubValues, 0, subValues, 0, i);
        }
      }
      subKeys[i] = key;
      subValues[i] = value;
    }

    public String toString() {
      return
        "SubstringCache for key: "+separator+" size: "+nsubs;
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
  }

  /**
   * URL-encoding for strings that typically don't require encoding.
   *
   * Saves some String allocations.
   */
  protected static final String encode(String s) {
    int n = s.length();
    for (int i = 0; i < n; i++) {
      int c = (int)s.charAt(i);
      if (!(DONT_NEED_ENCODING[i])) {
        return URLEncoder.encode(s);
      }
    }
    return s;
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
  protected static class XMLtoHTMLOutputStream extends FilterOutputStream
  {
    protected static final byte[] LESS_THAN;
    protected static final byte[] GREATER_THAN;
    //protected final static byte[] SPACE;
    //protected final static byte[] NEWLINE;
    static {
      LESS_THAN = "<font color=green>&lt;".getBytes();
      GREATER_THAN = "&gt;</font>".getBytes();
      //SPACE = "&nbsp;".getBytes();
      //NEWLINE = "<br>".getBytes();
    }

    public XMLtoHTMLOutputStream(OutputStream o) {
      super(o);
    }

    public void write(int b) throws IOException {
      /*
         if (b == ' ') {
         out.write(SPACE);
         } else if (b == '\n') {
         out.write(NEWLINE);
         } else */
      if (b == '<') {
        out.write(LESS_THAN);
      } else if (b == '>') {
        out.write(GREATER_THAN);
      } else {
        out.write(b);
      }
    }
  }

  /** END MISC UTILITIES **/

  protected static class MyPSPState extends PSPState {

    /** my additional fields **/

    /**
     * <pre>
     * MODE:
     *  0 = FRAME PAGE (WITH OPT TASK ITEM_ID)
     *  1 = ALL TASKS AT IDENTIFIED CLUSTER (WITH OPT VERB)
     *  2 = DISPLAY ALL CLUSTERS
     *  3 = TASK DETAILS (WITH REQUIRED ITEM_ID)
     *  4 = SUMMARY BY VERB OF ALL TASKS
     *  5 = PLAN ELEMENT DETAILS (WITH REQUIRED ITEM_ID)
     *  6 = DISPLAY ALL PLAN ELEMENTS
     *  7 = ASSET DETAILS (WITH REQUIRED ITEM_ID)
     *  8 = DISPLAY ALL ASSETS
     *  9 = SEARCH FORM
     * 10 = UNIQUE OBJECT DETAILS (WITH REQUIRED ITEM_ID)
     * 11 = UNIQUE OBJECT RAW DETAILS (WITH REQUIRED ITEM_ID)
     * 12 = MORE_ALL_UNIQUE_OBJECTS
     * 13 = WELCOME
     * 14 = WELCOME DETAILS
     * </pre>
     **/
    public static final String MODE = "mode";
    public static final int MODE_FRAME                        =  0;
    public static final int MODE_ALL_TASKS                    =  1;
    public static final int MODE_CLUSTERS                     =  2;
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
    public int mode;
    // filters
    public static final String ITEM_UID = "uid";
    public String itemUID;
    public static final String VERB = "verb";
    public String verbFilter;
    // limit quantity of data
    public static final String LIMIT = "limit";
    public boolean limit;
    // predicate
    public static final String PREDICATE = "pred";
    public String pred;
    // predicate style
    public static final String PREDICATE_STYLE = "predStyle";
    public String predStyle;
    // view parsed predicate for debugging
    public static final String PREDICATE_DEBUG = "predDebug";
    public boolean predDebug;

    /** constructor **/
    public MyPSPState(
        UISubscriber xsubscriber,
        HttpInput query_parameters,
        PlanServiceContext xpsc) {
      super(xsubscriber, query_parameters, xpsc);
      // set fields
      mode = -1;
    }

    /** use a query parameter to set a field **/
    public void setParam(String name, String value) {
      //super.setParam(name, value);
      if (name.equalsIgnoreCase(MODE)) {
        try {
          mode = Integer.parseInt(value);
        } catch (Exception eBadNumber) {
          System.err.println("INVALID MODE: "+name);
          mode = MyPSPState.MODE_FRAME;
        }
      } else if (name.equalsIgnoreCase(ITEM_UID)) {
        if (value != null) {
          itemUID = URLDecoder.decode(value);
        }
      } else if (name.equalsIgnoreCase(VERB)) {
        verbFilter = value;
      } else if (name.equalsIgnoreCase(LIMIT)) {
        limit = ((value != null) ?  value.equalsIgnoreCase("true") : true);
      } else if (name.equalsIgnoreCase(PREDICATE)) {
        pred = value;
      } else if (name.equalsIgnoreCase(PREDICATE_STYLE)) {
        predStyle = value;
      } else if (name.equalsIgnoreCase(PREDICATE_DEBUG)) {
        predDebug = ((value != null) ?  value.equalsIgnoreCase("true") : true);
      } else if (name.equalsIgnoreCase("task")) {
        // old parameter name
        itemUID = value;
      }
    }
  }

  //
  // ancient/uninteresting methods
  //

  public PSP_MESEPlanView() {
    super();
  }

  public PSP_MESEPlanView(String pkg, String id) throws RuntimePSPException {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc) {
    super.initializeTest();
    return false;
  }

  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  public String getDTD() {
    return null;
  }

  public void subscriptionChanged(Subscription subscription) {
  }

}
