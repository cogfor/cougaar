/*
 * <copyright>
 *  
 *  Copyright 1997-2004 Mobile Intelligence Corp
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
package org.cougaar.community;

import org.cougaar.core.component.Service;
import javax.naming.directory.ModificationItem;

/**
 * Interface defining CommunityProtectionService used to authorize
 * CommunityServuce client requests.
 */
public interface CommunityProtectionService extends Service {

  /**
   * Authorize request to read or modify community state.
   * @param communityName String  Name of affected community
   * @param requester String      Name of requesting agent
   * @param operation int         Requested operation (refer to
   *                         org.cougaar.core.service.CommunityServiceConstants
   *                              for valid op codes)
   * @param target String         Name of affected community member or null if
   *                              target is community
   * @param attrMods              Requested attribute modifications if request
   *                              type is MODIFY_ATTRIBUTES, ignored otherwise
   * @return boolean              Return true if request is authorized by
   *                              current policy
   */
  boolean authorize(String             communityName,
                    String             requester,
                    int                operation,
                    String             target,
                    ModificationItem[] attrMods);

}
