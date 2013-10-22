/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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
import java.util.Iterator;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ComponentSupport;

/**
 * This component simply copies its parameters into Java's System Properties.
 * <p>
 * Note that only "-D" system properties are supported.  There's no way to
 * set "-X" JVM properties at runtime.  Also, some "-Ds" may have already
 * been used at this point, such as the Cougaar bootstrapper's
 * "-Dorg.cougaar.install.path".
 * <p>
 * Example usage:<pre>
 *   &lt;component class='org.cougaar.core.node.SetProperties'&gt;
 *     &lt;argument&gt;-Dx=y&lt;/argument&gt;
 *     &lt;argument&gt;-Dfoo=bar&lt;/argument&gt;
 *   &lt;/component&gt;
 * </pre>
 */
public class SetProperties extends ComponentSupport {

  public void setParameter(Object o) {
    // remember if there are any pending $s 
    boolean any_dollars = false;

    for (Iterator iter = ((Collection) o).iterator(); iter.hasNext(); ) {
      String s = (String) iter.next();

      // expand "-Dx=$COUGAAR_INSTALL_PATH"
      boolean windows = false; // use unix style regardless of OS
      s = SystemProperties.resolveEnv(s, windows);
      if (s.indexOf('$') >= 0) {
        any_dollars = true;
      }

      // parse
      String key;
      String value;
      int sep = s.indexOf('=');
      if (sep < 0) {
        key = s;
        value = "";
      } else {
        key = s.substring(0,sep).trim();
        value = s.substring(sep+1).trim();
      }
      if (key.startsWith("unset(") && key.endsWith(")")) {
        key = key.substring(6, key.length()-1);
        value = null;
      }
      if (!key.startsWith("-D")) {
        throw new IllegalArgumentException("Expecting a \"-D\", not "+s);
      }

      // set
      SystemProperties.setProperty(key, value);
    }

    if (any_dollars) {
      // resolve "-Dx=\\${org.cougaar.install.path}/blah" property references.
      SystemProperties.expandProperties();
    }
  }
}
