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

package org.cougaar.core.service.wp;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.Service;
import org.cougaar.util.log.Logging;

/**
 * The white pages service provides access to the distributed name
 * server.
 * <p>
 * The primary function of the white pages is to allow agents
 * to register their message transport addresses and lookup the
 * addresses of other agents.  This service is the client-side
 * resolver and is backed by a cache.
 * <p>
 * The main method of this class is:<pre>
 *    public abstract Response submit(Request req);
 * </pre>
 * This submits an request with an asynchronous response.  The
 * caller can attach callbacks to the response or block until
 * the response result is set.  All the other methods of the
 * WhitePagesService are based upon the above "submit" method.
 * <p>
 * The white pages service currently does not support a "listener"
 * API to watch for changes, primarily due to scalability concerns.
 */
public abstract class WhitePagesService implements Service {

  //
  // no-timeout variations:
  //
  // these methods have all been deprecated (bug 2875).  Blocking
  // calls tie up the threads in the Cougaar thread pool.
  //
  // Clients should either use the non-blocking cache-only methods:
  //   get(name, type, -1)
  //   getAll(name, -1)
  //   list(suffix, -1)
  //   flush(name, minAge, ae, uncache, prefetch, -1)
  // or the asynchronous callback-based methods:
  //   get(name, type, callback)
  //   getAll(name, callback)
  //   list(suffix, callback)
  //   flush(name, minAge, ae, uncache, prefetch, callback)
  //   bind(ae, callback)
  //   rebind(ae, callback)
  //   hint(ae, callback)
  //   unbind(ae, callback)
  //   unhint(ae, callback)
  //
  // If the answer is already in the cache then the callback will
  // be invoked immediately.  Flush can use an asynchronous callback,
  // even though it's guaranteed not to block.
  //
  // In Cougaar 10.4.1 the cache-only methods will initiate a
  // background fetch for the data, which will eventually be entered
  // into the cache.  However, the cache has a fixed LRU size and
  // will expire entries, so if the result is required then a
  // callback should be used.
  //
  // If the client must block until the result is known, which is
  // discouraged, it can write a callback like:
  //   Response[] answer = new Response[1];
  //   Callback callback = new Callback() {
  //     public void execute(Response res) {
  //       synchronized (answer) {
  //         answer[0] = res;
  //         answer.notifyAll();
  //       }
  //     }
  //   };
  //   whitePagesService.getAll("testme", callback);
  //   Response res;
  //   synchronized (answer) {
  //     while (answer[0] == null) {
  //       answer.wait();
  //     }
  //     res = answer[0];
  //   }
  //
  // In Cougaar 10.4.2+ the following blocking methods will be
  // removed and the method signatures will be *reused* for the
  // cache-only APIs (i.e. the calls with a -1 as the last
  // parameter).
  //

  /**
   * @see Request.Get
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final AddressEntry get(
      String name, String type) throws Exception {
    return get(name, type, 0);
  }
  /**
   * @see Request.GetAll
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final Map getAll(String name) throws Exception {
    return getAll(name, 0);
  }
  /** 
   * @see Request.List 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final Set list(String suffix) throws Exception {
    return list(suffix, 0);
  }
  /** 
   * @see Request.Flush 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final boolean flush(
      String name, long minAge, AddressEntry ae,
      boolean uncache, boolean prefetch) throws Exception {
    return flush(name, minAge, ae, uncache, prefetch, 0);
  }
  /** 
   * @see Request.Bind 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final void bind(AddressEntry ae) throws Exception {
    bind(ae, 0);
  }
  /** 
   * @see Request.Bind 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final void rebind(AddressEntry ae) throws Exception {
    rebind(ae, 0);
  }
  /** 
   * @see Request.Bind 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final void hint(AddressEntry ae) throws Exception {
    hint(ae, 0);
  }
  /** 
   * @see Request.Unbind 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final void unbind(AddressEntry ae) throws Exception {
    unbind(ae, 0);
  }
  /** 
   * @see Request.Unbind 
   * @deprecated use a non-blocking cache-only lookup or callback
   */
  public final void unhint(AddressEntry ae) throws Exception {
    unhint(ae, 0);
  }

  // 
  // callback variations:
  //

