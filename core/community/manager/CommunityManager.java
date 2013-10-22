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

import javax.naming.directory.ModificationItem;

import org.cougaar.core.service.community.Community;
import org.cougaar.core.service.community.CommunityResponse;
import org.cougaar.core.service.community.Entity;
import org.cougaar.core.service.community.FindCommunityCallback;
import org.cougaar.core.service.wp.Callback;

/**
 * Interface for a CommunityManager that is responsible for maintaining
 * state for one or more communities.
 */
public interface CommunityManager {

  /**
   * Defines community to manage.
   * @param community Community
   */
  public void manageCommunity(Community community);
  
  /**
   * Defines community to manage.
   * @param community Community
   * @param callback callback invoked upon completion
   */
  public void manageCommunity(Community community, Callback callback);

  /**
   * Client request to be handled by manager.
   * @param source String  Name of agent submitting request
   * @param communityName String  Target Community
   * @param reqType int  Request type (Refer to
   *   org.cougaar.core.service.community.CommunityServiceConstants for list of
   *   recognized values)
   * @param entity Entity Affected Entity
   * @param attrMods ModificationItem[] Attribute modifications to be applied
   *    to affected entity
   * @return CommunityResponse  Response callback
   */
  public CommunityResponse processRequest(String             source,
                                          String             communityName,
                                          int                reqType,
                                          Entity             entity,
                                          ModificationItem[] attrMods);

  /**
   * Locate the manager for specified community.
   * @param communityName String  Target community
   * @param fmcb FindCommunityCallback  Callback that is invoked after manager
   *    has been located.
   */
  public void findManager(String                communityName,
                          FindCommunityCallback fmcb);

}
