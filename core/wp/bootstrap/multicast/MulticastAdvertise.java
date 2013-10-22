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

package org.cougaar.core.wp.bootstrap.multicast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.core.wp.bootstrap.AdvertiseBase;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;

/**
 * This component advertises bundles by listening for UDP multicast
 * requests and replying by UDP. 
 * <p> 
 * It looks in the {@link ConfigService} for config entries of type
 * "-MULTICAST_REG" and scheme "multicast", or "-MCAST_REG" and
 * "mcast", e.g.<pre>
 *   X={-MULTICAST_REG=multicast://224.22.165.34:7777}
 * </pre>
 * and creates a UDP multicast listener for that group:port and
 * replies to<pre>
 *   (Cougaar-ARP reply-to=HOST:PORT)
 * </pre>
 * requests with  text-encoded bundles tracked by the {@link
 * org.cougaar.core.wp.bootstrap.AdvertiseService} (i.e. locally bound
 * leases), e.g.<pre>
 *   (Cougaar-RARP from=test.com:9876 bundles:\
 *     A={-RMI=rmi://host:port/objId,\
 *   }\
 *   #)
 * </pre>
 * <p>
 * Note that there's currently no filtering of the local leases.
 */
public class MulticastAdvertise
extends AdvertiseBase
{

  private ConfigService configService;

  private final ConfigService.Client configClient =
    new ConfigService.Client() {
      public void add(Bundle b) {
        addAdvertiser(getBootEntry(b));
      }
      public void change(Bundle b) {
        add(b);
      }
      public void remove(Bundle b) {
        removeAdvertiser(getBootEntry(b));
      }
    };

  @Override
public void load() {
    super.load();

    configService = sb.getService(configClient, ConfigService.class, null);
    if (configService == null) {
      throw new RuntimeException("Unable to obtain ConfigService");
    }
  }

  @Override
public void unload() {
    if (configService != null) {
      sb.releaseService(configClient, ConfigService.class, configService);
      configService = null;
    }

    super.unload();
  }

  protected AddressEntry getBootEntry(Bundle b) {
    AddressEntry entry = MulticastUtil.getBootEntry(b);
    if (entry == null) {
      return null;
    }
    // assume we're it?
    return entry;
  }

  @Override
protected Advertiser createAdvertiser(Object bootObj) {
    return new MulticastAdvertiser(bootObj);
  }

  private class MulticastAdvertiser extends Advertiser {

    private static final int LISTEN_TIMEOUT = 60000;
    private static final int PACKET_SIZE = 512;

    private final AddressEntry bootEntry;

    private final Schedulable listenThread;

    private final Object lock = new Object();
    private boolean pleaseStop;
    private boolean running;

    public MulticastAdvertiser(Object bootObj) {
      super(bootObj);

      bootEntry = (AddressEntry) bootObj;

      Runnable listenRunner = new Runnable() {
        public void run() {
          runListener();
        }
      };
      listenThread = threadService.getThread(
          MulticastAdvertise.this, 
          listenRunner, 
          "White pages bootstrap query listener for "+
          bootObj,
          ThreadService.WILL_BLOCK_LANE);
    }

    @Override
   public void start() {
      synchronized (lock) {
        if (running) {
          return;
        }
        pleaseStop = false;
      }
      listenThread.start();
    }

    @Override
   public void update(String name, Bundle bundle) {
      // do nothing, since we server bundles (as opposed to posting
      // them at external server)
    }

    @Override
   public void stop() {
      synchronized (lock) {
        if (!running) {
          return;
        }
        pleaseStop = true;
      }
    }

    // our listenThread
    private void runListener() {
      if (log.isDebugEnabled()) {
        log.debug("Starting listener "+bootEntry);
      }

      URI uri = bootEntry.getURI();
      String listenHost = uri.getHost();
      int listenPort = uri.getPort();

      InetAddress listenAddr;
      try {
        listenAddr = InetAddress.getByName(listenHost);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Unable to getByName("+listenHost+")", e);
        }
        return;
      }

      // create multicast listener socket
      MulticastSocket listenSoc;
      try {
        listenSoc = new MulticastSocket(listenPort);
        if (LISTEN_TIMEOUT > 0) {
          listenSoc.setSoTimeout(LISTEN_TIMEOUT);
        }
        listenSoc.joinGroup(listenAddr);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error(
              "Unable to create multicast socket on "+uri,
              e);
        }
        return;
      }

      // create datagram reply socket
      DatagramSocket replySoc;
      InetAddress replyAddr;
      int replyPort;
      try {
        replySoc = new DatagramSocket();
        replyAddr = replySoc.getLocalAddress();
        replyPort = replySoc.getLocalPort();
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Unable to create reply socket", e);
        }
        try {
          listenSoc.leaveGroup(listenAddr);
          listenSoc.close();
        } catch (Exception e2) {
          if (log.isWarnEnabled()) {
            log.warn("Unable to close "+listenSoc+" socket", e2);
          }
        }
        return;
      }

      // let our send thread see the reply-to address
      if (log.isInfoEnabled()) {
        log.info(
            "Listening on "+listenHost+":"+listenPort+
            " for multicast queries, will reply on "+
            replyAddr+":"+replyPort);
      }
      synchronized (lock) {
        running = true;
      }

      // listen for queries
      DatagramPacket packet = new DatagramPacket(
          new byte[PACKET_SIZE], PACKET_SIZE);
      while (true) {
        synchronized (lock) {
          if (pleaseStop) {
            break;
          }
        }
        try {
          try {
            SchedulableStatus.beginNetIO("Multicast listen");
            listenSoc.receive(packet);
          } finally {
            SchedulableStatus.endBlocking();
          }
          String replyTo = readQuery(
                packet.getData(), 0, packet.getLength());
          if (replyTo == null) {
            continue;
          }
          sendReply(replySoc, replyTo);
        } catch (SocketTimeoutException ste) {
          if (log.isDebugEnabled()) {
            log.debug("Ignore socket timeout, keep listening...");
          }
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Failed query receive", e);
          }
        }
      }

      // shutdown
      try {
        listenSoc.leaveGroup(listenAddr);
        listenSoc.close();
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Unable to close "+listenSoc+" socket", e);
        }
      }
      synchronized (lock) {
        running = false;
        pleaseStop = false;
      }

      if (log.isDebugEnabled()) {
        log.debug("Stopped listener "+bootEntry);
      }
    }

    private String readQuery(byte[] bytes, int offset, int length) {
      //(Cougaar-ARP reply-to=HOST:PORT)

      if (log.isInfoEnabled()) {
        log.info(
            "Reading query to "+bootEntry.getURI()+": "+
            new String(bytes, offset, length));
      }

      InputStream is = new ByteArrayInputStream(
          bytes, offset, length);

      // parse query
      try {
        BufferedReader br = 
          new BufferedReader(new InputStreamReader(is));
        String header = br.readLine();
        if (header == null) {
          if (log.isErrorEnabled()) {
            log.error("Missing header");
          }
          return null;
        }
        String sp =
          "^\\s*"+
          "\\("+
          "\\s*"+
          "Cougaar-ARP"+
          "\\s+"+
          "reply-to=([^\\s:]+):(\\d+)"+
          "\\s*"+
          "\\)"+
          "\\s*"+
          "$";
        Pattern p = Pattern.compile(sp);
        Matcher m = p.matcher(header);
        if (!m.matches()) {
          if (log.isErrorEnabled()) {
            log.error("Invalid wp-query header: "+header);
          }
          return null;
        }
        String host = m.group(1);
        String sport = m.group(2);
        int port = Integer.parseInt(sport);

        return host+":"+port;
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("readQuery failed", e);
        }
      }

      return null;
    }

    private void sendReply(DatagramSocket replySoc, String replyTo) {
      int sep = replyTo.indexOf(':');
      String host = replyTo.substring(0, sep);
      String sport = replyTo.substring(sep+1);
      int port = Integer.parseInt(sport);
      
      if (host.equals("0.0.0.0")) {
        if (log.isInfoEnabled()) {
          log.info(
              "Ignoring request from invalid reply-to="+
              host+":"+port);
        }
        return;
      }
      // assume that the return address is valid!

      String msg = getReply();

      if (log.isInfoEnabled()) {
        log.info("Sending reply to "+replyTo+": "+msg);
      }

      try {
        byte[] b = msg.getBytes();
        InetAddress ia = InetAddress.getByName(host);
        DatagramPacket packet =
          new DatagramPacket(b, b.length, ia, port);
        replySoc.send(packet);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Failed reply to "+replyTo, e);
        }
      }
    }

    private String getReply() {
      Map bundles = getBundles();

      StringBuffer buf = new StringBuffer();
      buf.append("(Cougaar-RARP from=");
      URI uri = bootEntry.getURI();
      buf.append(uri.getHost());
      buf.append(":");
      buf.append(uri.getPort());
      buf.append(" bundles=\n");
      if (bundles != null) {
        for (Iterator iter = bundles.values().iterator();
            iter.hasNext();
            ) {
          Bundle b = (Bundle) iter.next();
          String s = b.encode();
          if (s == null) {
            continue;
          }
          buf.append(s).append("\n");
        }
      }
      buf.append("#)");
      String msg = buf.toString();
      return msg;
    }

    private Map getBundles() {
      // serve remote caller
      Map ret = MulticastAdvertise.this.getBundles();
      // filter?
      if (log.isDebugEnabled()) {
        log.debug("Serving bundles: "+ret);
      }
      return ret;
    }

  }
}
