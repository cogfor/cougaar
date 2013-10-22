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

package org.cougaar.core.wp;

import org.cougaar.core.mts.AttributeConstants;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;

/**
 * A utility class to attach an MTS timeout attribute to a message
 * address.
 */
public final class MessageTimeoutUtils implements AttributeConstants {

 private MessageTimeoutUtils() {}

  /**
   * Get the absolute deadline on an address, for example
   * <code>1060280361356</code> milliseconds.
   */
  public static long getDeadline(MessageAddress addr) {
    Number value = get(addr, MESSAGE_SEND_DEADLINE_ATTRIBUTE);
    return (value == null ? -1 : value.longValue());
  }

  /** Tag an address with a absolute timeout. */
  public static MessageAddress setDeadline(
      MessageAddress addr,
      long deadline) {
    return
      (deadline <= 0 ?
       (addr) :
       set(addr, MESSAGE_SEND_DEADLINE_ATTRIBUTE, new Long(deadline)));
  }

  /**
   * Get the relative timeout on an address, for example
   * <code>5000</code> milliseconds. 
   */
  public static long getTimeout(MessageAddress addr) {
    Number value = get(addr, MESSAGE_SEND_TIMEOUT_ATTRIBUTE);
    return (value == null ? -1 : value.longValue());
  }

  /** Tag an address with a relative timeout. */
  public static MessageAddress setTimeout(
      MessageAddress addr,
      long timeout) {
    int t = (int) timeout;
    return
      (t <= 0 ?
       (addr) :
       set(addr, MESSAGE_SEND_TIMEOUT_ATTRIBUTE, new Integer(t)));
  }

  // get a number attribute
  private static Number get(
      MessageAddress addr,
      String name) {
    if (addr == null) {
      return null;
    }
    MessageAttributes attrs = addr.getMessageAttributes();
    if (attrs == null) {
      return null;
    }
    Object o = attrs.getAttribute(name);
    if (!(o instanceof Number)) {
      return null;
    }
    return ((Number) o);
  }

  // set a number attribute
  private static MessageAddress set(
      MessageAddress addr,
      String name,
      Number value) {
    if (addr == null) {
      return null;
    }
    MessageAttributes attrs = addr.getMessageAttributes();
    if (attrs == null) {
      attrs = new SimpleMessageAttributes();
      addr = MessageAddress.getMessageAddress(addr, attrs);
    }
    attrs.setAttribute(name, value);
    return addr;
  }
}
