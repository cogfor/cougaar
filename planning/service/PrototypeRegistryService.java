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

package org.cougaar.planning.service;

import org.cougaar.core.component.Service;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.planning.ldm.PropertyProvider;
import org.cougaar.planning.ldm.PrototypeProvider;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.PropertyGroup;

public interface PrototypeRegistryService extends Service {
  void addPrototypeProvider(PrototypeProvider prov);

  void addPropertyProvider(PropertyProvider prov);
  
  void addLatePropertyProvider(LatePropertyProvider lpp);

   
  /** Request that a prototype be remembered by the LDM so that
   * getPrototype(aTypeName) is likely to return aPrototype
   * without having to make calls to PrototypeProvider.getPrototype(aTypeName).
   * Note that the lifespan of a prototype in the prototype registry may
   * be finite (or even zero!).
   * Note: this method should be used only by PrototypeProvider LDM Plugins.
   **/
  void cachePrototype(String aTypeName, Asset aPrototype);

  /** is there a prototype with the specified name currently in
   * the prototype cache?
   **/
  boolean isPrototypeCached(String aTypeName);   

  /** find the prototype Asset named by aTypeName.  This service
   * will actually be provided by a PrototypeProvider via a call to
   * getPrototype(aTypeName).
   * It will return null if no prototype is found or can be created
   * with that name.
   * There is no need for a client of this method to call cachePrototype
   * on the returned object (that task is left to whatever prototypeProvider
   * was responsible for generating the prototype).
   *
   * Some future release might want to throw an exception if not found.
   * An example aTypeName: "NSN/12345678901234".
   *
   * The returned Asset will usually, but not always have a primary 
   * type identifier that is equal to the aTypeName.  In cases where
   * it does not match, aTypeName must appear as one of the extra type
   * identifiers of the returned asset.  PrototypeProviders should cache
   * the prototype under both type identifiers in these cases.
   *
   * @param aTypeName specifies an Asset description. 
   * @param anAssetClass is an optional hint to LDM plugins
   * to reduce their potential work load.  If non-null, the returned asset 
   * (if any) should be an instance the specified class or one of its
   * subclasses.  When null, each PrototypeProvider will attemt to decode
   * the aTypeName enough to determine if it can supply prototypes of that
   * type.
   **/
  Asset getPrototype(String aTypeName, Class anAssetClass);

  /** equivalent to getPrototype(aTypeName, null);
   **/
  Asset getPrototype(String aTypeName);

  /** Notify LDM of a newly created asset.  This is generally for the use
   * of LDMPlugins, but others may use it to request that propertygroups
   * of the new Asset be filled in from various data sources.
   **/
  void fillProperties(Asset anAsset);

  /** Used by assets to activate LateBinding of PropertyGroups to Assets.
   * Called as late as possible when it is not yet known if there is
   * a PG for an asset.
   **/
  PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time);
 
  // metrics service hooks
  /** @return int Count of Prototype Providers  **/
  int getPrototypeProviderCount();

  /** @return int Count of Property Providers **/
  int getPropertyProviderCount();

  /** @return int Count of Cached Prototypes **/
  int getCachedPrototypeCount();
}
