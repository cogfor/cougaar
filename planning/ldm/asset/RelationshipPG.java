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
/** Primary client interface for RelationshipPG.
 *  @see NewRelationshipPG
 *  @see RelationshipPGImpl
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
public interface RelationshipPG extends PropertyGroup, LocalPG {
  /** a schedule of relationships for this asset. **/
  org.cougaar.planning.ldm.plan.RelationshipSchedule getRelationshipSchedule();
  boolean getLocal();

  PGDelegate copy(PropertyGroup pg);
  void readObject(java.io.ObjectInputStream in);
  void writeObject(java.io.ObjectOutputStream out);
  void init(NewRelationshipPG pg, org.cougaar.planning.ldm.plan.HasRelationships hasRelationships);
  boolean isLocal();
  boolean isSelf();
  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newRelationshipPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.planning.ldm.asset.NewRelationshipPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.planning.ldm.asset.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.planning.ldm.asset.RelationshipPG.class;
  String assetSetter = "setRelationshipPG";
  String assetGetter = "getRelationshipPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  RelationshipPG nullPG = new Null_RelationshipPG();

/** Null_PG implementation for RelationshipPG **/
final class Null_RelationshipPG
  implements RelationshipPG, Null_PG
{
  public org.cougaar.planning.ldm.plan.RelationshipSchedule getRelationshipSchedule() { throw new UndefinedValueException(); }
  public boolean getLocal() { throw new UndefinedValueException(); }
  public RelationshipBG getRelationshipBG() {
    throw new UndefinedValueException();
  }
  public void setRelationshipBG(RelationshipBG _relationshipBG) {
    throw new UndefinedValueException();
  }
  public PGDelegate copy(PropertyGroup pg) { throw new UndefinedValueException(); }
  public void readObject(java.io.ObjectInputStream in) { throw new UndefinedValueException(); }
  public void writeObject(java.io.ObjectOutputStream out) { throw new UndefinedValueException(); }
  public void init(NewRelationshipPG pg, org.cougaar.planning.ldm.plan.HasRelationships hasRelationships) { throw new UndefinedValueException(); }
  public boolean isLocal() { throw new UndefinedValueException(); }
  public boolean isSelf() { throw new UndefinedValueException(); }
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
    return RelationshipPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for RelationshipPG **/
final class Future
  implements RelationshipPG, Future_PG
{
  public org.cougaar.planning.ldm.plan.RelationshipSchedule getRelationshipSchedule() {
    waitForFinalize();
    return _real.getRelationshipSchedule();
  }
  public boolean getLocal() {
    waitForFinalize();
    return _real.getLocal();
  }
  public boolean equals(Object object) {
    waitForFinalize();
    return _real.equals(object);
  }
  public PGDelegate copy(PropertyGroup pg) {
    waitForFinalize();
    return _real.copy(pg);
  }
  public void readObject(java.io.ObjectInputStream in) {
    waitForFinalize();
    _real.readObject(in);
  }
  public void writeObject(java.io.ObjectOutputStream out) {
    waitForFinalize();
    _real.writeObject(out);
  }
  public void init(NewRelationshipPG pg, org.cougaar.planning.ldm.plan.HasRelationships hasRelationships) {
    waitForFinalize();
    _real.init(pg, hasRelationships);
  }
  public boolean isLocal() {
    waitForFinalize();
    return _real.isLocal();
  }
  public boolean isSelf() {
    waitForFinalize();
    return _real.isSelf();
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
    return RelationshipPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private RelationshipPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof RelationshipPG) {
      _real=(RelationshipPG) real;
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
