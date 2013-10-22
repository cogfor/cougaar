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

package org.cougaar.core.domain;

import org.cougaar.core.blackboard.BlackboardServesDomain;
import org.cougaar.core.component.Service;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * The "root" plan provides blackboard access to {@link Domain}
 * {@link LogicProvider}s, including a view of all {@link
 * UniqueObject}s on the blackboard.
 * <p>
 * This is the interface that most domains use to access the
 * blackboard.
 */
public interface RootPlan extends BlackboardServesDomain, XPlan, Service {

  /** Find {@link UniqueObject}s using the {@link UID} as key */
  UniqueObject findUniqueObject(UID uid);

  /**
   * Add a {@link DelayedLPAction} to the set of actions to execute
   * after most of the transaction work has completed.
   */
  void delayLPAction(DelayedLPAction dla);
}
