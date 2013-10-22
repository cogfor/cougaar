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

package org.cougaar.core.agent;

import org.cougaar.core.mts.MessageAddress;

/**
 * Interface to the {@link ClusterContextTable} ThreadLocal that
 * allows static deserialization clients to find the thread's agent
 * address.
 * <p>
 * For example, an object deep in RMI deserialization may need to
 * know which agent it belongs to.
 */
public interface ClusterContext
{
  /** The current agent's address */
  MessageAddress getMessageAddress();
  
  /** A null {@link ClusterContext}. */
  final class DummyClusterContext implements ClusterContext {
    private static final MessageAddress cid = MessageAddress.NULL_SYNC;
    public MessageAddress getMessageAddress() { return cid; }
  }
  
  /** A {@link ClusterContext} for delivery to groups.  No Agent-specific refs should be present */
  final class GroupDeliveryClusterContext implements ClusterContext {
    private static final MessageAddress cid = MessageAddress.NULL_SYNC;
    public MessageAddress getMessageAddress() { return cid; }
  }
}
