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
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.thread.SchedulableStatus;
import org.cougaar.core.wp.bootstrap.Bundle;
import org.cougaar.core.wp.bootstrap.ConfigService;
import org.cougaar.core.wp.bootstrap.DiscoveryBase;

/**
 * This component discovers bundles by sending a UDP multicast and
 * listening for UDP replies. 
 */
public class MulticastDiscovery
extends DiscoveryBase
{
  private ConfigService configService;

  private final ConfigService.Client configClient =
    new ConfigService.Client() {
      public void add(Bundle b) {
        addPoller(getBootEntry(b));
      }
      public void change(Bundle b) {
        add(b);
      }
      public void remove(Bundle b) {
        removePoller(getBootEntry(b));
      }
    };

  @Override
protected String getConfigPrefix() {
    return "org.cougaar.core.wp.resolver.multicast.";
  }

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
    return MulticastUtil.getBootEntry(b);
  }

  @Override
protected Map lookup(Object bootObj) {
    throw new InternalError("should use MulticastPoller!");
  }

  @Override
protected Poller createPoller(Object bootObj) {
    return new MulticastPoller(bootObj);
  }

  private class MulticastPoller extends Poller {

    private static final int LISTEN_TIMEOUT = 60000;
    private static final int PACKET_SIZE = 2048;

    private final AddressEntry bootEntry;

    private MulticastSocket sendSoc;

    private final Object lock = new Object();
    private boolean pleaseStop;
    private boolean running;
    private String listenHost;
    private int listenPort;

    private final Schedulable listenThread;

    public MulticastPoller(Object bootObj) {
      super(bootObj);

      bootEntry = (AddressEntry) bootObj;

      Runnable listenRunner = new Runnable() {
        public void run() {
          runListener();
        }
      };
      listenThread = threadService.getThread(
          MulticastDiscovery.this, 
          listenRunner, 
          "White pages bootstrap reply listener for "+bootObj,
          ThreadService.WILL_BLOCK_LANE);
    }

    @Override
   public void start() {
      ds.update(null);
      synchronized (lock) {
        if (running) {
          return;
        }
        pleaseStop = false;
      }
      listenThread.start();
    }

    @Override
   public void stop() {
      if (sendSoc != null) {
        try {
          sendSoc.close();
        } catch (Exception e) {
          if (log.isWarnEnabled()) {
            log.warn("Unable to close "+sendSoc+" socket", e);
          }
        }
      }

      synchronized (lock) {
        if (!running) {
          return;
        }
        pleaseStop = true;
      }
    }

    @Override
   public void doLookup() {
      // no lookup in this thread, but we send our ARP here.
      // The reply will arrive in our listenThread.
      sendQuery();
    }

    // our listenThread
    private void runListener() {
      if (log.isDebugEnabled()) {
        log.debug("Starting listener "+bootEntry);
      }

      // create reply socket
      DatagramSocket listenSoc;
      String host;
      int port;
      try {
        listenSoc = new DatagramSocket();
        if (LISTEN_TIMEOUT > 0) {
          listenSoc.setSoTimeout(LISTEN_TIMEOUT);
        }
        host = listenSoc.getLocalAddress().getHostAddress();
        port = listenSoc.getLocalPort();
        if ("0.0.0.0".equals(host)) {
          host = InetAddress.getLocalHost().getHostAddress();
        }
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Unable to open listener socket", e);
        }
        return;
      }

      // let our send thread see the reply-to address
      if (log.isInfoEnabled()) {
        log.info(
            "Listening on "+host+":"+port+" for replies to "+bootObj);
      }
      synchronized (lock) {
        running = true;
        listenHost = host;
        listenPort = port;
      }

      // listen for replies
      DatagramPacket packet =
        new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
      while (true) {
        synchronized (lock) {
          if (pleaseStop) {
            break;
          }
        }
        try {
          try {
            SchedulableStatus.beginNetIO("UDP listen");
            listenSoc.receive(packet);
          } finally {
            SchedulableStatus.endBlocking();
          }
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Exception on receive to "+host+":"+port, e);
          }
          continue;
        }
        Map bundles = readReply(
            packet.getData(), 0, packet.getLength());
        if (bundles == null || bundles.isEmpty()) {
          continue;
        }
        for (Iterator iter = bundles.values().iterator();
            iter.hasNext();
            ) {
          Bundle b = (Bundle) iter.next();
          ds.add(b.getName(), b);
        }
      }

      // shutdown
      try {
        listenSoc.close();
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Unable to close "+listenSoc+" socket", e);
        }
      }
      synchronized (lock) {
        running = false;
        pleaseStop = false;
        listenHost = null;
        listenPort = 0;
      }

      if (log.isDebugEnabled()) {
        log.debug("Stopped listener "+bootEntry);
      }
    }

    private Map readReply(byte[] bytes, int offset, int length) {
      //(Cougaar-RARP from=HOST:PORT bundles=\n
      //name=X ..\n
      //name=Y ..\n
      //..\n
      //)

      if (log.isInfoEnabled()) {
        URI uri = bootEntry.getURI();
        log.info(
            "Reading reply from "+uri.getHost()+":"+uri.getPort()+
            " "+new String(bytes, offset, length));
      }

      InputStream is = new ByteArrayInputStream(
          bytes, offset, length);

      // parse bundles
      Map newFound = null; 
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
          "Cougaar-RARP"+
          "\\s+"+
          "from=([^\\s:]+):(\\d+)"+
          "\\s+"+
          "bundles="+
          "\\s*"+
          "$";
        Pattern p = Pattern.compile(sp);
        Matcher m = p.matcher(header);
        if (!m.matches()) {
          if (log.isErrorEnabled()) {
            log.error("Invalid wp-reply header: "+header);
          }
          return null;
        }
        String host = m.group(1);
        String sport = m.group(2);
        int port = Integer.parseInt(sport);

        // record this host:port?
        if (log.isInfoEnabled()) {
          URI uri = bootEntry.getURI();
          log.info(
              "Reply to "+uri.getHost()+":"+uri.getPort()+
              " is from "+host+":"+port);
        }

        newFound = Bundle.decodeAll(br);

        br.close();
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Unable to parse reply", e);
        }
        return null;
      }

      return newFound;
    }

    private void sendQuery() {
      // create socket
      if (sendSoc == null || sendSoc.isClosed()) {
        try {
          sendSoc = new MulticastSocket();
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.info("Unable to open multicast socket", e);
          }
          return;
        }
      }

      // get reply address
      String replyHost;
      int replyPort;
      synchronized (lock) {
        if (!running) {
          return;
        }
        replyHost = listenHost;
        replyPort = listenPort;
      }

      // create message
      String msg =
        "(Cougaar-ARP reply-to="+replyHost+":"+replyPort+")";

      URI uri = bootEntry.getURI();
      String host = uri.getHost();
      int port = uri.getPort();

      if (log.isInfoEnabled()) {
        log.info("Sending "+msg+" to "+host+":"+port);
      }

      // send multicast
      try {
        byte[] b = msg.getBytes();
        InetAddress ia = InetAddress.getByName(host);
        DatagramPacket packet =
          new DatagramPacket(b, b.length, ia, port);
        sendSoc.send(packet);
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Multicast Cougaar-ARP to "+host+":"+port+" failed", e);
        }
      }

      // our reply will arrive in the listenThread!
    }
  }
}
