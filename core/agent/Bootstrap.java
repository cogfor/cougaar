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

package org.cougaar.core.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * This component is the first component added to an agent, and
 * is used to bootstrap the agent with the minimal initial
 * components.
 * <p>
 * The last component bootstrapped in is {@link FindComponentsEarly},
 * which checks for persisted component model state.
 *
 * @see FindComponentsEarly 
 */
public final class Bootstrap
extends GenericStateModelAdapter
implements Component
{

  private String agentName;

  private ServiceBroker sb;

  public void setParameter(Object x) {
    Object o = x;
    if (o instanceof List) {
      List l = (List) o;
      if (!l.isEmpty()) {
        o = l.get(0);
      }
    }
    if (o instanceof String) {
      agentName = (String) o;
    } else if (o instanceof MessageAddress) {
      agentName = ((MessageAddress) o).getAddress();
    } else {
      throw new IllegalArgumentException("Invalid agent parameter: "+o);
    }
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();
    List l = getInitialComponents();
    overrideComponentList(l);
  }

  private List getInitialComponents() {
    List l = new ArrayList();

    // query comp-init service
    boolean includesDefaultComponents = true;
    ComponentInitializerService cis = sb.getService(this, ComponentInitializerService.class, null);
    try {
      includesDefaultComponents = cis.includesDefaultComponents();
      ComponentDescription[] descs =
        cis.getComponentDescriptions(agentName, Agent.INSERTION_POINT);
      if (descs != null) {
        l.addAll(Arrays.asList(descs));
      }
    } catch (ComponentInitializerService.InitializerException cise) {
      Logger log = Logging.getLogger(this.getClass());
      if (log.isInfoEnabled()) {
        log.info("\nUnable to add "+agentName+"'s components", cise);
      }
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }

    if (!includesDefaultComponents) {
      // In the past we had a "DefaultComponents" class that hard-coded the XSL
      // template components for non-XML configurations, e.g. INIs and DBs.
      //
      // That was a mess and has long been deprecated.
      //
      // The proposed solution is outlined in Node comments on how the
      // ComponentInitializerService should be refactored.  The agent
      // configuration will be split into two services:
      //   a) an application configuration (XML, INI, DB, ...)
      //   b) an environment configuration (XSL template, ...)
      // All application configuration formats will be able to use the existing
      // XSL templates.
      Logger log = Logging.getLogger(this.getClass());
      log.error("Unable to find default components for "+agentName);
    }

    if (!l.isEmpty()) {
      // pass the agentName to first component
      //
      // This is a hack, as noted below in the "setAgentName" method.
      ComponentDescription c0 = (ComponentDescription) l.get(0);
      ComponentDescription new_c0 = setAgentName(c0, agentName);
      l.set(0, new_c0);
    }

    return l;
  }

  private void overrideComponentList(List l) {
    AgentBootstrapService abs = sb.getService(this, AgentBootstrapService.class, null);
    if (abs == null) {
      throw new RuntimeException(
          "Unable to obtain AgentBootstrapService"+
          ", can not override the agent's component list");
    }
    abs.overrideComponentList(l);
    sb.releaseService(this, AgentBootstrapService.class, abs);
    abs = null;
  }

  // modify a component description with the specified $AGENT_NAME
  //
  // This could be enhanced to support arbitrary parameters, not just
  // our first "agentName" parameter.  However, this should be the
  // ComponentInitializerService's job, not the Bootstrap's job.
  //
  // In fact, this agent name should be a expressed as a per-agent "envOption",
  // as described in the "future design ideas" in the Node class.  That
  // approach would correctly pass this agentName as an XSL template parameter,
  // which would make it accessable anywhere in the template.
  //
  // More generally, the "addAgent(...)" method should support arbitrary
  // "envOptions" and support an in-memory configuration.
  private static ComponentDescription setAgentName(
      ComponentDescription desc, String agentName) {
    // see if the first param matches "$AGENT_NAME"
    boolean matches = false;
    Object o = desc.getParameter();
    if (o instanceof List) {
      List l = (List) o;
      if (l.size() > 0) {
        Object v = l.get(0);
        if (v.equals("$AGENT_NAME")) {
          matches = true;
        }
      }
    }
    if (!matches) return desc;

    // replace the first param with our agent name
    List l2 = new ArrayList((List) o);
    l2.set(0, agentName);
    l2 = Collections.unmodifiableList(l2);

    // return the modified desc
    return new ComponentDescription(
        desc.getName(),
        desc.getInsertionPoint(),
        desc.getClassname(),
        desc.getCodebase(),
        l2, // new params
        desc.getCertificate(),
        desc.getLeaseRequested(),
        desc.getPolicy(),
        desc.getPriority());
  }
}
