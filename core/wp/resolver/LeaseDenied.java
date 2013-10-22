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

package org.cougaar.core.wp.resolver;

import java.io.Serializable;

import org.cougaar.core.util.UID;

/**
 * A "lease denied" response from the {@link ClientTransport}'s
 * {@link ModifyService}, indicating to the {@link LeaseManager}
 * a failed bind or lease renewal.
 * <p>
 * The UID will match the UID of the Record that has been denied.
 * <p>
 * Currently this can only be caused by server-side deconfliction
 * over race conditions, primarily based upon agent incarnation
 * numbers.
 * <p>
 * For example, say AgentX moves from NodeA to NodeB.
 * The following binds may be in progress:<pre>
 *   NodeA sends:
 *     AgentX={..., version=version:///1234/5678, ...}
 *   NodeB sends:
 *     AgentX={..., version=version:///1234/9999, ...}
 * </pre>
 * The format of the version entry URI is:<pre>
 *   version:///<i>incarnation</i>/<i>moveId</i>
 * </pre>
 * where the incarnation number is incremented per restart
 * (excluding moves) and the moveId is incremented per move
 * or restart (i.e. every time the agent is loaded).
 * The white pages servers will prefer the latest entries,
 * so it will deny NodeA's lease request.
 * <p>
 * Currently (see {@link Record}) this doesn't support bind-only
 * failures due to "already bound" entries.  The javadocs in
 * Record describe a proposed Map of failed-binds.
 */
public final class LeaseDenied implements Serializable {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
private final UID uid;
  private final Object reason;
  private final Object data;

  public LeaseDenied(
      UID uid,
      Object reason,
      Object data) {
    this.uid = uid;
    this.reason = reason;
    this.data = data;
    // validate
    String s =
      ((uid == null) ? "null uid" :
       (reason == null) ? "null reason" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The UID of the lease, as selected by the Record.
   * <p>
   * This is the "in response to" field.
   */
  public UID getUID() {
    return uid;
  }

  /**
   * The reason(s) for the failure.
   * <p>
   * This may indicate an "already bound" message for failed
   * "bind" requests, or some other failure condition.
   */
  public Object getReason() {
    return reason;
  }

  /**
   * The optional Record data.
   */
  public Object getData() {
    return data;
  }

  @Override
public String toString() {
    return
      "(lease-denied uid="+uid+
      " reason="+reason+
      " data="+data+")";
  }
}
