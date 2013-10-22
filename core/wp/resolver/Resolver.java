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

package org.cougaar.core.wp.resolver;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is the front-end for the client-side white pages
 * resolver, which advertises the {@link WhitePagesService}.
 * <p>
 * This is really just a front-end to the {@link CacheManager}'s
 * {@link LookupService} and the {@link LeaseManager}'s {@link
 * ModifyService}.
 */
public class Resolver
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;
  private ServiceBroker rootsb;
  private LoggingService log;

  private CacheService cacheService;
  private LeaseService leaseService;
  private ServiceProvider whitePagesSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setNodeControlService(NodeControlService ncs) {
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  @Override
public void load() {
    super.load();

    if (log.isDebugEnabled()) {
      log.debug("Loading resolver");
    }

    // get the key services that should be created by our
    // subcomponents.
    cacheService = sb.getService(this, CacheService.class, null);
    if (cacheService == null) {
      throw new RuntimeException(
          "Unable to obtain CacheService");
    }
    leaseService = sb.getService(this, LeaseService.class, null);
    if (leaseService == null) {
      throw new RuntimeException(
          "Unable to obtain LeaseService");
    }

    whitePagesSP = new WhitePagesSP();
    rootsb.addService(WhitePagesService.class, whitePagesSP);

    if (log.isInfoEnabled()) {
      log.info("Loaded white pages resolver");
    }
  }

  @Override
public void unload() {
    super.unload();

    // revoke white pages service
    if (whitePagesSP != null) {
      rootsb.revokeService(WhitePagesService.class, whitePagesSP);
      whitePagesSP = null;
    }

    if (leaseService != null) {
      sb.releaseService(
          this, LeaseService.class, leaseService);
      leaseService = null;
    }
    if (cacheService != null) {
      sb.releaseService(
          this, CacheService.class, cacheService);
      cacheService = null;
    }

    if (log != null) {
      sb.releaseService(
          this, LoggingService.class, log);
      log = null;
    }
  }

  private class WhitePagesSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (WhitePagesService.class.isAssignableFrom(serviceClass)) {
          String agent =
            ((requestor instanceof ResolverClient) ?
             ((ResolverClient) requestor).getAgent() :
             null); // assume it's our node-agent
          return new WhitePagesS(agent);
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  private class WhitePagesS
    extends WhitePagesService {

      private final String agent;

      public WhitePagesS(String agent) {
        this.agent = agent;
      }

      @Override
      public Response submit(Request req) {
        if (log.isDetailEnabled()) {
          log.detail("Resolver intercept wp request: "+req);
        }
        Response res = req.createResponse();

        cacheService.submit(res);

        boolean bind = 
          (req instanceof Request.Bind ||
           req instanceof Request.Unbind);

        if (bind) {
          leaseService.submit(res, agent);
        }
        return res;
      }
    }
}
