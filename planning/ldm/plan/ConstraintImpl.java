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

// import org.cougaar.planning.ldm.plan.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Constraint implementation
 * A Constraint is part of a Workflow.
 * Constraints provide pair-wise precedence
 * relationship information about the Tasks
 * contained in the Workflow.  Each Task can have
 * more than one applicable Constraint.
 **/
	
public class ConstraintImpl
  implements Constraint, NewConstraint, Cloneable, Serializable
{

  private int theConstraintOrder;
  private double theConstraintOffset = 0.0;
  private int theConstrainingAspect, theConstrainedAspect;
  private double theConstrainingValue = 0.0;
  private boolean isAbsolute;
  private Task theConstrainingTask, theConstrainedTask;

  private static final double EPSILON = 1.0; // Maybe this should be 0.0
	
  /**
   * ConstraintEvent Objects for constraining and constrained Tasks.
   * These are created the first time they are needed and are
   * redundant w.r.t. theConstrainedTask, theConstrainedAspect,
   * theConstrainingTask, and theConstrainingAspect
   **/
  private transient ConstraintEvent theConstrainingEvent = null;
  private transient ConstraintEvent theConstrainedEvent = null;

  /** no-arg constructor*/
  public ConstraintImpl() {
    super();
  }

  /** Constructor for new constraint API **/
  public ConstraintImpl(Task aConstrainedTask, int aConstrainedAspect,
                        Task aConstrainingTask, int aConstrainingAspect,
                        double anOffset, int anOrder){
    theConstraintOrder = anOrder;
    theConstraintOffset = anOffset;
    theConstrainingAspect = aConstrainingAspect;
    theConstrainingTask = aConstrainingTask;
    theConstrainedAspect = aConstrainedAspect;
    theConstrainedTask = aConstrainedTask;
    isAbsolute = false;
  }

  /**
   * Create a constraint against an absolute value on a task
   **/
  public ConstraintImpl(Task aConstrainedTask, int aConstrainedAspect,
                        double aConstrainingValue, int anOrder) {
    theConstraintOrder = anOrder;
    theConstrainingValue = aConstrainingValue;
    theConstrainedTask = aConstrainedTask;
    theConstrainedAspect = aConstrainedAspect;
    theConstrainingAspect = theConstrainedAspect; // In case anyone asks
    isAbsolute = true;
  }

  public void setConstrainingTask(Task task) {
    if (task == null) {
      if (theConstrainingTask == null) throw new RuntimeException("Constraining task already null");
    } else {
      if (isAbsolute) throw new RuntimeException("Constraint is absolute");
      if (theConstrainingTask != null)  throw new RuntimeException("Constraining task already set");
    }
    theConstrainingTask = task;
  }

  public void setConstrainingAspect(int aspect) {
    theConstrainingAspect = aspect;
  }

  /**
   * <PRE> Task mytask = myconstraint.getConstrainingTask(); </PRE>
   * @return Task  Returns the Task which is constraining another event or Task.
   **/
		
  public Task getConstrainingTask() {
    return theConstrainingTask;
  }

  /** returns the aspect type of the constraining task
   **/
  public int getConstrainingAspect() {
    return theConstrainingAspect;
  }

  /** Return the ConstraintEvent object for the constraining task **/
  public ConstraintEvent getConstrainingEventObject()
  {
    if (theConstrainingEvent == null) {
      if (isAbsolute) {
        theConstrainingEvent =
          new AbsoluteConstraintEvent(theConstrainingAspect,
                                      theConstrainingValue);
      } else {
        if (theConstrainingTask == null) {
          throw new RuntimeException("The constraining task is not set");
        }
        theConstrainingEvent =
          new TaskConstraintEvent.Constraining((NewTask) theConstrainingTask,
                                               theConstrainingAspect);
      }
    }
    return theConstrainingEvent;
  }

  public void setConstrainedTask(Task task) {
    if (task == null) {
      if (theConstrainedTask == null)  throw new RuntimeException("Constrained task already set");
    } else {
      if (theConstrainedTask != null)  throw new RuntimeException("Constrained task already set");
    }
    theConstrainedTask = task;
  }

  public void setConstrainedAspect(int aspect) {
    theConstrainedAspect = aspect;
  }

  /**
   * <PRE> Task mytask = myconstraint.getConstrainedTask(); </PRE>
   * @return Task  Returns a Task which is constrained by another event or Task.
   **/
	
  public Task getConstrainedTask() {
    if (theConstrainedTask == null) throw new RuntimeException("Constrained task is null");
    return theConstrainedTask;
  }
	
  /** returns the aspect type of the constrained task
   **/

  public int getConstrainedAspect() {
    return theConstrainedAspect;
  }

  /** Return the ConstraintEvent object for the constraining task **/

  public ConstraintEvent getConstrainedEventObject()
  {
    if (theConstrainedEvent == null) {
      if (theConstrainedTask == null) {
        throw new RuntimeException("Constrained task is null");
      } else {
        theConstrainedEvent =
          new TaskConstraintEvent.Constrained((NewTask) theConstrainedTask,
                                              theConstrainedAspect);
      }
    }
    return theConstrainedEvent;
  }
	
  /**
   * Returns an int which represents the
   * order of the Constraint.
   * <PRE> int myorder = myconstraint.getConstraintOrder(); </PRE>
   * @return int  The int value
   * will be equal to "0" (COINCIDENT), "-1" (BEFORE) or "1" (AFTER).
   * There are also order analogues for constraints on non-temporal aspects.
   * These are "1" (GREATERTHAN), "-1" (LESSTHAN) or "0" (EQUALTO).
   **/
		
  public int getConstraintOrder() {
    return theConstraintOrder;
  }

  /**
   * Returns a double which represents the offset
   * of the Constraint.
   * @return the value to be added to the constraining value before
   * comparing to the constrained value.
   **/
		
  public double getOffsetOfConstraint() {
    return theConstraintOffset;
  }

  public void setAbsoluteConstrainingValue(double value) {
    if (theConstrainingTask != null) {
      throw new RuntimeException("Constraining task has already been set");
    }
    theConstrainingValue = value;
    isAbsolute = true;
  }
 	
  /** setConstraintOrder allows you to set the order.
   * The order should be COINCIDENT, BEFORE, or AFTER.
   * <PRE> mynewconstraint.setConstraintOrder(BEFORE); </PRE>
   * @param order Should be COINCIDENT, BEFORE or AFTER only.
   **/
 		
  public void setConstraintOrder(int order) {
    theConstraintOrder = order;
  }
 	
  /** setOffsetofConstraint allows you to set the time
   * offset of the Constraint.  If it is + than the offset
   * is in the future, if it is - than the offset is in the
   * past.
   * <PRE> mynewconstraint.setOffsetofConstraint(-2000); </PRE>
   * @param offset  - offset of constraint
   **/
 		
  public void setOffsetOfConstraint(double offset) {
    theConstraintOffset = offset;
  }

  /** Computes a value from constraining, offset, and order to
   * satisfy constraint
   * Note that the current implementation only computes for temporal
   * constraint aspects.
   **/

  public double computeValidConstrainedValue() {
    double constVal =
      getConstrainingEventObject().getValue() + getOffsetOfConstraint();
    switch (getConstraintOrder()) {
    case Constraint.BEFORE:     // Same as LESSTHAN
      return constVal + EPSILON;
    case Constraint.COINCIDENT: // Same as EQUALTO
      return constVal;
    case Constraint.AFTER:
      return constVal - EPSILON;
    }
    return constVal;
  }

  public ConstraintImpl copy() {
    try {
      return (ConstraintImpl) clone();
    } catch (CloneNotSupportedException cnse) {
      return null;
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
  }


}
