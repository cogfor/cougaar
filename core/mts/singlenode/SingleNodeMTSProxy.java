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

import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.GroupMessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;

/**
 * Base Single-Node implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the corresponding registry.
 */
public class SingleNodeMTSProxy 
    implements MessageTransportService
{
  private SingleNodeRouterImpl router;

  public SingleNodeMTSProxy(SingleNodeRouterImpl router) {
    this.router = router;
  }

  void release() {
    router.release();
    router = null;
  }


  /**
   * Checks malformed message, if ok, 
   * -registers message,  
   * -puts in receiving agent's queue.
   */
  public void sendMessage(Message rawMessage) {	
    if (router.okToSend(rawMessage)) router.routeMessage(rawMessage);	
  }

  /**
   * Wait for all queued messages for our client to be either
   * delivered or dropped. 
   * @return the list of dropped messages, which could be null.
   */
  public synchronized ArrayList flushMessages() {
    ArrayList droppedMessages = new ArrayList();
    /* // no more
      link.flushMessages(droppedMessages);
      ArrayList rawMessages = new ArrayList(droppedMessages.size());
      Iterator itr = droppedMessages.iterator();
      while (itr.hasNext()) {
      AttributedMessage m = (AttributedMessage) itr.next();
      rawMessages.add(m.getRawMessage());
      }

      return rawMessages;
      */
    return droppedMessages;
  }



  public AgentState getAgentState() {
    return null;
  }


  /**
   * Adds client to SingleNodeMTSRegistry 
   */
  public synchronized void registerClient(MessageTransportClient client) {
    // Should throw an exception of client != this.client
    router.registerClient(client);
  }


  /**
   * Redirects the request to the MessageTransportRegistry. */
  public synchronized void unregisterClient(MessageTransportClient client) {
    // Should throw an exception of client != this.client
    router.unregisterClient(client);
  }


  /**
   * Redirects the request to the MessageTransportRegistry. */
  public String getIdentifier() {
    return router.getIdentifier();
  }

  /**
   * Redirects the request to the MessageTransportRegistry. */
  public boolean addressKnown(MessageAddress a) {
    return router.addressKnown(a);
  }

  public void joinGroup(MessageTransportClient client, GroupMessageAddress address) {
      throw new IllegalStateException("joinGroup is not supported");
  }

  public void leaveGroup(MessageTransportClient client, GroupMessageAddress address) {
      throw new IllegalStateException("leaveGroup is not supported");
  }
}

