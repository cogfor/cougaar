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

package org.cougaar.core.persist;

import java.util.List;
import java.util.Set;

import org.cougaar.core.blackboard.MessageManager;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;

/**
 * An extended persistence interface for {@link
 * BlackboardPersistence}.
 * <p> 
 * The {@link BlackboardPersistence} implementation recasts these
 * methods into the new {@link PersistenceService} interface.
 */
public interface Persistence {
  /**
   * End a persistence epoch by generating a persistence delta
   * @param undistributedEnvelopes Envelopes that the distribute is about to distribute
   * @param allEpochEnvelopes All envelopes from this epoch
   * @param subscriberStates The subscriber states to record
   */
    PersistenceObject persist(List undistributedEnvelopes,
                              List allEpochEnvelopes,
                              List subscriberStates,
                              boolean returnBytes,
                              boolean full,
                              MessageManager messageManager,
                              Object quiescenceMonitorState);

    /**
     * Get the rehydration envelope from the most recent persisted state.
     * @return null if there is no persisted state.
     */
    RehydrationResult rehydrate(PersistenceEnvelope oldObjects, Object state);
    PersistenceSubscriberState getSubscriberState(Subscriber subscriber);
    boolean hasSubscriberStates();
    void discardSubscriberState(Subscriber subscriber);

  /**
   * Get a set of the Keys of the SubscriberStates in the rehydration info.
   * Used by the Distributor to track which subscribers have not
   * rehydrated.
   */
  public Set getSubscriberStateKeys();

    java.sql.Connection getDatabaseConnection(Object locker);
    void releaseDatabaseConnection(Object locker);
    boolean isDummyPersistence();
    long getPersistenceTime();
}
