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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Bootstrap utility methods.
 */
public final class Util {

  private Util() {}

  public static final MessageAddress parseServer(Bundle b) {
    // either (name=X .. server=..)
    // or (name=WP-123 .. alias=name://X)
    String agent = null;
    if (b != null) {
      Map entries = b.getEntries();
      if (entries != null) {
        if (entries.containsKey("server")) {
          agent = b.getName();
        } else {
          Object o = entries.get("alias");
          if (o instanceof AddressEntry) {
            String name = b.getName();
            if (name != null && name.matches("WP(-\\d+)?")) {
              AddressEntry ae = (AddressEntry) o;
              agent = ae.getURI().getPath().substring(1);
            }
          }
        }
      }
    }
    if (agent == null) {
      return null;
    }
    return MessageAddress.getMessageAddress(agent);
  }

  /** Generic utility method to check if a host is localhost */
  public static final boolean isLocalHost(String addr) {
    Logger logger = Logging.getLogger(Util.class.getName());

    // quick test for localhost...
    if (addr.equals("localhost") ||
        addr.equals("127.0.0.1") ||
        addr.equals("localHost") // bogus
       ) {
      if (logger.isDetailEnabled()) {
        logger.detail(
            "isLocalHost("+addr+") is true");
      }
      return true;
    }
    try {
      InetAddress de = InetAddress.getByName(addr);
      // quick test for "getLocalHost"
      InetAddress lh = InetAddress.getLocalHost();
      if (logger.isDetailEnabled()) {
        logger.detail(
            "isLocalHost("+addr+
            "), getByName("+addr+")="+de+
            ", getLocalHost()="+lh+
            ", equal="+(lh.equals(de)));
      }
      if (lh.equals(de)) {
        return true;
      }
      // check all network interfaces
      for (Enumeration e1 = NetworkInterface.getNetworkInterfaces();
          e1.hasMoreElements();
          ) {
        NetworkInterface iface = (NetworkInterface) e1.nextElement();
        for (Enumeration e2 = iface.getInetAddresses();
            e2.hasMoreElements();
            ) {
          InetAddress me = (InetAddress) e2.nextElement();
          if (logger.isDetailEnabled()) {
            logger.detail(
                "isLocalHost("+addr+
                "), getByName("+addr+")="+de+
                ", network_interface[?]="+me+
                ", equal="+(me.equals(de)));
          }
          if (me.equals(de)) {
            return true;
          }
        }
      }
    } catch (UnknownHostException e) {
      if (logger.isInfoEnabled()) {
        logger.info("Unknown host "+addr);
      }
    } catch (SocketException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("isLocalHost("+addr+") failed", e);
      }
    }
    return false;
  }
}
