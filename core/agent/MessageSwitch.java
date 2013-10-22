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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.GroupMessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.wp.WhitePagesMessage;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.log.Logging;

/**
 * This component registers the agent in the
 * {@link MessageTransportService} and forwards all received
 * messages through the {@link MessageSwitchService} to the
 * agent-internal message handlers.
 *
 * @property org.cougaar.core.agent.showTraffic
 *   If <em>true</em>, shows '+' and '-' on message sends and receives
 *   except for white pages messages.  If <em>wp</em>, then shows
 *   both the above +/- for regular send/receive and W/w for white
 *   pages send/receive.
 *
 * @property org.cougaar.core.agent.quiet
 *   Makes standard output as quiet as possible.
 */
public final class MessageSwitch 
extends GenericStateModelAdapter
implements Component
{

  // maybe move to parameters
  private static final boolean showTraffic;
  private static final boolean showWhitePagesTraffic;
  static {
    boolean isQuiet = SystemProperties.getBoolean("org.cougaar.core.agent.quiet");
    if (isQuiet) {
      showTraffic = false;
      showWhitePagesTraffic = false;
    } else {
      String trafficParam =
        SystemProperties.getProperty("org.cougaar.core.agent.showTraffic", "true");
      if ("true".equalsIgnoreCase(trafficParam)) {
        showTraffic = true;
        showWhitePagesTraffic = false;
      } else if ("wp".equalsIgnoreCase(trafficParam)) {
        showTraffic = true;
        showWhitePagesTraffic = true;
      } else {
        showTraffic = false;
        showWhitePagesTraffic = false;
      }
    }
  }

  private ServiceBroker sb;

  private LoggingService log;
  private MessageTransportService messenger;
  private ReconcileAddressWatcherService raws;

  private PersistenceService ps;
  private PersistenceClient pc;

  private MessageSwitchService mss;

  private MessageSwitchUnpendServiceProvider msusp;

  private MessageSwitchShutdownServiceProvider msssp;

  private MessageAddress localAgent;
  private long localIncarnation;

  private MessageTransportClient mtsClientAdapter;

  private MessageSwitchImpl msi;
  private MessageSwitchServiceProvider mssp;

  // state from "suspend()", which is copied into the mobility state
  private List unsentMessages;
  private AgentState mtsState;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);

    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    raws = sb.getService(this, ReconcileAddressWatcherService.class, null);
    if (raws == null) {
      throw new RuntimeException(
          "Unable to obtain ReconcileAddressWatcherService");
    }

    register_persistence();
    Object o = rehydrate();
    if (o instanceof AgentMessageTransportState) {
      // fill in prior (mobile) identity
      AgentMessageTransportState amts = 
        (AgentMessageTransportState) o;
      unsentMessages = amts.unsentMessages;
      mtsState = amts.mtsState;
    }
    o = null;

    find_local_incarnation();
    load_create_message_switch();
    load_internal_register_with_mts();
    resume_resend_unsent_messages();
    load_add_message_switch_service();
    load_add_outgoing_traffic_watcher();

    msusp = new MessageSwitchUnpendServiceProvider();
    sb.addService(MessageSwitchUnpendService.class, msusp);

    msssp = new MessageSwitchShutdownServiceProvider();
    sb.addService(MessageSwitchShutdownService.class, msssp);

    // called later via MessageSwitchUnpendService:
    //load_unpend_messages();
  }

  @Override
public void suspend() {
    super.suspend();
    // called earlier via MessageSwitchShutdownService:
    //suspend_unregister_from_mts();
    suspend_shutdown_mts();
  }

  @Override
public void resume() {
    super.resume();
    resume_register_with_mts();
    // called later via MessageSwitchShutdownService:
    //resume_resend_unsent_messages();
  }

  @Override
