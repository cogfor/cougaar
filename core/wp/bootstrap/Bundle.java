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

package org.cougaar.core.wp.bootstrap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.cougaar.core.util.UID;

/**
 * Bootstrap data for a single agent, including its {@link 
 * org.cougaar.core.service.wp.AddressEntry}s.
 * <p>
 * The {@link UID} is an optional tag to track version changes.  If
 * the server changes the data then the server-side UID for the bundle
 * will also change.
 * <p>
 * This class mirrors the resolver's {@link
 * org.cougaar.core.wp.resolver.Record} class, but is kept separate
 * to allow greater flexability.
 */
public final class Bundle implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final String name;
  private final UID uid;
  private final long ttd;
  private final Map entries;

  public Bundle(String name, UID uid, long ttd, Map entries) {
    this.name = name;
    this.uid = uid;
    this.ttd = ttd;
    this.entries = entries;
  }

  /**
   * The agent name.
   */
  public String getName() {
    return name;
  }

  /**
   * The UID of the Bundle, which is an optional version tracker.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The expiration "time-to-death" relative to the base timestamp,
   * or negative if this is a proposed modification.
   */
  public long getTTD() {
    return ttd;
  }

  /**
   * AddressEntries for this bundle.
   * <p>
   * The entries must have the same name.
   */
  public Map getEntries() {
    return entries;
  }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Bundle)) {
      return false;
    }
    Bundle b = (Bundle) o;
    return 
      (uid == null ?
       b.uid == null :
       uid.equals(b.uid)) &&
      ttd == b.ttd &&
      (entries == null ?
       b.entries == null :
       entries.equals(b.entries));
  }

  @Override
public int hashCode() {
    return uid.hashCode();
  }

  @Override
public String toString() {
    return "(bundle "+encode()+")";
  }

  public String encode() {
    return BundleEncoder.encodeBundle(this);
  }
  public static Bundle decode(String s) {
    return BundleDecoder.decodeBundle(s);
  }
  public static Map decodeAll(InputStream is) throws Exception {
    return BundleDecoder.decodeBundles(is);
  }
  public static Map decodeAll(BufferedReader br) throws Exception {
    return BundleDecoder.decodeBundles(br);
  }
}
