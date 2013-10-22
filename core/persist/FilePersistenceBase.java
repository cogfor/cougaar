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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.util.log.Logger;

/**
 * This {@link PersistencePlugin} abstract base class saves and
 * restores blackboard objects in files. The actual opening of the
 * input and output streams remains abstract.
 * <p>
 * There is one optional parameter naming the persistence root
 * directory. If the parameter is omitted, the persistence root is
 * specified by system properties.
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
public abstract class FilePersistenceBase
  extends PersistencePluginAdapter
  implements PersistencePlugin
{
  private static final String NEWSEQUENCE = "newSequence";
  private static final String SEQUENCE = "sequence";
  private static final String MUTEX = "mutex";
  private static final String OWNER = "owner";
  private static final long MUTEX_TIMEOUT = 60000L;

  private static File getDefaultPersistenceRoot(String name) {
    String installPath = SystemProperties.getProperty("org.cougaar.install.path", "/tmp");
    File workspaceDirectory =
      new File(SystemProperties.getProperty("org.cougaar.workspace", installPath + "/workspace"));
    return new File(workspaceDirectory,
                    SystemProperties.getProperty("org.cougaar.core.persistence.path", name));
  }

  private File persistenceDirectory;
  private File persistenceRoot;
  private File ownerFile;
  private String instanceId;
  private FileMutex mutex;
  private int deltaNumber;      // The number of the currently open output file.

  @Override
protected void handleParameter(String param) {
    String value;
    if ((value = parseParamValue(param, PERSISTENCE_ROOT_PREFIX)) != null) {
      persistenceRoot = new File(value);
    } else {
      if (pps.getLogger().isWarnEnabled()) {
        pps.getLogger().warn(name + ": Unrecognized parameter " + param);
      }
    }
  }

  public void init(PersistencePluginSupport pps,
                   String name,
                   String[] params,
                   boolean deleteOldPersistence)
    throws PersistenceException
  {
    // Special case for old-style nameless first parameter (means persistenceRoot=<param>)
    if (params.length == 1 && params[0].indexOf('=') < 0) {
      params[0] = PERSISTENCE_ROOT_PREFIX + params[0];
    }
    init(pps, name, params);
    if (persistenceRoot == null) {
      persistenceRoot = getDefaultPersistenceRoot(name);
    }
    persistenceRoot.mkdirs();
    if (!persistenceRoot.isDirectory()) {
      pps.getLogger().fatal("Not a directory: " + persistenceRoot);
      throw new PersistenceException("Persistence root unavailable");
    }
    String agentName = pps.getMessageAddress().getAddress();
    persistenceDirectory = new File(persistenceRoot, agentName);
    if (!persistenceDirectory.isDirectory()) {
      if (!persistenceDirectory.mkdirs()) {
        String msg = "FilePersistence(" + name + ") not a directory: " + persistenceDirectory;
	pps.getLogger().fatal(msg);
	throw new PersistenceException(msg);
      }
    }
    if (deleteOldPersistence) deleteOldPersistence();
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    instanceId = format.format(new Date());
    mutex = new FileMutex(persistenceDirectory, MUTEX, MUTEX_TIMEOUT);
    lockOwnership();
    try {
      ownerFile = new File(persistenceDirectory, OWNER);
      DataOutputStream o = new DataOutputStream(new FileOutputStream(ownerFile));
      o.writeUTF(instanceId);
      o.close();
    } catch (IOException ioe) {
      pps.getLogger().fatal("assertOwnership exception", ioe);
      throw new PersistenceException("assertOwnership exception", ioe);
    } finally {    
      unlockOwnership();
    }
  }

  @Override
public boolean checkOwnership() throws PersistenceException {
    lockOwnership();
    try {
      DataInputStream i = new DataInputStream(new FileInputStream(ownerFile));
      return i.readUTF().equals(instanceId);
    } catch (IOException ioe) {
      throw new PersistenceException("checkOwnership exception", ioe);
    } finally {
      unlockOwnership();
    }
  }

  @Override
public void lockOwnership() throws PersistenceException {
    try {
      mutex.lock();
    } catch (IOException ioe) {
      throw new PersistenceException("lockOwnership exception", ioe);
    }
  }

  @Override
public void unlockOwnership() throws PersistenceException {
    try {
      mutex.unlock();
    } catch (SecurityException ioe) {
      throw new PersistenceException("unlockOwnership exception", ioe);
    }
  }

  protected abstract InputStream openFileInputStream(File file) throws FileNotFoundException;

  protected abstract OutputStream openFileOutputStream(File file) throws FileNotFoundException;

  protected abstract boolean rename(File from, File to);

  private File getSequenceFile(String suffix) {
    return new File(persistenceDirectory, SEQUENCE + suffix);
  }

  private File getNewSequenceFile(String suffix) {
    return new File(persistenceDirectory, NEWSEQUENCE + suffix);
  }

  private SequenceNumbers readSequenceFile(File sequenceFile) throws IOException {
    Logger ls = pps.getLogger();
    if (ls.isInfoEnabled()) {
      ls.info("Reading " + sequenceFile);
    }
    DataInputStream sequenceStream = new DataInputStream(openFileInputStream(sequenceFile));
    try {
      int first = sequenceStream.readInt();
      int last = sequenceStream.readInt();
      long timestamp = sequenceFile.lastModified();
      return new SequenceNumbers(first, last, timestamp);
    }
    finally {
      sequenceStream.close();
    }
  }

  public SequenceNumbers[] readSequenceNumbers(final String suffix) {
    FilenameFilter filter;
    if (suffix.equals("")) {
      filter = new FilenameFilter() {
        public boolean accept(File dir, String path) {
          return (path.startsWith(NEWSEQUENCE) || path.startsWith(SEQUENCE));
        }
      };
    } else {
      filter = new FilenameFilter() {
        public boolean accept(File dir, String path) {
          return (path.endsWith(suffix)
                  && (path.startsWith(NEWSEQUENCE) || path.startsWith(SEQUENCE)));
        }
      };
    }
    String[] names = persistenceDirectory.list(filter);
    List result = new ArrayList(names.length);
    File sequenceFile;
    for (int i = 0; i < names.length; i++) {
      if (names[i].startsWith(NEWSEQUENCE)) {
        File newSequenceFile = new File(persistenceDirectory, names[i]);
        sequenceFile = new File(persistenceDirectory,
                                SEQUENCE + names[i].substring(NEWSEQUENCE.length()));
        rename(newSequenceFile, sequenceFile);
      } else {
        sequenceFile = new File(persistenceDirectory, names[i]);
      }
      try {
        result.add(readSequenceFile(sequenceFile));
      } catch (IOException e) {
	pps.getLogger().error("Error reading " + sequenceFile, e);
      }
    }
    return (SequenceNumbers[]) result.toArray(new SequenceNumbers[result.size()]);
  }

  private void writeSequenceNumbers(SequenceNumbers sequenceNumbers, String suffix) {
    try {
      File sequenceFile = getSequenceFile(suffix);
      File newSequenceFile = getNewSequenceFile(suffix);
      DataOutputStream sequenceStream =
        new DataOutputStream(openFileOutputStream(newSequenceFile));
      try {
	sequenceStream.writeInt(sequenceNumbers.first);
	sequenceStream.writeInt(sequenceNumbers.current);
      }
      finally {
        sequenceStream.close();
	sequenceFile.delete();
	if (!rename(newSequenceFile, sequenceFile)) {
	  pps.getLogger().error("Failed to rename " + newSequenceFile + " to " + sequenceFile);
	}
      }
    }
    catch (Exception e) {
      pps.getLogger().error("Exception writing sequenceFile", e);
    }
  }

  public void cleanupOldDeltas(SequenceNumbers cleanupNumbers) {
    Logger ls = pps.getLogger();
    for (int deltaNumber = cleanupNumbers.first; deltaNumber < cleanupNumbers.current; deltaNumber++) {
      File deltaFile = getDeltaFile(deltaNumber);
      if (deltaFile.delete()) {
        if (ls.isInfoEnabled()) ls.info("Deleted " + deltaFile);
      } else {
	ls.error("Failed to delete " + deltaFile);
      }
    }
  }

  public void cleanupArchive() {
    Logger ls = pps.getLogger();
    if (archiveCount < Integer.MAX_VALUE) {
      FilenameFilter filter =
        new FilenameFilter() {
          public boolean accept(File dir, String path) {
            return path.startsWith(SEQUENCE) && !path.equals(SEQUENCE);
          }
        };
      String[] names = persistenceDirectory.list(filter);
      int excess = names.length - archiveCount;
      if (ls.isInfoEnabled()) {
        ls.info(excess + " excess archives to delete");
      }
      if (excess > 0) {
        Arrays.sort(names);
        for (int i = 0; i < excess; i++) {
          File sequenceFile = new File(persistenceDirectory, names[i]);
          if (ls.isInfoEnabled()) ls.info("Deleting " + sequenceFile);
          try {
            SequenceNumbers sn = readSequenceFile(sequenceFile);
            cleanupOldDeltas(sn);
            sequenceFile.delete();
          } catch (IOException ioe) {
            ls.error("cleanupArchive", ioe);
          }
        }
      }
    }
  }

  public OutputStream openOutputStream(int deltaNumber, boolean full) throws IOException {
    File tempFile = getTempFile(deltaNumber);
    Logger ls = pps.getLogger();
    this.deltaNumber = deltaNumber;
    if (ls.isInfoEnabled()) {
      ls.info("Persist to " + tempFile);
    }
    return openFileOutputStream(tempFile);
  }

  public void finishOutputStream(SequenceNumbers retainNumbers,
                                boolean full)
  {
    File tempFile = getTempFile(deltaNumber);
    File deltaFile = getDeltaFile(deltaNumber);
    tempFile.renameTo(deltaFile);
    writeSequenceNumbers(retainNumbers, "");
    if (full) writeSequenceNumbers(retainNumbers,
                                   PersistenceServiceComponent.formatDeltaNumber(retainNumbers.first));
  }

  public void abortOutputStream(SequenceNumbers retainNumbers)
  {
    getTempFile(retainNumbers.current).delete();
  }

  public InputStream openInputStream(int deltaNumber) throws IOException {
    File deltaFile = getDeltaFile(deltaNumber);
    Logger ls = pps.getLogger();
    if (ls.isInfoEnabled()) {
      ls.info("rehydrate " + deltaFile);
    }
    return openFileInputStream(deltaFile);
  }

  public void finishInputStream(int deltaNumber) {
  }

  private void deleteOldPersistence() {
    File[] files = persistenceDirectory.listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
  }

  public void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException
  {
    File file = getEncryptedKeyFile(deltaNumber);
    ObjectOutputStream ois = new ObjectOutputStream(openFileOutputStream(file));
    try {
      ois.writeObject(key);
    } finally {
      ois.close();
    }
  }

  public DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException
  {
    File file = getEncryptedKeyFile(deltaNumber);
    ObjectInputStream ois = new ObjectInputStream(openFileInputStream(file));
    try {
      return (DataProtectionKey) ois.readObject();
    } catch (ClassNotFoundException cnfe) {
      IOException ioe = new IOException("Read DataProtectionKey failed");
      ioe.initCause(cnfe);
      throw ioe;
    } finally {
      ois.close();
    }
  }

  private File getTempFile(int sequence) {
    return new File(persistenceDirectory,
                    instanceId + "_" + PersistenceServiceComponent.formatDeltaNumber(sequence));
  }

  private File getDeltaFile(int sequence) {
    return new File(persistenceDirectory,
                    "delta" + PersistenceServiceComponent.formatDeltaNumber(sequence));
  }

  private File getEncryptedKeyFile(int sequence) {
    return new File(persistenceDirectory,
                    "key" + PersistenceServiceComponent.formatDeltaNumber(sequence));
  }
}
