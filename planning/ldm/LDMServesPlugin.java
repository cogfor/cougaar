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

package org.cougaar.planning.ldm;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.service.PrototypeRegistryService;
import java.util.HashMap;
import java.util.Map;

/**
 * Plugins primary interface to the LDM.
 *
 * @see org.cougaar.planning.ldm.LDMPluginServesLDM
 **/
public interface LDMServesPlugin extends PrototypeRegistryService {

  /**
   * Equivalent to <code>((PlanningFactory) getFactory("planning"))</code>.
   */
  PlanningFactory getFactory();

  /** @return the Requested Domain's LDM Factory.
   **/
  Factory getFactory(String domainName);

  /** @return the Requested Domain's LDM Factory.
   **/
  Factory getFactory(Class domainClass);

  /** @return the classloader to be used for loading classes for the LDM.
   * Domain Plugins should not use this, as they will have been themselves
   * loaded by this ClassLoader.  Some infrastructure components which are
   * not loaded in the same way will require this for correct operation.
   **/
  ClassLoader getLDMClassLoader();

  /** The current agent's Address */
  MessageAddress getMessageAddress();
  
  UIDServer getUIDServer();

  /**
   * If the Delegator is used, this gets the real thing
   **/
  LDMServesPlugin getLDM();

  class Delegator implements LDMServesPlugin {
    private LDMServesPlugin ldm;
    Delegator() { }

    synchronized void setLDM(LDMServesPlugin ldm) {
      this.ldm = ldm;
    }

    public LDMServesPlugin getLDM() {
      return ldm != null ? ldm : this;
    }
    public void addPrototypeProvider(PrototypeProvider prov) {
      ldm.addPrototypeProvider(prov);
    }
    public void addPropertyProvider(PropertyProvider prov) {
      ldm.addPropertyProvider(prov);
    }
    public void addLatePropertyProvider(LatePropertyProvider lpp) {
      ldm.addLatePropertyProvider(lpp);
    }
    public void fillProperties(Asset anAsset) {
      ldm.fillProperties(anAsset);
    }
    public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time) {
      return ldm.lateFillPropertyGroup(anAsset, pg, time);
    }
    public int getPrototypeProviderCount() {
      return ldm.getPrototypeProviderCount();
    }
    public int getPropertyProviderCount() {
      return ldm.getPropertyProviderCount();
    }
    public PlanningFactory getFactory() {
      return ldm.getFactory();
    }
    public Factory getFactory(String domainName) {
      return ldm.getFactory(domainName);
    }
    public Factory getFactory(Class domainClass) {
      return ldm.getFactory(domainClass);
    }
    public ClassLoader getLDMClassLoader() {
      return ldm.getLDMClassLoader();
    }
    public MessageAddress getMessageAddress() {
      return ldm.getMessageAddress();
    }
    public UIDServer getUIDServer() {
      return ldm.getUIDServer();
    }

    //
    // set up a temporary prototype cache while bootstrapping
    //

    private HashMap _pcache;
    private HashMap pc() {      /* must be called within synchronized */
      if (_pcache == null) {
        _pcache = new HashMap(13);
      }
      return _pcache;
    }

    // called by LDMContextTable to read out any cached prototypes into the real one
    synchronized HashMap flushTemporaryPrototypeCache() {
      HashMap c = _pcache;
      _pcache = null;
      return c;
    }

    public synchronized void cachePrototype(String aTypeName, Asset aPrototype) {
      if (ldm != null) {
        ldm.cachePrototype(aTypeName, aPrototype);
      } else {
        pc().put(aTypeName, aPrototype);
      }
    }
    public synchronized boolean isPrototypeCached(String aTypeName) {
      if (ldm != null) {
        return ldm.isPrototypeCached(aTypeName);
      } else {
        return (_pcache==null?false:_pcache.get(aTypeName)!=null);
      }
    }
    public synchronized Asset getPrototype(String aTypeName, Class anAssetClass) {
      if (ldm != null) {
        return ldm.getPrototype(aTypeName, anAssetClass);
      } else {
        return (Asset) (_pcache==null?null:_pcache.get(aTypeName)); /*no hint passed, since we've got no actual providers*/
      }
    }
    public synchronized Asset getPrototype(String aTypeName) {
      if (ldm != null) {
        return ldm.getPrototype(aTypeName);
      } else {
        return (Asset) (_pcache == null?null:_pcache.get(aTypeName));
      }        
    }
    public synchronized int getCachedPrototypeCount() {
      if (ldm != null) {
        return ldm.getCachedPrototypeCount();
      } else {
        return (_pcache == null)?0:pc().size();
      }
    }
  }
}
