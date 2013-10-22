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

package org.cougaar.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.ProtectedInputStream;
import org.cougaar.core.mts.ProtectedOutputStream;

/**
 * This service is used to cryptographically protect incoming
 * and outgoing messages.
 * This service should be called by the transport service for
 * all Cougaar messages.
 */
public interface MessageProtectionService extends Service
{

  /**
   * Sign and/or encrypt the header of an outgoing message.
   *
   * When a message is sent out:
   * 1) The aspect calls protectHeader().
   * 2) The data protection service encrypts/signs the header.
   *    It uses the information provided in the source and destination
   *    to decide how to encrypt and/or sign.
   * 3) The encrypted header is returned.
   * 4) The aspect calls getOuputStream.
   *    - The source and destination should be the same as what was found
   *      in the call to protectHeader().
   * 5) The service returns an output stream where the MTS will serialize
   *    the clear-text message.
   * 6) The service encrypts the message and write the encrypte/signed
   *    message to the output stream.
   * 7) The encrypted message is actually sent over the network.
   *
   * @param attributes     data about the message for the header
   * @param source      The source of the message
   * @param destination The destination of the message
   * @return the protected header (sign and/or encrypted)
   * @throws GeneralSecurityException
   * @throws IOException
   */
  byte[] protectHeader(MessageAttributes attributes, 
                       MessageAddress source,
                       MessageAddress destination) 
    throws GeneralSecurityException, IOException;

  /**
   * Verify the signed and/or encrypted header of an incoming message.
   *
   * @param rawData     The signed and/or encrypted header
   * @param source      The source of the message
   * @param destination The destination of the message
   * @return the header in the clear
   * @throws GeneralSecurityException
   * @throws IOException
   */
  MessageAttributes unprotectHeader(byte[] rawData, 
                                    MessageAddress source,
                                    MessageAddress destination) 
    throws GeneralSecurityException, IOException;

  /** 
   * Gets a stream to encrypt and/or sign outgoing messages
   *
   * This method is called once for each outgoing message.
   * The implementation of this service must construct a
   * ProtectedOutputStream, which is a special kind of FilterOutputStream.
   * The service client (MTS) serializes a Message to this 
   * ProtectedOutputStream. The implementation of the service will in turn
   * write data to the 'os' stream it was given at creation time.
   * When the Message has been completely serialized and written 
   * to the ProtectedOutputStream, the service client calls the finish()
   * method of the ProtectedOutputStream.
   *
   * The first byte of the ProtectedOutputStream should be the first byte
   * of the (serialized) message content.
   *
   * Since messages may be resent, the method may be called multiple times
   * for the same message, but this is in a different context.
   *
   * @param os The output stream containing encrypted and/or signed data
   * @param source      The source of the outgoing message
   * @param destination The destination of the outgoing message
   * @param attrs       The attributes of the outgoing message
   * @return A filter output stream
   * @throws IOException
   */
  ProtectedOutputStream getOutputStream(OutputStream os,
                                        MessageAddress source,
                                        MessageAddress destination,
                                        MessageAttributes attrs)
    throws IOException;

  /** 
   * Gets a stream to verify incoming messages
   *
   * This method is called once for each incoming message.
   * The implementation of this service must construct a
   * ProtectedInputStream, which is a special kind of FilterInputStream.
   * The service reads an encrypted message from the ProtectedInputStream.
   * The service client (MTS) calls the finishInput() method when all the
   * message has been read.
   * The service client verifies the message. The service client reads
   * the clear-text message from the 'is' input stream.
   *
   * The first byte of the ProtectedInputStream should be the first byte
   * of the (serialized) message content.
   *
   * Since messages may be resent, the method may be called multiple times
   * for the same message, but this is in a different context.
   *
   * @param is The input stream containing the verified clear-text message
   * @param src      The source of the incoming message
   * @param dst The destination of the incoming message
   * @param attrs       The attributes of the incoming message
   * @return A filter intput stream
   * @throws IOException
   */
  ProtectedInputStream getInputStream(InputStream is,
                                      MessageAddress src,
                                      MessageAddress dst,
                                      MessageAttributes attrs)
    throws IOException;
}
