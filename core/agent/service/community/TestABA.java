/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.community;

import java.util.Collection;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.relay.SimpleRelay;
import org.cougaar.core.relay.SimpleRelaySource;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.Arguments;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin sends a simple ABA Relay to a target community.
 */
public class TestABA extends ComponentPlugin {

  private Arguments args = Arguments.EMPTY_INSTANCE;

  private LoggingService log;
  private UIDService uids;

  private IncrementalSubscription sub;

  @Override
public void setParameter(Object o) {
    args = new Arguments(o);
  }

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }
  public void setUIDService(UIDService uids) {
    this.uids = uids;
  }

  @Override
protected void setupSubscriptions() {
    sub = blackboard.subscribe(
        new UnaryPredicate() {
          /**
          * 
          */
         private static final long serialVersionUID = 1L;

         public boolean execute(Object o) {
            return (o instanceof Relay);
          }
        });

    String community = args.getString("target");
    if (community != null) {
      if (log.isInfoEnabled()) {
        log.info("Sending ABA Relay to "+community);
      }
      SimpleRelay sr = new SimpleRelaySource(
          uids.nextUID(),
          agentId,
          AttributeBasedAddress.getAttributeBasedAddress(
            community, "Role", "Member"),
          "(Test from \""+agentId+"\" to \""+community+"\")");
      blackboard.publishAdd(sr);
    }
  }

  @Override
protected void execute() {
    if (log.isInfoEnabled() && sub.hasChanged()) {
      for (int i = 0; i < 3; i++) {
        Collection c =
          (i == 0 ? sub.getAddedCollection() :
           i == 1 ? sub.getChangedCollection() :
           sub.getRemovedCollection());
        if (c.isEmpty()) continue;
        log.info(
            "Observed "+
            (i == 0 ? "add" : i == 1 ? "change" : "remove")+
            "["+c.size()+"]:"+c);
      }
    }
  }
}
