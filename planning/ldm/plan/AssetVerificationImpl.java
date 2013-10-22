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

import org.cougaar.planning.ldm.asset.Asset;


/** An implementation of AssetVerification
 */
public class AssetVerificationImpl extends PlanningDirectiveImpl
  implements AssetVerification, NewAssetVerification
{
  private transient Asset myAsset;
  private transient Asset myAssignee;
  private Schedule mySchedule;
                
  //no-arg constructor
  public AssetVerificationImpl() {
  }

  public AssetVerificationImpl(Asset asset, Asset assignee, Schedule schedule) {
    setAsset(asset);
    setAssignee(assignee);
    setSchedule(schedule);
  }


  /** implementation of the AssetVerification interface */

  /** 
   * Returns the asset the verification is in reference to.
   * @return asset
   **/
  public Asset getAsset() {
    return myAsset;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the asset the notification is in reference to.
   * @param asset Asset
   **/
                
  public void setAsset(Asset asset) {
    myAsset = asset;
  }


  /** implementation of the AssetVerification interface */

  /** 
   * Returns the asset the verification is in reference to.
   * @return asset
   **/
  public Asset getAssignee() {
    return myAssignee;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the asset the notification is in reference to.
   **/
  public void setAssignee(Asset assignee) {
    myAssignee = assignee;
  }

  /** implementation of the AssetVerification interface */

  /** 
   * Returns the schedule to be verified
   * @return Schedule
   **/
  public Schedule getSchedule() {
    return mySchedule;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the schedule to be verified
   * @param schedule Schedule
   **/
  public void setSchedule(Schedule schedule) {
    mySchedule = schedule;
  }

  
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(myAsset);
    stream.writeObject(myAssignee);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();

    myAsset = (Asset)stream.readObject();
    myAssignee = (Asset)stream.readObject();
  }

  public String toString() {
    return "<AssetVerification for asset " + myAsset + 
      " assigned to " + myAssignee + ">" + mySchedule.toString();
  }
}



