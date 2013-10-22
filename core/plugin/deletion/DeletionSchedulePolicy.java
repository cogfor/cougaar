/* 
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.plugin.deletion;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A time-based deletion policy.
 * <p> 
 * Inner class specifies the policy for when deletions should
 * occur. This consists of a periodic element plus ad hoc
 * elements. The policy may be modified to add or remove ad hoc
 * times as well as altering the periodic schedule. Don't forget
 * to publishChange the policy after making modifications.
 */
public class DeletionSchedulePolicy implements Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private SortedSet deletionTimes = new TreeSet();
    private long period;
    private long phase;
    
    public DeletionSchedulePolicy(long period, long phase) {
      setPeriodicSchedule(period, phase);
    }
  
    public synchronized void setPeriodicSchedule(long period, long phase)
    {
      this.period = period;
      this.phase = phase;
    }
  
    public long getDeletionPhase() {
        return phase;
    }
  
    public long getDeletionPeriod() {
      return period;
    }
  
    public synchronized void addDeletionTime(long time) {
        deletionTimes.add(new Long(time));
    }
  
    public synchronized void removeDeletionTime(long time) {
        deletionTimes.remove(new Long(time));
    }
  
    synchronized long getNextDeletionTime(long now) {
        long deletionPhase = getDeletionPhase();
        long deletionPeriod = getDeletionPeriod();
        long ivn = (now - deletionPhase) / deletionPeriod;
        long nextAlarm = (ivn + 1) * deletionPeriod + deletionPhase;
        SortedSet oldTimes = deletionTimes.headSet(new Long(now));
        deletionTimes.removeAll(oldTimes);
        if (!deletionTimes.isEmpty()) {
            Long first = (Long) deletionTimes.first();
            long adHoc = first.longValue();
            if (adHoc < nextAlarm) {
                nextAlarm = adHoc;
                deletionTimes.remove(first);
            }
        }
        return nextAlarm;
    }
}