  /** @see Request.Get */
  public final void get(
      String name, String type, Callback callback) {
    submit(new Request.Get(Request.NONE, name, type), callback);
  }
  /** @see Request.GetAll */
  public final void getAll(String name, Callback callback) {
    submit(new Request.GetAll(Request.NONE, name), callback);
  }
  /** @see Request.List */
  public final void list(String suffix, Callback callback) {
    submit(new Request.List(Request.NONE, suffix), callback);
  }
  /** @see Request.Flush */
  public final void flush(
      String name, long minAge, AddressEntry ae,
      boolean uncache, boolean prefetch, Callback callback) throws Exception {
    submit(
        new Request.Flush(
          Request.CACHE_ONLY, name, minAge, ae, uncache, prefetch),
        callback);
  }
  /** @see Request.Bind */
  public final void bind(AddressEntry ae, Callback callback) {
    submit(new Request.Bind(Request.NONE, ae, false, false), callback);
  }
  /** @see Request.Bind */
  public final void rebind(AddressEntry ae, Callback callback) {
    submit(new Request.Bind(Request.NONE, ae, true, false), callback);
  }
  /** @see Request.Bind */
  public final void hint(AddressEntry ae, Callback callback) {
    submit(new Request.Bind(Request.CACHE_ONLY, ae, true, false), callback);
  }
  /** @see Request.Unbind */
  public final void unbind(AddressEntry ae, Callback callback) {
    submit(new Request.Unbind(Request.NONE, ae), callback);
  }
  /** @see Request.Unbind */
  public final void unhint(AddressEntry ae, Callback callback) {
    submit(new Request.Unbind(Request.CACHE_ONLY, ae), callback);
  }

  //
  // timeout variations:
  //
  // any timeout duration that's greater than or equal to zero
  // is deprecated.  Only negative timeouts (cache-only) are
  // not deprecated.
  //

  public static final class TimeoutException 
    extends InterruptedException {
      /**
    * 
    */
   private static final long serialVersionUID = 1L;
      private final boolean b;
      public TimeoutException(boolean b) {
        super("Timeout on "+(b ? "Request" : "Response"));
        this.b = b;
      }
      /**
       * @return true if the Request timeout was too short, else
       * return false if the wait for the Response was too short.
       */
      public boolean isRequestTimeout() { return b; }
    }

  /** 
   * @see Request.Get
   * @param timeout non-negative values are deprecated
   */
  public final AddressEntry get(
      String name, String type,
      long timeout) throws Exception {
    int options = Request.NONE;
    if (timeout < 0) {
      options |= Request.CACHE_ONLY;
      timeout = 0;
    }
    Request.Get req = new Request.Get(options, name, type);
    Response.Get res = (Response.Get) assertSubmit(req, timeout);
    return res.getAddressEntry();
  }
  /** 
   * @see Request.GetAll
   * @param timeout non-negative values are deprecated
   */
  public final Map getAll(String name, long timeout) throws Exception {
    int options = Request.NONE;
    if (timeout < 0) {
      options |= Request.CACHE_ONLY;
      timeout = 0;
    }
    Request.GetAll req = new Request.GetAll(options, name);
    Response.GetAll res = (Response.GetAll) assertSubmit(req, timeout);
    return res.getAddressEntries();
  }
  /** 
   * @see Request.List
   * @param timeout non-negative values are deprecated
   */
  public final Set list(String suffix, long timeout) throws Exception {
    int options = Request.NONE;
    if (timeout < 0) {
      options |= Request.CACHE_ONLY;
      timeout = 0;
    }
    Request.List req = new Request.List(options, suffix);
    Response.List res = (Response.List) assertSubmit(req, timeout);
    return res.getNames();
  }
  /** 
   * @see Request.Flush
   * @param timeout non-negative values are deprecated
   */
  public final boolean flush(
      String name, long minAge, AddressEntry ae,
      boolean uncache, boolean prefetch,
      long timeout) throws Exception {
    int options = Request.NONE;
    if (timeout < 0) {
      options |= Request.CACHE_ONLY;
      timeout = 0;
    }
    Request.Flush req = new Request.Flush(
        options, name, minAge, ae, uncache, prefetch);
    Response.Flush res = (Response.Flush) assertSubmit(req, timeout);
    return res.modifiedCache();
  }
  /** 
   * @see Request.Bind
   * @param timeout non-negative values are deprecated
   */
  public final void bind(AddressEntry ae, long timeout) throws Exception {
    if (timeout < 0) {
      throw new IllegalArgumentException("Negative bind timeout");
    }
    Request.Bind req = new Request.Bind(Request.NONE, ae, false, false);
    Response.Bind res = (Response.Bind) assertSubmit(req, timeout);
    if (!res.didBind()) {
      throw new RuntimeException("Bind failed: "+res);
    }
  }
  /** 
   * @see Request.Bind
   * @param timeout non-negative values are deprecated
   */
  public final void rebind(AddressEntry ae, long timeout) throws Exception {
    if (timeout < 0) {
      throw new IllegalArgumentException("Negative rebind timeout");
    }
    Request.Bind req = new Request.Bind(Request.NONE, ae, true, false);
    Response.Bind res = (Response.Bind) assertSubmit(req, timeout);
    if (!res.didBind()) {
      throw new RuntimeException("Rebind failed: "+res);
    }
  }
  /** 
   * @see Request.Bind
   * @param timeout non-negative values are deprecated
   */
  public final void hint(AddressEntry ae, long timeout) throws Exception {
    if (timeout < 0) {
      timeout = 0; // timeout doesn't really apply here...
    }
    Request.Bind req = new Request.Bind(Request.CACHE_ONLY, ae, true, false);
    Response.Bind res = (Response.Bind) assertSubmit(req, timeout);
    if (!res.didBind()) {
      throw new RuntimeException("Hint failed: "+res);
    }
  }
  /** 
   * @see Request.Unbind
   * @param timeout non-negative values are deprecated
   */
  public final void unbind(AddressEntry ae, long timeout) throws Exception {
    if (timeout < 0) {
      throw new IllegalArgumentException("Negative unbind timeout");
    }
    Request.Unbind req = new Request.Unbind(Request.NONE, ae);
    assertSubmit(req, timeout);
  }
  /** 
   * @see Request.Unbind
   * @param timeout non-negative values are deprecated
   */
  public final void unhint(AddressEntry ae, long timeout) throws Exception {
    if (timeout < 0) {
      timeout = 0; // timeout doesn't really apply here...
    }
    Request.Unbind req = new Request.Unbind(Request.CACHE_ONLY, ae);
    assertSubmit(req, timeout);
  }

