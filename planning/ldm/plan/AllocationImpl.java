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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.log.Logger;


/** AllocationImpl.java
 * Implementation for allocation
 */
 
public class AllocationImpl extends PlanElementImpl 
  implements Allocation, RoleScheduleConflicts, AllocationforCollections
{

  private transient Asset asset;   // changed to transient : Persistence

  private transient UID allocTaskUID = null; // changed to transient : Persistence
  private transient boolean allocTaskDeleted = false;
  private transient boolean potentialconflict = false;
  private transient boolean stale = false;
  private transient boolean assetavailconflict = false;
  private transient boolean checkconflict = false;
  private Role theRole;

   public AllocationImpl() {}

  /* Constructor that takes the Asset, and assumes that there is not a good
   * estimate of the result for now.
   * @param p
   * @param t
   * @param a
   */
  public AllocationImpl(Plan p, Task t, Asset a) {
    super(p, t);
    setAsset(a);
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd now.
  }
  
  /* Constructor that takes the Asset, and an initial estimated result
   * @param p
   * @param t
   * @param a
   * @param estimatedresult
   */
  public AllocationImpl(Plan p, Task t, Asset a, AllocationResult estimatedresult, Role aRole) {
    super(p, t);
    setAsset(a);
    estAR = estimatedresult;
    this.theRole = aRole;
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd
  }
  
   
  /** Set the estimated allocation result so that a notification will
    * propagate up another level.
    * @param estimatedresult
    */
  public void setEstimatedResult(AllocationResult estimatedresult) {
    super.setEstimatedResult(estimatedresult);
    setCheckConflicts(true);
  }
 	
  /**
   * @return Asset - Asset associated with this allocation/subtask
   */
  public Asset getAsset() {
    return asset;
  }
  
  /**
    * @return boolean - true if there is a potential conflict with another 
    * allocation to the same asset.
    * @deprecated
    */
  public boolean isPotentialConflict() {
    //throw new RuntimeException("isPotentialConflict is temporarily deprecated.");
    System.err.println("AllocationImpl::isPotentialConflict() - a temporarily deprecated method - has been called");    
    return potentialconflict;
  }
  
  /** Checks to see if there is a potential conflict with the asset's
    * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
    * Will return true if there is a potential conflict.
    * @return boolean
    * @deprecated
    */
  public boolean isAssetAvailabilityConflict() {
//   throw new RuntimeException("isAssetAvailabilityConflict is temporarily deprecated.");
    System.err.println("AllocationImpl::isAssetAvailabilityConflict() - a temporarily deprecated method - has been called");    
   return assetavailconflict;
  }
  
  /** Check to see if this allocation is Stale and needs to be revisited.
    * Will return true if it is stale (needs to be revisted)
    * @return boolean
    */
  public boolean isStale() {
    return stale;
  }
  
  /** Return the Role that the Asset is performing while executing this PlanElement (Task).
   * @return Role
   **/
  public Role getRole() {
    return theRole;
  }
  
  /** Set the stale flag.  Usualy used by Trigger actions.
    * @param stalestate
    */
  public void setStale(boolean stalestate) {
    stale = stalestate;
  }

  /**
   * @param anAsset - set Asset associated with this allocation/subtask
   */
  private void setAsset(Asset anAsset) {
    asset = anAsset;
  }
  
 	
  /* INFRASTRUCTURE ONLY */
  public void setPotentialConflict(boolean conflict) {
    potentialconflict = conflict;
  }
  /* INFRASTRUCTURE ONLY */
  public void setAssetAvailabilityConflict(boolean availconflict) {
    assetavailconflict = availconflict;
  }
  /* INFRASTRUCTURE ONLY */
  public void setCheckConflicts(boolean check) {
    checkconflict = check;
  }
  /* INFRASTRUCTURE ONLY */
  public boolean checkConflicts() {
    return checkconflict;
  }
  

  // ActiveSubscriptionObject
  public void addingToBlackboard(Subscriber s, boolean commit) {
    super.addingToBlackboard(s, commit);
    Blackboard.getTracker().checkpoint(commit,asset,"getRoleSchedule");
    if (!commit) return;

    // check for conflicts

    addToRoleSchedule(asset);
  }
  public void changingInBlackboard(Subscriber s, boolean commit) {
    super.changingInBlackboard(s, commit);
    // check for conflicts
  }
  public void removingFromBlackboard(Subscriber s, boolean commit) {
    super.removingFromBlackboard(s, commit);
    Blackboard.getTracker().checkpoint(commit,asset,"getRoleSchedule");
    if (!commit) return;

    // check for conflicts

    removeFromRoleSchedule(asset);
  }

  public UID getAllocationTaskUID() {
    return allocTaskUID;
  }

  public void setAllocationTask(Task t) {
    setAllocationTaskUID(t.getUID());
  }

  public void setAllocationTaskUID(UID uid) {
    allocTaskUID = uid;
    allocTaskDeleted = false;
  }

  public boolean isAllocationTaskDeleted() {
    return allocTaskDeleted;
  }

  public void setAllocationTaskDeleted(boolean newDeleted) {
    allocTaskDeleted = newDeleted;
  }
	
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(asset);
    stream.writeObject(allocTaskUID);
    stream.writeBoolean(allocTaskDeleted);
 }



  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    /** ----------
      *    READ handlers common to Persistence and
      *    Network serialization.  NOte that these
      *    cannot be references to Persistable objects.
      *    defaultReadObject() is likely to belong here...
      * ---------- **/
    stream.defaultReadObject();
    asset = (Asset)stream.readObject();
    allocTaskUID = (UID)stream.readObject();
    allocTaskDeleted = stream.readBoolean();
  }


  public void postRehydration(Logger logger) {
    super.postRehydration(logger);
    fixAsset(getAsset());
  }

  public String toString() {
    return "[Allocation of " + getTask().getUID() + " to "+asset+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("asset", AllocationImpl.class, "getAsset", null));
    c.add(new PropertyDescriptor("role", AllocationImpl.class, "getRole", null));
    c.add(new PropertyDescriptor("allocationTaskUID", AllocationImpl.class, "getAllocationTaskUID", null));
    c.add(new PropertyDescriptor("stale", AllocationImpl.class, "isStale", null));
  }
}
