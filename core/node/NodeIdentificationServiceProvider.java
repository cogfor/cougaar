/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;

/**
 * {@link ServiceProvider} for the {@link NodeIdentificationService}.
 */
public class NodeIdentificationServiceProvider implements ServiceProvider {
  private MessageAddress nodeID;
  private InetAddress inetAddress;
  public NodeIdentificationServiceProvider(MessageAddress nodeID) {
    this.nodeID = nodeID;
    
    String addr = SystemProperties.getProperty("org.cougaar.node.inet.address");
    try {
        inetAddress = addr != null ? InetAddress.getByName(addr) : InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
        inetAddress = null;
    }
  }
  
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (NodeIdentificationService.class.isAssignableFrom(serviceClass)) {
      return new NodeIdentificationServiceProxy();
    } else {
      return null;
    }
  }
  
  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service)  {
  }

  private final class NodeIdentificationServiceProxy implements NodeIdentificationService {
    public MessageAddress getMessageAddress() {
      return nodeID;
    }
    public InetAddress getInetAddress() {
      return inetAddress;
    }
  } 
}
