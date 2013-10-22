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

package org.cougaar.core.wp.bootstrap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 * This utility class reads "-Dorg.cougaar.name.server" system
 * properties and the "alpreg.ini" for bootstrap {@link Bundle}s
 * provided by the {@link ConfigService}.
 * 
 * @see #parse(String) detailed parsing notes
 */
public class ConfigReader {

  private static final String FILENAME = "alpreg.ini";
  private static final String SERVER_PROP = "org.cougaar.name.server";
  private static final String PROP_PREFIX = SERVER_PROP+".";
  private static final String DEFAULT_SCHEME = "rmi";
  private static final long DEFAULT_TTD = 240000;
  // cougaar.org's IP with the first four bits masked to 1100:
  private static final String DEFAULT_MULTICAST_URI =
    "224.22.165.34:7777";

  private static final Object lock = new Object();

  private static Map bundles;

  // these are only used if "bundles" is null
  private static Logger logger; 
  private static String host;
  private static int port;

  public static Map getBundles() {
    synchronized (lock) {
      ensureLoad();
      return bundles;
    }
  }

  private static void ensureLoad() {
    if (bundles == null) {
      bundles = new HashMap();
      readConfig();
      if (bundles.isEmpty()) {
        bundles = Collections.EMPTY_MAP;
        return;
      }
      bundles = Collections.unmodifiableMap(bundles);
    }
  }

  private static void readConfig() {
    logger = LoggerFactory.getInstance().createLogger(
        ConfigReader.class);
    if (logger.isDetailEnabled()) {
      logger.detail("reading config");
    }
    try {
      read_file(FILENAME);
      read_props();
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed readConfig", e);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info("Read config: "+bundles);
    }
    logger = null;
  }

  private static void add(String line) {
    Object o = parse(line);
    if (o == null) {
      return;
    }
    if (logger.isDetailEnabled()) {
      logger.detail("parsed "+line+" as "+o);
    }
    if (o instanceof Bundle) {
      add((Bundle) o);
    } else if (o instanceof AddressEntry) {
      add((AddressEntry) o);
    } else if (o instanceof List) {
      add((List) o);
    } else {
      throw new RuntimeException("Illegal type: "+o);
    }
  }

