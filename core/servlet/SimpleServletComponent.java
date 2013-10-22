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

import java.lang.reflect.Method;
import java.util.List;

import javax.servlet.Servlet;

import org.cougaar.core.component.Component;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModel;

/**
 * Component that loads a <code>Servlet</code> and provides
 * the <code>SimpleServletSupport</code>.
 * <p>
 * Usage in an ".ini" file is:<pre>
 *   ...
 *   plugin = &lt;this-class&gt;(&lt;servlet-class&gt;, &lt;path&gt;)
 *   ...</pre><br>
 * where<pre>
 *   &lt;this-class&gt;
 *      is "org.cougaar.core.servlet.SimpleServletComponent"
 *   &lt;servlet-class&gt;
 *      is the class name of a Servlet.
 *      The servlet must have a zero-argument constructor.
 *      If the Servlet has a
 *        public void setSimpleServletSupport(SimpleServletSupport support)
 *      method then a SimpleServletSupport is passed, which provides
 *      <i>limited</i> access to Cougaar internals.
 *   &lt;path&gt;
 *      is the path for the Servlet, such as "/test".
 * </pre><br>
 * <p>
 *
 * Most of this code is "reflection-glue" to:<ul>
 *   <li>capture the (classname, path) parameters</li>
 *   <li>construct the class instance</li>
 *   <li>examine the class's method(s)</li>
 *   <li>setup and create a <code>SimpleServletSupportImpl</code>
 *       instance</li>
 * </ul>
 * Most subclass developers have the classname and path hard-coded,
 * so they should consider not extending SimpleServletComponent and
 * instead use <code>BaseServletComponent</code>.  The additional
 * benefit is that subclasses of BaseServletComponent have full
 * access to the ServiceBroker.
 *
 * @see SimpleServletSupport
 */
public class SimpleServletComponent
extends BaseServletComponent
{

  /**
   * Servlet classname from "setParameter(..)".
   */
  protected String classname;

  /**
   * Servlet path from "setParameter(..)".
   */
  protected String path;

  /**
   * Agent identifier for the Agent that loaded this Component.
   */
  protected MessageAddress agentId;

  // servlet that we'll load
  protected Servlet servlet;

  // backwards compatibility!
  protected Component comp;

  //
  // Services for our SimpleServletSupport use
  //
  protected BlackboardQueryService blackboardQuery;
  protected LoggingService log;

  public final void setAgentIdentificationService(AgentIdentificationService ais) {
    if ((ais != null)) {
      this.agentId = ais.getMessageAddress();
    }
  }

  /**
   * Save our Servlet's configurable path, for example
   * "/test".
   * <p>
   * This is only set during initialization and is constant
   * for the lifetime of the Servlet.
   */
  @Override
public void setParameter(Object o) {
    // expecting a List of [String, String]
    if (!(o instanceof List)) {
      throw new IllegalArgumentException(
        "Expecting a List parameter, not : "+
        ((o != null) ? o.getClass().getName() : "null"));
    }
    List l = (List)o;
    if (l.size() < 2) {
      throw new IllegalArgumentException(
          "Expecting a List with at least two elements,"+
          " \"classname\" and \"path\", not "+l.size());
    }
    Object o1 = l.get(0);
    Object o2 = l.get(1);
    if ((!(o1 instanceof String)) ||
        (!(o2 instanceof String))) {
      throw new IllegalArgumentException(
          "Expecting two Strings, not ("+o1+", "+o2+")");
    }

    // save the servlet classname and path
    this.classname = (String) o1;
    this.path = (String) o2;
  }

  @Override
protected String getPath() {
    return path;
  }

  @Override
protected Servlet createServlet() {
    // load the servlet class
    Class cl;
    try {
      cl = Class.forName(classname);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to load Servlet class: "+classname);
    }
    if (Servlet.class.isAssignableFrom(cl)) {
      // good
    } else if (Component.class.isAssignableFrom(cl)) {
      // deprecated!
    } else {
      throw new IllegalArgumentException(
          "Class \""+classname+"\" does not implement \""+
          Servlet.class.getName()+"\"");
    }

    // create a zero-arg instance
    Object inst;
    try {
      inst = cl.newInstance();
    } catch (Exception e) {
      // throw the general "no-constructor" exception
      throw new RuntimeException(
          "Unable to create Servlet instance: ", e);
    }

    if (inst instanceof Component) {
      this.comp = (Component) inst;
      try {
        Method m = cl.getMethod(
            "setParameter",
            new Class[]{Object.class});
        m.invoke(comp, new Object[]{path});
      } catch (NoSuchMethodException nsme) {
        // ignore, support a couple broken clients
	// (cant log cause log service comes later)
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to setParameter("+path+")", e);
      }
      activate(inst);
      return null;
    }

    this.servlet = (Servlet) inst;

    // check for the support requirement
    Method supportMethod;
    try {
      supportMethod = cl.getMethod(
          "setSimpleServletSupport",
          new Class[]{SimpleServletSupport.class});
    } catch (NoSuchMethodException e) {
      // simple non-cougaar-aware servlet
      return servlet;
    }

    // create the support
    SimpleServletSupport support;
    try {
      support = createSimpleServletSupport(servlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to create Servlet support", e);
    }

    // set the support
    try {
      supportMethod.invoke(servlet, new Object[]{support});
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to set Servlet support", e);
    }

    return servlet;
  }

  /**
   * Obtain services for the servlet, using the servlet as
   * the requestor.
   */
  protected SimpleServletSupport createSimpleServletSupport(
      Servlet servlet) {
    // the agentId is known from "setAgentIdentificationService(..)"

    // get the blackboard query service
    blackboardQuery = getService(
       servlet,
       BlackboardQueryService.class,
       null);
    if (blackboardQuery == null) {
      throw new RuntimeException(
          "Unable to obtain blackboard query service");
    }

    // get the logging service (for "getLogger")
    log = getService(
       servlet,
       LoggingService.class,
       null);
    if (log == null) {
      // continue loading -- let the "support" use a null-logger.
    }

    // create a new "SimpleServletSupport" instance
    return
      new SimpleServletSupportImpl(
        path, agentId, blackboardQuery, log);
  }

  @Override
public void unload() {
    // release all services
    super.unload();

    if (servlet != null) {
      if (log != null) {
        releaseService(
            servlet, LoggingService.class, log);
        log = null;
      }
      if (blackboardQuery != null) {
        releaseService(
            servlet, BlackboardQueryService.class, blackboardQuery);
        blackboardQuery = null;
      }
      servlet = null;
    }
    if (comp != null) {
      switch (comp.getModelState()) {
        case GenericStateModel.ACTIVE:
          comp.suspend();
          // fall-through
        case GenericStateModel.IDLE:
          comp.stop();
          // fall-through
        case GenericStateModel.LOADED:
          comp.unload();
          // fall-through
        case GenericStateModel.UNLOADED:
          // unloaded
          break;
        default:
          // never?
      }
      comp = null;
    }
  }

  @Override
public String toString() {
    return classname+"("+path+")";
  }
}
