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
/** Implementation of TypeIdentificationPG.
 *  @see TypeIdentificationPG
 *  @see NewTypeIdentificationPG
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

public final class TypeIdentificationPGImpl extends java.beans.SimpleBeanInfo
  implements NewTypeIdentificationPG, Cloneable
{
  public TypeIdentificationPGImpl() {
  }

  // Slots

  private String theTypeIdentification;
  public String getTypeIdentification(){ return theTypeIdentification; }
  public void setTypeIdentification(String type_identification) {
    theTypeIdentification=type_identification;
  }
  private String theNomenclature;
  public String getNomenclature(){ return theNomenclature; }
  public void setNomenclature(String nomenclature) {
    theNomenclature=nomenclature;
  }
  private String theAlternateTypeIdentification;
  public String getAlternateTypeIdentification(){ return theAlternateTypeIdentification; }
  public void setAlternateTypeIdentification(String alternate_type_identification) {
    theAlternateTypeIdentification=alternate_type_identification;
  }


  public TypeIdentificationPGImpl(TypeIdentificationPG original) {
    theTypeIdentification = original.getTypeIdentification();
    theNomenclature = original.getNomenclature();
    theAlternateTypeIdentification = original.getAlternateTypeIdentification();
  }

  public boolean equals(Object other) {

    if (!(other instanceof TypeIdentificationPG)) {
      return false;
    }

    TypeIdentificationPG otherTypeIdentificationPG = (TypeIdentificationPG) other;

    if (getTypeIdentification() == null) {
      if (otherTypeIdentificationPG.getTypeIdentification() != null) {
        return false;
      }
    } else if (!(getTypeIdentification().equals(otherTypeIdentificationPG.getTypeIdentification()))) {
      return false;
    }

    if (getNomenclature() == null) {
      if (otherTypeIdentificationPG.getNomenclature() != null) {
        return false;
      }
    } else if (!(getNomenclature().equals(otherTypeIdentificationPG.getNomenclature()))) {
      return false;
    }

    if (getAlternateTypeIdentification() == null) {
      if (otherTypeIdentificationPG.getAlternateTypeIdentification() != null) {
        return false;
      }
    } else if (!(getAlternateTypeIdentification().equals(otherTypeIdentificationPG.getAlternateTypeIdentification()))) {
      return false;
    }

    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient TypeIdentificationPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new TypeIdentificationPGImpl(TypeIdentificationPGImpl.this);
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

  private final static PropertyDescriptor properties[] = new PropertyDescriptor[3];
  static {
    try {
      properties[0]= new PropertyDescriptor("type_identification", TypeIdentificationPG.class, "getTypeIdentification", null);
      properties[1]= new PropertyDescriptor("nomenclature", TypeIdentificationPG.class, "getNomenclature", null);
      properties[2]= new PropertyDescriptor("alternate_type_identification", TypeIdentificationPG.class, "getAlternateTypeIdentification", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(TypeIdentificationPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements TypeIdentificationPG, Cloneable, LockedPG
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
         return TypeIdentificationPGImpl.this;
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
      return new TypeIdentificationPGImpl(TypeIdentificationPGImpl.this);
    }

    public boolean equals(Object object) { return TypeIdentificationPGImpl.this.equals(object); }
    public String getTypeIdentification() { return TypeIdentificationPGImpl.this.getTypeIdentification(); }
    public String getNomenclature() { return TypeIdentificationPGImpl.this.getNomenclature(); }
    public String getAlternateTypeIdentification() { return TypeIdentificationPGImpl.this.getAlternateTypeIdentification(); }
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
      return TypeIdentificationPGImpl.class;
    }

  }

}
