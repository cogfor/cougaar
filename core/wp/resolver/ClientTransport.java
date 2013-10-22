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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.wp.WhitePagesProtectionService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.wp.MessageTimeoutUtils;
import org.cougaar.core.wp.Parameters;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.util.RarelyModifiedList;

/**
 * This component sends and receives messages for the resolver.
 * <p>
 * This is the last outgoing stop for the resolver -- the request
 * wasn't in the cache and can't be batched with other already-pending
 * requests.
 * <p>
 * All of these properties are also component parameters by
 * removing the "org.cougaar.core.wp.resolver.transport."
 * prefix: 
 * <pre>
 * @property org.cougaar.core.wp.resolver.transport.nagleMillis
 *   Delay in milliseconds before sending messages, to improve
 *   batching.  Defaults to zero.
 * @property org.cougaar.core.wp.resolver.transport.noListNagle
 *   Ignore the "nagleMillis" delay if the request is a new
 *   name list (e.g. "list ."), which is often a user request.
 *   Defaults to false. 
 * @property org.cougaar.core.wp.resolver.transport.graceMillis
 *   Extended message timeout deadline after startup.  Defaults to
 *   zero.
 * @property org.cougaar.core.wp.resolver.transport.checkDeadlinesPeriod
 *   Time in milliseconds between checks for message timeouts if
 *   there are any outstanding messages.  Defaults to 10000.
 * </pre> 
 */
public class ClientTransport
extends TransportBase
{

  // this is a dummy address for messages that can't be
  // sent yet, e.g. because there are no WP servers.
  private static final MessageAddress NULL_ADDR =
    MessageTimeoutUtils.setTimeout(
        MessageAddress.getMessageAddress("wp-null"),
        15000);

  private ClientTransportConfig config;

  private WhitePagesProtectionService protectS;

  private SelectService selectService;

  private PingSP pingSP;
  private LookupSP lookupSP;
  private ModifySP modifySP;

  private RarelyModifiedList pingClients = 
    new RarelyModifiedList();
  private RarelyModifiedList lookupClients = 
    new RarelyModifiedList();
  private RarelyModifiedList modifyClients = 
    new RarelyModifiedList();

  private final SelectService.Client myClient = 
    new SelectService.Client() {
      public void onChange() {
        ClientTransport.this.onServerChange();
      }
    };

  //
  // output (send to WP server):
  //

  private final Object myLock = new Object();

  // this is our startup grace-time on message timeouts, which is
  // based upon the time we obtained our messageSwitchService plus
  // the configuration's "graceMillis".
  //
  // this is used to allow more delivery time when the system is
  // starting, since unusual costs usually occur (e.g. cryto
  // handshaking).
  private long graceTime;

  // lookup requests (name => Entry) that are either being delayed
  // (nagle) or have been sent but not ack'ed (outstanding).
  //
  // Map<String, Entry>
  private final Map lookups = new HashMap();

  // modify requests (name => Entry) that are either being delayed
  // (nagle) or have been sent but not ack'ed (outstanding).
  //
  // Map<String, Entry>
  private final Map mods = new HashMap();

  // the most recent modify for this node, separately locked
  // to avoid a ping/select deadlock.
  private final Object nodeModifyLock = new Object();
  private Map nodeModify;

  // temporary fields for use in "send" and related methods.
  // accessed within myLock.
  private long now;
  private boolean sendNow;
  private boolean sendLater;
  private final Set lookupNames = new HashSet();
  private final Set modifyNames = new HashSet();
  private final Map lookupAddrs = new HashMap();
  private final Map modifyAddrs = new HashMap();
  
  // "nagle" delayed release
  private long releaseTime;
  private Schedulable releaseThread;

  // periodic check for late message acks
  private long checkDeadlinesTime;
  private Schedulable checkDeadlinesThread;

  //
  // statistics
  //

  private final Stats lookupStats = new Stats();
  private final Stats modifyStats = new Stats();

  public void setParameter(Object o) {
    configure(o);
  }

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new ClientTransportConfig(o);
  }

  @Override
