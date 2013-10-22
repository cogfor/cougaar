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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.Parameters;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.HintService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;

/**
 * This component is the white pages client-side cache.
 * <p>
 * Features:
 * <ul>
 *   <li>Holds "get", "getAll", and "list" results</li>
 *   <li>Allows the client to add bootstrapped cache "hints"</li>
 *   <li>Holds both positive and negative results</li>
 *   <li>Evicts expired and least-recently-used entries</li>
 *   <li>Renews (in the background) recently-used entries that
 *       will soon expire</li>
 *   <li>Allows the client to flush and force-renewal entries
 *       that are known to be stale</li>
 *   <li>Upgrades "get" requests to "getAll" requests, to reduce
 *       server traffic</li>
 * </ul>
 * <p>
 * The cache doesn't manage "bind/unbind" leases; that's the job of
 * the LeaseManager.
 */
public class CacheManager
extends GenericStateModelAdapter
implements Component
{
  // per-entry access history bits
  //
  // note that this bits are taken from the lower bits of the
  // "Entry.renewalTime", so no more than 10 bits (~1 second)
  // is recommended.
  private static final int DEFAULT_ACCESS_BITS = 8;
  private static final int ACCESS_BITS = 
    SystemProperties.getInt(
        "org.cougaar.core.wp.resolver.accessBits",
        DEFAULT_ACCESS_BITS);

  // ratio of non-expired entries to evict if the cache is full
  // and doesn't contain expired entries
  private static final double EVICT_RATIO = (1.0/3.0);

  // RFE: timer-based prefetch
  //
  // use the ACCESS_BITS to check entries for late accesses

  private CacheConfig config;

  private ServiceBroker sb;
  private LoggingService logger;
  private ThreadService threadService;

  private LookupService lookupService;

  private final LookupService.Client myLookupClient = 
    new LookupService.Client() {
      public void lookupAnswer(long baseTime, Map m) {
        CacheManager.this.lookupAnswer(baseTime, m);
      }
    };

  private CacheSP cacheSP;
  private HintSP hintSP;

  private final Object lock = new Object();

  // our cache
  //
  // this is a Map of Strings to Entry objects:
  //   LRUMap<String, Entry>
  //
  // for keys starting with "."  (list):
  //   Entry data:  Map<String, Set<String>>
  //
  // for keys starting with [a-zA-Z0-9]  (get/getAll):
  //   Entry data: Map<String, Map<String, AddressEntry>>
  //
  // note that pending requests are also placed in the cache,
  // as well as non-evictable hints, so the cache can't be
  // simply cleared to free up space.
  private LRUMap cache;

  //
  // clean the cache:
  //

  private Schedulable cleanCacheThread;

  //
  // timer-based prefetch
  //

  private Schedulable prefetchThread;

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

  private void configure(Object o) {
    if (config != null) {
      return;
    }
    config = new CacheConfig(o);
  }

  @Override
public void load() {
    super.load();

    configure(null);

    cache = new LRUMap(config.maxSize);

    // register for lookups
    lookupService = sb.getService(
       myLookupClient, LookupService.class, null);
    if (lookupService == null) {
      throw new RuntimeException(
          "Unable to obtain LookupService");
    }

    // advertise our service
    cacheSP = new CacheSP();
    sb.addService(CacheService.class, cacheSP);
    hintSP = new HintSP();
    sb.addService(HintService.class, hintSP);

    if (0 < config.cleanPeriod) {
      // create expiration timer
      Runnable cleanCacheRunner =
        new Runnable() {
          public void run() {
            // assert (thread == cleanCacheThread);
            cleanCache();
          }
        };
      cleanCacheThread = threadService.getThread(
          this,
          cleanCacheRunner,
          "White pages client cache cleaner");
      cleanCacheThread.schedule(config.cleanPeriod);
    }

    if (0 < config.prefetchPeriod) {
      // create prefetch timer
      Runnable prefetchRunner =
        new Runnable() {
          public void run() {
            // assert (thread == prefetchThread);
            prefetch();
          }
        };
      prefetchThread = threadService.getThread(
          this,
          prefetchRunner,
          "White pages client prefetch");
      prefetchThread.schedule(config.prefetchPeriod);
    }
  }

  @Override
public void unload() {
    if (hintSP != null) {
      sb.revokeService(HintService.class, hintSP);
      hintSP = null;
    }
    if (cacheSP != null) {
      sb.revokeService(
          CacheService.class, cacheSP);
      cacheSP = null;
    }
    if (lookupService != null) {
      sb.releaseService(
          myLookupClient, LookupService.class, lookupService);
      lookupService = null;
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

  private void setResult(Response res, Object result) {
    res.setResult(result);
  }

  //
  // look in the cache:
  //

  private void submit(Response res) {
    Request req = res.getRequest();
    boolean cacheOnly = req.hasOption(Request.CACHE_ONLY);
    if (req instanceof Request.Get) {
      Request.Get r = (Request.Get) req;
      String name = r.getName();
      String type = r.getType();
      get(res, name, type, cacheOnly);
    } else if (req instanceof Request.GetAll) {
      Request.GetAll r = (Request.GetAll) req;
      String name = r.getName();
      getAll(res, name, cacheOnly);
    } else if (req instanceof Request.List) {
      Request.List r = (Request.List) req;
      String suffix = r.getSuffix();
      list(res, suffix, cacheOnly);
    } else if (req instanceof Request.Flush) {
      Request.Flush r = (Request.Flush) req;
      String name = r.getName();
      long minAge = r.getMinimumAge();
      AddressEntry ae = r.getAddressEntry();
      boolean uncache = r.isUncache();
      boolean prefetch = r.isPrefetch();
      flush(res, name, minAge, ae, uncache, prefetch);
    } else if (req instanceof Request.Bind) {
      Request.Bind r = (Request.Bind) req;
      AddressEntry ae = r.getAddressEntry();
      boolean overwrite = r.isOverWrite();
      if (cacheOnly) {
        hint(res, ae, overwrite);
      } else {
        boolean renewal = r.isRenewal();
        bind(ae, renewal);
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind r = (Request.Unbind) req;
      AddressEntry ae = r.getAddressEntry();
      if (cacheOnly) {
        unhint(res, ae);
      } else {
        unbind(ae);
      }
    } else {
      throw new IllegalArgumentException("Unknown action");
    }
  }

  private void getAll(
      Response res, String name, boolean cacheOnly) {
    get(res, name, null, cacheOnly);
  }

  private void list(
      Response res, String suffix, boolean cacheOnly) {
    get(res, suffix, null, cacheOnly);
  }

  private void get(
      Response res,
      String name,
      String type,
      boolean cacheOnly) {
    boolean hasResult;
    UID uid;
    Object result;
    boolean mustSend;
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      boolean isHint = false;
      long now = System.currentTimeMillis();
      Object hint;
      if (e == null) {
        // not cached
        hasResult = false;
        uid = null;
        result = null;
        e = newEntry(now, (cacheOnly ? null : res));
        cache.put(name, e);
        mustSend = true;
      } else if (!e.hasExpired(now)) {
        // valid data
        hasResult = true;
        uid = e.getUID();
        result = e.getData();
        mustSend = e.noteAccess(now);
      } else if ((hint = e.getHint(type)) != null) {
        // found hint
        hasResult = true;
        uid = null;
        result = hint;
        isHint = true;
        mustSend = false;
      } else {
        // expired, maybe already pending
        hasResult = false;
        uid = (e.hasData() ? e.getUID() : null);
        result = null;
        mustSend = e.noteExpired(now, (cacheOnly ? null : res));
      }

      if (logger.isDetailEnabled()) {
        logger.detail(
            "cache "+
            (hasResult ?
             ("HIT"+
              (mustSend ? 
               (" (RENEW "+
                Timestamp.toString(e.getExpirationTime(), now)+
                ")") :
               (isHint ? " (HINT)" : ""))) :
             ("MISS ("+
              (mustSend ? "SEND" : "PENDING")+
              ")"))+
            (cacheOnly ? " (CACHE_ONLY)" : "")+
            " for "+
            (name.charAt(0) == '.' ?
             ("list(suffix="+name+")") :
             ("get"+
              (type == null ? "All" : "")+
              "(name="+name+
              (type == null ? "" : ", type="+type)+
              ")"))+
            (uid == null ? "" : " (uid="+uid+")"));
      }
    }

    if (hasResult || cacheOnly) {

      // cache hit
      setResult(res, result);

    }

    if (mustSend) {
      if (hasResult) {
        // this is a renewal, so it can be batched
        Map m = Collections.singletonMap(name, uid);
        lookupService.lookup(m);
      } else {
        // we should send this asap, but we can also include
        // any batched lookups
        Map m = Collections.singletonMap(name, uid);
        lookupService.lookup(m);
      }
    }
  }

  private void bind(
      AddressEntry ae,
      boolean renewal) {
    String name = ae.getName();

    // flush the cache entry if it conflicts with the
    // bind entry, to avoid local confusion
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      boolean wasCached = false;
      boolean wasStale = false;
      long now = System.currentTimeMillis();
      if (e != null &&
          !e.hasExpired(now)) {
        wasCached = true;
        Map m = (Map) e.getData();
        Object oldAE = 
          (m == null ?
           (null) :
           m.get(ae.getType()));
        if (!ae.equals(oldAE)) {
          wasStale = true;
          if (e.flush(now)) {
            cache.remove(name);
          }
        }
      }
      if (logger.isDetailEnabled()) {
        logger.detail(
            "bind"+ae+" "+
            (renewal ? "renewal " : "")+
            (wasCached ?
             ((wasStale ? "flushed" : "already matches")+
              " cache entry: "+e) :
             "was not cached"));
      }
    }

    // let the lease manager handle the batching
  }

  private void hint(
      Response res,
      AddressEntry ae,
      boolean overwrite) {
    String name = ae.getName();
    String type = ae.getType();

    boolean hasResult;
    Object result;
    List pendingGets = null;
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      if (e == null) {
        // new hint
        Entry ewh = newEntryWithHints();
        cache.put(name, ewh);
        e = ewh;
        e.putHint(type, ae);

        if (logger.isInfoEnabled()) {
          logger.info("Added hint "+ae);
        }
        hasResult = false;
        result = null;
      } else {
        AddressEntry oldAE = (overwrite ? null : e.getHint(name));
        if (oldAE == null) {
          // new or replacement hint
          if (!e.hasHints()) {
            Entry ewh = newEntryWithHints(e);
            cache.put(name, ewh);
            e = ewh;
          }
          e.putHint(type, ae);

          if (logger.isInfoEnabled()) {
            logger.info("Added hint "+ae);
          }
          // find any pending "get" requests
          final String predType = type;
          UnaryPredicate pred = new UnaryPredicate() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public boolean execute(Object o) {
              if (!(o instanceof Response)) {
                return false;
              }
              Response pRes = (Response) o;
              Request  pReq = pRes.getRequest();
              if (!(pReq instanceof Request.Get)) {
                return false;
              }
              Request.Get pGReq = (Request.Get) pReq;
              String pType = pGReq.getType();
              if (!predType.equals(pType)) {
                return false;
              }
              // our new hint can answer this request
              return true;
            }
          };
          pendingGets = e.takeMatchingResponses(pred);
          hasResult = false;
          result = null;
        } else {
          // conflicts
          hasResult = true;
          if (ae.equals(oldAE)) {
            // same as the current hint
            result = Boolean.TRUE;
          } else {
            // a conflicting hint is already in place
            result = oldAE;
          }
        }
      }
    }

    if (hasResult) {
      setResult(res, result);
    }

    // answer any pending requests that match this hint
    int nPendingGets = 
      (pendingGets == null ? 0 : pendingGets.size());
    for (int i = 0; i < nPendingGets; i++) {
      Response pRes = (Response) pendingGets.get(i);
      if (logger.isInfoEnabled()) {
        logger.info(
            "Setting result for get("+name+", "+type+
            ") to hint "+ae+" for request "+pRes);
      }
      setResult(pRes, ae);
    }

    // let the hint pass on to the bootstrappers,
    // since it stops at the lease manager
  }

  private void unhint(Response res, AddressEntry ae) {
    String name = ae.getName();

    boolean hasResult;
    Object result;
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      if (e != null && e.hasHints()) {
        String type = ae.getType();
        AddressEntry oldAE = e.getHint(type);
        if (ae.equals(oldAE)) {
          if (!e.removeHint(type)) {
            // was hint-only, now nothing
            cache.remove(name);
          }
          hasResult = true;
          result = Boolean.TRUE;
        } else {
          hasResult = false;
          result = null;
        }
      } else {
        hasResult = false;
        result = null;
      }
    }

    if (hasResult) {
      setResult(res, result);
      if (logger.isInfoEnabled()) {
        logger.info("Removed hint "+ae);
      }
    }
    // let the hint pass on to the bootstrappers,
    // it stops at the lease manager
  }

  private void unbind(AddressEntry ae) {
    String name = ae.getName();

    // clear cache entry, just in case
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      boolean wasCached = false;
      long now = System.currentTimeMillis();
      if (e != null && e.flush(now)) {
        wasCached = true;
        cache.remove(name);
      }
      if (logger.isDetailEnabled()) {
        logger.detail(
            "unbind"+ae+" "+
            (wasCached ?
             ("flushed cache entry for "+name+": "+e) :
             "was not cached"));
      }
    }

    // let the lease manager handle the batching
  }

  private void flush(
      Response res,
      String name,
      long minAge,
      AddressEntry ae,
      boolean uncache,
      boolean prefetch) {

    UID uid = null;
    Object result;
    boolean mustSend = false;

    synchronized (lock) {
      boolean wasCached = false;
      boolean isOldEnough = true;

      // find matching entry
      Entry e = (Entry) cache.get(name);
      long now = System.currentTimeMillis();
      if (e == null ||
          e.hasExpired(now)) {
        // either no data or only hints
        isOldEnough = false;
      } else if (e.getDataAge(now) < minAge) {
        // not old enough yet
      } else {
        // valid old-enough data
        //
        // check to see if it matches the client's assertions,
        // to make this an atomic test/set
        if (ae == null) {
          // no assertions
          wasCached = true;
        } else {
          // match a single entry
          Map m = (Map) e.getData();
          String type = ae.getType();
          AddressEntry oldAE = (AddressEntry) m.get(type);
          wasCached = ae.equals(oldAE);
        }
      }

      if (wasCached) {
        // do the requested uncache/prefetch
        if (uncache) {
          if (e.flush(now)) {
            cache.remove(name);
            e = null;
          } else {
          }
        }
        if (prefetch) {
          if (e == null) {
            // let the "getAll" create it
            mustSend = true;
          } else {
            mustSend = e.renew(now);
          }
          uid = e.getUID();
        }
      }

      // the result is TRUE if we did something
      boolean b = (wasCached && (uncache || mustSend));
      result = Boolean.valueOf(b);

      if (logger.isDetailEnabled()) {
        logger.detail(
            (uncache ? 
             ("uncache"+(prefetch ? "+" : "")) : "")+
            (prefetch ? "prefetch" : "")+
            " "+
            name+
            " "+
            (wasCached ? 
             ((0 < minAge ?
               "is over "+minAge+" millis old, " :
               "")+
              (ae == null ?
               "" :
               "matched the cache assertion "+ae+", ")+
              (uncache ? "flushed entry data" : "") +
              (prefetch ? 
               (mustSend ? 
                "sending a new lookup" :
                "lookup already in progress") :
               "")) :
             ("is not "+
              (isOldEnough ?
               "over "+minAge+" millis old" :
               "in the cache"))));
      }
    }

    // this is a cache-only request
    setResult(res, result);

    if (mustSend) {
      // send a "getAll"
      // FIXME optimize, add delay, add UID support
      Map m = Collections.singletonMap(name, uid);
      lookupService.lookup(m);
    }
  }

  //
  // bootstrap entries into the cache:
  //

  private void add(String name, Bundle bundle) {
    long now = System.currentTimeMillis();
    UID uid = bundle.getUID();
    long ttd = bundle.getTTD();
    Object data = bundle.getEntries();
    if (data == null) {
      data = Collections.EMPTY_MAP;
    }
    Record r = new Record(uid, ttd, data);
    Map m = Collections.singletonMap(name, r);
    lookupAnswer(now, m, true);
  }

  private void remove(String name, Bundle bundle) {
    // TODO
    if (logger.isInfoEnabled()) {
      logger.info(
          "Unsupported cache remove("+name+", "+bundle+")"+
          ", just letting it time out...");
    }
  }

  //
  // callback for remote lookup answers:
  //

  private void lookupAnswer(long baseTime, Map m) {
    lookupAnswer(baseTime, m, false);
  }

  private void lookupAnswer(long baseTime, Map m, boolean create) {
    for (Iterator iter = m.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Object value = me.getValue();
      UID uid;
      long ttd;
      boolean hasData;
      Object data;
      if (value instanceof Record) {
        Record r = (Record) value;
        uid =  r.getUID();
        ttd = r.getTTD();
        hasData = true;
        data = r.getData();
      } else if (value instanceof RecordIsValid) {
        RecordIsValid riv = (RecordIsValid) value;
        uid =  riv.getUID();
        ttd = riv.getTTD();
        hasData = false;
        data = null;
      } else {
        throw new IllegalArgumentException(
            "Lookup callback map unexpected value: "+me);
      }
      gotAll(name, uid, baseTime, ttd, hasData, data, create);
    }
  }

  private void gotAll(
      String name,
      UID uid,
      long baseTime,
      long ttd,
      boolean hasData,
      Object data,
      boolean create) {
    List responses;
    synchronized (lock) {
      Entry e = (Entry) cache.get(name);
      if (create) {
        long now = System.currentTimeMillis();
        if (e == null) {
          e = newEntry(now, null);
          cache.put(name, e);
        }
        if (e.wasSent()) {
          if (uid != null &&
              e.hasData() &&
              e.getUID() != null &&
              !uid.equals(e.getUID())) {
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Avoiding \"UID mismatch\""+
                  ", already sent "+name+"="+e.toString(now)+
                  ", bootstrap "+name+"=(uid="+uid+
                  ", data="+data+"), overriding uid to null");
            }
            uid = null;
          }
        } else {
          e.setSendTime(now);
        }
      }
      if (e != null && e.wasSent()) {
        // set in cache, maybe force another lookup if !hasData
        responses = 
          gotAll(
              e,
              name,
              uid,
              baseTime,
              ttd,
              hasData,
              data);
        // use the cached data instead of the passed data, since the
        // data will be null if !hasData.
        data = e.getData();
      } else {
        // we didn't as for this (!)
        //
        // The LookupService usually protects us against this, but
        // sometimes races can occur.
        responses = null;
        if (logger.isDebugEnabled()) {
          long now = System.currentTimeMillis();
          long ttl = baseTime + ttd;
          logger.debug(
              "Ignoring a lookup result that we didn't send?"+
              " name="+name+
              " uid="+uid+
              " ttd="+ttd+
              " baseTime="+Timestamp.toString(baseTime, now)+
              " ttl="+Timestamp.toString(ttl, now)+
              " hasData="+hasData+
              " data="+(hasData ? data : null)+
              " entry="+e);
        }
      }
    }

    int n = (responses == null ? 0 : responses.size());
    for (int i = 0; i < n; i++) {
      Response res = (Response) responses.get(i);
      setResult(res, data);
    }
  }

  private List gotAll(
      Entry e,
      String name,
      UID uid,
      long baseTime,
      long ttd,
      boolean hasData,
      Object data) {

    long now = System.currentTimeMillis();

    // clean up data
    if (hasData) {
      // this includes the full data
      if (data == null) {
        // this is a negative cache
        if (name.charAt(0) == '.') {
          data = Collections.EMPTY_SET;
        } else {
          data = Collections.EMPTY_MAP;
        }
      } else {
        // optionally check for immutable collection
      }
      if (e.hasHints()) {
        // merge in hints.
        //
        // this is necessary because a server might lack our hints,
        // so it could return null data and disable our hints. 
        Map hints = e.getHints();
        Map mdata = (Map) data;
        boolean lacksHint = false;
        for (Iterator iter = hints.keySet().iterator();
            iter.hasNext();
            ) {
          Object key = iter.next();
          if (!mdata.containsKey(key)) {
            lacksHint = true;
            break;
          }
        }
        if (lacksHint) {
          Map m = new HashMap(hints);
          m.putAll(mdata);
          data = Collections.unmodifiableMap(m);
          if (logger.isDetailEnabled()) {
            logger.detail(
                "merged "+name+" data "+mdata+" with our hints "+
                hints+" to create "+data);
          }
          // FIXME: if we alter our hints we should flush the data.
        }
      }
    } else {
      // this is a UID-based renewal
      //
      // make sure we still have the old data
      if (e.hasData() &&
          (uid == null ||
           uid.equals(e.getUID()))) {
        // the local data is up-to-date
        data = e.getData();
      } else {
        // we've cached the wrong data (?)
        //
        // send a new lookup
        // FIXME optimize, add delay, add UID support
        Map m = Collections.singletonMap(name, null);
        lookupService.lookup(m);
        return null;
      }
    }

    // take pending responses
    List responses = e.takeResponses();

    // set data
    e.setDataTime(now);
    e.setUID(uid);
    e.setData(data);

    // compute the ttl
    long ttl = baseTime + ttd;
    long maxTTL = now + config.maxTTD;
    if (maxTTL < ttl) {
      // reduce ttl if too long
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Reduce ttl from "+
            Timestamp.toString(ttl,now)+
            " to "+
            Timestamp.toString(maxTTL,now));
      }
      ttl = maxTTL;
    }
    long minTTL = now + config.minTTD;
    if (ttl < minTTL) {
      // increase ttl if it's too short
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Increasing ttl from "+
            Timestamp.toString(ttl,now)+
            " to "+
            Timestamp.toString(minTTL,now));
      }
      ttl = minTTL;
    }

    // set timestamps
    long expireTime = ttl;
    long timeLeft = expireTime - now;
    long sentTime = e.getSendTime();
    long rtt = now - sentTime;
    // FIXME
    long earlyBy = (rtt << 1);
    if ((timeLeft >> 1) < earlyBy) {
      // never more than half the lifetime
      earlyBy = timeLeft >> 1;
    } else if (earlyBy < (timeLeft >> 3)) {
      // never less than an eighth
      earlyBy = timeLeft >> 3;
    }
    long renewalTime = expireTime - earlyBy;
    if (renewalTime < now) {
      // never in the past!
      renewalTime = now;
    }
    e.setRenewalTime(renewalTime);
    e.setExpireTime(expireTime);
    // let the entry steal the lower bits
    renewalTime = e.getRenewalTime();
 
    if (logger.isDebugEnabled()) {
      boolean isList = (name.charAt(0) == '.');
      logger.debug(
          "Caching "+
          (isList ? "list(suffix=" : "getAll(name=")+
          name+
          ", sent="+Timestamp.toString(sentTime, now)+
          ", renew="+Timestamp.toString(renewalTime, now)+
          ", expires="+Timestamp.toString(expireTime, now)+
          ", "+
          (isList ? "names=" : "entries=")+
          data+")");
    }

    // return the pending responses
    return responses;
  }

  /**
   * This is old code that flushed the cache if a bind-ack
   * conflicted with the local cache.
   * <p>
   * This is dead code but we may revisit it in the future...
   * <p>
   * <pre>
   *   bound(ae, ttl)  == bound(ae, ae, ttl);
   *   unbound(ae,ttl) == bound(ae, null, ttl);
   * </pre>
   * @param ae non-null if bind, otherwise should be oldAE
   */
