/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.freeze;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is a {@link javax.servlet.Servlet} that implements
 * the plugin freeze.
 * <p>
 * NOTE: This servlet duplicates much code from the FreezeTargetPlugin.  It 
 * could be refactored to extend that plugin, but since that capability may
 * be obsoleted by this, I'll shoose to copy-paste rather than inherit.
 * <p> 
 * This plugin implements the actual freezing of an agent. Freezing is
 * accomplished by preventing the ThreadService from running certain
 * classes of components. The relevant object is the so-called
 * "consumer" of the ThreadService. For plugins, this is the plugin
 * itself. For other uses of the ThreadService, the "consumer" may be
 * different.
 * <p>
 * Generally, all plugins except those involved in the freeze process
 * are prevented from running, but this can be modified by rules
 * specified as plugin parameters. The rules are applied in this
 * order:
 * <pre>
 * "allow " + FreezePlugin.class.getName()
 * first plugin parameter
 * second plugin parameter
 * etc.
 * "deny " + PluginBase.class.getName()
 * </pre>
 * <p>
 * The form of the rule is one of the words, "deny" or "allow",
 * followed by a space followed by the name of the class or interface
 * that should be affected by the rule. The rule matches if it is
 * legal to assign the consumer to a variable of the type named in the
 * rule. This includes the class of the consumer itself, all
 * interfaces implemented by the consumer or their superinterfaces,
 * all superclasses of the consumer, and all interfaces implemented by
 * any superclass or their superinterfaces.
 * <p>
 * The first rule is built-in and cannot be overridden. It allows all
 * the freeze plugins to run while frozen. This is obviously necessary
 * to handle thawing a frozen society. The last rule is always added
 * and prevents all plugins that extend PluginBase from running except
 * those allowed by preceding rules. While it is possible to write a
 * component that behaves as a plugin but does not extend PluginBase,
 * this does not happen in practice.
 * <p>
 * The effect of this final rule can be nullified by including rules
 * (as plugin parameters) that specifically allow individual plugins.
 * Indeed, the whole class of plugins extending PluginBase could be
 * allowed. It is possible to prevent anything from being frozen in an
 * agent by making the first plugin parameter be "allow
 * java.lang.Object". Since every class extends java.lang.Object, this
 * will allow every class to run.
 */
public class FreezeServlet extends FreezePlugin implements ThreadListener {
  private static class BadGuy {
    private Thread thread;
    private Schedulable schedulable;
    int hc;
    public BadGuy(Schedulable s, Thread t) {
      thread = t;
      schedulable = s;
      hc = System.identityHashCode(t) + System.identityHashCode(s);
    }
    @Override
   public int hashCode() {
      return hc;
    }
    @Override
   public boolean equals(Object o) {
      if (o == this)
        return true;
      if (o instanceof BadGuy) {
        BadGuy that = (BadGuy) o;
        return this.thread == that.thread
          && this.schedulable == that.schedulable;
      }
      return false;
    }
    @Override
   public String toString() {
      return schedulable.getState()
        + ": "
        + schedulable.getConsumer().toString();
    }
  }
  // True if we have frozen this agent.
  private boolean isFrozen = false;
  private boolean isFreezing = false;
  private ThreadListenerService threadListenerService;
  private ThreadControlService threadControlService;
  private ServletService servletService;
  private Rules rules = new Rules();
  private Set badGuys = new HashSet(); // Records the bad guys we have
  // seen enter the run state
  // that have not left the run
  // state

  @Override
public void unload() {
    if (threadControlService != null) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(
        this,
        ThreadListenerService.class,
        threadListenerService);
      sb.releaseService(this, ThreadControlService.class, threadControlService);
    }
    super.unload();
  }

  private UnaryPredicate myThreadQualifier = new UnaryPredicate() {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public boolean execute(Object o) {
      Schedulable schedulable = (Schedulable) o;
      Object consumer = schedulable.getConsumer();
      return rules.allow(consumer);
    }
  };

  // Thread control logic. Threads are classified as good or bad. When
  // frozen, we regulate the max running thread count to be no more
  // than the number of goodguys that are on the runnable queue. The
  // number of goodguys is the total of the known good guys (in the
  // goodGuys set) and the anonymous ones. We have to assume that any
  // thread we have never seen is a good guy. If an anonymous good guy
  // steps off the stage we will recognize him and reduce the
  // anonymousGoodGuys count.
  public synchronized void threadQueued(
    Schedulable schedulable,
    Object consumer) {
  }
  public synchronized void threadDequeued(
    Schedulable schedulable,
    Object consumer) {
  }
  public synchronized void threadStarted(
    Schedulable schedulable,
    Object consumer) {
    if (logger.isDetailEnabled())
      logger.detail("threadStarted: " + consumer);
    if (!rules.allow(consumer)) {
      badGuys.add(new BadGuy(schedulable, Thread.currentThread()));
    }
  }
  public synchronized void threadStopped(
    Schedulable schedulable,
    Object consumer) {
    if (logger.isDetailEnabled())
      logger.detail("threadStopped: " + consumer);
    if (!rules.allow(consumer)) {
      Thread currentThread = Thread.currentThread();
      badGuys.remove(new BadGuy(schedulable, currentThread));
    }
  }
  public void rightGiven(String consumer) {
  }
  public void rightReturned(String consumer) {
  }

  private void setThreadLimit() {
    threadControlService.setQualifier(myThreadQualifier);
  }

  private void unsetThreadLimit() {
    threadControlService.setQualifier(null);
  }

  @Override
