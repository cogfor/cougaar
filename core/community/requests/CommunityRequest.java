/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
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

package org.cougaar.community.requests;

import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Base class for community requests (i.e., Join, Leave, GetDescriptor, etc.).
 */
public class CommunityRequest implements java.io.Serializable, UniqueObject {

  public static final long NEVER = -1;
  public static final long DEFAULT_TIMEOUT = NEVER;

  private String communityName;
  private String requestType;
  private CommunityResponse resp;
  private long timeout = DEFAULT_TIMEOUT;
  private UID uid;

  /**
   * Default constructor.
   */
  public CommunityRequest() {
  }

  public CommunityRequest(String                    communityName,
                          UID                       uid,
                          long                      timeout) {
    this.communityName = communityName;
    this.timeout = timeout;
    this.uid = uid;
    String classname = this.getClass().getName();
    int lastSeparator = classname.lastIndexOf(".");
    requestType = (lastSeparator == -1)
                  ? classname
                  : classname.substring(lastSeparator+1,classname.length());
  }

  public CommunityRequest(String                    communityName,
                          UID                       uid) {
    this(communityName, uid, DEFAULT_TIMEOUT);
  }

  public String getCommunityName() {
    return communityName;
  }

  public String getRequestType() {
    return requestType;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setResponse(CommunityResponse resp) {
    this.resp = resp;
  }

  public CommunityResponse getResponse() {
    return resp;
  }

  public String toString() {
    return "request=" + getRequestType() +
           " community=" + getCommunityName() +
           " timeout=" + getTimeout() +
           " uid=" + getUID();
  }

  /**
   * Returns true if CommunityRequests have same request type
   * and target community.
   * @param o CommunityRequest to compare
   * @return true if CommunityRequests have same request type
   */
  public boolean equals(Object o) {
    if (!(o instanceof CommunityRequest)) return false;
    CommunityRequest cr = (CommunityRequest)o;
    if (!requestType.equals(cr.getRequestType())) return false;
    if (communityName == null) {
      return cr.getCommunityName() == null;
    } else {
      return communityName.equals(cr.getCommunityName());
    }
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
}
