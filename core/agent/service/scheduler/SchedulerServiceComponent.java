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

package org.cougaar.core.agent.service.scheduler;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the SchedulerService, which is typically
 * just a wrapper around the ThreadService. 
 */
public final class SchedulerServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private LoggingService loggingS;
  private ThreadService threadS;
  private SchedulerServiceProvider schedSP;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    // get the logger
    loggingS = sb.getService(this, LoggingService.class, null);

    // get the thread service
    threadS = sb.getService(this, ThreadService.class, null);

    // get the local agent address
    String agentName = "Anonymous";
    AgentIdentificationService agentIdS = sb.getService(this, AgentIdentificationService.class, null);
    if (agentIdS != null) {
      MessageAddress agentAddr = agentIdS.getMessageAddress();
      if (agentAddr != null) {
        agentName = agentAddr.getAddress();
      }
      sb.releaseService(this, AgentIdentificationService.class, agentIdS);
    }

    // create and advertise our service
    this.schedSP = new SchedulerServiceProvider(threadS, loggingS, agentName);
    sb.addService(SchedulerService.class, schedSP);
  }

  @Override
public void suspend() {
    schedSP.suspend();
    super.suspend();
  }

  @Override
public void resume() {
    super.resume();
    schedSP.resume();
  }

  @Override
public void unload() {
    // revoke our service
    if (schedSP != null) {
      sb.revokeService(SchedulerService.class, schedSP);
      schedSP = null;
    }
    // release services
    if (threadS != null) {
      sb.releaseService(this, ThreadService.class, threadS);
      threadS = null;
    }
    if (loggingS != null) {
      sb.releaseService(this, LoggingService.class, loggingS);
      loggingS = null;
    }
    super.unload();
  }
}
