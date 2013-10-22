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

package org.cougaar.core.blackboard;


/**
 * An object "claimed" by a single actor, typically a plugin
 * instance.
 * <p>
 * For example, a Claimable object would likely be "claimed" by a
 * plugin when that plugin creates the object or the object is
 * added into the blackboard.
 * <p>
 * Claiming of objects is done by the infrastruture *only* - plugins
 * should *never* call claim().
 */
public interface Claimable 
{
  /** @return true IFF this object been claimed. */
  boolean isClaimed();

  /** @return the current claim holder, or null if there is none. */
  Object getClaim();

  /**
   * Stake a Claim on the object.
   * @exception IllegalArgumentException If there is already a Claim
   * on the object which is not == the putativeClaimHolder.
   */
  void setClaim(Object putativeClaimHolder);

  /**
   * Try to stake a Claim on the object.
   * @return true IFF success.
   */
  boolean tryClaim(Object putativeClaimHolder);

  /**
   * Release a Claim on the object.
   * @exception IllegalArgumentExcpeiton If the object is not
   * currently claimed, or is claimed by someone else.
   */
  void resetClaim(Object oldClaimHolder);
}
