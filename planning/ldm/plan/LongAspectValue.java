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

/** An AspectValue implementation which stores a long.
 */
 
public class LongAspectValue extends TypedAspectValue {
  private final long value;

  protected LongAspectValue(int type, long value) {
    super(type);
    this.value = value;
  }

  public static AspectValue create(int type, Object o) {
    long value;
    if (o instanceof Number) {
      value = ((Number)o).longValue();
    } else if (o instanceof AspectValue) {
      value = ((AspectValue)o).longValue();
    } else {
      throw new IllegalArgumentException("Cannot construct a LongAspectValue from "+o);
    }
    return new LongAspectValue(type,value);
  }

  public final double doubleValue() {
    return (double) value;
  }
  public final long longValue() {
    return value;
  }
  public final float floatValue() {
    return (float) value;
  }
  public final int intValue() {
    return (int) value;
  }

  public boolean equals(Object v) {
    if (v instanceof LongAspectValue) {
      return (getType() == ((AspectValue)v).getType() &&
              longValue() == ((AspectValue)v).longValue());
    } else {
      return false;
    }
  }

  public int hashCode() {
    return getType()+((int)(longValue()*128));
  }

  public String toString() {
    return Long.toString(longValue())+"["+getType()+"]";
  }

}


