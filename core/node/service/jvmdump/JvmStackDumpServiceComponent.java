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

package org.cougaar.core.node.service.jvmdump;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.JvmStackDumpService;

/**
 * This component provides the {@link JvmStackDumpService}.
 * <p>
 * This is simply a node-level wrapper for our package-private
 * JNI implementation (JniStackDump).
 * <p>
 * This can be loaded in a node by adding this line to a
 * node's ".ini" or CSMART configuration:
 * <pre>
 *   Node.AgentManager.Agent.JvmDump = org.cougaar.core.node.service.jvmdump.JvmStackDumpServiceComponent
 * </pre>
 */
public class JvmStackDumpServiceComponent 
extends org.cougaar.util.GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private ServiceProvider mySP;

  // ignore "setServiceBroker", we want the node-level service broke

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs != null) {
      this.sb = ncs.getRootServiceBroker();
    }
  }

  @Override
public void load() {
    super.load();
    // create and advertise our service
    this.mySP = new JvmStackDumpServiceProviderImpl();
    sb.addService(JvmStackDumpService.class, mySP);
  }

  @Override
public void unload() {
    // revoke our service
    if (mySP != null) {
      sb.revokeService(JvmStackDumpService.class, mySP);
      mySP = null;
    }
    super.unload();
  }


  /**
   * Service provider for our <code>JvmStackDumpService</code>.
   */
  private class JvmStackDumpServiceProviderImpl
  implements ServiceProvider {

    // single service instance, since it's just a wrapper
    private final JvmStackDumpServiceImpl SINGLETON =
      new JvmStackDumpServiceImpl();

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (serviceClass != JvmStackDumpService.class) {
        throw new IllegalArgumentException(
            "JvmStackDumpService does not provide a service for: "+
            serviceClass);
      }
      return SINGLETON;
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      if (!(service instanceof JvmStackDumpServiceImpl)) {
        throw new IllegalArgumentException(
            "JvmStackDumpService unable to release service: "+
            ((service != null) ? service.getClass().toString() : "null"));
      }
      // ignore our singleton
    }

    private class JvmStackDumpServiceImpl
    implements JvmStackDumpService {
      public boolean dumpStack() {
        // call our package-private implementation!
        return JniStackDump.dumpStack();
      }
    }
  }
}

