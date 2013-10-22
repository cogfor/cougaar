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
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;

/**
 * A utility class to hide late-binding {@link Service} lookups.
 * <p> 
 * This could be moved into org.cougaar.core.component.
 */
public class ServiceFinder {

  private ServiceFinder() {}

  public interface Callback {
    void foundService(Service s);
  }

  public static boolean findServiceLater(
      final ServiceBroker sb,
      final Class cl,
      final Object requestor,
      final Callback cb) {
    final Object req = 
      (requestor == null ?
       ServiceFinder.class :
       requestor);
    if (sb.hasService(cl)) {
      Service s = (Service) sb.getService(req, cl, null);
      cb.foundService(s);
      return true;
    }
    ServiceAvailableListener sal =
      new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent ae) {
          if (cl.isAssignableFrom(ae.getService())) {
            Service s = (Service) sb.getService(req, cl, null);
            cb.foundService(s);
            //sb.removeServiceListener(this);
          }
        }
      };
    sb.addServiceListener(sal);
    return false;
  }
}
