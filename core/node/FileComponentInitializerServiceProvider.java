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

package org.cougaar.core.node;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.ConfigFinder;

/**
 * {@link ServiceProvider} for the {@link
 * FileComponentInitializerServiceComponent} that uses the {@link
 * INIParser}. 
 */
public class FileComponentInitializerServiceProvider implements ServiceProvider {

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != ComponentInitializerService.class) {
      throw new IllegalArgumentException(
          getClass() + " does not furnish " + serviceClass);
    }
    return new ComponentInitializerServiceImpl();
  }
  
  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class ComponentInitializerServiceImpl implements ComponentInitializerService {
    /**
     * Get the descriptions of components with the named parent having
     * an insertion point below the given container insertion point.
     */
    public ComponentDescription[] getComponentDescriptions(
        String parentName, String containerInsertionPoint) throws InitializerException {
      try {
        // parse the file (we could cache this!)
        String filename = parentName;
        if (! parentName.endsWith(".ini")) {
          filename = parentName + ".ini";
        }
        InputStream in = ConfigFinder.getInstance().open(filename);
        ComponentDescription[] allDescs;
        try {
          allDescs = INIParser.parse(in);
        } finally {
          in.close();
        }

        // extract the components at the specified insertion point
        List descs = new ArrayList();
        String cpr = containerInsertionPoint+".";
        int cprl  = cpr.length();
        for (int i = 0, n = allDescs.length; i < n; i++) {
          ComponentDescription cd = allDescs[i];
          String ip = cd.getInsertionPoint();
          if (ip.startsWith(cpr) &&
              ip.indexOf(".", cprl+1) < 0) {
            descs.add(cd);
          }
        }

        // return as array
        return (ComponentDescription[])
          descs.toArray(
              new ComponentDescription[descs.size()]);
      } catch (Exception e) {
        throw new InitializerException(
            "getComponentDescriptions("+parentName+", "+containerInsertionPoint+")",
            e);
      }
    }

    public boolean includesDefaultComponents() {
      return false;
    }
  }
}
