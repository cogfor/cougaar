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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.UIDServer;
import org.cougaar.core.service.UIDService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.service.LDMService;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The LDMServiceComponent is a provider class for the LDM 
 * service within an agent.
 */
public final class LDMServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private LDMServesPlugin lsp;

  private MessageAddress agentId;
  private AgentIdentificationService ais;
  private DomainService ds;
  private PrototypeRegistryService prs;
  private UIDService uids;

  private LDMService ldmS;
  private LDMServiceProvider ldmSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.ais = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.agentId = ais.getMessageAddress();
    }
  }
  public void setPrototypeRegistryService(PrototypeRegistryService prs) {
    this.prs = prs;
  }
  public void setUIDService(UIDService uids) {
    this.uids = uids;
  }
  
  public void load() {
    super.load();

    // ensure services
    Class missingServiceClass =
      (ais==null?AgentIdentificationService.class:
       prs==null?PrototypeRegistryService.class:
       uids==null?UIDService.class:
       null);
    if (missingServiceClass != null) {
      throw new RuntimeException(
          "Missing service: "+missingServiceClass.getName());
    }

    // create a single per-agent ldm service instance
    this.ldmS = new LDMServiceImpl();

    // create and advertise our service
    this.ldmSP = new LDMServiceProvider();
    sb.addService(LDMService.class, ldmSP);
  }

  /**
   * Work-around an awkward domain/ldm load order issue.
   * <p>
   * The PlanningDomain needs the LDMService to bind Assets, but our
   * LDMService needs the DomainService to do factory lookup.
   * Fortunately the DomainManager advertises the DomainService
   * prior to loading the domains, so this *should* work out okay.
   */
  private DomainService getDomainService() {
    if (ds == null) {
      ds = (DomainService)
        sb.getService(this, DomainService.class, null);
    }
    return ds;
  }

  public void unload() {
    // revoke our service
    if (ldmSP != null) {
      sb.revokeService(LDMService.class, ldmSP);
      ldmSP = null;
    }
    if (uids != null) {
      sb.releaseService(this, UIDService.class, uids);
      uids = null;
    }
    if (prs != null) {
      sb.releaseService(this, PrototypeRegistryService.class, prs);
      prs = null;
    }
    if (ds != null) {
      sb.releaseService(this, DomainService.class, ds);
      ds = null;
    }
    if (ais != null) {
      sb.releaseService(this, AgentIdentificationService.class, ais);
      ais = null;
    }
    super.unload();
  }

  private class LDMServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (LDMService.class.isAssignableFrom(serviceClass)) {
        return ldmS;
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

  private class LDMServiceImpl implements LDMService {
    private PlanningFactory pf;
    private LDMServesPlugin lsp;
    public MessageAddress getMessageAddress() {
      return agentId;
    }
    public LDMServesPlugin getLDM() {
      if (lsp == null) {
        lsp = new LDMAdapter();
      }
      return lsp;
    }
    public UIDServer getUIDServer() {
      return uids;
    }
    public PlanningFactory getFactory() {
      if (pf == null) {
        pf = (PlanningFactory) getFactory("planning");
      }
      return pf;
    }
    public Factory getFactory(String s) {
      return getDomainService().getFactory(s);
    }
    public Factory getFactory(Class cl) {
      return getDomainService().getFactory(cl);
    }
    public List getFactories() {
      return getDomainService().getFactories();
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
  
  private class LDMAdapter implements LDMServesPlugin {
    private PlanningFactory pf;
    public Asset getPrototype(String aTypeName) {
      return prs.getPrototype(aTypeName);
    }
    public Asset getPrototype(String aTypeName, Class anAssetClass) {
      return prs.getPrototype(aTypeName, anAssetClass);
    }
    public boolean isPrototypeCached(String aTypeName) {
      return prs.isPrototypeCached(aTypeName);   
    }
    public ClassLoader getLDMClassLoader() {
      return getClass().getClassLoader();
    }
    public Factory getFactory(Class domainClass) {
      return getDomainService().getFactory(domainClass);
    }
    public Factory getFactory(String domainName) {
      return getDomainService().getFactory(domainName);
    }
    public int getCachedPrototypeCount() {
      return prs.getCachedPrototypeCount();
    }
    public int getPropertyProviderCount() {
      return prs.getPropertyProviderCount();
    }
    public int getPrototypeProviderCount() {
      return prs.getPrototypeProviderCount();
    }
    public LDMServesPlugin getLDM() {
      return this;
    }
    public MessageAddress getMessageAddress() {
      return agentId;
    }
    public PlanningFactory getFactory() {
      if (pf == null) {
        pf = (PlanningFactory) getFactory("planning");
      }
      return pf;
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return prs.lateFillPropertyGroup(anAsset, pg, time);
    }
    public UIDServer getUIDServer() {
      return uids;
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      prs.addLatePropertyProvider(lpp);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      prs.addPropertyProvider(prov);
    }
    public void addPrototypeProvider(PrototypeProvider prov) {
      prs.addPrototypeProvider(prov);
    }
    public void cachePrototype(String aTypeName, Asset aPrototype) {
      prs.cachePrototype(aTypeName, aPrototype);
    }
    public void fillProperties(Asset anAsset) {
      prs.fillProperties(anAsset);
    }
  }
}
