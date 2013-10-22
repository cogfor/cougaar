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

import org.cougaar.planning.ldm.asset.Asset;

/**
 * A provider of prototype Assets to the LDM.
 * @see org.cougaar.planning.ldm.LDMPluginServesLDM
 **/
public interface PrototypeProvider extends LDMPluginServesLDM {
  
  /** return the prototype Asset described by aTypeName.
   * implementations should probably call LDMServesPlugin.cachePrototype
   * and LDMServesPlugin.fillProperties if needed before returning.
   *
   * May return null if aTypeName is not something that the implementation
   * knows about.
   *
   * An example aTypeName: "NSN/12345678901234".
   *
   * The returned Asset will usually, but not always have a primary 
   * type identifier that is equal to the aTypeName.  In cases where
   * it does not match, aTypeName must appear as one of the extra type
   * identifiers of the returned asset.  PrototypeProviders should cache
   * the prototype under both type identifiers in these cases.
   *
   * @param aTypeName specifies an Asset description. 
   * @param anAssetClassHint is an optional hint to LDM plugins
   * to reduce their potential work load.  If non-null, the returned asset 
   * (if any) should be an instance the specified class or one of its
   * subclasses.
   **/
  Asset getPrototype(String aTypeName, Class anAssetClassHint);

  /** bulk version of getPrototype(String).
   * Will never return null.
   **/
  // Enumeration getPrototypes(Enumeration typeNames);
}
