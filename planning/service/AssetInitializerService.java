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

package org.cougaar.planning.service;

import org.cougaar.core.component.Service;
import org.cougaar.planning.plugin.asset.AssetDataReader;

/**
 * Planning asset configuration service. Used to specify how Assets, particularly Entity assets, are loaded into the system (XML, database, ?).
 * <p>
 * @see org.cougaar.planning.plugin.asset.AssetDataPlugin
 */
public interface AssetInitializerService extends Service {

  String getAgentPrototype(String agentName) 
    throws InitializerException;

  String[] getAgentPropertyGroupNames(String agentName)
    throws InitializerException;

  Object[][] getAgentProperties(String agentName, String pgName)
    throws InitializerException;

  String[][] getAgentRelationships(String agentName)
    throws InitializerException;

  AssetDataReader getAssetDataReader();

  Object[] translateAttributeValue(String type, String key)
    throws InitializerException;

  /**
   * Generic exception for asset initializer failures.
   */
  public class InitializerException extends Exception {
    public InitializerException() {
      super();
    }
    public InitializerException(String msg) {
      super(msg);
    }
    public InitializerException(Throwable t) {
      super(t);
    }
    public InitializerException(String msg, Throwable t) {
      super(msg, t);
    }
  }
}
