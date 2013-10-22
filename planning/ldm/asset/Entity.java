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

/* @generated Wed Jun 06 07:52:51 EDT 2012 from assets.def - DO NOT HAND EDIT */
package org.cougaar.planning.ldm.asset;
import org.cougaar.planning.ldm.asset.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Vector;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.planning.ldm.plan.RelationshipScheduleImpl;
public class Entity extends EntityAdapter implements HasRelationships {

  public Entity() {
    myClusterPG = null;
    myEntityPG = null;
    myCommunityPGSchedule = null;
    myRelationshipPG = null;
    myLocationSchedulePG = null;
  }

  public Entity(Entity prototype) {
    super(prototype);
    myClusterPG=null;
    myEntityPG=null;
    myCommunityPGSchedule=null;
    myRelationshipPG=null;
    myLocationSchedulePG=null;
  }

  /** For infrastructure only - use org.cougaar.core.domain.Factory.copyInstance instead. **/
  public Object clone() throws CloneNotSupportedException {
    Entity _thing = (Entity) super.clone();
    if (myClusterPG!=null) _thing.setClusterPG(myClusterPG.lock());
    if (myEntityPG!=null) _thing.setEntityPG(myEntityPG.lock());
    if (myCommunityPGSchedule!=null) _thing.setCommunityPGSchedule((PropertyGroupSchedule) myCommunityPGSchedule.lock());
    if (myLocationSchedulePG!=null) _thing.setLocationSchedulePG(myLocationSchedulePG.lock());
    return _thing;
  }

  /** create an instance of the right class for copy operations **/
  public Asset instanceForCopy() {
    return new Entity();
  }

  /** create an instance of this prototype **/
  public Asset createInstance() {
    return new Entity(this);
  }

