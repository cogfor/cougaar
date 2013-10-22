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

import java.util.Map;

import org.cougaar.core.component.Service;

/**
 * This service is used by components to tell the node the initial
 * quiescence numbering and to toggle between quiescence and
 * non-quiescence.
 * <p>
 * "Quiescence" is partially a node-local concept, where all local
 * agents have completed processing, and a society-wide concept,
 * where all inter-agent messages have been delivered.  The {@link
 * EventService} is used to report node-local quiescence and
 * incoming/outgoing message numbers to an external script that
 * detects society-wide quiescence.  Quiescence detection is
 * primarily an execution-time planning tool used to advance the
 * execution time ({@link DemoControlService}) past idle planning
 * time (e.g. advance a day once all agents have planned the current
 * day).
 * <p>
 * Usually blackboard activity is enough to detect quiescence,
 * but a component that runs in a separate thread or queues work
 * must often tell the node that the agent is non-quiescent even
 * though blackboard activity has ceased. 
 */
public interface QuiescenceReportService extends Service {
  /**
   * Specifies the client agent's identity using his
   * AgentIdentificationService. This method must be called before any
   * other method and establishes the identity of the client agent in
   * a more or less unforgable way.
   * @param agentIdentificationService the agent's identification service
   */
  void setAgentIdentificationService(AgentIdentificationService agentIdentificationService);

  /**
   * Specify the maps of quiescence-relevant outgoing and incoming
   * message numbers associated with a newly achieved state of
   * quiescence. For efficiency, this method should be called before
   * calling setQuiescentState().
   * @param outgoingMessageNumbers a Map from agent MessageAddresses
   * to Integers giving the number of the last message sent. The
   * arguments must not be null.
   * @param incomingMessageNumbers a Map from agent MessageAddresses
   * to Integers giving the number of the last message received. The
   * arguments must not be null.
   */
  void setMessageNumbers(Map outgoingMessageNumbers, Map incomingMessageNumbers);

  /**
   * Specifies that, from this service instance point-of-view, the
   * agent is quiescent. The message number maps are not altered.
   */
  void setQuiescentState();

  /**
   * Specifies that this agent is no longer quiescent.
   */
  void clearQuiescentState();
}
