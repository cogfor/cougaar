/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import javax.naming.directory.ModificationItem;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.Entity;

/**
 * Blackboard Relay used by CommunityService to send request to remote
 * Community Manager.  This class should only be used by Community
 * Service implementations, it is not intended for use by clients.
 **/
public class RequestImpl
    implements Request, Relay.Source, Relay.Target, Serializable {

    protected final UID uid;
    protected final MessageAddress source;
    protected final MessageAddress target;

    protected String communityName;
    protected int requestType = UNDEFINED;
    protected Entity entity;

    protected CommunityResponse resp;
    protected ModificationItem[] mods;

    private transient Set targets;
    private transient Set listeners;

    public RequestImpl(MessageAddress           source,
                       MessageAddress           target,
                       String                   communityName,
                       int                      reqType,
                       Entity                   entity,
                       ModificationItem[]       attrMods,
                       UID                      uid,
                       CommunityResponseListener crl) {
      this.source = source;
      this.target = target;
      this.communityName = communityName;
      this.requestType = reqType;
      this.entity = entity;
      this.mods = attrMods;
      this.uid = uid;
      addCommunityResponseListener(crl);
      cacheTargets();
    }

    public String getCommunityName() {
      return communityName;
    }

    /**
     * Defines the type of request.
     * @param reqType Request type
     */
    public void setRequestType(int reqType) {
      this.requestType = reqType;
    }


    public int getRequestType() {
      return this.requestType;
    }

    /**
     * Entity for requests requiring one, such as a JOIN and LEAVE.
     * @param entity Affected entity
     */
    public void setEntity(Entity entity) {
      this.entity = entity;
    }

    public Entity getEntity() {
      return this.entity;
    }

    public void setResponse(CommunityResponse resp) {
      this.resp = resp;
    }

    public void setAttributeModifications(ModificationItem[] mods) {
      this.mods = mods;
    }

    public ModificationItem[] getAttributeModifications() {
      return mods;
    }

    public void addCommunityResponseListener(CommunityResponseListener crl) {
      if (listeners == null) listeners = new HashSet();
      listeners.add(crl);
    }

    public Set getCommunityResponseListeners() {
      return (listeners != null) ? listeners : Collections.EMPTY_SET;
    }

    public String getRequestTypeAsString(int type) {
      switch (type) {
        case Request.UNDEFINED: return "UNDEFINED";
        case Request.JOIN: return "JOIN";
        case Request.LEAVE: return "LEAVE";
        case Request.GET_COMMUNITY_DESCRIPTOR: return "GET_COMMUNITY_DESCRIPTOR";
        case Request.MODIFY_ATTRIBUTES: return "MODIFY_ATTRIBUTES";
        case Request.LIST: return "LIST";
      }
      return "INVALID_VALUE";
    }

    /**
     * Returns a string representation of the request
     * @return String - a string representation of the request.
     **/
    public String toString() {
      return "request=" + getRequestTypeAsString(requestType) +
             " community=" + communityName +
             " entity=" + (entity == null ? "null" : entity.getName()) +
             " resp=" + resp +
             " source=" + this.getSource() +
             " uid=" + uid;
    }

    public UID getUID() {
        return uid;
    }

    public void setUID(UID uid) {
        throw new UnsupportedOperationException();
    }

    public MessageAddress getTarget() {
        return target;
    }

    // Relay.Source:

    private void cacheTargets() {
        targets =
            ((target != null) ?
             Collections.singleton(target) :
             Collections.EMPTY_SET);
    }

    public Set getTargets() {
        return targets;
    }

    public Object getContent() {
        return this;
    }

    public Relay.TargetFactory getTargetFactory() {
        return RequestImplFactory.INSTANCE;
    }

    public int updateResponse(MessageAddress t, Object response) {
      this.resp = (CommunityResponse)response;
      return Relay.RESPONSE_CHANGE;
    }

    // Relay.Target:

    public MessageAddress getSource() {
        return source;
    }

    public Object getResponse() {
        return resp;
    }

    public int updateContent(Object content, Token token) {
        return Relay.NO_CHANGE;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RequestImpl)) {
            return false;
        } else {
            UID u = ((RequestImpl)o).uid;
            return uid.equals(u);
        }
    }

    public int hashCode() {
        return uid.hashCode();
    }

    private void readObject(ObjectInputStream stream)
        throws ClassNotFoundException, java.io.IOException
    {
        stream.defaultReadObject();
        cacheTargets();
    }

    protected RequestImpl target_copy() {
      return new RequestImpl(source, null, communityName, requestType, entity, mods, uid, null);
    }

  /**
   * Simple factory implementation.
   */
  private static class RequestImplFactory
      implements Relay.TargetFactory, Serializable {

      public static RequestImplFactory INSTANCE =
          new RequestImplFactory();


      public Relay.Target create(UID uid,
                                 MessageAddress source,
                                 Object content,
                                 Relay.Token token)
      {
          RequestImpl ati = (RequestImpl) content;
          return ati.target_copy();
      }

      private Object readResolve() {
          return INSTANCE;
      }
  }

}
