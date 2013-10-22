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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.asset.Asset;
 

/** 
 * Skeleton implementation of AssetAssignment
 */
public class AssetAssignmentImpl extends PlanningDirectiveImpl
  implements AssetAssignment, NewAssetAssignment
{
		
  private transient Asset assignedAsset;  // changed to transient : Persistence
  private transient Asset assigneeAsset;
  private Schedule assignSchedule;
  private byte _kind = NEW;

  //no-arg constructor
  public AssetAssignmentImpl () {
    super();
  }
		
  //constructor that takes one Asset
  public AssetAssignmentImpl (Asset a) {
    assignedAsset = a;
  }
		
  //constructor that takes multiple Assets
  /*
  public AssetAssignmentImpl (Enumeration as) {
    assignedAssets = new Vector();
    while (as.hasMoreElements()) {
      Asset asset = (Asset) as.nextElement();
      assignedAssets.addElement(asset);
    }
  }
  */
		
  //constructor that takes the Asset, the plan, the schedule
  // the source agent and the destination asset
  public AssetAssignmentImpl (Asset as, Plan p, Schedule s, 
                              MessageAddress sc, Asset da) {
    assignedAsset = as;
    super.setPlan(p);
    assignSchedule = s;
    super.setSource(sc);

    assigneeAsset = da;
    if (!assigneeAsset.hasClusterPG()) {
      throw new IllegalArgumentException("AssetAssignmentImpl: destination asset - " + assigneeAsset + " - does not have a ClusterPG");
    }
    super.setDestination(assigneeAsset.getClusterPG().getMessageAddress());
  }

		
  /** implementations of the AssetAssignment interface */
		
  public boolean isUpdate() { return _kind == UPDATE; }

  public boolean isRepeat() { return _kind == REPEAT; }

  public void setKind(byte value) { _kind = value; }

  /** @return Asset Asset that is being assigned */
  public Asset getAsset() {
    return assignedAsset;
  }
		
  public void setAsset(Asset newAssignedAsset) {
    assignedAsset = newAssignedAsset;
  }
			
  public Schedule getSchedule() {
    return assignSchedule;
  }
		
  public void setSchedule(Schedule sched) {
    assignSchedule = sched;
  }

  public Asset getAssignee() {
    return assigneeAsset;
  }
		
  public void setAssignee(Asset newAssigneeAsset) {
    assigneeAsset = newAssigneeAsset;
  }
  
  public String toString() {
    String scheduleDescr = "(Null AssignedSchedule)";
    if (assignSchedule != null) 
      scheduleDescr = assignSchedule.toString();
    String assetDescr = "(Null AssignedAsset)";
    if (assignedAsset != null)
      assetDescr = assignedAsset.toString();
    String toAssetDescr = "(Null AssigneeAsset)";
    if (assigneeAsset != null) 
      toAssetDescr = assigneeAsset.toString();
    String kind;
    switch (_kind) {
    case UPDATE: kind = "UPDATE"; break;
    case NEW: kind = "NEW"; break;
    case REPEAT: kind = "REPEAT"; break;
    default: kind = "BOGUS";
    }

    return "<AssetAssignment of " + assetDescr+", " + scheduleDescr + 
      " " + kind + " " + toAssetDescr + ">" + super.toString();
  }


  private void writeObject(ObjectOutputStream stream) throws IOException {

    /** ----------
     *    WRITE handlers common to Persistence and
     *    Network serialization.  NOte that these
     *    cannot be references to Persistable objects.
     *    defaultWriteObject() is likely to belong here...
     * ---------- **/
    stream.defaultWriteObject();
    
    stream.writeObject(assignedAsset);
    stream.writeObject(assigneeAsset);
  }



  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    assignedAsset = (Asset)stream.readObject();
    assigneeAsset = (Asset)stream.readObject();
  }
}
