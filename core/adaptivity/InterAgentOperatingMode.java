/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * A remotely-controlled Condition. Allows an adaptivity engine in one
 * agent to control a Condition of the adaptivity engine in another
 * agent. It is instantiated in the controlling agent and transferred
 * to the controlled agent using the Relay logic providers. A copy
 * of the instance is published in the controlled agent's blackboard
 * and used like any other Condition.
 **/
public class InterAgentOperatingMode
  extends OperatingModeImpl
  implements Relay.Source 
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// this can't be transient like other relays, cause not storing
  // target separately
  private Set targets = Collections.EMPTY_SET;
  private UID uid;

  // Constructors
  public InterAgentOperatingMode(String name,
                                 OMCRangeList allowedValues)
  {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }

  public InterAgentOperatingMode(String name,
                                 OMCRangeList allowedValues,
                                 Comparable value)
  {
    super(name, allowedValues, value);
  }

  // Initialization methods
  /**
   * Set the message address of the target. This implementation
   * presumes that there is but one target.
   * @param target the address of the target agent.
   **/
  public void setTarget(MessageAddress target) {
    targets = Collections.singleton(target);
  }

  // UniqueObject interface
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID");
    this.uid = uid;
  }

  // Relay.Source interface

  /** 
   * @return null -- this is the source copy of the Relay.
   */
  public MessageAddress getSource() {
    return null;
  }

  /**
   * Get all the addresses of the target agents to which this Relay
   * should be sent. For this implementation this is always a
   * singleton set contain just one target.
   **/
  public Set getTargets() {
    return targets;
  }

  /**
   * Get an object representing the value of this Relay suitable
   * for transmission. This implementation uses itself to represent
   * its Content.
   **/
  public Object getContent() {
    return this;
  }

  /**
   * @return a factory to convert the content to a Relay Target.
   **/
  public Relay.TargetFactory getTargetFactory() {
    return InterAgentConditionFactory.INSTANCE;
  }

  /**
   * Set the response that was sent from a target. For LP use only.
   * This implemenation does nothing because responses are not needed
   * or used.
   **/
  public int updateResponse(MessageAddress target, Object response) {
    // No response expected
    return Relay.NO_CHANGE;
  }

  /**
   * This factory creates a new InterAgentCondition.
   **/
  private static class InterAgentConditionFactory 
    implements Relay.TargetFactory, java.io.Serializable {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      public static final InterAgentConditionFactory INSTANCE = 
        new InterAgentConditionFactory();
      private InterAgentConditionFactory() { }
      public Relay.Target create(
          UID uid, MessageAddress source, Object content, Relay.Token owner) {
        InterAgentOperatingMode iaom = (InterAgentOperatingMode) content;
        return new InterAgentCondition(iaom, uid, source, owner);
      }
      private Object readResolve() { return INSTANCE; }
    }
}
