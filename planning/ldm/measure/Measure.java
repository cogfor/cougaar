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

import java.io.Serializable;

/** Base interface for all Measure classes.
 *
 * All concrete subclasses of AbstractMeasure are required to 
 * implement the method:
 *   public static AbstractMeasure newMeasure(String s, int unit);
 * Other AbstractMeasure-level constructors may be defined, depending
 * on each the primative-types that each concrete class can handle.
 * For instance, Distance is based on a double value, so in addition to
 * the above, Distance.newMeasure(double d, int unit) will be defined.
 *
 * The allowed values of the unit specifier depends on (and is defined
 * in) each Measure type.  Example: Distance.FEET is a static final int
 * with the correct value for use in constructing Distances in Feet.
 * 
 * Each Measure class will also define a getValue(int unit) method 
 * returning whatever primitive type it is based on (usually double).
 *
 * All Measure classes are equals() comparable (exact internal value 
 * equality), define toString() (the value in a standard unit and a unit 
 * abbreviation, e.g. "4.5m"), and define hashCode() (for completeness).
 *
 * @see AbstractMeasure for base implementation class of all measures.
 **/

public interface Measure extends Comparable, Serializable {
  /** @return a commonly-used unit used by this measure.  NOTE that this
   * is only a convenience and should not be depended on for computational
   * use, since the notion of a common unit is extremely dependent on
   * context.
   **/
  int getCommonUnit();

  /**
   * The Measure holds the value in a native unit - what is it?
   * @return the native unit - what the measure uses as the internal unit
   */
  int getNativeUnit();

  /** @return the value of the highest-valued unit known by this measure.
   * There is no implied relationship between "highest valued" unit and "size" 
   * of that unit.
   **/
  int getMaxUnit();

  /**
   * The result is undefined if the unit is not valid for this measure class.
   * @param unit to get name of
   * @return the name of the unit specified.
   **/
  String getUnitName(int unit);

  /** @return the value of the measure in terms of the specified units.
   *  @param unit must be in the range from 0 to getMaxUnit.
   **/
  double getValue(int unit);
  double getNativeValue();
  Measure add(Measure other);
  Measure subtract(Measure other);
  Measure negate();
  Measure scale(double scale);
  Measure floor(int unit);
  Measure valueOf(double value);
  Measure valueOf(double value, int unit);
  Duration divide(Rate rate);

  Measure min(Measure other);
  Measure max(Measure other);
  Measure apply(UnaryOperator op);
  Measure apply(BinaryOperator op, Measure other);
}
