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
import java.util.List;

import org.cougaar.util.Thunk;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

/**
 * A Schedule is an encapsulation of spatio-temporal relationships.
 * Current thought is to bind up both time and space into a single
 * object which may then be queried in various ways to test for
 * overlaps, inclusion, exclusion, etc with other schedules.
 *
 * Extends Collection to provide direct access to the collection api
 * accessors, and TimeSpan to allow comparison of whole schedules to 
 * each other.
 **/

public interface Schedule
  extends List, TimeSpan
{
  /** get a string describing what type of schedule this is (i.e.
   * what kind of scheduleelements this schedule containts).
   * Values are defined in ScheduleElementType.
   * @see org.cougaar.planning.ldm.plan.ScheduleElementType
   * @return String
   **/
  Class getScheduleElementType();
  
  /** get a description of what type of Schedule this is
   *  Values are defined in ScheduleType
   *  @see org.cougaar.planning.ldm.plan.ScheduleType
   *  @return ScheduleType
   **/
  String getScheduleType();
                
  /** get an enumeration of all of the schedule elements of this schedule.
   *  Enumeration backed by a copy of the Schedule.
   * @return Enumeration{ScheduleElement}
   */
  Enumeration getAllScheduleElements();
        
  /** Apply a Thunk to each ScheduleElement in the Schedule **/
  void applyThunkToScheduleElements(Thunk t);

  /** return subset of elements which meet the specified predicate
   * @return Collection(ScheduleElements)
   */
  Collection filter(UnaryPredicate predicate);

  /**
   * @deprecated Use getScheduleElementsWithTime(long aTime)
   */
  Collection getScheduleElementsWithDate(Date aDate);

  /** @return a sorted Collection of schedule elements that include this date.
   * Note that each schedule element will have a start date less than or equal to 
   * the specified date and an end date after the specified date.
   */
  Collection getScheduleElementsWithTime(long aTime);
        
  /** 
   * @deprecated Use getOverlappingScheduleElements(long, long)
   */
  Collection getOverlappingScheduleElements(Date startDate, Date endDate);

  /** @return a sorted Collection of schedule elements that have dates in the
   * given range of dates.  Note that these schedule elements may
   * or may not be fully bound by the date range - they may overlap.
   * Note that enddates are not included in time spans.
   */
  Collection getOverlappingScheduleElements(long startTime, long endTime);
        
  /** @deprecated use getEncapsulatedScheduleElements(long, long)
   */
  Collection getEncapsulatedScheduleElements(Date startDate, Date endDate);
  /**
   * @return a sorted collection of schedule elements that are fully bound
   * or encapsulated by a date range.
   * Note that enddates are not included in time spans.
   */
  Collection getEncapsulatedScheduleElements(long startTime, long endTime);
        
  /** @deprecated use getStartTime() **/
  Date getStartDate();

  /** @deprecated use getEndTime() **/
  Date getEndDate();

} 
