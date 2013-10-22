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

package org.cougaar.community.init;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.DBInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * A component which creates and advertises the appropriate
 * CommunityInitializerService ServiceProvider.
 * It can initialize from the CSMART database, using the <code>DBInitializerService</code>,
 * or from XML files, depending on where components were intialized from.
 * <p>
 * @see org.cougaar.community.init.FileCommunityInitializerServiceProvider
 * @see org.cougaar.community.init.DBCommunityInitializerServiceProvider
 **/
public final class CommunityInitializerServiceComponent
extends GenericStateModelAdapter
implements Component
{
  private static final String INITIALIZER_PROP =
    "org.cougaar.core.node.InitializationComponent";

  private ServiceBroker sb;

  private DBInitializerService dbInit;
  private ServiceProvider theSP;
  private LoggingService log;

  // ignore "setServiceBroker", we want the node-level service broker

  public void setNodeControlService(NodeControlService ncs) {
    if (ncs == null) {
      // Revocation
    } else {
      this.sb = ncs.getRootServiceBroker();
    }
  }

  /*
    // DBInitializerService isn't available in the node agent
  public void setDBInitializerService(DBInitializerService dbInit) {
    this.dbInit = dbInit;
  }
  */

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    dbInit = (DBInitializerService) sb.getService(this, DBInitializerService.class, null);

    // Do not provide this service if there is already one there.
    // This allows someone to provide their own component to provide
    // the community initializer service in their configuration
    if (sb.hasService(CommunityInitializerService.class)) {
      // already have CommunityInitializer service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the default community initializer service");
      }
      if (log != LoggingService.NULL) {
        sb.releaseService(this, LoggingService.class, log);
        log = null;
      }
      return;
    }

    theSP = chooseSP();
    if (theSP != null)
      sb.addService(CommunityInitializerService.class, theSP);

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  public void unload() {
    if (theSP != null) {
      sb.revokeService(CommunityInitializerService.class, theSP);
      theSP = null;
    }
    super.unload();
  }

  private ServiceProvider chooseSP() {
    try {
      ServiceProvider sp;
      String prop = System.getProperty(INITIALIZER_PROP);
      if (prop != null && prop.indexOf("DB") != -1 && dbInit != null) {
        sp = new DBCommunityInitializerServiceProvider(dbInit);
        if (log.isInfoEnabled())
          log.info("Using CSMART DB CommunityInitializer");
      } else {
        // Note that these files are XML
        sp = new FileCommunityInitializerServiceProvider();
        if (log.isInfoEnabled())
          log.info("Using File (XML) CommunityInitializer");
      }
      return sp;
    } catch (Exception e) {
      log.error("Exception creating CommunityInitializerService", e);
      return null;
    }
  }
}
