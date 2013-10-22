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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.IdentityHashSet;

/**
 * This component runs in white pages servers to advertise
 * locally-bound {@link Bundle}s from the {@link
 * org.cougaar.core.wp.resolver.LeaseManager} through the
 * advertisers (rmi, http, multicast, etc).
 */
public class AdvertiseManager
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;

  private BundleService bundleService;

  private final Object lock = new Object();

  private Map bundles = Collections.EMPTY_MAP;

  private AdvertiseSP advertiseSP;

  private final Set advertisers = new IdentityHashSet();

  private final BundleService.Client bundleClient = 
    new BundleService.Client() {
      public void add(String name, Bundle bundle) {
        AdvertiseManager.this.add(name, bundle);
      }
      public void addAll(Map m) {
        AdvertiseManager.this.updateAll(m);
      }
      public void change(String name, Bundle bundle) {
        AdvertiseManager.this.change(name, bundle);
      }
      public void remove(String name, Bundle bundle) {
        AdvertiseManager.this.remove(name, bundle);
      }
    };

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  @Override
public void load() {
    super.load();

    bundleService = sb.getService(bundleClient, BundleService.class, null);
    if (bundleService == null) {
      throw new RuntimeException("Unable to obtain BundleService");
    }

    // advertise our service
    advertiseSP = new AdvertiseSP();
    sb.addService(AdvertiseService.class, advertiseSP);
  }

  @Override
public void unload() {
    if (advertiseSP != null) {
      sb.revokeService(AdvertiseService.class, advertiseSP);
      advertiseSP = null;
    }

    if (bundleService != null) {
      sb.releaseService(
          bundleClient, BundleService.class, bundleService);
      bundleService = null;
    }

    super.unload();
  }

  private void updateAll(Map m) {
    for (Iterator iter = m.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Bundle bundle = (Bundle) me.getValue();
      add(name, bundle);
    }
  }
  private void add(String name, Bundle bundle) {
    update(true, name, bundle);
  }
  private void change(String name, Bundle bundle) {
    update(true, name, bundle);
  }
  private void remove(String name, Bundle bundle) {
    update(false, name, bundle);
  }

  private void update(boolean add, String name, Bundle bundle) {
    if (log.isDebugEnabled()) {
      log.debug("update("+add+", "+name+", "+bundle+")");
    }
    synchronized (lock) {
      // update bundles
      Bundle b = (Bundle) bundles.get(name);
      boolean put = false;
      boolean remove = false;
      if (b == null) {
        if (add) {
          put = true;
        } else {
          return;
        }
      } else {
        if (add) {
          if (bundle.equals(b)) {
            return;
          }
          put = true;
        } else {
          remove = true;
        }
      }
      // copy-on-write
      Map nb = new HashMap(bundles);
      if (put) {
        nb.put(name, bundle);
      } else if (remove) {
        nb.remove(name);
      }
      bundles = Collections.unmodifiableMap(nb);
      // tell our clients
      for (Iterator iter = advertisers.iterator(); iter.hasNext(); ) {
        AdvertiseService.Client asc = (AdvertiseService.Client)
          iter.next();
        if (b == null) {
          asc.add(name, bundle);
        } else if (add) {
          asc.change(name, bundle);
        } else {
          asc.remove(name, b);
        }
      }
    }
  }

  private void register(AdvertiseService.Client asc) {
    synchronized (lock) {
      // add to clients
      if (!advertisers.add(asc)) {
        return;
      }
      // fill with bundles
      for (Iterator iter = bundles.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Bundle bundle = (Bundle) me.getValue();
        asc.add(name, bundle);
      }
    }
  }

  private void unregister(AdvertiseService.Client asc) {
    synchronized (lock) {
      // remove from clients
      advertisers.remove(asc);
    }
  }

  private Bundle getBundle(String name) {
    synchronized (lock) {
      // get bundle
      return (Bundle) bundles.get(name);
    }
  }

  private Map getAllBundles() {
    synchronized (lock) {
      return bundles;
    }
  }

  private class AdvertiseSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!AdvertiseService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        AdvertiseService.Client client =
          (requestor instanceof AdvertiseService.Client ?
           (AdvertiseService.Client) requestor :
           null);
        AdvertiseServiceImpl asi = new AdvertiseServiceImpl(client);
        if (client != null) {
          AdvertiseManager.this.register(client);
        }
        return asi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof AdvertiseServiceImpl)) {
          return;
        }
        AdvertiseServiceImpl asi = (AdvertiseServiceImpl) service;
        AdvertiseService.Client client = asi.client;
        if (client != null) {
          AdvertiseManager.this.unregister(client);
        }
      }
      private class AdvertiseServiceImpl 
        implements AdvertiseService {
          protected final Client client;
          public AdvertiseServiceImpl(Client client) {
            this.client = client;
          }
          public Bundle getBundle(String name) {
            return AdvertiseManager.this.getBundle(name);
          }
          public Map getAllBundles() {
            return AdvertiseManager.this.getAllBundles();
          }
        }
    }


}
