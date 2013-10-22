/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.core.service.PlaybookReadService;
import org.cougaar.util.CircularQueue;

/**
 * A container for the active Plays. The plays are initialized from a
 * file specified as a plugin parameter. Playbook users access the
 * plays through two services: {@link PlaybookReadService} and
 * {@link PlaybookConstrainService}. The former returns the constrained
 * plays. The latter constrains the original plays with
 * {@link OperatingModePolicy}s.
 **/
public class PlaybookManager
  extends ComponentPlugin
  implements ServiceProvider
{
  private Playbook playbook;
  private LoggingService logger;
  private List listeners = new ArrayList(2);
  private CircularQueue todo = new CircularQueue();

  private class PlaybookReadServiceImpl implements PlaybookReadService {
    private boolean active = true;
    /**
     * Gets an array of the current plays
     * @return an array of the current plays
     **/
    public Play[] getCurrentPlays() {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      return playbook.getCurrentPlays();
    }

    /**
     * Add a listener to the playbook. The listener will be
     * publishChanged if this Playbook is modified.
     * @param l the Listener
     **/
    public void addListener(Listener l) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      listeners.add(l);
    }
    /**
     * Remove a listener to the playbook. The listener will no longer
     * be publishChanged if this Playbook is modified.
     * @param l the Listener
     **/
    public void removeListener(Listener l) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      listeners.remove(l);
    }
  }

  private class PlaybookConstrainServiceImpl implements PlaybookConstrainService {
    private boolean active = true;
    /**
     * Add another OperatingModePolicy constraint. The plays are
     * modified so that in all cases where the if clause of the
     * constraint is true the OperatingMode ranges will all fall
     * within the constraint.
     * @param omp the constraint to add
     **/
    public void constrain(OperatingModePolicy omp) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      playbook.addConstraint(omp);
      fireListenersLater();
    }

    /**
     * Remove an OperatingModePolicy constraint. The current plays are
     * recomputed to omit the removed constraint.
     * @param omp the constraint to remove
     **/
    public void unconstrain(OperatingModePolicy omp) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      playbook.removeConstraint(omp);
      fireListenersLater();
    }
  }

  /**
   * Override to register the services we provide.
   **/
  @Override
public void load() {
    super.load();
    logger = getServiceBroker().getService(this, LoggingService.class, null);
    playbook = new Playbook(logger);
    getServiceBroker().addService(PlaybookReadService.class, this);
    getServiceBroker().addService(PlaybookConstrainService.class, this);
  }

  /**
   * Override to unregister the services we provide.
   **/
  @Override
public void unload() {
    getServiceBroker().revokeService(PlaybookConstrainService.class, this);
    getServiceBroker().revokeService(PlaybookReadService.class, this);
    getServiceBroker().releaseService(this, LoggingService.class, logger);
    super.unload();
  }

  /**
   * Read the plays from a file.
   **/
  @Override
public void setupSubscriptions() {
    Iterator iter = getParameters().iterator();
    if (!iter.hasNext()) {
      logger.error("Missing playbook file name.");
    } else {
      String playFileName = iter.next().toString();
      try {
        Reader is = new InputStreamReader(getConfigFinder().open(playFileName));
        try {
          Parser p = new Parser(is, logger);
          Play[] plays = p.parsePlays();
          playbook.setPlays(plays);
          fireListeners();
        } finally {
          is.close();
        }
      } catch (Exception e) {
        logger.error("Error parsing play file", e);
      }
    }
  }

  /**
   * Handle requests that arrived through our services. These requests
   * all fire listeners. The services cannot themselves do this
   * because of the possibility of a deadlock due to attempts to open
   * two blackboard transactions simultaneously. The requests are
   * placed in a queue and executed here.
   **/
  @Override
public void execute() {
    synchronized (todo) {
      while (todo.size() > 0) {
        try {
          ((Runnable) todo.next()).run();
        } catch (RuntimeException e) {
          logger.error("Error running delayed job", e);
        }
      }
    }
  }

  /**
   * Gets (creates) one of our services. Part of the implementation of
   * the ServiceProvider API
   * @param sb the ServiceBroker making the request
   * @param requestor the actual requestor on whose behalf the broker is acting
   * @param serviceClass the class of the Service desired.
   * @return an instance of the requested Service if it one we supply.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == PlaybookReadService.class) {
      return new PlaybookReadServiceImpl();
    }
    if (serviceClass == PlaybookConstrainService.class) {
      return new PlaybookConstrainServiceImpl();
    }
    return null;
  }

  /**
   * Release one of our services. The services use no resources, so
   * there is nothing to do.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class
                             serviceClass, Object service) {
    if (service instanceof PlaybookReadServiceImpl) {
      ((PlaybookReadServiceImpl) service).active = false;
      return;
    }
    if (service instanceof PlaybookConstrainServiceImpl) {
      ((PlaybookConstrainServiceImpl) service).active = false;
      return;
    }
    throw new IllegalArgumentException("Not my service: " + service);
  }

  private void fireListenersLater() {
    synchronized (todo) {
      todo.add(new Runnable() {
        public void run() {
          fireListeners();
        }
      });
    }
    blackboard.signalClientActivity();
  }

  private void fireListeners() {
    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
      PlaybookReadService.Listener l = (PlaybookReadService.Listener) i.next();
      blackboard.publishChange(l);
    }
    if (logger.isDebugEnabled()) logger.debug("New constrained plays" + SystemProperties.getProperty("line.separator") + this);
  }
}
