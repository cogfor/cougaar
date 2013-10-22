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

package org.cougaar.core.blackboard;

import java.util.Collection;
import java.util.Enumeration;

import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * A {@link org.cougaar.core.domain.Domain}s view of the blackboard.
 */
public interface BlackboardServesDomain
{
  /** Apply predicate against the entire blackboard. */
  Enumeration searchBlackboard(UnaryPredicate predicate);

  /**
   * Add Object to the Blackboard Collection
   * (All subscribers will be notified)
   */
  void add(Object o);

  /**
   * Removed Object to the Blackboard Collection
   * (All subscribers will be notified)
   */
  void remove(Object o);

  /**
   * Change Object to the Blackboard Collection
   * (All subscribers will be notified)
   */
  void change(Object o, Collection changes);

  /**
   * Alias for sendDirective(dir, null);
   */
  void sendDirective(Directive dir);

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if agents crash.
   */
  void sendDirective(Directive dir, Collection changeReports);

  PublishHistory getHistory();

  /**
   * Get ABA translation status.
   * @return an ABATranslation giving translations of an ABA.
   * Returns null if the translations are unchanged.
   * @param aba the ABA to translate
   */
  ABATranslation getABATranslation(AttributeBasedAddress aba);
}
