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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.DiscoveryBase;

/**
 * This component discovers bundles through HTTP by using a {@link
 * URLConnection}.
 * <p> 
 * It looks in the {@link ConfigService} for config entries of type
 * "-HTTP_REG" and scheme "http", or "-HTTPS_REG" and "https",
 * e.g.<pre>
 *   X={-HTTP_REG=http://test.com:8800}
 * </pre>
 * and then polls the HTTP server on that host:port for a page
 * that lists {@link Bundle}s encoded as text.  If the URL lacks
 * a path then a default path of "/$~/wp_bootstrap" is assumed.
 * These bundles are then copied into the {@link 
 * org.cougaar.core.wp.bootstrap.DiscoveryService}.
 */
public class HttpDiscovery
extends DiscoveryBase
{
  private static final String DEFAULT_PATH = "/$~/wp_bootstrap";

  private ConfigService configService;

  private final ConfigService.Client configClient =
    new ConfigService.Client() {
      public void add(Bundle b) {
        addPoller(getBootEntry(b));
      }
      public void change(Bundle b) {
        add(b);
      }
      public void remove(Bundle b) {
        removePoller(getBootEntry(b));
      }
    };

  @Override
protected String getConfigPrefix() {
    return "org.cougaar.core.wp.resolver.http.";
  }

  @Override
public void load() {
    super.load();

    configService = sb.getService(configClient, ConfigService.class, null);
    if (configService == null) {
      throw new RuntimeException("Unable to obtain ConfigService");
    }
  }

  @Override
public void unload() {
    if (configService != null) {
      sb.releaseService(configClient, ConfigService.class, configService);
      configService = null;
    }

    super.unload();
  }

  protected AddressEntry getBootEntry(Bundle b) {
    return HttpUtil.getBootEntry(b);
  }

  @Override
protected Map lookup(Object bootObj) {
    throw new InternalError("should use HttpPoller!");
  }

  @Override
protected Poller createPoller(Object bootObj) {
    return new HttpPoller(bootObj);
  }

  private class HttpPoller extends Poller {

    private final AddressEntry bootEntry;

    private String filter;

    public HttpPoller(Object bootObj) {
      super(bootObj);

      bootEntry = (AddressEntry) bootObj;
    }

    @Override
   protected void initialize() {
      super.initialize();

      filter = HttpUtil.getFilter(bootEntry, null, log);
    }

    @Override
   public void stop() {
    }

    @Override
   protected Map lookup() {
      URI uri = bootEntry.getURI();

      URLConnection uc;
      URI u = uri;
      try {
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
          u = URI.create(uri.toString()+DEFAULT_PATH);
        }
        URL url = u.toURL();
        uc = url.openConnection();
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Unable to contact "+u);
        }
        return null;
      }

      Map newFound;
      try {
        if (uc instanceof HttpURLConnection) {
          HttpURLConnection huc = (HttpURLConnection) uc;
          int rc = huc.getResponseCode();
          if (rc != HttpURLConnection.HTTP_OK) {
            if (log.isInfoEnabled()) {
              if (rc == HttpURLConnection.HTTP_NOT_FOUND) {
                log.info("Path not found: "+u);
              } else {
                log.info(
                    "Non-OK response code: "+rc+" = "+
                    huc.getResponseMessage());
              }
            }
            return null;
          }
        }
        InputStream is = uc.getInputStream();
        newFound = Bundle.decodeAll(is);
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Lookup "+u+" failed", e);
        }
        return null;
      }

      if (newFound == null) {
        return null;
      }

      // filter for specific agent
      newFound = HttpUtil.filterBundles(newFound, filter, log); 

      return newFound;
    }
  }
}
