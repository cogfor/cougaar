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
 * An operator that can be applied on two measures.
 *
 * @see Measure#apply(BinaryOperator,Measure)
 */
public interface BinaryOperator<M extends Measure> {

  /**
   * Apply this operator to the specified measures.
   * @param a a measure, or null for zero
   * @param b a measure, or null for zero
   * @return the operator result, where null indicates zero.  If both
   *   of the input measures are non-zero then this method will not
   *   return null.
   */
  M apply(M a, M b);

  Add ADD = new Add();
  class Add<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) {
      return 
        (a == null ? b :
         b == null ? a :
         (M) a.add(b));
    }
  }
  
  /** @see Measure#subtract */
  Subtract SUBTRACT = new Subtract();
  class Subtract<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) {
      return 
        (a == null ? b :
         b == null ? a :
         (M) a.subtract(b));
    }
  }

  /** return the "a" graph */
  First FIRST = new First();
  class First<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) { return a; }
  }

  /** return the "b" graph */
  Second SECOND = new Second();
  class Second<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) { return b; }
  }

  // Multiply for GenericDerivative?

  /** Select the measure with the minimum {@link Measure#getNativeValue} */
  Min MIN = new Min();
  class Min<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) {
      return 
        (a == null ? (b == null ? null : (M) b.min(null)) :
         (M) a.min(b));
    }
  }

  /** Select the measure with the maximum {@link Measure#getNativeValue} */
  Max MAX = new Max();
  class Max<M extends Measure> implements BinaryOperator<M> {
    public M apply(M a, M b) {
      return 
        (a == null ? (b == null ? null : (M) b.max(null)) :
         (M) a.max(b));
    }
  }
}