public void load() {
    super.load();

    configure(null);

    if (logger.isDebugEnabled()) {
      logger.debug("Loading resolver remote handler");
    }

    protectS =
      sb.getService(this, WhitePagesProtectionService.class, null);
    if (logger.isDebugEnabled()) {
      logger.debug("White pages protection service: "+protectS);
    }

    // create threads
    if (config.nagleMillis > 0) { 
      Runnable releaseRunner =
        new Runnable() {
          public void run() {
            // assert (thread == releaseThread);
            releaseNow();
          }
        };
      releaseThread = threadService.getThread(
          this,
          releaseRunner,
          "White pages client \"nagle\" delayed sendler");
    }

    Runnable checkDeadlinesRunner =
      new Runnable() {
        public void run() {
          // assert (thread == checkDeadlinesThread);
          checkDeadlinesNow();
        }
      };
    checkDeadlinesThread = threadService.getThread(
        this,
        checkDeadlinesRunner,
        "White pages client transport send queue checker");

    // register to select servers
    selectService = sb.getService(myClient, SelectService.class, null);
    if (selectService == null) {
      throw new RuntimeException(
          "Unable to obtain SelectService");
    }

    // advertise our service
    pingSP = new PingSP();
    sb.addService(PingService.class, pingSP);
    lookupSP = new LookupSP();
    sb.addService(LookupService.class, lookupSP);
    modifySP = new ModifySP();
    sb.addService(ModifyService.class, modifySP);
  }

  @Override
public void unload() {
    if (modifySP != null) {
      sb.revokeService(ModifyService.class, modifySP);
      modifySP = null;
    }
    if (lookupSP != null) {
      sb.revokeService(LookupService.class, lookupSP);
      lookupSP = null;
    }
    if (pingSP != null) {
      sb.revokeService(PingService.class, pingSP);
      pingSP = null;
    }

    if (selectService != null) {
      sb.releaseService(
          myClient, SelectService.class, selectService);
      selectService = null;
    }

    if (protectS != null) {
      sb.releaseService(
          this, WhitePagesProtectionService.class, protectS);
      protectS = null;
    }

    super.unload();
  }

  @Override
