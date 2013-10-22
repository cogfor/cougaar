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

import org.cougaar.core.mts.MessageAddress;

/**
 * A {@link UniqueObject} with an agent "owner" field and allocation
 * stack context.
 */
public abstract class OwnedUniqueObject extends SimpleUniqueObject {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;

protected MessageAddress owner;

  private transient Throwable allocationContext;

  protected OwnedUniqueObject() {
    allocationContext = new Throwable("Allocation context");
  }

  public boolean isFrom(MessageAddress where) {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return where.equals(owner);
  }

  public void setOwner(MessageAddress newOwner) {
    allocationContext = new Throwable("Allocation context");
    owner = newOwner;
  }

  public MessageAddress getOwner() {
    if (owner == null) {
      ownerError("owner was never set");
    }
    return owner;
  }

  /** alias for getOwner */
  public MessageAddress getSource() {
    return getOwner();
  }

  private void ownerError(String reason) {
    if (allocationContext != null) {
      allocationContext.printStackTrace();
    } else {
      System.err.println("OwnedUniqueObject deserialized");
    }
    throw new RuntimeException(reason + ": " + this);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    if (owner == null) {
      ownerError("writeObject with no owner");
    }
    stream.defaultWriteObject();
  }
}

