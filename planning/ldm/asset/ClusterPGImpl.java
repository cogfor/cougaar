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
/** Implementation of ClusterPG.
 *  @see ClusterPG
 *  @see NewClusterPG
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

public final class ClusterPGImpl extends java.beans.SimpleBeanInfo
  implements NewClusterPG, Cloneable
{
  public ClusterPGImpl() {
  }

  // Slots

  private org.cougaar.core.mts.MessageAddress theMessageAddress;
  public org.cougaar.core.mts.MessageAddress getMessageAddress(){ return theMessageAddress; }
  public void setMessageAddress(org.cougaar.core.mts.MessageAddress message_address) {
    theMessageAddress=message_address;
  }
  private Predictor thePredictor;
  public Predictor getPredictor(){ return thePredictor; }
  public void setPredictor(Predictor predictor) {
    thePredictor=predictor;
  }


  public ClusterPGImpl(ClusterPG original) {
    theMessageAddress = original.getMessageAddress();
    thePredictor = original.getPredictor();
  }

  public boolean equals(Object other) {

    if (!(other instanceof ClusterPG)) {
      return false;
    }

    ClusterPG otherClusterPG = (ClusterPG) other;

    if (getMessageAddress() == null) {
      if (otherClusterPG.getMessageAddress() != null) {
        return false;
      }
    } else if (!(getMessageAddress().equals(otherClusterPG.getMessageAddress()))) {
      return false;
    }

    if (getPredictor() == null) {
      if (otherClusterPG.getPredictor() != null) {
        return false;
      }
    } else if (!(getPredictor().equals(otherClusterPG.getPredictor()))) {
      return false;
    }

    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient ClusterPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new ClusterPGImpl(ClusterPGImpl.this);
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
      properties[0]= new PropertyDescriptor("message_address", ClusterPG.class, "getMessageAddress", null);
      properties[1]= new PropertyDescriptor("predictor", ClusterPG.class, "getPredictor", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(ClusterPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements ClusterPG, Cloneable, LockedPG
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
         return ClusterPGImpl.this;
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
      return new ClusterPGImpl(ClusterPGImpl.this);
    }

    public boolean equals(Object object) { return ClusterPGImpl.this.equals(object); }
    public org.cougaar.core.mts.MessageAddress getMessageAddress() { return ClusterPGImpl.this.getMessageAddress(); }
    public Predictor getPredictor() { return ClusterPGImpl.this.getPredictor(); }
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
      return ClusterPGImpl.class;
    }

  }

}
