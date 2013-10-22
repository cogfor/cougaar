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

/**
 * A Blackboard object that can be deleted.
 * <p>
 * Deletion occurs when a blackboard object is no longer functionally
 * significant subject to a DeletionPolicy that may extend the
 * interval before an object is actually removed to support "best
 * effort" retention of historical data.  Actual deletion is performed
 * by deletion plugins according to policies. Not
 * all deletable blackboard objects implement this interface. It is only
 * implemented for simple deletable objects for which the "is it time to
 * delete" decision is uncomplicated.
 */
public interface Deletable {
  /**
   * Some Deletable objects are not actually deletable.
   * This method lets such
   * objects declare whether they are deletable or not.
   * @return true if the object with this interface can be deleted.
   */
  boolean isDeletable();
  
  /**
   * Indicates that this object has been deleted and that remove events should
   * be interpreted as deletion rather than rescind.
   * @return true if this object has been deleted.
   */
  boolean isDeleted();
  
  /**
   * Set the status of this object to "deleted" (isDeleted should return true).
   * There is no argument because there is no provision for clearing the
   * deleted status of an object.
   */
  void setDeleted();
  
  /**
   * Get the deletion time of this object. The deletion time, in conjunction
   * with the applicable DeletionPolicy determines the earliest time when an
   * object can be safely deleted. This time should be as early as possible
   * consistent with correct operation. A DeletionPolicy should be used to
   * extend the time an object remains on the blackboard for historical reasons.
   * @return The time (scenario time) in milliseconds at which this object can
   * be safely deleted.
   */
  long getDeletionTime();
  
  /**
   * Specifies that the time returned by getDeletion() is based on the value of
   * System.currentTimeMillis() rather than scenario time.
   * @return whether it uses scenario or system time
   */
  boolean useSystemTime();
}
