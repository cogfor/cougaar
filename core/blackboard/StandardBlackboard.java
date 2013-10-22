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

package org.cougaar.core.blackboard;

import java.util.List;

import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceObject;
import org.cougaar.core.service.BlackboardMetricsService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link BlackboardService}
 * and manages the {@link Blackboard}.
 */
public class StandardBlackboard
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb = null;
  private Blackboard bb = null;
  private Distributor d = null;

  private MessageSwitchService msgSwitch;

  private BlackboardForAgentServiceProvider bbAgentSP;
  private BlackboardServiceProvider bbSP;
  
  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public PersistenceObject getPersistenceObject() {
    try {
      return bb.getPersistenceObject();
    } catch (Exception e) {
      throw new RuntimeException("Unable to capture Blackboard state", e);
    }
  }

  @Override
public void load() {
    super.load();

    msgSwitch = sb.getService(this, MessageSwitchService.class, null);
    if (msgSwitch == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchService, which is required"+
          " for the blackboard to send messages!");
    }

    // create blackboard with optional prior-state
    bb = new Blackboard(msgSwitch, sb, null);
//     bb = new Blackboard(msgSwitch, sb, loadState);
//     loadState = null;

    bb.init();
    d = bb.getDistributor();
//     d.getPersistence().registerServices(sb);

    bb.connectDomains();

    // offer hooks back to the Agent
    bbAgentSP = new BlackboardForAgentServiceProvider(bb);
    sb.addService(BlackboardForAgent.class, bbAgentSP);

    //offer Blackboard service and Blackboard metrics service
    // both use the same service provider
    bbSP = new BlackboardServiceProvider(bb.getDistributor());
    sb.addService(BlackboardService.class, bbSP);
    sb.addService(BlackboardMetricsService.class, bbSP);
    sb.addService(BlackboardQueryService.class, bbSP);

    // add services here (none for now)
  }

  @Override
public void unload() {
    super.unload();
    
    // unload services in reverse order of "load()"
    sb.revokeService(BlackboardMetricsService.class, bbSP);
    sb.revokeService(BlackboardService.class, bbSP);
    sb.revokeService(BlackboardForAgent.class, bbAgentSP);
//     d.getPersistence().unregisterServices(sb);
    d.stop();
    bb.stop();
    if (msgSwitch != null) {
      sb.releaseService(
          this, MessageSwitchService.class, msgSwitch);
      msgSwitch = null;
    }
  }

  //
  // blackboardforagent support
  //
  private static class BlackboardForAgentServiceProvider 
    implements ServiceProvider 
  {
    Blackboard blackboard;
    BlackboardForAgentServiceProvider(Blackboard blackboard) {
      this.blackboard = blackboard;
    }
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (serviceClass == BlackboardForAgent.class) {
        return new BlackboardForAgentImpl(blackboard);
      }
      return null;
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
      if (service instanceof BlackboardForAgentImpl) {
        ((BlackboardForAgentImpl)service).release(blackboard);
      }
    }
  }

  private static class BlackboardForAgentImpl 
    implements BlackboardForAgent
  {
    private Blackboard blackboard;
    private BlackboardForAgentImpl(Blackboard bb) {
      this.blackboard = bb;
    }

    void release(Blackboard bb) {
      if (bb == blackboard) {
        this.blackboard = null;
      } else {
        throw new RuntimeException("Illegal attempt to revoke a "+this+".");
      }
    }
    // might be better for blackboard to be a message switch handler, eh?
    public void receiveMessages(List messages) {
      blackboard.getDistributor().receiveMessages(messages);
    }

    public void restartAgent(MessageAddress cid) {
      blackboard.getDistributor().restartAgent(cid);
    }

    public void suspend() {
      blackboard.getDistributor().suspend();
    }

    public void resume() {
      blackboard.getDistributor().resume();
    }

    public PersistenceObject getPersistenceObject() {
      return blackboard.getDistributor().getPersistenceObject();
    }

    public void persistNow() {
      blackboard.getDistributor().persistNow();
    }
  }
}

