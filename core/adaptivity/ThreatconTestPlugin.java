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

public class ThreatconTestPlugin extends ServiceUserPlugin {
  public static final String THREATCON_CONDITION_NAME = "ThreatconTestPlugin.THREATCON";

  private static final OMCRange[] THREATCON_RANGES = {
    new OMCPoint("low"),
    new OMCPoint("high")
  };

  private static final OMCRangeList THREATCON_VALUES = new OMCRangeList(THREATCON_RANGES);

  private ConditionService conditionService;

  private static final String[] threatconValues = {"low", "high"};

  private int threatconStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class ThreatconTestCondition extends SensorCondition implements NotPersistable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public ThreatconTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
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

  public ThreatconTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    ThreatconTestCondition threatcon =
      new ThreatconTestCondition(THREATCON_CONDITION_NAME, THREATCON_VALUES, threatconValues[0]);
    getBlackboardService().publishAdd(threatcon);
    if (haveServices()) setThreatconCondition();
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
        setThreatconCondition();
      }
    }
  }

  private void setThreatconCondition() {
    ThreatconTestCondition threatcon = (ThreatconTestCondition)
      conditionService.getConditionByName(THREATCON_CONDITION_NAME);
    if (threatcon != null) {
      if (logger.isInfoEnabled()) logger.info("Setting threatcon = " + threatconValues[threatconStep]);
      threatcon.setValue(threatconValues[threatconStep]);
      getBlackboardService().publishChange(threatcon);
      threatconStep++;
      if (threatconStep == threatconValues.length) threatconStep = 0;
    }
    resetTimer(115000);
  }
}
