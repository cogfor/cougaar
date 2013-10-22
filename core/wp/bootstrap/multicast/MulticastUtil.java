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

package org.cougaar.core.wp.bootstrap.multicast;

import java.net.URI;
import java.util.Map;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.Bundle;

/**
 * Multicast utility methods. 
 */
public final class MulticastUtil {

  private MulticastUtil() {}

  public static AddressEntry getBootEntry(Bundle b) {
    if (b != null) {
      Map m = b.getEntries();
      if (m != null) {
        Object o = m.get("-MULTICAST_REG");
        if (o == null) {
          o = m.get("-MCAST_REG");
        }
        if (o instanceof AddressEntry) {
          AddressEntry ae = (AddressEntry) o;
          URI uri = ae.getURI();
          if (uri != null) {
            String scheme= uri.getScheme();
            if ("multicast".equals(scheme) ||
                "mcast".equals(scheme)) {
              return ae;
            }
          }
        }
      }
    }
    return null;
  }
}
