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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.ThreadListener;
import org.cougaar.util.UnaryPredicate;

/**
 * This component implements the actual freezing of an agent. Freezing is
 * accomplished by preventing the ThreadService from running certain
 * classes of components. The relevant object is the so-called
 * "consumer" of the ThreadService. For plugins, this is the plugin
 * itself. For other uses of the ThreadService, the "consumer" may be
 * different.
 * <p>
 * Generally, all plugins except those involved in the freeze process
 * are prevented from running, but this can be modified by rules
 * specified as plugin parameters. The rules are applied in this
 * order:
 * <pre>
 * "allow " + FreezePlugin.class.getName()
 * first plugin parameter
 * second plugin parameter
 * etc.
 * "deny " + PluginBase.class.getName()
 * </pre>
 * <p>
 * The form of the rule is one of the words, "deny" or "allow",
 * followed by a space followed by the name of the class or interface
 * that should be affected by the rule. The rule matches if it is
 * legal to assign the consumer to a variable of the type named in the
 * rule. This includes the class of the consumer itself, all
 * interfaces implemented by the consumer or their superinterfaces,
 * all superclasses of the consumer, and all interfaces implemented by
 * any superclass or their superinterfaces.
 * <p>
 * The first rule is built-in and cannot be overridden. It allows all
 * the freeze plugins to run while frozen. This is obviously necessary
 * to handle thawing a frozen society. The last rule is always added
 * and prevents all plugins that extend PluginBase from running except
 * those allowed by preceding rules. While it is possible to write a
 * component that behaves as a plugin but does not extend PluginBase,
 * this does not happen in practice.
 * <p>
 * The effect of this final rule can be nullified by including rules
 * (as plugin parameters) that specifically allow individual plugins.
 * Indeed, the whole class of plugins extending PluginBase could be
 * allowed. It is possible to prevent anything from being frozen in an
 * agent by making the first plugin parameter be "allow
 * java.lang.Object". Since every class extends java.lang.Object, this
 * will allow every class to run.
 * <p>
 * NOTE: This is part of the older mechanism for freezing the society.  The
 * current mechanism uses FreezeServlet located on every agent in the society,
 * and depends on some external process to tell all agents to freeze.  This older
 * mechanism has not been removed so that people can continue to use a single servlet
 * to freeze the entire society, but the FreezeServlet mechanism is preferred now.
 */
public class FreezeTargetPlugin extends FreezePlugin implements ThreadListener {
  private static class BadGuy {
    private Thread thread;
    private Schedulable schedulable;
    int hc;
    public BadGuy(Schedulable s, Thread t) {
      thread = t;
      schedulable = s;
      hc = System.identityHashCode(t) + System.identityHashCode(s);
    }
    @Override
   public int hashCode() {
      return hc;
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) return true;
      if (o instanceof BadGuy) {
        BadGuy that = (BadGuy) o;
        return this.thread == that.thread && this.schedulable == that.schedulable;
      }
      return false;
    }
    @Override
   public String toString() {
      return schedulable.getState() + ": " + schedulable.getConsumer().toString();
    }
  }
  private IncrementalSubscription relaySubscription;
  // True if we have frozen this agent.
  private boolean isFrozen = false;
  private boolean isFreezing = false;
  private ThreadListenerService threadListenerService;
  private ThreadControlService threadControlService;
  private Rules rules = new Rules();
  private Set badGuys = new HashSet(); // Records the bad guys we have
                                       // seen enter the run state
                                       // that have not left the run
                                       // state

  @Override
