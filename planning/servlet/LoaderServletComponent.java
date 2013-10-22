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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.CSVUtility;

/**
 * Servlet that allows the client to add, remove, and check for
 * the existence of Components at both the servlet's agent 
 * (eg plugin) or node (eg agent).
 * <p>
 * The path of the servlet is "/load".  This is a bit of a 
 * misnomer, since in addition to "add" this servlet also
 * supports "remove" and "contains".
 * <p>
 * The URL parameters to this servlet are:
 * <ul><p>
 *   <li><tt>action=STRING</tt><br>
 *       Required action for container operation; this should 
 *       be either "add", "remove", or "contains", where the 
 *       default is "add".
 *       <p>
 *       <b>Important Note:</b>
 *       <p>
 *       "add" acts as an "ensure-is-loaded"; if the component 
 *       is already loaded then the action is ignored (success).  
 *       <p>
 *       "remove" similarily acts as an "ensure-is-not-loaded", 
 *       where a remove of a non-loaded component is ignored 
 *       (success).
 *       <p>
 *       "contains" will only return with a success status code 
 *       if the component is loaded ("assert-is-loaded").  If
 *       the component is not loaded then an error response
 *       is returned.
 *       </li><p>
 *   <li><tt>target=STRING</tt><br>
 *       Optional target of where to add this component;
 *       this should be either "agent" or "node", where the
 *       default is "agent".  This must agree with the
 *       "insertionPoint".</li><p>
 *   <li><tt>name=STRING</tt><br>
 *       Optional name of the component; defaults to the 
 *       classname.
 *       </li><p>
 *   <li><tt>insertionPoint=STRING</tt><br>
 *       Insertion point for the component; the default is:<br>
 *       <tt>Node.AgentManager.Agent.PluginManager.Plugin</tt>.
 *       </li><p>
 *   <li><tt>classname=STRING</tt><br>
 *       Name of the component class to load.<br>
 *       If this parameter is missing then a "usage" HTML
 *       page is generated.</li><p>
 *   <li><tt>parameters=STRING1,STRING2,..,STRINGN</tt> 
 *       Optional list of string parameters to pass to the
 *       component.  Defaults to null.</li><p>
 *   <li><tt>codebase=STRING</tt><br>
 *       Optional codebase URL for locating the class file(s).
 *       The default is to use the node's classpath.</li><p>
 * </ul>
 * <p>
 * Note the <b>SECURITY</b> issues in loading an arbitrary
 * Component!
 */
