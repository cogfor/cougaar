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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * This service is the backwards-compatible API for the {@link
 * UIDService}. 
 */
public interface UIDServer extends Service {
  /**
   * MessageAddress of the agent.
   * <p> 
   * This is primarily for backwards compatibility; components
   * should get their agent's address through the
   * {@link org.cougaar.core.service.AgentIdentificationService},
   * which the common plugin base classes provide as a
   * "getAgentIdentifier()" method.
   */
  MessageAddress getMessageAddress();

  /** Take the next Unique ID. */
  UID nextUID();

  /**
   * Assign the next UID to a unique object.
   * <p>
   * This is equivalent to <code>o.setUID(nextUID())</code>.
   */
  UID registerUniqueObject(UniqueObject o);

  /** persistence backwards compatibility. */
  PersistenceState getPersistenceState();
  void setPersistenceState(PersistenceState state);
}
