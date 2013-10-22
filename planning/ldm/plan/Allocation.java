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

/** 
 * Allocation Interface
 * An Allocation is a type of PlanElement
 * which represents the Asset that will complete
 * the Task.
 */
public interface Allocation extends PlanElement {
	
  /** Returns an Asset that has certain capabilities.
   * This Asset is assigned to complete the Task that is
   * matched with the Allocation in the PlanElement.
   *
   * @return Asset - a physical entity or agent that is assigned to perform the Task.
   **/
  org.cougaar.planning.ldm.asset.Asset getAsset();
   
  /** Checks to see if there is a potential conflict with another allocation
   * or asset transfer involving the same asset.
   * Will return true if there is a potential conflict.
   * Will return false if there is NOT a potential conflict.
   * @return boolean
   */
  boolean isPotentialConflict();
  
  /** Checks to see if there is a potential conflict with the asset's
   * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
   * Will return true if there is a potential conflict.
   * @return boolean
   */
  boolean isAssetAvailabilityConflict();
  
  /** Check to see if this allocation is Stale and needs to be revisited.
   * Will return true if it is stale (needs to be revisted)
   * @return boolean
   */
  boolean isStale();
  
  /** Set the stale flag.  Usualy used by Trigger actions.
   * @param stalestate
   */
  void setStale(boolean stalestate);
  
  /** Return the Role that the Asset is performing while executing
   * this PlanElement (Task).  
   *
   * @return Role
   **/
  Role getRole();
}
