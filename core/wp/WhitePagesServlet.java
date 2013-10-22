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

package org.cougaar.core.wp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Cert;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.servlet.ComponentServlet;

/**
 * This component loads the optional "/wp" servlet for viewing and
 * altering the white pages.
 * <p>
 * Load into any agent:<pre>
 *   &lt;component class="org.cougaar.core.wp.WhitePagesServlet"&gt;
 *     &lt;argument&gt;/wp&lt;/argument&gt;
 *   &lt;/component&gt;
 * </pre>
 * <p>
 * For starters, just click on "submit" to do a recursive
 * white pages dump.
 */
public class WhitePagesServlet extends ComponentServlet {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

// default "recursive_dump" depth-first recursion limit
  private static final int DEFAULT_LIMIT = 3;

  private LoggingService log;
  private WhitePagesService wps;

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  @Override
public void doGet(
      HttpServletRequest sreq,
      HttpServletResponse sres) throws IOException {
    // create a new handler per request, so we don't mangle our
    // per-request variables
    MyHandler h = new MyHandler(getEncodedAgentName(), log, wps);
    h.execute(sreq, sres);  
  }

  private static class MyHandler {

    private final String localAgent;
    private final LoggingService log;
    private final WhitePagesService wps;

    private HttpServletRequest sreq;
    private PrintWriter out;

    private String action;
    private String s_minAge;
    private long minAge;
    private String s_timeout;
    private long timeout;
    private boolean async;
    private String s_limit;
    private String name;
    private String type;
    private String s_uri;
    private String s_cert;
    private boolean cacheOnly;

    public MyHandler(
        String localAgent,
        LoggingService log,
        WhitePagesService wps) {
      this.localAgent = localAgent;
      this.log = log;
      this.wps = wps;
    }

    public void execute(
        HttpServletRequest sreq, 
        HttpServletResponse sres) throws IOException
      //, ServletException 
    {
      this.sreq = sreq;
      this.out = sres.getWriter();
      parseParams();
      printHeader();
      printForm();
      performRequest();
      printFooter();
    }

    private void parseParams() {
      action = param("action");
      s_minAge = param("minAge");
      if (s_minAge != null) {
        minAge = Long.parseLong(s_minAge);
      }
      s_timeout = param("timeout");
      if (s_timeout != null) {
        timeout = Long.parseLong(s_timeout);
      }
      async = "true".equals(param("async"));
      s_limit = param("limit");
      name = param("name");
      type = param("type");
      s_uri = param("uri");
      s_cert = param("cert");
      cacheOnly = "true".equals(param("cacheOnly"));
    }

    private void printHeader() {
      long now = System.currentTimeMillis();
      out.println(
          "<html>\n"+
          "<head>\n"+
          "<title>Cougaar White Pages</title>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function selectOp() {\n"+
          "  var i = document.f.action.selectedIndex;\n"+
          "  var s = document.f.action.options[i].text;\n"+
          "  var showLimit = (s == \"recursive_dump\");\n"+
          "  var showMinAge =\n"+
          "     (s == \"uncache\" ||\n"+
          "      s == \"prefetch\");\n"+
          "  var showType  = \n"+
          "     (s == \"get\" ||\n"+
          "      s == \"uncache\" ||\n"+
          "      s == \"prefetch\" ||\n"+
          "      s == \"bind\" ||\n"+
          "      s == \"rebind\" ||\n"+
          "      s == \"unbind\");\n"+
          "  var showURI =\n"+
          "     (s == \"uncache\" ||\n"+
          "      s == \"prefetch\" ||\n"+
          "      s == \"bind\" ||\n"+
          "      s == \"rebind\" ||\n"+
          "      s == \"unbind\");\n"+
          "  var showCert = showURI;\n"+
          "  enable(showLimit,  document.f.limit)\n"+
          "  enable(showMinAge, document.f.minAge)\n"+
          "  enable(showType,   document.f.type)\n"+
          "  enable(showURI,    document.f.uri)\n"+
          "  enable(showCert,   document.f.cert)\n"+
          "}\n"+
        "function enable(b, txt) {\n"+
        "  txt.disabled=!b;\n"+
        "  txt.style.background=(b?\"white\":\"grey\");\n"+
        "}\n"+
        "// -->\n"+
        "</script>\n"+
        "</head>\n"+
        "<body onLoad=selectOp()>"+
        "<h2>Cougaar White Pages Servlet</h2>"+
        "<p>\n"+
        "Agent: "+localAgent+
        "<p>\n"+
        "Time:  "+now+" ("+(new Date(now))+")"+
        "<p>");
    }

