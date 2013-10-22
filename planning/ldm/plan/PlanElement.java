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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.ClaimableHolder;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UniqueObject;

/**
 * PlanElements are the primitive building blocks from which 
 * planning models are constructed. A single PlanElement represents a 
 * cycle of work completed against a Task. A PlanElement is of 
 * type Expansion (represented by a Workflow and the implied tasks 
 * embodied in it), Allocation (represented by an Asset),
 *  Aggregation (represented by an MPWorkflow) or AssetTransfer. 
 **/

public interface PlanElement 
  extends ScheduleElement, UniqueObject, Annotatable, ClaimableHolder, Publishable
{
	
  /** @return Plan the Plan of this plan element.
   **/
  Plan getPlan();

  /** This returns the Task of the PlanElement. 
   * @return Task
   **/
  
  Task getTask();
  
  /** Returns the estimated allocation result that is related to performing
   * the Task.
   * @return AllocationResult
   **/

   AllocationResult getEstimatedResult();

  /**
   * Returns the reported allocation result. The reported result is
   * computed from the received and observed results
   * @return AllocationResult
   **/
   AllocationResult getReportedResult();

  /** Returns the received allocation result.
   * @return AllocationResult
   **/
   AllocationResult getReceivedResult();

  /**
   * Returns the observed allocation result.
   * @return AllocationResult
   **/
   AllocationResult getObservedResult();
   
  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   **/
  void setEstimatedResult(AllocationResult estimatedresult);
   
  /**
   * Set the observed allocation result that be incorporated into the
   * reported result
   * @param observedResult
   **/
  void setObservedResult(AllocationResult observedResult);
  
  interface PlanElementChangeReport extends ChangeReport {
  }

  abstract class ResultChangeReport implements PlanElementChangeReport {
    private int type;
    public final static int UNDEFINED_TYPE = AspectType.UNDEFINED;
    private double oldValue;
    public final static double UNDEFINED_VALUE = Double.MIN_VALUE;

    public ResultChangeReport() {
      type = UNDEFINED_TYPE;
      oldValue = UNDEFINED_VALUE;
    }
    public ResultChangeReport(int t) {
      type=t;
      oldValue = UNDEFINED_VALUE;
    }
    public ResultChangeReport(int t, double o) {
      type=t; oldValue=o;
    }
    /** May return AspectType.UNDEFINED if the aspect type id is unknown **/
    public int getAspectType() { return type; }
    /** old value if known.  If unknown (e.g. no previous value)
     * will return Double.MIN_VALUE.
     **/
    public double getOldValue() { return oldValue; }

    public int hashCode() { return getClass().hashCode()+type; }
    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         ((ResultChangeReport)o).type == type);
    }
    public String toString() {
      if (type == UNDEFINED_TYPE) {
        return " (?)";
      } else {
        return " ("+type+")";
      }
    }
  }
  // change reports

  /** Something in the Estimated result changed. **/
  class EstimatedResultChangeReport extends ResultChangeReport {
    public EstimatedResultChangeReport() { super(); }
    public EstimatedResultChangeReport(int t) { super(t); }
    public EstimatedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "EstimatedResultChangeReport"+super.toString();
    }
  }
  /** Something in the reported result changed. **/
  class ReportedResultChangeReport extends ResultChangeReport {
    public ReportedResultChangeReport() { super(); }
    public ReportedResultChangeReport(int t) { super(t); }
    public ReportedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "ReportedResultChangeReport"+super.toString();
    }
  }
  /** Something in the observed result changed. **/
  class ObservedResultChangeReport extends ResultChangeReport {
    public ObservedResultChangeReport() { super(); }
    public ObservedResultChangeReport(int t) { super(t); }
    public ObservedResultChangeReport(int t, double ov) { super(t,ov); }
    public String toString() {
      return "ObservedResultChangeReport"+super.toString();
    }
  }
}