protected void foundMessageTransport() {
    // super.foundMessageTransport();
    synchronized (myLock) {
      long now = System.currentTimeMillis();
      if (config.graceMillis >= 0) {
        this.graceTime = now + config.graceMillis;
      }
      // schedule a "send"
      checkDeadlinesTime = now;
      checkDeadlinesThread.start();
    }
  }

  private List getList(int action) {
    return
      (action == WPQuery.PING ? pingClients :
       action == WPQuery.LOOKUP ? lookupClients :
       action == WPQuery.MODIFY ? modifyClients :
       null);
  }
  private void register(int action, Object c) {
    getList(action).add(c);
  }
  private void unregister(int action, Object c) {
    getList(action).remove(c);
  }

  private void onServerChange() {
    // the list of servers has changed
    //
    // kick the thread, since either we've added a new server
    // (important if we had zero servers) or we've removed a
    // server (must revisit any messages we sent to that server).
    synchronized (myLock) {
      checkDeadlinesTime = System.currentTimeMillis();
      checkDeadlinesThread.start();
    }
  }

  //
  // send:
  //

  private void ping(MessageAddress addr, long deadline) {
    long now = System.currentTimeMillis();
    if (now > deadline) {
      // to late?
      return;
    }
    MessageAddress target = 
      MessageTimeoutUtils.setDeadline(
          addr,
          deadline);
    // must send our node's "modify" record first, otherwise
    // the target can't reply!
    Map modObj;
    synchronized (nodeModifyLock) {
      modObj = nodeModify;
    }
    if (modObj == null) {
      if (logger.isWarnEnabled()) {
        logger.warn(
            "Sending ping("+addr+
            ") without first sending reply-to data for "+agentId);
      }
    } else {
      WPQuery modq = new WPQuery(
          agentId,
          target,
          now,
          WPQuery.MODIFY,
          modObj);
      sendOrQueue(modq);
    }
    WPQuery wpq = new WPQuery(
        agentId,
        target,
        now,
        WPQuery.PING,
        null);
    sendOrQueue(wpq);
  }

  private void lookup(Map m) {
    send(true, m);
  }

  private void modify(Map m) {
    send(false, m);
  }
  
  private void releaseNow() {
    // call "send" with null, which will examine the releaseTime
    send(true, null);
  }

  private void checkDeadlinesNow() {
    // call "send" with null, which will examine the checkDeadlinesTime
    send(false, null);
  }

  private void send(boolean lookup, Map m) {
    stats(lookup).send(m);

    // The various callers are:
    //   - our clients (cache, leases)
    //   - our own releaseThread (adds batching delay)
    //   - our own check checkDeadlinesThread (check for timeouts
    //     or new servers)
    // These last two clients pass a null map.

    // stuff we will send:  (target => map(name => sendObj))
    Map lookupsToSend;
    Map modifiesToSend;

    // save modify-node record for ping use
    updateNodeModify(lookup, m); 

    synchronized (myLock) {
      try {
        // initialize temporary variables
        init();

        // create entries for the new queries
        checkSendMap(lookup, m);

        if (!canSendMessages()) {
          // no MTS yet?  We'll kick a thread when the MTS shows up
          return;
        }

        // check for delayed release entries, even if we're not the
        // releaseThread
        checkReleaseTimer();

        // check for message timeouts, even if we're not the
        // checkDeadlinesThread
        checkDeadlineTimer();

        if (!shouldReleaseNow()) {
          // our releaseThread will wake us later, allowing us to
          // batch these requests.
          return;
        }

        if (!collectMessagesToSend()) {
          // nothing to send.  Another possibility is that there are
          // no WP servers yet, in which case we'll kick a thread when
          // they show up.
          return;
        }

        // we're sending something now, so make sure we'll wake
        // up later to check timeouts
        ensureDeadlineTimer();

        // take maps stuff we will send
        lookupsToSend = takeMessagesToSend(true);
        modifiesToSend = takeMessagesToSend(false);
      } finally {
        cleanup();
      }
    }

    // send messages
    sendAll(lookupsToSend, modifiesToSend);
  }

  private void init() {
    now = System.currentTimeMillis();

    sendNow =
      (config.nagleMillis <= 0 ||
       (releaseTime > 0 && releaseTime <= now));

    sendLater = false;

    // these should already be cleared by "cleanup()":
    lookupNames.clear();
    modifyNames.clear();
    lookupAddrs.clear();
    modifyAddrs.clear();
  }

  private void updateNodeModify(boolean lookup, Map m) {
    synchronized (nodeModifyLock) {
      Map newM = 
        Util.updateNodeModify(
            lookup,
            m,
            agentId,
            nodeModify);
      if (newM != nodeModify) {
        if (logger.isDetailEnabled()) {
          logger.detail(
              "updated node "+agentId+
              " modify from "+nodeModify+" to "+newM);
        }
        nodeModify = newM;
      }
    }
  }

  private void checkSendMap(boolean lookup, Map m) {
    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    // check to see if this map contains a forced sendNow
    if (config.noListNagle && !sendNow) {
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object query = me.getValue();
        if (Util.mustSendNow(lookup, name, query)) {
          if (logger.isDetailEnabled()) {
            logger.detail(
                "mustSendNow("+lookup+", "+name+", "+query+")");
          }
          sendNow = true;
          break;
        }
      }
    }

    Map table = (lookup ? lookups : mods);
    Set names = (lookup ? lookupNames : modifyNames);
    Iterator iter = m.entrySet().iterator();
    for (int i = 0; i < n; i++) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Object query = me.getValue();
      Entry e = (Entry) table.get(name);
      // add to queue
      if (e != null &&
          !shouldSend(lookup, name, query, e.getQuery())) {
        continue;
      }
      // add or replace the entry
      e = new Entry(query, now);
      table.put(name, e);
      if (sendNow) {
        names.add(name);
        continue;
      }
      sendLater = true;
      if (logger.isDetailEnabled()) {
        logger.detail(
            "delaying initial release of "+
            (lookup ? "lookup" : "modify")+
            " "+name+"="+query);
      }
      stats(lookup).later();
    }
  }

  private boolean shouldSend(
      boolean lookup,
      String name,
      Object query,
      Object sentObj) {
    try {
      if (Util.shouldSend(lookup, name, query, sentObj)) {
        return true;
      }
    } catch (Exception err) {
      if (logger.isErrorEnabled()) {
        logger.error("shouldSend failed", err);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Not sending "+
          (lookup ? "lookup" : "modify")+
          " (name="+name+" query="+query+
          "), since we've already sent "+sentObj);
    }
    return false;
  }

  private boolean canSendMessages() {
    if (hasMessageTransport()) {
      return true;
    }
    if (logger.isDetailEnabled()) {
      logger.detail("waiting for message transport");
    }
    return false;
  }

  private void checkReleaseTimer() {
    if (!sendNow || config.nagleMillis <= 0) {
      return;
    }
    if (releaseTime > 0) {
      // timer is due
      releaseTime = 0;
      // cancel the timer.  This is a no-op if it's our thread.
      releaseThread.cancelTimer();
    }
    // find due entries (optimize me?)
    for (int t = 0; t < 2; t++) { 
      boolean tlookup = (t == 0);
      Map table = (tlookup ? lookups : mods);
      int tsize = table.size();
      if (tsize <= 0) {
        continue;
      }
      Set names = (tlookup ? lookupNames : modifyNames);
      Iterator iter = table.entrySet().iterator();
      for (int i = 0; i < tsize; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Entry e = (Entry) me.getValue();
        if (e.getTarget() != null) {
          continue;
        }
        names.add(name);
      }
    }
  }

  private void checkDeadlineTimer() {
    if (checkDeadlinesTime <= 0 || checkDeadlinesTime > now) {
      return;
    }
    // timer is due
    checkDeadlinesTime = 0;
    // now's a good time to dump debugging info
    debugQueues();
    boolean anyStillPending = false;
    // find due entries (optimize me?)
    for (int t = 0; t < 2; t++) { 
      boolean tlookup = (t == 0);
      Map table = (tlookup ? lookups : mods);
      int tsize = table.size();
      if (tsize <= 0) {
        continue;
      }
      Set names = (tlookup ? lookupNames : modifyNames);
      Iterator iter = table.entrySet().iterator();
      for (int i = 0; i < tsize; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Entry e = (Entry) me.getValue();
        MessageAddress target = e.getTarget();
        if (target == null && sendNow && config.nagleMillis > 0) {
          // waiting for releaseThread
          continue;
        }
        if (target != null && target != NULL_ADDR) {
          if (selectService.contains(target)) {
            long deadline = e.getDeadline();
            if (deadline <= 0 || deadline > now) {
              // give it more time for the ack
              anyStillPending = true; 
              continue;
            } 
            if (shortcutNodeModify(tlookup, name, e, now)) {
              // unusual case: local-node uid-based modify
              continue;
            }
          }
          // update server stats
          selectService.update(
              target,
              (now - e.getSendTime()),
              true);
        }
        if (!sendNow && logger.isDetailEnabled()) {
          logger.detail(
              "delaying retry release of "+
              (tlookup ? "lookup" : "modify")+
              " "+name+"="+e.getQuery()+", entry="+e.toString(now));
        }
        stats(tlookup).retry();
        e.setTarget(null);
        if (sendNow) {
          names.add(name);
          continue;
        }
        sendLater = true;
        stats(tlookup).later();
      }
    }
    if (anyStillPending) {
      // schedule our next deadline check
      ensureDeadlineTimer();
    }
  }

  /**
   * Special test for local-node uid-based modify requests.
   */
  private boolean shortcutNodeModify(
      boolean lookup,
      String name,
      Entry e,
      long now) {
    // replace with modify(ourNodeModify)
    Object query = e.getQuery();
    Object answer = Util.shortcutNodeModify(lookup, agentId, name, query);
    if (answer == null) {
      return false;
    }
    Map m = Collections.singletonMap(name, answer);
    WPAnswer wpa = new WPAnswer(
        e.getTarget(),   // from the server
        agentId,         // back to us
        e.getSendTime(), // our sendTime
        now,             // the "server" sendTime
        true,            // use the above time
        WPAnswer.MODIFY, // modify
        m);              // the lease-not-known answer
    if (logger.isInfoEnabled()) {
      logger.info(
          "Timeout waiting for uid-based modify response"+
          " (name="+name+" query="+query+
          "), pretending that the server"+
          " sent back a lease-not-known response: "+
          wpa);
    }
    receive(wpa);
    return true;
  }

  private boolean shouldReleaseNow() {
    if (sendNow || !sendLater) {
      return true;
    }
    // make sure timer is running to send later
    if (releaseTime == 0) {
      // start timer
      releaseTime = now + config.nagleMillis; 
      if (logger.isDetailEnabled()) {
        logger.detail("starting delayed release timer");
      }
      releaseThread.schedule(config.nagleMillis);
    }
    // wait for timer
    if (logger.isDetailEnabled()) {
      logger.detail(
          "waiting "+(releaseTime - now)+" for release timer");
    }
    return false;
  }

  private boolean collectMessagesToSend() {
    boolean anyToSend = false;
    for (int x = 0; x < 2; x++) {
      boolean xlookup = (x == 0);
      Set names = (xlookup ? lookupNames : modifyNames);
      if (names.isEmpty()) {
        continue;
      }
      Iterator iter = names.iterator();
      for (int i = 0, nsize = names.size(); i < nsize; i++) {
        String name = (String) iter.next();
        Map table = (xlookup ? lookups : mods);
        Entry e = (Entry) table.get(name);
        // accessing the "selectService" within our lock may be an
        // issue someday, but for now we'll assume it's allowed
        MessageAddress target = 
          selectService.select(xlookup, name);
        if (target == null) {
          // no target?  mark entry
          e.setTarget(NULL_ADDR); 
          if (logger.isDetailEnabled()) {
            logger.detail(
                "queuing message until WP servers are available: "+
                (xlookup ? "lookup" : "modify")+" "+name+"="+
                e.toString(now));
          }
          continue;
        }
        e.setTarget(target);

        // wrap query for security
        Object query = e.getQuery();
        Object sendObj = query;
        if (query != null) {
          sendObj = wrapQuery(xlookup, name, query);
          if (sendObj == null) {
            // wrapping rejected this query
            table.remove(name); 
            continue;
          }
        }

        anyToSend = true;

        // set timestamps
        e.setSendTime(now);
        long deadline = MessageTimeoutUtils.getDeadline(target);
        if (deadline > 0 && graceTime > 0 && graceTime > deadline) {
          // extend deadline to match initial "grace" period
          deadline = graceTime;
        }
        e.setDeadline(deadline);

        // add to (target => map(name => sendObj))
        Map xaddrs = (xlookup ? lookupAddrs : modifyAddrs);
        if (nsize == 1) {
          // minor optimization for single-element map
          xaddrs.put(target, Collections.singletonMap(name, sendObj));
          break;
        }
        Map addrMap = (Map) xaddrs.get(target);
        if (addrMap == null) {
          addrMap = new HashMap();
          xaddrs.put(target, addrMap);
        }
        // assert (!addrMap.containsKey(name));
        addrMap.put(name, sendObj);
      }
    }

    return anyToSend;
  }

  private void ensureDeadlineTimer() {
    if (checkDeadlinesTime > 0) {
      return;
    }
    // schedule our next deadline check
    checkDeadlinesTime = now + config.checkDeadlinesPeriod;
    if (logger.isDetailEnabled()) {
      logger.detail(
          "will send messages, scheduling timer to check deadlines");
    }
    checkDeadlinesThread.schedule(config.checkDeadlinesPeriod);
  }

  private Map takeMessagesToSend(boolean lookup) {
    Map addrs = (lookup ? lookupAddrs : modifyAddrs);
    int n = addrs.size();
    if (n == 0) {
      return null;
    }
    if (n == 1) {
      Iterator iter = addrs.entrySet().iterator();
      Map.Entry me = (Map.Entry) iter.next();
      return Collections.singletonMap(me.getKey(), me.getValue());
    }
    return new HashMap(addrs);
  }

  private void cleanup() {
    now = 0;
    sendNow = false;
    sendLater = false;
    lookupNames.clear();
    modifyNames.clear();
    lookupAddrs.clear();
    modifyAddrs.clear();
  }

  private void sendAll(Map lookupsToSend, Map modifiesToSend) {
    // send messages
    //
    // send the modifications first, so a lookup that matches our
    // own modifications will see our modifications instead of
    // the pre-modification state.
    //
    // we send the lookups and modifies separately, even if they're
    // going to the same target.  We lose some of our batching, but
    // this simplfies the security message-content checks.
    long now = System.currentTimeMillis();
    sendAll(false, modifiesToSend, now);
    sendAll(true, lookupsToSend, now);
  }

  private void sendAll(boolean lookup, Map addrMap, long now) {
    stats(lookup).sendAll(addrMap);
    int n = (addrMap == null ? 0 : addrMap.size());
    if (n == 0) {
      return;
    }
    Iterator iter = addrMap.entrySet().iterator();
    for (int i = 0; i < n; i++) {
      Map.Entry me = (Map.Entry) iter.next();
      MessageAddress target = (MessageAddress) me.getKey();
      Map map = (Map) me.getValue();
      send(lookup, target, map, now);
    }
  }

  private void send(
      boolean lookup,
      MessageAddress target,
      Map map,
      long now) {
    if (target == NULL_ADDR) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "queuing message until WP servers are available: "+
            (lookup ? "lookup" : "modify")+" "+ map);
      }
    } else {
      WPQuery wpq = new WPQuery(
          agentId, target, now,
          (lookup ? WPQuery.LOOKUP : WPQuery.MODIFY),
          map);
      if (logger.isDetailEnabled()) {
        logger.detail("sending message: "+wpq);
      }
      sendOrQueue(wpq);
    }
  }
  
  private Object wrapQuery(
      boolean lookup,
      String name,
      Object query) {
    if (lookup || protectS == null) {
      return query;
    }
    // wrap sendObj using protection service
    String agent; 
    if (query instanceof NameTag) {
      agent = ((NameTag) query).getName();
    } else {
      agent = agentId.getAddress();
    }
    WhitePagesProtectionService.Wrapper wrapper;
    try {
      wrapper = protectS.wrap(agent, query);
      if (wrapper == null) {
        throw new RuntimeException("Wrap returned null");
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(
            "Unable to wrap (agent="+agent+" name="+name+
            " query="+query+")", e);
      }
      wrapper = null;
    }
    Object ret = new NameTag(agent, wrapper);
    if (logger.isDetailEnabled()) {
      logger.detail(
          "wrapped (agent="+agent+" name="+name+" query="+
          query+") to "+ret);
    }
    return ret;
  }

  //
  // receive:
  //

  @Override
