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

import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * A message sent by the {@link DemoControl} component to
 * set the execution time on a single node, or to acknowledge
 * another node's DemoControlMessage.
 */
public final class DemoControlMessage extends Message {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
	private final ExecutionTimer.Parameters p;
  private final boolean ack;

  public DemoControlMessage(
      MessageAddress source,
      MessageAddress target,
      UID uid,
      ExecutionTimer.Parameters p,
      boolean ack) {
    super(source, target);
    this.uid = uid;
    this.p = p;
    this.ack = ack;
    String s =
      (source == null ? "source" :
       target == null ? "target" :
       uid == null ? "uid" :
       (!ack && p == null) ? "time-parameters (non-ack)" :
       null);
    if (s != null) {
      throw new IllegalArgumentException("null "+s);
    }
  }

  public UID getUID() {
    return uid;
  }

  public ExecutionTimer.Parameters getParameters() {
    return p;
  }

  public boolean isAck() {
    return ack;
  }

  @Override
public String toString() {
    return 
      "(DemoControlMessage "+(ack ? "ACK" : "SET")+
      " source="+getOriginator()+" target="+getTarget()+
      " uid="+getUID()+
      " parameters="+getParameters()+")";
  }
}
