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

package org.cougaar.planning.ldm;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.planning.plugin.asset.AssetDataFileReader;
import org.cougaar.planning.plugin.asset.AssetDataReader;
import org.cougaar.planning.service.AssetInitializerService;

public class FileAssetInitializerServiceProvider implements ServiceProvider {

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != AssetInitializerService.class) {
      throw new IllegalArgumentException(
          getClass() + " does not furnish " + serviceClass);
    }
    return new AssetInitializerServiceImpl();
  }
  
  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class AssetInitializerServiceImpl implements AssetInitializerService {
    public String getAgentPrototype(String agentName) {
      throw new UnsupportedOperationException();
    }
    public String[] getAgentPropertyGroupNames(String agentName) {
      throw new UnsupportedOperationException();
    }
    public Object[][] getAgentProperties(String agentName, String pgName) {
      throw new UnsupportedOperationException();
    }
    public String[][] getAgentRelationships(String agentName) {
      throw new UnsupportedOperationException();
    }
    public AssetDataReader getAssetDataReader() {
      return new AssetDataFileReader();
    }
    public Object[] translateAttributeValue(String type, String key) {
      return new Object[] {type, key};
    }
  }
}
