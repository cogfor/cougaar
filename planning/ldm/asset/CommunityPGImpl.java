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
/** Implementation of CommunityPG.
 *  @see CommunityPG
 *  @see NewCommunityPG
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.TimeSpan;
import java.util.*;



import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public final class CommunityPGImpl extends java.beans.SimpleBeanInfo
  implements NewCommunityPG, Cloneable
{
  public CommunityPGImpl() {
  }

//NewTimeSpan implementation
  private long theStartTime = TimeSpan.MIN_VALUE;
  public long getStartTime() {
    return theStartTime;
  }

  private long theEndTime = TimeSpan.MAX_VALUE;
  public long getEndTime() {
    return theEndTime;
  }

  public void setTimeSpan(long startTime, long endTime) {
    if ((startTime >= MIN_VALUE) && 
        (endTime <= MAX_VALUE) &&
        (endTime >= startTime + EPSILON)) {
      theStartTime = startTime;
      theEndTime = endTime;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public void setTimeSpan(TimeSpan timeSpan) {
    setTimeSpan(timeSpan.getStartTime(), timeSpan.getEndTime());
  }

  // Slots

  private Collection theCommunities = new ArrayList();
  public Collection getCommunities(){ return theCommunities; }
  public boolean inCommunities(String _element) {
    return (theCommunities==null)?false:(theCommunities.contains(_element));
  }
  public String[] getCommunitiesAsArray() {
    if (theCommunities == null) return new String[0];
    int l = theCommunities.size();
    String[] v = new String[l];
    int i=0;
    for (Iterator n=theCommunities.iterator(); n.hasNext(); ) {
      v[i]=(String) n.next();
      i++;
    }
    return v;
  }
  public String getIndexedCommunities(int _index) {
    if (theCommunities == null) return null;
    for (Iterator _i = theCommunities.iterator(); _i.hasNext();) {
      String _e = (String) _i.next();
      if (_index == 0) return _e;
      _index--;
    }
    return null;
  }
  public void setCommunities(Collection communities) {
    theCommunities=communities;
  }
  public void clearCommunities() {
    theCommunities.clear();
  }
  public boolean removeFromCommunities(String _element) {
    return theCommunities.remove(_element);
  }
  public boolean addToCommunities(String _element) {
    return theCommunities.add(_element);
  }


  public CommunityPGImpl(CommunityPG original) {
    setTimeSpan(original.getStartTime(), original.getEndTime());
    theCommunities = original.getCommunities();
  }

  public boolean equals(Object other) {

    if (!(other instanceof CommunityPG)) {
      return false;
    }

    CommunityPG otherCommunityPG = (CommunityPG) other;

    if (getCommunities() == null) {
      if (otherCommunityPG.getCommunities() != null) {
        return false;
      }
    } else if (!(getCommunities().equals(otherCommunityPG.getCommunities()))) {
      return false;
    }

    return true;
  }

  public final boolean hasDataQuality() { return false; }

  private transient CommunityPG _locked = null;
  public PropertyGroup lock(Object key) {
    if (_locked == null)_locked = new _Locked(key);
    return _locked; }
  public PropertyGroup lock() { return lock(null); }
  public NewPropertyGroup unlock(Object key) { return this; }

  public Object clone() throws CloneNotSupportedException {
    return new CommunityPGImpl(CommunityPGImpl.this);
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
      properties[0]= new IndexedPropertyDescriptor("communities", CommunityPG.class, "getCommunitiesAsArray", null, "getIndexedCommunities", null);
      properties[1]= new PropertyDescriptor("start_time", CommunityPG.class, "getStartTime", null);
      properties[2]= new PropertyDescriptor("end_time", CommunityPG.class, "getEndTime", null);
    } catch (Exception e) { 
      org.cougaar.util.log.Logging.getLogger(CommunityPG.class).error("Caught exception",e);
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }
  private final class _Locked extends java.beans.SimpleBeanInfo
    implements CommunityPG, Cloneable, LockedPG
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
         return CommunityPGImpl.this;
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
      return new CommunityPGImpl(CommunityPGImpl.this);
    }

    public long getStartTime() { return CommunityPGImpl.this.getStartTime(); }
    public long getEndTime() { return CommunityPGImpl.this.getEndTime(); }
    public boolean equals(Object object) { return CommunityPGImpl.this.equals(object); }
    public Collection getCommunities() { return CommunityPGImpl.this.getCommunities(); }
  public boolean inCommunities(String _element) {
    return CommunityPGImpl.this.inCommunities(_element);
  }
  public String[] getCommunitiesAsArray() {
    return CommunityPGImpl.this.getCommunitiesAsArray();
  }
  public String getIndexedCommunities(int _index) {
    return CommunityPGImpl.this.getIndexedCommunities(_index);
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
      return CommunityPGImpl.class;
    }

  }

}
