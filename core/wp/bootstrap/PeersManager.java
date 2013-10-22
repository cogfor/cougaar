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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component provides the {@link PeersService}, to tell the
 * server about their peer servers.
 * <p>
 * This is a very simple implementation that is based on the
 * ConfigService.  This could be enhanced to discover peers.
 */
public class PeersManager
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;

  private EnsureIsFoundService ensureService;

  private Set servers;

  private PeersSP peersSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  public void setEnsureIsFoundService(EnsureIsFoundService ensureService) {
    this.ensureService = ensureService;
  }

  @Override
public void load() {
    super.load();

    servers = findServers();

    ensureService = sb.getService(this, EnsureIsFoundService.class, null);
    if (ensureService == null) {
      if (log.isWarnEnabled()) {
        log.warn("Unable to obtain EnsureIsFoundService");
      }
    } else {
      // ensure that our peer servers are found
      for (Iterator iter = servers.iterator();
          iter.hasNext();
          ) {
        MessageAddress addr = (MessageAddress) iter.next();
        ensureService.add(addr.getAddress());
      }
    }

    // advertise our service
    peersSP = new PeersSP();
    sb.addService(PeersService.class, peersSP);
  }

  @Override
public void unload() {
    if (peersSP != null) {
      sb.revokeService(PeersService.class, peersSP);
      peersSP = null;
    }

    if (ensureService != null) {
      // no longer watching these servers
      for (Iterator iter = servers.iterator();
          iter.hasNext();
          ) {
        MessageAddress addr = (MessageAddress) iter.next();
        ensureService.remove(addr.getAddress());
      }
      sb.releaseService(
          this, EnsureIsFoundService.class, ensureService);
      ensureService = null;
    }

    super.unload();
  }

  protected Map getBundles() {
    return ConfigReader.getBundles();
  }

  protected Set findServers() {
    Set ret = new HashSet();

    Map m = getBundles();
    if (m != null) {
      for (Iterator iter = m.values().iterator();
          iter.hasNext();
          ) {
        Bundle b = (Bundle) iter.next();
        MessageAddress addr = Util.parseServer(b);
        if (addr != null) {
          ret.add(addr);
        }
      }
    }

    ret = Collections.unmodifiableSet(ret);
    return ret;
  }

  protected Set getServers() {
    return servers;
  }

  private class PeersSP implements ServiceProvider {
    private final PeersService INSTANCE =
      new PeersService() {
        public Set getServers() {
          return PeersManager.this.getServers();
        }
      };
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!PeersService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      if (requestor instanceof PeersService.Client) {
        // initialize client
        PeersService.Client psc =
          (PeersService.Client) requestor;
        Set s = PeersManager.this.getServers();
        if (s != null && !s.isEmpty()) {
          psc.addAll(s);
        }
      }
      return INSTANCE;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
    }
  }
}
