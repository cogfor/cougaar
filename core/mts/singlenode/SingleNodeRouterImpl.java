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

package org.cougaar.core.mts.singlenode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.LoggingService;

/**
 * The message router implementation.
 * <p>
 * The {@link #routeMessage} method finds the {@link
 * MessageTransportClient} for each message's target and
 * delivers the message.  If the client doesn't exist, the
 * message is buffered until the client registers.
 */
final class SingleNodeRouterImpl
{
    private LoggingService loggingService;
    private HashMap agentStates = new HashMap();
    private HashMap clients; 
    private HashMap waitingMsgs; 
    String agentID; 

    public SingleNodeRouterImpl(ServiceBroker sb) {
	clients = new HashMap();
	waitingMsgs = new HashMap();
	
	loggingService = sb.getService(this, LoggingService.class, null);
    }
    
    /**
     * Find destination agent's receiving queue then deliver the
     * message to the client
     */
    public void routeMessage(Message message) {
	MessageAddress dest = message.getTarget();
	MessageTransportClient dest_client = (MessageTransportClient)
	    clients.get(dest);
	// deliver msgs
	// first check client is registered, if not, hold onto the msg
	if(dest_client == null) {
	    ArrayList messages = (ArrayList) waitingMsgs.get(dest);
	    if (messages == null) {
		messages = new ArrayList();
		waitingMsgs.put(dest, messages);
	    }
	    messages.add(message);
	} else { 
	    deliverMessage(message, dest_client);
	    
	}
    }
    
        
    /** invokes the clients receiveMessage() */
    public synchronized void deliverMessage(Message message, MessageTransportClient client) {
	try {
	    client.receiveMessage(message);
	} catch (Throwable th) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("MessageTransportClient threw an exception in receiveMessage, not retrying.", th);
	}
    }
    
    public void release() {
	// removeAgentState(client.getMessageAddress());	Do we need this??
    }
    
    public void flushMessages(ArrayList droppedMessages) {
    }
    
    public void removeAgentState(MessageAddress id) {
	//agentStates.remove(id.getPrimary());
    }
    
    public boolean okToSend(Message message) {	
	MessageAddress target = message.getTarget();
	if (target == null || target.toString().equals("")) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Malformed message: "+message);
	    return false;
	} else {
	    return true;
	}
    }
    
    /** Redirects the request to the MessageTransportRegistry. */
    public void registerClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	
	// Deliver any pending messages.
	ArrayList msgs = (ArrayList) waitingMsgs.get(key);
	if (msgs != null) {
	    // look for undelivered msgs & deliver them to newly registerd client
	    for(Iterator iter=msgs.iterator(); iter.hasNext();) {
		Message message = ((Message)iter.next());
		deliverMessage(message, client);
	    }
	}

	// stick in hashmap
	try {
	    clients.put(key, client);
	} catch (Exception e) {
	    if (loggingService.isErrorEnabled())
		loggingService.error(e.toString());
	}
	
    }
    
    /**Redirects the request to the MessageTransportRegistry. */
    public void unregisterClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	// remove from client hash
	try {
	    clients.remove(key);
	    waitingMsgs.remove(key);
	} catch (Exception e) {} // Map declares exceptions we wont see here
    }
    
    public String getIdentifier() {
	return agentID;
    }
    
    /** Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return clients.containsKey(a);
    }
 

    public synchronized AgentState getAgentState(MessageAddress id) {
	MessageAddress canonical_id = id.getPrimary();
	Object raw =  agentStates.get(canonical_id);
	if (raw == null) {
	    AgentState state = new SimpleMessageAttributes();
	    agentStates.put(canonical_id, state);
	    return state;
	} else if (raw instanceof AgentState) {
	    return (AgentState) raw;
	} else {
	    throw new RuntimeException("Cached state for " +id+
				       "="  +raw+ 
				       " which is not an AgentState instance");
	}
    }
}
