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
/** Immutable implementation of Distance.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Distance extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double METERS_PER_MILES = 1609.344;
  public static final double MILES_PER_METERS = (1.0d/1609.344);
  public static final double METERS_PER_NAUTICAL_MILES = 1852.0;
  public static final double NAUTICAL_MILES_PER_METERS = (1.0d/1852.0);
  public static final double METERS_PER_YARDS = 0.9414;
  public static final double YARDS_PER_METERS = (1.0d/0.9414);
  public static final double METERS_PER_FEET = 0.3048;
  public static final double FEET_PER_METERS = (1.0d/0.3048);
  public static final double METERS_PER_INCHES = 0.0254;
  public static final double INCHES_PER_METERS = (1.0d/0.0254);
  public static final double METERS_PER_KILOMETERS = 1000.0;
  public static final double KILOMETERS_PER_METERS = (1.0d/1000.0);
  public static final double METERS_PER_CENTIMETERS = (1.0d/100);
  public static final double CENTIMETERS_PER_METERS = 100;
  public static final double METERS_PER_MILLIMETERS = (1.0d/1000);
  public static final double MILLIMETERS_PER_METERS = 1000;
  public static final double METERS_PER_FURLONGS = 201.168;
  public static final double FURLONGS_PER_METERS = (1.0d/201.168);

  // the value is stored as meters
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Distance() {}

  // private constructor
  private Distance(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Distance(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Distance(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("meters")) 
      theValue=n;
    else if (u.equals("miles")) 
      theValue=n*METERS_PER_MILES;
    else if (u.equals("nauticalmiles")) 
      theValue=n*METERS_PER_NAUTICAL_MILES;
    else if (u.equals("yards")) 
      theValue=n*METERS_PER_YARDS;
    else if (u.equals("feet")) 
      theValue=n*METERS_PER_FEET;
    else if (u.equals("inches")) 
      theValue=n*METERS_PER_INCHES;
    else if (u.equals("kilometers")) 
      theValue=n*METERS_PER_KILOMETERS;
    else if (u.equals("centimeters")) 
      theValue=n*METERS_PER_CENTIMETERS;
    else if (u.equals("millimeters")) 
      theValue=n*METERS_PER_MILLIMETERS;
    else if (u.equals("furlongs")) 
      theValue=n*METERS_PER_FURLONGS;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Distance newMeters(double v) {
    return new Distance(v);
  }
  public static final Distance newMeters(String s) {
    return new Distance((Double.valueOf(s).doubleValue()));
  }
  public static final Distance newMiles(double v) {
    return new Distance(v*METERS_PER_MILES);
  }
  public static final Distance newMiles(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_MILES);
  }
  public static final Distance newNauticalMiles(double v) {
    return new Distance(v*METERS_PER_NAUTICAL_MILES);
  }
  public static final Distance newNauticalMiles(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_NAUTICAL_MILES);
  }
  public static final Distance newYards(double v) {
    return new Distance(v*METERS_PER_YARDS);
  }
  public static final Distance newYards(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_YARDS);
  }
  public static final Distance newFeet(double v) {
    return new Distance(v*METERS_PER_FEET);
  }
  public static final Distance newFeet(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_FEET);
  }
  public static final Distance newInches(double v) {
    return new Distance(v*METERS_PER_INCHES);
  }
  public static final Distance newInches(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_INCHES);
  }
  public static final Distance newKilometers(double v) {
    return new Distance(v*METERS_PER_KILOMETERS);
  }
  public static final Distance newKilometers(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_KILOMETERS);
  }
  public static final Distance newCentimeters(double v) {
    return new Distance(v*METERS_PER_CENTIMETERS);
  }
  public static final Distance newCentimeters(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_CENTIMETERS);
  }
  public static final Distance newMillimeters(double v) {
    return new Distance(v*METERS_PER_MILLIMETERS);
  }
  public static final Distance newMillimeters(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_MILLIMETERS);
  }
  public static final Distance newFurlongs(double v) {
    return new Distance(v*METERS_PER_FURLONGS);
  }
  public static final Distance newFurlongs(String s) {
    return new Distance((Double.valueOf(s).doubleValue())*METERS_PER_FURLONGS);
  }


  public int getCommonUnit() {
    return METERS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "meters",
    "miles",
    "nautical_miles",
    "yards",
    "feet",
    "inches",
    "kilometers",
    "centimeters",
    "millimeters",
    "furlongs",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    METERS_PER_MILES,
    METERS_PER_NAUTICAL_MILES,
    METERS_PER_YARDS,
    METERS_PER_FEET,
    METERS_PER_INCHES,
    METERS_PER_KILOMETERS,
    METERS_PER_CENTIMETERS,
    METERS_PER_MILLIMETERS,
    METERS_PER_FURLONGS,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int METERS = 0;
  public static final int MILES = 1;
  public static final int NAUTICAL_MILES = 2;
  public static final int YARDS = 3;
  public static final int FEET = 4;
  public static final int INCHES = 5;
  public static final int KILOMETERS = 6;
  public static final int CENTIMETERS = 7;
  public static final int MILLIMETERS = 8;
  public static final int FURLONGS = 9;
  public static final int MAXUNIT = 9;

  // Index Typed factory methods
  public static final Distance newDistance(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Distance(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Distance newDistance(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Distance((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newDistance(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newDistance(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Distance)) throw new IllegalArgumentException();
    return new Distance(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Distance)) throw new IllegalArgumentException();
    return new Distance(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Distance(theValue*scale,0);
  }

  public final Measure negate() {
    return newDistance(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newDistance(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Distance(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Distance(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Distance)) {
      throw new IllegalArgumentException("Expecting a Distance/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getMeters() {
    return (theValue);
  }
  public double getMiles() {
    return (theValue/METERS_PER_MILES);
  }
  public double getNauticalMiles() {
    return (theValue/METERS_PER_NAUTICAL_MILES);
  }
  public double getYards() {
    return (theValue/METERS_PER_YARDS);
  }
  public double getFeet() {
    return (theValue/METERS_PER_FEET);
  }
  public double getInches() {
    return (theValue/METERS_PER_INCHES);
  }
  public double getKilometers() {
    return (theValue/METERS_PER_KILOMETERS);
  }
  public double getCentimeters() {
    return (theValue/METERS_PER_CENTIMETERS);
  }
  public double getMillimeters() {
    return (theValue/METERS_PER_MILLIMETERS);
  }
  public double getFurlongs() {
    return (theValue/METERS_PER_FURLONGS);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Distance &&
             theValue == ((Distance) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "m";
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
