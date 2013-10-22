/*
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.node;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the INI-based {@link
 * ComponentInitializerService}.
 *
 * @see FileComponentInitializerServiceProvider
 */
public class FileComponentInitializerServiceComponent
 extends GenericStateModelAdapter
 implements Component {
  private ServiceBroker sb;

  private ServiceProvider theSP;
  private LoggingService log;

  public void setServiceBroker(ServiceBroker sb) {
    // this is the *node* service broker!  The NodeControlService
    // is not available until the node-agent is created...
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();
    log = sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    if (sb.hasService(ComponentInitializerService.class)) {
      // already have a ComponentInitializer? 
      // Leave the existing one in place
      if (log.isInfoEnabled()) {
	log.info("Not loading the FileComponentInitializer service");
      }
    } else {
      try {
	theSP = new FileComponentInitializerServiceProvider();
      } catch (Exception e) {
	log.error("Unable to load FileComponentInitializerService", e);
      }
      if (log.isDebugEnabled())
	log.debug("Providing File (INI) Init service");
      sb.addService(ComponentInitializerService.class, theSP);
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  @Override
public void unload() {
    if (theSP != null) {
      sb.revokeService(ComponentInitializerService.class, theSP);
      theSP = null;
    }
    super.unload();
  }
}
