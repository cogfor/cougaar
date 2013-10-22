/*
 * <copyright>
 *  
 *  Copyright 2001-2007 BBNT Solutions, LLC
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

package org.cougaar.core.blackboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BindingUtility;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.SubscriptionLifeCycle;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.service.SuicideService;
import org.cougaar.util.SyncTriggerModelImpl;
import org.cougaar.util.Trigger;
import org.cougaar.util.TriggerModel;

/**
 * This component is a standard base class for {@link Component}s
 * that use the {@link BlackboardService}.
 * <p>
 * Plugins are the most common example of such components.<br>
 * <code>ComponentPlugin</code> is an extension of this class.
 * <p>
 * Create a derived class by implementing 
 * <tt>setupSubscriptions()</tt> and <tt>execute()</tt>.
 * <p>
 * Note that both "precycle()" and "cycle()" will be run by the
 * scheduler.  This means that the scheduling order <i>in relation to 
 * other scheduled Components</i> may be *random* (i.e. this 
 * BlackboardClientComponent might load first but be precycled last!).  In 
 * general a Component should <b>not</b> make assumptions about the 
 * load or schedule ordering.
 *
 * @see org.cougaar.core.plugin.ComponentPlugin
 */
public abstract class BlackboardClientComponent 
  extends org.cougaar.util.GenericStateModelAdapter
  implements Component, BlackboardClient, SubscriptionLifeCycle
{
  private Object parameter = null;

  protected MessageAddress agentId;
  private SchedulerService scheduler;
  protected BlackboardService blackboard;
  protected AlarmService alarmService;
  protected AgentIdentificationService agentIdentificationService;
  private SuicideService ss;
  protected String blackboardClientName;

  private BindingSite bindingSite;
  private ServiceBroker sb;

  private TriggerModel tm;
  private SubscriptionWatcher watcher;
  
  public BlackboardClientComponent() { 
  }
  
  /**
   * Called just after construction (via introspection) by the 
   * loader if a non-null parameter Object was specified by
   * the ComponentDescription.
   */
  public void setParameter(Object param) {
    parameter = param;
    setBlackboardClientName(computeBlackboardClientName(param));
  }
  
  /**
   * @return the parameter set by {@link #setParameter}
   */
  public Object getParameter() {
    return parameter;
  }

  /** 
   * Get any Component parameters passed by the instantiator.
   * @return The parameter specified
   * if it was a collection, a collection with one element (the parameter) if 
   * it wasn't a collection, or an empty collection if the parameter wasn't
   * specified.
   */
  public Collection getParameters() {        
    if (parameter == null) {
      return new ArrayList(0);
    } else {
      if (parameter instanceof Collection) {
        return (Collection) parameter;
      } else {
        List l = new ArrayList(1);
        l.add(parameter);
        return l;
      }
    }
  }
  
  /**
   * Binding site is set by reflection at creation-time.
   */
  public void setBindingSite(BindingSite bs) {
    bindingSite = bs;
    // set "sb" now, for backwards compatibility
    sb = bs.getServiceBroker();
  }

  /**
   * Get the binding site, for subclass use.
   */
  protected BindingSite getBindingSite() {
    return bindingSite;
  }

  /**
   * ServiceBroker is set by reflection at creation-time.
   */
  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }
  
  /** 
   * Get the ServiceBroker, for subclass use.
   */
  protected ServiceBroker getServiceBroker() {
    return sb;
  }

  // rely upon load-time introspection ("public void setX(X)") to
  // set these services.  don't worry about revokation.
  public final void setSchedulerService(SchedulerService ss) {
    scheduler = ss;
  }
  public final void setBlackboardService(BlackboardService bs) {
    blackboard = bs;
  }
  public final void setAlarmService(AlarmService s) {
    alarmService = s;
  }
  public final void setAgentIdentificationService(AgentIdentificationService ais) {
    agentIdentificationService = ais;
    MessageAddress an;
    if ((ais != null) &&
        ((an = ais.getMessageAddress()) != null)) {
      agentId = an;
      //    } else { // Revocation -- nothing more to do 
    }
  }
  public void setSuicideService(SuicideService ss) {
    this.ss = ss;
  }

  /**
   * Get the blackboard service, for subclass use.
   */
  protected BlackboardService getBlackboardService() {
    return blackboard;
  }

  /**
   * Get the alarm service, for subclass use.
   */
  protected AlarmService getAlarmService() {
    return alarmService;
  }
  
  protected final void requestCycle() {
    tm.trigger();
  }

  //
  // implement GenericStateModel:
  //

  @Override
