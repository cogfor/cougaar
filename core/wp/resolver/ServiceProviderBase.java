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

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;

/**
 * A base class for a ServiceProvider with registered clients.
 * <p> 
 * This could be moved into org.cougaar.core.component.
 */
public abstract class ServiceProviderBase implements ServiceProvider {
  protected abstract void register(Object client);
  protected abstract void unregister(Object client);
  protected abstract Class getServiceClass();
  protected abstract Class getClientClass();
  protected abstract Service getService(Object client);
  public Object getService(
      ServiceBroker sb, Object requestor, Class serviceClass) {
    if (!getServiceClass().isAssignableFrom(serviceClass)) {
      return null;
    }
    Class reqcl = (requestor == null ? null : requestor.getClass());
    if (!(getClientClass().isAssignableFrom(reqcl))) {
      throw new IllegalArgumentException(
          getServiceClass().getName()+
          " requestor must implement "+
          getServiceClass().getName());
    }
    Service si = getService(requestor);
    register(requestor);
    return si;
  }
  public void releaseService(
      ServiceBroker sb, Object requestor,
      Class serviceClass, Object service) {
    if (!(service instanceof MyServiceImpl)) {
      return;
    }
    MyServiceImpl si = (MyServiceImpl) service;
    unregister(si.client);
  }
  protected abstract class MyServiceImpl implements Service {
    protected final Object client;
    public MyServiceImpl(Object client) {
      this.client = client;
    }
  }
}
