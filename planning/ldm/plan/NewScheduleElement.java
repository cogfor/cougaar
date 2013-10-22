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
 
 /** NewScheduleElement extends ScheduleElement and provides
   * setter methods for building valid ScheduleElement objects.
   *
   *
   **/
 	 
public interface NewScheduleElement extends ScheduleElement {
 	
  /** @param startdate Set Start time for the time interval */
  void setStartDate(Date startdate);
	
  /** @param starttime Set Start time for the time interval */
  void setStartTime(long starttime);

  /** Note that end time is the <em>open</em> end of the interval.
   * @param enddate Set End time for the time interval 
   **/
  void setEndDate(Date enddate);
	
  /** Note that end time is the <em>open</em> end of the interval.
   * @param endtime Set End time for the time interval 
   **/
  void setEndTime(long endtime);

  /** One shot setter
   * @param starttime Set Start time for the time interval 
   * @param endtime Set End time for the time interval. 
   * Note that end time is the <em>open</em> end of the interval.
   */
  void setStartEndTimes(long starttime, long endtime);
}
