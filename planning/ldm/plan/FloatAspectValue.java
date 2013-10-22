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

/** An AspectValue implementation which stores a float.
 */
 
public class FloatAspectValue extends TypedAspectValue {
  private final float value;

  // zeros cache 
  private static final int ZEROS = 20;
  private static final AspectValue zero[] = new AspectValue[ZEROS];
  static {
    // dumb, but we'll not worry about it (minimal excess AV creation)
    for (int i=0;i<ZEROS;i++) {
      zero[i] = new FloatAspectValue(i,0.0f);
    }
  }

  protected FloatAspectValue(int type, float value) {
    super(type);
    if (Float.isNaN(value) || Float.isInfinite(value))
      throw new IllegalArgumentException("The value of a FloatAspectValue must be a finite, non-NaN");
    this.value = value;
  }

  public static AspectValue create(int type, Object o) {
    float value;
    if (o instanceof Number) {
      value = ((Number)o).floatValue();
    } else if (o instanceof AspectValue) {
      value = ((AspectValue)o).floatValue();
    } else {
      throw new IllegalArgumentException("Cannot construct a FloatAspectValue from "+o);
    }
    return create(type, value);
  }

  public static AspectValue create(int type, float value) {
    if (value == 0.0 &&
        type>=0 && type<ZEROS ) {
      return zero[type];
    }
    return new FloatAspectValue(type,value);
  }

  public final double doubleValue() {
    return (double) value;
  }
  public final long longValue() {
    return Math.round(value);
  }
  public final float floatValue() {
    return value;
  }
  public final int intValue() {
    return (int) Math.round(value);
  }

  public boolean equals(Object v) {
    if (v instanceof FloatAspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              floatValue() == ((AspectValue)v).floatValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+((int)(floatValue()*128));
  }

  public String toString() {
    return Float.toString(floatValue())+"["+getType()+"]";
  }


}


