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

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.ClusterMessage;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.StringUtility;

/**
 * A message acknowledgement manager used by the {@link Distributor}'s
 * non-lazy persistence mode to ensure that unacknowledged messages
 * are persisted.
 */
class MessageManagerImpl implements MessageManager, Serializable {

  public static final long serialVersionUID = -8662117243114391926L;

  private static final boolean debug =
    SystemProperties.getBoolean("org.cougaar.core.blackboard.MessageManager.debug");

  private static final long KEEP_ALIVE_INTERVAL = 55000L;

  private boolean USE_MESSAGE_MANAGER = false;

  /** The agent's mts */
  private transient MessageAddress self;
  private transient MessageSwitchService msgSwitch;

  private transient String agentNameForLog;

  /** Messages we need to send at the end of this epoch. */
  private transient ArrayList stuffToSend = new ArrayList();

  /** Tracks the sequence numbers of other agents */
  private HashMap agentInfo = new HashMap(13);

  /** Something has happened during this epoch. */
  private transient boolean needAdvanceEpoch = false;

  /** The retransmitter thread */
  private transient Retransmitter retransmitter;

  /** The acknowledgement sender thread */
  private transient AcknowledgementSender ackSender;

  /** The keep alive sender thread */
  private transient KeepAliveSender keepAliveSender;

  /** Debug logging */
  private transient PrintWriter logWriter = null;

  /** The format of timestamps in the log */
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

  /**
   * Inner static class to track the state of communication with
   * another agent.
   */
  private class AgentInfo implements java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /** The MessageAddress of the remote agent */
    private MessageAddress agentIdentifier;

    private long remoteIncarnationNumber = 0L;

    private long localIncarnationNumber = System.currentTimeMillis();

    private transient boolean restarted = false;

    public AgentInfo(MessageAddress cid) {
      agentIdentifier = cid;
    }

    public MessageAddress getMessageAddress() {
      return agentIdentifier;
    }

    /**
     * The last sequence number we transmited to the agent described
     * by this AgentInfo
     */
    private int currentTransmitSequenceNumber = 0;

    /**
     * The next sequence number we expect to receive from the agent
     * described by this AgentInfo
     */
    private int currentReceiveSequenceNumber = 0;

    /**
     * The record of messages that have been acknowledged. We acknowledge the highest

    /**
     * The queue of messages that are outstanding.
     */
    private TreeSet outstandingMessages = new TreeSet();

    public void addOutstandingMessage(TimestampedMessage tsm) {
      outstandingMessages.add(tsm);
      needAdvanceEpoch = true;
    }

    public TimestampedMessage[] getOutstandingMessages() {
      return (TimestampedMessage[])
        outstandingMessages.toArray(
            new TimestampedMessage[outstandingMessages.size()]);
    }

    public synchronized TimestampedMessage getFirstOutstandingMessage() {
      if (outstandingMessages.isEmpty()) return null;
      return (TimestampedMessage) outstandingMessages.first();
    }

    /** Which messages have we actually processed (need acks) */
    private AckSet ackSet = new AckSet(1);

    private boolean needSendAcknowledgement = false;

    private transient long transmissionTime = 0;

    public synchronized long getTransmissionTime() {
      return transmissionTime;
    }

    public synchronized void setTransmissionTime(long now) {
      transmissionTime = now;
    }

    public void acknowledgeMessage(ClusterMessage aMessage) {
      ackSet.set(aMessage.getContentsId());
      needAdvanceEpoch = true;
    }

    public boolean needSendAcknowledgement() {
      return needSendAcknowledgement;
    }

    public void setNeedSendAcknowledgment() {
      needSendAcknowledgement = true;
      ackSender.poke();
    }

    public boolean getRestarted() {
      return restarted;
    }

    public void setRestarted(boolean newRestarted) {
      restarted = newRestarted;
    }

