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

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ServletService;
import org.cougaar.util.annotations.Argument;
import org.cougaar.util.annotations.Cougaar;
import org.cougaar.util.Arguments;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.StateModelException;

/**
 * Abstract base-class for a Component that is also a Servlet.
 * <p>
 * This is very similar to BaseServletComponent, except that the component
 * itself is registered as the servlet.
 */
public abstract class ComponentServlet
      extends HttpServlet
      implements Component {

   private static final long serialVersionUID = 1L;

   // this class handles the "servletService" details:
   @Cougaar.ObtainService()
   public ServletService servletService;
   
   // the local agent address:
   @Cougaar.ObtainService()
   public AgentIdentificationService agentIdService;
   
   // default state model (no multiple inheritence!):
   private final GenericStateModel gsm = new GenericStateModelAdapter() {
   };

   private String myPath;

   private ServiceBroker serviceBroker;


   private MessageAddress agentId;
   private String encAgentName;


   /**
    * Get the path for the Servlet's registration.
    * <p>
    * Typically supplied by the component parameter, but subclasses can
    * hard-code the path by overriding this method.
    */
   protected String getPath() {
      return myPath;
   }

   protected MessageAddress getAgentIdentifier() {
      return agentId;
   }

   /** URL-encoded name of the local agent */
   protected String getEncodedAgentName() {
      return encAgentName;
   }

   public void setBindingSite(BindingSite bindingSite) {
   }

   public void setServiceBroker(ServiceBroker sb) {
      this.serviceBroker = sb;
   }

   protected <T extends Service> T getService(Object requestor, Class<T> serviceClass, ServiceRevokedListener srl) {
      return serviceBroker.getService(requestor, serviceClass, srl);
   }
   
   protected  <T extends Service> void releaseService(Object requestor, Class<T> serviceClass, T service) {
      serviceBroker.releaseService(requestor, serviceClass, service);
   }

   /**
    * Handle servlet path argument, which is not necessarily in the name=value form.
    */
   public void setParameter(Object o) {
      String candidate = null;
      if (o instanceof String) {
         candidate = (String) o;
      } else if (o instanceof List) {
         List stringsList = (List) o;
         if (!stringsList.isEmpty()) {
            Object o1 = stringsList.get(0);
            if (o1 instanceof String) {
               candidate = (String) o1;
            }
         }
      }
      if (candidate != null) {
         if (candidate.startsWith("path=")) {
            candidate = candidate.substring("path=".length());
         }
         myPath = candidate;
      }
   }
   
   /**
    * Standard support for annotated Args.
    * Can't handle path myPath this way since it isn't in the right form. [?]
    */
   public void setArguments(Arguments args) {
      try {
          new Argument(args).setAllFields(this);
      } catch (Exception e) {
         LoggingService log = serviceBroker.getService(this, LoggingService.class, null);
         if (log != null && log.isWarnEnabled()) {
            log.warn("Unable to set arguments");
         }
      }
  }


   public void initialize()
         throws StateModelException {
      gsm.initialize();
   }

   public void load() {
      gsm.load();
      
      if (agentIdService == null) {
         LoggingService log = serviceBroker.getService(this, LoggingService.class, null);
         if (log != null && log.isWarnEnabled()) {
            log.warn("Unable to obtain agent id service");
         }
         return;
      }
      this.agentId = agentIdService.getMessageAddress();
      if (agentId != null) {
         try {
            String name = agentId.getAddress();
            encAgentName = URLEncoder.encode(name, "UTF-8");
         } catch (java.io.UnsupportedEncodingException e) {
            // should never happen
            throw new RuntimeException("Unable to encode to UTF-8?");
         }
      }

      if (servletService == null) {
         LoggingService log = serviceBroker.getService(this, LoggingService.class, null);
         if (log != null && log.isWarnEnabled()) {
            log.warn("Unable to obtain servlet service");
         }
         return;
      }

      // register this servlet
      String path = getPath();
      try {
         servletService.register(path, this);
      } catch (Exception e) {
         String failMsg = (path == null ? "Servlet path not specified" : "Unable to register servlet with path \"" + path + "\"");
         throw new RuntimeException(failMsg, e);
      }

      // unlike ComponentPlugin, we typically do NOT want
      // BlackboardService or AlarmService or SchedulerService,
      // since servlets run in the servlet server's thread.
      //
      // see the BlackboardQueryService for simple blackboard
      // access.
   }

   public void start()
         throws StateModelException {
      gsm.start();
   }

   public void suspend()
         throws StateModelException {
      gsm.suspend();
   }

   public void resume()
         throws StateModelException {
      gsm.resume();
   }

   public void stop()
         throws StateModelException {
      gsm.stop();
   }

   public void halt()
         throws StateModelException {
      gsm.halt();
   }

   public int getModelState() {
      return gsm.getModelState();
   }

   public void unload() {
      gsm.unload();
   }
}
