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

package org.cougaar.planning.plugin.legacy;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModelAdapter;

  /** 
   * First cut at a class that performs basic new-fangled Plugin functions
   **/

public abstract class PluginSupport 
  extends GenericStateModelAdapter
  implements PluginBase
{

  private PluginBindingSite pluginBindingSite = null;

  public void setBindingSite(final BindingSite bs) {
    pluginBindingSite = new PluginBindingSite() {
      public MessageAddress getAgentIdentifier() {
        return PluginSupport.this.getAgentIdentifier();
      }
      public ConfigFinder getConfigFinder() {
        return PluginSupport.this.getConfigFinder();
      }
      public ServiceBroker getServiceBroker() {
        return bs.getServiceBroker();
      }
      public void requestStop() {
        bs.requestStop();
      }
    };
  }

  private MessageAddress agentId = null;
  public final void setAgentIdentificationService(
      AgentIdentificationService ais) {
    this.agentId = ais.getMessageAddress();
  }
  public MessageAddress getAgentIdentifier() {
    return agentId;
  }
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  protected final PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }

  /** storage for wasAwakened. 
   **/
  private boolean explicitlyAwakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only while running in the main plugin thread.
   */
  protected boolean wasAwakened() { return explicitlyAwakened; }

  /** For PluginBinder use only **/
  public final void setAwakened(boolean value) { explicitlyAwakened = value; }

}
