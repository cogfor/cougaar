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
/** Implementation of RelationshipPG.
 *  @see RelationshipPG
 *  @see NewRelationshipPG
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public final class RelationshipPGImpl extends java.beans.SimpleBeanInfo
  implements NewRelationshipPG, Cloneable
{
  public RelationshipPGImpl() {
  }

  // Slots

  private org.cougaar.planning.ldm.plan.RelationshipSchedule theRelationshipSchedule;
  public org.cougaar.planning.ldm.plan.RelationshipSchedule getRelationshipSchedule(){ return theRelationshipSchedule; }
  public void setRelationshipSchedule(org.cougaar.planning.ldm.plan.RelationshipSchedule relationshipSchedule) {
    theRelationshipSchedule=relationshipSchedule;
  }
  private boolean theLocal;
  public boolean getLocal(){ return theLocal; }
  public void setLocal(boolean local) {
    theLocal=local;
  }

  private RelationshipBG relationshipBG = null;
  public RelationshipBG getRelationshipBG() {
    return relationshipBG;
  }
  public void setRelationshipBG(RelationshipBG _relationshipBG) {
    if (relationshipBG != null) throw new IllegalArgumentException("relationshipBG already set");
    relationshipBG = _relationshipBG;
  }
  public PGDelegate copy(PropertyGroup pg) { return relationshipBG.copy(pg);  }
  public void readObject(java.io.ObjectInputStream in) { relationshipBG.readObject(in);  }
  public void writeObject(java.io.ObjectOutputStream out) { relationshipBG.writeObject(out);  }
  public void init(NewRelationshipPG pg, org.cougaar.planning.ldm.plan.HasRelationships hasRelationships) { relationshipBG.init(pg, hasRelationships);  }
  public boolean isLocal() { return relationshipBG.isLocal();  }
  public boolean isSelf() { return relationshipBG.isSelf();  }

  public RelationshipPGImpl(RelationshipPG original) {
    theRelationshipSchedule = original.getRelationshipSchedule();
    theLocal = original.getLocal();
  }

  public boolean equals(Object other) {

    if (!(other instanceof RelationshipPG)) {
      return false;
    }

    RelationshipPG otherRelationshipPG = (RelationshipPG) other;

    if (getRelationshipSchedule() == null) {
      if (otherRelationshipPG.getRelationshipSchedule() != null) {
        return false;
      }
    } else if (!(getRelationshipSchedule().equals(otherRelationshipPG.getRelationshipSchedule()))) {
      return false;
    }

    if (!(getLocal() == otherRelationshipPG.getLocal())) {
      return false;
    }

    if (other instanceof RelationshipPGImpl) {
      if (getRelationshipBG() == null) {
        if (((RelationshipPGImpl) otherRelationshipPG).getRelationshipBG() != null) {
          return false;
        }
      } else if (!(getRelationshipBG().equals(((RelationshipPGImpl) otherRelationshipPG).getRelationshipBG()))) {
        return false;
      }

    }
    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient RelationshipPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    RelationshipPGImpl _tmp = new RelationshipPGImpl(this);
    if (relationshipBG != null) {
      _tmp.relationshipBG = (RelationshipBG) relationshipBG.copy(_tmp);
    }
    return _tmp;
  }

  public PropertyGroup copy() {
    try {
      return (PropertyGroup) clone();
    } catch (CloneNotSupportedException cnse) { return null;}
  }

  public Class getPrimaryClass() {
    return primaryClass;
  }
  public String getAssetGetMethod() {
    return assetGetter;
  }
  public String getAssetSetMethod() {
    return assetSetter;
  }

  private final static PropertyDescriptor properties[] = new PropertyDescriptor[2];
  static {
    try {
      properties[0]= new PropertyDescriptor("relationshipSchedule", RelationshipPG.class, "getRelationshipSchedule", null);
      properties[1]= new PropertyDescriptor("local", RelationshipPG.class, "getLocal", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(RelationshipPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements RelationshipPG, Cloneable, LockedPG
  {
    private transient Object theKey = null;
    _Locked(Object key) { 
      if (this.theKey == null) this.theKey = key;
    }  

    public _Locked() {}

    public PropertyGroup lock() { return this; }
    public PropertyGroup lock(Object o) { return this; }

    public NewPropertyGroup unlock(Object key) throws IllegalAccessException {
       if( theKey.equals(key) ) {
         return RelationshipPGImpl.this;
       } else {
         throw new IllegalAccessException("unlock: mismatched internal and provided keys!");
       }
    }

    public PropertyGroup copy() {
      try {
        return (PropertyGroup) clone();
      } catch (CloneNotSupportedException cnse) { return null;}
    }


    public Object clone() throws CloneNotSupportedException {
      RelationshipPGImpl _tmp = new RelationshipPGImpl(this);
      if (relationshipBG != null) {
        _tmp.relationshipBG = (RelationshipBG) relationshipBG.copy(_tmp);
      }
      return _tmp;
    }

    public boolean equals(Object object) { return RelationshipPGImpl.this.equals(object); }
    public org.cougaar.planning.ldm.plan.RelationshipSchedule getRelationshipSchedule() { return RelationshipPGImpl.this.getRelationshipSchedule(); }
    public boolean getLocal() { return RelationshipPGImpl.this.getLocal(); }
  public PGDelegate copy(PropertyGroup pg) {
    return RelationshipPGImpl.this.copy(pg);
  }
  public void readObject(java.io.ObjectInputStream in) {
    RelationshipPGImpl.this.readObject(in);
  }
  public void writeObject(java.io.ObjectOutputStream out) {
    RelationshipPGImpl.this.writeObject(out);
  }
  public void init(NewRelationshipPG pg, org.cougaar.planning.ldm.plan.HasRelationships hasRelationships) {
    RelationshipPGImpl.this.init(pg, hasRelationships);
  }
  public boolean isLocal() {
    return RelationshipPGImpl.this.isLocal();
  }
  public boolean isSelf() {
    return RelationshipPGImpl.this.isSelf();
  }
  public final boolean hasDataQuality() { return false; }
    public Class getPrimaryClass() {
      return primaryClass;
    }
    public String getAssetGetMethod() {
      return assetGetter;
    }
    public String getAssetSetMethod() {
      return assetSetter;
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
      return properties;
    }

    public Class getIntrospectionClass() {
      return RelationshipPGImpl.class;
    }

  }

}
