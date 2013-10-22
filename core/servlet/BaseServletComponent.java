/*
 * <copyright>
 *  
 *  Copyright 2000-2004 BBNT Solutions, LLC
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

package org.cougaar.core.servlet;

import javax.servlet.Servlet;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.ServletService;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * Abstract base-class for a Component that obtains the ServletService and
 * registers a Servlet.
 */
public abstract class BaseServletComponent
      extends GenericStateModelAdapter
      implements Component {
   // subclasses are free to use both of these:
   private BindingSite bindingSite;
   private ServiceBroker serviceBroker;

   @Cougaar.ObtainService()
   public ServletService servletService;

   public BaseServletComponent() {
      super();
   }

   /**
    * Get the path for the Servlet's registration.
    */
   protected abstract String getPath();

   /**
    * Create the Servlet instance.
    * <p>
    * This is done within "load()", and is also a good time to aquire additional
    * services from the "serviceBroker" (for example, BlackboardService).
    */
   protected abstract Servlet createServlet();

   protected <T extends Service> T getService(Object requestor, Class<T> serviceClass, ServiceRevokedListener srl) {
      return serviceBroker.getService(requestor, serviceClass, srl);
   }
   
   protected  <T extends Service> void releaseService(Object requestor, Class<T> serviceClass, T service) {
      serviceBroker.releaseService(requestor, serviceClass, service);
   }
   
   protected void activate(Object target) {
      BindingUtility.activate(target, bindingSite, serviceBroker);
   }
   
   public void setBindingSite(BindingSite bindingSite) {
      this.bindingSite = bindingSite;
   }

   public void setServiceBroker(ServiceBroker sb) {
      this.serviceBroker = sb;
   }

   /**
    * Capture the (optional) load-time parameters.
    * <p>
    * This is typically a List of Strings.
    */
   public void setParameter(Object o) {
   }

   @Override
   public void load() {
      super.load();

      if (servletService == null) {
         throw new RuntimeException("Unable to obtain servlet service");
      }

      // get the path for the servlet
      String path = getPath();

      // load the servlet instance
      Servlet servlet = createServlet();

      // register the servlet
      if (servlet != null) {
         try {
            servletService.register(path, servlet);
         } catch (Exception e) {
            String message = "Unable to register servlet \"" + servlet.getClass().getName() + "\" with path \"" + path  + "\"";
            throw new RuntimeException(message, e);
         }
      }
   }
}
