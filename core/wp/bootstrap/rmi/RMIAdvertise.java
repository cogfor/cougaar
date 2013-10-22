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
import org.cougaar.core.wp.bootstrap.AdvertiseBase;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.Util;

/**
 * This component advertises bundles through an RMI registry stub.
 * <p>
 * It looks in the {@link ConfigService} for config entries of type
 * "-RMI_REG" and scheme "rmi", e.g.<pre>
 *   X={-RMI_REG=rmi://test.com:8888/Y}
 * </pre>
 * and if the localhost is "test.com" and the local agent is "Y"
 * (or this path is "*") then this component attempts to create
 * the RMI registry and bind "COUGAAR_NAMING/X" in it.  The bound
 * {@link RMIAccess} stub responds to "getBundles()" with the bundles
 * tracked by the {@link
 * org.cougaar.core.wp.bootstrap.AdvertiseService} (i.e. locally bound
 * leases).  These bundles are filtered for just agent "Y" (or no
 * filtering if this path is "*").
 */
public class RMIAdvertise
extends AdvertiseBase
{

  private ConfigService configService;

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

  private SocketFactoryService socFacService;

  private final Object socFacLock = new Object();
  private RMISocketFactory socFac;

  public void setSocketFactoryService(
      SocketFactoryService socFacService) {
    this.socFacService = socFacService;
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

  protected RMISocketFactory getRMISocketFactory() {
    synchronized (socFacLock) {
      if (socFac == null) {
        socFac = RMIUtil.getRMISocketFactory(socFacService);
      }
      return socFac;
    }
  }

  private AddressEntry getBootEntry(Bundle b) {
    AddressEntry ae = RMIUtil.getBootEntry(b);
    if (ae == null) {
      return null;
    }
    URI uri = ae.getURI();
    String host = (uri == null ? null : uri.getHost());
    if (!Util.isLocalHost(host)) {
      return null;
    }
    String filter = RMIUtil.getFilter(ae, agentId, log);
    if (!agentName.equals(filter)) {
      return null;
    }
    return ae;
  }

  @Override
protected Advertiser createAdvertiser(Object bootObj) {
    return new RMIAdvertiser(bootObj);
  }

  private class RMIAdvertiser extends Advertiser {

    private final AddressEntry bootEntry;

    private final String filter;

    // this is the active RMI RemoteObject that was found
    // in the RMI registry upon successful lookup.
    private RMIAccess rmiAccess;

    public RMIAdvertiser(Object bootObj) {
      super(bootObj);

      bootEntry = (AddressEntry) bootObj;

      filter = RMIUtil.getFilter(bootEntry, agentId, log);
    }

    @Override
   public void start() {
      String name = bootEntry.getName();
      URI uri = bootEntry.getURI();
      int port = uri.getPort();

      // assert Util.isLocalHost(uri.getHost());

      // create registry
      RMISocketFactory rsf = getRMISocketFactory();
      Registry r =
        RMIUtil.createRegistry(rsf, port, log);
      if (r == null) {
        // try again later?
        return;
      }

      // bind rmi object
      BundlesProvider bp = 
        new BundlesProvider() {
          public Map getBundles() {
            // serve remote caller
            return RMIAdvertiser.this.getBundles();
          }
        };
      RMIAccess newAccess = 
        RMIUtil.bindAccess(rsf, r, name, bp, log);
      if (newAccess == null) {
        // try again later?
        return; 
      }

      // save
      rmiAccess = newAccess;

      // okay, listening for calls to "getBundles()"

      // wake periodically to make sure we're still registered?
    }

    private Map getBundles() {
      // serve remote caller
      Map ret = RMIAdvertise.this.getBundles();
      // filter for specific agent
      ret = RMIUtil.filterBundles(ret, filter, log); 
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
      // not implemented yet
      //unbind from registry 
    }

    @Override
   public String toString() {
      return
        "(rmi_bootstrap "+
        super.toString()+
        ", rmiAccess="+rmiAccess+
        ")";
    }
  }
}