  private static void add(Bundle b) {
    if (b == null) {
      return;
    }
    String name = b.getName();
    if (name == null) {
      return;
    }
    Bundle oldB = (Bundle) bundles.get(name);
    if (b.equals(oldB)) {
      return;
    }
    bundles.put(name, b);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Adding "+b+
          (oldB == null ?
           "" :
           ", which overwrites "+oldB));
    }
  }

  private static void add(List l) {
    int n = (l == null ? 0 : l.size());
    if (n == 0) {
      return;
    }
    if (n == 1) {
      add((AddressEntry) l.get(0));
      return;
    }
    Map m = new HashMap();
    for (int i = 0; i < n; i++) {
      AddressEntry ae = (AddressEntry) l.get(i);
      String name = ae.getName();
      Map m2 = (Map) m.get(name);
      if (m2 == null) {
        m2 = new HashMap();
        m.put(name, m2);
      }
      m2.put(ae.getType(), ae);
    }
    for (Iterator iter = m.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Map m2 = (Map) me.getValue();
      Bundle b = new Bundle(
          name,
          null,
          DEFAULT_TTD,
          Collections.unmodifiableMap(m2));
      add(b);
    }
  }

  private static void add(AddressEntry ae) {
    if (ae == null) {
      return;
    }
    String name = ae.getName();
    String type = ae.getType();
    Bundle b = new Bundle(
        name,
        null,
        DEFAULT_TTD,
        Collections.singletonMap(type, ae));
    add(b);
  }

  /**
   * Parse a line of input into AddressEntries.
   * <p>
   * Lines starting with "#", "[", or "//" are ignored.
   * <p>
   * A line starting with "name=" is decoded as a compete bundle.<br>
   * Also, a line matching:<pre>
   *   NAME={DATA} 
   * </pre>
   * is parsed as a bundle:<pre>
   *   name=NAME ttd=DEFAULT_TTD entries={DATA}
   * </pre> 
   * For example:<pre> 
   *   Foo={\
   *     http=http://bar.com:8800/$Foo,\
   *     -RMI=rmi://123.45.67.89:9876/theStubId\
   *   } 
   * </pre>
   * See {@link BundleDecoder} for bundle parsing details.<br> 
   * <p>
   * Another pattern is:<pre>
   *   NAME=(AGENT@)?((TYPE[:,])?SCHEME://)?URI
   * </pre>
   * If an AGENT is specified then an entry is created:<pre>
   *   (name=NAME type=alias uri=name:///AGENT)
   * </pre>
   * <i>plus</i> an entry is created for the remainer of
   * the line:<br>
   * If TYPE and SCHEME are missing, and URI is "multicast",
   * then TYPE is left as null, SCHEME is set to "multicast",
   * and the URI is set to the DEFAULT_MULTICAST_URI.<br>
   * The default SCHEME is "rmi".</br>
   * The default TYPE is:<pre> 
   *   "-" + String.toUpperCase(SCHEME) + "_REG" 
   * </pre> 
   * The FILTER is the AGENT if specified, else "*".<br> 
   * If the SCHEME is "rmi" and the TYPE is "-RMI_REG", then
   * a ("/"+FILTER) is appended to the URI.<br>
   * If the SCHEME is "http" or "https" and the TYPE is "-HTTP"
   * or "-HTTPS" (respectively), and the URI lacks a path, then
   * the "/$"+FILTER+"/wp_bootstrap" is appended to the URI, where
   * the default FILTER is "~".
   * The line is then parsed to:<pre>
   *   (name=NAME type=TYPE uri=SCHEME://URI)
   * </pre>
   * <p>
   * For example,<pre>
   *   Foo=Bar@qux://a:1/b 
   * </pre> 
   * is parsed as two AddressEntries:<pre> 
   *   (name=Foo type=alias uri=name:///Bar) 
   *   (name=Foo type=-QUX_REG uri=qux://a:1/b) 
   * </pre> 
   * <p>
   * For backwards compatibility, the following lines are
   * reserved:<pre>
   *   hostname=HOST
   *   address=HOST
   *   port=PORT 
   *   alias=    <i>ignored</i>
   *   lpsport=  <i>ignored</i>
   * </pre>
   * HOST and PORT are saved and used at the end of the parsing to
   * create a "WP=HOST:PORT" line.<br>
   * Also, if the line ends in ":5555", then this is trimmed off,
   * since this is the ancient PSP server port.
   */
  protected static Object parse(String line) {
    if (logger.isDetailEnabled()) {
      logger.detail("parse: "+line);
    }
    if (line.startsWith("#") ||
        line.startsWith("[") ||
        line.startsWith("//")) {
      return null;
    }
    // we could save these patterns, but we'd only use them
    // at startup, so it's not worth the trouble.
    String s1 =
      "^\\s*"+
      "([^\\s=@:,/]+)="+
      "\\{"+
      "(.*)"+
      "\\}"+
      "\\s*$";
    Pattern p1 = Pattern.compile(s1);
    Matcher m1 = p1.matcher(line);
    if (m1.matches()) {
      return Bundle.decode(line);
    }
    String s2 =
        "^\\s*"+
        "([^\\s=@:,/]+)="+
        "(([^\\s=@:,/]+)@)?"+
        "("+
        "(([^\\s=@:,/]+)[:,])?"+
        "([^\\s=@:,/]+)://"+
        ")?"+
        "([^\\s]+)"+
        "\\s*$";
    Pattern p2 = Pattern.compile(s2);
    Matcher m2 = p2.matcher(line);
    if (!m2.matches()) {
      return null;
    }
    String name = m2.group(1);
    String agent = m2.group(3);
    String type = m2.group(6);
    String scheme = m2.group(7);
    String suri = m2.group(8);
    if (agent == null &&
        type == null &&
        scheme == null &&
        suri != null &&
        suri.endsWith("@") &&
        suri.matches("[^\\s=@:,/]+@")) {
      // fix bad pattern! just an agent name
      agent = suri.substring(0, suri.length()-1);
      suri = null;
    }
    AddressEntry ae2 =
      (agent == null ?
       (null) :
       AddressEntry.getAddressEntry(
         name, "alias", URI.create("name:///"+agent)));
    if (suri == null) {
      return ae2;
    }
    if ("address".equals(name) ||
        "hostname".equals(name)) {
      // save for later
      host = suri;
      if (logger.isDetailEnabled()) {
        logger.detail("host="+suri);
      }
      return null;
    }
    if ("port".equals(name)) {
      // save for later
      port = Integer.parseInt(suri);
      if (logger.isDetailEnabled()) {
        logger.detail("port="+suri);
      }
      return null;
    } 
    if ("alias".equals(name) ||
        "lpsport".equals(name)) {
      // ignore
      return null;
    }
    if (type == null && 
        scheme == null &&
        suri.endsWith(":5555")) {
      // trim off the old psp server port
      if (logger.isWarnEnabled()) {
        logger.warn("Removing trailing :5555 from "+suri);
      }
      suri = suri.substring(0, suri.length()-5); 
    }
    if (type == null &&
        scheme == null &&
        "multicast".equals(suri)) {
      scheme = suri;
      suri = DEFAULT_MULTICAST_URI;
    }
    if (scheme == null) {
      scheme = DEFAULT_SCHEME;
    }
    if (type == null) {
      type = "-"+scheme.toUpperCase()+"_REG";
    }
    if (scheme.equals("rmi") && type.equals("-RMI_REG")) {
      suri += "/" + (agent == null ? "*" : agent);
    }
    URI uri = URI.create(scheme+"://"+suri);
    if (((scheme.equals("http") && type.equals("-HTTP_REG")) ||
         (scheme.equals("https") && type.equals("-HTTPS_REG"))) &&
        (uri.getPath() == null || uri.getPath().length() == 0)) {
      suri = suri += "/$"+(agent == null ? "~" : agent)+"/wp_bootstrap";
      uri = URI.create(scheme+"://"+suri);
    }   
    AddressEntry ae = AddressEntry.getAddressEntry(
        name, type, uri);
    if (agent == null) {
      return ae;
    }
    // both AGENT@ and URI
    List l = new ArrayList(2);
    l.add(ae2);
    l.add(ae);
    return l;
  }

  private static void read_file(String filename) {
    if (logger.isDetailEnabled()) {
      logger.detail("read_file("+filename+")");
    }
    try {
      InputStream fs = ConfigFinder.getInstance().open(filename);
      if (fs == null) {
        if (logger.isDetailEnabled()) {
          logger.detail("unable to open file "+filename);
        }
        return;
      }
      if (logger.isDetailEnabled()) {
        logger.detail("reading file "+filename);
      }
      BufferedReader in = 
        new BufferedReader(new InputStreamReader(fs));
      String s = null;
      while (true) {
        String line = in.readLine();
        if (line == null) {
          break;
        }
        boolean more = line.endsWith("\\");
        if (more) {
          line = line.substring(0, line.length() - 1);
        }
        if (s == null) {
          s = line;
        } else {
          s += line;
        }
        if (more) {
          continue;
        }
        add(s);
        s = null;
      }
      if (logger.isDetailEnabled()) {
        logger.detail("closing file "+filename);
      }
      in.close();
      if (host != null && port > 0) {
        // backwards compatibility!
        if (logger.isDetailEnabled()) {
          logger.detail(
              "adding backwards compatible "+host+":"+port);
        }
        add("WP="+host+":"+port);
      }
      host = null;
      port = -1;
    } catch (FileNotFoundException fnfe) {
      if (logger.isDetailEnabled()) {
        logger.detail("file not found "+filename, fnfe);
      }
    } catch(Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error("Failed read of "+filename, e);
      }
    }
  }

  private static void read_props() {
    if (logger.isDetailEnabled()) {
      logger.detail("read_props()");
    }

    String server = SystemProperties.getProperty(SERVER_PROP);
    if (server != null) {
      if (logger.isDetailEnabled()) {
        logger.detail("adding \"WP="+server+"\" from "+SERVER_PROP);
      }
      add("WP="+server);
    }

    Properties props =
      SystemProperties.getSystemPropertiesWithPrefix(
          PROP_PREFIX);
    for (Enumeration en = props.propertyNames();
        en.hasMoreElements();
        ) {
      String key = (String) en.nextElement();
      String name = key.substring(PROP_PREFIX.length());
      String value = props.getProperty(key);
      String line = name+"="+value;
      if (logger.isDetailEnabled()) {
        logger.detail("adding \""+line+"\" from "+PROP_PREFIX+"*");
      }
      add(line);
    }

    if (host != null && port > 0) {
      // backwards compatibility!
      if (logger.isDetailEnabled()) {
        logger.detail(
            "adding backwards compatible "+host+":"+port);
      }
      add("WP="+host+":"+port);
    }
    host = null;
    port = -1;
  }
}
