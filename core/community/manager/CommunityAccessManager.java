/*
 * <copyright>
 *  
 *  Copyright 2001-2004 Mobile Intelligence Corp
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
package org.cougaar.community.manager;

import org.cougaar.community.CommunityProtectionService;
import org.cougaar.community.CommunityServiceConstants;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.MessageAddress;

import javax.naming.directory.ModificationItem;

/**
 * Performs access control for community manager.  All authorization requests
 * are delegated to the CommunityProtectionService if available.  If the
 * CommunityProtectionService is not available the requests are delegated to
 * the "authorizeUsingDefaultPolicy" method.  The base implementation of this
 * method approves all requests.  Alternate implementations should exend this
 * class and override the authorizeUsingDefaultPolicy method.  The use of an
 * alternate implementation is specified by defining the new class in the
 * "org.cougaar.community.access.manager.classname" system property.
 */
public class CommunityAccessManager
    implements CommunityProtectionService, CommunityServiceConstants {

  protected ServiceBroker serviceBroker;
  protected LoggingService logger;
  protected String agentName;

  public CommunityAccessManager(ServiceBroker sb) {
    this.serviceBroker = sb;
    agentName = getAgentName();
    logger =
      (LoggingService)serviceBroker.getService(this, LoggingService.class, null);
    logger = org.cougaar.core.logging.LoggingServiceWithPrefix.add(logger, agentName + ": ");
  }

  /**
   * Authorize request to read or modify community state.
   * @param communityName String  Name of affected community
   * @param requester String      Name of requesting agent
   * @param operation int         Requested operation (refer to
   *                         org.cougaar.core.service.CommunityServiceConstants
   *                              for valid op codes)
   * @param target String         Name of affected community member or null if
   *                              target is community
   * @param attrMods              Requested attribute changes
   * @return boolean              Return true if request is authorized by
   *                              current policy
   */
  public final boolean authorize(String communityName,
                                 String requester,
                                 int    operation,
                                 String target,
                                 ModificationItem[] attrMods) {
    boolean isAuthorized = false;
    CommunityProtectionService cps =
        (CommunityProtectionService)serviceBroker.getService(this,
                                                             CommunityProtectionService.class,
                                                             null);
    if (cps != null) {
      isAuthorized = cps.authorize(communityName, requester, operation, target, attrMods);
      serviceBroker.releaseService(this, CommunityProtectionService.class, cps);
    } else {
      isAuthorized =
        authorizeUsingDefaultPolicy(communityName, requester, operation, target, attrMods);
    }
    return isAuthorized;
  }

  /**
   * Authorization method that is used if the CommunityProtectionService is
   * not available.
   * @param communityName String  Name of affected community
   * @param requester String      Name of requesting agent
   * @param operation int         Requested operation (refer to
   *                         org.cougaar.core.service.CommunityServiceConstants
   *                              for valid op codes)
   * @param target String         Name of affected community member or null if
   *                              target is community
   * @return boolean              Return true if request is authorized by
   *                              current policy
   */
  protected boolean authorizeUsingDefaultPolicy(String             communityName,
                                                String             requester,
                                                int                operation,
                                                String             target,
                                                ModificationItem[] attrMods) {
    return true;
  }

  protected String getAgentName() {
    AgentIdentificationService ais =
        (AgentIdentificationService)serviceBroker.getService(this,
        AgentIdentificationService.class, null);
    MessageAddress addr = ais.getMessageAddress();
    serviceBroker.releaseService(this, AgentIdentificationService.class, ais);
    return addr.toString();
  }

}
