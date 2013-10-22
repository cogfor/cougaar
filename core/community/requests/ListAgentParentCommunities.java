package org.cougaar.community.requests;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.util.UniqueObject;

/**
 * Request to get the names of communities that are the direct ancestor
 * of specified member(community or agent).
 */

public class ListAgentParentCommunities implements Relay.Target, java.io.Serializable, UniqueObject {
  private String member;
  private UID uid;
  private MessageAddress source;
  private CommunityResponse resp;

  public ListAgentParentCommunities(MessageAddress source,
                                    UID uid,
                                    String member) {
      this.member = member;
      this.uid = uid;
      this.source = source;
    }

    public String getMember() {return member;}
    public UID getUID() { return uid;}
    public void setUID(UID uid) {
        throw new UnsupportedOperationException();
    }
    public MessageAddress getSource() { return source; }
    public void setResponse(CommunityResponse resp) {this.resp = resp;}
    public Object getResponse() { return resp; }

    public int updateContent(Object content, Token token) {
        return Relay.NO_CHANGE;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return false;
    }

    public int hashCode() { return uid.hashCode();}

    public String toString() {
      return "ListAgentParentCommunities: member=" + member;
    }


}
