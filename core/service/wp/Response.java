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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.thread.SchedulableStatus;

/**
 * A response from the {@link WhitePagesService}.
 */
public abstract class Response implements Callback, Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

/** A marker exception for a request timeout failure */
  public static final String TIMEOUT = "timeout";

  private final Request request;
  private final Object lock = new Object();
  private transient Set callbacks;
  private Object result;

  /**
   * Responses are created by asking a Request to
   * "createResponse()".
   */
  private Response(Request request) {
    this.request = request;
    if (request == null) {
      throw new RuntimeException("Null request");
    }
  }

  public final Request getRequest() { 
    return request;
  }

  public boolean isAvailable() {
    return (getResult() != null);
  }

  /**
   * Suspend the current thread until response.isAvailable()
   * will return true
   * <p>
   * @return true
   */
  public boolean waitForIsAvailable() throws InterruptedException {
    return waitForIsAvailable(0);
  }

  /**
   * Suspend the current thread until response.isAvailable() will
   * return true or the timeout is exceeded.
   * <p>
   * @param timeout How long to wait.
   * @return true if "isAvailable()"
   */
  public boolean waitForIsAvailable(long timeout) throws InterruptedException {
    synchronized (lock) {
      if (result != null) {
        return true;
      }
      try {
        SchedulableStatus.beginWait("WP lookup");
        lock.wait(timeout);
      } finally {
        SchedulableStatus.endBlocking();
      }
      return (result != null);
    }
  }

  /** 
   * Install a callback to be invoked when the response is available.
   * <p>
   * If the response is already available when this method is called,
   * the callback my be invoked in the calling thread immediately.
   * <p>
   * @param c A runnable to be executed when a result is
   * available.  This will be called exactly once.  The
   * callback.execute(Result) method should execute quickly - under
   * no circumstances should it ever block or perform any non-trivial
   * tasks.
   */
  public void addCallback(Callback c) {
    if (c == null) {
      throw new IllegalArgumentException("Null callback");
    }
    synchronized (lock) {
      if (result != null) {
        c.execute(this);
      } else {
        if (callbacks == null) {
          callbacks = Collections.singleton(c);
        } else if (!callbacks.contains(c)) {
          if (callbacks.size() == 1) {
            Object o = callbacks.iterator().next();
            callbacks = new HashSet(5);
            callbacks.add(o);
          }
          callbacks.add(c);
        }
      }
    }
  }

  /**
   * Remove a callback.
   */
  public void removeCallback(Callback c) {
    if (c == null) {
      throw new IllegalArgumentException("Null callback");
    }
    synchronized (lock) {
      if (result == null && 
          callbacks != null) {
        if (callbacks.size() == 1) {
          if (callbacks.contains(c)) {
            callbacks = null;
          }
        } else {
          callbacks.remove(c);
        }
      }
      // return true if was contained?  not safe, since
      // callbacks are invoked outside the lock...
    }
  }

  public final Object getResult() { 
    synchronized (lock) {
      return result;
    }
  }

  public final boolean setResult(Object r) {
    if (r == null) {
      r = getDefaultResult();
    }
    Set s;
    synchronized (lock) {
      if (result != null) {
        if (r == TIMEOUT || result == TIMEOUT) {
          // okay, ignored timeout
          return true;
        }
        return false;
      }
      this.result = r;
      lock.notifyAll();
      if (callbacks == null) {
        return true;
      }
      s = callbacks;
      callbacks = null;
    }
    for (Iterator iter = s.iterator(); iter.hasNext(); ) {
      Callback c = (Callback) iter.next();
      c.execute(this);
    }
    return true;
  }

  protected abstract Object getDefaultResult();

  // let a response be a callback, for easy chaining
  public void execute(Response res) {
    if (res == this) {
      // invalid chain!
      return;
    }
    if (res == null || !res.isAvailable()) {
      throw new IllegalArgumentException(
          "Invalid callbach result: "+res);
    }
    setResult(res.getResult());
  }

  // equals is ==

  @Override
