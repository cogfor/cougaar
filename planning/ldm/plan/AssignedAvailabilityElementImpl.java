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

import org.cougaar.planning.ldm.asset.Asset;

/**
 * AssignedAvailabilityElement represents the availability to a specific asset
 * over a time interval.
 *
 **/


public class AssignedAvailabilityElementImpl extends ScheduleElementImpl
  implements NewAssignedAvailabilityElement {

  private Asset myAssignee;

  /** constructor for factory use */
  public AssignedAvailabilityElementImpl() {
    super();
    setAssignee(null);
  }

  /** constructor for factory use that takes the start, end times & the
   *  assignee asset
  **/
  public AssignedAvailabilityElementImpl(Asset assignee, long start, long end) {
    super(start, end);
    setAssignee(assignee);
  }
        
  public Asset getAssignee() { 
    return myAssignee; 
  }

  public void setAssignee(Asset assignee) {
    myAssignee = assignee;
  }

  /** 
   * equals - performs field by field comparison
   *
   * @param object Object to compare
   * @return boolean if 'same' 
   */
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if (!(object instanceof AssignedAvailabilityElement)) {
      return false;
    }

    AssignedAvailabilityElement other = (AssignedAvailabilityElement)object;

    
    return (getAssignee().equals(other.getAssignee()) &&
            getStartTime() == other.getStartTime() &&
            getEndTime() == other.getEndTime());
  }
  
}
