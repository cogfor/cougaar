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
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.servlet.SimpleServletSupport;

/**
 * <pre>
 * Base class for all servlets in the datagrabber.
 *
 * Holds the simple servlet support object.
 * Automatically treats POST requests as GETs.
 * Defines the usage function as something to be called when there
 * are no url parameters.
 *
 * Most of the work of a servlet is done by the ServletWorker.
 * A new ServletWorker is created for every new request.
 * </pre>
 */
public abstract class ServletBase
  extends HttpServlet {
  public static final boolean DEBUG = false;

  public static boolean VERBOSE = false;

  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.ServletBase.verbose");
  }

  /**
   * Save our service broker during initialization.
   */
  protected SimpleServletSupport support;
  public SimpleServletSupport getSupport () { return support; }

  /** **/
  public void setSimpleServletSupport(SimpleServletSupport support) {
    this.support = support;
  }

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {    return getClass().getName();  }

  public void doGet(HttpServletRequest request,
		    HttpServletResponse response) throws IOException, ServletException {
    ServletWorker worker = createWorker ();
    if (!request.getParameterNames().hasMoreElements ()) {
      getUsage (response.getWriter(), support);
      return;
    }
    if (VERBOSE) {
      Enumeration paramNames = request.getParameterNames();
      for (int i = 0; paramNames.hasMoreElements (); )
	System.out.println ("ServletBase got param #" + i++ + " - " + paramNames.nextElement ());
    }
    worker.execute (request, response, support);
  }

  public void doPost(HttpServletRequest request,
		     HttpServletResponse response) throws IOException, ServletException {
    doGet (request, response);
  }

  protected abstract ServletWorker createWorker ();

  /** 
   * USAGE <p>
   *
   * Only called if no arguments are given.
   */
  public abstract void getUsage (PrintWriter out, SimpleServletSupport support);
}

