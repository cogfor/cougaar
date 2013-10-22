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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.ListAllNodes;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This component adds the agent's {@link DemoControlService}.
 * <p>
 * This implementation uses the {@link MessageSwitchService} to
 * send the society time-advance requests to other nodes.  Other
 * implementation options included blackboard relays (but blackboard
 * transactions could complicate matters) and servlets (but more
 * difficult to achieve parallelism).
 * 
 * @property org.cougaar.core.agent.demoControl.namingTimeout
 *   Timeout in milliseconds for a DemoControlService setSocietyTime
 *   lookup of all target nodes in the naming service.  Defaults to
 *   30000 millis.
 */
public class DemoControl
extends GenericStateModelAdapter
implements Component
{

  private static final long NAMING_LOOKUP_TIMEOUT =
    SystemProperties.getLong(
        "org.cougaar.core.agent.demoControl.namingTimeout",
         30000);

  private static final Long PENDING = new Long(-1);

  protected ServiceBroker sb;

  protected LoggingService log;
  protected MessageAddress localAgent;
  protected MessageAddress localNode;
  protected WhitePagesService wp;

  private NaturalTimeService xTimer;
  private UIDService uidService;
  private MessageSwitchService mss;

  private final Object lock = new Object();
  private final Map table = new HashMap(3);

  private ServiceProvider dcsp;

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  @Override
public void load() {
    super.load();

    localAgent = find_local_agent();
    localNode = find_local_node();

    log = sb.getService(this, LoggingService.class, null);
    log = LoggingServiceWithPrefix.add(log, localAgent+": ");

    // get execution timer
    xTimer = sb.getService(this, NaturalTimeService.class, null);
    if (xTimer == null) {
      throw new RuntimeException(
          "Unable to obtain NaturalTimeService");
    }

    uidService = sb.getService(this, UIDService.class, null);
    if (uidService == null) {
      throw new RuntimeException(
          "Unable to obtain UIDService");
    }

    wp = sb.getService(this, WhitePagesService.class, null);
    if (wp == null) {
      throw new RuntimeException(
          "Unable to obtain WhitePagesService");
    }

    mss = sb.getService(this, MessageSwitchService.class, null);
    if (mss == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchService");
    }

    // register message handler for DemoControlMessages
    MessageHandler mh = new MessageHandler() {
      public boolean handleMessage(Message message) {
        if (message instanceof DemoControlMessage) {
          receiveLater((DemoControlMessage) message);
          return true;
        } else {
          return false;
        }
      }
    };
    mss.addMessageHandler(mh);

    // add demo control
    dcsp = new DemoControlSP();
    sb.addService(DemoControlService.class, dcsp);
  }

  @Override
public void unload() {
    super.unload();

    sb.revokeService(DemoControlService.class, dcsp);
    dcsp = null;

    if (mss != null) {
      // mss.unregister?
      sb.releaseService(this, MessageSwitchService.class, mss);
      mss = null;
    }

    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }

    if (wp != null) {
      sb.releaseService(this, WhitePagesService.class, wp);
      wp = null;
    }

    if (xTimer != null) {
      sb.releaseService(this, NaturalTimeService.class, xTimer);
      xTimer = null;
    }

    if (log != null) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  /**
   * Get the set of all node names in the society.
   * <p>
   * This method is <tt>protected</tt> to allow a subclass to specify
   * an alternate implementation, e.g.:<ul>
   *   <li>read a config file or external authority</li>
   *   <li>return a partial list for a split society</li>
   *   <li>return just the agents that share the same execution
   *       time as this agent, in case agents in the same society
   *       or even node have different execution clocks</li>
   *   <li>etc</li> 
   * </ul> 
   */
  protected Set getSocietyTargets() {
    Set names;
    try {
      names = ListAllNodes.listAllNodes(
          wp,
          NAMING_LOOKUP_TIMEOUT);
    } catch (Exception e) {
      log.warn("Unable to listAllNodes", e);
      names = null;
    }
    boolean hasLocalNode = false;
    Set ret;
    int n = (names == null ? 0 : names.size());
    if (n == 0) {
      ret = Collections.EMPTY_SET;
    } else {
      ret = new HashSet(n);
      Iterator iter = names.iterator();
      for (int i = 0; i < n; i++) {
        String s = (String) iter.next();
        MessageAddress addr = MessageAddress.getMessageAddress(s);
        if (localNode.equals(addr)) {
          hasLocalNode = true;
        }
        ret.add(addr);
      }
    }
    if (!hasLocalNode) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Society node list["+ret.size()+"] from the naming"+
            " service lacks local node "+localNode+", adding it to"+
            " the list, but note that this suggests that other"+
            " nodes may be missing from the list! "+ret);
      }
      if (ret.isEmpty()) {
        ret = Collections.singleton(localNode);
      } else {
        ret.add(localNode);
      }
    }
    if (log.isInfoEnabled()) {
      log.info("WP lookup found nodes["+n+"]="+ret);
    }
    return ret;
  }

  //
  // the rest is private!
  //

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

  private MessageAddress find_local_node() {
    NodeIdentificationService nis = sb.getService(this, NodeIdentificationService.class, null);
    if (nis == null) {
      return null;
    }
    MessageAddress ret = nis.getMessageAddress();
    sb.releaseService(
        this, NodeIdentificationService.class, nis);
    return ret;
  }

  private void setLocalTime(
      long offset, double rate, long changeTime) {
    ExecutionTimer.Parameters p =
      new ExecutionTimer.Parameters(rate, offset, changeTime);
    if (log.isInfoEnabled()) {
      log.info("setSocietyTime(p="+p+")");
    }
    xTimer.setParameters(p);
  }

  private boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Set targets,
      long timeout) {
    // submit async request
    BlockingCallback bcb = new BlockingCallback();
    long startTime = System.currentTimeMillis(); 
    setSocietyTime(offset, rate, changeTime, targets, bcb);
    // block for the answer
    boolean ret = 
      bcb.waitForIsComplete(timeout);
    if (!ret && log.isWarnEnabled()) {
      log.warn(
          "Interrupted setSocietyTime("+
          offset+", "+rate+", "+changeTime+") after "+
          (System.currentTimeMillis() - startTime)+
          " millis, did not receive all \"acks\" yet!");
    }
    return ret;
  }

  private boolean setSocietyTime(
      long offset,
      double rate,
      long changeTime,
      Set targets,
      DemoControlService.Callback cb) {
    ExecutionTimer.Parameters p =
      new ExecutionTimer.Parameters(rate, offset, changeTime);
    int n = (targets == null ? 0 : targets.size());
    if (log.isInfoEnabled()) {
      log.info("setSocietyTime(p="+p+", targets["+n+"], cb="+cb+")");
    }
    // make sure our local node is listed, otherwise the naming
    // service likely contains partial data.  We want to set the
    // time on *all* nodes.
    Map m = new HashMap(n);
    boolean hasLocalAgent = false;
    if (n > 0) {
      Iterator iter = targets.iterator();
      for (int i = 0; i < n; i++) {
        MessageAddress addr = (MessageAddress) iter.next();
        MessageAddress key = addr.getPrimary();
        Object value;
        if (localAgent.equals(key)) {
          hasLocalAgent = true;
          value = new Long(0);
        } else {
          value = PENDING;
        }
        m.put(key, value);
      }
    }
    if (cb != null) {
      cb.sendingTimeAdvanceTo(
          Collections.unmodifiableSet(m.keySet()));
    }
    if (n == 1 && hasLocalAgent) {
      if (log.isInfoEnabled()) {
        log.info(
           "Just setting local agent's time to "+p+
           (cb == null ? "" : ", then invoking callback"));
      }
      // just local agent
      setLocalTime(offset, rate, changeTime);
      if (cb != null) {
        cb.updatedTime(localAgent, 0);
        m = Collections.singletonMap(localAgent, new Long(0));
        cb.completed(m);
      }
      return true;
    }
    // choose change_time that's better than the default:
    //   NaturalTimeService.DEFAULT_CHANGE_DELAY
    /* 
    long now = System.currentTimeMillis();
    long change_time =
      now +
      10000 +
      (targets.size() >> 1);
      */
    // FIXME modify p with change_time?
    // set local node's time
    if (hasLocalAgent) { 
      setLocalTime(offset, rate, changeTime);
    }
    UID uid = uidService.nextUID();
    synchronized (lock) {
      Entry e = new Entry();
      table.put(uid, e);
      e.setMap(m);
      e.setPendingCount(n - (hasLocalAgent ? 1 : 0));
      e.setCallback(cb);
      e.setSendTime(System.currentTimeMillis());
    }
    // send messages
    if (log.isInfoEnabled()) {
      log.info(
          "Sending messages (uid="+uid+", p="+p+") to targets["+n+
          "]="+targets);
    }
    Iterator iter = targets.iterator();
    for (int i = 0; i < n; i++) {
      MessageAddress addr = (MessageAddress) iter.next();
      MessageAddress key = addr.getPrimary();
      if (localAgent.equals(key)) {
        continue;
      }
      DemoControlMessage dcm = new DemoControlMessage(
        localAgent,
        addr,
        uid,
        p,
        false);
      mss.sendMessage(dcm);
    }
    return false;
  }

  private void receiveLater(DemoControlMessage dcm) {
    // we could put this on a queue and "thread.start()", to avoid
    // blocking the MTS, but xTimer shouldn't block, so we'll process
    // this message in the MTS thread.
    receiveNow(dcm);
  }

  private void receiveNow(DemoControlMessage dcm) {
    if (log.isDebugEnabled()) {
      log.debug("receiveNow("+dcm+")");
    }
    MessageAddress sender = dcm.getOriginator();
    if (sender.equals(localAgent)) {
      if (log.isWarnEnabled()) {
        log.warn("Ignoring message from self: "+dcm);
      }
      return;
    }
    if (!dcm.isAck()) {
      // client
      receiveAdvanceRequest(dcm);
      return;
    }
    // server
    receiveAck(dcm);
  }

  private void receiveAdvanceRequest(DemoControlMessage dcm) {
    // receive request to set our time, sent back ack
    MessageAddress sender = dcm.getOriginator();
    UID uid = dcm.getUID();
    ExecutionTimer.Parameters p = dcm.getParameters();
    if (log.isInfoEnabled()) {
      log.info(
          "Accepting "+sender+" request uid="+uid+
          " to set local node's time to "+p+
          ", will send ack message");
    }
    // remote request to setLocalTime
    setLocalTime(p.theOffset, p.theRate, p.theChangeTime);
    // send ack back to sender
    DemoControlMessage ack_dcm =
      new DemoControlMessage(
          localAgent, sender, uid, null, true);
    mss.sendMessage(ack_dcm);
  }

  private void receiveAck(DemoControlMessage dcm) {
    // receive ack, make sure we sent it
    MessageAddress sender = dcm.getOriginator();
    UID uid = dcm.getUID();
    dcm.getParameters();
    DemoControlService.Callback cb;
    long rtt;
    Map resultMap;
    synchronized (lock) {
      Entry e = (Entry) table.get(uid);
      if (e == null) {
        if (log.isWarnEnabled()) {
          log.warn("Ignoring unknown ack uid="+uid+", msg="+dcm);
        }
        return;
      }
      Map m = e.getMap();
      MessageAddress key = sender.getPrimary();
      Object o = m.get(key);
      if (o != PENDING) {
        if (log.isDebugEnabled()) {
          log.debug(
             "Ignoring "+
             (o == null ? "Unknown" : "Duplicate("+o+")")+
             " ack sender="+key+", msg="+dcm+
             ", targets["+m.size()+"]="+m.keySet());
        }
        return;
      }
      rtt = System.currentTimeMillis() - e.getSendTime();
      int pendingCount = e.getPendingCount();
      if (log.isInfoEnabled()) {
        log.info(
            "  received uid="+uid+" ack["+
            (m.size() - pendingCount)+" / "+m.size()+
            "] target="+key+" rtt="+rtt);
      }
      m.put(key, new Long(rtt));
      pendingCount--;
      e.setPendingCount(pendingCount);
      cb = e.getCallback();
      if (pendingCount <= 0) {
        // all acks are in
        if (log.isInfoEnabled()) {
          log.info(
              "Received all acks["+m.size()+"]"+
              ", rtt="+rtt+
              (cb == null ? "" : ", invoking callback"));
        }
        table.remove(key);
      }
      if (cb == null) {
        return;
      }
      if (pendingCount > 0) {
        // still more pending
        resultMap = null;
      } else {
        // callback data is map<addr, rtt>
        resultMap = Collections.unmodifiableMap(m);
      }
    }
    cb.updatedTime(sender.getPrimary(), rtt);
    if (resultMap != null) {
      cb.completed(resultMap);
    }
  }

  private static class Entry {
    private Map m;
    private int n;
    private DemoControlService.Callback cb;
    private long sendTime;
    public Map getMap() {
      return m;
    }
    public void setMap(Map m) {
      this.m = m;
    }
    public int getPendingCount() {
      return n;
    }
    public void setPendingCount(int n) {
      this.n = n;
    }
    public DemoControlService.Callback getCallback() {
      return cb;
    }
    public void setCallback(DemoControlService.Callback cb) {
      this.cb = cb;
    }
    public long getSendTime() {
      return sendTime;
    }
    public void setSendTime(long sendTime) {
      this.sendTime = sendTime;
    }
    @Override
   public String toString() {
      return 
        "(entry sentTime="+sendTime+" n="+n+" m="+m+" cb="+cb+")";
    }
  }
  
  /**
   * Simple DemoControlService.Callback that blocks the
   * <tt>completed</tt> call.
   */
  private static class BlockingCallback
    implements DemoControlService.Callback {
      private final Object lock = new Object();
      private boolean isComplete = false;
      @SuppressWarnings("unused")
      public boolean isComplete() {
        synchronized (lock) {
          return isComplete;
        }
      }
      public boolean waitForIsComplete(long millis) {
        synchronized (lock) {
          if (isComplete) {
            return true;
          }
          try {
            SchedulableStatus.beginWait("society time advance");
            lock.wait(millis);
          } catch (InterruptedException ie) {
            return isComplete; // make PMD happy
          } finally {
            SchedulableStatus.endBlocking();
          }
          return isComplete;
        }
      }
      public void sendingTimeAdvanceTo(Set addrs) {}
      public void updatedTime(MessageAddress addr, long rtt) {}
      public void completed(Map m) {
        synchronized (lock) {
          isComplete = true;
          lock.notifyAll();
        }
      }
    }

  private class DemoControlSP implements ServiceProvider {
    private final DemoControlService SERVICE_INSTANCE =
      new DemoControlSI();

    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (DemoControlService.class.isAssignableFrom(serviceClass)) {
        return SERVICE_INSTANCE;
      } else {
        return null;
      }
    }

    public void releaseService(
        ServiceBroker sb, Object requestor,
        Class serviceClass, Object service) {
    }
  }

  private final class DemoControlSI implements DemoControlService {
    // alarm service:
    private void die() { throw new UnsupportedOperationException(); }
    @SuppressWarnings("unused")
   public long currentTimeMillis() { die(); return -1; }
    @SuppressWarnings("unused")
   public void addAlarm(Alarm alarm) { die(); }
    @SuppressWarnings("unused")
   public void addRealTimeAlarm(Alarm alarm) { die(); }

    // demo service:
    public double getExecutionRate() {
      return xTimer.getRate();
    }
    public void setLocalTime(
        long offset, double newRate, long changeTime) {
      DemoControl.this.setLocalTime(
          offset, newRate, changeTime);
    }
    public void setNodeTime(
        long time, double newRate, long changeTime) {
      setNodeTime(
        time, true, newRate,
        false, changeTime, true);
    }
    public void advanceNodeTime(long timePeriod, double newRate) {
      setNodeTime(
        timePeriod, false, newRate,
        false, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void setNodeTime(long time, double newRate) {
      setNodeTime(
        time, true, newRate,
        false, NaturalTimeService.DEFAULT_CHANGE_DELAY, true);
    }
    private void setNodeTime(
       long millis, boolean millisIsAbsolute, double newRate,
       boolean forceRunning, long changeDelay, boolean changeIsAbsolute) {
      ExecutionTimer.Parameters p = 
          createParameters(
            millis, millisIsAbsolute, newRate,
            forceRunning, changeDelay, changeIsAbsolute);
      DemoControl.this.setLocalTime(
          p.theOffset, p.theRate, p.theChangeTime);
    }

    private ExecutionTimer.Parameters createParameters(
        long millis, boolean millisIsAbsolute, double newRate,
        boolean forceRunning, long changeDelay, boolean changeIsAbsolute) {
      return
        xTimer.createParameters(
            millis, millisIsAbsolute, newRate,
            forceRunning, changeDelay, changeIsAbsolute);
    }

    public boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Set targets,
      Callback cb) {
      return DemoControl.this.setSocietyTime(
          offset, rate, changeTime, targets, cb);
    }
    public boolean setSocietyTime(
        long offset, double rate, long changeTime,
        DemoControlService.Callback cb) {
      return setSocietyTime(
          offset, rate, changeTime, getSocietyTargets(), cb);
    }

    public boolean setSocietyTime(
      long offset, double rate, long changeTime,
      Set targets,
      long timeout) {
      return DemoControl.this.setSocietyTime(
          offset, rate, changeTime, targets, timeout);
    }
    public boolean setSocietyTime(
        long offset, double rate, long changeTime,
        long timeout) {
      return setSocietyTime(
          offset, rate, changeTime, getSocietyTargets(), timeout);
    }

    public boolean setSocietyTime(
        long offset, double rate, long changeTime) {
      return setSocietyTime(
          offset, rate, changeTime, getSocietyTargets(), changeTime);
    }

    public void setSocietyTime(long time) {
      setSocietyTime(
          time, true, 0.0,
          false, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void setSocietyTime(long time, boolean running) {
      setSocietyTime(
          time, true, 0.0,
          running, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void setSocietyTimeRate(double newRate) {
      setSocietyTime(
          0L, false, newRate,
          false, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void advanceSocietyTime(long timePeriod) {
      setSocietyTime(
          timePeriod, false, 0.0,
          false, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void advanceSocietyTime(long timePeriod, boolean running) {
      setSocietyTime(
          timePeriod, false, 0.0,
          running, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void advanceSocietyTime(long timePeriod, double newRate) {
      setSocietyTime(
          timePeriod, false, newRate,
          false, NaturalTimeService.DEFAULT_CHANGE_DELAY, false);
    }
    public void advanceSocietyTime(ExecutionTimer.Change[] changes) {
      ExecutionTimer.Parameters[] params = xTimer.createParameters(changes);
      for (int i = 0; i < params.length; i++) {
        ExecutionTimer.Parameters p = params[i];
        setSocietyTime(
          p.theOffset, p.theRate, p.theChangeTime);
      }
    }
    private void setSocietyTime(
        long millis, boolean millisIsAbsolute, double newRate,
        boolean forceRunning, long changeDelay, boolean changeIsAbsolute) {
      ExecutionTimer.Parameters p = 
        createParameters(
            millis, millisIsAbsolute, newRate,
            forceRunning, changeDelay, changeIsAbsolute);
      setSocietyTime(
          p.theOffset, p.theRate, p.theChangeTime);
    }
  }
}
