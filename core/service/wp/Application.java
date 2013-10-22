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
import java.util.HashMap;

/**
 * A deprecated field formerly required by an {@link AddressEntry}.
 *
 * @deprecated see new AddressEntry factory method
 */
public final class Application implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

private static final HashMap apps = new HashMap(13);
       
  private String name;
  private transient int _hc;
       
  public static Application getApplication(String as) {
    as = as.intern();
    synchronized (apps) {
      Application a = (Application) apps.get(as);
      if (a == null) {
        a = new Application(as);
        apps.put(as, a);
      }
      return a;
    }
  }

  /** @see #getApplication(String) */
  private Application(String a) {
    this.name = a;
    // assert (a.intern() == a);
  }
       
  @Override
public String toString() {
    return name;
  }
       
  @Override
public boolean equals(Object a) {
    return
      (this == a ||
       (a instanceof Application &&
        name == ((Application)a).name));
  }
 
  @Override
public int hashCode() {
    if (_hc == 0) _hc = name.hashCode();
    return _hc;
  }

  private Object readResolve() {
    // replace with an interned variation
    return getApplication(name);
  }
}
