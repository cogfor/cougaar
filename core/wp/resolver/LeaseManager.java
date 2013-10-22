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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.Parameters;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.BundleService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.RarelyModifiedList;

/**
 * This component watches for bind/unbind requests and maintains
 * the leases in the server.
 */
public class LeaseManager 
extends GenericStateModelAdapter
implements Component
{

  private LeaserConfig config;

  // name -> ActiveLease
  // Map<String, ActiveLease>
  private final Map leases = new HashMap();

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private LoggingService logger;
  private ThreadService threadService;
  private UIDService uidService;
  private ModifyService modifyService;

  private final ModifyService.Client myClient = 
    new ModifyService.Client() {
      public void modifyAnswer(long baseTime, Map m) {
        LeaseManager.this.modifyAnswer(baseTime, m);
      }
    };

  private LeaseSP leaseSP;
  private BundleSP bundleSP;

  private final RarelyModifiedList listeners =
    new RarelyModifiedList();

  //
  // renew leases:
  //

  private Schedulable renewLeasesThread;

  public void setParameter(Object o) {
    configure(o);
  }

  public void setServiceBroker(ServiceBroker sb) {
    this.sb = sb;
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new LeaserConfig(o);
  }

  @Override
public void load() {
    super.load();

    configure(null);

    // register for lookups
    modifyService = sb.getService(
       myClient, ModifyService.class, null);
    if (modifyService == null) {
      throw new RuntimeException(
          "Unable to obtain ModifyService");
    }

    Runnable renewLeasesRunner =
      new Runnable() {
        public void run() {
          // assert (thread == renewLeasesThread);
          renewLeases();
        }
      };
    renewLeasesThread = threadService.getThread(
        this,
        renewLeasesRunner,
        "White pages server renew leases");
    renewLeasesThread.schedule(config.checkLeasesPeriod);

    NodeControlService ncs = sb.getService(this, NodeControlService.class, null);
    if (ncs != null) {
      rootsb = ncs.getRootServiceBroker();
      sb.releaseService(this, NodeControlService.class, ncs);
    }

    // advertise our services
    leaseSP = new LeaseSP();
    sb.addService(LeaseService.class, leaseSP);
    bundleSP = new BundleSP();
    ServiceBroker bundleSB = (rootsb == null ? sb : rootsb);
    bundleSB.addService(BundleService.class, bundleSP);
  }

  @Override
public void unload() {
    // cancel existing scheduled threads
    renewLeasesThread.cancel();

    // release services
    if (bundleSP != null) {
      ServiceBroker bundleSB = (rootsb == null ? sb : rootsb);
      bundleSB.revokeService(BundleService.class, bundleSP);
      bundleSP = null;
    }
    if (leaseSP != null) {
      sb.revokeService(LeaseService.class, leaseSP);
      leaseSP = null;
    }
    if (modifyService != null) {
      sb.releaseService(
          myClient, ModifyService.class, modifyService);
      modifyService = null;
    }
    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    if (threadService != null) {
      // halt our threads?
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }
    super.unload();
  }


  private Bundle getBundle(String name) {
    synchronized (leases) {
      ActiveLease lease = (ActiveLease) leases.get(name);
      return asBundle(name, lease);
    }
  }
  private Map getAllBundles() {
    synchronized (leases) {
      Map ret = new HashMap(leases.size());
      for (Iterator iter = leases.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        ActiveLease lease = (ActiveLease) me.getValue();
        Bundle bundle = asBundle(name, lease);
        if (bundle == null) {
          continue;
        }
        ret.put(name, bundle);
      }
      return ret;
    }
  }
  private Bundle asBundle(String name, ActiveLease lease) {
    if (lease == null) {
      return null;
    }
    Record record = lease.record;
    if (record == null) {
      return null;
    }
    UID uid = record.getUID();
    long ttd;
    if (lease.sendTime > 0) {
      // still pending
      ttd = config.minBundleTTD;
    } else {
      // use server ttd
      ttd = lease.expireTime - lease.boundTime;
      if (ttd < config.minBundleTTD) {
        ttd = config.minBundleTTD;
      }
    }
    Map entries = (Map) record.getData();
    Bundle bundle = new Bundle(name, uid, ttd, entries);
    return bundle;
  }
  private void register(BundleService.Client bsc) {
    listeners.add(bsc);
    bsc.addAll(getAllBundles());
  }
  private void unregister(BundleService.Client bsc) {
    listeners.remove(bsc);
  }

  private void submit(Response res, String agent) {
    // watch for bind/unbind
    Request req = res.getRequest();
    if (req instanceof Request.Bind) {
      if (req.hasOption(Request.CACHE_ONLY)) {
        res.setResult(new Long(Long.MAX_VALUE));
      } else {
        bind(res, agent, (Request.Bind) req);
      }
    } else if (req instanceof Request.Unbind) {
      if (req.hasOption(Request.CACHE_ONLY)) {
        res.setResult(Boolean.TRUE);
      } else {
        unbind(res, agent, (Request.Unbind) req);
      }
    } else {
      // ignore
    }
  }

  private void bind(
      Response res, String agent, Request.Bind req) {
    long activeExpire = -1;
    List cancelledResponses = null;

    boolean createNewLease = false;
    String unifiedAgent = null;
    Record record = null;
    Bundle bundle = null;

    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();
    String type = ae.getType();

    if (logger.isInfoEnabled() && !req.isOverWrite()) {
      logger.info("Warning: treating bind as rebind: "+req);
    }

    synchronized (leases) {
      AddressEntry oldAE = null;
      List oldResponses = null;
      Map oldData = null;

      ActiveLease lease = (ActiveLease) leases.get(name);
      if (lease != null) {
        // get the bound/pending lease's entry
        oldData = (Map) lease.record.getData();
        oldAE = 
          (oldData == null ?
           (null) :
           (AddressEntry) oldData.get(type));
      }

      if (lease == null) {
        // new bind-pending lease
        createNewLease = true;
      } else if (ae.equals(oldAE)) {
        // the lease is active, so we know the status
        activeExpire = lease.getExpirationTime();
        if (logger.isDetailEnabled()) {
          logger.detail("lease already active: "+lease);
        }
      } else if (
          !lease.isBound() &&
          ae.equals(oldAE)) {
        // already in progress, we don't know the
        // status yet, so batch with our pending request.
        lease.addResponse(res);
        if (logger.isDetailEnabled()) {
          logger.detail(
              "lease rebind "+ae+
              " already in progress, batching: "+lease);
        }
        // batched here
      } else {
        // cancel old lease, initiate a new one
        //
        // Note that multiple pending binds may be in progress, so
        // we must handle these outstanding requests.  There are
        // two options:
        //   a) Cancel the outstanding requests and ignore the
        //      WP's answers when they arrive
        //   b) Send both asynchronously and tell the requests
        //      their answers, even if they conflict.
        // We'll go with (a) and cancel the pending requests.
        // This is more consistent with the notion of a
        // client-side lease manager, since only one binding will
        // be maintained and conflicts are a client-side error.
        //
        // Unbind must do a similar cancel.
        //
        createNewLease = true;
        // clear the old responses for cancelling
        cancelledResponses = lease.takeResponses(type);
        // keep the rest
        oldResponses = lease.takeResponses();
      }

      if (createNewLease) {
        // create a new lease and replace the old one
        if (oldAE != null && logger.isInfoEnabled()) {
          logger.info(
              "Binding replacement entry "+ae+
              " for current entry "+oldAE+
              " in lease "+lease);
        }

        Map data;
        int oldSize = (oldData == null ? 0 : oldData.size());
        if (oldSize == 0 ||
            (oldSize == 1 &&
             oldData.containsKey(type))) {
          data = Collections.singletonMap(type, ae);
          unifiedAgent = agent;
        } else {
          data = new HashMap(oldData);
          data.put(type, ae);
          data = Collections.unmodifiableMap(data);
          // figure out the "agent" owner, which is usually a mix of
          // null (for the MTS link) and a single agent (for the
          // clients within that agent).  If it's some other mix then
          // we'll generate a warning.
          String oldAgent = lease.agent;
          if (oldAgent == null) {
            unifiedAgent = agent;
          } else if (
              oldAgent.equals(agent) ||
              agent == null) {
            unifiedAgent = oldAgent;
          } else {
            // conflict!
            unifiedAgent = agent; 
            if (logger.isWarnEnabled()) {
              logger.warn(
                  "Agent "+agent+"'s lease request "+ae+
                  " is mixing with agent "+oldAgent+
                  "'s data for name="+name+"="+oldData+
                  ", assigning ownership to agent "+agent);
            }
          }
        }
        UID uid = uidService.nextUID();
        record = new Record(uid, -1, data);
        lease = new ActiveLease(unifiedAgent, record);
        leases.put(name, lease);
        if (oldResponses != null) {
          lease.addResponses(oldResponses);
        }
        lease.addResponse(res);

        if (oldAE == null && logger.isInfoEnabled()) {
          logger.info("Binding new lease: "+lease);
        }

        bundle = asBundle(name, lease);
      }
    }

    if (cancelledResponses != null) {
      // cancel conflicting pending binds
      for (int i = 0, n = cancelledResponses.size(); i < n; i++) {
        Response cancelledRes = (Response) cancelledResponses.get(i);
        cancelledRes.setResult(req);
      }
    }
    if (0 < activeExpire) {
      // lease already bound
      res.setResult(new Long(activeExpire));
      return;
    }

    if (!createNewLease) {
      return;
    }

    // tell listeners
    if (bundle != null) { 
      List l = listeners.getUnmodifiableList();
      for (Iterator iter = l.iterator(); iter.hasNext(); ) {
        BundleService.Client bsc = (BundleService.Client) iter.next();
        bsc.add(name, bundle);
      }
    }

    // send
    Object o = record; 
    if (unifiedAgent != null) {
      o = new NameTag(unifiedAgent, o); 
    }
    Map m = Collections.singletonMap(name, o);
    modifyService.modify(m);
  }
  
  private void unbind(
      Response res, String agent, Request.Unbind req) {
    boolean bind = false;

    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();
    String type = ae.getType();

    List cancelledResponses = null;

    boolean createNewLease = false;
    String unifiedAgent = null;

    Record record = null;
    Bundle bundle = null;

    synchronized (leases) {
      AddressEntry oldAE = null;
      Map oldData = null;
      List oldResponses = null;

      ActiveLease lease = (ActiveLease) leases.get(name);
      if (lease != null) {
        // get the old entry
        record = lease.record;
        oldData = (Map) record.getData();
        oldAE = 
          (oldData == null ?
           (null) :
           (AddressEntry) oldData.get(type));
      }

      if (lease == null) {
        // not bound, nothing to unbind
      } else if (ae.equals(oldAE)) {
        // found exact match
        // cancel any pending requests
        //
        // this can also be used to intentionally cancel a
        // pending local bind.
        cancelledResponses = lease.takeResponses(type);
        oldResponses = lease.takeResponses();
        createNewLease = true;
      } else {
        // was not bound
      }

      if (createNewLease) {
        // create a new lease and replace the old one
        if (logger.isInfoEnabled()) {
          logger.info("Unbinding entry "+ae+" in lease "+lease);
        }

        Map data;
        if (oldData == null || oldData.isEmpty()) {
          if (bind) {
            data = Collections.singletonMap(type, ae);
          } else {
            data = Collections.EMPTY_MAP;
          }
        } else {
          data = new HashMap(oldData);
          if (bind) {
            data.put(type, ae);
          } else {
            data.remove(type);
          }
          data = Collections.unmodifiableMap(data);
        }
        unifiedAgent = (lease == null ? agent : lease.agent);
        UID uid = uidService.nextUID();
        record = new Record(uid, -1, data);
        if (!bind && oldData.size() == 1) {
          leases.remove(name);
          lease = null;
        } else {
          lease = new ActiveLease(unifiedAgent, record);
          leases.put(name, lease);
          if (oldResponses != null) {
            lease.addResponses(oldResponses);
          }
        }

        if (logger.isInfoEnabled()) {
          logger.info("New lease: "+lease);
        }

        bundle = asBundle(name, lease);
      }
    }

    if (cancelledResponses != null) {
      // cancel conflicting pending binds
      for (int i = 0, n = cancelledResponses.size(); i < n; i++) {
        Response cancelledRes = (Response) cancelledResponses.get(i);
        cancelledRes.setResult(req);
      }
    }

    if (!bind) {
      // it's not bound from the local lease's point of view
      //
      // if it's bound in the server without our knowledge,
      // the lease expiration will unbind it for us
      res.setResult(Boolean.TRUE);
    }

    if (!createNewLease) {
      return;
    }

    // tell listeners
    if (bundle != null) { 
      Map entries = bundle.getEntries();
      boolean rem = (entries == null || entries.isEmpty());
      List l = listeners.getUnmodifiableList();
      for (Iterator iter = l.iterator(); iter.hasNext(); ) {
        BundleService.Client bsc = (BundleService.Client) iter.next();
        if (rem) {
          bsc.remove(name, bundle);
        } else {
          bsc.add(name, bundle);
        }
      }
    }

    // send
    Object o = record; 
    if (unifiedAgent != null) {
      o = new NameTag(unifiedAgent, o); 
    }
    Map m = Collections.singletonMap(name, o);
    modifyService.modify(m);
  }

  private void modifyAnswer(long baseTime, Map m) {
    for (Iterator iter = m.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Object value = me.getValue();
      if (value instanceof Lease) {
        Lease l = (Lease) value;
        UID uid =  l.getUID();
        long ttd = l.getTTD();
        leaseSuccess(name, uid, baseTime, ttd);
      } else if (value instanceof LeaseNotKnown) {
        LeaseNotKnown lnk = (LeaseNotKnown) value;
        UID uid = lnk.getUID();
        leaseNotKnown(name, uid);
      } else if (value instanceof LeaseDenied) {
        LeaseDenied ld = (LeaseDenied) value;
        UID uid = ld.getUID();
        Object reason = ld.getReason();
        leaseDenied(name, uid, reason);
      } else {
        if (logger.isErrorEnabled()) {
          logger.error(
              "Unexpected modify answer: (baseTime="+
              Timestamp.toString(baseTime)+
              ", name="+name+
              ", value="+
              (value == null ?
               "" :
               "("+value.getClass().getName()+")")+
              value);
        }
      }
    }
  }

  private boolean matchesLease(
      String name,
      UID uid,
      ActiveLease lease,
      String info) {

    if (lease != null &&
        uid.equals(lease.record.getUID())) {
      return true;
    }

    // ignore the response.
    //
    // The ModifyService usually protects us against this, but
    // sometimes races can occur.
    //
    // if the lease is null:
    //   either we never bound this entry (e.g. restart) or we've
    //   recently unbound the entry and a stale ack/renewal has
    //   raced back.
    // or
    //   we sent a bind followed by a replacement bind, and we're
    //   waiting for the ack on that second bind.  This is similar
    //   to the above "lease == null" race.
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Ignoring a lease answer that we didn't send?"+
          " info="+info+
          " name="+name+
          " uid="+uid+
          " lease="+lease);
    }
    return false;
  }

  private void leaseSuccess(
      String name,
      UID uid,
      long baseTime,
      long ttd) {
    List responses;
    synchronized (leases) {
      ActiveLease lease = (ActiveLease) leases.get(name);
      if (!matchesLease(name, uid, lease, "success")) {
        return;
      }
      responses = leaseSuccess(lease, baseTime, ttd);
      if (responses == null || responses.isEmpty()) {
        return;
      }
    }
    Object result = new Long(baseTime + ttd);
    for (int i = 0, n = responses.size(); i < n; i++) {
      Response res = (Response) responses.get(i);
      res.setResult(result);
    }
  }

  private List leaseSuccess(
      ActiveLease lease,
      long baseTime,
      long ttd) {

    long ttl = baseTime + ttd;

    boolean renewal = (0 < lease.expireTime);

    // good, we've created a new lease or
    // renewed an existing lease
    long now = System.currentTimeMillis();
    renewed(lease, now, ttl);

    if (renewal) {
      if (logger.isDebugEnabled()) {
        logger.debug("Renewed lease="+lease);
      }
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("Established lease: "+lease);
      }
    }

    return lease.takeResponses();
  }

  private void leaseNotKnown(
      String name,
      UID uid) {
    synchronized (leases) {
      ActiveLease lease = (ActiveLease) leases.get(name);
      if (!matchesLease(name, uid, lease, "not-known")) {
        return;
      }
      leaseNotKnown(lease, name, uid);
    }
    // we don't tell anyone, since we resend the uid-based
    // renewal with a full record-based renewal
  }

  private void leaseNotKnown(
      ActiveLease lease,
      String name,
      UID uid) {

    // send again, but this time send the full record instead
    // of just the UID
    String agent = lease.agent; 
    Record record = lease.record;

    // FIXME what if record.ttd < 0?  shouldn't happen...

    // FIXME tag lease?

    if (logger.isDebugEnabled()) {
      logger.debug(
          "Resending lease-not-known uid="+uid+
          " for our active lease "+lease);
    }

    Object o = record; 
    if (agent != null) {
      o = new NameTag(agent, o); 
    }
    Map m = Collections.singletonMap(name, o);
    modifyService.modify(m);
  }

  private void leaseDenied(
      String name,
      UID uid,
      Object reason) {
    List responses;
    synchronized (leases) {
      ActiveLease lease = (ActiveLease) leases.get(name);
      if (!matchesLease(name, uid, lease, "denied")) {
        return;
      }
      responses = leaseDenied(lease, name, uid, reason);
      if (responses == null || responses.isEmpty()) {
        return;
      }
    }
    for (int i = 0, n = responses.size(); i < n; i++) {
      Response res = (Response) responses.get(i);
      res.getRequest();
      res.setResult(reason);
    }
  }

  private List leaseDenied(
      ActiveLease lease,
      String name,
      UID uid,
      Object reason) {

    // we've lost all our lease entries
    //
    // see the Record javadocs for future "bind-only" enhancements

    leases.remove(name);

    if (logger.isWarnEnabled()) {
      logger.warn(
          "Lost lease "+
          ((0 < lease.expireTime) ? "renewal" : "creation")+
          " for "+
          "(name="+name+
          " uid="+uid+
          " reason="+reason+
          "), dead lease is: "+lease);
    }

    // FIXME tell agent suicide watcher?

    // we'll fail whatever's pending
    return lease.takeResponses();
  }

  /**
   * Check our leases and renew them if they'll expire soon.
   */
  private void renewLeases() {
    long now;
    Map m = null;
    synchronized (leases) {
      now = System.currentTimeMillis();
      for (Iterator iter = leases.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        ActiveLease lease = (ActiveLease) me.getValue();
        boolean renewNow = shouldRenew(name, lease, now);
        if (!renewNow) {
          continue;
        }
        String agent = lease.agent;
        UID uid = lease.record.getUID();
        if (m == null) {
          m = new HashMap();
        }
        Object o = uid; 
        if (agent != null) {
          o = new NameTag(agent, o); 
        }
        m.put(name, o);
      }
    }

    if (m != null) {
      modifyService.modify(m);
    }

    // run me again later
    renewLeasesThread.schedule(config.checkLeasesPeriod);
  }

  //
  // These are logically part of lease but require
  // access to the config of the outter class
  //

  private boolean shouldRenew(
      String name,
      ActiveLease lease,
      long now) {
    // calculate renewal time based upon:
    //   expiration time
    //   round-trip time for the last renewal delay
    //   some slack for the above round-trip time
    //   added safety in case the server forgets us
    //
    // here's the current guess:
    //
    // figure out the latest time we could renew
    if (0 < lease.sendTime) {
      // we're still waiting for the last renewal ack
      if (logger.isDetailEnabled()) {
        logger.detail(
            "lease (name="+name+", uid="+lease.record.getUID()+
            ") is still pending: "+lease.toString(now));
      }
      return false;
    }
    long latestRenew =
      lease.expireTime - lease.roundTripTime;
    // weight it to be a little early
    long renewalTime = (long) (
        lease.boundTime +
        (config.renewRatio *
         (latestRenew - lease.boundTime)));
    // adjust for timer period
    renewalTime -= config.checkLeasesPeriod;
    if (now < renewalTime) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "lease (name="+name+", uid="+lease.record.getUID()+
            ") doesn't need to be renewed until "+
            Timestamp.toString(renewalTime, now)+
            ": "+lease.toString(now));
      }
      return false;
    }
    // renew, mark the sendtime for round-trip measurement
    lease.sendTime = now;
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Renewing lease (name="+name+", uid="+
          lease.record.getUID()+") that will expire at "+
          Timestamp.toString(lease.expireTime)+": "+
          lease.toString(now));
    }
    return true;
  }

  private void renewed(ActiveLease lease, long now, long expTime) {
    // set our timestamps
    if (0 < lease.sendTime) {
      long tripTime = now - lease.sendTime;
      // soften this by averaging
      long weightedTripTime;
      if (lease.roundTripTime == 0) {
        // first time
        weightedTripTime = tripTime;
      } else {
        weightedTripTime = (long) (
            config.tripWeight * tripTime +
            (1.0 - config.tripWeight)*lease.roundTripTime);
      }
      lease.roundTripTime = weightedTripTime;
      lease.sendTime = 0;
    } else {
      // we don't recall sending this renewal, but we'll
      // accept it anyways.
    }
    lease.boundTime = now;
    // this expTime may be in the past, but it was a successful
    // bind and our timer will renew it soon.
    lease.expireTime = expTime;
  }

  /** config options */
  private static class LeaserConfig {
    public final double renewRatio;
    public final double tripWeight;
    public final long minBundleTTD;
    public final long checkLeasesPeriod;

    public LeaserConfig(Object o) {
      Parameters p = 
        new Parameters(o, "org.cougaar.core.wp.resolver.lease.");
      renewRatio = p.getDouble("renewRatio", 0.75);
      tripWeight = p.getDouble("tripWeight", 0.75);
      minBundleTTD = p.getLong("minBundleTTD", 60000);
      checkLeasesPeriod = p.getLong("checkLeasesPeriod", 20000);
    }
  }

  private static class ActiveLease {

    private static final List NO_RESPONSES = Collections.EMPTY_LIST;

    public final String agent;
    public final Record record;

    public final long bindTime;
    public long boundTime;
    public long sendTime;
    public long roundTripTime;
    public long expireTime;

    private List responses;

    public ActiveLease(
        String agent,
        Record record) {
      this.agent = agent;
      this.record = record;
      long now = System.currentTimeMillis();
      bindTime = now;
      boundTime = 0;
      sendTime = now;
      roundTripTime = 0;
      expireTime = 0;
      responses = NO_RESPONSES;
    }

    @SuppressWarnings("unused")
   public String getAgent() {
      return agent;
    }
    @SuppressWarnings("unused")
   public Record getRecord() {
      return record;
    }
    public boolean isBound() {
      return 0 < boundTime;
    }
    public long getExpirationTime() {
      return expireTime;
    }

    public void addResponses(List l) {
      for (int i = 0, n = l.size(); i < n; i++) {
        Response res = (Response) l.get(i);
        addResponse(res);
      }
    }

    public void addResponse(Response res) {
      if (!(res instanceof Response.Bind)) {
        throw new IllegalArgumentException("Non-bind res: "+res);
      }
      if (isBound()) {
        throw new IllegalStateException(
            "Lease is already bound, not expecting responses: "+
            this);
      }
      if (responses == NO_RESPONSES) {
        responses = new ArrayList(3);
      }
      responses.add(res);
    }

    public List takeResponses(String type) {
      if (type == null) {
        throw new IllegalArgumentException("null type");
      }
      List ret = NO_RESPONSES;
      for (int i = 0, n = responses.size(); i < n; i++) {
        Response res = (Response) responses.get(i);
        Request req = res.getRequest();
        AddressEntry ae;
        if (req instanceof Request.Bind) {
          ae = ((Request.Bind) req).getAddressEntry();
        } else if (req instanceof Request.Unbind) {
          ae = ((Request.Unbind) req).getAddressEntry();
        } else {
          // invalid?
          continue;
        }
        if (!type.equals(ae.getType())) {
          continue;
        }
        if (ret.isEmpty()) {
          ret = new ArrayList();
          ret.add(res);
        }
        responses.remove(i);
        --i;
        --n;
        if (n <= 0) {
          responses = NO_RESPONSES;
        }
      }
      return ret;
    }

    public List takeResponses() {
      List l = responses;
      responses = NO_RESPONSES;
      return l;
    }

    @Override
   public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      return 
        "(lease"+
        " agent="+agent+
        " record="+record.toString(bindTime, now)+
        " bindTime="+
        Timestamp.toString(bindTime, now)+
        " boundTime="+
        Timestamp.toString(boundTime, now)+
        " sendTime="+
        Timestamp.toString(sendTime, now)+
        " roundTripTime="+roundTripTime+
        " expireTime="+
        Timestamp.toString(expireTime, now)+
        " pending["+responses.size()+"]="+responses+
        ")";
    }
  }

  private class LeaseSP 
    implements ServiceProvider {
      private final LeaseService ls =
        new LeaseService() {
          public void submit(Response res, String agent) {
            LeaseManager.this.submit(res, agent);
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!LeaseService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        return ls;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  private class BundleSP 
    implements ServiceProvider {
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!BundleService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        BundleService.Client client =
          (requestor instanceof BundleService.Client ?
           (BundleService.Client) requestor :
           null);
        BundleServiceImpl rsi = new BundleServiceImpl(client);
        if (client != null) {
          LeaseManager.this.register(client);
        }
        return rsi;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
        if (!(service instanceof BundleServiceImpl)) {
          return;
        }
        BundleServiceImpl rsi = (BundleServiceImpl) service;
        BundleService.Client client = rsi.client;
        if (client != null) {
          LeaseManager.this.unregister(client);
        }
      }
      private class BundleServiceImpl 
        implements BundleService {
          protected final Client client;
          public BundleServiceImpl(Client client) {
            this.client = client;
          }
          public Bundle getBundle(String name) {
            return LeaseManager.this.getBundle(name);
          }
          public Map getAllBundles() {
            return LeaseManager.this.getAllBundles();
          }
        }
    }
}
