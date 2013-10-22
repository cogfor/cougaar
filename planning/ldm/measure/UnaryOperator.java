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
package org.cougaar.planning.ldm.measure;

/**
 * An operator that can be applied on a single measure.
 *
 * @see Measure#apply(UnaryOperator)
 */
public interface UnaryOperator<M extends Measure> {

  /**
   * Apply this operator to the specified measure.
   * @param m the measure, or null for zero
   * @return the operator result, where null indicates zero.  If
   *   the input measure is non-null then this method will not
   *   return null.
   */
  M apply(M m);

  /** @see Measure#add */
  class Add<M extends Measure> implements UnaryOperator<M> {
    private final M toAdd;
    public Add(M toAdd) {
      this.toAdd = toAdd;
    }
    public M apply(M m) {
      return 
        (m == null ? toAdd :
         toAdd == null ? m :
         (M) m.add(toAdd));
    }
  }
  
  /** @see Measure#subtract */
  class Subtract<M extends Measure> extends Add<M> {
    public Subtract(M toSubtract) {
      super(toSubtract == null ? null : (M) toSubtract.negate());
    }
  }

  /**
   * Returns the given measure instance.
   * @see Area
   */
  Identity IDENTITY = new Identity();
  class Identity<M extends Measure> implements UnaryOperator<M> {
    public M apply(M m) {
      return m;
    }
  }

  /** @see Measure#negate */
  Negate NEGATE = new Negate();
  class Negate<M extends Measure> implements UnaryOperator<M> {
    public M apply(M m) {
      return (m == null ? null : (M) m.negate());
    }
  }

  /** @see Measure#scale */
  class Scale<M extends Measure> implements UnaryOperator<M> {
    private final double scale;
    public Scale(double scale) {
      this.scale = scale;
    }
    public M apply(M m) {
      return (m == null ? null : (M) m.scale(scale));
    }
  }

  /** @see Measure#floor */
  class Floor<M extends Measure> implements UnaryOperator<M> {
    private final int unit;
    public Floor(int unit) {
      this.unit = unit;
    }
    public M apply(M m) {
      return (m == null ? null : (M) m.floor(unit));
    }
  }

  /**
   * Note: the {@link Divide#apply} result is a {@link Duration}.
   *
   * @see Measure#divide
   */
  class Divide<M extends Measure> implements UnaryOperator<M> {
    private final Rate rate;
    public Divide(Rate rate) {
      this.rate = rate;
    }
    public M apply(M m) {
      return (m == null ? null : (M) m.divide(rate));
    }
  }

  // Multiply for GenericDerivative?

  /**
   * Select the measure with the minimum {@link Measure#getNativeValue}.
   */
  class Min<M extends Measure> implements UnaryOperator<M> {
    public final M other;
    public Min(M other) {
      this.other = other;
    }
    public M apply(M m) {
      return 
        (m == null ? (other == null ? null : (M) other.min(null)) :
         (M) m.min(other));
    }
  }

  /** Select the measure with the maximum {@link Measure#getNativeValue} */
  class Max<M extends Measure> implements UnaryOperator<M> {
    public final M other;
    public Max(M other) {
      this.other = other;
    }
    public M apply(M m) {
      return 
        (m == null ? (other == null ? null : (M) other.max(null)) :
         (M) m.max(other));
    }
  }

  /** @see Max same as max(null) */
  AboveZero ABOVE_ZERO = new AboveZero();
  class AboveZero<M extends Measure> extends Max<M> {
    public AboveZero() { super(null); }
  }

  /** @see Min same as min(null) */
  BelowZero BELOW_ZERO = new BelowZero();
  class BelowZero<M extends Measure> extends Min<M> {
    public BelowZero() { super(null); }
  }

  /**
   * Sum the values at the individual points.
   * @see Integrate sum-of-areas
   */
  class Sum<M extends Measure> implements UnaryOperator<M> {
    private M sum = null;
    public M apply(M m) {
      sum = (sum == null ? m : (M) sum.add(m));
      return sum;
    }
  }

  /**
   * An "identity" operator where the caller is required to pass
   * in the integrated volume between points.
   * <p>
   * E.g., if the prior measure was "time=10 value=5" and the
   * current measure is "time=20 value=0" then the caller should
   * apply a value of 25.
   *
   * @see Identity
   */
  Area AREA = new Area();
  class Area<M extends Measure> implements UnaryOperator<M> {
    public M apply(M m) {
      return m;
    }
  }

  /**
   * Sum of {@link #AREA}s.
   * @see Sum sum-of-values
   */
  class Integrate<M extends Measure> extends Area<M> {
    private M sum = null;
    public M apply(M m) {
      sum = (sum == null ? m : (M) sum.add(m));
      return sum;
    }
  }
}
