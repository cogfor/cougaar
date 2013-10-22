/*
 * <copyright>
 *
 *  Copyright 2001-2004 Mobile Intelligence Corp
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

package org.cougaar.community;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.directory.ModificationItem;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.util.log.Logger;

import org.cougaar.core.service.community.CommunityResponseListener;
import org.cougaar.core.service.community.Entity;

/**
 * Queue for processing Community Requests at a future time.  This is
 * typically used to hold requests that have failed and are to be retried.
 * For instance, a WP lookup.
 */
public class CommunityRequestQueue {

  private List queue = Collections.synchronizedList(new ArrayList());
  private RequestQueueTimer timer;
  private DefaultCommunityServiceImpl commSvc;
  private Logger logger;
  private String agentName;
  private ServiceBroker serviceBroker;

  public CommunityRequestQueue(ServiceBroker sb,
                               DefaultCommunityServiceImpl dcs) {
    serviceBroker = sb;
    commSvc = dcs;
    agentName = getAgentName();
    logger = (LoggingService)serviceBroker.getService(this, LoggingService.class, null);
  }

  public synchronized void add(long delay,
                               String communityName,
                               int requestType,
                               Entity entity,
                               ModificationItem[] attrMods,
                               long timeout,
                               CommunityResponseListener crl) {
    if (logger.isDebugEnabled()) {
      logger.debug(agentName+": add:" +
                   "delay=" + delay +
                   " community=" + communityName +
                   " type=" + requestType +
                   " entity=" + entity);
    }
    QueuedRequest req = new QueuedRequest(now() + delay,
                                          communityName,
                                          requestType,
                                          entity,
                                          attrMods,
                                          timeout,
                                          crl);
    queue.add(req);
    execute();
  }

  protected void execute() {
    if (logger.isDetailEnabled()) {
      logger.detail(agentName+": execute:" +
                    " itemsInQueue=" + queue.size());
    }
    if (timer != null && !timer.hasExpired()) {
      timer.cancel();
    }
    int n;
    List l;
    synchronized (queue) {
      n = queue.size();
      if (n <= 0) { return; }
      l = new ArrayList(queue);
      queue.clear();
    }
    long now = now();
    long fireAt = 0;
    for (int i = 0; i < n; i++) {
      QueuedRequest req = (QueuedRequest)l.get(i);
      if (req.processAt <= now) {
        if (logger.isDebugEnabled()) {
          logger.debug(agentName + ": sendCommunityRequest:" +
                       " community=" + req.communityName +
                       " type=" + req.type +
                       " entity=" + req.entity);
        }
        commSvc.sendCommunityRequest(req.communityName,
                                     req.type,
                                     req.entity,
                                     req.mods,
                                     req.timeout,
                                     req.crl);
      } else {  // not yet
        if (fireAt == 0 || req.processAt < fireAt) {
          fireAt = req.processAt;
        }
        queue.add(req);
      }
    }
    if (fireAt > 0) {
      timer = new RequestQueueTimer(fireAt);
      AlarmService alarmService =
          (AlarmService)serviceBroker.getService(this, AlarmService.class, null);
      alarmService.addRealTimeAlarm(timer);
      serviceBroker.releaseService(this, AlarmService.class, alarmService);
    }
  }

  protected String getAgentName() {
    AgentIdentificationService ais =
        (AgentIdentificationService)serviceBroker.getService(this,
        AgentIdentificationService.class, null);
    MessageAddress addr = ais.getMessageAddress();
    serviceBroker.releaseService(this, AgentIdentificationService.class, ais);
    return addr.toString();
  }

  private long now() { return System.currentTimeMillis(); }

  class QueuedRequest {
    long processAt;
    String communityName;
    int type;
    long timeout;
    Entity entity;
    ModificationItem[] mods;
    CommunityResponseListener crl;
    QueuedRequest(long time,
                  String cname,
                  int t,
                  Entity e,
                  ModificationItem[] m,
                  long to,
                  CommunityResponseListener l) {
      processAt = time;
      communityName = cname;
      type = t;
      entity = e;
      mods = m;
      this.timeout = to;
      crl = l;
    }
  }

  protected class RequestQueueTimer implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public RequestQueueTimer(long expirationTime) {expiresAt = expirationTime;}
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        execute();
      }
    }
    public boolean hasExpired() {return expired;}
    public synchronized boolean cancel() {
      boolean was = expired;
      expired = true;
      return was;
    }
  }

}
