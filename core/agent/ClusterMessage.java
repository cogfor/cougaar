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

package org.cougaar.core.agent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/**
 * The base class for blackboard messages, which pass through the agent's
 * {@link QueueHandler} before entering the blackboard.
 * <p>
 * This should really be abstract and only used by the
 * {@link org.cougaar.core.blackboard.DirectiveMessage} subclass.
 */
public class ClusterMessage 
  extends Message
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
protected long theIncarnationNumber;

  /**
   * @param s The MessageAddress of creator agent 
   * @param d The MessageAddress of the target agent
   */
  public ClusterMessage(MessageAddress s, MessageAddress d, long incarnationNumber) {
    super(s, d);
    theIncarnationNumber = incarnationNumber;
  }

  public ClusterMessage() {
    super();
  }

  // the agent's incarnation number, which is not typically used
  public long getIncarnationNumber() {
    return theIncarnationNumber;
  }
  public void setIncarnationNumber(long incarnationNumber) {
    theIncarnationNumber = incarnationNumber;
  }

  // for backwards compatibility, rename a couple methods:
  //   originator --> source
  //   target --> destination
  public final MessageAddress getSource() {
    return getOriginator();
  }
  public final MessageAddress getDestination() {
    return getTarget();
  }
  public final void setSource(MessageAddress asource) {
    setOriginator(asource);
  }
  public final void setDestination(MessageAddress adestination) {
    setTarget(adestination);
  }

  @Override
public String toString() {
    return "<ClusterMessage "+getSource()+" - "+getDestination()+">";
  }

  @Override
public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(theIncarnationNumber);
  }

  @Override
public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    super.readExternal(in);
    theIncarnationNumber = in.readLong();
  }
}
