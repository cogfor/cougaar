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

import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.TimeSpan;

/**
 * A RelationshipImpl is the encapsulation of a time phased relationship
 *
 **/

public class RelationshipImpl extends ScheduleElementImpl 
  implements Relationship { 

  private Role myRoleA; 
  private HasRelationships myA;
  private Role myRoleB;
  private HasRelationships myB;

  /** no-arg constructor */
  public RelationshipImpl() {
    super();
  }

   /** constructor for factory use that takes the start, end, role, 
    *  direct and indirect objects 
    **/
  public RelationshipImpl(TimeSpan timeSpan, 
                          Role role1, HasRelationships object1, 
                          HasRelationships object2) {
    this(timeSpan.getStartTime(), timeSpan.getEndTime(), role1, object1, 
         object2);
  }

   /** constructor for factory use that takes the start, end, role, 
    *  direct and indirect objects 
    **/
  public RelationshipImpl(long startTime, long endTime , 
                          Role role1, HasRelationships object1, 
                          HasRelationships object2) {
    super(startTime, endTime);
    
    Role role2 = role1.getConverse();

    // Normalize on roles so that we don't end up with relationships which
    // differ only in the A/B ordering, i.e. 
    // rel1.A == rel2.B && rel1.roleA == rel2.roleB &&
    // rel2.A == rel1.B && rel2.roleA == rel1.roleB
    if (role1.getName().compareTo(role2.getName()) < 0) {
      myRoleA = role1;
      myA = object1;
      myRoleB = role2;
      myB = object2;
    } else {
      myRoleA = role2;
      myA = object2;
      myRoleB = role1;
      myB = object1;
    }
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

    if (!(object instanceof Relationship)) {
      return false;
    }

    Relationship other = (Relationship)object;

    
    return (getRoleA().equals(other.getRoleA()) &&
            getA().equals(other.getA()) &&
            getRoleB().equals(other.getRoleB()) &&
            getB().equals(other.getB()) && 
            getStartTime() == other.getStartTime() &&
            getEndTime() == other.getEndTime());
  }
 
  public int hashCode() {
    return (int) (getStartTime() + (getEndTime() * 1000) + getA().hashCode() + 
		  getRoleA().hashCode() + getB().hashCode() + 
		  getRoleB().hashCode());
  }
    
  /** Role performed by HasRelationship A
   * @return Role which HasRelationships A performs
   */
  public Role getRoleA() {
    return myRoleA;
  }

  /** Role performed  by HasRelationships B
   * @return Role which HasRelationships B performs
   */
  public Role getRoleB() {
    return myRoleB;
  }

  /**
   * @return HasRelationships A
   */
  public HasRelationships getA() {
    return myA;
  }
  
  /**
   * @return HasRelationships B
   */
  public HasRelationships getB() {
    return myB;
  }

  public String toString() {
    String AStr;
    if (getA() instanceof Asset) {
      AStr = 
        ((Asset) getA()).getItemIdentificationPG().getNomenclature();
    } else if (getA() instanceof  UniqueObject) {
      AStr = ((UniqueObject)getA()).getUID().toString();
    } else {
      AStr = getA().toString();
    }

    String BStr;
    if (getB() instanceof Asset) {
      BStr = 
        ((Asset) getB()).getItemIdentificationPG().getNomenclature();
    } else if (getB() instanceof UniqueObject) {
      BStr = ((UniqueObject)getB()).getUID().toString();
    } else {
      BStr = getB().toString();
    }

    return "<start:" + new Date(getStartTime()) + 
      " end:" + new Date(getEndTime()) + 
      " roleA:" + getRoleA()+
      " A:" + AStr +
      " roleB:" + getRoleB()+
      " B:" + BStr + ">";
  }
}









