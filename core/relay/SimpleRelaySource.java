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

package org.cougaar.core.relay;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * Source-side implementation of {@link SimpleRelay}, which creates
 * the {@link SimpleRelayTarget} on the target's blackboard.
 * <p>
 * Note that this class, {@link SimpleRelaySource}, and the
 * target-side implementation, {@link SimpleRelayTarget}, could be
 * easily merged into a single "SimpleRelayImpl" class.  However,
 * in practice this tends to cause implementation bugs in similar
 * relays, due to aliasing and confusion over the ownership of the
 * instance fields.  In particular, developers often incorrectly
 * attempt to share fields between the source and target, or allow the
 * source to modify the target's "reply" field or vice versa, which
 * leads to tricky race condition bugs.  Also, note that two agents
 * within the same node will share (alias) the same "SimpleRelayImpl"
 * if the relay itself is used as the content and the factory is
 * either null or simply casts the content back into the
 * "SimpleRelayImpl".  Lastly, there are known bugs for relays that
 * implement both source and target, specificially if they use ABA
 * targets or attempt to "chain" multiple relays together across many
 * agents.  For the purposes of this example and most relay
 * implementations, it's best to separate the source and target relay
 * class implementations and not implement both APIs in a single
 * class.
 */
public final class SimpleRelaySource 
extends SimpleRelayBase
implements Relay.Source {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private Set targets;
  private Relay.TargetFactory factory;

  // constructor:
  public SimpleRelaySource(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      Object query) {
    super(uid, source, target);
    // set initial query value, which can be null
    this.query = query;
    // the relay API wants a set of targets, so we save one
    targets = Collections.singleton(target);
    // our factory to create the target-side instance
    factory = new MyFactory(target);
  }

  // prevent "setReply", since this is the source-side instance.
  // Only "updateResponse" from the target can change the reply.
  @Override
public void setReply(Object reply) {
    throw new UnsupportedOperationException(
        "Unable to modify the reply on the sender-side, "+
        " it can only be modified at the target ("+target+")");
  }

  // implement Relay.Source:
  public Set getTargets() {
    return targets;
  }
  public Object getContent() {
    return query;
  }
  public Relay.TargetFactory getTargetFactory() {
    return factory;
  }
  public int updateResponse(
      MessageAddress target, Object response) {
    this.reply = response;
    return Relay.RESPONSE_CHANGE;
  }

  // factory method, which creates the target-side instance:
  private static class MyFactory 
    implements Relay.TargetFactory, Serializable {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      private final MessageAddress target;
      public MyFactory(MessageAddress target) {
        this.target = target;
      }
      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        Object query = content;
        return new SimpleRelayTarget(uid, source, target, query);
      }
    }
}
