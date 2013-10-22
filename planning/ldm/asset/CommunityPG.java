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
/** Primary client interface for CommunityPG.
 *  @see NewCommunityPG
 *  @see CommunityPGImpl
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.TimeSpan;
import java.util.*;



public interface CommunityPG extends TimePhasedPropertyGroup {
  /** Collection of community names to which the asset belongs at the specified time. **/
  Collection getCommunities();
  /** test to see if an element is a member of the communities Collection **/
  boolean inCommunities(String element);

  /** array getter for beans **/
  String[] getCommunitiesAsArray();

  /** indexed getter for beans **/
  String getIndexedCommunities(int index);


  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newCommunityPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "org.cougaar.planning.ldm.asset.NewCommunityPG";
  /** the factory class **/
  Class factoryClass = org.cougaar.planning.ldm.asset.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = org.cougaar.planning.ldm.asset.CommunityPG.class;
  String assetSetter = "setCommunityPG";
  String assetGetter = "getCommunityPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  CommunityPG nullPG = new Null_CommunityPG();

/** Null_PG implementation for CommunityPG **/
final class Null_CommunityPG
  implements CommunityPG, Null_PG
{
  public Collection getCommunities() { throw new UndefinedValueException(); }
  public boolean inCommunities(String element) { return false; }
  public String[] getCommunitiesAsArray() { return null; }
  public String getIndexedCommunities(int index) { throw new UndefinedValueException(); }
  public long getStartTime() { throw new UndefinedValueException(); }
  public long getEndTime() { throw new UndefinedValueException(); }
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
    return CommunityPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
}

/** Future PG implementation for CommunityPG **/
final class Future
  implements CommunityPG, Future_PG
{
  public Collection getCommunities() {
    waitForFinalize();
    return _real.getCommunities();
  }
  public boolean inCommunities(String element) {
    waitForFinalize();
    return _real.inCommunities(element);
  }
  public String[] getCommunitiesAsArray() {
    waitForFinalize();
    return _real.getCommunitiesAsArray();
  }
  public String getIndexedCommunities(int index) {
    waitForFinalize();
    return _real.getIndexedCommunities(index);
  }
  public boolean equals(Object object) {
    waitForFinalize();
    return _real.equals(object);
  }
  public long getStartTime() {
    waitForFinalize();
    return _real.getStartTime();
  }
  public long getEndTime() {
    waitForFinalize();
    return _real.getEndTime();
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
    return CommunityPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }

  // Finalization support
  private CommunityPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof CommunityPG) {
      _real=(CommunityPG) real;
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
