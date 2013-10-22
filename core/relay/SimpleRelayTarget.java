/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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

package org.cougaar.core.relay;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * Target-side implementation of {@link SimpleRelay}, constructed
 * by the {@link SimpleRelaySource}.
 * <p>
 * @see SimpleRelaySource for implementation notes 
 */
public final class SimpleRelayTarget 
extends SimpleRelayBase
implements Relay.Target {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
// constructor:
  public SimpleRelayTarget(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      Object query) {
    super(uid, source, target);
    this.query = query;
  }

  // prevent "setQuery", since this is the target-side instance.
  // Only "updateContent" from the source can change the content.
  @Override
public void setQuery(Object reply) {
    throw new UnsupportedOperationException(
        "Unable to modify the query on the target-side, "+
        "it can only be set at the source ("+source+")");
  }

  // implement Relay.Target:
  public Object getResponse() {
    return reply;
  }
  public int updateContent(Object content, Token token) {
    // assert content != null
    this.query = content;
    return Relay.CONTENT_CHANGE;
  }
}
