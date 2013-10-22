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
/* ConstraintEvent objects can be used to denote
 * either constraining or constrained events
 */

package org.cougaar.planning.ldm.plan;

public interface ConstraintEvent
{
  /**
   * This value is used to denote an unknown aspect value in all
   * constraint events.
   **/
  double NOVALUE = Double.NaN;

  /* getValue returns the allocation result of the
   * aspect when the task is constraining or
   * the preferred value of the aspect when the
   * task is constrained. isConstraining is true
   * when task is constraining, false when task is
   * constrained.
   * @return the value of this ConstrainEvent. NOVALUE is returned if
   * the value is not known. For example, the value for a constrained
   * task that has not yet been disposed will be NOVALUE.
   */
  double getValue();

  /* getResultValue returns the allocation result of the
   * aspect without regard to whether the event isConstraining()
   * @return the value of this ConstrainEvent. NOVALUE is returned if
   * the value is not known. For example, the value for a constrained
   * task that has not yet been disposed will be NOVALUE.
   */
  double getResultValue();

  /**
   * The aspect involved in this end of the constraint.
   * @return the aspect type of the preference or allocation result.
   **/
  int getAspectType();

  /**
   * Return the task, if any. AbsoluteConstraintEvents have no task.
   * @return the task. null is returned for absolute constraints.
   **/
  Task getTask();

  /**
   * Tests if this is a constraining (vs. constrained) event.
   **/
  boolean isConstraining();
}
