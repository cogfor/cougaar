/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.util.Set;

import org.cougaar.core.adaptivity.Condition;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;

/** 
 * This service is used by the {@link
 * org.cougaar.core.adaptivity.AdaptivityEngine} to look up sensor
 * data on the blackboard.
 */
public interface ConditionService extends Service {
  /**
   * The interface to be implemented by listener objects for this
   * service. Note that no implementation of the methods of this
   * interface should do any more than set variables within the
   * object. In particular, they should not use any services or
   * synchronize on or wait for anything.
   *
   * There are currently no methods defined
   */
  interface Listener extends NotPersistable {
  }

  /**
   * Get a Condition object by name.
   */ 
  Condition getConditionByName(String sensor);

  /**
   * Get the names of all known Conditions.
   */
  Set getAllConditionNames();

  /**
   * Add a listener object. The given object will be publishChanged
   * whenever any Condition is added, removed, or changed. The
   * Object must already have been publishedAdded to the blackboard by
   * the caller.
   */
  void addListener(Listener cb);
  /**
   * Remove a listener object. The given object will no longer be
   * publishChanged whenever any Condition is added, removed,
   * or changed. The Object should not be removed from the blackboard
   * until it has been removed as a listener.
   */
  void removeListener(Listener cb);
}


