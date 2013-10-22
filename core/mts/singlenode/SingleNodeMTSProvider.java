/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.mts.singlenode;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a baseline implementation of a single-node
 * {@link MessageTransportService}.
 * <p>
 * Consists of only a registry, a router, and wp service loading. 
 */
public final class SingleNodeMTSProvider 
    extends GenericStateModelAdapter
    implements Component, ServiceProvider
{
  private final static String NOT_A_CLIENT =
    "Requestor is not a MessageTransportClient";

  private ServiceBroker sb;
  protected LoggingService loggingService;
  private SingleNodeMTSProxy proxy;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  // does all loading of services
  @Override
public void load() {
    super.load();

    loggingService = 
      sb.getService(this, LoggingService.class, null);

    // Make router
    SingleNodeRouterImpl router = new SingleNodeRouterImpl(sb);

    // Make proxy
    proxy = new SingleNodeMTSProxy(router);

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);

    ServiceBroker rootsb = ncs.getRootServiceBroker();
    rootsb.addService(MessageTransportService.class, this);
  }

  // ServiceProvider
  public Object getService(
      ServiceBroker sb, 
      Object requestor, 
      Class serviceClass) {
    if (serviceClass == MessageTransportService.class) {
      if (requestor instanceof MessageTransportClient) {
        return proxy;
      } else {
        throw new IllegalArgumentException(NOT_A_CLIENT);
      }
    } else {
      return null;
    }
  }

  public void releaseService(
      ServiceBroker sb, 
      Object requestor, 
      Class serviceClass, 
      Object service) {
  }
}
