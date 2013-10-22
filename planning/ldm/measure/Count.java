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
/** Immutable implementation of Count.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Count extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double UNITS_PER_EACHES = (1.0d/1.0);
  public static final double EACHES_PER_UNITS = 1.0;
  public static final double UNITS_PER_DOZEN = 12;
  public static final double DOZEN_PER_UNITS = (1.0d/12);
  public static final double UNITS_PER_HUNDRED = 100;
  public static final double HUNDRED_PER_UNITS = (1.0d/100);
  public static final double UNITS_PER_GROSS = 144;
  public static final double GROSS_PER_UNITS = (1.0d/144);
  public static final double UNITS_PER_MILLION = 1000000;
  public static final double MILLION_PER_UNITS = (1.0d/1000000);

  // the value is stored as units
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Count() {}

  // private constructor
  private Count(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Count(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Count(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("units")) 
      theValue=n;
    else if (u.equals("eaches")) 
      theValue=n*UNITS_PER_EACHES;
    else if (u.equals("dozen")) 
      theValue=n*UNITS_PER_DOZEN;
    else if (u.equals("hundred")) 
      theValue=n*UNITS_PER_HUNDRED;
    else if (u.equals("gross")) 
      theValue=n*UNITS_PER_GROSS;
    else if (u.equals("million")) 
      theValue=n*UNITS_PER_MILLION;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Count newUnits(double v) {
    return new Count(v);
  }
  public static final Count newUnits(String s) {
    return new Count((Double.valueOf(s).doubleValue()));
  }
  public static final Count newEaches(double v) {
    return new Count(v*UNITS_PER_EACHES);
  }
  public static final Count newEaches(String s) {
    return new Count((Double.valueOf(s).doubleValue())*UNITS_PER_EACHES);
  }
  public static final Count newDozen(double v) {
    return new Count(v*UNITS_PER_DOZEN);
  }
  public static final Count newDozen(String s) {
    return new Count((Double.valueOf(s).doubleValue())*UNITS_PER_DOZEN);
  }
  public static final Count newHundred(double v) {
    return new Count(v*UNITS_PER_HUNDRED);
  }
  public static final Count newHundred(String s) {
    return new Count((Double.valueOf(s).doubleValue())*UNITS_PER_HUNDRED);
  }
  public static final Count newGross(double v) {
    return new Count(v*UNITS_PER_GROSS);
  }
  public static final Count newGross(String s) {
    return new Count((Double.valueOf(s).doubleValue())*UNITS_PER_GROSS);
  }
  public static final Count newMillion(double v) {
    return new Count(v*UNITS_PER_MILLION);
  }
  public static final Count newMillion(String s) {
    return new Count((Double.valueOf(s).doubleValue())*UNITS_PER_MILLION);
  }


  public int getCommonUnit() {
    return UNITS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "units",
    "eaches",
    "dozen",
    "hundred",
    "gross",
    "million",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    UNITS_PER_EACHES,
    UNITS_PER_DOZEN,
    UNITS_PER_HUNDRED,
    UNITS_PER_GROSS,
    UNITS_PER_MILLION,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int UNITS = 0;
  public static final int EACHES = 1;
  public static final int DOZEN = 2;
  public static final int HUNDRED = 3;
  public static final int GROSS = 4;
  public static final int MILLION = 5;
  public static final int MAXUNIT = 5;

  // Index Typed factory methods
  public static final Count newCount(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Count(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Count newCount(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Count((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newCount(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newCount(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Count)) throw new IllegalArgumentException();
    return new Count(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Count)) throw new IllegalArgumentException();
    return new Count(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Count(theValue*scale,0);
  }

  public final Measure negate() {
    return newCount(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newCount(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Count(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Count(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Count)) {
      throw new IllegalArgumentException("Expecting a Count/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getUnits() {
    return (theValue);
  }
  public double getEaches() {
    return (theValue/UNITS_PER_EACHES);
  }
  public double getDozen() {
    return (theValue/UNITS_PER_DOZEN);
  }
  public double getHundred() {
    return (theValue/UNITS_PER_HUNDRED);
  }
  public double getGross() {
    return (theValue/UNITS_PER_GROSS);
  }
  public double getMillion() {
    return (theValue/UNITS_PER_MILLION);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Count &&
             theValue == ((Count) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "units";
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
