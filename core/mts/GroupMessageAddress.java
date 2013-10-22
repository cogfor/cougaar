/*
 *
 * Copyright 2008 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.mts;

/**
 * An address that refers indirectly to a group of destinations.
 * The canonical example is an internet multicast address.
 */
public abstract class GroupMessageAddress extends SimpleMessageAddress {
    // The protocol-specific entity that describes the destinations via intention.
    private transient Object reference;
    
    // for deserialization only
    public GroupMessageAddress() {
    }
    
    public GroupMessageAddress(Object reference) {
        super(reference.toString());
        this.reference = reference;
    }
    
    abstract protected Object makeReference(String id);
    
    public Object getReference() {
        if (reference == null) {
            reference = makeReference(getAddress());
        }
        return reference;
    }
    
    @Override
   public boolean isGroupAddress() {
        return true;
    }
}
