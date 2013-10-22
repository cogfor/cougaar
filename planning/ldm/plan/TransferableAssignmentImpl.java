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

/** 
 * PlanningDirective message containing a Transferable 
 **/

public class TransferableAssignmentImpl 
  extends PlanningDirectiveImpl
  implements TransferableAssignment, NewTransferableAssignment
{
  private transient Transferable assignedTransferable;

  public TransferableAssignmentImpl() {
    super();
  }

  public TransferableAssignmentImpl(Transferable transferable) {
    assignedTransferable = transferable;
  }

  public TransferableAssignmentImpl(Transferable transferable, MessageAddress src, 
			      MessageAddress dest) {
    assignedTransferable = transferable;
    super.setSource(src);
    super.setDestination(dest);
  }

  /** implementations of the TransferableAssignment interface */
		
  /** @return transferable that has beeen assigned */
  public Transferable getTransferable() {
    return assignedTransferable;
  }

  /** implementation methods for the NewTransferableAssignment interface */
  /** @param newtransferable sets the transferable being assigned */
  public void setTransferable(Transferable newtransferable) {
    assignedTransferable = newtransferable;
  }


  public String toString() {
    String transferableDescr = "(Null AssignedTransferable)";
    if( assignedTransferable != null ) transferableDescr = assignedTransferable.toString();

    return "<TransferableAssignment "+transferableDescr+", " + ">" + super.toString();
  }


  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(assignedTransferable);
  }



  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();
    assignedTransferable = (Transferable)stream.readObject();
  }



}
