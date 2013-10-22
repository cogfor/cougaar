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

package org.cougaar.core.wp.resolver;

import java.util.ArrayList;
import java.util.List;

import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component is a base class that handles {@link
 * MessageSwitchService} details for the {@link ClientTransport}.
 * <p>
 * This is nearly generic; with a bit more work it could be a
 * useful generic base class. 
 */
public abstract class TransportBase
extends GenericStateModelAdapter
implements Component
{

  protected ServiceBroker sb;
  protected LoggingService logger;
  protected MessageAddress agentId;
  protected ThreadService threadService;

  private MessageSwitchService messageSwitchService;

  private final Object sendLock = new Object();
  private List sendQueue;

  private Schedulable receiveThread;
  private final List receiveQueue = new ArrayList();
  private final List receiveTmp = new ArrayList();

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  @Override
public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver remote handler");
    }

    // which agent are we in?
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

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
        "White pages client handle incoming responses");

    // register our message switch (now or later)
    ServiceFinder.Callback sfc =
      new ServiceFinder.Callback() {
        public void foundService(Service s) {
          TransportBase.this.foundService(s);
        }
      };
    ServiceFinder.findServiceLater(
        sb, MessageSwitchService.class, null, sfc);
  }

  @Override
public void unload() {
    MessageSwitchService mss;
    synchronized (sendLock) {
      mss = messageSwitchService;
      messageSwitchService = null;
    }
    if (mss != null) {
      //mss.removeMessageHandler(myMessageHandler);
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }

    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (logger != null) {
      sb.releaseService(this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
  }

  private void foundService(Service s) {
    // service broker now has the MessageSwitchService
    //
    // should we do this in a separate thread?
    if (hasMessageTransport()) {
      if (logger.isErrorEnabled()) {
        logger.error("Already obtained our message switch");
      }
      return;
    }
    if (!(s instanceof MessageSwitchService)) {
      if (logger.isErrorEnabled()) {
        logger.error("Unable to obtain MessageSwitchService");
      }
      return;
    }
    MessageSwitchService mss = (MessageSwitchService) s;
    MessageHandler myMessageHandler =
      new MessageHandler() {
        public boolean handleMessage(Message m) {
          return receive(m);
        }
      };
    mss.addMessageHandler(myMessageHandler);
    if (logger.isInfoEnabled()) {
      logger.info("Registered with message transport");
    }
    synchronized (sendLock) {
      messageSwitchService = mss;
    }
    foundMessageTransport();
  }

  protected void foundMessageTransport() {
    flushSendQueueLater();
  }

  protected boolean hasMessageTransport() {
    synchronized (sendLock) {
      return (messageSwitchService != null);
    }
  }

  protected void sendOrQueue(Message m) {
    List l = null;
    MessageSwitchService mss;
    synchronized (sendLock) {
      mss = messageSwitchService;
      if (mss == null) {
        if (m != null) {
          // queue to send once the MTS is up
          if (sendQueue == null) {
            sendQueue = new ArrayList();
          }
          sendQueue.add(m);
        }
        return;
      } else if (sendQueue != null) {
        // flush pending messages
        l = sendQueue;
        sendQueue = null;
      } else {
        // typical case
      }
    }
    if (l != null) {
      // flush pending messages
      for (int i = 0, n = l.size(); i < n; i++) {
        Message qm = (Message) l.get(i);
        send(mss, qm);
      }
    }
    if (m != null) {
      send(mss, m);
    }
  }

  private void send(MessageSwitchService mss, Message m) {
    // assert (messageSwitchService != null);
    if (logger.isDetailEnabled()) {
      logger.detail("sending message: "+m);
    }
    mss.sendMessage(m);
  }

  private void flushSendQueueLater() {
    // send queued messages
    Runnable flushSendQueueRunner =
      new Runnable() {
        public void run() {
          sendOrQueue(null);
        }
      };
    Schedulable flushSendQueueThread = 
      threadService.getThread(
          this,
          flushSendQueueRunner,
          "Flush queued output messages");
    flushSendQueueThread.start();
  }

  //
  // receive:
  //

  protected abstract boolean shouldReceive(Message m);

  protected boolean receive(Message m) {
    if (shouldReceive(m)) {
      receiveLater(m);
      return true;
    }
    return false;
  }

  private void receiveLater(Message m) {
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
    receiveNow(receiveTmp);
    receiveTmp.clear();
  }

  protected void receiveNow(List l) {
    for (int i = 0, n = l.size(); i < n; i++) {
      Message m = (Message) l.get(i);
      receiveNow(m);
    }
  }

  protected abstract void receiveNow(Message m);

}
