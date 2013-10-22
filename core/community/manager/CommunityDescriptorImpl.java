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

package org.cougaar.community.manager;

import org.cougaar.community.CommunityDescriptor;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.community.Community;
import org.cougaar.core.util.UID;

/**
 * Implementation of CommunityDescriptor interface.  The CommunityDescriptor
 * wraps an org.cougaar.core.service.community.Community instance for
 * transmission to remote agents using blackboard relay.
 **/
public class CommunityDescriptorImpl
  implements CommunityDescriptor, java.io.Serializable, NotPersistable {

  protected MessageAddress source;
  protected Community community;
  protected UID uid;

  /**
   * Constructor.
   * @param source MessageAddress of sender
   * @param community Associated Community
   * @param uid Unique identifier
   */
  public CommunityDescriptorImpl(MessageAddress source,
                                 Community community,
                                 UID uid) {
    this.source = source;
    this.community = community;
    this.uid = uid;
  }

  /**
   * Gets the community.
   * @return Community
   */
  public Community getCommunity() {
    return community;
  }

  public String getName() {
    return community.getName();
  }

  //
  // Relay.Target Interface methods
  //
  public Object getResponse() {
    return null;
  }

  public MessageAddress getSource() {
    return source;
  }

  public int updateContent(Object content, Relay.Token token) {
    CommunityDescriptor cd = (CommunityDescriptorImpl)content;
    community = cd.getCommunity();
    return Relay.CONTENT_CHANGE;
  }

  public String toXML() {
    return community.toXml();
  }

  //
  // UniqueObject Interface methods
  //
  public void setUID(UID uid) {
    if (uid != null) {
      RuntimeException rt = new RuntimeException("Attempt to call setUID() more than once.");
      throw rt;
    }
    this.uid = uid;
  }
  public UID getUID() {
    return this.uid;
  }

  /**
   * Returns a string representation
   * @return String - a string representation
   **/
  public String toString() {
    return "CommunityDescriptor: community=" + community.getName();
  }
}