    public void advance() {
      int oldMin = ackSet.getMinSequence();
      if (ackSet.advance() > oldMin) {
        needAdvanceEpoch = true; // State has change, need to persist
        setNeedSendAcknowledgment(); // Also need to send an ack
      }
    }

    /**
     * Create an acknowledgement for the current min sequence of the
     * ackset.
     */
    public AckDirectiveMessage getAcknowledgement() {
      int firstZero = ackSet.getMinSequence();
      AckDirectiveMessage ack = new AckDirectiveMessage(getMessageAddress(),
                                                        self,
                                                        firstZero - 1,
                                                        remoteIncarnationNumber);
      needSendAcknowledgement = false;
      return ack;
    }

    public void receiveAck(MessageManagerImpl mm, int sequence, boolean isRestart) {
      long now = System.currentTimeMillis();
      for (Iterator messages = outstandingMessages.iterator(); messages.hasNext(); ) {
        TimestampedMessage tsm = (TimestampedMessage) messages.next();
        if (tsm.getSequenceNumber() <= sequence) {
          mm.printMessage("Remv", tsm);
          messages.remove();
        } else if (isRestart) {
          tsm.setTimestamp(now); // Retransmit this ASAP
        } else {
          break;                // Nothing left to do
        }
      }
    }

    /**
     * Check that the given message has the right sequence number.
     * @return a code indicating whether the message is old, current,
     * or future.
     */
    public int checkReceiveSequenceNumber(DirectiveMessage aMessage) {
      int seq = aMessage.getContentsId();
      if (remoteIncarnationNumber == 0L) return FUTURE;
      if (seq <= currentReceiveSequenceNumber) return DUPLICATE;
      if (seq > currentReceiveSequenceNumber + 1) return FUTURE;
      return PRESENT;
    }

    public long getLocalIncarnationNumber() {
      return localIncarnationNumber;
    }

    public long getRemoteIncarnationNumber() {
      return remoteIncarnationNumber;
    }

    public void setRemoteIncarnationNumber(long incarnationNumber) {
      remoteIncarnationNumber = incarnationNumber;
    }

    public int getCurrentTransmitSequenceNumber() {
      return currentTransmitSequenceNumber;
    }

    public int getNextTransmitSequenceNumber() {
      return ++currentTransmitSequenceNumber;
    }

    @SuppressWarnings("unused")
   public int getCurrentReceiveSequenceNumber() {
      return currentReceiveSequenceNumber;
    }

    /**
     * Update the current receive sequence number of this other agent.
     * @param seqno the new current sequence number of this other
     * agent.
     */
    public void updateReceiveSequenceNumber(int seqno) {
      currentReceiveSequenceNumber = seqno;
      needAdvanceEpoch = true;  // Our state changed need to persist
    }

