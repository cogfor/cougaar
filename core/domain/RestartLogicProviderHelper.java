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

package org.cougaar.core.domain;

import org.cougaar.core.mts.MessageAddress;

/**
 * Helper methods for {@link RestartLogicProvider}s.
 */
public abstract class RestartLogicProviderHelper {

  private RestartLogicProviderHelper() { 
    // just helper methods
  }

  /**
   * Utility method for RestartLogicProviders to
   * use when performing "restart(cid)".
   *
   * @return true if this "self" agent, asked to
   *    "restart(cid)", should reconcile with the
   *    specified "dest" agent.
   */
  public static final boolean matchesRestart(
       MessageAddress self,
       MessageAddress cid,
       MessageAddress dest) {
    // Do getPrimary on all non-null MessageAddresses to avoid comparing attributes
    return 
      cid != null ? 
      cid.getPrimary().equals((dest != null ? dest.getPrimary() : dest)) : 
      (dest != null && 
       !dest.getPrimary().equals((self != null ? 
				  self.getPrimary() : 
				  self)));
  }

}
