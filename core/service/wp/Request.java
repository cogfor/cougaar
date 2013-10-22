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

/**
 * A request for the {@link WhitePagesService}.
 * <p>
 * Request objects are immutable.  A client submits a request
 * to the white pages and watches the mutable Response object.
 */
public abstract class Request implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

/**
   * The options flag to indicate no options.
   */
  public static final int NONE = 0;

  /**
   * Flag to limit the operation to the local cache.
   * <p>
   * For "get", "getAll", and "list", a CACHE_ONLY flag limits the
   * lookup to the local cache.  If the result is not in the cache
   * then the result will be set to the default value as defined
   * below.
   * <p>
   * For "bind", this can be used to bootstrap entries into the local
   * (client-side) "get" table.  This can be used for both the local
   * agents and remote agents discovered through non-WP mechanisms.
   * If a future "get" request is not in the cache, then the hints
   * are checked and will be used if present.  A hint can be removed
   * with an "unbind-hint".
   */
  public static final int CACHE_ONLY   = (1<<1);
  // todo: recurse, authority-only, etc

  private final int options;

  private Request(int options) {
    this.options = options;
  }

  public final boolean hasOption(int mask) {
    return ((options & mask) != 0);
  }

  /**
   * Create a response object for this request.
   */
  public abstract Response createResponse();

  @Override
