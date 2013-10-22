/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.Arguments;

/**
 * This component is a base class for {@link ComponentInitializerService}
 * override implementations.
 * 
 * @see #overrideComponentDescriptions
 */
public abstract class ConfiguratorBase extends ComponentSupport {

  protected Arguments args = Arguments.EMPTY_INSTANCE;

  protected MessageAddress localNode;
  private ComponentInitializerService root_cis;
  private ServiceBroker rootsb;

  protected LoggingService log;

  private ServiceProvider sp;

  public void setParameter(Object o) {
    args = new Arguments(o);
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
    }
  }

  public void setComponentInitializerService(ComponentInitializerService cis) {
    root_cis = cis;
  }

  @Override
public void load() {
    super.load();

    localNode = find_local_node();

    // override the top-level comp-init service
    //
    // note that we can add this service at the root, even though we were able
    // to obtained it.  This isn't a conflict because the initial service is
    // advertised *above* the root, in the Node, otherwise our call to 
    // "addService" would fail.
    sp = new MySP();
    rootsb.addService(ComponentInitializerService.class, sp);
  }

  @Override
public void unload() {
    if (sp != null) {
      rootsb.revokeService(ComponentInitializerService.class, sp);
      sp = null;
    }
    super.unload();
  }

  protected ComponentDescription[] getComponentDescriptions(
      String agentName, 
      String insertionPoint
      ) throws ComponentInitializerService.InitializerException {
    ComponentDescription[] orig = null;
    ComponentDescription[] ret = null;
    Exception e = null;
    try {
      orig = root_cis.getComponentDescriptions(agentName, insertionPoint);
      ret = overrideComponentDescriptions(agentName, insertionPoint, orig);
    } catch (Exception ex) {
      e = ex;
    }
    if (e != null) {
      if (e instanceof ComponentInitializerService.InitializerException) {
        throw (ComponentInitializerService.InitializerException) e;
      } else if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(null, e);
      }
    }
    return ret;
  }

  /**
   * Define this method to override each agent's configuration.
   * <p>
   * @return a new list of components, or the passed-in components
   * for no override.
   */
  protected abstract ComponentDescription[] overrideComponentDescriptions(
      String agentName, 
      String insertionPoint,
      ComponentDescription[] components);

  // TODO move to ComponentDescription
  protected static ComponentDescription newComponentDescription(
      String classname) {
    return newComponentDescription(classname, null);
  }
  protected static ComponentDescription newComponentDescription(
      String classname, Object parameter) {
    return newComponentDescription(
        "Node.AgentManager.Agent.PluginManager.Plugin",
        classname,
        parameter);
  }
  protected static ComponentDescription newComponentDescription(
      String insertionPoint, String classname, Object parameter) {
    List args = 
      (parameter == null ? Collections.EMPTY_LIST :
       parameter instanceof String ? Collections.singletonList(parameter) :
       parameter instanceof String[] ? Arrays.asList((String[]) parameter) :
       (List) parameter);
    String name;
    {
      StringBuffer buf = new StringBuffer();
      buf.append(classname);
      buf.append("_").append(insertionPoint).append("(");
      for (int i = 0; i < args.size(); i++) {
        if (i > 0) {
          buf.append(",");
        }
        buf.append(args.get(i));
      }
      buf.append(")");
      name = buf.toString();
    }
    return new ComponentDescription(
        name,
        insertionPoint,
        classname,
        null,  // codebase
        (args.isEmpty() ? null : args),
        null,  // certificate
        null,  // lease
        null,  // policy
        ComponentDescription.PRIORITY_COMPONENT);
  }
  protected static String toXML(
      ComponentDescription cd, String indent, boolean verbose) {
    StringBuffer buf = new StringBuffer();
    buf.append(indent).append("<component");
    if (verbose) {
      buf.append(indent).append("  name='");
      buf.append(cd.getName());
      buf.append("'").append(indent).append(" ");
    }
    buf.append(" class='");
    buf.append(cd.getClassname());
    buf.append("'");
    int p = cd.getPriority();
    if (verbose || p != ComponentDescription.PRIORITY_COMPONENT) {
      buf.append(indent).append("  ");
      buf.append("priority='");
      buf.append(ComponentDescription.priorityToString(p));
      buf.append("'");
    }
    String ip = cd.getInsertionPoint();
    if (verbose ||
        !ip.equals("Node.AgentManager.Agent.PluginManager.Plugin")) {
      buf.append(indent).append("  ");
      buf.append("insertionpoint='");
      buf.append(cd.getInsertionPoint());
      buf.append("'");
    }
    buf.append(">");
    Object o = cd.getParameter();
    if (o instanceof List) {
      List args = (List) o;
      for (int i = 0; i < args.size(); i++) {
        buf.append(indent).append("  <argument>");
        buf.append(indent).append("    ").append(args.get(i));
        buf.append(indent).append("  </argument>");
      }
    }
    buf.append(indent).append("</component>");
    return buf.toString();
  }

  protected boolean includesDefaultComponents() {
    return root_cis.includesDefaultComponents();
  }

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = getServiceBroker().getService(
       this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }

  private class MySP implements ServiceProvider {
    private final ComponentInitializerService cis = 
      new ComponentInitializerService() {
        public ComponentDescription[] getComponentDescriptions(
            String agentName, 
            String insertionPoint) throws InitializerException {
          return ConfiguratorBase.this.getComponentDescriptions(
              agentName, insertionPoint);
        }
        public boolean includesDefaultComponents() {
          return ConfiguratorBase.this.includesDefaultComponents();
        }
      };
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      return
        (ComponentInitializerService.class.isAssignableFrom(serviceClass) ?
         cis : null);
    }
    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service) {
    }
  }
}
