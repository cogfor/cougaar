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

package org.cougaar.core.servlet;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility methods that may assist in writing Servlets.
 */
public final class ServletUtil {

  private ServletUtil() {
    // just utility functions.
  }

  /**
   * Get the "/$name" encoded Agent name from the request path.
   */
  public static String getEncodedAgentName(
      HttpServletRequest request) 
  {
    String uri = request.getRequestURI();

    // return everything after the '$' & before the '/' 
    String name = new String();
    uri = uri.substring(uri.indexOf('$')+1);
    name = uri.substring(0, uri.indexOf('/'));
    return name;
  }

  /**
   * Get the path after the "/$name".
   */
  public static String getPath(
      HttpServletRequest request) 
  {
    // return everything beyond $name/ in the uri
    String uri = request.getRequestURI();
    int begin = uri.indexOf('/', 2);
    return uri.substring(begin);
  }

  /**
   * Encode a String for HTML.
   * <p>
   * The String should only be one line (i.e. no CRLFs).
   * <pre>
   * Converts:
   *   &amp; to &amp;amp;
   *   &lt; to &amp;lt;
   *   &gt; to &amp;gt;
   * </pre>
   */
  public static final String encodeForHTML(final String s) {
    int slen = ((s != null) ? s.length() : 0);
    StringBuffer sb = new StringBuffer((int)(1.10*slen));
    for (int i = 0; i < slen; i++) {
      char ci = s.charAt(i);
      switch (ci) {
        default:
          if ((ci >= ' ') && (ci <= '~')) {
            sb.append(ci);
          } else {
            // unsupported?
            sb.append("?");
          }
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
      }
    }
    return sb.toString();
  }

  /**
   * Encode a String for Java.
   * <pre>
   * Converts:
   *   " to \\\"
   *   \ to \\
   *   CRLF to \\n
   * </pre>
   * Can be used to create javascript pages.
   */
  public static final String encodeForJava(final String s) {
    int slen = ((s != null) ? s.length() : 0);
    StringBuffer sb = new StringBuffer((int)(1.10*slen));
    for (int i = 0; i < slen; i++) {
      char ci = s.charAt(i);
      switch (ci) {
        default:
          if ((ci >= ' ') && (ci <= '~')) {
            sb.append(ci);
          } else {
            // unsupported?
            sb.append("?");
          }
          break;
        case '\"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\n':
          sb.append("\\n");
          break;
      }
    }
    return sb.toString();
  }

  /**
   * Simplify the parsing of URL parameters.
   *
   * @see ParamVisitor inner-class defined at the end of this class
   */
  public static void parseParams(
      ParamVisitor vis, 
      HttpServletRequest req)
  {  
    Map m = req.getParameterMap();
    parseParams(vis, m);
  }

  /**
   * Given a <code>Map</code> of (name, value) pairs, call back 
   * to the given <code>ParamVisitor</code>'s "setParam(name,value)"
   * method.
   *
   * @see ParamVisitor inner-class defined at the end of this class
   */
  public static void parseParams(
      ParamVisitor vis, 
      Map m) {
    if (m.isEmpty()) {
      return;
    }
    Iterator iter = m.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry me = (Map.Entry) iter.next();
      String key = (String) me.getKey();
      if (key == null) {
        continue;
      }
      String[] values = (String[]) me.getValue();
      if ((values == null) || (values.length <= 0)) {
        continue;
      }
      String value = values[0];
      if (value == null) {
        continue;
      }
      vis.setParam(key, value);      
    }
  }

  /**
   * Simple callback API for use with "setParams(..)".
   */
  public interface ParamVisitor {
    void setParam(String key, String value);
  }

}
