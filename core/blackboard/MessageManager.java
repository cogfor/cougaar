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

import java.util.Iterator;

import org.cougaar.core.agent.service.MessageSwitchService;

/**
 * A message acknowledgement manager used by the {@link
 * org.cougaar.core.blackboard.Distributor}'s non-lazy persistence
 * mode to ensure that unacknowledged messages are persisted.
 */
public interface MessageManager {
  int OK      = 0;
  int RESTART = 4;
  int IGNORE  = 8;
  int DUPLICATE = IGNORE + 1;
  int FUTURE    = IGNORE + 2;
  int PRESENT   = OK;

  /**
   * Start the message manager running. The message manager should be
   * inactive until this method is called because it does know know
   * the identity of the agent it is in.
   */
  void start(MessageSwitchService msgSwitch, boolean didRehydrate);
  /**
   * Stop the message manager, halting any internal activity.
   */
  void stop();
  /**
   * Add a set of messages to the queue of messages waiting to be
   * transmitted. When persistence is enabled, the messages are held
   * until the end of the epoch.
   */
  void sendMessages(Iterator messages);
  /**
   * Incorporate a directive message into the message manager's state.
   * @return Normally, the message is returned, but duplicate and from
   * the future messages are ignored by returning null.
   */
  int receiveMessage(DirectiveMessage aMessage);
  /**
   * Incorporate a directive acknowledgement into the message
   * manager's state. The acknowledged messages are removed from the
   * retransmission queues.
   */
  int receiveAck(AckDirectiveMessage theAck);
  /**
   * Prepare to acknowledge a list of directive messages. The
   * acknowledgements are not actually sent until the end of the
   * epoch.
   */
  void acknowledgeMessages(Iterator messagesToAck);

  /**
   * Advance epoch.  Bring the current epoch to an end in preparation
   * for a persistence delta.
   */
  void advanceEpoch();

  boolean needAdvanceEpoch();
}