  protected void fillAllPropertyGroups(Vector v) {
    super.fillAllPropertyGroups(v);
    { Object _tmp = getClusterPG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
    { Object _tmp = getEntityPG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
    { Object _tmp = getCommunityPGSchedule();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
    { Object _tmp = getRelationshipPG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
    { Object _tmp = getLocationSchedulePG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
  }

  private transient ClusterPG myClusterPG;

  public ClusterPG getClusterPG() {
    ClusterPG _tmp = (myClusterPG != null) ?
      myClusterPG : (ClusterPG)resolvePG(ClusterPG.class);
    return (_tmp == ClusterPG.nullPG)?null:_tmp;
  }
  public void setClusterPG(PropertyGroup arg_ClusterPG) {
    if (!(arg_ClusterPG instanceof ClusterPG))
      throw new IllegalArgumentException("setClusterPG requires a ClusterPG argument.");
    myClusterPG = (ClusterPG) arg_ClusterPG;
  }

  private transient EntityPG myEntityPG;

  public EntityPG getEntityPG() {
    EntityPG _tmp = (myEntityPG != null) ?
      myEntityPG : (EntityPG)resolvePG(EntityPG.class);
    return (_tmp == EntityPG.nullPG)?null:_tmp;
  }
  public void setEntityPG(PropertyGroup arg_EntityPG) {
    if (!(arg_EntityPG instanceof EntityPG))
      throw new IllegalArgumentException("setEntityPG requires a EntityPG argument.");
    myEntityPG = (EntityPG) arg_EntityPG;
  }

  private transient PropertyGroupSchedule myCommunityPGSchedule;

  public CommunityPG getCommunityPG(long time) {
    CommunityPG _tmp = (myCommunityPGSchedule != null) ?
      (CommunityPG)myCommunityPGSchedule.intersects(time) :
      (CommunityPG)resolvePG(CommunityPG.class, time);
    return (_tmp == CommunityPG.nullPG)?null:_tmp;
  }
  public PropertyGroupSchedule getCommunityPGSchedule() {
    PropertyGroupSchedule _tmp = (myCommunityPGSchedule != null) ?
         myCommunityPGSchedule : resolvePGSchedule(CommunityPG.class);
    return _tmp;
  }

  public void setCommunityPG(PropertyGroup arg_CommunityPG) {
    if (!(arg_CommunityPG instanceof CommunityPG))
      throw new IllegalArgumentException("setCommunityPG requires a CommunityPG argument.");
    if (myCommunityPGSchedule == null) {
      myCommunityPGSchedule = PropertyGroupFactory.newCommunityPGSchedule();
    }

    myCommunityPGSchedule.add(arg_CommunityPG);
  }

  public void setCommunityPGSchedule(PropertyGroupSchedule arg_EntitySchedule) {
    if (!(CommunityPG.class.equals(arg_EntitySchedule.getPGClass())))
      throw new IllegalArgumentException("setCommunityPGSchedule requires a PropertyGroupSchedule ofCommunityPGs.");

    myCommunityPGSchedule = arg_EntitySchedule;
  }

  private transient RelationshipPG myRelationshipPG;

  public RelationshipSchedule getRelationshipSchedule() {
    return getRelationshipPG().getRelationshipSchedule();
  }
  public void setRelationshipSchedule(RelationshipSchedule schedule) {
    NewRelationshipPG _argRelationshipPG = (NewRelationshipPG) getRelationshipPG().copy();
    _argRelationshipPG.setRelationshipSchedule(schedule);
    setRelationshipPG(_argRelationshipPG);
  }

  public boolean isLocal() {
    return getRelationshipPG().getLocal();
  }
  public void setLocal(boolean localFlag) {
    NewRelationshipPG _argRelationshipPG = (NewRelationshipPG) getRelationshipPG().copy();
    _argRelationshipPG.setLocal(localFlag);
    setRelationshipPG(_argRelationshipPG);
  }

  public boolean isSelf() {
    return getRelationshipPG().getLocal();
  }

  public RelationshipPG getRelationshipPG() {
    RelationshipPG _tmp = (myRelationshipPG != null) ?
      myRelationshipPG : (RelationshipPG)resolvePG(RelationshipPG.class);
    return (_tmp == RelationshipPG.nullPG)?null:_tmp;
  }
  public void setRelationshipPG(PropertyGroup arg_RelationshipPG) {
    if (!(arg_RelationshipPG instanceof RelationshipPG))
      throw new IllegalArgumentException("setRelationshipPG requires a RelationshipPG argument.");
    myRelationshipPG = (RelationshipPG) arg_RelationshipPG;
  }

  private transient LocationSchedulePG myLocationSchedulePG;

  public LocationSchedulePG getLocationSchedulePG() {
    LocationSchedulePG _tmp = (myLocationSchedulePG != null) ?
      myLocationSchedulePG : (LocationSchedulePG)resolvePG(LocationSchedulePG.class);
    return (_tmp == LocationSchedulePG.nullPG)?null:_tmp;
  }
  public void setLocationSchedulePG(PropertyGroup arg_LocationSchedulePG) {
    if (!(arg_LocationSchedulePG instanceof LocationSchedulePG))
      throw new IllegalArgumentException("setLocationSchedulePG requires a LocationSchedulePG argument.");
    myLocationSchedulePG = (LocationSchedulePG) arg_LocationSchedulePG;
  }

  // generic search methods
  public PropertyGroup getLocalPG(Class c, long t) {
    if (ClusterPG.class.equals(c)) {
      return (myClusterPG==ClusterPG.nullPG)?null:myClusterPG;
    }
    if (EntityPG.class.equals(c)) {
      return (myEntityPG==EntityPG.nullPG)?null:myEntityPG;
    }
    if (CommunityPG.class.equals(c)) {
      if (myCommunityPGSchedule==null) {
        return null;
      } else {
        if (t == UNSPECIFIED_TIME) {
          return (CommunityPG)myCommunityPGSchedule.getDefault();
        } else {
          return (CommunityPG)myCommunityPGSchedule.intersects(t);
        }
      }
    }
    if (RelationshipPG.class.equals(c)) {
      return (myRelationshipPG==RelationshipPG.nullPG)?null:myRelationshipPG;
    }
    if (LocationSchedulePG.class.equals(c)) {
      return (myLocationSchedulePG==LocationSchedulePG.nullPG)?null:myLocationSchedulePG;
    }
    return super.getLocalPG(c,t);
  }

  public PropertyGroupSchedule getLocalPGSchedule(Class c) {
    if (CommunityPG.class.equals(c)) {
      return myCommunityPGSchedule;
    }
    return super.getLocalPGSchedule(c);
  }

  public void setLocalPG(Class c, PropertyGroup pg) {
    if (ClusterPG.class.equals(c)) {
      myClusterPG=(ClusterPG)pg;
    } else
    if (EntityPG.class.equals(c)) {
      myEntityPG=(EntityPG)pg;
    } else
    if (CommunityPG.class.equals(c)) {
      if (myCommunityPGSchedule==null) {
        myCommunityPGSchedule=PropertyGroupFactory.newCommunityPGSchedule();
      } else {
        myCommunityPGSchedule.removeAll(myCommunityPGSchedule.intersectingSet((TimePhasedPropertyGroup) pg));
      }
      myCommunityPGSchedule.add(pg);
    } else
    if (RelationshipPG.class.equals(c)) {
      myRelationshipPG=(RelationshipPG)pg;
    } else
    if (LocationSchedulePG.class.equals(c)) {
      myLocationSchedulePG=(LocationSchedulePG)pg;
    } else
      super.setLocalPG(c,pg);
  }

  public void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {
    if (CommunityPG.class.equals(pgSchedule.getPGClass())) {
      myCommunityPGSchedule=pgSchedule;
    } else
      super.setLocalPGSchedule(pgSchedule);
  }

  public PropertyGroup removeLocalPG(Class c) {
    PropertyGroup removed = null;
    if (ClusterPG.class.equals(c)) {
      removed=myClusterPG;
      myClusterPG=null;
    } else if (EntityPG.class.equals(c)) {
      removed=myEntityPG;
      myEntityPG=null;
    } else if (CommunityPG.class.equals(c)) {
      if (myCommunityPGSchedule!=null) {
        if (myCommunityPGSchedule.getDefault()!=null) {
          removed=myCommunityPGSchedule.getDefault();
        } else if (myCommunityPGSchedule.size() > 0) {
          removed=(PropertyGroup) myCommunityPGSchedule.get(0);
        }
        myCommunityPGSchedule=null;
      }
    } else if (RelationshipPG.class.equals(c)) {
      removed=myRelationshipPG;
      myRelationshipPG=null;
    } else if (LocationSchedulePG.class.equals(c)) {
      removed=myLocationSchedulePG;
      myLocationSchedulePG=null;
    } else {
      removed=super.removeLocalPG(c);
    }
    return removed;
  }

  public PropertyGroup removeLocalPG(PropertyGroup pg) {
    Class pgc = pg.getPrimaryClass();
    if (ClusterPG.class.equals(pgc)) {
      PropertyGroup removed=myClusterPG;
      myClusterPG=null;
      return removed;
    } else if (EntityPG.class.equals(pgc)) {
      PropertyGroup removed=myEntityPG;
      myEntityPG=null;
      return removed;
    } else if (CommunityPG.class.equals(pgc)) {
if ((myCommunityPGSchedule!=null) && 
          (myCommunityPGSchedule.remove(pg))) {
        return pg;
      }
    } else if (RelationshipPG.class.equals(pgc)) {
      PropertyGroup removed=myRelationshipPG;
      myRelationshipPG=null;
      return removed;
    } else if (LocationSchedulePG.class.equals(pgc)) {
      PropertyGroup removed=myLocationSchedulePG;
      myLocationSchedulePG=null;
      return removed;
    } else {}
    return super.removeLocalPG(pg);
  }

  public PropertyGroupSchedule removeLocalPGSchedule(Class c) {
    if (CommunityPG.class.equals(c)) {
      PropertyGroupSchedule removed=myCommunityPGSchedule;
      myCommunityPGSchedule=null;
      return removed;
    } else 
   {
      return super.removeLocalPGSchedule(c);
    }
  }

  public PropertyGroup generateDefaultPG(Class c) {
    if (ClusterPG.class.equals(c)) {
      return (myClusterPG= new ClusterPGImpl());
    } else
    if (EntityPG.class.equals(c)) {
      return (myEntityPG= new EntityPGImpl());
    } else
    if (CommunityPG.class.equals(c)) {
      return null;
    } else
    if (RelationshipPG.class.equals(c)) {
      return (myRelationshipPG= new RelationshipPGImpl());
    } else
    if (LocationSchedulePG.class.equals(c)) {
      return (myLocationSchedulePG= new LocationSchedulePGImpl());
    } else
      return super.generateDefaultPG(c);
  }

  // dumb serialization methods

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
      if (myClusterPG instanceof Null_PG || myClusterPG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myClusterPG);
      }
      if (myEntityPG instanceof Null_PG || myEntityPG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myEntityPG);
      }
      if (myCommunityPGSchedule instanceof Null_PG || myCommunityPGSchedule instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myCommunityPGSchedule);
      }
      if (myRelationshipPG instanceof Null_PG || myRelationshipPG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myRelationshipPG);
      }
      if (myLocationSchedulePG instanceof Null_PG || myLocationSchedulePG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myLocationSchedulePG);
      }
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
      myClusterPG=(ClusterPG)in.readObject();
      myEntityPG=(EntityPG)in.readObject();
      myCommunityPGSchedule=(PropertyGroupSchedule)in.readObject();
      myRelationshipPG=(RelationshipPG)in.readObject();
      myLocationSchedulePG=(LocationSchedulePG)in.readObject();
  }
  // beaninfo support
  private static PropertyDescriptor properties[];
  static {
    try {
      properties = new PropertyDescriptor[5];
      properties[0] = new PropertyDescriptor("ClusterPG", Entity.class, "getClusterPG", null);
      properties[1] = new PropertyDescriptor("EntityPG", Entity.class, "getEntityPG", null);
      properties[2] = new PropertyDescriptor("CommunityPGSchedule", Entity.class, "getCommunityPGSchedule", null);
      properties[3] = new PropertyDescriptor("RelationshipPG", Entity.class, "getRelationshipPG", null);
      properties[4] = new PropertyDescriptor("LocationSchedulePG", Entity.class, "getLocationSchedulePG", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pds = super.getPropertyDescriptors();
    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+5];
    System.arraycopy(pds, 0, ps, 0, pds.length);
    System.arraycopy(properties, 0, ps, pds.length, 5);
    return ps;
  }
}
