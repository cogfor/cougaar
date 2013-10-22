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

package org.cougaar.core.blackboard;


/**
 * The creation time and most recent modification time for a
 * {@link org.cougaar.core.util.UniqueObject} on the blackboard.
 * <p>
 * This class is immutable.
 * 
 * @see TimestampSubscription
 */
public final class TimestampEntry implements java.io.Serializable {

  /**
   * The "unknown time" is used when the time was not 
   * recorded.
   */
  public static final long UNKNOWN_TIME = -1;

  private long creationTime;
  private final long lastModTime;

  public TimestampEntry(long creationTime, long lastModTime) {
    this.creationTime = creationTime;
    this.lastModTime = lastModTime;
  }

  /**
   * Get the creation time.
   */
  public long getCreationTime() { return creationTime; }

  /**
   * Get the most recent modification time, or the creation
   * time if the object has never been modified.
   */
  public long getModificationTime() { return lastModTime; }

  /**
   * Package-private modifier for the creation time -- for
   * infrastructure use only!.
   * <p>
   * This is only used before the instance is released
   * to the clients, after which the instance is no longer
   * modified and presents an immutable API.
   */
  void private_setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  @Override
public String toString() {
    return "("+creationTime+" + "+(lastModTime-creationTime)+")";
  }

  private static final long serialVersionUID = -1209831829038126385L;
}
