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

package org.cougaar.core.agent;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is loaded late in the agent, to announce
 * the agent Suspening/Stopping/<i>etc</i> state transitions.
 * @see BeginLogger 
 */
public final class EndLogger
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    // get our local agent's address
    MessageAddress localAgent = null;
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // get logging service
    log = sb.getService(this, LoggingService.class, null);

    // prefix with agent name
    String prefix = localAgent+": ";
    log = LoggingServiceWithPrefix.add(log, prefix);

    if (log.isInfoEnabled()) {
      log.info("Loaded");
    }
  }
  @Override
public void start() {
    super.start();
    if (log.isInfoEnabled()) {
      log.info("Started");
    }
  }
  @Override
public void resume() {
    super.resume();
    if (log.isInfoEnabled()) {
      log.info("Resumed");
    }
  }

  // the component model is "first loaded is last unload", so these
  // are the begin-states (i.e. the "*ing" instead of "*ed").

  @Override
public void suspend() {
    super.suspend();
    if (log.isInfoEnabled()) {
      log.info("Suspending");
    }
  }
  @Override
public void stop() {
    super.stop();
    if (log.isInfoEnabled()) {
      log.info("Stopping");
    }
  }
  @Override
public void unload() {
    super.unload();
    if (log.isInfoEnabled()) {
      log.info("Unloading");
    }
  }
}
