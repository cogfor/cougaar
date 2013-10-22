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

package org.cougaar.core.service.wp;

import java.io.Serializable;
import java.net.URI;

/**
 * An AddressEntry represents a single entry in the white pages.
 * <p>
 * Address entries are immutable.
 */
public final class AddressEntry implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final String name;
  private final String type;
  private final URI uri;
  private final Cert cert;
  private transient int _hc;

  private AddressEntry(
      String name, String type, URI uri, Cert cert) {
    this.name = name;
    this.type = type;
    this.uri = uri;
    this.cert = cert;
    if (name==null || type==null || uri==null | cert==null) {
      throw new IllegalArgumentException("Null argument");
    }
    // validate name?
  }

  public static AddressEntry getAddressEntry(
      String name, String type, URI uri) {
    return getAddressEntry(name, type, uri, Cert.NULL);
  }

  public static AddressEntry getAddressEntry(
      String name, String type, URI uri, Cert cert) {
    return new AddressEntry(name, type, uri, cert);
  }

  /** @return the non-null name (e.g.: "foo.bar") */
  public String getName() { return name; }

  /** @return the non-null type (e.g.: "mts") */
  public String getType() { return type; }

  /** @return the non-null uri (e.g.: "rmi://foo.com:123/xyz") */
  public URI getURI() { return uri; }

  /** @return the non-null cert (e.g.: Cert.NULL) */
  public Cert getCert() { return cert; }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AddressEntry)) {
      return false;
    } else {
      AddressEntry ae = (AddressEntry) o;
      return 
        name.equals(ae.name) &&
        type.equals(ae.type) &&
        uri.equals(ae.uri) &&
        cert.equals(ae.cert);
    }
  }

  @Override
public int hashCode() {
    if (_hc == 0) {
      int h = 0;
      h = 31*h + name.hashCode();
      h = 31*h + type.hashCode();
      h = 31*h + uri.hashCode();
      h = 31*h + cert.hashCode();
      _hc = h;
    }
    return  _hc;
  }

  @Override
public String toString() {
    return 
      "(name="+name+
      " type="+type+
      " uri="+uri+
      " cert="+cert+
      ")";
  }

  private Object readResolve() {
    return getAddressEntry(name, type, uri, cert);
  }

  //
  // deprecated, to be removed in Cougaar 10.4.1+
  //

  /** @deprecated use "getAddressEntry(name, type, uri)" */
  public AddressEntry(
      String name, Application app, URI uri, Cert cert, long ttl) {
    this(name, app.toString(), uri, cert);
  }
  /** @deprecated use "String getType()" */
  public Application getApplication() { 
    return Application.getApplication(type);
  }
  /** @deprecated use "URI getURI()" */
  public URI getAddress() { return getURI(); }
  /** @deprecated the TTL is a wp-internal variable */
  public long getTTL() { return 0; }
}
