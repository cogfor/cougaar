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

import java.io.Serializable;

public interface PropertyGroup extends Serializable, Cloneable {

  Object clone() throws CloneNotSupportedException;

  /** Unlock the PropertyGroup by returning an object which
   * has setter methods that side-effect this object.
   * The key must be == the key that locked the property
   * in the first place or an Exception is thrown.
   * @exception IllegalAccessException
   **/
  NewPropertyGroup unlock(Object key) throws IllegalAccessException;

  /** lock a property by returning an immutable object which
   * has a private view into the original object.
   * If key == null, the result is a locked object which cannot be unlocked.
   **/
  PropertyGroup lock(Object key);

  /** alias for lock(null)
   **/
  PropertyGroup lock();

  /** Convenience method. equivalent to clone();
   **/
  PropertyGroup copy();

  /** returns the class of the main property interface for this 
   * property group.  
   **/
  Class getPrimaryClass();

  /** @return the method name on an asset to retrieve the PG **/
  String getAssetGetMethod();
  /** @return the method name on an asset to set the PG **/
  String getAssetSetMethod();


  // DataQuality
  /** @return true IFF the instance not only supports DataQuality
   * queries (e.g. is instanceof HasDataQuality), but getDataQuality()
   * will return non-null.
   **/
  boolean hasDataQuality();
}
