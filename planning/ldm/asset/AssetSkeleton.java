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
/** Abstract Asset Skeleton implementation
 * Implements default property getters, and additional property
 * lists.
 * Intended to be extended by org.cougaar.planning.ldm.asset.Asset
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;


import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public abstract class AssetSkeleton extends org.cougaar.planning.ldm.asset.AssetSkeletonBase {

  protected AssetSkeleton() {}

  protected AssetSkeleton(AssetSkeleton prototype) {
    super(prototype);
  }

  /**                 Default PG accessors               **/

  /** Search additional properties for a LocationSchedulePG instance.
   * @return instance of LocationSchedulePG or null.
   **/
  public LocationSchedulePG getLocationSchedulePG()
  {
    LocationSchedulePG _tmp = (LocationSchedulePG) resolvePG(LocationSchedulePG.class);
    return (_tmp==LocationSchedulePG.nullPG)?null:_tmp;
  }

  /** Test for existence of a LocationSchedulePG
   **/
  public boolean hasLocationSchedulePG() {
    return (getLocationSchedulePG() != null);
  }

  /** Set the LocationSchedulePG property.
   * The default implementation will create a new LocationSchedulePG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setLocationSchedulePG(PropertyGroup aLocationSchedulePG) {
    if (aLocationSchedulePG == null) {
      removeOtherPropertyGroup(LocationSchedulePG.class);
    } else {
      addOtherPropertyGroup(aLocationSchedulePG);
    }
  }

  /** Search additional properties for a CommunityPG instance.
   * @return instance of CommunityPG or null.
   **/
  public CommunityPG getCommunityPG(long time)
  {
    CommunityPG _tmp = (CommunityPG) resolvePG(CommunityPG.class, time);
    return (_tmp==CommunityPG.nullPG)?null:_tmp;
  }

  public CommunityPG getCommunityPG()
  {
    PropertyGroupSchedule pgSchedule = getCommunityPGSchedule();
    if (pgSchedule != null) {
      return (CommunityPG) pgSchedule.getDefault();
    } else {
      return null;
    }
  }

  /** Test for existence of a default CommunityPG
   **/
  public boolean hasCommunityPG() {
    return (getCommunityPG() != null);
  }

  /** Test for existence of a CommunityPG at a specific time
   **/
  public boolean hasCommunityPG(long time) {
    return (getCommunityPG(time) != null);
  }

  /** Set the CommunityPG property.
   * The default implementation will create a new CommunityPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setCommunityPG(PropertyGroup aCommunityPG) {
    if (aCommunityPG == null) {
      removeOtherPropertyGroup(CommunityPG.class);
    } else {
      addOtherPropertyGroup(aCommunityPG);
    }
  }

  public PropertyGroupSchedule getCommunityPGSchedule()
  {
    return searchForPropertyGroupSchedule(CommunityPG.class);
  }

  public void setCommunityPGSchedule(PropertyGroupSchedule schedule) {
    removeOtherPropertyGroup(CommunityPG.class);
    if (schedule != null) {
      addOtherPropertyGroupSchedule(schedule);
    }
  }

  /** Search additional properties for a ItemIdentificationPG instance.
   * @return instance of ItemIdentificationPG or null.
   **/
  public ItemIdentificationPG getItemIdentificationPG()
  {
    ItemIdentificationPG _tmp = (ItemIdentificationPG) resolvePG(ItemIdentificationPG.class);
    return (_tmp==ItemIdentificationPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a ItemIdentificationPG
   **/
  public boolean hasItemIdentificationPG() {
    return (getItemIdentificationPG() != null);
  }

  /** Set the ItemIdentificationPG property.
   * The default implementation will create a new ItemIdentificationPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setItemIdentificationPG(PropertyGroup aItemIdentificationPG) {
    if (aItemIdentificationPG == null) {
      removeOtherPropertyGroup(ItemIdentificationPG.class);
    } else {
      addOtherPropertyGroup(aItemIdentificationPG);
    }
  }

  /** Search additional properties for a TypeIdentificationPG instance.
   * @return instance of TypeIdentificationPG or null.
   **/
  public TypeIdentificationPG getTypeIdentificationPG()
  {
    TypeIdentificationPG _tmp = (TypeIdentificationPG) resolvePG(TypeIdentificationPG.class);
    return (_tmp==TypeIdentificationPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a TypeIdentificationPG
   **/
  public boolean hasTypeIdentificationPG() {
    return (getTypeIdentificationPG() != null);
  }

  /** Set the TypeIdentificationPG property.
   * The default implementation will create a new TypeIdentificationPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setTypeIdentificationPG(PropertyGroup aTypeIdentificationPG) {
    if (aTypeIdentificationPG == null) {
      removeOtherPropertyGroup(TypeIdentificationPG.class);
    } else {
      addOtherPropertyGroup(aTypeIdentificationPG);
    }
  }

  /** Search additional properties for a RelationshipPG instance.
   * @return instance of RelationshipPG or null.
   **/
  public RelationshipPG getRelationshipPG()
  {
    RelationshipPG _tmp = (RelationshipPG) resolvePG(RelationshipPG.class);
    return (_tmp==RelationshipPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a RelationshipPG
   **/
  public boolean hasRelationshipPG() {
    return (getRelationshipPG() != null);
  }

  /** Set the RelationshipPG property.
   * The default implementation will create a new RelationshipPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setRelationshipPG(PropertyGroup aRelationshipPG) {
    if (aRelationshipPG == null) {
      removeOtherPropertyGroup(RelationshipPG.class);
    } else {
      addOtherPropertyGroup(aRelationshipPG);
    }
  }

  /** Search additional properties for a EntityPG instance.
   * @return instance of EntityPG or null.
   **/
  public EntityPG getEntityPG()
  {
    EntityPG _tmp = (EntityPG) resolvePG(EntityPG.class);
    return (_tmp==EntityPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a EntityPG
   **/
  public boolean hasEntityPG() {
    return (getEntityPG() != null);
  }

  /** Set the EntityPG property.
   * The default implementation will create a new EntityPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setEntityPG(PropertyGroup aEntityPG) {
    if (aEntityPG == null) {
      removeOtherPropertyGroup(EntityPG.class);
    } else {
      addOtherPropertyGroup(aEntityPG);
    }
  }

  /** Search additional properties for a ClusterPG instance.
   * @return instance of ClusterPG or null.
   **/
  public ClusterPG getClusterPG()
  {
    ClusterPG _tmp = (ClusterPG) resolvePG(ClusterPG.class);
    return (_tmp==ClusterPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a ClusterPG
   **/
  public boolean hasClusterPG() {
    return (getClusterPG() != null);
  }

  /** Set the ClusterPG property.
   * The default implementation will create a new ClusterPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setClusterPG(PropertyGroup aClusterPG) {
    if (aClusterPG == null) {
      removeOtherPropertyGroup(ClusterPG.class);
    } else {
      addOtherPropertyGroup(aClusterPG);
    }
  }

}
