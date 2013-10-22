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

import java.util.Set;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * A domain provides problem-specific support for blackboard
 * objects, such as an object {@link Factory} and {@link
 * LogicProvider}s to automate object behaviors and communicate with
 * remote agents.
 * <p>
 * Technically, domains don't need to contain separate {@link
 * LogicProvider}s, since these operations could be performed by
 * the domain itself, but typically separate component-like
 * {@link LogicProvider} classes are used.
 */
public interface Domain {

  /** returns the domain name, which must be unique */
  String getDomainName();

  /** returns the XPlan instance for the domain */
  XPlan getXPlan();

  /** returns the Factory for this Domain */
  Factory getFactory();

  /** invoke the MessageLogicProviders for this domain */
  void invokeMessageLogicProviders(DirectiveMessage message);

  /** invoke the EnvelopeLogicProviders for this domain */
  void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                    boolean isPersistenceEnvelope);

  /** invoke the RestartLogicProviders for this domain */
  void invokeRestartLogicProviders(MessageAddress cid);

  /**
   * invoke the ABAChangeLogicProviders for this domain.
   *
   * @param communities the set of communities with potiential
   * changes. If null, all communities may have changed.
   */
  void invokeABAChangeLogicProviders(Set communities);

}
