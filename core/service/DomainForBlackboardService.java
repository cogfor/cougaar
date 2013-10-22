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

import java.util.Set;

import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service is an extended {@link DomainService} for the
 * {@link Blackboard}'s use.
 */
public interface DomainForBlackboardService extends DomainService {
  /** set the blackboard for all the domains */
  void setBlackboard(Blackboard blackboard);

  /** invoke delayed LP actions on the domain's XPlans */
  void invokeDelayedLPActions();

  /** invoke EnvelopeLogicProviders across all currently loaded domains */
  void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                    boolean persistenceEnv);
  /** invoke MessageLogicProviders across all currently loaded domains */
  void invokeMessageLogicProviders(DirectiveMessage message);

  /** invoke RestartLogicProviders across all currently loaded domains */
  void invokeRestartLogicProviders(MessageAddress cid);

  /** invoke ABAChangeLogicProviders across all currently loaded domains */
  void invokeABAChangeLogicProviders(Set communities);
}  