    private void printForm() {
      out.println(
        "<table border=1>\n"+
        "<form name=\"f\" method=\"GET\" action=\""+
        sreq.getRequestURI()+
        "\">\n"+
        "<tr><th colspan=2>Request</th></tr>\n"+
        "<tr><td>Action</td><td>"+
        "<select name=\"action\" onChange=\"selectOp()\">\n"+
        option("recursive_dump", action)+
        option("get", action)+
        option("getAll", action)+
        option("list", action)+
        option("uncache", action)+
        option("prefetch", action)+
        option("bind", action)+
        option("rebind", action)+
        option("unbind", action)+
        "</select>\n"+
        "</td><tr>\n"+
        "<tr><td>Cache-only</td><td>"+
        "<select name=\"cacheOnly\">"+
        "<option value=\"false\""+
        (cacheOnly ? "" : " selected") +
        ">false</option>\n"+
        "<option value=\"true\""+
        (cacheOnly ? " selected" : "") +
        ">true</option>\n"+
        "</select>\n"+
        "</td></tr>\n"+
        "<tr><td>Min. Age</td><td>"+
        input("minAge", s_minAge)+
        "</td></tr>\n"+
        "<tr><td>Timeout</td><td>"+
        input("timeout", s_timeout)+
        "</td></tr>\n"+
        "<tr><td>Blocking</td><td>"+
        "<select name=\"async\">"+
        "<option value=\"false\""+
        (async ? "" : " selected") +
        ">Wait for response</option>\n"+
        "<option value=\"true\""+
        (async ? " selected" : "") +
        ">Don't wait (async)</option>\n"+
        "</select>\n"+
        "</td></tr>\n"+
        "<tr><td>Recursion limit</td><td>\n"+
        input("limit", s_limit, 4)+
        "&nbsp;<i>(&lt; 0 is none, default is "+
        DEFAULT_LIMIT+
        ")</i>"+
        "</td><tr>\n"+
        "<tr><td>Entry</td><td>\n"+
        "<table border=1>\n"+
        "<tr><td>Name</td><td>"+
        input("name", name)+
        "</td></tr>\n<tr><td>Type</td><td>"+
        input("type", type)+
        "</td></tr>\n<tr><td>URI</td><td>"+
        input("uri", s_uri, 40)+
        "</td></tr>\n<tr><td>Cert</td><td>"+
        input("cert", s_cert, 40)+
        "</td></tr>\n</table>\n"+
        "</td></tr>\n"+
        "<tr><td>"+
        "<input type=\"submit\" value=\"Submit\">\n"+
        "</td><td>"+
        "<input type=\"reset\" value=\"Reset\">\n"+
        "</td></tr>\n"+
        "</form>\n"+
        "</table>");
    }

    private void performRequest() {
      try {
        Request req = null;
        int options = 
          (cacheOnly ?
           Request.CACHE_ONLY :
           Request.NONE);
        if ("recursive_dump".equals(action)) {
          String suffix = (name == null ? "." : name);
          if (suffix.startsWith(".")) {
            int limit = DEFAULT_LIMIT;
            if (s_limit != null) {
              limit = Integer.parseInt(s_limit);
            }
            submitDump(suffix, limit);
          } else {
            out.println(
                "<font color=\"red\">Recursive dump suffix must"+
                " start with \".\", not \""+suffix+"\"</font>");
          }
        } else if ("get".equals(action)) {
          if (name == null) {
            out.println(
                "<font color=\"red\">Please specify a name</font>");
          } else if (type == null) {
            out.println(
                "<font color=\"red\">Please specify a type</font>");
          } else {
            req = new Request.Get(options, name, type);
          }
        } else if ("getAll".equals(action)) {
          if (name == null) {
            out.println(
                "<font color=\"red\">Please specify a name</font>");
          } else {
            req = new Request.GetAll(options, name);
          }
        } else if ("list".equals(action)) {
          String tmp = (name == null ? "." : name);
          if (tmp.startsWith(".")) {
            req = new Request.List(options, tmp);
          } else {
            out.println(
                "<font color=\"red\">List suffix must start"+
                " with \".\", not \""+tmp+"\"</font>");
          }
        } else if (
            "uncache".equals(action) ||
            "prefetch".equals(action)) {
          boolean uncache = "uncache".equals(action);
          boolean prefetch = "prefetch".equals(action);
          AddressEntry ae = 
            ((type==null || s_uri==null) ?
             (null) :
             parseEntry());
          if (name == null) {
            out.println(
              "<font color=\"red\">Please specify name</font>");
          } else {
            req = new Request.Flush(
                Request.CACHE_ONLY, name, minAge, ae,
                uncache, prefetch);
          }
        } else if (
            "bind".equals(action) ||
            "rebind".equals(action)) {
          boolean overWrite = "rebind".equals(action);
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Bind(Request.NONE, ae, overWrite, false);
          }
        } else if ("unbind".equals(action)) {
          AddressEntry ae = parseEntry();
          if (ae != null) {
            req = new Request.Unbind(Request.NONE, ae);
          }
        } else if (action != null) {
          out.println(
              "<font color=\"red\">Unknown action: "+action+"</font>");
        }
        if (req != null) {
          submit(req);
        }
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    }
    
