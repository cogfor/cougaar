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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logger;

/**
 * The QuiescenceMonitor is used by the {@link Distributor} to
 * determine if an agent is quiescent.
 * <p>
 * The QM tracks which blackboard clients we care about quiescence
 * for, and numbers incoming and outgoing messsages for those we care
 * about. The QM uses a ConfigFinder accessed text file to identify
 * which BlackboardClients to count in the quiescence calculation.
 * Typically this excludes all infrastructure components, and includes
 * only application-internal components.
 */
class QuiescenceMonitor {
  private static final String CONFIG_FILE = "quiescencemonitor.dat";
  private static final String[] defaultExcludedClients = {
    "exclude .*"
  };

  private static final String finalExclusion = "exclude .*";

  private QuiescenceReportService quiescenceReportService;
  private Logger logger;
  private boolean isQuiescent = false;
  private String messageNumbersChangedFor = null;
  private static class State implements Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   State(Map imn, Map omn, boolean isQ) {
      outgoingMessageNumbers = omn;
      incomingMessageNumbers = imn;
      isQuiescent = isQ;
    }
    protected Map outgoingMessageNumbers;
    protected Map incomingMessageNumbers;
    protected boolean isQuiescent;
  }
  private Map outgoingMessageNumbers = new HashMap();
  private Map incomingMessageNumbers = new HashMap();
  private int messageNumberCounter;
  private Map checkedClients = new HashMap();
  private List exclusions = new ArrayList();

  QuiescenceMonitor(QuiescenceReportService qrs, Logger logger) {
    this.logger = logger;
    quiescenceReportService = qrs;
    initMessageNumberCounter();
    try {
      BufferedReader is =
        new BufferedReader(
            new InputStreamReader(
              ConfigFinder.getInstance().open(CONFIG_FILE)));
      try {
        String line;
        while ((line = is.readLine()) != null) {
          int hash = line.indexOf('#');
          if (hash >= 0) {
            line = line.substring(0, hash);
          }
          line = line.trim();
          if (line.length() > 0) {
            addExclusion(line);
          }
        }
        addExclusion(finalExclusion);
      } finally {
        is.close();
      }
    } catch (FileNotFoundException e) {
      if (logger.isInfoEnabled()) {
        logger.info("File not found: " + e.getMessage() + ". Using defaults");
      }
      installDefaultExclusions();
    } catch (Exception e) {
      logger.error("Error parsing " + CONFIG_FILE + ". Using defaults", e);
      installDefaultExclusions();
    }
  }

  void setState(Object newState) {
    State state = (State) newState;
    incomingMessageNumbers = state.incomingMessageNumbers;
    outgoingMessageNumbers = state.outgoingMessageNumbers;
    isQuiescent = state.isQuiescent;
    messageNumbersChangedFor = "setState";
    setSubscribersAreQuiescent(isQuiescent);
  }

  Object getState() {
    return new State(incomingMessageNumbers, outgoingMessageNumbers, isQuiescent);
  }

  private void initMessageNumberCounter() {
    messageNumberCounter = (int) System.currentTimeMillis();
    nextMessageNumber();
  }

  private int nextMessageNumber() {
    if (++messageNumberCounter == 0) messageNumberCounter++;
    return messageNumberCounter;
  }

  private void installDefaultExclusions() {
    exclusions.clear();
    for (int i = 0; i < defaultExcludedClients.length; i++) {
      addExclusion(defaultExcludedClients[i]);
    }
  }

  private void addExclusion(String line) {
    exclusions.add(new Exclusion(line));
  }

  // Is quiescence required for this blackboard client (by name)
  // Note that exclusions typically end in .*, so if this is really
  // a PersistenceSubscriberState.getKey which has extra stuff at the end,
  // this will still match
  boolean isQuiescenceRequired(String clientName) {
    Boolean required = (Boolean) checkedClients.get(clientName);
    if (required == null) {
      required = Boolean.TRUE;
      loop:
      for (int i = 0, n = exclusions.size(); i < n; i++) {
        Exclusion p = (Exclusion) exclusions.get(i);
        switch (p.match(clientName)) {
        case Exclusion.EXCLUDE:
          required = Boolean.FALSE;
          break loop;
        case Exclusion.INCLUDE:
          required = Boolean.TRUE;
          break loop;
        default:
          continue loop;
        }
      }
      if (logger.isInfoEnabled()) {
        logger.info("isQuiescenceRequired(" + clientName + ")=" + required);
      }
      checkedClients.put(clientName, required);
    }
    return required.booleanValue();
  }

  // Is quiescence required for this blackboard client. Will use the client name
  boolean isQuiescenceRequired(BlackboardClient client) {
    String clientName = client.getBlackboardClientName();
    return isQuiescenceRequired(clientName);
  }

  synchronized void setSubscribersAreQuiescent(boolean subscribersAreQuiescent) {
    if (subscribersAreQuiescent) {
      if (messageNumbersChangedFor != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("updateMessageNumbers because of " + messageNumbersChangedFor);
        }
        quiescenceReportService
          .setMessageNumbers(outgoingMessageNumbers,
                             incomingMessageNumbers);
        messageNumbersChangedFor = null;
      }
      if (!isQuiescent) {
        isQuiescent = true;
        if (logger.isDebugEnabled()) {
          logger.debug("setQuiescentState");
        }
        quiescenceReportService.setQuiescentState();
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Still quiescent");
        }
      }
    } else {
      if (isQuiescent) {
        isQuiescent = false;
        if (messageNumbersChangedFor != null) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "clearQuiescentState: messageNumbersChangedFor " +
                messageNumbersChangedFor);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("clearQuiescentState: subscribers active");
          }
        }
        quiescenceReportService.clearQuiescentState();
      }
    }
  }

  synchronized boolean numberIncomingMessage(DirectiveMessage msg) {
    MessageAddress src = msg.getSource();
    src = src.getPrimary(); // Strip any attributes
    int messageNumber = msg.getContentsId();
    if (messageNumber == 0) return false; // Message from plugin not required for quiescence
    Integer last = (Integer) incomingMessageNumbers.put(src, new Integer(messageNumber));
    if (logger.isDebugEnabled()) {
      MessageAttributes ma = msg.getSource().getMessageAttributes();
      logger.debug(
          "Numbered incoming message from " + src + ": " + msg +
          " with number " + messageNumber + ", previous messageNumber was " +
          last + (ma != null ? (", attributes: " + ma.getAttributesAsString()) : ""));
    }
    messageNumbersChangedFor = src.toString();
    return true;
  }

  synchronized void numberOutgoingMessage(DirectiveMessage msg) {
    MessageAddress dst = msg.getDestination();
    dst = dst.getPrimary(); // Strip any attributes
    int messageNumber = nextMessageNumber();
    msg.setContentsId(messageNumber);
    Integer last = (Integer)outgoingMessageNumbers.put(dst, new Integer(messageNumber));
    if (logger.isDebugEnabled()) {
      MessageAttributes ma = msg.getDestination().getMessageAttributes();
      logger.debug(
          "Numbered outgoing message to " + dst + ": " + msg +
          " with number " + messageNumber + ", previous messageNumber was " +
          last + (ma != null ? (", attributes: " + ma.getAttributesAsString()) : ""));
    }
    messageNumbersChangedFor = dst.toString();
  }

  private static class Exclusion {
    private static final int EXCLUDE = 0;
    private static final int INCLUDE = 1;
    private static final int DONT_KNOW = -1;
    private static final String EXCLUDE_PREFIX = "exclude ";
    private static final String INCLUDE_PREFIX = "include ";
    private int matchCode;
    private Pattern p;

    public Exclusion(String line) {
      if (line.startsWith(EXCLUDE_PREFIX)) {
        matchCode = EXCLUDE;
        p = Pattern.compile(line.substring(EXCLUDE_PREFIX.length()));
      } else if (line.startsWith(INCLUDE_PREFIX)) {
        matchCode = INCLUDE;
        p = Pattern.compile(line.substring(INCLUDE_PREFIX.length()));
      } else {
        throw new IllegalArgumentException("Parse error: " + line);
      }
    }

    public int match(String clientName) {
      if (p.matcher(clientName).matches()) {
        return matchCode;
      }
      return DONT_KNOW;
    }
  }
}
