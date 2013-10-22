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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;

/**
 * Interface that describes the methods an object needs to be
 * transfered from one agent to another using the Transferable Logic
 * Providers
 **/
public interface Transferable extends Cloneable, UniqueObject {
  /** A Transferable must be fully cloneable, otherwise unwanted side effects
   * may show up when object replicas are on agents in the same VM
   **/
  Object clone();

  /** 
   * A "close enough" version of equals() used by the Logic Provider
   * to find the local version of an object transfered from another agent
   **/
  boolean same(Transferable other);

  /**
   * Set all relevent parameters to the values in other.
   * Almost a deep copy.
   * @param other - must be of same type as this
   **/
  void setAll(Transferable other);

  boolean isFrom(MessageAddress src);

  /**
   * @see #isFrom
   */
  MessageAddress getSource();
}
