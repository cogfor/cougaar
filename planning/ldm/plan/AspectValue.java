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

import org.cougaar.util.MoreMath;

/** AspectValue is the essential "value" abstraction with respect to
 * evaluation of the goodness or correctness of a particular solution.
 * AspectValues are no longer mutable and no longer implement AspectType.
 * @see AllocationResult
 */
 
public abstract class AspectValue implements Serializable {
  //
  // Factories
  // 

  // MIK - we might want to optimize the "standard" cases to avoid
  // boxing the primatives.
  
  public static final AspectValue newAspectValue(int type, long value) {
    return newAspectValue(type, new Long(value));
  }
  public static final AspectValue newAspectValue(int type, double value) {
    return newAspectValue(type, new Double(value));
  }
  public static final AspectValue newAspectValue(int type, float value) {
    return newAspectValue(type, new Float(value));
  }
  public static final AspectValue newAspectValue(int type, int value) {
    return newAspectValue(type, new Integer(value));
  }
  public static final AspectValue newAspectValue(int type, Object o) {
    AspectType.Factory f = AspectType.registry.get(type);
    if (f == null) {
      throw new IllegalArgumentException("Type "+type+" is not a known Aspect type");
    } else {
      try {
        return f.newAspectValue(o);
      } catch (IllegalArgumentException iae) {
        throw new RuntimeException(
            "Unable to create new AspectValue("+type+", "+o+")", iae);
      }
    }
  }
  public static final AspectValue newAspectValue(AspectType.Factory type, Object o) {
    return type.newAspectValue(o);
  }
  public static final AspectValue newAspectValue(AspectType.Factory type, long value) {
    return newAspectValue(type, new Long(value));
  }
  public static final AspectValue newAspectValue(AspectType.Factory type, double value) {
    return newAspectValue(type, new Double(value));
  }
  public static final AspectValue newAspectValue(AspectType.Factory type, float value) {
    return newAspectValue(type, new Float(value));
  }
  public static final AspectValue newAspectValue(AspectType.Factory type, int value) {
    return newAspectValue(type, new Integer(value));
  }
    
  //
  // pseudo-factories
  //

  /** factory for possibly creating a new AspectValue with the same type 
   * but a different value.
   **/
  public AspectValue dupAspectValue(double newvalue) {
    return (doubleValue() == newvalue)?this:newAspectValue(getType(), newvalue);
  }
  public AspectValue dupAspectValue(float newvalue) {
    return (floatValue() == newvalue)?this:newAspectValue(getType(), newvalue);
  }
  public AspectValue dupAspectValue(long newvalue) {
    return (longValue() == newvalue)?this:newAspectValue(getType(), newvalue);
  }
  public AspectValue dupAspectValue(int newvalue) {
    return (intValue() == newvalue)?this:newAspectValue(getType(), newvalue);
  }

  //
  // accessors
  //

  /** Non-preferred alias for #getType()
   * @note may be deprecated in the future - use getType instead.
   */
  public int getAspectType() { return getType();}

  /** @return int The Aspect Type.
   * @see org.cougaar.planning.ldm.plan.AspectType
   */
  public abstract int getType();
  
  /** Non-preferred alias for #doubleValue().
   * @note may be deprecated in the future - use doubleValue instead.
   */
  public double getValue() { return doubleValue(); }

  /** The value of the aspect as a double. */
  public abstract double doubleValue();
  /** The value of the aspect as a float **/
  public abstract float floatValue();
  /** The value of the aspect as a long **/
  public abstract long longValue();
  /** The value of the aspect as an int **/
  public abstract int intValue();

  //
  // comparisons
  //

  public boolean nearlyEquals(Object o) {
    if (o instanceof AspectValue) {
      AspectValue that = (AspectValue) o;
      if (this.getAspectType() == that.getAspectType()) {
        return MoreMath.nearlyEquals(this.getValue(), that.getValue());
      }
    }
    return false;
  }
  
  public boolean isLessThan(AspectValue v) {
    return (getValue() < v.getValue());
  }
  public boolean isGreaterThan(AspectValue v) {
    return (getValue() > v.getValue());
  }

  public double minus(AspectValue v) {
    return getValue() - v.getValue();
  }

  public boolean isBetween(AspectValue low, AspectValue hi) {
    return (! ( isLessThan(low) ||
                isGreaterThan(hi) ));
  } 

  // 
  // basic object methods

  // //Too difficult to maintain
  // public boolean equals(AspectValue v) {
  //    return (v.getType() == getType() &&
  //            v.getValue() == getValue());
  //  }
  

  public boolean equals(Object v) {
    if (v instanceof AspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              getValue() == ((AspectValue)v).getValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+(((int)getValue())<<2);
  }

  public String toString() {
    return Double.toString(getValue())+"["+getType()+"]";
  }

  /////// statics

  /**
   * This should be in AspectType, but that's an interface and can't
   * have methods. This is the closest place that makes any sense and
   * avoids creating a new class just to convert aspect types into
   * strings.
   **/
  public static String aspectTypeToString(int aspectType) {
    if (aspectType >=0 && aspectType < AspectType.ASPECT_STRINGS.length) {
      return AspectType.ASPECT_STRINGS[aspectType];
    } else {
      return String.valueOf(aspectType);
    }
  }

  /**
   * Clone an array of AspectValue.  Does not, of course, clone the elements
   * since they are immutable.
   * @param avs an array of AspectValue
   * @return a copy of the array with copies of array element values.
   **/
  public static AspectValue[] clone(AspectValue[] avs) {
    AspectValue[] result = new AspectValue[avs.length];
    for (int i = 0; i < avs.length; i++) {
      result[i] = avs[i];
    }
    return result;
  }

  /**
   * Compare two arrays of AspectValues. Since the values are not
   * necessarily in the same order, we first check assuming they are
   * in the same order. If that fails because they are not in the same
   * order, we try again reordering the values as needed. Arrays
   * having repeated AspectTypes produce unspecified results. Such
   * arrays are intrinsically ambiguous.
   **/
  public static boolean equals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    if (len != avs2.length) return false; // Can't be equal if different length
  outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
    inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.equals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

  public static boolean nearlyEquals(AspectValue[] avs1, AspectValue[] avs2) {
    int len = avs1.length;
    if (len != avs2.length) return false; // Can't be equal if different length
  outer:
    for (int i = 0; i < len; i++) {
      AspectValue av1 = avs1[i];
      int type1 = av1.getAspectType();
    inner:
      for (int j = 0; j < len; j++) {
        int k = (i + j) % len;
        AspectValue av2 = avs2[k];
        int type2 = av2.getAspectType();
        if (type1 == type2) {
          if (av1.nearlyEquals(av2)) continue outer;
          break inner;
        }
      }
      return false;             // Found no match
    }
    return true;                // Found a match for every aspect
  }

}
