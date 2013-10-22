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

package org.cougaar.planning.ldm.plan;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;

/**
 * A RoleSchedule is a representation of an asset's scheduled
 * commitments. These commitments(plan elements) are stored
 * in a collection.  RoleSchedules do not travel with an
 * asset accross agent boundaries, therefore, the roleschedule 
 * is only valid while that asset is assigned to the current agent.
 **/
public class RoleScheduleImpl 
  extends ScheduleImpl
  implements RoleSchedule, NewRoleSchedule
{
  private transient Schedule availableschedule;
  private Asset asset;
	
  /** Constructor
   * @param theasset this roleschedule is attached to
   **/
  public RoleScheduleImpl(Asset theasset) {
    super();
    setScheduleType(ScheduleType.ROLE);
    setScheduleElementType(ScheduleElementType.PLAN_ELEMENT);
    asset = theasset;
  }
	
  /** @return the Asset of this roleschedule.
   **/
  public Asset getAsset() {
    return asset;
  }
	
  /** SHOULD *ONLY* BE CALLED BY THE ASSET CREATOR or THE ASSETTRANSFER LP!
   * set the availableschedule
   * @param avschedule - the schedule that the asset is assigned 
   * or available to this agent
   **/
  public void setAvailableSchedule(Schedule avschedule) {
    availableschedule = avschedule;
  }

  //return the available schedule for this asset
  public Schedule getAvailableSchedule() {
    return availableschedule;
  }

  /**
   *  Cougaar INTERNAL METHOD - SHOULD NEVER BE CALLED BY A PLUGIN
   *  add a single planelement to the roleschedule container
   *  @param aPlanElement PlanElement to add
   *  @deprecated Use add(Object aPlanElement) instead.
   **/
  public synchronized void addToRoleSchedule(PlanElement aPlanElement) {
    add(aPlanElement);
  }
	
  /**
   *  Cougaar INTERNAL METHOD - SHOULD NEVER BE CALLED BY A PLUGIN
   *  remove a single planelement from the roleschedule container
   *  @param aPlanElement PlanElement to remove
   *  @deprecated Use remove(Object aPlanElement) instead.
   **/
  public synchronized void removeFromRoleSchedule(PlanElement aPlanElement) {
    remove(aPlanElement);
  }

  public Collection getEncapsulatedRoleSchedule(Date start, Date end) {
    return getEncapsulatedRoleSchedule(start.getTime(), end.getTime());
  }

  public synchronized Collection getEncapsulatedRoleSchedule(long start, long end) {
    return getEncapsulatedScheduleElements(start, end);
  }

  public synchronized Collection getEqualAspectValues(final int aspect, final double value) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object obj) {
          AllocationResult ar = ((PlanElement)obj).getEstimatedResult();
          if (ar != null) {
            return (value == ar.getValue(aspect));
          }
          return false;
        }
      });
  }
  
  public synchronized Collection getMatchingRoleElements(final Role aRole) {
    return filter(new UnaryPredicate() {
        public boolean execute (Object obj) {
          if (obj instanceof Allocation) {
            Role disrole = ((Allocation)obj).getRole();
            if (disrole.equals(aRole)){
              return true;
            }
          } else if (obj instanceof AssetTransfer) {
            Role disrole = ((AssetTransfer)obj).getRole();
            if (disrole.equals(aRole)) {
              return true;
            }
          }
          return false;
        }
      });
  }
  
  public Collection getOverlappingRoleSchedule(Date start, Date end) {
    return getOverlappingRoleSchedule(start.getTime(), end.getTime());
  }
  public synchronized Collection getOverlappingRoleSchedule(long start, long end) {
    return getOverlappingScheduleElements(start,end);
  }
  
  /** get an enumeration over a copy of all of the schedule elements of this 
   * schedule.
   * Note that this is a copy, changes to the underlying schedule will not be 
   * reflected in the Enumeration.
   * @return Enumeration{ScheduleElement}
   */
  public Enumeration getRoleScheduleElements() {
    return getAllScheduleElements();
  }

  /** Convenience utility that adds the requested aspectvalues of the estimated
    * allocationresult of each PlanElement (RoleSchedule Element) in the given
    * orderedset.
    * If the requested aspecttype is not defined for any of the elements, nothing
    * will be added to the sum for that particular element.
    * This utility should be used to add aspecttypes like quantity, cost, etc.
    * @return double The sum of the aspectvalues
    * @param elementsToAdd  A set of roleschedule elements (planelements) to add
    * @see org.cougaar.planning.ldm.plan.AspectType
    **/
  public double addAspectValues(Collection elementsToAdd, 
                                       int aspecttype) {
    double acc = 0.0;

    synchronized (elementsToAdd) {
      if (elementsToAdd instanceof List) {
        int listSize = elementsToAdd.size();
        
        for (int index = 0; index < listSize; index++) {
          PlanElement anElement = 
            (PlanElement)((List) elementsToAdd).get(index);
          AllocationResult aResult = anElement.getEstimatedResult();
          if (aResult != null && aResult.isDefined(aspecttype)) {
            acc += aResult.getValue(aspecttype);
          }
        }
      } else {
        for (Iterator i = elementsToAdd.iterator(); i.hasNext(); ) {
          PlanElement anElement = (PlanElement)i.next();
          AllocationResult aResult = anElement.getEstimatedResult();
          if (aResult != null && aResult.isDefined(aspecttype)) {
            acc += aResult.getValue(aspecttype);
          }
        }
      }
    } // end synchronization on elementsToAdd

    return acc;
  }
      
  // for BeanInfo
  public synchronized String[] getRoleScheduleIDs() {
    int l = size();
    String[] IDs = new String[l];
    for (int i = 0; i < l; i++) {
      IDs[i] = ((PlanElement)get(i)).getUID().toString();
    }
    return IDs;
  }

  public String getRoleScheduleID(int i) {
    return ((PlanElement)get(i)).getUID().toString();
  }

}




