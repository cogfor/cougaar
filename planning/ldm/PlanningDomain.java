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

import java.util.ArrayList;
import java.util.Collection;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.planning.ldm.lps.AssetTransferLP;
import org.cougaar.planning.ldm.lps.ComplainingLP;
import org.cougaar.planning.ldm.lps.DeletionLP;
import org.cougaar.planning.ldm.lps.NotificationLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetRescindLP;
import org.cougaar.planning.ldm.lps.ReceiveAssetVerificationLP;
import org.cougaar.planning.ldm.lps.ReceiveDeletionLP;
import org.cougaar.planning.ldm.lps.ReceiveNotificationLP;
import org.cougaar.planning.ldm.lps.ReceiveRescindLP;
import org.cougaar.planning.ldm.lps.ReceiveTaskLP;
import org.cougaar.planning.ldm.lps.RemoteAllocationLP;
import org.cougaar.planning.ldm.lps.RescindLP;
import org.cougaar.planning.service.LDMService;

/**
 * This is the "planning" domain, which defines planning
 * data types (Task, PlanElement, etc) and loads related LPs.
 */
public class PlanningDomain extends DomainAdapter {
  public static final String PLANNING_NAME = "planning";

  protected AgentIdentificationService agentIdService;
  protected MessageAddress self;
  private LDMService ldmService;
  protected AlarmService alarmService;

  public String getDomainName() {
    return PLANNING_NAME;
  }

  public void setAgentIdentificationService(AgentIdentificationService ais) {
    this.agentIdService = ais;
    if (ais == null) {
      // Revocation
    } else {
      this.self = ais.getMessageAddress();
    }
  }

  public void setLDMService(LDMService ldmService) {
    this.ldmService = ldmService;
  }

  public void setAlarmService(AlarmService alarmService) {
    this.alarmService = alarmService;
  }

  public void load() {
    super.load();
    if (ldmService != null) {
      LDMServesPlugin ldm = ldmService.getLDM();
      LDMContextTable.setLDM(self, ldm);
    }
  }

  public void unload() {
    ServiceBroker sb = getServiceBroker();
    if (agentIdService != null) {
      sb.releaseService(this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    if (ldmService != null) {
      sb.releaseService(this, LDMService.class, ldmService);
      ldmService = null;
      LDMContextTable.setLDM(self, null);
    }
    if (alarmService != null) {
      sb.releaseService(this, AlarmService.class, alarmService);
      alarmService = null;
    }
    super.unload();
  }

  public Collection getAliases() {
    ArrayList l = new ArrayList(2);
    l.add("planning");
    l.add("log");
    return l;
  }

  protected void loadFactory() {
    LDMServesPlugin ldm = ldmService.getLDM();
    Factory f = new PlanningFactoryImpl(ldm);
    setFactory(f);
  }

  protected void loadXPlan() {
    LogPlan logplan = new LogPlanImpl();
    setXPlan(logplan);
  }

  protected void loadLPs() {
    RootPlan rootplan = (RootPlan) getXPlanForDomain("root");
    if (rootplan == null) {
      throw new RuntimeException("Missing \"root\" plan!");
    }

    LogPlan logplan = (LogPlan) getXPlan();
    PlanningFactory ldmf = (PlanningFactory) getFactory();

    // input LPs
    addLogicProvider(new ReceiveAssetLP(rootplan, logplan, ldmf, self));
    addLogicProvider(new ReceiveAssetVerificationLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveAssetRescindLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveNotificationLP(rootplan, logplan, ldmf));
    addLogicProvider(new ReceiveDeletionLP(rootplan, logplan, ldmf, self));
    addLogicProvider(new ReceiveRescindLP(rootplan, logplan));
    addLogicProvider(new ReceiveTaskLP(rootplan, logplan, self, ldmf, alarmService));
    
    // output LPs (+ some input)
    addLogicProvider(new AssetTransferLP(rootplan, logplan, ldmf, self));    
    addLogicProvider(new NotificationLP(rootplan, logplan, ldmf, self));
    addLogicProvider(new DeletionLP(rootplan, ldmf, self));
    addLogicProvider(new RemoteAllocationLP(rootplan, ldmf, self, alarmService, logplan));
    // Below also does some error detection
    addLogicProvider(new RescindLP(rootplan, logplan, ldmf, self));
    
    // error detection LP
    addLogicProvider(new ComplainingLP(rootplan, self));
  }
}
