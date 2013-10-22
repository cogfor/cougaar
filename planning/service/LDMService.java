/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

import java.util.List;

import org.cougaar.core.component.Service;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDServer;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.LatePropertyProvider;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.planning.ldm.PropertyProvider;
import org.cougaar.planning.ldm.PrototypeProvider;

/**
 * Placeholder to clean up plugin-to-manager interactions
 */
public interface LDMService extends Service {

  //
  // Mix of DomainService & PrototypeRegistryService
  //

  /** Get a reference to the LDM object.  
   * @todo This should be refactored
   **/
  LDMServesPlugin getLDM();

  //
  // AgentIdentificationService
  //

  MessageAddress getMessageAddress();

  //
  // UIDService
  //

  UIDServer getUIDServer();

  //
  // Hack for planning DomainService
  //

  PlanningFactory getFactory();

  //
  // DomainService
  //

  /** return a domain-specific factory **/
  Factory getFactory(String domainName);

  /** return a domain-specific factory **/
  Factory getFactory(Class domainClass);

  /** return a list of all domain-specific factories **/
  List getFactories();

  //
  // PrototypeRegistryService
  //

  /** Add a PrototypeProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addPrototypeProvider(PrototypeProvider plugin);
  /** Add a PropertyProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addPropertyProvider(PropertyProvider plugin);
  /** Add a LatePropertyProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addLatePropertyProvider(LatePropertyProvider plugin);
}
