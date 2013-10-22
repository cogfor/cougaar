/*
 * <copyright>
 *  
 *  Copyright 1997-2012 Raytheon BBN Technologies
 *  under partial sponsorship of the Defense Advanced Research Projects
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

/* @generated Wed Jun 06 07:52:59 EDT 2012 from properties.def - DO NOT HAND EDIT */
/** Primary client interface for ItemIdentificationPG.
 * Identification of a unique, actual Asset, e.g. by VIN, SSN, etc.
 *  @see NewItemIdentificationPG
 *  @see ItemIdentificationPGImpl
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



public interface ItemIdentificationPG extends PropertyGroup {
  /** A String of the form "<type>/<id>", e.g. "SN/105G13F7YM0G" **/
  String getItemIdentification();
  /** A String describing the Asset in human-readable form. **/
  String getNomenclature();
  /**  Not to be used as MessageAddress.  Use ClusterPG.getMessageAddress() instead. **/
  String getAlternateItemIdentification();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newItemIdentificationPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.planning.ldm.asset.NewItemIdentificationPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.planning.ldm.asset.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.planning.ldm.asset.ItemIdentificationPG.class;
  String assetSetter = "setItemIdentificationPG";
  String assetGetter = "getItemIdentificationPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  ItemIdentificationPG nullPG = new Null_ItemIdentificationPG();

/** Null_PG implementation for ItemIdentificationPG **/
final class Null_ItemIdentificationPG
  implements ItemIdentificationPG, Null_PG
{
  public String getItemIdentification() { throw new UndefinedValueException(); }
  public String getNomenclature() { throw new UndefinedValueException(); }
  public String getAlternateItemIdentification() { throw new UndefinedValueException(); }
  public boolean equals(Object object) { throw new UndefinedValueException(); }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return ItemIdentificationPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for ItemIdentificationPG **/
final class Future
  implements ItemIdentificationPG, Future_PG
{
  public String getItemIdentification() {
    waitForFinalize();
    return _real.getItemIdentification();
  }
  public String getNomenclature() {
    waitForFinalize();
    return _real.getNomenclature();
  }
  public String getAlternateItemIdentification() {
    waitForFinalize();
    return _real.getAlternateItemIdentification();
  }
  public boolean equals(Object object) {
    waitForFinalize();
    return _real.equals(object);
  }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return ItemIdentificationPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private ItemIdentificationPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof ItemIdentificationPG) {
      _real=(ItemIdentificationPG) real;
      notifyAll();
    } else {
      throw new IllegalArgumentException("Finalization with wrong class: "+real);
    }
  }
  private synchronized void waitForFinalize() {
    while (_real == null) {
      try {
        wait();
      } catch (InterruptedException _ie) {
        // We should really let waitForFinalize throw InterruptedException
        Thread.interrupted();
      }
    }
  }
}
}
