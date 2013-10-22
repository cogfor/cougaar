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
/** Immutable implementation of Mass.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Mass extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double GRAMS_PER_KILOGRAMS = 1000;
  public static final double KILOGRAMS_PER_GRAMS = (1.0d/1000);
  public static final double GRAMS_PER_OUNCES = (1.0d/0.035273962);
  public static final double OUNCES_PER_GRAMS = 0.035273962;
  public static final double GRAMS_PER_POUNDS = (1.0d/0.0022046226);
  public static final double POUNDS_PER_GRAMS = 0.0022046226;
  public static final double GRAMS_PER_TONS = 907184.74;
  public static final double TONS_PER_GRAMS = (1.0d/907184.74);
  public static final double GRAMS_PER_SHORT_TONS = 907184.74;
  public static final double SHORT_TONS_PER_GRAMS = (1.0d/907184.74);
  public static final double GRAMS_PER_LONG_TONS = 1016046.9;
  public static final double LONG_TONS_PER_GRAMS = (1.0d/1016046.9);

  // the value is stored as grams
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Mass() {}

  // private constructor
  private Mass(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Mass(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Mass(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("grams")) 
      theValue=n;
    else if (u.equals("kilograms")) 
      theValue=n*GRAMS_PER_KILOGRAMS;
    else if (u.equals("ounces")) 
      theValue=n*GRAMS_PER_OUNCES;
    else if (u.equals("pounds")) 
      theValue=n*GRAMS_PER_POUNDS;
    else if (u.equals("tons")) 
      theValue=n*GRAMS_PER_TONS;
    else if (u.equals("shorttons")) 
      theValue=n*GRAMS_PER_SHORT_TONS;
    else if (u.equals("longtons")) 
      theValue=n*GRAMS_PER_LONG_TONS;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Mass newGrams(double v) {
    return new Mass(v);
  }
  public static final Mass newGrams(String s) {
    return new Mass((Double.valueOf(s).doubleValue()));
  }
  public static final Mass newKilograms(double v) {
    return new Mass(v*GRAMS_PER_KILOGRAMS);
  }
  public static final Mass newKilograms(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_KILOGRAMS);
  }
  public static final Mass newOunces(double v) {
    return new Mass(v*GRAMS_PER_OUNCES);
  }
  public static final Mass newOunces(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_OUNCES);
  }
  public static final Mass newPounds(double v) {
    return new Mass(v*GRAMS_PER_POUNDS);
  }
  public static final Mass newPounds(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_POUNDS);
  }
  public static final Mass newTons(double v) {
    return new Mass(v*GRAMS_PER_TONS);
  }
  public static final Mass newTons(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_TONS);
  }
  public static final Mass newShortTons(double v) {
    return new Mass(v*GRAMS_PER_SHORT_TONS);
  }
  public static final Mass newShortTons(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_SHORT_TONS);
  }
  public static final Mass newLongTons(double v) {
    return new Mass(v*GRAMS_PER_LONG_TONS);
  }
  public static final Mass newLongTons(String s) {
    return new Mass((Double.valueOf(s).doubleValue())*GRAMS_PER_LONG_TONS);
  }


  public int getCommonUnit() {
    return SHORT_TONS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "grams",
    "kilograms",
    "ounces",
    "pounds",
    "tons",
    "short_tons",
    "long_tons",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    GRAMS_PER_KILOGRAMS,
    GRAMS_PER_OUNCES,
    GRAMS_PER_POUNDS,
    GRAMS_PER_TONS,
    GRAMS_PER_SHORT_TONS,
    GRAMS_PER_LONG_TONS,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int GRAMS = 0;
  public static final int KILOGRAMS = 1;
  public static final int OUNCES = 2;
  public static final int POUNDS = 3;
  public static final int TONS = 4;
  public static final int SHORT_TONS = 5;
  public static final int LONG_TONS = 6;
  public static final int MAXUNIT = 6;

  // Index Typed factory methods
  public static final Mass newMass(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Mass(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Mass newMass(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Mass((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newMass(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newMass(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Mass)) throw new IllegalArgumentException();
    return new Mass(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Mass)) throw new IllegalArgumentException();
    return new Mass(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Mass(theValue*scale,0);
  }

  public final Measure negate() {
    return newMass(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newMass(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Mass(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Mass(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Mass)) {
      throw new IllegalArgumentException("Expecting a Mass/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getGrams() {
    return (theValue);
  }
  public double getKilograms() {
    return (theValue/GRAMS_PER_KILOGRAMS);
  }
  public double getOunces() {
    return (theValue/GRAMS_PER_OUNCES);
  }
  public double getPounds() {
    return (theValue/GRAMS_PER_POUNDS);
  }
  public double getTons() {
    return (theValue/GRAMS_PER_TONS);
  }
  public double getShortTons() {
    return (theValue/GRAMS_PER_SHORT_TONS);
  }
  public double getLongTons() {
    return (theValue/GRAMS_PER_LONG_TONS);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Mass &&
             theValue == ((Mass) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "g";
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
