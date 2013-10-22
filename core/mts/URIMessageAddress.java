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

package org.cougaar.core.mts;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;


/**
 * A {@link MessageAddress} containing a standard URI.
 * <p>
 * Typically {@link SimpleMessageAddress} is used for agent-to-agent
 * communications, relying upon the naming service to resolve agent
 * names to network addresses.
 * <p>
 * The URI must be directly interpretable by the message transport.
 */
public class URIMessageAddress 
  extends MessageAddress 
{
  private URI uri;

  /** @return the MessageAddress as a URI */
  public URI toURI() { 
    return uri;
  }

  // public for externalizable use
  public URIMessageAddress() {}

  protected URIMessageAddress(URI uri) {
    this.uri = uri;
  }

  @Override
public final String toAddress() {
    return uri.toString();
  }


  public static URIMessageAddress getURIMessageAddress(URI uri) {
    return new URIMessageAddress(uri);
  }

  public boolean equals(URIMessageAddress ma ){
    return uri.equals(ma.uri);
  }

  @Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof URIMessageAddress) {
      return uri.equals(((URIMessageAddress)o).uri);      
    } else {
      return false;
    }
  }

  @Override
public final int hashCode() { 
    return uri.hashCode();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    uri = (URI) in.readObject();
  }

  /*
  protected Object readResolve() {
    return cacheSimpleMessageAddress(this);
  }
  */
}
