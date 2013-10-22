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


package org.cougaar.planning.ldm.lps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.RoleSchedule;
import org.cougaar.planning.ldm.plan.RoleScheduleConflicts;
import org.cougaar.planning.ldm.plan.ScheduleElement;

/** RoleScheduleConflictLP checks an Asset's roleschedule for potential
  * schedule conflicts.  Each time a new allocation or assettransfer is published or
  * a change is published for an allocation or assettransfer, the allocation's/assettransfer's estimated
  * allocation result schedule will be checked against the rest of the 
  * roleschedule.    If there is a conflict, the conflicting
  * allocation's/assettransfer's potentialconflict flags will be set.
  * The schedule will also be checked for dates outside
  * of the available schedule.  If an available conflict occurs, the allocation's/assettransfer's
  * assetavailabilityconflict flag will be set.
  **/

public class RoleScheduleConflictLP
implements LogicProvider, EnvelopeLogicProvider {

  private final RootPlan rootplan;

  public RoleScheduleConflictLP(
      RootPlan rootplan) {
    this.rootplan = rootplan;
  }

  public void init() {
  }

  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    // We don't need to test on Envelope Contents/Action again...
    
    if (!((obj instanceof Allocation) || (obj instanceof AssetTransfer))) return;

    if (!(o.isAdd() || 
          (o.isChange() && ((RoleScheduleConflicts)obj).checkConflicts())))
      return;

    PlanElement pe = (PlanElement) obj;
    // re-set the checkconflict flag
    ((RoleScheduleConflicts)pe).setCheckConflicts(false);
    
    Asset theasset = null;
    boolean currentpc = false;
    boolean currentaac = false;
    if (pe instanceof Allocation) {
      Allocation alloc = (Allocation) pe;
      theasset = alloc.getAsset();
      currentpc = alloc.isPotentialConflict();
      currentaac = alloc.isAssetAvailabilityConflict();
    } else if ( pe instanceof AssetTransfer ) {
      AssetTransfer at = (AssetTransfer) pe;
      theasset = at.getAsset();
      currentpc = at.isPotentialConflict();
      currentaac = at.isAssetAvailabilityConflict();
    }
    RoleSchedule rs = theasset.getRoleSchedule();
    
    boolean withinAvailSched = checkAvailableSchedule(pe, rs);
    List conflicts = checkRoleSchedule(pe, rs);
    
    if ( ! conflicts.isEmpty() ) {
      for ( ListIterator lit = conflicts.listIterator(); lit.hasNext(); ) {
        PlanElement tomark = (PlanElement) lit.next();
        // make sure this pe's potentialconflict flag isn't already true before marking it
        boolean othercurrentpc = false;
        if ( tomark instanceof Allocation) {
          othercurrentpc = ((Allocation)tomark).isPotentialConflict();
        } else if (tomark instanceof AssetTransfer) {
          othercurrentpc = ((AssetTransfer)tomark).isPotentialConflict();
        }
        if ( ! othercurrentpc ) {
          ((RoleScheduleConflicts)tomark).setPotentialConflict(true);
          rootplan.change(tomark, changes);
        }
      }
      // now mark ourselves if our potentialconflict flag isn't already true
      if ( ! currentpc ) {
        ((RoleScheduleConflicts)pe).setPotentialConflict(true);
        rootplan.change(pe, changes);
      }
    } else {
      // If there are no conflicts, check to see if our conflict flag was
      // previously set to true.  If it was, re-set it to false.
      if ( currentpc ) {
          ((RoleScheduleConflicts)pe).setPotentialConflict(false);
          rootplan.change(pe, changes);
      } 
    }
      
    // if its not within the available schedule and the asset availability conflict
    // flag isn't already true, set it!      
    if ( (! withinAvailSched) && (! currentaac) ) {
      // if there was a conflict here set our asset availability conflict flag
        ((RoleScheduleConflicts)pe).setAssetAvailabilityConflict(true);
        rootplan.change(pe, changes);
    } else {
      // if there was no conflict, check to see if our conflict flag was
      // previously set to true.  If it was, re-set if to false.
      if ( currentaac ) {
        ((RoleScheduleConflicts)pe).setAssetAvailabilityConflict(false);
        rootplan.change(pe, changes);
      }
    }
  }
       
    
  private boolean checkAvailableSchedule(PlanElement thepe, RoleSchedule thers) {
    // if the available schedule is null, return true
    if ( thers.getAvailableSchedule() == null ) {
      return true;
    }
    AllocationResult estar = thepe.getEstimatedResult();
    // make sure that the start time and end time aspects are defined.
    // if they aren't return true 
    //(this could happen with a propagating failed allocation result).
    if ( (estar == null) ||
	 (! (estar.isDefined(AspectType.START_TIME)) ) || 
	 (! (estar.isDefined(AspectType.END_TIME)) ) ) {
      return true;
    }
    double sdate = estar.getValue(AspectType.START_TIME);
    double edate = estar.getValue(AspectType.END_TIME);
    // get the available schedule, remember there may be more than one available time segment.
    Enumeration availsched = thers.getAvailableSchedule().getAllScheduleElements();
    while (availsched.hasMoreElements()) {
      ScheduleElement se = (ScheduleElement) availsched.nextElement();
      long availsdate = se.getStartDate().getTime();
      long availedate = se.getEndDate().getTime();
      if ( (sdate >= availsdate) && (edate <= availedate) ) {
        // if the dates fall into atleast one of the time segments return true.
        return true;
      }
    }
    // if we are here it didn't fall into any available schedule
    return false;
  }
  
  private List checkRoleSchedule(PlanElement thepe, RoleSchedule thers) {
    List conflictlist = new ArrayList();
    AllocationResult estar = thepe.getEstimatedResult();
    
    // make sure that the start time and end time aspects are defined.
    // if they aren't return the empty conflictlist
    // (this could happen with a propagating failed allocation result).
    if ((estar != null) && 
	(estar.isDefined(AspectType.START_TIME) ) && 
	(estar.isDefined(AspectType.END_TIME) ) ) {
      
      long stime = ((long)estar.getValue(AspectType.START_TIME));
      long etime = ((long)estar.getValue(AspectType.END_TIME));
    
      // check for encapsulating schedules of other plan elements.
      Collection encap = thers.getEncapsulatedRoleSchedule(stime, etime);
      for (Iterator ec = encap.iterator(); ec.hasNext(); ) {
        PlanElement conflictpe = (PlanElement) ec.next();
        // make sure its not our pe.
        if ( !(conflictpe == thepe) ) {
          conflictlist.add(conflictpe);
        }
      }
    
      // check for ovelapping schedules of other plan elements.
      Collection overlap = thers.getOverlappingRoleSchedule(stime, etime);
      for (Iterator oc = overlap.iterator(); oc.hasNext(); ) {
        PlanElement overconflictpe = (PlanElement) oc.next();
        // once again, make sure its not our pe.
        if ( !(overconflictpe == thepe) ) {
          conflictlist.add(overconflictpe);
        }
      }
    }
    
    return conflictlist;
  }
}
