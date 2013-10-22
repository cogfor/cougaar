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

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.service.DataProtectionKey;

/**
 * A media-specific persistence handler for reading and writing
 * snapshots.
 * <p> 
 * PersistencePlugin defines the API that media-specific persistence
 * plugins must implement. A persistence plugin defines a medium that
 * can be used to store a series of persistence snapshots. When an
 * agent restarts, a set of these snapshots called a rehydration set
 * is retrieved from the persistence medium to reconstitute or
 * "rehydrate" the previous state of the agent.
 */
public interface PersistencePlugin {

  /**
   * Initialize the plugin with PersistencePluginSupport and
   * parameters. After initialization, the plugin should be ready to
   * service all methods.
   * @param pps the persistence plugin support specifies the context
   * within which persistence is being performed.
   * @param name the name of this plugin.
   * @param params String parameters to configure the plugin. The
   * parameters come from configuration information and
   * interpretation is up to the plugin.
   */
  void init(PersistencePluginSupport pps, String name, String[] params, boolean deleteOldPersistence)
    throws PersistenceException;

  /**
   * Gets the name of the PersistencePlugin. Every PersistencePlugin
   * should have a distinct name. The name can be computed by the
   * plugin based on its class and parameters or it can be specified
   * as an argument in the constructor.
   */
  String getName();

  /**
   * Get the average interval between persistence snapshots for this plugin
   */
  long getPersistenceInterval();

  void setPersistenceInterval(long newInterval);

  /**
   * Is this persistence medium writable? Non-writable media are only
   * used for rehydration.
   */
  boolean isWritable();

  void setWritable(boolean newDisable);

  /**
   * Get the number of incremental snapshots between full snapshots.
   */
  int getConsolidationPeriod();

  void setConsolidationPeriod(int newPeriod);

  /**
   * Get the number of parameters for this plugin.
   * @return the number of parameters
   */
  int getParamCount();

  /**
   * Get a specific plugin parameter.
   * @param i the index of the desired parameter. Must be between 0
   * (inclusive) and the value returned by
   * {@link #getParamCount getParamCount} (exclusive).
   * @return the value of the specified parameter.
   */
  String getParam(int i);

  /**
   * Gets the names of all media-specific controls. The names of
   * these controls must not conflict with the
   * (@link BasePersistence#getMediaControlNames names that all
   * media plugins have}.
   * @return an array of the names of the controls for this media
   * plugin.
   */
  String[] getControlNames();

  /**
   * Gets the list of allowed ranges for values of the named
   * control. Values supplied to {@link #setControl} are guaranteed
   * to be in the specified ranges.
   * @return the list or allowed ranges.
   */
  OMCRangeList getControlValues(String controlName);

  /**
   * Set value of a particular control. Values are guaranteed to be
   * in the ranges specified by {@link #getControlValues}
   * @param controlName the name of the control
   * @param newValue the new value of the control
   */
  void setControl(String controlName, Comparable newValue);

  /**
   * Read the specified set of sequence numbers. These numbers
   * should identify a complete set of persistence deltas needed to
   * restore the specified state. A specific archive may be
   * specified using the suffix argument.
   * @return an array of possible rehydration sets. The timestamp of
   * each indicates how recent each rehydration set is.
   * @param suffix identifies which set of persistence deltas are
   * wanted. A non-empty suffix specifies an specific, archived
   * state. An empty suffix specifies all available sets.
   */
  SequenceNumbers[] readSequenceNumbers(String suffix);

  /**
   * Cleanup old deltas as specified by cleanupNumbers. These deltas
   * are <em>never</em> part of the current state. When archiving is
   * enabled, the old deltas constituting an archive are not
   * discarded.
   * @param cleanupNumbers the numbers to be discarded (or archived).
   */
  void cleanupOldDeltas(SequenceNumbers cleanupNumbers);

  /**
   * Delete old archives
   */
  void cleanupArchive();

  /**
   * Open an OutputStream onto which a persistence delta can be
   * written. The stream returned should be relatively non-blocking
   * since it is possible for the entire agent to be blocked waiting
   * for completion. Implementations that may block indefinitely
   * should perform buffering as needed. Also, the OutputStream
   * should be unique relative to other instances of the same agent
   * @param deltaNumber the number of the delta that will be
   * written. Numbers are never re-used so this number can be used
   * to uniquely identify the delta.
   * @param full indicates that the information to be written is a
   * complete state dump and does not depend on any earlier deltas.
   * It may be useful to distinctively mark such deltas.
   */
  OutputStream openOutputStream(int deltaNumber, boolean full)
    throws IOException;

  /**
   * Clean up after output was aborted.
   * Called in response to an exception during the writing of the
   * current stream.
   * @param retainNumbers the numbers of the deltas excluding the
   * one just written that comprise a complete rehydration set.
   * Subsequent calls to readSequenceNumbers should return these
   * values.
   */
  void abortOutputStream(SequenceNumbers retainNumbers);

  /**
   * Clean up after closing the output stream. This method is called
   * within a mutual exclusion semaphore such that multiple
   * instances of the same agent cannot both be calling this or
   * related methods. This is the opportunity to rename the output
   * stream to its real identity.
   * @param retainNumbers the numbers of the deltas including the
   * one just written that comprise a complete rehydration set.
   * Subsequent calls to readSequenceNumbers should return these
   * values.
   */
  void finishOutputStream(SequenceNumbers retainNumbers, boolean full);

  /**
   * Open an InputStream from which a persistence delta can be
   * read.
   * @param deltaNumber the number of the delta to be opened
   */
  InputStream openInputStream(int deltaNumber)
    throws IOException;

  /**
   * Clean up after closing the input stream
   * @param deltaNumber the number of the delta being closed.
   * Provided as a convenience to the method
   */
  void finishInputStream(int deltaNumber);

  /**
   * Get the connection to the database into which persistence
   * deltas are being written for coordinated transaction
   * management. Non-database implementations should throw an
   * UnsupportedOperationException
   */
  java.sql.Connection getDatabaseConnection(Object locker) throws UnsupportedOperationException;

  /**
   * Release the connection to the database into which persistence
   * deltas are being written for coordinated transaction
   * management. Non-database implementations should throw an
   * UnsupportedOperationException
   */
  void releaseDatabaseConnection(Object locker) throws UnsupportedOperationException;

  /**
   * Store an encrypted key for a particular delta number
   * @param deltaNumber the number of the delta for which the key is used.
   * @param key has the encrypted key to be stored
   */
  void storeDataProtectionKey(int deltaNumber, DataProtectionKey key)
    throws IOException;

  /**
   * Retrieve an encrypted key for a particular delta number
   * @param deltaNumber the number of the delta for which the key is used.
   */
  DataProtectionKey retrieveDataProtectionKey(int deltaNumber)
    throws IOException;

  /**
   * Check that this agent instance still owns the persistence data
   */
  boolean checkOwnership() throws PersistenceException;

  /**
   * Lock out other instances of this agent.
   */
  void lockOwnership() throws PersistenceException;

  /**
   * Release the lockout of other instances of this agent.
   */
  void unlockOwnership() throws PersistenceException;
}
