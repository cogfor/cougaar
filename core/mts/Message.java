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

package org.cougaar.core.mts;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * A message is an object sent from one agent (the "originator")
 * to another agent (the "target") through the {@link
 * org.cougaar.core.service.MessageTransportService}.
 */
public abstract class Message 
  implements Serializable 
{

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

private static final MessageAddress sink = MessageAddress.NULL_SYNC;

  // sender:
  private MessageAddress theOriginator;
  // target:
  private MessageAddress theTarget;
  // optional sequence number:
  private int theSequenceNumber = 0;

  /**
   * Default Constructor for factory.
   */
  public Message() {
    this( sink, sink, 0 );
  }

  /**
   * Standard message contructor, which specifies the source and
   * target agent addresses.
   * <p>
   * @param aSource creator of this message
   * @param aTarget target for this message
   */
  public Message(MessageAddress aSource, MessageAddress aTarget) {
    this(aSource, aTarget, 0);
  }

  /**
   * Message constructor with sequence number. 
   *
   * @param aSource creator of this message
   * @param aTarget target for this message
   * @param anId Sequence number
   */
  public Message(MessageAddress aSource, MessageAddress aTarget, int anId) {
    setOriginator(aSource);
    setTarget(aTarget);
    setContentsId(anId);
  }
    
  /**
   * Copy constructor. 
   *
   * @param aMessage The message to use as the data source for construction.
   */
  public Message(Message aMessage) {
    this(aMessage.getOriginator(),
         aMessage.getTarget(),
	 aMessage.getContentsId());
  }

  /** @return the sequence number */
  public final int getContentsId() {
    return theSequenceNumber;
  }

  /** @return the sender */
  public final MessageAddress getOriginator() { return theOriginator; }
 
  /** @return the destination */
  public final MessageAddress getTarget() { return theTarget; }

  /** Set the sequence number */
  public final void setContentsId(int aContentsId) {
    theSequenceNumber = aContentsId;
  }

  /** Set the sender */
  public final void setOriginator(MessageAddress aSource) { theOriginator = aSource; }

  /** Set the destination */
  public final void setTarget(MessageAddress aTarget) { theTarget = aTarget; }

  @Override
public String toString() {
    try {
      return "The source: " + getOriginator().toString() +
        " The Target: " + getTarget().toString() +
        " The Message Id: " + getContentsId();
    } catch (NullPointerException npe) {
      String output = "a Malformed Message: ";
      if ( getOriginator() != null )
        output += " The source: " + getOriginator().toString();
      else
        output += " The source: NULL";
      if ( getTarget() != null )
        output += "The Target: " + getTarget().toString();
      else  
        output += " The Target: NULL";

      return output;
    }
  }

  // externalizable support
  // we don't actually implement the Externalizable interface, so it is
  // up to subclasses to call these methods.
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(theOriginator);
    out.writeObject(theTarget);
    out.writeInt(theSequenceNumber);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    theOriginator=(MessageAddress)in.readObject();
    theTarget=(MessageAddress)in.readObject();
    theSequenceNumber = in.readInt();
  }
}

