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

package org.cougaar.planning.ldm;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.DBInitializerService;
import org.cougaar.core.node.Node;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.service.AssetInitializerService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.Parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A component which creates and advertises the appropriate
 * AssetInitializerService ServiceProvider.
 * <p/>
 * The rule is that we use the CSMART DB if components were intialized from there.
 * Otherwise, if the components coming from XML,
 * we use the non-CSMART DB. Otherwise we try to initialize from INI-style files.
 * <p/>
 *
 * @see FileAssetInitializerServiceProvider
 * @see DBAssetInitializerServiceProvider
 * @see NonCSMARTDBInitializerServiceImpl
 */
public final class AssetInitializerServiceComponent
    extends GenericStateModelAdapter
    implements Component {

  // Used below to confirm good cougaar.rc file for use with XML files
  private static final String DATABASE = "org.cougaar.refconfig.database";
  private static final String USER = "org.cougaar.refconfig.user";
  private static final String PASSWORD = "org.cougaar.refconfig.password";

  private ServiceBroker sb;

  private DBInitializerService dbInit = null;
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

  public void load() {
    super.load();

    log = (LoggingService)
        sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    // Do not provide this service if there is already one there.
    // This allows someone to provide their own component to provide
    // the asset initializer service in their configuration
    if (sb.hasService(AssetInitializerService.class)) {
      // already have AssetInitializer service!
      //
      // leave the existing service in place
      if (log.isInfoEnabled()) {
        log.info("Not loading the default asset initializer service");
      }
      if (log != LoggingService.NULL) {
        sb.releaseService(this, LoggingService.class, log);
        log = null;
      }
      return;
    }

    theSP = chooseSP();
    if (theSP != null)
      sb.addService(AssetInitializerService.class, theSP);

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  public void unload() {
    if (theSP != null) {
      sb.revokeService(AssetInitializerService.class, theSP);
      theSP = null;
    }

    super.unload();
  }

  // If the DB property was supplied and we have a DBInializerService,
  // we initializer Organization/Entity assets from the configuration.database
  // Otherwise if the XML property was supplied, we check for a valid
  // cougaar.rc file for the refconfig.database. If we have one,
  // we try to initialize assets from that database.
  // If we have no such cougaar.rc file, or if some other
  // parameter was supplied, we initialize assets strictly
  // from files. 
  private ServiceProvider chooseSP() {
    try {
      ServiceProvider sp = null;
      String prop = System.getProperty(Node.INITIALIZER_PROP);

      // If user specified to load from the database
      if (prop != null && prop.indexOf("DB") != -1) {
        // Init from CSMART DB
        dbInit = (DBInitializerService) sb.getService(this, DBInitializerService.class, null);
	if (dbInit != null) {
	  if (log.isInfoEnabled())
	    log.info("Will init OrgAssets from CSMART DB");
	}
      } else if (prop != null && prop.indexOf("XML") != -1) {
        // Else if user specified to load from XML
	// First check to see if user set up a DB for use with XML files
	if (rcFileExists() && isValidRCFile()) {
	  // Initing config from XML. Assets will come from non-CSMART DB
	  // Create a new DBInitializerService
	  dbInit = new NonCSMARTDBInitializerServiceImpl();
	  if (dbInit != null) {
	    if (log.isInfoEnabled()) {
	      log.info("Will init OrgAssets from NON CSMART DB!");
	    }
	  }
	}
      }
      
      // If we got a good DB set up, then use that.
      if (dbInit != null) {
	sp = new DBAssetInitializerServiceProvider(dbInit);
      }

      // Handle this separately in case the above fails somehow
      if (sp == null) {
	// If user specified INI files, or set up no database, ie
	// if didn't get a good DBInitializerService, then
	// use files. This may be INI or XML.
	sp = new FileAssetInitializerServiceProvider();
	if (log.isInfoEnabled())
	  log.info("Not using a database, initializing solely from Files.");
      }

      return sp;
    } catch (Exception e) {
      log.error("Exception while creating AssetInitializerService", e);
      return null;
    }
  }

  // Helper function to check for a cougaar.rc file without parsing it
  private boolean rcFileExists() {
    boolean found = false;
    try {
      File f = new File(System.getProperty("user.home") + File.separator + ".cougaarrc");
      if (!f.exists()) {
        InputStream in = ConfigFinder.getInstance().open("cougaar.rc");
        if (in != null) {
          found = true;
        }
      } else {
        found = true;
      }
    } catch (IOException e) {
      //We really do want to ignore this!
    }

    if (!found) {
      // util.Parameters will tell people this already
      if (log.isInfoEnabled())
	log.info("No Cougaar rc file found.");
    }

    return found;
  }

  // Use Parameters utilities parse of the cougaar.rc file
  // to check for key parameters needed when running from XML files
  private boolean isValidRCFile() {
    boolean valid = false;

    valid = (Parameters.findParameter(DATABASE) == null) ? false : true;
    valid &= (Parameters.findParameter(USER) == null) ? false : true;
    valid &= (Parameters.findParameter(PASSWORD) == null) ? false : true;
    return valid;
  }

}
