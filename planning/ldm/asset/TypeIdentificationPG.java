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
/** Primary client interface for TypeIdentificationPG.
 * Identification of a type of Asset, e.g. by NSN, part number, etc.
 *  @see NewTypeIdentificationPG
 *  @see TypeIdentificationPGImpl
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



public interface TypeIdentificationPG extends PropertyGroup {
  /** A String of the form "<type>/<id>", e.g. "NSN/0913801298439" **/
  String getTypeIdentification();
  /** A human-readable description of this type. **/
  String getNomenclature();
  /** examples: "LIN/T13168", "DODIC/C786" **/
  String getAlternateTypeIdentification();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newTypeIdentificationPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.planning.ldm.asset.NewTypeIdentificationPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.planning.ldm.asset.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.planning.ldm.asset.TypeIdentificationPG.class;
  String assetSetter = "setTypeIdentificationPG";
  String assetGetter = "getTypeIdentificationPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  TypeIdentificationPG nullPG = new Null_TypeIdentificationPG();

/** Null_PG implementation for TypeIdentificationPG **/
final class Null_TypeIdentificationPG
  implements TypeIdentificationPG, Null_PG
{
  public String getTypeIdentification() { throw new UndefinedValueException(); }
  public String getNomenclature() { throw new UndefinedValueException(); }
  public String getAlternateTypeIdentification() { throw new UndefinedValueException(); }
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
    return TypeIdentificationPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for TypeIdentificationPG **/
final class Future
  implements TypeIdentificationPG, Future_PG
{
  public String getTypeIdentification() {
    waitForFinalize();
    return _real.getTypeIdentification();
  }
  public String getNomenclature() {
    waitForFinalize();
    return _real.getNomenclature();
  }
  public String getAlternateTypeIdentification() {
    waitForFinalize();
    return _real.getAlternateTypeIdentification();
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
    return TypeIdentificationPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private TypeIdentificationPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof TypeIdentificationPG) {
      _real=(TypeIdentificationPG) real;
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
