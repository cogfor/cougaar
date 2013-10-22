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

package org.cougaar.planning.servlet;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.servlet.SimpleServletSupportImpl;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.util.ConfigFinder;

/** 
 * <pre>
 * This support class offers additional services on top of the
 * SimpleServletSupport class, including access to the blackboard,
 * config finder, root factory, ldm serves plugin, and scheduler service.
 * </pre>
 */
public class BlackboardServletSupport extends SimpleServletSupportImpl {
  public BlackboardServletSupport(
      String path,
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      LoggingService logger,
      BlackboardService blackboard,
      ConfigFinder configFinder,
      PlanningFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler) {
    super (path, agentId, blackboardQuery, logger);
    this.blackboard = blackboard;
    this.configFinder = configFinder;
    this.ldmf = ldmf;
    this.ldm = ldm;
    this.scheduler = scheduler;
  }
  protected BlackboardService blackboard;
  protected ConfigFinder configFinder;
  protected PlanningFactory ldmf;
  protected LDMServesPlugin ldm;
  protected SchedulerService scheduler;

  // I need access to the blackboard so I can publish to it
  public BlackboardService getBlackboardService () { return blackboard; }
  public ConfigFinder getConfigFinder () { return configFinder; }
  public PlanningFactory getLDMF () { return ldmf; }
  public LDMServesPlugin getLDM () { return ldm; }
  public SchedulerService getSchedulerService () { return scheduler; }
}
