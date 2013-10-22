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
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.ConditionService;

public class MetricsTestPlugin extends ServiceUserPlugin {
  public static final String JIPS_CONDITION_NAME = "MetricsTestPlugin.JIPS";

  private static final OMCRange[] JIPS_RANGES = {
    new OMCRange(0.0, Double.MAX_VALUE)
  };

  private static final OMCRangeList JIPS_VALUES = new OMCRangeList(JIPS_RANGES);

  private ConditionService conditionService;

  private MetricsService metricsService;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class MetricsTestCondition extends SensorCondition implements NotPersistable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public MetricsTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    @Override
   public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class,
    MetricsService.class
  };

  public MetricsTestPlugin() {
    super(requiredServices);
  }

  @Override
public void setupSubscriptions() {
    MetricsTestCondition jips =
      new MetricsTestCondition(JIPS_CONDITION_NAME, JIPS_VALUES, new Double(1.0));
    getBlackboardService().publishAdd(jips);
    if (haveServices()) setMetricsConditions();
  }

  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      conditionService = sb.getService(this, ConditionService.class, null);
      metricsService = sb.getService(this, MetricsService.class, null);
      return true;
    }
    return false;
  }

  @Override
public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setMetricsConditions();
      }
    }
  }

  private void setMetricsConditions() {
    logger.info("setMetricsConditions");
    // raw CPU capacity
    Metric metric =  metricsService.getValue("Agent(Provider):Jips");
    // Includes effects of load average, but different units
    // Metric metric =  svc.getValue("Agent(3ID):EffectiveMJips");
    MetricsTestCondition jips = (MetricsTestCondition)
      conditionService.getConditionByName(JIPS_CONDITION_NAME);
    if (metric != null) {
      if (jips != null) {
        Double value = new Double(metric.doubleValue());
        if (logger.isInfoEnabled()) logger.info("Setting jips = " + value);
        jips.setValue(value);
        getBlackboardService().publishChange(jips);
      } else {
        logger.warn("jips is null");
      }
    } else {
      logger.warn("metric is null");
    }
    resetTimer(10000);
  }
}
