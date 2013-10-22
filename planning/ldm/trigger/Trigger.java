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

package org.cougaar.planning.ldm.trigger;

import org.cougaar.planning.plugin.legacy.PluginDelegate;

/**
 * A Trigger is an object containing information indicating an action
 * to be taken if a particular state exists among a specified set of objects.
 * The trigger contains three pieces:
 *    MONITOR - Establishes the set of objects on which to test for the state
 *    TESTER - Determines if the state exists
 *    ACTION - Performs an action on the set of objects (or other class
 *                        state info)
 *
 * The Trigger contains an Execute method which captures the logic 
 * of executing trigger actions when the monitored state exists.
 *
 */

public class Trigger implements java.io.Serializable {
  
  private TriggerMonitor my_monitor;
  private TriggerTester my_tester;
  private TriggerAction my_action;

  /** Basic Constructor.
    * @param monitor
    * @param tester
    * @param action
    */
  public Trigger(TriggerMonitor monitor, TriggerTester tester, TriggerAction action) {
    my_monitor = monitor;
    my_tester = tester;
    my_action = action;
  }

  /**
   * Is this trigger fully filled in, and if so, is the monitor ready to run?
   */
  public boolean ReadyToRun(PluginDelegate pid) { 
    // note don't worry if the tester is null, we could have a monitor and an action.
    if ( (my_monitor != null) &&  (my_action != null) 
      && (my_monitor.ReadyToRun(pid)) ) {
      return true;
    } else {
      return false;
    }
  }
  
  /** @return The monitor associated with this Trigger. */
  public TriggerMonitor getMonitor() {
    return my_monitor;
  }


  /**
   * Run the trigger : if the condition exists on the objects, fire the action
   */
  public void Execute(PluginDelegate pid) {
    Object[] objects = my_monitor.getAssociatedObjects();
    if (my_tester != null) {
      if (my_tester.Test(objects)) {
        my_action.Perform(objects, pid);
      }
    } else {
      // if we don't have a tester go straight to the action
      // but make sure that objects is not empty since the monitor objects
      // returned end up being our tester
      if (objects.length > 0 ) {
        my_action.Perform(objects, pid);
      }
    }
    my_monitor.IndicateRan(pid);
  }
  
  
}






