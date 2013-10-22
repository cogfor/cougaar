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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This {@link PersistencePlugin} saves and restores blackboard
 * objects in files. There is one optional parameter naming the persistence
 * root directory. If the parameter is omitted, the persistence root
 * is specified by system properties.
 *
 * @property org.cougaar.install.path
 * Used by FilePersistence as the
 * parent directory for persistence snapshots when there is no
 * directory specified in configuration parameters and
 * org.cougaar.core.persistence.path is a relative pathname. This
 * property is not used if the plugin is configured with a specific
 * parameter specifying the location of the persistence root.
 *
 * @property org.cougaar.core.persistence.path
 * Specifies the directory
 * in which persistence snapshots should be saved. If this is a
 * relative path, it the base will be the value or
 * org.cougaar.install.path. This property is not used if the plugin
 * is configured with a specific parameter specifying the location of
 * the persistence root.
 */
public class FilePersistence
  extends FilePersistenceBase
{
  /**
   * Wrap a FileOutputStream to prove safe close semantics. Explicitly
   * sync the file descriptor on close() to insure the file has been
   * completely written to the disk.
   */
  private static class SafeFileOutputStream extends OutputStream {
    private FileOutputStream fileOutputStream;

    public SafeFileOutputStream(File file) throws FileNotFoundException {
      fileOutputStream = new FileOutputStream(file);
    }

    @Override
   public void write(int b) throws IOException {
      fileOutputStream.write(b);
    }

    @Override
   public void write(byte[] b)  throws IOException {
      fileOutputStream.write(b, 0, b.length);
    }

    @Override
   public void write(byte[] b, int offset, int nbytes) throws IOException {
      fileOutputStream.write(b, offset, nbytes);
    }

    @Override
   public void flush() throws IOException {
      fileOutputStream.flush();
    }

    @Override
   public void close() throws IOException {
      fileOutputStream.flush();
      fileOutputStream.getFD().sync();
      fileOutputStream.close();
    }
  }

  @Override
protected OutputStream openFileOutputStream(File file) throws FileNotFoundException {
    return new SafeFileOutputStream(file);
  }

  @Override
protected InputStream openFileInputStream(File file) throws FileNotFoundException {
    return new FileInputStream(file);
  }

  @Override
protected boolean rename(File from, File to) {
    return from.renameTo(to);
  }
}