public String toString() {
    Object r = getResult();
    return
      "(response oid="+
      System.identityHashCode(this)+
      " req="+request+
      (r == null ? "" : " result="+r)+
      ")";
  }

  public boolean isSuccess() {
    Object r = getResult();
    return 
      (!
       (r == null ||
        r == TIMEOUT ||
        r instanceof Exception));
  }

  public boolean isTimeout() {
    Object r = getResult();
    return (r == TIMEOUT);
  }

  public Exception getException() {
    Object r = getResult();
    return 
      (r instanceof Exception) ?
      ((Exception) r) :
      null;
  }

  /** @see Request.Get */
  public static class Get extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public static final Object NULL = new Object() {
      private Object readResolve() { return NULL; }
      @Override
      public String toString() { return "null_get"; }
    };
    public Get(Request q) {
      this((Request.Get) q);
    }
    public Get(Request.Get q) {
      super(q);
    }
    /**
     * @return true if the named agent exists, even if it
     * lacks the requested entry type
     * (<code>getAddressEntry() == null</code>).
     */
    public boolean agentExists() {
      // this implementation requires the server to respond with a
      // "getAll" map or non-null entry.  NULL should only be used
      // if the agent is unknown.
      Object r = getResult();
      return 
        (r instanceof AddressEntry ||
         ((r instanceof Map) && 
          (!((Map) r).isEmpty())));
    }
    /**
     * @return the matching entry, or null if the agent does
     * not exist (<code>!agentExists()</code>) or the agent exists
     * but lacks the requested entry type.
     */
    public AddressEntry getAddressEntry() { 
      Object r = getResult();
      if (r instanceof AddressEntry) {
        return ((AddressEntry) r);
      } else if (r == NULL) {
        return null;
      } else if (r instanceof Map) {
        // the server can answer "get" with "getAll" map
        Map m = (Map) r;
        Request.Get rg = (Request.Get) getRequest();
        String type = rg.getType();
        return (AddressEntry) m.get(type);
      } else {
        // !isSuccess
        return null;
      }
    }
    @Override
   protected Object getDefaultResult() {
      return NULL;
    }
  }

  /** @see Request.GetAll */
  public static class GetAll extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public GetAll(Request q) {
      this((Request.GetAll) q);
    }
    public GetAll(Request.GetAll q) {
      super(q);
    }
    /**
     * @return true if the agent exists
     */
    public boolean agentExists() {
      Map m = getAddressEntries();
      return (m != null && !m.isEmpty());
    }
    /**
     * @return a Map of (String type, AddressEntry entry) pairs,
     * or either an empty map or null if the agent does not exist.
     */
    // Map<String,AddressEntry>
    public Map getAddressEntries() { 
      Object r = getResult();
      return
        (r instanceof Map) ?
        ((Map) r) :
        null;
    }
    @Override
   protected Object getDefaultResult() {
      return Collections.EMPTY_MAP;
    }
  }

  /** @see Request.List */
  public static class List extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public List(Request q) {
      this((Request.List) q);
    }
    public List(Request.List q) {
      super(q);
    }
    /**
     * @return true if the listed suffix contains entries.
     */
    public boolean suffixExists() {
      Set s = getNames();
      return (s != null && !s.isEmpty());
    }
    /**
     * @return a Set of entry names.
     */
    // Set<String>
    public Set getNames() { 
      Object r = getResult();
      return
        (r instanceof Set) ?
        ((Set) r) :
        null;
    }
    @Override
   protected Object getDefaultResult() {
      return Collections.EMPTY_SET;
    }
  }

  /** @see Request.Flush */
  public static class Flush extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public Flush(Request q) {
      this((Request.Flush) q);
    }
    public Flush(Request.Flush q) {
      super(q);
    }
    /** Did the flush modify the local cache or force a lookup? */
    public boolean modifiedCache() {
      Object r = getResult();
      return
        (r instanceof Boolean) ?
        ((Boolean) r).booleanValue() :
        false;
    }
    @Override
   protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }

  /** @see Request.Bind */
  public static class Bind extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public Bind(Request q) {
      this((Request.Bind) q);
    }
    public Bind(Request.Bind q) {
      super(q);
    }
    /** Was the bind successful? */
    public boolean didBind() {
      return (getExpirationTime() > 0);
    }
    /** 
     * If <code>(didBind() == false)<code>, was the failure
     * due to another conflicting local bind while this bind
     * request was still pending?
     * <p>
     * Other possibilities include a bind-usurper
     * (<code>getUsurperEntry()</code>) or an exception
     * (<code>getException()</code>).
     */
    public boolean wasCanceled() {
      return (getCancelingRequest() != null);
    }
    /**
     * If successfully bound or renewed, when does the lease
     * expire?
     */
    public long getExpirationTime() {
      Object r = getResult();
      return
        (r instanceof Long) ?
        ((Long) r).longValue() :
        -1;
    }
    /**
     * If not bound or renewed, who took our place?
     * <p>
     * This is always null for a rebind (overwrite == true).
     * <p>
     * Isn't this !isSuccess ?
     */
    public AddressEntry getUsurperEntry() {
      Object r = getResult();
      return
        (r instanceof AddressEntry) ?
        ((AddressEntry) r) :
        null;
    }
    /**
     * If this request was canceled by another conflicting
     * local bind, what was the request?
     *
     * @return a Request.Bind or Request.Unbind
     */
    public Request getCancelingRequest() {
      Object r = getResult();
      return
        (r instanceof Request) ?
        ((Request) r) :
        null;
    }
    @Override
   protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }

  /** @see Request.Unbind */
  public static class Unbind extends Response {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public Unbind(Request q) {
      this((Request.Unbind) q);
    }
    public Unbind(Request.Unbind q) {
      super(q);
    }
    /**
     * Did the unbind succeed?
     * <p>
     * isn't this the same as isSuccess ?
     */
    public final boolean didUnbind() {
      Object r = getResult();
      return Boolean.TRUE.equals(r);
    }
    @Override
   protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }
}
