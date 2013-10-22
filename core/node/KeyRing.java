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

package org.cougaar.core.node;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.HashMap;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.util.ConfigFinder;

/**
 * A container for security keystore information and functionality.
 *
 * @property org.cougaar.install.path
 * Used to find keystore as "org.cougaar.install.path/configs/common/.keystore"
 *
 * @property org.cougaar.security.keystore.password
 * The password to the cougaar keystore.
 *
 * @property org.cougaar.security.keystore
 * The URL of the cougaar keystore.
 */
public final class KeyRing {
  private static String ksPass;
  private static String ksPath;
  
  private static KeyStore keystore = null;

  static {
    String installpath = SystemProperties.getProperty("org.cougaar.install.path");
    String defaultKeystorePath = installpath + File.separatorChar
                                + "configs" + File.separatorChar + "common"
                                + File.separatorChar + ".keystore";

    ksPass = SystemProperties.getProperty("org.cougaar.security.keystore.password","alpalp");
    ksPath = SystemProperties.getProperty("org.cougaar.security.keystore", defaultKeystorePath);

    System.out.println("Secure message keystore: path=" + ksPath + ", pass=" + ksPass);
  }
  
  private static void init() {
    try {
      keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream kss = ConfigFinder.getInstance().open(ksPath);
      keystore.load(kss, ksPass.toCharArray());
      kss.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static Object guard = new Object();
  
  public static KeyStore getKeyStore() { 
    synchronized (guard) {
      if (keystore == null) 
        init();
      return keystore; 
    }
  }

  private static HashMap privateKeys = new HashMap(89);
  static PrivateKey getPrivateKey(String name) {
    PrivateKey pk = null;
    try {
      synchronized (privateKeys) {
        pk = (PrivateKey) privateKeys.get(name);
        if (pk == null) {
          pk = (PrivateKey) getKeyStore().getKey(name, ksPass.toCharArray());
          privateKeys.put(name, pk);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to get PrivateKey for \""+name+"\": "+e);
      e.printStackTrace();
    }
    return pk;
  }

  private static HashMap certs = new HashMap(89);

  static java.security.cert.Certificate getCert(String name) {
    java.security.cert.Certificate cert = null;
    try {
      synchronized (certs) {
        cert = (java.security.cert.Certificate) certs.get(name);
        if (cert == null) {
          cert = getKeyStore().getCertificate(name);
          certs.put(name, cert);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to get Certificate for \""+name+"\": "+e);
    }
    return cert;
  }
}
