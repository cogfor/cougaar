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

/**
  * Constraint Interface
  * A Constraint is part of a Workflow.
  * Constraints provide pair-wise precedence
  * relationship information about the Tasks
  * contained in the Workflow.  A Task can have
  * more than one applicable Constraint.
  **/
public interface Constraint
{
  int COINCIDENT = 0;
  int BEFORE = -1;
  int AFTER = 1;
  int GREATERTHAN = 1;
  int LESSTHAN = -1;
  int EQUALTO = 0;

  /** 
   * <PRE> Task mytask = myconstraint.getConstrainingTask(); </PRE>
   * @return Task  Returns the Task which is constraining another event or Task.
   **/
  Task getConstrainingTask();

  /** 
   * <PRE> Task mytask = myconstraint.getConstrainedTask(); </PRE>
   * @return Task  Returns a Task which is constrained by another event or Task.
   **/
  Task getConstrainedTask();
	
  /** 
   * Returns an int which represents the
   * order of the Constraint.  
   * <PRE> int myorder = myconstraint.getConstraintOrder(); </PRE>
   * @return int  The int value
   * will be equal to "0" (COINCIDENT), "-1" (BEFORE) or "1" (AFTER).
   * There are also order analogues for constraints on non-temporal aspects.
   * These are "1" (GREATERTHAN), "-1" (LESSTHAN) or "0" (EQUALTO).
   **/
  int getConstraintOrder();

  /**
   * Returns the aspect type of the constraint for the constraining task
   * For non temporal constraints, constraining aspect and constrained aspect
   * will be the same. For temporal constraints, they can be different.
   * Eg (START_TIME and END_TIME)
   **/
  int getConstrainingAspect();

   /**
   * Returns the aspect type of the constraint for the constrained task
   * For non temporal constraints, constraining aspect and constrained aspect
   * will be the same. For temporal constraints, they can be different.
   * Eg (START_TIME and END_TIME)
   **/
  int getConstrainedAspect();

  /** Return the ConstraintEvent object for the constraining task
   */
  ConstraintEvent getConstrainingEventObject();
  
  /** Return the ConstraintEvent object for the constrained task
   */
  ConstraintEvent getConstrainedEventObject();

  /** 
   * Returns a double which represents the offset
   * of the Constraint. 
   * @return the value to be added to the constraining value before
   * comparing to the constrained value.
   **/
  double getOffsetOfConstraint();

  /* Calculate a value from constraining event, offset and order
   * to alleviate constraint violation on constrained event
   * Note that the current implementation only computes for 
   * temporal constraint aspects.
   */
  double computeValidConstrainedValue();
	
}
		
		
