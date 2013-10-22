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

import java.io.Serializable;

import org.cougaar.core.mts.MessageAddress;

/**
 * A generic blackboard alert object used to notify agent or plugin
 * about anomalous data on an agent's blackboard.
 */
public class BlackboardAlert implements Serializable {
  
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private String sensorName, description;
  private long compromisedTimeStamp = 0;
  private MessageAddress victimAgent;
  private MessageAddress attackerAgent = null;
  
  /**
   * Basic Constructor
   * @param sensor sensor type, descriptive name or component name
   * @param time time of compromise if known - usually specified in terms of society time
   * @param victim the compromised agent address
   * @param attacker the attacking agent if known
   * @param desc description of problem, attack, classification or other information
   */
  public BlackboardAlert(String sensor, long time, MessageAddress victim, 
                         MessageAddress attacker, String desc) {
    sensorName = sensor;
    compromisedTimeStamp = time;
    if (victim == null) {
      throw new IllegalArgumentException("Victim address must be non-null");
    }
    victimAgent = victim;
    attackerAgent = attacker;
    this.description = desc;
  }

  /**
   * Minimal Constructor
   * @param sensor sensor type, descriptive name or component name
   * @param victim the compromised agent address
   */
  public BlackboardAlert(String sensor, MessageAddress victim) {
    sensorName = sensor;
    if (victim == null) {
      throw new IllegalArgumentException("Victim address must be non-null");
    }
    victimAgent = victim;
  }

  public String getSensorName() {
    return sensorName;
  }

  public long getTimeOfCompromise() {
    return compromisedTimeStamp;
  }

  public void setTimeOfCompromise(long time) {
    compromisedTimeStamp = time;
  }

  public MessageAddress getVictimAgent() {
    return victimAgent;
  }

  public MessageAddress getAttackerAgent() {
    return attackerAgent;
  }

  public void setAttackerAgent(MessageAddress attacker) {
    attackerAgent = attacker;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String desc) {
    description = desc;
  }
    

}
