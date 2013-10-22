/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentView;
import org.cougaar.core.component.ContainerView;
import org.cougaar.core.component.ServiceView;
import org.cougaar.core.component.ViewService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.servlet.ComponentServlet;

/**
 * This component loads the "/components" servlet, which displays
 * information from the component model's {@link ViewService}.
 * <p>
 * Load with:<pre>
 *  &lt;component class="org.cougaar.core.util.ComponentViewServlet"&gt;
 *    &lt;argument&gt;/components&lt;/argument&gt;
 *  &lt;/component&gt;
 * </pre> 
 * <p> 
 * The supported URL-parameter arguments can easily be seen
 * in the zero-parameter <code>printUsage</code> method's page.
 * <p>
 * This servlet generates XML.  A future client-side GUI could use
 * this XML to draw pretty graphical displays of the component model.
 * Another idea is to generate output in a standard graph
 * node/edge format (e.g. GXL for client-side JGraph viewing, or
 * DOT for <a href="http://www.graphviz.org/webdot/">webdot</a>
 * PNGs).
 * <p>
 * <pre>
 * In the generated XML, there are <b>&lt;component-view&gt;</b>s and
 * <b>&lt;container-view&gt;</b>s, where the container view includes all
 * the fields of the component view, which are:
 *   <b>id=..</b> unique identifier, also useful for sorting
 *   <b>&lt;load-time&gt;</b> millis when loaded
 *   <b>&lt;component&gt;</b> the component description
 *   <b>&lt;advertised-services&gt;</b> "addService" classes and info
 *   <b>&lt;obtained-services&gt;</b> "getService" class info
 * and the <b>&lt;container-view&gt;</b> adds:
 *   <b>&lt;children&gt;</b> child component/container views.
 * The <b>&lt;advertised-services&gt;</b> contains:
 *   <b>class=..</b> service class name
 *   <b>id=..</b> unique identifier
 *   <b>&lt;advertise-time&gt;</b> timestamp when advertised, if not
 *      revoked yet
 * and <b>&lt;obtained-services&gt;</b> contains:
 *   <b>class=..</b> service class name
 *   <b>id=..</b> unique identifier
 *   <b>sp-id=..</b> if the URL parameters <i>?hideT=true</i> and
 *      <i>?hideSP=true</i> are enabled and the service provider's
 *      <b>id</b> is known
 *   <b>&lt;obtain-time&gt;</b> timestamp when obtained, if not
 *      released yet
 *   <b>&lt;service-provider&gt;</b> service provider of this service,
 *      plus the provider's <b>id</b> and <b>&lt;component&gt;</b>
 *      description.
 *   <b>&lt;indirectly-advertised-services&gt;</b> if the service
 *      contains a "get.*ServiceBroker" method,
 *      such as {@link org.cougaar.core.node.NodeControlService},
 *      this will show services indirectly advertised through
 *      that ServiceBroker.
 * </pre>
 */
public class ComponentViewServlet extends ComponentServlet {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

private LoggingService log;

  private ComponentView view;

  @Override
public void load() {
    super.load();

    log = getService(this, LoggingService.class, null);

    ViewService viewService = getService(this, ViewService.class, null);
    if (viewService != null) {
      view = viewService.getComponentView();
      releaseService(
          this, ViewService.class, viewService);
    }
    if (view == null && log.isWarnEnabled()) {
      log.warn("Unable to obtain ViewService");
    }
  }

  // find the top-most parent, which is usually the AgentManager 
  private ContainerView findRoot() {
    if (view == null) {
      return null;
    }
    ContainerView curr = view.getParentView();
    ContainerView next = curr;
    while (next != null) {
      curr = next;
      next = next.getParentView();
    }
    return curr;
  }

