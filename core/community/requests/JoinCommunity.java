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

import javax.naming.directory.Attributes;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.util.UID;

/**
 * Request to join (and optionally create) a community.
 */
public class JoinCommunity
    extends CommunityRequest implements java.io.Serializable {

  private String entityName;
  private Attributes entityAttrs;
  private int entityType;
  private boolean createIfNotFound;
  private Attributes communityAttrs;

  public JoinCommunity(String                    communityName,
                       String                    entityName,
                       int                       entityType,
                       Attributes                entityAttrs,
                       UID                       uid) {
    this(communityName,
         entityName,
         entityType,
         entityAttrs,
         false,
         null,
         uid,
         CommunityRequest.DEFAULT_TIMEOUT);
  }

  public JoinCommunity(String                    communityName,
                       String                    entityName,
                       int                       entityType,
                       Attributes                entityAttrs,
                       boolean                   createIfNotFound,
                       Attributes                communityAttrs,
                       UID                       uid) {

    this(communityName,
         entityName,
         entityType,
         entityAttrs,
         createIfNotFound,
         communityAttrs,
         uid,
         CommunityRequest.DEFAULT_TIMEOUT);
  }

  public JoinCommunity(String                    communityName,
                       String                    entityName,
                       int                       entityType,
                       Attributes                entityAttrs,
                       boolean                   createIfNotFound,
                       Attributes                communityAttrs,
                       UID                       uid,
                       long                      timeout) {
    super(communityName, uid, timeout);
    this.entityName = entityName;
    this.entityType = entityType;
    this.entityAttrs = entityAttrs;
    this.createIfNotFound = createIfNotFound;
    this.communityAttrs = communityAttrs;
  }

  public String getEntityName() {
    return entityName;
  }

  public int getEntityType() {
    return entityType;
  }

  public Attributes getEntityAttributes() {
    return entityAttrs;
  }

  public boolean createIfNotFound() {
    return createIfNotFound;
  }

  public Attributes getCommunityAttributes() {
    return communityAttrs;
  }

  private String entityTypeAsString(int type) {
    if (type == CommunityService.AGENT) return "AGENT";
    if (type == CommunityService.COMMUNITY) return "COMMUNITY";
    return "UNKNOWN_TYPE";
  }

  public String toString() {
    return "request=" + getRequestType() +
           " community=" + getCommunityName() +
           " entity=" + getEntityName() + "(" + entityTypeAsString(getEntityType()) + ")" +
           " createCommunityIfNotFound=" + createIfNotFound() +
           " timeout=" + getTimeout() +
           " uid=" + getUID();
  }

}