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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.log.Logger;

/**
 * Implementation for AssetTransfer
 */
public class AssetTransferImpl extends PlanElementImpl 
  implements AssetTransfer, RoleScheduleConflicts
{
 
  private transient Asset asset;     // changed to transient : Persistence
  private transient Asset assigneeAsset;
  private MessageAddress assignerAgent;
  private Schedule assetSchedule;
  private Role theRole;
  private transient boolean potentialconflict = false;
  private transient boolean assetavailconflict = false;
  private transient boolean checkconflict = false;

  private transient boolean isAssetChanged = false;

  //private String role = "RemoteAssignment";
  
  public AssetTransferImpl() {}

  /** Constructor that assumes there is not a good estimated result at this time.
   * @param p
   * @param t
   * @param a  The asset being transferred
   * @param s  The schedule representing the time frame that the asset is being transferred for.
   * @param to  The Agent that will receive this asset for use
   * @param from  The Agent that is provided this asset for use
   */
  public AssetTransferImpl(Plan p, Task t, Asset a, Schedule s, Asset to, MessageAddress from) {
    super(p, t);
    setAsset(a);
    setSchedule(s);
    setAssignee(to);
    setAssignor(from);
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done in publishAdd
  }
	
  
  /** Constructor 
   * @param p
   * @param t
   * @param a  The asset being transferred
   * @param s  The schedule representing the time frame that the asset is being transferred for.
   * @param to  The Agent that will receive this asset for use
   * @param from  The Agent that is provided this asset for use
   * @param estimatedresult
   * @param aRole
   */
  public AssetTransferImpl(Plan p, Task t, Asset a, Schedule s, Asset to, MessageAddress from, AllocationResult estimatedresult, Role aRole) {
    super(p, t);
    setAsset(a);
    setSchedule(s);
    setAssignee(to);
    setAssignor(from);
    setEstimatedResult(estimatedresult);
    this.theRole = aRole;
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd
  }
  
  
  //AssetTransfer interface implementations
  
  /**
   * @return Asset - Asset being assigned
   */
  public Asset getAsset() {
    return asset;
  }
  
  /**
   * @return Asset - The Asset to which the Asset is being assigned
   */
  public Asset getAssignee() {
    return assigneeAsset;
  }
  
  /** Returns the Agent that the asset is assigned from.
   * @return MessageAddress representing the source of the asset
   */
 	
  public MessageAddress getAssignor() {
    return assignerAgent;
  }
 
  /** Returns the Schedule for the "ownership" of the asset being transfered.
   *  @return Schedule
   */
  public Schedule getSchedule() {
    return assetSchedule;
  }
  
  public long getStartTime() {
    return (assetSchedule == null)?MIN_VALUE:assetSchedule.getStartTime();
  }

  public long getEndTime() {
    return (assetSchedule == null)?MAX_VALUE:assetSchedule.getEndTime();
  }


  /** Checks to see if there is a protential conflict with another allocation
   * to the same asset.
   * Will return true if there is a potential conflict.
   * Will return false if there is NOT a potential conflict.
   * @return boolean
   */
  public boolean isPotentialConflict() {
    throw new RuntimeException("isPotentialConflict is temporarily deprecated.");
    //return potentialconflict;
  }
  
  /** Checks to see if there is a potential conflict with the asset's
   * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
   * Will return true if there is a potential conflict.
   * @return boolean
   */
  public boolean isAssetAvailabilityConflict() {    
    throw new RuntimeException("isAssetAvailabilityConflict is temporarily deprecated.");
    //return assetavailconflict;
  }
  
  /** Return the Role this Asset is performing while transferred.
   *  @return Role
   **/
  public Role getRole() {
    return theRole;
  }
 	
 
  //NewAssetTransfer inteface implementations
 	
  /**
   * @param anAsset - set the Asset being assigned
   */
  private void setAsset(Asset anAsset) {
    asset = anAsset;
  }
  
  /**
   * @param toAsset - Set the Asset being assigned the Asset
   */
  private void setAssignee(Asset toAsset) {
    assigneeAsset = toAsset;
  }
  
  /** Sets the Agent that the asset is assigned from.
   * @param aAgent
   */
 	
  private void setAssignor(MessageAddress aAgent) {
    assignerAgent = aAgent;
  }
 
  /** Sets the Schedule for the "ownership" of the asset being transfered.
   *  @param aSchedule
   */
  private void setSchedule(Schedule aSchedule) {
    assetSchedule = aSchedule;
  }
  
   
  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   */
  public void setEstimatedResult(AllocationResult estimatedresult) {
    super.setEstimatedResult(estimatedresult);
    setCheckConflicts(true);
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
  
  synchronized public void indicateAssetChange() {
    isAssetChanged = true;
  }
  /** Infrastructure only: this actually should be locked
   * before calling this method so that we can tell if it
   * has changed again.
   **/
  synchronized public void resetAssetChangeIndicated() {
    isAssetChanged = false;
  }
  
  synchronized public boolean isAssetChangeIndicated() {
    return isAssetChanged;
  }

  // ActiveSubscriptionObject
  public void addingToBlackboard(Subscriber s, boolean commit) {
    super.addingToBlackboard(s, commit);
    Blackboard.getTracker().checkpoint(commit, asset,"getRoleSchedule");
    if (!commit) return;
    // check for conflicts.

    addToRoleSchedule(asset);
  }
  public void changingInBlackboard(Subscriber s, boolean commit) {
    super.changingInBlackboard(s, commit);
    // check for conflicts
  }
  public void removingFromBlackboard(Subscriber s, boolean commit) {
    super.removingFromBlackboard(s, commit);
    Blackboard.getTracker().checkpoint(commit, asset,"getRoleSchedule");
    if (!commit) return;

    removeFromRoleSchedule(asset);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(asset);
    stream.writeObject(assigneeAsset);
  }

 
  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();

    asset = (Asset)stream.readObject();
    assigneeAsset = (Asset)stream.readObject();
  }

  public void postRehydration(Logger logger) {
    super.postRehydration(logger);
    fixAsset(getAsset());
    fixAsset(getAssignee());
  }

  public String toString() {
    return "[Transfer of "+asset+" from "+assignerAgent+" to "+assigneeAsset+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("asset", AssetTransferImpl.class, "getAsset", null));
    c.add(new PropertyDescriptor("role", AssetTransferImpl.class, "getRole", null));
    c.add(new PropertyDescriptor("assignee", AssetTransferImpl.class, "getAssignee",null));
    c.add(new PropertyDescriptor("assignor", AssetTransferImpl.class, "getAssignor",null));
    c.add(new PropertyDescriptor("schedule", AssetTransferImpl.class, "getSchedule", null));
  }
}
