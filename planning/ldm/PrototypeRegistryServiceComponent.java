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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PrototypeRegistry;
import org.cougaar.planning.service.PrototypeRegistryService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component provides the PrototypeRegistryService.
 */
public final class PrototypeRegistryServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private PrototypeRegistry pr;
  private PrototypeRegistryService prS;
  private PrototypeRegistryServiceProvider prSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void load() {
    super.load();

    // create a single per-agent uid service instance
    this.pr = new PrototypeRegistry();
    this.prS = new PrototypeRegistryServiceImpl();

    // create and advertise our service
    this.prSP = new PrototypeRegistryServiceProvider();
    sb.addService(PrototypeRegistryService.class, prSP);
  }

  public void unload() {
    // revoke our service
    if (prSP != null) {
      sb.revokeService(PrototypeRegistryService.class, prSP);
      prSP = null;
    }
    // clear pr?
    super.unload();
  }

  private class PrototypeRegistryServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (PrototypeRegistryService.class.isAssignableFrom(serviceClass)) {
        return prS;
      } else {
        return null;
      }
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service)  {
    }
  }

  /** adapter for PrototypeRegistry -to- PrototypeRegistryService */
  private final class PrototypeRegistryServiceImpl implements PrototypeRegistryService {
    public void addPrototypeProvider(PrototypeProvider prov) {
      pr.addPrototypeProvider(prov);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      pr.addPropertyProvider(prov);
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      pr.addLatePropertyProvider(lpp);
    }
    public void cachePrototype(String aTypeName, Asset aPrototype) {
      pr.cachePrototype(aTypeName, aPrototype);
    }
    public boolean isPrototypeCached(String aTypeName) {
      return pr.isPrototypeCached(aTypeName);
    }
    public Asset getPrototype(String aTypeName, Class anAssetClass) {
      return pr.getPrototype(aTypeName, anAssetClass);
    }
    public Asset getPrototype(String aTypeName) {
      return pr.getPrototype(aTypeName);
    }
    public void fillProperties(Asset anAsset) {
      pr.fillProperties(anAsset);
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return pr.lateFillPropertyGroup(anAsset, pg, time);
    }
    //metrics service hooks
    public int getPrototypeProviderCount() {
      return pr.getPrototypeProviderCount();
    }
    public int getPropertyProviderCount() {
      return pr.getPropertyProviderCount();
    }
    public int getCachedPrototypeCount() {
      return pr.getCachedPrototypeCount();
    }
  }  // end of PrototypeRegistryServiceImpl

}