protected boolean shouldReceive(Message m) {
    if (m instanceof WPAnswer) {
      WPAnswer wpa = (WPAnswer) m;
      int action = wpa.getAction();
      return 
        (action == WPAnswer.LOOKUP ||
         action == WPAnswer.MODIFY ||
         action == WPAnswer.PING);
    }
    return false;
  }

  @Override
protected void receiveNow(Message msg) {
    if (logger.isDetailEnabled()) {
      logger.detail("receiving message: "+msg);
    }

    WPAnswer wpa = (WPAnswer) msg;
    int action = wpa.getAction();

    MessageAddress addr = wpa.getOriginator();
    long sendTime = wpa.getSendTime();
    long now = System.currentTimeMillis();
    long rtt = (now - sendTime);

    if (action == WPAnswer.PING) {
      List l = pingClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        PingService.Client c = (PingService.Client) l.get(i);
        c.pingAnswer(addr, rtt);
      }
      return;
    }

    boolean lookup = (action == WPAnswer.LOOKUP);
    Map m = wpa.getMap();

    stats(lookup).receiveNow(m);

    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }

    long replyTime = wpa.getReplyTime();
    boolean useServerTime = wpa.useServerTime();

    Map answerMap = null;

    // remove from pending queue
    synchronized (myLock) {
      Iterator iter = m.entrySet().iterator();
      for (int i = 0; i < n; i++) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object answer = me.getValue();
        // tell a queue
        if (!shouldReceive(lookup, addr, name, answer, now)) {
          continue;
        }
        if (n == 1) {
          answerMap = m;
          continue;
        }
        // add to the per-name map
        if (answerMap == null) {
          answerMap = new HashMap();
        }
        answerMap.put(name, answer);
      }

      if (answerMap == null) {
        return;
      }

      // reward the server
      selectService.update(addr, rtt, false);
    }

    // compute the base time
    long baseTime;
    if (useServerTime) {
      // use the server's clock
      baseTime = replyTime;
    } else {
      // use a round-trip-time estimate as defined in WPAnswer
      baseTime = sendTime + (rtt >> 1);
    }

    stats(lookup).accept(answerMap);

    // tell our clients
    if (lookup) {
      List l = lookupClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        LookupService.Client c = (LookupService.Client) l.get(i);
        c.lookupAnswer(baseTime, answerMap);
      }
    } else {
      List l = modifyClients.getUnmodifiableList();
      for (int i = 0, ln = l.size(); i < ln; i++) {
        ModifyService.Client c = (ModifyService.Client) l.get(i);
        c.modifyAnswer(baseTime, answerMap);
      }
    }
  }

  /**
   * Figure out if we should accept this request response, including
   * whether or not we sent it and any necessary ordering/version
   * tests.
   */
  private boolean shouldReceive(
      boolean lookup, 
      MessageAddress addr,
      String name,
      Object answer,
      long now) {
    // assert (Thread.holdsLock(myLock));

    Map table = (lookup ? lookups : mods);

    boolean accepted;
    Entry e = (Entry) table.get(name);
    if (e == null) {
      // not sent?
      accepted = false; 
    } else {
      Object sentObj = e.getQuery();
      accepted = Util.shouldReceive(lookup, name, answer, sentObj);
      if (accepted) {
        // clear the table entry
        table.remove(name);
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info(
          (accepted ? "Accepting" : "Ignoring")+
          " "+
          (lookup ? "lookup" : "modify")+
          " response (name="+
          name+", answer="+answer+
          ") returned by "+addr+
          ", since it "+
          (accepted ? "matches" : "doesn't match")+
          " our sent query: "+
          (e == null ? "<null>" : e.toString(now)));
    }

    return accepted;
  }


  //
  // debug printer:
  //

  private Stats stats(boolean lookup) {
    return (lookup ? lookupStats : modifyStats);
  }

  private void debugQueues() {
    if (!logger.isDebugEnabled()) {
      return;
    }

    // stats
    logger.debug("header, agent, "+stats(true).getHeader());
    logger.debug("lookup, "+agentId+", "+stats(true).getStats());
    logger.debug("modify, "+agentId+", "+stats(false).getStats());

    String currentServers = selectService.toString();
    synchronized (myLock) {
      String s = "";
      s += "\n##### client transport output queue #######################";
      s += "\nservers="+currentServers;
      long now = System.currentTimeMillis();
      boolean firstPass = true;
      while (true) {
        Map m = (firstPass ? lookups : mods);
        int n = m.size();
        s += 
          "\n"+
          (firstPass ? "lookup" : "modify")+
          "["+n+"]: ";
        if (n > 0) { 
          for (Iterator iter = m.entrySet().iterator();
              iter.hasNext();
              ) {
            Map.Entry me = (Map.Entry) iter.next();
            String name = (String) me.getKey();
            Entry e = (Entry) me.getValue();
            s += "\n   "+name+"\t => "+e.toString(now);
          }
        }
        if (firstPass)  {
          firstPass = false;
        } else {
          break;
        }
      }
      s += "\n###########################################################";
      logger.debug(s);
    }
  }

  //
  // classes:
  //

  private static class Entry {

    private final Object query;

    private final long creationTime;

    private long sendTime;
    private long deadline;
    private MessageAddress target;

    public Entry(Object query, long now) {
      this.query = query;
      this.creationTime = now;
    }

    public Object getQuery() {
      return query;
    }

    public long getCreationTime() {
      return creationTime;
    }

    public long getSendTime() {
      return sendTime;
    }
    public void setSendTime(long sendTime) {
      this.sendTime = sendTime;
    }

    public long getDeadline() {
      return deadline;
    }
    public void setDeadline(long deadline) {
      this.deadline = deadline;
    }

    public MessageAddress getTarget() {
      return target;
    }
    public void setTarget(MessageAddress target) {
      this.target = target;
    }
    
    @Override
   public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return 
        "(created="+Timestamp.toString(getCreationTime(), now)+
        " sent="+Timestamp.toString(getSendTime(), now)+
        " deadline="+Timestamp.toString(getDeadline(), now)+
        " target="+getTarget()+
        " query="+getQuery()+
        ")";
    }
  }

  private static class Stats {

    private final Object lock = new Object();

    private int count;
    private int size;
    private int later;
    private int sendCount;
    private int sendSize;
    private int retrySize;
    private int receiveCount;
    private int receiveSize;
    private int acceptCount;
    private int acceptSize;

    private String getHeader() {
      return
        "count"+
        ", size"+
        ", later"+
        ", sendC"+
        ", sendS"+
        ", retryS"+
        ", recvC"+
        ", recvS"+
        ", accC"+
        ", accS";
    }

    private String getStats() {
      synchronized (lock) {
        return
          count+
          ", "+size+
          ", "+later+
          ", "+sendCount+
          ", "+sendSize+
          ", "+retrySize+
          ", "+receiveCount+
          ", "+receiveSize+
          ", "+acceptCount+
          ", "+acceptSize;
      }
    }

    private void send(Map m) {
      int s = (m == null ? 0 : m.size());
      if (s <= 0) {
        return;
      }
      synchronized (lock) {
        count++;
        size += s;
      }
    }
    private void later() {
      synchronized (lock) {
        later++;
      }
    }
    private void sendAll(Map addrMap) {
      int n = (addrMap == null ? 0 : addrMap.size());
      if (n <= 0) {
        return;
      }
      synchronized (lock) {
        sendCount += n;
        int s = 0;
        Iterator iter = addrMap.entrySet().iterator();
        for (int i = 0; i < n; i++) {
          Map.Entry me = (Map.Entry) iter.next();
          Map m = (Map) me.getValue();
          s += m.size();
        }
        sendSize += s;
      }
    }
    private void retry() {
      synchronized (lock) {
        retrySize++;
      }
    }
    private void receiveNow(Map m) {
      synchronized (lock) {
        receiveCount++;
        int n = (m == null ? 0 : m.size());
        receiveSize += n;
      }
    }
    private void accept(Map answerMap) {
      synchronized (lock) {
        acceptCount++;
        int n = (answerMap == null ? 0 : answerMap.size());
        acceptSize += n;
      }
    }
  }

  private abstract class SPBase extends ServiceProviderBase {
    protected abstract int getAction();
    @Override
   protected void register(Object client) {
      ClientTransport.this.register(getAction(), client);
    }
    @Override
   protected void unregister(Object client) {
      ClientTransport.this.unregister(getAction(), client);
    }
  }

  private class PingSP extends SPBase {
    @Override
   protected int getAction() { return WPQuery.PING; }
    @Override
   protected Class getServiceClass() { return PingService.class; }
    @Override
   protected Class getClientClass() { return PingService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements PingService {
      public SI(Object client) { super(client); }
      public void ping(MessageAddress addr, long deadline) {
        ClientTransport.this.ping(addr, deadline);
      }
    }
  }
  private class LookupSP extends SPBase {
    @Override
   protected int getAction() { return WPQuery.LOOKUP; }
    @Override
   protected Class getServiceClass() { return LookupService.class; }
    @Override
   protected Class getClientClass() { return LookupService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements LookupService {
      public SI(Object client) { super(client); }
      public void lookup(Map m) {
        ClientTransport.this.lookup(m);
      }
    }
  }
  private class ModifySP extends SPBase {
    @Override
   protected int getAction() { return WPQuery.MODIFY; }
    @Override
   protected Class getServiceClass() { return ModifyService.class; }
    @Override
   protected Class getClientClass() { return ModifyService.Client.class; }
    @Override
   protected Service getService(Object client) { return new SI(client); }
    protected class SI extends MyServiceImpl implements ModifyService {
      public SI(Object client) { super(client); }
      public void modify(Map m) {
        ClientTransport.this.modify(m);
      }
    }
  }

  /** config options */
  private static class ClientTransportConfig {
    public final long nagleMillis;
    public final boolean noListNagle;
    public final long checkDeadlinesPeriod;
    public final long graceMillis;

    public ClientTransportConfig(Object o) {
      Parameters p =
        new Parameters(o, "org.cougaar.core.wp.resolver.transport.");
      nagleMillis = p.getLong("nagleMillis", 0);
      noListNagle = p.getBoolean("noListNagle", false);
      checkDeadlinesPeriod = p.getLong("checkDeadlinesPeriod", 10000);
      graceMillis = p.getLong("graceMillis", 0);
    }
  }
}
