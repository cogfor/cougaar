/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import java.util.HashMap;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Table of agent addresses on this VM, allowing static
 * deserialization threads to find their agent's address.
 * <p>
 * @note This is a minor security hole, but at least it only
 * allows access to the agent addresses and not the agents
 * themselves.
 */
public final class ClusterContextTable {

  private static final Logger logger = Logging.getLogger(ClusterContextTable.class);
  
  /**
   * Our map from a MessageAddress to the ClusterContext wrapper.
   * <p>
   * Most values are simple MessageAddress wrappers, but the
   * table can also contain MessageContext wrappers that add
   * to/from address information.
   */
  private static final HashMap contextTable = new HashMap(89);
  
  /** find the agent named by the parameter in my local VM.
   * Anyone caught using this in plugins will be shot.
   */
  static ClusterContext findContext(MessageAddress cid) {
    synchronized (contextTable) {
      return (ClusterContext) contextTable.get(cid);
    }
  }

  /** Add a context to the context table */
  static void addContext(final MessageAddress cid) {
    ClusterContext c = new ClusterContext() {
      public MessageAddress getMessageAddress() {
        return cid;
      }
    };
    synchronized (contextTable) {
      contextTable.put(cid, c);
    }
  }
  
  /** Remove a context from the context table */
  static void removeContext(MessageAddress cid) {
    synchronized (contextTable) {
      contextTable.remove(cid);
    }
  }

  /** The thread-local "current" context */
  private static final ThreadLocal theContext = new ThreadLocal() {};

  /** Internal object for keeping track of contexts. */
  public static class ContextState {
    private ClusterContext cc;
    public ContextState(ClusterContext c) {
      cc = c;
    }
    public final ClusterContext getClusterContext() { return cc; }
  }

  /**
   * A context for message deserialization, which includes the
   * source and target addresses.
   */
  public static final class MessageContext extends ContextState {
    private MessageAddress from;
    private MessageAddress to;
    public MessageContext(ClusterContext c, MessageAddress f, MessageAddress t) {
      super(c);
      from = f;
      to = t;
    }
    public final MessageAddress getFromAddress() { return from; }
    public final MessageAddress getToAddress() { return to; }
  }

  public static ContextState getContextState() {
    ContextState ret = (ContextState) theContext.get();
    if (ret == _groupDeliveryContext) {
      logger.error(
          "Attempted to get the context state from a message sent to a GroupMessageAddress");
    }
    return ret;
  }

  public static MessageContext getMessageContext() {
    ContextState cs = getContextState();
    return (cs instanceof MessageContext)?((MessageContext)cs):null;
  }

  public static ClusterContext getClusterContext() {
    ContextState cs = getContextState();
    
    return (cs!=null)?cs.getClusterContext():null;
  }

  private static final ClusterContext _groupDeliveryContext = 
      new ClusterContext.GroupDeliveryClusterContext(); 
  
  private static final void withGroupDeliveryClusterContext(Runnable thunk) {
      withClusterContext(_groupDeliveryContext, thunk);
  }
  
  private static final ClusterContext _dummyContext = new ClusterContext.DummyClusterContext();

  /** May be used by non-society classes to provide an empty context
   * for deserialization of objects sent from within a society.
   * The resulting instances may still be "broken" in various ways, wrapping
   * this call around the deserialization will at least allow 
   * avoiding warning messages.  <em>WARNING</em>:  This must <em>never</em> be
   * used within society code.
   */
  public static final void withEmptyClusterContext(Runnable thunk) {
    withClusterContext(_dummyContext, thunk);
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair */
  public static final void withClusterContext(ClusterContext cc, Runnable thunk) {
    withContextState(new ContextState(cc), thunk);
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair */
  public static final void withClusterContext(MessageAddress ma, Runnable thunk) {
    ClusterContext cc = findContext(ma);
    if (cc == null) {
      throw new IllegalArgumentException("Address \""+ma+"\" is not an Agent on this node.");
    } else {
      withContextState(new ContextState(cc), thunk);
    }
  }

  /** Convenient shortcut for a safe enterContext - exitContext pair */
  public static final void withMessageContext(MessageAddress ma, MessageAddress from, MessageAddress to, 
                                              Runnable thunk) {
    if (to.isGroupAddress()) {
      // this is a multicast, use a special context
      withGroupDeliveryClusterContext(thunk);
      return;
    }
    ClusterContext cc = getClusterContext();
    if (cc == null) {
      cc = findContext(ma);
      if (cc != null) {
        // normal case for 99% of the time
        withContextState(new MessageContext(cc, from, to), thunk);
      } else {
        // target is not on this node
        //
        // This is likely due to a race between remote MTS routing and
        // local agent removal for mobility (bug 1316).  The remote MTS
        // will catch this exception and re-route the message.
        throw new IllegalArgumentException(
            "Address \""+ma+"\" is not an Agent on this node.");
      }
    } else {
      MessageAddress oldMA = cc.getMessageAddress();
      if ((ma != null) ? ma.equals(oldMA) : (oldMA == null)) {
        // valid nesting, but rare in practice
        withContextState(new MessageContext(cc, from, to), thunk);
      } else {
        // unusual nested context, use the dummy context since its
        // not valid for the nested message to access a different agent
        //
        // In agent mobility this occurs when the node transfers agent
        // state that contains unsent messages (bug 1629 + bug 1634).
        // When the agent is created it will sent these nested messages
        // itself.
        withEmptyClusterContext(thunk);
      }
    }
  }

  public static final void withContextState(ContextState cs, Runnable thunk) {
    ContextState old = (ContextState) theContext.get();
    theContext.set(cs);
    try {
      thunk.run();
    } finally {
      theContext.set(old);
    }
  }
}
