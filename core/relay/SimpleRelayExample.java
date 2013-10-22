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

package org.cougaar.core.relay;

import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is an example {@link SimpleRelay} client, which
 * both sends relays and replies to them.
 * <p>
 * To use, add this component to an agent and specify a target,
 * for example in "AgentA" with a target of "AgentB":<pre> 
 *   &lt;component
 *       name='org.cougaar.core.relay.SimpleRelayExample(target=AgentB)'
 *       class='org.cougaar.core.relay.SimpleRelayExample'
 *       priority='COMPONENT'
 *       insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'&gt;
 *     &lt;argument&gt;target=AgentB&gt;/argument&gt;
 *   &lt;/component&gt;
 * </pre>
 * In the target agent add the component without a target argument:<pre>
 *   &lt;component
 *       name='org.cougaar.core.relay.SimpleRelayExample(target=AgentB)'
 *       class='org.cougaar.core.relay.SimpleRelayExample'
 *       priority='COMPONENT'
 *       insertionpoint='Node.AgentManager.Agent.PluginManager.Plugin'/&gt;
 * </pre>
 * You should see output similar to the following, which excludes
 * logging timestamps and other details:<pre> 
 *   .. AgentA: Sending (.. query=ping reply=null)
 *   .. AgentB: Reply (.. query=ping reply=echo-ping)
 *   .. AgentA: Received (.. query=ping reply=echo-ping)
 * </pre>
 * <p>
 * It would be straight-forward to extend this example to a more
 * general remote procedure call (<u>RPC</u>) utility:  the query
 * specifies a String "method" name and Object[] parameters, and
 * the reply is either a Throwable or a non-error value, plus a
 * wrapper if the non-error value is null or a Throwable.  As in
 * RMI, the parameters and return value must be Serializable and
 * treated as immutable.
 */
public class SimpleRelayExample extends ComponentPlugin {

  private LoggingService log;
  private UIDService uids;

  private IncrementalSubscription sub;

  @Override
public void load() {
    super.load();

    // get services
    ServiceBroker sb = getServiceBroker();
    log = sb.getService(this, LoggingService.class, null);
    uids = sb.getService(this, UIDService.class, null);

    // prefix all logging calls with our agent name
    log = LoggingServiceWithPrefix.add(log, agentId+": ");

    if (log.isDebugEnabled()) {
      log.debug("loaded");
    }
  }

  @Override
protected void setupSubscriptions() {
    if (log.isDebugEnabled()) {
      log.debug("setupSubscriptions");
    }

    // create relay subscription
    sub = blackboard.subscribe(new MyPred(agentId));

    // send relays
    for (Iterator iter = getParameters().iterator(); iter.hasNext();) {
      String s = (String) iter.next();
      if (!s.startsWith("target=")) {
        continue;
      }
      String target_name = s.substring("target=".length());
      MessageAddress target = 
        MessageAddress.getMessageAddress(target_name);
      if (agentId.equals(target)) {
        if (log.isWarnEnabled()) {
          log.warn("Ignoring target that matches self: "+target);
        }
        continue;
      }
      UID uid = uids.nextUID();
      Object query = "ping";
      SimpleRelay sr = new SimpleRelaySource(
          uid, agentId, target, query);
      if (log.isShoutEnabled()) {
        log.shout("Sending "+sr);
      }
      blackboard.publishAdd(sr);
    }
  }

  @Override
protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug("execute");
    }

    if (!sub.hasChanged()) {
      // usually never happens, since the only reason to execute
      // is a subscription change
      return;
    }

    // observe added relays
    for (Enumeration en = sub.getAddedList(); en.hasMoreElements();) {
      SimpleRelay sr = (SimpleRelay) en.nextElement();
      if (log.isDebugEnabled()) {
        log.debug("observe added "+sr);
      }
      if (agentId.equals(sr.getTarget())) {
        // send back reply
        sr.setReply("echo-"+sr.getQuery()); 
        if (log.isShoutEnabled()) {
          log.shout("Reply "+sr);
        }
        blackboard.publishChange(sr);
      } else {
        // ignore relays we sent
      }
    }

    // observe changed relays
    for (Enumeration en = sub.getChangedList(); en.hasMoreElements();) {
      SimpleRelay sr = (SimpleRelay) en.nextElement();
      if (log.isDebugEnabled()) {
        log.debug("observe changed "+sr);
      }
      if (agentId.equals(sr.getSource())) {
        // got back answer
        if (log.isShoutEnabled()) {
          log.shout("Received "+sr);
        } 
        // remove query both locally and at the remote target.
        //
        // this is optional, but it's a good idea to clean up and
        // free some memory.
        blackboard.publishRemove(sr); 
      } else {
        // ignore our reply
      }
    }

    if (log.isDebugEnabled()) {
      // removed relays
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements();) {
        SimpleRelay sr = (SimpleRelay) en.nextElement();
        log.debug("observe removed "+sr);
      }
    }
  }

  /**
   * My subscription predicate, which matches SimpleRelays where my
   * local address matches either the source or target.
   */ 
  private static class MyPred implements UnaryPredicate {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final MessageAddress agentId;
    public MyPred(MessageAddress agentId) {
      this.agentId = agentId;
    }
    public boolean execute(Object o) {
      if (o instanceof SimpleRelay) {
        SimpleRelay sr = (SimpleRelay) o;
        return 
          (agentId.equals(sr.getSource()) ||
           agentId.equals(sr.getTarget()));
      }
      return false;
    }
  }
}
