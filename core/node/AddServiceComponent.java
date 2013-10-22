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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.util.Reflection;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component can be used to import an external service into the node agent.
 * <p>
 * For example, the {@link NodeApplet} uses this component to advertise the
 * {@link org.cougaar.core.service.AppletService}.
 * <p>
 * Two parameters are required:<br>
 * &nbsp;&nbsp; 1) The Service class or classname.<br>
 * &nbsp;&nbsp; 2) The Service class, classname, or instance, or the
 * &nbsp;&nbsp;&nbsp;&nbsp; ServiceProvider class, classname, or instance.<br>
 * A third parameter is optional:<br>
 * &nbsp;&nbsp; 3) "true" to use the root ServiceBroker, defaults to false.<br>
 * <p>
 * Reflection is used wrap the 2nd parameter as the correct API, even if it
 * doesn't implement the correct interface.  For example, an external client
 * can specify an implementation for:<pre>
 *   public interface FooService extends Service {
 *     void foo();
 *   }
 * </pre>
 * as:
 * <pre>
 *   public class MyFoo {
 *     public void foo() { .. }
 *   }
 * </pre>
 * even though "MyFoo instanceof FooService" is false.  This is supported to
 * avoid awkward compile and classloader dependencies.
 */
public class AddServiceComponent
extends GenericStateModelAdapter
implements Component {

  private ServiceBroker sb;
  private List params;

  private ServiceBroker the_sb;

  private Class cl;
  private ServiceProvider sp;

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

    try {
      _load();
    } catch (Exception e) {
      throw new RuntimeException("Unable to load "+this, e);
    }
  }

  private void _load() throws Exception {
    // extract parameters
    int n = (params == null ? 0 : params.size());
    if (n < 2 || n > 3) {
      throw new RuntimeException("Expecting 2..3 parameters, not "+n);
    }
    Object cl_obj = params.get(0);
    Object sv_obj = params.get(1);
    Object is_root_obj = (n > 2 ? params.get(2) : null);

    // select broker
    the_sb = sb;
    boolean is_root = "true".equals(is_root_obj);
    if (is_root) {
      NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
      if (ncs == null) {
        throw new RuntimeException("Unable to obtain NodeControlService");
      }
      the_sb = ncs.getRootServiceBroker();
      sb.releaseService(this, NodeControlService.class, ncs);
      if (the_sb == null) {
        throw new RuntimeException("Null root_sb");
      }
    }

    // get class
    if (cl_obj instanceof String) {
      cl_obj = Class.forName((String) cl_obj);
    }
    if (!(cl_obj instanceof Class)) {
      throw new RuntimeException("Expecting a Class or String, not "+cl_obj);
    }
    cl = (Class) cl_obj;

    // get svc
    if (sv_obj == null) {
      throw new RuntimeException("Null service_object");
    }
    if (sv_obj instanceof String) {
      sv_obj = Class.forName((String) sv_obj);
    }
    if (sv_obj instanceof Class) {
      sv_obj = ((Class) sv_obj).newInstance();
    }
    boolean inst_sp = (sv_obj instanceof ServiceProvider);
    boolean inst_cl = cl.isAssignableFrom(sv_obj.getClass());
    if (!inst_sp && !inst_cl) {
      // must use reflection
      Object o;
      try {
        // try to wrap as "cl"
        o = Reflection.makeProxy(sv_obj, cl);
        inst_cl = true;
      } catch (Exception e) {
        try {
          // try to wrap as "sp"
          o = Reflection.makeProxy(sv_obj, ServiceProvider.class);
          inst_sp = true;
        } catch (Exception e2) {
          // missing both cl and sp methods
          throw new RuntimeException(
              "Unable to create "+cl+" proxy for "+sv_obj.getClass(), e);
        }
      }
      sv_obj = o;
    }
    if (inst_cl && !inst_sp) {
      // wrap as provider
      inst_cl = false;
      inst_sp = true;
      final Object svc = sv_obj;
      sv_obj = new ServiceProvider() {
        public Object getService(
            ServiceBroker sb, Object requestor, Class serviceClass) {
          return (cl.isAssignableFrom(serviceClass) ? svc : null);
        }
        public void releaseService(
            ServiceBroker sb, Object requestor,
            Class serviceClass, Object service) {
        }
      };
    }
    sp = (ServiceProvider) sv_obj;

    // add the service
    the_sb.addService(cl, sp);
  }

  @Override
public void unload() {
    if (sp != null) {
      the_sb.revokeService(cl, sp);
      sp = null;
    }

    super.unload();
  }
}