public void unload() {
    super.unload();

    if (msssp != null) {
      sb.revokeService(MessageSwitchShutdownService.class, msssp);
      msssp = null;
    }
    if (msusp != null) {
      sb.revokeService(MessageSwitchUnpendService.class, msusp);
      msusp = null;
    }

    unload_remove_outgoing_traffic_watcher();
    unload_revoke_message_switch_service();

    unregister_persistence();

    if (raws != null) {
      sb.releaseService(
          this, ReconcileAddressWatcherService.class, raws);
      raws = null;
    }
  }

  private void find_local_incarnation() {
    // get the local agent's incarnation number from the
    // TopologyService
    if (log.isInfoEnabled()) {
      log.info("Finding the local agent's incarnation");
    }

    TopologyService ts = sb.getService(this, TopologyService.class, null);
    if (ts != null) {
      localIncarnation = ts.getIncarnationNumber();
      sb.releaseService(
          this, TopologyService.class, ts);
    }
  }

  private void load_create_message_switch() {
    msi = new MessageSwitchImpl(log);
  }

  private void load_internal_register_with_mts() {
    // register with the message transport
    if (log.isInfoEnabled()) {
      log.info("Registering with the message transport");
    }

    mtsClientAdapter =
      new MessageTransportClient() {
        public void receiveMessage(Message message) {
          receive(message);
        }
        public MessageAddress getMessageAddress() {
          return localAgent;
        }
        public long getIncarnationNumber() {
          return localIncarnation;
        }
      };

    messenger = sb.getService(mtsClientAdapter, MessageTransportService.class, null);
    if (messenger == null) {
      throw new RuntimeException(
          "Unable to obtain MessageTransportService");
    }

    if (mtsState != null) {
      messenger.getAgentState().mergeAttributes(mtsState);
      mtsState = null;
    }

    messenger.registerClient(mtsClientAdapter);
  }

  private void load_add_message_switch_service() {
    mssp = new MessageSwitchServiceProvider();
    sb.addService(MessageSwitchService.class, mssp);
  }

  private void load_add_outgoing_traffic_watcher() {
    if (!showTraffic) {
      return;
    }

    mss = sb.getService(this, MessageSwitchService.class, null);

    // register message handler to observe all incoming messages
    MessageHandler mh = new MessageHandler() {
      public boolean handleMessage(Message message) {
        if (message instanceof WhitePagesMessage) {
          if (showWhitePagesTraffic) {
            Logging.printDot("w");
          }
        } else {
          Logging.printDot("-");
        }
        return false;         // don't ever consume it
      }
    };
    mss.addMessageHandler(mh);
  }

  private Object captureState() {
    if (getModelState() == ACTIVE) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring persist while active");
      }
      return null;
    }

    AgentMessageTransportState amts = 
      new AgentMessageTransportState();
    amts.unsentMessages = unsentMessages;
    amts.mtsState = mtsState;
    return amts;
  }

  private void register_persistence() {
    // get persistence
    pc = 
      new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          String id = getClass().getName();
          return new PersistenceIdentity(id);
        }
        public List getPersistenceData() {
          Object o = captureState();
          // must return mutable list!
          List l = new ArrayList(1);
          l.add(o);
          return l;
        }
      };
    ps = 
      sb.getService(
          pc, PersistenceService.class, null);
  }

  private void unregister_persistence() {
    if (ps != null) {
      sb.releaseService(
          pc, PersistenceService.class, ps);
      ps = null;
      pc = null;
    }
  }

  private Object rehydrate() {
    RehydrationData rd = ps.getRehydrationData();
    if (rd == null) {
      if (log.isInfoEnabled()) {
        log.info("No rehydration data found");
      }
      return null;
    }

    List l = rd.getObjects();
    rd = null;
    int lsize = (l == null ? 0 : l.size());
    if (lsize < 1) {
      if (log.isInfoEnabled()) {
        log.info("Invalid rehydration list? "+l);
      }
      return null;
    }
    Object o = l.get(0);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("Null rehydration state?");
      }
      return null;
    }

    if (log.isInfoEnabled()) {
      log.info("Found rehydrated state");
      if (log.isDetailEnabled()) {
        log.detail("state is "+o);
      }
    }

    return o;
  }

  private void load_unpend_messages() {
    msi.unpendMessages();
  }

  private void suspend_unregister_from_mts() {
    // shutdown the MTS
    if (log.isInfoEnabled()) {
      log.info("Shutting down message transport");
    }

    if (messenger != null) {
      messenger.unregisterClient(mtsClientAdapter);
    }
  }

  private void suspend_shutdown_mts() {
    if (messenger == null) {
      unsentMessages = null;
      mtsState = null;
      return;
    }

    // flush outgoing messages, block further input.
    // get a list of unsent messages.
    unsentMessages = messenger.flushMessages();

    // get MTS-internal state for this agent
    mtsState = messenger.getAgentState();

    // release messenger, remove agent name-server entry.
    sb.releaseService(
        mtsClientAdapter, MessageTransportService.class, messenger);
    messenger = null;
  }

  private void resume_register_with_mts() {
    // re-register MTS
    if (messenger == null) {
      if (log.isInfoEnabled()) {
        log.info(
            "Registering with the message transport service");
      }
      messenger = sb.getService(
            mtsClientAdapter, MessageTransportService.class, null);

      if (mtsState != null) {
        messenger.getAgentState().mergeAttributes(mtsState);
        mtsState = null;
      }

      messenger.registerClient(mtsClientAdapter);
    }

    if (log.isInfoEnabled()) {
      log.info("Resuming message transport");
    }
  }

  private void resume_resend_unsent_messages() {
    // send all unsent messages
    List l = unsentMessages;
    unsentMessages = null;
    int n = (l == null ? 0 : l.size());
    for (int i = 0; i < n; i++) {
      Message cmi = (Message) l.get(i);
      send(cmi);
    }
  }

  private void unload_remove_outgoing_traffic_watcher() {
    if (!showTraffic) {
      return;
    }

    if (mss != null) {
      // mss.unregister?
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }
  }

  private void unload_revoke_message_switch_service() {
    sb.revokeService(MessageSwitchService.class, mssp);
    mssp = null;
  }

  private void recordSend(MessageAddress addr) {
    raws.sentMessageTo(addr);
  }
  private void recordReceive(MessageAddress addr) {
    raws.receivedMessageFrom(addr);
  }

  private void send(Message message) {
    if (message instanceof ClusterMessage) {
      recordSend(message.getTarget());
    }

    if (showTraffic) {
      if (message instanceof WhitePagesMessage) {
        if (showWhitePagesTraffic) {
          Logging.printDot("W");
        }
      } else {
        Logging.printDot("+");
      }
    }
    try {
      if (messenger == null) {
        if (log.isWarnEnabled()) {
          log.warn("MessageTransport unavailable, dropped: "+message);
        }
        return;
      }
      messenger.sendMessage(message);
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("Problem sending message", ex);
      }
    }
  }

  private void receive(Message message) {
    if (message instanceof ClusterMessage) {
      recordReceive(message.getTarget());
    }

    if (message.getTarget().equals(MessageAddress.MULTICAST_SOCIETY)) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Ignoring message received by "+localAgent+
            " with target MULTICAST_SOCIETY");
      }
      return;
    }

    try {
      // messageHandler will now pend and warn about unhandled messages
      msi.handleMessage(message);
    } catch (Exception e) {
      log.error(
          "Uncaught Exception while handling Message ("+
          message.getClass()+"): "+message,
          e);
    }
  }

  private class MessageSwitchServiceProvider implements ServiceProvider {
    private final MessageSwitchService mss;
    public MessageSwitchServiceProvider() {
      mss =
        new MessageSwitchService() {
          public void sendMessage(Message m) {
            send(m);
          }
          public void addMessageHandler(MessageHandler mh) {
            msi.addMessageHandler(mh);
          }
          public MessageAddress getMessageAddress() {
            return localAgent;
          }
          
          public void joinGroup(GroupMessageAddress address) {
            messenger.joinGroup(mtsClientAdapter, address);
          }
          public void leaveGroup(GroupMessageAddress address) {
            messenger.leaveGroup(mtsClientAdapter, address);
          }
        };
    }
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (MessageSwitchService.class.isAssignableFrom(serviceClass)) {
        return mss;
      } else {
        return null;
      }
    }
    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service) {
    }
  }

  private final class MessageSwitchUnpendServiceProvider
    implements ServiceProvider {
      private final MessageSwitchUnpendService msus;
      public MessageSwitchUnpendServiceProvider() {
        msus = new MessageSwitchUnpendService() {
          public void unpendMessages() {
            load_unpend_messages();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (MessageSwitchUnpendService.class.isAssignableFrom(serviceClass)) {
          return msus;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }

  private final class MessageSwitchShutdownServiceProvider
    implements ServiceProvider {
      private final MessageSwitchShutdownService msss;
      public MessageSwitchShutdownServiceProvider() {
        msss = new MessageSwitchShutdownService() {
          public void shutdown() {
            suspend_unregister_from_mts();
          }
          public void restore() {
            resume_resend_unsent_messages();
          }
        };
      }
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (MessageSwitchShutdownService.class.isAssignableFrom(serviceClass)) {
          return msss;
        } else {
          return null;
        }
      }
      public void releaseService(
          ServiceBroker sb, Object requestor, 
          Class serviceClass, Object service) {
      }
    }
  private static final class AgentMessageTransportState implements java.io.Serializable {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public List unsentMessages;
    public AgentState mtsState;
  }
  /**
   * MessageSwitchImpl is a MessageHandler which calls an ordered 
   * list of other MessageHandler instances in order until 
   * one returns a true value from handle.
   */
  private static final class MessageSwitchImpl implements MessageHandler {
    private final LoggingService log;
    /** List of MessageHandler instances */
    private final List handlers = new ArrayList(11);
    /** list of pending (unhandled) messages - protected by lock on handlers. */
    private List pendingMessages = new ArrayList(11);

    public MessageSwitchImpl(LoggingService log) {
      this.log = log;
    }

    public boolean handleMessage(Message m) {
      synchronized (handlers) {
        for (int i=0, l=handlers.size(); i<l; i++) {
          MessageHandler h = (MessageHandler) handlers.get(i);
          if (h.handleMessage(m)) return true;
        }
        pendMessage(m);
      }
      return false;
    }

    public void addMessageHandler(MessageHandler mh) {
      synchronized (handlers) {
        handlers.add(mh);
        resubmitPendingMessages(mh);
      }
    }
    @SuppressWarnings("unused")
   public void removeMessageHandler(MessageHandler mh) {
      synchronized (handlers) {
        handlers.remove(mh);
      }
    }

    // must be called within synchronized(handlers), e.g. only from addMessageHandler
    private void resubmitPendingMessages(MessageHandler mh) {
      if (pendingMessages == null) {
        return;
      }
      for (Iterator it = pendingMessages.iterator();
          it.hasNext();
          ) {
        Message m = (Message) it.next();
        try {
          boolean handled = mh.handleMessage(m);
          if (handled) {
            if (log.isInfoEnabled()) {
              log.info("Handled previously unhandled Message ("+
                  m.getClass()+"): "+m);
            }
            it.remove();
          } else {
            // probably not worth the effort...
            if (log.isDebugEnabled()) {
              log.debug("Still not handling pending message "+
                  m+" with handler "+mh);
            }
          }                
        } catch (Exception e) {
          log.error(
              "Uncaught Exception while resubmitting pending Message ("+
              m.getClass()+"): "+m, e);
        }
      }
    }

    // must be called within synchronized(handlers)
    private void pendMessage(Message m) {
      if (pendingMessages == null) {
        logUnhandledMessage(m);
      } else {
        if (log.isInfoEnabled()) {
          log.info("Delaying unhandled Message ("+m.getClass()+"): "+m);
        }
        pendingMessages.add(m);
      }
    }

    private void logUnhandledMessage(Message m) {
      log.error("Dropping unhandled Message ("+m.getClass()+"): "+m);
    }

    private void unpendMessages() {
      List ms;
      synchronized (handlers) {
        assert pendingMessages != null;
        ms = pendingMessages;
        pendingMessages = null;
      }

      for (Iterator it = ms.iterator(); it.hasNext(); ) {
        Message m = (Message) it.next();
        logUnhandledMessage(m);
      }
    }
  }
}
