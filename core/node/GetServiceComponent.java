/*
 * <copyright>
 *  
 *  Copyright 2001-2007 BBNT Solutions, LLC
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

import java.util.List;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.Reflection;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component can be used to export an internal service out to an
 * external framework.
 * <p>
 * For example, the {@link NodeApplet} uses this component to get the
 * {@link NodeControlService}.
 * <p>
 * Two parameters are required:<br>
 * &nbsp;&nbsp; 1) The Service class or classname, which can be {@link ServiceBroker}.<br>
 * &nbsp;&nbsp; 2) The {@link GetServiceCallback} listener, which can be<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; specified as a class, classname, or instance.<br>
 * A third parameter is optional:<br>
 * &nbsp;&nbsp; 3) The service requestor, or "this" for this component, which<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; defaults to "this".<br>
 * A fourth parameter is optional:<br>
 * &nbsp;&nbsp; 4) Use a late-binding service listener if the service is not<br>
 * &nbsp;&nbsp;&nbsp;&nbsp; available at load time, defaults to "true".<br>
 * <p>
 * Reflection is used to wrap the callback as the GetServiceCallback API, even
 * if it doesn't implement that API.  This allows an external client to
 * specify:<pre>
 *    public class MyCallback {
 *      public void setService(Class cl, Object service) {..}
 *    }
 * </pre>
 * even though "MyCallback instanceof GetServiceCallback" is false.  This is
 * supported to avoid awkward compile and classloader dependencies.
 */
public class GetServiceComponent
extends GenericStateModelAdapter
implements Component {

  private ServiceBroker sb;
  private List params;

  private LoggingService log;

  private Class cl;
  private boolean is_sb;
  private Object svc;
  private GetServiceCallback cb;
  private Object req;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setParameter(Object o) {
    if (!(o instanceof List)) {
      throw new IllegalArgumentException(
          "Expecting a List, not "+
          (o == null ? "null" : o.getClass().getName()));
    }
    params = (List) o;
  }

  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    try {
      _load();
    } catch (Exception e) {
      throw new RuntimeException("Unable to load "+this, e);
    }
  }

  private void _load() throws Exception {
    // extract parameters
    int n = (params == null ? 0 : params.size());
    if (n < 2 || n > 4) {
      throw new RuntimeException("Expecting 2..4 parameters, not "+n);
    }
    Object cl_obj = params.get(0);
    Object cb_obj = params.get(1);
    Object req_obj = (n > 2 ? params.get(2) : null);
    Object is_late_obj = (n > 3 ? params.get(3) : "true");

    // get class
    if (cl_obj instanceof String) {
      cl_obj = Class.forName((String) cl_obj);
    }
    if (!(cl_obj instanceof Class)) {
      throw new RuntimeException("Expecting a Class or String, not "+cl_obj);
    }
    cl = (Class) cl_obj;
    is_sb = ServiceBroker.class.isAssignableFrom(cl);

    // get callback
    if (cb_obj == null) {
      throw new RuntimeException("Null callback_object");
    }
    if (cb_obj instanceof String) {
      cb_obj = Class.forName((String) cb_obj);
    }
    if (cb_obj instanceof Class) {
      cb_obj = ((Class) cb_obj).newInstance();
    }
    if (!(cb_obj instanceof GetServiceCallback)) {
      cb_obj = Reflection.makeProxy(cb_obj, GetServiceCallback.class);
    }
    cb = (GetServiceCallback) cb_obj;

    // get requestor, which is typically "this"
    if (req_obj == null || "this".equals(req_obj) || is_sb) {
      req_obj = this;
    }
    if (req_obj instanceof String) {
      req_obj = Class.forName((String) req_obj);
    }
    if (req_obj instanceof Class) {
      req_obj = ((Class) req_obj).newInstance();
    }
    req = req_obj;

    // get service
    if (is_sb || sb.hasService(cl)) {
      // found it
      svc = (is_sb ? sb : sb.getService(req, cl, null));
      cb.setService(cl, svc);
      return;
    }
    if (!"true".equals(is_late_obj)) {
      // fail
      cb.setService(cl, null);
      return;
    }
    // listen
    ServiceAvailableListener sal =
      new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent ae) {
          if (cl.isAssignableFrom(ae.getService())) {
            svc = sb.getService(req, cl, null);
            cb.setService(cl, svc);
            //sb.removeServiceListener(this);
          }
        }
      };
    sb.addServiceListener(sal);
  }

  @Override
public void unload() {
    if (svc != null) {
      try {
        cb.setService(cl, null);
      } catch (Exception e) {
      }
      if (!is_sb) {
        sb.releaseService(req, cl, svc);
      }
      svc = null;
    }

    cl = null;
    cb = null;
    req = null;

    if (log != null && log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }

    super.unload();
  }
}
