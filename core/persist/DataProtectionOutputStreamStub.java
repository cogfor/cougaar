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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.cougaar.core.service.DataProtectionKey;

/**
 * A trivial {@link OutputStream} for testing the {@link
 * org.cougaar.core.service.DataProtectionService}.
 *
 * @see DataProtectionInputStreamStub
 */
public class DataProtectionOutputStreamStub extends FilterOutputStream {
  private DataProtectionKeyStub keyStub;
  // Our own buffer so we don't pollute the caller's buffers.
  private byte[] buf = new byte[8192];
  public DataProtectionOutputStreamStub(OutputStream s, DataProtectionKey keyStub) {
    super(s);
    this.keyStub = (DataProtectionKeyStub) keyStub;
  }

  @Override
public void write(int b) throws IOException {
    byte bb = (byte) b;
    bb ^= keyStub.xor;
    out.write(bb);
  }

  @Override
public void write(byte[] b, int offset, int nb) throws IOException {
    byte xor = keyStub.xor;
    while (nb > 0) {
      int tnb = Math.min(nb, buf.length);
      for (int i = 0; i < tnb; i++) {
        buf[i] = (byte) (b[offset + i] ^ xor);
      }
      out.write(buf, 0, tnb);
      offset += tnb;
      nb -= tnb;
    }
  }

  @Override
public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
public String toString() {
    return "DataProtectionOutputStreamStub key " + keyStub;
  }
}
