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

package org.cougaar.core.wp.bootstrap.http;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.Bundle;

/**
 * HTTP utility methods. 
 */
public final class HttpUtil {

  private HttpUtil() {}

  public static boolean isBootEntry(AddressEntry entry) {
    String type = entry.getType();
    String scheme = entry.getURI().getScheme();
    return
      (("-HTTP_REG".equals(type) ||
       "-HTTPS_REG".equals(type)) &&
       ("http".equals(scheme) ||
        "https".equals(scheme)));
  }

  public static AddressEntry getBootEntry(Bundle b) {
    if (b != null) {
      Map m = b.getEntries();
      if (m != null) {
        Object o = m.get("-HTTP_REG");
        if (o == null) {
          o = m.get("-HTTPS_REG");
        }
        if (o instanceof AddressEntry) {
          AddressEntry ae = (AddressEntry) o;
          URI uri = ae.getURI();
          if (uri != null) {
            String scheme= uri.getScheme();
            if ("http".equals(scheme) ||
                "https".equals(scheme)) {
              return ae;
            }
          }
        }
      }
    }
    return null;
  }

  public static String getFilter(
      AddressEntry bootEntry,
      MessageAddress agentId,
      LoggingService log) {
    return (agentId == null ? null : agentId.getAddress());
  }

  public static Map filterBundles(
      Map m,
      String filter,
      LoggingService log) {
    if (filter == null || m == null || m.isEmpty()) {
      if (log.isDetailEnabled()) {
        log.detail("no-op filterBundles("+m+", "+filter+")");
      }
      return m;
    }
    Bundle b = (Bundle) m.get(filter);
    if (b == null) {
      if (log.isDetailEnabled()) {
        log.detail("empty filterBundles("+m+", "+filter+")");
      }
      return Collections.EMPTY_MAP;
    }
    if (log.isDebugEnabled()) {
      log.debug("Filtered to just "+filter+" out of "+m);
    }
    return Collections.singletonMap(filter, b);
  }
}
