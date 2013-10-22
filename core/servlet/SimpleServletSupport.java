/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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

import java.util.Collection;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * This is a simple API for a <code>Servlet</code> to access
 * COUGAAR Services, such as the Blackboard.
 * <p>
 * This is for Servlets that are loaded by the
 * <code>SimpleServletComponent</code>.  See SimplerServletComponent
 * for loading details (".ini" usage, etc).
 * <p>
 * The Servlet class (e.g. "MyServlet") must have either a
 * "support" constructor:<pre>
 *    ... 
 *    public MyServlet(SimpleServletSupport support) {
 *      // save support for later use
 *    }
 *    ...</pre><br>
 * or the default constructor:<pre>
 *    ...
 *    public MyServlet() {
 *      // load with no support
 *    }
 *    ...</pre><br>
 * The default constructor can be used to load pure Servlets (i.e. 
 * Servlets without Cougaar references).
 * <p>
 *
 * @see SimpleServletComponent
 */
public interface SimpleServletSupport {

  /**
   * Get the path that this Servlet was loaded under.
   * <p>
   * This can also be obtained from the 
   * <tt>HttpServletRequest.getRequestURI()</tt>, which
   * will be "/$encoded-agent-name/path".
   */
  String getPath();

  /**
   * Query the blackboard for all Objects that match the
   * given predicate.
   * <p>
   * Each call to "query" is a snapshot of the blackboard
   * that may contain different information than the last
   * "query" call, even within the same Servlet 
   * "service(..)" request.  The Objects returned should
   * be considered read-only!
   * <p>
   * This is the only blackboard access that is provided
   * to <i>simple</i> Servlets.  Servlets that need
   * to publish/subscribe/etc will require more complex
   * transaction open/close logic -- they should 
   * obtain the <code>ServletService</code> directly.
   * See <code>SimpleServletComponent</code> as a guide.
   */
  Collection queryBlackboard(UnaryPredicate pred);

  /**
   * Get the URL- and HTML-safe (encoded) name of the
   * Agent that contains this Servlet instance.
   * <p>
   * Equivalent to 
   * <tt>encodeAgentName(getAgentIdentifier().getAddress())</tt>.
   * <p>
   * All "/$name/*" URLS must use the encoded Agent name.
   * In general the raw "agentName" may contain characters
   * that are not URL/HTML safe, such as:
   * <ul>
   *   <li>URL reserved characters  (" ", ":", "/", etc)</li>
   *   <li>HTML reserved characters ("&gt;", "&lt;")</li>
   *   <li>Arbitrary control characters ("\\n", "\\t", etc)</li>
   * </ul>
   *
   * @see #encodeAgentName
   */
  String getEncodedAgentName();

  /**
   * Get the Agent's identifier for the Agent that contains
   * this Servlet instance.
   * <p>
   * The <tt>getAgentIdentifier().getAddress()</tt> is the 
   * "raw" name of the agent and may contain unsafe URL/HTML
   * characters.
   *
   * @see #getEncodedAgentName
   */
  MessageAddress getAgentIdentifier();

  /**
   * Obtain access to the logging service.
   * <p>
   * This is guaranteed to be non-null.
   */
  LoggingService getLog();

  /**
   * Utility method to encode an Agent name -- 
   * equivalent to <tt>java.net.URLEncoder.encode(name)</tt>.
   *
   * @see #getEncodedAgentName
   */
  String encodeAgentName(String name);

  //
  // add other COUGAAR-specific methods here.
  //
  // note that we want this to remain a *simple* API.
  //
}