    private void printFooter() {
      out.println("</body></html>");
    }

    private String param(String n) {
      String s = sreq.getParameter(n);
      if (s==null || s.length()==0) {
        s = null;
      }
      return s;
    }

    private static String option(String n, String v) {
      return 
        "<option value=\""+n+"\""+
        ((v!=null && v.equals(n)) ? " selected" : "")+
        ">"+n+"</option>";
    }

    private static String input(String n, String v) {
      return input(n, v, 40);
    }

    private static String input(String n, String v, int size) {
      return
        "<input type=\"text\" size="+size+" name=\""+n+"\""+
        (v==null?"":" value=\""+v+"\"")+">";
    }

    private AddressEntry parseEntry() throws Exception {
      String x_uri = s_uri;
      if ("unbind".equals(action) && s_uri == null) {
        x_uri = "ignored://wp-servlet";
      }
      if (name==null || type==null || x_uri==null) {
        out.println("<font color=\"red\">Missing required field</font>");
        return null;
      }
      if (name.startsWith(".")) {
        out.println("<font color=\"red\">Name can't start with \".\"</font>");
        return null;
      }
      URI uri = URI.create(s_uri);
      Cert cert;
      if (s_cert == null ||
          "null".equals(s_cert) ||
          "null_cert".equals(s_cert)) {
        cert = Cert.NULL;
      } else {
        cert = new Cert.Indirect(s_cert);
      }
      AddressEntry ae = AddressEntry.getAddressEntry(name, type, uri, cert);
      return ae;
    }

    private void submitDump(
        String suffix, int limit) throws Exception {
      out.println("<p><hr><p>");
      if (wps == null) {
        out.println(
            "<font color=\"red\">No WhitePagesService?</font>");
        return;
      }
      printTableStart();
      recurseDump(suffix, 0, limit);
      printTableEnd();
    }

    // recursive!
    private int recurseDump(
        String suffix, int idx, int limit) throws Exception {
      int newIdx = idx;
      if (limit == 0) {
        ++newIdx;
        out.println(
            "<tr>"+
            "<td align=right>"+newIdx+"&nbsp;</td>"+
            "<td colspan=4><a href=\""+
            sreq.getRequestURI()+
            "?action=recursive_dump"+
            "&cacheOnly="+cacheOnly+
            "&name="+suffix+
            (type == null ? "" : ("&type="+type))+
            (s_uri == null ? "" : ("&uri="+s_uri))+
            (s_timeout == null ? "" : ("&timeout="+s_timeout))+
            (s_limit == null ? "" : ("&limit="+s_limit))+
            "\">"+suffix+"</a></td>"+
            "</tr>");
        return newIdx;
      }
      Set names = wps.list(suffix, timeout);
      int n = (names == null ? 0 : names.size());
      if (n <= 0) {
        ++newIdx;
        out.println(
            "<tr>"+
            "<td align=right>"+newIdx+"&nbsp;</td>"+
            "<td colspan=4>None listed for \""+suffix+"\"</td>"+
            "</tr>");
        return newIdx;
      }
      List l = new ArrayList(names);
      Collections.sort(l);
      for (int i = 0; i < n; i++) {
        String s = (String) l.get(i);
        if (s == null) {
        } else if (s.length() > 0 && s.charAt(0) == '.') {
          newIdx = recurseDump(s, newIdx, (limit - 1));
        } else {
          Map m = wps.getAll(s, timeout);
          newIdx = print(m, newIdx);
        }
      }
      return newIdx;
    }

    private void submit(Request req) {
      out.println("<p><hr><p>");
      if (wps == null) {
        out.println(
            "<font color=\"red\">No WhitePagesService?</font>");
        return;
      }
      Response res = wps.submit(req);
      if (async) {
        Callback c = new Callback() {
          public void execute(Response res) {
            if (log != null && log.isInfoEnabled()) {
              log.info(localAgent+": "+res);
            }
          }
        };
        res.addCallback(c);
        out.println("Submitted asynchronous request");
      } else {
        try {
          res.waitForIsAvailable(timeout);
        } catch (InterruptedException ie) {
          out.println(
              "<p><font color=\"red\">Interruped: "+
              ie+"</font><p>");
        }
        print(res);
      }
    }