    @Override
   public String toString() {
      return "AgentInfo " + agentIdentifier + " " +
        incarnationToString(localIncarnationNumber) + "->" +
        incarnationToString(remoteIncarnationNumber);
    }
  }

  /**
   * Tag a message on a retransmit queue with the time at which it
   * should next be sent.
   */
  private class TimestampedMessage implements Comparable, java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   protected transient long timestamp = System.currentTimeMillis();
    private transient int nTries = 0;

    protected transient DirectiveMessage theMessage;

    private MessageAddress theDestination;
    private int theSequenceNumber;
    private long theIncarnationNumber;
    private Directive[] theDirectives;
    private AgentInfo info;

    TimestampedMessage(AgentInfo info, DirectiveMessage aMsg) {
      this.info = info;
      theMessage = aMsg;
      theDestination = aMsg.getDestination();
      theSequenceNumber = aMsg.getContentsId();
      theIncarnationNumber = aMsg.getIncarnationNumber();
      theDirectives = aMsg.getDirectives();
    }

    public void send(long now) {
      msgSwitch.sendMessage(getMessage());
      long nextRetransmission =
        now + retransmitSchedule[Math.min(nTries++, retransmitSchedule.length - 1)];
      setTimestamp(nextRetransmission);
    }

    /**
     * Get the DirectiveMessage. If theMessage is null, create a new
     * one. theMessage will be null only after rehydration of the
     * message manager.
     */
    public DirectiveMessage getMessage() {
      if (theMessage == null) {
        theMessage = new DirectiveMessage(getSource(),
                                          getDestination(),
                                          theIncarnationNumber,
                                          getDirectives());
        theMessage.setContentsId(theSequenceNumber);
      }
      theMessage.setAllMessagesAcknowledged(info.getFirstOutstandingMessage() == this);
      return theMessage;
    }

    public MessageAddress getDestination() {
      return theDestination;
    }

    public MessageAddress getSource() {
      return self;
    }

    public int getSequenceNumber() {
      return theSequenceNumber;
    }

    public long getIncarnationNumber() {
      return theIncarnationNumber;
    }

    public Directive[] getDirectives() {
      return theDirectives;
    }

    public void setTimestamp(long ts) {
      timestamp = ts;
    }

    public int compareTo(Object other) {
      TimestampedMessage otherMsg = (TimestampedMessage) other;
      return this.theSequenceNumber - otherMsg.theSequenceNumber;
    }

    @Override
   public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("seq(");
      buf.append(theIncarnationNumber);
      buf.append("/");
      buf.append(theSequenceNumber);
      buf.append(") ");
      StringUtility.appendArray(buf, theDirectives);
      return buf.substring(0);
    }
  }

  private static long[] retransmitSchedule = {
    20000L, 20000L, 60000L, 120000L, 300000L
  };

  public MessageManagerImpl(boolean enable) {
    USE_MESSAGE_MANAGER = enable;
  }

  public void start(MessageSwitchService msgSwitch, boolean didRehydrate) {
    self = msgSwitch.getMessageAddress();
    this.msgSwitch = msgSwitch;
    String agentName = self.getAddress();
    agentNameForLog =
      "               ".substring(Math.min(14, agentName.length())) +
      agentName + " ";
    if (debug) {
      try {
        logWriter = new PrintWriter(new FileWriter("MessageManager_" +
                                                   agentName +
                                                   ".log",
                                                   true || didRehydrate));
        printLog("MessageManager Started");
      }
      catch (IOException e) {
        System.err.println("Can't open MessageManager log file: " + e);
      }
    }

    if (USE_MESSAGE_MANAGER) {
      retransmitter = new Retransmitter(agentName);
      retransmitter.start();
      ackSender = new AcknowledgementSender(agentName);
      ackSender.start();
      keepAliveSender = new KeepAliveSender(agentName);
      keepAliveSender.start();
    }
  }

  public void stop() {
    if (USE_MESSAGE_MANAGER) {
      // TODO postponed until needed
      System.err.println(
          "\nFIXME MessageManager \"stop()\" for (USE_MESSAGE_MANAGER == true) "+
          "should halt internal threads");
    }
  }

  private synchronized void sendKeepAlive() {
    ArrayList messages = new ArrayList(agentInfo.size());
    Directive[] directives = new Directive[0];
    long now = System.currentTimeMillis();
    for (Iterator agents = agentInfo.values().iterator(); agents.hasNext(); ) {
      AgentInfo info = (AgentInfo) agents.next();
      if (info.getFirstOutstandingMessage() == null) {
        if (now > info.getTransmissionTime() + KEEP_ALIVE_INTERVAL) {
          DirectiveMessage ndm =
            new DirectiveMessage(self,
                                 info.getMessageAddress(),
                                 info.getLocalIncarnationNumber(),
                                 directives);
          messages.add(ndm);
        }
      }
    }
    sendMessages(messages.iterator());
  }

  private void printMessage(String prefix, DirectiveMessage aMessage) {
    printMessage(prefix,
                 aMessage.getIncarnationNumber(),
		 aMessage.getContentsId(),
		 aMessage.getSource().getAddress(),
		 aMessage.getDestination().getAddress(),
                 (aMessage.areAllMessagesAcknowledged() ? " yes" : " no") +
		 StringUtility.arrayToString(aMessage.getDirectives()));
  }

  private void printMessage(String prefix, AckDirectiveMessage aMessage) {
    printMessage(prefix,
                 aMessage.getIncarnationNumber(),
		 aMessage.getContentsId(),
		 aMessage.getSource().getAddress(),
		 aMessage.getDestination().getAddress(),
		 "");
  }

  private void printMessage(String prefix, TimestampedMessage tsm) {
    printMessage(prefix,
		 tsm.getIncarnationNumber(),
                 tsm.getSequenceNumber(),
		 tsm.getSource().getAddress(),
		 tsm.getDestination().getAddress(),
                 " ???" + StringUtility.arrayToString(tsm.getDirectives()));
  }

  private Date tDate = new Date();
  private SimpleDateFormat incarnationFormat =
    new SimpleDateFormat("yyyy/MM/dd/hh:mm:ss.SSS");

  private String incarnationToString(long l) {
    if (l == 0L) return "<none>";
    tDate.setTime(l);
    return incarnationFormat.format(tDate);
  }

  private void printMessage(String prefix, long incarnationNumber, int sequence,
                            String from, String to, String contents)
  {
    tDate.setTime(incarnationNumber);
    String msg =
      prefix + " " + sequence + " " + from + "->" +
      to + " (" + incarnationFormat.format(tDate) + "): " +
      contents;
//      System.out.println(msg);
    if (logWriter != null) {
      printLog(msg);
    }
  }

  private void printLog(String msg) {
    logWriter.print(logTimeFormat.format(new Date(System.currentTimeMillis())));
    logWriter.print(agentNameForLog);
    logWriter.println(msg);
    logWriter.flush();
  }

  /**
   * Submit a DirectiveMessage for transmission from this agent. The
   * message is added to the set of message to be transmitted at the
   * end of the current epoch.
   */
  public void sendMessages(Iterator messages) {
    if (USE_MESSAGE_MANAGER) {
      synchronized (this) {
        while (messages.hasNext()) {
          DirectiveMessage aMessage = (DirectiveMessage) messages.next();
          AgentInfo info = getAgentInfo(aMessage.getDestination());
          if (info == null) {
            if (debug) printLog("sendMessage createNewConnection");
            info = createNewConnection(aMessage.getDestination(), 0L);
          }
          aMessage.setIncarnationNumber(info.getLocalIncarnationNumber());
          aMessage.setContentsId(info.getNextTransmitSequenceNumber());
          stuffToSend.add(new TimestampedMessage(info, aMessage));
          if (debug) printMessage("QSnd", aMessage);
        }
        needAdvanceEpoch = true;
      }
    } else {
      while (messages.hasNext()) {
        msgSwitch.sendMessage((DirectiveMessage) messages.next());
      }
    }
  }

  private Directive[] emptyDirectives = new Directive[0];

  private void sendInitializeMessage(AgentInfo info) {
    DirectiveMessage msg = new DirectiveMessage(self,
                                                info.getMessageAddress(),
                                                info.getLocalIncarnationNumber(),
                                                emptyDirectives);
    msg.setContentsId(info.getNextTransmitSequenceNumber());
    stuffToSend.add(new TimestampedMessage(info, msg));
    if (debug) printMessage("QSnd", msg);
    needAdvanceEpoch = true;
  }

  private AgentInfo getAgentInfo(MessageAddress agentIdentifier) {
    return (AgentInfo) agentInfo.get(agentIdentifier);
  }

  private AgentInfo createAgentInfo(MessageAddress agentIdentifier) {
    AgentInfo info = new AgentInfo(agentIdentifier);
    agentInfo.put(agentIdentifier, info);
    return info;
  }

  /**
   * Check a received DirectiveMessage for being a duplicate.
   * @param aMessage The received DirectiveMessage
   * @return DUPLICATE, FUTURE, RESTART, IGNORE, or OK
   */
  public int receiveMessage(DirectiveMessage directiveMessage) {
    if (!USE_MESSAGE_MANAGER) return OK;
    synchronized (this) {
      boolean restarted = false;
      MessageAddress sourceIdentifier = directiveMessage.getSource();
      AgentInfo info = getAgentInfo(sourceIdentifier);
      boolean isFirst = directiveMessage.getContentsId() == 1;
      if (info != null) {
        if (info.getRestarted()) {
          restarted = true;
          info.setRestarted(false);
        }
        long infoIncarnation = info.getRemoteIncarnationNumber();
        long messageIncarnation = directiveMessage.getIncarnationNumber();
        if (infoIncarnation != messageIncarnation) {
          if (infoIncarnation == 0L) {
            if (isFirst) {
              info.setRemoteIncarnationNumber(messageIncarnation);
            } else {
              if (debug) printMessage("Nnz1", directiveMessage);
              info.setNeedSendAcknowledgment();
              return restarted ? (IGNORE | RESTART) : IGNORE;      // Stray message
            }
          } else if (messageIncarnation < infoIncarnation) {
            if (debug) printMessage("Prev", directiveMessage);
            info.setNeedSendAcknowledgment();
            // Message from previous incarnation of remote agent
            return restarted ? (IGNORE | RESTART) : IGNORE;
          } else if (messageIncarnation > infoIncarnation) {
                                // Message from new incarnation
            if (isFirst) {      // Synchronize to new incarnation
              if (debug) printLog("receiveMessage messageIncarnation > infoIncarnation");
              info = createNewConnection(
                  sourceIdentifier, directiveMessage.getIncarnationNumber());
              restarted = true;
            } else {
              if (debug) printMessage("Nnz2", directiveMessage);
              info.setNeedSendAcknowledgment();
              // Apparently new incarnation, but not sequence 0
              return restarted ? (IGNORE | RESTART) : IGNORE;
            }
          }
        }
      } else {
        if (isFirst) {
          if (debug) printLog("receiveMessage null info is first");
          info = createNewConnection(sourceIdentifier, directiveMessage.getIncarnationNumber());
        } else {
          if (debug) printMessage("receiveMessage null info not first", directiveMessage);
          info = createNewConnection(sourceIdentifier, 0L);
          return IGNORE; // Must have sequence zero to synchronize
        }
      }
      switch (info.checkReceiveSequenceNumber(directiveMessage)) {
      case DUPLICATE:
        if (debug) printMessage("Dupl", directiveMessage);
        info.setNeedSendAcknowledgment();
        return IGNORE;
      default:
      case FUTURE:
        if (directiveMessage.areAllMessagesAcknowledged()) {
          // We are out of sync
          if (debug) printLog("receiveMessage from future all acked");
          info = createNewConnection(sourceIdentifier, 0L);
          return IGNORE | RESTART;
        }
        if (debug) printMessage("Futr", directiveMessage);
        return IGNORE;        // Message out of order; ignore it
      case OK:
        if (debug) printMessage("Rcvd", directiveMessage);
        info.updateReceiveSequenceNumber(directiveMessage.getContentsId());
        needAdvanceEpoch = true;
        return restarted ? (RESTART | OK) : OK;
      }
    }
  }

  private AgentInfo createNewConnection(
      MessageAddress sourceIdentifier, long remoteIncarnationNumber) {
    AgentInfo info = createAgentInfo(sourceIdentifier); // New connection
    info.setRemoteIncarnationNumber(remoteIncarnationNumber);
    if (debug) printLog("New Connection: " + info.toString());
    sendInitializeMessage(info);
    return info;
  }

  public void acknowledgeMessages(Iterator messages) {
    if (!USE_MESSAGE_MANAGER) return;
    synchronized (this) {
      while (messages.hasNext()) {
        DirectiveMessage aMessage = (DirectiveMessage) messages.next();
        if (aMessage.getContentsId() == 0) return; // Not reliably sent
        AgentInfo info = getAgentInfo(aMessage.getSource());
        info.acknowledgeMessage(aMessage);
        needAdvanceEpoch = true;
        if (debug) printMessage("QAck", aMessage);
      }
    }
  }

  /**
   * Process a directive acknowledgement. The acknowledged messages
   * are removed from the retransmission queues. If the ack is marked
   * as having been sent during a agent restart, we speed up the
   * retransmission process to hasten the recovery process.
   */
  public int receiveAck(AckDirectiveMessage theAck) {
    synchronized (this) {
      if (debug) printMessage("RAck", theAck);
      AgentInfo info = getAgentInfo(theAck.getSource());
      if (info != null) {
        boolean restarted = false;
        if (info.getRestarted()) {
          info.setRestarted(false);
          restarted = true;
        }
        long localIncarnationNumber = info.getLocalIncarnationNumber();
        long ackIncarnationNumber = theAck.getIncarnationNumber();
        if (localIncarnationNumber == ackIncarnationNumber) {
          int seq = theAck.getContentsId();
          if (info.getCurrentTransmitSequenceNumber() < seq) {
            if (debug) printLog("receiveAck from future same incarnation");
            createNewConnection(info.getMessageAddress(), 0L);
            return RESTART;
          }
          info.receiveAck(this, seq, false);
          return restarted ? (RESTART | OK) : OK;
        } else if (localIncarnationNumber < ackIncarnationNumber) {
          // We are living in the past. We must have rehydrated with
          // an old set of connections.
          if (debug) printLog("receiveAck from future incarnation");
          createNewConnection(info.getMessageAddress(), 0L);
          return RESTART;
        } else {
          // The other end is living in the past. Hopefully, he will
          // eventually get with the program.
          if (debug) printLog("receiveAck from past incarnation");
          return restarted ? (IGNORE | RESTART) : IGNORE;
        }
      } else {
        return IGNORE;
      }
    }
  }

  /**
   * Determine if anything has happened during this epoch.
   * @return true if anything has changed.
   */
  public boolean needAdvanceEpoch() {
    return needAdvanceEpoch;
  }

  /**
   * Wrap up the current epoch and get into the correct state to be
   * persisted. Every message that has been queued for transmission is
   * sent. Acknowledgement numbers are advanced so we begin
   * acknowledging messages we have received and processed. This
   * method must be called while this MessageManager is
   * synchronized. We purposely omit the "synchronized" here because
   * proper operation is precluded unless the synchronization is
   * performed externally.
   */
  public void advanceEpoch() {
    if (!USE_MESSAGE_MANAGER) return;
    // Advance the information about every other agent
    for (Iterator agents = agentInfo.values().iterator(); agents.hasNext(); ) {
      AgentInfo info = (AgentInfo) agents.next();
      info.advance();
    }
    needAdvanceEpoch = false;
    for (Iterator iter = stuffToSend.iterator(); iter.hasNext(); ) {
      TimestampedMessage tsm = (TimestampedMessage) iter.next();
      getAgentInfo(tsm.getDestination()).addOutstandingMessage(tsm);
      retransmitter.poke();
    }
    stuffToSend.clear();
    if (logWriter != null) {
      printLog("Advanced epoch");
    }
  }

  private class KeepAliveSender extends Thread {
    public KeepAliveSender(String agentName) {
      super("Keep Alive Sender/" + agentName);
    }
    @Override
   public void run() {
      while (true) {
        sendKeepAlive();
        try {
          sleep(KEEP_ALIVE_INTERVAL);
        }
        catch (InterruptedException ie) {
        }
      }
    }
  }

  private class AcknowledgementSender extends Thread {
    private boolean poked = false;
    ArrayList acksToSend = new ArrayList();

    public AcknowledgementSender(String agentName) {
      super("Ack Sender/" + agentName);
    }

    public synchronized void poke() {
      poked = true;
      AcknowledgementSender.this.notify();
    }

    @Override
   public void run() {
      while (true) {
        synchronized (AcknowledgementSender.this) {
          while (!poked) {
            try {
              AcknowledgementSender.this.wait();
            }
            catch (InterruptedException ie) {
            }
          }
          poked = false;
        }
        synchronized (MessageManagerImpl.this) {
          for (Iterator agents = agentInfo.values().iterator(); agents.hasNext(); ) {
            AgentInfo info = (AgentInfo) agents.next();
            if (info.needSendAcknowledgement()) {
              acksToSend.add(info.getAcknowledgement());
            }
          }
        }
        for (Iterator iter = acksToSend.iterator(); iter.hasNext(); ) {
          AckDirectiveMessage ack = (AckDirectiveMessage) iter.next();
          if (debug) printMessage("SAck", ack);
          msgSwitch.sendMessage(ack);
        }
        acksToSend.clear();
      }
    }
  }

  private class Retransmitter extends Thread {
    private boolean poked = false;
    private ArrayList messagesToRetransmit = new ArrayList();

    public Retransmitter(String agentName) {
      super(agentName + "/Message Manager");
    }

    public synchronized void poke() {
      poked = true;
      Retransmitter.this.notify();
    }

    /**
     * Retransmit messages that have not been acknowledged. Iterate
     * through all the agents for which we have AgentInfo and
     * interate through all the outstandmessage that have been sent to
     * that agent. Check the time to retransmit of the message and if
     * the current time has passed that time, then retransmit the
     * message. Keep the earliest time of any message that is not ready
     * to be retransmitted and sleep long enough so that there could be
     * at least one message to retransmit when we awaken.
     */
    @Override
   public void run() {
      while (true) {
        try {
          long now = System.currentTimeMillis();
          long earliestTime = now + retransmitSchedule[0];
          synchronized (MessageManagerImpl.this) {
            for (Iterator agents = agentInfo.values().iterator();
                 agents.hasNext(); )
              {
                AgentInfo info = (AgentInfo) agents.next();
                TimestampedMessage tsm = info.getFirstOutstandingMessage();
                if (tsm == null) continue;
                if (tsm.timestamp <= now) {
                  TimestampedMessage[] messages = info.getOutstandingMessages();
                  info.setTransmissionTime(now);
                  messagesToRetransmit.addAll(java.util.Arrays.asList(messages));
                } else if (tsm.timestamp < earliestTime) {
                  earliestTime = tsm.timestamp;
                }
              }
          }
          if (!messagesToRetransmit.isEmpty()) {
            for (Iterator iter = messagesToRetransmit.iterator(); iter.hasNext(); ) {
              TimestampedMessage tsm = (TimestampedMessage) iter.next();
              tsm.send(now);
              if (tsm.timestamp < earliestTime) {
                earliestTime = tsm.timestamp;
              }
              if (debug) printMessage(tsm.nTries == 1 ? "Send" : ("Rxm" + tsm.nTries), tsm);
            }
            messagesToRetransmit.clear();
          }
          synchronized (Retransmitter.this) {
            if (!poked) {
              long sleepTime = 5000L + earliestTime - now;
              if (sleepTime > 30000L) sleepTime = 30000L;
              Retransmitter.this.wait(sleepTime);
            }
            poked = false;
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /** Serialize ourselves. Used for persistence. */
  private void writeObject(ObjectOutputStream os) throws IOException {
    synchronized (this) {
      if (stuffToSend.size() > 0) {
        throw new IOException("Non-empty stuffToSend");
      }
      os.defaultWriteObject();
    }
  }

  private void readObject(ObjectInputStream is)
    throws IOException, ClassNotFoundException
  {
    is.defaultReadObject();
    stuffToSend = new ArrayList();
    needAdvanceEpoch = false;
//      for (Iterator agents = agentInfo.values().iterator(); agents.hasNext(); ) {
//        AgentInfo info = (AgentInfo) agents.next();
//        info.setRestarted(true);
//      }
  }
}
