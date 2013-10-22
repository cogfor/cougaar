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

package org.cougaar.core.wp.bootstrap;

import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component provides the {@link ConfigService} that loads the
 * static {@link ConfigReader} contents.
 * <p>
 * The ConfigService supports changes and removals, but for now
 * we don't use these methods. 
 */
public class ConfigManager
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private ConfigSP configSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    // advertise our service
    configSP = new ConfigSP();
    sb.addService(ConfigService.class, configSP);
  }

  @Override
public void unload() {
    if (configSP != null) {
      sb.revokeService(ConfigService.class, configSP);
      configSP = null;
    }

    super.unload();
  }

  protected Map getBundles() {
    return ConfigReader.getBundles();
  }

  private class ConfigSP implements ServiceProvider {
    private final ConfigService INSTANCE =
      new ConfigService() {
        public Map getBundles() {
          return ConfigManager.this.getBundles();
        }
      };
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!ConfigService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      if (requestor instanceof ConfigService.Client) {
        // initialize client
        ConfigService.Client csc = (ConfigService.Client) requestor;
        Map m = ConfigManager.this.getBundles();
        if (m != null) {
          for (Iterator iter = m.values().iterator();
              iter.hasNext();
              ) {
            Bundle b = (Bundle) iter.next();
            csc.add(b);
          }
        }
      }
      return INSTANCE;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
    }
  }
}
