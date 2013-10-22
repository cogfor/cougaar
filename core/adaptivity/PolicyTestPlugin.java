/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.io.InputStreamReader;
import java.io.Reader;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.PlaybookConstrainService;

/**
 * Test plugin that alternately constrains and unconstrains
 * the playbook with an operating mode policy it read froma file
 */
public class PolicyTestPlugin extends ServiceUserPlugin {
  private PlaybookConstrainService playbookConstrainService;

  private boolean constrained = false;

  private OperatingModePolicy[] policies;

  private static final Class[] requiredServices = {
    PlaybookConstrainService.class
  };

  public PolicyTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    String policyFileName = getParameters().iterator().next().toString();
    try {
      Reader is = new InputStreamReader(getConfigFinder().open(policyFileName));
      try {
        Parser p = new Parser(is, logger);
        policies = p.parseOperatingModePolicies();
      } finally {
        is.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing policy file", e);
    }
    if (haveServices()) setPolicies();
  }

  private boolean haveServices() {
    if (playbookConstrainService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      playbookConstrainService = sb.getService(this, PlaybookConstrainService.class, null);
      return true;
    }
    return false;
  }

  @Override
public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setPolicies();
      }
    }
  }

  private void setPolicies() {
    // This method is meant to alternately add & remove
    // threatcon policies, based on the constarained flag
    if (constrained) {
      if (logger.isInfoEnabled()) logger.info("Adding threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        playbookConstrainService.constrain(policies[i]);
      }
      constrained = false;
    } else {
      if (logger.isInfoEnabled()) logger.info("Removing threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        playbookConstrainService.unconstrain(policies[i]);
      }
      constrained = true;
    }
    resetTimer(75000);
  }
}





