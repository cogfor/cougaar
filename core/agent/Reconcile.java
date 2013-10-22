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

package org.cougaar.core.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.IncarnationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component watches blackboard message traffic and periodically
 * checks the white pages for agent restarts, which require
 * blackboard-to-blackboard state reconciliation.
 */
public final class Reconcile
extends GenericStateModelAdapter
implements Component
{
  private ServiceBroker sb;

  private LoggingService log;
  private MessageAddress localAgent;
  private IncarnationService incarnationService;

  private PersistenceService ps;
  private PersistenceClient pc;

  private ReconcileAddressWatcherServiceProvider rawsp;
  private ReconcileEnablerServiceProvider resp;

  private BlackboardForAgent bb;

  private Schedulable reconcileThread;
  private final List reconcileTmp = new ArrayList();

  private final List queue = new ArrayList();
  private boolean active;

  private final IncarnationService.Callback cb =
    new ReconcileCallback();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    localAgent = find_local_agent();

    log = sb.getService(this, LoggingService.class, null);
    String prefix = localAgent+": ";
    log = LoggingServiceWithPrefix.add(log, prefix);

    // get incarnation service
    incarnationService = sb.getService(this, IncarnationService.class, null);
    if (incarnationService == null) {
      throw new RuntimeException(
          "Unable to obtain IncarnationService");
    }

    // create queue thread
    Runnable reconcileRunner = 
      new Runnable() {
        public void run() {
          reconcileNow();
        }
      };
    ThreadService threadService = sb.getService(this, ThreadService.class, null);
    reconcileThread = threadService.getThread(
        this, reconcileRunner, "Reconciler");
    sb.releaseService(this, ThreadService.class, threadService);

    register_persistence();

    // get mobile state, to make sure we don't miss a reconcile
    // and avoid unnecessary reconciles
    Object o = rehydrate();
    if (o instanceof Map) {
      resubscribe((Map) o);
    }
    o = null;

    // create "recordAddress" access
    rawsp = new ReconcileAddressWatcherServiceProvider();
    sb.addService(ReconcileAddressWatcherService.class, rawsp);

    // create "enableReconcile" to enable reconcile processing
    // once the agent has loaded, and "disableReconcile" to
    // disable reconciles when the agent is persisting or unloading.
    resp = new ReconcileEnablerServiceProvider();
    sb.addService(ReconcileEnablerService.class, resp);
  }

  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  @Override
public void start() {
    super.start();
    // called later via ReconcileEnablerService:
    //enableReconcile();
  }

  @Override
public void suspend() {
    super.suspend();
    // called earlier via ReconcileEnablerService:
    //disableReconcile();
  }

  @Override
public void resume() {
    super.resume();
    // called later via ReconcileEnablerService:
    //enableReconcile();
  }

  @Override
public void stop() {
    super.stop();
    // called earlier via ReconcileEnablerService:
    //disableReconcile();
  }

  @Override
