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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.AgentQuiescenceStateService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.QuiescenceReportForDistributorService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.FilteredIterator;
import org.cougaar.util.Memo;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * {@link ServiceProvider} for the {@link QuiescenceReportService}.
 * <p> 
 * The QRS collects quiescence information from the Agent Distributors
 * and other QRS clients in the Node. It also matches sent and
 * received messages between agents in the Node. When the collective
 * quiescence state of the Node changs, the QRS sends an Event indicating
 * this change.
 *
 * @property org.cougaar.core.node.quiescenceAnnounceDelay specifies the 
 * number of milliseconds that the Node waits when it thinks it has become 
 * quiescent to send an event announcing the fact. This suppresses 
 * possible toggling. Default is 20 seconds.
 */
class QuiescenceReportServiceProvider implements ServiceProvider {
  private Map quiescenceStates = new HashMap();
  private boolean isQuiescent = false;

  // Predicate to get the quiscence states for agents that have enabled
  // their QRS and have not been marked as dead (duplicated elsewhere)
  private UnaryPredicate enabledQuiescenceStatePredicate = new UnaryPredicate() {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
	QuiescenceState qs = (QuiescenceState)o;
        return (qs.isEnabled() && qs.isAlive());
      }
    };
  private Logger logger;
  private String nodeName;
  private AgentContainer agentContainer;
  private ServiceBroker sb;
  private QuiescenceAnnouncer quiescenceAnnouncer;
  private AgentQuiescenceStateService aqsService = null;

  private static final long ANNOUNCEMENT_DELAY =
    SystemProperties.getLong("org.cougaar.core.node.quiescenceAnnounceDelay", 20000);

  //  private static final long ANNOUNCEMENT_DELAY = 30000L;
  private static final String EOL = " "; //SystemProperties.getProperty("line.separator");

  QuiescenceReportServiceProvider(String nodeName,
                                  AgentContainer agentContainer,
                                  ServiceBroker sb)
  {
    this.nodeName = nodeName;
    this.agentContainer = agentContainer;
    this.sb = sb;
    logger = new LoggingServiceWithPrefix(Logging.getLogger(getClass()), nodeName + ": ");
    quiescenceAnnouncer = new QuiescenceAnnouncer();
  }

  public Object getService(ServiceBroker xsb,
                           Object requestor,
                           Class serviceClass)
  {
    if (serviceClass == QuiescenceReportService.class) {
      if (requestor instanceof MessageAddress) {
        // special case, just for node-agent!
        MessageAddress addr = (MessageAddress) requestor;
        addr = addr.getPrimary(); // drop MessageAttributes
        return new QuiescenceReportServiceImpl(addr);
      } else {
        String name = requestor.toString() + " hash: " + requestor.hashCode();
        return new QuiescenceReportServiceImpl(name);
      }
    }
    if (serviceClass == QuiescenceReportForDistributorService.class) {
      return new QuiescenceReportForDistributorServiceImpl(requestor.toString());
    }
    if (serviceClass == AgentQuiescenceStateService.class) {
      if (aqsService == null)
	aqsService = new AgentQuiescenceStateServiceImpl();
      return aqsService;
    }
    throw new IllegalArgumentException("Cannot provide " + serviceClass.getName());
  }

  public void releaseService(ServiceBroker xsb, Object requestor,
                             Class serviceClass, Object service)
  {
    if (service instanceof QuiescenceReportService) {
      QuiescenceReportService quiescenceReportService = (QuiescenceReportService) service;
      quiescenceReportService.setQuiescentState();
    }
    // Nothing to do to release the aqsService
  }

  protected void revokeService() {
    quiescenceAnnouncer.stopQuiescenceAnnouncer();
  }
  
  private synchronized QuiescenceState getQuiescenceState(MessageAddress me) {
    QuiescenceState quiescenceState = (QuiescenceState) quiescenceStates.get(me);
    if (quiescenceState == null) {
      quiescenceState = new QuiescenceState(me, logger);
      quiescenceStates.put(me, quiescenceState);
    }
    return quiescenceState;
  }

  /** Just like getQuiescenceState, except does not create empty ones for misses */
  private synchronized QuiescenceState accessQuiescenceState(MessageAddress me) {
    return (QuiescenceState) quiescenceStates.get(me);
  }

  private Iterator getQuiescenceStatesIterator() {
    return new FilteredIterator(quiescenceStates.values().iterator(), enabledQuiescenceStatePredicate);
  }

  private boolean isLocalAgent(MessageAddress otherAgent) {
    QuiescenceState otherState = (QuiescenceState) quiescenceStates.get(otherAgent);
    if (otherState == null) return false;
    // Also return false if the otherState.isEnabled() && otherState's agent isDead
    return (otherState.isEnabled() && otherState.isAlive());
  }

  /**
   * Memoization of the quiescence state set.
   * Avoids bothering to make changes to 
   * unless there might be a difference.
   */
  private Memo _quiescenceStatesMemo = Memo.get(new Memo.Function() {
      @Override
      public String toString() { return "Memo<active quiescenceStates>"; }
      public Object eval(Object o) {
        Set agentAddresses = (Set) o;
        if (quiescenceStates.keySet().retainAll(agentAddresses)) {
          // retainAll() == true means that the collection changed, so we'll return
          return quiescenceStates;
        } else {
          // if we executed this, but it didn't change, we'll return null to 
          // indicate the same end-result as if we hadn't executed at all!
          return null;
        }
      }});

  /**
   * An agent has become quiescent. If all agents are quiescent and no
   * messages are outstanding, we announce our quiescence. Otherwise,
   * we cancel quiescence.
   */
  private void checkQuiescence() { // We might be quiescent...
    // If an agent moves out of this node, we need to clean out any
    // remembered message numbers.
    // FIXME: To be safe, should do getPrimary() on all the addresses returned
    // by the agentContainer, to strip off MessageAttributes
    _quiescenceStatesMemo.evalIfNew(agentContainer.getAgentAddresses());  // returns non-null if it changed!
    // if the set of agents hasn't changed, can we skip any of the below?

    // note that this expression short-circuits the expensive noMessagesAreOutstanding
    // call when !allAgentsAreQuiescent()
    if (allAgentsAreQuiescent() && noMessagesAreOutstanding()) {
      announceQuiescence();
      if (!isQuiescent && logger.isInfoEnabled()) {
        logger.info("Is quiescent");
      }
      isQuiescent = true;
    } else {
      if (isQuiescent) {
        cancelQuiescence();
      } 
      // else { still_quiescent(); }
    }
  }

  private void cancelQuiescence() {
    if (isQuiescent) {
      announceNonQuiescence();
      isQuiescent = false;
    }
  }

  private synchronized void setQuiescenceReportEnabled(QuiescenceState quiescenceState,
                                                       boolean enabled)
  {
    quiescenceState.setEnabled(enabled);
    if (logger.isInfoEnabled()) {
      logger.info((enabled ? "Enabled " : "Disabled ")
                  + quiescenceState.getAgentName());
    }
    checkQuiescence();
  }

  private synchronized void setMessageNumbers(QuiescenceState quiescenceState,
                                              Map outgoing, Map incoming)
  {
    quiescenceState.setMessageNumbers(outgoing, incoming);
    checkQuiescence();
  }

  private synchronized void setQuiescent(QuiescenceState quiescenceState, boolean isAgentQuiescent, String blocker) {
    quiescenceState.setQuiescent(isAgentQuiescent, blocker);
    if (isAgentQuiescent && quiescenceState.isEnabled() && quiescenceState.isAlive()) {
      checkQuiescence();
    } else if (quiescenceState.isEnabled() && quiescenceState.isAlive()) {
      // Only cancel quiescence if the state that announced it was not quiescent isEnabled
      // This prevents early-loading plugins from toggling quiescence of the Node
      cancelQuiescence();
    }
  }

  private boolean allAgentsAreQuiescent() {
    boolean result = true;
    // Loop through each Agent whose quiescence we care about
    for (Iterator theseStates = getQuiescenceStatesIterator(); theseStates.hasNext(); ) {
      QuiescenceState thisAgentState = (QuiescenceState) theseStates.next();
      if (!thisAgentState.isQuiescent()) {
        result = false;
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgentState.getAgentName() + " is not quiescent");
        } else {
          break;
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(thisAgentState.getAgentName() + " is quiescent");
        }
      }
    }
    return result;
  }

  /** Check known QS to see if all locally-sent messages have been recieved */
  private boolean noMessagesAreOutstanding() {
    // Old version:  O(N^2)
    //   for each X in local agents {
    //    for each Y in local agents {
    //     if (X.sentTo(Y) != Y.receivedFrom(X)) return false;
    //   }} 
    //   return true;
    //
    // this is bad because:
    //  - major: VERY few agents talk to all (or even most) other local agents
    //  - minor: sentTo cannot be < receivedFrom (factor of 2)
    //
    // So now, we'll do:
    //   for each X in local agents {
    //    foreach Y in (X.sentToList) {
    //     if (Y.isLocal) {
    //      if (X.sentTo(Y) != Y.receivedFrom(X)) return false;
    //   }}}
    //   return true;
    
    int local_agents = 0;
    int number_compares = 0;
    for (Iterator theseStates = getQuiescenceStatesIterator(); theseStates.hasNext(); ) {
      local_agents++;
      QuiescenceState thisAgentState = (QuiescenceState) theseStates.next();
      MessageAddress thisAgent = thisAgentState.getAgent();
      for (Iterator theseNumbers = thisAgentState.getOutgoingEntrySet().iterator(); theseNumbers.hasNext(); ) {
        Map.Entry thisNumber = (Map.Entry) theseNumbers.next();

        MessageAddress thatAgent = (MessageAddress) thisNumber.getKey();
        QuiescenceState thatAgentState = accessQuiescenceState(thatAgent);
        if (thatAgentState != null && thatAgentState.isEnabled() && thatAgentState.isAlive()) {
          number_compares++;
          Integer sentNumber = thisAgentState.getOutgoingMessageNumber(thatAgent);
          Integer rcvdNumber = thatAgentState.getIncomingMessageNumber(thisAgent);
          boolean match;
          if (sentNumber == null) {
            match = rcvdNumber == null;
          } else {
            match = sentNumber.equals(rcvdNumber);
          }
          if (!match) {
            if (logger.isDebugEnabled()) {
              logger.debug("Quiescence prevented by "
                           + thisAgent + " sent " + sentNumber + ", but "
                           + thatAgent + " rcvd " + rcvdNumber);
            }
            return false;
          }
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Quiescence message compare statistics:  locals="+local_agents+", compares="+number_compares);
    }
    return true;
  }

  private void appendMessageNumbers(StringBuffer ms, Map messages, String listTag, String itemTag) {
    ms.append("  <").append(listTag).append(">").append(EOL);
    for (Iterator entries = messages.entrySet().iterator(); entries.hasNext(); ) {
      Map.Entry entry = (Map.Entry) entries.next();
      MessageAddress otherAgent = (MessageAddress) entry.getKey();
      if (isLocalAgent(otherAgent)) continue; // Exclude local agents
      Integer msgnum = (Integer) entry.getValue(); // 
      ms.append("   <").append(itemTag).append(" agent=\"").append(otherAgent)
        .append("\" msgnum=\"").append(msgnum).append("\"/>").append(EOL);
    }
    ms.append("  </").append(listTag).append(">").append(EOL);
  }

  private void announceQuiescence() {
    // Spit out the message numbers we sent to agents of other
    // nodes and the message numbers we received from agents
    // of other nodes.
    StringBuffer ms = new StringBuffer();
    ms.append("<node name=\"").append(nodeName).append("\" quiescent=\"true\">").append(EOL);
    // Loop over relevant agents to get message numbers
    for (Iterator states = getQuiescenceStatesIterator(); states.hasNext(); ) {
      QuiescenceState quiescenceState = (QuiescenceState) states.next();
      ms.append(" <agent name=\"")
        .append(quiescenceState.getAgentName())
        .append("\">")
        .append(EOL);
      appendMessageNumbers(ms, quiescenceState.getOutgoingMessageNumbers(), "receivers", "dest");
      appendMessageNumbers(ms, quiescenceState.getIncomingMessageNumbers(), "senders", "src");
      ms.append(" </agent>").append(EOL);
    }
    ms.append("</node>").append(EOL);
    quiescenceAnnouncer.announceQuiescence(ms.toString());
  } 

  private void announceNonQuiescence() {
    quiescenceAnnouncer.announceNonquiescence("<node name=\""
                                              + nodeName
                                              + "\" quiescent=\"false\"/>"
                                              + EOL);
  }

  private class QuiescenceAnnouncer implements Runnable {
    private String pendingAnnouncement;
    private boolean lastAnnouncedQuiescence = true;
    private long announcementTime;
    private EventService eventService;
    private Object eventServiceLock = new Object();
    private Schedulable schedulable;
    private boolean isRunning;
    
    public QuiescenceAnnouncer() {
      super();
      isRunning = true;
    }

    /**
     * Stops the quiescence announcer.
     * <p>
     * 
     * This method should be invoked when the quiescence report service is revoked.
     */
    protected synchronized void stopQuiescenceAnnouncer() {
      isRunning = false;
      interrupt();
    }

    public synchronized void interrupt() {
      schedulable.cancel();
    }

    public synchronized void announceNonquiescence(String announcement) {
      if (isRunning) {
        // Cancel pending announcment if any
        pendingAnnouncement = null;
        if (lastAnnouncedQuiescence) {
          event(announcement);
          lastAnnouncedQuiescence = false;
        }
      }
    }

    public synchronized void announceQuiescence(String announcement) {
      if (isRunning) {
        if (schedulable == null) {
          // first time
        ThreadService tsvc = sb.getService(this, ThreadService.class, null);
          schedulable = tsvc.getThread(this, this, "Quiescence Announcer");
          sb.releaseService(this, ThreadService.class, tsvc);
        }
        // Replace the pending announcement
        pendingAnnouncement = announcement;
        // and restart the timeout
        announcementTime = System.currentTimeMillis() + ANNOUNCEMENT_DELAY;
        schedulable.start();
      }
    }

    private long getDelay() {
      return announcementTime - System.currentTimeMillis();
    }

    private void event(String msg) {
      synchronized (eventServiceLock) {
        if (eventService == null) {
          eventService = sb.getService(QuiescenceReportServiceProvider.this, EventService.class, null);
          if (eventService == null) {
            logger.error("No EventService available for " + EOL + msg);
          }
        }
      }
      if (eventService != null)
	eventService.event(msg);
      else if (logger.isInfoEnabled())
	logger.info(msg);
    }

    public synchronized void run() {
      long delay;

      if (isRunning) {
        try {
          if (pendingAnnouncement == null) {
            // Nothing to announce. No-op.
          } else if ((delay = getDelay()) > 0L) {
            // run again later
            schedulable.schedule(delay);
          } else {
            event(pendingAnnouncement);
            pendingAnnouncement = null;
            lastAnnouncedQuiescence = true;
          }
        } catch (Exception e) {
          logger.error("QuiescenceAnnouncer", e);
        }
      }
    }
  }

  private class QuiescenceReportForDistributorServiceImpl
    extends QuiescenceReportServiceImpl
    implements QuiescenceReportForDistributorService
  {
    public QuiescenceReportForDistributorServiceImpl(String requestorName) {
      super(requestorName);
    }

    public void setQuiescenceReportEnabled(boolean enabled) {
      QuiescenceReportServiceProvider.this.setQuiescenceReportEnabled(quiescenceState, enabled);
    }
  }

  // FIXME: Need a factory so can ensure unique requestorNames?

  private class QuiescenceReportServiceImpl implements QuiescenceReportService {
    protected QuiescenceState quiescenceState = null;

    // We haven't been counted as not quiescent yet
    // This tracks whether this particular client of the QRS says it is quiescent, where the Distributor
    // is just one
    private boolean isServiceImplQuiescent = true; 
    private String requestorName;

    // constructor used only by NodeAgent
    public QuiescenceReportServiceImpl(MessageAddress agent) {
      quiescenceState = getQuiescenceState(agent); // Drop messageAttributes
      this.requestorName= agent.toString();
    }

    // requestorName is the toString on the service requestor object, plus uniqueID
    public QuiescenceReportServiceImpl(String requestorName) {
      this.requestorName= requestorName;
    }

    public void setAgentIdentificationService(AgentIdentificationService ais) {
      if (ais == null) {
        quiescenceState = null;
      } else {
        quiescenceState = getQuiescenceState(ais.getMessageAddress());
      }
    }

    /**
     * Specify the maps of quiescence-relevant outgoing and incoming
     * message numbers associated with a newly achieved state of
     * quiescence. For efficiency, this method should be called before
     * calling setQuiescentState().
     * @param outgoingMessageNumbers a Map from agent MessageAddresses
     * to Integers giving the number of the last message sent. The
     * arguments must not be null.
     * @param incomingMessageNumbers a Map from agent MessageAddresses
     * to Integers giving the number of the last message received. The
     * arguments must not be null.
     */
    public void setMessageNumbers(Map outgoing, Map incoming) {
      if (quiescenceState == null) throw new RuntimeException("AgentIdentificationService has not be set");
      QuiescenceReportServiceProvider.this.setMessageNumbers(quiescenceState, outgoing, incoming);
    }

    /**
     * Specifies that, from this service instance point-of-view, the
     * agent is quiescent.
     */
    public void setQuiescentState() {
      if (quiescenceState == null) {
        throw new RuntimeException("AgentIdentificationService has not been set.");
      }

      // RFE 3760: Remove this requestor from qState's list of blockers
      if (isServiceImplQuiescent) {
	// If this serviceImpl already thinks it is quiescent, no need to say so twice
	if (logger.isDebugEnabled())
	  logger.debug("setQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName() + ", but this ServiceImpl already quiescent.");
      } else {
	if (logger.isDebugEnabled()) {
	  logger.debug("setQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName());
	}
	isServiceImplQuiescent = true;
	QuiescenceReportServiceProvider.this.setQuiescent(quiescenceState, true, requestorName);
      }
    }

    /**
     * Specifies that this agent is no longer quiescent.
     * That is, this client of the QRS (requestor) is saying that it
     * has work to do, and therefore the agent cannot be quiescent.
     */
    public void clearQuiescentState() {
      if (quiescenceState == null) {
        throw new RuntimeException("AgentIdentificationService has not been set.");
      }

      // RFE 3760: Add this requestor to qState's list of blockers
      // [Commented this out so can track all blockers of quiescence...]
      if (!isServiceImplQuiescent) {
	// FIXME: Without this return, 2 requestors with same name can conflict - second will make both non-q
	// but also it only takes 1 to make both Q
	// return;

	// If this serviceImpl already thinks it is blocking quiescence, no need to say so twice
	if (logger.isDebugEnabled()) {
	  logger.debug("clearQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName() + ", but this ServiceImpl already not quiescent.");
	}
      } else {
	if (logger.isDebugEnabled()) {
	  logger.debug("clearQuiescentState for " + requestorName + " of " + quiescenceState.getAgentName());
	}
	isServiceImplQuiescent = false;
	QuiescenceReportServiceProvider.this.setQuiescent(quiescenceState, false, requestorName);
      }
    }
  } // end of QuiescenceReportServiceImpl

  private synchronized MessageAddress[] listQuiescenceStates() {
    // return a new collection from quiescenceStates.keySet();
    Collection states = quiescenceStates.keySet();
    return (MessageAddress[])states.toArray(new MessageAddress[states.size()]);
  }

  // Put this in separate synch method so can ensure setDead is protected, and can call checkQuiescence
  private synchronized void setAgentDead(MessageAddress agentAddress) {
    QuiescenceState qState = accessQuiescenceState(agentAddress);
    if (qState == null)
      return;
    qState.setDead();
    checkQuiescence();
  }

  // Service to support servlet to mark agents as dead (and view all agent registered)
  private class AgentQuiescenceStateServiceImpl implements AgentQuiescenceStateService {
    /** Is the Node altogether quiescent */
    public boolean isNodeQuiescent() {
      return QuiescenceReportServiceProvider.this.isQuiescent;
    }

    /**
     * List the local agents with quiescence states for the Node to consider
     * @return an array of MessageAddresses registered with the Nodes QuiescenceReportService
     */
    public MessageAddress[] listAgentsRegistered() {
      return listQuiescenceStates();
    }

    /**
     * Is the named agent's quiescence service enabled (ie the Distributor is fully loaded)?
     * @param agentAddress The agent to query
     * @return true if the agent's quiescence service has been enabled and it counts towards Node quiescence, false otherwise or if it does not exist locally
     */
    public boolean isAgentEnabled(MessageAddress agentAddress) {
      if (agentAddress == null)
	return false;
      QuiescenceState qState = accessQuiescenceState(agentAddress);
      if (qState == null)
	return false;
      return qState.isEnabled();
    }

    /**
     * Is the named agent quiescent?
     * @param agentAddress The agent to query
     * @return true if the Agent's Distributor is quiescent, false otherwise or if the agent does not exist locally
     */
    public boolean isAgentQuiescent(MessageAddress agentAddress) {
      if (agentAddress == null)
	return false;
      QuiescenceState qState = accessQuiescenceState(agentAddress);
      if (qState == null)
	return false;
      return qState.isQuiescent();
    }

    /**
     * Is the named agent alive for quiescence purposes, or has it been
     * marked as dead to be ignored?
     * @param agentAddress The agent to query
     * @return false if the agent is dead and should be ignored for local quiescence or does not exist locally
     */
    public boolean isAgentAlive(MessageAddress agentAddress) {
      if (agentAddress == null)
	return false;
      QuiescenceState qState = accessQuiescenceState(agentAddress);
      if (qState == null)
	return false;
      return qState.isAlive();
    }

    /**
     * Mark the named agent as dead - it has been restarted elsewhere, and should
     * be ignored locally for quiescence calculations.
     * @param agentAddress The Agent to mark as dead
     */
    public void setAgentDead(MessageAddress agentAddress) {
      if (agentAddress == null)
	return;
      QuiescenceReportServiceProvider.this.setAgentDead(agentAddress);
    }

    // Other options: list message numbers? 

    // RFE 3760: List blockers of an agents quiescence (return a String)
    // FIXME: Synchronize something?
    public String getAgentQuiescenceBlockers(MessageAddress agentAddress) {
      if (agentAddress == null)
	return "";
      QuiescenceState qState = accessQuiescenceState(agentAddress);
      if (qState == null)
	return "";
      return qState.getBlockersString();
    }
  }
}
