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
/** Immutable implementation of Energy.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



public final class Energy extends AbstractMeasure implements Externalizable {
  // Conversion factor constants
  public static final double CAL_PER_JOULE = (1.0d/4.1868);
  public static final double JOULE_PER_CAL = 4.1868;
  public static final double CAL_PER_KCAL = 1000;
  public static final double KCAL_PER_CAL = (1.0d/1000);

  // the value is stored as cal
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public Energy() {}

  // private constructor
  private Energy(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public Energy(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public Energy(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("cal")) 
      theValue=n;
    else if (u.equals("joule")) 
      theValue=n*CAL_PER_JOULE;
    else if (u.equals("kcal")) 
      theValue=n*CAL_PER_KCAL;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final Energy newCal(double v) {
    return new Energy(v);
  }
  public static final Energy newCal(String s) {
    return new Energy((Double.valueOf(s).doubleValue()));
  }
  public static final Energy newJoule(double v) {
    return new Energy(v*CAL_PER_JOULE);
  }
  public static final Energy newJoule(String s) {
    return new Energy((Double.valueOf(s).doubleValue())*CAL_PER_JOULE);
  }
  public static final Energy newKcal(double v) {
    return new Energy(v*CAL_PER_KCAL);
  }
  public static final Energy newKcal(String s) {
    return new Energy((Double.valueOf(s).doubleValue())*CAL_PER_KCAL);
  }


  public int getCommonUnit() {
    return 0;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "cal",
    "joule",
    "kcal",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    CAL_PER_JOULE,
    CAL_PER_KCAL,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int CAL = 0;
  public static final int JOULE = 1;
  public static final int KCAL = 2;
  public static final int MAXUNIT = 2;

  // Index Typed factory methods
  public static final Energy newEnergy(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Energy(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final Energy newEnergy(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Energy((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newEnergy(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newEnergy(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof Energy)) throw new IllegalArgumentException();
    return new Energy(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof Energy)) throw new IllegalArgumentException();
    return new Energy(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new Energy(theValue*scale,0);
  }

  public final Measure negate() {
    return newEnergy(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newEnergy(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new Energy(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new Energy(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof Energy)) {
      throw new IllegalArgumentException("Expecting a Energy/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getCal() {
    return (theValue);
  }
  public double getJoule() {
    return (theValue/CAL_PER_JOULE);
  }
  public double getKcal() {
    return (theValue/CAL_PER_KCAL);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Energy &&
             theValue == ((Energy) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "cal";
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
