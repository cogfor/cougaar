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

package org.cougaar.core.mts;

/**
 * Message attribute key/value constants.
 */
public interface AttributeConstants
{
  String IS_STREAMING_ATTRIBUTE = "IsStreaming";
  String ENCRYPTED_SOCKET_ATTRIBUTE = "EncryptedSocket";

  String DELIVERY_ATTRIBUTE = "DeliveryStatus";
  String DELIVERY_STATUS_DELIVERED = "Delivered"; // Successfully delivered to agent
  String DELIVERY_STATUS_CLIENT_ERROR = "ClientException"; //Receiving agent did not like message
  String DELIVERY_STATUS_DROPPED_DUPLICATE = "DroppedDuplicate"; //delivered to node, but dropped
  String DELIVERY_STATUS_HELD = "Held"; // delivered to node out of order, being held temporarily
  String DELIVERY_STATUS_STORE_AND_FORWARD  = "Store&Forward"; //should be used by email and dtn
  String DELIVERY_STATUS_BEST_EFFORT = "BestEffort"; // Sent UDP-like
  String DELIVERY_STATUS_OLD_INCARNATION = "OldIncarnaion"; // Originator is Old Incarnation
  String DELIVERY_STATUS_DROPPED = "Dropped";
  
  String RECEIPT_REQUESTED = "ReceiptRequsted";

  String MESSAGE_BYTES_ATTRIBUTE = "MessageBytes";
  String HEADER_BYTES_ATTRIBUTE = "HeaderBytes";
  String SENT_BYTES_ATTRIBUTE = "SentBytes";
  String RECEIVED_BYTES_ATTRIBUTE = "ReceivedBytes";

  String INCARNATION_ATTRIBUTE = "AgentIncarnationNumber";


  // System clock when the client sent the message
  String MESSAGE_SEND_TIME_ATTRIBUTE = "MessageSendTime";

  // Relative Timeout Attribute - 5000=5 seconds
  String MESSAGE_SEND_TIMEOUT_ATTRIBUTE = "MessageSendTimeout";
  // Absolute Timeout Attribute - 1060280361356 the system clock
  String MESSAGE_SEND_DEADLINE_ATTRIBUTE = "MessageSendDeadline";
}
