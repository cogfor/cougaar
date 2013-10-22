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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A wrapped {@link MessageAddress} with added {@link
 * MessageAttributes}.
 */
public class MessageAddressWithAttributes 
  extends MessageAddress
{
  private transient MessageAddress delegate;
  private transient MessageAttributes attributes;

  public MessageAddressWithAttributes() {}

  protected MessageAddressWithAttributes (MessageAddress delegate,
                                          MessageAttributes attributes)
  {
    this.delegate = delegate;
    this.attributes = attributes;
  }

  /** @deprecated Why would you want a MessageAddress that only has attributes? */
  protected MessageAddressWithAttributes(MessageAttributes attributes)
  {
    Logger logger = Logging.getLogger(getClass().getName());
    if (logger.isErrorEnabled())
	logger.error("Creating a MessageAddress with attributes but no name!");
    delegate = null;
    this.attributes = attributes;
  }

  protected MessageAddressWithAttributes(String addr, MessageAttributes attrs) {
    delegate = MessageAddress.getMessageAddress(addr);
    attributes = attrs;
  }

  /** @return The MessageAddress without the MessageAtributes */
  @Override
public final MessageAddress getPrimary() {
      return delegate == null ? null : delegate.getPrimary();
  }

  /**
   * @return The Parent MessageAddress.  This is usually the same
   * as the result of getPrimary();
   */
  public final MessageAddress getDelegate() {
    return delegate;
  }

  @Override
public final String toAddress() {
      return (delegate == null) ? null : delegate.toAddress();
  }

  @Override
public final MessageAttributes getMessageAttributes() {
    return attributes;
  }

  public static final MessageAddress getMessageAddressWithAttributes(MessageAddress ma,
                                                                     MessageAttributes mas) {
    return new MessageAddressWithAttributes(ma, mas);
  }

  public static final MessageAddress getMessageAddressWithAttributes(String address,
                                                                     MessageAttributes mas) {
    MessageAddress ma = MessageAddress.getMessageAddress(address);
    return new MessageAddressWithAttributes(ma, mas);
  }

  /** @deprecated Why would you want a MessageAddress that only has attributes? */
  public static final MessageAddress getMessageAddressWithAttributes(MessageAttributes mas) {
    return new MessageAddressWithAttributes(mas);
  }

  //
  // io
  //

  // should never be used - see writeReplace
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(delegate);
    // attributes are transient
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    delegate = (MessageAddress) in.readObject();
    // attributes are transient
  }

  private Object writeReplace() {
    return delegate;
  }

}


