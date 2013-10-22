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
/** New interface as part of new Constraint API
 * containing setValue method to adjust aspect value
 * of constrained task
 **/

package org.cougaar.planning.ldm.plan;

public interface SettableConstraintEvent
	extends ConstraintEvent
{
  /**
   * Sets (preferences for) the aspect value needed to satisfy the
   * constraint placed on the task of this event.
   * @param value the constraining value
   * @param constraintOrder specifies whether the constrained value
   * must be BEFORE (LESSTHAN), COINCIDENT (EQUALTO), or AFTER
   * (GREATERTHAN) the constraining value. The score function of the
   * preference is selected to achieve the constraint.
   * @param slope specifies the rate at which the score function
   * degrades on the allowed side of the constraint. The disallowed
   * side always has a failing score.
   **/
  void setValue(double value, int constraintOrder, double slope);
  /**
   * Sets (preferences for) the aspect value needed to satisfy the
   * constraint placed on the task of this event.
   * @param value the constraining value
   **/
  void setValue(double value);
}
