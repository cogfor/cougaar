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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.ServletUtil;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.planning.servlet.data.xml.XMLWriter;
import org.cougaar.planning.servlet.data.xml.XMLable;

/**
 * <pre>
 * ServletWorker is the base class for all servlet workers.
 *
 * ServletWorkers assume that the result can be returned in any of
 * three formats : html, xml, or serialized java object.  These
 * formats are specified by the URL format parameter, i.e.
 *   http://localhost:8800/$AGENT_NAME/SERVLET_NAME?format=html
 *
 * The format parameter is set in the getSettings method.  This should
 * be called from a subclass if overridden.
 *
 * Defines the writeResponse method, which returns the type of result
 * indicated by the format parameter to the output stream of the servlet 
 * response.
 *
 * Note that if you make a mistake and don't provide a value for a 
 * parameter, e.g.
 *   http://localhost:8800/$AGENT_NAME/SERVLET_NAME?format
 *                                                        ^^^^ - no value
 * the ServletUtil object will throw an exception.
 *
 * </pre>
 */
public class ServletWorker {
  public static final boolean DEBUG = false;

  public static boolean VERBOSE = false;
  public static final int FORMAT_DATA = 0;
  public static final int FORMAT_XML  = 1;
  public static final int FORMAT_HTML = 2;

  static {
      VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.ServletWorker.verbose");
  }

  protected int format;

  /**
   * Main method. <p>
   * Most of the work is done in getHierarchy.
   * This method mainly checks that the parameters are the right 
   * number and sets the format and recurse fields.
   * <p>
   * Uses the ServletUtil to parse the parameters.
   * @see org.cougaar.core.servlet.ServletUtil#parseParams
   */
  public void execute(HttpServletRequest request, 
		      HttpServletResponse response,
		      SimpleServletSupport support) throws IOException, ServletException {
    ServletUtil.ParamVisitor vis = 
      new ServletUtil.ParamVisitor() {
	  public void setParam(String name, String value) {
	    getSettings (name, value);
	  }
	};

    // visit the URL parameters
    ServletUtil.parseParams(vis, request);

    // generate our response.
  }

  /** 
   * <pre>
   * sets format 
   *
   * format  is either data, xml, or html
   *
   * see class description for what these values mean
   * </pre>
   */
  protected void getSettings (String name, String value) {
    if (eq("format", name)) {
      if (eq("data", value)) {
	format = FORMAT_DATA;
      } else if (eq("xml", value)) {
	format = FORMAT_XML;
      } else if (eq("html", value)) {
	format = FORMAT_HTML;
      }
      // stay backwards-compatable
    } else if (eq("data", name)) {
      format = FORMAT_DATA;
    } else if (eq("xml", name)) {
      format = FORMAT_XML;
    } else if (eq("html", name)) {
      format = FORMAT_HTML;
    }
  }

  protected boolean isHtmlFormat () { return (format == FORMAT_HTML); }
  protected boolean isXmlFormat  () { return (format == FORMAT_XML); }
  protected boolean isDataFormat () { return (format == FORMAT_DATA); }

  protected String getPrefix () { return ""; }

  /**
   * Write XMLable result to output. <p>
   *
   * Output format is either data, xml, or html <p>
   *
   * For an example of how this is used, see the following references.
   * @see org.cougaar.planning.servlet.HierarchyWorker#getHierarchy
   * @see org.cougaar.planning.servlet.HierarchyWorker#writeResponse
   */
  protected void writeResponse(XMLable result, 
			       OutputStream out, HttpServletRequest request, 
			       SimpleServletSupport support,
			       int format) {
    // write data
    try {
      if (format == FORMAT_DATA) {
	// serialize
	ObjectOutputStream oos = new ObjectOutputStream(out);
	oos.writeObject(result);
	oos.flush();
      } else {
	// xml or html-wrapped xml
	XMLWriter w;
	PrintWriter writer = new PrintWriter (out);

	if (format == FORMAT_HTML) {
	  // wrapped xml
	  writer.println(
			 "<HTML><HEAD><TITLE> "+getPrefix () + 
			 support.getAgentIdentifier()+
			 "</TITLE></HEAD><BODY>\n"+
			 "<H2><CENTER>" + getPrefix () + 
			 support.getAgentIdentifier()+
			 "</CENTER></H2><p><pre>\n");
	  writer.flush ();
	  w = 
	    new XMLWriter(
			  new OutputStreamWriter(
						 new XMLtoHTMLOutputStream(out)),
			  true);
	} else {
	  // raw xml
	  writer.println("<?xml version='1.0' ?>");
	  writer.flush ();
	  w = 
	    new XMLWriter(
			  new OutputStreamWriter(out));
	}
	// write as xml
	result.toXML(w);
	w.flush();
	if (format == FORMAT_HTML) {
	  writer.println("\n</pre></BODY></HTML>\n");
	}
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Convert XML to HTML-friendly output.
   *
   * Taken from PSP_PlanView.  For Internet Explorer this isn't such
   * a big deal...
   */
  protected static class XMLtoHTMLOutputStream 
      extends FilterOutputStream {
    protected static final byte[] LESS_THAN;
    protected static final byte[] GREATER_THAN;
    static {
      LESS_THAN = "<font color=green>&lt;".getBytes();
      GREATER_THAN = "&gt;</font>".getBytes();
    }
    public XMLtoHTMLOutputStream(OutputStream o) {
      super(o);
    }
    public void write(int b) throws IOException {
      if (b == '<') {
        out.write(LESS_THAN);
      } else if (b == '>') {
        out.write(GREATER_THAN);
      } else {
        out.write(b);
      }
    }
  }

  public static final boolean eq(String a, String b) {
    return a.regionMatches(true, 0, b, 0, a.length());
  }
}

