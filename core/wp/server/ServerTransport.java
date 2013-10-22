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

package org.cougaar.core.wp.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceAvailableEvent;
import org.cougaar.core.component.ServiceAvailableListener;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.core.wp.Parameters;
import org.cougaar.core.wp.WhitePagesMessage;
import org.cougaar.core.wp.bootstrap.PeersService;
import org.cougaar.core.wp.resolver.ServiceProviderBase;
import org.cougaar.core.wp.resolver.WPAnswer;
import org.cougaar.core.wp.resolver.WPQuery;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.RarelyModifiedList;

/**
 * This component sends and receives messages for the {@link
 * RootAuthority}.
 * <p>
 * This component is responsible for the server-side hierarchy
 * traversal and replication.
 */
public class ServerTransport
extends GenericStateModelAdapter
implements Component
{

  /**
   * Should timestamps be relative to the server's clock or
   * the client's measured round-trip-time?
   *
   * @see WPAnswer
   */
  private final boolean USE_SERVER_TIME =
    SystemProperties.getBoolean(
        "org.cougaar.core.wp.server.useServerTime");

  // pick an action that doesn't conflict with WPQuery
  private static final int FORWARD_ANSWER = 4;

  private ServerTransportConfig config;

  private ServiceBroker sb;
  private LoggingService logger;
  private MessageAddress agentId;
  private ThreadService threadService;
  private WhitePagesService wps;

  private PeersService peersService;
  private final PeersService.Client
    peersClient =
    new PeersService.Client() {
      public void add(MessageAddress addr) {
        ServerTransport.this.addPeer(addr);
      }
      public void addAll(Set s) {
        for (Iterator iter = s.iterator(); iter.hasNext(); ) {
          add((MessageAddress) iter.next());
        }
      }
      public void remove(MessageAddress addr) {
        ServerTransport.this.removePeer(addr);
      }
      public void removeAll(Set s) {
        for (Iterator iter = s.iterator(); iter.hasNext(); ) {
          remove((MessageAddress) iter.next());
        }
      }
    };

  private PingAckSP pingAckSP;
  private LookupAckSP lookupAckSP;
  private ModifyAckSP modifyAckSP;
  private ForwardAckSP forwardAckSP;
  private ForwardSP forwardSP;

  private RarelyModifiedList pingAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList lookupAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList modifyAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList forwardAckClients = 
    new RarelyModifiedList();
  private RarelyModifiedList forwardClients = 
    new RarelyModifiedList();

  //
  // peer servers
  //

  private final Object peersLock = new Object();
  private Set peers = Collections.EMPTY_SET;

  //
  // output (send to WP server):
  //

  private final Object sendLock = new Object();

  private MessageSwitchService messageSwitchService;

  // this is our startup grace-time on message timeouts, which is
  // based upon the time we obtained our messageSwitchService plus
  // the configuration's "graceMillis".
  //
  // this is used to allow more delivery time when the system is
  // starting, since unusual costs usually occur (e.g. cryto
  // handshaking).
  private long graceTime;

  // messages queued until the messageSwitchService is available
  //
  // List<WhitePagesMessage> 
  private List sendQueue;

  //
  // input (receive from WP server):
  //

  private Schedulable receiveThread;

  // received messages
  //
  // List<WhitePagesMessage>
  private final List receiveQueue = new ArrayList();

  // temporary list for use within "receiveNow()"
  //
  // List<Object>
  private final List receiveTmp = new ArrayList();

  //
  // debug queues:
  //

  private Schedulable debugThread;

  public void setParameter(Object o) {
    configure(o);
  }

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new ServerTransportConfig(o);
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  @Override
public void load() {
    super.load();

    configure(null);

    if (logger.isDebugEnabled()) {
      logger.debug("Loading server remote handler");
    }

    // which agent are we in?
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    // watch for peer servers
    peersService = sb.getService(
       peersClient,
       PeersService.class,
       null);
    if (peersService == null) {
      throw new RuntimeException(
          "Unable to obtain PeersService");
    }

    // create threads
    Runnable receiveRunner =
      new Runnable() {
        public void run() {
          // assert (thread == receiveThread);
          receiveNow();
        }
      };
    receiveThread = threadService.getThread(
        this,
        receiveRunner,
        "White pages server handle incoming responses");

    if (0 < config.debugQueuesPeriod &&
        logger.isDebugEnabled()) {
      Runnable debugRunner =
        new Runnable() {
          public void run() {
            // assert (thread == debugThread);
            debugQueues();
          }
        };
      debugThread = threadService.getThread(
          this,
          debugRunner,
          "White pages server handle outgoing requests");
      debugThread.start();
    }

    // tell the WP that we're a server
    bindServerFlag(true); 

    // register our message switch (now or later)
    if (sb.hasService(MessageSwitchService.class)) {
      registerMessageSwitch();
    } else {
      ServiceAvailableListener sal =
        new ServiceAvailableListener() {
          public void serviceAvailable(ServiceAvailableEvent ae) {
            Class cl = ae.getService();
            if (MessageSwitchService.class.isAssignableFrom(cl)) {
              registerMessageSwitch();
            }
          }
        };
      sb.addServiceListener(sal);
    }

    // advertise our services
    pingAckSP = new PingAckSP();
    sb.addService(PingAckService.class, pingAckSP);
    lookupAckSP = new LookupAckSP();
    sb.addService(LookupAckService.class, lookupAckSP);
    modifyAckSP = new ModifyAckSP();
    sb.addService(ModifyAckService.class, modifyAckSP);
    forwardAckSP = new ForwardAckSP();
    sb.addService(ForwardAckService.class, forwardAckSP);
    forwardSP = new ForwardSP();
    sb.addService(ForwardService.class, forwardSP);
  }

  @Override
public void unload() {
    if (forwardSP != null) {
      sb.revokeService(ForwardService.class, forwardSP);
      forwardSP = null;
    }
    if (forwardAckSP != null) {
      sb.revokeService(ForwardAckService.class, forwardAckSP);
      forwardAckSP = null;
    }
    if (modifyAckSP != null) {
      sb.revokeService(ModifyAckService.class, modifyAckSP);
      modifyAckSP = null;
    }
    if (lookupAckSP != null) {
      sb.revokeService(LookupAckService.class, lookupAckSP);
      lookupAckSP = null;
    }
    if (pingAckSP != null) {
      sb.revokeService(PingAckService.class, pingAckSP);
      pingAckSP = null;
    }

    if (messageSwitchService != null) {
      //messageSwitchService.removeMessageHandler(myMessageHandler);
      sb.releaseService(
          this, MessageSwitchService.class, messageSwitchService);
      messageSwitchService = null;
    }

    bindServerFlag(false); 

    if (peersService != null) {
      sb.releaseService(
          peersClient,
          PeersService.class,
          peersService);
      peersService = null;
    }

    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
  }

  private void bindServerFlag(boolean bind) {
    AddressEntry entry = 
      AddressEntry.getAddressEntry(
          agentId.getAddress(),
          "server",
          URI.create("server:///true"));
    // should really pay attention
    final LoggingService ls = logger;
    Callback callback = new Callback() {
      public void execute(Response res) {
        if (res.isSuccess()) {
          if (ls.isInfoEnabled()) {
            ls.info("WP Response: "+res);
          }
        } else {
          ls.error("WP Error: "+res);
        }
      }
    };
    if (bind) {
      wps.rebind(entry, callback);
    } else {
      wps.unbind(entry, callback);
    }
  }

  private void addPeer(MessageAddress addr) {
    updatePeer(true, addr);
  }
  private void removePeer(MessageAddress addr) {
    updatePeer(false, addr);
  }
  private void updatePeer(boolean add, MessageAddress addr) {
    if (addr == null) {
      return;
    }
    synchronized (peersLock) {
      MessageAddress a = addr.getPrimary();
      if (add == peers.contains(a)) {
        if (logger.isInfoEnabled()) {
          logger.info(
              "Ignoring "+(add ? "add" : "remove")+" of peer "+a+
              " that is "+(add ? "already" : "not")+
              " in our peers["+peers.size()+"]="+peers);
        }
        return;
      }
      // copy-on-write
      Set np = new HashSet(peers); 
      if (add) {
        np.add(a);
      } else {
        np.remove(a);
      }
      peers = Collections.unmodifiableSet(np);
      if (logger.isInfoEnabled()) {
        logger.info(
            (add ? "Added" : "Removed")+
            " peer server "+a+" "+
            (add ? "to" : "from")+
            " peers["+peers.size()+"]="+peers);
      }
    }
    // TODO on "add" we should forward old messages within the
    // expire ttd, but this would require help from the server
    // tables.  For now we'll ignore this case and let the next
    // "forward" take care of new peers.
  }
  private Set getPeers() {
    synchronized (peersLock) {
      return peers;
    }
  }
  private boolean isPeer(MessageAddress addr) {
    if (addr == null) {
      return false;
    }
    MessageAddress a = addr.getPrimary();
    if (agentId.equals(a)) {
      return false;
    }
    return getPeers().contains(a);
  }

  private void registerMessageSwitch() {
    // service broker now has the MessageSwitchService
    //
    // should we do this in a separate thread?
    if (messageSwitchService != null) {
      if (logger.isErrorEnabled()) {
        logger.error("Already obtained our message switch");
      }
      return;
    }
    MessageSwitchService mss = sb.getService(this, MessageSwitchService.class, null);
    if (mss == null) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to obtain MessageSwitchService");
      }
      return;
    }
    MessageHandler myMessageHandler =
      new MessageHandler() {
        public boolean handleMessage(Message m) {
          return receive(m);
        }
      };
    mss.addMessageHandler(myMessageHandler);
    if (logger.isInfoEnabled()) {
      logger.info("Registered server message handler");
    }
    synchronized (sendLock) {
      this.messageSwitchService = mss;
      if (0 <= config.graceMillis) {
        this.graceTime = 
          System.currentTimeMillis() + config.graceMillis;
      }
      if (sendQueue != null) {
        // send queued messages
        //
        Runnable flushSendQueueRunner =
          new Runnable() {
            public void run() {
              synchronized (sendLock) {
                flushSendQueue();
              }
            }
          };
        Schedulable flushSendQueueThread = 
          threadService.getThread(
              this,
              flushSendQueueRunner,
              "White pages server flush queued output messages");
        flushSendQueueThread.start();
        // this may race with the normal message-send code,
        // so we also check the sendQueue there.  This means
        // that the above "flushSendQueue()" call may find a
        // null sendQueue by the time it is run.
      }
    }
  }

  private List getList(int action) {
    return
      (action == WPAnswer.LOOKUP ? lookupAckClients :
       action == WPAnswer.MODIFY ? modifyAckClients :
       action == WPAnswer.FORWARD ? forwardAckClients :
       action == WPAnswer.PING ? pingAckClients :
       action == FORWARD_ANSWER ? forwardClients :
       null);
  }
  private void register(int action, Object c) {
    getList(action).add(c);
  }
  private void unregister(int action, Object c) {
    getList(action).remove(c);
  }
  private void tellClients(
      int action,
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    // tell our clients (refactor me?)
    int n = (m == null ? 0 : m.size());
    if (n == 0 && action != WPAnswer.PING) {
      return;
    }
    if (action == WPAnswer.LOOKUP) {
      List l = lookupAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        LookupAckService.Client c = (LookupAckService.Client) l.get(i);
        c.lookup(clientAddr, clientTime, m);
      }
    } else if (action == WPAnswer.MODIFY) {
      List l = modifyAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ModifyAckService.Client c = (ModifyAckService.Client) l.get(i);
        c.modify(clientAddr, clientTime, m);
      }
    } else if (action == WPAnswer.FORWARD) {
      List l = forwardAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ForwardAckService.Client c = (ForwardAckService.Client) l.get(i);
        c.forward(clientAddr, clientTime, m);
      }
    } else if (action == WPAnswer.PING) {
      List l = pingAckClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        PingAckService.Client c = (PingAckService.Client) l.get(i);
        c.ping(clientAddr, clientTime, m);
      }
    } else if (action == FORWARD_ANSWER) {
      List l = forwardClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ForwardService.Client c = (ForwardService.Client) l.get(i);
        c.forwardAnswer(clientAddr, clientTime, m);
      }
    } else if (logger.isErrorEnabled()) {
      logger.error("Unknown action "+action);
    }
  }

  private void send(
      int action,
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    if ((m == null || m.isEmpty()) &&
        (action != WPAnswer.PING)) {
      return;
    }
    if (action == WPAnswer.FORWARD && !isPeer(clientAddr)) {
      // ignore, either the local server or a non-peer
      return;
    }

    long now = System.currentTimeMillis();

    long timeout =
      (action == WPAnswer.LOOKUP ? config.lookupTimeoutMillis :
       action == WPAnswer.PING ? config.pingTimeoutMillis :
       config.modifyTimeoutMillis);
    if (0 < timeout && 0 < graceTime) {
      long diff = graceTime - now;
      if (0 < diff && timeout < diff) {
        timeout = diff;
      }
    }
    long deadline = now + timeout;

    // tag with optional timeout attribute
    MessageAddress target = 
      MessageTimeoutUtils.setDeadline(
          clientAddr,
          deadline);

    WPAnswer wpa = new WPAnswer(
        agentId,
        target,
        clientTime,
        now,
        USE_SERVER_TIME,
        action,
        m);

    sendOrQueue(wpa);
  }

  private void forward(Map m, long ttd) {
    Set targets = getPeers();
    int n = targets.size();
    if (logger.isDetailEnabled()) {
      logger.detail(
          "forwarding "+m+" to all peers["+n+"]="+targets+
          " except ourselves("+agentId+")");
    }
    long now = System.currentTimeMillis();
    long ttl = now + ttd;
    Iterator iter = targets.iterator();
    for (int i = 0; i < n; i++) {
      MessageAddress target = (MessageAddress) iter.next();
      if (agentId.equals(target.getPrimary())) {
        // exclude the local server
        continue;
      }
      // send to this target
      target = MessageTimeoutUtils.setDeadline(target, ttl);
      WPQuery wpq = new WPQuery(
        agentId,
        target,
        now,
        WPQuery.FORWARD,
        m);
      sendOrQueue(wpq);
    }
  }
  
  private void forward(MessageAddress addr, Map m, long ttd) {
    if (!isPeer(addr)) {
      // ignore, either the local server or a non-peer
      return;
    }
    // send to this target
    long now = System.currentTimeMillis();
    long deadline = now + ttd;
    MessageAddress target =
      MessageTimeoutUtils.setDeadline(addr, deadline);
    WPQuery wpq = new WPQuery(
        agentId,
        target,
        now,
        WPQuery.FORWARD,
        m);
    sendOrQueue(wpq);
  }

  private void sendOrQueue(WhitePagesMessage m) {
    synchronized (sendLock) {
      if (messageSwitchService == null) {
        // queue to send once the MTS is up
        if (sendQueue == null) {
          sendQueue = new ArrayList();
        }
        sendQueue.add(m);
        return;
      } else if (sendQueue != null) {
        // flush pending messages
        flushSendQueue();
      } else {
        // typical case
      }
      send(m);
    }
  }

  private void send(WhitePagesMessage m) {
    // assert (Thread.holdsLock(sendLock));
    // assert (messageSwitchService != null);
    if (logger.isDetailEnabled()) {
      logger.detail("sending message: "+m);
    }
    messageSwitchService.sendMessage(m);
  }

  private void flushSendQueue() {
    // assert (Thread.holdsLock(sendLock));
    // assert (messageSwitchService != null);
    List l = sendQueue;
    sendQueue = null;
    int n = (l == null ? 0 : l.size());
    if (n != 0) {
      // must drain in reverse order, since we appended
      // to the end.
      for (int i = n-1; 0 <= i; i--) {
        WhitePagesMessage m = (WhitePagesMessage) l.get(i);
        send(m);
      }
    }
  }
  
  private void receiveNow(WhitePagesMessage wpm) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+wpm);
    }

    MessageAddress clientAddr = wpm.getOriginator();

    Map m;
    long clientTime;
    int action;
    if (wpm instanceof WPQuery) {
      WPQuery wpq = (WPQuery) wpm;
      m = wpq.getMap();
      clientTime = wpq.getSendTime();
      action = wpq.getAction();
    } else {
      WPAnswer wpa = (WPAnswer) wpm;
      m = wpa.getMap();
      clientTime = wpa.getReplyTime();
      action = FORWARD_ANSWER;
    }

    tellClients(action, clientAddr, clientTime, m);
  }

  //
  // message receive queue
  //

  private boolean receive(Message m) {
    if (m instanceof WPQuery) {
      // match
    } else if (m instanceof WPAnswer) {
      if (((WPAnswer) m).getAction() != WPAnswer.FORWARD) {
        return false;
      }
    } else {
      return false;
    }
    WhitePagesMessage wpm = (WhitePagesMessage) m;
    receiveLater(wpm);
    return true;
  }

  private void receiveLater(WhitePagesMessage m) {
    // queue to run in our thread
    synchronized (receiveQueue) {
      receiveQueue.add(m);
    }
    receiveThread.start();
  }

  private void receiveNow() {
    synchronized (receiveQueue) {
      if (receiveQueue.isEmpty()) {
        if (logger.isDetailEnabled()) {
          logger.detail("input queue is empty");
        }
        return;
      }
      receiveTmp.addAll(receiveQueue);
      receiveQueue.clear();
    }
    // receive messages
    for (int i = 0, n = receiveTmp.size(); i < n; i++) {
      WhitePagesMessage m = (WhitePagesMessage) receiveTmp.get(i);
      receiveNow(m);
    }
    receiveTmp.clear();
  }

  private void debugQueues() {
    if (!logger.isDebugEnabled()) {
      return;
    }

    synchronized (receiveQueue) {
      String s = "";
      s += "\n##### server transport input queue ################";
      int n = receiveQueue.size();
      s += "\nreceive["+n+"]: ";
      for (int i = 0; i < n; i++) {
        WhitePagesMessage m = (WhitePagesMessage) receiveQueue.get(i);
        s += "\n   "+m;
      }
      s += "\n###################################################";
      logger.debug(s);
    }

    synchronized (sendLock) {
      String s = "";
      s += "\n##### server transport output queue ###############";
      s += "\nmessageSwitchService="+messageSwitchService;
      int n = (sendQueue == null ? 0 : sendQueue.size());
      s += "\nsendQueue["+n+"]: "+sendQueue;
      s += "\n###################################################";
      logger.debug(s);
    }

    // run me again later
    debugThread.schedule(config.debugQueuesPeriod);
  }

  private abstract class SPBase extends ServiceProviderBase {
    protected abstract int getAction();
    @Override
   protected void register(Object client) {
      ServerTransport.this.register(getAction(), client);
    }
    @Override
   protected void unregister(Object client) {
      ServerTransport.this.unregister(getAction(), client);
    }
  }

  private class PingAckSP extends SPBase {
    @Override
   protected int getAction() { return WPAnswer.PING; }
    @Override
   protected Class getServiceClass() { return PingAckService.class; }
    @Override
   protected Class getClientClass() { return PingAckService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements PingAckService {
      public SI(Object client) { super(client); }
      public void pingAnswer(MessageAddress clientAddr, long clientTime, Map m) {
        ServerTransport.this.send(WPAnswer.PING, clientAddr, clientTime, m);
      }
    }
  }
  private class LookupAckSP extends SPBase {
    @Override
   protected int getAction() { return WPAnswer.LOOKUP; }
    @Override
   protected Class getServiceClass() { return LookupAckService.class; }
    @Override
   protected Class getClientClass() { return LookupAckService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements LookupAckService {
      public SI(Object client) { super(client); }
      public void lookupAnswer(MessageAddress clientAddr, long clientTime, Map m) {
        ServerTransport.this.send(WPAnswer.LOOKUP, clientAddr, clientTime, m);
      }
    }
  }
  private class ModifyAckSP extends SPBase {
    @Override
   protected int getAction() { return WPAnswer.MODIFY; }
    @Override
   protected Class getServiceClass() { return ModifyAckService.class; }
    @Override
   protected Class getClientClass() { return ModifyAckService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements ModifyAckService {
      public SI(Object client) { super(client); }
      public void modifyAnswer(MessageAddress clientAddr, long clientTime, Map m) {
        ServerTransport.this.send(WPAnswer.MODIFY, clientAddr, clientTime, m);
      }
    }
  }
  private class ForwardAckSP extends SPBase {
    @Override
   protected int getAction() { return WPAnswer.FORWARD; }
    @Override
   protected Class getServiceClass() { return ForwardAckService.class; }
    @Override
   protected Class getClientClass() { return ForwardAckService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements ForwardAckService {
      public SI(Object client) { super(client); }
      public void forwardAnswer(MessageAddress clientAddr, long clientTime, Map m) {
        ServerTransport.this.send(WPAnswer.FORWARD, clientAddr, clientTime, m);
      }
    }
  }
  private class ForwardSP extends SPBase {
    @Override
   protected int getAction() { return FORWARD_ANSWER; }
    @Override
   protected Class getServiceClass() { return ForwardService.class; }
    @Override
   protected Class getClientClass() { return ForwardService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements ForwardService {
      public SI(Object client) { super(client); }
      public void forward(Map m, long ttd) {
        ServerTransport.this.forward(m, ttd);
      }
      public void forward(MessageAddress target, Map m, long ttd) {
        ServerTransport.this.forward(target, m, ttd);
      }
    }
  }

  /** config options */
  private static class ServerTransportConfig {
    public final long debugQueuesPeriod;
    public final long graceMillis;
    // these should match the server TTLs
    public final long lookupTimeoutMillis;
    public final long modifyTimeoutMillis;
    public final long pingTimeoutMillis;
    public ServerTransportConfig(Object o) {
      Parameters p = 
        new Parameters(o, "org.cougaar.core.wp.server.");
      debugQueuesPeriod = p.getLong("debugQueuesPeriod", 30000);
      graceMillis = p.getLong("graceMillis", 0);
      lookupTimeoutMillis = p.getLong("lookupTimeoutMillis", 90000);
      modifyTimeoutMillis = p.getLong("modifyTimeoutMillis", 90000);
      pingTimeoutMillis = p.getLong("pingTimeoutMillis", 90000);
    }
  }
}
