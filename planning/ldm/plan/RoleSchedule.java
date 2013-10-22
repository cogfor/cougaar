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

import org.cougaar.planning.ldm.asset.Asset;

/** 
 * A RoleSchedule is a representation of an asset's scheduled
 * commitments. These commitments(plan elements) are stored
 * in a Collection.  RoleSchedules do not travel with an
 * asset across agent boundaries, therefore, the roleschedule 
 * is only valid while that asset is assigned to the current agent.
 **/

public interface RoleSchedule 
  extends Schedule
{
  /** @return Asset the Asset of this roleschedule.
   **/
  Asset getAsset();

  /** @return a sorted Collection containing planelements
   * whose estimatedschedule falls within the given date range.
   * @param start Start time of the desired range.
   * @param end End time of the desired range.
   **/
  Collection getEncapsulatedRoleSchedule(long start, long end);
  /** @deprecated use getEncapsulatedRoleSchedule(long, long) **/
  Collection getEncapsulatedRoleSchedule(Date start, Date end);
  
  /**
   * @param aspecttype  The AspectType
   * @param value The double representing value of the given AspectType
   * @return a sorted Collection containing planelements with
   * a given AspectType and value.
   **/
  Collection getEqualAspectValues(int aspecttype, double value);
	
  /** @return a sorted collection containing planelements
   * whose estimatedschedule overlaps with the given date range
   * @param start Start time of overlapping range
   * @param end End time of overlapping range
   **/
  Collection getOverlappingRoleSchedule(long start, long end);

  /** @deprecated use getOverlappingRoleSchedule(long, long) **/
  Collection getOverlappingRoleSchedule(Date start, Date end);
	
  /** @return an Enumeration of PlanElements representing 
   * the entire roleschedule
   **/
  Enumeration getRoleScheduleElements();
  
  /** The AvailableSchedule represents the time period that this asset
   * is assigned to a agent for use.  It does not represent any usage
   * of this asset - that information is elsewhere in the RoleSchedule.
   *
   * @return the schedule marking the availability time frame for the asset
   * in this agent.
   **/
  Schedule getAvailableSchedule();
  
  /** @return a time ordered Collection containing planelements with a given Role.
   * @param aRole  The Role to find
   **/
  Collection getMatchingRoleElements(Role aRole);

  /** 
   * @return a time sorted Collection of planelements which
   * include this time.
   */
  Collection getScheduleElementsWithTime(long aDate);
  /** @deprecated use getScheduleElementsWithTime(long) **/
  Collection getScheduleElementsWithDate(Date aDate);
  
  /** Convenience utility that adds the requested aspectvalues of the estimated
   * allocationresult of each PlanElement (RoleSchedule Element) in the given
   * collection.
   * If the requested aspecttype is not defined for any of the elements, nothing
   * will be added to the sum for that particular element.
   * This utility should be used to add aspecttypes like quantity, cost, etc.
   * @return The sum of the aspectvalues
   * @param elementsToAdd  A set of roleschedule elements (planelements) to add
   * @see org.cougaar.planning.ldm.plan.AspectType
   **/
  double addAspectValues(Collection elementsToAdd, int aspecttype);
}





