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

package org.cougaar.core.wp.resolver;

import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.WhitePagesMessage;

/**
 * A message from a white pages cache to a server, or between
 * servers.
 */
public final class WPQuery extends WhitePagesMessage {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
public static final int LOOKUP  = 0;
  public static final int MODIFY  = 1;
  public static final int FORWARD = 2;
  public static final int PING = 3;

  private final long sendTime;
  private final int action;
  private final Map m;

  public WPQuery(
      MessageAddress source,
      MessageAddress target,
      long sendTime,
      int action,
      Map m) {
    super(source, target);
    this.sendTime = sendTime;
    this.action = action;
    this.m = m;
    // validate
    String s =
      ((sendTime < 0) ? "invalid send time: "+sendTime :
       (m == null && action != PING) ? "null map" :
       (action != LOOKUP &&
        action != MODIFY &&
        action != FORWARD &&
        action != PING) ? "invalid action: "+action : 
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The time on the client's clock when this was sent.
   */
  public long getSendTime() {
    return sendTime;
  }

  /**
   * @return the action of request
   */ 
  public int getAction() {
    return action;
  }

  /**
   * The content of this message.
   */
  public Map getMap() {
    return m;
  }

  @Override
public String toString() {
    long now = System.currentTimeMillis();
    return toString(now);
  }

  public String toString(long now) {
    return 
      "("+
      (action == LOOKUP ? "lookup" :
       action == MODIFY ? "modify" :
       action == FORWARD ? "forward" :
       "ping")+
      " from="+getOriginator()+
      " to="+getTarget()+
      " sent="+Timestamp.toString(sendTime, now)+
      " "+m+")";
  }
}
