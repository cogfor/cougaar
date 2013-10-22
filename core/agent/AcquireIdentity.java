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

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceService;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.identity.AgentIdentityClient;
import org.cougaar.core.service.identity.AgentIdentityService;
import org.cougaar.core.service.identity.CrlReason;
import org.cougaar.core.service.identity.TransferableIdentity;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component acquires the agent's security identity from the
 * optional {@link AgentIdentityService} and transfers the identity
 * when the agent moves.
 *
 * @see AgentIdentityService 
 */
public final class AcquireIdentity
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;

  // identity either from prior location or moving away
  private TransferableIdentity mobileIdentity;

  private AgentIdentityClient agentIdentityClient;

  private LoggingService log;

  private PersistenceService ps;
  private PersistenceClient pc;

  private AgentIdentityService agentIdentityService;

  private MobilityNotificationClient mnc;
  private MobilityNotificationService mns;

  private MessageAddress moveTargetNode;

  private MessageAddress localAgent;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    log = sb.getService(this, LoggingService.class, null);

    localAgent = find_local_agent();

    register_persistence();

    // get mobile state
    Object o = rehydrate();
    if (o instanceof TransferableIdentity) {
      // fill in prior (mobile) identity
      mobileIdentity = (TransferableIdentity) o;
    }
    o = null;

    // take and clear the saved identity
    TransferableIdentity tmp = mobileIdentity;
    mobileIdentity = null;
    if (tmp == NULL_MOBILE_IDENTITY) {
      tmp = null;
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Acquiring "+
          ((tmp == null) ?
           ("new identity") :
           ("transfered identity: "+tmp)));
    }

    agentIdentityClient = 
      new AgentIdentityClient() {
        public void identityRevoked(CrlReason reason) {
          log.warn("Identity has been revoked: "+reason);
          // ignore for now, re-acquire or die TBA
        }
        public String getName() {
          return localAgent.getAddress();
        }
      };
    agentIdentityService = sb.getService(
       agentIdentityClient, AgentIdentityService.class, null);
    if (agentIdentityService == null) {
      if (log.isInfoEnabled()) {
        log.info("Agent identity service not found");
      }
      return;
    }

    try {
      agentIdentityService.acquire(tmp);
    } catch (Exception e) {
      throw new RuntimeException(
          ("Unable to acquire agent identity for agent "+
           localAgent+
           ((tmp == null) ? 
            ("") :
            (" from transfered identity: "+tmp))),
          e);
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

  @Override
public void suspend() {
    super.suspend();

    if (moveTargetNode == null) {
      // non-moving suspend?
      if (log.isInfoEnabled()) {
        log.info("Releasing identity");
      }
      if (agentIdentityService != null) {
        agentIdentityService.release();
      }
    } else {
      // moving, delay identity transfer until persist
      if (log.isInfoEnabled()) {
        log.info(
            "Postponing identity transfer to node "+
            moveTargetNode);
      }
    }
  }

  @Override
public void resume() {
    super.resume();

    // re-establish our identity
    if (moveTargetNode == null) {
      // resume after non-move suspend
      if (log.isInfoEnabled()) {
        log.info(
            "Acquiring agent identify from scratch");
      }
      if (agentIdentityService != null) {
        try {
          agentIdentityService.acquire(null);
        } catch (Exception e) {
          throw new RuntimeException(
              "Unable to resume agent "+localAgent+
              " after non-move suspend", e);
        }
      }
    } else {
      // failed move, restart
      MessageAddress mtn = moveTargetNode; 
      moveTargetNode = null;
      if (mobileIdentity == null) {
        // never transfered identity (state capture failed?)
        if (log.isInfoEnabled()) {
          log.info(
              "Identity was never transfered to "+
              mtn);
        }
      } else {
        // take and clear the saved identity
        TransferableIdentity tmp = mobileIdentity;
        mobileIdentity = null;
        if (tmp == NULL_MOBILE_IDENTITY) {
          tmp = null;
        }
        if (log.isInfoEnabled()) {
          log.info(
              "Acquiring agent identify from"+
              " failed move to "+mtn+
              " and transfer-identity "+tmp);
        }
        if (agentIdentityService != null) {
          try {
            agentIdentityService.acquire(tmp);
          } catch (Exception e) {
            throw new RuntimeException(
                "Unable to restart agent "+localAgent+
                " after failed move to "+mtn+
                " and transfer-identity "+tmp, e);
          }
        }
      }
    }
  }

  @Override
public void unload() {
    super.unload();

    if (mns != null) {
      sb.releaseService(mnc, MobilityNotificationService.class, mns);
      mns = null;
    }

    if (agentIdentityService != null) {
      sb.releaseService(
          agentIdentityClient,
          AgentIdentityService.class,
          agentIdentityService);
      agentIdentityService = null;
    }

    unregister_persistence();
  }

  private MessageAddress find_local_agent() {
    AgentIdentificationService ais = sb.getService(this, AgentIdentificationService.class, null);
    if (ais == null) {
      return null;
    }
    MessageAddress ret = ais.getMessageAddress();
    sb.releaseService(
        this, AgentIdentificationService.class, ais);
    return ret;
  }

  private Object captureState() {
    if (getModelState() == ACTIVE) {
      if (log.isDebugEnabled()) {
        log.debug("Ignoring identity persist while active");
      }
      return null;
    }

    return mobileIdentity;
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

  private static final TransferableIdentity NULL_MOBILE_IDENTITY =
    new TransferableIdentity() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      private Object readResolve() { return NULL_MOBILE_IDENTITY; }
    };
}
