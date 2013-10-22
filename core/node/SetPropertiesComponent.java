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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component can be used to set {@link SystemProperties} values
 * and initialize the properties map.
 * <p>
 * For an example, see {@link NodeApplet}'s "createProperties()" method.
 * <p>
 * Each parameter must be one of:<ul>
 *   <li>A Properties object</li>
 *   <li>A Map of String names to String values</li>
 *   <li>A Collection of String name=value pairs</li>
 *   <li>A single String name=value pair</li>
 * </ul>
 * <p>
 * Four special properties are filtered out and processed before all
 * others, and in the following order:<ol>
 *   <li>"override_props":  If true, call 
 *       {@link SystemProperties#overrideProperties} with an
 *       empty Properties table.</li>
 *   <li>"put_system_props":  If true, copy all System properties
 *       into the SystemProperties table.</li> 
 *   <li>"put_applet_props":  If true, copy all System properties
 *       that Applets are allowed to access into the SystemProperties
 *       table.</li> 
 *   <li>"finalize_props":  If true, call
 *       {@link SystemProperties.finalizeProperties}.</li>
 * </ol>
 * <p>
 * Property values that specify "$" variables are expanded in place, using the
 * SystemProperties "resolveEnv" and "expandProperties" methods.
 */
public class SetPropertiesComponent
extends GenericStateModelAdapter
implements Component {

  // applets are following to get the following properties
  private static final String[] APPLET_PROPS = new String[] {
    "file.separator",
    "java.class.version",
    "java.vendor",
    "java.vendor.url",
    "java.version",
    "line.separator",
    "os.arch",
    "os.name",
    "path.separator",
  };

  private Object param;

  public void setServiceBroker(ServiceBroker sb) {
    // ignore
  }

  public void setParameter(Object o) {
    param = o;
  }

  @Override
public void load() {
    super.load();

    // parse parameters
    Map m = parse(param);
    param = null;

    // check for special control options
    boolean override_props = "true".equals(m.remove("override_props"));
    boolean put_system_props = "true".equals(m.remove("put_system_props"));
    boolean put_applet_props = "true".equals(m.remove("put_applet_props"));
    boolean finalize_props = "true".equals(m.remove("finalize_props"));

    if (override_props) {
      SystemProperties.overrideProperties(new Properties());
    }
    if (put_system_props) {
      Properties props = System.getProperties();
      for (Enumeration en = props.propertyNames(); en.hasMoreElements(); ) {
        String key = (String) en.nextElement();
        String value = props.getProperty(key);
        SystemProperties.setProperty(key, value);
      }
    }
    if (put_applet_props) {
      for (int i = 0; i < APPLET_PROPS.length; i++) {
        String key = APPLET_PROPS[i];
        String value = System.getProperty(key);
        if (value != null) {
          SystemProperties.setProperty(key, value);
        }
      }
    }
    if (finalize_props) {
      SystemProperties.finalizeProperties();
    }

    // remember if there are any pending $s 
    boolean any_dollars = false;

    for (Iterator iter = m.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry me = (Map.Entry) iter.next();

      String key = (String) me.getKey();
      String value = (String) me.getValue();

      if (key.startsWith("-D")) {
        key = key.substring(2);
      }
      if (value == null) {
        value = "";
      }

      // expand "-Dx=$COUGAAR_INSTALL_PATH"
      boolean windows = false; // use unix style regardless of OS
      value = SystemProperties.resolveEnv(value, windows);
      if (value.indexOf('$') >= 0) {
        any_dollars = true;
      }

      if (key.startsWith("unset(") && key.endsWith(")")) {
        key = key.substring(6, key.length()-1);
        value = null;
      }

      // set
      SystemProperties.setProperty(key, value);
    }

    if (any_dollars) {
      // resolve "-Dx=\\${org.cougaar.install.path}/blah" property references.
      SystemProperties.expandProperties();
    }
  }

  private Map parse(Object o) {
    Map ret = new HashMap();
    parse(param, ret);
    return ret;
  }

  // recursive!
  private void parse(Object o, Map toMap) {
    if (o instanceof String) {
      // base case
      String s = (String) o;
      int sep = s.indexOf('=');
      if (sep < 0) {
        toMap.put(s, "");
      } else {
        String key = s.substring(0,sep).trim();
        String value = s.substring(sep+1).trim();
        toMap.put(key, value);
      }
    } else if (o instanceof Collection) {
      // typical case, name=value pairs
      Collection c = (Collection) o;
      for (Iterator iter = c.iterator(); iter.hasNext(); ) {
        Object oi = iter.next();
        // recurse!
        parse(oi, toMap);
      }
    } else if (o instanceof Map) {
      toMap.putAll((Map) o);
    } else if (o instanceof Properties) {
      Properties props = (Properties) o;
      for (Enumeration en = props.propertyNames(); en.hasMoreElements(); ) {
        String key = (String) en.nextElement();
        String value = props.getProperty(key);
        toMap.put(key, (value == null ? "" : value));
      }
    } else {
      throw new RuntimeException(
          "Invalid parameter type: "+
          (o == null ? "null" : o.getClass().getName()));
    }
  }

}
