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

package org.cougaar.core.node;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageSecurityManager;

/**
 * A trivial {@link MessageSecurityManager}.
 * <p>
 * This implementation looks like a {@link MessageSecurityManager},
 * but doesn't actually add any real security at all.  Instead, it
 * merely wraps each "secure" message inside another message for
 * transmission.
 * <p>
 * For debugging use, it prints '{' for each message "encoded" and
 * '}' for each message decoded.
 */
public class DummyMessageSecurityManager implements MessageSecurityManager 
{
  public Message secureMessage(Message m) {
    System.err.print("{");
    return new DummySecureMessage(m);
  }

  public Message unsecureMessage(SecureMessage m) {
    if (m instanceof DummySecureMessage) {
      System.err.print("}");
      return ((DummySecureMessage)m).m;
    } else {
      return null;
    }
  }

  private static class DummySecureMessage 
    extends Message
    implements SecureMessage 
    {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      protected Message m;

      private DummySecureMessage(Message m) {
        super(m.getOriginator(), m.getTarget());
        this.m = m;
      }
    }
}