    private void print(Response res) {
      out.print("<b>"+action+":</b><br>");
      if (!res.isAvailable()) {
        out.println("Not available yet");
      } else if (res.isTimeout()) {
        out.print("Timeout");
      } else if (!res.isSuccess()) {
        print(res.getException());
      } else {
        // success:
        if (res instanceof Response.Get) {
          AddressEntry ae = ((Response.Get) res).getAddressEntry();
          print(ae);
        } else if (res instanceof Response.GetAll) {
          Map m = ((Response.GetAll) res).getAddressEntries();
          print(m);
        } else if (res instanceof Response.List) {
          Set names = ((Response.List) res).getNames();
          print(names);
        } else if (res instanceof Response.Bind) {
          Response.Bind bres = (Response.Bind) res;
          if (bres.didBind()) {
            long leaseExpireTime = bres.getExpirationTime();
            out.print(
                "Bind succeded, lease renewal due at "+
                leaseExpireTime+" ("+
                (new Date(leaseExpireTime))+")");
          } else {
            AddressEntry usurperEntry = bres.getUsurperEntry();
            if (usurperEntry == null) {
              out.println("Bind failed, reason is unknown");
            } else {
              out.println("Bind failed, usurper entry is:<br>");
              print(usurperEntry);
            }
          }
        } else if (res instanceof Response.Unbind) {
          if (((Response.Unbind) res).didUnbind()) {
            out.println("Unbind succeeded");
          } else {
            out.println("Unbind failed?");
          }
        } else {
          out.println(res);
        }
      }
    }

    private void printTableStart() {
      out.print(
          "<table border=1>\n"+
          "<tr>"+
          "<th>&nbsp;</th>"+
          "<th>Name</th>"+
          "<th>Type</th>"+
          "<th>URI</th>"+
          "<th>Cert</th>"+
          "</tr>\n");
    }

    private void printTableEnd() {
      out.println("</table>");
    }

    private void print(Map m) {
      printTableStart();
      print(m, 0);
      printTableEnd();
    }

    private void print(AddressEntry ae) {
      printTableStart();
      print(ae, 0);
      printTableEnd();
    }

    private int print(
        Map m,
        int idx) {
      int ret = idx;
      if (m != null && !m.isEmpty()) {
        for (Iterator iter = m.values().iterator();
            iter.hasNext();
            ) {
          AddressEntry ae = (AddressEntry) iter.next();
          if (print(ae, ret)) {
            ++ret;
          }
        }
      }
      return ret;
    }

    private boolean print(
        AddressEntry ae,
        int idx) {
      if (ae != null) {
        out.print(
            "<tr>"+
            "<td align=right>"+(idx+1)+"&nbsp;</td>"+
            "<td align=right>"+ae.getName()+"</td>"+
            "<td align=right>"+ae.getType()+"</td>"+
            "<td>"+ae.getURI()+"</td>"+
            "<td align=right>"+ae.getCert()+"</td>"+
            "</td></tr>\n");
        return true;
      }
      return false;
    }

    private void print(Set names) {
      int n = (names==null ? 0 : names.size());
      out.println("Names["+n+"]:<br>");
      if (n <= 0) {
        return;
      }
      out.println("<table border=0>\n<li>");
      List l = new ArrayList(names);
      Collections.sort(l);
      for (int i = 0; i < n; i++) {
        String ni = (String) l.get(i);
        out.print(
            "<tr><td align=right>"+
            (i+1)+
            ".&nbsp;</td>"+
            "<td align=right>");
        if (ni == null) {
          out.print("<i>null?</i>");
        } else if (ni.length() == 0) {
          out.print("<i>empty?</i>");
        } else if (ni.charAt(0) != '.') {
          out.print(
              "<a href=\""+
              sreq.getRequestURI()+
              "?action=get&name="+
              ni+"\">"+ni+"</a>");
        } else {
          out.print(
              "<a href=\""+
              sreq.getRequestURI()+
              "?action=list&name="+
              ni+"\">"+ni+"</a>");
        }
        out.println("</td></tr>\n");
      }
      out.println("</table>");
    }

    private void print(Exception e) {
      out.println("Failure</td><td>");
      if (e == null) {
        out.print("null");
        return;
      }
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }
  }
}
