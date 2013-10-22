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

package org.cougaar.core.mobility.ldm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Package-private implementation of {@link AgentControl} using
 * {@link Relay}s.
 */
class AgentControlImpl 
implements AgentControl, Relay.Source, Relay.Target {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

private static final ControlStatus NO_CONTROL_STATUS =
    new ControlStatus(NONE, null);

  private final UID uid;
  private final UID ownerUID;
  private final MessageAddress source;
  private final MessageAddress target;
  private final AbstractTicket ticket;
  private ControlStatus controlStatus;

  private transient Set _targets;

  public AgentControlImpl(
      UID uid,
      UID ownerUID,
      MessageAddress source,
      MessageAddress target,
      AbstractTicket ticket) {
    this.uid = uid;
    this.ownerUID = ownerUID;
    this.source = source;
    this.target = target;
    this.ticket = ticket;
    if ((uid == null) ||
        (source == null) ||
        (ticket == null)) {
      throw new IllegalArgumentException(
          "null uid/ticket");
    }
    // expecting target to be either addA or origN
    cacheTargets();
    // initial Status:
    this.controlStatus = NO_CONTROL_STATUS;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // AgentControl:

  public UID getOwnerUID() {
    return ownerUID;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public AbstractTicket getAbstractTicket() {
    return ticket;
  }

  public int getStatusCode() {
    return controlStatus.getStatusCode();
  }

  public String getStatusCodeAsString() {
    int i = getStatusCode();
    switch (i) {
      case NONE: return "NONE";
      case CREATED: return "CREATED";
      case ALREADY_EXISTS: return "ALREADY_EXISTS";
      case REMOVED: return "REMOVED";
      case DOES_NOT_EXIST: return "DOES_NOT_EXIST";
      case MOVED: return "MOVED";
      case ALREADY_MOVED: return "ALREADY_MOVED";
      case FAILURE: return "FAILURE";
      default: return "Unknown ("+i+")";
    }
  }

  public Throwable getFailureStackTrace() {
    return controlStatus.getStack();
  }

  public void setStatus(int statusCode, Throwable stack) {
    ControlStatus newCS = new ControlStatus(statusCode, stack);
    if (!(controlStatus.equals(NO_CONTROL_STATUS))) {
      throw new IllegalArgumentException(
          "Status already set to "+controlStatus+
          ", can't override with "+newCS);
    }
    controlStatus = newCS;
  }


  // Relay.Source:

  private void cacheTargets() {
    _targets = 
      (((target != null) && (!(target.equals(source)))) ? 
       Collections.singleton(target) :
       Collections.EMPTY_SET);
  }

  public Set getTargets() {
    return _targets;
  }

  public Object getContent() {
    return this;
  }

  public Relay.TargetFactory getTargetFactory() {
    return AgentControlImplFactory.INSTANCE;
  }

  public int updateResponse(
      MessageAddress t, Object response) {
    ControlStatus newCS = (ControlStatus) response;
    // assert local-agent == getSource()
    // assert newMS != null
    if (!(controlStatus.equals(newCS))) {
      controlStatus = newCS;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Relay.Target:

  public MessageAddress getSource() {
    return source;
  }
  public Object getResponse() {
    return 
      ((!(controlStatus.equals(NO_CONTROL_STATUS))) ?
       (controlStatus) : 
       null);
  }


  public int updateContent(Object content, Token token) {
    // currently the content is immutable
    // maybe support "abort" content in the future
    return Relay.NO_CHANGE;
  }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AgentControlImpl)) { 
      return false;
    } else {
      UID u = ((AgentControlImpl) o).uid;
      return uid.equals(u);
    }
  }
  @Override
public int hashCode() {
    return uid.hashCode();
  }
  private void readObject(ObjectInputStream stream) 
    throws ClassNotFoundException, IOException {
      stream.defaultReadObject();
      cacheTargets();
    }

  @Override
public String toString() {
    return 
      "Agent control request "+uid+
      " owned by "+ownerUID+
      " and ticket ("+ticket+
      ") with source "+source+
      " and target "+target+
      ", status is "+controlStatus;
  }

  private static class ControlStatus implements Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public final int statusCode;
    public final Throwable stack;
    public ControlStatus(int statusCode, Throwable stack) {
      this.statusCode = statusCode;
      this.stack = stack;
    }
    public int getStatusCode() { return statusCode; }
    public Throwable getStack() { return stack; }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof ControlStatus)) {
        return false;
      } else {
        ControlStatus cs = (ControlStatus) o;
        return 
          ((statusCode == cs.statusCode) && 
           ((stack != null) ?
            (stack.equals(cs.stack)) :
            (cs.stack == null)));
      }
    }
    @Override
   public String toString() {
      return "status ("+statusCode+") stack("+stack+")";
    }
    
   @Override
   public int hashCode() {
      return statusCode + stack.hashCode();
   }
  }

  /**
   * Simple factory implementation.
   */
  private static class AgentControlImplFactory 
    implements Relay.TargetFactory, Serializable {

      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      public static AgentControlImplFactory INSTANCE = 
        new AgentControlImplFactory();

      private AgentControlImplFactory() { }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        AgentControlImpl adi = (AgentControlImpl) content;
        return new AgentControlImpl(
            adi.uid, adi.ownerUID, source, null, adi.ticket);
      }

      private Object readResolve() {
        return INSTANCE;
      }
    }
}
