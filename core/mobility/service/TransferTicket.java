/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.persist.PersistenceInputStream;
import org.cougaar.core.persist.PersistenceOutputStream;

/**
 * Package-private ticket to transfer an agent between nodes.
 */
final class TransferTicket 
extends AbstractTicket {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final MoveTicket moveTicket;
  private final ComponentDescription desc;
  private Object state;

  public TransferTicket(
      MoveTicket moveTicket,
      ComponentDescription desc,
      Object state) {
    this.moveTicket = moveTicket;
    this.desc = desc;
    this.state = state;
  }

  public MoveTicket getMoveTicket() {
    return moveTicket;
  }

  public ComponentDescription getComponentDescription() {
    return desc;
  }

  public Object getState() {
    return state;
  }

  public void clearState() {
    // force GC
    state = null;
  }

  private void writeObject(
      ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    if (stream instanceof PersistenceOutputStream) {
      // don't persist state!
    } else {
      stream.writeObject(state);
    }
  }
  private void readObject(ObjectInputStream stream) 
    throws ClassNotFoundException, IOException {
      stream.defaultReadObject();
      if (stream instanceof PersistenceInputStream) {
	// don't try reading state
      } else {
        state = stream.readObject();
      }
    }

  @Override
public int hashCode() {
    return moveTicket.hashCode();
  }

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof TransferTicket)) {
      return false;
    } else {
      TransferTicket t = (TransferTicket) o;
      return moveTicket.equals(t.moveTicket);
    }
  }
  
  @Override
public String toString() {
    return "Node-to-Node transfer of "+moveTicket;
  }
}
