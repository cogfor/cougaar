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

package org.cougaar.core.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.SimpleUniqueObject;
import org.cougaar.core.util.UID;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;

/**
 * This component creates a test {@link AttributeBasedAddress}
 * {@link Relay}.
 */
public class TestABA extends ComponentPlugin {
  private IncrementalSubscription relays;
  private UIDService uidService;
  private LoggingService logger;
  private MyRelay myRelay;
  private UnaryPredicate relayPredicate =
    new UnaryPredicate() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      public boolean execute(Object o) {
        return o instanceof MyRelay;
      }
    };

  public void setUIDService(UIDService s) {
    uidService = s;
  }

  public void setLoggingService(LoggingService s) {
    logger = s;
    logger = LoggingServiceWithPrefix.add(logger, getAgentIdentifier() + ": ");
  }

  @Override
public void setupSubscriptions() {
    String cid = getAgentIdentifier().toString();
    boolean is135ARBN = cid.equals("1-35-ARBN");
    relays = blackboard.subscribe(relayPredicate);
    if (is135ARBN) {
      logger.info("Adding relay at " + cid);
      AttributeBasedAddress target =
        AttributeBasedAddress.getAttributeBasedAddress(
            "2-BDE-1-AD-COMM", "Role", "Member");
      myRelay = new MyRelay(Collections.singleton(target));
      myRelay.setUID(uidService.nextUID());
      blackboard.publishAdd(myRelay);
    } else {
      logger.debug("Waiting at " + cid);
    }
  }

  @Override
public void execute() {
    if (relays.hasChanged()) {
      int n = relays.size();
      printList("Added", relays.getAddedCollection(), n);
      printList("Removed", relays.getRemovedCollection(), n);
    }
  }

  private void printList(String msg, Collection list, int size) {
    for (Iterator i = list.iterator(); i.hasNext(); ) {
      MyRelay relay = (MyRelay) i.next();
      if (relay == myRelay) continue;
      MessageAddress src = relay.getSource();
      logger.info(msg + "(" + size + "): " + src);
    }
  }

  private static class MyRelay extends SimpleUniqueObject
    implements Relay.Source, Relay.Target, Relay.TargetFactory
  {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   transient Set targets;
    transient MessageAddress source;

    MyRelay(Set targets) {
      this.targets = targets;
    }
    MyRelay(UID uid, MessageAddress src) {
      this.targets = Collections.EMPTY_SET;
      this.source = src;
      setUID(uid);
    }
    public Set getTargets() {
      return targets;
    }
    public Object getContent() {
      return this;
    }

    public TargetFactory getTargetFactory() {
      return this;
    }
    public int updateResponse(MessageAddress target, Object response) {
      return NO_CHANGE;
    }
    public MessageAddress getSource() {
      return source;
    }
    public Object getResponse() {
      return null;
    }
    public int updateContent(Object content, Token token) {
      return CONTENT_CHANGE;
    }
    public Relay.Target create(UID uid, MessageAddress source, Object content, Token token)
    {
      return new MyRelay(uid, source);
    }
  }
}
