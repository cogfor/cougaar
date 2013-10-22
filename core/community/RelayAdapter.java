/*
 * <copyright>
 *  
 *  Copyright 2001-2004 Mobile Intelligence Corp
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

package org.cougaar.community;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.util.UID;

/**
 * Generic wrapper for blackboard objects to be sent to remote agents
 * using Realay.
 */

public class RelayAdapter implements Relay.Source {
  protected Set interestedAgents = Collections.synchronizedSet(new HashSet());
  protected Set myTargetSet = new HashSet();
  MessageAddress source;
  Object content;
  UID myUID;
  Object resp;
  transient CommunityResponseListener crl;

  public RelayAdapter(MessageAddress source,
               Object content,
               UID uid) {
    this.source = source;
    this.content = content;
    this.myUID = uid;
  }
  public Object getContent() {
    return content;
  }

  /*
   * Get a factory for creating the target.
   */
  public TargetFactory getTargetFactory() {
    return null;
  }

  public Object getResponse() {
    return resp;
  }

  /*
   * Set the response that was sent from a target.
   */
  public int updateResponse(MessageAddress target, Object response) {
    this.resp = response;
    return Relay.RESPONSE_CHANGE;
  }

  /**
   * Get all the addresses of the target agents to which this Relay
   * should be sent.
   * @return Set of MessageAddress objects
   **/
  public Set getTargets() {
    if (myTargetSet == null) {
      return Collections.EMPTY_SET;
    } else {
      return Collections.unmodifiableSet(myTargetSet);
    }
  }
  /**
   * Add a target destination.
   * @param target MessageAddress of agent to add to targets
   **/
  public void addTarget(MessageAddress target) {
    if (myTargetSet != null) {
      myTargetSet.add(target);
    }
  }

  public Set getInterestedAgents() {
    return interestedAgents;
  }

 public UID getUID() {
    return myUID;
  }

  /* set the UID of a UniqueObject.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the UID was already set.
   */

  public void setUID(UID uid) {
    if (myUID != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }
    myUID = uid;
  }

  public void setCommunityResponseListener(CommunityResponseListener crl) {
    this.crl = crl;
  }
  
  public CommunityResponseListener getCommunityResponseListener() {
    return crl;
  }
  
  public static String targetsToString(Relay.Source rs) {
    StringBuffer sb = new StringBuffer("[");
    if (rs != null) {
      for (Iterator it = rs.getTargets().iterator(); it.hasNext(); ) {
        MessageAddress addr = (MessageAddress)it.next();
        if (addr != null) {
          sb.append(addr.toString());
          if (it.hasNext())
            sb.append(",");
        }
      }
    }
    sb.append("]");
    return sb.toString();
  }

  public String toString() {
    return "RelayAdapter:" +
        " uid=" + myUID +
        " source=" + source +
        " content=" + content +
        " targets=" + targetsToString(this);
  }

}

