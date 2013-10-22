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

package org.cougaar.core.wp.bootstrap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.Parameters;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component advertises the {@link EnsureIsFoundService}, which
 * allows servers to ensures that their peers are reachable.
 * <p>
 * This implementation maintains the {@link Set} of ensured names
 * and, if non-empty, continually polls (non-blocking) the
 * local {@link WhitePagesService} to make sure the {@link
 * WhitePagesService#getAll(String,long)}s return non-empty data,
 * otherwise it starts the {@link BootstrapService} until the
 * data is found.
 * <p>
 * This is slightly inefficient for a couple reasons:
 * <ol><li>
 * We're polling our cache even if the names are not necessary at all
 * times (e.g. the MTS is still using the data if found a long time
 * ago)
 * </li><li>
 * We're restarting the full bootstrap instead of indicating which
 * specific name(s) we're looking for (e.g. a discoverer just for
 * agent "A" can't help us find agent "B").
 * </li><li>
 * We wait until the next poll cycle to stop our search instead
 * of stopping when the cache finds the data. 
 * </li></ol>
 * However, this code only runs on nodes containing servers, and
 * the data is typically in the cache, so this shouldn't be a real
 * problem.
 * 
 * @property org.cougaar.core.wp.bootstrap.ensureIsFound.checkNamesPeriod
 * Specify how often to check for WP server names.
 * Set to 45 seconds by default.
 * 
 * @property org.cougaar.core.wp.bootstrap.ensureIsFound.checkMtsReachability
 * Specify if MTS-reachability should be tested when checking WP server names.
 * Set to true by default. Should never be set to false (see bug 3907).
 */
public class EnsureIsFoundManager
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;
  private LoggingService logger;
  private ThreadService threadService;
  private ServiceBroker rootsb;

  private EnsureIsFoundManagerConfig config;

  private Schedulable checkNamesThread;

  private BootstrapService bootstrapService;

  private WhitePagesService wp;

  private EnsureIsFoundSP ensureSP;

  private final Map table = new HashMap();

  // locked by table
  private boolean searching;
  
  private AgentStatusService agentStatusService;

  public void setParameter(Object o) {
    configure(o);
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setNodeControlService(NodeControlService ncs) {
    rootsb = (ncs == null ? null : ncs.getRootServiceBroker());
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void setBootstrapService(BootstrapService bootstrapService) {
    this.bootstrapService = bootstrapService;
  }

  public void setWhitePagesService(WhitePagesService wp) {
    this.wp = wp;
  }

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new EnsureIsFoundManagerConfig(o);
  }

  @Override
public void load() {
    super.load();

    configure(null);

    // get the AgentStatusService in load, *but* it's loaded after the
    // WP, so we must use a ServiceAvailableLister.
    ServiceFinder.Callback sfc =
      new ServiceFinder.Callback() {
        public void foundService(Service s) {
          EnsureIsFoundManager.this.foundService(s);
        }
      };
    ServiceFinder.findServiceLater(
        sb, AgentStatusService.class, null, sfc);

    // advertise our service
    ensureSP = new EnsureIsFoundSP();
    rootsb.addService(EnsureIsFoundService.class, ensureSP);
  }

  @Override
public void unload() {
    if (ensureSP != null) {
      rootsb.revokeService(
          EnsureIsFoundService.class, ensureSP);
      ensureSP = null;
    }
    if (bootstrapService != null) {
      sb.releaseService(
          this,
          BootstrapService.class,
          bootstrapService);
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
    if (agentStatusService != null) {
      sb.releaseService(
          this, AgentStatusService.class, agentStatusService);
      agentStatusService = null;
    }
    super.unload();
  }

  private void add(String name) {
    synchronized (table) {
      Entry e = (Entry) table.get(name);
      if (logger.isDetailEnabled()) {
        logger.detail("add("+name+"): "+e);
      }
      if (e == null) {
        e = new Entry();
        table.put(name, e);
      }
      e.i++;
      if (e.i > 1) {
        return;
      }
      if (logger.isInfoEnabled()) {
        logger.info("Added ensured name: "+name);
      }
      if (table.size() > 1) {
        return;
      }
      if (logger.isInfoEnabled()) {
        logger.info("Starting checkNamesThread");
      }
      if (checkNamesThread == null) {
        Runnable checkNamesRunner =
          new Runnable() {
            public void run() {
              // assert (thread == checkNamesThread);
              checkNames();
            }
          };
        checkNamesThread = threadService.getThread(
            this,
            checkNamesRunner,
            "White pages EnsureIsFoundManager");
      }
      checkNamesThread.start();
    }
  }

  private void remove(String name) {
    synchronized (table) {
      Entry e = (Entry) table.get(name);
      if (logger.isDetailEnabled()) {
        logger.detail("remove("+name+"): "+e);
      }
      if (e == null) {
        return;
      }
      e.i--;
      if (e.i <= 0) {
        if (logger.isInfoEnabled()) {
          logger.info("Removed ensured name: "+name);
        }
        table.remove(name);
      }
      if (!table.isEmpty()) {
        return;
      }
      if (logger.isInfoEnabled()) {
        logger.info("Cancelling checkNamesThread");
      }
      if (searching) {
        bootstrapService.stopSearching();
        searching = false;
      }
      if (checkNamesThread != null) {
        checkNamesThread.cancelTimer();
      }
    }
  }

  // timer fired, see if we've found our servers
  private void checkNames() {
    synchronized (table) {
      if (logger.isDebugEnabled()) {
        logger.debug(
            (searching ? "Checking" : "Verifying")+
            " reachability for names["+table.size()+"]: "+
            table.keySet());
      }
      if (table.isEmpty()) {
        return;
      }
      String missingName = null;
      boolean missingDueToQueue = false;
      for (Iterator iter = table.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Entry e = (Entry) me.getValue();
        Map m;
        try {
          m = wp.getAll(name, -1); // non-blocking wp lookup
        } catch (Exception ex) {
          if (logger.isErrorEnabled()) {
            logger.error("White pages lookup("+name+") failed", ex);
          }
          m = null;
        }
        if (logger.isDetailEnabled()) {
          logger.detail("wp lookup("+name+") found "+m);
        }
        if (m == null || m.isEmpty()) {
          missingName = name;
          break;
        }
        // A successfull White Pages lookup is not sufficient.
        // It's possible to access the white pages without being able to
        // send messages. See bug 3907.
        if (config.checkMtsReachability) {
          AgentStatusService.AgentState state =
            (agentStatusService == null ?
                (null) :
                  agentStatusService.getRemoteAgentState(
                      MessageAddress.getMessageAddress(name)));
          if (logger.isDetailEnabled()) {
            logger.detail("Length of " + name + " MTS queue: " +
                (state == null ?
                    "AgentStatusService not available yet" :
                      state.queueLength + " messages"));
          }
          if (state == null || state.queueLength > 0) {
            missingDueToQueue = true;
            missingName = name;
            break;
          }
        }
        // White pages and MTS reachability tests were successful.
        e.found = true;
      }
      if (missingName == null) {
        // found wp entries for all names, stop searching
        if (searching) {
          if (logger.isInfoEnabled()) {
            logger.info(
                "Stopping bootstrap search, all "+table.size()+
                " names are reachable: "+table.keySet());
          }
          bootstrapService.stopSearching();
          searching = false;
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Verified that all names are reachable");
          }
        }
      } else {
        // continue bootstrap search
        if (searching) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Keep searching, "+missingName+
                " was not reachable yet " +
                (missingDueToQueue ?
                    "due to MTS queue" :
                    "due to failed white pages lookup"));
          }
        } else {
          if (logger.isInfoEnabled()) {
            logger.info(
                "Starting bootstrap search, "+
                missingName+" was not reachable yet " +
                (missingDueToQueue ?
                    "due to MTS queue" :
                    "due to failed white pages lookup"));
          }
          bootstrapService.startSearching();
          searching = true;
        }
      }
      // wake later
      checkNamesThread.schedule(config.checkNamesPeriod);
    }
  }
  
  private void foundService(Service s) {
    if (logger.isDetailEnabled()) {
      logger.detail("foundService("+s+"), searching="+searching);
    }
    if (s instanceof AgentStatusService) {
      agentStatusService = (AgentStatusService) s;
    }
  }

  private static class Entry {
    public int i;
    public boolean found;
    @Override
   public String toString() {
      return "(entry i="+i+" found="+found+")";
    }
  }

  private class EnsureIsFoundSP implements ServiceProvider {
    private final EnsureIsFoundService eifs =
      new EnsureIsFoundService() {
        public void add(String name) {
          EnsureIsFoundManager.this.add(name);
        }
        public void remove(String name) {
          EnsureIsFoundManager.this.remove(name);
        }
      };
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (!EnsureIsFoundService.class.isAssignableFrom(serviceClass)) {
        return null;
      }
      return eifs;
    }
    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
    }
  }

  /** config options */
  private static class EnsureIsFoundManagerConfig {
    public final long checkNamesPeriod;
    public final boolean checkMtsReachability;
    
    public EnsureIsFoundManagerConfig(Object o) {
      Parameters p =
        new Parameters(o, "org.cougaar.core.wp.bootstrap.ensureIsFound.");
      checkNamesPeriod = p.getLong("checkNamesPeriod", 45000);
      checkMtsReachability = p.getBoolean("checkMtsReachability", true);
    }
  }
}
