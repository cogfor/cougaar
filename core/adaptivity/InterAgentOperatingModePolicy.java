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
 * A remotely-controlled OperatingModePolicy. Allows a policy manager in one
 * agent to control a Policy of the in another. It is instantiated in 
 * the controlling agent and transferred
 * to the controlled agent using the Relay logic providers. A copy
 * of the instance is published in the controlled agent's blackboard
 * and used like any other OperatingModePolicy.
 **/
public class InterAgentOperatingModePolicy
  extends OperatingModePolicy
  implements Relay.Source, Relay.Target
{
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// this can't be transient like other relays, cause not storing
  // target separately
  private Set targets = Collections.EMPTY_SET;
  protected MessageAddress source;
  protected Relay.Token owner;

  // Constructors
  public InterAgentOperatingModePolicy(PolicyKernel pk) {
    super(pk);
  }

  public InterAgentOperatingModePolicy(ConstrainingClause ifClause, 
				       ConstraintPhrase[] omConstraints) {
    super(ifClause, omConstraints);
  }

  public InterAgentOperatingModePolicy(String name,
				       ConstrainingClause ifClause, 
				       ConstraintPhrase[] omConstraints,
				       String authority) {
    super(name, ifClause, omConstraints, authority);
  }

  protected InterAgentOperatingModePolicy(InterAgentOperatingModePolicy other,
                                          MessageAddress src, 
                                          Relay.Token owner) {
    this(other.getName(),
	 other.getIfClause(), 
	 other.getOperatingModeConstraints(),
	 other.getAuthority());
    setUID(other.getUID());
    this.owner = owner;
    this.source = src;
  }

  // Initialization methods
  /**
   * Set the message address of the target. This implementation
   * presumes that there is but one target.
   * @param targetAddress the address of the target agent.
   **/
  public void setTarget(MessageAddress targetAddress) {
    targets = Collections.singleton(targetAddress);
  }

  /**
   * appliesToThisAgent - return true if InterAgentOperatingModePolicy applies to 
   * this Agent.
   * Default behaviour is to assume that InterAgentOperatingModePolicy does not
   * apply to the originating Agent.
   */
  public boolean appliesToThisAgent() {
    // Policy only valid is it did not originate here
    return (getSource() != null);
  }
  
  // Relay.Source interface
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
    return InterAgentPolicyFactory.INSTANCE;
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
   * is the value of the operating mode.
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
    InterAgentOperatingModePolicy newOMP = (InterAgentOperatingModePolicy) content;
    // brute force, no brains
    setPolicyKernel(newOMP.getPolicyKernel());
    return Relay.CONTENT_CHANGE;
  }

  /**
   * This factory creates a new InterAgentOperatingModePolicy.
   **/
  private static class InterAgentPolicyFactory 
    implements Relay.TargetFactory, java.io.Serializable 
  {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public static final InterAgentPolicyFactory INSTANCE = 
      new InterAgentPolicyFactory();
    private InterAgentPolicyFactory() { }
    public Relay.Target create(UID uid, 
			       MessageAddress source, 
			       Object content, 
			       Relay.Token owner) {
      InterAgentOperatingModePolicy iaomp 
	= (InterAgentOperatingModePolicy) content;
      return new InterAgentOperatingModePolicy(iaomp, source, owner);
    }
    private Object readResolve() { return INSTANCE; }
  }
}


