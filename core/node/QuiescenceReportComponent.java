/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AgentQuiescenceStateService;
import org.cougaar.core.service.QuiescenceReportForDistributorService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link QuiescenceReportService} to
 * all agents.
 */ 
public final class QuiescenceReportComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private QuiescenceReportService quiescenceReportService;

  private MessageAddress localAgent = null;

  private QuiescenceReportServiceProvider qrsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    AgentContainer agentContainer;

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      // not in the node agent
      return;
    }
    rootsb = ncs.getRootServiceBroker();
    agentContainer = ncs.getRootContainer();
    sb.releaseService(this, NodeControlService.class, ncs);

    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    qrsp = 
      new QuiescenceReportServiceProvider(
          localAgent.getAddress(),
          agentContainer,
          sb);
    rootsb.addService(QuiescenceReportService.class, qrsp);
    rootsb.addService(QuiescenceReportForDistributorService.class, qrsp);
    // Service for querying the quiescence / enabled state of agents, used
    // by the QuiescenceStateServlet
    rootsb.addService(AgentQuiescenceStateService.class, qrsp);

    // mark our node as non-quiescent until we are started, which
    // will occur after all other components have been loaded.
    //
    // note that we pass our MessageAddress as the requestor.
    quiescenceReportService = sb.getService(
       localAgent, QuiescenceReportService.class, null);

    quiescenceReportService.clearQuiescentState();
  }

  @Override
public void start() {
    super.start();

    if (quiescenceReportService != null) {
      quiescenceReportService.setQuiescentState();
      sb.releaseService(
          localAgent,
          QuiescenceReportService.class,
          quiescenceReportService);
      quiescenceReportService = null;
    }
  }

  @Override
public void unload() {
    super.unload();

    if (qrsp != null) {
      rootsb.revokeService(QuiescenceReportService.class, qrsp);
      // Need to cleanup tasks performed in separate thread.
      qrsp.revokeService();
      qrsp = null;
    }
  }
}
