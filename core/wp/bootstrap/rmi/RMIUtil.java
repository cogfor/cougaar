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

package org.cougaar.core.wp.bootstrap.rmi;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMISocketFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SocketFactoryService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.Bundle;

/**
 * RMI utilities.
 *
 * @property org.cougaar.core.wp.resolver.rmi.resolveHosts
 *   Boolean-valued property to do an InetAddress resolution of the
 *   bootstrap entry's host name when using the RMI registry.  For
 *   example, if the bootstrap entry was:<pre>
 *    (NodeX, -RMI_REG, rmi://localhost:8000/NodeX)
 *   </pre> then this would resolve "localhost" to "127.0.0.1" and
 *   act as if the bootstrap entry was:<pre>
 *    (NodeX, -RMI_REG, rmi://127.0.0.1:8000/NodeX)
 *   </pre>.  RMI registry access is fairly picky about the host
 *   name, so this defaults to 'true'.
 *  
 * @property org.cougaar.core.wp.resolver.rmi.useSSL
 *   Boolean-valued property which controls whether or not ssl is used
 *   in communication to the RMI registry.  Defaults to 'false'.
 *
 * @property org.cougaar.core.naming.useSSL
 *   Backwards compatibility for the
 *   "org.cougaar.core.wp.resolver.rmi.useSSL"
 *   system property.
 */
public final class RMIUtil {

  // should we do an InetAddress lookup of the bootEntry's
  // host name?
  private static final String RESOLVE_HOSTS_PROP =
    "org.cougaar.core.wp.resolver.rmi.resolveHosts";
  private static final boolean RESOLVE_HOSTS =
    SystemProperties.getBoolean(RESOLVE_HOSTS_PROP, true);

  // all RMI registry lookups will prefix the name
  // with this path prefix, to keep the registry tidy
  private static final String DIR_PREFIX = "COUGAAR_NAMING/";

  // use ssl
  private static final String USE_SSL_PROP =
    "org.cougaar.core.wp.resolver.rmi.useSSL";
  private static final String OLD_USE_SSL_PROP =
    "org.cougaar.core.naming.useSSL";
  private static final boolean USE_SSL =
    SystemProperties.getBoolean(USE_SSL_PROP) ||
    SystemProperties.getBoolean(OLD_USE_SSL_PROP);

  private RMIUtil() {}

  public static RMISocketFactory getRMISocketFactory(
      SocketFactoryService socketFactoryService) {
    return getRMISocketFactory(socketFactoryService, USE_SSL);
  }

  public static RMISocketFactory getRMISocketFactory(
      SocketFactoryService socketFactoryService,
      boolean useSSL) {
    HashMap p = new HashMap(2);
    p.put("ssl", Boolean.valueOf(useSSL));
    p.put("aspects", Boolean.FALSE);

    RMISocketFactory socFac = (RMISocketFactory)
      socketFactoryService.getSocketFactory(
          RMISocketFactory.class, p);

    return socFac;
  }

  public static boolean isBootEntry(AddressEntry entry) {
    String type = entry.getType();
    String scheme = entry.getURI().getScheme();
    return
      ("-RMI_REG".equals(type) &&
       "rmi".equals(scheme));
  }

  public static AddressEntry getBootEntry(Bundle b) {
    if (b != null) {
      Map m = b.getEntries();
      if (m != null) {
        Object o = m.get("-RMI_REG");
        if (o instanceof AddressEntry) {
          AddressEntry ae = (AddressEntry) o;
          URI uri = ae.getURI();
          if (uri != null) {
            String scheme = uri.getScheme();
            if ("rmi".equals(scheme)) {
              return ae;
            }
          }
        }
      }
    }
    return null;
  }

