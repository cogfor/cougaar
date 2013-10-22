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

package org.cougaar.core.blackboard;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.util.UID;

/**
 * Implementation of a relay class that contains a BlackboardAlert
 * object as its content.
 * <p>
 * Components must compare their local agent's address to the
 * "getSource()" and "getTarget()" addresses to decide whether they
 * are the sender or recipient.
 */
public class BlackboardAlertRelay 
implements SimpleRelay, Relay.Source, Relay.Target {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
  private final MessageAddress source;
  private final MessageAddress target;

  private BlackboardAlert query;
  private Object reply;

  private transient Set _targets;
  private transient Relay.TargetFactory _factory;

  /**
   * Create an instance.
   *
   * @param uid unique object id from the UIDService 
   * @param source the local agent's address 
   * @param target the remote agent's address 
   * @param query alert object.
   */
  public BlackboardAlertRelay(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      BlackboardAlert query) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.query = query;
    cacheTargets();
  }

  // SimpleRelay:

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  public MessageAddress getSource() {
    return source;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public Object getQuery() {
    return query;
  }

  public void setQuery(Object query) {
    this.query = (BlackboardAlert)query;
  }

  public Object getReply() {
    return reply;
  }

  public void setReply(Object reply) {
    this.reply = reply;
  }

  // Relay.Source:

  private void cacheTargets() {
    _targets = Collections.singleton(target);
    _factory = new BlackboardAlertRelayFactory(target);
  }
  public Set getTargets() {
    return _targets;
  }
  public Object getContent() {
    return query;
  }
  public Relay.TargetFactory getTargetFactory() {
    return _factory;
  }
  public int updateResponse(
      MessageAddress target, Object response) {
    this.reply = response;
    return Relay.RESPONSE_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    return reply;
  }
  public int updateContent(Object content, Token token) {
    // assert content != null
    this.query = (BlackboardAlert)content;
    return Relay.CONTENT_CHANGE;
  }

  // Object:

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof BlackboardAlertRelay) { 
      UID u = ((BlackboardAlertRelay) o).uid;
      return uid.equals(u);
    } else {
      return false;
    }
  }
  @Override
public int hashCode() {
    return uid.hashCode();
  }
  private void readObject(java.io.ObjectInputStream os) 
    throws ClassNotFoundException, java.io.IOException {
      os.defaultReadObject();
      cacheTargets();
    }
  @Override
public String toString() {
    return 
      "(BlackboardAlertRelay"+
      " uid="+uid+
      " source="+source+
      " target="+target+
      " query="+query+
      " reply="+reply+
      ")";
  }

  // factory method:

  private static class BlackboardAlertRelayFactory 
    implements Relay.TargetFactory, Serializable {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      private final MessageAddress target;
      public BlackboardAlertRelayFactory(MessageAddress target) {
        this.target = target;
      }
      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        BlackboardAlert query = (BlackboardAlert)content;
        return new BlackboardAlertRelay(uid, source, target, query);
      }
    }
}
