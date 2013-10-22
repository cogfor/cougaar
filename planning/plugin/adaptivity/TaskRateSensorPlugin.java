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

package org.cougaar.planning.plugin.adaptivity;

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.SensorCondition;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.ConditionService;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin to sense incoming task rate and publish a Condition
 **/
public class TaskRateSensorPlugin extends ServiceUserPlugin {
  private static final String CONDITION_NAME = "TaskRateSensorPlugin.TASKRATE";

  private static final OMCRangeList TASKRATE_VALUES = new OMCRangeList(
      new Double(0.0), new Double(Double.MAX_VALUE));

  private static final double TIME_CONSTANT = 5000.0; // Five second time constant

  private ConditionService conditionService;

  private double filteredTaskRate = 0.0;
  private long then = System.currentTimeMillis();
  private IncrementalSubscription tasksSubscription;
  private UnaryPredicate tasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        return true;
      }
      return false;
    }
  };

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class TaskRateTestCondition extends SensorCondition implements NotPersistable {
    public TaskRateTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public TaskRateSensorPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    TaskRateTestCondition taskRate =
      new TaskRateTestCondition(CONDITION_NAME, TASKRATE_VALUES, new Double(0.0));
    blackboard.publishAdd(taskRate);
    tasksSubscription = (IncrementalSubscription) blackboard.subscribe(tasksPredicate);
    if (haveServices()) updateTaskRateSensor(true);
  }

  /**
   * Test if all needed services have been acquired. Test the
   * conditionService variable for null. If still null ask
   * acquireServices to continue trying to acquire services. If true
   * is returned, fill in the service variables and return true.
   * Subsequent calls will return true immediately.
   **/
  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      conditionService = (ConditionService)
        sb.getService(this, ConditionService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (haveServices()) {
      updateTaskRateSensor(timerExpired());
    }
  }

  private void updateTaskRateSensor(boolean publish) {
    long now = System.currentTimeMillis();
    long elapsed = now - then;
    int newCount = tasksSubscription.getAddedCollection().size()
      + tasksSubscription.getChangedCollection().size()
      + tasksSubscription.getRemovedCollection().size();
    then = now;
    filteredTaskRate /= Math.exp(elapsed / TIME_CONSTANT);
    filteredTaskRate += newCount;
    if (publish) {
      cancelTimer();
      if (logger.isDebugEnabled()) logger.debug("newCount=" + newCount);
      TaskRateTestCondition taskRate = (TaskRateTestCondition)
        conditionService.getConditionByName(CONDITION_NAME);
      if (taskRate != null) {
        if (logger.isInfoEnabled()) logger.info("Setting " + CONDITION_NAME + " = " + filteredTaskRate);
        taskRate.setValue(new Double(filteredTaskRate));
        blackboard.publishChange(taskRate);
      }
      resetTimer(2000);
    }
  }
}
