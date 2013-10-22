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

package org.cougaar.core.service;

import java.io.IOException;

/**
 * Implementations of this interface contain a secret key used to
 * encrypt/decrypt persisted data. The getOutputStream method of the
 * PersistenceProtectionService places the (encrypted) key used for
 * encrypting the output stream in the envelope. The getInputStream
 * method, retrieves the key from the envelope to decrypt the input
 * stream. In addition, the PersistenceProtectionServiceClient has an
 * iterator method that returns an iterator over a collection of
 * PersistedKeyEnvelopes. When the PersistenceProtectionService
 * iterates over the keys, it uses the getPersistedKey() method to
 * retrieve the key. The PersistenceProtectionService can re-encrypt
 * the key if it wishes, and call setPersistedKey() to notify the
 * service client that it should persist the new key.
 */
public interface DataProtectionKeyEnvelope
{
  /** 
   * Returns the persisted key in this envelope.
   */
  DataProtectionKey getDataProtectionKey() throws IOException;

  /** 
   * Saves an updated key to persisted storage.
   */
  void setDataProtectionKey(DataProtectionKey pk) throws IOException;
}
