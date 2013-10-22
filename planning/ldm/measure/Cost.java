/*
 * <copyright>
 *  
 *  Copyright 1997-2012 Raytheon BBN Technologies
 *  under partial sponsorship of the Defense Advanced Research Projects
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

/* @generated Wed Jun 06 07:55:05 EDT 2012 from measures.def - DO NOT HAND EDIT */
/** Immutable implementation of Cost.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Cost extends AbstractMeasure implements Externalizable {
  // Conversion factor constants

  // the value is stored as dollars
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Cost() {}

  // private constructor
  private Cost(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Cost(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Cost(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("dollars")) 
      theValue=n;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Cost newDollars(double v) {
    return new Cost(v);
  }
  public static final Cost newDollars(String s) {
    return new Cost((Double.valueOf(s).doubleValue()));
  }


  public int getCommonUnit() {
    return 0;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "dollars",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int DOLLARS = 0;
  public static final int MAXUNIT = 0;

  // Index Typed factory methods
  public static final Cost newCost(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Cost(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Cost newCost(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Cost((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newCost(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newCost(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Cost)) throw new IllegalArgumentException();
    return new Cost(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Cost)) throw new IllegalArgumentException();
    return new Cost(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Cost(theValue*scale,0);
  }

  public final Measure negate() {
    return newCost(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newCost(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Cost(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Cost(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Cost)) {
      throw new IllegalArgumentException("Expecting a Cost/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getDollars() {
    return (theValue);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Cost &&
             theValue == ((Cost) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "usd";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  // serialization
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeDouble(theValue);
  }
  public void readExternal(ObjectInput in) throws IOException {
    theValue = in.readDouble();
  }
}
