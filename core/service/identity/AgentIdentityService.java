/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Networks Associates Technology, Inc
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
 *
 * CHANGE RECORD
 * - 
 */

package org.cougaar.core.service.identity;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service is used by agents to obtain a cryptographic
 * identity and transfer this identity if the agent moves. 
 * <p>
 * The requestor must implement {@link AgentIdentityClient}.
 */
public interface AgentIdentityService
  extends Service
{
  /**
   * Creates a cryptographic identity for an agent. 
   * This method is called by Cougaar core services before
   * an agent is initialized.
   * <p>
   * If the agent already has a cryptographic identity, the
   * method returns immediately. If the agent does not have
   * a cryptographic key, or if no key is valid, a new key
   * is created.
   * <p>
   * This service provider will call checkPermission() to
   * make sure that only known entities will call the service.
   * <p>
   * If the 'id' parameter is not null, the cryptographic service
   * attempts to install keys from an agent that was previously
   * running on a remote node. The 'id' parameter should be the
   * TransferableIdentity object that was returned on the original
   * host when transferTo() was called.
   * The TransferableIdentity should then have been sent to the
   * new host when the agent was moved.
   *  
   * @param id the identity of an agent that was moved from another node.
   *
   * @exception  PendingRequestException the certificate authority
   *             did not sign the request immediately. The same request
   *             should be sent again later
   * @exception  IdentityDeniedException the certificiate authority
   *             refused to sign the key
   */
  void acquire(TransferableIdentity id)
    throws PendingRequestException,
    IdentityDeniedException;

  /**
   * Notifies the cryptographic service that the cryptographic identity
   * of the requestor is no longer needed.
   * This does not mean the key should be revoked or deleted.
   * The key is not used until the agent is restarted.
   */
  void release();

  /**
   * Notify the cryptographic service that an agent is about
   * to move to another node.
   * Depending on the cryptographic policy:<pre>
   * - Wrap agent key pair and protect it with remote node public key
   * - Revoke agent key (remote node must create a new key)
   * </pre> 
   *
   * @param targetNode       the name of the remote NodeAgentagent where
   *                         the agent will be run next.
   * @return an encrypted object that should be sent to the remote
   * node agent
   */
  TransferableIdentity transferTo(MessageAddress targetNode);

}