  public static Registry createRegistry(
      RMISocketFactory rsf,
      int port,
      LoggingService log) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Create registry (port="+port+")");
    }

    Registry r = null;
    try {
      r = LocateRegistry.createRegistry(port, rsf, rsf);
    } catch (Exception e2) {
      String e2Msg = e2.getMessage();
      if (e2 instanceof java.rmi.server.ExportException &&
          e2Msg != null &&
          e2Msg.startsWith("Port already in use")) {
        if (log.isInfoEnabled()) {
          log.info(
              "Unable to create registry on port "+port+
              " (possibly another local Node raced ahead and"+
              " created it and we'll find it later)",
              e2);
        }
      } else {
        if (log.isErrorEnabled()) {
          boolean isMultipleRegistryBug =
            (e2 instanceof ExportException &&
             e2Msg != null &&
             e2Msg.equals("internal error: ObjID already in use"));
          log.error(
              "Unable to create RMI registry on port "+port+
              (isMultipleRegistryBug ?
               ", is another RMI registry running"+
               " on this JVM (Sun bug 4267864)" :
               ""), e2);
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Created registry on port "+port+": "+r);
    }
    return r;
  }

  public static Registry lookupRegistry(
      RMISocketFactory rsf,
      String hostname,
      int port,
      LoggingService log) {

    if (log.isDebugEnabled()) {
      log.debug(
          "Lookup registry (host="+hostname+", port="+port+")");
    }

    String host = hostname;
    if (RESOLVE_HOSTS) {
      String ip;
      try {
        InetAddress ia = InetAddress.getByName(hostname);
        ip = ia.getHostAddress();
      } catch (UnknownHostException uhe) {
        if (log.isWarnEnabled()) {
          log.warn(
              "Unable to resolve hostname "+hostname+
              " to IP address");
        }
        ip = null;
      }
      if (ip != null && !hostname.equals(ip)) {
        host = ip;
        if (log.isDebugEnabled()) {
          log.debug("Using host "+hostname+" IP address "+ip);
        }
      }
    }

    Registry r = null;
    try {
      r = LocateRegistry.getRegistry(host, port, rsf);

      // test that this is a real registry
      try {
        r.list();
      } catch (Exception e) {
        r = null;
        throw e;
      }

      if (log.isDebugEnabled()) {
        log.debug("Found existing registry: "+r);
      }
    } catch (Exception e) {
      String eMsg = e.getMessage();
      if (e instanceof ConnectException &&
          eMsg != null &&
          eMsg.startsWith("Connection refused")) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Unable to access registry on "+host+":"+port+
              " (Connection refused, the registry probably"+
              "  doesn't exist)");
        }
      } else if (e instanceof java.rmi.UnknownHostException) {
        if (log.isInfoEnabled()) {
          log.info("Unknown registry host "+host);
        }
      } else {
        if (log.isInfoEnabled()) {
          log.info(
              "Unable to access registry on "+host+":"+port+
              " (unknown exception)", e);
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Located registry (host="+host+", port="+port+"): "+r);
    }
    return r;
  }

  public static RMIAccess bindAccess(
      RMISocketFactory rsf,
      Registry r,
      String name,
      BundlesProvider bp,
      LoggingService log) {
    if (log.isDebugEnabled()) {
      log.debug(
          "Bind remote object (name="+name+")");
    }

    RMIAccess rObj = null;
    String path = DIR_PREFIX + name;
    try {
      RMIAccess t = new RMIAccessImpl(bp, rsf, rsf);
      r.bind(path, t);
      rObj = t;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Failed rmi object bind ("+
            "name="+name+
            ", path="+path+
            ", reg="+r+")", e);
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Bound rmi object "+name+": "+rObj);
    }
    return rObj;
  }

  public static RMIAccess lookupAccess(
      Registry r,
      String name,
      LoggingService log) {
    if (log.isDebugEnabled()) {
      log.debug("Lookup rmi object "+name+" in "+r);
    }

    RMIAccess rObj = null;
    if (r != null) {
      String path = DIR_PREFIX + name;
      try {
        rObj = (RMIAccess) r.lookup(path);
      } catch (NotBoundException nbe) {
        if (log.isDebugEnabled()) {
          log.debug(
              "RMI object not bound ("+
              "name="+name+
              ", path="+path+
              ", reg="+r+")");
        }
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info(
              "Failed rmi object lookup ("+
              "name="+name+
              ", path="+path+
              ", reg="+r+")", e);
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Located rmi object "+name+": "+rObj);
    }
    return rObj;
  }

  public static Map getBundles(
      RMIAccess rObj,
      LoggingService log) {
    if (log.isDebugEnabled()) {
      log.debug("Lookup records in remote object "+rObj);
    }

    Map m = null;
    if (rObj != null) {
      try {
        m = rObj.getBundles();
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Failed remote object "+rObj+" invocation", e);
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Found remote object "+rObj+" map: "+m);
    }
    return m;
  }
  
  public static String getFilter(
      AddressEntry bootEntry,
      MessageAddress agentId,
      LoggingService log) {
    if (bootEntry != null) {
      URI uri = bootEntry.getURI();
      if (uri != null) {
        String path = uri.getPath();
        if (path != null && path.length() > 0) {
          String s = path.substring(1);
          if (!"*".equals(s)) {
            return s;
          }
        }
      }
    }
    return (agentId == null ? null : agentId.getAddress());
  }

  public static Map filterBundles(
      Map m,
      String filter,
      LoggingService log) {
    if (filter == null || m == null || m.isEmpty()) {
      if (log.isDebugEnabled()) {
        log.debug("no-op filterBundles("+m+", "+filter+")");
      }
      return m;
    }
    Bundle b = (Bundle) m.get(filter);
    if (b == null) {
      if (log.isDebugEnabled()) {
        log.debug("empty filterBundles("+m+", "+filter+")");
      }
      return Collections.EMPTY_MAP;
    }
    if (log.isDebugEnabled()) {
      log.debug("singleton filterBundles("+m+", "+filter+")="+b);
    }
    return Collections.singletonMap(filter, b);
  }
}