  /**
   * Submit a request and return the response if it is completed
   * within the request's timeout and successful, otherwise throw
   * an exception.
   */
  public final Response assertSubmit(
      Request req, long timeout) throws Exception {
    Response res = submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return res;
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  /**
   * Submit with a callback.
   * <p>
   * Equivalent to:<pre>
   *    Response res = submit(req);
   *    res.addCallback(c);
   *    return res;
   * </pre>
   */
  public final Response submit(Request req, Callback c) {
    Response res = submit(req);
    res.addCallback(c);
    return res;
  }

  /**
   * Submit a request, get back a "future reply" response.
   * <p>
   * An example cache-only non-blocking usage:<pre>
   *   try {
   *     Map m = wps.getAll("foo", -1);
   *     System.out.println("cached entries for foo: "+m);
   *   } catch (Exception e) {
   *     System.out.println("failed: "+e);
   *   }
   * </pre>
   * <p>
   * An example asynchronous "callback" usage:<pre>
   *    Request req = new Request.GetAll(Request.NONE, "foo");
   *    Response r = wps.submit(req);
   *    Callback callback = new Callback() {
   *      public void execute(Response res) {
   *        // note that (res == r)
   *        if (res.isSuccess()) {
   *          Map m = ((Response.GetAll) res).getAddressEntries();
   *          System.out.println("got all entries for foo: "+m);
   *        } else {
   *          System.out.println("failed: "+res);
   *        }
   *      }
   *    };
   *    // add callback, will execute immediately if 
   *    // there is already an answer
   *    r.addCallback(callback);
   *    // keep going
   * </pre>
   *
   * @param req the non-null request
   * @return a non-null response
   */
  public abstract Response submit(Request req);

  //
  // deprecated, to be removed in Cougaar 10.4.1+
  //

  /** @deprecated use "Map getAll(name)" */
  public final AddressEntry[] get(String name) throws Exception {
    return get(name, 0);
  }
  /** @deprecated use "get(name,type)" */
  public final AddressEntry get(
      String name, Application app, String scheme) throws Exception {
    return get(name, app, scheme, 0);
  }
  /** @deprecated use "Map getAll(name,timeout)" */
  public final AddressEntry[] get(String name, long timeout) throws Exception {
    Map m = getAll(name, 0);
    AddressEntry[] ret;
    if (m == null || m.isEmpty()) {
      ret = new AddressEntry[0];
    } else {
      int msize = m.size();
      ret = new AddressEntry[msize];
      int i = 0;
      for (Iterator iter = m.values().iterator();
          iter.hasNext();
          ) {
        AddressEntry aei = (AddressEntry) iter.next();
        ret[i++] = aei;
      }
    }
    return ret;
  }
  /** @deprecated use "get(name,type,timeout)" */
  public final AddressEntry get(
      String name,
      Application app,
      String scheme,
      long timeout) throws Exception {
    String origType = app.toString();
    String type = origType;
    // backwards compatibility hack:
    if ("topology".equals(type) && "version".equals(scheme)) {
      type="version";
    }
    if (type != origType) {
      Exception e = 
        new RuntimeException(
            "White pages \"get("+name+
            ", "+origType+", "+scheme+
            ")\" should be replaced with"+
            "\"get("+name+", "+type+")\"");
      Logging.getLogger(getClass()).warn(
          null, e);
    }
    AddressEntry ae = get(name, type, timeout);
    if (ae != null &&
        !scheme.equals(ae.getURI().getScheme())) {
      Exception e = 
        new RuntimeException(
            "White pages \"get("+name+
            ", "+origType+", "+scheme+
            ")\" returned an entry with a different"+
            " URI scheme: "+ae);
      Logging.getLogger(getClass()).warn(
          null, e);
    }
    return ae;
  }
}
