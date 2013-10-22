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
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.ConditionService;

public class CPUTestPlugin extends ServiceUserPlugin {
  public static final String CPU_CONDITION_NAME = "CPUTestPlugin.CPU";

  private static final OMCRange[] CPU_RANGES = {
    new OMCRange(0.0, 1.0)
  };

  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  private ConditionService conditionService;

  private static final Double[] cpuValues = {
    new Double(0.0),
    new Double(0.1),
    new Double(0.2),
    new Double(0.3),
    new Double(0.4),
    new Double(0.5),
    new Double(0.6),
    new Double(0.7),
    new Double(0.8),
    new Double(0.9),
    new Double(0.8),
    new Double(0.7),
    new Double(0.6),
    new Double(0.7),
    new Double(0.8),
    new Double(0.9),
    new Double(0.8),
    new Double(0.7),
    new Double(0.6),
    new Double(0.5),
    new Double(0.4),
    new Double(0.3),
    new Double(0.2),
    new Double(0.1),
  };

  private int cpuStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class CPUTestCondition extends SensorCondition implements NotPersistable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public CPUTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    @Override
   public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public CPUTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    CPUTestCondition cpu =
      new CPUTestCondition(CPU_CONDITION_NAME, CPU_VALUES, cpuValues[0]);
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) setCPUCondition();
  }

  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      conditionService = sb.getService(this, ConditionService.class, null);
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
    CPUTestCondition cpu = (CPUTestCondition)
      conditionService.getConditionByName(CPU_CONDITION_NAME);
    if (cpu != null) {
      if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
      cpu.setValue(cpuValues[cpuStep]);
      getBlackboardService().publishChange(cpu);
      cpuStep++;
      if (cpuStep == cpuValues.length) cpuStep = 0;
    }
    resetTimer(5000);
  }
}
