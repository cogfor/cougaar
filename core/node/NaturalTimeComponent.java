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

import java.util.HashSet;
import java.util.Set;

import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.agent.service.alarm.Timer;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link NaturalTimeService}, which
 * is wrapped by each agent's {@link
 * org.cougaar.core.service.AlarmService}.
 * <p> 
 * This component is typically loaded into the node-agent, allowing
 * the Timer to be shared by all agents.  It can also be loaded
 * into a regular agent for a per-agent Timer, by modifying the node
 * template ($CIP/configs/common/NodeAgent.xsl method "HIGH_node_1b")
 * to remove this component and add it to the agent template
 * ($CIP/configs/common/SimpleAgent.xsl at the beginning of
 * "HIGH_agent_2").
 */
public final class NaturalTimeComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private NTSP ntsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
      sb.releaseService(this, NodeControlService.class, ncs);
    }

    ntsp = new NTSP(sb);
    ntsp.start();

    ServiceBroker the_sb = (rootsb == null ? sb : rootsb);
    the_sb.addService(NaturalTimeService.class, ntsp);
  }

  @Override
public void unload() {
    super.unload();

    ServiceBroker the_sb = (rootsb == null ? sb : rootsb);
    the_sb.revokeService(NaturalTimeService.class, ntsp);
    ntsp.stop();
    ntsp = null;
  }

  private static class NTSP implements ServiceProvider {
    private final ServiceBroker sb;
    private final Set services = new HashSet(11);
    private ExecutionTimer xTimer;

    protected NTSP(ServiceBroker sb) {
      this.sb = sb;
    }

    /** Starts the timers */
    protected void start() {
      ThreadService tsvc = sb.getService(this, ThreadService.class, null);
      xTimer = new ExecutionTimer();
      xTimer.start(tsvc);
      sb.releaseService(this, ThreadService.class, tsvc);
    }

    protected void stop() {
      xTimer.stop();
    }

    // implement ServiceProvider
    public Object getService(
        ServiceBroker xsb, Object requestor, Class serviceClass) {
      if (serviceClass == NaturalTimeService.class) {
        Object s = new NTSI(xTimer, requestor);
        synchronized (services) { 
          services.add(s);
        }
        return s;
      } else {
        throw new IllegalArgumentException(
            "Can only provide NaturalTimeService!");
      }
    }

    public void releaseService(
        ServiceBroker xsb, Object requestor,
        Class serviceClass, Object service) {
      synchronized (services) { 
        if (services.remove(service)) {
          ((NTSI) service).clear();
        } else {
          throw new IllegalArgumentException(
              "Cannot release service "+service);
        }
      }
    }
  }

  /** simple proxy */
  private static class NTSI 
    extends TimeServiceBase
    implements NaturalTimeService {
      private final ExecutionTimer xTimer;
      private NTSI(ExecutionTimer xTimer, Object r) {
        super(r);
        this.xTimer = xTimer;
      }
      @Override
      protected Timer getTimer() {
        return xTimer;
      }
      public void setParameters(ExecutionTimer.Parameters x) {
        xTimer.setParameters(x);
      }
      /**
       * @deprecated Use the version that allows specifying absolute
       * change time instead
       */
      public ExecutionTimer.Parameters createParameters(
          long millis, boolean millisIsAbsolute, double newRate,
          boolean forceRunning, long changeDelay) {
        return xTimer.create(
            millis, millisIsAbsolute, newRate, forceRunning, changeDelay);
      }
      public ExecutionTimer.Parameters createParameters(
          long millis, boolean millisIsAbsolute, double newRate,
          boolean forceRunning, long changeTime, boolean changeIsAbsolute) {
        return xTimer.create(
            millis, millisIsAbsolute, newRate,
            forceRunning, changeTime, changeIsAbsolute);
      }
      public ExecutionTimer.Parameters[] createParameters(
          ExecutionTimer.Change[] changes) {
        return xTimer.create(changes);
      }
      public double getRate() {
        return xTimer.getRate();
      }
    }
}
