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

import java.util.Date;

import org.cougaar.util.TimeSpan;

/**
 * A ScheduleElement is an encapsulation of spatio-temporal relationships.
 * Current thought is to bind up both time and space into a single
 * object which may then be queried in various ways to test for
 * overlaps, inclusion, exclusion, etc with other schedules.
 *
 *
 **/

public interface ScheduleElement 
  extends TimeSpan
{
	
  /** Start date is a millisecond-precision, inclusive time of start.
   * @return Date Start time for the task 
   **/
  Date getStartDate();
	
  /** End Date is millisecond-precision, <em>exclusive</em> time of end.
   * @return Date End time for the task 
   **/
  Date getEndDate();
	
  /** is the Date on or after the start time and strictly before the end time?
   *  @return boolean whether the date is included in this time interval.  
   **/
  boolean included(Date date);
	
  /** is the time on or after the start time and strictly before the end time?
   * @return boolean whether the time is included in this time interval 
   **/
  boolean included(long time);

  /** Does the scheduleelement overlap (not merely abut) the schedule?
   * @return boolean whether schedules overlap 
   **/
  boolean overlapSchedule(ScheduleElement scheduleelement);

  /** Does the scheduleElement meet/abut the schedule?
   **/
  boolean abutSchedule(ScheduleElement scheduleelement);

} 
