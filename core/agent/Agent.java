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

package org.cougaar.core.agent;

import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** 
 * The abstract agent base class, which is subclassed by {@link
 * AgentImpl}.
 * <p>
 * This is primarily a marker class, since AgentImpl is used for
 * all agent types. 
 */
public abstract class Agent 
extends ContainerSupport
{
  /** The Insertion point for any Agent, defined relative to the AgentManager. */
  public static final String INSERTION_POINT = AgentManager.INSERTION_POINT + ".Agent";

  @Override
public boolean add(Object o) {
    try {
      return super.add(o);
    } catch (RuntimeException re) {
      Logger logger = Logging.getLogger(this.getClass());
      logger.error("Failed to add "+o+" to "+this, re);
      throw re;
    }
  }

  @Override
protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  //backwards compatability for direct pointers to the agent
  public abstract MessageAddress getAgentIdentifier();
}
