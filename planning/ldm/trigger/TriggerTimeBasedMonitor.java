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
 * A TriggerTimeBasedMonitor is a kind of monitor that generates an
 * interrupt at regular intervals to check for a particular
 * condition on a fixed set of objects
 *
 * Uses system time
 */

public class TriggerTimeBasedMonitor implements TriggerMonitor {
  
  private Object[] my_objects;
  long my_last_ran;
  long my_msec_interval;

  public TriggerTimeBasedMonitor(long msec_interval, Object[] objects, PluginDelegate pid) 
  {
    my_objects = objects;
    my_msec_interval = msec_interval;
    my_last_ran = System.currentTimeMillis();
  }
  
  public long getMsecInterval() {
    return my_msec_interval;
  }

  public Object[] getAssociatedObjects() {
    return my_objects;
  }

  public boolean ReadyToRun(PluginDelegate pid) { 
    return (System.currentTimeMillis() - my_last_ran) > my_msec_interval;
  }

  public void IndicateRan(PluginDelegate pid) { 
    my_last_ran = System.currentTimeMillis(); 
  }

  public long getRemainingTime() {
    return (my_msec_interval - (System.currentTimeMillis() - my_last_ran));
  }

}


