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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Cert;
import org.cougaar.core.util.UID;

import sun.misc.BASE64Encoder;

/**
 * Encode {@link Bundle}s and {@link Cert}s to encoded text.
 *
 * @see BundleDecoder
 */
public final class BundleEncoder {

  private static final String BEGIN_CERT =
    "-----BEGIN CERTIFICATE-----\\\n";
  private static final String END_CERT =
    "\\\n-----END CERTIFICATE-----";

  private BundleEncoder() {}

  public static String encodeBundle(Bundle b) {
    return encodeBundle(b, true);
  }

  public static String encodeBundle(Bundle b, boolean indent) {
    StringBuffer buf = new StringBuffer();
    Map entries = b.getEntries();
    if (entries == null) {
      entries = Collections.EMPTY_MAP;
    }
    buf.append(b.getName()).append("={");
    UID uid = b.getUID();
    long ttd = b.getTTD();
    boolean needsComma = false;
    int n = entries.size();
    Iterator iter = entries.entrySet().iterator();
    for (int i = -2; i < n; i++) {
      String type;
      AddressEntry ae = null;
      if (i == -2) {
        if (uid == null) {
          continue;
        }
        type = "uid";
      } else if (i == -1) {
        if (ttd < 0) {
          continue;
        }
        type = "ttd";
      } else {
        Map.Entry me = (Map.Entry) iter.next();
        type = (String) me.getKey();
        ae = (AddressEntry) me.getValue();
      }

      if (needsComma) {
        buf.append(",");
        if (!indent) {
          buf.append(" ");
        }
      }
      if (indent) {
        buf.append("\\\n  ");
      }
      buf.append(type).append("=");

      if (i == -2) {
        buf.append(uid);
      } else if (i == -1) {
        buf.append(ttd);
      } else {
        Cert cert = ae.getCert();
        boolean null_cert =
          (cert == null || Cert.NULL.equals(cert));
        if (!null_cert) {
          buf.append("(uri=");
        }
        buf.append(ae.getURI());
        if (!null_cert) {
          buf.append(" cert=");
          String scert = encodeCert(cert);
          buf.append(scert);
          buf.append(")");
        }
      }

      needsComma = true;
    }
    if (indent) { buf.append("\\\n"); }
    buf.append("}");
    return buf.toString();
  }

  public static String encodeCert(Cert cert) {
    if (cert == null || cert.equals(Cert.NULL)) {
      return "NULL";
    }
    if (cert.equals(Cert.PROXY)) {
      return "PROXY";
    }
    try {
      if (cert instanceof Cert.Indirect) {
        String q = ((Cert.Indirect) cert).getQuery();
        String v = URLEncoder.encode(q, "UTF-8");
        return "Indirect:"+v;
      }
      if (cert instanceof Cert.Direct) {
        Certificate c = ((Cert.Direct) cert).getCertificate();
        byte[] ba = c.getEncoded();
        String v = (new UUEncoder()).encode(ba);
        if (v.startsWith(BEGIN_CERT)) {
          v = v.substring(BEGIN_CERT.length());
        }
        if (v.endsWith(END_CERT)) {
          v = v.substring(0, v.length() - END_CERT.length());
        }
        return "Direct:\\\n"+v;
      }
      // a custom cert type -- serialize it!
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(cert);
      oos.flush();
      byte[] ba = baos.toByteArray();
      String v = (new UUEncoder()).encode(ba);
      return "Object:\\\n"+v;
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to encodeCert("+cert+")", e);
    }
  }

  private static class UUEncoder extends BASE64Encoder {
    @Override
   protected void encodeLineSuffix(
        OutputStream aStream) throws IOException {
      pStream.println("\\");
    }
  }
}
