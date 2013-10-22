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

/**
 * A simple {@link MessageAddress} containing an agent name.
 */
public class SimpleMessageAddress 
  extends MessageAddress 
{
  protected transient byte[] addressBytes;
  protected transient String _as = null;

  // public for externalizable use
  public SimpleMessageAddress() {}

  protected SimpleMessageAddress(String address) {
    this.addressBytes = address.getBytes();
    _as = address.intern();
  }

  @Override
public final String getAddress() {
    return _as;
  }

  public boolean equals(SimpleMessageAddress ma ){
    return (ma != null && _as == ma._as);
  }

  @Override
public boolean equals(Object o ){
    if (this == o) return true;
    // use == since the strings are interned.
    if (o instanceof MessageAddress) {
      MessageAddress oma = (MessageAddress) o;
      return (_as== oma.toAddress());
    } else {
      return false;
    }
  }
  @Override
public String toAddress() {
    return _as;
  }

  @Override
public final int hashCode() { 
    return _as.hashCode();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    int l = addressBytes.length;
    out.writeByte(l);
    out.write(addressBytes,0,l);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    int l = in.readByte();
    addressBytes=new byte[l];
    in.readFully(addressBytes,0,l);
    _as = new String(addressBytes).intern();
  }

  protected Object readResolve() {
    return cacheSimpleMessageAddress(this);
  }

  private static java.util.HashMap cache = new java.util.HashMap(89);

  public static SimpleMessageAddress getSimpleMessageAddress(String as) {
      if (as == null) return null;
    as = as.intern();
    synchronized (cache) {
      SimpleMessageAddress a = (SimpleMessageAddress) cache.get(as);
      if (a != null) {
        return a;
      } else {
        a = new SimpleMessageAddress(as);
        cache.put(as, a);
        return a;
      }
    }
  }

  public static SimpleMessageAddress cacheSimpleMessageAddress(SimpleMessageAddress a) {
      if (a == null) return null;
    synchronized (cache) {
      String as = a._as;
      SimpleMessageAddress x = (SimpleMessageAddress) cache.get(as);
      if (x != null) {
        return x;
      } else {
        cache.put(as, a);
        return a;
      }
    }
  }

}
