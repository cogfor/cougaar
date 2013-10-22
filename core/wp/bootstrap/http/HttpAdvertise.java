/*
 * <copyright>
 *  
 *  Copyright 2002-2007 BBNT Solutions, LLC
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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.ServletService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.AdvertiseBase;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.Util;

/**
 * This component advertises bundles through HTTP by using the
 * {@link ServletService}.
 * <p>
 * It looks in the {@link ConfigService} for config entries of type
 * "-HTTP_REG" and scheme "http", or "-HTTPS_REG" and "https",
 * e.g.<pre>
 *   X={-HTTP_REG=http://test.com:8800}
 * </pre>
 * and if the localhost is "test.com" and the local {@link
 * ServletService} port is "8800" then this component registers in
 * the {@link ServletService} as "/wp_bootstrap".  The bound
 * {@link Servlet} responds to "doGet" request with text-encoded
 * bundles tracked by the {@link
 * org.cougaar.core.wp.bootstrap.AdvertiseService} (i.e. locally bound
 * leases).
 * <p> 
 * Another possibility is to push bundles to a remote server, using
 * a {@link java.net.URLConnection}.
 */
public class HttpAdvertise
extends AdvertiseBase
{

  private static final String DEFAULT_PATH = "/wp_bootstrap";

  private String defaultPath = DEFAULT_PATH;
  private ConfigService configService;
  private ServletService servletService;

  private final ConfigService.Client configClient =
    new ConfigService.Client() {
      public void add(Bundle b) {
        addAdvertiser(getBootEntry(b));
      }
      public void change(Bundle b) {
        add(b);
      }
      public void remove(Bundle b) {
        removeAdvertiser(getBootEntry(b));
      }
    };

  public void setParameter(Object o) {
    List l;
    Object p;
    if ((o instanceof List) &&
        (!(l = (List) o).isEmpty()) &&
        ((p = l.get(0)) instanceof String)) {
      defaultPath = (String) p;
    }
  }

  @Override
public void load() {
    super.load();

    servletService = sb.getService(this, ServletService.class, null);
    if (servletService == null && log.isWarnEnabled()) {
      log.warn("Unable to obtain ServletService");
    }
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
    if (servletService != null) {
      sb.releaseService(this, ServletService.class, servletService);
      servletService = null;
    }

    super.unload();
  }

  protected AddressEntry getBootEntry(Bundle b) {
    AddressEntry entry = HttpUtil.getBootEntry(b);
    if (entry == null) {
      return null;
    }
    URI uri = entry.getURI();
    String host = (uri == null ? null : uri.getHost());
    if (!Util.isLocalHost(host)) {
      return null;
    }
    int port = uri.getPort();
    String scheme = uri.getScheme();
    boolean isHttp = "http".equals(scheme);
    if (servletService == null) {
      return null;
    }
    int localport = 
      (isHttp ?
       servletService.getHttpPort() :
       servletService.getHttpsPort());
    if (port != localport) {
      return null;
    }
    return entry;
  }

  @Override
protected Advertiser createAdvertiser(Object bootObj) {
    return new HttpAdvertiser(bootObj);
  }

  private class HttpAdvertiser extends Advertiser {

    private final AddressEntry bootEntry;

    private final String filter;
    private final String path;
    private Servlet servlet;

    public HttpAdvertiser(Object bootObj) {
      super(bootObj);

      bootEntry = (AddressEntry) bootObj;

      filter = HttpUtil.getFilter(bootEntry, agentId, log);

      URI uri = bootEntry.getURI();
      String s = uri.getPath();
      if (s != null && s.startsWith("/$")) {
        // remove "/$name" prefix
        if (s.startsWith("/$"+agentName+"/")) {
          s = s.substring(2 + agentName.length());
        } else if (s.startsWith("/$~/")) {
          s = s.substring(3);
        }
        if (s.equals("/")) {
          s = null;
        }
      }
      if (s == null || s.length() == 0) {
        s = defaultPath;
      }
      path = s;
    }

    @Override
   public void start() {
      // register servlet
      servlet = new MyServlet();
      try {
        servletService.register(path, servlet);
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to register servlet", e);
      }

      // okay, listening for calls to "getBundles()"
    }

    private Map getBundles() {
      // serve remote caller
      Map ret = HttpAdvertise.this.getBundles();
      // filter for specific agent
      ret = HttpUtil.filterBundles(ret, filter, log); 
      if (log.isDebugEnabled()) {
        log.debug("Serving bundles: "+ret);
      }
      return ret;
    }

    @Override
   public void update(String name, Bundle bundle) {
      // do nothing, since we server bundles (as opposed to posting
      // them at external server)
    }

    @Override
   public void stop() {
      servletService.unregister(path);
    }

    @Override
   public String toString() {
      return
        "(http_advertise "+super.toString()+")";
    }

    private class MyServlet extends HttpServlet {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      @Override
      public void doGet(
          HttpServletRequest sreq,
          HttpServletResponse sres) throws IOException {
        Map m = getBundles();
        sres.setContentType("text/plain");
        PrintWriter pr = sres.getWriter();
        pr.println("# white pages bootstrap data");
        for (Iterator iter = m.values().iterator();
            iter.hasNext();
            ) {
          Bundle b = (Bundle) iter.next();
          String s = b.encode();
          if (s == null) {
            continue;
          }
          pr.println(s);
        }
        pr.close();
      }
    }
  }
}
