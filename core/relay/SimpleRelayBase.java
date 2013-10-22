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
 * Abstract base class for {@link SimpleRelay} implementations.
 */
public abstract class SimpleRelayBase 
implements SimpleRelay {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
protected final UID uid;
  protected final MessageAddress source;
  protected final MessageAddress target;

  protected Object query;
  protected Object reply;

  public SimpleRelayBase(
      UID uid,
      MessageAddress source,
      MessageAddress target) {
    this.uid = uid;
    this.source = source;
    this.target = target;
  }

  // SimpleRelay:

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  public MessageAddress getSource() {
    return source;
  }

  public MessageAddress getTarget() {
    return target;
  }

  public Object getQuery() {
    return query;
  }

  public void setQuery(Object query) {
    this.query = query;
  }

  public Object getReply() {
    return reply;
  }

  public void setReply(Object reply) {
    this.reply = reply;
  }

  // Object:

  @Override
public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof SimpleRelay) { 
      UID u = ((SimpleRelay) o).getUID();
      return uid.equals(u);
    } else {
      return false;
    }
  }
  @Override
public int hashCode() {
    return uid.hashCode();
  }
  @Override
public String toString() {
    return 
      "(SimpleRelay"+
      " uid="+uid+
      " source="+source+
      " target="+target+
      " query="+query+
      " reply="+reply+
      ")";
  }
}
