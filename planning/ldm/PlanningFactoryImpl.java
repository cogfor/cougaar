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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.FactoryException;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.AssetFactory;
import org.cougaar.planning.ldm.asset.EssentialAssetFactory;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupFactory;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.planning.ldm.plan.ClusterObjectFactoryImpl;

/**
 * Factory methods for all LDM objects.
 **/
class PlanningFactoryImpl
  extends ClusterObjectFactoryImpl
  implements Factory, PlanningFactory, ClusterObjectFactory
{

  /** map of propertyclassname to factorymethod **/
  private final Map propertyNames = new HashMap(89);

  /** map of assetname -> assetClass **/
  private final Map assetClasses = new HashMap(89);

  /** set of property(group) factory classes **/
  private final Set propertyFactories = new HashSet(11);

  /** set of assetFactories **/
  private final Set assetFactories = new HashSet(11);

  /**
   *   Constructor. Create a new instance of the Factory.
   * @param ldm LDM object so that Factory can provide convenience aliases to 
   * prototype cache, etc.
   **/
  public PlanningFactoryImpl(LDMServesPlugin ldm) {
    super(ldm, ldm.getMessageAddress());

    // add the asset factories
    addAssetFactory(new AssetFactory());
    addPropertyGroupFactory(new PropertyGroupFactory());
  }
    
  /** register a propertyfactory with us so that short (no package!) 
   * property group names may be used in createPropertyGroup(String).
   * Either a PropertyGroupFactory class or an instance of such may be passed in.
   **/
  public final void addPropertyGroupFactory(Object pf) {
    try {
      Class pfc;
      if (pf instanceof Class) {
        pfc = (Class)pf;
      } else {
        pfc = pf.getClass();
      }

      synchronized (propertyFactories) {
        if (propertyFactories.contains(pfc)) {
          return;
        } else {
          propertyFactories.add(pfc);
        }
      }

      Field f = pfc.getField("properties");
      String[][] properties = (String[][])f.get(pf);
      int l = properties.length;
      for (int i = 0; i<l; i++) {
        String fullname = properties[i][0];
        //Class pc = Class.forName(fullname);
        loadClass(fullname);
        String name = trimPackage(fullname);

        /*
         * Don't support explicitly creating PropertyGroupSchedules through createPropertyGroup.
         * Can create TimePhasedPropertyGroup through createPropertyGroup.
         * 
         * Schedules created implicitly by creating a TimePhasedPropertyGroup and then adding to
         * the Asset. Schedules created explicitly by using PropertyGroupFactory methods.
         */
        if (!name.equals("PropertyGroupSchedule")) {
          
          Method fm = pfc.getMethod(properties[i][1], null);
          
          Object old = propertyNames.put(name.intern(),fm);
          if (old != null) {
            System.err.println("Warning: PropertyGroupFactory "+pf+" overlaps with another propertyFactory at "+name);
          }
          propertyNames.put(fullname.intern(),fm);
        }
      }
    } catch (Exception e) {
      System.err.println("addPropertyGroupFactory of non-PropertyGroupFactory:");
      e.printStackTrace();
    }
  }

  /** @return true iff the factory parameter is already registered as a
   * propertygroup factory.
   **/
  public final boolean containsPropertyGroupFactory(Object pf) {
    Class pfc;
    if (pf instanceof Class) {
      pfc = (Class)pf;
    } else {
      pfc = pf.getClass();
    }
    synchronized (propertyFactories) {
      return propertyFactories.contains(pfc);
    }
  }

  private final Method findPropertyGroupFactoryMethod(String name) {
    return (Method) propertyNames.get(name);
  }

  /** register an assetfactory with us so that we can
   * (1) find an asset class from an asset name and (2)
   * can figure out which factory to use for a given 
   * asset class.
   **/
  public final void addAssetFactory(EssentialAssetFactory af) {
    try {
      // check for redundant add
      synchronized (assetFactories) {
        if (assetFactories.contains(af)) {
          return;
        } else {
          assetFactories.add(af);
        }
      }

      Class afc = af.getClass();
      Field f = afc.getField("assets");
      String[] assets = (String[]) f.get(af);
      int l = assets.length;
      for (int i = 0; i<l; i++) {
        String fullname = assets[i];
        Class ac = loadClass(fullname); 
        String name = trimPackage(fullname);
        Object old = assetClasses.put(name.intern(), ac);
        if (old != null) {
          System.err.println("Warning: AssetFactory "+af+" overlaps with another PropertyGroupFactory at "+name);
        }
        assetClasses.put(fullname.intern(), ac);
      }
    } catch (Exception e) {
      System.err.println("addAssetFactory of non-functional AssetFactory "+af);
      e.printStackTrace();
    }
  }

  public final boolean containsAssetFactory(Object f) {
    return assetFactories.contains(f);
  }


  private String trimPackage(String classname){
    int i = classname.lastIndexOf(".");
    if (i < 0) 
      return classname;
    else 
      return classname.substring(i+1);
  }

  private Class findAssetClass(String name) {
    return (Class) assetClasses.get(name);
  }

  /** Find a prototype Asset based on it's typeid description,
   * (e.g. "NSN/1234567890123") either by looking up an existing
   * object or by creating one of the appropriate type.
   *
   * Shorthand for LDMServesPlugin.getPrototype(aTypeName);
   **/
  public final Asset getPrototype(String aTypeName) {
    return ldm.getPrototype(aTypeName);
  }

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
  public final Asset createAsset(String classname) {
    if (classname == null) throw new IllegalArgumentException("Classname must be non-null");
    try {
      Class ac = findAssetClass(classname);
      if (ac == null) {
        throw new IllegalArgumentException("createAsset(String): \""+classname+"\" is not a known Asset class.  This may be due to a misloaded LDM domain.");
      }
      Asset asset = createAsset(ac);
      return asset;
    } catch (Exception e) {
      throw new FactoryException("Could not createAsset("+classname+"): "+e, e);
    }
  }

  /** Create a raw Asset instance for use by LDM Plugins
   * which are PrototypeProviders.
   * The asset created will have *no* propertygroups.
   * This *always* creates a prototype of the specific class.
   * most plugins want to call getPrototype(String typeid);
   *
   * @param assetClass an LDM Asset class.
   **/
  public final Asset createAsset(Class assetClass) {
    if (assetClass == null) throw new IllegalArgumentException("assetClass must be non-null");

    try {
      Asset asset = (Asset) assetClass.newInstance();
      asset.registerWithLDM(ldm);
      return asset;
    } catch (Exception e) {
      throw new FactoryException("Could not createAsset("+assetClass+"): "+e, e);
    }
  }

  /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * to the specified string.
   **/
  public final Asset createPrototype(String classname, String typeid) {
    if (classname == null) throw new IllegalArgumentException("classname must be non-null");
    //if (typeid == null) throw new IllegalArgumentException("typeid must be non-null");

    Asset proto = createAsset(classname);
    NewTypeIdentificationPG tip = (NewTypeIdentificationPG)proto.getTypeIdentificationPG();
    tip.setTypeIdentification(typeid);
    return proto;
  }

 /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * to the specified string.
   **/
  public final Asset createPrototype(Class assetclass, String typeid) {
    if (assetclass == null) throw new IllegalArgumentException("assetclass must be non-null");
    //if (typeid == null) throw new IllegalArgumentException("typeid must be non-null");

    Asset proto = createAsset(assetclass);
    NewTypeIdentificationPG tip = (NewTypeIdentificationPG)proto.getTypeIdentificationPG();
    tip.setTypeIdentification(typeid);
    return proto;
  }


  /** convenience routine for creating prototype assets.
   * does a createAsset followed by setting the TypeIdentification
   * and the nomenclature to the specified string.
   **/
  public final Asset createPrototype(String classname, String typeid, String nomen) {
    if (classname == null) throw new IllegalArgumentException("classname must be non-null");
    //if (typeid == null) throw new IllegalArgumentException("typeid must be non-null");

    Asset proto = createAsset(classname);
    NewTypeIdentificationPG tip = (NewTypeIdentificationPG)proto.getTypeIdentificationPG();
    tip.setTypeIdentification(typeid);
    tip.setNomenclature(nomen);
    return proto;
  }


  /** Create an instance of a prototypical asset.
   * This variation does <em>not</em> add an ItemIdentificationCode
   * to the constructed asset instance.  Without itemIDs,
   * multiple instances of a prototype will test as .equals(), and
   * can be confusing if they're added to the logplan.
   * Most users will find #createInstance(Asset, String) more convenient.
   **/
  public final Asset createInstance(Asset prototypeAsset) {
    Asset asset = prototypeAsset.createInstance();
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** Create an instance of a prototypical asset.
   * This variation does <em>not</em> add an ItemIdentificationCode
   * to the constructed asset instance.  Without itemIDs,
   * multiple instances of a prototype will test as .equals(), and
   * can be confusing if they're added to the logplan.
   * Most users will find #createInstance(String, String) more convenient.
   **/
  public final Asset createInstance(String prototypeAssetTypeId) {
    if (prototypeAssetTypeId == null) throw new IllegalArgumentException("prototypeAssetTypeId must be non-null");

    Asset proto = ldm.getPrototype(prototypeAssetTypeId);
    if (proto == null)
      throw new FactoryException("Could not find a prototype with TypeId = "+ 
                                 prototypeAssetTypeId);
    Asset asset =  proto.createInstance();
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** Create an instance of a prototypical asset, specifying an initial 
   * UniqueID for its itemIdentificationPG .
   **/
  public final Asset createInstance(Asset prototypeAsset, String uniqueId) {
    Asset asset = prototypeAsset.createInstance(uniqueId);
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** Create an instance of a prototypical asset, specifying an initial UniqueID 
   * for its itemIdentificationPG.
   **/
  public final Asset createInstance(String prototypeAssetTypeId, String uniqueId) {
    if (prototypeAssetTypeId == null) throw new IllegalArgumentException("prototypeAssetTypeId must be non-null");

    Asset proto = ldm.getPrototype(prototypeAssetTypeId);
    if (proto == null)
      throw new FactoryException("Could not find a prototype with TypeId = "+ 
                                 prototypeAssetTypeId);
    Asset asset = proto.createInstance(uniqueId);
    asset.registerWithLDM(ldm);
    return asset;
  }

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
  public final Asset copyInstance(Asset asset) {
    Asset copy = asset.copy();
    copy.registerWithLDM(ldm);
    return copy;
  }

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
  public final Asset cloneInstance(Asset asset) {
    Asset copy = asset.copy();
    // We need to reuse the original UID so we do this:
    copy.setUID(asset.getUID());
    copy.bindToLDM(ldm);
    // instead of:
    // copy.registerWithLDM(ldm);
    return copy;
  }

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  public final Asset createAggregate(Asset prototypeAsset, int quantity) {
    Asset asset = prototypeAsset.createAggregate(quantity);
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  public final Asset createAggregate(String prototypeAssetTypeId, int quantity) {
    if (prototypeAssetTypeId == null) throw new IllegalArgumentException("prototypeAssetTypeId must be non-null");

    Asset proto = ldm.getPrototype(prototypeAssetTypeId);
    if (proto == null)
      throw new FactoryException("Could not find a prototype with TypeId = "+ 
                                 prototypeAssetTypeId);
    Asset asset = proto.createAggregate(quantity);
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** Create an aggregate asset instance of a prototypical asset.
   **/
  public Asset createInstance(String prototypeAssetTypeId, int quantity) {
    if (prototypeAssetTypeId == null) throw new IllegalArgumentException("prototypeAssetTypeId must be non-null");

    Asset proto = ldm.getPrototype(prototypeAssetTypeId);
    if (proto == null)
      throw new FactoryException("Could not find a prototype with TypeId = "+ 
                                 prototypeAssetTypeId);
    Asset asset = proto.createAggregate(quantity);
    asset.registerWithLDM(ldm);
    return asset;
  }

  /** create a new property group, given a PropertyGroup name.
   * The name should not have any package prefix and should
   * be the cannonical name (not the implementation class name).
   **/
  public final PropertyGroup createPropertyGroup(String propertyName) {
    try {
      Method factoryMethod = findPropertyGroupFactoryMethod(propertyName);
      if (factoryMethod == null) 
        throw new IllegalArgumentException(propertyName+" is not a known PropertyGroup name");

      return (PropertyGroup) factoryMethod.invoke(null, null); // class/static method, no args
    } catch (Exception e) {
      throw new FactoryException("Could not createPropertyGroup("+propertyName+"): "+e, e);
    }
  }

  /** create a new property group, given a PropertyGroupGroup name.
   * The name should not have any package prefix and should
   * be the cannonical name (not the implementation class name).
   **/
  public final PropertyGroup createPropertyGroup(Class propertyClass) 
  {
    try {
      // we got the interface name, not the impl - find and run the 
      // correct property factory method.
      Field f = propertyClass.getField("factoryMethod");
      String fmname = (String) f.get(null);
      Field ff = propertyClass.getField("factoryClass");
      Class factory = (Class) ff.get(null);
      Method fm = factory.getMethod(fmname, null);
      return (PropertyGroup) fm.invoke(null, null);
    } catch (Exception e) {
      throw new FactoryException("Could not createPropertyGroup("+propertyClass+"): "+e, e);
    }
  }

  /** @return a copy of another property group **/
  public final PropertyGroup createPropertyGroup(PropertyGroup originalProperty) {
    return originalProperty.copy();
  }

  /** dummy for create(String) **/
  public Object create(String objectname) { return null; }
  /** dummy for create(Class) **/
  public Object create(Class objectclass) { return null; }
}
