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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.bootstrap.XURLClassLoader;
import org.cougaar.core.servlet.ComponentServlet;

/**
 * This component loads the "/showJars" servlet, which displays
 * the jars loaded by the agent's ClassLoader.
 * <p>
 * Load with:<pre>
 *  &lt;component class="org.cougaar.core.util.CheckJarsServlet"&gt;
 *    &lt;argument&gt;/showJars&lt;/argument&gt;
 *  &lt;/component&gt;
 * </pre> 
 */
public class CheckJarsServlet extends ComponentServlet {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

@Override
public void doGet(
      HttpServletRequest request,
      HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head><title>");
    out.println("Jar version information from agent "+
        getEncodedAgentName());
    out.println("</title></head>");
    out.println("<body>");
    ClassLoader cl = this.getClass().getClassLoader();
    while (cl != null) {
      out.println("<h2>");
      out.println("Classloader <em>"+cl+"</em>");
      out.println("</h2>");
      List info = scanClassLoader(cl);
      report(out, info);
      cl = cl.getParent();
    }
    out.println("</body>");
    out.println("</html>");
    out.close();
  }
    
  private static void report(PrintWriter out, List info) {
    if (info.size()==0) return;
    out.println("<table border=1>");
    out.println("<tr><th>NAME</th><th>URL</th><th>COMMENT</th><th>TAG</th><th>TIME</th><th>ATIME</th></tr>");
    for (int i=0, l = info.size(); i<l; i++) {
      Map m = (Map) info.get(i);
      out.println("<tr>");
      out.println("<td>"+ empty(m.get("NAME")) + "</td>");
      out.println("<td>"+ empty(m.get("URL")) + "</td>");
      out.println("<td>"+ empty(m.get("COMMENT")) + "</td>");
      out.println("<td>"+ empty(m.get("REPOSITORY_TAG")) + "</td>");
      out.println("<td>"+ empty(m.get("REPOSITORY_TIME")) + "</td>");
      out.println("<td>"+ empty(m.get("ARCHIVE_TIME")) + "</td>");
      out.println("</tr>");
    }
    out.println("</table>");
  }

  private static String empty(Object s) {
    return (s==null)?"":(s.toString());
  }

  private static List scanClassLoader(ClassLoader cl) {
    ArrayList list = new ArrayList(11);

    URL[] urls = null;
    if (cl instanceof URLClassLoader) {
      urls = ((URLClassLoader)cl).getURLs();
    } else if (cl instanceof XURLClassLoader) {
      urls = ((XURLClassLoader)cl).getURLs();
    }

    if (urls != null) {
      for (int i=0; i<urls.length; i++) {
        Map m = checkURL(cl, urls[i]);
        if (m != null) {
          list.add(m);
        }
      }
    }

    return list;
  }

  private static Pattern jarPattern = Pattern.compile(".*[:/]([^:/]+)\\.(?:jar|zip|plugin)", Pattern.CASE_INSENSITIVE);
  private static Pattern varPattern = Pattern.compile("(\\w+)=(.*)");

  private static Map checkURL(ClassLoader cl, URL url) {
    String us = url.toString();

    HashMap map = new HashMap(7);
    // add the url info
    map.put("URL", us);

    Matcher m = jarPattern.matcher(us);
    if (m.lookingAt()) {
      String arg = m.group(1);
      map.put("NAME", arg);
      try {
        InputStream is = cl.getResourceAsStream("Manifest/"+arg+".version");
        if (is != null) {
          BufferedReader r = new BufferedReader(new InputStreamReader(is));
          String line;
          while ( (line = r.readLine()) != null) {
            Matcher lm = varPattern.matcher(line);
            if (lm.lookingAt()) {
              String var = lm.group(1);
              String val = lm.group(2);
              if ("NAME".equals(var)) {
                if (!(arg.equals(val))) {
                  map.put(var, val+"!");
                } // else no-op
              } else {
                map.put(var, val);
              }
            }
          }
          r.close();
        }
      } catch (Exception e) {
        map.put("NAME", arg+"?");
      }
    } else {
      map.put("NAME", "?");
    }
    return map;
  }
}

 
