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

import org.cougaar.core.mts.MessageAddress;

/** 
 * A {@link LogicProvider} that handles agent restart reconciliation.
 */
public interface RestartLogicProvider extends LogicProvider {

  /**
   * Called by the Blackboard whenever this agent or a remote agent 
   * restarts.
   * <p>
   * The primary function of this API is to allow a logic providers 
   * to reconcile the state of restarted agents.
   * <p>
   * If the given "cid" is null then <i>this</i> agent has 
   * restarted.  This logic provider should resend/confirm its 
   * state with all (remote) agents that it has communicated 
   * with.
   * <p>
   * If the given "cid" is non-null then the "cid" is
   * for a remote agent that has been restarted.  This
   * logic provider should resend/confirm its state 
   * <i>with regards to that one remote agent</i>.
   *
   * @param cid null if this agent restarted, otherwise the
   *            MessageAddress of a remote agent that restarted
   *
   * @see RestartLogicProviderHelper utility method to test the 
   *    given "cid"
   */
  void restart(MessageAddress cid);
}
