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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.ListAllAgents;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.plugin.completion.CompletionCalculator;
import org.cougaar.planning.servlet.data.completion.AbstractTask;
import org.cougaar.planning.servlet.data.completion.CompletionData;
import org.cougaar.planning.servlet.data.completion.FailedTask;
import org.cougaar.planning.servlet.data.completion.FullCompletionData;
import org.cougaar.planning.servlet.data.completion.SimpleCompletionData;
import org.cougaar.planning.servlet.data.completion.UnconfidentTask;
import org.cougaar.planning.servlet.data.completion.UnestimatedTask;
import org.cougaar.planning.servlet.data.completion.UnplannedTask;
import org.cougaar.planning.servlet.data.xml.XMLWriter;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Filters;


/**
 * A <code>Servlet</code>, loaded by the 
 * <code>SimpleServletComponent</code>, that generates 
 * HTML, XML, and serialized-Object views of Task completion
 * information.
 */
public class CompletionServlet
extends BaseServletComponent
{
  protected static final UnaryPredicate TASK_PRED = new TaskPredicate();
  private static final class TaskPredicate implements UnaryPredicate {
    public boolean execute(Object o) {
      return (o instanceof Task);
    }
  }

  protected static class RootTaskPredicate implements UnaryPredicate {
    private Verb rootVerb;
    private Verb parentVerb;

    public RootTaskPredicate(Verb rootVerb, Verb parentVerb) {
      this.rootVerb = rootVerb;
      this.parentVerb = parentVerb;
    }
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task t = (Task) o;
        Verb verb = null;
        Workflow wf = null;
        Task parentTask = null;
        if (t != null && (verb = t.getVerb()) != null && verb.equals(rootVerb)) {
          if ((wf = t.getWorkflow()) != null && (parentTask = wf.getParentTask()) != null) {
            return parentTask.getVerb().equals(parentVerb);
          }
        }
      }
      return false;
    }

    public String toString() {
      return getClass().getName()+"[verb="+rootVerb+"]";
    }
  }

  protected static final String[] iframeBrowsers = {
    "mozilla/5",
    "msie 5",
    "msie 6"
  };

  protected static final double DEFAULT_RED_THRESHOLD = 0.89;

  protected static final double DEFAULT_YELLOW_THRESHOLD = 0.99;

  protected static final int MAX_AGENT_FRAMES = 18;

  protected String path;

  protected MessageAddress localAgent;

  protected String encLocalAgent;

  protected AgentIdentificationService agentIdService;
  protected BlackboardQueryService blackboardQueryService;
  protected WhitePagesService whitePagesService;

  protected CompletionCalculator calc;
  protected final Object lock = new Object();
  protected LoggingService logger;

  protected static UnaryPredicate projectSupplyRootTaskPred =
      new RootTaskPredicate(Verb.get("ProjectSupply"), Verb.get("GenerateProjections"));
  protected static UnaryPredicate supplyRootTaskPred =
      new RootTaskPredicate(Verb.get("Supply"), Verb.get("GenerateProjections"));
  protected static UnaryPredicate transportRootTaskPred =
      new RootTaskPredicate(Verb.get("Transport"), Verb.get("DetermineRequirements"));

  public CompletionServlet() {
    super();
    path = getDefaultPath();
  }

  public void setParameter(Object o) {
    if (o instanceof String) {
      path = (String) o;
    } else if (o instanceof Collection) {
      Collection c = (Collection) o;
      if (!(c.isEmpty())) {
        path = (String) c.iterator().next();
      }
    } else if (o == null) {
      // ignore
    } else {
      throw new IllegalArgumentException(
          "Invalid parameter: "+o);
    }
  }

  protected String getDefaultPath() {
    return "/completion";
  }

  protected String getPath() {
    return path;
  }

  protected Servlet createServlet() {
    return new CompletorServlet();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    if (agentIdService == null) {
      // Revocation
    } else {
      this.localAgent = agentIdService.getMessageAddress();
      encLocalAgent = formURLEncode(localAgent.getAddress());
    }
  }

  public void setBlackboardQueryService(
      BlackboardQueryService blackboardQueryService) {
    this.blackboardQueryService = blackboardQueryService;
  }

  public void setWhitePagesService(
      WhitePagesService whitePagesService) {
    this.whitePagesService = whitePagesService;
  }

  public void load() {
    super.load();
    logger = (LoggingService)
      getService(this, LoggingService.class, null);
  }

  public void unload() {
    super.unload();
    if (whitePagesService != null) {
      releaseService(
          this, WhitePagesService.class, whitePagesService);
      whitePagesService = null;
    }
    if (blackboardQueryService != null) {
      releaseService(
          this, BlackboardQueryService.class, blackboardQueryService);
      blackboardQueryService = null;
    }
    if (agentIdService != null) {
      releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  protected List getAllEncodedAgentNames() {
    try {
      // do full WP list (deprecated!)
      Set s = ListAllAgents.listAllAgents(whitePagesService);
      // URLEncode the names and sort
      List l = ListAllAgents.encodeAndSort(s);
      return l;
    } catch (Exception e) {
      throw new RuntimeException(
          "List all agents failed", e);
    }
  }

  protected List getAllAgentNames() {
    try {
      // do full WP list (deprecated!)
      List result = new ArrayList(ListAllAgents.listAllAgents(whitePagesService));
      Collections.sort(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException(
          "List all agents failed", e);
    }
  }

  protected Collection queryBlackboard(UnaryPredicate pred) {
    return blackboardQueryService.query(pred);
  }

  protected String getEncodedAgentName() {
    return encLocalAgent;
  }

  protected String formURLEncode(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  protected CompletionCalculator getCalculator() {
    synchronized (lock) {
      if (calc == null) {
        calc = new CompletionCalculator();
      }
      return calc;
    }
  }

  protected String getTitlePrefix() {
    return ""; // must not contain special URL characters
  }

  /**
   * Inner-class that's registered as the servlet.
   */
  protected class CompletorServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      (new Completor(request, response)).execute();    
    }

    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      (new Completor(request, response)).execute();  
    }
  }

  /** 
   * Inner-class to hold state and generate the response.
   */
  protected class Completor {

    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private int format;
    private boolean showTables;

    private HttpServletRequest request;
    private HttpServletResponse response;

    // writer from the request for HTML output
    private PrintWriter out;

    // various form params
    double redThreshold;
    double yellowThreshold;
    int refreshInterval;

    public Completor(
        HttpServletRequest request, 
        HttpServletResponse response)
    {
      this.request = request;
      this.response = response;
    }         

    public void execute() throws IOException, ServletException 
    {
      try {
        redThreshold = Double.parseDouble(request.getParameter("redThreshold"));
      } catch (Exception e) {
        redThreshold = DEFAULT_RED_THRESHOLD;
      }
      try {
        yellowThreshold = Double.parseDouble(request.getParameter("yellowThreshold"));
      } catch (Exception e) {
        yellowThreshold = DEFAULT_YELLOW_THRESHOLD;
      }
      try {
        refreshInterval = Integer.parseInt(request.getParameter("refreshInterval"));
      } catch (Exception e) {
        refreshInterval = 0;
      }
      String formatParam = request.getParameter("format");
      if (formatParam == null) {
        format = FORMAT_HTML; // default
      } else if ("data".equals(formatParam)) {
        format = FORMAT_DATA;
      } else if ("xml".equals(formatParam)) {
        format = FORMAT_XML;
      } else if ("html".equals(formatParam)) {
        format = FORMAT_HTML;
      } else {
        format = FORMAT_HTML; // other
      }

      String showTablesParam = request.getParameter("showTables");
      if (showTablesParam == null) {
        showTables = false; // default
      } else if ("true".equals(showTablesParam)) {
        showTables = true;
      } else {
        showTables = false; // other
      }

      String viewType = request.getParameter("viewType");
      if (viewType == null) {
        viewDefault(); // default
      } else if ("viewAgentSubmit".equals(viewType)) {
        viewAgentSubmit();
      } else if ("viewAgentBig".equals(viewType)) {
        viewAgentBig();
      } else if ("viewAllAgents".equals(viewType)) {
        viewAllAgents();
      } else if ("viewSelectedAgents".equals(viewType)) {
        viewSelectedAgents();
      } else if ("viewManyAgents".equals(viewType)) {
        viewManyAgents();
      } else if ("viewTitle".equals(viewType)) {
        viewTitle();
      } else if ("viewAgentSmall".equals(viewType)) {
        viewAgentSmall();
      } else if ("viewMoreLink".equals(viewType)) {
        viewMoreLink();
      } else {
        viewDefault(); // other
      }

      // done
    }

    private void viewDefault() throws IOException {
      if (format == FORMAT_HTML) {
        // generate outer frame page:
        //   top:    select "/agent"
        //   middle: "viewAgentSubmit" buttons
        //   bottom: "viewAgentBig" frame
        //
        // Note that the top and middle frames must be on 
        // the same host, due to javascript security.  Only
        // the bottom frame is updated by the submit button.
        response.setContentType("text/html");
        this.out = response.getWriter();
        out.print(
            "<html><head><title>"+
            getTitlePrefix()+
            "Completion Viewer</title></head>"+
            "<frameset rows=\"10%,12%,78%\">\n"+
            "<frame src=\""+
            "/agents?format=select&suffix="+
            getEncodedAgentName()+
            "\" name=\"agentFrame\">\n"+
            "<frame src=\"/$"+
            getEncodedAgentName()+getPath()+
            "?viewType=viewAgentSubmit\" name=\"viewAgentSubmit\">\n"+
            "<frame src=\"/$"+
            getEncodedAgentName()+getPath()+
            "?viewType=viewAgentBig\" name=\"viewAgentBig\">\n"+
            "</frameset>\n"+
            "<noframes>Please enable frame support</noframes>"+
            "</html>\n");
        out.flush();
      } else {
        // for other formats, just get the data
        viewAgentBig();
      }
    }

    protected void viewAgentSubmit() throws IOException {
      response.setContentType("text/html");
      this.out = response.getWriter();
      // javascript based on PlanViewServlet
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var obj = top.agentFrame.document.agent.name;\n"+
          "  var encAgent = obj.value;\n"+
          "  if (encAgent.charAt(0) == '.') {\n"+
          "    alert(\"Please select an agent name\")\n"+
          "    return false;\n"+
          "  }\n"+
          "  document.myForm.target=\"viewAgentBig\"\n"+
          "  document.myForm.action=\"/$\"+encAgent+\""+
          getPath()+"\"\n"+
          "  return true\n"+
          "}\n"+
          "// -->\n"+
          "</script>\n"+
          "<head>\n"+
          "<title>"+
          getTitlePrefix()+
          "Completion"+
          "</title>"+
          "</head>\n"+
          "<body>"+
          "<form name=\"myForm\" method=\"get\" "+
          "onSubmit=\"return mySubmit()\">\n"+
          getTitlePrefix()+
          "Select an agent above, "+
          "<input type=\"hidden\""+
          " name=\"viewType\""+
          " value=\"viewAgentBig\" "+
          "<input type=\"checkbox\""+
          " name=\"showTables\""+
          " value=\"true\" ");
      if (showTables) {
        out.print("checked");
      }
      out.println(
          "> show table, \n"+
          "<input type=\"submit\""+
          " name=\"formSubmit\""+
          " value=\"Submit\"><br>");
      out.println(
          "<a href=\"/$"+
          getEncodedAgentName()+getPath()+
          "?viewType=viewAllAgents"+
          "\" target=\"_top\">Show all agents.</a>");
      out.println(
          "<a href=\"/$"+
          getEncodedAgentName()+getPath()+
          "?viewType=viewManyAgents"+
          "\" target=\"_top\"> Show several selected agents.</a>");
      out.println("</form>");
      out.print("</body></html>");
      out.flush();
    }

    private void viewAgentBig() {
      // get result
      CompletionData result = getCompletionData();

      // write data
      try {
        if (format == FORMAT_HTML) {
          // html      
          response.setContentType("text/html");
          this.out = response.getWriter();
          printCompletionDataAsHTML(result);
        } else {
          // unsupported
          if (format == FORMAT_DATA) {      
            // serialize
            //response.setContentType("application/binary");
            OutputStream out = response.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(result);
            oos.flush();
          } else {
            // xml
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            out.write(("<?xml version='1.0'?>\n").getBytes());
            XMLWriter w =
              new XMLWriter(
                  new OutputStreamWriter(out));
            result.toXML(w);
            w.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void viewMoreLink() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>"+
          getTitlePrefix()+
          "Completion of More Agents</title>\n" +
          "</head>\n"+
          "<body>");
      String firstAgent = request.getParameter("firstAgent");
      if (firstAgent != null) {
        out.println("<A href=\"/$"
                    + getEncodedAgentName()+getPath()
                    + "?viewType=viewAllAgents&refreshInterval="
                    + refreshInterval
                    + "&redThreshold="
                    + redThreshold
                    + "&yellowThreshold="
                    + yellowThreshold
                    + "&firstAgent="
                    + firstAgent
                    + "\" + target=\"_top\">\n"
                    + "<h2><center>More Agents</h2></center>\n"
                    + "</A>");
      }
      out.println("</body>\n</html>");
    }

    private void viewTitle() throws IOException {
      String title = request.getParameter("title");
      response.setContentType("text/html");
      if (refreshInterval > 0) {
        response.setHeader("Refresh", String.valueOf(refreshInterval));
      }
      List agents = getSelectedAgents();
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>" + title + "</title>\n" +
          "</head>\n"+
          "<body>\n"+
          "<h2><center>" + title + "</h2></center>\n");
      int totalAgents = agents.size();
      int nPages = (totalAgents + MAX_AGENT_FRAMES - 1) / MAX_AGENT_FRAMES;
      String[] menu = null;
      if (nPages > 1) {
        menu = new String[nPages]; 
        for (int page = 0; page < nPages; page++) {
          int nagents;
          int agent0;
          agent0 = (page * totalAgents + nPages - 1) / nPages;
          nagents = (((page + 1) * totalAgents + nPages - 1) / nPages) - agent0;
          String item =
            "Agents " + ((String) agents.get(agent0)) +
            " Through " + ((String) agents.get(agent0 + nagents - 1));
          menu[page] = item;
        }
      }
      String thisPage = request.getParameter("thisPage");
      printThresholdAndRefreshForm(out, agents, menu, thisPage);
      out.println(
          "</body>\n"+
          "</html>");
    }

    private void printThresholdAndRefreshForm(PrintWriter out, List selectedAgents, String[] menu, String thisPage) {
      out.println("<form name=\"viewTitle\" action=\"/$" + getEncodedAgentName() + getPath() + "\"method=\"post\" target=\"_top\">");
      out.println("<table>");
      out.print("<tr><td>");
      out.print("Red Threshold");
      out.print("</td><td>");
      out.print("<input name=\"redThreshold\" type=\"text\" value=\""
                + redThreshold
                + "\">");
      out.print("</td><td>");
      out.print("Yellow Threshold");
      out.print("</td><td>");
      out.print("<input name=\"yellowThreshold\" type=\"text\" value=\""
                + yellowThreshold
                + "\">");
      out.println("</td><td rowspan=3>");
      if (menu != null) {
        out.println("<select name=\"page\" size=3 onclick=\"document.viewTitle.submit()\">");
        for (int page = 0; page < menu.length; page++) {
          out.println("<option value=\"" + page + "\" onclick=\"document.viewTitle.submit()\">");
          out.println(menu[page]);
          out.println("</option>");
        }
        out.println("</select>");
      }
      out.println("</td></tr>");
      out.print("<tr><td>");
      out.print("Refresh Interval");
      out.print("</td><td>");
      out.print("<input name=\"refreshInterval\" type=\"text\" value=\""
                + refreshInterval
                + "\">");
      out.print("</td><td>");
      out.print("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      out.println("</td>");
      out.println("</tr>");
      out.println("</table>");
      out.println("<input type=\"hidden\" name=\"viewType\" value=\"viewSelectedAgents\">");
      for (int i = 0, n = selectedAgents.size(); i < n; i++) {
        String agentName = (String) selectedAgents.get(i);
        out.println("<input type=\"hidden\" name=\"selectedAgents\" value=\"" + agentName + "\">");
      }
      out.println("<input type=\"hidden\" name=\"currentPage\" value=\"" + thisPage + "\">");
      out.println("</form>");
    }

    // Output a page showing summary info for all agents
    private void viewAllAgents() throws IOException {
      viewSelectedAgents(getAllAgentNames(), "All");
    }

    private void viewSelectedAgents() throws IOException {
      viewSelectedAgents(getSelectedAgents(), "Selected");
    }

    private void viewSelectedAgents(List agents, String titleModifier) throws IOException {
      response.setContentType("text/html");
      if (refreshInterval > 0) {
        response.setHeader("Refresh", String.valueOf(refreshInterval));
      }
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      String title = getTitlePrefix() + "Completion of " + titleModifier + " Agents";
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>" + title + "</title>\n" +
          "</head>");
      boolean use_iframes = false;
      String browser = request.getHeader("user-agent").toLowerCase();
      if (browser != null) {
        for (int i = 0; i < iframeBrowsers.length; i++) {
          if (browser.indexOf(iframeBrowsers[i]) >= 0) {
            use_iframes = true;
            break;
          }
        }
      }
      if (use_iframes) {
        out.println(
            "<body>\n"+
            "<h2><center>"+
            title+
            "</h2></center>");
        printThresholdAndRefreshForm(out, agents, null, null);
        for (int i = 0, n = agents.size(); i < n; i++) {
          String agentName = (String) agents.get(i);
          out.println("<iframe src=\"/$"
                      + formURLEncode(agentName)
                      + getPath()
                      + "?viewType=viewAgentSmall&redThreshold="
                      + redThreshold
                      + "&yellowThreshold="
                      + yellowThreshold
                      + "\" scrolling=\"no\" width=300 height=151>"
                      + agentName
                      + "</iframe>");
        }
        out.println("</body>");
      } else {
        int totalAgents = agents.size();
        int nPages = (totalAgents + MAX_AGENT_FRAMES - 1) / MAX_AGENT_FRAMES;
        int nagents;
        int agent0;
        int page;
        if (nPages > 1) {
          try {
            page = Integer.parseInt(request.getParameter("page"));
          } catch (Exception e) {
            try {
              page = Integer.parseInt(request.getParameter("currentPage"));
            } catch (Exception e1) {
              page = 0;
            }
          }
          agent0 = (page * totalAgents + nPages - 1) / nPages;
          nagents = (((page + 1) * totalAgents + nPages - 1) / nPages) - agent0;
          title =
            titleModifier + " Agents " + ((String) agents.get(agent0)) +
            " Through " + ((String) agents.get(agent0 + nagents - 1));
        } else {
          agent0 = 0;
          nagents = totalAgents;
          title = "All Agents";
          page = 0;
        }
        int nrows = (nagents + 2) / 3;
        out.print("<frameset rows=\"100");
        for (int row = 0; row < nrows; row++) {
          out.print(",100");
        }
        out.print("\">\n"
                  + "  <frame src=\"/$"
                  + getEncodedAgentName()
                  + getPath()
                  + "?viewType=viewTitle&title="
                  + getTitlePrefix()+"Completion+of+"
                  + formURLEncode(title)
                  + "&refreshInterval="
                  + refreshInterval
                  + "&redThreshold="
                  + redThreshold
                  + "&yellowThreshold="
                  + yellowThreshold
                  + "&thisPage="
                  + page
                  + "&nextPage="
                  + ((page + 1) % nPages));
        for (int i = 0; i < totalAgents; i++) {
          String agentName = (String) agents.get(i);
          out.print("&selectedAgents=" + formURLEncode(agentName));
        }
        out.println("\" scrolling=\"no\">");
        for (int row = 0; row < nrows; row++) {
          out.println("  <frameset cols=\"300,300,300\">");
          for (int col = 0; col < 3; col++) {
            int agentn = agent0 + row * 3 + col;
            if (agentn < agent0 + nagents) {
              String agentName = (String) agents.get(agentn);
              out.println("    <frame src=\""
                          + "/$"
                          + formURLEncode(agentName)
                          + getPath()
                          + "?viewType=viewAgentSmall&redThreshold="
                          + redThreshold
                          + "&yellowThreshold="
                          + yellowThreshold
                          + "\" scrolling=\"no\">");
            } else if (agentn == agent0 + nagents) {
            }
          }
          out.println("  </frameset>");
        }
        out.println("</frameset>");
      }
      out.println("<html>");
    }

    private List getSelectedAgents() {
      String[] selectedAgents = request.getParameterValues("selectedAgents");
      if (selectedAgents != null) {
        List ret = new ArrayList(Arrays.asList(selectedAgents));
        Collections.sort(ret);
        return ret;
      } else {
        return Collections.EMPTY_LIST;
      }
    }

    // Output a checkbox form allowing selection of multiple agents
    private void viewManyAgents() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      SortedSet selectedAgents = new TreeSet(getSelectedAgents());
      boolean selectAll = false;
      boolean selectNone = false;
      String submit = request.getParameter("submit");
      if ("Show".equals(submit)) {
        viewSelectedAgents(new ArrayList(selectedAgents), "Selected");
        return;
      }
      if ("Select All".equals(submit)) {
        selectAll = true;
      } else if ("Select None".equals(submit)) {
        selectNone = true;
      }
      List l = getAllAgentNames();
      Collections.sort(l);
      String title ="Select Agents for Completion Display";
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>"+
          getTitlePrefix()+
          title+"</title>\n" +
          "</head>");
      out.println(
          "<body>\n"+
          "<h2><center>"+title+"</h2></center>");
      out.println("<form method=\"post\" action=\"/$"+
          getEncodedAgentName()+getPath()+
          "\" target=\"_top\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Select All\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Select None\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Show\">");
      out.println("<input type=\"hidden\" name=\"viewType\" value=\"viewManyAgents\">");
      out.println("<table><tr>");
      int nagents = l.size();
      int agent0 = 0;
      int NCOL = 4;
      for (int col = 0; col < NCOL; col++) {
        out.println("<td valign=\"top\">");
        int agent1 = ((col + 1) * nagents + NCOL - 1) / NCOL;
        for (; agent0 < agent1; agent0++) {
          String agentName = (String) l.get(agent0);
          String selected;
          if (selectAll || (!selectNone && selectedAgents.contains(agentName))) {
            selected = " checked=\"true\"";
          } else {
            selected = "";
          }
          out.println("<input type=\"checkbox\" name=\"selectedAgents\" value=\""
                      + agentName
                      + "\""
                      + selected
                      + ">"
                      + agentName
                      + "</input><br>");
        }
        out.println("</td>");
        agent0 = agent1;
      }
      out.println("</tr></table>");
      out.println("</form>");
      out.println("</body>");
      out.println("</html>");
    }

    // Output a small page showing summary info for one agent
    private void viewAgentSmall() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      String agent = getEncodedAgentName();
      CompletionData result = getCompletionData();
      double ratio = result.getRatio();
      int nTasks = result.getNumberOfTasks();
      int nUnplannedTasks = result.getNumberOfUnplannedTasks();
      int nPlannedTasks = (nTasks - nUnplannedTasks);
      int nRootProjectSupplyTasks = result.getNumberOfRootProjectSupplyTasks();
      int nRootSupplyTasks = result.getNumberOfRootSupplyTasks();
      int nRootTransportTasks = result.getNumberOfRootTransportTasks();

      double percentPlannedTasks =
        ((nTasks > 0) ? 
         (1.0 * nPlannedTasks) / nTasks :
         0.0);
      int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
      int nEstimatedTasks = (nPlannedTasks - nUnestimatedTasks);
