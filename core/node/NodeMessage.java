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

package org.cougaar.core.node;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/**
 * @deprecated unused {@link Message} base for nodes.
 */
public class NodeMessage 
  extends Message
{
  //
  // Unlike ClusterMessage there is no IncarnationNumber.  This might
  // be required in the future for reliable Node communication.
  // 

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

/**
   * Constructor
   * <p>
   * @param s The MessageAddress of creator node 
   * @param d The MessageAddress of the target node
   */
  public NodeMessage(MessageAddress s, MessageAddress d) {
    super(s, d);
  }

  /** 
   * no-arg Constructor.
   * This is not generally allowed in 1.1 event handling because 
   * EventObject requires a source object during construction.  Base 
   * class does not support this type of construction so it cannot 
   * be done here.
   */
  public NodeMessage() {
    super();
  }

  /**
   *  We provide the translation from the object version.  Unfortunately 
   * we cannot return a different type in java method overloading so the 
   * method signature is changed.  Mark it final to allow the compilier 
   * to inline optimize the function.
   * @return MessageAddress Identifies the originator of this directive
   */
  public final MessageAddress getSource(){
    return getOriginator();
  }

  /**
   * We provide the translation from the Object version in Message to the 
   * Type sepecific version for the Node messageing subsystem.
   * Mark it final to allow the compilier to inline optimize the function.
   * @return MessageAddress Identifies the reciever of the directive
   */
  public final MessageAddress getDestination() {
    return getTarget();
  }

  /**
   * Source is stored as na object so that message can service all objects.
   * Mark it final to allow the compilier to inline optimize the function.
   * @param asource - Set the MessageAddress of the originator of this message
   */
  public final void setSource(MessageAddress asource) {
    setOriginator( asource );
  }

  /**
   * Target is stored as na object so that message can service all objects.
   * Mark it final to allow the compilier to inline optimize the function.
   * @param adestination - Set the MessageAddress of the receiver of this message
   */
  public final void setDestination(MessageAddress adestination) {
    setTarget(adestination);
  }

  @Override
public String toString() {
    return "<NodeMessage "+getSource()+" - "+getDestination()+">";
  }
}
