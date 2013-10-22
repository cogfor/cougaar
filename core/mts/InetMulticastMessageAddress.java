/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.mts;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *  Encapsulate an {@link InetSocketAddress} in a Cougaar {@link GroupMessageAddress}
 *  to support internet multicasts.
 */
public class InetMulticastMessageAddress
        extends GroupMessageAddress {
    
    public InetMulticastMessageAddress() {
    }
    
    public InetMulticastMessageAddress(InetAddress host, int port) {
        super(new InetSocketAddress(host, port));
    }

    @Override
   public InetSocketAddress makeReference(String addressString) {
        String[] hostAndPort = addressString.split(":");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        return new InetSocketAddress(host, port);
    }
    
    @Override
   public InetSocketAddress getReference() {
        return (InetSocketAddress) super.getReference();
    }
}
