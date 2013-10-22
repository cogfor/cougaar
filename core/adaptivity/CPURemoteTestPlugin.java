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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.UIDService;

/**
 * A plugin to exercise the Relay mechanism by setting a
 * RelayedOperatingMode that is targeted to the Provider agent. The
 * target manifestation of the operating mode is a Sensor that is used
 * by the adaptivity engine to select plays.
 **/
public class CPURemoteTestPlugin extends ServiceUserPlugin {
  /** The name of the OperatingMode and Condition **/
  public static final String CPU_CONDITION_NAME = "CPURemoteTestPlugin.CPU";

  /** A range from 0.0 thru 1.0 **/
  private static final OMCRange[] CPU_RANGES = {new OMCRange(0.0, 1.0)};

  /** A value list with just one range from 0.0 thru 1.0 **/
  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  private UIDService uidService;

  private InterAgentCondition cpu;

  private static final Double[] cpuValues = {
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
  };

  private int cpuStep = 0;

  private static final Class[] requiredServices = {
    UIDService.class
  };

  public CPURemoteTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    cpu = new InterAgentCondition(CPU_CONDITION_NAME,
                                      CPU_VALUES, cpuValues[0]);
    cpu.setTarget(MessageAddress.getMessageAddress("Provider"));
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) {
      uidService.registerUniqueObject(cpu);
      setCPUCondition();
    }
  }

  private boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = sb.getService(this, UIDService.class, null);
      return true;
    }
    return false;
  }

  @Override
public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setCPUCondition();
      }
    }
  }

  private void setCPUCondition() {
    if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
    cpu.setValue(cpuValues[cpuStep]);
    getBlackboardService().publishChange(cpu);
    cpuStep++;
    if (cpuStep == cpuValues.length) cpuStep = 0;
    resetTimer(60000);
  }
}