//      double percentEstimatedTasks =
//        ((nPlannedTasks > 0) ? 
//         (1.0 * nEstimatedTasks) / nPlannedTasks :
//         0.0);
      int nFailedTasks = result.getNumberOfFailedTasks();
      int nSuccessfulTasks = (nEstimatedTasks - nFailedTasks);
      double percentSuccessfulTasks =
        ((nEstimatedTasks > 0) ? 
         (1.0 * nSuccessfulTasks) / nEstimatedTasks :
         0.0);
      int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
      int nFullConfidenceTasks = (nSuccessfulTasks - nUnconfidentTasks);
      double percentFullConfidenceTasks =
        ((nSuccessfulTasks > 0) ? 
         (1.0 * nFullConfidenceTasks) / nSuccessfulTasks :
         0.0);
      String bgcolor, fgcolor, lncolor;
      if (ratio < redThreshold) {
        bgcolor = "#aa0000";
        fgcolor = "#ffffff";
        lncolor = "#ffff00";
      } else if (ratio < yellowThreshold) {
        bgcolor = "#ffff00";
        fgcolor = "#000000";
        lncolor = "#0000ff";
      } else {
        bgcolor = "#d0ffd0";
        fgcolor = "#000000";
        lncolor = "#0000ff";
      }
      out.println(
          "<html>\n"+
          "<head>\n"+
          "</head>\n"+
          "<body"+
          " bgcolor=\"" + bgcolor + 
          "\" text=\"" + fgcolor + 
          "\" vlink=\"" + lncolor + 
          "\" link=\"" + lncolor + "\">\n"+
          "<pre><a href=\""+
          "/$" + agent + getPath() +
          "\" target=\"_top\">"+
          agent+
          "</a>");
      out.println(formatLabel("Ratio:") + "  <b>" + formatPercent(ratio) + "</b> ("+ratio+")");
      out.println(formatLabel("Tasks:") + formatInteger(nTasks));
      out.println(formatLabel("Planned:")        + formatInteger(nPlannedTasks)        + "(" + formatPercent(percentPlannedTasks)        + ")");
      out.println(formatLabel("Successful:")     + formatInteger(nSuccessfulTasks)     + "(" + formatPercent(percentSuccessfulTasks)     + ")");
      out.println(formatLabel("Completed:")      + formatInteger(nFullConfidenceTasks) + "(" + formatPercent(percentFullConfidenceTasks) + ")");
      out.println(formatLabel("Root Proj Supply Tasks:") + formatInteger(nRootProjectSupplyTasks));
      out.println(formatLabel("Root Supply Tasks:") + formatInteger(nRootSupplyTasks));
      out.println(formatLabel("Root Transport Tasks:") + formatInteger(nRootTransportTasks) + "</pre>");
      out.println("</body>\n</html>");
    }

    private String formatLabel(String lbl) {
      int nchars = lbl.length();
      if (nchars > 24) return lbl;
      return lbl + "                        ".substring(nchars);
    }

    private String formatInteger(int n) {
      return formatInteger(n, 5);
    }

    private final String SPACES = "                    ";
    private final int NSPACES = SPACES.length();

    private String formatInteger(int n, int w) {
      if (w > NSPACES) w = NSPACES;
      String r = String.valueOf(n);
      int needed = w - r.length();
      if (needed <= 0) return r;
      return SPACES.substring(0, needed) + r;
    }

    private String formatPercent(double percent) {
      return formatInteger((int) (percent * 100.0), 3) + "%";
    }

    private String formatColorBar(String color) {
      return 
        "<table width=\"100%\" bgcolor=\""+color+
        "\"><tr><td>&nbsp;</td></tr></table>";
    }

    protected Collection getAllTasks() {
      Collection col = queryBlackboard(TASK_PRED);
      if (col == null) col = Collections.EMPTY_LIST;
      return col;
    }

    protected double getRatio(Collection tasks) {
      Collection objs;
      CompletionCalculator cc = getCalculator();
      if (cc.getClass() == CompletionCalculator.class) {
        // short cut for basic task completion
        objs = tasks;
      } else {
        UnaryPredicate pred = cc.getPredicate();
        objs = queryBlackboard(pred);
      }
      return cc.calculate(objs);
    }

    public Collection filterTasks(Collection allTasks, UnaryPredicate predicate) {
      return Filters.filter(allTasks, predicate);
    }

    protected CompletionData getCompletionData() {
      // get tasks
      Collection tasks = getAllTasks();
      CompletionCalculator cc = getCalculator();
      long nowTime = System.currentTimeMillis();
      double ratio = getRatio(tasks);
      int nTasks = tasks.size();
      int nRootProjectSupplyTasks = filterTasks(tasks, projectSupplyRootTaskPred).size();
      int nRootSupplyTasks = filterTasks(tasks, supplyRootTaskPred).size();
      int nRootTransportTasks = filterTasks(tasks, transportRootTaskPred).size();

      Iterator taskIter = tasks.iterator();
      if (showTables) {
        // create and initialize our result
        FullCompletionData result = new FullCompletionData();
        result.setNumberOfTasks(nTasks);
        result.setRatio(ratio);
        result.setTimeMillis(nowTime);
        result.setNumberOfRootProjectSupplyTasks(nRootProjectSupplyTasks);
        result.setNumberOfRootSupplyTasks(nRootSupplyTasks);
        result.setNumberOfRootTransportTasks(nRootTransportTasks);

        // examine tasks
        for (int i = 0; i < nTasks; i++) {
          Task ti = (Task)taskIter.next();
          PlanElement pe = ti.getPlanElement();
          if (pe != null) {
            AllocationResult peEstResult = pe.getEstimatedResult();
            if (peEstResult != null) {
              double estConf = peEstResult.getConfidenceRating();
              if (peEstResult.isSuccess()) {
                if (cc.isConfident(estConf)) {
                  // Confident
                } else {
                  result.addUnconfidentTask(
                      makeUnconfidentTask(estConf, ti));
                }
              } else {
                result.addFailedTask(makeFailedTask(estConf, ti));
              }
            } else {
              result.addUnestimatedTask(makeUnestimatedTask(ti));
            }
          } else {
            result.addUnplannedTask(makeUnplannedTask(ti));
          }
        }
        return result;
      } else {
        // create and initialize our result
        SimpleCompletionData result = new SimpleCompletionData();
        result.setNumberOfTasks(nTasks);
        result.setRatio(ratio);
        result.setTimeMillis(nowTime);
        result.setNumberOfRootProjectSupplyTasks(nRootProjectSupplyTasks);
        result.setNumberOfRootSupplyTasks(nRootSupplyTasks);
        result.setNumberOfRootTransportTasks(nRootTransportTasks);
        // examine tasks
        int nUnplannedTasks = 0;
        int nUnestimatedTasks = 0;
        int nFailedTasks = 0;
        int nUnconfidentTasks = 0;
        for (int i = 0; i < nTasks; i++) {
          Task ti = (Task)taskIter.next();
          PlanElement pe = ti.getPlanElement();
          if (pe != null) {
            AllocationResult peEstResult = pe.getEstimatedResult();
            if (peEstResult != null) {
              double estConf = peEstResult.getConfidenceRating();
              if (peEstResult.isSuccess()) {
                if (cc.isConfident(estConf)) {
                  // 100% success
                } else {
                  nUnconfidentTasks++;
                }
              } else {
                nFailedTasks++;
              }
            } else {
              nUnestimatedTasks++;
            }
          } else {
            nUnplannedTasks++;
          }
        }
        result.setNumberOfUnplannedTasks(nUnplannedTasks);
        result.setNumberOfUnestimatedTasks(nUnestimatedTasks);
        result.setNumberOfFailedTasks(nFailedTasks);
        result.setNumberOfUnconfidentTasks(nUnconfidentTasks);
        return result;
      }
    }

    /**
     * Create an <code>UnplannedTask</code> for the given <code>Task</code>.
     */
    protected UnplannedTask makeUnplannedTask(Task task) {
      UnplannedTask upt = new UnplannedTask();
      fillAbstractTask(upt, task);
      // leave confidence as 0%
      return upt;
    }

    /**
     * Create an <code>UnestimatedTask</code> for the given <code>Task</code>.
     */
    protected UnestimatedTask makeUnestimatedTask(Task task) {
      UnestimatedTask uet = new UnestimatedTask();
      fillAbstractTask(uet, task);
      // leave confidence as 0%
      return uet;
    }

    /**
     * Create an <code>UnconfidentTask</code> for the given <code>Task</code>.
     *
     * @param confidence a double &gt;= 0.0 and &lt; 1.0
     */
    protected UnconfidentTask makeUnconfidentTask(double confidence, Task task) {
      UnconfidentTask uct = new UnconfidentTask();
      fillAbstractTask(uct, task);
      uct.setConfidence(confidence);
      return uct;
    }

    /**
     * Create a <code>FailedTask</code> for the given <code>Task</code>.
     */
    protected FailedTask makeFailedTask(double confidence, Task task) {
      FailedTask ft = new FailedTask();
      fillAbstractTask(ft, task);
      ft.setConfidence(confidence);
      return ft;
    }

    /**
     * Fill an <code>AbstractTask</code> for the given <code>Task</code>,
     * which will grab:<pre>
     *   the UID, 
     *   TASK.PSP's URL for that UID,
     *   the ParentUID, 
     *   TASK.PSP's URL for that ParentUID,
     *   a String description of the PlanElement</pre>.
     */
    protected void fillAbstractTask(AbstractTask toAbsTask, Task task) {

      // set task UID
      UID taskUID = ((task != null) ? task.getUID() : null);
      String sTaskUID = ((taskUID != null) ? taskUID.toString() : null);
      if (sTaskUID == null) {
        return;
      }
      toAbsTask.setUID(sTaskUID);
      String sourceAgentId = 
        formURLEncode(task.getSource().getAddress());
      toAbsTask.setUID_URL(
          getTaskUID_URL(getEncodedAgentName(), sTaskUID));
      // set parent task UID
      UID pTaskUID = task.getParentTaskUID();
      String spTaskUID = ((pTaskUID != null) ? pTaskUID.toString() : null);
      if (spTaskUID != null) {
        toAbsTask.setParentUID(spTaskUID);
        toAbsTask.setParentUID_URL(
            getTaskUID_URL(sourceAgentId, spTaskUID));
      }
      // set plan element
      toAbsTask.setPlanElement(getPlanElement(task.getPlanElement()));
      // set verb
      toAbsTask.setVerb(task.getVerb().toString());
    }

    /**
     * Get the TASKS.PSP URL for the given UID String.
     *
     * Assumes that the TASKS.PSP URL is fixed at "/tasks".
     */
    protected String getTaskUID_URL(
        String agentId, String sTaskUID) {
      /*
        // FIXME prefix with base URL?
        
        String baseURL =   
          request.getScheme()+
          "://"+
          request.getServerName()+
          ":"+
          request.getServerPort()+
          "/";
      */
      return 
        "/$"+
        agentId+
        "/tasks?mode=3&uid="+
        sTaskUID;
    }

    /**
     * Get a brief description of the given <code>PlanElement</code>.
     */
    protected String getPlanElement(
        PlanElement pe) {
      return 
        (pe instanceof Allocation) ?
        "Allocation" :
        (pe instanceof Expansion) ?
        "Expansion" :
        (pe instanceof Aggregation) ?
        "Aggregation" :
        (pe instanceof Disposition) ?
        "Disposition" :
        (pe instanceof AssetTransfer) ?
        "AssetTransfer" :
        (pe != null) ?
        pe.getClass().getName() : 
        null;
    }

    /**
     * Write the given <code>CompletionData</code> as formatted HTML.
     */
    protected void printCompletionDataAsHTML(CompletionData result) {
      // javascript based on PlanViewServlet
      out.print(
          "<html><body>\n"+
          "<h2><center>"+
          getTitlePrefix()+
          "Completion at "+
          getEncodedAgentName()+
          "</center></h2>\n");
      printCountersAsHTML(result);
      printTablesAsHTML(result);
      out.print("</body></html>");
      out.flush();
    }

    protected void printCountersAsHTML(CompletionData result) {
      double ratio = result.getRatio();
      String ratioColor;
      CompletionCalculator cc = getCalculator();
      if (ratio < redThreshold) {
        ratioColor = "red";
      } else if (ratio < yellowThreshold) {
        ratioColor = "yellow";
      } else {
        ratioColor = "#00d000";
      }
      out.print(
          formatColorBar(ratioColor)+
          "<pre>\n"+
          "Time: <b>");
      long timeMillis = result.getTimeMillis();
      out.print(new Date(timeMillis));
      out.print("</b>   (");
      out.print(timeMillis);
      out.print(" MS)\n"+
          getTitlePrefix()+
          "Completion ratio: <b>"+
          formatPercent(ratio)+
          "</b>"+
          "\nNumber of Tasks: <b>");
      int nTasks = result.getNumberOfTasks();
      out.print(nTasks);
      out.print("\n</b>Subset of Tasks[");
      out.print(nTasks);
      out.print("] planned (non-null PlanElement): <b>");
      int nUnplannedTasks = result.getNumberOfUnplannedTasks();
      int nPlannedTasks = (nTasks - nUnplannedTasks);
      out.print(nPlannedTasks);
      out.print("</b>  (<b>");
      double percentPlannedTasks =
        ((nTasks > 0) ? 
         (100.0 * (((double)nPlannedTasks) / nTasks)) :
         0.0);
      out.print(percentPlannedTasks);
      out.print(
          " %</b>)"+
          "\nSubset of planned[");
      out.print(nPlannedTasks);
      out.print("] estimated (non-null EstimatedResult): <b>");
      int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
      int nEstimatedTasks = (nPlannedTasks - nUnestimatedTasks);
      out.print(nEstimatedTasks);
      out.print("</b>  (<b>");
      double percentEstimatedTasks =
        ((nPlannedTasks > 0) ? 
         (100.0 * (((double)nEstimatedTasks) / nPlannedTasks)) :
         0.0);
      out.print(percentEstimatedTasks);
      out.print(
          " %</b>)"+
          "\nSubset of estimated[");
      out.print(nEstimatedTasks);
      out.print("] that are estimated successful: <b>");
      int nFailedTasks = result.getNumberOfFailedTasks();
      int nSuccessfulTasks = (nEstimatedTasks - nFailedTasks);
      out.print(nSuccessfulTasks);
      out.print("</b>  (<b>");
      double percentSuccessfulTasks =
        ((nEstimatedTasks > 0) ? 
         (100.0 * (((double)nSuccessfulTasks) / nEstimatedTasks)) :
         0.0);
      out.print(percentSuccessfulTasks);
      out.print(
          " %</b>)"+
          "\nSubset of estimated successful[");
      out.print(nSuccessfulTasks);
      out.print("] with " + cc.getConfidenceThreshholdString(true) + " : <b>");
      int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
      int nFullConfidenceTasks = (nSuccessfulTasks - nUnconfidentTasks);
      out.print(nFullConfidenceTasks);
      out.print("</b>  (<b>");
      double percentFullConfidenceTasks =
        ((nSuccessfulTasks > 0) ? 
         (100.0 * (((double)nFullConfidenceTasks) / nSuccessfulTasks)) :
         0.0);
      out.print(percentFullConfidenceTasks);
      out.print(" %</b>)\n");
      out.print("</b>"+"\nNumber of Root ProjectSupply Tasks: <b>");
      out.print(result.getNumberOfRootProjectSupplyTasks());
      out.print("</b>"+"\nNumber of Root Supply Tasks: <b>");
      out.print(result.getNumberOfRootSupplyTasks());
      out.print("</b>"+"\nNumber of Root Transport Tasks: <b>");
      out.print(result.getNumberOfRootTransportTasks());
      out.print("</pre>\n");
    }

    protected void printTablesAsHTML(CompletionData result) {
      CompletionCalculator cc = getCalculator();
      if (result instanceof FullCompletionData) {
        int nUnplannedTasks = result.getNumberOfUnplannedTasks();
        beginTaskHTMLTable(
            ("Unplanned Tasks["+nUnplannedTasks+"]"),
            "(PlanElement == null)");
        for (int i = 0; i < nUnplannedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnplannedTaskAt(i));
        }
        endTaskHTMLTable();
        int nUnestimatedTasks = result.getNumberOfUnestimatedTasks();
        beginTaskHTMLTable(
            ("Unestimated Tasks["+nUnestimatedTasks+"]"),
            "(Est. == null)");
        for (int i = 0; i < nUnestimatedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnestimatedTaskAt(i));
        }
        endTaskHTMLTable();
        int nFailedTasks = result.getNumberOfFailedTasks();
        beginTaskHTMLTable(
            ("Failed Tasks["+nFailedTasks+"]"),
            "(Est.isSuccess() == false)");
        for (int i = 0; i < nFailedTasks; i++) {
          printAbstractTaskAsHTML(i, result.getFailedTaskAt(i));
        }
        endTaskHTMLTable();
        int nUnconfidentTasks = result.getNumberOfUnconfidentTasks();
        beginTaskHTMLTable(
            ("Unconfident Tasks["+nUnconfidentTasks+"]"),
            "((Est.isSuccess() == true) &amp;&amp; (" + cc.getConfidenceThreshholdString(false) + ")");
        for (int i = 0; i < nUnconfidentTasks; i++) {
          printAbstractTaskAsHTML(i, result.getUnconfidentTaskAt(i));
        }
        endTaskHTMLTable();
      } else {
        // no table data
        out.print(
            "<p>"+
            "<a href=\"");
        out.print("/$");
        out.print(getEncodedAgentName());
        out.print(getPath());
        out.print(
            "?showTables=true&viewType=viewAgentBig\" target=\"viewAgentBig\">"+
            "Full Listing of Unplanned/Unestimated/Failed/Unconfident Tasks (");
        out.print(
            (result.getNumberOfTasks() - 
             result.getNumberOfFullySuccessfulTasks()));
        out.println(
            " lines)</a><br>");
      }
    }

    /**
     * Begin a table of <tt>printAbstractTaskAsHTML</tt> entries.
     */
    protected void beginTaskHTMLTable(
        String title, String subTitle) {
      out.print(
          "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n"+
          "<tr bgcolor=lightgrey><th align=left colspan=6>");
      out.print(title);
      if (subTitle != null) {
        out.print(
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<tt><i>");
        out.print(subTitle);
        out.print("</i></tt>");
      }
      out.print(
          "</th></tr>\n"+
          "<tr>"+
          "<th></th>"+
          "<th>UID</th>"+
          "<th>ParentUID</th>"+
          "<th>Verb</th>"+
          "<th>Confidence</th>"+
          "<th>PlanElement</th>"+
          "</tr>\n");
    }

    /**
     * End a table of <tt>printAbstractTaskAsHTML</tt> entries.
     */
    protected void endTaskHTMLTable() {
      out.print(
          "</table>\n"+
          "<p>\n");
    }

    /**
     * Write the given <code>AbstractTask</code> as formatted HTML.
     */
    protected void printAbstractTaskAsHTML(int index, AbstractTask at) {
      out.print("<tr align=right><td>");
      out.print(index);
      out.print("</td><td>");
      String uidURL = at.getUID_URL();
      if (uidURL != null) {
        out.print("<a href=\"");
        out.print(uidURL);
        out.print("\" target=\"itemFrame\">");
      }
      out.print(at.getUID());
      if (uidURL != null) {
        out.print("</a>");
      }
      out.print("</td><td>");
      String pUidURL = at.getParentUID_URL();
      if (pUidURL != null) {
        out.print("<a href=\"");
        out.print(at.getParentUID_URL());
        out.print("\" target=\"itemFrame\">");
      }
      out.print(at.getParentUID());
      if (pUidURL != null) {
        out.print("</a>");
      }
      out.print("</td><td>");
      out.print(at.getVerb());
      out.print("</td><td>");
      double conf = at.getConfidence();
      out.print(
          (conf < 0.001) ? 
          "0.0%" : 
          ((100.0 * conf) + "%"));
      out.print("</td><td>");
      out.print(at.getPlanElement());
      out.print("</td></tr>\n");
    }
  }
}
