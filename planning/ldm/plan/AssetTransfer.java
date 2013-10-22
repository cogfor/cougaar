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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.asset.Asset;

/** 
 * AssetTransfer Interface
 * An AssetTransfer is a type of PlanElement
 * which represents an Asset being assigned to another Agent for use.
 * An AssetAssignment PlanningDirective is closely related
 *
 **/
public interface AssetTransfer extends PlanElement {
	
  /** 
   * Returns an Asset that has certain capabilities.
   * This Asset is being assigned to a agent for use.
   *
   * @return org.cougaar.planning.ldm.asset.Asset - a physical entity or agent that is assigned to a Agent.
   */		
  Asset getAsset();
 	
  /** 
   * Returns the Asset to which the asset is being assigned.
   * @return Asset representing the destination asset
   */ 	
  Asset getAssignee();
 
  /** 
   * Returns the Agent from which the asset was assigned.
   * @return MessageAddress representing the source of the asset
   */ 	
  MessageAddress getAssignor();
 
  /** 
   * Returns the Schedule for the "ownership" of the asset being transfered.
   *  @return Schedule
   */
  Schedule getSchedule();
  
  /** 
   * Checks to see if there is a potential conflict with another allocation
   * or asset transfer involving the same asset.
   * Will return true if there is a potential conflict.
   * Will return false if there is NOT a potential conflict.
   * @return boolean
   */
  boolean isPotentialConflict();
  
  /**
   * Checks to see if there is a potential conflict with the asset's
   * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
   * Will return true if there is a potential conflict.
   * @return boolean
   */
  boolean isAssetAvailabilityConflict();

  /**
   * request that the destination organization be re-contacted due 
   * to changes in the transferred asset (e.g. Organization predictor
   * has been modified.  The AssetTransfer object also should be 
   * publishChange()ed.
   **/
  void indicateAssetChange();

  /** infrastructure hook for resetting AssetChange flag **/
  void resetAssetChangeIndicated();

  /** is there an unprocessed asset change pending? **/
  boolean isAssetChangeIndicated();
  
  /** 
   * Return the Role this Asset is performing while transferred.
   *  @return Role
   */
  Role getRole();
}
