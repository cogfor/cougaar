/*
 * <copyright>
 *  
 *  Copyright 1997-2007 BBNT Solutions, LLC
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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.cougaar.util.StackElements;

/**
 * An envelope that records the subscriber name, time of
 * "openTransaction()", and time of "closeTransaction()".
 * 
 * @see Subscriber option system property that must be enabled
 *    for these Envelopes to be used.
 */
public class TimestampedEnvelope extends Envelope {

  /**
    * 
    */
   private static final long serialVersionUID = 1L;

// weakly cache "publishAdd" stacks
  private static final Map stacks = new WeakHashMap();

  private String name;
  private long openTime;
  private long closeTime;

  public TimestampedEnvelope() {
  }

  @Override
public Envelope newInstance() {
    TimestampedEnvelope ret = new TimestampedEnvelope();
    ret.name = name;
    ret.openTime = openTime;
    ret.closeTime = closeTime;
    return ret;
  }

  public final void setName(String name) { 
    this.name = name; 
  }

  @Override
AddEnvelopeTuple newAddEnvelopeTuple(Object o) {
    return new Add(o, captureStack());
  }
  @Override
ChangeEnvelopeTuple newChangeEnvelopeTuple(Object o, List changes) {
    return new Change(o, changes, captureStack());
  }
  @Override
RemoveEnvelopeTuple newRemoveEnvelopeTuple(Object o) {
    return new Remove(o, captureStack());
  }

  private static StackElements captureStack() {
    StackElements se = new StackElements(new Throwable());
    synchronized (stacks) {
      StackElements cached_se = (StackElements) stacks.get(se);
      if (cached_se == null) {
        stacks.put(se, se);
      } else {
        se = cached_se;
      }
    }
    return se;
  }

  public final void setTransactionOpenTime(long openTime) { 
    this.openTime = openTime; 
  }

  public final void setTransactionCloseTime(long closeTime) { 
    this.closeTime = closeTime; 
  }

  /**
   * @return true if the envelope is from the blackboard (LPs)
   */
  public boolean isBlackboard() { return false; }

  /**
   * @return the name of the subscriber that created this envelope
   */
  public final String getName() { return name; }

  /**
   * @return time in milliseconds when the transaction was opened
   */
  public final long getTransactionOpenTime() { return openTime; }

  /**
   * @return time in milliseconds when the transaction was closed
   */
  public final long getTransactionCloseTime() { return closeTime; }

  // package-private subscription needs to see this
  boolean get_isVisible() { return isVisible(); }

  @Override
public String toString() {
    return 
      super.toString()+
      " ("+
      (isBlackboard() ? "blackboard, " : "client, ")+
      name+", "+
      openTime+" + "+
      (closeTime-openTime)+")";
  }

  private static final class Add extends AddEnvelopeTuple {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private StackElements se;
    public Add(Object o, StackElements se) {
      super(o);
      this.se = se;
    }
    @Override
   public StackElements getStack() { return se; }
  }
  private static final class Change extends ChangeEnvelopeTuple {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private StackElements se;
    public Change(Object o, List changes, StackElements se) {
      super(o, changes);
      this.se = se;
    }
    @Override
   public StackElements getStack() { return se; }
  }
  private static final class Remove extends RemoveEnvelopeTuple {
    /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private StackElements se;
    public Remove(Object o, StackElements se) {
      super(o);
      this.se = se;
    }
    @Override
   public StackElements getStack() { return se; }
  }
}
