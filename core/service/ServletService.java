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

package org.cougaar.core.service;

import javax.servlet.Servlet;

import org.cougaar.core.component.Service;

/**
 * This service can be used to register a <code>Servlet</code>
 * for all HTTP requests that match a specified String path.
 * <p>
 * This is analogous to a <code>java.util.Map</code>:<pre>
 *   - register with "put(name, value)"
 *   - unregister with "remove(name)"
 *   - unregister-all with "clear()"
 * </pre>
 * <p>
 * "unregisterAll" is called automatically when this service
 * is released back to the ServiceBroker. 
 * <p>
 * "Servlet.init(..)" will be called upon <tt>register</tt>
 * and "Servlet.destroy()" will be called upon <tt>unregister</tt>.
 * <p>
 * Also note that multiple concurrent "Servlet.service(..)" calls 
 * can occur, so your Servlet must be reentrant.
 * <p>
 * See:
 *  <a href=
 *  "http://java.sun.com/docs/books/tutorial/servlets">
 *   http://java.sun.com/docs/books/tutorial/servlets</a> for
 * tutorials on how to write Servlets and HttpServlets.
 */
public interface ServletService extends Service {

  /** 
   * Register a path to call the given <code>Servlet</code>.
   * <p>
   * This method will throw an <code>IllegalArgumentException</code>
   * if the path has already been registered by another ServletService
   * instance..
   * 
   * @see #unregister(String)
   */
  void register(
      String path,
      Servlet servlet) throws Exception;

  /** 
   * Unregister the <code>Servlet</code> with the specified path.
   * <p>
   * This method can only be used to unregister Servlets that have 
   * been registered with <b>this</b> service instance.
   *
   * @see #register(String,Servlet)
   * @see #unregisterAll()
   */
  void unregister(
      String path);

  /**
   * Unregister all <code>Servlet</code>s that have been registered
   * by <b>this</b> registration service instance.
   * <p>
   * This can be used as a one-stop cleanup utility.
   *
   * @see #unregister(String)
   */
  void unregisterAll();

  /**
   * Get the HTTP port for the local servlet server.
   * <p>
   * A typical HTTP port is 8800, but a different port may be
   * used due to either node configuration or to support multiple
   * nodes on the same machine.
   *
   * @return the HTTP port, or -1 if HTTP is disabled.
   */
  int getHttpPort();

  /**
   * Get the HTTPS port for the local servlet server.
   * <p>
   * A typical HTTPS port is 8400, but a different port may be
   * used due to either node configuration or to support multiple
   * nodes on the same machine.
   *
   * @return the HTTPS port, or -1 if HTTPS is disabled.
   */
  int getHttpsPort();

}
