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

package org.cougaar.core.blackboard;


/**
 * Contains metrics on transaction open and close timestamps.
 * <p>
 * @see EnvelopeMetricsSubscription
 */
public final class EnvelopeMetrics implements java.io.Serializable {

  /**
   * If "isBlackboard()" is true, then the "getName()" response
   * is == to this interned "&lt;blackboard&gt;" string constant.
   */
  public static final String BLACKBOARD = "<blackboard>";

  /**
   * If "isBlackboard()" is false, and the name is not known,
   * the then "getName()" response is == to the interned
   * "&lt;unknown&gt;" string constant.
   */
  public static final String UNKNOWN = "<unknown>";

  private final String name;
  private final long openTime;
  private final long closeTime;

  public EnvelopeMetrics(TimestampedEnvelope te) {
    this.name = _getName(te);
    this.openTime = te.getTransactionOpenTime();
    this.closeTime = te.getTransactionCloseTime();

    // could also easily get the raw tuples and 
    // count the number of adds / changes / removes
  }

  /**
   * @return true if the envelope is from the blackboard (LPs)
   */
  public final boolean isBlackboard() { return (name == null); }

  /**
   * @return the name of the subscriber that created this envelope.
   * @see #BLACKBOARD
   * @see #UNKNOWN
   */
  public final String getName() { 
    return ((name != null) ? name : BLACKBOARD);
  }

  /**
   * @return time in milliseconds when the transaction was opened
   */
  public final long getTransactionOpenTime() { return openTime; }

  /**
   * @return time in milliseconds when the transaction was closed
   */
  public final long getTransactionCloseTime() { return closeTime; }

  //
  // use the name as the hash-code and equality?
  //

  @Override
public String toString() {
    return 
      "EnvelopeMetrics {"+
      "\n  bb:    "+isBlackboard()+
      "\n  name:  "+getName()+
      "\n  open:  "+openTime+
      "\n  close: "+closeTime+" (+"+(closeTime-openTime)+")"+
      "}";
  }

  // helper for constructor
  private static final String _getName(TimestampedEnvelope te) {
    if (te.isBlackboard()) {
      return null;
    }
    String s = te.getName();
    if (s == null) {
      return UNKNOWN;
    }
    /*
    // is this worth checking?
    if (s.equals(BLACKBOARD)) {
      return "dup"+BLACKBOARD;
    } else if (s.equals(UNKNOWN)) {
      return "dup"+UNKNOWN;
    }
    */
    return s;
  }

  private static final long serialVersionUID = -5208392019823789283L;
}
