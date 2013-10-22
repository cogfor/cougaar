/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
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

package org.cougaar.planning.ldm;

import org.cougaar.core.domain.Factory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.EssentialAssetFactory;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;

/**
 * Factory methods for all LDM objects.
 **/

public interface PlanningFactory
extends Factory, ClusterObjectFactory
{
  /** register a propertyfactory with us so that short (no package!) 
   * property group names may be used in createPropertyGroup(String).
   * Either a PropertyGroupFactory class or an instance of such may be passed in.
   **/
  void addPropertyGroupFactory(Object pf);

  /** @return true iff the factory parameter is already registered as a
   * propertygroup factory.
   **/
  boolean containsPropertyGroupFactory(Object pf);

  /** register an assetfactory with us so that we can
   * (1) find an asset class from an asset name and (2)
   * can figure out which factory to use for a given 
   * asset class.
   **/
  void addAssetFactory(EssentialAssetFactory af);

  boolean containsAssetFactory(Object f);

  /** Find a prototype Asset based on it's typeid description,
   * (e.g. "NSN/1234567890123") either by looking up an existing
   * object or by creating one of the appropriate type.
   *
   * Shorthand for LDMServesPlugin.getPrototype(aTypeName);
   **/
  Asset getPrototype(String aTypeName);

  /** Create a raw Asset instance for use by LDM Plugins
   * which are PrototypeProviders.
   * The asset created will have *no* propertygroups.
   * This *always* creates a prototype of the specific class.
   * most plugins want to call getPrototype(String typeid);
   *
   * @param classname One of the defined LDM class names.  This must
   * be the actual class name without the package path.  For example,
   * "Container" is correct, "org.cougaar.planning.ldm.asset.Container" is not.
   **/
  Asset createAsset(String classname);

  /** Create a raw Asset instance for use by LDM Plugins
   * which are PrototypeProviders.
   * The asset created will have *no* propertygroups.
   * This *always* creates a prototype of the specific class.
   * most plugins want to call getPrototype(String typeid);
   *
   * @param assetClass an LDM Asset class.
   **/
  Asset createAsset(Class assetClass);

  /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * to the specified string.
   **/
  Asset createPrototype(String classname, String typeid);

  /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * to the specified string.
   **/
  Asset createPrototype(Class assetclass, String typeid);

  /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * and the nomenclature to the specified string.
   **/
  Asset createPrototype(String classname, String typeid, String nomen);

  /** Create an instance of a prototypical asset.
   * This variation does <em>not</em> add an ItemIdentificationCode
   * to the constructed asset instance.  Without itemIDs,
   * multiple instances of a prototype will test as .equals(), and
   * can be confusing if they're added to the logplan.
   * Most users will find #createInstance(Asset, String) more convenient.
   **/
  Asset createInstance(Asset prototypeAsset);

  /** Create an instance of a prototypical asset.
   * This variation does <em>not</em> add an ItemIdentificationCode
   * to the constructed asset instance.  Without itemIDs,
   * multiple instances of a prototype will test as .equals(), and
   * can be confusing if they're added to the logplan.
   * Most users will find #createInstance(String, String) more convenient.
   **/
  Asset createInstance(String prototypeAssetTypeId);

  /** Create an instance of a prototypical asset, specifying an initial 
   * UniqueID for its itemIdentificationPG .
   **/
  Asset createInstance(Asset prototypeAsset, String uniqueId);

  /** Create an instance of a prototypical asset, specifying an initial UniqueID 
   * for its itemIdentificationPG.
   **/
  Asset createInstance(String prototypeAssetTypeId, String uniqueId);

  /** Make a copy of an instance.  The result will be a shallow copy
   * of the original - that is, it will share most PropertyGroups with the
   * original instance.  The differences will be that the copy's PGs will
   * be locked and the copy will have a different UID.
   * The copy will truly be a different asset which happens to (initially) have
   * identical propertygroups.
   * This method should be used to create new assets which are very much
   * like another instance.  The use of this method is a less-desirable alternative
   * to creating a new instance of your original's prototype and then adding back
   * any extra properties.  This is less desirable because it doesn't allow the
   * LDM to participate in the construction of the copy.
   **/
  Asset copyInstance(Asset asset);

  /** make an evil twin of an instance.  The result will be a shallow copy of the 
   * original (as in copyInstance), with locked PropertyGroups. The copy
   * <em> will </em> have the same UID as the original, so will, in a systems sense
   * actually be the same asset.  It could be very bad for multiple clones of an
   * asset to show up in someone's Blackboard.  
   * This method should be used when subsetting the capabilities of an asset
   * for some other consumer.  Eg. when you want to allow a client to use just one
   * capability of your organization.
   * Note: This method name may change.
   **/
  Asset cloneInstance(Asset asset);

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  Asset createAggregate(Asset prototypeAsset, int quantity);

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  Asset createAggregate(String prototypeAssetTypeId, int quantity);

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  Asset createInstance(String prototypeAssetTypeId, int quantity);

  /** create a new property group, given a PropertyGroup name.
   * The name should not have any package prefix and should
   * be the cannonical name (not the implementation class name).
   **/
  PropertyGroup createPropertyGroup(String propertyName);

  /** create a new property group, given a PropertyGroupGroup name.
   * The name should not have any package prefix and should
   * be the cannonical name (not the implementation class name).
   **/
  PropertyGroup createPropertyGroup(Class propertyClass);

  /** @return a copy of another property group **/
  PropertyGroup createPropertyGroup(PropertyGroup originalProperty);

  /** dummy for create(String) **/
  Object create(String objectname);
  /** dummy for create(Class) **/
  Object create(Class objectclass);
}
