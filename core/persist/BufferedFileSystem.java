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
import java.util.ArrayList;
import java.util.List;

import org.cougaar.util.CircularQueue;
import org.cougaar.util.log.Logger;

/**
 * Support for buffered and otherwise queued access to the
 * file system. Primarily, this serializes access to writing and
 * renaming and reading files. All file write and rename actions
 * return immediately without waiting for the actual filesystem
 * operations to complete. File reads invariably block until prior
 * writes and renames have concluded.
 */
public class BufferedFileSystem implements Runnable {
  private static final int BUFSIZE=100000;
  private static final int MAXBUFFERS=100; // 10 Mbytes max
  private static final int MAXKEPTBUFFERS=20; // 2 Mbytes max

  private static List buffers = new ArrayList();
  private static int totalBuffers = 0;

  /**
   * Get a buffer for writing. This is a bit complicated to avoid
   * excessive allocation activity and excessive consumption of memory
   * for this purpose. Up to MAXKEPTBUFFERS are allocated and reused
   * freely. If demand exceeds MAXKEPTBUFFERS, additional buffers are
   * allocated up to MAXBUFFERS. Demand in excess of MAXBUFFERS blocks.
   */
  private static byte[] getBuffer() {
    synchronized (buffers) {
      while (totalBuffers >= MAXBUFFERS) {
        try {
          buffers.wait();
        } catch (InterruptedException ie) {
        }
      }
      int len = buffers.size();
      if (len > 0) {
        return (byte[]) buffers.remove(len - 1);
      }
      totalBuffers++;
      return new byte[BUFSIZE];
    }
  }

  private static void releaseBuffer(byte[] buf) {
    synchronized (buffers) {
      if (buffers.size() < MAXKEPTBUFFERS) {
        buffers.add(buf);
      } else {
        totalBuffers--;
      }
      buffers.notifyAll();
    }
  }

  private Logger logger;

  private Thread thread;

  private boolean active;

  private CircularQueue queue = new CircularQueue();
  private boolean executingJob = false;

  /**
   * Wrap a FileOutputStream to provide safe close semantics.
   * Explicitly sync the file descriptor on close() to insure the file
   * has been completely written to the disk.
   */
  private class BufferedFileOutputStream extends OutputStream {
    // The real OutputStream
    private FileOutputStream fileOutputStream;
    private byte[] buffer;
    private int nbytes;
    public BufferedFileOutputStream(FileOutputStream stream) {
      fileOutputStream = stream;
      newBuffer();
    }

    private void newBuffer() {
      buffer = getBuffer();
      nbytes = 0;
    }

    private void switchBuffer(final int nbytes) {
      enqueueJob(new Runnable() {
          byte[] buf = buffer;
          public void run() {
            try {
              fileOutputStream.write(buf, 0, nbytes);
            } catch (IOException ioe) {
              throw new BufferedFileException(ioe);
            } finally {
              releaseBuffer(buf);
            }
          }
          @Override
         public String toString() {
            return "Write " + nbytes;
          }
        });
      newBuffer();
    }
    
    @Override
   public void write(int b) throws IOException {
      buffer[nbytes++] = (byte) b;
      if (nbytes == BUFSIZE) switchBuffer(nbytes);
    }

    @Override
   public void write(byte[] b, int offset, int nb) throws IOException {
      while (nb > 0) {
        int tnb = Math.min(nb, BUFSIZE - nbytes);
        System.arraycopy(b, offset, buffer, nbytes, tnb);
        nbytes += tnb;
        if (nbytes == BUFSIZE) switchBuffer(nbytes);
        offset += tnb;
        nb -= tnb;
      }
    }

    @Override
   public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
    }

    @Override
   public void flush() throws IOException {
      if (nbytes > 0) switchBuffer(nbytes);
    }

    @Override
   public void close() throws IOException {
      flush();
      enqueueJob(new Runnable() {
          public void run() {
            try {
              fileOutputStream.flush();
              fileOutputStream.getFD().sync();
              fileOutputStream.close();
            } catch (IOException ioe) {
              throw new BufferedFileException(ioe);
            }
          }
          @Override
         public String toString() {
            return "close";
          }
        });
      if (logger.isInfoEnabled()) logger.info("Buffered closed");
    }
  }

  private static class BufferedFileException extends RuntimeException {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public BufferedFileException(IOException t) {
      super("Buffered IOException", t);
    }
  }

  public BufferedFileSystem(Logger ls) {
    logger = ls;
  }

  private void enqueueJob(Runnable job) {
    synchronized (queue) {
      queue.add(job);
      if (thread == null) {
        active = true;
        thread = new Thread(this, "BufferedFileSystem");
        thread.start();
      }
    }
  }

  public void run() {
    while (true) {
      Runnable job;
      synchronized (queue) {
        while (queue.size() == 0) {
          try {
            queue.wait();
          } catch (InterruptedException ie) {
          }
          if (!active) return;
        }
        job = (Runnable) queue.next();
        executingJob = true;
      }
      if (logger.isInfoEnabled()) logger.info("Buffered job " + job);
      try {
        job.run();
      } catch (Throwable bfe) {
        logger.error(bfe.getMessage(), bfe.getCause());
      }
      synchronized (queue) {
        executingJob = false;
        queue.notifyAll();
      }
    }
  }

  public void waitForPrevious() {
    synchronized (queue) {
      while (queue.size() > 0 || executingJob) {
        try {
          queue.wait();
        } catch (InterruptedException ie) {
        }
      }
    }
  }

  public void stop() {
    synchronized (queue) {
      active = false;
      queue.notify();
      try {
        thread.join();
      } catch (InterruptedException ie) {
      }
    }
  }

  public OutputStream openOutputStream(File file) throws FileNotFoundException {
    return new BufferedFileOutputStream(new FileOutputStream(file));
  }

  public InputStream openInputStream(File file) throws FileNotFoundException {
    waitForPrevious();
    return new FileInputStream(file);
  }

  public boolean rename(final File from, final File to) {
    enqueueJob(new Runnable() {
        public void run() {
          from.renameTo(to);
        }
        @Override
      public String toString() {
          return "rename " + from + " to " + to;
        }
      });
    return true;
  }
}
