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
package org.cougaar.planning.ldm.measure;

/** Interface for all Derivative or "Rate" Measures.
 **/

public interface Derivative extends Measure {
  /** @return the numerator class of the derivative measure (dx of dx/dy) **/
  Class getNumeratorClass();

  /** @return the denominator class of the derivative measure (dy of dx/dy) **/
  Class getDenominatorClass();

  /** The value of the canonical instance will have no relationship to 
   * the value of the Derivative Measure, but is to be used for introspection
   * purposes.
   * @return a canonical instance of the numerator class.
   **/
  Measure getCanonicalNumerator();

  /** The value of the canonical instance will have no relationship to 
   * the value of the Derivative Measure, but is to be used for introspection
   * purposes.
   * @return a canonical instance of the denominator class.
   **/
  Measure getCanonicalDenominator();

  /** Get the value of the derivative measure by specifying both numerator and
   * denominator units.
   * @param numerator_unit to use
   * @param denominator_unit to use
   * @return value given these units
   **/
  double getValue(int numerator_unit, int denominator_unit);
  
  /** integrate the denominator, resulting in a non-derivative numerator.
   * For example, computes a Distance given a Speed and a Duration.
   *
   * This is a synonym for multiply
   * @param denominator to use
   * @return a newly created Numerator measure.
   **/
  Measure computeNumerator(Measure denominator);
  double divideRate(Rate other);
}
