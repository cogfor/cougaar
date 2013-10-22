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

import org.cougaar.core.adaptivity.OperatingModePolicy;
import org.cougaar.core.component.Service;

/**  
 * This service allows components to modify {@link
 * org.cougaar.core.adaptivity.AdaptivityEngine} plays and playbooks. 
 * <p> 
 * This part of the service is used to alter the plays in the playbook
 * by adding, modifying, or removing OperatingMode Policies.
 * <p>
 * <pre> 
 *   Play - if THREATCON &gt; 3 &amp;&amp; ENCLAVE == THEATER
 *          then ENCRYPT = {56, 128}  - the first value is preferred
 *
 *   Policy - if THREATCON &gt; 3 &amp;&amp; CPU &gt; 90
 *            then ENCRYPT &gt;= 128
 *
 * Plays formed by calling constrain(Policy)
 *
 *        if (THREATCON &gt; 3 &amp;&amp; ENCLAVE == THEATER) 
 *           &amp;&amp; (THREATCON &gt;3 &amp;&amp; CPU &gt; 90)
 *        then ENCRYPT = {128}
 *
 *        if (THREATCON &gt; 3 &amp;&amp; ENCLAVE == THEATER) 
 *           &amp;&amp; !(THREATCON &gt;3 &amp;&amp; CPU &gt; 90)
 *        then ENCRYPT = {56, 128}
 * </pre> 
 * This play can constrain this policy because their "then" clauses
 * are operating on the same knobs.
 * <p>
 * Two plays result from constraining the original play with the
 * policy. The "if" clause of the first new play has the "if" clause
 * of the policy tacked on to it, and the knob on the "then" clause
 * must be set to 128. The "if" clause on the second new play has 
 * the -negated- "if" clause of the policy tacked on. The "then"
 * clause is untouched.
 */
public interface PlaybookConstrainService extends Service {

  /* constrain and unconstrain called by OperatingModePolicyManager */

  /**
   * might replace one play with two in current playbook 
   * @param omp OperatingModePolicy may constrain the play
   */
  void constrain(OperatingModePolicy omp);

  /**
   * might replace two plays with one in current playbook 
   * @param omp OperatingModePolicy 
   */
  void unconstrain(OperatingModePolicy omp);

}

