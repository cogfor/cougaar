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

import java.util.List;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.GroupMessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;

/**
 * This service is the node-level message transport that agents
 * use to send and receive messages.
 *
 * @see org.cougaar.core.agent.service.MessageSwitchService for
 * intra-agent message handling.
 */
public interface MessageTransportService extends Service
{

    /**
     * Ask MessageTransport to deliver a message (asynchronously).
     * message.getTarget() names the destination.  The client must be
     * registered, otherwise the message will not be sent.
     */
    void sendMessage(Message m);

    /** 
     * Register a client with MessageTransport.  A client is any
     * object which can receive Messages directed to it as the Target
     * of a message.
     */
    void registerClient(MessageTransportClient client);


    /** 
     * Unregister a client with MessageTransport.  No further
     * sendMessage calls will be accepted, and any queued messages
     * which aren't successfully delivered will be dropped.
     */
    void unregisterClient(MessageTransportClient client);


    /**
     * Block until all queued messages have been sent (or dropped).
     * @return The list of dropped messages (could be null).
     */
    List<Message> flushMessages();

    /**
     * @return the name of the entity that this MessageTransport
     * represents.  Will usually be the name of a node.
     */
    String getIdentifier();

    /** @return true IFF the MessageAddress is known to the nameserver */
    boolean addressKnown(MessageAddress a);

    AgentState getAgentState();

    // Multicast
    void joinGroup(MessageTransportClient client, GroupMessageAddress multicastAddress);
    void leaveGroup(MessageTransportClient client, GroupMessageAddress multicastAddress);
}

