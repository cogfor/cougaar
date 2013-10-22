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

package org.cougaar.core.persist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A file lock.
 */
public class FileMutex {
  private static final SimpleDateFormat uniqueNameFormat;
  static {
    uniqueNameFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
  }

  private File uniqueFile;
  private File commonFile;
  private long timeout;

  public FileMutex(File directory, String commonName, long timeout) {
    this.timeout = timeout;
    commonFile = new File(directory, commonName);
    uniqueFile = new File(directory, uniqueNameFormat.format(new Date()));
  }

  public void lock() throws IOException {
    long endTime = System.currentTimeMillis() + timeout;
    new FileOutputStream(uniqueFile).close(); // No content needed
    while (!uniqueFile.renameTo(commonFile)) {
      if (System.currentTimeMillis() > endTime) {
        unlock();               // Unlock the mutex for the
                                // (apparently dead) other instance
        continue;               // Rename should now work
      }
      try {
        Thread.sleep(5000);     // Wait for the other instance to
                                // unlock
      } catch (InterruptedException ie) {
      }
    }
  }

  public void unlock() {
    commonFile.delete();
  }

  public static void main(String[] args) {
    String TMP = "/tmp";
    String SEQ = TMP + "/seq";
    String PREFIX = TMP + "/";
    String COMMON = "filemutexlock";
    String SUFFIX = args[0];
    DecimalFormat format = new DecimalFormat("00000");
    FileMutex fm = new FileMutex(new File(TMP), COMMON, 30000L);
    try {
      for (int i = 0; i < 1000; i++) {
        fm.lock();
        int seq;
        try {
          DataInputStream r = new DataInputStream(new FileInputStream(SEQ));
          try {
            seq = r.readInt();
          } finally {
            r.close();
          }
        } catch (IOException ioe) {
          seq = 0;
        }
        String fileName = PREFIX + format.format(seq) + SUFFIX;
        new FileOutputStream(fileName).close();
        DataOutputStream o = new DataOutputStream(new FileOutputStream(SEQ));
        seq++;
        o.writeInt(seq);
        o.close();
        fm.unlock();
        Thread.sleep(100);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
