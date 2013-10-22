/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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
package org.cougaar.core.plugin;

import org.cougaar.core.blackboard.BlackboardClientComponent;
import org.cougaar.util.ConfigFinder;

/**
 * This component is a base class for standard blackboard-client
 * "plugins".
 * <p>
 * Create a derived class by implementing 
 * <tt>setupSubscriptions()</tt> and <tt>execute()</tt>.
 * <p>
 * Note that both "precycle()" and "cycle()" will be run by the
 * scheduler.  This means that the scheduling order <i>in relation to 
 * other scheduled Components</i> may be *random* (i.e. this 
 * ComponentPlugin might load first but be precycled last!).  In 
 * general a Component should <b>not</b> make assumptions about the 
 * load or schedule ordering.
 */
public abstract class ComponentPlugin 
  extends BlackboardClientComponent
  implements PluginBase
{
  public ComponentPlugin() { 
  }
  
  // 10.0: Old method PluginBindingSite getBindingSite() is gone!
  // If you want the AgentIdentifier, you may simply call getAgentIdentifier()
  // -- inherited from BlackboardClientComponent

  /**
   * Called once after initialization, as a "pre-execute()".
   */
  @Override
protected abstract void setupSubscriptions();
  
  /**
   * Called every time this component is scheduled to run.
   */
  @Override
protected abstract void execute();
  
  //
  // misc utility methods:
  //

  protected ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }
  
}
