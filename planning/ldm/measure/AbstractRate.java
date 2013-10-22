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
/** Immutable implementation of AbstractRate.
 **/


package org.cougaar.planning.ldm.measure;
import java.io.*;



/** @deprecated Use a real Rate Measure instead **/
public final class AbstractRate extends AbstractMeasure implements Externalizable {
  // Conversion factor constants
  public static final double PER_SECOND_PER_PER_MINUTE = (1.0d/60);
  public static final double PER_MINUTE_PER_PER_SECOND = 60;
  public static final double PER_SECOND_PER_PER_HOUR = (1.0d/3600);
  public static final double PER_HOUR_PER_PER_SECOND = 3600;
  public static final double PER_SECOND_PER_PER_DAY = (1.0d/86400);
  public static final double PER_DAY_PER_PER_SECOND = 86400;
  public static final double PER_SECOND_PER_PER_WEEK = (1.0d/604800);
  public static final double PER_WEEK_PER_PER_SECOND = 604800;
  public static final double PER_SECOND_PER_PER_MILLISECOND = 1000;
  public static final double PER_MILLISECOND_PER_PER_SECOND = (1.0d/1000);
  public static final double PER_SECOND_PER_PER_KILOSECOND = (1.0d/1000);
  public static final double PER_KILOSECOND_PER_PER_SECOND = 1000;

  // the value is stored as per_second
  private double theValue;

  /** No-arg constructor is only for use by serialization **/
  public AbstractRate() {}

  // private constructor
  private AbstractRate(double v) {
    theValue = v;
  }

