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

import org.cougaar.community.init.CommunityInitializerService;
import org.cougaar.community.init.CommunityConfig;
import org.cougaar.community.init.EntityConfig;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;

import org.cougaar.core.component.ServiceBroker;

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
public class SimpleCommunityAccessManager extends CommunityAccessManager {

  // The following fields are used for the default authorization policy
  private Set knownEntities; // List of predefined agents/communities in society

  public SimpleCommunityAccessManager(ServiceBroker sb) {
    super(sb);
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
  protected boolean authorizeUsingDefaultPolicy(String communityName,
                                                String requester,
                                                int operation,
                                                String target) {
    // Simply verify that requester was included in predefined
    // community configuration defined in communities.xml
    return getKnownEntities().contains(requester);
  }

  /**
   * Get entity names from communities.xml file on config path.
   * @return Set of predefined agent/community names
   */
  protected Set getKnownEntities() {
    if (knownEntities == null) {
      knownEntities = new HashSet();
      CommunityInitializerService cis = (CommunityInitializerService)
          serviceBroker.getService(this, CommunityInitializerService.class, null);
      try {
        Collection communityConfigs = cis.getCommunityDescriptions(null);
        for (Iterator it = communityConfigs.iterator(); it.hasNext(); ) {
          CommunityConfig cc = (CommunityConfig)it.next();
          knownEntities.add(cc.getName());
          for (Iterator it1 = cc.getEntities().iterator(); it1.hasNext(); ) {
            EntityConfig ec = (EntityConfig)it1.next();
            knownEntities.add(ec.getName());
          }
        }
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Unable to obtain community information for agent " +
                      agentName);
        }
      } finally {
        serviceBroker.releaseService(this, CommunityInitializerService.class,
                                     cis);
      }
    }
    return knownEntities;
  }
}
