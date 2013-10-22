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

package org.cougaar.planning.ldm.plan;

import java.io.Serializable;

/**
 *
 *
 */
 
public class AspectScorePoint implements Serializable, Cloneable {
  private AspectValue value;
  private double score;

  public AspectScorePoint(AspectValue value, double score) {
    this.value = value;
    this.score = score;
  }

  /** @deprecated Use AspectScorePoint(AspectValue,double) instead **/
  public AspectScorePoint(double value, double score, int type) {
    this.value = AspectValue.newAspectValue(type,value);
    this.score = score;
  }

  public boolean equals(Object o) {
    if (o instanceof AspectScorePoint) {
      AspectScorePoint that = (AspectScorePoint) o;
      return this.score == that.score && this.value.equals(that.value);
    }
    return false;
  }

  public int hashCode() {
    long bits = Double.doubleToRawLongBits(score);
    return (int) (bits & 0xffffffffL)
      ^ (int) ((bits >> 32) & 0xFFFFFFFFL)
      ^ value.hashCode();
  }

  public Object clone() {
    return new AspectScorePoint(value, score);
  }

  /* @return double The 'score'.
   */
  public double getScore() { return score; }
   
  /* @return Aspect The value and type of aspect.
   * @see org.cougaar.planning.ldm.plan.AspectValue
   */
  public AspectValue getAspectValue() { return value; }
   
  public double getValue() { return value.getValue(); }
  public int getAspectType() { return value.getAspectType(); }

  public static final AspectScorePoint getNEGATIVE_INFINITY(int type) {
    return new AspectScorePoint(AspectValue.newAspectValue(type,0.0), Double.NEGATIVE_INFINITY);
  }
  public static final AspectScorePoint getPOSITIVE_INFINITY(int type) {
    return new AspectScorePoint(AspectValue.newAspectValue(type,0.0), Double.POSITIVE_INFINITY);
  }

}
