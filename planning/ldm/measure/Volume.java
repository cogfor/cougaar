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
/** Immutable implementation of Volume.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Volume extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double LITERS_PER_OUNCES = (1.0d/33.814023);
  public static final double OUNCES_PER_LITERS = 33.814023;
  public static final double LITERS_PER_GALLONS = 3.785412;
  public static final double GALLONS_PER_LITERS = (1.0d/3.785412);
  public static final double LITERS_PER_IMPERIAL_GALLONS = 4.546090;
  public static final double IMPERIAL_GALLONS_PER_LITERS = (1.0d/4.546090);
  public static final double LITERS_PER_CUBIC_FEET = 28.316847;
  public static final double CUBIC_FEET_PER_LITERS = (1.0d/28.316847);
  public static final double LITERS_PER_CUBIC_YARDS = 764.55486;
  public static final double CUBIC_YARDS_PER_LITERS = (1.0d/764.55486);
  public static final double LITERS_PER_MTONS = 1132.67388;
  public static final double MTONS_PER_LITERS = (1.0d/1132.67388);
  public static final double LITERS_PER_CUBIC_CENTIMETERS = (1.0d/1000);
  public static final double CUBIC_CENTIMETERS_PER_LITERS = 1000;
  public static final double LITERS_PER_CUBIC_METERS = 1000;
  public static final double CUBIC_METERS_PER_LITERS = (1.0d/1000);
  public static final double LITERS_PER_BARRELS = 158.98729;
  public static final double BARRELS_PER_LITERS = (1.0d/158.98729);

  // the value is stored as liters
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Volume() {}

  // private constructor
  private Volume(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Volume(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Volume(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("liters")) 
      theValue=n;
    else if (u.equals("ounces")) 
      theValue=n*LITERS_PER_OUNCES;
    else if (u.equals("gallons")) 
      theValue=n*LITERS_PER_GALLONS;
    else if (u.equals("imperialgallons")) 
      theValue=n*LITERS_PER_IMPERIAL_GALLONS;
    else if (u.equals("cubicfeet")) 
      theValue=n*LITERS_PER_CUBIC_FEET;
    else if (u.equals("cubicyards")) 
      theValue=n*LITERS_PER_CUBIC_YARDS;
    else if (u.equals("mtons")) 
      theValue=n*LITERS_PER_MTONS;
    else if (u.equals("cubiccentimeters")) 
      theValue=n*LITERS_PER_CUBIC_CENTIMETERS;
    else if (u.equals("cubicmeters")) 
      theValue=n*LITERS_PER_CUBIC_METERS;
    else if (u.equals("barrels")) 
      theValue=n*LITERS_PER_BARRELS;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Volume newLiters(double v) {
    return new Volume(v);
  }
  public static final Volume newLiters(String s) {
    return new Volume((Double.valueOf(s).doubleValue()));
  }
  public static final Volume newOunces(double v) {
    return new Volume(v*LITERS_PER_OUNCES);
  }
  public static final Volume newOunces(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_OUNCES);
  }
  public static final Volume newGallons(double v) {
    return new Volume(v*LITERS_PER_GALLONS);
  }
  public static final Volume newGallons(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_GALLONS);
  }
  public static final Volume newImperialGallons(double v) {
    return new Volume(v*LITERS_PER_IMPERIAL_GALLONS);
  }
  public static final Volume newImperialGallons(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_IMPERIAL_GALLONS);
  }
  public static final Volume newCubicFeet(double v) {
    return new Volume(v*LITERS_PER_CUBIC_FEET);
  }
  public static final Volume newCubicFeet(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_CUBIC_FEET);
  }
  public static final Volume newCubicYards(double v) {
    return new Volume(v*LITERS_PER_CUBIC_YARDS);
  }
  public static final Volume newCubicYards(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_CUBIC_YARDS);
  }
  public static final Volume newMtons(double v) {
    return new Volume(v*LITERS_PER_MTONS);
  }
  public static final Volume newMtons(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_MTONS);
  }
  public static final Volume newCubicCentimeters(double v) {
    return new Volume(v*LITERS_PER_CUBIC_CENTIMETERS);
  }
  public static final Volume newCubicCentimeters(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_CUBIC_CENTIMETERS);
  }
  public static final Volume newCubicMeters(double v) {
    return new Volume(v*LITERS_PER_CUBIC_METERS);
  }
  public static final Volume newCubicMeters(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_CUBIC_METERS);
  }
  public static final Volume newBarrels(double v) {
    return new Volume(v*LITERS_PER_BARRELS);
  }
  public static final Volume newBarrels(String s) {
    return new Volume((Double.valueOf(s).doubleValue())*LITERS_PER_BARRELS);
  }


  public int getCommonUnit() {
    return GALLONS;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "liters",
    "ounces",
    "gallons",
    "imperial_gallons",
    "cubic_feet",
    "cubic_yards",
    "mtons",
    "cubic_centimeters",
    "cubic_meters",
    "barrels",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    LITERS_PER_OUNCES,
    LITERS_PER_GALLONS,
    LITERS_PER_IMPERIAL_GALLONS,
    LITERS_PER_CUBIC_FEET,
    LITERS_PER_CUBIC_YARDS,
    LITERS_PER_MTONS,
    LITERS_PER_CUBIC_CENTIMETERS,
    LITERS_PER_CUBIC_METERS,
    LITERS_PER_BARRELS,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int LITERS = 0;
  public static final int OUNCES = 1;
  public static final int GALLONS = 2;
  public static final int IMPERIAL_GALLONS = 3;
  public static final int CUBIC_FEET = 4;
  public static final int CUBIC_YARDS = 5;
  public static final int MTONS = 6;
  public static final int CUBIC_CENTIMETERS = 7;
  public static final int CUBIC_METERS = 8;
  public static final int BARRELS = 9;
  public static final int MAXUNIT = 9;

  // Index Typed factory methods
  public static final Volume newVolume(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Volume(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Volume newVolume(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Volume((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newVolume(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newVolume(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Volume)) throw new IllegalArgumentException();
    return new Volume(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Volume)) throw new IllegalArgumentException();
    return new Volume(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Volume(theValue*scale,0);
  }

  public final Measure negate() {
    return newVolume(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newVolume(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Volume(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Volume(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Volume)) {
      throw new IllegalArgumentException("Expecting a Volume/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getLiters() {
    return (theValue);
  }
  public double getOunces() {
    return (theValue/LITERS_PER_OUNCES);
  }
  public double getGallons() {
    return (theValue/LITERS_PER_GALLONS);
  }
  public double getImperialGallons() {
    return (theValue/LITERS_PER_IMPERIAL_GALLONS);
  }
  public double getCubicFeet() {
    return (theValue/LITERS_PER_CUBIC_FEET);
  }
  public double getCubicYards() {
    return (theValue/LITERS_PER_CUBIC_YARDS);
  }
  public double getMtons() {
    return (theValue/LITERS_PER_MTONS);
  }
  public double getCubicCentimeters() {
    return (theValue/LITERS_PER_CUBIC_CENTIMETERS);
  }
  public double getCubicMeters() {
    return (theValue/LITERS_PER_CUBIC_METERS);
  }
  public double getBarrels() {
    return (theValue/LITERS_PER_BARRELS);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Volume &&
             theValue == ((Volume) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "l";
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
