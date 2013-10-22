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
/** Implementation of ItemIdentificationPG.
 *  @see ItemIdentificationPG
 *  @see NewItemIdentificationPG
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

public final class ItemIdentificationPGImpl extends java.beans.SimpleBeanInfo
  implements NewItemIdentificationPG, Cloneable
{
  public ItemIdentificationPGImpl() {
  }

  // Slots

  private String theItemIdentification;
  public String getItemIdentification(){ return theItemIdentification; }
  public void setItemIdentification(String item_identification) {
    theItemIdentification=item_identification;
  }
  private String theNomenclature;
  public String getNomenclature(){ return theNomenclature; }
  public void setNomenclature(String nomenclature) {
    theNomenclature=nomenclature;
  }
  private String theAlternateItemIdentification;
  public String getAlternateItemIdentification(){ return theAlternateItemIdentification; }
  public void setAlternateItemIdentification(String alternate_item_identification) {
    theAlternateItemIdentification=alternate_item_identification;
  }


  public ItemIdentificationPGImpl(ItemIdentificationPG original) {
    theItemIdentification = original.getItemIdentification();
    theNomenclature = original.getNomenclature();
    theAlternateItemIdentification = original.getAlternateItemIdentification();
  }

  public boolean equals(Object other) {

    if (!(other instanceof ItemIdentificationPG)) {
      return false;
    }

    ItemIdentificationPG otherItemIdentificationPG = (ItemIdentificationPG) other;

    if (getItemIdentification() == null) {
      if (otherItemIdentificationPG.getItemIdentification() != null) {
        return false;
      }
    } else if (!(getItemIdentification().equals(otherItemIdentificationPG.getItemIdentification()))) {
      return false;
    }

    if (getNomenclature() == null) {
      if (otherItemIdentificationPG.getNomenclature() != null) {
        return false;
      }
    } else if (!(getNomenclature().equals(otherItemIdentificationPG.getNomenclature()))) {
      return false;
    }

    if (getAlternateItemIdentification() == null) {
      if (otherItemIdentificationPG.getAlternateItemIdentification() != null) {
        return false;
      }
    } else if (!(getAlternateItemIdentification().equals(otherItemIdentificationPG.getAlternateItemIdentification()))) {
      return false;
    }

    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient ItemIdentificationPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new ItemIdentificationPGImpl(ItemIdentificationPGImpl.this);
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
      properties[0]= new PropertyDescriptor("item_identification", ItemIdentificationPG.class, "getItemIdentification", null);
      properties[1]= new PropertyDescriptor("nomenclature", ItemIdentificationPG.class, "getNomenclature", null);
      properties[2]= new PropertyDescriptor("alternate_item_identification", ItemIdentificationPG.class, "getAlternateItemIdentification", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(ItemIdentificationPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements ItemIdentificationPG, Cloneable, LockedPG
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
         return ItemIdentificationPGImpl.this;
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
      return new ItemIdentificationPGImpl(ItemIdentificationPGImpl.this);
    }

    public boolean equals(Object object) { return ItemIdentificationPGImpl.this.equals(object); }
    public String getItemIdentification() { return ItemIdentificationPGImpl.this.getItemIdentification(); }
    public String getNomenclature() { return ItemIdentificationPGImpl.this.getNomenclature(); }
    public String getAlternateItemIdentification() { return ItemIdentificationPGImpl.this.getAlternateItemIdentification(); }
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
      return ItemIdentificationPGImpl.class;
    }

  }

}