public void setupSubscriptions() {
    super.setupSubscriptions();
    rules.addAllowRule(FreezePlugin.class);
    // Hope this is a List cause order is important.
    Collection params = getParameters();
    for (Iterator i = params.iterator(); i.hasNext();) {
      String ruleSpec = (String) i.next();
      try {
        rules.addRule(ruleSpec);
      } catch (Exception e) {
        logger.error("Bad parameter: " + ruleSpec + " : "+ e.getMessage());
      }
    }
    rules.addDenyRule(PluginBase.class);
    if (logger.isInfoEnabled())
      logger.info("rules=" + rules);
    ServiceBroker sb = getServiceBroker();
    threadControlService =
      sb.getService(
        this,
        ThreadControlService.class,
        null);
    threadListenerService =
      sb.getService(
        this,
        ThreadListenerService.class,
        null);
    threadListenerService.addListener(this);
    servletService =
      sb.getService(
        this,
        ServletService.class,
        null);
    try {
      servletService.register("/freezeControl", new MyServlet());
      if (logger.isDebugEnabled())
        logger.debug("Registered /freezeControl servlet");
    } catch (Exception ex) {
      if (logger.isErrorEnabled())
        logger.error("Unable to register freezeControl servlet", ex);
    }
  }

  @Override
public void execute() {
    if (timerExpired()) {
      cancelTimer();
      if (isFreezing)
        checkStopped();
    }
  }

  private class MyServlet extends HttpServlet {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /**
     * @see javax.servlet.http.HttpServlet#service(HttpServletRequest, HttpServletResponse)
     */
    @Override
   protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
      throws ServletException, IOException {
      handleRequest(arg0, arg1);
    }

  }

  private void handleRequest(
    HttpServletRequest req,
    HttpServletResponse resp) {

    String response = "";
    String action = req.getParameter("action");
    if ("freeze".equals(action)) {
      if (!isFrozen) {
        if (logger.isDebugEnabled())
          logger.debug("freezing");
        setThreadLimit();
        isFrozen = true;
        isFreezing = true;
        checkStopped();
      }
      response = "Freezing initiated";
    } else if ("thaw".equals(action)) {
      if (isFrozen) {
        if (logger.isDebugEnabled())
          logger.debug("thawed");
        unsetThreadLimit(); // Unset thread limit
        isFrozen = false;
        isFreezing = false;
      }
      response = "Thawing initiated";
    } else {
      if (isFreezing)
        response = "Agent is Freezing";
      else if (isFrozen)
        response = "Agent is Frozen";
      else
        response = "Agent is Thawed";
    }
    try {
      PrintWriter out = resp.getWriter();
      out.println("<html><head></head><body>");

      out.println(response);
      
      out.println("<table>");
      out.println("<tr><td><FORM METHOD=\"GET\" ACTION=\"freezeControl\">");
      out.println("<INPUT TYPE=\"hidden\" Name=\"action\" Value=\"freeze\">");
      out.println("<INPUT TYPE=\"submit\" Value=\"Freeze\">");
      out.println("</FORM>");
      out.println("</td>");
      out.println("<td><FORM METHOD=\"GET\" ACTION=\"freezeControl\">");
      out.println("<INPUT TYPE=\"hidden\" Name=\"action\" Value=\"thaw\">");
      out.println("<INPUT TYPE=\"submit\" Value=\"Thaw\">");
      out.println("</FORM>");
      out.println("</td></tr>");
      out.println("</table>");
      out.println("<p><FORM METHOD=\"GET\" ACTION=\"freezeControl\">");
      out.println("<INPUT TYPE=\"submit\" Value=\"Refresh\">");
      out.println("</FORM>");
      out.println("</body>");
    } catch (IOException ioe) {
      if (logger.isErrorEnabled())
        logger.error("Error generating response", ioe);
    }
  }

  private synchronized void checkStopped() {
    int stillRunning = badGuys.size();
    if (stillRunning <= 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Frozen");
      }
      isFreezing = false;
    } else {
      if (logger.isDebugEnabled()) {
        Set consumerSet = new HashSet();
        for (Iterator i = badGuys.iterator(); i.hasNext();) {
          BadGuy bg = (BadGuy) i.next();
          consumerSet.add(bg.toString());
        }
        logger.debug("Still running: " + consumerSet);
      }
      resetTimer(5000);
    }
  }
}
