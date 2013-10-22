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
/** Immutable implementation of Power.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Power extends AbstractMeasure implements Externalizable {
  // Conversion factor constants
  public static final double WATTS_PER_KILOWATTS = (1.0d/0.001);
  public static final double KILOWATTS_PER_WATTS = 0.001;
  public static final double WATTS_PER_HORSEPOWER = (1.0d/0.0013410221);
  public static final double HORSEPOWER_PER_WATTS = 0.0013410221;

  // the value is stored as watts
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Power() {}

  // private constructor
  private Power(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Power(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Power(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("watts")) 
      theValue=n;
    else if (u.equals("kilowatts")) 
      theValue=n*WATTS_PER_KILOWATTS;
    else if (u.equals("horsepower")) 
      theValue=n*WATTS_PER_HORSEPOWER;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Power newWatts(double v) {
    return new Power(v);
  }
  public static final Power newWatts(String s) {
    return new Power((Double.valueOf(s).doubleValue()));
  }
  public static final Power newKilowatts(double v) {
    return new Power(v*WATTS_PER_KILOWATTS);
  }
  public static final Power newKilowatts(String s) {
    return new Power((Double.valueOf(s).doubleValue())*WATTS_PER_KILOWATTS);
  }
  public static final Power newHorsepower(double v) {
    return new Power(v*WATTS_PER_HORSEPOWER);
  }
  public static final Power newHorsepower(String s) {
    return new Power((Double.valueOf(s).doubleValue())*WATTS_PER_HORSEPOWER);
  }


  public int getCommonUnit() {
    return KILOWATTS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "watts",
    "kilowatts",
    "horsepower",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    WATTS_PER_KILOWATTS,
    WATTS_PER_HORSEPOWER,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int WATTS = 0;
  public static final int KILOWATTS = 1;
  public static final int HORSEPOWER = 2;
  public static final int MAXUNIT = 2;

  // Index Typed factory methods
  public static final Power newPower(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Power(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Power newPower(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Power((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newPower(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newPower(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Power)) throw new IllegalArgumentException();
    return new Power(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Power)) throw new IllegalArgumentException();
    return new Power(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Power(theValue*scale,0);
  }

  public final Measure negate() {
    return newPower(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newPower(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Power(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Power(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Power)) {
      throw new IllegalArgumentException("Expecting a Power/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getWatts() {
    return (theValue);
  }
  public double getKilowatts() {
    return (theValue/WATTS_PER_KILOWATTS);
  }
  public double getHorsepower() {
    return (theValue/WATTS_PER_HORSEPOWER);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Power &&
             theValue == ((Power) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "w";
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
