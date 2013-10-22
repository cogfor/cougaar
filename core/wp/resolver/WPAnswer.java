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
 * A message from a white pages server to a client, or between
 * servers.
 */
public final class WPAnswer extends WhitePagesMessage {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;
public static final int LOOKUP  = 0;
  public static final int MODIFY  = 1;
  public static final int FORWARD = 2;
  public static final int PING = 3;

  private final long sendTime;
  private final long replyTime;
  private final boolean useServerTime;
  private final int action;
  private final Map m;

  public WPAnswer(
      MessageAddress source,
      MessageAddress target,
      long sendTime,
      long replyTime,
      boolean useServerTime,
      int action,
      Map m) {
    super(source, target);
    this.sendTime = sendTime;
    this.replyTime = replyTime;
    this.useServerTime = useServerTime;
    this.action = action;
    this.m = m;
    // validate
    String s =
      ((sendTime <= 0) ? "invalid send time: "+sendTime :
       (replyTime <= 0) ? "invalid reply time: "+replyTime :
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
   * The time on the client's clock when the query was sent.
   */
  public long getSendTime() {
    return sendTime;
  }

  /**
   * The time on the server's clock when this response was sent.
   */
  public long getReplyTime() {
    return replyTime;
  }

  /**
   * If true, then all timestamp offsets are relative to the server's
   * reply time, otherwise they should be based upon the client's
   * send time plus half the client's measured round-trip-time.
   * <p>
   * This flag is chosen by the server.  The advantage of this flag:
   * <ol>
   *   <li><i>true</i> (server time):<br>
   *       This assumes that the client's clock is well
   *       synchronized with the server's clock (e.g. NTP).</li>
   *   <li><i>false</i> (client time):<br>
   *       This avoids clock synchronization issues but assumes
   *       quick message delivery and approximately equal message
   *       send/reply delivery times.</li>
   * </ol>
   */
  public boolean useServerTime() {
    return useServerTime;
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
      "-answer from="+getOriginator()+
      " to="+getTarget()+
      " sent="+Timestamp.toString(sendTime, now)+
      " reply="+Timestamp.toString(replyTime, now)+
      " useServer="+useServerTime+
      " "+m+")";
  }
}
