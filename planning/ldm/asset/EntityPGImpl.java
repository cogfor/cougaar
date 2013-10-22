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
/** Implementation of EntityPG.
 *  @see EntityPG
 *  @see NewEntityPG
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public final class EntityPGImpl extends java.beans.SimpleBeanInfo
  implements NewEntityPG, Cloneable
{
  public EntityPGImpl() {
  }

  // Slots

  private Collection theRoles = new ArrayList();
  public Collection getRoles(){ return theRoles; }
  public boolean inRoles(Role _element) {
    return (theRoles==null)?false:(theRoles.contains(_element));
  }
  public Role[] getRolesAsArray() {
    if (theRoles == null) return new Role[0];
    int l = theRoles.size();
    Role[] v = new Role[l];
    int i=0;
    for (Iterator n=theRoles.iterator(); n.hasNext(); ) {
      v[i]=(Role) n.next();
      i++;
    }
    return v;
  }
  public Role getIndexedRoles(int _index) {
    if (theRoles == null) return null;
    for (Iterator _i = theRoles.iterator(); _i.hasNext();) {
      Role _e = (Role) _i.next();
      if (_index == 0) return _e;
      _index--;
    }
    return null;
  }
  public void setRoles(Collection roles) {
    theRoles=roles;
  }
  public void clearRoles() {
    theRoles.clear();
  }
  public boolean removeFromRoles(Role _element) {
    return theRoles.remove(_element);
  }
  public boolean addToRoles(Role _element) {
    return theRoles.add(_element);
  }


  public EntityPGImpl(EntityPG original) {
    theRoles = original.getRoles();
  }

  public boolean equals(Object other) {

    if (!(other instanceof EntityPG)) {
      return false;
    }

    EntityPG otherEntityPG = (EntityPG) other;

    if (getRoles() == null) {
      if (otherEntityPG.getRoles() != null) {
        return false;
      }
    } else if (!(getRoles().equals(otherEntityPG.getRoles()))) {
      return false;
    }

    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient EntityPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new EntityPGImpl(EntityPGImpl.this);
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

  private final static PropertyDescriptor properties[] = new PropertyDescriptor[1];
  static {
    try {
      properties[0]= new IndexedPropertyDescriptor("roles", EntityPG.class, "getRolesAsArray", null, "getIndexedRoles", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(EntityPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements EntityPG, Cloneable, LockedPG
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
         return EntityPGImpl.this;
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
      return new EntityPGImpl(EntityPGImpl.this);
    }

    public boolean equals(Object object) { return EntityPGImpl.this.equals(object); }
    public Collection getRoles() { return EntityPGImpl.this.getRoles(); }
  public boolean inRoles(Role _element) {
    return EntityPGImpl.this.inRoles(_element);
  }
  public Role[] getRolesAsArray() {
    return EntityPGImpl.this.getRolesAsArray();
  }
  public Role getIndexedRoles(int _index) {
    return EntityPGImpl.this.getIndexedRoles(_index);
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
      return EntityPGImpl.class;
    }

  }

}
