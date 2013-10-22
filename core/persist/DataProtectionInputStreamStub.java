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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cougaar.core.service.DataProtectionKey;

/**
 * A trivial {@link InputStream} for testing the {@link
 * org.cougaar.core.service.DataProtectionService}.
 *
 * @see DataProtectionOutputStreamStub
 */
public class DataProtectionInputStreamStub extends FilterInputStream {
  private DataProtectionKeyStub keyStub;
  // Our own buffer so we don't pollute the callers buffers.
  private byte[] buf = new byte[8192];

  public DataProtectionInputStreamStub(InputStream s, DataProtectionKey keyStub) {
    super(s);
    this.keyStub = (DataProtectionKeyStub) keyStub;
  }

  @Override
public int read() throws IOException {
    int b = super.read();
    if (b < 0) return b;
    return (b ^ keyStub.xor) & 0xff;
  }

  @Override
public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
public int read(byte[] b, int offset, int nb) throws IOException {
    int total = -1;
    byte xor = keyStub.xor;
    while (nb > 0) {
      int tnb = Math.min(nb, buf.length);
      tnb = super.read(buf, 0, tnb);
      if (tnb < 0) break;
      for (int i = 0; i < tnb; i++) {
        b[offset + i] = (byte) (buf[i] ^ xor);
      }
      offset += tnb;
      nb -= tnb;
      if (total < 0) {
        total = tnb;
      } else {
        total += tnb;
      }
    }
    return total;
  }

  @Override
public String toString() {
    return "DataProtectionInputStreamStub key " + keyStub;
  }
}
