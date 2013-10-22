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
 * Base class for ScheduleElement.  Millisecond accuracy times are stored
 * in seconds since java epoch start, GMT.  Start time is closed, end time is open.
 */

public class ScheduleElementImpl 
  implements ScheduleElement, NewScheduleElement, java.io.Serializable
{
  protected long stime = TimeSpan.MIN_VALUE;
  protected long etime = TimeSpan.MAX_VALUE;

  /** no-arg constructor */
  public ScheduleElementImpl () { }
        
  /** constructor for factory use that takes the start and end dates */
  public ScheduleElementImpl(Date start, Date end) {
    stime = start.getTime();
    etime = end.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }

  /** constructor for factory use that takes the start and end dates */
  public ScheduleElementImpl(long start, long end) {
    stime = start;
    etime = end;
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
        
  public Date getStartDate() { return new Date(stime); }
  public long getStartTime() { return stime; }
  public Date getEndDate() { return new Date(etime); }
  public long getEndTime() { return etime; }
        
        
  /** @return boolean whether the date is included in this schedule */
  public boolean included(Date date) {
    return included(date.getTime());
  }
        
  public boolean included(long time) {
    return ( (time >= stime) && (time < etime) );
  }

  /** @return boolean whether schedules overlap */
  public boolean overlapSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime < etime &&
             tetime > stime );
  }
        
  public boolean abutSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime == etime ||
             tetime == stime );
  }

  // NewSchedule interface implementations
        
  /** @param startdate Set Start time for the task */
  public void setStartDate(Date startdate) {
    stime = startdate.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
  
  public void setStartTime(long t) { 
    stime = t; 
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }

                
  /** @param enddate Set End time for the task */
  public void setEndDate(Date enddate) {
    etime = enddate.getTime();
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
  public void setEndTime(long t) { 
    etime = t; 
    if (stime==etime) 
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }
        
  public void setStartEndTimes(long starttime, long endtime) {
    stime = starttime;
    etime = endtime;
    if (etime<=stime)
      throw new IllegalArgumentException("ScheduleElements must span a non-zero amount of time.");
  }


  public String toString() {
    return "<"+getStartDate()+"-"+getEndDate()+">";
  }

}
