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
 
 /** NewAssignedRelationshipElement extends AssignedRelationshipElement and
   * provides setter methods for building valid AssignedRelationshipElement 
   * objects.
   *
   *
   **/
 	 
public interface NewAssignedRelationshipElement extends AssignedRelationshipElement, 
  NewScheduleElement {
 	
  /** Set the string identifier for the Asset mapping to HasRelationships A in
   * the associated relationship
   * @param itemID String
   **/ 
   void setItemIDA(String itemID);

  /** Set the string identifier for the Asset mapping to HasRelationships A in
   * the associated relationship
   * @param asset Asset 
   **/ 
   void setItemIDA(Asset asset);

  /** Set the string identifier for the Asset mapping to HasRelationships B in
   * the associated relationship
   * @param itemID String
   **/ 
   void setItemIDB(String itemID);

  /** Set the string identifier for the Asset mapping to HasRelationships B in
   * the associated relationship
   * @param asset Asset
   **/ 
   void setItemIDB(Asset asset);

  /** Set the Role for the Asset identified by itemIDA
   * @param role Role
   **/
   void setRoleA(Role role);

  /** Set the Role for the Asset identified by itemIDB
   * @param role Role
   **/
   void setRoleB(Role role);
}


