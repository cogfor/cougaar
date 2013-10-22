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

/**
  * A Transferable Transfer should be used to transfer a Transferable object to
  * another agent (org asset).
  */
public class TransferableTransferImpl
  implements TransferableTransfer, NewTransferableTransfer, java.io.Serializable
{
  
  private Transferable thetransferable;
  private Asset theagent;
  
  /** no-arg constructor - use the setters in the NewTransferableTransfer Interface
    * to build a complete object
    */
  public TransferableTransferImpl() {
    super();
  }
  
  /** Simple constructor 
    * @param aTransferable - the Transferable being sent
    * @param anAsset - An Organization Asset representing the Agent that the Transferable is being sent to
    */
  public TransferableTransferImpl(Transferable aTransferable, Asset anAsset) {
    super();
    this.setTransferable(aTransferable);
    this.setAsset(anAsset);
  }
  
  /** The Transferable being sent
    * @return Transferable
    */
  public Transferable getTransferable() {
    return thetransferable;
  }
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Agent.
    * @return Asset
    */
  public Asset getAsset() {
    return theagent;
  }
  
  /** The Transferable being sent
    * @param aTransferable
    */
  public void setTransferable(Transferable aTransferable) {
    thetransferable = aTransferable;
  }
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Agent.
    * @param anAsset
    */
  public void setAsset(Asset anAsset) {
    // double check that this is an org asset for now
    if (anAsset.getClusterPG() != null) {
      theagent = anAsset;
    } else {
      throw new IllegalArgumentException("TransferableTransfer.setAsset(anAsset) expects an Asset with a clusterPG!");
    }
  }
  
}
