/*
 * <copyright>
 *
 *  Copyright 2000-2006 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ServletService;
import org.cougaar.util.UnaryPredicate;

/**
 * @deprecated use ServletPlugin.
 * @see org.cougaar.core.plugin.ServletPlugin
 */
public abstract class BlackboardServlet extends ComponentPlugin {

  private String myPath;
  private ServletService servletService;
  private String encAgentName;

  public BlackboardServlet() {
    super();
  }

  /**
   * Optional subscriptions.
   */
  @Override
protected void setupSubscriptions() {
  }
  @Override
protected void execute() {
  }

  protected <T> Collection<T> query(UnaryPredicate<T> pred) {
    if (blackboard.isTransactionOpen()) {
      return blackboard.query(pred);
    } else {
      blackboard.openTransaction();
      Collection<T> c = blackboard.query(pred);
      blackboard.closeTransactionDontReset();
      return c;
    }
  }

  protected void publishAdd(Object o) {
    if (blackboard.isTransactionOpen()) {
      blackboard.publishAdd(o);
    } else {
      blackboard.openTransaction();
      blackboard.publishAdd(o);
      blackboard.closeTransactionDontReset();
    }
  }

  protected void publishChange(Object o) {
    publishChange(o, null);
  }
  protected void publishChange(Object o, Collection changes) {
    if (blackboard.isTransactionOpen()) {
      blackboard.publishChange(o, changes);
    } else {
      blackboard.openTransaction();
      blackboard.publishChange(o, changes);
      blackboard.closeTransactionDontReset();
    }
  }

  protected void publishRemove(Object o) {
    if (blackboard.isTransactionOpen()) {
      blackboard.publishRemove(o);
    } else {
      blackboard.openTransaction();
      blackboard.publishRemove(o);
      blackboard.closeTransactionDontReset();
    }
  }

  /**
   * Create our servlet, which by default calls our doGet/doPost/doPost
   * methods.
   * <p>
   * A subclass can override this method if it's designed to create a separate
   * servlet anyways.
   */
  protected HttpServlet createServlet() {
    return new HttpServlet() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;
      @Override
      protected void doGet(
          HttpServletRequest req, HttpServletResponse resp
          ) throws ServletException, IOException {
        BlackboardServlet.this.doGet(req, resp);
      }
      @Override
      protected void doPost(
          HttpServletRequest req, HttpServletResponse resp
          ) throws ServletException, IOException {
        BlackboardServlet.this.doPost(req, resp);
      }
      @Override
      protected void doPut(
          HttpServletRequest req, HttpServletResponse resp
          ) throws ServletException, IOException {
        BlackboardServlet.this.doPut(req, resp);
      }
      // note: we're missing some methods and "getOptions()" will be wrong
      // This should be fine for nearly all servlets.
    };
  }

  /** Basic servlet methods */
  protected void doGet(
      HttpServletRequest req, HttpServletResponse resp
      ) throws IOException {
    notSupported(req, resp, "GET");
  }
  protected void doPost(
      HttpServletRequest req, HttpServletResponse resp
      ) throws IOException {
    notSupported(req, resp, "POST");
  }
  protected void doPut(
      HttpServletRequest req, HttpServletResponse resp
      ) throws IOException {
    notSupported(req, resp, "PUT");
  }

  private void notSupported(
      HttpServletRequest req, HttpServletResponse resp,
      String type) throws IOException {
    String protocol = req.getProtocol();
    String msg = "HTTP method "+type+" is not supported by this URL";
    if (protocol.endsWith("1.1")) {
      resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
    }
  }

  @Override
public void load() {
    super.load();

    // set path
    String path = getPath();
    if (path == null) {
      Collection params = getParameters();
      if (!params.isEmpty()) {
        path = (String) params.iterator().next();
      }
    }
    if (path == null) {
      throw new IllegalArgumentException("Missing path parameter");
    }
    myPath = path;

    // get encoded agent name
    String agentName = (agentId == null ? null : agentId.getAddress());
    encAgentName = (agentName == null ? null : encode(agentName));

    // get our servlet service
    servletService = getServiceBroker().getService(this, ServletService.class, null);
    if (servletService == null) {
      throw new RuntimeException("Unable to obtain ServletService");
    }

    // register our servlet
    try {
      HttpServlet servlet = createServlet();
      servletService.register(path, servlet);
    } catch (Exception e) {
      throw new RuntimeException("Unable to register "+path, e);
    }
  }

  @Override
public void unload() {
    if (servletService != null) {
      // this will automatically unregister our servlet
      getServiceBroker().releaseService(
          this, ServletService.class, servletService);
      servletService = null;
    }

    super.unload();
  }

  /**
   * Get the path for the Servlet's registration.
   * <p>
   * Typically supplied by the component parameter, but subclasses can
   * hard-code the path by overriding this method.
   */
  protected String getPath() {
    return myPath;
  }

  /** URL-encoded name of the local agent */
  protected String getEncodedAgentName() {
    return encAgentName;
  }

  protected String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }
  protected String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to decode to UTF-8?");
    }
  }
}
