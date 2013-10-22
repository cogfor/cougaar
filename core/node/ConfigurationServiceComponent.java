/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.cougaar.core.component.ComponentSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.ConfigurationService;
import org.cougaar.util.ConfigFinder;
import org.w3c.dom.Document;

/**
 * This component advertises the {@link ConfigurationService},
 * wrapping the {@link ConfigFinder}.
 */
public class ConfigurationServiceComponent
  extends ComponentSupport
{
  private final ConfigurationService cs;

  public ConfigurationServiceComponent() {
    cs = new CFAdapter(ConfigFinder.getInstance());
  }

  private ServiceBroker rootsb; /*the whole-node broker*/

  private ServiceProvider sp;   /*our service provider*/

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
    } else {
      rootsb = null;
    }
  }

  @Override
public void load() {
    super.load();
    sp = new SP();
    rootsb.addService(ConfigurationService.class, sp);
  }

  @Override
public void unload() {
    super.unload();
    rootsb.revokeService(ConfigurationService.class, sp);
    sp = null;
  }

  private class SP implements ServiceProvider {
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      return (ConfigurationService.class.equals(serviceClass) ? cs : null);
    }
    
    public void releaseService( ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
      // ignore
    }
  }
   
  private class CFAdapter implements ConfigurationService {
    private final ConfigFinder cf;
    private CFAdapter(ConfigFinder cf) { this.cf = cf; }

    public List getConfigurationPath() { return cf.getConfigPath(); }
    public File locateFile(String filename) { return cf.locateFile(filename); }
    public URL resolveName(String name) throws MalformedURLException { return cf.resolveName(name); }
    public InputStream open(String u) throws IOException { return cf.open(u); }
    public URL find(String u) throws IOException { return cf.find(u); }
    public Document parseXMLConfigFile(String f) throws IOException { return cf.parseXMLConfigFile(f); }
  }
}

  
