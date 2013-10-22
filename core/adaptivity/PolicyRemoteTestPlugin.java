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
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDService;

/**
 * Test plugin that reads policies from a file, creates 
 * InterAgentOperatingModePolicies and alternately forwards 
 * the policy to another agent and revokes the policy.
 *
 **/
public class PolicyRemoteTestPlugin extends ServiceUserPluginBase {

  private boolean published = false;

  private InterAgentOperatingModePolicy[] policies;

  private UIDService uidService;

  private static final Class[] requiredServices = {
    UIDService.class
  };

  public PolicyRemoteTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    OperatingModePolicy[] tempPolicies = null;
    String policyFileName = getParameters().iterator().next().toString();
    try {
      Reader r = new InputStreamReader(getConfigFinder().open(policyFileName));
      try {
        Parser p = new Parser(r, logger);
        tempPolicies = p.parseOperatingModePolicies();
      } finally {
        r.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing policy file", e);
    }

    policies = new InterAgentOperatingModePolicy[tempPolicies.length];
    for (int i = 0; i < tempPolicies.length; i++) {
      InterAgentOperatingModePolicy iaomp 
	= new InterAgentOperatingModePolicy(tempPolicies[i].getName(),
					    tempPolicies[i].getIfClause(), 
					    tempPolicies[i].getOperatingModeConstraints(),
					    getAgentIdentifier().toString());

      policies[i] = iaomp;
      if (haveServices()) {
  	uidService.registerUniqueObject(policies[i]);
      }
    }
    setPolicies();

  }

  @Override
public void execute() {
    if (timerExpired()) {
      cancelTimer();
      setPolicies();
    }
  }

  private boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = sb.getService(this, UIDService.class, null);
      return true;
    }
    return false;
  }

  private void setPolicies() {
    if (!published) {
      if (logger.isInfoEnabled()) logger.info("publishing policy");
      for (int i = 0; i < policies.length; i++) {
 	policies[i].setTarget(MessageAddress.getMessageAddress("Provider"));
	getBlackboardService().publishAdd(policies[i]);
	published = true;
      }
    } else {
      if (logger.isInfoEnabled()) logger.info("Removing policy");
      for (int i = 0; i < policies.length; i++) {
  	//policies[i].setTarget((MessageAddress)null);
	//getBlackboardService().publishChange(policies[i]);        
  	getBlackboardService().publishRemove(policies[i]);        
      }
      published = false;
    }
    startTimer(75000);
  }
}