//  private void bound(
//      AddressEntry oldAE,
//      AddressEntry ae,
//      long ttl) {
//    String name = oldAE.getName();
//    synchronized (lock) {
//      Entry e = (Entry) cache.get(name);
//      if (e == null) {
//        // no entry?
//        return;
//      }
//      long now = System.currentTimeMillis();
//      if (e.hasExpired(now)) {
//        // no data to patch
//        return;
//      }
//      if (e.canEvict(now, false)) {
//        // it's expired and no lookup is in progress
//        cache.remove(name);
//        return;
//      }
//      // patch the data
//      Map m = (Map) e.getData();
//      long oldTTL = e.getExpirationTime();
//      if (oldTTL < ttl) {
//        // keep the old ttl, swap in new data
//        //
//        // this may be a bit confusing, since the cache will contain
//        // a mix of server data and local bound overrides, but it's
//        // better than flushing the entry
//        Map newMap = new HashMap(m);
//        String type = oldAE.getType();
//        newMap.put(type, ae);
//        newMap = Collections.unmodifiableMap(newMap);
//        e.setData(newMap);
//      } else {
//        // unusual: the server has reduced the ttl!
//        //
//        // rather than patching the existing entry,
//        // we'll expire it immediately.
//        if (e.flush(now)) {
//          cache.remove(name);
//        }
//      }
//    }
//  }

  private void prefetch() {
    Map m = null;
    synchronized (lock) {
      // scan the cache, which is ordered from least-recently-used
      // to most-recently-used.
      //
      // renew entries that will expire soon.
      //
      // could optimize this by keeping a minTTL
      long now = System.currentTimeMillis();
      long nextTime = (now + config.prefetchPeriod);
      int n = cache.size();
      for (Iterator iter = cache.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        Entry e = (Entry) me.getValue();
        if (!e.shouldPrefetch(nextTime, now)) {
          continue;
        }
        String name = (String) me.getKey();
        if (m == null) {
          m = new HashMap();
        }
        UID uid = (e.hasData() ? e.getUID() : null);
        if (logger.isDetailEnabled()) {
          if (m.isEmpty()) {
            logger.detail("prefetching cache["+n+"] {");
          }
          logger.detail(
              "  cache PREFETCH (RENEW "+
              Timestamp.toString(e.getExpirationTime(), now)+
              ") for "+
              (name.charAt(0) == '.' ?
               "list(suffix=" :
               "getAll(name=")+
              name+")"+
              (uid == null ? "" : " (uid="+uid+")"));
        }
        m.put(name, uid);
      }
      if (m != null && logger.isDetailEnabled()) {
        logger.detail("}");
        logger.detail("prefetch "+m.size()+" of "+n+" entries");
      }
    }

    if (m != null) {
      lookupService.lookup(m);
    }

    // run me again later
    prefetchThread.schedule(config.prefetchPeriod);
  }

  // this is optional since the LRU will clean itself
  private void cleanCache() {
    synchronized (lock) {
      long now = System.currentTimeMillis();

      // remove expired entries
      removeExpiredEntries(now);

      // might as well debug our misses-table on the timer thread
      if (logger.isDebugEnabled()) {
        StringBuffer buf = new StringBuffer();
        buf.append("\n##### cache requests & hints ######################\n");
        boolean moreInfo = logger.isDetailEnabled();
        cacheToString(buf, moreInfo, moreInfo, now);
        buf.append("\n###################################################");
        String s = buf.toString();
        logger.debug(s);
      }
    }

    // run me again later
    cleanCacheThread.schedule(config.cleanPeriod);
  }

  private void cacheToString(
      StringBuffer buf,
      boolean showNormal,
      boolean showExpired,
      long now) {
    buf.append("Cache[").append(cache.size()).append("] {");
    int nNormal = 0;
    int nExpired = 0;
    for (Iterator iter = cache.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Entry e = (Entry) me.getValue();
      if (!showExpired && e.hasExpired(now)) {
        nExpired++;
        continue;
      }
      if (!showNormal && e.canEvict(now, false)) {
        nNormal++;
        continue;
      }
      buf.append("\n  ").append(name).append(" --> ");
      buf.append(e.toString(now));
    }
    if (0 < nNormal) {
      buf.append("\n  <skipping ").append(nNormal);
      buf.append(" normal (non-pending & non-hint) entries>");
    }
    if (0 < nExpired) {
      buf.append("\n  <skipping ").append(nExpired);
      buf.append(" expired entries>");
    }
    buf.append("\n}");
  }

  private boolean evictLRU(Entry eldestE) {
    synchronized (cache) {
      if (cache.size() <= config.maxSize) {
        // still plenty of room
        return false;
      }
      long now = System.currentTimeMillis();
      // quick-check for an expired eldest
      if (eldestE != null &&
          eldestE.canEvict(now, false)) {
        // remove the oldest entry
        if (logger.isDetailEnabled()) {
          logger.detail("evicting eldest: "+eldestE);
        }
        return true;
      }
      // remove expired entries
      if (removeExpiredEntries(now)) {
        // freed some expired entries
        //
        // must return false since we modified the map
        return false;
      }
      // okay, try removing non-expired entries
      if (evictEntries(now, EVICT_RATIO)) {
        // freed some non-expired entries
        //
        // must return false since we modified the map
        return false;
      }
      // oy, we can't remove any entries!  This will be
      // slightly painful since every "put" may force
      // us to rescan the list.  Hopefully this is rare
      // in practice.
      if (logger.isInfoEnabled()) {
        logger.info(
            "Can't evict an entry from the cache["+
            cache.size()+"],"+
            " either due to pending requests or hints,"+
            " allowing the cache to exceed its maximum size "+
            config.maxSize);
      }
      return false;
    }
  }

  private boolean removeExpiredEntries(long now) {
    // scan the cache, which is ordered from least-recently-used
    // to most-recently-used.
    //
    // remove expired entries
    //
    // could optimize this by keeping a minTTL
    int n = cache.size();
    int nfreed = 0;
    for (Iterator iter = cache.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      Entry e = (Entry) me.getValue();
      if (!e.canEvict(now, false)) {
        continue;
      }
      if (logger.isDetailEnabled()) {
        if (nfreed == 0) {
          logger.detail("cleaning cache["+n+"] {");
        }
        String name = (String) me.getKey();
        logger.detail("  expired "+name+"="+e);
      }
      ++nfreed;
      iter.remove();
    }
    if (0 < nfreed && logger.isDetailEnabled()) {
      logger.detail("}");
      logger.detail("removed "+nfreed+" of "+n+" expired entries");
    }
    return (0 < nfreed);
  }

  private boolean evictEntries(long now, double percent) {
    // evict a percent of the cache, even if the entries haven't
    // expired yet
    int n = cache.size();
    int nfreed = 0;
    int enoughFreed = (int) (percent * n);
    for (Iterator iter = cache.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      Entry e = (Entry) me.getValue();
      if (!e.canEvict(now, true)) {
        continue;
      }
      if (logger.isDebugEnabled()) {
        if (nfreed == 0) {
          logger.debug("Evicting LRU entries["+n+"] {");
        }
        String name = (String) me.getKey();
        logger.debug("  Evicting non-expired "+name+"="+e);
      }
      ++nfreed;
      iter.remove();
      if (enoughFreed <= nfreed) {
        // that's enough for now...
        break;
      }
    }
    if (0 < nfreed && logger.isDebugEnabled()) {
      logger.debug("}");
      logger.debug("Evicted "+nfreed+" of "+n+" expired entries");
    }
    return (0 < nfreed);
  }

  /** config options */
  private static class CacheConfig {
    public final long cleanPeriod;
    public final long prefetchPeriod;
    public final long minTTD;
    public final long maxTTD;
    public final int minSize;
    @SuppressWarnings("unused")
   public final int initSize;
    public final int maxSize;

    public CacheConfig(Object o) {
      Parameters p = 
        new Parameters(o, "org.cougaar.core.wp.resolver.cache.");
      cleanPeriod = p.getLong("cleanPeriod", 10000);
      prefetchPeriod = p.getLong("prefetchPeriod", 10000);
      minTTD = p.getLong("minTTD", 5000);
      maxTTD = p.getLong("maxTTD", 600000);
      minSize = p.getInt("minSize", 16);
      initSize = p.getInt("initSize", minSize);
      maxSize = p.getInt("maxSize", 2048);
      if (maxSize <= 0 || maxSize < minSize) {
        throw new RuntimeException(
            "Invalid cache size (min="+minSize+", max="+maxSize+")");
      }
    }
  }

  private Entry newEntry(long now, Response res) {
    Entry e = new Entry();
    e.newLookup(now, res);
    return e;
  }
  private Entry newEntryWithHints() {
    EntryWithHints ewh = new EntryWithHints();
    return ewh;
  }
  private Entry newEntryWithHints(Entry e) {
    EntryWithHints ewh = new EntryWithHints(e);
    return ewh;
  }

  static class Entry {

    protected long dataTime;
    protected long renewalTime;
    protected long expireTime;
    protected UID uid;
    protected Object data;
    protected List responses;

    /**
     * Initialize an entry, which must be sent.
     */
    public void newLookup(long now, Response res) {
      // assert (0 < now);
      dataTime = -1;
      uid = null;
      data = null;
      expireTime = -1;
      setSendTime(now);
      responses =
        (res == null ?
         Collections.EMPTY_LIST :
         Collections.singletonList(res));
    }

    /**
     * The time that the data was cached, or negative if there's
     * no cached data.
     */
    public void setDataTime(long now) {
      // assert (0 < now);
      dataTime = now;
    }
    public long getDataAge(long now) {
      // assert (0 < now);
      // assert (0 < dataTime);
      // assert (dataTime <= now);
      return (now - dataTime);
    }
    public long getDataTime() {
      // assert (0 < dataTime);
      return dataTime;
    }
    /** Most clients should use "hasExpired(now)" */
    public boolean hasData() {
      return (0 < dataTime);
    }

    /**
     * The UID of the cached data, which may be null.
     */
    public void setUID(UID uid) {
      this.uid = uid;
    }
    public UID getUID() {
      // assert (hasData());
      return uid;
    }

    /**
     * The cached data, which may be null.
     */
    public void setData(Object data) {
      this.data = data;
    }
    public Object getData() {
      // assert (hasData());
      return data;
    }

    /**
     * The expiration time for the data, or negative if there's
     * no cached data.
     */
    public void setExpireTime(long expireTime) {
      // assert (0 < expireTime);
      this.expireTime = expireTime;
    }
    public long getExpirationTime() {
      return expireTime;
    }
    /** @return false if there is valid data */
    public boolean hasExpired(long now) {
      // if there's no data then the expireTime is negative,
      // which is equivalent to saying it's expired.
      return (expireTime < now);
    }

    /**
     * The time that a pending lookup was sent.
     * <p>
     * This is stored as a negative renewalTime, since an
     * entry is either active (positive renewal) or pending.
     */
    private void setSendTime(long now) {
      // assert (0 <= renewalTime);
      // assert (0 < now);
      renewalTime = -now;
    }
    public long getSendTime() {
      // assert (renewalTime < 0);
      return -renewalTime;
    }
    public boolean wasSent() {
      return (renewalTime < 0);
    }

    /**
     * If an entry has data, this is the time after which a
     * prefetch lookup should be sent (to keep the data current).
     */
    public void setRenewalTime(long renewalTime) {
      // assert (0 < renewalTime);
      this.renewalTime = maskTime(renewalTime);
    }
    public long getRenewalTime() {
      // assert (0 < renewalTime);
      return maskTime(renewalTime);
    }
    private boolean shouldRenew(long now) {
      // assert (0 < now);
      // assert (!wasSent());
      //
      // no need to mask, since this is a simple comparison
      return (renewalTime < now);
    }

    /** @return true if a lookup should be sent */
    public boolean renew(long now) {
      // assert (0 < now);
      if (wasSent()) {
        // lookup already in progress
        return false;
      }
      // force early renewal
      setRenewalTime(now);
      // this will queue the "getAll"
      return true;
    }
    /** @return true if a lookup should be sent */
    public boolean noteAccess(long now) {
      // assert (0 < now);
      // assert (!hasExpired(now));
      recordAccess(now);

      if (!shouldRenew(now)) {
        // it's too early to prefetch
        return false;
      }
      if (wasSent()) {
        // already sent
        return false;
      }
      // send now
      setSendTime(now);
      return true;
    }
    /** @return true if a lookup should be sent */
    public boolean shouldPrefetch(long nextTime, long now) {
      // assert (0 < now);
      // assert (now <= nextTime);
      if (!shouldRenew(nextTime)) {
        // too early
        return false;
      }
      if (wasSent()) {
        // already sent
        return false;
      }
      if (!hasData()) {
        // lost the data?
        return false;
      }
      long ttd = (expireTime - dataTime);
      long halfTime = dataTime + (ttd >> 1);
      if (!wasAccessedAfter(halfTime)) {
        // no recent access
        return false;
      }
      // send now
      setSendTime(now);
      return true;
    }
    /** @return true if a lookup should be sent */
    public boolean noteExpired(long now, Response res) {
      // assert (0 < now);
      // assert (hasExpired(now));
      // assert (res != null);
      if (!wasSent()) {
        // just expired, send now
        //
        // keep uid+data
        //
        // assert (0 < now);
        setSendTime(now);
        responses =
          (res == null ?
           Collections.EMPTY_LIST :
           Collections.singletonList(res));
        return true;
      }
      if (res != null) {
        // add this response to the list
        int n = (responses == null ? 0 : responses.size());
        if (n == 0) {
          responses = Collections.singletonList(res);
        } else {
          if (n == 1) {
            List l = new ArrayList();
            Object o1 = responses.get(0);
            l.add(o1);
            responses = l;
          }
          responses.add(res);
        }
      }
      return false;
    }
    public boolean hasHints() {
      return false;
    }
    public AddressEntry getHint(String type) {
      return null;
    }
    public Map getHints() {
      return null;
    }
    public void putHint(String type, AddressEntry ae) {
      throw new RuntimeException("Not an EntryWithHints!");
    }
    public boolean removeHint(String type) {
      throw new RuntimeException("Not an EntryWithHints!");
    }
    /** @return true if this entry can be deleted */
    public boolean canEvict(long now, boolean force) {
      // assert (0 < now);
      if (!force && !hasExpired(now)) {
        // hasn't expired yet
        return false;
      }
      if (wasSent()) {
        // lookup in progress
        //
        // we could clear the uid+data now, to free up space,
        // but this risks invalidating the uid-based lookup
        return false;
      }
      if (hasHints()) {
        // hints keep the entry alive
        return false;
      }
      // okay to evict
      return true;
    }
    /** @return true if this entry should be deleted */
    public boolean flush(long now) {
      if (wasSent()) {
        // force expiration if not already expired
        if (!hasExpired(now)) {
          setExpireTime(now);
        }
        // keep uid+data
        // lookup in progress
        return false;
      }
      if (hasHints()) {
        if (hasData()) {
          // discard data, but can't evict hints
          dataTime = -1;
          uid = null;
          data = null;
          expireTime = -1;
          renewalTime = 0;
          responses = null;
        } else {
          // just hints, leave as-is
        }
        return false;
      }
      // normal entry, can dispose
      return true;
    }
    public List takeResponses() {
      List l = responses;
      responses = null;
      return l;
    }
    public List takeMatchingResponses(UnaryPredicate pred) {
      List ret = null;
      List l = responses;
      int n = (l == null ? 0 : l.size());
      for (int i = 0; i < n; i++) {
        Object oi = l.get(i);
        if (!pred.execute(oi)) {
          continue;
        }
        if (1 < n) {
          // at least one response remains
          l.remove(i);
        } else {
          // back to only hints
          dataTime = -1;
          uid = null;
          data = null;
          expireTime = -1;
          renewalTime = 0;
          responses = null;
        }
        --i;
        --n;
        if (ret == null) {
          ret = new ArrayList(3);
        }
        ret.add(oi);
      }
      return ret;
    }

    //
    // Use a couple bits to record access points over the
    // lifetime of the entry.
    //
    // For example, use 4 bits to record if there's been an
    // access during:
    //    - the first 25% of the entry's life
    //    - the second quarter
    //    - the third quarter
    //    - the last quarter
    // This is an inexpensive debugging tool and can be used
    // by a timer thread to periodically renew entries (e.g.
    // if used in the third or last quarter, renew towards the
    // end).
    //
    // We'll steal the lower bits from the renewalTime, since that's
    // a rough estimate of when to force a renewal.  Of course, we
    // could dedicate a boolean[] or a separate int/long field.
    //

    private long maskTime(long t) {
      if (0 < ACCESS_BITS) {
        return maskBits(t);
      } else {
        return t;
      }
    }
    private void recordAccess(long now) {
      if (0 < ACCESS_BITS) {
        int i = findAccessBit(now);
        renewalTime = markBit(renewalTime, i);
      }
    }
    public boolean wasAccessedAfter(long t) {
      // assert (0 < t);
      if (0 < ACCESS_BITS) {
        int i = findAccessBit(t);
        for (; i < ACCESS_BITS; i++) {
          if (getBit(renewalTime, i)) {
            return true;
          }
        }
        return false;
      } else {
        // assume an access?
        return true;
      }
    }
    private String printAccessHistory(long now) {
      if (0 < ACCESS_BITS) {
        int i = findAccessBit(now);
        return " usage=["+printBits(renewalTime, i)+"]";
      } else {
        return "";
      }
    }
    private int findAccessBit(long now) {
      long age = getDataAge(now);
      long ttl = expireTime - dataTime;
      double percent = ((double) age / ttl);
      int i = findBit(percent);
      return i;
    }

    private int findBit(double percent) {
      return (int) (ACCESS_BITS * percent);
    }
    private long maskBits(long t) {
      return (t & ~((1<<ACCESS_BITS) - 1));
    }
    private long markBit(long t, int i) {
      return (t | (1 << i));
    }
    private boolean getBit(long t, int i) {
      return ((t & (1 << i)) != 0);
    }
    private String printBits(long t, int n) {
      // output looks like "101101  ", where
      //   "1" means bit is on
      //   "0" means bit is off
      //   " " means index is greater than n
      StringBuffer buf = new StringBuffer(ACCESS_BITS);
      int i = 0;
      for (; i <= n; i++) {
        boolean b = getBit(t, i);
        buf.append(b ? "1" : "0");
      }
      for (; i < ACCESS_BITS; i++) {
        buf.append(" ");
      }
      return buf.toString();
    }

    @Override
   public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public String toString(long now) {
      // assert (0 < now);
      boolean hasData = hasData();
      boolean isExpired = hasExpired(now);
      boolean showTimestamps =
        (!isExpired ||
         (hasData && !hasHints()));
      boolean wasSent = wasSent();

      StringBuffer buf = new StringBuffer();
      buf.append("(");
      if (showTimestamps) {
        buf.append("cached=");
        buf.append(Timestamp.toString(dataTime, now));
        if (!wasSent) {
          buf.append(" renew=");
          buf.append(Timestamp.toString(getRenewalTime(), now));
        }
        buf.append(" expire");
        buf.append(isExpired ? "d" : "s");
        buf.append("=");
        buf.append(Timestamp.toString(expireTime, now));
        if (!wasSent) {
          long t = (isExpired ? (expireTime - 1) : now);
          buf.append(printAccessHistory(t));
        }
        buf.append(" ");
      }
      if (wasSent) {
        long sendTime = getSendTime();
        buf.append("sent");
        buf.append("=");
        buf.append(Timestamp.toString(sendTime, now));
        List l = responses;
        if (l == null) {
          l = Collections.EMPTY_LIST;
        }
        buf.append(" responses[");
        buf.append(l.size());
        buf.append("]: ");
        buf.append(l);
      }
      if (hasData) {
        buf.append(" uid=");
        buf.append(uid);
        buf.append(" data=");
        buf.append(data);
      }
      if (hasHints()) {
        buf.append(" hints[");
        Map m = getHints();
        buf.append(m.size());
        buf.append("]: ");
        buf.append(m);
      }
      buf.append(")");

      return buf.toString();
    }
  }

  static class EntryWithHints extends Entry {

    private static final Map NO_HINTS = Collections.EMPTY_MAP;

    private Map hints = NO_HINTS;

    public EntryWithHints() {
      this.dataTime = -1;
      this.uid = null;
      this.data = null;
      this.expireTime = -1;
      this.renewalTime = 0;
      this.responses = null;
    }

    public EntryWithHints(Entry e) {
      this.dataTime = e.dataTime;
      this.uid = e.uid;
      this.data = e.data;
      this.expireTime = e.expireTime;
      this.renewalTime = e.renewalTime;
      this.responses = e.responses;
    }

    @Override
   public boolean hasHints() {
      return hints != NO_HINTS;
    }
    @Override
   public AddressEntry getHint(String type) {
      if (type == null || hints == NO_HINTS) {
        return null;
      }
      return (AddressEntry) hints.get(type);
    }
    @Override
   public Map getHints() {
      return hints;
    }
    @Override
   public void putHint(String type, AddressEntry ae) {
      if (hints == NO_HINTS) {
        hints = new HashMap();
      }
      hints.put(type, ae);
    }
    @Override
   public boolean removeHint(String type) {
      if (hints == NO_HINTS) {
        return data == null;
      } else {
        hints.remove(type);
        if (hints.isEmpty()) {
          hints = NO_HINTS;
          return data == null;
        } else {
          return false;
        }
      }
    }
  }

  class LRUMap extends LinkedHashMap {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public LRUMap(int initialSize) {
      super(initialSize, 0.75f, true);
    }
    @Override
   protected boolean removeEldestEntry(Map.Entry eldest) {
      Entry eldestE =
        (eldest == null ?
         (null) :
         ((Entry) eldest.getValue()));
      return evictLRU(eldestE);
    }
  }

  private class CacheSP 
    implements ServiceProvider {
      private final CacheService cs =
        new CacheService() {
          public void submit(Response res) {
            CacheManager.this.submit(res);
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!CacheService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        return cs;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }

  private class HintSP
    implements ServiceProvider {
      private final HintService hs =
        new HintService() {
          public void add(String name, Bundle bundle) {
            CacheManager.this.add(name, bundle);
          }
          public void remove(String name, Bundle bundle) {
            CacheManager.this.remove(name, bundle);
          }
        };
      public Object getService(
          ServiceBroker sb, Object requestor, Class serviceClass) {
        if (!HintService.class.isAssignableFrom(serviceClass)) {
          return null;
        }
        return hs;
      }
      public void releaseService(
          ServiceBroker sb, Object requestor,
          Class serviceClass, Object service) {
      }
    }
}
