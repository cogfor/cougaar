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

package org.cougaar.core.plugin.freeze;

import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is a base class for freeze plugins.
 */
public abstract class FreezePlugin extends ServiceUserPlugin {
  protected LoggingService logger;
  protected FreezePlugin() {
    super(new Class[0]);
  }

  @Override
public void setupSubscriptions() {
    logger = getServiceBroker()
      .getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(logger, getAgentIdentifier().toString() + ": ");

  }

  protected static UnaryPredicate targetRelayPredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return o instanceof FreezeRelayTarget;
      }
    };

  protected static UnaryPredicate sourceRelayPredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return o instanceof FreezeRelaySource;
      }
    };
}
