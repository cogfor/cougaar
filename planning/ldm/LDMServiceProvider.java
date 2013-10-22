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

package org.cougaar.planning.ldm;

import java.util.List;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.UIDServer;
import org.cougaar.planning.service.LDMService;
import org.cougaar.planning.service.PrototypeRegistryService;

/** placeholder to clean up plugin->manager interactions **/
public class LDMServiceProvider implements ServiceProvider
{
  private LDMService ls;
  /*
  public LDMServiceProvider(LDMServesPlugin lsp, PrototypeRegistryService prs, DomainService ds) {
    // rather this was an assert!
    if (lsp == null || prs == null || ds == null)
      throw new IllegalArgumentException("LDMServiceProvider Constructor arguments must be non-null ("+
                                         lsp+", "+prs+", "+ds+")");

    this.ls = new LDMServiceImpl(lsp,prs,ds);
  }
  */
  public LDMServiceProvider(LDMService ls) {
    this.ls = ls;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (LDMService.class.isAssignableFrom(serviceClass)) {
      return ls;
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private static class LDMServiceImpl implements LDMService {
    private LDMServesPlugin lsp;
    private PrototypeRegistryService prs;
    private DomainService ds;
    private LDMServiceImpl(LDMServesPlugin lsp, PrototypeRegistryService prs, DomainService ds) {
      this.lsp = lsp;
      this.prs = prs;
      this.ds = ds;
    }
    public MessageAddress getMessageAddress() {
      return lsp.getMessageAddress();
    }
    public LDMServesPlugin getLDM() {
      return lsp;
    }
    public UIDServer getUIDServer() {
      return lsp.getUIDServer();
    }
    public PlanningFactory getFactory() {
      return (PlanningFactory) getFactory("planning");
    }
    public Factory getFactory(String s) {
      return ds.getFactory(s);
    }
    public Factory getFactory(Class cl) {
      return ds.getFactory(cl);
    }
    public List getFactories() {
      return ds.getFactories();
    }

    // standin API for LDMService called by PluginBinder for temporary support
    public void addPrototypeProvider(PrototypeProvider plugin) {
      prs.addPrototypeProvider(plugin);
    }
    public void addPropertyProvider(PropertyProvider plugin) {
      prs.addPropertyProvider(plugin);
    }
    public void addLatePropertyProvider(LatePropertyProvider plugin) {
      prs.addLatePropertyProvider(plugin);
    }
  }
}
  
