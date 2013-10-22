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

package org.cougaar.planning.ldm.asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.planning.ldm.PropertyProvider;
import org.cougaar.planning.ldm.PrototypeProvider;
import org.cougaar.planning.service.PrototypeRegistryService;

public class PrototypeRegistry implements PrototypeRegistryService {
  /** THEINITIALREGISTRYSIZE specifies the initial size of the HashMap theRegistry,
   *   which contains the Strings of RegistryTerms
   **/
  private final static int THE_INITIAL_REGISTRY_SIZE = 89;
  private final HashMap myRegistry = new HashMap(THE_INITIAL_REGISTRY_SIZE);

  public PrototypeRegistry() {}

  /** set of PrototypeProvider LDM Plugins **/
  // might want this to be prioritized lists
  private final List prototypeProviders = new ArrayList();

  public void addPrototypeProvider(PrototypeProvider prov) {
    if (prov == null) throw new IllegalArgumentException("prov must be non-null");
    synchronized (prototypeProviders) {
      prototypeProviders.add(prov);
    }
  }

  /** set of PropertyProvider LDM Plugins **/
  private final List propertyProviders = new ArrayList();
  public void addPropertyProvider(PropertyProvider prov) {
    if (prov == null) throw new IllegalArgumentException("prov must be non-null");
    synchronized (propertyProviders) {
      propertyProviders.add(prov);
    }
  }

  // use the registry for registering prototypes for now.
  // later, just replace with a hash table.
  public void cachePrototype(String aTypeName, Asset aPrototype) {
    if (aTypeName == null) throw new IllegalArgumentException("aTypeName must be non-null");
    synchronized (myRegistry) {
      myRegistry.put(aTypeName.intern(), aPrototype);
    }
  }

  public boolean isPrototypeCached(String aTypeName) {
    if (aTypeName == null) throw new IllegalArgumentException("aTypeName must be non-null");
    synchronized (myRegistry) {
      return (myRegistry.get(aTypeName) != null);
    }
  }    

  public Asset getPrototype(String aTypeName) {
    if (aTypeName == null) throw new IllegalArgumentException("aTypeName must be non-null");
    return getPrototype(aTypeName, null);
  }
  public Asset getPrototype(String aTypeName, Class anAssetClass) {
    if (aTypeName == null) throw new IllegalArgumentException("aTypeName must be non-null");

    Asset found = null;

    // look in our registry first.
    // the catch is in case some bozo registered a non-asset under this
    // name.
    try {
      synchronized (myRegistry) {
        found = (Asset) myRegistry.get(aTypeName);
      }
      if (found != null) return found;
    } catch (ClassCastException cce) {}
    
    synchronized (prototypeProviders) {
      // else, try the prototype providers
      int l = prototypeProviders.size();
      for (int i = 0; i<l; i++) {
        PrototypeProvider pp = (PrototypeProvider) prototypeProviders.get(i);
        found = pp.getPrototype(aTypeName, anAssetClass);
        if (found != null) return found;
      }
    }

    // might want to throw an exception in a later version
    return null;
  }

  public void fillProperties(Asset anAsset) {
    if (anAsset == null) throw new IllegalArgumentException("anAsset must be non-null");

    // expose the asset to all propertyproviders
    synchronized (propertyProviders) {
      int l = propertyProviders.size();
      for (int i=0; i<l; i++) {
        PropertyProvider pp = (PropertyProvider) propertyProviders.get(i);
        pp.fillProperties(anAsset);
      }
    }
  }
        
  /** hash of PropertyGroup interface to Lists of LatePropertyProvider instances. **/
  private final HashMap latePPs = new HashMap(11);
  /** list of LatePropertyProviders who supply all PropertyGroups **/
  private final ArrayList defaultLatePPs = new ArrayList(3); 
  public void addLatePropertyProvider(LatePropertyProvider lpp) {
    Collection c = lpp.getPropertyGroupsProvided();
    if (c == null) {
      synchronized (defaultLatePPs) {
        defaultLatePPs.add(lpp);
      }
    } else {
      try {
        for (Iterator it = c.iterator(); it.hasNext(); ) {
          Class pgc = (Class) it.next();
          synchronized (latePPs) {
            ArrayList l = (ArrayList) latePPs.get(pgc);
            if (l == null) {
              l = new ArrayList(3);
              latePPs.put(pgc,l);
            }
            synchronized (l) {
              l.add(lpp);
            }
          }
        }
      } catch (ClassCastException e) {
        System.err.println("LatePropertyProvider "+lpp+" returned an illegal PropertyGroup spec:");
        e.printStackTrace();
      }
    }
  }

  /** hook for late-binding **/
  public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pgclass, long t) {
    // specifics
    ArrayList c;
    synchronized (latePPs) {
      c = (ArrayList) latePPs.get(pgclass);
    }
    PropertyGroup pg = null;
    if (c != null) {
      pg = tryLateFillers(c, anAsset, pgclass, t);
    }
    if (pg == null) {
      pg = tryLateFillers(defaultLatePPs, anAsset, pgclass, t);
    }
    return pg;
  }

  /** utility method of lateFillPropertyGroup() **/
  private PropertyGroup tryLateFillers(ArrayList c, Asset anAsset, Class pgclass, long t)
  {
    synchronized (c) {
      int l = c.size();
      for (int i = 0; i<l; i++) {
        LatePropertyProvider lpp = (LatePropertyProvider) c.get(i);
        PropertyGroup pg = lpp.fillPropertyGroup(anAsset, pgclass, t);
        if (pg != null) 
          return pg;
      }
    }
    return null;
  }    
  
 /** Expose the Registry to consumers. 
   **/
  /*
  public Registry getRegistry() {
    return myRegistry;
  }
  */

  //metrics service hooks
  public int getPrototypeProviderCount() {
    return prototypeProviders.size();
  }
  public int getPropertyProviderCount() {
    return propertyProviders.size();
  }
  public int getCachedPrototypeCount() {
    return myRegistry.size();
  }

}


