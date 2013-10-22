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

package org.cougaar.core.node;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link DBInitializerService} and
 * CSMART-database {@link ComponentInitializerService}.
 *
 * @property org.cougaar.node.name org.cougaar.experiment.id
 * CSMART database experiment identifier
 */
public class DBComponentInitializerServiceComponent
extends GenericStateModelAdapter
implements Component
{
  private static final String EXPTID_PROP = "org.cougaar.experiment.id";

  private ServiceBroker sb;
  private ServiceProvider theInitSP;
  private ServiceProvider theDBSP;

  public void setServiceBroker(ServiceBroker sb) {
    // this is the *node* service broker!  The NodeControlService
    // is not available until the node-agent is created...
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    LoggingService log = sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // Do not provide this service if there is already one there.
    // This allows someone to provide their own component to provide
    // the asset initializer service in their configuration
    DBInitializerService dbInit;
    String experimentId = SystemProperties.getProperty(EXPTID_PROP);
    if (sb.hasService(DBInitializerService.class)) {
      // already have DBInitializer service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DBInitializer service"+
            ", it already exists");
      }
      dbInit = sb.getService(
          this, DBInitializerService.class, null);
    } else if (experimentId == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DBInitializer service"+
            ", missing system property -D"+
            EXPTID_PROP);
      }
      dbInit = null;
    } else {
      if (log.isInfoEnabled()) {
        log.info(
            "Creating a new DBInitializer service"+
            ", using system property -D"+
            EXPTID_PROP+"="+experimentId);
      }
      try {
        dbInit = new DBInitializerServiceImpl(experimentId);
      } catch (Exception e) {
        throw new RuntimeException(
            "Unable to load Database Initializer.");
      }
      theInitSP = new DBInitializerServiceProvider(dbInit);
      sb.addService(DBInitializerService.class, theInitSP);
    }

    if (sb.hasService(ComponentInitializerService.class)) {
      // already have a ComponentInitializer? 
      // Leave the existing one in place
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DB ComponentInitializer service"+
            ", it already exists");
      }
    } else if (dbInit == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Not loading the DB ComponentInitializer service"+
            ", missing the DBInitializer service");
      }
    } else {
      if (log.isDebugEnabled())
        log.debug(
            "Creating a new DB ComponentInitializer service"+
            " based on the DBInitializer service");
      theDBSP = new DBComponentInitializerServiceProvider(dbInit);
      sb.addService(ComponentInitializerService.class, theDBSP);
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  @Override
public void unload() {
    if (theInitSP != null) {
      sb.revokeService(DBInitializerService.class, theInitSP);
      theInitSP = null;
    }
    if (theDBSP != null) {
      sb.revokeService(ComponentInitializerService.class, theDBSP);
      theDBSP = null;
    }
    super.unload();
  }

  private static class DBInitializerServiceProvider
    implements ServiceProvider {

      private final DBInitializerService dbInit;

      public DBInitializerServiceProvider(
          DBInitializerService dbInit) {
        this.dbInit = dbInit;
      }

      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (serviceClass != DBInitializerService.class) {
          throw new IllegalArgumentException(
              getClass()+" does not furnish "+serviceClass);
        }
        return dbInit;
      }

      public void releaseService(ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
