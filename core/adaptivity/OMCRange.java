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

/**
 * Holds a range specification for an operating mode or condition
 * value. Ranges are half-open intervals. The value of max _must_
 * exceed low.
 **/
public class OMCRange implements Serializable {
  /**
    * 
    */
   private static final long serialVersionUID = 1L;
protected Comparable min, max;

  /**
   * Constructor for int values
   **/
  protected OMCRange(int min, int max) {
    this(new Integer(min), new Integer(max));
  }

  /**
   * Constructor for double values
   **/
  protected OMCRange(double min, double max) {
    this(new Double(min), new Double(max));
  }

  /**
   * Constructor for Comparable values
   **/
  protected OMCRange(Comparable min, Comparable max) {
    if (min.getClass() != max.getClass()) {
      throw new IllegalArgumentException("Min and max have different classes");
    }
    if (min.compareTo(max) > 0) {
      throw new IllegalArgumentException("Min must not exceed max");
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Test if a value is in this range.
   * @return true if the value is in the (closed) interval between min
   * and max.
   * @param v The value to compare.
   **/
  public boolean contains(Comparable v) {
    return min.compareTo(v) <= 0 && max.compareTo(v) >= 0;
  }

  /**
   * Gets the minimum value in the range.
   **/
  public Comparable getMin() {
    return min;
  }

  /**
   * Gets the maximum value in the range.
   **/
  public Comparable getMax() {
    return max;
  }

  @Override
public int hashCode() {
    return min.hashCode() ^ max.hashCode();
  }

  @Override
public boolean equals(Object o) {
    if (o instanceof OMCRange) {
      OMCRange that = (OMCRange) o;
      return this.min.equals(that.min) && this.max.equals(that.max);
    }
    return false;
  }
}
