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

/**
 * Required service requestor API for the {@link PersistenceService}.
 */ 
public interface PersistenceClient {
  /**
   * Get the PersistenceIdentity of the client. A PersistenceIdentity
   * uniquely identifies a PersistenceClient regardless of restarts.
   * It is used as the key to retrieving the persisted state of the
   * client after rehydration.
   * @return the PersistenceIdentity of the client.
   */
  PersistenceIdentity getPersistenceIdentity();

  /**
   * Get a list of things to persist. If an object in the list is an
   * Envelope, the objects in the EnvelopeTuples in the Envelope are
   * persisted. If an object in the list is an EnvelopeTuple, the
   * object(s) in the EnvelopeTuple are persisted. In either case,
   * rehydration will return all such objects inside a
   * PersistenceEnvelope. Persisted Envelopes and EnvelopeTuples obey
   * add/remove semantics across incremental snapshots. If a remove
   * tuple appears, the rehydrated state will not include the object.
   * All other types of objects are simple persisted with no
   * add/remove semantics.
   */
  List getPersistenceData();
}
