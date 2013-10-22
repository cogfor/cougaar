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

package org.cougaar.core.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.log.Logger;

/**
 * The quiescence state of a particular agent
 */
class QuiescenceState {
  public QuiescenceState(MessageAddress me, Logger logger) {
    this.me = me;
    this.logger = logger;
  }

  public Map getOutgoingMessageNumbers() {
    return outgoingMessageNumbers;
  }

  public Map getIncomingMessageNumbers() {
    return incomingMessageNumbers;
  }

  public Integer getOutgoingMessageNumber(MessageAddress receiver) {
    return (Integer) getOutgoingMessageNumbers().get(receiver);
  }

  public Integer getIncomingMessageNumber(MessageAddress sender) {
    return (Integer) getIncomingMessageNumbers().get(sender);
  }

  public Set getOutgoingEntrySet() {
    return getOutgoingMessageNumbers().entrySet();
  }
  public Set getIncomingEntrySet() {
    return getIncomingMessageNumbers().entrySet();
  }
  
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean newEnabled) {
    enabled = newEnabled;
  }

  /**
   * Is the agent "alive" - as in, not marked as dead cause its a duplicate
   */
  public boolean isAlive() {
    return isAlive;
  }

  /**
   * Called when the agent is dead - another instance of the agent elsewhere
   * is the real instance, and this one should be ignored. Not un-doable.
   */
  public void setDead() {
    isAlive = false;
  }

  public boolean isQuiescent() {
    return blockers.isEmpty();
  }

  public void setQuiescent(boolean isQuiescent, String requestor) {
    if (isQuiescent) {
      // Remove this blocker, and log if that fails
      if (! removeBlocker(requestor) && logger.isDebugEnabled())
	logger.debug("Requestor " + requestor + " tried to say was quiescent, but was not on blockers list.");
      if (logger.isDetailEnabled()) {
        logger.detail("nonQuiescentCount is " + blockers.size() + " for " + (enabled ? "" : "disabled ") + (isAlive ? "" : "dead ") + me);
	if (! blockers.isEmpty())
	  logger.detail(me + " Blockers: " + getBlockersString());
      } else if (blockers.isEmpty() && logger.isDebugEnabled()) {
        logger.debug(me + " is quiescent");
      }
    } else {
      // Add this blocker, and log if that fails
      if (! addBlocker(requestor) && logger.isDebugEnabled())
	logger.debug("Requestor " + requestor + " tried to say was blocking, but was already on blockers list.");
      if (logger.isDetailEnabled()) {
        logger.detail("nonQuiescentCount is " + blockers.size() + " for " + (enabled ? "" : "disabled ") + (isAlive ? "" : "dead ") + me);
	logger.detail(me + " Blockers: " + getBlockersString());
      } else if (! blockers.isEmpty() && logger.isDebugEnabled()) {
        logger.debug(me + " is not quiescent");
      }
    }
  }

  public void setMessageNumbers(Map outgoing, Map incoming) {
    outgoingMessageNumbers = updateMap(outgoingMessageNumbers, outgoing);
    incomingMessageNumbers = updateMap(incomingMessageNumbers, incoming);
    if (logger.isDetailEnabled()) {
      logger.detail("setMessageNumbers for " + me
                    + ", outgoing=" + outgoing
                    + ", incoming=" + incoming);
    } else if (logger.isDebugEnabled()) {
      logger.debug("setMessageNumbers for " + me);
    }
  }

  public MessageAddress getAgent() {
    return me;
  }

  public String getAgentName() {
    return me.toString();
  }

  /**
   * Update an existing map to equal a new Map while minimizing
   * additional memory allocation where the keys in the old map are
   * very likely to be the same as the keys in the new Map.
   */
  private static Map updateMap(Map oldMap, Map newMap) {
    if (newMap == null) {
      throw new IllegalArgumentException("Null Map");
    }
    if (oldMap == null) return new HashMap(newMap);
    // Flush all keys missing from the new map
    oldMap.keySet().retainAll(newMap.keySet());
    // Avoid oldMap.putAll(newMap) since it expands the (Hash)Map
    // assuming the newMap has a non-intersecting keyset.
    for (Iterator i = newMap.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      oldMap.put(entry.getKey(), entry.getValue());
    }
    return oldMap;
  }


  private boolean addBlocker(String blocker) {
    // if not already in blockers list, add it.
    // If we added it, return true. Else false.
    return blockers.add(blocker);
  }

  private boolean removeBlocker(String blocker) {
    // if present, remove blocker. Return true if removed
    return blockers.remove(blocker);
  }

  public String getBlockersString() {
    // Iterate thru blocker list, producing a String
    return blockers.toString();
  }

  // RFE 3760: Add a Collection of Strings - blockers of quiescence.
  // FIXME: Synchronize this object? Or where?
  private Set blockers = new HashSet();

  private Map incomingMessageNumbers = new HashMap(13);
  private Map outgoingMessageNumbers = new HashMap(13);
  //  private int nonQuiescenceCount = 0;
  private MessageAddress me;
  private boolean enabled = false;
  private Logger logger;
  // Until we have agent suicide, agents can be local, enabled, but dead -- no one
  // should be communicating with them, and we don't want to consider them for local quiescence
  private boolean isAlive = true;
}
