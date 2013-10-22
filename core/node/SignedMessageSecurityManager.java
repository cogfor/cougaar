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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignedObject;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageSecurityManager;

/**
 * A useful {@link MessageSecurityManager} which signs each message,
 * but depends on the destination to recover the public key of the
 * sender.
 */
public class SignedMessageSecurityManager implements MessageSecurityManager {

  public SignedMessageSecurityManager() {
  }

  public Message secureMessage(Message m) {
    return new SignedSecureMessage(m);
  }

  public Message unsecureMessage(SecureMessage m) {
    if (m instanceof SignedSecureMessage) {
      return ((SignedSecureMessage)m).extract();
    } else {
      return null;
    }
  }

  private static class SignedSecureMessage extends Message implements SecureMessage {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private SignedObject secret;
    //private java.security.cert.Certificate cert;

    SignedSecureMessage(Message m) {
      super(m.getOriginator(), m.getTarget());
      secret = SignedMessageSecurityManager.sign(m);
      //cert = SignedMessageSecurityManager.getCert(origin.getAddress());
    }

    Message extract() {
      try {
        java.security.cert.Certificate cert = 
          KeyRing.getCert(getOriginator().getAddress());
        if (cert == null) {
          System.err.println("\nWarning: Dropping message, No public certificate for Origin \""+
                             getOriginator().getAddress()+"\": "+secret.getObject());
          return (Message) secret.getObject();
        }
        if (verify(secret, cert)) {
          return (Message) secret.getObject();
        } else {
          return null;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }
  }


  static SignedObject sign(Message m) {
    try {
      String origin = m.getOriginator().getAddress();
      PrivateKey pk = KeyRing.getPrivateKey(origin);
      if (pk == null) {
        System.err.println("\nWarning: Dropping message, Could not find private key for Origin \""+
                           origin+"\": "+m);
        return null;
      }
      Signature se = Signature.getInstance(pk.getAlgorithm());
      return new SignedObject(m, pk, se);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  static boolean verify(SignedObject so, java.security.cert.Certificate cert) {
    // check for bogus conditions.
    if (so == null || cert == null) return false;
    try {
      PublicKey pk = cert.getPublicKey();
      Signature ve = Signature.getInstance(so.getAlgorithm());
      return so.verify(pk, ve);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

}
