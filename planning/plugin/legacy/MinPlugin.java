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

package org.cougaar.planning.plugin.legacy;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.SyncTriggerModelImpl;
import org.cougaar.util.Trigger;
import org.cougaar.util.TriggerModel;

/**
 * A minimal Plugin that illustrates use of the scheduler 
 * service.
 */
public abstract class MinPlugin
extends GenericStateModelAdapter
implements PluginBase
{
  
  protected Object parameter;

  protected BindingSite bindingSite;
  protected ServiceBroker serviceBroker;

  protected SchedulerService scheduler;
  protected TriggerModel tm;
  
  public MinPlugin() { 
  }
  
  /**
   * Called just after construction (via introspection) by the 
   * loader if a non-null parameter Object was specified by
   * the ComponentDescription.
   **/
  public void setParameter(Object param) {
    this.parameter = param;
  }
  
  /**
   * Binding site is set by reflection at creation-time.
   */
  public void setBindingSite(BindingSite bs) {
    this.bindingSite = bs;
    this.serviceBroker = bs.getServiceBroker();
  }

  //
  // implement GenericStateModel:
  //

  public void initialize() {
    super.initialize();
  }

  public void load() {
    super.load();
    
    // obtain the scheduler service
    scheduler = (SchedulerService)
      serviceBroker.getService(
          this,
          SchedulerService.class,
          null);
    if (scheduler == null) {
      throw new RuntimeException(
          this+" unable to obtain scheduler");
    }

    // create a callback for running this component
    Trigger myTrigger = 
      new Trigger() {
        public void trigger() {
          cycle();
        }
        public String toString() {
          return MinPlugin.this.toString();
        }
      };

    // create the trigger model
    this.tm = new SyncTriggerModelImpl(scheduler, myTrigger);

    // activate the trigger model
    tm.initialize();
    tm.load();
  }

  public void start() {
    super.start();
    tm.start();
    // run "cycle()" at least once
    tm.trigger();
  }

  public void suspend() {
    super.suspend();
    tm.suspend();
  }

  public void resume() {
    super.resume();
    tm.resume();
  }

  public void stop() {
    super.stop();
    tm.stop();
  }

  public void halt() {
    super.halt();
    tm.halt();
  }
  
  public void unload() {
    super.unload();
    if (tm != null) {
      tm.unload();
      tm = null;
    }
    if (scheduler != null) {
      serviceBroker.releaseService(
          this, SchedulerService.class, scheduler);
      scheduler = null;
    }
  }

  /**
   * "cycle()" is called by the scheduler thread to perform
   * MinPlugin activity.
   * <p>
   * The first "cycle()" is requested by "start()".  Later cycles 
   * can be requested by using "tm.trigger()", from either within
   * "cycle()" itself or from another class (e.g. a blackboard 
   * watcher).
   * <p>
   * Typically the first "cycle()" is used as an initialization
   * step, so a "boolean didPrecycle" is kept in the subclass.
   * <p>
   * This method should not be synchronized, since it is only
   * called by the scheduler's (synchronized) trigger model.
   */
  protected abstract void cycle();

}
