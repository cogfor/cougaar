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

import java.net.URI;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Map;

import org.cougaar.core.service.SocketFactoryService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.DiscoveryBase;

/**
 * This component discovers bundles through RMI registry polling.
 * <p>
 * It looks in the {@link ConfigService} for config entries of type
 * "-RMI_REG" and scheme "rmi", e.g.<pre>
 *   X={-RMI_REG=rmi://test.com:8888/Y}
 * </pre>
 * and then polls the RMI registry on that host:port for an RMI
 * stub with name "COUGAAR_NAMING/X" for agent "Y"'s bundles.
 * These bundles are then copied into the {@link 
 * org.cougaar.core.wp.bootstrap.DiscoveryService}.
 */
public class RMIDiscovery
extends DiscoveryBase
{
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

  private SocketFactoryService socketFactoryService;

  private final Object socFacLock = new Object();
  private RMISocketFactory socFac;

  public void setSocketFactoryService(
      SocketFactoryService socketFactoryService) {
    this.socketFactoryService = socketFactoryService;
  }

  @Override
protected String getConfigPrefix() {
    return "org.cougaar.core.wp.resolver.rmi.";
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
      sb.releaseService(
          configClient, ConfigService.class, configService);
      configService = null;
    }

    super.unload();
  }

  private RMISocketFactory getRMISocketFactory() {
    synchronized (socFacLock) {
      if (socFac == null) {
        socFac = RMIUtil.getRMISocketFactory(socketFactoryService);
      }
      return socFac;
    }
  }

  protected AddressEntry getBootEntry(Bundle b) {
    return RMIUtil.getBootEntry(b);
  }

  @Override
protected Map lookup(Object bootObj) {
    throw new InternalError("should use RMIPoller!");
  }

  @Override
protected Poller createPoller(Object bootObj) {
    return new RMIPoller(bootObj);
  }

  private class RMIPoller extends Poller {

    private final AddressEntry bootEntry;

    private String filter;

    // this is the active RMI RemoteObject that was found
    // in the RMI registry upon successful lookup.
    private RMIAccess rmiAccess;

    public RMIPoller(Object bootObj) {
      super(bootObj);
      bootEntry = (AddressEntry) bootObj;
    }

    @Override
   protected void initialize() {
      super.initialize();

      filter = RMIUtil.getFilter(bootEntry, null, log);
    }

    @Override
   public void stop() {
      rmiAccess = null;
    }

    @Override
   protected Map lookup() {
      String name = bootEntry.getName();
      URI uri = bootEntry.getURI();
      String host = uri.getHost();
      int port = uri.getPort();

      if (rmiAccess == null) {
        // lookup/create registry
        RMISocketFactory rsf = getRMISocketFactory();
        Registry r =
          RMIUtil.lookupRegistry(rsf, host, port, log);
        if (r == null) {
          return null;
        }

        // lookup/create exported rmi object
        RMIAccess newAccess = 
          RMIUtil.lookupAccess(r, name, log);
        if (newAccess == null) {
          return null;
        }

        rmiAccess = newAccess;
      }

      // lookup the entries in the rmi object
      Map newFound = RMIUtil.getBundles(rmiAccess, log);
      if (newFound == null) {
        return null;
      }

      // filter for specific agent
      newFound = RMIUtil.filterBundles(newFound, filter, log); 

      return newFound;
    }
  }
}
