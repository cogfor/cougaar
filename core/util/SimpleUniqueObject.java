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

package org.cougaar.core.util;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A {@link UniqueObject} that records the allocation stack and
 * complains if the {@link UID} is reset.
 */
public abstract class SimpleUniqueObject implements UniqueObject {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;

/** The UID of the object */
  protected UID uid;

  /**
   * DEBUGGING
   * @deprecated Should be turned off
   */
  private transient Throwable allocationContext;

  protected SimpleUniqueObject() {
    allocationContext = new Throwable("Allocation context");
  }

  /**
   * @return the UID of a UniqueObject.  If the object was created
   * correctly (e.g. via a Factory), will be non-null.
   */
  public UID getUID() {
    if (uid == null) {
      uidError("uid was never set");
    }
    return uid;
  }

  /**
   * Set the UID of a UniqueObject.  This should only be done by
   * a domain factory.  Will throw a RuntimeException if
   * the UID was already set.
   */
  public void setUID(UID newUID) {
    if (uid != null && !uid.equals(newUID)) {
      uidError("uid already set");
    }
    allocationContext = new Throwable("setUID context");
    uid = newUID;
  }

  private void uidError(String reason) {
    if (allocationContext != null) {
      allocationContext.printStackTrace();
    } else {
      System.err.println("UniqueObject deserialized");
    }
    throw new RuntimeException(reason + ": " + this);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    if (uid == null) {
      uidError("writeObject with no uid");
    }
    stream.defaultWriteObject();
  }
}