public class LoaderServletComponent
extends BaseServletComponent
{
  protected MessageAddress agentId;
  protected MessageAddress nodeId;

  protected AgentContainmentService agentContainer;

  protected NodeIdentificationService nodeIdService;

  private static final String[] VALID_ACTIONS = {
    "add",
    "remove",
    "contains",
  };

  protected String getPath() {
    return "/load";
  }

  protected Servlet createServlet() {
    // create inner class
    return new MyServlet();
  }

  // aquire services:
  public void load() {
    AgentIdentificationService ais = (AgentIdentificationService)
      getService(
          this, AgentIdentificationService.class, null);
    if (ais != null) {
      this.agentId = ais.getMessageAddress();
      releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // get the nodeId
    this.nodeIdService = (NodeIdentificationService)
     getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain NodeIdentificationService for \""+
          getPath()+"\" servlet");
    }
    this.nodeId = nodeIdService.getMessageAddress();
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node's id? for \""+
          getPath()+"\" servlet");
    }

    // get the agent containment service
    this.agentContainer = (AgentContainmentService)
      getService(
          this,
          AgentContainmentService.class,
          null);
    if (agentContainer == null) {
      throw new RuntimeException(
          "Unable to obtain AgentContainmentService for \""+
          getPath()+
          "\" servlet");
    }

    // FIXME get node containment service

    super.load();
  }

  // release services:
  public void unload() {
    super.unload();
    if (agentContainer != null) {
      releaseService(
          this, AgentContainmentService.class, agentContainer);
      agentContainer = null;
    }
    if (nodeIdService != null) {
      releaseService(
          this, NodeIdentificationService.class, nodeIdService);
      nodeIdService = null;
    }
    // release agentIdService
  }

  private boolean performAction(
      String action,
      String target,
      ComponentDescription desc) {
    // add security-check here
    if (!("agent".equalsIgnoreCase(target))) {
      throw new UnsupportedOperationException(
          "Only \"agent\" target is supported, not \""+
          target+"\" (see bug 1112)");
    }
    if ("add".equalsIgnoreCase(action)) {
      return agentContainer.add(desc);
    } else if ("remove".equalsIgnoreCase(action)) {
      return agentContainer.remove(desc);
    } else if ("contains".equalsIgnoreCase(action)) {
      if (!(agentContainer.contains(desc))) {
        throw new RuntimeException(
            target+" doesn't contain the component.");
      }
      return true;
    } else {
      throw new UnsupportedOperationException(
          "Unknown action: \""+action+"\"");
    }
  }

  /**
   * Servlet to handle requests.
   */
  private class MyServlet extends HttpServlet {

    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      MyWorker mw = new MyWorker(req, res);
      mw.execute();
    }

    private class MyWorker {

      // from the "doGet(..)":
      private HttpServletRequest request;
      private HttpServletResponse response;

      // from the URL-params:
      //    (see the class-level javadocs for details)

      public static final String ACTION_PARAM = "op";
      public String action;

      public static final String TARGET_PARAM = "target";
      public static final String OLD_TARGET_PARAM = "into";
      public String target;

      public static final String COMPONENT_NAME_PARAM = 
        "name";
      public String compName;

      public static final String INSERTION_POINT_PARAM = 
        "insertionPoint";
      public String insertionPoint;

      public static final String CLASSNAME_PARAM = "classname";
      public String classname;

      public static final String PARAMETERS_PARAM = "params";
      public List parameters;

      public static final String CODEBASE_PARAM = "codebase";
      public String codebase;

      // add "lease", "certificate", "policy"???
      
      // worker constructor:
      public MyWorker(
          HttpServletRequest request,
          HttpServletResponse response) {
        this.request = request;
        this.response = response;
      }

      // handle a request:
      public void execute() throws IOException {
        parseParams();
        writeResponse();
      }

      private void parseParams() throws IOException {
        // set defaults, postpone compName default
        action = "add";
        target = "agent";
        insertionPoint =
          "Node.AgentManager.Agent.PluginManager.Plugin";
        // get "name=value" parameters
        for (Enumeration en = request.getParameterNames();
            en.hasMoreElements();
            ) {
          // extract (name, value)
          String name = (String) en.nextElement();
          if (name == null) {
            continue;
          }
          String values[] = request.getParameterValues(name);
          int nvalues = ((values != null) ? values.length : 0);
          if (nvalues <= 0) {
            continue;
          }
          String value = values[nvalues - 1];
          if ((value == null) ||
              (value.length() <= 0)) {
            continue;
          }
          value = URLDecoder.decode(value, "UTF-8");

          // save parameters
          if (name.equals(ACTION_PARAM)) {
            action = value;
          } else if (name.equals(TARGET_PARAM) ||
                     name.equals(OLD_TARGET_PARAM)) {
            target = value;
          } else if (name.equals(COMPONENT_NAME_PARAM)) {
            compName = value;
          } else if (name.equals(INSERTION_POINT_PARAM)) {
            insertionPoint = value;
          } else if (name.equals(CLASSNAME_PARAM)) {
            classname = value;
          } else if (name.equals(PARAMETERS_PARAM)) {
            // parse (s1, s2, .., sN)
            parameters = CSVUtility.parseToList(value);
          } else if (name.equals(CODEBASE_PARAM)) {
            codebase = value;
          } else {
          }
        }
        // set default compName
        if (compName == null) {
          compName = classname;
        }
      }

      private void writeResponse() throws IOException {
        if (classname != null) {
          ComponentDescription desc =
            createComponentDescription();
          boolean ret;
          try {
            ret = performAction(action, target, desc);
          } catch (Exception e) {
            writeFailure(e);
            return;
          }
          writeSuccess(ret);
        } else {
          writeUsage();
        }
      }

      private void writeUsage() throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print(
            "<html><head><title>"+
            "Component loader"+
            "</title></head>"+
            "<body>\n"+
            "<h2>Component Loader Servlet</h2>\n"+
            "Please fill in these parameters:\n");
        writeParameters(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeFailure(Exception e) throws IOException {
        // select response message
//        String msg = action+" failed";

        // generate an HTML error response, with a 404 error code.
        //
        // use "setStatus" instead of "sendError" -- see bug 1259

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter out = response.getWriter();

        // build up response
        out.print(
            "<html><head><title>"+
            action+
            " failed"+
            "</title></head>"+
            "<body>\n"+
            "<center><h1>"+
            action+
            " failed"+
            "</h1></center>"+
            "<p><pre>\n");
        boolean isUnknownClass = false;
        for (Throwable t = e; t != null; t = t.getCause()) {
          if (t instanceof ClassNotFoundException) {
            isUnknownClass = true;
            break;
          }
        }
        if (isUnknownClass) {
          out.print("<h2>Unknown class \""+classname+"\"</h2>");
        } else {
          e.printStackTrace(out);
        }
        out.print(
            "\n</pre><p>"+
            "Please double-check these parameters:\n");
        writeParameters(out);
        out.print(
            "</body></html>\n");
        out.close();
      }

      private void writeSuccess(boolean ret) throws IOException {
        // select response message
        String msg;
        if ("add".equalsIgnoreCase(action)) {
          if (ret) {
            msg = "New component added";
          } else {
            msg = "Component already exists";
          }
        } else if ("remove".equalsIgnoreCase(action)) {
          if (ret) {
            msg = "Removed existing component";
          } else {
            msg = "No such component exists";
          }
        } else if ("contains".equalsIgnoreCase(action)) {
          if (ret) {
            msg = "Component exists";
          } else {
            // never - exception thrown when !contains
            msg = "Internal error";
          }
        } else {
          // never - exception thrown when action is unknown
          msg = "Internal error";
        }
        // write response
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.print("<html><head><title>");
        out.print(msg);
        out.print(
            "</title></head>"+
            "<body>\n"+
            "<center><h1>");
        out.print(msg);
        out.print("</h1></center><p>\n");
        writeParameters(out);
        out.print("</body></html>\n");
        out.close();
      }

      private void writeParameters(
          PrintWriter out) throws IOException {
        out.print(
            "<form method=\"GET\" action=\"");
        out.print(request.getRequestURI());
        out.print(
            "\">\n"+
            "<table>\n"+
            "<tr><td>"+
            "Action"+
            "</td><td>\n"+
            "<select name=\""+
            ACTION_PARAM+
            "\">");
        for (int i = 0; i < VALID_ACTIONS.length; i++) {
          String ai = VALID_ACTIONS[i];
          out.print(
              "<option value=\"");
          out.print(ai);
          out.print("\"");
          if (ai.equalsIgnoreCase(action)) {
            out.print(" selected");
          }
          out.print(">");
          out.print(ai);
          out.print("</option>");
        }
        out.print(
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Target"+
            "</td><td>\n"+
            "<select name=\""+
            TARGET_PARAM+
            "\">"+
            "<option value=\"agent\"");
        if ("agent".equalsIgnoreCase(target)) {
          out.print(" selected");
        }
        out.print(
            ">agent ");
        out.print(agentId);
        out.print(
            "</option>"+
            "<option value=\"node\"");
        if ("node".equalsIgnoreCase(target)) {
          out.print(" selected");
        }
        out.print(
            ">node ");
        out.print(nodeId);
        out.print(
            "</option>"+
            "</select>\n"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Insertion point"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            INSERTION_POINT_PARAM+
            "\" size=70");
        if (insertionPoint != null) {
          out.print(" value=\"");
          out.print(insertionPoint);
          out.print("\"");
        }
        out.print(
            "> <i>(required)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Classname"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            CLASSNAME_PARAM+
            "\" size=70");
        if (classname != null) {
          out.print(" value=\"");
          out.print(classname);
          out.print("\"");
        }
        out.print(
            "> <i>(required)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Parameters"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            PARAMETERS_PARAM+
            "\" size=70");
        if (parameters != null) {
          out.print(" value=\"");
          int n = (parameters.size() - 1);
          for (int i = 0; i <= n; i++) {
            out.print(parameters.get(i));
            if (i < n) {
              out.print(", ");
            }
          }
          out.print("\"");
        }
        out.print(
            "> <i>(optional comma-separated list)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Component name"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            COMPONENT_NAME_PARAM+
            "\" size=70");
        if (compName != null) {
          out.print(" value=\"");
          out.print(compName);
          out.print("\"");
        }
        out.print(
            "> <i>(defaults to the classname)</i>"+
            "</td></tr>\n"+
            "<tr><td>"+
            "Codebase URL"+
            "</td><td>"+
            "<input type=\"text\" name=\""+
            CODEBASE_PARAM+
            "\"");
        if (codebase != null) {
          out.print(" value=\"");
          out.print(codebase);
          out.print("\"");
        }
        out.print(
            " size=70>  <i>(optional; see bug 1029)</i>"+
            "</td></tr>\n"+
            "<tr><td colwidth=2>"+
            "<input type=\"submit\" value=\"Submit\">"+
            "</td></tr>\n"+
            "</table>\n"+
            "</form>\n");
      }

      private ComponentDescription createComponentDescription() {
        // convert codebase to url
        URL codebaseURL;
        if (codebase != null) {
          try {
            codebaseURL = new URL(codebase);
          } catch (MalformedURLException badUrlE) {
            throw new IllegalArgumentException(
                "Illegal codebase URL: "+badUrlE);
          }
        } else {
          codebaseURL = null;
        }

        // create a new ComponentDescription
        ComponentDescription desc =
          new ComponentDescription(
              compName,
              insertionPoint,
              classname,
              codebaseURL,
              parameters,
              null,  // certificate
              null,  // lease
              null); // policy
        return desc;
      }
    }
  }
}
