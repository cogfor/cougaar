/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.planning.plugin;

import java.util.*;
import org.cougaar.util.log.Logger;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceFilter;
import org.cougaar.core.component.ServiceFilterBinder;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.planning.ldm.plan.Task;

/** A Binder for plugins which complains when various known-bad plugin behaviors
 * are observed.
 * Currently, it complains (at warn level) about non-local Tasks which are added or changed.
 **/
public class PluginSanityBinder
  extends ServiceFilter
{
  private Logger _log;
  public void setLoggingService(LoggingService l) { _log = l; }
  protected Logger log() { return _log; }

  private AgentIdentificationService _ais;
  private MessageAddress _ma;
  public void setAgentIdentificationService(AgentIdentificationService s) { 
    _ais = s; 
    _ma = s!=null?s.getMessageAddress():null;
  }
  protected MessageAddress getMessageAddress() { return _ma; }

  // strictly-speaking, this is a BinderFactory and Enforcer is the actual binder

  // activate the Enforcer
  public Binder getBinder(Object child) {
    return new Enforcer(this, child);
  }
  
  /** Enforcer installs service proxies as needed **/
  protected class Enforcer
    extends ServiceFilterBinder
  {
    public Enforcer(BinderFactory bf, Object child) { super(bf,child); }

    // install the ServiceBroker proxy 
    protected ServiceBroker createFilteringServiceBroker(ServiceBroker sb) {
      return new EnforcerServiceBroker(sb); 
    }

    // Sb proxy to insert Service Proxies
    protected class EnforcerServiceBroker extends FilteringServiceBroker
    {
      public EnforcerServiceBroker(ServiceBroker sb) { super(sb); }

      // insert our own BlackboardServiceProxy
      protected Object getServiceProxy(Object service, Class serviceClass, Object client) {
        if (service instanceof BlackboardService) {
          return new BlackboardServiceProxy((BlackboardService) service, client);
        } 
        return null;
      }
    }
  }

  /** PluginSanityBinder's BlackboardService.
   * Here we check for any obvious subscription/BB bad behavior.
   * In particular, we log complaints when:
   **
   **/
  private class BlackboardServiceProxy extends BlackboardService.Delegate {
    private final Object client;
    public BlackboardServiceProxy(BlackboardService bs, Object client) {
      super(bs);
      this.client=client;
      if (log().isInfoEnabled()) {
        log().info("SanityChecking "+client);      
      }
    }
    
    public void publishAdd(Object o) {
      auditPublishAdd(o);
      super.publishAdd(o);
    }
    public void publishRemove(Object o) {
      auditPublishRemove(o);
      super.publishRemove(o);
    }
    public void publishChange(Object o) {
      auditPublishChange(o);
      super.publishChange(o);
    }
    public void publishChange(Object o, Collection changes) {
      auditPublishChange(o);
      super.publishChange(o,changes);
    }
  }

  void auditPublishAdd(Object o) {
    if (o instanceof Task) {
      Task t = (Task) o;
      if (!equalp(t.getSource(), getMessageAddress())) {
        log().warn("publishAdd of non-local task: "+t);
      }
    }
  }
  void auditPublishRemove(Object o) {
  }
  void auditPublishChange(Object o) {
    if (o instanceof Task) {
      Task t = (Task) o;
      if (!equalp(t.getSource(), getMessageAddress())) {
        log().warn("publishChange of non-local task: "+t);
      }
    }
  }

  static final boolean equalp(Object a, Object b) {
    return (a==null)?(b==null):(a.equals(b));
  }
}