  @Override
public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    (new Worker(request, response)).execute();
  }

  private class Worker {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private PrintWriter out;
    private XMLWriter xml;

    private String format;
    private String agent;
    private boolean hideCV;
    private boolean hideS;
    private boolean hideSP;
    private boolean hideT;

    public Worker(
        HttpServletRequest request,
        HttpServletResponse response) {
      this.request = request;
      this.response = response;
    }

    public void execute() throws IOException {
      getParams();
      if ("xml".equals(format)) {
        printXML();
      } else {
        printUsage();
      }
    }

    private void getParams() {
      format = getParam("format");
      agent = getParam("agent");
      if ("*".equals(agent)) {
        agent = null;
      }
      hideCV = "true".equals(getParam("hideCV"));
      if (hideCV) {
        hideS = true;
        hideSP = true;
        hideT = true;
      } else {
        hideS = "true".equals(getParam("hideS"));
        hideSP = "true".equals(getParam("hideSP"));
        hideT = "true".equals(getParam("hideT"));
      }
    }
    private String getParam(String name) {
      String ret = request.getParameter(name);
      if (ret != null) {
        ret = ret.trim();
        if (ret.length() == 0) {
          ret = null;
        }
      }
      return ret;
    }

    private void printUsage() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      String title = getEncodedAgentName()+" Component Model View";
      out.println(
          "<html><head><title>"+title+
          "</title></head><body>"+
          "<h2>"+title+"</h2>\n"+
          "<p>\n"+
          "This servlet displays component model information based on"+
          " on the <code>"+
          "org.cougaar.core.component.<b>ViewService</b>"+
          "</code>.<br>\n"+
          "  In addition to showing which components are currently"+
           " loaded, this servlet can show each component's load"+
          " timestamp, advertised services, and obtained services"+
          " (including the component that advertised each obtained"+
          " service).\n"+
          "<p>\n"+
          "The minimal view to show just the component descriptions"+
          " can be enabled by selecting:<br>\n<b>"+
          "&nbsp;&nbsp;Hide all <code>&lt;component-view&gt;</code>"+
          " details</b></br>\n"+
          "<p>\n"+
          "Note that the component model will show infrastructure"+
          " components specified in the agent template (e.g. in"+
          " <code>"+
          "$COUGAAR_INSTALL_PATH/configs/common/SimpleAgent.xsl"+
          "</code>).  For more information about agent templates,"+
          " see the <a \"http://cougaar.org/doc/11_4/online/CDG_11_4.pdf\">"+
          "Cougaar Developers' Guide</a>.\n"+
          "<p>\n"+
          "<form method=\"GET\" action=\""+request.getRequestURI()+
          "\">\n"+
          "<table border=0>\n"+
          "<tr><td>Agent Filter</td><td>"+
        "<input type=\"text\" name=\"agent\" size=\"50\""+
        " value=\""+(agent == null ? "*" : agent)+"\""+
        "/></td></tr>\n"+
        "<tr><td>Hide all <code>&lt;component-view&gt;</code>"+
        " details</td><td>"+
        "<input type=\"checkbox\" name=\"hideCV\" value=\"true\""+
        (hideCV ? " checked" : "")+
        "/></td></tr>\n"+
        "<tr><td>Hide advertised/obtained services</td><td>"+
        "<input type=\"checkbox\" name=\"hideS\" value=\"true\""+
        (hideS ? " checked" : "")+
        "/></td></tr>\n"+
        "<tr><td>Hide <code>&lt;service-provider&gt;</code>"+
        " component info</td><td>"+
        "<input type=\"checkbox\" name=\"hideSP\" value=\"true\""+
        (hideSP ? " checked" : "")+
        "/></td></tr>\n"+
        "<tr><td>Hide timestamps</td><td>"+
        "<input type=\"checkbox\" name=\"hideT\""+
        " value=\"true\""+
        (hideT ? " checked" : "")+
        "/></td></tr>\n"+
        "<tr><td>&nbsp;</td><td>"+
        "<input type=\"hidden\" name=\"format\" value=\"xml\"/>\n"+
        "<input type=\"submit\" name=\"action\" value=\"Submit\"/>\n"+
        "</td></tr>\n"+
        "</table></form>\n"+
        "</body></html>\n");
      out.flush();
      out.close();
    }

    private void printXML() throws IOException {
      response.setContentType("text/xml");
      out = response.getWriter();
      xml = new XMLWriter(out);
      xml.header();
      printComments();
      xml.begin("component-model-view");
      xml.attr("agent", getEncodedAgentName());
      ContainerView root = findRoot();
      printView(root);
      xml.end("component-model-view");
      out.flush();
      out.close();
    }

    private void printComments() {
      xml.comment("Cougaar component model view");
      if (agent != null) {
        xml.comment(
            "Filtering to only show agent \'"+agent+"\'");
      }
      if (hideCV) {
        xml.comment(
            "Hiding component-view details");
      } else {
        if (hideS) {
          xml.comment(
              "Hiding advertised/obtained Service detail");
        }
        if (hideSP) {
          xml.comment(
              "Hiding ServiceProvider component detail");
        }
        if (hideT) {
          xml.comment(
              "Hiding timestamps");
        }
      }
    }

    // recursive!  print a view, do depth-first print of children
    private void printView(ComponentView v) {
      if (v == null) {
        return;
      }
      boolean isContainer = (v instanceof ContainerView);
      String tag =
        (isContainer ? "container-view" :
         hideCV ? null :
         "component-view");
      if (tag != null) {
        xml.begin(tag);
        int id = (hideCV ? 0 : v.getId());
        if (id > 0) {
          xml.attr("id", id);
        }
      }
      long timestamp = (hideT ? 0 : v.getTimestamp());
      if (timestamp > 0) {
        xml.value("load-time", timestamp);
      }
      ComponentDescription desc = v.getComponentDescription();
      if (desc == null) {
        xml.comment(
            "component loaded by \'container.add(Object)\'?");
      } else {
        printDesc(desc);
        if (agent != null && !isAgent(desc)) {
          xml.comment("skipping this agent, it is not \'"+agent+"\'");
          xml.end(tag);
          return;
        }
      }
      //v.getParentView();
      if (!hideS) {
        printServices(
            "advertised-services", v.getAdvertisedServices(), true);
        printServices(
            "obtained-services", v.getObtainedServices(), false);
      }
      if (isContainer) {
        ContainerView contV = (ContainerView) v;
        List l = contV.getChildViews();
        int n = (l == null ? 0 : l.size());
        if (n > 0) {
          xml.begin("children");
          for (int i = 0; i < n; i++) {
            Object o = l.get(i);
            if (o instanceof ComponentView) {
              // recurse!
              printView((ComponentView) o);
            }
          }
          xml.end("children");
        }
      }
      if (tag != null) {
        xml.end(tag);
      }
    }

    // print a component description
    private void printDesc(ComponentDescription desc) {
      String name = desc.getName();
      String classname = desc.getClassname();
      int priority = desc.getPriority();
      String ip = desc.getInsertionPoint();
      Object param = desc.getParameter();
      List args = 
        (param instanceof List ?
         (List) param : 
         ((param instanceof String ||
           param instanceof MessageAddress) ?
          Collections.singletonList(param) :
          null));
      int n = (args == null ? 0 : args.size());
      // filter out boring defaults:
      if (isDefaultName(name, classname, args)) {
        name = null;
      }
      String priorityString =
        (priority == ComponentDescription.PRIORITY_COMPONENT ?
         (null) :
         ComponentDescription.priorityToString(priority));
      if (ip != null &&
          ip.equals("Node.AgentManager.Agent.PluginManager.Plugin")) {
        ip = null;
      }
      // print it:
      xml.begin("component");
      if (name != null) {
        xml.attr("name", name);
      }
      if (classname != null) {
        xml.attr("class", classname);
      }
      if (priorityString != null) {
        xml.attr("priority", priorityString);
      }
      if (ip != null) {
        xml.attr("insertionpoint", ip);
      }
      for (int i = 0; i < n; i++) {
        Object o = args.get(i);
        if (o instanceof MessageAddress) {
          o = ((MessageAddress) o).getAddress();
        }
        if (o instanceof String) {
          String s = (String) o;
          xml.value("argument", s);
        }
      }
      xml.end("component");
    }

    // recursive!  print advertised/obtained services:
    private void printServices(
        String tag, Map m, boolean isAdvertised) {
      int n = (m == null ? 0 : m.size());
      if (n <= 0) {
        return;
      }
      xml.begin(tag);
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        Object key = me.getKey();
        String classname = 
          (key instanceof Class ? ((Class) key).getName() : key+"?");
        Object value = me.getValue();
        ServiceView sv = 
          (value instanceof ServiceView ?
           ((ServiceView) value) :
           null);
        int id = (sv == null ? 0 : sv.getId());
        int providerId = 
          (sv == null ? 0 : sv.getProviderId());
        xml.begin("service");
        xml.attr("class", classname);
        if (id > 0) {
          xml.attr("id", id);
        }
        if (hideSP && hideT && providerId > 0) {
          xml.attr("sp-id", providerId);
        }
        if (sv != null) {
          long timestamp = (hideT ? 0 : sv.getTimestamp());
          ComponentDescription providerDesc = 
            (hideSP ? 
             (null) :
             sv.getProviderComponentDescription());
          Map indirects = sv.getIndirectlyAdvertisedServices();
          if (timestamp > 0) {
            String timeTag = 
              (isAdvertised ? "advertise" : "obtain")+"-time";
            xml.value(timeTag, timestamp);
          }
          if (!(hideSP && hideT) &&
              (providerId > 0 || providerDesc != null)) {
            xml.begin("service-provider");
            if (providerId > 0) {
              xml.attr("id", providerId);
            }
            if (providerDesc != null) {
              printDesc(providerDesc);
            }
            xml.end("service-provider");
          }
          if (indirects != null) {
            // recurse!
            printServices(
                "indirectly-advertised-services", indirects, true);
          }
        }
        xml.end("service");
      }
      xml.end(tag);
    }

    // if this is an AgentImpl, does the first arg match our "agent"?
    private boolean isAgent(ComponentDescription desc) {
      if (agent == null ||
          desc == null || 
          !("org.cougaar.core.agent.AgentImpl".equals(
              desc.getClassname()))) {
        return true;
      }
      String name;
      Object o = desc.getParameter();
      if (o instanceof List) {
        List l = (List) o;
        Object o2 = (l.isEmpty() ? null : l.get(0));
        name = 
          (o2 == null ? null :
           o2 instanceof String ? (String) o2 :
           o2 instanceof MessageAddress ? 
           ((MessageAddress) o2).getAddress() :
           null);
      } else if (o instanceof String) {
        name = (String) o;
      }  else if (o instanceof MessageAddress) {
        name = ((MessageAddress) o).getAddress();
      } else {
        name = null;
      }
      return agent.equals(name);
    }

    // compare name to "classname("+comma_separated_args+")"
    private boolean isDefaultName(
        String name, String classname, List args) {
      if (name == null) {
        return true;
      }
      if (!name.startsWith(classname)) {
        return false;
      }
      String tail = name.substring(classname.length());
      int n = (args == null ? 0 : args.size());
      if (n <= 0) {
        return tail.equals("") || tail.equals("()");
      }
      StringBuffer buf = new StringBuffer();
      buf.append('(');
      for (int i = 0; ; ) {
        buf.append(args.get(i));
        if (++i >= n) {
          break;
        }
        buf.append(',');
      }
      buf.append(')');
      String s = buf.toString();
      return tail.equals(s);
    }
  }
}
