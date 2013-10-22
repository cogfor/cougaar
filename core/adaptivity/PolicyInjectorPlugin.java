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
import java.util.Iterator;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;

/**
 * Plugin that reads OperatingModePolicies from files and publishes them
 * to the blackboard
 **/
public class PolicyInjectorPlugin extends ComponentPlugin {

  private LoggingService logger;

  private OperatingModePolicy[] policies;

  private UIDService uidService = null;

  public void setUIDService(UIDService service) {
    uidService = service;
  }

  public void setLoggingService (LoggingService ls) {
    logger = ls;
  }

  @Override
public void setupSubscriptions() {
    String here = getAgentIdentifier().toString();
    for (Iterator fileIterator = getParameters().iterator(); 
	 fileIterator.hasNext();) {
      String policyFileName = fileIterator.next().toString();
      try {
	Reader is = new InputStreamReader(getConfigFinder().open(policyFileName));
	try {
	  Parser p = new Parser(is, logger);
	  policies = p.parseOperatingModePolicies();
	} finally {
	  is.close();
	}
      } catch (Exception e) {
	logger.error("Error parsing policy file " + policyFileName, e);
      }
      for (int i=0; i<policies.length; i++) {
	policies[i].setAuthority(here);
	uidService.registerUniqueObject(policies[i]);
	blackboard.publishAdd(policies[i]);
      }
    }
  }
  
  @Override
public void execute() {
  }

}
