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
/** Immutable implementation of Area.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Area extends Scalar implements Externalizable {
  // Conversion factor constants
  public static final double SQUARE_METERS_PER_SQUARE_FEET = (1.0d/10.76391);
  public static final double SQUARE_FEET_PER_SQUARE_METERS = 10.76391;
  public static final double SQUARE_METERS_PER_SQUARE_YARDS = (1.0d/1.19599);
  public static final double SQUARE_YARDS_PER_SQUARE_METERS = 1.19599;
  public static final double SQUARE_METERS_PER_ACRES = 4046.8564;
  public static final double ACRES_PER_SQUARE_METERS = (1.0d/4046.8564);
  public static final double SQUARE_METERS_PER_HECTARES = 10000;
  public static final double HECTARES_PER_SQUARE_METERS = (1.0d/10000);
  public static final double SQUARE_METERS_PER_SQUARE_INCHES = (1.0d/1550.0031);
  public static final double SQUARE_INCHES_PER_SQUARE_METERS = 1550.0031;

  // the value is stored as square_meters
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Area() {}

  // private constructor
  private Area(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Area(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Area(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("squaremeters")) 
      theValue=n;
    else if (u.equals("squarefeet")) 
      theValue=n*SQUARE_METERS_PER_SQUARE_FEET;
    else if (u.equals("squareyards")) 
      theValue=n*SQUARE_METERS_PER_SQUARE_YARDS;
    else if (u.equals("acres")) 
      theValue=n*SQUARE_METERS_PER_ACRES;
    else if (u.equals("hectares")) 
      theValue=n*SQUARE_METERS_PER_HECTARES;
    else if (u.equals("squareinches")) 
      theValue=n*SQUARE_METERS_PER_SQUARE_INCHES;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Area newSquareMeters(double v) {
    return new Area(v);
  }
  public static final Area newSquareMeters(String s) {
    return new Area((Double.valueOf(s).doubleValue()));
  }
  public static final Area newSquareFeet(double v) {
    return new Area(v*SQUARE_METERS_PER_SQUARE_FEET);
  }
  public static final Area newSquareFeet(String s) {
    return new Area((Double.valueOf(s).doubleValue())*SQUARE_METERS_PER_SQUARE_FEET);
  }
  public static final Area newSquareYards(double v) {
    return new Area(v*SQUARE_METERS_PER_SQUARE_YARDS);
  }
  public static final Area newSquareYards(String s) {
    return new Area((Double.valueOf(s).doubleValue())*SQUARE_METERS_PER_SQUARE_YARDS);
  }
  public static final Area newAcres(double v) {
    return new Area(v*SQUARE_METERS_PER_ACRES);
  }
  public static final Area newAcres(String s) {
    return new Area((Double.valueOf(s).doubleValue())*SQUARE_METERS_PER_ACRES);
  }
  public static final Area newHectares(double v) {
    return new Area(v*SQUARE_METERS_PER_HECTARES);
  }
  public static final Area newHectares(String s) {
    return new Area((Double.valueOf(s).doubleValue())*SQUARE_METERS_PER_HECTARES);
  }
  public static final Area newSquareInches(double v) {
    return new Area(v*SQUARE_METERS_PER_SQUARE_INCHES);
  }
  public static final Area newSquareInches(String s) {
    return new Area((Double.valueOf(s).doubleValue())*SQUARE_METERS_PER_SQUARE_INCHES);
  }


  public int getCommonUnit() {
    return SQUARE_FEET;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "square_meters",
    "square_feet",
    "square_yards",
    "acres",
    "hectares",
    "square_inches",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    SQUARE_METERS_PER_SQUARE_FEET,
    SQUARE_METERS_PER_SQUARE_YARDS,
    SQUARE_METERS_PER_ACRES,
    SQUARE_METERS_PER_HECTARES,
    SQUARE_METERS_PER_SQUARE_INCHES,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int SQUARE_METERS = 0;
  public static final int SQUARE_FEET = 1;
  public static final int SQUARE_YARDS = 2;
  public static final int ACRES = 3;
  public static final int HECTARES = 4;
  public static final int SQUARE_INCHES = 5;
  public static final int MAXUNIT = 5;

  // Index Typed factory methods
  public static final Area newArea(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Area(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Area newArea(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Area((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newArea(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newArea(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Area)) throw new IllegalArgumentException();
    return new Area(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Area)) throw new IllegalArgumentException();
    return new Area(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Area(theValue*scale,0);
  }

  public final Measure negate() {
    return newArea(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newArea(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Area(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Area(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Area)) {
      throw new IllegalArgumentException("Expecting a Area/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getSquareMeters() {
    return (theValue);
  }
  public double getSquareFeet() {
    return (theValue/SQUARE_METERS_PER_SQUARE_FEET);
  }
  public double getSquareYards() {
    return (theValue/SQUARE_METERS_PER_SQUARE_YARDS);
  }
  public double getAcres() {
    return (theValue/SQUARE_METERS_PER_ACRES);
  }
  public double getHectares() {
    return (theValue/SQUARE_METERS_PER_HECTARES);
  }
  public double getSquareInches() {
    return (theValue/SQUARE_METERS_PER_SQUARE_INCHES);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Area &&
             theValue == ((Area) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "m^2";
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
