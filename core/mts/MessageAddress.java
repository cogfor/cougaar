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

import java.io.Externalizable;
import java.net.URI;

import org.cougaar.util.annotations.Cougaar;

/**
 * An address for a {@link Message} sender or receiver.
 */
public abstract class MessageAddress 
  implements Externalizable
{

  /** @return the attributes associated with the address, or null */
  public MessageAttributes getMessageAttributes() {
    return null;
  }
  
  /** @see #toAddress */
  public String getAddress() {
    return toAddress();
  }

  /**
   * @return a string representation of this address, which may not be
   * human readable or parsable. 
   */
  public abstract String toAddress();

  /** @see #toAddress */
  @Override
public String toString() {
    return toAddress();
  }

  /** @see #getPrimary */
  @Override
public int hashCode() {
    return super.hashCode();
  }

  /** @see #getPrimary */
  @Override
public boolean equals(Object o) {
    return super.equals(o);
  }

  /**
   * Return the primary address associated with this {@link
   * MessageAddress}, suitable for hashing.
   * <p> 
   * For example, if an address has MessageAttributes, getPrimary() will
   * return the Address without the attributes.
   * @note This is usually an identity operation.
   */
  public MessageAddress getPrimary() {
    return this;
  }

  public boolean isGroupAddress() {
    return false;
  }
  
  //
  // factory items
  //

  /** @deprecated */
  public static final MessageAddress NULL_SYNC = getMessageAddress("NULL");
  public static final MessageAddress MULTICAST_SOCIETY = MulticastMessageAddress.getMulticastMessageAddress("SOCIETY");
  public static final MessageAddress MULTICAST_COMMUNITY = MulticastMessageAddress.getMulticastMessageAddress("COMMUNITY");
  public static final MessageAddress MULTICAST_LOCAL = MulticastMessageAddress.getMulticastMessageAddress("LOCAL");

  /** @return an address with the specified name */
  @Cougaar.Resolver()
  public static final MessageAddress getMessageAddress(String address) {
    return SimpleMessageAddress.getSimpleMessageAddress(address);
  }

  /** @return an address with the specified name and attributes */
  public static final MessageAddress getMessageAddress(String address, MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(address,mas);
  }

  /** @return an address plus the specified attributes */
  public static final MessageAddress getMessageAddress(MessageAddress address, MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(address,mas);
  }

  /** @deprecated Why would you want a MessageAddress that only has attributes? */
  public static final MessageAddress getMessageAddress(MessageAttributes mas) {
    return MessageAddressWithAttributes.getMessageAddressWithAttributes(mas);
  }

  /** @see #getMessageAddress(String) for agent addresses */
  public static final MessageAddress getMessageAddress(URI uri) {
    return URIMessageAddress.getURIMessageAddress(uri);
  }

  /** @see #getMessageAddress(String) for agent addresses */
  public static final MessageAddress getMessageAddress(URI uri, MessageAttributes mas) {
    MessageAddress ma = URIMessageAddress.getURIMessageAddress(uri);
    return new MessageAddressWithAttributes(ma, mas);
  }
}
