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

package org.cougaar.core.agent.service.uid;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * The UIDService implementation.
 */
final class UIDServiceImpl implements UIDService {
  private MessageAddress cid;
  private String prefix;
  private long count = System.currentTimeMillis();

  public UIDServiceImpl(MessageAddress cid) {
    this.cid = cid;
    prefix = cid.getAddress();
  }

  /**
   * MessageAddress of the agent.
   * <p> 
   * This is primarily for backwards compatibility; components
   * should get their agent's address through the
   * {@link org.cougaar.core.service.AgentIdentificationService},
   * which the common component base classes provide as a
   * "getAgentIdentifier()" method.
   */
  public MessageAddress getMessageAddress() {
    return cid;
  }

  private synchronized long nextID() {
    return ++count;
  }

  /** Take the next Unique ID. */
  public UID nextUID() {
    return new UID(prefix, nextID());
  }

  /**
   * Assign the next UID to a unique object.
   * <p>
   * This is equivalent to <code>o.setUID(nextUID())</code>.
   */
  public UID registerUniqueObject(UniqueObject o) {
    UID uid = nextUID();
    o.setUID(uid);
    return uid;
  }
    
  // persistence backwards compatibility
  //
  // This is no longer used, since we assume that a rehydrated
  // agent's current time is greater than its crashed instance's
  // counter.  If we hand out UIDs faster than one a millisecond
  // then this could cause problems!
  public synchronized PersistenceState getPersistenceState() {
    return new UIDServerPersistenceState(count);
  }
  public synchronized void setPersistenceState(PersistenceState state) {
    if (state instanceof UIDServerPersistenceState) {
      long persistedCount = ((UIDServerPersistenceState)state).count;
      if (persistedCount > count) count = persistedCount;
    } else {
      throw new IllegalArgumentException(state.toString());
    }
  }
  private static class UIDServerPersistenceState implements PersistenceState {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public long count;
    public UIDServerPersistenceState(long count) {
      this.count = count;
    }
  }
}