public void load() {
    super.load();
    
    // create a blackboard watcher
    this.watcher = 
      new SubscriptionWatcher() {
        @Override
      public void signalNotify(int event) {
          // gets called frequently as the blackboard objects change
          super.signalNotify(event);
          requestCycle();
        }
        @Override
      public String toString() {
          return "ThinWatcher("+BlackboardClientComponent.this.toString()+")";
        }
      };

    // create a callback for running this component
    Trigger myTrigger =
      new Trigger() {
        // we must do this within our inner class, since
        //   org.cougaar.util.annotations.Cougaar#getAnnotatedFields()
        // uses reflection to load this class very early, before we have a
        // chance to finalize our system properties in
        //   org.cougaar.core.node.SetPropertiesComponent
        //
        // If this is moved to the outer class then Applets complain that they
        // can't access this (unwrapped) system property.
        final boolean SET_THREAD_NAME = 
          SystemProperties.getBoolean(
              "org.cougaar.core.blackboard.client.setThreadName",
              true);

        String compName = null;
        private boolean didPrecycle = false;
        // no need to "sync" when using "SyncTriggerModel"
        public void trigger() {
          Thread currentThread = Thread.currentThread();
          String savedName = null;
          if (SET_THREAD_NAME) {
            savedName = currentThread.getName();
            if (compName == null) compName = getBlackboardClientName();
            currentThread.setName(compName);
          }
          awakened = watcher.clearSignal();
          try {
            if (didPrecycle) {
              cycle();
            } else {
              didPrecycle = true;
              precycle();
            }
          } finally {
            awakened = false;
            if (SET_THREAD_NAME) {
              currentThread.setName(savedName);
            }
          }
        }
        @Override
      public String toString() {
          return "Trigger("+BlackboardClientComponent.this.toString()+")";
        }
      };

    // create the trigger model
    this.tm = new SyncTriggerModelImpl(scheduler, myTrigger);

    // activate the blackboard watcher
    blackboard.registerInterest(watcher);

    // activate the trigger model
    //
    // note that this jumps the gun a bit, since we'll request our
    // precycle before starting, but that's the behavior the plugins
    // currently expect.
    tm.initialize();
    tm.load();
  }

   @Override
   public void start() {
      BindingUtility.setUnboundServices(this, getServiceBroker());

      // super will change the run-state from LOADED to ACTIVE
      super.start();
      tm.start();
   }

   // Tell the scheduler to run me at least this once
   public void startSubscriptions() {
      requestCycle();
   }

@Override
public void suspend() {
    super.suspend();
    tm.suspend();
  }

  @Override
public void resume() {
    super.resume();
    tm.resume();
  }

  @Override
public void stop() {
    super.stop();
    tm.stop();
  }

  @Override
public void halt() {
    super.halt();
    tm.halt();
  }
  
  @Override
public void unload() {
    super.unload();
    if (tm != null) {
      tm.unload();
      tm = null;
    }
    blackboard.unregisterInterest(watcher);
    BindingUtility.releaseAnnotatedServices(this, getServiceBroker());
    if (alarmService != null) {
      sb.releaseService(this, AlarmService.class, alarmService);
      alarmService = null;
    }
    if (blackboard != null) {
      sb.releaseService(this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (scheduler != null) {
      sb.releaseService(this, SchedulerService.class, scheduler);
      scheduler = null;
    }
  }

  //
  // implement basic "callback" actions
  //

  protected void precycle() {
    try {
      blackboard.openTransaction();
      setupSubscriptions();
    
      // run execute here so subscriptions don't miss out on the first
      // batch in their subscription addedLists
      execute();                // MIK: I don't like this!!!
    } catch (Throwable t) {
      if (ss != null) ss.die(agentId, t);
    } finally {
      blackboard.closeTransaction();
    }
  }      
  
  protected void cycle() {
    // do stuff
    try {
      blackboard.openTransaction();
      if (shouldExecute()) {
        execute();
      }
    } catch (Throwable t) {
      if (ss != null) ss.die(agentId, t);
    } finally {
      blackboard.closeTransaction();
    }
  }
  
  protected boolean shouldExecute() {
    return (wasAwakened() || blackboard.haveCollectionsChanged());
  }

  /**
   * Get the local agent's address.
   *
   * Also consider adding a "getMessageAddress()" method backed
   * by the NodeIdentificationService.
   */
  protected MessageAddress getAgentIdentifier() {
    return agentId;
  }

  /** @deprecated Use getAgentIdentifier() */
  protected MessageAddress getMessageAddress() {
    return getAgentIdentifier();
  }

  /**
   * Called once after initialization, as a pre-{@link #execute}.
   */
  protected abstract void setupSubscriptions();
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected abstract void execute();
  
  /** storage for wasAwakened - only valid during cycle(). */
  private boolean awakened = false;

  /**
   * True IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only within the scope of the cycle() method.
   */
  protected final boolean wasAwakened() { return awakened; }

  //
  // oddball methods required by BlackboardClient:
  //

  // for BlackboardClient use
  // DO NOT SYNCHRONIZE THIS METHOD; a deadlock will result.
  // It doesn't matter if two threads compute the name.
  public String getBlackboardClientName() {
    if (blackboardClientName == null) {
      setBlackboardClientName(computeBlackboardClientName(parameter));
    }
    return blackboardClientName;
  }

  private void setBlackboardClientName(String newName) {
    blackboardClientName = newName;
  }

  private String computeBlackboardClientName(Object parameter) {
    StringBuffer buf = new StringBuffer();
    buf.append(getClass().getName());
    if (parameter instanceof Collection) {
      buf.append("[");
      String sep = "";
      for (Iterator params = ((Collection) parameter).iterator(); params.hasNext(); ) {
        buf.append(sep);
        buf.append(params.next().toString());
        sep = ",";
      }
      buf.append("]");
    }
    return buf.substring(0);
  }
  
  public long currentTimeMillis() {
    if (alarmService != null)
      return alarmService.currentTimeMillis();
    else
      return System.currentTimeMillis();
  }
  
  @Override
public String toString() {
    return getBlackboardClientName();
  }
}
