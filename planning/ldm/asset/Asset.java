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

/* 
 * This file is hand generated!  Don't be fooled by the fact that most other
 * asset-related classes are machine generated.
 */

package org.cougaar.planning.ldm.asset;

import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.agent.ClusterContextTable;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.LDMContextTable;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.RoleScheduleImpl;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;
import org.cougaar.util.ToStringMemo;
import org.cougaar.util.PropertyParser;

/**
 * Base class for all instantiable assets.
 *
 * @property org.cougaar.planning.ldm.asset.Asset.regeneratePrototypeCache If set to true
 * (the default), the prototypeCache will be regenerated as much as possible
 * from persistence information on rehydration.
 * @property org.cougaar.planning.ldm.asset.Asset.sendPrototypeAsID If set to false (the default)
 * then when an Asset is sent through a messaging system, we will send the Asset's prototype
 * as a real object if possible.  When set to true, the prototype will be sent merely as the
 * TypeIdentification string.  This latter mode has the effect of allowing the "shape" of an
 * asset to shift as it traverses the messaging system so that agents may choose to know exactly which
 * prototypical parameters are required.
 **/

public class Asset extends org.cougaar.planning.ldm.asset.AssetSkeleton
  implements Cloneable, UniqueObject, Publishable 
{
  public static final boolean regeneratePrototypeCacheP = 
    PropertyParser.getBoolean("org.cougaar.planning.ldm.asset.Asset.regeneratePrototypeCache", true);

  public static final boolean sendPrototypeAsIDP = 
    PropertyParser.getBoolean("org.cougaar.planning.ldm.asset.Asset.sendPrototypeAsID", false);

  static final long serialVersionUID = -7188316484839955973L;

  private transient RoleSchedule roleschedule;

  private static final String AGGREGATE_TYPE_ID = "AggregateAsset";
  
  private static final Logger log = Logging.getLogger(Asset.class);

  public Asset() {
    myPrototype = null;         // no prototype, by default

    myItemIdentificationPG = PropertyGroupFactory.newItemIdentificationPG();
    myTypeIdentificationPG = PropertyGroupFactory.newTypeIdentificationPG();
    initRoleSchedule();
  }


  protected Asset(Asset prototype) {
    super(prototype);
    // my prototype is who we're based on
    myPrototype = prototype;
    // I always get my own item ID which starts out as a copy of me.
    myItemIdentificationPG=(ItemIdentificationPG)prototype.getItemIdentificationPG().copy();
    // instead, lets share the itemID, but make sure it is locked.
    //myItemIdentificationPG=(ItemIdentificationPG)prototype.getItemIdentificationPG().lock();
    // share the typeID with proto
    myTypeIdentificationPG=prototype.getTypeIdentificationPG();
    // new roleschedule
    initRoleSchedule();
  }

  /** Create an instance of a prototype asset **/
  public Asset createInstance() {
    return new Asset(this);
  }

  /** Create an instance of a prototype asset, giving the new instance
   * an itemid of id.
   **/
  public final Asset createInstance(String id) {
    Asset a = createInstance();
    ((NewItemIdentificationPG)a.getItemIdentificationPG()).setItemIdentification(id);
    return a;
  }

  private UID uid= null;
  public UID getUID() { return uid; }
  public void setUID(UID uid) { this.uid = uid; }

  /** return all the properties as a new vector */
  public Vector fetchAllProperties() {
    Vector v = new Vector();
    fillAllPropertyGroups(v);
    return v;
  }

  /** return all the properties as a new vector */
  public Vector fetchAllProperties(long time) {
    Vector v = new Vector();
    fillAllPropertyGroups(v, time);
    return v;
  }

  protected void fillAllPropertyGroups(Vector v) {
    // stick ours on the front - all others add to the end.
    v.addElement(myItemIdentificationPG);
    v.addElement(myTypeIdentificationPG);
    super.fillAllPropertyGroups(v);
  }

  protected void fillAllPropertyGroups(Vector v, long time) {
    // stick ours on the front - all others add to the end.
    v.addElement(myItemIdentificationPG);
    v.addElement(myTypeIdentificationPG);
    super.fillAllPropertyGroups(v, time);
  }


  /** For internal use only - use Factory.copyInstance() instead.
   * extended
   **/
  public Asset copy() {
    try {
      return (Asset) clone();
    } catch (CloneNotSupportedException cnse) {
      cnse.printStackTrace();
      return null;
    }
  }

  /** For internal use only - use Factory.copyInstance() instead.
   * extended
   **/
  public Object clone() throws CloneNotSupportedException {
    Asset a = instanceForCopy();
    
    // bind the new asset to our LDM
    a.bindToLDM(_ldm.getLDM()); // use the real LDM if we have a delegator

    a.privatelySetPrototype(getPrototype());
    // set the itemID to a copy of the original
    a.setItemIdentificationPG(getItemIdentificationPG().copy());
    // share the typeid
    a.setTypeIdentificationPG(getTypeIdentificationPG().lock());
    // share the uid (for now, at least!)
    a.setUID(getUID());

    // handle assetskeleton params immediately
    for (Enumeration ops = getOtherProperties(); ops.hasMoreElements();) {
      Object p = ops.nextElement();
      
      if (p instanceof PropertyGroup) {
        a.addOtherPropertyGroup(((PropertyGroup) p).lock());
      } else {
        a.addOtherPropertyGroupSchedule((PropertyGroupSchedule) ((PropertyGroupSchedule) p).clone());
      }
        
    }

    return a;
  }

  /** creates an instance of the the right class, suitable for filling
   * with properties by clone() methods.
   *
   **/

  public Asset instanceForCopy() {
    //return new Asset();
    try {
      return (Asset) this.getClass().newInstance();
    } catch (Exception e) {
      return null;
    }
  }

  // myPrototype used to default property values for subs
  // so we don't have to copy links or property groups.
  protected transient Asset myPrototype;

  public Asset getPrototype() {
    return myPrototype;
  }

  public Asset setPrototype(Asset arg_Prototype) {
    throw new RuntimeException("It is illegal to set the asset prototype explicitly");
    /*
    myPrototype= arg_Prototype;
    return arg_Prototype;
    */
  }

  /** used internally **/
  void privatelySetPrototype(Asset arg) {
    myPrototype= arg;
  }
    

  private transient ItemIdentificationPG myItemIdentificationPG;

  public ItemIdentificationPG getItemIdentificationPG() {
    return myItemIdentificationPG;
  }

  public void setItemIdentificationPG(PropertyGroup arg_ItemIdentificationPG) {
    if (!(arg_ItemIdentificationPG instanceof ItemIdentificationPG))
      throw new IllegalArgumentException("setItemIdentificationPG requires a ItemIdentificationPG argument.");
    myItemIdentificationPG= (ItemIdentificationPG) arg_ItemIdentificationPG;
  }

  private transient TypeIdentificationPG myTypeIdentificationPG;

  public TypeIdentificationPG getTypeIdentificationPG() {
    return myTypeIdentificationPG;
  }

  public void setTypeIdentificationPG(PropertyGroup arg_TypeIdentificationPG) {
    if (!(arg_TypeIdentificationPG instanceof TypeIdentificationPG))
      throw new IllegalArgumentException("setTypeIdentificationPG requires a TypeIdentificationPG argument.");
    myTypeIdentificationPG= (TypeIdentificationPG) arg_TypeIdentificationPG;
  }

  // for allocation/roleschedule stuff
  
  /** @return RoleSchedule - the RoleSchedule object associated with this Asset
    * Note that the old Enumeration getRoleSchedule() is no longer used in asset
    * use asset.getRoleSchedule().getRoleSchedule for the enumeration of the roleschedule
    **/
  public RoleSchedule getRoleSchedule() {
    org.cougaar.core.blackboard.Blackboard.getTracker().checkAccess(this,"getRoleSchedule");
    return roleschedule;
  }

  //method should ONLY be called by the constructor and serialization
  private void initRoleSchedule() {
    roleschedule = new RoleScheduleImpl(this);
  }
  
  //end of disposition container / roleschedule stuff

  // aggregate creation support
  public Asset createAggregate(int quantity) {
    AggregateAsset aa = new AggregateAsset();
    aa.setAsset(this);
    aa.setQuantity(quantity);
    NewTypeIdentificationPG tip = PropertyGroupFactory.newTypeIdentificationPG();
    tip.setTypeIdentification(AGGREGATE_TYPE_ID);
    tip.setNomenclature(AGGREGATE_TYPE_ID+" aggregating quantity " + quantity + " of "+this);
    aa.setTypeIdentificationPG(tip);
    return aa;
  }

  
  // compile-compatibility with org.cougaar.planning.ldm.plan.asset
  public Enumeration getCapabilities() { return null; }
  public String getName() { return null; }


  public PropertyGroupSchedule searchForPropertyGroupSchedule(Class c) {
    return super.searchForPropertyGroupSchedule(c);
  }



  // Keep track of the enclosing LDM so that we can resolve
  // late bindings and such.
  private transient LDMServesPlugin _ldm = null;
  
  /** Called by readObject and factory to tell an asset where it
   * is resident to allow such things as late binding of PGs.
   **/
  public final void bindToLDM(LDMServesPlugin ldm) {
    _ldm = ldm;
  }

  /** Called by Asset factories to register the asset with the LDM.
   * This assigns a UID to the Asset and binds the instance to
   * the ldm.
   **/
  public final void registerWithLDM(LDMServesPlugin ldm) {
    setUID(ldm.getUIDServer().nextUID());
    bindToLDM(ldm);
  }

  // serialization

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeObject(myTypeIdentificationPG);
    out.writeObject(myItemIdentificationPG);

    ClusterContextTable.ContextState cs = ClusterContextTable.getContextState();
    if (cs instanceof ClusterContextTable.MessageContext) {
      // "Network" serialization

      // Urk!  We really should be sending the TID of the prototype instead of the prototype 
      // itself.  Older versions has a horrible hack here which needed to be dropped.
      Object sp = myPrototype;
      if (sp != null && sendPrototypeAsIDP) {
        TypeIdentificationPG ptip = myPrototype.getTypeIdentificationPG();
        sp = ptip.getTypeIdentification();
      }
      out.writeObject(sp);
    } else {
      // "File" serialization
      out.writeObject(myPrototype);

      if (out instanceof org.cougaar.core.persist.PersistenceOutputStream) {
        out.writeObject(roleschedule.getAvailableSchedule());
      }
    }      // End Network serialization
  } 

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    myTypeIdentificationPG = (TypeIdentificationPG) in.readObject();
    myItemIdentificationPG = (ItemIdentificationPG) in.readObject();

    ClusterContextTable.ContextState cs = ClusterContextTable.getContextState();
    ClusterContext cc = cs.getClusterContext();
    LDMServesPlugin ldm;
    if (cc != null) {
      MessageAddress ma = cc.getMessageAddress();
      ldm = LDMContextTable.getLDM(ma);
      bindToLDM(ldm);
    } else {
      log.error("Contextless deserialization of "+this);
      ldm = null;
    }
      
    Object proto = in.readObject();
    myPrototype = grokPrototype(ldm, proto);

    initRoleSchedule();

    if (cs instanceof ClusterContextTable.MessageContext) { // a message?
      // done
    } else {                    // from a file?
      if (in instanceof org.cougaar.core.persist.PersistenceInputStream) {
        Schedule schedule = (Schedule) in.readObject();
        ((RoleScheduleImpl)roleschedule).setAvailableSchedule(schedule);
      }
    }
  }

  /** Figure out how to interpret an object which was supplied as the prototype
   * of a deserialized Asset.
   **/
  private static Asset grokPrototype(LDMServesPlugin ldm, Object po) {
    if (po == null) {
      return null;
    } else {
      if (po instanceof Asset) {
        return grokPrototypeAsAsset(ldm, (Asset) po);
      } else if (po instanceof String) {
        return grokPrototypeAsTID(ldm, (String) po);
      } else {
        log.error("Deserialized an unknown prototype class: "+po); 
        return null;
      }
    }
  }

  /** Figure out how to handle an asset's prototype when deserialized as 
   * a real (whole) asset.
   **/
  private static Asset grokPrototypeAsAsset(LDMServesPlugin ldm, Asset pa) {
    if (ldm != null) {      // if we've got an LDM, check the prototype cache
      TypeIdentificationPG ptip = pa.getTypeIdentificationPG();
      String tid = ptip.getTypeIdentification();

      if (ldm.isPrototypeCached(tid)) { // is there a cached prototype already? yes.
        // use the cached one instead of this
        Asset putativeProto = ldm.getPrototype(tid);
        if (putativeProto != pa) {
          if (log.isInfoEnabled()) {
            log.info("Deserialization used cached prototype "+putativeProto+
                     " instead of stored prototype "+pa);
          }
        }
        // use the cached one instead
        pa = putativeProto;
      } else {                // is there a cached prototype already? no.
        // consider caching it
        // if we're trying to regenerate the prototype cache,
        // AND we've got a prototype AND we have a live LDM, then try to cache it
        if (regeneratePrototypeCacheP) {
          ldm.cachePrototype(tid, pa);
          if (log.isDebugEnabled()) {
            log.debug("Deserialization cached prototype "+pa);
          } 
        }
      }
    } else {
      // log.warn("Performance problem: Deserialization of Asset Prototypes without LDM context");
    }
    return pa;
  }

  /** Figure out how to handle an asset's prototype when deserialized as
   * a raw TypeIdentification string.
   **/
  private static Asset grokPrototypeAsTID(LDMServesPlugin ldm, String tid) {
    Asset pa = null;
    if (ldm != null) {
      // get the proto from OUR ldm...
      // for now, we just look at the TId of the TIP sent.
      pa = ldm.getPrototype(tid);

      // if no proto found, we're on our own,
      // so we'll create a proto with the tid and no other
      // props.
      if (pa == null) {
        PlanningFactory ldmf = (PlanningFactory) ldm.getFactory("planning");
        pa = ldmf.createPrototype(Asset.class, tid);
        // even though there is no prototype provider, there
        // might be property providers that can handle it, so
        // we'll expose our new instance to them.
        ldm.fillProperties(pa);
        // might as well register it, since we're unlikely to get
        // a proto provider later.
        ldm.cachePrototype(tid,pa);
        log.error("Deserialized an Asset with an unknown prototype ID "+tid+" (prototype will be empty)");
      } // else got a prototype from the cache, so we're done
    } else {
      // no ldm, so we cannot do anything here
      log.error("Deserialized an asset with prototype "+tid+" without an LDM context (will not have a prototype)");
    }
    return pa;
  }

  private transient ToStringMemo toStringMemo = null;
  public synchronized String toString() {
    if (toStringMemo == null) {
      toStringMemo = ToStringMemo.getInstance(new Object() {
          public String toString() {
            String cn = Asset.this.getClass().getName();
            int p = cn.lastIndexOf('.');
            if (p>=0) cn = cn.substring(p+1);
            String ti = myTypeIdentificationPG.getTypeIdentification();
            String ii = myItemIdentificationPG.getItemIdentification(); 
            if (ti == null) { ti = "?"; }
            if (ii == null) { ii = "#"+hashCode(); }
            return "<"+cn+" "+ti+" "+ii+">";
          }
        });
    }
    return toStringMemo.toString();
  }

  //dummy PropertyChangeSupport for the Jess Interpreter.
  public transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
      pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
      pcs.removePropertyChangeListener(pcl);
  }

  /** this is a table of AssetClass->settermap
   * where settermap is a table of settername->method
   **/
  private static HashMap _classSetters = new HashMap(13);

  private static final Class[] _pgClassArgs = new Class[] { PropertyGroup.class };

  /**
   * Introspection-based propertygroup setter.  The property will be
   * added to either the built-in-property slots or the Other property set
   * as appropriate to the actual class of the asset.
   **/
  public void setPropertyGroup(NewPropertyGroup property) {
    try {
      String pS = property.getAssetSetMethod();
      Method m;
      HashMap _setterTable;
      synchronized (_classSetters) {
        _setterTable = (HashMap) _classSetters.get(this.getClass());
        if (_setterTable == null) {
          _setterTable = new HashMap(3);
          _classSetters.put(this.getClass(), _setterTable);
        }
      }
      synchronized (_setterTable) {
        m = (Method) _setterTable.get(pS);
        if (m == null) {
          Class assetC = this.getClass();
          try {
            m = assetC.getMethod(pS, _pgClassArgs);
          } catch (NoSuchMethodException e) {
            // add-on property.  sigh.
            try {
              m = assetC.getMethod("addOtherPropertyGroup", _pgClassArgs);
            } catch (NoSuchMethodException e1) {
              log.error("Couldn't find addOtherPropertyGroup in "+this, e1);
            }
          }
          _setterTable.put(pS, m);
          /*
          if (m != null) {
            System.err.println("\n"+this.getClass()+"."+pS+"() found "+m);
          }
          */
        }
      }
      if (m != null) {
        // we could sync the calls to avoid consing the arglist
        m.invoke(this, new Object[] {property});
      }
    } catch (Exception e) {
      log.error("setPropertyGroup problem", e);
    }
  }

  private static PropertyDescriptor properties[];

  static {
    try {
      properties = new PropertyDescriptor[5];
      properties[0] = new PropertyDescriptor("roleSchedule", Asset.class, "getRoleSchedule", null);
      properties[1] = new PropertyDescriptor("UID", Asset.class, "getUID", null);
      properties[2] = new PropertyDescriptor("typeIdentificationPG", Asset.class, "getTypeIdentificationPG", null);
      properties[3] = new PropertyDescriptor("itemIdentificationPG", Asset.class, "getItemIdentificationPG", null);
      properties[4] = new PropertyDescriptor("class", Asset.class, "getClass", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }

  public int hashCode() {
    int hc = 1;
    String tid = getTypeIdentificationPG().getTypeIdentification();
    String iid = getItemIdentificationPG().getItemIdentification();
    if (tid != null) hc+=tid.hashCode();
    if (iid != null) hc+=iid.hashCode();
    return hc;
  }

  /** Equals for assets is defined as being exactly the same class
   * and having equals TypeIdentification and ItemIdentification codes.
   **/

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(getClass() == o.getClass())) return false;
    Asset oa = (Asset) o;
    TypeIdentificationPG tpg = getTypeIdentificationPG();
    TypeIdentificationPG opg = oa.getTypeIdentificationPG();
    if ( tpg == null || opg == null) return false;
    String ttid = tpg.getTypeIdentification();
    String otid = opg.getTypeIdentification();
    if (ttid == null || otid == null || !(ttid.equals(otid))) return false;
    ItemIdentificationPG tipg = getItemIdentificationPG();
    ItemIdentificationPG oipg = oa.getItemIdentificationPG();
    if ( tipg == null || oipg == null) return false;
    String tiid = tipg.getItemIdentification();
    String oiid = oipg.getItemIdentification();

    return compareStrings(tiid,oiid);
    /*
    if (tiid == null || oiid == null || !(tiid.equals(oiid))) return false;
    return true;
    */
  }

  private final boolean compareStrings(String s1, String s2) {
    if (s1 == s2) return true;  // catches null==null, x==x

    if (s1 != null) {
      // x==null, x==y, x==x
      return s1.equals(s2); 
    } else {
      // null==*
      return false;
    }
  }

  //
  // new PG resolution support
  //

  // search methods inherited from AssetSkeletonBase


  /** return the value of the specified PG if it is 
   * already present in a slot.
   **/
  protected PropertyGroup getLocalPG(Class pgc, long t) {
    if (ItemIdentificationPG.class.equals(pgc)) 
      return myItemIdentificationPG;
    if (TypeIdentificationPG.class.equals(pgc)) 
      return myTypeIdentificationPG;
    return super.getLocalPG(pgc,t);
  }

  /** Set the apropriate slot in the asset to the specified pg.
   * Scheduled PGs have the time range in them, so the time (range)
   * should not be specified in the arglist.
   **/
  protected void setLocalPG(Class pgc, PropertyGroup pg) {
    if (ItemIdentificationPG.class.equals(pgc)) {
      myItemIdentificationPG = (ItemIdentificationPG)pg;
    }
    else if (TypeIdentificationPG.class.equals(pgc)) {
      myTypeIdentificationPG = (TypeIdentificationPG)pg;
    } 
    else {
      super.setLocalPG(pgc,pg);
    }
  }


  /** return the value of the specified PG if it is 
   * already present in a slot.
   **/
  protected PropertyGroupSchedule getLocalPGSchedule(Class pgc) {
    return super.getLocalPGSchedule(pgc);
  }

  /** Set the apropriate slot in the asset to the specified pg.
   * Scheduled PGs have the time range in them, so the time (range)
   * should not be specified in the arglist.
   **/
  protected void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {
    super.setLocalPGSchedule(pgSchedule);
  }



  /** @return true IFF the specified PG class is set, available and non-null on 
   * the Asset instance.  No checks for late-binding or prototype are ever performed
   * for this check.
   **/
  public final boolean isPGLocal(Class pgc) {
    return isPGLocal(pgc, UNSPECIFIED_TIME);
  }

  /** @return true IFF the specified PG class is set, available and non-null on 
   * the Asset instance.  No checks for late-binding or prototype are ever performed
   * for this check.  This variation is for querying at a specific time.  
   * If time is specified as Asset.UNSPECIFIED_TIME, then this call is equivalent to
   * the single-argument isPGLocal(class).
   **/
  public final boolean isPGLocal(Class pgc, long t) {
    return getLocalPG(pgc, t) != null;
  }


  /** @return true IFF the specified PGSchedule class is set, available and non-null on 
   * the Asset instance.  No checks for late-binding or prototype are ever performed
   * for this check.
   **/
  public final boolean isPGScheduleLocal(Class pgc) {
    return getLocalPGSchedule(pgc) != null;
  }


  /** get and possibly cache a PG value.
   * The information can come from a number of places:
   *   a local slot 
   *   a late binding
   *   the prototype (recurse to resolve on the prototype)
   *   a default value
   *
   * Will return Null_PG instances if present.
   * Defined as abstract in AssetSkeletonBase
   **/
  public final PropertyGroup resolvePG(Class pgc, long t) {
    // check local slots - this call never sets
    PropertyGroup pg = getLocalPG(pgc, t); 
    if (pg != null) return pg;  // return it - already set

    // check late binding
    if ((pg = lateBindPG(pgc, t)) != null) {
      //setLocalPG(pgc, pg);
      return pg;
    }

    // check the prototype
    if (myPrototype != null) {
      // recurse
      if ((pg = myPrototype.resolvePG(pgc, t)) != null) {
        // should we cache the prototype's PG value?
        // Let's not.
        return pg;
      }
    }

    // possibly default
    pg = generateDefaultPG(pgc);

    return pg;
  }


  /** request late binding from the LDM for this asset/PGClass.
   * Late binders should set the asset's PG as appropriate in 
   * addition to returning the PG.
   * Implements an abstract method from AssetSkeletonBase.
   * @return null or a PropertyGroup instance.
   */
  protected final PropertyGroup lateBindPG(Class pgc, long t) {
    if (_ldm != null) {
      // Pass along the requested time in case we can get just
      // a single time slice late-bound.  This brings up other
      // issues which we will just hand wave about for now...
      return _ldm.lateFillPropertyGroup(this, pgc, t);
    }
    else {
      log.error("Asset "+this+" is not bound to an LDM instance!");
    }
    return null;
  }

  /** get and possibly cache a PropertyGroupSchedule.
   * The information can come from a number of places:
   *   a local slot 
   *   the prototype (recurse to resolve on the prototype)
   *
   * Defined as abstract in AssetSkeletonBase
   **/
  public final PropertyGroupSchedule resolvePGSchedule(Class pgc) {
    // check local slots - this call never sets
    PropertyGroupSchedule pgSchedule = getLocalPGSchedule(pgc); 
    if (pgSchedule != null) return pgSchedule;  // return it - already set

    // check the prototype
    if (myPrototype != null) {
      // recurse
      if ((pgSchedule = myPrototype.resolvePGSchedule(pgc)) != null) {
        // should we cache the prototype's PG value?
        // Let's not.
        return pgSchedule;
      }
    }

    return null;
  }

  // 
  // implement Publishable
  //
  public boolean isPersistable() { return true; }

  private transient TypeItemKey myKey = null;

  public Object getKey() {
    if (myKey == null) {
      myKey = new TypeItemKey(this);
    }
    
    return myKey;
  }

  private static class TypeItemKey implements java.io.Serializable {
    private String myTypeString = null;
    private String myItemString = null;
    private int myHashCode;
    
    public TypeItemKey(Asset asset) {
      TypeIdentificationPG tipg = asset.getTypeIdentificationPG();
      if (tipg != null) {
        myTypeString = tipg.getTypeIdentification();
      }
      if (myTypeString == null) {
        myTypeString = "";
      }
      
      ItemIdentificationPG iipg = asset.getItemIdentificationPG();
      if (iipg != null) {
        myItemString = iipg.getItemIdentification();
      }
      if (myItemString == null) {
        myItemString = "";
      }

      if ((myTypeString.equals("")) && (myItemString.equals(""))) {
        log.error("Unable to create unique key for asset " + asset +
                  " - type and item identification are not set.");
      }

      myHashCode = myTypeString.hashCode() + myItemString.hashCode();
    }

    public int hashCode() {
      return myHashCode;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (o instanceof TypeItemKey) {
        TypeItemKey that = (TypeItemKey) o;
        return (this.myTypeString.equals(that.myTypeString) && 
                this.myItemString.equals(that.myItemString));
      } 

      return false;
    }

    public String toString() {
      return "<" + myTypeString + " " + myItemString + ">";
    }
  }

  // ChangeReport tracking
  //
  public interface AssetChangeReport extends ChangeReport {}
}




