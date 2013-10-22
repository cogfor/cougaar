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

package org.cougaar.core.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection helper methods.
 */
public final class Reflection {

  private Reflection() {}

  /**
   * Create a reflective proxy from one class to an interface with matching
   * method names (but not necessarily an "instanceof").
   * <p>
   * For example, this utility method can be used to wrap class:<pre>
   *   public class Impl {
   *     public void foo() { .. }
   *   }
   * </pre>
   * as interface:<pre>
   *   public interface Service {
   *     void foo();
   *   }
   * </pre>
   * using:<pre>
   *   Impl x = new Impl();
   *   Service s = (Service) Reflection.makeProxy(x, Service.class);
   * </pre>
   * even though "x instanceof Service" is false.  The only requirement is that
   * Impl have all the methods defined in Service.
   * <p>
   * We primarily use this technique to avoid compile and classloader problems
   * when importing/exporting services from/to an external Node container.
   */
  public static Object makeProxy(
      final Object from_obj, final Class to_cl) throws Exception {
    // validate
    if (from_obj == null) {
      throw new IllegalArgumentException("null from_obj");
    }
    if (to_cl == null || !to_cl.isInterface()) {
      throw new IllegalArgumentException(
          "to_class must be an interface, not "+to_cl);
    }
    final Class from_cl = from_obj.getClass();

    // create mapping of "to_cl" to our "from_cl"
    //
    // To help avoid confusion, we'll use "in/out" names instead of
    // "to/from" names, since we're creating a reverse mapping
    final Map mapping = new HashMap();
    Method[] in_ma = to_cl.getMethods();
    Method[] out_ma = from_cl.getMethods();
    for (int i = 0; i < in_ma.length; i++) {
      Method in_m = in_ma[i];
      String in_name = in_m.getName();
      int in_argc = in_m.getParameterTypes().length;
      Method out_m = null;
      for (int j = 0; j < out_ma.length; j++) {
        Method oj_m = out_ma[j];
        String oj_name = oj_m.getName();
        if (!in_name.equals(oj_name)) continue;
        int oj_argc = oj_m.getParameterTypes().length;
        if (in_argc != oj_argc) continue;
        // assume no awkward polymorphism, e.g. no
        //    void foo(X x)
        //    void foo(Y y)
        out_m = oj_m;
        break;
      }
      if (out_m == null) {
        throw new RuntimeException(
            "Unable to find mapping from "+to_cl+" to "+from_cl+
            " for method "+in_m);
      }
      mapping.put(in_m, out_m);
    }

    // create method handler
    InvocationHandler ih = new InvocationHandler() {
      public Object invoke(
          Object proxy, Method in_m, Object[] args) throws Throwable {
        Method out_m = (Method) mapping.get(in_m);
        if (out_m == null) {
          throw new RuntimeException(
              "Internal error, unable to map "+in_m+" to "+
              from_cl+" method in "+mapping);
        }
        return out_m.invoke(from_obj, args);
      }
    };

    // create proxy
    Object ret =
      Proxy.newProxyInstance(
          to_cl.getClassLoader(), new Class[] { to_cl }, ih);
    return ret;
  }
}
