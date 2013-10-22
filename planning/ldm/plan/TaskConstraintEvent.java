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
/* Implementation of SettableConstraintEvent. New
   API to be used as utility functions for resolving
   constraints
*/

package org.cougaar.planning.ldm.plan;


public abstract class TaskConstraintEvent implements SettableConstraintEvent
{
  public static class Constrained extends TaskConstraintEvent {
    public Constrained(NewTask t, int aspect) {
      super(t, aspect, NOVALUE);
    }

    public boolean isConstraining() {
      return false;
    }
  }

  public static class Constraining extends TaskConstraintEvent {
    public Constraining(NewTask t, int aspect) {
      super(t, aspect, NOVALUE);
    }

    public boolean isConstraining() {
      return true;
    }
  }

  private NewTask task; /* constrained or constraining task */
  private int event; /* constrained or constraining aspect */
  private double value = NOVALUE; /* A value to use if a constraining task has NOVALUE */

  protected TaskConstraintEvent(NewTask t, int aspect, double value) {
    task = t;
    event = aspect;
    this.value = value;
  }

  public int getAspectType() {
    return event;
  }

  public Task getTask() {
    return task;
  }

  public abstract boolean isConstraining();

  /**
   * getValue()
   * @return the allocation result of particular aspect if
   * isConstraining else returns the preference on that aspect.
   * @note The value may be Double.NaN if the task has an undefined preferred value.
   **/
  public double getValue() {
    if (isConstraining()) {
      return getResultValue();
    } else {
      return task.getPreferredValue(event);
    }
  }

  /**
   * getResultValue()
   * @return the allocation result of particular aspect
   **/
  public double getResultValue() {
    PlanElement pe = task.getPlanElement();
    if (pe == null) return value;
    AllocationResult ar = pe.getEstimatedResult();
    if (ar == null) return value;
    return ar.getValue(event);
  }

  /**
   * setValue sets the aspect value. To be used on a Constrained task
   * to set the aspect value to a value that will satisfy constraints
   **/
  public void setValue(double value, int constraintOrder, double slope) {
    this.value = value;  // local copy
    if (isConstraining()) {
      throw new RuntimeException("setValue only applies to a constrained event");
    }
    // set preference on the constrained task
    ScoringFunction sf;
    switch (constraintOrder) {
    case Constraint.BEFORE: // Same as LESSTHAN
      sf = ScoringFunction.createNearOrBelow(AspectValue.newAspectValue(event, value), slope);
      break;
    case Constraint.AFTER: // Same as GREATERTHAN
      sf = ScoringFunction.createNearOrAbove(AspectValue.newAspectValue(event, value), slope);
      break;
    case Constraint.COINCIDENT: // Same as EQUALTO
      sf = ScoringFunction.createStrictlyAtValue(AspectValue.newAspectValue(event, value));
      break;
    default:
      return;
    }
    Preference pref = new PreferenceImpl(event, sf);
    task.setPreference(pref);
  }
  /**
   * setValue sets the aspect value. To be used on a Constrained task
   * to set the aspect value to a value that will satisfy constraints
   **/
  public void setValue(double value) {
    setValue(value, Constraint.COINCIDENT, 0.0);
  }

}
