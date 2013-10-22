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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.cougaar.core.service.DataProtectionKey;

/**
 * A {@link PersistencePlugin} that does nothing.
 * <p>
 * Persistence clients can check the {@link
 * Persistence#isDummyPersistence} method to avoid the (minimal)
 * overhead imposed by this implementation.
 * <p>
 * Running with this persistence implementation incurs most
 * of the work of doing persistence, but discards the results.
 * This is often used for performance testing.
 */
public class DummyPersistence
  extends PersistencePluginAdapter
  implements PersistencePlugin
{

  public void init(PersistencePluginSupport pps, String name, String[] params, boolean deleteOldPersistence)
    throws PersistenceException
  {
    init(pps, name, params);
  }

  @Override
protected void handleParameter(String param) {
    // Ignore params
  }

  @Override
public boolean isWritable() {
    return true;                // Cannot be disabled.
  }

  public SequenceNumbers[] readSequenceNumbers(String suffix) {
    return new SequenceNumbers[0];
  }

  public void writeSequenceNumbers(SequenceNumbers sequenceNumbers) {
  }

  public void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
  }

  public void cleanupArchive() {
  }

  public OutputStream openOutputStream(int deltaNumber, boolean full)
    throws IOException
  {
    return null;                // Null means don't write output
  }

  public void finishOutputStream(SequenceNumbers retain,
                                 boolean full)
  {
  }

  public void abortOutputStream(SequenceNumbers retain) {
  }

  public InputStream openInputStream(int deltaNumber) throws IOException {
    throw new IOException("No dummy input");
  }

  public void finishInputStream(int deltaNumber) {
  }

  public void deleteOldPersistence() {
  }

  public void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException
  {
  }

  public DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException
  {
    return null;
  }

  @Override
  public boolean checkOwnership()
        throws PersistenceException {
     return true;
  }

  @Override
  public void lockOwnership()
        throws PersistenceException {
  }

  @Override
  public void unlockOwnership()
        throws PersistenceException {
  }
}
