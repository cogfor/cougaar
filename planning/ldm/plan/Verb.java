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

package org.cougaar.planning.ldm.plan;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Verb is the action part of a Task.
 *
 */

public class Verb implements Serializable {
  private String name;
	
  /** Constructor takes a String that represents the verb.
   * @note pre-11.0 this was deprecated and public - now Verb.get(String) should be used.
   */
  protected Verb(String v) {
    if (v == null) throw new IllegalArgumentException();
    name = v.intern();
  }
	
  /** @return String toString returns the String that represents the verb */
  public final String toString() {
    return name;
  }
	
  /** Capabilities are equal IFF they encapsulate the same string
   */
  public final boolean equals(Object v) {
    // use == since verb strings are interned
    return (this == v || 
            (v instanceof Verb && name == ((Verb)v).name) ||
            (v instanceof String && name.equals((String) v))
            );
  }
  
  /** convenience method for verb testing */
  public final boolean equals(String v) {
    return ( name==v || name.equals(v));
  }

  public final int hashCode()
  {
    return name.hashCode();
  }

  // replace with an interned variation
  protected Object readResolve() {
    return getVerb(name);
  }

  // 
  // verb cache
  //

  private static final HashMap verbs = new HashMap(29);
  	
  /** older alias for Verb.get() 
   * @deprecated Use Verb.get(String)
   **/
  public static Verb getVerb(String vs) {
    return get(vs);
  }
  /** Verb factory method.  Constructs or returns cached verb instances
   * matching the requested paramater.
   * Note that this will only construct and/or return direct instances of
   * Verb and never any subclass.
   **/
  public static Verb get(String vs) {
    synchronized (verbs) {
      Verb v = (Verb) verbs.get(vs);
      if (v != null) 
        return v;
      else {
        vs = vs.intern();
        v = new Verb(vs);
        verbs.put(vs, v);
        return v;
      }
    }
  }
}