public void unload() {
    super.unload();

    if (resp != null) {
      sb.revokeService(ReconcileEnablerService.class, resp);
      resp = null;
    }
    if (rawsp != null) {
      sb.revokeService(ReconcileAddressWatcherService.class, rawsp);
      rawsp = null;
    }

    unregister_persistence();

    if (incarnationService != null) {
      // release service, unsubscribes our callbacks:
      sb.releaseService(
          this, IncarnationService.class, incarnationService);
      incarnationService = null;
    }
  }

  private Object captureState() {
    if (getModelState() == ACTIVE) {
      if (log.isDetailEnabled()) {
        log.detail("ignoring persist while active");
      }
      return null;
    }

    synchronized (queue) {
      if (active && log.isErrorEnabled()) {
        log.error("Attempting to captureState while active!");
      }
    }

    // capture our Map<agentId, Long> state
    //
    // TODO: our active flag is false, so we won't reconcile while
    // we're capturing our state.  However, we don't actually lockout
    // the incarnationChanged callbacks, so there's a small risk that
    // our queue is not empty, meaning that we'd miss a reconcile,
    // but we'd rather not block the incarnationService. 
    Map ret;
    // get Map<agentId, Set<Callback>>
    Map subs = incarnationService.getSubscriptions();
    if (subs == null || subs.isEmpty()) {
      ret = Collections.EMPTY_MAP;
    } else {
      // for all agentIds, get our inc
      ret = new HashMap(subs.size());
      for (Iterator iter = subs.keySet().iterator();
          iter.hasNext();
          ) {
        MessageAddress agentId = (MessageAddress) iter.next();
        long inc = incarnationService.getIncarnation(agentId);
        ret.put(agentId, new Long(inc));
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("Captured state["+ret.size()+"]: "+ret);
    }
    return ret;
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName();
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          Object o = captureState();
          // must return mutable list!
          List l = new ArrayList(1);
          l.add(o);
          return l;
        }
      };
    ps = 
      sb.getService(
          pc, PersistenceService.class, null);
  }

  private void unregister_persistence() {
    if (ps != null) {
      sb.releaseService(
          pc, PersistenceService.class, ps);
      ps = null;
      pc = null;
    }
  }

  private Object rehydrate() {
    RehydrationData rd = ps.getRehydrationData();
    if (rd == null) {
      if (log.isInfoEnabled()) {
        log.info("No rehydration data found");
      }
      return null;
    }

    List l = rd.getObjects();
    rd = null;
    int lsize = (l == null ? 0 : l.size());
    if (lsize < 1) {
      if (log.isInfoEnabled()) {
        log.info("Invalid rehydration list? "+l);
      }
      return null;
    }
    Object o = l.get(0);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("Null rehydration state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Found rehydrated state");
      if (log.isDetailEnabled()) {
        log.detail("state is "+o);
      }
    }

    return o;
  }

  private void resubscribe(Map m) {
    if (m == null || m.isEmpty()) {
      return;
    }
    for (Iterator iter = m.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      MessageAddress agentId = (MessageAddress) me.getKey();
      Long l = (Long) me.getValue();
      long inc = l.longValue();
      boolean b = incarnationService.subscribe(agentId, cb, inc);
      if (b) {
        if (log.isDebugEnabled()) {
          log.debug("rehydrate-recordAddress("+agentId+", "+inc+")");
        }
      } else {
        if (log.isWarnEnabled()) {
          log.warn(
              "subscribe("+agentId+", "+cb+", "+inc+
              ") returned false");
        }
      }
    }
  }

  private void recordAddress(MessageAddress agentId) {
    if (incarnationService.subscribe(agentId, cb) &&
        log.isDebugEnabled()) {
      log.debug("recordAddress("+agentId+")");
    }
  }

  private void enableReconcile() {
    if (bb == null) {
      bb = sb.getService(this, BlackboardForAgent.class, null);
      if (bb == null) {
        throw new RuntimeException(
            "Unable to obtain BlackboardForAgent");
      }
    }

    synchronized (queue) {
      if (active) {
        return;
      }
      active = true;
      if (log.isDebugEnabled()) {
        log.debug("Enabled");
      }
      if (!queue.isEmpty()) {
        reconcileThread.start();
      }
    }
  }

  private void disableReconcile() {
    synchronized (queue) {
      if (!active) {
        return;
      }
      active = false;
      if (log.isDebugEnabled()) {
        log.debug("Disabled");
      }
    }

    if (bb != null) {
      sb.releaseService(this, BlackboardForAgent.class, bb);
      bb = null;
    }
  }

  private void reconcileLater(MessageAddress agentId) {
    synchronized (queue) {
      queue.add(agentId);
      if (active) {
        reconcileThread.start();
      }
    }
  }

  private void reconcileNow() {
    synchronized (queue) {
      if (!active || queue.isEmpty()) {
        return;
      }
      reconcileTmp.addAll(queue);
      queue.clear();
    }
    for (int i = 0, n = reconcileTmp.size(); i < n; i++) {
      MessageAddress agentId = (MessageAddress) reconcileTmp.get(i);
      reconcileNow(agentId);
    }
    reconcileTmp.clear();
  }

  private void reconcileNow(MessageAddress agentId) {
    if (log.isInfoEnabled()) {
      log.info(
          "Detected (re)start of agent "+agentId+
          ", synchronizing blackboards");
    }
    bb.restartAgent(agentId);
  }

  private class ReconcileCallback
    implements IncarnationService.Callback, Comparable {
      public void incarnationChanged(MessageAddress addr, long inc) {
        // this could block, so run it in a separate thread
        Reconcile.this.reconcileLater(addr);
      }
      public int compareTo(Object o) {
        // make me last, so MTS links are reset before I reconcile!
        return (o instanceof ReconcileCallback ?  0 : -1);
      }
      @Override
      public String toString() {
        return "(reconcile for "+localAgent+")";
      }
    }

  private final class ReconcileAddressWatcherServiceProvider
    implements ServiceProvider {
      private final ReconcileAddressWatcherService raws;
      public ReconcileAddressWatcherServiceProvider() {
        raws = new ReconcileAddressWatcherService() {
          public void sentMessageTo(MessageAddress addr) {
            recordAddress(addr);
          }
          public void receivedMessageFrom(MessageAddress addr) {
            recordAddress(addr);
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (ReconcileAddressWatcherService.class.isAssignableFrom(serviceClass)) {
          return raws;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }

  private final class ReconcileEnablerServiceProvider
    implements ServiceProvider {
      private final ReconcileEnablerService res;
      public ReconcileEnablerServiceProvider() {
        res = new ReconcileEnablerService() {
          public void startTimer() {
            enableReconcile();
          }
          public void stopTimer() {
            disableReconcile();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (ReconcileEnablerService.class.isAssignableFrom(serviceClass)) {
          return res;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }
}
