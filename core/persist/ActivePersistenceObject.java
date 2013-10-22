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

import org.cougaar.util.log.Logger;

/**
 * An object that requires special persistence and rehydration
 * handling.
 * <p>
 * This is typically used as a hack for odd blackboard objects that
 * can't [de]serialize correctly, or unusual domains.  <b>AVOID</b>
 * this interface unless you absolutely must use it!
 */
public interface ActivePersistenceObject {

  /**
   * Confirm that this object, not published to the blackboard
   * but reachable from a blackboard object, should be
   * persisted.
   * <p>
   * For example, if object X has a pointer to object Y,
   * and only X is on the blackboard, then Y is "weakly reachable".
   * If Y implements the ActivePersistenceObject interface, Y will be 
   * asked to confirm if Y should be persisted.
   * <p>
   * The default for objects that don't implement the
   * ActivePersistenceObject interface is "false" (ie. persist
   * all reachable objects).
   *
   * @return true if this object should <b>not</b> be persisted
   */
  boolean skipUnpublishedPersist(Logger logger);

  /**
   * Validate an object that has just been persistence
   * deserialized.
   * <p>
   * This occurs prior to the "postRehydration" validation,
   * but is very similar.  The difference is that this
   * check is done as the objects are being deserialized,
   * instead of after all of them have been deserialized.
   */
  void checkRehydration(Logger logger);

  /**
   * Fix an object once rehydration has completed.
   * <p>
   * This is used as a last-minute cleanup, in case the
   * object requires special deserialization work.
   */
  void postRehydration(Logger logger);
}
