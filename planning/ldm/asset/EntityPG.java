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
/** Primary client interface for EntityPG.
 *  @see NewEntityPG
 *  @see EntityPGImpl
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



public interface EntityPG extends PropertyGroup {
  /** Collection of Roles the organization is capable of fulfilling. **/
  Collection getRoles();
  /** test to see if an element is a member of the roles Collection **/
  boolean inRoles(Role element);

  /** array getter for beans **/
  Role[] getRolesAsArray();

  /** indexed getter for beans **/
  Role getIndexedRoles(int index);


  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newEntityPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.planning.ldm.asset.NewEntityPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.planning.ldm.asset.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.planning.ldm.asset.EntityPG.class;
  String assetSetter = "setEntityPG";
  String assetGetter = "getEntityPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  EntityPG nullPG = new Null_EntityPG();

/** Null_PG implementation for EntityPG **/
final class Null_EntityPG
  implements EntityPG, Null_PG
{
  public Collection getRoles() { throw new UndefinedValueException(); }
  public boolean inRoles(Role element) { return false; }
  public Role[] getRolesAsArray() { return null; }
  public Role getIndexedRoles(int index) { throw new UndefinedValueException(); }
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
    return EntityPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for EntityPG **/
final class Future
  implements EntityPG, Future_PG
{
  public Collection getRoles() {
    waitForFinalize();
    return _real.getRoles();
  }
  public boolean inRoles(Role element) {
    waitForFinalize();
    return _real.inRoles(element);
  }
  public Role[] getRolesAsArray() {
    waitForFinalize();
    return _real.getRolesAsArray();
  }
  public Role getIndexedRoles(int index) {
    waitForFinalize();
    return _real.getIndexedRoles(index);
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
    return EntityPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private EntityPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof EntityPG) {
      _real=(EntityPG) real;
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
