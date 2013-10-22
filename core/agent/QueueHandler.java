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
import java.util.List;

import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.Component;
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
 * This component buffers blackboard messages while the agent is
 * loading, plus switches threads when receiving messages to avoid
 * blocking the message transport thread.
 */
public final class QueueHandler
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private MessageSwitchService mss;

  private BlackboardForAgent bb;

  private MessageAddress localAgent;

  private QueueHandlerBody body;
  private boolean isStarted;
  private Object lock = new Object();

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

    mss = sb.getService(this, MessageSwitchService.class, null);


    // register message handler to observe all incoming messages
    MessageHandler mh = new MessageHandler() {
      public boolean handleMessage(Message message) {
        if (message instanceof ClusterMessage) {
          // internal message queue
          getHandler().addMessage((ClusterMessage) message);
          return true;
        } else {
          return false;
        }
      }
    };
    mss.addMessageHandler(mh);
  }

  @Override
public void start() {
    super.start();


    // get blackboard service
    //
    // this is delayed until "start()" because the queue handler
    // is loaded after the blackboard.  Messages are buffered
    // between the top-level "load()" MTS unpend and our "start()",
    // then released by "startThread()".
    bb = sb.getService(this, BlackboardForAgent.class, null);
    if (bb == null) {
      throw new RuntimeException(
          "Unable to obtain BlackboardForAgent");
    }

    startThread();

  }

  @Override
public void suspend() {
    super.suspend();
    stopThread();
    bb.suspend();
  }

  @Override
public void resume() {
    super.resume();
    bb.resume();
    startThread();
  }

  @Override
public void stop() {
    super.stop();
    stopThread();
    if (bb != null) {
      sb.releaseService(this, BlackboardForAgent.class, null);
      bb = null;
    }
  }

  @Override
public void unload() {
    super.unload();

    if (mss != null) {
      // mss.unregister?
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }
  }

  private void startThread() {
    synchronized (lock) {
      if (!isStarted) {
        getHandler().start();
        isStarted = true;
      }
    }
  }

  private void stopThread() {
    synchronized (lock) {
      if (isStarted) {
        getHandler().halt();
        isStarted = false;
        body = null;
      }
    }
  }

  private final void receiveMessages(List messages) {
    try {
      bb.receiveMessages(messages);
    } catch (Exception e) {
      log.error("Uncaught Exception while handling Queued Messages", e);
    }
  }

  private QueueHandlerBody getHandler() {
    synchronized (lock) {
      if (body == null) {
        QueueClient qc = new QueueClient() {
          public MessageAddress getMessageAddress() {
            return localAgent;
          }
          public void receiveQueuedMessages(List messages) {
            receiveMessages(messages);
          }
        };
        ThreadService tsvc = sb.getService(this, ThreadService.class, null);
        body = new QueueHandlerBody(qc, tsvc);
        sb.releaseService(this, ThreadService.class, tsvc);
      }
      return body;
    }
  }

  interface QueueClient {
    MessageAddress getMessageAddress();
    void receiveQueuedMessages(List messages);
  }

  private static final class QueueHandlerBody implements Runnable {
    private QueueClient client;
    private final List queue = new ArrayList();
    private final List msgs = new ArrayList();
    private boolean ready = false;
    private boolean active = false;
    private Schedulable sched;
    public QueueHandlerBody(QueueClient client,
        ThreadService tsvc)
    {
      this.client = client;
      sched = tsvc.getThread(this, this, 
          client.getMessageAddress()+"/RQ");
    }
    void start() {
      synchronized (queue) {
        ready = true;
        sched.start();
      }
    }
    public void halt() {
      synchronized (queue) {
        ready = false;
        sched.cancel();
        while (active) {
          try {
            queue.wait();
          } catch (InterruptedException ie) {
          }
        }
      }
      client = null;
    }
    public void run() {
      synchronized (queue) {
        if (!ready) {
          return;
        }
        if (queue.isEmpty()) {
          return;
        }

        active = true;
        msgs.addAll(queue);
        queue.clear();
      }
      if (!msgs.isEmpty()) {
        client.receiveQueuedMessages(msgs);
        msgs.clear();
      }
      synchronized (queue) {
        active = false;
        queue.notify(); // only used for halt()
      }
    }
    public void addMessage(ClusterMessage m) {
      synchronized (queue) {
        queue.add(m);
        if (ready) sched.start(); // restart 
      }
    }
  }
}
