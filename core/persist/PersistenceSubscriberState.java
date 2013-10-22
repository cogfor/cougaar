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

import java.util.List;

import org.cougaar.core.blackboard.Subscriber;

/**
 * Persistence state for a blackboard {@link Subscriber}.
 */
public class PersistenceSubscriberState implements java.io.Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
public String clientName;	// The name of the client of the subscriber
  public String subscriberName;		// The name of the subscriber
  public List pendingEnvelopes;
  public List transactionEnvelopes;

  public PersistenceSubscriberState(Subscriber subscriber) {
    clientName = subscriber.getClient().getBlackboardClientName();
    subscriberName = subscriber.getName();
    if (subscriber.shouldBePersisted()) {
      this.pendingEnvelopes = subscriber.getPendingEnvelopes();
      this.transactionEnvelopes = subscriber.getTransactionEnvelopes();
    }
  }

  public boolean isSameSubscriberAs(Subscriber subscriber) {
    if (subscriber.getClient().getBlackboardClientName().equals(clientName) &&
	subscriber.getName().equals(subscriberName)) {
      return true;
    }
    return false;
  }

  public String getKey() {
    return clientName + "." + subscriberName;
  }

  @Override
public String toString() {
    return super.toString() + " " + getKey();
  }
}