public void unload() {
    if (threadControlService != null) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, ThreadListenerService.class, threadListenerService);
      sb.releaseService(this, ThreadControlService.class, threadControlService);
    }
    super.unload();
  }

  private UnaryPredicate myThreadQualifier =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        Schedulable schedulable = (Schedulable) o;
        Object consumer = schedulable.getConsumer();
        return rules.allow(consumer);
      }
    };

  // Thread control logic. Threads are classified as good or bad. When
  // frozen, we regulate the max running thread count to be no more
  // than the number of goodguys that are on the runnable queue. The
  // number of goodguys is the total of the known good guys (in the
  // goodGuys set) and the anonymous ones. We have to assume that any
  // thread we have never seen is a good guy. If an anonymous good guy
  // steps off the stage we will recognize him and reduce the
  // anonymousGoodGuys count.
  public synchronized void threadQueued(Schedulable schedulable, Object consumer) {}
  public synchronized void threadDequeued(Schedulable schedulable, Object consumer) {}
  public synchronized void threadStarted(Schedulable schedulable, Object consumer) {
    if (logger.isDetailEnabled()) logger.detail("threadStarted: " + consumer);
    if (!rules.allow(consumer)) {
      badGuys.add(new BadGuy(schedulable, Thread.currentThread()));
    }
  }
  public synchronized void threadStopped(Schedulable schedulable, Object consumer) {
    if (logger.isDetailEnabled()) logger.detail("threadStopped: " + consumer);
    if (!rules.allow(consumer)) {
      Thread currentThread = Thread.currentThread();
      badGuys.remove(new BadGuy(schedulable, currentThread));
    }
  }
  public void rightGiven(String consumer) {}
  public void rightReturned(String consumer) {}

  private void setThreadLimit() {
    threadControlService.setQualifier(myThreadQualifier);
  }

  private void unsetThreadLimit() {
    threadControlService.setQualifier(null);
  }

  @Override
public void setupSubscriptions() {
    super.setupSubscriptions();
    rules.addAllowRule(FreezePlugin.class);
    // Hope this is a List cause order is important.
    Collection params = getParameters();
    for (Iterator i = params.iterator(); i.hasNext(); ) {
      String ruleSpec = (String) i.next();
      try {
        rules.addRule(ruleSpec);
      } catch (Exception e) {
        logger.error("Bad parameter: " + ruleSpec, e);
      }
    }
    rules.addDenyRule(PluginBase.class);
    if (logger.isInfoEnabled()) logger.info("rules=" + rules);
    ServiceBroker sb = getServiceBroker();
    threadControlService = sb.getService(this, ThreadControlService.class, null);
    threadListenerService = sb.getService(this, ThreadListenerService.class, null);
    threadListenerService.addListener(this);
    relaySubscription = blackboard.subscribe(targetRelayPredicate);
  }

  @Override
public void execute() {
    if (timerExpired()) {
      cancelTimer();
      if (isFreezing) checkStopped();
    }
    if (relaySubscription.hasChanged()) {
      if (relaySubscription.isEmpty()) {
        if (logger.isDebugEnabled()) {
          logger.debug(relaySubscription.getRemovedCollection().size() + " removes");
        }
        if (isFrozen) {
          if (logger.isDebugEnabled()) logger.debug("thawed");
          unsetThreadLimit();       // Unset thread limit
          isFrozen = false;
        }
      } else {
        if (!isFrozen) {
          if (logger.isDebugEnabled()) logger.debug("freezing");
          setThreadLimit();
          isFrozen = true;
          isFreezing = true;
          checkStopped();
        }
      }
    }
  }

  private synchronized void checkStopped() {
    int stillRunning = badGuys.size();
    Set unfrozenAgents;
    if (stillRunning <= 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("Frozen");
        isFreezing = false;
      }
      unfrozenAgents = Collections.EMPTY_SET;
    } else {
      if (logger.isDebugEnabled()) {
        Set consumerSet = new HashSet();
        for (Iterator i = badGuys.iterator(); i.hasNext(); ) {
          BadGuy bg = (BadGuy) i.next();
          consumerSet.add(bg.toString());
        }
        logger.debug("Still running: " + consumerSet);
      }
      unfrozenAgents = Collections.singleton(getAgentIdentifier());
      resetTimer(5000);
    }
    for (Iterator i = relaySubscription.iterator(); i.hasNext(); ) {
      FreezeRelayTarget relay = (FreezeRelayTarget) i.next();
      relay.setUnfrozenAgents(unfrozenAgents);
      blackboard.publishChange(relay);
    }
  }
}    
