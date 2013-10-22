/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.mobility;

import org.cougaar.core.mts.MessageAddress;

/**
 * A ticket requesting agent removal.
 */
public final class RemoveTicket extends AbstractTicket {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final Object id;
  private final MessageAddress mobileAgent;
  private final MessageAddress destNode;

  public RemoveTicket(
      Object id,
      MessageAddress mobileAgent,
      MessageAddress destNode) {
    this.id = id;
    this.mobileAgent = mobileAgent;
    this.destNode = destNode;
  }

  /**
   * An optional identifier for this removeticket instance.
   * <p>
   * The identifier <u>must</u> be serializable and should be
   * immutable.   A UID is a good identifier.
   */
  public Object getIdentifier() {
    return id;
  }

  /**
   * The agent to be moved.
   * <p>
   * An agent can only pass a RemoveTicket to "dispatch(..)" if
   * the agent <i>is</i> the one moving.  Aside from this
   * sanity check, tagging the ticket with the agent address
   * aids debugging.
   * <p>
   * If the agent is null then the caller of the 
   * MobilityService is assumed.
   */
  public MessageAddress getMobileAgent() {
    return mobileAgent;
  }

  /**
   * Destination node for the mobile agent.
   * <p>
   * If the destination node is null then the agent should 
   * remain at its current node.  This is is useful for
   * a "force-restart".
   */
  public MessageAddress getDestinationNode() {
    return destNode;
  }

  @Override
public int hashCode() {
    return 
      (((id != null) ? id.hashCode() : 17) ^
       ((mobileAgent != null) ? mobileAgent.hashCode() : 53));
  }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RemoveTicket)) {
      return false;
    } else {
      RemoveTicket t = (RemoveTicket) o;
      return
	((id == null) ? 
	 (t.id == null) :
	 (id.equals(t.id))) &&
	((mobileAgent == null) ? 
	 (t.mobileAgent == null) :
	 (mobileAgent.equals(t.mobileAgent))) &&
	((destNode == null) ? 
	 (t.destNode == null) :
	 (destNode.equals(t.destNode)));
    }
  }
  
  @Override
public String toString() {
    // cache?
    return 
      "Remove "+
      ((id != null) ? 
       (id) :
       (" unidentified "))+
      " of "+
      ((mobileAgent != null) ? 
       "agent \""+mobileAgent+"\"" :
       "this agent")+
      " from "+
      ((destNode != null) ? 
       "node \""+destNode+"\"" :
       "this node");
  }
}
