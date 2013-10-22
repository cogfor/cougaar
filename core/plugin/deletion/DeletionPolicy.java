/* 
 * <copyright>
 *  
 *  Copyright 2004 BBNT Solutions, LLC
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
package org.cougaar.core.plugin.deletion;

import org.cougaar.util.UnaryPredicate;

/**
 * A policy that controls the timing of the deletion of a blackboard object.
 * A predicate checks for the applicability of the policy to the object. The
 * priority disambiguates the case where multiple policies might apply. The
 * deletionDelay supplies an additional delay before the object is actually
 * deleted
 */
public interface DeletionPolicy {
  /**
   * A value signifying that the policy has no defined delay. If the applicable
   * policy has such a value, deletion does not occur.
   */
  long NO_DELETION_DELAY = Long.MIN_VALUE;
  
  /**
   * A priority guaranteed to be less than any defined priority. Only the
   * default policy has NO_PRIORITY. Real policies should have a minimum of
   * MIN_PRIORITY.
   */
  int NO_PRIORITY = Integer.MIN_VALUE;
  
  /**
   * The minimum allowable priority for any non-default policy.
   */
  int MIN_PRIORITY = Integer.MIN_VALUE + 1;
  
  /**
   * The maximum allowable priority for any policy.
   */
  int MAX_PRIORITY = Integer.MAX_VALUE;
  
  /**
   * Get the name of this policy. The name is arbitrary and only used in user
   * interfaces, if any.
   * @return the name of the policy.
   */
  String getName();
  
  /**
   * Get the predicate for this policy. The predicate should return true when
   * applied to objects to which this policy applies.
   * @return the predicate selecting objects for which this policy applies.
   */
  UnaryPredicate getPredicate();
  
  /**
   * Get the additional delay (in scenario time) that should be inserted before
   * a blackboard object is deleted.
   * @return the number of milliseconds of delay before deletion.
   */
  long getDeletionDelay();
  
  /**
   * Get the priority of this policy over other deletion policies. When multiple
   * policies match an object the highest priority policy is used.
   * @return the priority of this policy.
   */
  int getPriority();
}
