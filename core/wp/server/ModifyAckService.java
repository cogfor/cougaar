/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.wp.server;

import java.util.Map;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * This service sends "modify-ack"s.
 * <p>
 * This API mirrors the client's ModifyService.
 * <p>
 * The clientAddr is the address of the client that sent
 * the modify message, which is where the response message
 * will be sent.
 * <p>
 * The clientTime is the time that the client sent it's request.
 * Answer messages sent back to the client are tagged with both
 * the client's time and the server's time, which allows
 * the client to measure the round-trip-time and correct for
 * any clock drift.
 * <p>
 * This API hides the MTS and messaging details.
 * <p>
 * The service requestor must implement the Client API.
 *
 * @see org.cougaar.core.wp.resolver.ModifyService
 */
public interface ModifyAckService extends Service {

  /** Acknowledge a client's "modify" request. */
  void modifyAnswer(
      MessageAddress clientAddr, long clientTime, Map m);

  interface Client {
    /**
     * Handle a client's modify request.
     * <p>
     * The server should call the "modifyAnswer" method.
     */
    void modify(
        MessageAddress clientAddr, long clientTime, Map m);
  }
}