public String toString() {
    return 
      " oid="+System.identityHashCode(this)+
      " cacheOnly="+hasOption(CACHE_ONLY)+
      ")";
  }

  /**
   * Test to see if the specified name string is valid
   * for use in Get, GetAll, Bind, and Unbind requests.
   * <p>
   * Clients should follow the Internet host name format
   * (RFC 2396).
   */
  public static final boolean isValidName(String name) {
    if (name == null ||
        name.length() == 0 ||
        !Character.isLetterOrDigit(name.charAt(0))) {
      return false;
    }
    return true;
  }

  /**
   * Test to see if the specified suffix string is valid
   * for use in List requests.
   * <p>
   * The suffix must start with '.'.  For names other than '.',
   * trailing '.'s will be ignored (e.g. ".x." is the same as
   * ".x").
   * <p>
   * The name space is separated by using the "." character, just
   * like internet host names (RFC 952).  All children of a name
   * must have a matching suffix, and only the direct (first-level)
   * children will be listed.
   * <p>
   * For example, given:<pre>
   *    list(".foo.com")
   * </pre>the result may look like be:<pre>
   *    { "www.foo.com", ".bar.foo.com" }
   * </pre>where "www.foo.com" is an entry, and ".bar.foo.com" is a
   * suffix for one or more child entries.  If there were entries
   * for both "www.foo.com" and subchild "test.www.foo.com", then both
   * would be listed:<pre>
   *    { "www.foo.com", ".www.foo.com", ".bar.foo.com" }
   * </pre>Note that listing ".foo.com" will not list the second
   * level children, such as "test.www.foo.com".  The client must
   * do the <i>(non-scalable)</i> depth traversal itself.
   * <p>
   * The precise regex pattern is:<pre>
   *     new java.util.regex.Pattern(
   *       "^\.?[^\.]+" +
   *       suffix +
   *       "$");</pre>
   */
  public static final boolean isValidSuffix(String suffix) {
    if (suffix == null ||
        suffix.length() == 0 ||
        suffix.charAt(0) != '.') {
      return false;
    }
    return true;
  }

  /**
   * Get the entry with the matching (name, type) fields.
   *
   * @see Request.GetAll get all entries with a given name
   */
  public static final class Get extends Request {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final String name;
    private final String type;
    private transient int _hc;
    public Get(
        int options,
        String name,
        String type) {
      super(options);
      this.name = name;
      this.type = type;
      if (!isValidName(name)) {
        throw new IllegalArgumentException("Invalid name: "+name);
      }
      if (type == null) {
        throw new IllegalArgumentException("Null type");
      }
    }
    public String getName() { return name; }
    public String getType() { return type; }
    @Override
   public Response createResponse() {
      return new Response.Get(this);
    }
    @Override
   public int hashCode() {
      if (_hc == 0) {
        int h = 0;
        h = 31*h + name.hashCode();
        h = 31*h + type.hashCode();
        _hc = h;
      }
      return _hc;
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Get)) {
        return false;
      } else {
        Get g = (Get) o;
        return
          (name.equals(g.name) &&
           type.equals(g.type));
      }
    }
    @Override
   public String toString() {
      return 
        "(get name="+getName()+
        " type="+getType()+
        super.toString();
    }
  }

  /**
   * Get all entries associated with the given name.
   *
   * @see Request.Get do a specific (name, type) lookup
   */
  public static final class GetAll extends Request {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final String name;
    public GetAll(
        int options,
        String name) {
      super(options);
      this.name = name;
      if (!isValidName(name)) {
        throw new IllegalArgumentException("Invalid name: "+name);
      }
    }
    public String getName() { 
      return name;
    }
    @Override
   public Response createResponse() {
      return new Response.GetAll(this);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof GetAll)) {
        return false;
      } else {
        return name.equals(((GetAll) o).name);
      }
    }
    @Override
   public int hashCode() {
      return name.hashCode();
    }
    @Override
   public String toString() {
      return 
        "(getAll name="+getName()+
        super.toString();
    }
  }

  /**
   * List the name of all direct children with the given suffix.
   * <p>
   * This is similar to a DNS zone transfer (AXFR) limited to depth=1.
   */
  public static final class List extends Request {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final String suffix;
    public List(
        int options,
        String suffix) {
      super(options);
      if (!isValidSuffix(suffix)) {
        throw new IllegalArgumentException("Invalid suffix: "+suffix);
      }
      // trim tail '.'s
      String suf = suffix;
      int len = (suf == null ? 0 : suf.length());
      while (--len > 0 && suf.charAt(len) == '.') {
        suf = suf.substring(0, len);
      }
      this.suffix = suf;
    }
    public String getSuffix() { 
      return suffix;
    }
    @Override
   public Response createResponse() {
      return new Response.List(this);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof List)) {
        return false;
      } else {
        return suffix.equals(((List) o).suffix);
      }
    }
    @Override
   public int hashCode() {
      return suffix.hashCode();
    }
    @Override
   public String toString() {
      return
        "(list suffix="+getSuffix()+
        super.toString();
    }
  }

  /**
   * Modifies the local white pages cache to remove or refetch
   * cached "getAll" or "list" data.
   * <p>
   * This is always CACHE_ONLY, since it can only modify the
   * local white pages cache.
   * <p>
   * The client must specify the name of the "getAll" entry (or
   * equivalent "list" name suffix).  One or more of the
   * following optional assertions can also be specified:<ul>
   *   <li>The minimal age for the cached data in milliseconds,
   *       to avoid flushing new data.</li>
   *   <li>An AddressEntry that must be listed in the current
   *       cached "getAll" map of entries, to assert that the
   *       cache hasn't been update while the flush is in
   *       progress.</li>
   * </ul>
   * <p>
   * If the above assertions are satisfied, one or more of these
   * specified actions can be performed:<ul>
   *   <li>To <i>uncache</i> all entries associated with the name.
   *       This should only be done if the locally cached entry
   *       is certainly stale (e.g. due to information received
   *       from a third party)</li>
   *   <li>To <i>prefetch</i> in a background thread for updated
   *       cache data, without uncaching the maybe-stale cached
   *       entries.  This should be used if the locally cached entry
   *       is usable for now but suspected to be stale (e.g. a
   *       message address used to work but is now unreachable).</li>
   * </ul>
   * Clients that use flush requests should carefully weigh the
   * performance costs of local cache validity verses increased
   * white pages message traffic.
   */
  public static final class Flush extends Request {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final String name;
    private final long minAge;
    private final AddressEntry ae;
    private final boolean uncache;
    private final boolean prefetch;

    public Flush(
        int options,
        String name,
        long minAge,
        AddressEntry ae,
        boolean uncache,
        boolean prefetch) {
      super(options);
      this.name = name;
      this.minAge = minAge;
      this.ae = ae;
      this.uncache = uncache;
      this.prefetch = prefetch;
      if (!hasOption(CACHE_ONLY)) {
        throw new IllegalArgumentException(
            "Flush must be \"CACHE_ONLY\"");
      }
      if (name == null || name.length() <= 0) {
        throw new IllegalArgumentException("Invalid name: "+name);
      }
      if (ae != null && !name.equals(ae.getName())) {
        throw new IllegalArgumentException(
            "Asserted AddressEntry name "+ae.getName()+
            " doesn't match the specified flush name: "+name+
            " "+ae);
      }
      if (name.charAt(0) == '.' && ae != null) {
        throw new IllegalArgumentException(
            "List suffix "+name+" can't specify an AddressEntry"+
            " assertion "+ae);
      }
      if (!uncache && !prefetch) {
        throw new IllegalArgumentException(
            "Must specify either \"uncache\" and/or \"prefetch\"");
      }
    }
    public String getName() {
      return name;
    }
    public long getMinimumAge() {
      return minAge;
    }
    public AddressEntry getAddressEntry() {
      return ae;
    }
    public boolean isUncache() {
      return uncache;
    }
    public boolean isPrefetch() {
      return prefetch;
    }
    @Override
   public Response createResponse() {
      return new Response.Flush(this);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Flush)) {
        return false;
      } else {
        Flush f = (Flush) o;
        return 
          (name.equals(f.name) &&
           minAge == f.minAge &&
           (ae == null ? f.ae == null : ae.equals(f.ae)) &&
           (uncache == f.uncache) &&
           (prefetch == f.prefetch));
      }
    }
    @Override
   public int hashCode() {
      return 
        (name.hashCode() +
         (uncache ? 1 : 2) +
         (prefetch ? 3 : 4));
    }
    @Override
   public String toString() {
      return 
        "("+
        (isUncache() ?
         ("uncache"+(isPrefetch() ? "+" : "")) :
         "")+
        (isPrefetch() ? "prefetch" : "")+
        " "+
        getName()+
        (0 < getMinimumAge() ?
         (" minAge="+getMinimumAge()) :
         "")+
        (getAddressEntry() == null ?
         "" :
         (" entry="+getAddressEntry()))+
        super.toString();
    }
  }

  /**
   * Bind a new entry, or rebind an existing entry if the overwrite
   * flag is false.
   * <p>
   * See the above notes on the CACHE_ONLY flag for binding
   * client-side bootstrap "hints".
   * <p>
   * The renewal flag is for the infrastructure's use, for renewing
   * bind leases.
   */
  public static final class Bind extends Request {

    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final AddressEntry ae;
    private final boolean overWrite;
    private final boolean renewal;

    public Bind(
        int options,
        AddressEntry ae,
        boolean overWrite,
        boolean renewal) {
      super(options);
      this.ae = ae;
      this.overWrite = overWrite;
      this.renewal = renewal;
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
      if (!isValidName(ae.getName())) {
        // this could be moved into AddressEntry itself...
        throw new IllegalArgumentException(
            "Invalid name: "+ae.getName());
      }
      if (renewal && (overWrite || hasOption(CACHE_ONLY))) {
        throw new IllegalArgumentException(
            "Renewal implies non-overwrite and non-cache-only");
      }
    }
    public AddressEntry getAddressEntry() { 
      return ae;
    }
    public boolean isOverWrite() {
      return overWrite;
    }
    public boolean isRenewal() {
      return renewal;
    }
    @Override
   public Response createResponse() {
      return new Response.Bind(this);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Bind)) {
        return false;
      } else {
        Bind b = (Bind) o;
        return 
          (ae.equals(b.ae) &&
           overWrite == b.overWrite &&
           renewal == b.renewal);
      }
    }
    @Override
   public int hashCode() {
      return 
        (ae.hashCode() + 
         (overWrite ? 1 : 2) +
         (renewal ? 3 : 4));
    }
    @Override
   public String toString() {
      return 
        "("+
        (hasOption(CACHE_ONLY) ? "hint_" : "")+
        (isOverWrite() ? "rebind" :
         isRenewal() ? "renew" :
         "bind")+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }

  /**
   * Destroy the binding for the specified entry.
   * <p>
   * The client must pass the current value for the bound entry.
   */
  public static final class Unbind extends Request {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final AddressEntry ae;
    public Unbind(
        int options,
        AddressEntry ae) {
      super(options);
      this.ae = ae;
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
      if (!isValidName(ae.getName())) {
        // this could be moved into AddressEntry itself...
        throw new IllegalArgumentException(
            "Invalid name: "+ae.getName());
      }
    }
    public AddressEntry getAddressEntry() {
      return ae;
    }
    @Override
   public Response createResponse() {
      return new Response.Unbind(this);
    }
    @Override
   public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Unbind)) {
        return false;
      } else {
        Unbind u = (Unbind) o;
        return ae.equals(u.ae);
      }
    }
    @Override
   public int hashCode() {
      return ae.hashCode();
    }
    @Override
   public String toString() {
      return 
        "("+
        (hasOption(CACHE_ONLY) ? "unhint" : "unbind")+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }
}
