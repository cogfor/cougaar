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

package org.cougaar.core.logging;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.log4j.DetailPriority;
import org.cougaar.util.log.log4j.ShoutPriority;

/**
 * This component is a {@link Servlet} that allows clients to view
 * and modify the {@link org.cougaar.core.service.LoggingService}
 * configuration.
 */
public class LoggingConfigServlet extends BaseServletComponent {

  @Override
protected String getPath() {
    return "/log";
  }

  @Override
protected Servlet createServlet() {
    return new MyServlet();
  }

  // servlet:

  private class MyServlet extends HttpServlet {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Override
   public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {

      String action = req.getParameter("action");
      String getlog = req.getParameter("getlog");
      String setlog = req.getParameter("setlog");
      String setlevel = req.getParameter("level");
      if (getlog != null) getlog = getlog.trim();
      if (setlog != null) setlog = setlog.trim();
      if (setlevel != null) setlevel = setlevel.trim();
      if ("".equals(getlog)) getlog = null;
      if ("".equals(setlog)) setlog = null;
      int isetlevel = 
        (setlevel != null ? 
         convertStringToInt(setlevel) :
         -1);

      res.setContentType("text/html");
      PrintWriter out = res.getWriter();

      out.print(
          "<html><head><title>"+
          "Logger Configuration"+
          "</title></head><body>\n"+
          "<h2>Logger Configuration</h2><p>");

      if (getlog != null && 
          "Get".equalsIgnoreCase(action)) { 
        // get the level
        Exception e = null;
        int l = -1;
        try {
          l = getLevel(getlog);
        } catch (Exception e2) {
          e = e2;
        }
        if (l >= 0 && e == null) {
          out.print(
              "Level for \""+getlog+"\" is "+
              convertIntToString(l));
        } else {
          out.print(
              "Unable to get logging level of \""+
              getlog+"\": ");
          if (e == null) {
            out.print("invalid name");
          } else {
            e.printStackTrace(out);
          }
        }
      } else if (
          setlog != null && 
          setlevel != null && 
          "Set".equalsIgnoreCase(action)) {
        // set the level
        Exception e = null;
        try {
          setLevel(setlog, isetlevel);
        } catch (Exception e2) {
          e = e2;
        }
        if (e == null) {
          out.print(
              "Set \""+setlog+"\" to "+
              convertIntToString(isetlevel));
        } else {
          out.print(
              "Unable to change logging level of \""+
              setlog+"\": ");
          e.printStackTrace(out);
        }
//        } else {
//          // Neither get or set invoked. Print a Usage message?
      }

      out.print(
          "<br><hr>\n"+
          "<form method=\"GET\" action=\""+
          req.getRequestURI()+
          "\">\n"+
          "<input type=\"RESET\" name=\"Clear\" value=\"Clear\">"+
          "<p>\n"+
          "Get the level for "+
          "<input type=\"text\" name=\"getlog\" size=\"50\""+
          (getlog != null ? (" value=\""+getlog+"\"") : "")+
          "/>"+
          "<input type=\"submit\" name=\"action\" value=\"Get\">"+
          "<p>\n"+
          "Set the level for "+
          "<input type=\"text\" name=\"setlog\" size=\"50\""+
          (setlog != null ? (" value=\""+setlog+"\"") : "")+
          "/> to "+
          "<select name=\"level\">"+
          "<option"+
          (isetlevel == Logger.DETAIL ? " selected" : "")+">DETAIL</option>"+
          "<option"+
          (isetlevel == Logger.DEBUG ? " selected" : "")+">DEBUG</option>"+
          "<option "+
          (isetlevel == Logger.INFO ? " selected" : "")+">INFO</option>"+
          "<option"+
          (isetlevel == Logger.WARN ? " selected" : "")+">WARN</option>"+
          "<option"+
          (isetlevel == Logger.ERROR ? " selected" : "")+">ERROR</option>"+
          "<option"+
          (isetlevel == Logger.SHOUT ? " selected" : "")+">SHOUT</option>"+
          "<option"+
          (isetlevel == Logger.FATAL ? " selected" : "")+">FATAL</option>"+
          "</select>"+
          "<input type=\"submit\" name=\"action\" value=\"Set\">\n"+
          "</form></body></html>\n");
      out.close();
    }
  }

  // logger utilities:

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

  private int convertStringToInt(String s) {
    if (s == null) {
      return -1;
    } else if (s.equalsIgnoreCase("DETAIL")) {
      return Logger.DETAIL;
    } else if (s.equalsIgnoreCase("DEBUG")) {
      return Logger.DEBUG;
    } else if (s.equalsIgnoreCase("INFO")) {
      return Logger.INFO;
    } else if (s.equalsIgnoreCase("WARN")) {
      return Logger.WARN;
    } else if (s.equalsIgnoreCase("ERROR")) {
      return Logger.ERROR;
    } else if (s.equalsIgnoreCase("SHOUT")) {
      return Logger.SHOUT;
    } else if (s.equalsIgnoreCase("FATAL")) {
      return Logger.FATAL;
    } else {
      return -1;
    }
  }

  // log4j utilities
  //
  // these should be moved to "org.cougaar.util.log"!

  // okay public api:
  private int getLevel(String name) {
    org.apache.log4j.Logger cat = getLogger(name);
    return getLevel(cat);
  }

  // okay public api:
  private void setLevel(String name, int level) {
    org.apache.log4j.Logger cat = getLogger(name);
    setLevel(cat, level);
  }

  // hack:
  static final Level SHOUT = 
    ShoutPriority.toLevel("SHOUT", null);

  // hack:
  static final Level DETAIL = 
    DetailPriority.toLevel("DETAIL", null);

  // log4j private
  private org.apache.log4j.Logger getLogger(String name) {
    return
      (name != null ?
       ((name.equals("root") ||
         name.equals(""))?
        org.apache.log4j.Logger.getRootLogger() :
        org.apache.log4j.Logger.getLogger(name)) :
       null);
  }

  // log4j private
  private int getLevel(org.apache.log4j.Logger cat) {
    if (cat != null) {
      Level p = cat.getEffectiveLevel();
      return convertLevelToInt(p);
    } else {
      return -1;
    }
  }

  // log4j private
  private void setLevel(org.apache.log4j.Logger cat, int level) {
    if (cat != null) {
      Level p = convertIntToLevel(level);
      cat.setLevel(p);
    } else {
      throw new RuntimeException("null category");
    }
  }

  // log4j private
  private Level convertIntToLevel(int level) {
    switch (level) {
    case Logger.DETAIL : return DETAIL;
    case Logger.DEBUG : return Level.DEBUG;
    case Logger.INFO  : return Level.INFO;
    case Logger.WARN  : return Level.WARN;
    case Logger.ERROR : return Level.ERROR;
    case Logger.SHOUT : return SHOUT;
    case Logger.FATAL : return Level.FATAL;
    default: return null;
    }
  }

  // log4j private
  private int convertLevelToInt(Level level) {
    switch (level.toInt()) {
      case Priority.DEBUG_INT:      return Logger.DEBUG;
      case Priority.INFO_INT :      return Logger.INFO;
      case Priority.WARN_INT :      return Logger.WARN;
      case Priority.ERROR_INT:      return Logger.ERROR;
      case Priority.FATAL_INT:      return Logger.FATAL;
      default: 
        if (level.equals(SHOUT)) {
          return Logger.SHOUT;
        } else if (level.equals(DETAIL)) {
	  return Logger.DETAIL;
	}
        return 0;
    }
  }

}
