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

/** AssetRescind implementation
 * AssetRescind allows a asset to be rescinded from the Plan. 
 **/


public class AssetRescindImpl extends PlanningDirectiveImpl
  implements
  AssetRescind,
  NewAssetRescind
{

  private transient Asset rescindedAsset;
  private transient Asset rescindeeAsset;
  private Schedule rescindedSchedule;
        
  public AssetRescindImpl(MessageAddress src, MessageAddress dest, Plan plan,
                          Asset rescindedAsset, Asset rescindeeAsset, 
                          Schedule rescindSchedule) {
    setSource(src);
    setDestination(dest);
    super.setPlan(plan);
    
    setAsset(rescindedAsset);
    setRescindee(rescindeeAsset);
    setSchedule(rescindSchedule);
  }

  /**
   * Returns the asset to be rescinded
   * @return Asset
   **/

  public Asset getAsset() {
    return rescindedAsset;
  }
    
  /**
   * Sets the asset to be rescinded
   **/

  public void setAsset(Asset asset) {
    rescindedAsset = asset;
  }

  public Asset getRescindee() {
    return rescindeeAsset;
  }
		
  public void setRescindee(Asset newRescindeeAsset) {
    rescindeeAsset = newRescindeeAsset;
  }

  public Schedule getSchedule() {
    return rescindedSchedule;
  }
		
  public void setSchedule(Schedule sched) {
    rescindedSchedule = sched;
  }
       
  public String toString() {
    String scheduleDescr = "(Null RescindedSchedule)";
    if (rescindedSchedule != null) 
      scheduleDescr = rescindedSchedule.toString();
    String assetDescr = "(Null RescindedAsset)";
    if (rescindedAsset != null)
      assetDescr = rescindedAsset.toString();
    String toAssetDescr = "(Null RescindeeAsset)";
    if (rescindeeAsset != null) 
      toAssetDescr = rescindeeAsset.toString();


    return "<AssetRescind "+assetDescr+", "+ scheduleDescr + 
      " to " + toAssetDescr + ">" + super.toString();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {

    /** ----------
     *    WRITE handlers common to Persistence and
     *    Network serialization.  NOte that these
     *    cannot be references to Persistable objects.
     *    defaultWriteObject() is likely to belong here...
     * ---------- **/
    stream.defaultWriteObject();
    
    stream.writeObject(rescindedAsset);
    stream.writeObject(rescindeeAsset);
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    rescindedAsset = (Asset)stream.readObject();
    rescindeeAsset = (Asset)stream.readObject();
  }

}
