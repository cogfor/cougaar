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
/** AbstractFactory implementation for Properties.
 * Prevents clients from needing to know the implementation
 * class(es) of any of the properties.
 **/

package org.cougaar.planning.ldm.asset;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;



public class PropertyGroupFactory {
  // brand-new instance factory
  public static NewLocationSchedulePG newLocationSchedulePG() {
    return new LocationSchedulePGImpl();
  }
  // instance from prototype factory
  public static NewLocationSchedulePG newLocationSchedulePG(LocationSchedulePG prototype) {
    return new LocationSchedulePGImpl(prototype);
  }

  // brand-new instance factory
  public static NewCommunityPG newCommunityPG() {
    return new CommunityPGImpl();
  }
  // brand-new instance factory
  public static PropertyGroupSchedule newCommunityPGSchedule() {
    return new PropertyGroupSchedule(newCommunityPG());
  }
  // instance from prototype factory
  public static NewCommunityPG newCommunityPG(CommunityPG prototype) {
    return new CommunityPGImpl(prototype);
  }

  // instance from prototype factory
  public static PropertyGroupSchedule newCommunityPGSchedule(CommunityPG prototype) {
    return new PropertyGroupSchedule(newCommunityPG(prototype));
  }

  // instance from prototype schedule
  public static PropertyGroupSchedule newCommunityPGSchedule(PropertyGroupSchedule prototypeSchedule) {
    if (!prototypeSchedule.getPGClass().equals(CommunityPG.class)) {
      throw new IllegalArgumentException("newCommunityPGSchedule requires that getPGClass() on the PropertyGroupSchedule argument return CommunityPG.class");
    }
    return new PropertyGroupSchedule(prototypeSchedule);
  }

  // brand-new instance factory
  public static NewItemIdentificationPG newItemIdentificationPG() {
    return new ItemIdentificationPGImpl();
  }
  // instance from prototype factory
  public static NewItemIdentificationPG newItemIdentificationPG(ItemIdentificationPG prototype) {
    return new ItemIdentificationPGImpl(prototype);
  }

  // brand-new instance factory
  public static NewTypeIdentificationPG newTypeIdentificationPG() {
    return new TypeIdentificationPGImpl();
  }
  // instance from prototype factory
  public static NewTypeIdentificationPG newTypeIdentificationPG(TypeIdentificationPG prototype) {
    return new TypeIdentificationPGImpl(prototype);
  }

  // brand-new instance factory
  public static NewRelationshipPG newRelationshipPG() {
    return new RelationshipPGImpl();
  }
  // instance from prototype factory
  public static NewRelationshipPG newRelationshipPG(RelationshipPG prototype) {
    return new RelationshipPGImpl(prototype);
  }

  // brand-new instance factory
  public static NewEntityPG newEntityPG() {
    return new EntityPGImpl();
  }
  // instance from prototype factory
  public static NewEntityPG newEntityPG(EntityPG prototype) {
    return new EntityPGImpl(prototype);
  }

  // brand-new instance factory
  public static NewClusterPG newClusterPG() {
    return new ClusterPGImpl();
  }
  // instance from prototype factory
  public static NewClusterPG newClusterPG(ClusterPG prototype) {
    return new ClusterPGImpl(prototype);
  }

  /** Abstract introspection information.
   * Tuples are {<classname>, <factorymethodname>}
   * return value of <factorymethodname> is <classname>.
   * <factorymethodname> takes zero or one (prototype) argument.
   **/
  public static String properties[][]={
    {"org.cougaar.planning.ldm.asset.LocationSchedulePG", "newLocationSchedulePG"},
    {"org.cougaar.planning.ldm.asset.CommunityPG", "newCommunityPG"},
    {"org.cougaar.planning.ldm.asset.PropertyGroupSchedule", "newCommunityPGSchedule"},
    {"org.cougaar.planning.ldm.asset.ItemIdentificationPG", "newItemIdentificationPG"},
    {"org.cougaar.planning.ldm.asset.TypeIdentificationPG", "newTypeIdentificationPG"},
    {"org.cougaar.planning.ldm.asset.RelationshipPG", "newRelationshipPG"},
    {"org.cougaar.planning.ldm.asset.EntityPG", "newEntityPG"},
    {"org.cougaar.planning.ldm.asset.ClusterPG", "newClusterPG"},
  };
}