  /** parameterized constructor **/
  public AbstractRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      theValue = v*getConvFactor(unit);
    else
      throw new UnknownUnitException();
  }

  /** takes strings of the form "Number unit" **/
  public AbstractRate(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("persecond")) 
      theValue=n;
    else if (u.equals("perminute")) 
      theValue=n*PER_SECOND_PER_PER_MINUTE;
    else if (u.equals("perhour")) 
      theValue=n*PER_SECOND_PER_PER_HOUR;
    else if (u.equals("perday")) 
      theValue=n*PER_SECOND_PER_PER_DAY;
    else if (u.equals("perweek")) 
      theValue=n*PER_SECOND_PER_PER_WEEK;
    else if (u.equals("permillisecond")) 
      theValue=n*PER_SECOND_PER_PER_MILLISECOND;
    else if (u.equals("perkilosecond")) 
      theValue=n*PER_SECOND_PER_PER_KILOSECOND;
    else 
      throw new UnknownUnitException();
  }

  // TypeNamed factory methods
  public static final AbstractRate newPerSecond(double v) {
    return new AbstractRate(v);
  }
  public static final AbstractRate newPerSecond(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue()));
  }
  public static final AbstractRate newPerMinute(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_MINUTE);
  }
  public static final AbstractRate newPerMinute(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_MINUTE);
  }
  public static final AbstractRate newPerHour(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_HOUR);
  }
  public static final AbstractRate newPerHour(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_HOUR);
  }
  public static final AbstractRate newPerDay(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_DAY);
  }
  public static final AbstractRate newPerDay(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_DAY);
  }
  public static final AbstractRate newPerWeek(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_WEEK);
  }
  public static final AbstractRate newPerWeek(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_WEEK);
  }
  public static final AbstractRate newPerMillisecond(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_MILLISECOND);
  }
  public static final AbstractRate newPerMillisecond(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_MILLISECOND);
  }
  public static final AbstractRate newPerKilosecond(double v) {
    return new AbstractRate(v*PER_SECOND_PER_PER_KILOSECOND);
  }
  public static final AbstractRate newPerKilosecond(String s) {
    return new AbstractRate((Double.valueOf(s).doubleValue())*PER_SECOND_PER_PER_KILOSECOND);
  }


  public int getCommonUnit() {
    return PER_HOUR;
  }

  public int getMaxUnit() { return MAXUNIT; }

  // unit names for getUnitName
  private static final String unitNames[]={
    "per_second",
    "per_minute",
    "per_hour",
    "per_day",
    "per_week",
    "per_millisecond",
    "per_kilosecond",
  };

  public String getUnitName(int unit) {
    return unitNames[unit];
  }

  // Index Typed factory methods
  static final double convFactor[]={
    1.0,
    PER_SECOND_PER_PER_MINUTE,
    PER_SECOND_PER_PER_HOUR,
    PER_SECOND_PER_PER_DAY,
    PER_SECOND_PER_PER_WEEK,
    PER_SECOND_PER_PER_MILLISECOND,
    PER_SECOND_PER_PER_KILOSECOND,
  };

  public static final double getConvFactor(int i) { return convFactor[i]; }

  // indexes into factor array
  public static final int PER_SECOND = 0;
  public static final int PER_MINUTE = 1;
  public static final int PER_HOUR = 2;
  public static final int PER_DAY = 3;
  public static final int PER_WEEK = 4;
  public static final int PER_MILLISECOND = 5;
  public static final int PER_KILOSECOND = 6;
  public static final int MAXUNIT = 6;

  // Index Typed factory methods
  public static final AbstractRate newAbstractRate(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new AbstractRate(v*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public static final AbstractRate newAbstractRate(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new AbstractRate((Double.valueOf(s).doubleValue())*getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static final AbstractMeasure newMeasure(String s, int unit) {
    return newAbstractRate(s, unit);
  }
  public static final AbstractMeasure newMeasure(double v, int unit) {
    return newAbstractRate(v, unit);
  }
  // simple math : addition and subtraction
  public final Measure add(Measure toAdd) {
    if (!(toAdd instanceof AbstractRate)) throw new IllegalArgumentException();
    return new AbstractRate(theValue + toAdd.getNativeValue());
  }
  public final Measure subtract(Measure toSubtract) {
    if (!(toSubtract instanceof AbstractRate)) throw new IllegalArgumentException();
    return new AbstractRate(theValue - toSubtract.getNativeValue());
  }

  public final Measure scale(double scale) {
    return new AbstractRate(theValue*scale,0);
  }

  public final Measure negate() {
    return newAbstractRate(-1 * theValue,0);
  }

  public final Measure floor(int unit) {
    return newAbstractRate(Math.floor(getValue(unit)),unit);
  }

  public final Measure valueOf(double value) {
    return new AbstractRate(value);
  }

  public final Measure valueOf(double value, int unit) {
    return new AbstractRate(value, unit);
  }

  public final double getNativeValue() {
    return theValue;
  }

  public final int getNativeUnit() {
    return 0;
  }

  public final Duration divide(Rate toRate) {
    Measure canonicalNumerator = toRate.getCanonicalNumerator();
    if (!(toRate.getCanonicalNumerator() instanceof AbstractRate)) {
      throw new IllegalArgumentException("Expecting a AbstractRate/Duration");
    }
    int durationNativeUnit = toRate.getCanonicalDenominator().getNativeUnit();  // seconds
    double value = toRate.getValue(canonicalNumerator.getNativeUnit(), durationNativeUnit); // ?/seconds
    return new Duration(theValue/ value,durationNativeUnit);  // ?/?/second = seconds
  }

  // Unit-based Reader methods
  public double getPerSecond() {
    return (theValue);
  }
  public double getPerMinute() {
    return (theValue/PER_SECOND_PER_PER_MINUTE);
  }
  public double getPerHour() {
    return (theValue/PER_SECOND_PER_PER_HOUR);
  }
  public double getPerDay() {
    return (theValue/PER_SECOND_PER_PER_DAY);
  }
  public double getPerWeek() {
    return (theValue/PER_SECOND_PER_PER_WEEK);
  }
  public double getPerMillisecond() {
    return (theValue/PER_SECOND_PER_PER_MILLISECOND);
  }
  public double getPerKilosecond() {
    return (theValue/PER_SECOND_PER_PER_KILOSECOND);
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return (theValue/getConvFactor(unit));
    else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof AbstractRate &&
             theValue == ((AbstractRate) o).theValue);
  }
  public String toString() {
    return Double.toString(theValue) + "1/s";
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
