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
 * A marker class for multicasting messages.
 * <p>
 * Will likely be deprecated in the future, to be replaced with an enhanced
 * implementation.
 * <p>
 * Used by constant addresses in MessageAddress.
 */
public class MulticastMessageAddress
        extends SimpleMessageAddress {
    // for Externalizable use only
    public MulticastMessageAddress() {
    }

    protected MulticastMessageAddress(String address) {
        super(address);
    }

    public boolean hasReceiverClass() {
        return false;
    }

    public Class<?> getReceiverClass() {
        return null;
    }

    // factory methods

    public static final MulticastMessageAddress getMulticastMessageAddress(String address) {
        return new MulticastMessageAddress(address);
    }

    public static final MulticastMessageAddress getMulticastMessageAddress(Class<?> clientClass) {
        return new MMAWithClass(clientClass);
    }
    
    /**
     * @deprecated Why would you want a MessageAddress that only has attributes?
     */
    public static final MessageAddress getMulticastMessageAddress(MessageAttributes ma) {
        return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma);
    }

    public static final MessageAddress getMulticastMessageAddress(String address,
                                                                  MessageAttributes attrs) {
        MessageAddress ma = MessageAddress.getMessageAddress(address);
        return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma, attrs);
    }

    public static final MessageAddress getMulticastMessageAddress(Class<?> clientClass,
                                                                  MessageAttributes attrs) {
        MessageAddress ma = getMulticastMessageAddress(clientClass);
        return MessageAddressWithAttributes.getMessageAddressWithAttributes(ma, attrs);
    }

    // private classes
    private static class MMAWithClass
            extends MulticastMessageAddress {
        private transient Class<?> klass = null;

        // for externalizable
        @SuppressWarnings("unused")
      public MMAWithClass() {
        }
        
        public MMAWithClass(Class<?> clazz) {
            super(clazz.getName());
        }

        @Override
      public boolean hasReceiverClass() {
            return true;
        }

        @Override
      public synchronized Class<?> getReceiverClass() {
            if (klass != null) {
                return klass;
            } else {
                try {
                    klass = Class.forName(toAddress());
                    return klass;
                } catch (ClassNotFoundException cnf) {
                    return null;
                }
            }
        }
    }
}
