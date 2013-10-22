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

import java.net.InetAddress;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component generates {@link EventService} events
 * to announce the agent's load/start/stop/<i>etc</i> and move.
 */
public final class Events 
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  private LoggingService log;
  private EventService eventService;
  private WhitePagesService wps;

  private MobilityNotificationClient mnc;
  private MobilityNotificationService mns;

  private MessageAddress moveTargetNode;

  private MessageAddress localAgent;
  private long localIncarnation;
  private MessageAddress localNode;
  private String localHost;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();
    obtainServices();
  }

  @Override
public void start() {
    super.start();
    event("Started");
  }

  @Override
public void suspend() {
    super.suspend();
    if (moveTargetNode != null) {
      event("Moving");
    }
  }

  @Override
public void resume() {
    super.resume();
    if (moveTargetNode != null) {
      event("NotMoved");
      moveTargetNode = null;
    }
  }

  @Override
public void stop() {
    super.stop();
    if (moveTargetNode == null) {
      event("Stopped");
    } else {
      event("Moved");
    }
  }

  @Override
public void unload() {
    super.unload();
    releaseServices();
  }

  private void obtainServices() {
    log = sb.getService(
       this, LoggingService.class, null);

    eventService = sb.getService(
       this, EventService.class, null);
    if (eventService == null) {
      throw new RuntimeException(
          "Unable to obtain EventService");
    }

    wps = sb.getService(this, WhitePagesService.class, null);

    // local agent
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    // local agent's incarnation
    TopologyService ts = sb.getService(this, TopologyService.class, null);
    if (ts != null) {
      localIncarnation = ts.getIncarnationNumber();
      sb.releaseService(
          this, TopologyService.class, ts);
    }

    // local node
    NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
    if (nis != null) {
      localNode = nis.getMessageAddress();
      InetAddress localAddr = nis.getInetAddress();
      localHost = localAddr != null ? localAddr.getHostName() : "?";
      sb.releaseService(
          this, NodeIdentificationService.class, nis);
    }

    // mobility watcher
    mnc =
      new MobilityNotificationClient() {
        public void movingTo(MessageAddress destinationNode) {
          moveTargetNode = destinationNode;
        }
      };
    mns = sb.getService(mnc, MobilityNotificationService.class, null);
    if (mns == null && log.isInfoEnabled()) {
      log.info(
         "Unable to obtain MobilityNotificationService"+
         ", mobility is disabled");
    }
  }

  private void event(String cycle) {
    if (eventService == null || !eventService.isEventEnabled()) {
      return;
    }
    String tail;
    if (moveTargetNode == null) {
      tail = "";
    } else {
      String moveTargetHost = "?";
      try {
        AddressEntry nodeEntry = 
          wps.get(
              moveTargetNode.getAddress(),
              "topology",
              10000); // wait at most 10 seconds
        if (nodeEntry != null) {
          moveTargetHost = nodeEntry.getURI().getHost();
        }
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info(
              "Unable to get host for destination node "+
              moveTargetNode,
              e);
        }
      }
      tail =
        " ToNode("+moveTargetNode+
        ") ToHost("+moveTargetHost+
        ")";
    }
    eventService.event(
        "AgentLifecycle("+cycle+
        ") Agent("+localAgent+
        ") Node("+localNode+
        ") Host("+localHost+
        ") Incarnation("+localIncarnation+
        ")"+tail);
  }

  private void releaseServices() {
    if (mns != null) {
      sb.releaseService(mnc, MobilityNotificationService.class, mns);
      mns = null;
    }
    if (wps != null) {
      sb.releaseService(this, WhitePagesService.class, wps);
      wps = null;
    }
    if (eventService != null) {
      sb.releaseService(this, EventService.class, eventService);
      eventService = null;
    }
  }
}
