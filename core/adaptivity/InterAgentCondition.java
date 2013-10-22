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
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * The Condition part of a remotely-controlled Condition. This is the
 * Relay.Target that receives updates from the InterAgentOperatingMode
 * Relay.Source. An instance of this class is instantiated on the
 * target's blackboard and acts as any other Condition such as
 * providing an input to the adaptivity engine.
 **/
public class InterAgentCondition
  extends OMCBase
  implements Relay.Source, Relay.Target, Condition
{

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

// this can't be transient like other relays, cause not storing
  // target separately
  private Set targets = Collections.EMPTY_SET;

  private UID uid;
  private MessageAddress source;
  private Relay.Token owner;

  InterAgentCondition(
      InterAgentOperatingMode iaom, UID uid, MessageAddress src, 
      Relay.Token owner) {
    super(iaom.getName(), iaom.getAllowedValues(), iaom.getValue());
    this.owner = owner;
    this.uid = uid;
    this.source = src;
  }

  InterAgentCondition(
      InterAgentCondition other, UID uid, MessageAddress src, 
      Relay.Token owner) {
    super(other.getName(), other.getAllowedValues(), other.getValue());
    this.owner = owner;
    this.uid = uid;
    this.source = src;
  }

  InterAgentCondition(String name, OMCRangeList allowedValues) {
    super(name, allowedValues);
  }

  InterAgentCondition(String name, OMCRangeList allowedValues, Comparable initialValue) {
    super(name, allowedValues, initialValue);
  }

  // UniqueObject implementation
  public UID getUID() {
    return uid;
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

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID");
    this.uid = uid;
  }

  // Relay.Source implementation
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
        InterAgentCondition iac = (InterAgentCondition) content;
        return new InterAgentCondition(iac, uid, source, owner);
      }
      private Object readResolve() { return INSTANCE; }
    }


  // Relay.Target implementation
  /**
   * Get the address of the Agent holding the Source copy of
   * this Relay.
   **/
  public MessageAddress getSource() {
    return source;
  }

  /**
   * Get the current response for this target. Null indicates that
   * this target has no response. This implementation never has a
   * response so it always returns null.
   **/
  public Object getResponse() {
    return null;
  }

  /**
   * Update with new content. The only part of the source content used
   * is the value of the operating mode.  This will accept an InterAgentOperatingMode
   * or an InterAgentCondition as the content.  Anything else will cause a ClassCastException.
   * @return true if the update changed the Relay. The LP should
   * publishChange the Relay. This implementation returns true only
   * if the new value differs from the current value.
   **/
  public int updateContent(Object content, Relay.Token token) {
    if (token != owner) {
      Logger logger = Logging.getLogger(getClass());
      if (logger.isInfoEnabled()) {
        logger.info(
          "Ignoring \"Not owner\" bug in \"updateContent()\","+
          " possibly a rehydration bug (token="+
          token+", owner="+owner+")");
      }
    }
    if (content instanceof InterAgentOperatingMode) {
      InterAgentOperatingMode newMode = (InterAgentOperatingMode) content;
      if (getValue().compareTo(newMode.getValue()) != 0) {
        setValue(newMode.getValue());
        return Relay.CONTENT_CHANGE;
      }
    } else {
      // In this case this should be an InterAgentCondition.  This currently has a Protected
      // contructor, so this is probably not the case, but it used this way in
      // CPURemoteTestPlugin. Let it throw the ClassCastException if it's anything else.
      InterAgentCondition newCond = (InterAgentCondition) content;
      if (getValue().compareTo(newCond.getValue()) != 0) {
        setValue(newCond.getValue());
        return Relay.CONTENT_CHANGE;
      }
    }
    return Relay.NO_CHANGE;
  }
}
