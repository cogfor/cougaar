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

package org.cougaar.core.adaptivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OMCRangeList implements Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;

public static final OMCRangeList ALL_DOUBLE_RANGE_LIST =
    new OMCRangeList(Double.MIN_VALUE, Double.MAX_VALUE);

  public static final OMCRangeList ALL_INTEGER_RANGE_LIST =
    new OMCRangeList(Integer.MIN_VALUE, Integer.MAX_VALUE);

  public static final OMCRangeList ALL_LONG_RANGE_LIST =
    new OMCRangeList(Long.MIN_VALUE, Long.MAX_VALUE);

  public static final OMCRangeList ALL_STRING_RANGE_LIST =
    new OMCRangeList("", "\ffff");

  OMCRange[] allowedValues;

  public OMCRangeList(OMCRange[] av) {
    allowedValues = av;
  }

  public OMCRangeList(OMCRange av) {
    allowedValues = new OMCRange[] {av};
  }

  public OMCRangeList(double v) {
    this(new OMCRange[] {new OMCPoint(v)});
  }

  public OMCRangeList(int v) {
    this(new OMCRange[] {new OMCPoint(v)});
  }

  public OMCRangeList(Comparable v) {
    this(new OMCRange[] {new OMCPoint(v)});
  }

  public OMCRangeList(int[] vs) {
    this(createRange(vs));
  }

  public OMCRangeList(double[] vs) {
    this(createRange(vs));
  }

  public OMCRangeList(Comparable[] vs) {
    this(createRange(vs));
  }

  public OMCRangeList(double min, double max) {
    this(createRange(new Double(min), new Double(max)));
  }

  public OMCRangeList(int min, int max) {
    this(createRange(new Integer(min), new Integer(max)));
  }

  public OMCRangeList(long min, long max) {
    this(createRange(new Long(min), new Long(max)));
  }

  public OMCRangeList(Comparable min, Comparable max) {
    this(createRange(min, max));
  }

  private static OMCRange[] createRange(int[] vs) {
    Comparable[] cs = new Comparable[vs.length];
    for (int i = 0; i < vs.length; i++) {
      cs[i] = new Integer(vs[i]);
    }
    return createRange(cs);
  }

  private static OMCRange[] createRange(double[] vs) {
    Comparable[] cs = new Comparable[vs.length];
    for (int i = 0; i < vs.length; i++) {
      cs[i] = new Double(vs[i]);
    }
    return createRange(cs);
  }

  private static OMCRange[] createRange(Comparable[] vs) {
    OMCRange[] result = new OMCRange[vs.length];
    for (int i = 0; i < vs.length; i++) {
      result[i] = new OMCPoint(vs[i]);
    }
    return result;
  }

  private static OMCRange[] createRange(Comparable min, Comparable max) {
    OMCRange[] result = {
      new OMCRange(min, max)
    };
    return result;
  }

  public Comparable getMax() {
    Comparable max = allowedValues[0].getMax();
    for (int i = 1; i < allowedValues.length; i++) {
      OMCRange range = allowedValues[i];
      Comparable tmax = range.getMax();
      if (tmax.compareTo(max) > 0) max = tmax;
    }
    return max;
  }

  public Comparable getMin() {
    Comparable min = allowedValues[0].getMin();
    for (int i = 1; i < allowedValues.length; i++) {
      OMCRange range = allowedValues[i];
      Comparable tmin = range.getMin();
      if (tmin.compareTo(min) < 0) min = tmin;
    }
    return min;
  }

  /**
   * Return a value that is the equivalent under the IN
   * ConstraintOperator to this value under the given operator. For
   * example, if the operator is LESSTHANOREQUAL, and this value is a
   * single value, the returned value will be the range from the
   * minimum value to the single value. This allows ConstraintPhrases
   * to be converted to a form that can be directly combined with
   * other ConstraintPhrases from other Plays.
   **/
  public OMCRangeList applyOperator(ConstraintOperator op) {
    if (op.equals(ConstraintOperator.GREATERTHAN)) {
      Comparable max = getMax();
      return new OMCRangeList(ComparableHelper.increment(max), ComparableHelper.getMax(max));
    }
    if (op.equals(ConstraintOperator.GREATERTHANOREQUAL)) {
      Comparable max = getMax();
      return new OMCRangeList(max, ComparableHelper.getMax(max));
    }
    if (op.equals(ConstraintOperator.LESSTHAN)) {
      Comparable min = getMin();
      return new OMCRangeList(ComparableHelper.getMin(min), ComparableHelper.decrement(min));
    }
    if (op.equals(ConstraintOperator.LESSTHANOREQUAL)) {
      Comparable min = getMin();
      return new OMCRangeList(ComparableHelper.getMin(min), min);
    }
    if (op.equals(ConstraintOperator.EQUAL)) return this;
    if (op.equals(ConstraintOperator.ASSIGN)) return this;
    if (op.equals(ConstraintOperator.NOTIN) ||
        op.equals(ConstraintOperator.NOTEQUAL)) {
      OMCRangeList newValue = complementRange(allowedValues[0]);
      for (int i = 1; i < allowedValues.length; i++) {
        newValue = newValue.intersect(complementRange(allowedValues[i]));
      }
      return newValue;
    }
    if (op.equals(ConstraintOperator.IN)) return this;
    return this;
  }

  private OMCRangeList complementRange(OMCRange range) {
    Comparable min = range.getMin();
    Comparable max = range.getMax();
    OMCRange[] compRange = {
      new OMCRange(ComparableHelper.getMin(min), ComparableHelper.decrement(min)),
      new OMCRange(ComparableHelper.increment(max), ComparableHelper.getMax(max))
    };
    return new OMCRangeList(compRange);
  }

  public OMCRangeList intersect(OMCRangeList that) {
    List result = new ArrayList(allowedValues.length);
    OMCRange[] thatAllowedValues = that.getAllowedValues();
    for (int i = 0; i < allowedValues.length; i++) {
      Comparable thisMin = allowedValues[i].getMin();
      Comparable thisMax = allowedValues[i].getMax();
      for (int j = 0; j < thatAllowedValues.length; j++) {
        Comparable thatMin = thatAllowedValues[j].getMin();
        Comparable thatMax = thatAllowedValues[j].getMax();
        if (thatMin.compareTo(thisMax) > 0) continue; // No overlap
        if (thisMin.compareTo(thatMax) > 0) continue; // No overlap
        Comparable newMin;
        Comparable newMax;
        if (thisMin.compareTo(thatMin) < 0) {
          newMin = thatMin;
        } else {
          newMin = thisMin;
        }
        if (thisMax.compareTo(thatMax) > 0) {
          newMax = thatMax;
        } else {
          newMax = thisMax;
        }
        result.add(new OMCRange(newMin, newMax));
      }
    }
    return new OMCRangeList((OMCRange[]) result.toArray(new OMCRange[result.size()]));
  }

  public Comparable getEffectiveValue() {
    return allowedValues[0].getMin();
  }

  public OMCRange[] getAllowedValues() {
    return allowedValues;
  }

  public boolean isAllowed(Comparable v) {
    if (v.getClass() != getEffectiveValue().getClass()) return false; // Wrong class (exception?)
    for (int i = 0; i < allowedValues.length; i++) {
      if (allowedValues[i].contains(v)) {
        return true;
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return allowedValues.length == 0;
  }

  @Override
public int hashCode() {
    int hc = 0;
    for (int i = 0; i < this.allowedValues.length; i++) {
      hc = 31 * hc + allowedValues[i].hashCode();
    }
    return hc;
  }

  @Override
public boolean equals(Object o) {
    if (!(o instanceof OMCRangeList)) return false;
    OMCRangeList that = (OMCRangeList) o;
    if (this.allowedValues.length != that.allowedValues.length) return false;
    for (int i = 0; i < this.allowedValues.length; i++) {
      if (!this.allowedValues[i].equals(that.allowedValues[i])) return false;
    }
    return true;
  }

  @Override
public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append('{');
    for (int i = 0; i < allowedValues.length; i++) {
      if (i > 0) buf.append(',');
      buf.append(allowedValues[i]);
    }
    buf.append('}');
    return buf.toString();
  }

  static OMCRangeList[] v = {
    new OMCRangeList(new OMCRange[] {
      new OMCPoint(1.0),
      new OMCPoint(3.0),
      new OMCThruRange(3.5, 5.0)
    }),
    new OMCRangeList(new OMCRange[] {
      new OMCThruRange(2.5, 4.6)
    }),
    new OMCRangeList(new OMCRange[] {
      new OMCPoint(3.0),
      new OMCPoint(5.5),
    })
  };

  public static void main(String[] args) {
    OMCRangeList o = new OMCRangeList("Abc");
    OMCRangeList x = o.applyOperator(ConstraintOperator.NOTEQUAL);
    System.out.println(o + " -> " + x);
  }
}
