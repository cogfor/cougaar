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


/**
 * A LocationRangeScheduleElement is an encapsulation of temporal relationships
 * and locations over that interval.
 *
 **/

public class LocationRangeScheduleElementImpl extends ScheduleElementImpl
  implements LocationRangeScheduleElement, NewLocationRangeScheduleElement {
        
  private Location sloc, eloc;
        
  /** no-arg constructor */
  public LocationRangeScheduleElementImpl () {
    super();
  }
        
  /** constructor for factory use that takes the start and end dates and a
   * start and end locations*/
  public LocationRangeScheduleElementImpl(Date start, Date end, Location sl, Location el) {
    super(start, end);
    sloc = sl;
    eloc = el;
  }
        
  /** @return Location start location related to this schedule */
  public Location getStartLocation() {
    return sloc;
  }
        
  /** @return Location end location related to this schedule */
  public Location getEndLocation() {
    return eloc;
  }
                
        
  // NewLocationRangeScheduleElement interface implementations
        
  /** @param aStartLocation set the start location related to this schedule */
  public void setStartLocation(Location aStartLocation) {
    sloc = aStartLocation;
  }
        
  /** @param anEndLocation set the end location related to this schedule */
  public void setEndLocation(Location anEndLocation) {
    eloc = anEndLocation;
  }

} 
