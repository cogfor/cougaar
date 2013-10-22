/*
 * <copyright>
 *
 *  Copyright 2000-2007 BBNT Solutions, LLC
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

package org.cougaar.core.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.TodoSubscription;
import org.cougaar.core.service.ServletService;
import org.cougaar.util.FutureResult;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.annotations.Cougaar;

/**
 * This plugin is a base class for servlets that either modify or subscribe to
 * the blackboard.
 * <p>
 * If a servlet only requires blackboard queries and no subscriptions or
 * add/change/remove modifications, then {@link ComponentServlet} is
 * recommended.
 * <p>
 * If {@link #isTransactional} is true then all servlet processing work is done
 * single-threaded in the "execute()" method, where it can access subscriptions
 * and modify the blackboard using the standard "blackboard.publish*" methods.
 * 
 * @param org.cougaar.core.servlet.ServletPlugin.timeout=60000 Default timeout
 *        for ServletPlugin requests, which are processed in the plugin's
 *        "execute()" thread.
 */
public abstract class ServletPlugin
      extends AnnotatedSubscriptionsPlugin {

   @Cougaar.Arg()
   public String path;

   @Cougaar.Arg(defaultValue = "60000")
   public long timeout;

   @Cougaar.ObtainService()
   public ServletService servletService;

   private boolean isTrans;
   private TodoSubscription todo;
   private String encAgentName;

   public ServletPlugin() {
      super();
   }

   /**
    * Return true (the default) if all servlet requests should run
    * single-threaded in the "execute()" method, otherwise false if they should
    * run in the servlet engine's threads (and possibly in parallel with other
    * servlet requests and the "execute()" thread).
    * <p>
    * If a servlet requires a cross-transaction result, e.g.:
    * <ol>
    * <li>publishAdd object X</li>
    * <li>wait another plugin to react to object X, e.g. via a "notify()"
    * callback or published response</li>
    * <li>finish the servlet call</li>
    * </ol>
    * then "isTransactional()" must return false.
    * <p>
    * Also, if a servlet has no subscriptions or private state, then it's
    * slightly more efficient to have this method return false. This will allow
    * requests to run in parallel.
    * <p>
    * In all other cases, this method should return true. This allows the
    * "doGet(...)" method to access subscriptions and other internal state
    * without a synchronization lock, since the "execute()" method is always
    * single-threaded and runs in a blackboard transaction.
    */
   protected boolean isTransactional() {
      return true;
   }

   /**
    * Get the path for the Servlet's registration.
    * <p>
    * Typically the path is supplied by a "path=" plugin argument, but a
    * subclass can hard-code the path by overriding this method.
    */
   protected String getPath() {
      return path;
   }

   @Override
   public void load() {
      super.load();

      // set threading
      isTrans = isTransactional();

      // get encoded agent name
      String agentName = (agentId == null ? null : agentId.getAddress());
      encAgentName = (agentName == null ? null : encode(agentName));

      // register our servlet
      try {
         HttpServlet servlet = createServlet();
         servletService.register(path, servlet);
      } catch (Exception e) {
         throw new RuntimeException("Unable to register " + path, e);
      }
   }

   /** Get the URL-encoded name of the local agent */
   protected String getEncodedAgentName() {
      return encAgentName;
   }

   @Override
   protected void setupSubscriptions() {
      super.setupSubscriptions();
      if (isTrans) {
         todo = blackboard.subscribe(new TodoSubscription("x"));
      }
   }

   @Override
   protected void execute() {
      super.execute();
      if (!isTrans) {
         return;
      }
      ensureTodo();
      if (!todo.hasChanged()) {
         return;
      }
      Collection<HttpJob> addedCollection = todo.getAddedCollection();
      for (HttpJob job : addedCollection) {
         try {
            service(job.getHttpServletRequest(), job.getHttpServletResponse());
            job.notifySuccess();
         } catch (Exception e) {
            job.notifyFailure(e);
         }
      }
   }

   private void ensureTodo() {
      if (todo == null) {
         throw new RuntimeException("The \"todo\" subscription is null.  Is \"setupSubscriptions()\""
               + " missing a call to \"super.setupSubscriptions()\"?");
      }
   }

   protected Subscription subscribe(UnaryPredicate pred) {
      if (!isTrans || !blackboard.isTransactionOpen()) {
         throw new IllegalStateException("Can only subscribe if \"isTransactional()\" is true");
      }
      return blackboard.subscribe(pred);
   }

   protected Collection query(UnaryPredicate pred) {
      if (isTrans || blackboard.isTransactionOpen()) {
         return blackboard.query(pred);
      } else {
         blackboard.openTransaction();
         Collection c = blackboard.query(pred);
         blackboard.closeTransactionDontReset();
         return c;
      }
   }

   protected void publishAdd(Object o) {
      if (isTrans || blackboard.isTransactionOpen()) {
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
      if (isTrans || blackboard.isTransactionOpen()) {
         blackboard.publishChange(o, changes);
      } else {
         blackboard.openTransaction();
         blackboard.publishChange(o, changes);
         blackboard.closeTransactionDontReset();
      }
   }

   protected void publishRemove(Object o) {
      if (isTrans || blackboard.isTransactionOpen()) {
         blackboard.publishRemove(o);
      } else {
         blackboard.openTransaction();
         blackboard.publishRemove(o);
         blackboard.closeTransactionDontReset();
      }
   }

   protected void serviceLater(HttpServletRequest req, HttpServletResponse resp)
         throws ServletException, IOException {
      if (!isTrans) {
         throw new IllegalStateException("Can only call \"serviceLater(...)\" if \"isTransactional()\" is" + " true.");
      }
      ensureTodo();

      // put on our "todo" queue
      HttpJob job = new HttpJob(req, resp);
      todo.add(job);

      // wait for the result, which will rethrow any "notifyFailure" exception
      job.waitForNotify(timeout);
   }

   /**
    * Create our servlet, which by default calls our doGet/doPost/doPost
    * methods.
    * <p>
    * A subclass can override this method if it's designed to create a separate
    * servlet. However, if {@link #isTransactional} is true then it should use
    * {@link #serviceLater} to do all blackboard work in the "execute()" method
    * instead of the servlet callback thread.
    * <p>
    * Even though we switch to the "execute()" thread, we must still block the
    * servlet callback until we finish the work, otherwise the servlet engine
    * will close our response stream.
    */
   protected HttpServlet createServlet() {
      return new HttpServlet() {
         /**
       * 
       */
         private static final long serialVersionUID = 1L;

         @Override
         protected void service(HttpServletRequest req, HttpServletResponse resp)
               throws ServletException, IOException {
            if (isTrans) {
               serviceLater(req, resp);
            } else {
               ServletPlugin.this.service(req, resp);
            }
         }
      };
   }

   /** Basic servlet methods */
   protected void service(HttpServletRequest req, HttpServletResponse resp)
         throws IOException {
      String method = req.getMethod();
      if ("GET".equals(method)) {
         doGet(req, resp);
      } else if ("POST".equals(method)) {
         doPost(req, resp);
      } else if ("PUT".equals(method)) {
         doPut(req, resp);
      } else if ("OPTIONS".equals(method)) {
         // RFE do same thing as in HttpServlet
         resp.setHeader("Allow", "GET, HEAD, POST, PUT, OPTIONS");
      } else {
         notSupported(req, resp);
      }
   }

   protected void doGet(HttpServletRequest req, HttpServletResponse resp)
         throws IOException {
      notSupported(req, resp);
   }

   protected void doPost(HttpServletRequest req, HttpServletResponse resp)
         throws IOException {
      notSupported(req, resp);
   }

   protected void doPut(HttpServletRequest req, HttpServletResponse resp)
         throws IOException {
      notSupported(req, resp);
   }

   protected void notSupported(HttpServletRequest req, HttpServletResponse resp)
         throws IOException {
      String method = req.getMethod();
      String msg = "HTTP method " + method + " is not supported by this URL";
      String protocol = req.getProtocol();
      int sc =
            (protocol != null && protocol.endsWith("1.1") ? HttpServletResponse.SC_METHOD_NOT_ALLOWED
                  : HttpServletResponse.SC_BAD_REQUEST);
      resp.sendError(sc, msg);
   }

   /** @return the UTF-8 encoded string */
   protected String encode(String s) {
      try {
         return (s == null ? null : URLEncoder.encode(s, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         // should never happen
         throw new RuntimeException("Unable to encode to UTF-8?");
      }
   }

   /** @return the UTF-8 decoded string */
   protected String decode(String s) {
      try {
         return (s == null ? null : URLDecoder.decode(s, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         // should never happen
         throw new RuntimeException("Unable to decode to UTF-8?");
      }
   }

   protected static final class HttpJob {

      private final HttpServletRequest req;
      private final HttpServletResponse resp;
      private final FutureResult future = new FutureResult();

      public HttpJob(HttpServletRequest req, HttpServletResponse resp) {
         this.req = req;
         this.resp = resp;
      }

      public HttpServletRequest getHttpServletRequest() {
         return req;
      }

      public HttpServletResponse getHttpServletResponse() {
         return resp;
      }

      public void notifySuccess() {
         future.set(Boolean.TRUE);
      }

      public void notifyFailure(Throwable t) {
         future.setException(t);
      }

      public void waitForNotify(long timeout)
            throws ServletException, IOException {
         // wait for the result
         Throwable t;
         try {
            Object o = future.timedGet(timeout);
            if (o == Boolean.TRUE) {
               // success
               return;
            }
            t = new InternalError("Unexpected submit result: " + o);
         } catch (InvocationTargetException ite) {
            t = ite.getCause();
         } catch (InterruptedException ie) {
            t = ie;
         }

         // rethrow the exception
         if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
         } else if (t instanceof ServletException) {
            throw (ServletException) t;
         } else if (t instanceof IOException) {
            throw (IOException) t;
         } else if (t instanceof InterruptedException) {
            throw new RuntimeException("Request timeout");
         } else if (t instanceof Error) {
            throw (Error) t;
         } else {
            throw new RuntimeException("Wrapped exception", t);
         }
      }
   }
}
